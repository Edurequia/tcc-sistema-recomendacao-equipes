package br.ufn.tfg.teamrec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Metricas de avaliacao da equipe selecionada, gravadas no momento da geracao.
 *
 * @param coberturaPapeis         % das vagas por papel atendidas.
 * @param coberturaTecnologias    % dos pesos da stack dominados por ao menos um integrante.
 * @param desvioPadraoSenioridade menor = equipe mais nivelada em experiencia.
 */
@Schema(description = "Metricas de avaliacao da equipe proposta.")
public record MetricasEquipeResponse(
        BigDecimal coberturaPapeis,
        BigDecimal coberturaTecnologias,
        BigDecimal desvioPadraoSenioridade,
        BigDecimal desvioPadraoScore,
        BigDecimal scoreMedioEquipe
) {
}
