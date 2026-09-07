package br.ufn.tfg.teamrec.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Area tecnica de atuacao do desenvolvedor.
 *
 * Trata-se de um dado categorico nominal: nao ha hierarquia entre os papeis.
 * No motor de predicao cada papel vira uma coluna binaria (codificacao one-hot).
 *
 * O campo {@code chaveDataset} guarda o rotulo EXATO da coluna correspondente
 * na matriz de treinamento, extraida da Stack Overflow Developer Survey 2024.
 * Manter esse vinculo aqui evita espalhar strings magicas pelo codigo quando
 * o vetor de atributos for montado (Fase 4).
 */
public enum PapelAtuacao {

    BACKEND("Developer, back-end", "Backend"),
    FRONTEND("Developer, front-end", "Frontend"),
    FULLSTACK("Developer, full-stack", "FullStack"),
    MOBILE("Developer, mobile", "Mobile"),
    QA("Developer, QA or test", "QA / Testes"),
    DEVOPS("DevOps specialist", "DevOps");

    private final String chaveDataset;
    private final String descricao;

    PapelAtuacao(String chaveDataset, String descricao) {
        this.chaveDataset = chaveDataset;
        this.descricao = descricao;
    }

    /** Nome exato da coluna one-hot na matriz de treinamento. */
    public String getChaveDataset() {
        return chaveDataset;
    }

    public String getDescricao() {
        return descricao;
    }

    public static Optional<PapelAtuacao> porChaveDataset(String chave) {
        return Arrays.stream(values())
                .filter(p -> p.chaveDataset.equalsIgnoreCase(chave))
                .findFirst();
    }
}
