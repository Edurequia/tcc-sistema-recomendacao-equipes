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
 * Tecnologia exigida pelo projeto, com peso e indicacao de obrigatoriedade.
 *
 * Sustenta a metrica de <strong>cobertura de tecnologias</strong>: o percentual
 * de aderencia entre a stack requerida e o somatorio de proficiencia dos
 * integrantes sugeridos.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "projeto_habilidade_requerida",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_projeto_habilidade",
                columnNames = {"projeto_id", "habilidade_id"})
)
public class ProjetoHabilidadeRequerida extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    /**
     * Se verdadeiro, a ausencia desta tecnologia na equipe reprova a cobertura.
     * Se falso, ela apenas contribui positivamente para o percentual de aderencia.
     */
    @Column(nullable = false)
    private boolean obrigatoria = true;

    /** Importancia relativa da tecnologia para o projeto, de 1 a 5. */
    @Column(nullable = false)
    private Integer peso = 3;

    public ProjetoHabilidadeRequerida(Projeto projeto, Habilidade habilidade, boolean obrigatoria, Integer peso) {
        this.projeto = projeto;
        this.habilidade = habilidade;
        this.obrigatoria = obrigatoria;
        this.peso = peso;
    }
}
