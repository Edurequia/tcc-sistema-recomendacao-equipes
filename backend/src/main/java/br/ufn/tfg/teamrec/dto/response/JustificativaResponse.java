package br.ufn.tfg.teamrec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Justificativa em linguagem natural de uma equipe recomendada.
 *
 * @param modelo      identificador do modelo que respondeu; nulo quando o texto
 *                    veio do que ja estava gravado
 * @param geradaAgora falso quando a justificativa ja existia e foi reaproveitada
 */
@Schema(description = "Justificativa da equipe produzida pelo modelo de linguagem.")
public record JustificativaResponse(
        Long recomendacaoId,
        String justificativa,
        String modelo,
        boolean geradaAgora
) {
}
