package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.OrigemRecomendacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Versao enxuta para o historico de um projeto.
 *
 * O ranking e omitido: carrega-lo para cada execucao dispararia uma consulta
 * por recomendacao. As metricas, gravadas na propria recomendacao, permitem
 * comparar execucoes sem abrir cada uma.
 */
@Schema(description = "Recomendacao em formato reduzido, para o historico do projeto.")
public record RecomendacaoResumoResponse(
        Long id,
        Long projetoId,
        OrigemRecomendacao origem,
        String origemDescricao,
        OffsetDateTime criadoEm,
        BigDecimal coberturaPapeis,
        BigDecimal coberturaTecnologias,
        BigDecimal scoreMedioEquipe,
        String observacaoRestricao
) {
}
