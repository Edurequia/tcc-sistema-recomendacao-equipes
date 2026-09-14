package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;

import java.util.List;
import java.util.Map;

/**
 * O que o projeto exige da equipe.
 *
 * @param horasExigidas  horas semanais por integrante; usado na mensagem de restricao.
 * @param vagasPorPapel  vagas minimas por papel, na ordem em que serao preenchidas.
 * @param tecnologias    stack tecnica exigida (apenas habilidades HARD).
 */
public record RequisitosEquipe(
        int tamanhoEquipe,
        int horasExigidas,
        Map<PapelAtuacao, Integer> vagasPorPapel,
        List<TecnologiaExigida> tecnologias
) {
}
