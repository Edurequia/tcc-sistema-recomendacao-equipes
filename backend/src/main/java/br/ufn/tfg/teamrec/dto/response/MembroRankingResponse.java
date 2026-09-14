package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Uma posicao do ranking.
 *
 * @param papel            papel na equipe: o da vaga preenchida, ou o principal do candidato.
 * @param aderenciaTecnica proporcao da stack exigida que o candidato domina, de 0 a 1.
 * @param coberturaHoras   quanto da carga semanal do projeto o candidato atende, de 0 a 1.
 * @param elegivel         se tinha livres todas as horas exigidas; quem nao tem nunca e selecionado.
 * @param selecionado      se integra a equipe proposta.
 */
@Schema(description = "Candidato posicionado no ranking de uma recomendacao.")
public record MembroRankingResponse(
        Integer posicao,
        Long desenvolvedorId,
        String nome,
        Senioridade senioridade,
        String senioridadeDescricao,
        PapelAtuacao papel,
        String papelDescricao,
        BigDecimal scoreAfinidade,
        BigDecimal aderenciaTecnica,
        BigDecimal coberturaHoras,
        boolean elegivel,
        boolean selecionado
) {
}
