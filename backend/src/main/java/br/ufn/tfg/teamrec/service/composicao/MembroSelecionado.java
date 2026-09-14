package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;

/**
 * Integrante escolhido e o papel que ocupa na equipe.
 *
 * @param papelSugerido o papel da vaga preenchida; nas posicoes sem papel fixo,
 *                      o proprio papel principal do candidato.
 */
public record MembroSelecionado(
        CandidatoRanqueado candidato,
        PapelAtuacao papelSugerido
) {
}
