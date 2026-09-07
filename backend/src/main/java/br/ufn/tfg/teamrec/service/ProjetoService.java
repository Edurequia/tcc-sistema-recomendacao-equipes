package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.dto.request.HabilidadeRequeridaRequest;
import br.ufn.tfg.teamrec.dto.request.PapelRequeridoRequest;
import br.ufn.tfg.teamrec.dto.request.ProjetoRequest;
import br.ufn.tfg.teamrec.dto.request.StatusProjetoRequest;
import br.ufn.tfg.teamrec.dto.response.PaginaResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResumoResponse;
import br.ufn.tfg.teamrec.mapper.ProjetoMapper;
import br.ufn.tfg.teamrec.shared.exception.ConflitoException;
import br.ufn.tfg.teamrec.shared.exception.RecursoNaoEncontradoException;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras de negocio do cadastro de projetos.
 *
 * O projeto e o lado da demanda na recomendacao. As validacoes aqui existem
 * para impedir que uma especificacao incoerente contamine as metricas
 * calculadas mais adiante: uma equipe de tres pessoas com quatro vagas
 * declaradas tornaria a cobertura de papeis impossivel de satisfazer.
 */
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository repository;
    private final HabilidadeRepository habilidadeRepository;
    private final ProjetoMapper mapper;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaResponse<ProjetoResumoResponse> listar(String nome,
                                                        StatusProjeto status,
                                                        CriticidadeProjeto criticidade,
                                                        Pageable pageable) {
        Page<Projeto> pagina = repository.buscarComFiltros(
                textoOuNulo(nome), status, criticidade, pageable);

        return PaginaResponse.de(pagina, mapper::toResumo);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarCompleto(id));
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public ProjetoResponse criar(ProjetoRequest request) {
        garantirNomeDisponivel(request.nome(), null);
        validarRequisicao(request);

        Projeto projeto = new Projeto(
                request.nome(),
                request.descricao(),
                request.criticidade(),
                request.tamanhoEquipe(),
                request.horasSemanaisPorIntegrante());

        projeto.setDataInicioPrevista(request.dataInicioPrevista());
        projeto.setDataFimPrevista(request.dataFimPrevista());

        aplicarPapeis(projeto, request.papeisOuVazio());
        aplicarHabilidades(projeto, request.habilidadesOuVazio());

        return mapper.toResponse(repository.save(projeto));
    }

    /** Substitui integralmente o cadastro, inclusive papeis e tecnologias exigidos. */
    @Transactional
    public ProjetoResponse atualizar(Long id, ProjetoRequest request) {
        Projeto projeto = buscarCompleto(id);
        garantirNomeDisponivel(request.nome(), id);
        validarRequisicao(request);

        projeto.setNome(request.nome());
        projeto.setDescricao(request.descricao());
        projeto.setCriticidade(request.criticidade());
        projeto.setTamanhoEquipe(request.tamanhoEquipe());
        projeto.setHorasSemanaisPorIntegrante(request.horasSemanaisPorIntegrante());
        projeto.setDataInicioPrevista(request.dataInicioPrevista());
        projeto.setDataFimPrevista(request.dataFimPrevista());

        projeto.removerTodosPapeisRequeridos();
        projeto.removerTodasHabilidadesRequeridas();
        aplicarPapeis(projeto, request.papeisOuVazio());
        aplicarHabilidades(projeto, request.habilidadesOuVazio());

        return mapper.toResponse(projeto);
    }

    /**
     * Altera apenas o ciclo de vida.
     *
     * Operacao propria porque o status decide se o projeto consome horas dos
     * desenvolvedores: conclui-lo libera a alocacao de toda a equipe, e isso
     * nao deveria exigir o reenvio do cadastro inteiro.
     */
    @Transactional
    public ProjetoResponse alterarStatus(Long id, StatusProjetoRequest request) {
        Projeto projeto = buscarCompleto(id);
        projeto.setStatus(request.status());
        return mapper.toResponse(projeto);
    }

    /**
     * Cancelamento: o equivalente a exclusao logica para projetos.
     *
     * Projetos cancelados param de disputar desenvolvedores, mas continuam
     * existindo — as recomendacoes ja geradas para eles fazem parte do
     * historico do trabalho.
     */
    @Transactional
    public void cancelar(Long id) {
        buscarEntidade(id).setStatus(StatusProjeto.CANCELADO);
    }

    // ------------------------------------------------------------------
    // Validacoes
    // ------------------------------------------------------------------

    private void validarRequisicao(ProjetoRequest request) {
        validarPeriodo(request);
        validarVagas(request);
    }

    private void validarPeriodo(ProjetoRequest request) {
        if (request.dataInicioPrevista() == null || request.dataFimPrevista() == null) {
            return;
        }
        if (request.dataFimPrevista().isBefore(request.dataInicioPrevista())) {
            throw new RegraDeNegocioException(
                    "A data de termino prevista nao pode ser anterior a data de inicio.");
        }
    }

    /**
     * O somatorio das vagas por papel nao pode exceder o tamanho da equipe.
     *
     * Somar menos e legitimo: representa posicoes sem papel fixo, que serao
     * preenchidas por quem tiver o melhor escore de afinidade. Somar mais e
     * quase sempre erro de digitacao, e tornaria a cobertura de papeis
     * matematicamente impossivel de atingir.
     */
    private void validarVagas(ProjetoRequest request) {
        int vagasDeclaradas = request.papeisOuVazio().stream()
                .mapToInt(PapelRequeridoRequest::quantidade)
                .sum();

        if (vagasDeclaradas > request.tamanhoEquipe()) {
            throw new RegraDeNegocioException(
                    ("Os papeis exigidos somam %d vagas, mas a equipe tem %d integrantes. "
                            + "Reduza as vagas ou aumente o tamanho da equipe.")
                            .formatted(vagasDeclaradas, request.tamanhoEquipe()));
        }
    }

    private void garantirNomeDisponivel(String nome, Long idAtual) {
        boolean duplicado = idAtual == null
                ? repository.existsByNomeIgnoreCase(nome)
                : repository.existsByNomeIgnoreCaseAndIdNot(nome, idAtual);

        if (duplicado) {
            throw new ConflitoException("Ja existe um projeto chamado '%s'.".formatted(nome));
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Projeto buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", id));
    }

    /**
     * Carrega o projeto com as duas colecoes de requisitos inicializadas.
     *
     * Sao duas consultas de proposito: um unico JOIN FETCH sobre ambas
     * produziria o produto cartesiano entre papeis e tecnologias. Como rodam na
     * mesma transacao, as duas devolvem a MESMA instancia gerenciada.
     */
    private Projeto buscarCompleto(Long id) {
        Projeto projeto = repository.buscarComPapeisRequeridos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", id));
        repository.buscarComHabilidadesRequeridas(id);
        return projeto;
    }

    private void aplicarPapeis(Projeto projeto, List<PapelRequeridoRequest> papeis) {
        if (papeis.isEmpty()) {
            return;
        }

        List<PapelAtuacao> duplicados = papeis.stream()
                .collect(Collectors.groupingBy(PapelRequeridoRequest::papel, Collectors.counting()))
                .entrySet().stream()
                .filter(entrada -> entrada.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (!duplicados.isEmpty()) {
            throw new RegraDeNegocioException(
                    ("Papel informado mais de uma vez: %s. Declare cada papel uma unica vez, "
                            + "somando as vagas.").formatted(duplicados));
        }

        papeis.forEach(papel -> projeto.adicionarPapelRequerido(papel.papel(), papel.quantidade()));
    }

    /**
     * Resolve os identificadores contra o catalogo e recria os requisitos.
     * As habilidades sao buscadas em UMA consulta, e nao uma por identificador.
     */
    private void aplicarHabilidades(Projeto projeto, List<HabilidadeRequeridaRequest> requisitos) {
        if (requisitos.isEmpty()) {
            return;
        }

        Set<Long> idsSolicitados = requisitos.stream()
                .map(HabilidadeRequeridaRequest::habilidadeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (idsSolicitados.size() < requisitos.size()) {
            throw new RegraDeNegocioException(
                    "A mesma habilidade foi exigida mais de uma vez. Declare cada uma apenas uma vez.");
        }

        Map<Long, Habilidade> encontradas = habilidadeRepository.findByIdIn(idsSolicitados).stream()
                .collect(Collectors.toMap(Habilidade::getId, Function.identity()));

        List<Long> inexistentes = idsSolicitados.stream()
                .filter(idHabilidade -> !encontradas.containsKey(idHabilidade))
                .toList();

        if (!inexistentes.isEmpty()) {
            throw new RecursoNaoEncontradoException(
                    "Habilidade(s) inexistente(s): %s.".formatted(inexistentes));
        }

        List<String> inativas = encontradas.values().stream()
                .filter(habilidade -> !habilidade.isAtiva())
                .map(Habilidade::getNome)
                .toList();

        if (!inativas.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Habilidade(s) inativa(s) nao podem ser exigidas: %s.".formatted(inativas));
        }

        requisitos.forEach(requisito -> projeto.adicionarHabilidadeRequerida(
                encontradas.get(requisito.habilidadeId()),
                requisito.obrigatoria(),
                requisito.peso()));
    }

    private String textoOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
