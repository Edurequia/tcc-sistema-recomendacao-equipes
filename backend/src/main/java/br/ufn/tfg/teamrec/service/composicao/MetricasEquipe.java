package br.ufn.tfg.teamrec.service.composicao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Metricas de avaliacao definidas na metodologia do trabalho.
 *
 * @param coberturaPapeis         % das vagas por papel atendidas (0 a 100).
 * @param coberturaTecnologias    % dos pesos da stack dominados por ao menos um integrante (0 a 100).
 * @param desvioPadraoSenioridade dispersao da experiencia; menor = equipe mais nivelada. Nulo sem equipe.
 * @param desvioPadraoScore       dispersao dos escores de afinidade. Nulo sem equipe.
 * @param scoreMedioEquipe        media dos escores. Nulo sem equipe.
 * @param tecnologiasObrigatoriasAusentes obrigatorias que nenhum integrante domina.
 */
public record MetricasEquipe(
        BigDecimal coberturaPapeis,
        BigDecimal coberturaTecnologias,
        BigDecimal desvioPadraoSenioridade,
        BigDecimal desvioPadraoScore,
        BigDecimal scoreMedioEquipe,
        List<String> tecnologiasObrigatoriasAusentes
) {
}
