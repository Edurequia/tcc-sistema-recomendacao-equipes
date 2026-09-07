package br.ufn.tfg.teamrec.domain.enums;

/**
 * Ciclo de vida do projeto.
 *
 * Apenas projetos em PLANEJAMENTO ou EM_ANDAMENTO disputam desenvolvedores;
 * os demais sao ignorados no calculo de conflito de alocacao.
 */
public enum StatusProjeto {

    PLANEJAMENTO("Em planejamento", true),
    EM_ANDAMENTO("Em andamento", true),
    CONCLUIDO("Concluido", false),
    CANCELADO("Cancelado", false);

    private final String descricao;
    private final boolean consomeAlocacao;

    StatusProjeto(String descricao, boolean consomeAlocacao) {
        this.descricao = descricao;
        this.consomeAlocacao = consomeAlocacao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Indica se projetos neste status ocupam horas dos desenvolvedores. */
    public boolean isConsomeAlocacao() {
        return consomeAlocacao;
    }
}
