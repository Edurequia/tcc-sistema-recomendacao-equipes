package br.ufn.tfg.teamrec.domain.enums;

/**
 * Nivel de criticidade do projeto, conforme definido na metodologia do trabalho.
 *
 * A criticidade determina como os atributos sao ponderados no calculo do escore
 * de afinidade: quanto maior o nivel, mais peso recebem a senioridade e as
 * competencias comportamentais em relacao as competencias puramente tecnicas.
 */
public enum CriticidadeProjeto {

    BAIXA(1, "Foco majoritario no atendimento das competencias tecnicas (hard skills) e papeis exigidos."),
    MEDIA(2, "Balanceamento padrao entre habilidades tecnicas e perfil comportamental."),
    ALTA(3, "Projeto complexo e de alto risco: senioridade e soft skills recebem multiplicadores de peso elevados.");

    private final int nivel;
    private final String descricao;

    CriticidadeProjeto(int nivel, String descricao) {
        this.nivel = nivel;
        this.descricao = descricao;
    }

    /** Nivel numerico (1 a 3) enviado ao motor de predicao. */
    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }
}
