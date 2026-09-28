package br.ufn.tfg.teamrec.integration.llm;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Primeira metade da via conversacional: converte o pedido em portugues do
 * gestor nos requisitos estruturados de um projeto.
 *
 * O catalogo de tecnologias vai no prompt a cada chamada porque e ele que
 * delimita as escolhas possiveis: pedir ao modelo que escolha de uma lista
 * fechada evita que ele invente uma tecnologia que o sistema nao conhece.
 */
public class ExtratorParametros {

    static final String SERVICO = "gemini";

    private final Supplier<ChatModel> fonteDoModelo;
    private final String promptSistema;
    private final BeanOutputConverter<ParametrosExtraidos> conversor =
            new BeanOutputConverter<>(ParametrosExtraidos.class);

    public ExtratorParametros(Supplier<ChatModel> fonteDoModelo, String promptSistema) {
        this.fonteDoModelo = fonteDoModelo;
        this.promptSistema = promptSistema;
    }

    /**
     * Extracao de uma tacada so: o que o texto nao informar e erro do cliente.
     * Usada pela chamada avulsa, em que nao ha conversa para completar o que falta.
     */
    public ParametrosExtraidos extrair(String consulta, List<String> tecnologiasDisponiveis) {
        ParametrosExtraidos parametros = extrairParcial(consulta, tecnologiasDisponiveis);

        List<String> faltantes = parametros.faltantes();
        if (!faltantes.isEmpty()) {
            // 422: a requisicao esta bem formada, o que falta e informacao no
            // texto. Dizer qual e mais util do que pedir para reformular.
            throw new RegraDeNegocioException(
                    "Nao foi possivel entender %s a partir da sua solicitacao. "
                            .formatted(String.join(", ", faltantes))
                            + "Informe esses dados no texto e tente de novo.");
        }
        return parametros;
    }

    /**
     * Extracao sem recusa: devolve o que deu para entender, ainda que
     * incompleto. E o que a conversa precisa, porque ali um pedido pela metade
     * nao e erro, e o turno seguinte da pergunta.
     */
    public ParametrosExtraidos extrairParcial(String consulta, List<String> tecnologiasDisponiveis) {
        ChatModel modelo = fonteDoModelo.get();
        if (modelo == null) {
            throw new ServicoExternoException(SERVICO,
                    "A integracao com o modelo de linguagem nao esta configurada. "
                            + "Defina GEMINI_API_KEY e SPRING_AI_MODEL_CHAT=google-genai.", null);
        }

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(promptSistema),
                new UserMessage(mensagem(consulta, tecnologiasDisponiveis))));

        ChatResponse resposta;
        try {
            resposta = modelo.call(prompt);
        } catch (RuntimeException ex) {
            throw new ServicoExternoException(SERVICO,
                    "O modelo de linguagem esta indisponivel no momento.", ex);
        }

        return converter(resposta);
    }

    private ParametrosExtraidos converter(ChatResponse resposta) {
        String texto = resposta == null || resposta.getResult() == null
                || resposta.getResult().getOutput() == null
                ? null
                : resposta.getResult().getOutput().getText();
        try {
            ParametrosExtraidos parametros = conversor.convert(texto == null ? "" : texto);
            if (parametros == null) {
                throw new IllegalStateException("conversao devolveu nulo");
            }
            return parametros;
        } catch (RuntimeException ex) {
            throw new ServicoExternoException(SERVICO,
                    "O modelo de linguagem devolveu uma resposta que nao pode ser interpretada.", ex);
        }
    }

    private String mensagem(String consulta, List<String> tecnologiasDisponiveis) {
        return """
                SOLICITACAO DO GESTOR
                %s

                PAPEIS ACEITOS (use exatamente estes identificadores)
                %s

                CRITICIDADES ACEITAS
                %s

                TECNOLOGIAS DO CATALOGO (escolha apenas entre estas, pelo nome exato)
                %s

                FORMATO DA RESPOSTA
                %s
                """.formatted(consulta, valores(PapelAtuacao.values()), valores(CriticidadeProjeto.values()),
                String.join(", ", tecnologiasDisponiveis), conversor.getFormat());
    }

    private static String valores(Enum<?>[] valores) {
        return Arrays.stream(valores).map(Enum::name).collect(Collectors.joining(", "));
    }
}
