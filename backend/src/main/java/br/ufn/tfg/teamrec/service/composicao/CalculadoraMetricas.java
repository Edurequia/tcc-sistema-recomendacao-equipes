package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Calcula as metricas de avaliacao de uma equipe qualquer.
 *
 * Nao sabe como a equipe foi formada: recebe a lista de integrantes e os
 * requisitos. Assim, a mesma conta vale para a equipe do prototipo e para
 * as composicoes aleatorias da linha de base, o que torna a comparacao justa.
 */
@Component
public class CalculadoraMetricas {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public MetricasEquipe calcular(List<CandidatoRanqueado> equipe, RequisitosEquipe requisitos) {
        return new MetricasEquipe(
                coberturaPapeis(equipe, requisitos.vagasPorPapel()),
                coberturaTecnologias(equipe, requisitos.tecnologias()),
                equipe.isEmpty() ? null : escala(desvioPadrao(equipe, CandidatoRanqueado::nivelSenioridade), 3),
                equipe.isEmpty() ? null : escala(desvioPadrao(equipe, CandidatoRanqueado::score), 3),
                equipe.isEmpty() ? null : escala(media(equipe, CandidatoRanqueado::score), 3),
                obrigatoriasAusentes(equipe, requisitos.tecnologias()));
    }

    /**
     * Vagas atendidas sobre vagas exigidas. Cada papel conta no maximo a
     * quantidade exigida: tres backends nao compensam a falta de um frontend.
     */
    private BigDecimal coberturaPapeis(List<CandidatoRanqueado> equipe, Map<PapelAtuacao, Integer> vagas) {
        int exigidas = vagas.values().stream().mapToInt(Integer::intValue).sum();
        if (exigidas == 0) {
            return percentual(1, 1);
        }

        Map<PapelAtuacao, Long> naEquipe = equipe.stream()
                .collect(Collectors.groupingBy(CandidatoRanqueado::papel, Collectors.counting()));

        long atendidas = vagas.entrySet().stream()
                .mapToLong(vaga -> Math.min(naEquipe.getOrDefault(vaga.getKey(), 0L), vaga.getValue()))
                .sum();

        return percentual(atendidas, exigidas);
    }

    /** Pesos das tecnologias dominadas por ao menos um integrante sobre o peso total da stack. */
    private BigDecimal coberturaTecnologias(List<CandidatoRanqueado> equipe, List<TecnologiaExigida> tecnologias) {
        int pesoTotal = tecnologias.stream().mapToInt(TecnologiaExigida::peso).sum();
        if (pesoTotal == 0) {
            return percentual(1, 1);
        }

        Set<Long> dominadas = habilidadesDaEquipe(equipe);
        int pesoCoberto = tecnologias.stream()
                .filter(tecnologia -> dominadas.contains(tecnologia.habilidadeId()))
                .mapToInt(TecnologiaExigida::peso)
                .sum();

        return percentual(pesoCoberto, pesoTotal);
    }

    private List<String> obrigatoriasAusentes(List<CandidatoRanqueado> equipe, List<TecnologiaExigida> tecnologias) {
        Set<Long> dominadas = habilidadesDaEquipe(equipe);
        return tecnologias.stream()
                .filter(TecnologiaExigida::obrigatoria)
                .filter(tecnologia -> !dominadas.contains(tecnologia.habilidadeId()))
                .map(TecnologiaExigida::nome)
                .toList();
    }

    private static Set<Long> habilidadesDaEquipe(List<CandidatoRanqueado> equipe) {
        return equipe.stream()
                .flatMap(integrante -> integrante.habilidadeIds().stream())
                .collect(Collectors.toSet());
    }

    /** Desvio padrao populacional: a equipe e o conjunto inteiro, nao uma amostra. */
    private static double desvioPadrao(List<CandidatoRanqueado> equipe, ToDoubleFunction<CandidatoRanqueado> valor) {
        double media = media(equipe, valor);
        double somaQuadrados = equipe.stream()
                .mapToDouble(integrante -> Math.pow(valor.applyAsDouble(integrante) - media, 2))
                .sum();
        return Math.sqrt(somaQuadrados / equipe.size());
    }

    private static double media(List<CandidatoRanqueado> equipe, ToDoubleFunction<CandidatoRanqueado> valor) {
        return equipe.stream().mapToDouble(valor).average().orElse(0);
    }

    private static BigDecimal percentual(long parte, long total) {
        return BigDecimal.valueOf(parte).multiply(CEM)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP);
    }
}
