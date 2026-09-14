package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As metricas sao as da metodologia do trabalho e alimentam as tabelas dos
 * cenarios de avaliacao; os valores esperados aqui foram calculados a mao.
 */
@DisplayName("Metricas de avaliacao da equipe")
class CalculadoraMetricasTest {

    private static final long JAVA = 1L;
    private static final long SPRING = 2L;
    private static final long POSTGRES = 3L;

    private final CalculadoraMetricas calculadora = new CalculadoraMetricas();

    @Test
    @DisplayName("cobertura de papeis conta as vagas atendidas, limitadas ao exigido por papel")
    void coberturaDePapeis() {
        RequisitosEquipe requisitos = requisitos(vagas(PapelAtuacao.BACKEND, 2, PapelAtuacao.FRONTEND, 1), List.of());
        List<CandidatoRanqueado> equipe = List.of(
                candidato(1, PapelAtuacao.FRONTEND, 3, 80),
                candidato(2, PapelAtuacao.FRONTEND, 2, 70),   // segunda frontend nao cobre vaga extra
                candidato(3, PapelAtuacao.BACKEND, 2, 60));

        // backend 1 de 2 + frontend 1 de 1 = 2 de 3 vagas
        assertThat(calculadora.calcular(equipe, requisitos).coberturaPapeis()).isEqualByComparingTo("66.67");
    }

    @Test
    @DisplayName("sem papeis exigidos, a cobertura de papeis e plena")
    void semPapeisExigidos() {
        MetricasEquipe metricas = calculadora.calcular(
                List.of(candidato(1, PapelAtuacao.QA, 1, 50)), requisitos(Map.of(), List.of()));

        assertThat(metricas.coberturaPapeis()).isEqualByComparingTo("100.00");
        assertThat(metricas.coberturaTecnologias()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("cobertura de tecnologias pondera pelo peso e aponta obrigatorias ausentes")
    void coberturaDeTecnologias() {
        RequisitosEquipe requisitos = requisitos(Map.of(), List.of(
                new TecnologiaExigida(JAVA, "Java", 5, true),
                new TecnologiaExigida(SPRING, "Spring Boot", 4, true),
                new TecnologiaExigida(POSTGRES, "PostgreSQL", 3, false)));
        List<CandidatoRanqueado> equipe = List.of(
                candidato(1, PapelAtuacao.BACKEND, 3, 90, JAVA),
                candidato(2, PapelAtuacao.BACKEND, 2, 80, JAVA, POSTGRES));

        MetricasEquipe metricas = calculadora.calcular(equipe, requisitos);

        // Java (5) + PostgreSQL (3) de 12; tecnologia coberta por dois conta uma vez
        assertThat(metricas.coberturaTecnologias()).isEqualByComparingTo("66.67");
        assertThat(metricas.tecnologiasObrigatoriasAusentes()).containsExactly("Spring Boot");
    }

    @Test
    @DisplayName("dispersao usa o desvio padrao populacional da senioridade e do escore")
    void dispersaoEMedia() {
        List<CandidatoRanqueado> equipe = List.of(
                candidato(1, PapelAtuacao.BACKEND, 3, 90),
                candidato(2, PapelAtuacao.BACKEND, 1, 70),
                candidato(3, PapelAtuacao.BACKEND, 2, 80));

        MetricasEquipe metricas = calculadora.calcular(equipe, requisitos(Map.of(), List.of()));

        assertThat(metricas.desvioPadraoSenioridade()).isEqualByComparingTo("0.816");
        assertThat(metricas.desvioPadraoScore()).isEqualByComparingTo("8.165");
        assertThat(metricas.scoreMedioEquipe()).isEqualByComparingTo("80.000");
    }

    @Test
    @DisplayName("integrante unico tem dispersao zero")
    void integranteUnico() {
        MetricasEquipe metricas = calculadora.calcular(
                List.of(candidato(1, PapelAtuacao.BACKEND, 3, 91.015)), requisitos(Map.of(), List.of()));

        assertThat(metricas.desvioPadraoSenioridade()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metricas.scoreMedioEquipe()).isEqualByComparingTo("91.015");
    }

    @Test
    @DisplayName("equipe vazia nao cobre nada e nao tem dispersao")
    void equipeVazia() {
        RequisitosEquipe requisitos = requisitos(vagas(PapelAtuacao.BACKEND, 1, null, 0),
                List.of(new TecnologiaExigida(JAVA, "Java", 5, true)));

        MetricasEquipe metricas = calculadora.calcular(List.of(), requisitos);

        assertThat(metricas.coberturaPapeis()).isEqualByComparingTo("0.00");
        assertThat(metricas.coberturaTecnologias()).isEqualByComparingTo("0.00");
        assertThat(metricas.desvioPadraoSenioridade()).isNull();
        assertThat(metricas.desvioPadraoScore()).isNull();
        assertThat(metricas.scoreMedioEquipe()).isNull();
        assertThat(metricas.tecnologiasObrigatoriasAusentes()).containsExactly("Java");
    }

    // ------------------------------------------------------------------

    static CandidatoRanqueado candidato(long id, PapelAtuacao papel, int senioridade, double score, Long... habilidades) {
        return new CandidatoRanqueado(id, "Dev " + id, papel, senioridade, score, 1.0, Set.of(habilidades));
    }

    static RequisitosEquipe requisitos(Map<PapelAtuacao, Integer> vagas, List<TecnologiaExigida> tecnologias) {
        return new RequisitosEquipe(4, 20, vagas, tecnologias);
    }

    static Map<PapelAtuacao, Integer> vagas(PapelAtuacao papelA, int qtdA, PapelAtuacao papelB, int qtdB) {
        Map<PapelAtuacao, Integer> vagas = new LinkedHashMap<>();
        vagas.put(papelA, qtdA);
        if (papelB != null) {
            vagas.put(papelB, qtdB);
        }
        return vagas;
    }
}
