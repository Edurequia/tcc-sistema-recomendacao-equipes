package br.ufn.tfg.teamrec.integration.prediction;

import java.util.List;

/**
 * Resposta do motor: um escore por candidato, na mesma ordem enviada.
 *
 * @param algoritmo nome do estimador carregado no servico, util para auditoria.
 */
public record PredicaoMotorResponse(
        String algoritmo,
        List<Predicao> predicoes
) {
}
