package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.entity.Alocacao;
import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.repository.AlocacaoRepository;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import br.ufn.tfg.teamrec.integration.prediction.CandidatoVetor;
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
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre o fluxo de recomendacao com banco real e o motor substituido por
 * um duble: o que se verifica aqui e a orquestracao, nao o modelo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API de recomendacoes")
class RecomendacaoControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private HabilidadeRepository habilidadeRepository;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private ProjetoRepository projetoRepository;
    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @MockitoBean
    private MotorPredicaoClient motor;

    @Test
    @DisplayName("gera o ranking completo, ordenado pelo escore, e devolve 201")
    void deveGerarRankingOrdenado() throws Exception {
        Projeto projeto = projetoBackend();
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", Senioridade.JUNIOR, PapelAtuacao.FRONTEND, 40);
        Desenvolvedor carla = desenvolvedor("Carla Reis", Senioridade.PLENO, PapelAtuacao.FULLSTACK, 20);
        Desenvolvedor inativo = desenvolvedor("Diego Inativo", Senioridade.SENIOR, PapelAtuacao.BACKEND, 40);
        inativo.setAtivo(false);
        motorPontua(Map.of(ana.getId(), 91.015, bruno.getId(), 40.5, carla.getId(), 77.2));

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/recomendacoes/\\d+")))
                .andExpect(jsonPath("$.projetoId").value(projeto.getId()))
                .andExpect(jsonPath("$.projetoNome").value("Portal Academico"))
                .andExpect(jsonPath("$.origem").value("FORMULARIO"))
                .andExpect(jsonPath("$.totalCandidatos").value(3))
                .andExpect(jsonPath("$.ranking.length()").value(3))
                .andExpect(jsonPath("$.ranking[0].posicao").value(1))
                .andExpect(jsonPath("$.ranking[0].nome").value("Ana Souza"))
                .andExpect(jsonPath("$.ranking[0].scoreAfinidade").value(91.015))
                .andExpect(jsonPath("$.ranking[0].papel").value("BACKEND"))
                .andExpect(jsonPath("$.ranking[0].elegivel").value(true))
                .andExpect(jsonPath("$.ranking[1].nome").value("Carla Reis"))
                .andExpect(jsonPath("$.ranking[2].nome").value("Bruno Lima"))
                .andExpect(jsonPath("$.ranking[2].posicao").value(3));
    }

    @Test
    @DisplayName("seleciona a equipe, calcula as metricas e sinaliza as restricoes")
    void deveSelecionarEquipeECalcularMetricas() throws Exception {
        // Projeto: 4 integrantes, 2 vagas de backend, Java e Spring obrigatorios
        Projeto projeto = projetoBackend();
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        ana.adicionarHabilidade(habilidade("Java"), 5);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", Senioridade.JUNIOR, PapelAtuacao.FRONTEND, 40);
        Desenvolvedor carla = desenvolvedor("Carla Reis", Senioridade.PLENO, PapelAtuacao.FULLSTACK, 20);
        motorPontua(Map.of(ana.getId(), 90.0, bruno.getId(), 60.0, carla.getId(), 75.0));

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tamanhoEquipe").value(4))
                .andExpect(jsonPath("$.integrantesSelecionados").value(3))
                .andExpect(jsonPath("$.ranking[0].selecionado").value(true))
                .andExpect(jsonPath("$.ranking[1].selecionado").value(true))
                .andExpect(jsonPath("$.ranking[2].selecionado").value(true))
                // 1 de 2 vagas de backend
                .andExpect(jsonPath("$.metricas.coberturaPapeis").value(50.0))
                // so Java (5) de 12
                .andExpect(jsonPath("$.metricas.coberturaTecnologias").value(41.67))
                // senioridades 3, 2, 1
                .andExpect(jsonPath("$.metricas.desvioPadraoSenioridade").value(0.816))
                .andExpect(jsonPath("$.metricas.scoreMedioEquipe").value(75.0))
                .andExpect(jsonPath("$.observacaoRestricao").value(containsString("Equipe parcial: 3 de 4 integrantes")))
                .andExpect(jsonPath("$.observacaoRestricao").value(containsString("Backend (1)")))
                .andExpect(jsonPath("$.observacaoRestricao").value(containsString("Spring Boot")));
    }

    @Test
    @DisplayName("candidato sem as horas exigidas fica fora da equipe, mesmo com o maior escore")
    void inelegivelFicaForaDaEquipe() throws Exception {
        Projeto projeto = projetoBackend();
        Desenvolvedor lotada = desenvolvedor("Ana Lotada", Senioridade.SENIOR, PapelAtuacao.BACKEND, 10);
        Desenvolvedor livre = desenvolvedor("Bruno Livre", Senioridade.PLENO, PapelAtuacao.BACKEND, 20);
        motorPontua(Map.of(lotada.getId(), 95.0, livre.getId(), 80.0));

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ranking[0].nome").value("Ana Lotada"))
                .andExpect(jsonPath("$.ranking[0].elegivel").value(false))
                .andExpect(jsonPath("$.ranking[0].selecionado").value(false))
                .andExpect(jsonPath("$.ranking[1].selecionado").value(true))
                .andExpect(jsonPath("$.observacaoRestricao")
                        .value(containsString("1 candidato(s) sem as 20 horas semanais livres exigidas")));
    }

    @Test
    @DisplayName("confirmar a equipe aloca os selecionados e reduz a disponibilidade para outro projeto")
    void confirmacaoGeraConflitoEntreProjetos() throws Exception {
        Projeto projetoA = projetoBackend("Projeto A");
        Projeto projetoB = projetoBackend("Projeto B");
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", Senioridade.SENIOR, PapelAtuacao.BACKEND, 40);
        motorPontua(Map.of(ana.getId(), 95.0, bruno.getId(), 90.0));

        Integer idRecomendacaoA = JsonPath.read(mvc.perform(post("/api/projetos/{id}/recomendacoes", projetoA.getId()))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/recomendacoes/{id}/confirmacao", idRecomendacaoA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horasSemanais").value(20))
                .andExpect(jsonPath("$[0].projetoId").value(projetoA.getId()));

        // Ana: (30h - 20h no A) / 20h = 0,5 -> inelegivel em B ; Bruno: (40h - 20h) / 20h = 1
        mvc.perform(post("/api/projetos/{id}/recomendacoes", projetoB.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ranking[0].nome").value("Ana Souza"))
                .andExpect(jsonPath("$.ranking[0].coberturaHoras").value(0.5))
                .andExpect(jsonPath("$.ranking[0].selecionado").value(false))
                .andExpect(jsonPath("$.ranking[1].nome").value("Bruno Lima"))
                .andExpect(jsonPath("$.ranking[1].selecionado").value(true));
    }

    @Test
    @DisplayName("linha de base sorteia equipes entre os elegiveis e compara com o prototipo")
    void linhaBaseAleatoria() throws Exception {
        // 2 vagas de backend numa equipe de 4; so ha 2 backends elegiveis entre 5 elegiveis.
        Projeto projeto = projetoBackend();
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", Senioridade.PLENO, PapelAtuacao.BACKEND, 30);
        Desenvolvedor carla = desenvolvedor("Carla Reis", Senioridade.PLENO, PapelAtuacao.FRONTEND, 30);
        Desenvolvedor diego = desenvolvedor("Diego Alves", Senioridade.JUNIOR, PapelAtuacao.QA, 30);
        Desenvolvedor elisa = desenvolvedor("Elisa Prado", Senioridade.JUNIOR, PapelAtuacao.MOBILE, 30);
        Desenvolvedor lotado = desenvolvedor("Fabio Lotado", Senioridade.SENIOR, PapelAtuacao.BACKEND, 5);
        motorPontua(Map.of(ana.getId(), 95.0, bruno.getId(), 90.0, carla.getId(), 70.0,
                diego.getId(), 60.0, elisa.getId(), 50.0, lotado.getId(), 99.0));

        Integer id = JsonPath.read(mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/recomendacoes/{id}/linha-base", id).param("sorteios", "500").param("semente", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacaoId").value(id))
                .andExpect(jsonPath("$.sorteios").value(500))
                .andExpect(jsonPath("$.semente").value(42))
                .andExpect(jsonPath("$.candidatosElegiveis").value(5))
                .andExpect(jsonPath("$.tamanhoEquipeSorteada").value(4))
                .andExpect(jsonPath("$.prototipo.coberturaPapeis").value(100.0))
                // sorteando 4 de 5, a chance de levar os dois backends e 60%: media bem abaixo de 100
                .andExpect(jsonPath("$.linhaBase.media.coberturaPapeis").value(
                        org.hamcrest.Matchers.both(org.hamcrest.Matchers.greaterThan(70.0))
                                .and(org.hamcrest.Matchers.lessThan(90.0))))
                .andExpect(jsonPath("$.linhaBase.desvioPadrao.coberturaPapeis").exists())
                .andExpect(jsonPath("$.linhaBase.media.scoreMedioEquipe").exists());

        // Padroes: 100 sorteios e semente 42
        mvc.perform(get("/api/recomendacoes/{id}/linha-base", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sorteios").value(100))
                .andExpect(jsonPath("$.semente").value(42));
    }

    @Test
    @DisplayName("linha de base recusa parametros invalidos, recomendacao inexistente ou sem elegiveis")
    void linhaBaseInvalida() throws Exception {
        mvc.perform(get("/api/recomendacoes/{id}/linha-base", 999999)).andExpect(status().isNotFound());

        Projeto projeto = projetoBackend();
        Desenvolvedor lotada = desenvolvedor("Ana Lotada", Senioridade.SENIOR, PapelAtuacao.BACKEND, 5);
        motorPontua(Map.of(lotada.getId(), 95.0));
        Integer id = JsonPath.read(mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/recomendacoes/{id}/linha-base", id).param("sorteios", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/recomendacoes/{id}/linha-base", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("confirmacao de recomendacao inexistente ou sem equipe e recusada")
    void confirmacaoInvalida() throws Exception {
        mvc.perform(post("/api/recomendacoes/{id}/confirmacao", 999999))
                .andExpect(status().isNotFound());

        Projeto projeto = projetoBackend();
        Desenvolvedor lotada = desenvolvedor("Ana Lotada", Senioridade.SENIOR, PapelAtuacao.BACKEND, 5);
        motorPontua(Map.of(lotada.getId(), 95.0));
        Integer id = JsonPath.read(mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/recomendacoes/{id}/confirmacao", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("envia ao motor as features calculadas, descontando horas de outros projetos")
    void deveEnviarFeaturesCalculadas() throws Exception {
        Projeto projeto = projetoBackend();
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        ana.adicionarHabilidade(habilidade("Java"), 5);
        ana.adicionarHabilidade(habilidade("Spring Boot"), 4);

        Projeto outro = projetoRepository.save(new Projeto("Outro Projeto", null, CriticidadeProjeto.BAIXA, 2, 10));
        alocacaoRepository.save(new Alocacao(ana, outro, PapelAtuacao.BACKEND, 12, LocalDate.now()));
        // Alocacao no proprio projeto nao reduz a disponibilidade para ele.
        alocacaoRepository.save(new Alocacao(ana, projeto, PapelAtuacao.BACKEND, 20, LocalDate.now()));
        motorPontua(Map.of(ana.getId(), 88.0));

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ranking[0].aderenciaTecnica").value(0.75))
                .andExpect(jsonPath("$.ranking[0].coberturaHoras").value(0.9));

        ArgumentCaptor<PredicaoMotorRequest> enviado = ArgumentCaptor.forClass(PredicaoMotorRequest.class);
        verify(motor).prever(enviado.capture());
        assertThat(enviado.getValue().projeto().criticidade()).isEqualTo(3);
        assertThat(enviado.getValue().projeto().requisitoBackend()).isTrue();
        assertThat(enviado.getValue().projeto().requisitoFrontend()).isFalse();
        // Java (5) + Spring (4) de 12 ; (30h - 12h no outro projeto) / 20h exigidas
        assertThat(enviado.getValue().candidatos())
                .containsExactly(new CandidatoVetor(ana.getId(), 3, "Developer, back-end", 4.0, 0.75, 0.9));
    }

    @Test
    @DisplayName("a recomendacao gravada pode ser consultada depois")
    void deveConsultarRecomendacaoGravada() throws Exception {
        Projeto projeto = projetoBackend();
        Desenvolvedor ana = desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        Desenvolvedor bruno = desenvolvedor("Bruno Lima", Senioridade.JUNIOR, PapelAtuacao.FRONTEND, 40);
        motorPontua(Map.of(ana.getId(), 91.0, bruno.getId(), 40.0));

        String criada = mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criada, "$.id");

        mvc.perform(get("/api/recomendacoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ranking.length()").value(2))
                .andExpect(jsonPath("$.ranking[0].nome").value("Ana Souza"))
                .andExpect(jsonPath("$.ranking[1].scoreAfinidade").value(40.0));

        mvc.perform(get("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].origem").value("FORMULARIO"));
    }

    @Test
    @DisplayName("projeto inexistente responde 404")
    void projetoInexistente() throws Exception {
        mvc.perform(post("/api/projetos/{id}/recomendacoes", 999999))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/projetos/{id}/recomendacoes", 999999))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/recomendacoes/{id}", 999999))
                .andExpect(status().isNotFound());

        verify(motor, never()).prever(any());
    }

    @Test
    @DisplayName("projeto cancelado ou concluido nao recebe recomendacao (422)")
    void projetoEncerradoNaoRecebeRecomendacao() throws Exception {
        Projeto projeto = projetoBackend();
        projeto.setStatus(StatusProjeto.CANCELADO);
        desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Cancelado")));

        verify(motor, never()).prever(any());
    }

    @Test
    @DisplayName("sem desenvolvedores ativos nao ha o que recomendar (422)")
    void semCandidatos() throws Exception {
        Projeto projeto = projetoBackend();
        desenvolvedor("Ana Inativa", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30).setAtivo(false);

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isUnprocessableEntity());

        verify(motor, never()).prever(any());
    }

    @Test
    @DisplayName("motor indisponivel responde 503 e nada e gravado")
    void motorIndisponivel() throws Exception {
        Projeto projeto = projetoBackend();
        desenvolvedor("Ana Souza", Senioridade.SENIOR, PapelAtuacao.BACKEND, 30);
        when(motor.prever(any())).thenThrow(new ServicoExternoException(
                "motor-predicao", "O motor de predicao esta indisponivel.", new RuntimeException()));

        mvc.perform(post("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(status().isServiceUnavailable());

        mvc.perform(get("/api/projetos/{id}/recomendacoes", projeto.getId()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------

    private void motorPontua(Map<Long, Double> scores) {
        when(motor.prever(any())).thenAnswer(chamada -> {
            PredicaoMotorRequest requisicao = chamada.getArgument(0);
            return new PredicaoMotorResponse("LGBMRegressor", requisicao.candidatos().stream()
                    .map(c -> new Predicao(c.id(), scores.get(c.id())))
                    .toList());
        });
    }

    private Projeto projetoBackend() {
        return projetoBackend("Portal Academico");
    }

    private Projeto projetoBackend(String nome) {
        Projeto projeto = new Projeto(nome, null, CriticidadeProjeto.ALTA, 4, 20);
        projeto.adicionarPapelRequerido(PapelAtuacao.BACKEND, 2);
        projeto.adicionarHabilidadeRequerida(habilidade("Java"), true, 5);
        projeto.adicionarHabilidadeRequerida(habilidade("PostgreSQL"), false, 3);
        projeto.adicionarHabilidadeRequerida(habilidade("Spring Boot"), true, 4);
        return projetoRepository.save(projeto);
    }

    private Desenvolvedor desenvolvedor(String nome, Senioridade senioridade, PapelAtuacao papel, int horas) {
        String email = nome.toLowerCase().replace(' ', '.') + "@ufn.edu.br";
        return desenvolvedorRepository.save(
                new Desenvolvedor(nome, email, senioridade, papel, new BigDecimal("4.00"), horas));
    }

    private Habilidade habilidade(String nome) {
        return habilidadeRepository.findByNomeIgnoreCase(nome).orElseThrow();
    }
}
