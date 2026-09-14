package br.ufn.tfg.teamrec.integration.prediction;

/**
 * Atributos do projeto enviados uma unica vez por requisicao ao motor.
 *
 * @param criticidade       nivel de 1 a 3 (coluna Proj_Criticidade).
 * @param requisitoBackend  coluna Proj_Requisito_Backend.
 * @param requisitoFrontend coluna Proj_Requisito_Frontend.
 */
public record ProjetoContexto(
        int criticidade,
        boolean requisitoBackend,
        boolean requisitoFrontend
) {
}
