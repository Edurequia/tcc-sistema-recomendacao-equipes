package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.integration.llm.ExtratorParametros;
import br.ufn.tfg.teamrec.integration.llm.ParametrosExtraidos;
import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoClient;
import br.ufn.tfg.teamrec.integration.prediction.Predicao;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorRequest;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorResponse;
import br.ufn.tfg.teamrec.shared.exception.RegraDeNegocioException;
import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Via conversacional de ponta a ponta, com o extrator e o motor dublados: o
 * texto vira projeto, o projeto entra na mesma pipeline e a recomendacao fica
 * gravada com a origem e a consulta que a produziram.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API da via conversacional")
class ConversaControllerTest {

    private static final String CONSULTA =
            "Preciso de uma equipe de 2 pessoas para o Portal do Aluno, projeto critico em Java, "
                    + "com 20 horas semanais por integrante e um desenvolvedor de backend.";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private ProjetoRepository projetoRepository;

    @MockitoBean
    private MotorPredicaoClient motor;
    @MockitoBean
    private ExtratorParametros extrator;

    @BeforeEach
    void preparar() {
        Desenvolvedor ana = desenvolvedor("Ana Conversa", "ana.conversa@ufn.edu.br",
                Senioridade.SENIOR, PapelAtuacao.BACKEND);
        Desenvolvedor bruno = desenvolvedor("Bruno Conversa", "bruno.conversa@ufn.edu.br",
                Senioridade.PLENO, PapelAtuacao.FRONTEND);
        when(motor.prever(any(PredicaoMotorRequest.class))).thenReturn(new PredicaoMotorResponse(
                "LGBMRegressor",
                List.of(new Predicao(ana.getId(), 88.0), new Predicao(bruno.getId(), 70.0))));
    }

    @Test
    @DisplayName("cria o projeto a partir do texto e grava a recomendacao com origem CHAT")
    void deveGerarPelaConversa() throws Exception {
        when(extrator.extrair(anyString(), anyList())).thenReturn(parametros("Portal do Aluno", "Java"));

        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"" + CONSULTA + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.matchesPattern(".*/api/recomendacoes/\\d+")))
                .andExpect(jsonPath("$.projetoNome").value("Portal do Aluno"))
                .andExpect(jsonPath("$.origem").value("CHAT"))
                .andExpect(jsonPath("$.integrantesSelecionados").value(2))
                .andExpect(jsonPath("$.ranking[0].nome").value("Ana Conversa"));

        assertThat(projetoRepository.findAll())
                .extracting(p -> p.getNome())
                .contains("Portal do Aluno");
    }

    @Test
    @DisplayName("oferece ao extrator o catalogo de tecnologias ativas")
    void deveEnviarCatalogoAoExtrator() throws Exception {
        when(extrator.extrair(anyString(), anyList())).thenReturn(parametros("Portal Catalogo", "Java"));

        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"" + CONSULTA + "\"}"))
                .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> catalogo = ArgumentCaptor.forClass(List.class);
        verify(extrator).extrair(anyString(), catalogo.capture());
        assertThat(catalogo.getValue()).contains("Java", "React", "Spring Boot");
    }

    @Test
    @DisplayName("tecnologia fora do catalogo e recusada com 422 em vez de ignorada")
    void deveRecusarTecnologiaDesconhecida() throws Exception {
        when(extrator.extrair(anyString(), anyList()))
                .thenReturn(parametros("Portal Laravel", "Laravel"));

        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"" + CONSULTA + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Laravel")));

        verify(motor, never()).prever(any());
    }

    @Test
    @DisplayName("texto incompleto devolve 422 com o que falta, sem criar projeto")
    void deveRecusarTextoIncompleto() throws Exception {
        when(extrator.extrair(anyString(), anyList())).thenThrow(new RegraDeNegocioException(
                "Nao foi possivel entender o tamanho da equipe a partir da sua solicitacao."));

        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"preciso de um time para amanha\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("tamanho da equipe")));

        assertThat(projetoRepository.count()).isZero();
    }

    @Test
    @DisplayName("consulta curta demais e barrada pela validacao, sem chegar ao modelo")
    void deveValidarAConsulta() throws Exception {
        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"oi\"}"))
                .andExpect(status().isBadRequest());

        verify(extrator, never()).extrair(anyString(), anyList());
    }

    @Test
    @DisplayName("integracao desligada devolve 503 e nao cria projeto")
    void deveResponder503() throws Exception {
        when(extrator.extrair(anyString(), anyList())).thenThrow(new ServicoExternoException(
                "gemini", "A integracao com o modelo de linguagem nao esta configurada.", null));

        mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"" + CONSULTA + "\"}"))
                .andExpect(status().isServiceUnavailable());

        assertThat(projetoRepository.count()).isZero();
    }

    @Test
    @DisplayName("a recomendacao gerada pelo formulario continua com origem FORMULARIO")
    void deveManterOrigemDoFormulario() throws Exception {
        when(extrator.extrair(anyString(), anyList())).thenReturn(parametros("Portal Origem", "Java"));

        String corpo = mvc.perform(post("/api/recomendacoes/conversa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consulta\": \"" + CONSULTA + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long projetoId = ((Number) com.jayway.jsonpath.JsonPath.read(corpo, "$.projetoId")).longValue();

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projetoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("FORMULARIO"));
    }

    // ------------------------------------------------------------------

    private static ParametrosExtraidos parametros(String nome, String tecnologia) {
        return new ParametrosExtraidos(nome, "Extraido do pedido do gestor.",
                CriticidadeProjeto.ALTA, 2, 20,
                List.of(new ParametrosExtraidos.PapelExtraido(PapelAtuacao.BACKEND, 1)),
                List.of(new ParametrosExtraidos.TecnologiaExtraida(tecnologia, 5, true)));
    }

    private Desenvolvedor desenvolvedor(String nome, String email, Senioridade senioridade, PapelAtuacao papel) {
        return desenvolvedorRepository.saveAndFlush(new Desenvolvedor(nome, email, senioridade, papel,
                new BigDecimal("4.00"), 40));
    }
}
