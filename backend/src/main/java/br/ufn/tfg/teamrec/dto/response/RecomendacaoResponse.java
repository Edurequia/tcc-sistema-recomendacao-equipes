package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.OrigemRecomendacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Recomendacao gerada para um projeto: equipe proposta, metricas e o ranking
 * completo dos candidatos.
 *
 * O ranking inteiro e devolvido, e nao so os selecionados: o gestor precisa
 * enxergar quem ficou de fora e por qual motivo (escore ou disponibilidade).
 *
 * @param observacaoRestricao o que nao pode ser atendido (equipe parcial, vaga
 *                            sem o papel exigido, tecnologia obrigatoria ausente).
 *                            Omitido quando todos os requisitos foram cumpridos.
 */
@Schema(description = "Recomendacao com equipe proposta, metricas e ranking completo.")
public record RecomendacaoResponse(
        Long id,
        Long projetoId,
        String projetoNome,
        OrigemRecomendacao origem,
        String origemDescricao,
        OffsetDateTime criadoEm,
        int tamanhoEquipe,
        int totalCandidatos,
        int integrantesSelecionados,
        MetricasEquipeResponse metricas,
        String observacaoRestricao,
        String justificativaLlm,
        List<MembroRankingResponse> ranking
) {
}
