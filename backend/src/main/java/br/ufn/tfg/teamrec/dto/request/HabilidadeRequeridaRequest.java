package br.ufn.tfg.teamrec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Tecnologia exigida pelo projeto.
 *
 * @param obrigatoria quando verdadeira, a ausencia desta tecnologia na equipe
 *                    reprova a cobertura; quando falsa, ela apenas contribui
 *                    positivamente para o percentual de aderencia.
 * @param peso        importancia relativa, de 1 a 5. O padrao e 3.
 */
@Schema(description = "Tecnologia exigida pelo projeto.")
public record HabilidadeRequeridaRequest(

        @NotNull(message = "O identificador da habilidade e obrigatorio.")
        @Schema(example = "5")
        Long habilidadeId,

        @Schema(example = "true", defaultValue = "true")
        Boolean obrigatoria,

        @Min(value = 1, message = "O peso minimo e 1.")
        @Max(value = 5, message = "O peso maximo e 5.")
        @Schema(example = "4", defaultValue = "3")
        Integer peso
) {

    /** Aplica os padroes quando o cliente omite os campos opcionais. */
    public HabilidadeRequeridaRequest {
        obrigatoria = obrigatoria == null || obrigatoria;
        peso = peso == null ? 3 : peso;
    }
}
