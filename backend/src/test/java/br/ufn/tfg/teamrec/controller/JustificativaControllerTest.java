package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.integration.llm.ContextoJustificativa;
import br.ufn.tfg.teamrec.integration.llm.GeradorJustificativa;
import br.ufn.tfg.teamrec.integration.llm.Justificativa;
import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoClient;
import br.ufn.tfg.teamrec.integration.prediction.Predicao;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorRequest;
import br.ufn.tfg.teamrec.integration.prediction.PredicaoMotorResponse;
import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Justificativa da equipe com o motor e o modelo de linguagem substituidos por
 * dubles: verifica a orquestracao, o reaproveitamento do texto ja gravado e a
 * traducao da indisponibilidade em 503.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de justificativa da recomendacao")
class JustificativaControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private ProjetoRepository projetoRepository;

    @MockitoBean
    private MotorPredicaoClient motor;
    @MockitoBean
    private GeradorJustificativa gerador;

    @Test
    @DisplayName("gera o texto, grava na recomendacao e devolve o modelo usado")
    void deveGerarEGravar() throws Exception {
        long recomendacaoId = recomendacao();
        when(gerador.gerar(any())).thenReturn(
                new Justificativa("A equipe cobre os dois papeis exigidos.", "gemini-2.5-flash"));

        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacaoId").value(recomendacaoId))
                .andExpect(jsonPath("$.justificativa").value("A equipe cobre os dois papeis exigidos."))
                .andExpect(jsonPath("$.modelo").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.geradaAgora").value(true));

        mvc.perform(get("/api/recomendacoes/{id}", recomendacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justificativaLlm")
                        .value("A equipe cobre os dois papeis exigidos."));
    }

    @Test
    @DisplayName("envia ao modelo a equipe, os preteridos e as metricas gravadas")
    void deveMontarOContexto() throws Exception {
        long recomendacaoId = recomendacao();
        when(gerador.gerar(any())).thenReturn(new Justificativa("ok", "gemini-2.5-flash"));

        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isOk());

        ArgumentCaptor<ContextoJustificativa> capturado =
                ArgumentCaptor.forClass(ContextoJustificativa.class);
        verify(gerador).gerar(capturado.capture());
        ContextoJustificativa contexto = capturado.getValue();

        assertThat(contexto.projeto()).isEqualTo("Portal Academico");
        assertThat(contexto.criticidade()).contains("ALTA");
        assertThat(contexto.papeisExigidos()).isNotEmpty();
        assertThat(contexto.equipe()).extracting(ContextoJustificativa.Integrante::nome)
                .containsExactly("Ana Souza");
        assertThat(contexto.preteridos()).extracting(ContextoJustificativa.Integrante::nome)
                .contains("Bruno Lima");
        assertThat(contexto.metricas().coberturaPapeis()).isNotNull();
    }

    @Test
    @DisplayName("reaproveita a justificativa ja gravada em vez de consultar o modelo de novo")
    void deveReaproveitarTextoGravado() throws Exception {
        long recomendacaoId = recomendacao();
        when(gerador.gerar(any())).thenReturn(new Justificativa("Primeira versao.", "gemini-2.5-flash"));

        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isOk());
        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justificativa").value("Primeira versao."))
                .andExpect(jsonPath("$.geradaAgora").value(false));

        verify(gerador, times(1)).gerar(any());
    }

    @Test
    @DisplayName("regerar=true consulta o modelo novamente e substitui o texto")
    void deveRegerarQuandoPedido() throws Exception {
        long recomendacaoId = recomendacao();
        when(gerador.gerar(any()))
                .thenReturn(new Justificativa("Primeira versao.", "gemini-2.5-flash"))
                .thenReturn(new Justificativa("Segunda versao.", "gemini-2.5-flash"));

        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isOk());
        mvc.perform(post("/api/recomendacoes/{id}/justificativa?regerar=true", recomendacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justificativa").value("Segunda versao."))
                .andExpect(jsonPath("$.geradaAgora").value(true));

        verify(gerador, times(2)).gerar(any());
    }

    @Test
    @DisplayName("recomendacao inexistente devolve 404 sem consultar o modelo")
    void deveResponder404() throws Exception {
        mvc.perform(post("/api/recomendacoes/{id}/justificativa", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Nao encontrado"));

        verify(gerador, org.mockito.Mockito.never()).gerar(any());
    }

    @Test
    @DisplayName("modelo indisponivel vira 503 e nada e gravado")
    void deveResponder503() throws Exception {
        long recomendacaoId = recomendacao();
        when(gerador.gerar(any())).thenThrow(new ServicoExternoException(
                "gemini", "A integracao com o modelo de linguagem nao esta configurada.", null));

        mvc.perform(post("/api/recomendacoes/{id}/justificativa", recomendacaoId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem", containsString("nao esta configurada")));

        mvc.perform(get("/api/recomendacoes/{id}", recomendacaoId))
                .andExpect(jsonPath("$.justificativaLlm").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Cenario minimo: um projeto, dois candidatos, um selecionado.
    // ------------------------------------------------------------------

    private long recomendacao() throws Exception {
        Projeto projeto = new Projeto("Portal Academico", "Projeto de teste",
                CriticidadeProjeto.ALTA, 1, 20);
        projeto.adicionarPapelRequerido(PapelAtuacao.BACKEND, 1);
        projetoRepository.saveAndFlush(projeto);

        Desenvolvedor ana = desenvolvedor("Ana Souza", "ana.justificativa@ufn.edu.br",
                Senioridade.SENIOR, PapelAtuacao.BACKEND);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", "bruno.justificativa@ufn.edu.br",
                Senioridade.JUNIOR, PapelAtuacao.BACKEND);

        when(motor.prever(any(PredicaoMotorRequest.class))).thenReturn(new PredicaoMotorResponse(
                "LGBMRegressor",
                List.of(new Predicao(ana.getId(), 90.0), new Predicao(bruno.getId(), 40.0))));

        String corpo = mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private Desenvolvedor desenvolvedor(String nome, String email, Senioridade senioridade, PapelAtuacao papel) {
        return desenvolvedorRepository.saveAndFlush(new Desenvolvedor(nome, email, senioridade, papel,
                new BigDecimal("4.00"), 40));
    }
}
