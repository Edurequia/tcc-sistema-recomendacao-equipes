package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.entity.Recomendacao;
import br.ufn.tfg.teamrec.domain.entity.RecomendacaoMembro;
import br.ufn.tfg.teamrec.domain.enums.OrigemRecomendacao;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.domain.repository.AlocacaoRepository;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.HorasAlocadasProjection;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.domain.repository.RecomendacaoRepository;
import br.ufn.tfg.teamrec.dto.response.LinhaBaseResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResumoResponse;
import br.ufn.tfg.teamrec.integration.prediction.CandidatoVetor;
import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoClient;
import br.ufn.tfg.teamrec.integration.prediction.Predicao;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorRequest;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorResponse;
import br.ufn.tfg.teamrec.mapper.RecomendacaoMapper;
import br.ufn.tfg.teamrec.service.composicao.CandidatoRanqueado;
import br.ufn.tfg.teamrec.service.composicao.ComposicaoEquipe;
import br.ufn.tfg.teamrec.service.composicao.LinhaBaseAleatoria;
import br.ufn.tfg.teamrec.service.composicao.ResultadoLinhaBase;
import br.ufn.tfg.teamrec.service.composicao.MembroSelecionado;
import br.ufn.tfg.teamrec.service.composicao.MetricasEquipe;
import br.ufn.tfg.teamrec.service.composicao.RequisitosEquipe;
import br.ufn.tfg.teamrec.service.composicao.ResultadoComposicao;
import br.ufn.tfg.teamrec.service.composicao.TecnologiaExigida;
import br.ufn.tfg.teamrec.shared.exception.RecursoNaoEncontradoException;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestra a geracao de recomendacoes: le o dominio, consulta o motor de
 * predicao e grava o ranking.
 *
 * A geracao usa transacoes programaticas em vez de {@code @Transactional} no
 * metodo inteiro. A chamada ao motor pode levar ate o read-timeout configurado;
 * dentro de uma transacao, isso seguraria uma conexao do banco durante toda a
 * espera. Por isso sao tres etapas: leitura, chamada sem transacao, gravacao.
 */
@Service
public class RecomendacaoService {

    private static final List<StatusProjeto> STATUS_QUE_CONSOMEM_ALOCACAO =
            List.of(StatusProjeto.PLANEJAMENTO, StatusProjeto.EM_ANDAMENTO);

    private final ProjetoRepository projetoRepository;
    private final DesenvolvedorRepository desenvolvedorRepository;
    private final AlocacaoRepository alocacaoRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final MontadorVetorAtributos montador;
    private final MotorPredicaoClient motor;
    private final ComposicaoEquipe composicaoEquipe;
    private final LinhaBaseAleatoria linhaBaseAleatoria;
    private final RecomendacaoMapper mapper;
    private final TransactionTemplate transacaoLeitura;
    private final TransactionTemplate transacaoEscrita;

    public RecomendacaoService(ProjetoRepository projetoRepository,
                               DesenvolvedorRepository desenvolvedorRepository,
                               AlocacaoRepository alocacaoRepository,
                               RecomendacaoRepository recomendacaoRepository,
                               MontadorVetorAtributos montador,
                               MotorPredicaoClient motor,
                               ComposicaoEquipe composicaoEquipe,
                               LinhaBaseAleatoria linhaBaseAleatoria,
                               RecomendacaoMapper mapper,
                               PlatformTransactionManager transactionManager) {
        this.projetoRepository = projetoRepository;
        this.desenvolvedorRepository = desenvolvedorRepository;
        this.alocacaoRepository = alocacaoRepository;
        this.recomendacaoRepository = recomendacaoRepository;
        this.montador = montador;
        this.motor = motor;
        this.composicaoEquipe = composicaoEquipe;
        this.linhaBaseAleatoria = linhaBaseAleatoria;
        this.mapper = mapper;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------
    // Geracao
    // ------------------------------------------------------------------

    /**
     * Pontua todos os desenvolvedores ativos para o projeto, compoe a equipe
     * a partir do ranking e grava o resultado com as metricas de avaliacao.
     */
    public RecomendacaoResponse gerar(Long projetoId) {
        Preparacao preparacao = transacaoLeitura.execute(status -> preparar(projetoId));
        PredicaoMotorResponse resposta = motor.prever(preparacao.requisicao());
        return transacaoEscrita.execute(status -> gravar(projetoId, preparacao, resposta));
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public RecomendacaoResponse buscarPorId(Long id) {
        return mapper.toResponse(recomendacaoRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", id)));
    }

    /**
     * Sorteia equipes ao acaso entre os elegiveis desta recomendacao e compara
     * suas metricas com as da equipe proposta.
     *
     * Usa os escores e a cobertura de horas gravados na recomendacao, entao o
     * grupo sorteado e exatamente aquele de onde a equipe foi escolhida. Os
     * requisitos, a senioridade e as competencias sao os atuais: para a tabela
     * de resultados, gere a linha de base logo apos a recomendacao.
     */
    @Transactional(readOnly = true)
    public LinhaBaseResponse linhaBase(Long recomendacaoId, int sorteios, long semente) {
        Recomendacao recomendacao = recomendacaoRepository.buscarCompleta(recomendacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", recomendacaoId));

        Long projetoId = recomendacao.getProjeto().getId();
        Projeto projeto = projetoRepository.buscarComPapeisRequeridos(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", projetoId));
        projetoRepository.buscarComHabilidadesRequeridas(projetoId);

        Map<Long, Set<Long>> competencias = desenvolvedorRepository.buscarComHabilidadesPorIds(
                        recomendacao.getMembros().stream().map(m -> m.getDesenvolvedor().getId()).toList())
                .stream()
                .collect(Collectors.toMap(Desenvolvedor::getId, PerfilCandidato::habilidadesDe));

        List<CandidatoRanqueado> candidatos = recomendacao.getMembros().stream()
                .map(membro -> candidatoDoMembro(membro, competencias))
                .toList();

        if (candidatos.stream().noneMatch(CandidatoRanqueado::elegivel)) {
            throw new RegraDeNegocioException(
                    "A recomendacao nao possui candidatos elegiveis para o sorteio da linha de base.");
        }

        ResultadoLinhaBase resultado = linhaBaseAleatoria.sortear(
                candidatos, requisitosDaEquipe(projeto), sorteios, semente);

        return new LinhaBaseResponse(recomendacaoId, projetoId, sorteios, semente,
                resultado.candidatosElegiveis(), resultado.tamanhoEquipeSorteada(),
                mapper.toMetricas(recomendacao),
                new LinhaBaseResponse.Estatisticas(
                        mapper.fromAgregadas(resultado.media()),
                        mapper.fromAgregadas(resultado.desvioPadrao())));
    }

    /**
     * O papel sugerido gravado e o papel principal do candidato no momento da
     * geracao (nas vagas por papel, ele coincide com o da vaga).
     */
    private static CandidatoRanqueado candidatoDoMembro(RecomendacaoMembro membro, Map<Long, Set<Long>> competencias) {
        Desenvolvedor desenvolvedor = membro.getDesenvolvedor();
        return new CandidatoRanqueado(desenvolvedor.getId(), desenvolvedor.getNome(), membro.getPapelSugerido(),
                desenvolvedor.getSenioridade().getValorOrdinal(),
                membro.getScoreAfinidade().doubleValue(),
                membro.getCoberturaHoras() == null ? 0 : membro.getCoberturaHoras().doubleValue(),
                competencias.getOrDefault(desenvolvedor.getId(), Set.of()));
    }

    @Transactional(readOnly = true)
    public List<RecomendacaoResumoResponse> listarPorProjeto(Long projetoId) {
        if (!projetoRepository.existsById(projetoId)) {
            throw new RecursoNaoEncontradoException("Projeto", projetoId);
        }
        return recomendacaoRepository.findByProjetoIdOrderByCriadoEmDesc(projetoId).stream()
                .map(mapper::toResumo)
                .toList();
    }

    // ------------------------------------------------------------------
    // Etapas da geracao
    // ------------------------------------------------------------------

    private Preparacao preparar(Long projetoId) {
        Projeto projeto = projetoRepository.buscarComPapeisRequeridos(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", projetoId));
        // Segunda consulta de proposito: inicializa a stack na mesma instancia,
        // sem o produto cartesiano de um JOIN FETCH duplo.
        projetoRepository.buscarComHabilidadesRequeridas(projetoId);

        if (!projeto.getStatus().isConsomeAlocacao()) {
            throw new RegraDeNegocioException(
                    "Nao e possivel gerar recomendacao para um projeto com status '%s'."
                            .formatted(projeto.getStatus().getDescricao()));
        }

        List<Desenvolvedor> candidatos = desenvolvedorRepository.buscarAtivosComHabilidades();
        if (candidatos.isEmpty()) {
            throw new RegraDeNegocioException("Nao ha desenvolvedores ativos para compor a recomendacao.");
        }

        Map<Long, Integer> horasEmOutrosProjetos = alocacaoRepository
                .somarHorasComprometidasForaDoProjeto(STATUS_QUE_CONSOMEM_ALOCACAO, projetoId).stream()
                .collect(Collectors.toMap(HorasAlocadasProjection::getDesenvolvedorId,
                        HorasAlocadasProjection::getTotalHoras));

        List<CandidatoVetor> vetores = candidatos.stream()
                .map(dev -> montador.vetorDoCandidato(dev, projeto,
                        horasEmOutrosProjetos.getOrDefault(dev.getId(), 0)))
                .toList();

        Map<Long, PerfilCandidato> perfis = candidatos.stream()
                .collect(Collectors.toMap(Desenvolvedor::getId, PerfilCandidato::de));

        return new Preparacao(
                new PredicaoMotorRequest(montador.contextoDoProjeto(projeto), vetores),
                perfis,
                requisitosDaEquipe(projeto));
    }

    private RecomendacaoResponse gravar(Long projetoId, Preparacao preparacao, PredicaoMotorResponse resposta) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", projetoId));

        Map<Long, CandidatoVetor> vetores = preparacao.requisicao().candidatos().stream()
                .collect(Collectors.toMap(CandidatoVetor::id, Function.identity()));
        Map<Long, Desenvolvedor> desenvolvedores = desenvolvedorRepository.findAllById(vetores.keySet()).stream()
                .collect(Collectors.toMap(Desenvolvedor::getId, Function.identity()));

        // Empate no escore e desfeito pelo nome, para o ranking ser deterministico.
        List<CandidatoRanqueado> ranking = resposta.predicoes().stream()
                .map(predicao -> preparacao.perfis().get(predicao.id())
                        .ranquear(predicao, vetores.get(predicao.id())))
                .sorted(Comparator.comparingDouble(CandidatoRanqueado::score).reversed()
                        .thenComparing(CandidatoRanqueado::nome))
                .toList();

        ResultadoComposicao composicao = composicaoEquipe.compor(ranking, preparacao.requisitos());
        Map<Long, PapelAtuacao> papelNaEquipe = composicao.equipe().stream()
                .collect(Collectors.toMap(m -> m.candidato().desenvolvedorId(), MembroSelecionado::papelSugerido));

        Recomendacao recomendacao = new Recomendacao(projeto, OrigemRecomendacao.FORMULARIO);
        aplicarMetricas(recomendacao, composicao);

        int posicao = 1;
        for (CandidatoRanqueado candidato : ranking) {
            long id = candidato.desenvolvedorId();
            boolean selecionado = papelNaEquipe.containsKey(id);

            RecomendacaoMembro membro = new RecomendacaoMembro(desenvolvedores.get(id),
                    escala(candidato.score(), 3),
                    selecionado ? papelNaEquipe.get(id) : candidato.papel(),
                    posicao++, selecionado);
            membro.setAderenciaTecnica(escala(vetores.get(id).aderenciaTecnica(), 4));
            membro.setCoberturaHoras(escala(candidato.coberturaHoras(), 4));
            recomendacao.adicionarMembro(membro);
        }

        return mapper.toResponse(recomendacaoRepository.save(recomendacao));
    }

    private static void aplicarMetricas(Recomendacao recomendacao, ResultadoComposicao composicao) {
        MetricasEquipe metricas = composicao.metricas();
        recomendacao.setCoberturaPapeis(metricas.coberturaPapeis());
        recomendacao.setCoberturaTecnologias(metricas.coberturaTecnologias());
        recomendacao.setDesvioPadraoSenioridade(metricas.desvioPadraoSenioridade());
        recomendacao.setDesvioPadraoScore(metricas.desvioPadraoScore());
        recomendacao.setScoreMedioEquipe(metricas.scoreMedioEquipe());
        recomendacao.setObservacaoRestricao(composicao.observacaoRestricao());
    }

    /** Vagas na ordem declarada e stack tecnica (so HARD, como na aderencia). */
    private static RequisitosEquipe requisitosDaEquipe(Projeto projeto) {
        Map<PapelAtuacao, Integer> vagas = new LinkedHashMap<>();
        projeto.getPapeisRequeridos().forEach(vaga -> vagas.put(vaga.getPapel(), vaga.getQuantidade()));

        List<TecnologiaExigida> tecnologias = projeto.getHabilidadesRequeridas().stream()
                .filter(requisito -> requisito.getHabilidade().getTipo() == TipoHabilidade.HARD)
                .map(requisito -> new TecnologiaExigida(requisito.getHabilidade().getId(),
                        requisito.getHabilidade().getNome(), requisito.getPeso(), requisito.isObrigatoria()))
                .toList();

        return new RequisitosEquipe(projeto.getTamanhoEquipe(), projeto.getHorasSemanaisPorIntegrante(),
                vagas, tecnologias);
    }

    private static BigDecimal escala(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP);
    }

    /** O que a leitura entrega para as etapas seguintes, sem entidades gerenciadas. */
    private record Preparacao(PredicaoMotorRequest requisicao,
                              Map<Long, PerfilCandidato> perfis,
                              RequisitosEquipe requisitos) {
    }

    /** Copia do que a composicao precisa de cada desenvolvedor, lida na transacao de leitura. */
    private record PerfilCandidato(String nome, PapelAtuacao papel, int nivelSenioridade, Set<Long> habilidadeIds) {

        static PerfilCandidato de(Desenvolvedor desenvolvedor) {
            return new PerfilCandidato(desenvolvedor.getNome(), desenvolvedor.getPapelPrincipal(),
                    desenvolvedor.getSenioridade().getValorOrdinal(), habilidadesDe(desenvolvedor));
        }

        static Set<Long> habilidadesDe(Desenvolvedor desenvolvedor) {
            return desenvolvedor.getHabilidades().stream()
                    .map(vinculo -> vinculo.getHabilidade().getId())
                    .collect(Collectors.toUnmodifiableSet());
        }

        CandidatoRanqueado ranquear(Predicao predicao, CandidatoVetor vetor) {
            return new CandidatoRanqueado(predicao.id(), nome, papel, nivelSenioridade,
                    predicao.score(), vetor.coberturaHoras(), habilidadeIds);
        }
    }
}
