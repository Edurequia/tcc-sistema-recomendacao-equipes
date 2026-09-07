package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.repository.AlocacaoRepository;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.dto.request.DesenvolvedorRequest;
import br.ufn.tfg.teamrec.dto.request.DisponibilidadeRequest;
import br.ufn.tfg.teamrec.dto.request.VinculoHabilidadeRequest;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResponse;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResumoResponse;
import br.ufn.tfg.teamrec.dto.response.PaginaResponse;
import br.ufn.tfg.teamrec.mapper.DesenvolvedorMapper;
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
 * Regras de negocio do cadastro de desenvolvedores.
 *
 * Alem do CRUD, este service e responsavel por manter coerente o conjunto de
 * atributos que alimenta o motor de predicao: e-mail unico, competencias
 * existentes e ativas, e disponibilidade compativel com o que ja esta alocado.
 */
@Service
@RequiredArgsConstructor
public class DesenvolvedorService {

    /** Projetos nestes status ocupam horas dos desenvolvedores. */
    private static final List<StatusProjeto> STATUS_QUE_CONSOMEM_ALOCACAO =
            List.of(StatusProjeto.PLANEJAMENTO, StatusProjeto.EM_ANDAMENTO);

    private final DesenvolvedorRepository repository;
    private final HabilidadeRepository habilidadeRepository;
    private final AlocacaoRepository alocacaoRepository;
    private final DesenvolvedorMapper mapper;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaResponse<DesenvolvedorResumoResponse> listar(String nome,
                                                              Senioridade senioridade,
                                                              PapelAtuacao papel,
                                                              Boolean ativo,
                                                              Pageable pageable) {
        Page<Desenvolvedor> pagina = repository.buscarComFiltros(
                textoOuNulo(nome), senioridade, papel, ativo, pageable);

        return PaginaResponse.de(pagina, mapper::toResumo);
    }

    @Transactional(readOnly = true)
    public DesenvolvedorResponse buscarPorId(Long id) {
        return mapper.toResponse(repository.buscarComHabilidades(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Desenvolvedor", id)));
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public DesenvolvedorResponse criar(DesenvolvedorRequest request) {
        garantirEmailDisponivel(request.email(), null);

        Desenvolvedor desenvolvedor = new Desenvolvedor(
                request.nome(),
                request.email(),
                request.senioridade(),
                request.papelPrincipal(),
                request.softSkillsScore(),
                request.horasDisponiveis());

        aplicarHabilidades(desenvolvedor, request.habilidadesOuVazio());

        return mapper.toResponse(repository.save(desenvolvedor));
    }

    /**
     * Substitui integralmente o perfil, inclusive a lista de competencias.
     *
     * A semantica de substituicao (e nao de acrescimo) e a que o formulario do
     * frontend espera: o que aparece na tela e exatamente o que fica gravado.
     */
    @Transactional
    public DesenvolvedorResponse atualizar(Long id, DesenvolvedorRequest request) {
        Desenvolvedor desenvolvedor = buscarComHabilidades(id);
        garantirEmailDisponivel(request.email(), id);
        garantirDisponibilidadeSuficiente(id, request.horasDisponiveis());

        desenvolvedor.setNome(request.nome());
        desenvolvedor.setEmail(request.email());
        desenvolvedor.setSenioridade(request.senioridade());
        desenvolvedor.setPapelPrincipal(request.papelPrincipal());
        desenvolvedor.setSoftSkillsScore(request.softSkillsScore());
        desenvolvedor.setHorasDisponiveis(request.horasDisponiveis());

        desenvolvedor.removerTodasHabilidades();
        aplicarHabilidades(desenvolvedor, request.habilidadesOuVazio());

        return mapper.toResponse(desenvolvedor);
    }

    @Transactional
    public DesenvolvedorResponse atualizarDisponibilidade(Long id, DisponibilidadeRequest request) {
        Desenvolvedor desenvolvedor = buscarComHabilidades(id);
        garantirDisponibilidadeSuficiente(id, request.horasDisponiveis());

        desenvolvedor.setHorasDisponiveis(request.horasDisponiveis());

        return mapper.toResponse(desenvolvedor);
    }

    /**
     * Inativacao logica. O desenvolvedor deixa de ser candidato em novas
     * recomendacoes, mas continua visivel nas que ja o incluem — apagar o
     * registro romperia o historico que sustenta a avaliacao do trabalho.
     */
    @Transactional
    public void inativar(Long id) {
        buscarEntidade(id).setAtivo(false);
    }

    @Transactional
    public DesenvolvedorResponse reativar(Long id) {
        Desenvolvedor desenvolvedor = buscarComHabilidades(id);
        desenvolvedor.setAtivo(true);
        return mapper.toResponse(desenvolvedor);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Desenvolvedor buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Desenvolvedor", id));
    }

    private Desenvolvedor buscarComHabilidades(Long id) {
        return repository.buscarComHabilidades(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Desenvolvedor", id));
    }

    /** O e-mail ja chega aparado e em minusculas pelo record de request. */
    private void garantirEmailDisponivel(String email, Long idAtual) {
        boolean duplicado = idAtual == null
                ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, idAtual);

        if (duplicado) {
            throw new ConflitoException("Ja existe um desenvolvedor com o e-mail '%s'.".formatted(email));
        }
    }

    /**
     * Impede que a disponibilidade seja reduzida abaixo do que ja esta
     * comprometido com projetos ativos.
     *
     * Sem esta checagem, o vetor enviado ao motor de predicao passaria a
     * declarar horas livres que na pratica nao existem.
     */
    private void garantirDisponibilidadeSuficiente(Long id, int novasHoras) {
        int comprometidas = alocacaoRepository.somarHorasComprometidas(id, STATUS_QUE_CONSOMEM_ALOCACAO);

        if (novasHoras < comprometidas) {
            throw new RegraDeNegocioException(
                    ("Nao e possivel reduzir a disponibilidade para %d horas: o desenvolvedor ja "
                            + "possui %d horas semanais comprometidas em projetos ativos.")
                            .formatted(novasHoras, comprometidas));
        }
    }

    /**
     * Resolve os identificadores enviados contra o catalogo e recria os vinculos.
     *
     * As habilidades sao buscadas em UMA consulta, e nao uma por identificador:
     * um perfil com quinze competencias geraria quinze idas ao banco.
     */
    private void aplicarHabilidades(Desenvolvedor desenvolvedor, List<VinculoHabilidadeRequest> vinculos) {
        if (vinculos.isEmpty()) {
            return;
        }

        Set<Long> idsSolicitados = vinculos.stream()
                .map(VinculoHabilidadeRequest::habilidadeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Duplicata e recusada em vez de resolvida silenciosamente: com dois niveis
        // de proficiencia para a mesma competencia, qualquer escolha automatica
        // gravaria algo diferente do que o cliente pretendia.
        if (idsSolicitados.size() < vinculos.size()) {
            throw new RegraDeNegocioException(
                    "A mesma habilidade foi informada mais de uma vez. Declare cada uma apenas uma vez.");
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
                    "Habilidade(s) inativa(s) nao podem ser vinculadas: %s.".formatted(inativas));
        }

        // Os identificadores ja foram validados como unicos acima.
        Map<Long, Integer> proficiencias = vinculos.stream().collect(Collectors.toMap(
                VinculoHabilidadeRequest::habilidadeId,
                VinculoHabilidadeRequest::proficiencia));

        idsSolicitados.forEach(idHabilidade ->
                desenvolvedor.adicionarHabilidade(encontradas.get(idHabilidade), proficiencias.get(idHabilidade)));
    }

    private String textoOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
