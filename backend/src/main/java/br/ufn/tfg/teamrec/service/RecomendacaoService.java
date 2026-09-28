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
import br.ufn.tfg.teamrec.dto.response.JustificativaResponse;
import br.ufn.tfg.teamrec.dto.response.LinhaBaseResponse;
import br.ufn.tfg.teamrec.integration.llm.ContextoJustificativa;
import br.ufn.tfg.teamrec.integration.llm.GeradorJustificativa;
import br.ufn.tfg.teamrec.integration.llm.ExtratorParametros;
import br.ufn.tfg.teamrec.integration.llm.Justificativa;
import br.ufn.tfg.teamrec.integration.llm.ParametrosExtraidos;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.dto.request.HabilidadeRequeridaRequest;
import br.ufn.tfg.teamrec.dto.request.PapelRequeridoRequest;
import br.ufn.tfg.teamrec.dto.request.ProjetoRequest;
import br.ufn.tfg.teamrec.dto.response.ProjetoResponse;
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
import java.util.ArrayList;
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

    /** Quantos candidatos preteridos acompanham a equipe no prompt da justificativa. */
    private static final int MAX_PRETERIDOS_NO_PROMPT = 5;

    /** Tamanho da coluna consulta_original em Recomendacao. */
    private static final int LIMITE_CONSULTA_ORIGINAL = 2000;

    private final ProjetoRepository projetoRepository;
    private final DesenvolvedorRepository desenvolvedorRepository;
    private final AlocacaoRepository alocacaoRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final MontadorVetorAtributos montador;
    private final MotorPredicaoClient motor;
    private final ComposicaoEquipe composicaoEquipe;
    private final LinhaBaseAleatoria linhaBaseAleatoria;
    private final GeradorJustificativa geradorJustificativa;
    private final ExtratorParametros extratorParametros;
    private final HabilidadeRepository habilidadeRepository;
    private final ProjetoService projetoService;
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
                               GeradorJustificativa geradorJustificativa,
                               ExtratorParametros extratorParametros,
                               HabilidadeRepository habilidadeRepository,
                               ProjetoService projetoService,
                               RecomendacaoMapper mapper,
                               PlatformTransactionManager transactionManager) {
        this.geradorJustificativa = geradorJustificativa;
        this.extratorParametros = extratorParametros;
        this.habilidadeRepository = habilidadeRepository;
        this.projetoService = projetoService;
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
        return gerar(projetoId, OrigemRecomendacao.FORMULARIO, null);
    }

    private RecomendacaoResponse gerar(Long projetoId, OrigemRecomendacao origem, String consultaOriginal) {
        Preparacao preparacao = transacaoLeitura.execute(status -> preparar(projetoId));
        PredicaoMotorResponse resposta = motor.prever(preparacao.requisicao());
        return transacaoEscrita.execute(
                status -> gravar(projetoId, preparacao, resposta, origem, consultaOriginal));
    }

    /**
     * Via conversacional: o texto do gestor vira requisitos, os requisitos viram
     * projeto e o projeto entra na mesma pipeline de recomendacao.
     *
     * O projeto e criado pelo {@link ProjetoService}, e nao aqui, para que o
     * pedido em linguagem natural passe exatamente pelas validacoes do
     * cadastro por formulario. A consulta original fica gravada na
     * recomendacao: saber por qual via o gestor chegou e dado de pesquisa.
     */
    public RecomendacaoResponse gerarPorConversa(String consulta) {
        return gerarPorParametros(extratorParametros.extrair(consulta, catalogoDeTecnologias()), consulta);
    }

    /**
     * Segunda metade da via conversacional, a partir de requisitos ja
     * reconhecidos. Separada da extracao porque a conversa com historico chega
     * aqui com os parametros prontos, depois de varios turnos.
     */
    public RecomendacaoResponse gerarPorParametros(ParametrosExtraidos parametros, String consultaOriginal) {
        ProjetoResponse projeto = projetoService.criar(projetoRequestDe(parametros));
        return gerar(projeto.id(), OrigemRecomendacao.CHAT, truncar(consultaOriginal));
    }

    /** Nomes das tecnologias que o gestor pode citar: e o que o modelo pode escolher. */
    public List<String> catalogoDeTecnologias() {
        return habilidadeRepository.findByTipoAndAtivaTrueOrderByNomeAsc(TipoHabilidade.HARD).stream()
                .map(Habilidade::getNome)
                .toList();
    }

    /** A consulta original e gravada em uma coluna de 2000 caracteres. */
    private static String truncar(String consulta) {
        if (consulta == null || consulta.length() <= LIMITE_CONSULTA_ORIGINAL) {
            return consulta;
        }
        return consulta.substring(0, LIMITE_CONSULTA_ORIGINAL);
    }

    private ProjetoRequest projetoRequestDe(ParametrosExtraidos parametros) {
        List<PapelRequeridoRequest> papeis = parametros.papeisOuVazio().stream()
                .map(papel -> new PapelRequeridoRequest(papel.papel(),
                        papel.quantidade() == null ? 1 : papel.quantidade()))
                .toList();

        List<String> desconhecidas = new ArrayList<>();
        List<HabilidadeRequeridaRequest> tecnologias = new ArrayList<>();
        for (ParametrosExtraidos.TecnologiaExtraida tecnologia : parametros.tecnologiasOuVazio()) {
            habilidadeRepository.findByNomeIgnoreCase(tecnologia.nome())
                    .ifPresentOrElse(
                            habilidade -> tecnologias.add(new HabilidadeRequeridaRequest(
                                    habilidade.getId(), tecnologia.obrigatoria(), tecnologia.peso())),
                            () -> desconhecidas.add(tecnologia.nome()));
        }

        if (!desconhecidas.isEmpty()) {
            // Seguir sem a tecnologia pedida produziria uma equipe avaliada por
            // requisitos diferentes dos que o gestor descreveu.
            throw new RegraDeNegocioException(
                    "Estas tecnologias nao estao no catalogo: %s. Cadastre-as ou reescreva a solicitacao."
                            .formatted(String.join(", ", desconhecidas)));
        }

        return new ProjetoRequest(parametros.nome(), parametros.descricao(), parametros.criticidadeOuPadrao(),
                parametros.tamanhoEquipe(), parametros.horasSemanaisPorIntegrante(),
                null, null, papeis, tecnologias);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public RecomendacaoResponse buscarPorId(Long id) {
        return mapper.toResponse(recomendacaoRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", id)));
    }

    // ------------------------------------------------------------------
    // Justificativa
    // ------------------------------------------------------------------

    /**
     * Pede ao modelo de linguagem a explicacao da equipe ja recomendada.
     *
     * Segue a mesma divisao em tres etapas da geracao, e pelo mesmo motivo: a
     * chamada ao modelo pode demorar, e segurar uma conexao do banco durante a
     * espera esgotaria o pool. O texto ja gravado e reaproveitado, porque cada
     * nova chamada consome cota da API; {@code regerar} forca uma nova.
     */
    public JustificativaResponse justificar(Long recomendacaoId, boolean regerar) {
        ContextoOuTextoGravado preparacao = transacaoLeitura.execute(
                status -> prepararJustificativa(recomendacaoId, regerar));

        if (preparacao.textoGravado() != null) {
            return new JustificativaResponse(recomendacaoId, preparacao.textoGravado(), null, false);
        }

        Justificativa justificativa = geradorJustificativa.gerar(preparacao.contexto());

        return transacaoEscrita.execute(status -> {
            Recomendacao recomendacao = recomendacaoRepository.findById(recomendacaoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", recomendacaoId));
            recomendacao.setJustificativaLlm(justificativa.texto());
            return new JustificativaResponse(recomendacaoId, justificativa.texto(),
                    justificativa.modelo(), true);
        });
    }

    private ContextoOuTextoGravado prepararJustificativa(Long recomendacaoId, boolean regerar) {
        Recomendacao recomendacao = recomendacaoRepository.buscarCompleta(recomendacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", recomendacaoId));

        String gravada = recomendacao.getJustificativaLlm();
        if (!regerar && gravada != null && !gravada.isBlank()) {
            return new ContextoOuTextoGravado(null, gravada);
        }

        Long projetoId = recomendacao.getProjeto().getId();
        Projeto projeto = projetoRepository.buscarComPapeisRequeridos(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", projetoId));
        projetoRepository.buscarComHabilidadesRequeridas(projetoId);

        return new ContextoOuTextoGravado(contextoDa(recomendacao, projeto), null);
    }

    private static ContextoJustificativa contextoDa(Recomendacao recomendacao, Projeto projeto) {
        List<ContextoJustificativa.Integrante> equipe = recomendacao.getMembros().stream()
                .filter(RecomendacaoMembro::isSelecionado)
                .map(RecomendacaoService::integranteDe)
                .toList();

        // O contraste que explica a escolha esta nos primeiros preteridos; o
        // ranking inteiro so encheria o prompt sem acrescentar argumento.
        List<ContextoJustificativa.Integrante> preteridos = recomendacao.getMembros().stream()
                .filter(membro -> !membro.isSelecionado())
                .limit(MAX_PRETERIDOS_NO_PROMPT)
                .map(RecomendacaoService::integranteDe)
                .toList();

        List<String> papeis = projeto.getPapeisRequeridos().stream()
                .map(papel -> "%s (%d)".formatted(papel.getPapel().getDescricao(), papel.getQuantidade()))
                .toList();

        List<String> tecnologias = projeto.getHabilidadesRequeridas().stream()
                .map(requisito -> "%s (peso %d%s)".formatted(requisito.getHabilidade().getNome(),
                        requisito.getPeso(), requisito.isObrigatoria() ? ", obrigatoria" : ""))
                .toList();

        return new ContextoJustificativa(
                projeto.getNome(), "%s (nivel %d)".formatted(projeto.getCriticidade().name(),
                        projeto.getCriticidade().getNivel()),
                projeto.getTamanhoEquipe(), projeto.getHorasSemanaisPorIntegrante(),
                papeis, tecnologias, equipe, preteridos,
                new ContextoJustificativa.Metricas(
                        recomendacao.getCoberturaPapeis(), recomendacao.getCoberturaTecnologias(),
                        recomendacao.getDesvioPadraoSenioridade(), recomendacao.getScoreMedioEquipe()),
                recomendacao.getObservacaoRestricao());
    }

    private static ContextoJustificativa.Integrante integranteDe(RecomendacaoMembro membro) {
        BigDecimal coberturaHoras = membro.getCoberturaHoras();
        return new ContextoJustificativa.Integrante(
                membro.getDesenvolvedor().getNome(),
                membro.getPapelSugerido().getDescricao(),
                membro.getDesenvolvedor().getSenioridade().getDescricao(),
                membro.getScoreAfinidade(), membro.getAderenciaTecnica(), coberturaHoras,
                coberturaHoras != null && coberturaHoras.compareTo(BigDecimal.ONE) >= 0);
    }

    /** Ou ha contexto para gerar, ou ha texto ja gravado para devolver. Nunca os dois. */
    private record ContextoOuTextoGravado(ContextoJustificativa contexto, String textoGravado) {
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

    private RecomendacaoResponse gravar(Long projetoId, Preparacao preparacao, PredicaoMotorResponse resposta,
                                        OrigemRecomendacao origem, String consultaOriginal) {
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

        Recomendacao recomendacao = new Recomendacao(projeto, origem);
        recomendacao.setConsultaOriginal(consultaOriginal);
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
