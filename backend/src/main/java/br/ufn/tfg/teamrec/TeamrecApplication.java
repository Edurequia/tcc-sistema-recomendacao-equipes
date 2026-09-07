package br.ufn.tfg.teamrec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do backend orquestrador do Sistema de Recomendacao
 * para Formacao de Equipes em Projetos de Desenvolvimento de Software.
 *
 * Responsabilidades desta aplicacao dentro da arquitetura de microsservicos:
 *  - Persistir e gerenciar desenvolvedores, habilidades e projetos (PostgreSQL);
 *  - Montar o vetor de atributos e consumir o motor de predicao (FastAPI/PyCaret);
 *  - Aplicar as regras de composicao e nivelamento da equipe;
 *  - Intermediar a interface conversacional com o LLM (Gemini, via Spring AI).
 */
@SpringBootApplication
public class TeamrecApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamrecApplication.class, args);
    }
}
