package br.ufn.tfg.teamrec.integration.prediction;

import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Cliente HTTP do motor de predicao")
class MotorPredicaoClientTest {

    private static final PredicaoMotorRequest REQUISICAO = new PredicaoMotorRequest(
            new ProjetoContexto(3, true, false),
            List.of(new CandidatoVetor(1L, 3, "Developer, back-end", 4.0, 0.75, 0.9),
                    new CandidatoVetor(2L, 1, "Developer, QA or test", 2.5, 0.0, 1.0)));

    private MockRestServiceServer servidor;
    private MotorPredicaoClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://motor");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new MotorPredicaoClient(builder.build());
    }

    @Test
    @DisplayName("envia o contrato em camelCase e devolve os escores")
    void deveEnviarContratoEReceberEscores() {
        servidor.expect(requestTo("http://motor/predicoes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projeto.criticidade").value(3))
                .andExpect(jsonPath("$.projeto.requisitoBackend").value(true))
                .andExpect(jsonPath("$.candidatos[0].nivelSenioridade").value(3))
                .andExpect(jsonPath("$.candidatos[0].papel").value("Developer, back-end"))
                .andExpect(jsonPath("$.candidatos[0].aderenciaTecnica").value(0.75))
                .andExpect(jsonPath("$.candidatos[1].coberturaHoras").value(1.0))
                .andRespond(withSuccess("""
                        {"algoritmo": "LGBMRegressor",
                         "predicoes": [{"id": 1, "score": 91.015}, {"id": 2, "score": 40.2}]}
                        """, MediaType.APPLICATION_JSON));

        PredicaoMotorResponse resposta = cliente.prever(REQUISICAO);

        assertThat(resposta.algoritmo()).isEqualTo("LGBMRegressor");
        assertThat(resposta.predicoes()).containsExactly(new Predicao(1L, 91.015), new Predicao(2L, 40.2));
        servidor.verify();
    }

    @Test
    @DisplayName("erro do motor vira falha de servico externo")
    void erroDoMotorViraServicoExterno() {
        servidor.expect(requestTo("http://motor/predicoes"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cliente.prever(REQUISICAO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("motor de predicao");
    }

    @Test
    @DisplayName("recusa de validacao do motor tambem vira falha de servico externo")
    void recusaDoMotorViraServicoExterno() {
        servidor.expect(requestTo("http://motor/predicoes"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> cliente.prever(REQUISICAO)).isInstanceOf(ServicoExternoException.class);
    }

    @Test
    @DisplayName("motor fora do ar vira falha de servico externo")
    void motorForaDoArViraServicoExterno() {
        servidor.expect(requestTo("http://motor/predicoes"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> cliente.prever(REQUISICAO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    @DisplayName("resposta que nao cobre todos os candidatos e rejeitada")
    void respostaIncompletaERejeitada() {
        servidor.expect(requestTo("http://motor/predicoes"))
                .andRespond(withSuccess("""
                        {"algoritmo": "LGBMRegressor", "predicoes": [{"id": 1, "score": 91.0}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.prever(REQUISICAO))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("[2]");
    }
}
