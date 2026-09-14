package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Compromisso confirmado entre um desenvolvedor e um projeto.
 *
 * @param ativa   enquanto verdadeira e o projeto estiver ativo, as horas contam
 *                como comprometidas.
 * @param dataFim preenchida quando a alocacao e encerrada.
 */
@Schema(description = "Alocacao de um desenvolvedor em um projeto.")
public record AlocacaoResponse(
        Long id,
        Long desenvolvedorId,
        String desenvolvedorNome,
        Long projetoId,
        String projetoNome,
        PapelAtuacao papel,
        String papelDescricao,
        Integer horasSemanais,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativa
) {
}
