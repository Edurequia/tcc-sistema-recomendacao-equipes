package br.ufn.tfg.teamrec.integration.prediction;

import java.util.List;

/** Corpo de {@code POST /predicoes}: o projeto uma vez e todos os candidatos em lote. */
public record PredicaoMotorRequest(
        ProjetoContexto projeto,
        List<CandidatoVetor> candidatos
) {
}
