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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre os endpoints de desenvolvedores atravessando todas as camadas reais.
 * As habilidades usadas vem da carga inicial do catalogo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de desenvolvedores")
class DesenvolvedorControllerTest {

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
    @DisplayName("cadastra um desenvolvedor com competencias e devolve 201")
    void deveCadastrarComHabilidades() throws Exception {
        Long java = idDaHabilidade("Java");
        Long spring = idDaHabilidade("Spring Boot");

        String corpo = json.writeValueAsString(Map.of(
                "nome", "Ana Souza",
                "email", "ana.souza@ufn.edu.br",
                "senioridade", "SENIOR",
                "papelPrincipal", "BACKEND",
                "softSkillsScore", "4.20",
                "horasDisponiveis", 30,
                "habilidades", List.of(
                        Map.of("habilidadeId", java, "proficiencia", 5),
                        Map.of("habilidadeId", spring, "proficiencia", 4))));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.senioridadeDescricao").value("Senior"))
                .andExpect(jsonPath("$.papelPrincipalDescricao").value("Backend"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.habilidades.length()").value(2))
                .andExpect(jsonPath("$.habilidades[?(@.nome == 'Java')].proficiencia").value(5));
    }

    @Test
    @DisplayName("normaliza o e-mail para minusculas")
    void deveNormalizarEmail() throws Exception {
        criar("Bruno Lima", "  BRUNO.LIMA@UFN.EDU.BR ");

        mvc.perform(get("/api/desenvolvedores").param("nome", "Bruno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].email").value("bruno.lima@ufn.edu.br"));
    }

    @Test
    @DisplayName("recusa e-mail duplicado com 409")
    void deveRecusarEmailDuplicado() throws Exception {
        criar("Carla Reis", "carla@ufn.edu.br");

        String corpo = perfil("Outra Carla", "CARLA@ufn.edu.br", List.of());

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Ja existe um desenvolvedor com o e-mail 'carla@ufn.edu.br'."));
    }

    @Test
    @DisplayName("recusa escore fora da escala do modelo e horas negativas")
    void deveValidarEscalas() throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", "Diego Alves",
                "email", "diego@ufn.edu.br",
                "senioridade", "PLENO",
                "papelPrincipal", "MOBILE",
                "softSkillsScore", "7.50",
                "horasDisponiveis", -5));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'softSkillsScore')].motivo")
                        .value("O escore maximo e 5."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'horasDisponiveis')].motivo")
                        .value("As horas disponiveis nao podem ser negativas."));
    }

    @Test
    @DisplayName("recusa e-mail malformado")
    void deveRecusarEmailInvalido() throws Exception {
        String corpo = perfil("Eva Martins", "eva-sem-arroba", List.of());

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'email')]").exists());
    }

    @Test
    @DisplayName("recusa vinculo com habilidade inexistente indicando qual e")
    void deveRecusarHabilidadeInexistente() throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", "Felipe Rocha",
                "email", "felipe@ufn.edu.br",
                "senioridade", "JUNIOR",
                "papelPrincipal", "DEVOPS",
                "softSkillsScore", "3.50",
                "horasDisponiveis", 40,
                "habilidades", List.of(Map.of("habilidadeId", 999999, "proficiencia", 3))));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Habilidade(s) inexistente(s): [999999]."));
    }

    @Test
    @DisplayName("recusa vinculo com habilidade inativada, com 422")
    void deveRecusarHabilidadeInativa() throws Exception {
        Long redis = idDaHabilidade("Redis");
        mvc.perform(delete("/api/habilidades/" + redis)).andExpect(status().isNoContent());

        String corpo = json.writeValueAsString(Map.of(
                "nome", "Gabriela Dias",
                "email", "gabriela@ufn.edu.br",
                "senioridade", "PLENO",
                "papelPrincipal", "BACKEND",
                "softSkillsScore", "4.00",
                "horasDisponiveis", 40,
                "habilidades", List.of(Map.of("habilidadeId", redis, "proficiencia", 3))));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "Habilidade(s) inativa(s) nao podem ser vinculadas: [Redis]."));
    }

    @Test
    @DisplayName("recusa proficiencia fora da faixa de 1 a 5")
    void deveValidarProficiencia() throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", "Heitor Nunes",
                "email", "heitor@ufn.edu.br",
                "senioridade", "SENIOR",
                "papelPrincipal", "BACKEND",
                "softSkillsScore", "4.30",
                "horasDisponiveis", 40,
                "habilidades", List.of(Map.of("habilidadeId", idDaHabilidade("Java"), "proficiencia", 9))));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].motivo").value("A proficiencia maxima e 5."));
    }

    @Test
    @DisplayName("recusa a mesma habilidade informada duas vezes com 422")
    void deveRecusarHabilidadeDuplicada() throws Exception {
        Long java = idDaHabilidade("Java");

        String corpo = json.writeValueAsString(Map.of(
                "nome", "Igor Salles",
                "email", "igor@ufn.edu.br",
                "senioridade", "PLENO",
                "papelPrincipal", "BACKEND",
                "softSkillsScore", "3.90",
                "horasDisponiveis", 40,
                "habilidades", List.of(
                        Map.of("habilidadeId", java, "proficiencia", 5),
                        Map.of("habilidadeId", java, "proficiencia", 2))));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "A mesma habilidade foi informada mais de uma vez. "
                                + "Declare cada uma apenas uma vez."));
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("devolve a listagem no envelope de paginacao proprio da API")
    void deveListarPaginado() throws Exception {
        criar("Ines Prado", "ines@ufn.edu.br");
        criar("Joao Bastos", "joao@ufn.edu.br");

        mvc.perform(get("/api/desenvolvedores").param("size", "1").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(false))
                // A listagem e enxuta: sem habilidades, para nao gerar consulta por linha.
                .andExpect(jsonPath("$.conteudo[0].habilidades").doesNotExist());
    }

    @Test
    @DisplayName("combina os filtros de nome, senioridade e papel")
    void deveCombinarFiltros() throws Exception {
        criarCompleto("Karina Melo", "karina@ufn.edu.br", "SENIOR", "FRONTEND");
        criarCompleto("Karla Nunes", "karla@ufn.edu.br", "JUNIOR", "FRONTEND");
        criarCompleto("Lucas Vieira", "lucas@ufn.edu.br", "SENIOR", "BACKEND");

        mvc.perform(get("/api/desenvolvedores").param("papel", "FRONTEND"))
                .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(get("/api/desenvolvedores")
                        .param("papel", "FRONTEND").param("senioridade", "SENIOR"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Karina Melo"));

        // Busca parcial, sem diferenciar maiusculas.
        mvc.perform(get("/api/desenvolvedores").param("nome", "kar"))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    // ------------------------------------------------------------------
    // Atualizacao
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a atualizacao substitui a lista de competencias por inteiro")
    void deveSubstituirHabilidades() throws Exception {
        Long id = criarCom("Maria Luz", "maria@ufn.edu.br",
                List.of(Map.of("habilidadeId", idDaHabilidade("Java"), "proficiencia", 5)));

        String corpo = perfil("Maria Luz", "maria@ufn.edu.br",
                List.of(Map.of("habilidadeId", idDaHabilidade("Python"), "proficiencia", 3)));

        mvc.perform(put("/api/desenvolvedores/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilidades.length()").value(1))
                .andExpect(jsonPath("$.habilidades[0].nome").value("Python"));
    }

    @Test
    @DisplayName("ajusta apenas a disponibilidade, preservando as competencias")
    void deveAjustarDisponibilidade() throws Exception {
        Long id = criarCom("Nuno Barros", "nuno@ufn.edu.br",
                List.of(Map.of("habilidadeId", idDaHabilidade("Docker"), "proficiencia", 4)));

        String corpo = json.writeValueAsString(Map.of("horasDisponiveis", 12));

        mvc.perform(patch("/api/desenvolvedores/" + id + "/disponibilidade")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasDisponiveis").value(12))
                .andExpect(jsonPath("$.habilidades.length()").value(1));
    }

    @Test
    @DisplayName("inativa e reativa preservando o registro")
    void deveInativarEReativar() throws Exception {
        Long id = criar("Olivia Terra", "olivia@ufn.edu.br");

        mvc.perform(delete("/api/desenvolvedores/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/desenvolvedores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(get("/api/desenvolvedores").param("ativo", "true"))
                .andExpect(jsonPath("$.totalElementos").value(0));

        mvc.perform(post("/api/desenvolvedores/" + id + "/reativacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("devolve 404 ao atualizar identificador inexistente")
    void deveRetornar404NaAtualizacao() throws Exception {
        mvc.perform(put("/api/desenvolvedores/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("Fantasma", "fantasma@ufn.edu.br", List.of())))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Long idDaHabilidade(String nome) {
        return habilidadeRepository.findByNomeIgnoreCase(nome).orElseThrow().getId();
    }

    private String perfil(String nome, String email, List<Map<String, Object>> habilidades) throws Exception {
        return json.writeValueAsString(Map.of(
                "nome", nome,
                "email", email,
                "senioridade", "PLENO",
                "papelPrincipal", "FULLSTACK",
                "softSkillsScore", "3.80",
                "horasDisponiveis", 40,
                "habilidades", habilidades));
    }

    private Long criar(String nome, String email) throws Exception {
        return criarCom(nome, email, List.of());
    }

    private Long criarCom(String nome, String email, List<Map<String, Object>> habilidades) throws Exception {
        String resposta = mvc.perform(post("/api/desenvolvedores")
                        .contentType(MediaType.APPLICATION_JSON).content(perfil(nome, email, habilidades)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(resposta).get("id").asLong();
    }

    private void criarCompleto(String nome, String email, String senioridade, String papel) throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "nome", nome,
                "email", email,
                "senioridade", senioridade,
                "papelPrincipal", papel,
                "softSkillsScore", "4.00",
                "horasDisponiveis", 40));

        mvc.perform(post("/api/desenvolvedores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
    }
}
