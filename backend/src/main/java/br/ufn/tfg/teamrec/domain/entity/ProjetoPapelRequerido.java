package br.ufn.tfg.teamrec.domain.entity;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quantas vagas de determinado papel o projeto exige (ex.: 2 backend, 1 QA).
 *
 * E a base da metrica de <strong>cobertura de papeis</strong>: verificar se a
 * equipe recomendada preenche todas as posicoes minimas do escopo.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "projeto_papel_requerido",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_projeto_papel",
                columnNames = {"projeto_id", "papel"})
)
public class ProjetoPapelRequerido extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelAtuacao papel;

    /** Numero de integrantes necessarios neste papel. */
    @Column(nullable = false)
    private Integer quantidade;

    public ProjetoPapelRequerido(Projeto projeto, PapelAtuacao papel, Integer quantidade) {
        this.projeto = projeto;
        this.papel = papel;
        this.quantidade = quantidade;
    }
}
