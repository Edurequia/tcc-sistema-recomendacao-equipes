package br.ufn.tfg.teamrec.service.composicao;

/**
 * Tecnologia da stack do projeto.
 *
 * @param peso        importancia de 1 a 5; pondera a cobertura de tecnologias.
 * @param obrigatoria se ausente na equipe inteira, a restricao e sinalizada.
 */
public record TecnologiaExigida(
        long habilidadeId,
        String nome,
        int peso,
        boolean obrigatoria
) {
}
