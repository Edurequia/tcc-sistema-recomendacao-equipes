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

import java.math.BigDecimal;

/**
 * Posicao de um desenvolvedor no ranking de uma recomendacao.
 *
 * O sistema persiste o ranking inteiro, e nao apenas os escolhidos. Isso
 * sustenta dois requisitos: o gestor consegue ver quem ficou de fora e por
 * qual margem, e o LLM recebe contexto suficiente para explicar por que um
 * perfil foi preterido em favor de outro.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "recomendacao_membro")
public class RecomendacaoMembro extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recomendacao_id", nullable = false)
    private Recomendacao recomendacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desenvolvedor_id", nullable = false)
    private Desenvolvedor desenvolvedor;

    /** Escore de afinidade devolvido pelo motor de predicao. */
    @Column(name = "score_afinidade", nullable = false, precision = 6, scale = 3)
    private BigDecimal scoreAfinidade;

    /** Papel que este integrante ocupa na composicao proposta. */
    @Enumerated(EnumType.STRING)
    @Column(name = "papel_sugerido", nullable = false, length = 20)
    private PapelAtuacao papelSugerido;

    /** Posicao no ranking geral, iniciando em 1. */
    @Column(name = "posicao_ranking", nullable = false)
    private Integer posicaoRanking;

    /** Verdadeiro para quem integra a equipe final; falso para o restante do ranking. */
    @Column(nullable = false)
    private boolean selecionado;

    /**
     * Features relacionais enviadas ao motor no momento da geracao, de 0 a 1.
     *
     * Gravadas porque dependem do projeto e das alocacoes daquele instante:
     * sem elas, o escore persistido deixaria de ser explicavel assim que o
     * perfil ou a agenda do desenvolvedor mudassem.
     */
    @Column(name = "aderencia_tecnica", precision = 5, scale = 4)
    private BigDecimal aderenciaTecnica;

    @Column(name = "cobertura_horas", precision = 5, scale = 4)
    private BigDecimal coberturaHoras;

    public RecomendacaoMembro(Desenvolvedor desenvolvedor, BigDecimal scoreAfinidade,
                              PapelAtuacao papelSugerido, Integer posicaoRanking, boolean selecionado) {
        this.desenvolvedor = desenvolvedor;
        this.scoreAfinidade = scoreAfinidade;
        this.papelSugerido = papelSugerido;
        this.posicaoRanking = posicaoRanking;
        this.selecionado = selecionado;
    }
}
