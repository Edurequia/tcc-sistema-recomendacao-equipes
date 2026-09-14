package br.ufn.tfg.teamrec.service.composicao;

/**
 * Resultado dos sorteios da linha de base.
 *
 * @param candidatosElegiveis   tamanho do grupo de onde as equipes foram sorteadas.
 * @param tamanhoEquipeSorteada integrantes por sorteio: o tamanho da equipe, ou
 *                              todos os elegiveis quando nao ha suficientes.
 * @param media                 valor de referencia de cada metrica.
 * @param desvioPadrao          variacao de cada metrica entre os sorteios.
 */
public record ResultadoLinhaBase(
        int candidatosElegiveis,
        int tamanhoEquipeSorteada,
        MetricasAgregadas media,
        MetricasAgregadas desvioPadrao
) {
}
