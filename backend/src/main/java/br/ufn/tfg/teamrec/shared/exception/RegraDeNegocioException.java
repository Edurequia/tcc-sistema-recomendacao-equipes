package br.ufn.tfg.teamrec.shared.exception;

/**
 * Lancada quando a requisicao e sintaticamente valida mas viola uma regra
 * do dominio (por exemplo, alocar um desenvolvedor acima das horas disponiveis).
 * Resulta em HTTP 422.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
