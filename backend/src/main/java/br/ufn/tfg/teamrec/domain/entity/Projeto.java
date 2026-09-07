package br.ufn.tfg.teamrec.domain.entity;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Projeto para o qual uma equipe sera formada.
 *
 * Concentra a demanda contra a qual os desenvolvedores sao avaliados: quais
 * papeis precisam ser preenchidos, qual a stack exigida, quao critico e o
 * projeto e de quantas pessoas o time deve ser composto.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "projeto")
public class Projeto extends EntidadeBase {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 2000)
    private String descricao;

    /** Define como senioridade e soft skills sao ponderadas no escore de afinidade. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CriticidadeProjeto criticidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProjeto status = StatusProjeto.PLANEJAMENTO;

    /** Quantidade de integrantes desejada para a equipe. */
    @Column(name = "tamanho_equipe", nullable = false)
    private Integer tamanhoEquipe;

    /** Carga horaria semanal esperada de cada integrante. Base do controle de conflito de alocacao. */
    @Column(name = "horas_semanais_por_integrante", nullable = false)
    private Integer horasSemanaisPorIntegrante;

    @Column(name = "data_inicio_prevista")
    private LocalDate dataInicioPrevista;

    @Column(name = "data_fim_prevista")
    private LocalDate dataFimPrevista;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjetoPapelRequerido> papeisRequeridos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjetoHabilidadeRequerida> habilidadesRequeridas = new LinkedHashSet<>();

    public Projeto(String nome, String descricao, CriticidadeProjeto criticidade,
                   Integer tamanhoEquipe, Integer horasSemanaisPorIntegrante) {
        this.nome = nome;
        this.descricao = descricao;
        this.criticidade = criticidade;
        this.tamanhoEquipe = tamanhoEquipe;
        this.horasSemanaisPorIntegrante = horasSemanaisPorIntegrante;
        this.status = StatusProjeto.PLANEJAMENTO;
    }

    // ------------------------------------------------------------------
    // Metodos auxiliares de relacionamento
    // ------------------------------------------------------------------

    public void adicionarPapelRequerido(br.ufn.tfg.teamrec.domain.enums.PapelAtuacao papel, Integer quantidade) {
        papeisRequeridos.add(new ProjetoPapelRequerido(this, papel, quantidade));
    }

    public void adicionarHabilidadeRequerida(Habilidade habilidade, boolean obrigatoria, Integer peso) {
        habilidadesRequeridas.add(new ProjetoHabilidadeRequerida(this, habilidade, obrigatoria, peso));
    }

    public void removerTodosPapeisRequeridos() {
        papeisRequeridos.clear();
    }

    public void removerTodasHabilidadesRequeridas() {
        habilidadesRequeridas.clear();
    }

    /** Soma das vagas declaradas por papel. Usada para validar contra o tamanho da equipe. */
    public int totalDeVagasPorPapel() {
        return papeisRequeridos.stream()
                .mapToInt(ProjetoPapelRequerido::getQuantidade)
                .sum();
    }
}
