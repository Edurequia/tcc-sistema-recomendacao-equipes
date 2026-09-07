package br.ufn.tfg.teamrec.shared.exception;

/**
 * Lancada quando um microsservico externo falha ou esta indisponivel
 * (motor de predicao em FastAPI ou API do Gemini). Resulta em HTTP 503.
 *
 * Isolar essa falha e o que impede que a indisponibilidade de um servico
 * de apoio derrube o backend inteiro.
 */
public class ServicoExternoException extends RuntimeException {

    private final String servico;

    public ServicoExternoException(String servico, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.servico = servico;
    }

    public String getServico() {
        return servico;
    }
}
