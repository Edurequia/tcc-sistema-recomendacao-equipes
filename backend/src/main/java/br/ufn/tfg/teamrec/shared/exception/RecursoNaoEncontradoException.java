package br.ufn.tfg.teamrec.shared.exception;

/** Lancada quando um recurso referenciado por id nao existe. Resulta em HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s de id %s nao encontrado(a).".formatted(recurso, id));
    }
}
