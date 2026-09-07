package br.ufn.tfg.teamrec.domain;

import br.ufn.tfg.teamrec.config.JpaAuditingConfig;
import br.ufn.tfg.teamrec.domain.entity.Alocacao;
import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.domain.repository.AlocacaoRepository;
import br.ufn.tfg.teamrec.domain.repository.DesenvolvedorRepository;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import br.ufn.tfg.teamrec.domain.repository.HorasAlocadasProjection;
import br.ufn.tfg.teamrec.domain.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercita o modelo de dominio contra um banco real (H2), cobrindo o que a
 * compilacao nao garante: mapeamentos, restricoes de unicidade, cascata e o
 * comportamento das consultas de alocacao.
 */
@DataJpaTest
@ActiveProfiles("dev")
@Import(JpaAuditingConfig.class)
@DisplayName("Modelo de dominio")
class DominioPersistenciaTest {

    private static final List<StatusProjeto> STATUS_ATIVOS =
            List.of(StatusProjeto.PLANEJAMENTO, StatusProjeto.EM_ANDAMENTO);

    @Autowired
    private TestEntityManager em;
    @Autowired
    private DesenvolvedorRepository desenvolvedorRepository;
    @Autowired
    private HabilidadeRepository habilidadeRepository;
    @Autowired
    private ProjetoRepository projetoRepository;
    @Autowired
    private AlocacaoRepository alocacaoRepository;

    private Habilidade java;
    private Habilidade postgres;

    @BeforeEach
    void prepararCatalogo() {
        java = habilidadeRepository.save(
                new Habilidade("Java", TipoHabilidade.HARD, CategoriaHabilidade.LINGUAGEM, "Java"));
        postgres = habilidadeRepository.save(
                new Habilidade("PostgreSQL", TipoHabilidade.HARD, CategoriaHabilidade.BANCO_DE_DADOS, "PostgreSQL"));
    }

    // ------------------------------------------------------------------
    // Desenvolvedor
    // ------------------------------------------------------------------

    @Test
    @DisplayName("persiste desenvolvedor com habilidades em cascata e as recupera sem N+1")
    void devePersistirDesenvolvedorComHabilidades() {
        Desenvolvedor dev = novoDesenvolvedor("Ana Souza", "ana@ufn.edu.br",
                Senioridade.SENIOR, PapelAtuacao.BACKEND, "4.50", 30);
        dev.adicionarHabilidade(java, 5);
        dev.adicionarHabilidade(postgres, 4);

        Long id = desenvolvedorRepository.save(dev).getId();
        em.flush();
        em.clear();

        Optional<Desenvolvedor> recuperado = desenvolvedorRepository.buscarComHabilidades(id);

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getHabilidades()).hasSize(2);
        assertThat(recuperado.get().getHabilidades())
                .extracting(vinculo -> vinculo.getHabilidade().getNome())
                .containsExactlyInAnyOrder("Java", "PostgreSQL");
    }

    @Test
    @DisplayName("preenche os carimbos de auditoria automaticamente")
    void devePreencherAuditoria() {
        Desenvolvedor dev = desenvolvedorRepository.save(novoDesenvolvedor(
                "Bruno Lima", "bruno@ufn.edu.br", Senioridade.PLENO, PapelAtuacao.FRONTEND, "3.80", 40));

        assertThat(dev.getCriadoEm()).isNotNull();
        assertThat(dev.getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("rejeita e-mail duplicado")
    void deveRejeitarEmailDuplicado() {
        desenvolvedorRepository.save(novoDesenvolvedor(
                "Carla Reis", "carla@ufn.edu.br", Senioridade.JUNIOR, PapelAtuacao.QA, "3.00", 20));
        em.flush();

        // Com GenerationType.IDENTITY o Hibernate executa o INSERT ja no save(),
        // pois precisa do id gerado pelo banco: a violacao aparece aqui, nao no flush.
        assertThatThrownBy(() -> desenvolvedorRepository.save(novoDesenvolvedor(
                "Outra Carla", "carla@ufn.edu.br", Senioridade.SENIOR, PapelAtuacao.QA, "4.00", 20)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("mantem o escore comportamental sem perda de precisao")
    void devePreservarPrecisaoDoScore() {
        Long id = desenvolvedorRepository.save(novoDesenvolvedor(
                "Diego Alves", "diego@ufn.edu.br", Senioridade.PLENO, PapelAtuacao.MOBILE, "4.10", 25)).getId();
        em.flush();
        em.clear();

        assertThat(desenvolvedorRepository.findById(id).orElseThrow().getSoftSkillsScore())
                .isEqualByComparingTo(new BigDecimal("4.10"));
    }

    // ------------------------------------------------------------------
    // Projeto
    // ------------------------------------------------------------------

    @Test
    @DisplayName("persiste projeto com papeis e habilidades requeridas em cascata")
    void devePersistirProjetoComRequisitos() {
        Projeto projeto = new Projeto("Portal Academico", "Reescrita do portal",
                CriticidadeProjeto.ALTA, 4, 20);
        projeto.adicionarPapelRequerido(PapelAtuacao.BACKEND, 2);
        projeto.adicionarPapelRequerido(PapelAtuacao.FRONTEND, 2);
        projeto.adicionarHabilidadeRequerida(java, true, 5);
        projeto.adicionarHabilidadeRequerida(postgres, false, 3);

        Long id = projetoRepository.save(projeto).getId();
        em.flush();
        em.clear();

        // As duas consultas devolvem a MESMA instancia gerenciada, cada uma
        // inicializando uma colecao, sem produzir produto cartesiano.
        Projeto comPapeis = projetoRepository.buscarComPapeisRequeridos(id).orElseThrow();
        Projeto comHabilidades = projetoRepository.buscarComHabilidadesRequeridas(id).orElseThrow();

        assertThat(comPapeis).isSameAs(comHabilidades);
        assertThat(comPapeis.getPapeisRequeridos()).hasSize(2);
        assertThat(comPapeis.getHabilidadesRequeridas()).hasSize(2);
        assertThat(comPapeis.totalDeVagasPorPapel()).isEqualTo(4);
        assertThat(comPapeis.getStatus()).isEqualTo(StatusProjeto.PLANEJAMENTO);
    }

    // ------------------------------------------------------------------
    // Alocacao: base do cenario de conflito entre projetos simultaneos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("soma apenas as horas de alocacoes ativas em projetos que consomem alocacao")
    void deveSomarApenasHorasQueRealmenteComprometem() {
        Desenvolvedor dev = desenvolvedorRepository.save(novoDesenvolvedor(
                "Eva Martins", "eva@ufn.edu.br", Senioridade.SENIOR, PapelAtuacao.FULLSTACK, "4.20", 40));

        Projeto emAndamento = projetoSalvo("Projeto A", StatusProjeto.EM_ANDAMENTO);
        Projeto planejado = projetoSalvo("Projeto B", StatusProjeto.PLANEJAMENTO);
        Projeto concluido = projetoSalvo("Projeto C", StatusProjeto.CONCLUIDO);

        alocacaoRepository.save(new Alocacao(dev, emAndamento, PapelAtuacao.FULLSTACK, 12, LocalDate.now()));
        alocacaoRepository.save(new Alocacao(dev, planejado, PapelAtuacao.FULLSTACK, 8, LocalDate.now()));

        // Projeto ja concluido: nao deve entrar na soma.
        alocacaoRepository.save(new Alocacao(dev, concluido, PapelAtuacao.FULLSTACK, 20, LocalDate.now()));

        // Alocacao encerrada em projeto ativo: tambem nao deve entrar.
        Alocacao encerrada = new Alocacao(dev, emAndamento, PapelAtuacao.BACKEND, 15, LocalDate.now().minusMonths(3));
        encerrada.encerrar(LocalDate.now().minusDays(1));
        alocacaoRepository.save(encerrada);

        em.flush();
        em.clear();

        assertThat(alocacaoRepository.somarHorasComprometidas(dev.getId(), STATUS_ATIVOS)).isEqualTo(20);
    }

    @Test
    @DisplayName("retorna zero para desenvolvedor sem nenhuma alocacao")
    void deveRetornarZeroSemAlocacao() {
        Desenvolvedor dev = desenvolvedorRepository.save(novoDesenvolvedor(
                "Felipe Rocha", "felipe@ufn.edu.br", Senioridade.JUNIOR, PapelAtuacao.DEVOPS, "3.50", 40));
        em.flush();

        assertThat(alocacaoRepository.somarHorasComprometidas(dev.getId(), STATUS_ATIVOS)).isZero();
    }

    @Test
    @DisplayName("agrupa as horas comprometidas de varios desenvolvedores em uma unica consulta")
    void deveAgruparHorasPorDesenvolvedor() {
        Desenvolvedor primeiro = desenvolvedorRepository.save(novoDesenvolvedor(
                "Gabriela Dias", "gabriela@ufn.edu.br", Senioridade.PLENO, PapelAtuacao.BACKEND, "4.00", 40));
        Desenvolvedor segundo = desenvolvedorRepository.save(novoDesenvolvedor(
                "Heitor Nunes", "heitor@ufn.edu.br", Senioridade.SENIOR, PapelAtuacao.BACKEND, "4.30", 40));
        // Terceiro sem alocacao: nao deve aparecer no resultado.
        Desenvolvedor semAlocacao = desenvolvedorRepository.save(novoDesenvolvedor(
                "Ines Prado", "ines@ufn.edu.br", Senioridade.JUNIOR, PapelAtuacao.QA, "3.20", 40));

        Projeto projeto = projetoSalvo("Projeto D", StatusProjeto.EM_ANDAMENTO);
        alocacaoRepository.save(new Alocacao(primeiro, projeto, PapelAtuacao.BACKEND, 10, LocalDate.now()));
        alocacaoRepository.save(new Alocacao(segundo, projeto, PapelAtuacao.BACKEND, 25, LocalDate.now()));

        em.flush();
        em.clear();

        Map<Long, Integer> horasPorDev = alocacaoRepository
                .somarHorasComprometidasPorDesenvolvedor(STATUS_ATIVOS).stream()
                .collect(Collectors.toMap(HorasAlocadasProjection::getDesenvolvedorId,
                        HorasAlocadasProjection::getTotalHoras));

        assertThat(horasPorDev)
                .containsEntry(primeiro.getId(), 10)
                .containsEntry(segundo.getId(), 25)
                .doesNotContainKey(semAlocacao.getId());
    }

    // ------------------------------------------------------------------
    // Enums: contrato com o motor de predicao
    // ------------------------------------------------------------------

    @Test
    @DisplayName("os papeis mapeiam exatamente as colunas one-hot da matriz de treinamento")
    void papeisDevemBaterComODataset() {
        assertThat(List.of(PapelAtuacao.values()))
                .extracting(PapelAtuacao::getChaveDataset)
                .containsExactlyInAnyOrder(
                        "Developer, back-end",
                        "Developer, front-end",
                        "Developer, full-stack",
                        "Developer, mobile",
                        "Developer, QA or test",
                        "DevOps specialist");

        assertThat(PapelAtuacao.porChaveDataset("Developer, back-end")).contains(PapelAtuacao.BACKEND);
        assertThat(PapelAtuacao.porChaveDataset("inexistente")).isEmpty();
    }

    @Test
    @DisplayName("a senioridade expoe escala ordinal crescente")
    void senioridadeDeveSerOrdinalCrescente() {
        assertThat(List.of(Senioridade.values()))
                .extracting(Senioridade::getValorOrdinal)
                .containsExactly(1, 2, 3, 4);
    }

    // ------------------------------------------------------------------

    private Desenvolvedor novoDesenvolvedor(String nome, String email, Senioridade senioridade,
                                            PapelAtuacao papel, String score, int horas) {
        return new Desenvolvedor(nome, email, senioridade, papel, new BigDecimal(score), horas);
    }

    private Projeto projetoSalvo(String nome, StatusProjeto status) {
        Projeto projeto = new Projeto(nome, null, CriticidadeProjeto.MEDIA, 3, 20);
        projeto.setStatus(status);
        return projetoRepository.save(projeto);
    }
}
