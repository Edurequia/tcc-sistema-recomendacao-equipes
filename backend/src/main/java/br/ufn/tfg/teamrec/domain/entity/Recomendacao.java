package br.ufn.tfg.teamrec.domain.entity;

import br.ufn.tfg.teamrec.domain.enums.OrigemRecomendacao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de uma equipe sugerida para um projeto, com os indicadores de
 * qualidade calculados e a justificativa redigida pelo modelo de linguagem.
 *
 * Cada execucao gera um novo registro em vez de sobrescrever o anterior: o
 * historico das recomendacoes e um dado de pesquisa deste trabalho, permitindo
 * comparar os resultados obtidos nos diferentes cenarios de avaliacao.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "recomendacao")
public class Recomendacao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    /** Por qual das duas vias de entrada esta recomendacao foi solicitada. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemRecomendacao origem = OrigemRecomendacao.FORMULARIO;

    /** Texto em linguagem natural digitado pelo gestor. Preenchido apenas na origem CHAT. */
    @Column(name = "consulta_original", length = 2000)
    private String consultaOriginal;

    /** Avaliacao qualitativa produzida pelo LLM explicando a composicao sugerida. */
    @Column(name = "justificativa_llm", columnDefinition = "TEXT")
    private String justificativaLlm;

    // ------------------------------------------------------------------
    // Metricas de avaliacao definidas na metodologia do trabalho.
    // Sao gravadas junto da recomendacao para que o resultado permaneca
    // auditavel mesmo que os perfis dos desenvolvedores mudem depois.
    // ------------------------------------------------------------------

    /** Percentual de papeis exigidos efetivamente preenchidos pela equipe (0 a 100). */
    @Column(name = "cobertura_papeis", precision = 5, scale = 2)
    private BigDecimal coberturaPapeis;

    /** Percentual de aderencia entre a stack requerida e a dominada pela equipe (0 a 100). */
    @Column(name = "cobertura_tecnologias", precision = 5, scale = 2)
    private BigDecimal coberturaTecnologias;

    /** Desvio padrao da senioridade dentro do time. Valores baixos indicam melhor nivelamento. */
    @Column(name = "desvio_padrao_senioridade", precision = 6, scale = 3)
    private BigDecimal desvioPadraoSenioridade;

    /** Desvio padrao do escore preditivo dentro do time. */
    @Column(name = "desvio_padrao_score", precision = 6, scale = 3)
    private BigDecimal desvioPadraoScore;

    /** Media dos escores de afinidade dos integrantes selecionados. */
    @Column(name = "score_medio_equipe", precision = 6, scale = 3)
    private BigDecimal scoreMedioEquipe;

    /** Registrado quando a equipe nao pode ser completada (cenario de escassez de profissionais). */
    @Column(name = "observacao_restricao", length = 1000)
    private String observacaoRestricao;

    @OneToMany(mappedBy = "recomendacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicaoRanking ASC")
    private List<RecomendacaoMembro> membros = new ArrayList<>();

    public Recomendacao(Projeto projeto, OrigemRecomendacao origem) {
        this.projeto = projeto;
        this.origem = origem;
    }

    public void adicionarMembro(RecomendacaoMembro membro) {
        membro.setRecomendacao(this);
        membros.add(membro);
    }

    /** Integrantes que de fato compoem a equipe sugerida, ignorando o restante do ranking. */
    public List<RecomendacaoMembro> getMembrosSelecionados() {
        return membros.stream().filter(RecomendacaoMembro::isSelecionado).toList();
    }
}
