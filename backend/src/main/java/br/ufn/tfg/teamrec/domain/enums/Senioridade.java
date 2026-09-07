package br.ufn.tfg.teamrec.domain.enums;

/**
 * Nivel de experiencia do desenvolvedor.
 *
 * Trata-se de um dado ordinal: existe hierarquia natural entre os niveis.
 * O campo {@code valorOrdinal} e a escala numerica crescente consumida pelo
 * motor de predicao (coluna "Nivel_Senioridade" da matriz de treinamento),
 * e tambem serve de base para o calculo de dispersao de senioridade da equipe.
 *
 * O enum e persistido por NOME (EnumType.STRING), nunca por indice, para que
 * a insercao de um novo nivel no futuro nao corrompa os registros existentes.
 */
public enum Senioridade {

    JUNIOR(1, "Junior"),
    PLENO(2, "Pleno"),
    SENIOR(3, "Senior"),
    ESPECIALISTA(4, "Especialista");

    private final int valorOrdinal;
    private final String descricao;

    Senioridade(int valorOrdinal, String descricao) {
        this.valorOrdinal = valorOrdinal;
        this.descricao = descricao;
    }

    /** Escala numerica crescente enviada ao motor de predicao. */
    public int getValorOrdinal() {
        return valorOrdinal;
    }

    public String getDescricao() {
        return descricao;
    }
}
