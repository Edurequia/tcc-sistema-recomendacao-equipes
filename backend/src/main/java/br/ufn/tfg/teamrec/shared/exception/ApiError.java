package br.ufn.tfg.teamrec.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Corpo padronizado de todas as respostas de erro da API.
 * Manter um unico formato evita que o frontend precise tratar
 * cada endpoint de um jeito diferente.
 *
 * @param campos detalhamento por campo; presente apenas em erros de validacao.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos
) {

    public static ApiError de(int status, String erro, String mensagem, String caminho) {
        return new ApiError(OffsetDateTime.now(), status, erro, mensagem, caminho, null);
    }

    public static ApiError deValidacao(int status, String mensagem, String caminho, List<CampoInvalido> campos) {
        return new ApiError(OffsetDateTime.now(), status, "Erro de validacao", mensagem, caminho, campos);
    }

    /** Um campo reprovado na validacao e o motivo. */
    public record CampoInvalido(String campo, String motivo) {
    }
}
