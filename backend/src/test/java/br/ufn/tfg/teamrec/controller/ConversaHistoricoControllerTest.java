package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.domain.repository.RecomendacaoRepository;
import br.ufn.tfg.teamrec.integration.llm.ExtratorParametros;
import br.ufn.tfg.teamrec.integration.llm.ParametrosExtraidos;
import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoClient;
import br.ufn.tfg.teamrec.integration.prediction.Predicao;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorRequest;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conversa com historico: um pedido incompleto vira pergunta, e a conversa
 * segue ate os requisitos fecharem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API da conversa com historico")
class ConversaHistoricoControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private ProjetoRepository projetoRepository;
    @Autowired
    private RecomendacaoRepository recomendacaoRepository;

    @MockitoBean
    private MotorPredicaoClient motor;
    @MockitoBean
    private ExtratorParametros extrator;

    @BeforeEach
    void preparar() {
        Desenvolvedor ana = desenvolvedor("Ana Historico", "ana.historico@ufn.edu.br",
                Senioridade.SENIOR, PapelAtuacao.BACKEND);
        Desenvolvedor bruno = desenvolvedor("Bruno Historico", "bruno.historico@ufn.edu.br",
                Senioridade.PLENO, PapelAtuacao.FRONTEND);
        when(motor.prever(any(PredicaoMotorRequest.class))).thenReturn(new PredicaoMotorResponse(
                "LGBMRegressor",
                List.of(new Predicao(ana.getId(), 88.0), new Predicao(bruno.getId(), 70.0))));
    }

    @Test
    @DisplayName("pedido incompleto vira pergunta com 200, nomeando o que falta")
    void devePerguntarOQueFalta() throws Exception {
        when(extrator.extrairParcial(anyString(), anyList()))
                .thenReturn(incompleto("Portal do Aluno"));

        mvc.perform(conversa("""
                        {"mensagens": [
                          {"autor": "GESTOR", "texto": "Quero uma equipe para o Portal do Aluno."}
                        ]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PERGUNTA"))
                .andExpect(jsonPath("$.resposta", containsString("o tamanho da equipe")))
                .andExpect(jsonPath("$.resposta", containsString("horas semanais")))
                .andExpect(jsonPath("$.faltantes.length()").value(2))
                .andExpect(jsonPath("$.entendido.nome").value("Portal do Aluno"))
                .andExpect(jsonPath("$.recomendacao").doesNotExist());

        assertThat(projetoRepository.count()).isZero();
        verify(motor, never()).prever(any());
    }

    @Test
    @DisplayName("quando os requisitos fecham, a conversa entrega a recomendacao")
    void deveRecomendarQuandoCompleto() throws Exception {
        when(extrator.extrairParcial(anyString(), anyList())).thenReturn(completo("Portal do Aluno"));

        mvc.perform(conversa("""
                        {"mensagens": [
                          {"autor": "GESTOR", "texto": "Quero uma equipe para o Portal do Aluno."},
                          {"autor": "ASSISTENTE", "texto": "Para montar a equipe, ainda preciso saber o tamanho da equipe e as horas semanais por integrante."},
                          {"autor": "GESTOR", "texto": "Sao 2 pessoas, 20 horas por semana cada."}
                        ]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECOMENDACAO"))
                .andExpect(jsonPath("$.resposta", containsString("Portal do Aluno")))
                // a justificativa ja acompanha a recomendacao; nao ha o que pedir
                .andExpect(jsonPath("$.resposta",
                        org.hamcrest.Matchers.not(containsString("peca a justificativa"))))
                .andExpect(jsonPath("$.faltantes").isEmpty())
                .andExpect(jsonPath("$.recomendacao.origem").value("CHAT"))
                .andExpect(jsonPath("$.recomendacao.integrantesSelecionados").value(2));

        assertThat(recomendacaoRepository.findAll())
                .singleElement()
                .satisfies(recomendacao -> assertThat(recomendacao.getConsultaOriginal())
                        .contains("Portal do Aluno")
                        .contains("20 horas por semana")
                        .doesNotContain("Para montar a equipe"));   // so as falas do gestor
    }

    @Test
    @DisplayName("o modelo recebe a transcricao inteira, com os autores identificados")
    void deveEnviarTranscricaoCompleta() throws Exception {
        when(extrator.extrairParcial(anyString(), anyList())).thenReturn(completo("Portal Transcricao"));

        mvc.perform(conversa("""
                        {"mensagens": [
                          {"autor": "GESTOR", "texto": "Quero uma equipe."},
                          {"autor": "ASSISTENTE", "texto": "Quantas pessoas?"},
                          {"autor": "GESTOR", "texto": "Duas, para o Portal Transcricao."}
                        ]}"""))
                .andExpect(status().isOk());

        ArgumentCaptor<String> transcricao = ArgumentCaptor.forClass(String.class);
        verify(extrator).extrairParcial(transcricao.capture(), anyList());

        assertThat(transcricao.getValue())
                .contains("Gestor: Quero uma equipe.")
                .contains("Assistente: Quantas pessoas?")
                .contains("Gestor: Duas, para o Portal Transcricao.");
    }

    @Test
    @DisplayName("conversa terminada no assistente e recusada com 400")
    void deveExigirQueOUltimoTurnoSejaDoGestor() throws Exception {
        mvc.perform(conversa("""
                        {"mensagens": [
                          {"autor": "GESTOR", "texto": "Quero uma equipe."},
                          {"autor": "ASSISTENTE", "texto": "Quantas pessoas?"}
                        ]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].motivo", containsString("ultima mensagem")));

        verify(extrator, never()).extrairParcial(anyString(), anyList());
    }

    @Test
    @DisplayName("conversa sem mensagem alguma e recusada com 400")
    void deveExigirAoMenosUmaMensagem() throws Exception {
        mvc.perform(conversa("{\"mensagens\": []}"))
                .andExpect(status().isBadRequest());

        verify(extrator, never()).extrairParcial(anyString(), anyList());
    }

    @Test
    @DisplayName("integracao desligada devolve 503 tambem na conversa")
    void deveResponder503() throws Exception {
        when(extrator.extrairParcial(anyString(), anyList())).thenThrow(new ServicoExternoException(
                "gemini", "A integracao com o modelo de linguagem nao esta configurada.", null));

        mvc.perform(conversa("""
                        {"mensagens": [
                          {"autor": "GESTOR", "texto": "Quero uma equipe para o Portal."}
                        ]}"""))
                .andExpect(status().isServiceUnavailable());

        assertThat(projetoRepository.count()).isZero();
    }

    // ------------------------------------------------------------------

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder conversa(String corpo) {
        return post("/api/recomendacoes/conversa/turnos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }

    private static ParametrosExtraidos incompleto(String nome) {
        return new ParametrosExtraidos(nome, null, CriticidadeProjeto.MEDIA, null, null, null, null);
    }

    private static ParametrosExtraidos completo(String nome) {
        return new ParametrosExtraidos(nome, "Pedido do gestor.", CriticidadeProjeto.ALTA, 2, 20,
                List.of(new ParametrosExtraidos.PapelExtraido(PapelAtuacao.BACKEND, 1)),
                List.of(new ParametrosExtraidos.TecnologiaExtraida("Java", 5, true)));
    }

    private Desenvolvedor desenvolvedor(String nome, String email, Senioridade senioridade, PapelAtuacao papel) {
        return desenvolvedorRepository.saveAndFlush(new Desenvolvedor(nome, email, senioridade, papel,
                new BigDecimal("4.00"), 40));
    }
}
