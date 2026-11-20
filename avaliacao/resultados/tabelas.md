# Resultados dos cenarios de avaliacao

Gerado por `cenarios.py` · 100 sorteios por cenario · semente 42.
Os valores saem de `GET /api/recomendacoes/{id}/linha-base`.

## Cenario 1 — Formacao de equipes restritas a papeis minimos obrigatorios

Projeto **Sistema de Matricula Online**, criticidade média, equipe de 5 integrantes a 20 h semanais. Papéis exigidos: Backend (2), Frontend (2), QA / Testes (1). Stack: Java (peso 5, obrigatória), Spring Boot (peso 4, obrigatória), React (peso 4, obrigatória), PostgreSQL (peso 3), Docker (peso 3). Base de 20 profissionais, 20 deles elegíveis.

| Metrica | Prototipo | Linha de base (aleatoria) |
|---|:---:|:---:|
| Cobertura de papeis (%) | 100,00 | 59,60 ± 16,97 |
| Cobertura de tecnologias (%) | 100,00 | 93,84 ± 8,52 |
| Desvio padrao da senioridade | 0,490 | 0,702 ± 0,148 |
| Escore medio da equipe | 69,674 | 62,882 ± 5,690 |

Equipe proposta: Ana Souza (Backend, Senior, score 86.66); Fabio Nunes (Backend, Senior, score 85.745); Nelson Rocha (Frontend, Senior, score 70.765); Olivia Ramos (Frontend, Pleno, score 59.216); Quezia Amaral (QA / Testes, Pleno, score 45.984).

Restricao registrada: nenhuma.

## Cenario 2 — Balanceamento de experiencia entre os integrantes

Projeto **Plataforma de Telemedicina**, criticidade alta, equipe de 4 integrantes a 20 h semanais. Papéis exigidos: Backend (2), Frontend (1). Stack: Java (peso 5, obrigatória), Spring Boot (peso 4, obrigatória), React (peso 4, obrigatória), PostgreSQL (peso 3), Docker (peso 3). Base de 20 profissionais, 20 deles elegíveis.

| Metrica | Prototipo | Linha de base (aleatoria) |
|---|:---:|:---:|
| Cobertura de papeis (%) | 100,00 | 78,33 ± 20,21 |
| Cobertura de tecnologias (%) | 100,00 | 88,37 ± 11,05 |
| Desvio padrao da senioridade | 0,000 | 0,631 ± 0,251 |
| Escore medio da equipe | 86,491 | 61,474 ± 7,625 |

Equipe proposta: Davi Esteves (FullStack, Senior, score 92.297); Alice Bender (Backend, Senior, score 88.058); Breno Cardoso (Backend, Senior, score 86.973); Cecilia Duarte (Frontend, Senior, score 78.637).

Restricao registrada: nenhuma.

## Cenario 3 — Recomendacao em contexto de escassez e disponibilidade desbalanceada

Projeto **Migracao de Sistema Legado**, criticidade alta, equipe de 5 integrantes a 30 h semanais. Papéis exigidos: Backend (3), Frontend (1), QA / Testes (1). Stack: Java (peso 5, obrigatória), Spring Boot (peso 4, obrigatória), React (peso 4, obrigatória), PostgreSQL (peso 3), Docker (peso 3). Base de 14 profissionais, 3 deles elegíveis.

| Metrica | Prototipo | Linha de base (aleatoria) |
|---|:---:|:---:|
| Cobertura de papeis (%) | 60,00 | 60,00 ± 0,00 |
| Cobertura de tecnologias (%) | 100,00 | 100,00 ± 0,00 |
| Desvio padrao da senioridade | 0,471 | 0,471 ± 0,000 |
| Escore medio da equipe | 73,958 | 73,958 ± 0,000 |
| Integrantes alocados / previstos | 3/5 | 3/5 |

Equipe proposta: Adriana Lacerda (Backend, Senior, score 88.058); Bernardo Maia (Backend, Pleno, score 69.653); Clarice Neves (Frontend, Pleno, score 64.162).

Restricao registrada: Equipe parcial: 3 de 5 integrantes. 11 candidato(s) sem as 30 horas semanais livres exigidas. Vagas sem profissional do papel exigido: Backend (1), QA / Testes (1).

## Cenario 4 — Conflito de alocacao entre projetos simultaneos

Projetos **Portal do Paciente** (A, confirmado primeiro) e **Aplicativo de Agendamento** (B), ambos com equipe de 4 integrantes a 20 h semanais, sobre a mesma base de 12 profissionais com 30 h disponiveis cada.

| Metrica | Projeto A | Projeto B |
|---|:---:|:---:|
| Cobertura de papeis (%) | 100,00 | 100,00 |
| Cobertura de tecnologias (%) | 100,00 | 84,21 |
| Desvio padrao da senioridade | 0,000 | 0,000 |
| Escore medio da equipe | 85,952 | 68,518 |
| Profissionais em sobreposicao (antes da confirmacao) | 4 | 4 |
| Profissionais em sobreposicao (apos a confirmacao) | 0 | 0 |

Antes da confirmacao de A, as duas equipes propostas tinham 4 profissionais em comum: Aurora Bilac, Caio Drummond, Gustavo Holanda, Julia Krause. Apos a confirmacao, a sobreposicao caiu para 0.

Alocacao manual de um profissional ja comprometido em A: HTTP 422 — Conflito de alocacao: 'Aurora Bilac' tem 10 horas semanais livres, mas a alocacao exige 20.
