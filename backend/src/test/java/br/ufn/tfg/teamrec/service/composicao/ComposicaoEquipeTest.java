package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Composicao da equipe a partir do ranking")
class ComposicaoEquipeTest {

    private final ComposicaoEquipe composicao = new ComposicaoEquipe(new CalculadoraMetricas());

    @Test
    @DisplayName("preenche primeiro as vagas por papel e depois as posicoes livres pelo escore")
    void vagasPorPapelDepoisPosicoesLivres() {
        List<CandidatoRanqueado> ranking = List.of(
                candidato(1, PapelAtuacao.FULLSTACK, 95, 1.0),
                candidato(2, PapelAtuacao.BACKEND, 90, 1.0),
                candidato(3, PapelAtuacao.QA, 85, 1.0),
                candidato(4, PapelAtuacao.FRONTEND, 60, 1.0),
                candidato(5, PapelAtuacao.BACKEND, 50, 1.0));
        RequisitosEquipe requisitos = new RequisitosEquipe(3, 20,
                vagas(PapelAtuacao.BACKEND, 1, PapelAtuacao.FRONTEND, 1), List.of());

        ResultadoComposicao resultado = composicao.compor(ranking, requisitos);

        // backend: o melhor backend (2); frontend: o unico (4); posicao livre: melhor restante (1)
        assertThat(resultado.equipe()).extracting(m -> m.candidato().desenvolvedorId())
                .containsExactly(2L, 4L, 1L);
        assertThat(resultado.equipe()).extracting(MembroSelecionado::papelSugerido)
                .containsExactly(PapelAtuacao.BACKEND, PapelAtuacao.FRONTEND, PapelAtuacao.FULLSTACK);
        assertThat(resultado.metricas().coberturaPapeis()).isEqualByComparingTo("100.00");
        assertThat(resultado.observacaoRestricao()).isNull();
    }

    @Test
    @DisplayName("sem papeis exigidos, forma a equipe com os melhores escores elegiveis")
    void semPapeisExigidos() {
        List<CandidatoRanqueado> ranking = List.of(
                candidato(1, PapelAtuacao.QA, 95, 1.0),
                candidato(2, PapelAtuacao.MOBILE, 90, 1.0),
                candidato(3, PapelAtuacao.DEVOPS, 85, 1.0));

        ResultadoComposicao resultado = composicao.compor(ranking, new RequisitosEquipe(2, 20, Map.of(), List.of()));

        assertThat(resultado.equipe()).extracting(m -> m.candidato().desenvolvedorId()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("quem nao tem as horas exigidas nunca e selecionado, mesmo com escore maior")
    void inelegivelNuncaSelecionado() {
        List<CandidatoRanqueado> ranking = List.of(
                candidato(1, PapelAtuacao.BACKEND, 98, 0.4),
                candidato(2, PapelAtuacao.BACKEND, 80, 1.0));

        ResultadoComposicao resultado = composicao.compor(ranking,
                new RequisitosEquipe(1, 20, vagas(PapelAtuacao.BACKEND, 1, null, 0), List.of()));

        assertThat(resultado.equipe()).extracting(m -> m.candidato().desenvolvedorId()).containsExactly(2L);
        assertThat(resultado.observacaoRestricao()).isNull();
    }

    @Test
    @DisplayName("com poucos elegiveis, propoe equipe parcial e explica a restricao")
    void equipeParcial() {
        List<CandidatoRanqueado> ranking = List.of(
                candidato(1, PapelAtuacao.BACKEND, 98, 0.5),
                candidato(2, PapelAtuacao.BACKEND, 90, 1.0),
                candidato(3, PapelAtuacao.FRONTEND, 80, 0.0));

        ResultadoComposicao resultado = composicao.compor(ranking,
                new RequisitosEquipe(3, 20, vagas(PapelAtuacao.BACKEND, 2, null, 0), List.of()));

        assertThat(resultado.equipe()).hasSize(1);
        assertThat(resultado.metricas().coberturaPapeis()).isEqualByComparingTo("50.00");
        assertThat(resultado.observacaoRestricao())
                .contains("Equipe parcial: 1 de 3 integrantes")
                .contains("2 candidato(s) sem as 20 horas semanais livres exigidas")
                .contains("Backend (1)");
    }

    @Test
    @DisplayName("vaga sem profissional do papel e ocupada pelo melhor disponivel e sinalizada")
    void vagaSemPapelOcupadaPorOutro() {
        List<CandidatoRanqueado> ranking = List.of(
                candidato(1, PapelAtuacao.FULLSTACK, 88, 1.0),
                candidato(2, PapelAtuacao.FRONTEND, 70, 1.0));

        ResultadoComposicao resultado = composicao.compor(ranking,
                new RequisitosEquipe(2, 20, vagas(PapelAtuacao.BACKEND, 1, PapelAtuacao.FRONTEND, 1), List.of()));

        assertThat(resultado.equipe()).extracting(m -> m.candidato().desenvolvedorId()).containsExactly(2L, 1L);
        assertThat(resultado.metricas().coberturaPapeis()).isEqualByComparingTo("50.00");
        assertThat(resultado.observacaoRestricao())
                .doesNotContain("Equipe parcial")
                .contains("Vagas sem profissional do papel exigido: Backend (1)");
    }

    @Test
    @DisplayName("tecnologia obrigatoria ausente na equipe entra na observacao")
    void obrigatoriaAusente() {
        List<CandidatoRanqueado> ranking = List.of(candidato(1, PapelAtuacao.BACKEND, 90, 1.0));
        RequisitosEquipe requisitos = new RequisitosEquipe(1, 20, Map.of(),
                List.of(new TecnologiaExigida(7L, "Kubernetes", 4, true)));

        assertThat(composicao.compor(ranking, requisitos).observacaoRestricao())
                .contains("Tecnologias obrigatorias sem cobertura na equipe: Kubernetes");
    }

    // ------------------------------------------------------------------

    private static CandidatoRanqueado candidato(long id, PapelAtuacao papel, double score, double coberturaHoras) {
        return new CandidatoRanqueado(id, "Dev " + id, papel, 2, score, coberturaHoras, Set.of());
    }

    private static Map<PapelAtuacao, Integer> vagas(PapelAtuacao papelA, int qtdA, PapelAtuacao papelB, int qtdB) {
        Map<PapelAtuacao, Integer> vagas = new LinkedHashMap<>();
        vagas.put(papelA, qtdA);
        if (papelB != null) {
            vagas.put(papelB, qtdB);
        }
        return vagas;
    }
}
