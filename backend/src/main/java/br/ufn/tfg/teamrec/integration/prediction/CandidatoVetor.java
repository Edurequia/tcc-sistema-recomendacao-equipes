package br.ufn.tfg.teamrec.integration.prediction;

/**
 * Atributos de um desenvolvedor em relacao a um projeto, no formato do motor.
 *
 * @param id               identificador do desenvolvedor, devolvido junto do escore.
 * @param nivelSenioridade escala ordinal de 1 a 3 (coluna Nivel_Senioridade).
 * @param papel            rotulo exato da coluna do papel na matriz de treinamento.
 * @param softSkillsScore  escore comportamental, de 0 a 5.
 * @param aderenciaTecnica proporcao dos pesos da stack exigida que o desenvolvedor domina.
 * @param coberturaHoras   horas efetivas sobre as horas exigidas, limitado a 1.
 */
public record CandidatoVetor(
        long id,
        int nivelSenioridade,
        String papel,
        double softSkillsScore,
        double aderenciaTecnica,
        double coberturaHoras
) {
}
