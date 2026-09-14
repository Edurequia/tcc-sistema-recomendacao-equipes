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
  React  ──HTTP──▶  [ Backend Spring Boot ]  ──HTTP──▶  FastAPI + LightGBM (escore de afinidade)
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

**Perfil `local` (padrão, PostgreSQL):**

```bash
cp .env.example .env              # e defina DB_PASSWORD
docker compose up -d              # PostgreSQL em localhost:5433
./mvnw spring-boot:run
```

O `.env` é lido pelo docker compose e pelo Spring, então as credenciais ficam em um
só lugar. Para usar um PostgreSQL instalado na máquina em vez do container, ajuste
`DB_PORT=5432` e a senha no `.env` e crie o banco `teamrec`.

**Perfil `dev` (H2 em memória, zero instalação, dados descartados ao desligar):**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Os testes automatizados usam sempre o perfil `dev`.

**Motor de predição** (necessário para gerar recomendações; o restante da API funciona
sem ele). Instruções completas em [`../api_motor_predicao/README.md`](../api_motor_predicao/README.md):

```powershell
cd ..\api_motor_predicao
.\.venv\Scripts\python.exe -m uvicorn app.main:app --port 8000
```

Com o motor fora do ar, `POST /api/projetos/{id}/recomendacoes` responde **503**.

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
`DB_NAME`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` (ou `DB_URL` completa), `PREDICTION_BASE_URL`,
`GEMINI_API_KEY`, `APP_CORS_ORIGINS`.

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
- [x] **Fase 4** — Cliente do motor de predição + endpoint de recomendação
- [x] **Fase 5** — Regras de composição: cobertura de papéis/tecnologias, dispersão, conflito de alocação
- [ ] **Fase 6** — Spring AI + Gemini: justificativa e extração de entidades
- [ ] **Fase 7** — Testes de integração, Docker, observabilidade

## Recomendações (Fases 4 e 5)

| Método | Rota | O quê |
|---|---|---|
| POST | `/api/projetos/{id}/recomendacoes` | pontua os ativos, compõe a equipe, calcula as métricas e grava tudo (201) |
| GET | `/api/projetos/{id}/recomendacoes` | histórico do projeto com as métricas, mais recente primeiro |
| GET | `/api/recomendacoes/{id}` | equipe, métricas, restrições e ranking completo |
| POST | `/api/recomendacoes/{id}/confirmacao` | aloca os selecionados no projeto (tudo ou nada) |
| GET | `/api/recomendacoes/{id}/linha-base` | compara a equipe proposta com equipes sorteadas ao acaso |

Fluxo em três etapas, para a espera pelo motor não segurar conexão do banco:

1. **Leitura** (transação somente leitura): projeto, desenvolvedores ativos e horas já
   alocadas em *outros* projetos ativos. `MontadorVetorAtributos` calcula as features.
2. **Motor** (sem transação): `MotorPredicaoClient` envia o lote; qualquer falha → 503.
3. **Gravação**: ordena por escore (empate pelo nome), `ComposicaoEquipe` escolhe a equipe
   e grava `Recomendacao` (com as métricas) + `RecomendacaoMembro` (com `aderenciaTecnica`
   e `coberturaHoras` daquele instante, para o escore continuar explicável depois).

Regras: projeto `CONCLUIDO`/`CANCELADO` ou nenhum desenvolvedor ativo → 422.
A senioridade tem **três níveis** (JUNIOR, PLENO, SENIOR), os mesmos do treinamento.

### Composição da equipe (`service/composicao`)

1. **Elegibilidade** — só entra quem tem livres todas as horas semanais exigidas
   (`coberturaHoras = 1`). O escore mede afinidade; disponibilidade é restrição e não é
   compensada por escore alto. Inelegíveis continuam no ranking com `elegivel = false`.
2. **Vagas por papel** — cada vaga declarada vai para o elegível de maior escore com
   exatamente aquele papel.
3. **Posições livres** — o restante, até `tamanhoEquipe`, vai para os maiores escores de
   qualquer papel.

Sem elegíveis suficientes, a equipe sai **parcial** e `observacaoRestricao` explica:
integrantes faltantes, candidatos sem horas, vagas sem o papel exigido e tecnologias
obrigatórias que ninguém da equipe domina.

### Métricas (`CalculadoraMetricas`)

| Métrica | Cálculo |
|---|---|
| `coberturaPapeis` | Σ min(integrantes do papel, vagas do papel) ÷ vagas exigidas × 100 |
| `coberturaTecnologias` | Σ pesos das tecnologias HARD dominadas por ao menos um integrante ÷ Σ pesos × 100 |
| `desvioPadraoSenioridade` | desvio padrão populacional do nível (1–3) dos integrantes |
| `desvioPadraoScore` | desvio padrão populacional dos escores dos integrantes |
| `scoreMedioEquipe` | média dos escores dos integrantes |

Sem papéis ou sem tecnologias exigidas, a cobertura correspondente é 100. A calculadora
não depende de como a equipe foi formada — serve também para a linha de base aleatória.

### Linha de base aleatória (`LinhaBaseAleatoria`)

`GET /api/recomendacoes/{id}/linha-base?sorteios=100&semente=42`

Responde se a equipe proposta é melhor do que uma escolha ao acaso. Sorteia `sorteios`
equipes do tamanho do projeto **apenas entre os candidatos elegíveis** daquela
recomendação (o mesmo grupo de onde o protótipo escolhe), mede cada uma com a mesma
`CalculadoraMetricas` e devolve, lado a lado, as métricas do protótipo e a **média** e o
**desvio padrão** de cada métrica entre os sorteios.

- A semente fixa torna o resultado reproduzível (mesma semente → mesmo resultado).
- Usa os escores e a cobertura de horas gravados na recomendação; requisitos,
  senioridade e competências são os atuais — gere a linha de base logo após a recomendação.
- Sem elegíveis → 422; `sorteios` fora de 1–10.000 → 400.

## Alocações (Fase 5)

| Método | Rota | O quê |
|---|---|---|
| POST | `/api/projetos/{id}/alocacoes` | aloca um desenvolvedor (201). Só `desenvolvedorId` é obrigatório |
| GET | `/api/projetos/{id}/alocacoes` | equipe alocada (ativas) |
| GET | `/api/alocacoes/{id}` | uma alocação, ativa ou encerrada |
| DELETE | `/api/alocacoes/{id}` | encerra (lógico) e libera as horas (204) |

Padrões: papel principal do desenvolvedor, horas por integrante do projeto, data de hoje.
Recusas: horas acima das livres (somando outros projetos ativos) → 422; mesma pessoa já
alocada no projeto → 409; projeto encerrado ou desenvolvedor inativo → 422.

É a alocação — e não a recomendação — que consome horas. Confirmar a equipe do projeto A
reduz a `coberturaHoras` desses profissionais no projeto B, que passam a inelegíveis
(Cenário 4, conflito entre projetos simultâneos).
