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
import static org.assertj.core.api.Assertions.within;

@DisplayName("Linha de base: composicao aleatoria entre os elegiveis")
class LinhaBaseAleatoriaTest {

    private final LinhaBaseAleatoria linhaBase = new LinhaBaseAleatoria(new CalculadoraMetricas());

    @Test
    @DisplayName("a media converge para o valor esperado pelo acaso")
    void mediaConvergeParaOEsperado() {
        // 2 backends e 2 frontends, equipe de 2, uma vaga de cada.
        // Das 6 duplas possiveis, 4 cobrem os dois papeis (100%) e 2 cobrem um so (50%):
        // esperado = (4 x 100 + 2 x 50) / 6 = 83,33%
        List<CandidatoRanqueado> candidatos = List.of(
                candidato(1, PapelAtuacao.BACKEND, 1.0),
                candidato(2, PapelAtuacao.BACKEND, 1.0),
                candidato(3, PapelAtuacao.FRONTEND, 1.0),
                candidato(4, PapelAtuacao.FRONTEND, 1.0));

        ResultadoLinhaBase resultado = linhaBase.sortear(candidatos, requisitos(2), 10_000, 42L);

        assertThat(resultado.media().coberturaPapeis().doubleValue()).isCloseTo(83.33, within(1.0));
        assertThat(resultado.candidatosElegiveis()).isEqualTo(4);
        assertThat(resultado.tamanhoEquipeSorteada()).isEqualTo(2);
    }

    @Test
    @DisplayName("quem nao tem as horas exigidas nunca entra no sorteio")
    void sorteiaApenasElegiveis() {
        // So o inelegivel domina a tecnologia exigida: se ele fosse sorteado, a cobertura subiria.
        List<CandidatoRanqueado> candidatos = List.of(
                candidato(1, PapelAtuacao.BACKEND, 1.0),
                candidato(2, PapelAtuacao.BACKEND, 1.0),
                new CandidatoRanqueado(3, "Sem horas", PapelAtuacao.BACKEND, 3, 99, 0.5, Set.of(7L)));
        RequisitosEquipe requisitos = new RequisitosEquipe(2, 20, Map.of(),
                List.of(new TecnologiaExigida(7L, "Kubernetes", 5, true)));

        ResultadoLinhaBase resultado = linhaBase.sortear(candidatos, requisitos, 500, 1L);

        assertThat(resultado.candidatosElegiveis()).isEqualTo(2);
        assertThat(resultado.media().coberturaTecnologias()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a mesma semente reproduz exatamente o mesmo resultado")
    void sementeTornaReproduzivel() {
        List<CandidatoRanqueado> candidatos = List.of(
                candidato(1, PapelAtuacao.BACKEND, 1.0), candidato(2, PapelAtuacao.FRONTEND, 1.0),
                candidato(3, PapelAtuacao.QA, 1.0), candidato(4, PapelAtuacao.MOBILE, 1.0),
                candidato(5, PapelAtuacao.FULLSTACK, 1.0));

        ResultadoLinhaBase primeira = linhaBase.sortear(candidatos, requisitos(3), 100, 42L);
        ResultadoLinhaBase segunda = linhaBase.sortear(List.copyOf(candidatos).reversed(), requisitos(3), 100, 42L);

        assertThat(segunda).isEqualTo(primeira);
    }

    @Test
    @DisplayName("com menos elegiveis que o tamanho da equipe, todo sorteio e a mesma equipe parcial")
    void poucosElegiveis() {
        List<CandidatoRanqueado> candidatos = List.of(
                candidato(1, PapelAtuacao.BACKEND, 1.0),
                candidato(2, PapelAtuacao.FRONTEND, 0.2));

        ResultadoLinhaBase resultado = linhaBase.sortear(candidatos, requisitos(3), 50, 42L);

        assertThat(resultado.tamanhoEquipeSorteada()).isEqualTo(1);
        assertThat(resultado.media().coberturaPapeis()).isEqualByComparingTo("50.00");
        assertThat(resultado.desvioPadrao().coberturaPapeis()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("o desvio entre sorteios mostra o quanto o acaso varia")
    void desvioEntreSorteios() {
        List<CandidatoRanqueado> candidatos = List.of(
                candidato(1, PapelAtuacao.BACKEND, 1.0), candidato(2, PapelAtuacao.BACKEND, 1.0),
                candidato(3, PapelAtuacao.FRONTEND, 1.0), candidato(4, PapelAtuacao.FRONTEND, 1.0));

        ResultadoLinhaBase resultado = linhaBase.sortear(candidatos, requisitos(2), 2_000, 7L);

        // valores 100 (2/3) e 50 (1/3): desvio esperado ~23,6
        assertThat(resultado.desvioPadrao().coberturaPapeis().doubleValue()).isCloseTo(23.6, within(1.5));
    }

    // ------------------------------------------------------------------

    private static CandidatoRanqueado candidato(long id, PapelAtuacao papel, double coberturaHoras) {
        return new CandidatoRanqueado(id, "Dev " + id, papel, 2, 50 + id, coberturaHoras, Set.of());
    }

    private static RequisitosEquipe requisitos(int tamanho) {
        Map<PapelAtuacao, Integer> vagas = new LinkedHashMap<>();
        vagas.put(PapelAtuacao.BACKEND, 1);
        vagas.put(PapelAtuacao.FRONTEND, 1);
        return new RequisitosEquipe(tamanho, 20, vagas, List.of());
    }
}
