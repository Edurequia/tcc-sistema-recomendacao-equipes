package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Versao enxuta usada na listagem paginada.
 *
 * Papeis e tecnologias sao omitidos de proposito: carrega-los por linha
 * dispararia duas consultas por projeto.
 */
@Schema(description = "Projeto em formato reduzido, para listagens.")
public record ProjetoResumoResponse(
        Long id,
        String nome,
        CriticidadeProjeto criticidade,
        String criticidadeDescricao,
        StatusProjeto status,
        String statusDescricao,
        Integer tamanhoEquipe,
        Integer horasSemanaisPorIntegrante,
        LocalDate dataInicioPrevista,
        LocalDate dataFimPrevista
) {
}
