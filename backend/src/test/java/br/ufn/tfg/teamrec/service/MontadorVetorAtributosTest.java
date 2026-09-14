package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.integration.prediction.CandidatoVetor;
import br.ufn.tfg.teamrec.integration.prediction.ProjetoContexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As features calculadas aqui precisam reproduzir exatamente as do script de
 * treinamento; qualquer divergencia faz o modelo pontuar algo diferente do
 * que aprendeu, sem erro algum.
 */
@DisplayName("Montagem do vetor de atributos enviado ao motor")
class MontadorVetorAtributosTest {

    private final MontadorVetorAtributos montador = new MontadorVetorAtributos();

    private final Habilidade java = habilidade(1L, "Java", TipoHabilidade.HARD);
    private final Habilidade postgres = habilidade(2L, "PostgreSQL", TipoHabilidade.HARD);
    private final Habilidade spring = habilidade(3L, "Spring Boot", TipoHabilidade.HARD);
    private final Habilidade comunicacao = habilidade(4L, "Comunicacao", TipoHabilidade.SOFT);

    @Nested
    @DisplayName("contexto do projeto")
    class Contexto {

        @Test
        @DisplayName("usa o nivel numerico da criticidade")
        void deveUsarNivelDaCriticidade() {
            Projeto projeto = projeto(CriticidadeProjeto.ALTA, 20);
            projeto.adicionarPapelRequerido(PapelAtuacao.BACKEND, 1);

            assertThat(montador.contextoDoProjeto(projeto).criticidade()).isEqualTo(3);
        }

        @Test
        @DisplayName("backend exigido liga apenas o requisito de backend")
        void backendLigaSomenteBackend() {
            Projeto projeto = projeto(CriticidadeProjeto.MEDIA, 20);
            projeto.adicionarPapelRequerido(PapelAtuacao.BACKEND, 2);
            projeto.adicionarPapelRequerido(PapelAtuacao.QA, 1);

            assertThat(montador.contextoDoProjeto(projeto))
                    .isEqualTo(new ProjetoContexto(2, true, false));
        }

        @Test
        @DisplayName("frontend exigido liga apenas o requisito de frontend")
        void frontendLigaSomenteFrontend() {
            Projeto projeto = projeto(CriticidadeProjeto.BAIXA, 20);
            projeto.adicionarPapelRequerido(PapelAtuacao.FRONTEND, 1);

            assertThat(montador.contextoDoProjeto(projeto))
                    .isEqualTo(new ProjetoContexto(1, false, true));
        }

        @Test
        @DisplayName("full-stack exigido liga os dois requisitos")
        void fullstackLigaAmbos() {
            Projeto projeto = projeto(CriticidadeProjeto.BAIXA, 20);
            projeto.adicionarPapelRequerido(PapelAtuacao.FULLSTACK, 1);

            assertThat(montador.contextoDoProjeto(projeto))
                    .isEqualTo(new ProjetoContexto(1, true, true));
        }

        @Test
        @DisplayName("sem papel de backend ou frontend, liga os dois (o treino nunca teve projeto sem demanda)")
        void semDemandaDeclaradaLigaAmbos() {
            Projeto semPapeis = projeto(CriticidadeProjeto.MEDIA, 20);
            Projeto soQa = projeto(CriticidadeProjeto.MEDIA, 20);
            soQa.adicionarPapelRequerido(PapelAtuacao.QA, 1);

            assertThat(montador.contextoDoProjeto(semPapeis)).isEqualTo(new ProjetoContexto(2, true, true));
            assertThat(montador.contextoDoProjeto(soQa)).isEqualTo(new ProjetoContexto(2, true, true));
        }
    }

    @Nested
    @DisplayName("vetor do candidato")
    class Candidato {

        @Test
        @DisplayName("converte perfil, aderencia ponderada e cobertura de horas")
        void deveMontarVetorCompleto() {
            Projeto projeto = projetoComStack();
            Desenvolvedor ana = desenvolvedor(10L, Senioridade.SENIOR, PapelAtuacao.BACKEND, "4.00", 18);
            ana.adicionarHabilidade(java, 5);
            ana.adicionarHabilidade(spring, 4);

            CandidatoVetor vetor = montador.vetorDoCandidato(ana, projeto, 0);

            // Java (5) + Spring Boot (4) de um total de 12 = 0,75 ; 18h de 20h = 0,9
            assertThat(vetor).isEqualTo(new CandidatoVetor(10L, 3, "Developer, back-end", 4.0, 0.75, 0.9));
        }

        @Test
        @DisplayName("habilidade comportamental exigida nao entra na aderencia tecnica")
        void deveIgnorarSoftSkillNaAderencia() {
            Projeto projeto = projetoComStack();
            projeto.adicionarHabilidadeRequerida(comunicacao, true, 5);
            Desenvolvedor ana = desenvolvedor(10L, Senioridade.SENIOR, PapelAtuacao.BACKEND, "4.00", 20);
            ana.adicionarHabilidade(java, 5);
            ana.adicionarHabilidade(spring, 4);

            assertThat(montador.vetorDoCandidato(ana, projeto, 0).aderenciaTecnica()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("projeto sem tecnologia exigida tem aderencia plena")
        void semStackExigidaAderenciaPlena() {
            Projeto projeto = projeto(CriticidadeProjeto.MEDIA, 20);
            projeto.adicionarHabilidadeRequerida(comunicacao, true, 5);
            Desenvolvedor dev = desenvolvedor(11L, Senioridade.JUNIOR, PapelAtuacao.QA, "3.10", 20);

            assertThat(montador.vetorDoCandidato(dev, projeto, 0).aderenciaTecnica()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("desconta as horas ja comprometidas em outros projetos")
        void deveDescontarHorasComprometidas() {
            Projeto projeto = projeto(CriticidadeProjeto.MEDIA, 20);
            Desenvolvedor dev = desenvolvedor(12L, Senioridade.PLENO, PapelAtuacao.FRONTEND, "3.50", 18);

            assertThat(montador.vetorDoCandidato(dev, projeto, 10).coberturaHoras()).isEqualTo(0.4);
        }

        @Test
        @DisplayName("cobertura nunca fica negativa nem passa de 1")
        void coberturaLimitadaEntreZeroEUm() {
            Projeto projeto = projeto(CriticidadeProjeto.MEDIA, 20);
            Desenvolvedor lotado = desenvolvedor(13L, Senioridade.PLENO, PapelAtuacao.FRONTEND, "3.50", 18);
            Desenvolvedor folgado = desenvolvedor(14L, Senioridade.PLENO, PapelAtuacao.FRONTEND, "3.50", 40);

            assertThat(montador.vetorDoCandidato(lotado, projeto, 30).coberturaHoras()).isZero();
            assertThat(montador.vetorDoCandidato(folgado, projeto, 0).coberturaHoras()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("arredonda as features em quatro casas, a mesma escala gravada no banco")
        void deveArredondarEmQuatroCasas() {
            Projeto projeto = projeto(CriticidadeProjeto.MEDIA, 30);
            projeto.adicionarHabilidadeRequerida(java, true, 1);
            projeto.adicionarHabilidadeRequerida(postgres, true, 1);
            projeto.adicionarHabilidadeRequerida(spring, true, 1);
            Desenvolvedor dev = desenvolvedor(15L, Senioridade.PLENO, PapelAtuacao.FULLSTACK, "3.50", 10);
            dev.adicionarHabilidade(java, 3);

            CandidatoVetor vetor = montador.vetorDoCandidato(dev, projeto, 0);

            assertThat(vetor.aderenciaTecnica()).isEqualTo(0.3333);
            assertThat(vetor.coberturaHoras()).isEqualTo(0.3333);
        }
    }

    // ------------------------------------------------------------------

    private Projeto projetoComStack() {
        Projeto projeto = projeto(CriticidadeProjeto.ALTA, 20);
        projeto.adicionarHabilidadeRequerida(java, true, 5);
        projeto.adicionarHabilidadeRequerida(postgres, false, 3);
        projeto.adicionarHabilidadeRequerida(spring, true, 4);
        return projeto;
    }

    private static Projeto projeto(CriticidadeProjeto criticidade, int horasPorIntegrante) {
        return new Projeto("Projeto", null, criticidade, 4, horasPorIntegrante);
    }

    private static Desenvolvedor desenvolvedor(Long id, Senioridade senioridade, PapelAtuacao papel,
                                               String softSkills, int horas) {
        Desenvolvedor dev = new Desenvolvedor("Dev " + id, "dev" + id + "@ufn.edu.br",
                senioridade, papel, new BigDecimal(softSkills), horas);
        ReflectionTestUtils.setField(dev, "id", id);
        return dev;
    }

    private static Habilidade habilidade(Long id, String nome, TipoHabilidade tipo) {
        CategoriaHabilidade categoria = tipo == TipoHabilidade.SOFT
                ? CategoriaHabilidade.COMPORTAMENTAL : CategoriaHabilidade.LINGUAGEM;
        Habilidade habilidade = new Habilidade(nome, tipo, categoria, null);
        ReflectionTestUtils.setField(habilidade, "id", id);
        return habilidade;
    }
}
