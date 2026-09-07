package br.ufn.tfg.teamrec.config;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.enums.CategoriaHabilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.domain.repository.HabilidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga inicial do catalogo de habilidades.
 *
 * As trinta competencias tecnicas abaixo sao exatamente as colunas de
 * tecnologia da matriz de treinamento, extraida da Stack Overflow Developer
 * Survey 2024. O campo {@code chaveDataset} guarda o rotulo literal daquela
 * coluna; o campo {@code nome} guarda a forma que aparece na interface.
 *
 * Sem esta carga nao ha como cadastrar um desenvolvedor com competencias que o
 * motor de predicao reconheca — e um vinculo criado a mao, com grafia
 * divergente, produziria um vetor de atributos silenciosamente incorreto.
 *
 * As competencias comportamentais nao possuem chave: o modelo nao as recebe
 * individualmente, apenas o escore consolidado do desenvolvedor.
 *
 * A execucao e idempotente — nada acontece se o catalogo ja tiver registros.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.catalogo", havingValue = "true")
public class CatalogoHabilidadesSeeder implements ApplicationRunner {

    private final HabilidadeRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.debug("Catalogo de habilidades ja populado; carga inicial ignorada.");
            return;
        }

        List<Habilidade> catalogo = new java.util.ArrayList<>(hardSkills());
        catalogo.addAll(softSkills());
        repository.saveAll(catalogo);

        log.info("Catalogo inicial gravado: {} habilidades ({} com coluna no modelo preditivo).",
                catalogo.size(), catalogo.stream().filter(Habilidade::possuiColunaNoModelo).count());
    }

    /** As 30 colunas de tecnologia da matriz de treinamento. */
    private List<Habilidade> hardSkills() {
        return List.of(
                // --- Linguagens ---
                hard("C#", CategoriaHabilidade.LINGUAGEM, "C#"),
                hard("Dart", CategoriaHabilidade.LINGUAGEM, "Dart"),
                hard("Go", CategoriaHabilidade.LINGUAGEM, "Go"),
                hard("HTML/CSS", CategoriaHabilidade.LINGUAGEM, "HTML/CSS"),
                hard("Java", CategoriaHabilidade.LINGUAGEM, "Java"),
                hard("JavaScript", CategoriaHabilidade.LINGUAGEM, "JavaScript"),
                hard("Kotlin", CategoriaHabilidade.LINGUAGEM, "Kotlin"),
                hard("Python", CategoriaHabilidade.LINGUAGEM, "Python"),
                hard("Swift", CategoriaHabilidade.LINGUAGEM, "Swift"),
                hard("TypeScript", CategoriaHabilidade.LINGUAGEM, "TypeScript"),

                // --- Frameworks e bibliotecas ---
                hard("Angular", CategoriaHabilidade.FRAMEWORK, "Angular"),
                hard("Django", CategoriaHabilidade.FRAMEWORK, "Django"),
                hard("FastAPI", CategoriaHabilidade.FRAMEWORK, "FastAPI"),
                hard("Next.js", CategoriaHabilidade.FRAMEWORK, "Next.js"),
                hard("Node.js", CategoriaHabilidade.FRAMEWORK, "Node.js"),
                hard("React", CategoriaHabilidade.FRAMEWORK, "React"),
                hard("Spring Boot", CategoriaHabilidade.FRAMEWORK, "Spring Boot"),
                hard("Vue.js", CategoriaHabilidade.FRAMEWORK, "Vue.js"),

                // --- Bancos de dados ---
                hard("Microsoft SQL Server", CategoriaHabilidade.BANCO_DE_DADOS, "Microsoft SQL Server"),
                hard("MongoDB", CategoriaHabilidade.BANCO_DE_DADOS, "MongoDB"),
                hard("MySQL", CategoriaHabilidade.BANCO_DE_DADOS, "MySQL"),
                hard("PostgreSQL", CategoriaHabilidade.BANCO_DE_DADOS, "PostgreSQL"),
                hard("Redis", CategoriaHabilidade.BANCO_DE_DADOS, "Redis"),

                // --- Nuvem ---
                hard("AWS", CategoriaHabilidade.CLOUD, "Amazon Web Services (AWS)"),
                hard("Google Cloud", CategoriaHabilidade.CLOUD, "Google Cloud"),
                hard("Microsoft Azure", CategoriaHabilidade.CLOUD, "Microsoft Azure"),

                // --- DevOps ---
                hard("Docker", CategoriaHabilidade.DEVOPS, "Docker"),
                hard("Kubernetes", CategoriaHabilidade.DEVOPS, "Kubernetes"),

                // --- Mobile ---
                hard("Flutter", CategoriaHabilidade.MOBILE, "Flutter"),
                hard("React Native", CategoriaHabilidade.MOBILE, "React Native"));
    }

    /** Competencias comportamentais: entram no escore consolidado, nao no vetor. */
    private List<Habilidade> softSkills() {
        return List.of(
                soft("Comunicação"),
                soft("Liderança"),
                soft("Trabalho em equipe"),
                soft("Proatividade"),
                soft("Resolução de problemas"),
                soft("Gestão de tempo"),
                soft("Adaptabilidade"),
                soft("Mentoria"));
    }

    private Habilidade hard(String nome, CategoriaHabilidade categoria, String chaveDataset) {
        return new Habilidade(nome, TipoHabilidade.HARD, categoria, chaveDataset);
    }

    private Habilidade soft(String nome) {
        return new Habilidade(nome, TipoHabilidade.SOFT, CategoriaHabilidade.COMPORTAMENTAL, null);
    }
}
