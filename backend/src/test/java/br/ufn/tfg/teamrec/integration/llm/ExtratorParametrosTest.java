package br.ufn.tfg.teamrec.integration.llm;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extracao dos parametros do projeto a partir do texto do gestor.
 *
 * O modelo e um duble: o que se verifica e o contrato de ida (o que ele recebe
 * para escolher) e o de volta (como uma resposta ruim e tratada).
 */
@DisplayName("Extrator de parametros da conversa")
class ExtratorParametrosTest {

    private static final List<String> CATALOGO = List.of("Java", "Spring Boot", "React", "PostgreSQL");

    private ChatModel modelo;
    private ExtratorParametros extrator;

    @BeforeEach
    void preparar() {
        modelo = mock(ChatModel.class);
        extrator = new ExtratorParametros(() -> modelo, "Voce extrai parametros de projeto.");
    }

    @Test
    @DisplayName("converte o texto do gestor no conjunto de parametros")
    void deveExtrairParametros() {
        responde("""
                {
                  "nome": "Portal do Aluno",
                  "descricao": "Reescrita do portal",
                  "criticidade": "ALTA",
                  "tamanhoEquipe": 4,
                  "horasSemanaisPorIntegrante": 20,
                  "papeis": [{"papel": "BACKEND", "quantidade": 2}],
                  "tecnologias": [{"nome": "Java", "peso": 5, "obrigatoria": true}]
                }
                """);

        ParametrosExtraidos parametros = extrator.extrair("Preciso de 4 pessoas para o Portal do Aluno", CATALOGO);

        assertThat(parametros.nome()).isEqualTo("Portal do Aluno");
        assertThat(parametros.criticidade()).isEqualTo(CriticidadeProjeto.ALTA);
        assertThat(parametros.tamanhoEquipe()).isEqualTo(4);
        assertThat(parametros.horasSemanaisPorIntegrante()).isEqualTo(20);
        assertThat(parametros.papeisOuVazio()).singleElement()
                .satisfies(papel -> {
                    assertThat(papel.papel()).isEqualTo(PapelAtuacao.BACKEND);
                    assertThat(papel.quantidade()).isEqualTo(2);
                });
        assertThat(parametros.tecnologiasOuVazio()).singleElement()
                .satisfies(tecnologia -> {
                    assertThat(tecnologia.nome()).isEqualTo("Java");
                    assertThat(tecnologia.peso()).isEqualTo(5);
                    assertThat(tecnologia.obrigatoria()).isTrue();
                });
    }

    @Test
    @DisplayName("oferece ao modelo o catalogo e os valores validos dos enums")
    void deveEnviarCatalogoEEnums() {
        responde(jsonCompleto());

        extrator.extrair("time para o portal", CATALOGO);

        ArgumentCaptor<Prompt> capturado = ArgumentCaptor.forClass(Prompt.class);
        verify(modelo).call(capturado.capture());
        String enviado = capturado.getValue().getInstructions().get(1).getText();

        assertThat(enviado)
                .contains("time para o portal")
                .contains("Java")
                .contains("Spring Boot")
                .contains("BACKEND")          // papeis aceitos
                .contains("ALTA");            // criticidades aceitas
    }

    @Test
    @DisplayName("criticidade omitida fica nula, e o padrao media so entra no uso")
    void deveDistinguirCriticidadeOmitidaDoPadrao() {
        responde("""
                {"nome": "Portal", "tamanhoEquipe": 3, "horasSemanaisPorIntegrante": 20}
                """);

        ParametrosExtraidos parametros = extrator.extrair("time para o portal", CATALOGO);

        // O que o gestor disse e o que o sistema assume sao coisas diferentes:
        // a primeira vai para a tela de conferencia, a segunda para o projeto.
        assertThat(parametros.criticidade()).isNull();
        assertThat(parametros.criticidadeOuPadrao()).isEqualTo(CriticidadeProjeto.MEDIA);
    }

    @Test
    @DisplayName("texto sem os dados essenciais e recusado dizendo o que falta")
    void deveRecusarTextoIncompleto() {
        responde("""
                {"nome": null, "tamanhoEquipe": null, "horasSemanaisPorIntegrante": null}
                """);

        assertThatThrownBy(() -> extrator.extrair("preciso de um time", CATALOGO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nome do projeto")
                .hasMessageContaining("tamanho da equipe")
                .hasMessageContaining("horas semanais");
    }

    @Test
    @DisplayName("a extracao parcial nao recusa texto incompleto: apenas relata o que falta")
    void deveExtrairParcialSemRecusar() {
        responde("""
                {"nome": "Portal", "tamanhoEquipe": null, "horasSemanaisPorIntegrante": null}
                """);

        ParametrosExtraidos parametros = extrator.extrairParcial("quero um time para o portal", CATALOGO);

        assertThat(parametros.nome()).isEqualTo("Portal");
        assertThat(parametros.faltantes())
                .containsExactly("o tamanho da equipe", "as horas semanais por integrante");
    }

    @Test
    @DisplayName("resposta que nao e o JSON esperado vira indisponibilidade")
    void deveTratarRespostaInvalida() {
        responde("desculpe, nao entendi o pedido");

        assertThatThrownBy(() -> extrator.extrair("time para o portal", CATALOGO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("resposta");
    }

    @Test
    @DisplayName("falha do modelo vira indisponibilidade de servico externo")
    void deveTratarFalhaDoModelo() {
        when(modelo.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> extrator.extrair("time para o portal", CATALOGO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    @DisplayName("sem chave configurada, informa que a integracao esta desligada")
    void deveAvisarQuandoNaoConfigurado() {
        ExtratorParametros semModelo = new ExtratorParametros(() -> null, "prompt");

        assertThatThrownBy(() -> semModelo.extrair("time para o portal", CATALOGO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("nao esta configurada");
    }

    // ------------------------------------------------------------------

    private void responde(String texto) {
        when(modelo.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(texto)))));
    }

    private static String jsonCompleto() {
        return """
                {"nome": "Portal", "criticidade": "MEDIA", "tamanhoEquipe": 3,
                 "horasSemanaisPorIntegrante": 20}
                """;
    }
}
