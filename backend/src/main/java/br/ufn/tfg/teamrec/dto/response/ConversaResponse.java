package br.ufn.tfg.teamrec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta de um turno da conversa.
 *
 * Os dois desfechos chegam com HTTP 200: uma pergunta nao e erro, e o passo
 * seguinte da conversa. Quem distingue e o campo {@code status}.
 *
 * @param resposta      o que mostrar ao gestor
 * @param faltantes     o que ainda impede gerar a recomendacao
 * @param entendido     o que o sistema compreendeu ate aqui, para o gestor conferir
 * @param recomendacao  preenchido apenas quando o status e RECOMENDACAO
 */
@Schema(description = "Turno de resposta do assistente.")
public record ConversaResponse(
        StatusConversa status,
        String resposta,
        List<String> faltantes,
        ParametrosEntendidosResponse entendido,
        RecomendacaoResponse recomendacao
) {

    public enum StatusConversa {

        /** Faltam dados; a resposta traz a pergunta a ser feita ao gestor. */
        PERGUNTA,

        /** Os requisitos fecharam: o projeto foi criado e a equipe, recomendada. */
        RECOMENDACAO
    }
}
