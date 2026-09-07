package br.ufn.tfg.teamrec.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vinculo entre um desenvolvedor e uma habilidade, com o nivel de dominio.
 *
 * A relacao poderia ser um N:N simples, mas ela carrega um atributo proprio —
 * a proficiencia —, o que exige uma entidade associativa explicita.
 *
 * A matriz de treinamento atual e binaria (domina / nao domina), entao na
 * montagem do vetor de atributos qualquer proficiencia maior que zero e
 * convertida em 1. Guardar a graduacao desde ja permite refinar o modelo
 * depois sem alterar o esquema do banco.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "desenvolvedor_habilidade",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_desenvolvedor_habilidade",
                columnNames = {"desenvolvedor_id", "habilidade_id"})
)
public class DesenvolvedorHabilidade extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desenvolvedor_id", nullable = false)
    private Desenvolvedor desenvolvedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    /** Nivel de dominio declarado, de 1 (basico) a 5 (especialista). */
    @Column(nullable = false)
    private Integer proficiencia;

    public DesenvolvedorHabilidade(Desenvolvedor desenvolvedor, Habilidade habilidade, Integer proficiencia) {
        this.desenvolvedor = desenvolvedor;
        this.habilidade = habilidade;
        this.proficiencia = proficiencia;
    }
}
