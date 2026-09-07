package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.dto.request.HabilidadeRequest;
import br.ufn.tfg.teamrec.dto.response.HabilidadeResponse;
import br.ufn.tfg.teamrec.mapper.HabilidadeMapper;
import br.ufn.tfg.teamrec.shared.exception.ConflitoException;
import br.ufn.tfg.teamrec.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negocio do catalogo de habilidades.
 *
 * O catalogo e centralizado justamente para evitar que "React", "ReactJS" e
 * "react" convivam como registros distintos: a checagem de nome duplicado
 * ignora maiusculas e minusculas.
 */
@Service
@RequiredArgsConstructor
public class HabilidadeService {

    private final HabilidadeRepository repository;
    private final HabilidadeMapper mapper;

    @Transactional(readOnly = true)
    public List<HabilidadeResponse> listar(TipoHabilidade tipo, boolean apenasAtivas) {
        List<Habilidade> habilidades;

        if (tipo != null && apenasAtivas) {
            habilidades = repository.findByTipoAndAtivaTrueOrderByNomeAsc(tipo);
        } else if (apenasAtivas) {
            habilidades = repository.findByAtivaTrueOrderByNomeAsc();
        } else if (tipo != null) {
            habilidades = repository.findAll(Sort.by("nome")).stream()
                    .filter(h -> h.getTipo() == tipo)
                    .toList();
        } else {
            habilidades = repository.findAll(Sort.by("nome"));
        }

        return mapper.toResponseList(habilidades);
    }

    @Transactional(readOnly = true)
    public HabilidadeResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public HabilidadeResponse criar(HabilidadeRequest request) {
        garantirNomeDisponivel(request.nome(), null);

        Habilidade habilidade = new Habilidade(
                request.nome(),
                request.tipo(),
                request.categoria(),
                request.chaveDataset());

        return mapper.toResponse(repository.save(habilidade));
    }

    @Transactional
    public HabilidadeResponse atualizar(Long id, HabilidadeRequest request) {
        Habilidade habilidade = buscarEntidade(id);
        garantirNomeDisponivel(request.nome(), id);

        habilidade.setNome(request.nome());
        habilidade.setTipo(request.tipo());
        habilidade.setCategoria(request.categoria());
        habilidade.setChaveDataset(request.chaveDataset());

        return mapper.toResponse(habilidade);
    }

    /**
     * Inativacao logica. A habilidade some das listagens e dos novos cadastros,
     * mas os vinculos e as recomendacoes que ja a referenciam continuam intactos.
     */
    @Transactional
    public void inativar(Long id) {
        buscarEntidade(id).setAtiva(false);
    }

    @Transactional
    public HabilidadeResponse reativar(Long id) {
        Habilidade habilidade = buscarEntidade(id);
        habilidade.setAtiva(true);
        return mapper.toResponse(habilidade);
    }

    // ------------------------------------------------------------------

    private Habilidade buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Habilidade", id));
    }

    /** O nome ja chega aparado pelo record de request. */
    private void garantirNomeDisponivel(String nome, Long idAtual) {
        boolean duplicado = idAtual == null
                ? repository.existsByNomeIgnoreCase(nome)
                : repository.existsByNomeIgnoreCaseAndIdNot(nome, idAtual);

        if (duplicado) {
            throw new ConflitoException("Ja existe uma habilidade chamada '%s'.".formatted(nome));
        }
    }
}
