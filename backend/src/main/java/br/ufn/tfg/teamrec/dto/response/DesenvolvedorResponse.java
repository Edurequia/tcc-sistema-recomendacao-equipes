package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Perfil completo de um desenvolvedor, com as competencias resolvidas.
 *
 * As descricoes acompanham os enums para que o frontend exiba
 * "QA / Testes" sem precisar manter sua propria tabela de traducao.
 */
@Schema(description = "Perfil completo do desenvolvedor.")
public record DesenvolvedorResponse(
        Long id,
        String nome,
        String email,
        Senioridade senioridade,
        String senioridadeDescricao,
        PapelAtuacao papelPrincipal,
        String papelPrincipalDescricao,
        BigDecimal softSkillsScore,
        Integer horasDisponiveis,
        boolean ativo,
        List<VinculoHabilidadeResponse> habilidades
) {
}
