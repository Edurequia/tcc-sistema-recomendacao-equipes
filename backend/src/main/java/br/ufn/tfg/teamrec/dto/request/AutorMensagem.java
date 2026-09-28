package br.ufn.tfg.teamrec.dto.request;

/** Quem escreveu um turno da conversa. */
public enum AutorMensagem {

    GESTOR("Gestor"),
    ASSISTENTE("Assistente");

    private final String rotulo;

    AutorMensagem(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Como o autor aparece na transcricao enviada ao modelo. */
    public String getRotulo() {
        return rotulo;
    }
}
