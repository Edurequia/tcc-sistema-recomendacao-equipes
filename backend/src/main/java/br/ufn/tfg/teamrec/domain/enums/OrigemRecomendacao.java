package br.ufn.tfg.teamrec.domain.enums;

/**
 * Identifica por qual das duas vias de entrada a recomendacao foi solicitada.
 *
 * O trabalho preve uma abordagem hibrida: o gestor pode preencher formularios
 * estruturados ou descrever a necessidade em linguagem natural, caso em que o
 * LLM atua como extrator de entidades antes de acionar a mesma pipeline.
 */
public enum OrigemRecomendacao {

    FORMULARIO("Via de fluxo estruturado (formularios)"),
    CHAT("Via de fluxo conversacional (interface conversacional)");

    private final String descricao;

    OrigemRecomendacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
