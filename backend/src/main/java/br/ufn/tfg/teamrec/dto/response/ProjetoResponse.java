package br.ufn.tfg.teamrec.dto.response;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Projeto completo, com papeis e tecnologias exigidos.
 *
 * @param criticidadeNivel  valor numerico de 1 a 3 enviado ao motor de predicao.
 * @param vagasDeclaradas   somatorio das vagas distribuidas por papel.
 * @param vagasSemPapelFixo posicoes ainda livres na equipe. Maior que zero
 *                          significa que parte do time pode ser preenchida por
 *                          qualquer papel, conforme o escore de afinidade.
 */
@Schema(description = "Projeto completo, com seus requisitos.")
public record ProjetoResponse(
        Long id,
        String nome,
        String descricao,
        CriticidadeProjeto criticidade,
        String criticidadeDescricao,
        int criticidadeNivel,
        StatusProjeto status,
        String statusDescricao,
        Integer tamanhoEquipe,
        Integer horasSemanaisPorIntegrante,
        LocalDate dataInicioPrevista,
        LocalDate dataFimPrevista,
        int vagasDeclaradas,
        int vagasSemPapelFixo,
        List<PapelRequeridoResponse> papeisRequeridos,
        List<HabilidadeRequeridaResponse> habilidadesRequeridas
) {
}
