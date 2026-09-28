package br.ufn.tfg.teamrec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido do gestor em linguagem natural, a via conversacional de entrada. */
@Schema(description = "Solicitacao de equipe escrita em texto livre.")
public record ConsultaRequest(

        @NotBlank(message = "A consulta e obrigatoria.")
        @Size(min = 10, max = 2000, message = "A consulta deve ter entre 10 e 2000 caracteres.")
        @Schema(example = "Preciso de uma equipe de 4 pessoas para o Portal do Aluno, "
                + "um projeto critico em Java e React, com 20 horas semanais por integrante. "
                + "Sao necessarios dois desenvolvedores de backend e um de frontend.")
        String consulta
) {

    /** Normaliza na desserializacao, antes da validacao, como nos demais requests. */
    public ConsultaRequest {
        consulta = consulta == null ? null : consulta.trim();
    }
}
