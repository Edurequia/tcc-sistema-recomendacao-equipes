# Cenários de avaliação

Carga de dados e execução dos quatro cenários controlados descritos na seção
*Cenários de Avaliação* do texto. O script `cenarios.py` cadastra a base pela própria
API REST, gera as recomendações, consulta a linha de base aleatória e grava as tabelas
de resultados em `resultados/`.

Nada aqui usa biblioteca externa: só a biblioteca padrão do Python.

## Como executar

Os três passos precisam acontecer nesta ordem, e o backend precisa estar com a **base
vazia** — o script recusa rodar sobre uma base já povoada, porque os cenários dependem
de saber exatamente quem está cadastrado.

```bash
# 1. Motor de predição (com motor_predicao_tcc.pkl na pasta)
cd api_motor_predicao
.venv/Scripts/python.exe -m uvicorn app.main:app --port 8000

# 2. Backend no perfil dev (H2 em memória: toda reinicialização zera a base)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Cenários
cd avaliacao
python cenarios.py
```

Para repetir a execução, reinicie o backend antes: o perfil `dev` recria a base a cada
inicialização, e é isso que torna o experimento reproduzível.

## O que cada cenário configura

| Cenário | Situação | Configuração |
|---|---|---|
| 1 | Papéis mínimos obrigatórios | Equipe de 5 com 2 backend, 2 frontend e 1 QA; base de 20 profissionais, todos elegíveis, com distribuição de papéis desbalanceada (8 backend, 5 full-stack, 3 frontend, 2 QA, 2 DevOps) |
| 2 | Balanceamento de experiência | Projeto de criticidade alta, equipe de 4; base de 20 profissionais com 10 júniores, 6 plenos e 4 sêniores |
| 3 | Escassez de disponibilidade | Equipe de 5 a 30 h semanais; base de 14 profissionais, dos quais apenas 3 têm as 30 h livres |
| 4 | Conflito entre projetos | Dois projetos de 4 integrantes a 20 h sobre a mesma base de 12 profissionais com 30 h cada; o projeto A é confirmado antes de o B ser recomendado |

O desbalanceamento do cenário 1 e a composição do cenário 2 são deliberados: se a base
fosse homogênea, qualquer sorteio acertaria os papéis e a linha de base não teria poder
de discriminação.

## Por que a base é maior que a equipe

A linha de base sorteia equipes **apenas entre os elegíveis**. Quando o número de
elegíveis é igual ao tamanho da equipe, todo sorteio devolve exatamente a mesma
composição, o desvio padrão é zero e a comparação não informa nada. Por isso os
cenários 1, 2 e 4 mantêm de três a quatro vezes mais elegíveis do que vagas.

O cenário 3 é a exceção proposital: nele os elegíveis são **menos** que as vagas, e a
linha de base coincide com o protótipo — em escassez, não há escolha a fazer. O que o
cenário avalia é a equipe parcial e a restrição registrada, não a comparação.

## Saídas

| Arquivo | Conteúdo |
|---|---|
| `resultados/tabelas.md` | Tabelas dos quatro cenários, com a configuração e a equipe proposta |
| `resultados/tabelas.tex` | As mesmas tabelas no formato do texto (`tab:cenario1` … `tab:cenario3`) |
| `resultados/cenarioN.json` | Resposta bruta da API: ranqueamento completo, métricas e linha de base |

Os parâmetros da linha de base (100 sorteios, semente 42) ficam no topo de
`cenarios.py`, em `SORTEIOS` e `SEMENTE`.
