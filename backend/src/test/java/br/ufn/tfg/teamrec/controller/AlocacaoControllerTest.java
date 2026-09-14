package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alocacao e o compromisso confirmado: e ela, e nao a recomendacao, que
 * consome horas e gera conflito entre projetos simultaneos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de alocacoes")
class AlocacaoControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private ProjetoRepository projetoRepository;

    @Test
    @DisplayName("aloca com o papel principal e as horas do projeto quando omitidos")
    void deveAlocarComPadroes() throws Exception {
        Projeto projeto = projeto("Portal Academico", 20);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 30);

        alocar(projeto, Map.of("desenvolvedorId", ana.getId()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/alocacoes/\\d+")))
                .andExpect(jsonPath("$.desenvolvedorId").value(ana.getId()))
                .andExpect(jsonPath("$.desenvolvedorNome").value("Ana Souza"))
                .andExpect(jsonPath("$.projetoNome").value("Portal Academico"))
                .andExpect(jsonPath("$.papel").value("BACKEND"))
                .andExpect(jsonPath("$.horasSemanais").value(20))
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    @DisplayName("aceita papel e horas explicitos")
    void deveAlocarComValoresInformados() throws Exception {
        Projeto projeto = projeto("Portal Academico", 20);
        Desenvolvedor carla = desenvolvedor("Carla Reis", PapelAtuacao.FULLSTACK, 30);

        alocar(projeto, Map.of("desenvolvedorId", carla.getId(), "papel", "FRONTEND", "horasSemanais", 12))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("FRONTEND"))
                .andExpect(jsonPath("$.horasSemanais").value(12));
    }

    @Test
    @DisplayName("recusa alocacao acima das horas livres, somando outros projetos (422)")
    void deveRecusarConflitoDeHoras() throws Exception {
        Projeto projetoA = projeto("Projeto A", 20);
        Projeto projetoB = projeto("Projeto B", 20);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 30);

        alocar(projetoA, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isCreated());

        alocar(projetoB, Map.of("desenvolvedorId", ana.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("10 horas semanais livres")));
    }

    @Test
    @DisplayName("recusa alocar duas vezes a mesma pessoa no mesmo projeto (409)")
    void deveRecusarDuplicada() throws Exception {
        Projeto projeto = projeto("Portal Academico", 10);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 40);

        alocar(projeto, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isCreated());
        alocar(projeto, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("recusa projeto encerrado e desenvolvedor inativo (422)")
    void deveRecusarProjetoEncerradoOuDevInativo() throws Exception {
        Projeto concluido = projeto("Projeto Concluido", 10);
        concluido.setStatus(StatusProjeto.CONCLUIDO);
        Projeto ativo = projeto("Projeto Ativo", 10);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 40);
        Desenvolvedor inativo = desenvolvedor("Bruno Inativo", PapelAtuacao.BACKEND, 40);
        inativo.setAtivo(false);

        alocar(concluido, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isUnprocessableEntity());
        alocar(ativo, Map.of("desenvolvedorId", inativo.getId())).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("projeto ou desenvolvedor inexistente responde 404; corpo sem desenvolvedor, 400")
    void recursosInexistentes() throws Exception {
        Projeto projeto = projeto("Portal Academico", 10);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 40);

        mvc.perform(post("/api/projetos/{id}/alocacoes", 999999).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("desenvolvedorId", ana.getId()))))
                .andExpect(status().isNotFound());
        alocar(projeto, Map.of("desenvolvedorId", 999999)).andExpect(status().isNotFound());
        alocar(projeto, new HashMap<>()).andExpect(status().isBadRequest());
        mvc.perform(get("/api/alocacoes/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("lista as alocacoes ativas; encerrar tira da lista e libera as horas")
    void encerrarLiberaHoras() throws Exception {
        Projeto projetoA = projeto("Projeto A", 20);
        Projeto projetoB = projeto("Projeto B", 20);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 30);

        String criada = alocar(projetoA, Map.of("desenvolvedorId", ana.getId()))
                .andReturn().getResponse().getContentAsString();
        Integer idAlocacao = JsonPath.read(criada, "$.id");

        mvc.perform(get("/api/projetos/{id}/alocacoes", projetoA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/alocacoes/{id}", idAlocacao)).andExpect(status().isNoContent());

        mvc.perform(get("/api/projetos/{id}/alocacoes", projetoA.getId()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/alocacoes/{id}", idAlocacao))
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.dataFim").exists());
        alocar(projetoB, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("com horas comprometidas, a disponibilidade nao pode cair abaixo delas (422)")
    void disponibilidadeRespeitaAlocacoes() throws Exception {
        Projeto projeto = projeto("Portal Academico", 20);
        Desenvolvedor ana = desenvolvedor("Ana Souza", PapelAtuacao.BACKEND, 30);
        alocar(projeto, Map.of("desenvolvedorId", ana.getId())).andExpect(status().isCreated());

        mvc.perform(patch("/api/desenvolvedores/{id}/disponibilidade", ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horasDisponiveis\": 10}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------------

    private ResultActions alocar(Projeto projeto, Map<String, Object> corpo) throws Exception {
        return mvc.perform(post("/api/projetos/{id}/alocacoes", projeto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    private Projeto projeto(String nome, int horasPorIntegrante) {
        return projetoRepository.save(new Projeto(nome, null, CriticidadeProjeto.MEDIA, 3, horasPorIntegrante));
    }

    private Desenvolvedor desenvolvedor(String nome, PapelAtuacao papel, int horas) {
        String email = nome.toLowerCase().replace(' ', '.') + "@ufn.edu.br";
        return desenvolvedorRepository.save(
                new Desenvolvedor(nome, email, Senioridade.PLENO, papel, new BigDecimal("4.00"), horas));
    }
}
