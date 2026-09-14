package br.ufn.tfg.teamrec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Comparacao entre a equipe proposta e equipes sorteadas ao acaso.
 *
 * Os dois lados partem do mesmo grupo de candidatos elegiveis e sao medidos
 * pela mesma calculadora de metricas: a unica diferenca e o criterio de escolha.
 *
 * @param prototipo metricas da equipe proposta, gravadas na recomendacao.
 * @param linhaBase media e desvio padrao das metricas ao longo dos sorteios.
 */
@Schema(description = "Metricas do prototipo lado a lado com a linha de base aleatoria.")
public record LinhaBaseResponse(
        Long recomendacaoId,
        Long projetoId,
        int sorteios,
        long semente,
        int candidatosElegiveis,
        int tamanhoEquipeSorteada,
        MetricasEquipeResponse prototipo,
        Estatisticas linhaBase
) {

    @Schema(description = "Media e desvio padrao de cada metrica entre os sorteios.")
    public record Estatisticas(MetricasEquipeResponse media, MetricasEquipeResponse desvioPadrao) {
    }
}
