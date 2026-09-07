package br.ufn.tfg.teamrec.domain.entity;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
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

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Profissional candidato a compor equipes. E o "item" recomendado pelo sistema.
 *
 * Os campos desta entidade correspondem diretamente as variaveis independentes
 * (features) consumidas pelo motor de predicao:
 *
 * <table>
 *   <tr><td>{@code senioridade}</td>      <td>escala ordinal crescente</td></tr>
 *   <tr><td>{@code papelPrincipal}</td>   <td>colunas binarias one-hot</td></tr>
 *   <tr><td>{@code habilidades}</td>      <td>colunas binarias por tecnologia</td></tr>
 *   <tr><td>{@code softSkillsScore}</td>  <td>escore comportamental consolidado</td></tr>
 *   <tr><td>{@code horasDisponiveis}</td> <td>variavel numerica continua</td></tr>
 * </table>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "desenvolvedor")
public class Desenvolvedor extends EntidadeBase {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Senioridade senioridade;

    /** Area tecnica prioritaria. Um desenvolvedor tem exatamente um papel principal. */
    @Enumerated(EnumType.STRING)
    @Column(name = "papel_principal", nullable = false, length = 20)
    private PapelAtuacao papelPrincipal;

    /**
     * Escore comportamental consolidado, de 0.00 a 5.00.
     *
     * BigDecimal em vez de double para que a nota persistida seja exatamente
     * a informada — um valor 4.1 nao deve virar 4.099999 no banco.
     */
    @Column(name = "soft_skills_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal softSkillsScore;

    /** Carga horaria semanal livre para alocacao em novos projetos. */
    @Column(name = "horas_disponiveis", nullable = false)
    private Integer horasDisponiveis;

    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * Competencias do desenvolvedor com o respectivo nivel de proficiencia.
     *
     * Cascade + orphanRemoval porque a associativa nao tem vida propria: existe
     * unicamente enquanto vinculada a este desenvolvedor.
     */
    @OneToMany(mappedBy = "desenvolvedor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DesenvolvedorHabilidade> habilidades = new LinkedHashSet<>();

    public Desenvolvedor(String nome, String email, Senioridade senioridade, PapelAtuacao papelPrincipal,
                         BigDecimal softSkillsScore, Integer horasDisponiveis) {
        this.nome = nome;
        this.email = email;
        this.senioridade = senioridade;
        this.papelPrincipal = papelPrincipal;
        this.softSkillsScore = softSkillsScore;
        this.horasDisponiveis = horasDisponiveis;
        this.ativo = true;
    }

    // ------------------------------------------------------------------
    // Metodos auxiliares de relacionamento.
    // Mantem os dois lados da associacao sincronizados: confiar apenas no
    // setter da colecao deixaria o campo "desenvolvedor" do outro lado nulo.
    // ------------------------------------------------------------------

    public void adicionarHabilidade(Habilidade habilidade, Integer proficiencia) {
        DesenvolvedorHabilidade vinculo = new DesenvolvedorHabilidade(this, habilidade, proficiencia);
        habilidades.add(vinculo);
    }

    public void removerTodasHabilidades() {
        habilidades.clear();
    }
}
