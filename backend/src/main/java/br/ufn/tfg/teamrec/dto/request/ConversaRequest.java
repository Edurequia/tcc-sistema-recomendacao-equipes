package br.ufn.tfg.teamrec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Conversa inteira a cada chamada: o cliente guarda o historico e o reenvia.
 *
 * O backend nao mantem estado de sessao de proposito. Guardar a conversa
 * exigiria uma entidade nova, e o que tem valor de pesquisa neste trabalho e a
 * consulta que originou a recomendacao, ja gravada em {@code consultaOriginal}.
 */
@Schema(description = "Historico completo da conversa, do primeiro turno ate a mensagem atual.")
public record ConversaRequest(

        @NotEmpty(message = "A conversa deve ter ao menos uma mensagem.")
        @Size(max = 20, message = "A conversa esta longa demais; comece uma nova.")
        @Valid
        List<Mensagem> mensagens
) {

    @Schema(description = "Um turno da conversa.")
    public record Mensagem(

            @NotNull(message = "O autor da mensagem e obrigatorio.")
            @Schema(example = "GESTOR")
            AutorMensagem autor,

            @Size(min = 1, max = 2000, message = "A mensagem deve ter entre 1 e 2000 caracteres.")
            @Schema(example = "Preciso de uma equipe para o Portal do Aluno.")
            String texto
    ) {

        public Mensagem {
            texto = texto == null ? null : texto.trim();
        }
    }

    /**
     * A conversa so avanca quando a bola esta com o sistema. Terminar no
     * assistente significaria pedir uma resposta para a propria pergunta.
     */
    @AssertTrue(message = "A ultima mensagem deve ser do gestor.")
    @Schema(hidden = true)
    public boolean isUltimaMensagemDoGestor() {
        if (mensagens == null || mensagens.isEmpty()) {
            return true;   // a ausencia de mensagens ja e reprovada por @NotEmpty
        }
        return mensagens.get(mensagens.size() - 1).autor() == AutorMensagem.GESTOR;
    }

    /** Transcricao para o modelo, com os papeis identificados. */
    public String transcricao() {
        return mensagens.stream()
                .map(mensagem -> "%s: %s".formatted(mensagem.autor().getRotulo(), mensagem.texto()))
                .reduce((anterior, proxima) -> anterior + "\n" + proxima)
                .orElse("");
    }

    /** So o que o gestor disse, que e o conteudo a guardar como consulta original. */
    public String falasDoGestor() {
        return mensagens.stream()
                .filter(mensagem -> mensagem.autor() == AutorMensagem.GESTOR)
                .map(ConversaRequest.Mensagem::texto)
                .reduce((anterior, proxima) -> anterior + " " + proxima)
                .orElse("");
    }
}
