package br.ufn.tfg.teamrec.shared.exception;

/**
 * Lancada quando a operacao colide com um registro que ja existe, como um
 * e-mail ou um nome de habilidade duplicado. Resulta em HTTP 409.
 *
 * A verificacao e feita no service, antes de chegar ao banco, para que a
 * resposta diga exatamente qual valor conflitou — a violacao de restricao
 * capturada no handler global produz apenas uma mensagem generica.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
