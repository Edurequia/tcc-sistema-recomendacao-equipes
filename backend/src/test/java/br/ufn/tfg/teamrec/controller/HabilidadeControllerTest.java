package br.ufn.tfg.teamrec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre os endpoints do catalogo passando por todas as camadas reais:
 * controller, service, mapper, repositorio e banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de habilidades")
class HabilidadeControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lista o catalogo carregado na inicializacao")
    void deveListarCatalogoInicial() throws Exception {
        mvc.perform(get("/api/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(38))
                .andExpect(jsonPath("$[?(@.nome == 'Spring Boot')].chaveDataset").value("Spring Boot"))
                .andExpect(jsonPath("$[?(@.nome == 'AWS')].chaveDataset").value("Amazon Web Services (AWS)"))
                .andExpect(jsonPath("$[?(@.nome == 'AWS')].usadaNoModelo").value(true));
    }

    @Test
    @DisplayName("filtra por tipo de competencia")
    void deveFiltrarPorTipo() throws Exception {
        mvc.perform(get("/api/habilidades").param("tipo", "HARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30));

        mvc.perform(get("/api/habilidades").param("tipo", "SOFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].usadaNoModelo").value(false));
    }

    @Test
    @DisplayName("devolve 404 para identificador inexistente")
    void deveRetornar404() throws Exception {
        mvc.perform(get("/api/habilidades/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Habilidade de id 999999 nao encontrado(a)."));
    }

    @Test
    @DisplayName("devolve 400 quando o identificador nao e numerico")
    void deveRetornar400ParaIdInvalido() throws Exception {
        mvc.perform(get("/api/habilidades/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("id"));
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cadastra uma habilidade e devolve 201 com o cabecalho Location")
    void deveCadastrar() throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", "Quarkus",
                "tipo", "HARD",
                "categoria", "FRAMEWORK"));

        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Quarkus"))
                .andExpect(jsonPath("$.ativa").value(true))
                // Sem chave no dataset: nao influencia o escore de afinidade.
                .andExpect(jsonPath("$.usadaNoModelo").value(false))
                .andExpect(jsonPath("$.chaveDataset").doesNotExist());
    }

    @Test
    @DisplayName("recusa nome duplicado com 409, ignorando maiusculas")
    void deveRecusarNomeDuplicado() throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", "spring boot",
                "tipo", "HARD",
                "categoria", "FRAMEWORK"));

        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Ja existe uma habilidade chamada 'spring boot'."));
    }

    @Test
    @DisplayName("recusa corpo invalido com 400 e detalha o campo reprovado")
    void deveRecusarCorpoInvalido() throws Exception {
        String corpo = json.writeValueAsString(Map.of("nome", "", "tipo", "HARD"));

        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'categoria')]").exists());
    }

    @Test
    @DisplayName("atualiza uma habilidade existente")
    void deveAtualizar() throws Exception {
        Long id = criarHabilidade("Astro");

        String corpo = json.writeValueAsString(Map.of(
                "nome", "Astro Framework",
                "tipo", "HARD",
                "categoria", "FRAMEWORK",
                "chaveDataset", "Astro"));

        mvc.perform(put("/api/habilidades/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Astro Framework"))
                .andExpect(jsonPath("$.usadaNoModelo").value(true));
    }

    @Test
    @DisplayName("inativa logicamente e some das listagens de ativas")
    void deveInativarSemApagar() throws Exception {
        Long id = criarHabilidade("Svelte");

        mvc.perform(delete("/api/habilidades/" + id)).andExpect(status().isNoContent());

        // Continua existindo: a exclusao e logica, nao fisica.
        mvc.perform(get("/api/habilidades/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(get("/api/habilidades").param("apenasAtivas", "false"))
                .andExpect(jsonPath("$[?(@.nome == 'Svelte')]").exists());

        mvc.perform(get("/api/habilidades"))
                .andExpect(jsonPath("$[?(@.nome == 'Svelte')]").doesNotExist());
    }

    @Test
    @DisplayName("reativa uma habilidade inativada")
    void deveReativar() throws Exception {
        Long id = criarHabilidade("Solid.js");
        mvc.perform(delete("/api/habilidades/" + id)).andExpect(status().isNoContent());

        mvc.perform(post("/api/habilidades/" + id + "/reativacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    // ------------------------------------------------------------------

    private Long criarHabilidade(String nome) throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", nome, "tipo", "HARD", "categoria", "FRAMEWORK"));

        String resposta = mvc.perform(post("/api/habilidades")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(resposta).get("id").asLong();
    }
}
