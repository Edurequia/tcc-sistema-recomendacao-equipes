package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Quantas vagas de um papel o projeto exige.
 *
 * O somatorio destas quantidades alimenta a metrica de cobertura de papeis:
 * verificar se a equipe recomendada preenche todas as posicoes minimas.
 */
@Schema(description = "Vagas exigidas em um papel especifico.")
public record PapelRequeridoRequest(

        @NotNull(message = "O papel e obrigatorio.")
        @Schema(example = "BACKEND")
        PapelAtuacao papel,

        @NotNull(message = "A quantidade e obrigatoria.")
        @Min(value = 1, message = "A quantidade minima e 1.")
        @Max(value = 50, message = "A quantidade maxima e 50.")
        @Schema(example = "2")
        Integer quantidade
) {
}
