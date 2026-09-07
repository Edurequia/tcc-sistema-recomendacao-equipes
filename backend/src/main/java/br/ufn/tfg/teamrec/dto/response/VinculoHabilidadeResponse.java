package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import io.swagger.v3.oas.annotations.media.Schema;

/** Uma competencia do desenvolvedor, ja resolvida com os dados do catalogo. */
@Schema(description = "Competencia declarada pelo desenvolvedor.")
public record VinculoHabilidadeResponse(
        Long habilidadeId,
        String nome,
        TipoHabilidade tipo,
        CategoriaHabilidade categoria,
        Integer proficiencia
) {
}
