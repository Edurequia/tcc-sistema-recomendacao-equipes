package br.ufn.tfg.teamrec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Ajuste pontual da carga horaria semanal disponivel.
 *
 * Existe como operacao propria porque e a unica alteracao que o proprio
 * desenvolvedor faz com frequencia — sem precisar reenviar o perfil inteiro
 * e correr o risco de sobrescrever suas habilidades sem querer.
 */
@Schema(description = "Nova disponibilidade semanal do desenvolvedor.")
public record DisponibilidadeRequest(

        @NotNull(message = "As horas disponiveis sao obrigatorias.")
        @Min(value = 0, message = "As horas disponiveis nao podem ser negativas.")
        @Max(value = 60, message = "O limite e 60 horas semanais.")
        @Schema(example = "24")
        Integer horasDisponiveis
) {
}
