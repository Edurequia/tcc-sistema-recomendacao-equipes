package br.ufn.tfg.teamrec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Competencia declarada por um desenvolvedor, com o nivel de dominio.
 *
 * A matriz de treinamento atual e binaria, entao a proficiencia ainda nao
 * influencia o escore: qualquer valor maior que zero vira 1 no vetor de
 * atributos. Ela e coletada desde ja para permitir refinar o modelo depois.
 */
@Schema(description = "Vinculo entre desenvolvedor e habilidade.")
public record VinculoHabilidadeRequest(

        @NotNull(message = "O identificador da habilidade e obrigatorio.")
        @Schema(example = "1")
        Long habilidadeId,

        @NotNull(message = "A proficiencia e obrigatoria.")
        @Min(value = 1, message = "A proficiencia minima e 1.")
        @Max(value = 5, message = "A proficiencia maxima e 5.")
        @Schema(example = "4", description = "1 (basico) a 5 (especialista).")
        Integer proficiencia
) {
}
