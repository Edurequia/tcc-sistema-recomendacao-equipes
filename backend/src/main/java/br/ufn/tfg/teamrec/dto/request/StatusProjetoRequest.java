package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Mudanca isolada do ciclo de vida do projeto.
 *
 * E operacao propria porque o status determina se o projeto consome horas dos
 * desenvolvedores: concluir um projeto libera a alocacao de todo mundo, e isso
 * nao deveria depender de reenviar o cadastro inteiro.
 */
@Schema(description = "Novo status do projeto.")
public record StatusProjetoRequest(

        @NotNull(message = "O status e obrigatorio.")
        @Schema(example = "EM_ANDAMENTO")
        StatusProjeto status
) {
}
