package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados de entrada para cadastro e atualizacao de um desenvolvedor.
 *
 * Os limites declarados aqui espelham as escalas usadas no treinamento do
 * modelo: escore comportamental de 0 a 5 e carga horaria semanal ate 60 horas.
 * Validar na borda evita que um valor fora de escala chegue ao motor de
 * predicao e produza um escore sem sentido.
 */
@Schema(description = "Perfil de um desenvolvedor candidato a compor equipes.")
public record DesenvolvedorRequest(

        @NotBlank(message = "O nome e obrigatorio.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
        @Schema(example = "Ana Souza")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        @Size(max = 150, message = "O e-mail deve ter no maximo 150 caracteres.")
        @Schema(example = "ana.souza@ufn.edu.br")
        String email,

        @NotNull(message = "A senioridade e obrigatoria.")
        @Schema(example = "SENIOR")
        Senioridade senioridade,

        @NotNull(message = "O papel principal e obrigatorio.")
        @Schema(example = "BACKEND")
        PapelAtuacao papelPrincipal,

        @NotNull(message = "O escore de soft skills e obrigatorio.")
        @DecimalMin(value = "0.0", message = "O escore minimo e 0.")
        @DecimalMax(value = "5.0", message = "O escore maximo e 5.")
        @Digits(integer = 1, fraction = 2, message = "Use no maximo duas casas decimais.")
        @Schema(example = "4.20")
        BigDecimal softSkillsScore,

        @NotNull(message = "As horas disponiveis sao obrigatorias.")
        @Min(value = 0, message = "As horas disponiveis nao podem ser negativas.")
        @Max(value = 60, message = "O limite e 60 horas semanais.")
        @Schema(example = "30")
        Integer horasDisponiveis,

        @Valid
        @Schema(description = "Competencias do desenvolvedor. Substitui integralmente a lista anterior na atualizacao.")
        List<VinculoHabilidadeRequest> habilidades
) {

    /**
     * Normaliza os campos textuais no momento da desserializacao.
     *
     * A ordem importa: o Jackson constroi o record e so entao o Bean Validation
     * examina os campos. Fazer a limpeza aqui — e nao no service — garante que
     * um e-mail colado com espaco em volta seja aceito, em vez de reprovado
     * pelo @Email antes mesmo de chegar a regra de negocio.
     */
    public DesenvolvedorRequest {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim().toLowerCase();
    }

    /** Evita null pelo resto do codigo quando o campo e omitido no JSON. */
    public List<VinculoHabilidadeRequest> habilidadesOuVazio() {
        return habilidades == null ? List.of() : habilidades;
    }
}
