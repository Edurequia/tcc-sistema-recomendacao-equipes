package br.ufn.tfg.teamrec.integration.llm;

/**
 * Texto produzido pelo modelo de linguagem e o identificador do modelo que o
 * produziu, lido da propria resposta em vez da configuracao: o que interessa
 * registrar no trabalho e o modelo que de fato respondeu.
 */
public record Justificativa(String texto, String modelo) {
}
