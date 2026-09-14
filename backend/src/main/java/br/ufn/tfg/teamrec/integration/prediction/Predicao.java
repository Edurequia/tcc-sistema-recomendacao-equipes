package br.ufn.tfg.teamrec.integration.prediction;

/**
 * Escore de afinidade de um candidato.
 *
 * @param score valor aproximadamente entre 0 e 100.
 */
public record Predicao(
        long id,
        double score
) {
}
