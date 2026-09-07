package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Versao enxuta usada na listagem paginada.
 *
 * As habilidades sao omitidas de proposito: carrega-las para cada linha da
 * pagina dispararia uma consulta por desenvolvedor. Quem precisa da lista
 * completa busca o perfil pelo identificador.
 */
@Schema(description = "Desenvolvedor em formato reduzido, para listagens.")
public record DesenvolvedorResumoResponse(
        Long id,
        String nome,
        String email,
        Senioridade senioridade,
        String senioridadeDescricao,
        PapelAtuacao papelPrincipal,
        String papelPrincipalDescricao,
        BigDecimal softSkillsScore,
        Integer horasDisponiveis,
        boolean ativo
) {
}
