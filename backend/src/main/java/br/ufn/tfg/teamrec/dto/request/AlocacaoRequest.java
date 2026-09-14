package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Confirmacao de um desenvolvedor em um projeto.
 *
 * Apenas o desenvolvedor e obrigatorio; o restante assume o que o cadastro
 * ja diz: o papel principal da pessoa, as horas exigidas pelo projeto e a
 * data de hoje.
 */
@Schema(description = "Alocacao confirmada de um desenvolvedor em um projeto.")
public record AlocacaoRequest(
        @NotNull(message = "O identificador do desenvolvedor e obrigatorio.")
        @Schema(example = "1")
        Long desenvolvedorId,

        @Schema(example = "BACKEND", description = "Omitido, usa o papel principal do desenvolvedor.")
        PapelAtuacao papel,

        @Min(value = 1, message = "O minimo e 1 hora semanal.")
        @Max(value = 60, message = "O limite e 60 horas semanais.")
        @Schema(example = "20", description = "Omitido, usa as horas semanais por integrante do projeto.")
        Integer horasSemanais,

        @Schema(example = "2026-10-01", description = "Omitida, usa a data de hoje.")
        LocalDate dataInicio
) {
}
