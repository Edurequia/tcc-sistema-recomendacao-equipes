package br.ufn.tfg.teamrec.domain.entity;

import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item do catalogo de competencias: uma tecnologia (hard skill) ou uma
 * competencia comportamental (soft skill).
 *
 * O catalogo e centralizado de proposito. Se cada desenvolvedor digitasse
 * livremente suas tecnologias, "React", "ReactJS" e "react" virariam entradas
 * distintas e o vetor de atributos enviado ao motor de predicao ficaria
 * inconsistente.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "habilidade")
public class Habilidade extends EntidadeBase {

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoHabilidade tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaHabilidade categoria;

    /**
     * Rotulo exato da coluna correspondente na matriz de treinamento
     * (ex.: "Amazon Web Services (AWS)", "HTML/CSS").
     *
     * Fica separado de {@code nome} porque o rotulo do dataset costuma ser
     * verboso demais para a interface. Habilidades sem coluna no modelo
     * mantem este campo nulo e sao simplesmente ignoradas ao montar o vetor.
     */
    @Column(name = "chave_dataset", unique = true, length = 120)
    private String chaveDataset;

    @Column(nullable = false)
    private boolean ativa = true;

    public Habilidade(String nome, TipoHabilidade tipo, CategoriaHabilidade categoria, String chaveDataset) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
        this.chaveDataset = chaveDataset;
        this.ativa = true;
    }

    /** Indica se a habilidade possui coluna correspondente no motor de predicao. */
    public boolean possuiColunaNoModelo() {
        return chaveDataset != null && !chaveDataset.isBlank();
    }
}
