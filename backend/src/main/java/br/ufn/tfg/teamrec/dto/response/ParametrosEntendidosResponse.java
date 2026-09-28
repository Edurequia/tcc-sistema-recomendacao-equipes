package br.ufn.tfg.teamrec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * O que o sistema entendeu do pedido, em forma legivel.
 *
 * Existe para o gestor poder corrigir um mal-entendido no turno seguinte, em
 * vez de descobrir o erro so quando a equipe vier errada.
 */
@Schema(description = "Requisitos reconhecidos no pedido ate o momento.")
public record ParametrosEntendidosResponse(
        String nome,
        String criticidade,
        Integer tamanhoEquipe,
        Integer horasSemanaisPorIntegrante,
        List<String> papeis,
        List<String> tecnologias
) {
}
