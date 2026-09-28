package br.ufn.tfg.teamrec.integration.llm;

import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.function.Supplier;

/**
 * Traduz uma recomendacao ja calculada em um texto que o gestor consegue ler.
 *
 * O modelo chega por um {@link Supplier} e nao por injecao direta porque a
 * integracao e opcional: sem chave de API configurada nao existe bean de
 * {@link ChatModel}, e o restante do sistema precisa continuar funcionando.
 * Nesse caso a chamada falha de forma explicita, como 503, em vez de o
 * endpoint sumir do contrato.
 */
public class GeradorJustificativa {

    static final String SERVICO = "gemini";

    private final Supplier<ChatModel> fonteDoModelo;
    private final String promptSistema;

    public GeradorJustificativa(Supplier<ChatModel> fonteDoModelo, String promptSistema) {
        this.fonteDoModelo = fonteDoModelo;
        this.promptSistema = promptSistema;
    }

    public Justificativa gerar(ContextoJustificativa contexto) {
        ChatModel modelo = fonteDoModelo.get();
        if (modelo == null) {
            throw new ServicoExternoException(SERVICO,
                    "A integracao com o modelo de linguagem nao esta configurada. "
                            + "Defina GEMINI_API_KEY e SPRING_AI_MODEL_CHAT=google-genai.", null);
        }

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(promptSistema),
                new UserMessage(contexto.formatar())));

        ChatResponse resposta;
        try {
            resposta = modelo.call(prompt);
        } catch (RuntimeException ex) {
            throw new ServicoExternoException(SERVICO,
                    "O modelo de linguagem esta indisponivel no momento.", ex);
        }

        String texto = textoDe(resposta);
        if (texto.isBlank()) {
            // Gravar uma justificativa vazia deixaria a recomendacao parecendo
            // explicada quando nao esta; melhor falhar e deixar o gestor repetir.
            throw new ServicoExternoException(SERVICO,
                    "O modelo de linguagem devolveu uma resposta vazia.", null);
        }
        return new Justificativa(texto.trim(), modeloDe(resposta));
    }

    private static String textoDe(ChatResponse resposta) {
        if (resposta == null || resposta.getResult() == null || resposta.getResult().getOutput() == null) {
            return "";
        }
        String texto = resposta.getResult().getOutput().getText();
        return texto == null ? "" : texto;
    }

    private static String modeloDe(ChatResponse resposta) {
        return resposta.getMetadata() == null ? null : resposta.getMetadata().getModel();
    }
}
