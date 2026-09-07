package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados de entrada para cadastro e atualizacao de um projeto.
 *
 * Este e o lado da demanda: descreve contra o que os desenvolvedores serao
 * avaliados. A criticidade e o campo de maior consequencia — e ela que define
 * o quanto senioridade e competencias comportamentais pesam no calculo do
 * escore de afinidade, em relacao as competencias puramente tecnicas.
 *
 * @param horasSemanaisPorIntegrante carga esperada de cada integrante. Base do
 *                                   controle de conflito quando varios projetos
 *                                   disputam a mesma pessoa.
 */
@Schema(description = "Projeto para o qual uma equipe sera formada.")
public record ProjetoRequest(

        @NotBlank(message = "O nome e obrigatorio.")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
        @Schema(example = "Portal Academico")
        String nome,

        @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres.")
        @Schema(example = "Reescrita do portal do aluno, com integracao ao sistema academico.")
        String descricao,

        @NotNull(message = "A criticidade e obrigatoria.")
        @Schema(example = "ALTA")
        CriticidadeProjeto criticidade,

        @NotNull(message = "O tamanho da equipe e obrigatorio.")
        @Min(value = 1, message = "A equipe deve ter ao menos 1 integrante.")
        @Max(value = 50, message = "O limite e 50 integrantes.")
        @Schema(example = "4")
        Integer tamanhoEquipe,

        @NotNull(message = "As horas semanais por integrante sao obrigatorias.")
        @Min(value = 1, message = "O minimo e 1 hora semanal.")
        @Max(value = 60, message = "O limite e 60 horas semanais.")
        @Schema(example = "20")
        Integer horasSemanaisPorIntegrante,

        @Schema(example = "2026-10-01")
        LocalDate dataInicioPrevista,

        @Schema(example = "2027-03-31")
        LocalDate dataFimPrevista,

        @Valid
        @Schema(description = "Vagas por papel. O somatorio nao pode exceder o tamanho da equipe.")
        List<PapelRequeridoRequest> papeisRequeridos,

        @Valid
        @Schema(description = "Stack exigida. Substitui integralmente a anterior na atualizacao.")
        List<HabilidadeRequeridaRequest> habilidadesRequeridas
) {

    /** Normaliza os campos textuais na desserializacao, antes da validacao. */
    public ProjetoRequest {
        nome = nome == null ? null : nome.trim();
        descricao = (descricao == null || descricao.isBlank()) ? null : descricao.trim();
    }

    public List<PapelRequeridoRequest> papeisOuVazio() {
        return papeisRequeridos == null ? List.of() : papeisRequeridos;
    }

    public List<HabilidadeRequeridaRequest> habilidadesOuVazio() {
        return habilidadesRequeridas == null ? List.of() : habilidadesRequeridas;
    }
}
