package br.ufn.tfg.teamrec.domain.repository;

/**
 * Projecao usada para obter, em UMA consulta, quantas horas cada desenvolvedor
 * ja tem comprometidas.
 *
 * Consultar isso desenvolvedor por desenvolvedor produziria um problema N+1
 * justamente no caminho mais quente do sistema — a avaliacao de dezenas de
 * candidatos durante a formacao de uma equipe.
 */
public interface HorasAlocadasProjection {

    Long getDesenvolvedorId();

    Integer getTotalHoras();
}
