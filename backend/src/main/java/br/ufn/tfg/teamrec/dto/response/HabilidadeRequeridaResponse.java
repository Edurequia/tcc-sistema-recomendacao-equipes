package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tecnologia exigida, ja resolvida com os dados do catalogo.
 *
 * @param usadaNoModelo indica se a tecnologia tem coluna no motor de predicao.
 *                      Exigir algo que o modelo desconhece e legitimo, mas nao
 *                      influencia o escore de afinidade — apenas a metrica de
 *                      cobertura calculada pelo backend.
 */
@Schema(description = "Tecnologia exigida pelo projeto.")
public record HabilidadeRequeridaResponse(
        Long habilidadeId,
        String nome,
        CategoriaHabilidade categoria,
        boolean obrigatoria,
        Integer peso,
        boolean usadaNoModelo
) {
}
