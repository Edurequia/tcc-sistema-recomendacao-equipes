package br.ufn.tfg.teamrec.domain.enums;

/** Agrupamento das habilidades, usado para organizar o catalogo na interface. */
public enum CategoriaHabilidade {

    LINGUAGEM("Linguagem de programacao"),
    FRAMEWORK("Framework ou biblioteca"),
    BANCO_DE_DADOS("Banco de dados"),
    CLOUD("Plataforma de nuvem"),
    DEVOPS("Ferramenta de DevOps"),
    MOBILE("Desenvolvimento mobile"),
    COMPORTAMENTAL("Competencia comportamental"),
    OUTRA("Outra");

    private final String descricao;

    CategoriaHabilidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
