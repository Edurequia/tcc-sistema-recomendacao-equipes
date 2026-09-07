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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Compromisso efetivo de um desenvolvedor com um projeto, em horas semanais.
 *
 * Esta entidade e o que torna possivel o cenario de avaliacao de
 * <strong>resolucao de conflitos de alocacao</strong>, previsto na metodologia:
 * ao formar varias equipes simultaneamente, o sistema precisa saber quantas
 * horas de cada profissional ja estao comprometidas com outros projetos.
 *
 * A distincao em relacao a {@link RecomendacaoMembro} e importante: aquela e
 * uma <em>sugestao</em> gerada pelo motor; esta e a alocacao <em>confirmada</em>
 * pelo gestor, e so ela consome disponibilidade.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "alocacao")
public class Alocacao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desenvolvedor_id", nullable = false)
    private Desenvolvedor desenvolvedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    /** Papel exercido pelo desenvolvedor neste projeto especifico. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelAtuacao papel;

    /** Horas semanais comprometidas com este projeto. */
    @Column(name = "horas_semanais", nullable = false)
    private Integer horasSemanais;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Nulo enquanto a alocacao nao tiver termino definido. */
    @Column(name = "data_fim")
    private LocalDate dataFim;

    /**
     * Alocacoes encerradas sao mantidas como historico, mas deixam de consumir
     * a disponibilidade do desenvolvedor.
     */
    @Column(nullable = false)
    private boolean ativa = true;

    public Alocacao(Desenvolvedor desenvolvedor, Projeto projeto, PapelAtuacao papel,
                    Integer horasSemanais, LocalDate dataInicio) {
        this.desenvolvedor = desenvolvedor;
        this.projeto = projeto;
        this.papel = papel;
        this.horasSemanais = horasSemanais;
        this.dataInicio = dataInicio;
        this.ativa = true;
    }

    /** Encerra a alocacao, liberando as horas do desenvolvedor a partir da data informada. */
    public void encerrar(LocalDate dataFim) {
        this.dataFim = dataFim;
        this.ativa = false;
    }
}
