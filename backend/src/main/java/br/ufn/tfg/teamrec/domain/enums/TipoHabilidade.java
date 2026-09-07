package br.ufn.tfg.teamrec.domain.enums;

/**
 * Distingue competencias tecnicas de comportamentais.
 *
 * A separacao importa porque as duas entram de forma diferente no motor de
 * predicao: as HARD viram colunas binarias por tecnologia, enquanto as SOFT
 * sao consolidadas em um unico escore medio por desenvolvedor.
 */
public enum TipoHabilidade {

    HARD("Competencia tecnica"),
    SOFT("Competencia comportamental");

    private final String descricao;

    TipoHabilidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
