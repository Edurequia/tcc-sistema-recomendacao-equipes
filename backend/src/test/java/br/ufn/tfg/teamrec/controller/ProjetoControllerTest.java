package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre os endpoints de projetos atravessando todas as camadas reais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de projetos")
class ProjetoControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private HabilidadeRepository habilidadeRepository;

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cadastra um projeto com papeis e tecnologias exigidos")
    void deveCadastrarComRequisitos() throws Exception {
        Map<String, Object> corpo = projetoBase("Portal Academico", 4);
        corpo.put("papeisRequeridos", List.of(
                Map.of("papel", "BACKEND", "quantidade", 2),
                Map.of("papel", "FRONTEND", "quantidade", 2)));
        corpo.put("habilidadesRequeridas", List.of(
                Map.of("habilidadeId", idDaHabilidade("Java"), "obrigatoria", true, "peso", 5),
                Map.of("habilidadeId", idDaHabilidade("React"), "obrigatoria", false, "peso", 3)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Portal Academico"))
                .andExpect(jsonPath("$.status").value("PLANEJAMENTO"))
                .andExpect(jsonPath("$.criticidadeNivel").value(3))
                .andExpect(jsonPath("$.criticidadeDescricao").exists())
                .andExpect(jsonPath("$.papeisRequeridos.length()").value(2))
                .andExpect(jsonPath("$.habilidadesRequeridas.length()").value(2))
                .andExpect(jsonPath("$.vagasDeclaradas").value(4))
                .andExpect(jsonPath("$.vagasSemPapelFixo").value(0))
                .andExpect(jsonPath("$.habilidadesRequeridas[?(@.nome == 'Java')].peso").value(5))
                .andExpect(jsonPath("$.habilidadesRequeridas[?(@.nome == 'Java')].usadaNoModelo").value(true));
    }

    @Test
    @DisplayName("aplica os padroes de obrigatoriedade e peso quando omitidos")
    void deveAplicarPadroesDaTecnologia() throws Exception {
        Map<String, Object> corpo = projetoBase("Projeto Padroes", 3);
        corpo.put("habilidadesRequeridas", List.of(
                Map.of("habilidadeId", idDaHabilidade("Docker"))));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.habilidadesRequeridas[0].obrigatoria").value(true))
                .andExpect(jsonPath("$.habilidadesRequeridas[0].peso").value(3));
    }

    @Test
    @DisplayName("aceita menos vagas que o tamanho da equipe, deixando posicoes livres")
    void devePermitirVagasSemPapelFixo() throws Exception {
        Map<String, Object> corpo = projetoBase("Projeto Flexivel", 5);
        corpo.put("papeisRequeridos", List.of(Map.of("papel", "BACKEND", "quantidade", 2)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vagasDeclaradas").value(2))
                .andExpect(jsonPath("$.vagasSemPapelFixo").value(3));
    }

    @Test
    @DisplayName("recusa vagas alem do tamanho da equipe com 422")
    void deveRecusarVagasExcedentes() throws Exception {
        Map<String, Object> corpo = projetoBase("Projeto Inconsistente", 3);
        corpo.put("papeisRequeridos", List.of(
                Map.of("papel", "BACKEND", "quantidade", 2),
                Map.of("papel", "FRONTEND", "quantidade", 2)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "Os papeis exigidos somam 4 vagas, mas a equipe tem 3 integrantes. "
                                + "Reduza as vagas ou aumente o tamanho da equipe."));
    }

    @Test
    @DisplayName("recusa o mesmo papel declarado duas vezes com 422")
    void deveRecusarPapelDuplicado() throws Exception {
        Map<String, Object> corpo = projetoBase("Projeto Duplicado", 6);
        corpo.put("papeisRequeridos", List.of(
                Map.of("papel", "BACKEND", "quantidade", 2),
                Map.of("papel", "BACKEND", "quantidade", 1)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "Papel informado mais de uma vez: [BACKEND]. Declare cada papel uma "
                                + "unica vez, somando as vagas."));
    }

    @Test
    @DisplayName("recusa data de termino anterior a de inicio com 422")
    void deveRecusarPeriodoInvertido() throws Exception {
        Map<String, Object> corpo = projetoBase("Projeto Invertido", 3);
        corpo.put("dataInicioPrevista", "2026-10-01");
        corpo.put("dataFimPrevista", "2026-09-01");

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "A data de termino prevista nao pode ser anterior a data de inicio."));
    }

    @Test
    @DisplayName("recusa nome duplicado com 409")
    void deveRecusarNomeDuplicado() throws Exception {
        criar("Projeto Unico", 3);

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(projetoBase("projeto unico", 4))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Ja existe um projeto chamado 'projeto unico'."));
    }

    @Test
    @DisplayName("recusa campos obrigatorios ausentes com 400")
    void deveValidarCamposObrigatorios() throws Exception {
        String corpo = json.writeValueAsString(Map.of("nome", "ab"));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'criticidade')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'tamanhoEquipe')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'horasSemanaisPorIntegrante')]").exists());
    }

    @Test
    @DisplayName("recusa tecnologia inexistente com 404 e inativa com 422")
    void deveValidarTecnologias() throws Exception {
        Map<String, Object> inexistente = projetoBase("Projeto Fantasma", 3);
        inexistente.put("habilidadesRequeridas", List.of(Map.of("habilidadeId", 999999)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(inexistente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Habilidade(s) inexistente(s): [999999]."));

        Long redis = idDaHabilidade("Redis");
        mvc.perform(delete("/api/habilidades/" + redis)).andExpect(status().isNoContent());

        Map<String, Object> inativa = projetoBase("Projeto Inativo", 3);
        inativa.put("habilidadesRequeridas", List.of(Map.of("habilidadeId", redis)));

        mvc.perform(post("/api/projetos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(inativa)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "Habilidade(s) inativa(s) nao podem ser exigidas: [Redis]."));
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lista paginado e combina os filtros")
    void deveListarComFiltros() throws Exception {
        criarCom("Sistema Alfa", 3, "ALTA");
        criarCom("Sistema Beta", 3, "BAIXA");
        criarCom("Outro Gama", 3, "ALTA");

        mvc.perform(get("/api/projetos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                // A listagem e enxuta: sem requisitos, para nao consultar por linha.
                .andExpect(jsonPath("$.conteudo[0].papeisRequeridos").doesNotExist());

        mvc.perform(get("/api/projetos").param("criticidade", "ALTA"))
                .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(get("/api/projetos").param("nome", "sistema").param("criticidade", "ALTA"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Sistema Alfa"));
    }

    @Test
    @DisplayName("devolve 404 para projeto inexistente")
    void deveRetornar404() throws Exception {
        mvc.perform(get("/api/projetos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Projeto de id 999999 nao encontrado(a)."));
    }

    // ------------------------------------------------------------------
    // Atualizacao e ciclo de vida
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a atualizacao substitui papeis e tecnologias por inteiro")
    void deveSubstituirRequisitos() throws Exception {
        Map<String, Object> inicial = projetoBase("Projeto Mutavel", 4);
        inicial.put("papeisRequeridos", List.of(Map.of("papel", "BACKEND", "quantidade", 2)));
        inicial.put("habilidadesRequeridas", List.of(Map.of("habilidadeId", idDaHabilidade("Java"))));
        Long id = criarERetornarId(inicial);

        Map<String, Object> novo = projetoBase("Projeto Mutavel", 4);
        novo.put("papeisRequeridos", List.of(Map.of("papel", "MOBILE", "quantidade", 1)));
        novo.put("habilidadesRequeridas", List.of(Map.of("habilidadeId", idDaHabilidade("Flutter"))));

        mvc.perform(put("/api/projetos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeisRequeridos.length()").value(1))
                .andExpect(jsonPath("$.papeisRequeridos[0].papel").value("MOBILE"))
                .andExpect(jsonPath("$.habilidadesRequeridas.length()").value(1))
                .andExpect(jsonPath("$.habilidadesRequeridas[0].nome").value("Flutter"))
                .andExpect(jsonPath("$.vagasSemPapelFixo").value(3));
    }

    @Test
    @DisplayName("altera o status isoladamente")
    void deveAlterarStatus() throws Exception {
        Long id = criar("Projeto Ativo", 3);

        mvc.perform(patch("/api/projetos/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("status", "EM_ANDAMENTO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.statusDescricao").value("Em andamento"));
    }

    @Test
    @DisplayName("o cancelamento e uma exclusao logica: o projeto continua existindo")
    void deveCancelarSemApagar() throws Exception {
        Long id = criar("Projeto Cancelado", 3);

        mvc.perform(delete("/api/projetos/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/projetos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        mvc.perform(get("/api/projetos").param("status", "PLANEJAMENTO"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Long idDaHabilidade(String nome) {
        return habilidadeRepository.findByNomeIgnoreCase(nome).orElseThrow().getId();
    }

    private Map<String, Object> projetoBase(String nome, int tamanhoEquipe) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("descricao", "Projeto usado no cenario de teste.");
        corpo.put("criticidade", "ALTA");
        corpo.put("tamanhoEquipe", tamanhoEquipe);
        corpo.put("horasSemanaisPorIntegrante", 20);
        return corpo;
    }

    private Long criar(String nome, int tamanhoEquipe) throws Exception {
        return criarERetornarId(projetoBase(nome, tamanhoEquipe));
    }

    private void criarCom(String nome, int tamanhoEquipe, String criticidade) throws Exception {
        Map<String, Object> corpo = projetoBase(nome, tamanhoEquipe);
        corpo.put("criticidade", criticidade);
        criarERetornarId(corpo);
    }

    private Long criarERetornarId(Map<String, Object> corpo) throws Exception {
        String resposta = mvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(resposta).get("id").asLong();
    }
}
