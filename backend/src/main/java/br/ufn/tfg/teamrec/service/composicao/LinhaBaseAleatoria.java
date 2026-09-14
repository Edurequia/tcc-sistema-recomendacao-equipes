package br.ufn.tfg.teamrec.service.composicao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Linha de base da avaliacao: equipes formadas ao acaso.
 *
 * Responde se as metricas da equipe proposta sao melhores do que as de uma
 * escolha sem nenhum criterio. Para a comparacao ser justa:
 * <ul>
 *   <li>o sorteio acontece so entre os <b>elegiveis</b> (quem tem as horas
 *       exigidas), o mesmo grupo de onde o prototipo escolhe;</li>
 *   <li>cada equipe sorteada e medida pela mesma {@link CalculadoraMetricas};</li>
 *   <li>a semente fixa torna o experimento reproduzivel.</li>
 * </ul>
 */
@Component
public class LinhaBaseAleatoria {

    private final CalculadoraMetricas calculadora;

    public LinhaBaseAleatoria(CalculadoraMetricas calculadora) {
        this.calculadora = calculadora;
    }

    public ResultadoLinhaBase sortear(List<CandidatoRanqueado> candidatos, RequisitosEquipe requisitos,
                                      int sorteios, long semente) {
        // Ordenar antes de embaralhar: o resultado depende so da semente,
        // e nao da ordem em que os candidatos chegaram.
        List<CandidatoRanqueado> elegiveis = candidatos.stream()
                .filter(CandidatoRanqueado::elegivel)
                .sorted(Comparator.comparingLong(CandidatoRanqueado::desenvolvedorId))
                .toList();
        if (elegiveis.isEmpty()) {
            throw new IllegalArgumentException("Nao ha candidatos elegiveis para sortear.");
        }

        int tamanho = Math.min(requisitos.tamanhoEquipe(), elegiveis.size());
        Random aleatorio = new Random(semente);
        List<CandidatoRanqueado> baralho = new ArrayList<>(elegiveis);
        List<MetricasEquipe> resultados = new ArrayList<>(sorteios);

        for (int i = 0; i < sorteios; i++) {
            Collections.shuffle(baralho, aleatorio);
            resultados.add(calculadora.calcular(List.copyOf(baralho.subList(0, tamanho)), requisitos));
        }

        return new ResultadoLinhaBase(elegiveis.size(), tamanho,
                agregar(resultados, LinhaBaseAleatoria::media),
                agregar(resultados, LinhaBaseAleatoria::desvioPadrao));
    }

    private static MetricasAgregadas agregar(List<MetricasEquipe> resultados, Function<double[], Double> estatistica) {
        return new MetricasAgregadas(
                escala(estatistica.apply(valores(resultados, MetricasEquipe::coberturaPapeis)), 2),
                escala(estatistica.apply(valores(resultados, MetricasEquipe::coberturaTecnologias)), 2),
                escala(estatistica.apply(valores(resultados, MetricasEquipe::desvioPadraoSenioridade)), 3),
                escala(estatistica.apply(valores(resultados, MetricasEquipe::desvioPadraoScore)), 3),
                escala(estatistica.apply(valores(resultados, MetricasEquipe::scoreMedioEquipe)), 3));
    }

    private static double[] valores(List<MetricasEquipe> resultados, Function<MetricasEquipe, BigDecimal> metrica) {
        return resultados.stream().map(metrica).mapToDouble(BigDecimal::doubleValue).toArray();
    }

    private static Double media(double[] valores) {
        return Arrays.stream(valores).average().orElse(0);
    }

    /** Desvio padrao populacional, a mesma convencao das metricas da equipe. */
    private static Double desvioPadrao(double[] valores) {
        double media = media(valores);
        return Math.sqrt(Arrays.stream(valores).map(v -> Math.pow(v - media, 2)).sum() / valores.length);
    }

    private static BigDecimal escala(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP);
    }
}
