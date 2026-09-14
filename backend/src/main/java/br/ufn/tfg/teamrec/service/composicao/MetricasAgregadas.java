package br.ufn.tfg.teamrec.service.composicao;

import java.math.BigDecimal;

/**
 * Uma estatistica (media ou desvio padrao) de cada metrica ao longo dos sorteios.
 */
public record MetricasAgregadas(
        BigDecimal coberturaPapeis,
        BigDecimal coberturaTecnologias,
        BigDecimal desvioPadraoSenioridade,
        BigDecimal desvioPadraoScore,
        BigDecimal scoreMedioEquipe
) {
}
