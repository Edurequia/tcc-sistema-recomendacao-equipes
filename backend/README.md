# TeamRec — Backend

Backend orquestrador do **Sistema Web de Recomendação para Formação de Equipes em
Projetos de Desenvolvimento de Software**.

Trabalho Final de Graduação — Sistemas de Informação — UFN (Universidade Franciscana).
Eduardo Requia Souza / Orientador: Alexandre Zamberlan.

## Documentação

A visão completa do backend — arquitetura, modelo de domínio, contrato com o motor de
predição, decisões técnicas e roadmap — está em [`docs/documentacao.html`](docs/documentacao.html)
(abra no navegador) e publicada em <https://claude.ai/code/artifact/2bf85962-f767-4335-903a-4d2a066854f4>.

## Papel na arquitetura

Este módulo é o **orquestrador**. Ele não calcula o escore de afinidade nem gera texto:
persiste o domínio, monta o vetor de atributos, consome os dois serviços de apoio e
aplica as regras de composição da equipe.

```
  React  ──HTTP──▶  [ Backend Spring Boot ]  ──HTTP──▶  FastAPI + PyCaret  (escore de afinidade)
                            │      │
                            │      └────Spring AI────▶  API do Gemini      (justificativa / extração)
                            ▼
                       PostgreSQL
```

## Stack

| Item | Versão |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.5.16 |
| PostgreSQL | perfil `local` |
| H2 (em memória) | perfil `dev` |
| Spring AI + Gemini | 1.1.8 — entra na Fase 6 |
| springdoc-openapi | 2.8.13 |
| MapStruct / Lombok | 1.6.3 / 1.18.36 |

## Como rodar

Não é preciso instalar Maven — o projeto usa o Maven Wrapper.

**Perfil `dev` (padrão, H2 em memória, zero instalação):**

```bash
./mvnw spring-boot:run
```

**Perfil `local` (PostgreSQL):**

```bash
export DB_PASSWORD=sua-senha
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

| Endereço | O quê |
|---|---|
| http://localhost:8080/swagger-ui.html | Documentação interativa da API |
| http://localhost:8080/v3/api-docs | Contrato OpenAPI (JSON) |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | Console do H2 (só no perfil `dev`) |

Testes: `./mvnw test`

## Organização do código

```
br.ufn.tfg.teamrec
├── config/            CORS, OpenAPI, clients HTTP
├── controller/        REST — camada fina: recebe, valida, delega
├── service/           regras de negócio; único lugar com @Transactional
├── domain/
│   ├── entity/        entidades JPA
│   ├── enums/         Senioridade, PapelAtuacao, Criticidade...
│   └── repository/    Spring Data JPA
├── dto/request|response/   records + Bean Validation
├── mapper/            MapStruct (entity ⇄ DTO)
├── integration/
│   ├── prediction/    cliente do motor de predição (FastAPI)
│   └── llm/           integração com o Gemini via Spring AI
└── shared/exception/  ApiError + GlobalExceptionHandler
```

Regras que o projeto segue:

- Entidade JPA **nunca** é serializada como JSON; o que entra e sai são DTOs.
- `@Transactional` vive no *service*, nunca no *controller*.
- `open-in-view: false` — o carregamento é explícito, sem consultas escondidas.
- Nenhum segredo em arquivo versionado; tudo por variável de ambiente (`.env.example`).

## Configuração

Copie `.env.example` para `.env` e preencha. Variáveis relevantes:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PREDICTION_BASE_URL`, `GEMINI_API_KEY`, `APP_CORS_ORIGINS`.

## Modelo de dominio

```
Habilidade ──< DesenvolvedorHabilidade >── Desenvolvedor ──< Alocacao >── Projeto
     │                                              │                        │
     └──< ProjetoHabilidadeRequerida >──────────────┼────────────────────────┤
                                                    │      ProjetoPapelRequerido
                    Recomendacao ──< RecomendacaoMembro >┘
                          │
                          └── pertence a um Projeto
```

Nove tabelas. `Alocacao` guarda o compromisso **confirmado** (consome horas);
`RecomendacaoMembro` guarda a **sugestão** do motor (não consome).
A distinção é o que permite detectar conflito entre projetos simultâneos.

## Andamento

- [x] **Fase 0** — Esqueleto, perfis, OpenAPI, tratamento global de erros
- [x] **Fase 1** — Entidades, enums e repositórios
- [x] **Fase 2** — CRUD de Desenvolvedor e Habilidade
- [x] **Fase 3** — CRUD de Projeto e seus requisitos
- [ ] **Fase 4** — Cliente do motor de predição + endpoint de recomendação
- [ ] **Fase 5** — Regras de composição: cobertura de papéis/tecnologias, dispersão, conflito de alocação
- [ ] **Fase 6** — Spring AI + Gemini: justificativa e extração de entidades
- [ ] **Fase 7** — Testes de integração, Docker, observabilidade
