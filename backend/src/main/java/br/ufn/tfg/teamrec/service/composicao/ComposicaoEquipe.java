package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regras de composicao da equipe a partir do ranking do motor.
 *
 * <ol>
 *   <li><b>Elegibilidade:</b> so participa quem tem livres todas as horas
 *       semanais exigidas. O escore mede afinidade; a disponibilidade e uma
 *       restricao, e nao pode ser compensada por um escore alto.</li>
 *   <li><b>Vagas por papel:</b> cada vaga declarada e preenchida pelo elegivel
 *       de melhor escore com exatamente aquele papel.</li>
 *   <li><b>Posicoes livres:</b> o restante, ate o tamanho da equipe, vai para
 *       os melhores escores ainda nao escolhidos, de qualquer papel.</li>
 * </ol>
 *
 * Se nao houver elegiveis suficientes, a equipe sai parcial e a restricao e
 * descrita, em vez de nao haver resultado.
 */
@Component
public class ComposicaoEquipe {

    private final CalculadoraMetricas calculadora;

    public ComposicaoEquipe(CalculadoraMetricas calculadora) {
        this.calculadora = calculadora;
    }

    /**
     * @param ranking candidatos ordenados do maior para o menor escore.
     */
    public ResultadoComposicao compor(List<CandidatoRanqueado> ranking, RequisitosEquipe requisitos) {
        List<CandidatoRanqueado> elegiveis = ranking.stream().filter(CandidatoRanqueado::elegivel).toList();
        List<CandidatoRanqueado> disponiveis = new ArrayList<>(elegiveis);
        List<MembroSelecionado> equipe = new ArrayList<>();
        Map<PapelAtuacao, Integer> vagasNaoPreenchidas = new LinkedHashMap<>();

        for (Map.Entry<PapelAtuacao, Integer> vaga : requisitos.vagasPorPapel().entrySet()) {
            int preenchidas = 0;
            for (CandidatoRanqueado candidato : List.copyOf(disponiveis)) {
                if (preenchidas == vaga.getValue() || equipe.size() == requisitos.tamanhoEquipe()) {
                    break;
                }
                if (candidato.papel() == vaga.getKey()) {
                    equipe.add(new MembroSelecionado(candidato, vaga.getKey()));
                    disponiveis.remove(candidato);
                    preenchidas++;
                }
            }
            if (preenchidas < vaga.getValue()) {
                vagasNaoPreenchidas.put(vaga.getKey(), vaga.getValue() - preenchidas);
            }
        }

        for (CandidatoRanqueado candidato : disponiveis) {
            if (equipe.size() == requisitos.tamanhoEquipe()) {
                break;
            }
            equipe.add(new MembroSelecionado(candidato, candidato.papel()));
        }

        List<CandidatoRanqueado> integrantes = equipe.stream().map(MembroSelecionado::candidato).toList();
        MetricasEquipe metricas = calculadora.calcular(integrantes, requisitos);
        int inelegiveis = ranking.size() - elegiveis.size();

        return new ResultadoComposicao(equipe, metricas,
                observacao(equipe.size(), inelegiveis, vagasNaoPreenchidas, metricas, requisitos));
    }

    private static String observacao(int selecionados, int inelegiveis, Map<PapelAtuacao, Integer> vagasNaoPreenchidas,
                                     MetricasEquipe metricas, RequisitosEquipe requisitos) {
        List<String> restricoes = new ArrayList<>();

        if (selecionados < requisitos.tamanhoEquipe()) {
            String parcial = "Equipe parcial: %d de %d integrantes.".formatted(selecionados, requisitos.tamanhoEquipe());
            if (inelegiveis > 0) {
                parcial += " %d candidato(s) sem as %d horas semanais livres exigidas."
                        .formatted(inelegiveis, requisitos.horasExigidas());
            }
            restricoes.add(parcial);
        }

        if (!vagasNaoPreenchidas.isEmpty()) {
            restricoes.add("Vagas sem profissional do papel exigido: %s.".formatted(
                    vagasNaoPreenchidas.entrySet().stream()
                            .map(vaga -> "%s (%d)".formatted(vaga.getKey().getDescricao(), vaga.getValue()))
                            .collect(Collectors.joining(", "))));
        }

        if (!metricas.tecnologiasObrigatoriasAusentes().isEmpty()) {
            restricoes.add("Tecnologias obrigatorias sem cobertura na equipe: %s.".formatted(
                    String.join(", ", metricas.tecnologiasObrigatoriasAusentes())));
        }

        return restricoes.isEmpty() ? null : String.join(" ", restricoes);
    }
}
