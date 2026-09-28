package br.ufn.tfg.teamrec.integration.llm;

import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica o que o backend envia ao modelo de linguagem e como trata a
 * resposta. O modelo e substituido por um duble: o que se testa aqui e o
 * contrato, nao a qualidade do texto gerado.
 */
@DisplayName("Gerador da justificativa da equipe")
class GeradorJustificativaTest {

    private static final ContextoJustificativa CONTEXTO = new ContextoJustificativa(
            "Sistema de Matricula Online", "Alta", 5, 20,
            List.of("Backend (2)", "Frontend (2)", "QA / Testes (1)"),
            List.of("Java (peso 5, obrigatoria)", "React (peso 4, obrigatoria)"),
            List.of(integrante("Ana Souza", "Backend", "Senior", 86.66, true),
                    integrante("Quezia Amaral", "QA / Testes", "Pleno", 45.98, true)),
            List.of(integrante("Iara Teixeira", "FullStack", "Senior", 88.03, true),
                    integrante("Eva Martins", "Backend", "Junior", 20.52, false)),
            new ContextoJustificativa.Metricas(new BigDecimal("100.00"), new BigDecimal("86.67"),
                    new BigDecimal("0.490"), new BigDecimal("69.674")),
            "Equipe parcial: 4 de 5 integrantes.");

    private ChatModel modelo;
    private GeradorJustificativa gerador;

    @BeforeEach
    void preparar() {
        modelo = mock(ChatModel.class);
        gerador = new GeradorJustificativa(() -> modelo, "Voce explica composicoes de equipe a um gestor.");
    }

    @Test
    @DisplayName("devolve o texto produzido pelo modelo, sem espacos em volta")
    void deveDevolverOTextoDoModelo() {
        when(modelo.call(any(Prompt.class))).thenReturn(resposta("  A equipe cobre todos os papeis.  "));

        Justificativa justificativa = gerador.gerar(CONTEXTO);

        assertThat(justificativa.texto()).isEqualTo("A equipe cobre todos os papeis.");
    }

    @Test
    @DisplayName("envia o prompt de sistema e o contexto completo da recomendacao")
    void deveEnviarContextoCompleto() {
        when(modelo.call(any(Prompt.class))).thenReturn(resposta("ok"));

        gerador.gerar(CONTEXTO);

        ArgumentCaptor<Prompt> capturado = ArgumentCaptor.forClass(Prompt.class);
        org.mockito.Mockito.verify(modelo).call(capturado.capture());
        List<Message> mensagens = capturado.getValue().getInstructions();

        assertThat(mensagens).hasSize(2);
        assertThat(mensagens.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(mensagens.get(0).getText()).contains("gestor");

        String usuario = mensagens.get(1).getText();
        assertThat(mensagens.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(usuario)
                .contains("Sistema de Matricula Online")
                .contains("Backend (2)")
                .contains("Java (peso 5, obrigatoria)")
                .contains("Ana Souza")
                .contains("Quezia Amaral")
                .contains("Iara Teixeira")          // preterido: o contraste que explica a escolha
                .contains("100.00")                 // cobertura de papeis
                .contains("Equipe parcial: 4 de 5 integrantes.");
    }

    @Test
    @DisplayName("identifica no contexto quem ficou de fora por falta de horas")
    void deveMarcarInelegiveis() {
        when(modelo.call(any(Prompt.class))).thenReturn(resposta("ok"));

        gerador.gerar(CONTEXTO);

        ArgumentCaptor<Prompt> capturado = ArgumentCaptor.forClass(Prompt.class);
        org.mockito.Mockito.verify(modelo).call(capturado.capture());

        assertThat(capturado.getValue().getInstructions().get(1).getText())
                .contains("Eva Martins")
                .containsIgnoringCase("sem horas");
    }

    @Test
    @DisplayName("falha do modelo vira indisponibilidade de servico externo")
    void deveTraduzirFalhaDoModelo() {
        when(modelo.call(any(Prompt.class))).thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> gerador.gerar(CONTEXTO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    @DisplayName("resposta vazia do modelo e recusada em vez de gravada")
    void deveRecusarRespostaVazia() {
        when(modelo.call(any(Prompt.class))).thenReturn(resposta("   "));

        assertThatThrownBy(() -> gerador.gerar(CONTEXTO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("sem chave configurada, informa que a integracao esta desligada")
    void deveAvisarQuandoNaoConfigurado() {
        GeradorJustificativa semModelo = new GeradorJustificativa(() -> null, "prompt");

        assertThatThrownBy(() -> semModelo.gerar(CONTEXTO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("nao esta configurada");
    }

    // ------------------------------------------------------------------

    private static ChatResponse resposta(String texto) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))));
    }

    private static ContextoJustificativa.Integrante integrante(String nome, String papel, String senioridade,
                                                               double score, boolean elegivel) {
        return new ContextoJustificativa.Integrante(nome, papel, senioridade,
                BigDecimal.valueOf(score), new BigDecimal("0.7500"),
                elegivel ? BigDecimal.ONE : new BigDecimal("0.5000"), elegivel);
    }
}
