package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Alocacao;
import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.entity.Recomendacao;
import br.ufn.tfg.teamrec.domain.entity.RecomendacaoMembro;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.repository.AlocacaoRepository;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.domain.repository.RecomendacaoRepository;
import br.ufn.tfg.teamrec.dto.request.AlocacaoRequest;
import br.ufn.tfg.teamrec.dto.response.AlocacaoResponse;
import br.ufn.tfg.teamrec.mapper.AlocacaoMapper;
import br.ufn.tfg.teamrec.shared.exception.ConflitoException;
import br.ufn.tfg.teamrec.shared.exception.RecursoNaoEncontradoException;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Regras das alocacoes: o compromisso confirmado que consome horas.
 *
 * A recomendacao so sugere; e a alocacao que reduz a disponibilidade. Por isso
 * o controle de conflito entre projetos simultaneos vive aqui: ninguem e
 * alocado alem das horas que ainda tem livres.
 */
@Service
@RequiredArgsConstructor
public class AlocacaoService {

    private static final List<StatusProjeto> STATUS_QUE_CONSOMEM_ALOCACAO =
            List.of(StatusProjeto.PLANEJAMENTO, StatusProjeto.EM_ANDAMENTO);

    private final AlocacaoRepository repository;
    private final ProjetoRepository projetoRepository;
    private final DesenvolvedorRepository desenvolvedorRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final AlocacaoMapper mapper;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AlocacaoResponse buscarPorId(Long id) {
        return mapper.toResponse(repository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alocacao", id)));
    }

    @Transactional(readOnly = true)
    public List<AlocacaoResponse> listarAtivasDoProjeto(Long projetoId) {
        if (!projetoRepository.existsById(projetoId)) {
            throw new RecursoNaoEncontradoException("Projeto", projetoId);
        }
        return repository.buscarAtivasDoProjeto(projetoId).stream().map(mapper::toResponse).toList();
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public AlocacaoResponse alocar(Long projetoId, AlocacaoRequest request) {
        Projeto projeto = buscarProjetoQueAceitaAlocacao(projetoId);
        Desenvolvedor desenvolvedor = desenvolvedorRepository.findById(request.desenvolvedorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Desenvolvedor", request.desenvolvedorId()));

        Alocacao alocacao = criar(projeto, desenvolvedor,
                request.papel() != null ? request.papel() : desenvolvedor.getPapelPrincipal(),
                request.horasSemanais() != null ? request.horasSemanais() : projeto.getHorasSemanaisPorIntegrante(),
                request.dataInicio() != null ? request.dataInicio() : LocalDate.now());

        return mapper.toResponse(alocacao);
    }

    /**
     * Aloca de uma vez toda a equipe selecionada em uma recomendacao.
     *
     * Tudo ou nada: se um integrante ja nao tiver horas livres (a agenda pode
     * ter mudado desde a geracao), nenhuma alocacao e criada e a recomendacao
     * deve ser gerada de novo. Quem ja esta alocado no projeto e mantido.
     *
     * @return a equipe alocada no projeto apos a confirmacao.
     */
    @Transactional
    public List<AlocacaoResponse> confirmarRecomendacao(Long recomendacaoId) {
        Recomendacao recomendacao = recomendacaoRepository.buscarCompleta(recomendacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendacao", recomendacaoId));

        List<RecomendacaoMembro> selecionados = recomendacao.getMembrosSelecionados();
        if (selecionados.isEmpty()) {
            throw new RegraDeNegocioException("A recomendacao nao possui equipe selecionada para confirmar.");
        }

        Projeto projeto = buscarProjetoQueAceitaAlocacao(recomendacao.getProjeto().getId());
        for (RecomendacaoMembro membro : selecionados) {
            Desenvolvedor desenvolvedor = membro.getDesenvolvedor();
            if (!repository.existsByDesenvolvedorIdAndProjetoIdAndAtivaTrue(desenvolvedor.getId(), projeto.getId())) {
                criar(projeto, desenvolvedor, membro.getPapelSugerido(),
                        projeto.getHorasSemanaisPorIntegrante(), LocalDate.now());
            }
        }

        return repository.buscarAtivasDoProjeto(projeto.getId()).stream().map(mapper::toResponse).toList();
    }

    /** Encerramento logico: libera as horas, mas preserva o historico. Idempotente. */
    @Transactional
    public void encerrar(Long id) {
        Alocacao alocacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alocacao", id));
        if (alocacao.isAtiva()) {
            alocacao.encerrar(LocalDate.now());
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Alocacao criar(Projeto projeto, Desenvolvedor desenvolvedor, PapelAtuacao papel,
                           int horas, LocalDate inicio) {
        if (!desenvolvedor.isAtivo()) {
            throw new RegraDeNegocioException(
                    "O desenvolvedor '%s' esta inativo e nao pode ser alocado.".formatted(desenvolvedor.getNome()));
        }

        if (repository.existsByDesenvolvedorIdAndProjetoIdAndAtivaTrue(desenvolvedor.getId(), projeto.getId())) {
            throw new ConflitoException("O desenvolvedor '%s' ja esta alocado no projeto '%s'."
                    .formatted(desenvolvedor.getNome(), projeto.getNome()));
        }

        int comprometidas = repository.somarHorasComprometidas(desenvolvedor.getId(), STATUS_QUE_CONSOMEM_ALOCACAO);
        int livres = Math.max(0, desenvolvedor.getHorasDisponiveis() - comprometidas);
        if (horas > livres) {
            throw new RegraDeNegocioException(
                    "Conflito de alocacao: '%s' tem %d horas semanais livres, mas a alocacao exige %d."
                            .formatted(desenvolvedor.getNome(), livres, horas));
        }

        return repository.save(new Alocacao(desenvolvedor, projeto, papel, horas, inicio));
    }

    private Projeto buscarProjetoQueAceitaAlocacao(Long projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto", projetoId));
        if (!projeto.getStatus().isConsomeAlocacao()) {
            throw new RegraDeNegocioException("Nao e possivel alocar em um projeto com status '%s'."
                    .formatted(projeto.getStatus().getDescricao()));
        }
        return projeto;
    }
}
