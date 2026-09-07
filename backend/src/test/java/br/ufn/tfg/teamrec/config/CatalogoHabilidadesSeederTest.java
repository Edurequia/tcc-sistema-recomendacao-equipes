package br.ufn.tfg.teamrec.config;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a carga inicial do catalogo contra as colunas reais da matriz de
 * treinamento.
 *
 * A lista abaixo foi transcrita do cabecalho do arquivo usado no treinamento
 * (matriz_tcc_final.csv, separador ponto e virgula). Se alguem alterar uma
 * chave no seeder, o build quebra aqui — antes que o desvio de grafia chegue
 * ao motor de predicao e produza um vetor de atributos silenciosamente errado.
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Carga inicial do catalogo")
class CatalogoHabilidadesSeederTest {

    /** As 30 colunas de tecnologia da matriz de treinamento, na grafia original. */
    private static final List<String> COLUNAS_DO_DATASET = List.of(
            "Amazon Web Services (AWS)", "Angular", "C#", "Dart", "Django", "Docker",
            "FastAPI", "Flutter", "Go", "Google Cloud", "HTML/CSS", "Java", "JavaScript",
            "Kotlin", "Kubernetes", "Microsoft Azure", "Microsoft SQL Server", "MongoDB",
            "MySQL", "Next.js", "Node.js", "PostgreSQL", "Python", "React", "React Native",
            "Redis", "Spring Boot", "Swift", "TypeScript", "Vue.js");

    @Autowired
    private HabilidadeRepository repository;

    @Test
    @DisplayName("cobre exatamente as 30 colunas de tecnologia do modelo")
    void deveCobrirTodasAsColunasDoModelo() {
        List<String> chavesCadastradas = repository.findByChaveDatasetIsNotNullOrderByChaveDatasetAsc()
                .stream()
                .map(Habilidade::getChaveDataset)
                .toList();

        assertThat(chavesCadastradas)
                .hasSize(30)
                .containsExactlyInAnyOrderElementsOf(COLUNAS_DO_DATASET);
    }

    @Test
    @DisplayName("as competencias comportamentais nao possuem coluna no modelo")
    void softSkillsNaoDevemTerChave() {
        List<Habilidade> softSkills = repository.findByTipoAndAtivaTrueOrderByNomeAsc(TipoHabilidade.SOFT);

        assertThat(softSkills).isNotEmpty();
        assertThat(softSkills).allSatisfy(habilidade -> {
            assertThat(habilidade.getChaveDataset()).isNull();
            assertThat(habilidade.possuiColunaNoModelo()).isFalse();
        });
    }

    @Test
    @DisplayName("a carga e idempotente e nao duplica registros")
    void naoDeveDuplicar() {
        long total = repository.count();

        // Reexecutar o runner com o catalogo populado nao pode inserir nada.
        new CatalogoHabilidadesSeeder(repository).run(null);

        assertThat(repository.count()).isEqualTo(total);
    }
}
