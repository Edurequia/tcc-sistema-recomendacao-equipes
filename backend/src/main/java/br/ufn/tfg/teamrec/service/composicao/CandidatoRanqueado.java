package br.ufn.tfg.teamrec.service.composicao;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;

import java.util.Set;

/**
 * Candidato ja pontuado pelo motor, com o necessario para compor a equipe e
 * calcular as metricas. Nao depende de JPA: a mesma estrutura serve para a
 * recomendacao real e para composicoes de comparacao, como a linha de base.
 *
 * @param nivelSenioridade escala ordinal de 1 a 3.
 * @param coberturaHoras   horas efetivas sobre as exigidas, de 0 a 1.
 * @param habilidadeIds    habilidades do catalogo que o candidato domina.
 */
public record CandidatoRanqueado(
        long desenvolvedorId,
        String nome,
        PapelAtuacao papel,
        int nivelSenioridade,
        double score,
        double coberturaHoras,
        Set<Long> habilidadeIds
) {

    /** Elegivel e quem tem livres todas as horas semanais que o projeto exige. */
    public boolean elegivel() {
        return coberturaHoras >= 1.0;
    }
}
