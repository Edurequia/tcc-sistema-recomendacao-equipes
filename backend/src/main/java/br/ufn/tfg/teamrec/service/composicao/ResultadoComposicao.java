package br.ufn.tfg.teamrec.service.composicao;

import java.util.List;

/**
 * Equipe proposta, suas metricas e, quando houver, a explicacao do que nao
 * pode ser atendido.
 *
 * @param equipe              integrantes na ordem em que foram escolhidos.
 * @param observacaoRestricao nula quando todos os requisitos foram atendidos.
 */
public record ResultadoComposicao(
        List<MembroSelecionado> equipe,
        MetricasEquipe metricas,
        String observacaoRestricao
) {
}
