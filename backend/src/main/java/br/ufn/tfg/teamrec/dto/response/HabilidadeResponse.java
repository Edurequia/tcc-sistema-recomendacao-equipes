package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Habilidade como a API a devolve.
 *
 * @param usadaNoModelo indica se a habilidade tem coluna correspondente no motor
 *                      de predicao. O frontend usa isso para sinalizar quais
 *                      competencias de fato influenciam o escore de afinidade.
 */
@Schema(description = "Habilidade do catalogo.")
public record HabilidadeResponse(
        Long id,
        String nome,
        TipoHabilidade tipo,
        String tipoDescricao,
        CategoriaHabilidade categoria,
        String categoriaDescricao,
        String chaveDataset,
        boolean usadaNoModelo,
        boolean ativa
) {
}
