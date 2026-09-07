package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import io.swagger.v3.oas.annotations.media.Schema;

/** Vagas exigidas em um papel. */
@Schema(description = "Papel exigido pelo projeto.")
public record PapelRequeridoResponse(
        PapelAtuacao papel,
        String papelDescricao,
        Integer quantidade
) {
}
