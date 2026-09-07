package br.ufn.tfg.teamrec.dto.request;

import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para cadastro e atualizacao de uma habilidade do catalogo.
 *
 * @param chaveDataset rotulo da coluna correspondente na matriz de treinamento.
 *                     Opcional: habilidades sem coluna no modelo (soft skills e
 *                     tecnologias fora da pesquisa) simplesmente nao o informam.
 */
@Schema(description = "Habilidade do catalogo: uma tecnologia ou competencia comportamental.")
public record HabilidadeRequest(

        @NotBlank(message = "O nome e obrigatorio.")
        @Size(max = 80, message = "O nome deve ter no maximo 80 caracteres.")
        @Schema(example = "Spring Boot")
        String nome,

        @NotNull(message = "O tipo e obrigatorio.")
        @Schema(example = "HARD")
        TipoHabilidade tipo,

        @NotNull(message = "A categoria e obrigatoria.")
        @Schema(example = "FRAMEWORK")
        CategoriaHabilidade categoria,

        @Size(max = 120, message = "A chave do dataset deve ter no maximo 120 caracteres.")
        @Schema(example = "Spring Boot", description = "Nome exato da coluna na matriz de treinamento.")
        String chaveDataset
) {

    /**
     * Normaliza os campos textuais na desserializacao, antes da validacao.
     * A chave vazia vira nula: a coluna e unica no banco e aceita varios nulos,
     * mas nao varias strings vazias.
     */
    public HabilidadeRequest {
        nome = nome == null ? null : nome.trim();
        chaveDataset = (chaveDataset == null || chaveDataset.isBlank()) ? null : chaveDataset.trim();
    }
}
