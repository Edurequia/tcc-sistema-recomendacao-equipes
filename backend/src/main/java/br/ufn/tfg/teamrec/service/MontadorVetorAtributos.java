package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.DesenvolvedorHabilidade;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.entity.ProjetoHabilidadeRequerida;
import br.ufn.tfg.teamrec.domain.entity.ProjetoPapelRequerido;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.integration.prediction.CandidatoVetor;
import br.ufn.tfg.teamrec.integration.prediction.ProjetoContexto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converte o dominio relacional nas features do motor de predicao.
 *
 * Cada calculo aqui reproduz o script que gerou a base de treinamento. Uma
 * divergencia nao produz erro nenhum: o modelo simplesmente pontua algo
 * diferente do que aprendeu. Por isso a classe e isolada e testada sem banco.
 */
@Component
public class MontadorVetorAtributos {

    private static final Set<PapelAtuacao> PAPEIS_DE_BACKEND = EnumSet.of(PapelAtuacao.BACKEND, PapelAtuacao.FULLSTACK);
    private static final Set<PapelAtuacao> PAPEIS_DE_FRONTEND = EnumSet.of(PapelAtuacao.FRONTEND, PapelAtuacao.FULLSTACK);

    /** Mesma escala das colunas aderencia_tecnica e cobertura_horas. */
    private static final int CASAS_DECIMAIS = 4;

    public ProjetoContexto contextoDoProjeto(Projeto projeto) {
        Set<PapelAtuacao> exigidos = projeto.getPapeisRequeridos().stream()
                .map(ProjetoPapelRequerido::getPapel)
                .collect(Collectors.toSet());

        boolean backend = exigidos.stream().anyMatch(PAPEIS_DE_BACKEND::contains);
        boolean frontend = exigidos.stream().anyMatch(PAPEIS_DE_FRONTEND::contains);

        // O treinamento nunca gerou projeto sem demanda de backend nem de frontend.
        // Sem essa indicacao, o projeto e tratado como aberto aos dois lados.
        if (!backend && !frontend) {
            backend = true;
            frontend = true;
        }

        return new ProjetoContexto(projeto.getCriticidade().getNivel(), backend, frontend);
    }

    /**
     * @param horasComprometidas horas ja alocadas em OUTROS projetos ativos.
     */
    public CandidatoVetor vetorDoCandidato(Desenvolvedor desenvolvedor, Projeto projeto, int horasComprometidas) {
        return new CandidatoVetor(
                desenvolvedor.getId(),
                desenvolvedor.getSenioridade().getValorOrdinal(),
                desenvolvedor.getPapelPrincipal().getChaveDataset(),
                desenvolvedor.getSoftSkillsScore().doubleValue(),
                arredondar(aderenciaTecnica(desenvolvedor, projeto)),
                arredondar(coberturaHoras(desenvolvedor, projeto, horasComprometidas)));
    }

    /**
     * Soma dos pesos das tecnologias exigidas que o desenvolvedor domina,
     * dividida pela soma de todos os pesos exigidos.
     *
     * So entram habilidades HARD: a stack sorteada no treinamento continha
     * apenas tecnologias, e o comportamental ja chega pelo softSkillsScore.
     * Sem nenhuma tecnologia exigida, nada falta, e a aderencia e plena.
     */
    private double aderenciaTecnica(Desenvolvedor desenvolvedor, Projeto projeto) {
        List<ProjetoHabilidadeRequerida> stack = projeto.getHabilidadesRequeridas().stream()
                .filter(requisito -> requisito.getHabilidade().getTipo() == TipoHabilidade.HARD)
                .toList();

        if (stack.isEmpty()) {
            return 1.0;
        }

        Set<Long> dominadas = desenvolvedor.getHabilidades().stream()
                .map(DesenvolvedorHabilidade::getHabilidade)
                .map(Habilidade::getId)
                .collect(Collectors.toSet());

        int pesoTotal = stack.stream().mapToInt(ProjetoHabilidadeRequerida::getPeso).sum();
        int pesoAtendido = stack.stream()
                .filter(requisito -> dominadas.contains(requisito.getHabilidade().getId()))
                .mapToInt(ProjetoHabilidadeRequerida::getPeso)
                .sum();

        return (double) pesoAtendido / pesoTotal;
    }

    /** Horas livres (descontados outros projetos) sobre as horas exigidas, entre 0 e 1. */
    private double coberturaHoras(Desenvolvedor desenvolvedor, Projeto projeto, int horasComprometidas) {
        int efetivas = Math.max(0, desenvolvedor.getHorasDisponiveis() - horasComprometidas);
        return Math.min(1.0, (double) efetivas / projeto.getHorasSemanaisPorIntegrante());
    }

    private static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP).doubleValue();
    }
}
