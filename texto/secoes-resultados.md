# Seções de Resultados — Texto para o TFG

Sistema Web de Recomendação para Formação de Equipes em Projetos de Desenvolvimento de Software
Eduardo Requia Souza / Orientador: Alexandre Zamberlan — UFN

---

> **Como usar este documento**
>
> O texto abaixo está pronto para ser transportado para o artigo, respeitando a
> numeração das subseções já existentes na Seção III-C (Resultados e Discussões).
>
> As marcações `[[ PREENCHER: ... ]]` indicam os pontos que dependem dos artefatos
> do treinamento (tabela do `compare_models()`, métricas obtidas, script gerador do
> escore de afinidade). **Nenhum valor foi estimado ou inventado** — preencher esses
> campos com dados reais é indispensável.
>
> As figuras estão numeradas a partir da Figura 4, dando continuidade às três já
> presentes no texto.

---

## 1) Protótipo

> *Substitui o título hoje vazio na Seção III-C.*

O protótipo foi implementado seguindo a arquitetura de microsserviços apresentada na
Figura 3, na qual três serviços cooperam por meio de requisições HTTP com troca de
mensagens em formato JSON. O serviço principal, desenvolvido em Java com o framework
Spring Boot, atua como orquestrador: concentra as regras de negócio, a persistência
dos dados e a comunicação com os demais serviços. O serviço de predição, construído
em Python e exposto por meio do framework FastAPI, encapsula o modelo de
reconhecimento gerado na etapa de treinamento. A interface de usuário, desenvolvida
em React, é responsável pela captura dos dados e pela apresentação das recomendações.

A responsabilidade de cada componente foi delimitada de modo que o cálculo do escore
de afinidade permanecesse isolado no ecossistema Python, onde estão as bibliotecas de
aprendizado de máquina, enquanto a lógica de composição das equipes e a persistência
permanecessem no ecossistema Java. Essa separação também determina que a interface de
usuário não acesse diretamente nem o serviço de predição nem a API do modelo de
linguagem: todas as requisições são intermediadas pelo backend.

### Modelagem relacional

Para atender ao objetivo específico de mapeamento quantitativo e qualitativo das
competências, dos perfis de desenvolvedores e dos requisitos de projetos, elaborou-se
a estrutura relacional apresentada na Figura 4. O modelo é composto por nove
entidades, organizadas em três agrupamentos: o cadastro dos profissionais, a
especificação dos projetos e o registro das recomendações geradas.

> **Figura 4.** Diagrama de entidade-relacionamento do protótipo.
> *[[ INSERIR FIGURA: o diagrama pode ser gerado no Astah a partir da descrição das
> nove entidades apresentada nesta seção ]]*

O cadastro dos profissionais é sustentado pelas entidades `Desenvolvedor`,
`Habilidade` e `DesenvolvedorHabilidade`. A entidade `Habilidade` constitui um
catálogo centralizado de competências, no qual cada registro armazena, além do nome
de exibição, o rótulo correspondente à coluna da matriz de treinamento. Esse vínculo
explícito assegura que as competências informadas no cadastro correspondam exatamente
aos atributos reconhecidos pelo modelo preditivo. A entidade associativa
`DesenvolvedorHabilidade` registra o nível de proficiência declarado, permitindo
representar graus distintos de domínio sobre uma mesma tecnologia.

A especificação dos projetos é composta pelas entidades `Projeto`,
`ProjetoPapelRequerido` e `ProjetoHabilidadeRequerida`. A primeira concentra os
atributos que caracterizam a demanda, entre os quais o nível de criticidade — que
determina a ponderação aplicada à senioridade e às competências comportamentais — e o
tamanho pretendido da equipe. As duas entidades associativas expressam,
respectivamente, o número de vagas exigidas em cada papel de atuação e a stack
tecnológica requerida, esta última acompanhada de um peso relativo e da indicação de
obrigatoriedade. Esses dois conjuntos de requisitos constituem a base de cálculo das
métricas de cobertura de papéis e de cobertura de tecnologias.

O registro das recomendações é realizado pelas entidades `Recomendacao` e
`RecomendacaoMembro`. Cada execução do motor preditivo origina um novo registro, de
modo que o histórico das recomendações seja preservado e possa ser comparado entre os
diferentes cenários de avaliação. A entidade `Recomendacao` armazena, além da
justificativa produzida pelo modelo de linguagem, os valores das métricas calculadas
no momento da geração, assegurando que o resultado permaneça auditável mesmo diante de
alterações posteriores nos perfis dos profissionais. A entidade `RecomendacaoMembro`
preserva o ranqueamento completo dos candidatos avaliados, e não apenas os
selecionados, o que possibilita identificar quais perfis foram preteridos e por qual
margem.

Por fim, a entidade `Alocacao` registra o compromisso efetivamente confirmado entre um
desenvolvedor e um projeto, expresso em horas semanais. A distinção entre essa entidade
e `RecomendacaoMembro` é necessária ao tratamento do cenário de disputa por
profissionais: a segunda representa uma sugestão produzida pelo motor preditivo,
enquanto a primeira representa um compromisso aceito pelo gestor. Somente as alocações
confirmadas reduzem a disponibilidade de um profissional, o que permite ao sistema
identificar conflitos quando múltiplas equipes são formadas simultaneamente.

### Funcionalidades implementadas

O orquestrador disponibiliza uma interface de programação de aplicações no estilo REST,
organizada em três conjuntos de operações. O primeiro compreende a manutenção do
catálogo de competências, atribuição do perfil Administrador. O segundo compreende o
cadastro e a atualização dos perfis de desenvolvedores, incluindo a declaração das
competências dominadas e o ajuste da disponibilidade semanal — operação acessível ao
próprio profissional, conforme previsto no diagrama de casos de uso. O terceiro
compreende o cadastro dos projetos e a definição de seus requisitos, atribuição do
perfil Gestor.

O catálogo de competências é inicializado com as trinta tecnologias correspondentes às
colunas da matriz de treinamento, acrescidas de um conjunto de competências
comportamentais. Essa carga inicial garante a correspondência entre os dados
cadastrados no protótipo e os atributos processados pelo motor preditivo.

Registros de desenvolvedores, competências e projetos não são removidos fisicamente da
base. A inativação de um profissional o exclui das recomendações subsequentes, mas
preserva sua participação nas recomendações já geradas — condição necessária à
integridade do histórico utilizado na avaliação deste trabalho.

---

## 3) Uso e aplicação do Modelo de Aprendizagem

> *Substitui o título hoje vazio na Seção III-C.*

Concluída a etapa de treinamento, o modelo de reconhecimento foi incorporado ao
protótipo como serviço de predição. Esta seção descreve como os dados persistidos na
base relacional são convertidos no vetor de atributos esperado pelo modelo e como o
escore resultante é utilizado na composição das equipes.

### Fluxo de execução da recomendação

A Figura 5 apresenta a sequência de interações desencadeada pela solicitação de uma
recomendação.

> **Figura 5.** Diagrama de sequência do fluxo de recomendação.
> *[[ INSERIR FIGURA: pode ser gerada no Astah a partir da sequência descrita a
> seguir ]]*

O fluxo é iniciado pelo gestor, por meio da interface gráfica ou da interface
conversacional. Na segunda situação, o texto em linguagem natural é previamente
submetido ao modelo de linguagem, que atua como extrator de entidades, convertendo a
solicitação em parâmetros estruturados. A partir desse ponto, ambas as vias convergem
para o mesmo processamento.

O orquestrador recupera os requisitos do projeto e o conjunto de profissionais
disponíveis, constrói o vetor de atributos correspondente a cada candidato e submete
o conjunto ao serviço de predição. Este carrega o modelo previamente treinado, calcula
o escore de afinidade de cada candidato e devolve os resultados em formato JSON. De
posse dos escores, o orquestrador ordena os candidatos, aplica as regras de composição
da equipe e calcula as métricas de avaliação. O resultado consolidado é então
encaminhado ao modelo de linguagem, que redige a justificativa qualitativa
apresentada ao gestor.

### Construção do vetor de atributos

A conversão entre a estrutura relacional e o vetor de trinta e nove posições esperado
pelo modelo é apresentada na Tabela I. A correspondência reproduz, em tempo de
execução, as mesmas transformações de engenharia de atributos aplicadas durante o
treinamento.

**Tabela I — Correspondência entre a base relacional e os atributos do modelo**

| Posições | Origem na base relacional | Transformação aplicada |
|:---:|---|---|
| 1 | `Desenvolvedor.senioridade` | Mapeamento para escala ordinal crescente, de 1 a 4 |
| 2–7 | `Desenvolvedor.papelPrincipal` | Codificação binária: valor 1 na coluna correspondente ao papel e 0 nas demais |
| 8–37 | `DesenvolvedorHabilidade` | Codificação binária por tecnologia, associada por meio do rótulo do conjunto de dados |
| 38 | `Desenvolvedor.softSkillsScore` | Valor direto, em escala de 0 a 5 |
| 39 | `Desenvolvedor.horasDisponiveis` menos as horas confirmadas em `Alocacao` | Disponibilidade efetiva |

A última linha da Tabela I merece destaque metodológico. A disponibilidade submetida ao
modelo não corresponde ao valor declarado no cadastro, mas à diferença entre esse valor
e o total de horas semanais já comprometidas em projetos ativos. Dessa forma, um
profissional já alocado em outras frentes apresenta disponibilidade efetiva reduzida e
tende a ocupar posições inferiores no ranqueamento, sem que seja necessária qualquer
regra de exclusão específica. Esse mecanismo constitui o tratamento adotado para o
cenário de disputa por profissionais entre projetos simultâneos.

### Exemplo de aplicação

> *[[ PREENCHER: esta subseção requer uma execução real do protótipo. Sugere-se
> apresentar um projeto de exemplo, o vetor de atributos de dois ou três candidatos,
> os escores devolvidos pelo serviço de predição e a justificativa produzida pelo
> modelo de linguagem. Um exemplo numérico completo demonstra a integração de forma
> mais convincente do que a descrição textual. ]]*

---

## 4) Cenários de Avaliação

> *Substitui o título hoje vazio na Seção III-C.*

A avaliação do protótipo foi conduzida por meio da simulação de cenários controlados,
representativos de situações recorrentes na formação de equipes de engenharia de
software. Esta seção descreve o procedimento adotado, a linha de base utilizada para
comparação e os resultados obtidos em cada cenário.

### Procedimento e linha de base

Para verificar se as recomendações produzidas pelo protótipo são superiores a
alocações não orientadas por dados, adotou-se como linha de base a **composição
aleatória de equipes**. Em cada cenário, o conjunto de profissionais elegíveis foi
submetido a `[[ PREENCHER: número de repetições, sugere-se 100 ]]` sorteios
independentes, respeitando apenas o tamanho da equipe definido para o projeto. As
métricas foram calculadas para cada composição sorteada, e a média dos resultados
constitui o valor de referência apresentado nas tabelas a seguir.

A escolha da composição aleatória como linha de base decorre da ausência de registros
históricos de alocações reais. Trata-se, portanto, de uma referência conservadora:
demonstra que o desempenho do protótipo não é atribuível ao acaso, embora não permita
compará-lo diretamente ao desempenho de gestores humanos.

As métricas empregadas são as três definidas na Seção III-B: cobertura de papéis,
cobertura de tecnologias e dispersão interna da equipe, esta última expressa pelo
desvio padrão da senioridade dos integrantes. Nas duas primeiras, valores mais
elevados indicam melhor desempenho; na terceira, valores menores indicam equipes mais
niveladas.

### Cenário 1 — Formação de equipes restritas a papéis mínimos obrigatórios

Neste cenário, avalia-se a capacidade do protótipo de compor equipes que atendam
integralmente às posições exigidas pelo escopo do projeto.

> *[[ PREENCHER: descrever a configuração do projeto utilizado — número de
> integrantes, papéis exigidos e respectivas quantidades, stack requerida e nível de
> criticidade — bem como o número de profissionais disponíveis na base. ]]*

**Tabela II — Resultados do Cenário 1**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Cobertura de tecnologias (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Desvio padrão da senioridade | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

> *[[ PREENCHER: análise do resultado. ]]*

### Cenário 2 — Balanceamento de experiência entre os integrantes

Neste cenário, verifica-se o nivelamento da equipe recomendada, com atenção à
possibilidade de composições formadas majoritariamente por profissionais de um mesmo
nível de senioridade.

> *[[ PREENCHER: configuração do cenário. ]]*

**Tabela III — Resultados do Cenário 2**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Cobertura de tecnologias (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Desvio padrão da senioridade | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

> *[[ PREENCHER: análise do resultado, observando especialmente o comportamento do
> protótipo em projetos de criticidade alta, nos quais a senioridade recebe
> multiplicadores de peso mais elevados. ]]*

### Cenário 3 — Recomendação em contexto de escassez e disponibilidade desbalanceada

Neste cenário, o número de profissionais com disponibilidade suficiente é inferior ao
necessário para a composição integral da equipe. Avalia-se a capacidade do protótipo
de propor a melhor composição possível e de sinalizar explicitamente a restrição
encontrada, em vez de retornar ausência de resultado.

> *[[ PREENCHER: configuração do cenário. ]]*

**Tabela IV — Resultados do Cenário 3**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Cobertura de tecnologias (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Integrantes alocados / previstos | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

> *[[ PREENCHER: análise do resultado. ]]*

### Cenário 4 — Conflito de alocação entre projetos simultâneos

Neste cenário, dois ou mais projetos disputam o mesmo conjunto de profissionais.
Avalia-se o efeito do desconto das horas já comprometidas sobre o ranqueamento dos
candidatos, conforme descrito na Seção III-C-3.

> *[[ PREENCHER: configuração do cenário, indicando os projetos envolvidos e a ordem
> de formação das equipes. ]]*

**Tabela V — Resultados do Cenário 4**

| Métrica | Projeto A | Projeto B |
|---|:---:|:---:|
| Cobertura de papéis (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Cobertura de tecnologias (%) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Profissionais em sobreposição | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

> *[[ PREENCHER: análise do resultado. ]]*

### Discussão dos resultados

> *[[ PREENCHER: síntese comparativa dos quatro cenários. Recomenda-se responder
> explicitamente à afirmação apresentada na Seção III-B — se a integração entre o
> motor preditivo e a interface generativa produziu recomendações consistentes,
> justificáveis e superiores às alocações não orientadas por dados. ]]*

---

## Complementos à subseção *2) Modelo de Aprendizagem*

> *Os trechos abaixo complementam a subseção já redigida. Recomenda-se inseri-los
> nos pontos indicados.*

### Complemento A — Dimensionamento do conjunto de dados

> *Inserir após o parágrafo que descreve a filtragem dos profissionais.*

Após a filtragem por área de atuação, o conjunto de dados resultante reuniu 33.462
registros de profissionais. A aplicação das técnicas de engenharia de atributos
produziu uma matriz de 39 variáveis independentes, assim distribuídas: uma variável
ordinal correspondente ao nível de senioridade, seis variáveis binárias
correspondentes aos papéis de atuação, trinta variáveis binárias correspondentes às
tecnologias dominadas, uma variável contínua correspondente ao escore de competências
comportamentais e uma variável contínua correspondente à disponibilidade semanal.

### Complemento B — Parâmetros dos dados sintéticos

> *Inserir após o parágrafo que descreve a injeção de dados sintéticos.*

A geração dos atributos ausentes na base pública seguiu distribuições normais com os
parâmetros apresentados na Tabela `[[ AJUSTAR NUMERAÇÃO ]]`. Os valores gerados foram
truncados aos limites de cada escala, de modo a preservar a validade do domínio.

| Atributo | Distribuição | Média | Desvio padrão | Limites |
|---|:---:|:---:|:---:|:---:|
| Escore de competências comportamentais | Normal | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | 0 a 5 |
| Disponibilidade semanal (horas) | Normal | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

O cruzamento dos perfis com os cenários de projetos fictícios resultou em um conjunto
de treinamento com `[[ PREENCHER: número de registros ]]` observações, obtidas a
partir de `[[ PREENCHER: número de cenários ]]` configurações distintas de projeto.

### Complemento C — Formulação do escore de afinidade

> *Inserir após o parágrafo que apresenta os três níveis de criticidade. Esta é a
> adição mais importante desta seção: sem a explicitação da função geradora, o
> experimento não é reprodutível.*

O escore de afinidade, adotado como variável dependente, foi obtido pela combinação
ponderada dos atributos normalizados de cada profissional em face das exigências de
cada cenário de projeto, conforme a Equação 1:

```
S(d, p) = w_t(c) · T(d, p) + w_s(c) · N(d) + w_c(c) · C(d) + w_h · H(d) + ε
```

na qual `S(d, p)` representa o escore de afinidade do desenvolvedor `d` em relação ao
projeto `p`; `T(d, p)` a proporção das tecnologias exigidas pelo projeto que são
dominadas pelo profissional; `N(d)` o nível de senioridade normalizado; `C(d)` o
escore de competências comportamentais normalizado; `H(d)` a disponibilidade semanal
normalizada; e `ε` um termo de ruído aleatório, incorporado para representar a
imprevisibilidade inerente ao comportamento humano.

Os coeficientes `w_t`, `w_s` e `w_c` são função do nível de criticidade `c` do
projeto, assumindo os valores apresentados na Tabela `[[ AJUSTAR NUMERAÇÃO ]]`. Essa
dependência operacionaliza a definição dos três níveis de criticidade: à medida que a
criticidade aumenta, cresce o peso atribuído à senioridade e às competências
comportamentais, em detrimento do peso atribuído à aderência estritamente técnica.

| Nível de criticidade | `w_t` (técnica) | `w_s` (senioridade) | `w_c` (comportamental) | `w_h` (disponibilidade) |
|---|:---:|:---:|:---:|:---:|
| Baixa (Nível 1) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Média (Nível 2) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |
| Alta (Nível 3) | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` |

O termo de ruído `ε` foi amostrado de uma distribuição normal de média zero e desvio
padrão `[[ PREENCHER ]]`.

Cabe registrar que a variável dependente é, portanto, de natureza sintética, derivada
de uma função conhecida. O treinamento de um modelo preditivo sobre tais dados
justifica-se por três razões. Em primeiro lugar, o modelo aprende uma aproximação
capaz de generalizar para combinações de atributos não representadas no conjunto de
treinamento, o que a aplicação direta da função não asseguraria diante de perfis
incompletos. Em segundo lugar, a presença do termo de ruído impede a reprodução exata
da função geradora, exigindo do algoritmo a identificação do padrão subjacente e não
a memorização de valores. Em terceiro lugar, a inferência a partir do modelo
consolidado apresenta custo computacional inferior ao da reavaliação da função para
cada par de desenvolvedor e projeto, o que se torna relevante à medida que a base de
profissionais cresce.

### Complemento D — Comparação entre algoritmos e resultados obtidos

> *Substituir o parágrafo que menciona a seleção do LightGBM sem apresentar valores.*

A comparação entre os algoritmos de regressão foi conduzida com a biblioteca PyCaret,
sobre conjunto de treinamento e teste divididos na proporção
`[[ PREENCHER: ex. 70/30 ]]`, com validação cruzada em `[[ PREENCHER: número ]]`
partições. A Tabela `[[ AJUSTAR NUMERAÇÃO ]]` apresenta o desempenho dos algoritmos
avaliados, ordenados pelo Erro Quadrático Médio.

| Algoritmo | RMSE | R² | MAE | Tempo (s) |
|---|:---:|:---:|:---:|:---:|
| `[[ PREENCHER ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` |
| `[[ PREENCHER ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` |
| `[[ PREENCHER ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` |
| `[[ PREENCHER ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` |
| `[[ PREENCHER ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` | `[[ ]]` |

> *A saída da função `compare_models()` do PyCaret fornece diretamente estas colunas.
> Recomenda-se apresentar os cinco algoritmos de melhor desempenho.*

O algoritmo Light Gradient Boosting Machine apresentou o melhor desempenho, com RMSE
de `[[ PREENCHER ]]` e coeficiente de determinação de `[[ PREENCHER ]]`. Submetido à
otimização de hiperparâmetros por validação cruzada, o modelo alcançou RMSE de
`[[ PREENCHER ]]` e R² de `[[ PREENCHER ]]`, valores que representam
`[[ PREENCHER: descrever a variação obtida ]]` em relação à configuração inicial.

O procedimento foi executado em ambiente Python 3.10, com a biblioteca PyCaret na
versão `[[ PREENCHER ]]`, em `[[ PREENCHER: Google Colab ou execução local ]]`.

### Complemento E — Limitações do modelo

> *Inserir ao final da subseção. Declarar as limitações é preferível a omiti-las.*

O procedimento adotado apresenta limitações que devem ser consideradas na
interpretação dos resultados. A principal delas refere-se à origem da variável
dependente: por não existir base pública que associe perfis de desenvolvedores a
desfechos reais de alocação, o escore de afinidade foi construído a partir de uma
função definida neste trabalho. Consequentemente, as métricas de desempenho obtidas
indicam a capacidade do algoritmo de aproximar essa função, e não sua capacidade de
prever o sucesso efetivo de uma equipe em ambiente organizacional.

De forma análoga, os atributos referentes às competências comportamentais e à
disponibilidade semanal foram gerados sinteticamente, uma vez que não constavam da
base pública utilizada. Ainda que os parâmetros das distribuições tenham sido
definidos de modo a representar um ambiente corporativo plausível, tais valores não
correspondem a observações reais.

Por fim, a base utilizada resulta de autodeclaração dos respondentes, o que implica
que o domínio sobre determinada tecnologia expressa percepção individual, sem
verificação independente do nível de proficiência.

A superação dessas limitações demandaria a aplicação do protótipo em ambiente
organizacional real, com registro histórico de alocações e de seus desfechos —
possibilidade indicada entre os trabalhos futuros.

---

## Conclusões — proposta de reescrita

> *A conclusão atual está redigida no tempo futuro, prevendo a implementação do
> protótipo como trabalho futuro. Como essa etapa foi concluída, sugere-se a
> reescrita a seguir, preservando a estrutura original.*

Este trabalho apresentou um protótipo Web voltado à recomendação para a formação de
equipes em projetos de desenvolvimento de software. Ao longo do texto, evidenciou-se
que a alocação de profissionais, quando realizada de forma manual e baseada em
percepções subjetivas, pode resultar em times desbalanceados e comprometer os prazos e
a qualidade das entregas organizacionais. Diante desse cenário, a pesquisa estruturou
um sistema que integra algoritmos de aprendizado de máquina, responsáveis pelo cálculo
quantitativo de afinidade, a Grandes Modelos de Linguagem, encarregados de processar
interações e justificar as escolhas geradas de forma qualitativa.

O desenvolvimento contemplou a fundamentação teórica, a análise de trabalhos
correlatos e a especificação do sistema por meio da modelagem estrutural e funcional.
No plano da implementação, construiu-se a estrutura relacional de dados destinada ao
mapeamento de competências, perfis e requisitos de projetos, bem como a camada de
orquestração responsável pela lógica de composição das equipes e pela integração entre
os serviços. O motor preditivo foi treinado e avaliado em ambiente Python, tendo o
algoritmo Light Gradient Boosting Machine apresentado o melhor desempenho entre os
modelos comparados.

> *[[ PREENCHER: acrescentar aqui um parágrafo sintetizando os resultados dos
> cenários de avaliação, uma vez executados. ]]*

Entre as contribuições do trabalho, destaca-se a articulação entre duas abordagens
usualmente tratadas de forma isolada: o processamento analítico, responsável pelo
ranqueamento quantitativo dos candidatos, e a geração de linguagem natural,
responsável por tornar esse ranqueamento interpretável pelo gestor. Essa combinação
busca responder à limitação apontada na introdução, segundo a qual recomendações
produzidas exclusivamente por algoritmos preditivos carecem de transparência
metodológica.

Reconhecem-se, contudo, as limitações discutidas na Seção `[[ AJUSTAR REFERÊNCIA ]]`,
sobretudo a natureza sintética da variável dependente e de parte dos atributos
utilizados no treinamento. Tais limitações restringem o alcance das conclusões à
validação técnica da solução proposta, sem permitir inferências sobre o desempenho de
equipes em contexto organizacional real.

Como trabalhos futuros, indica-se a aplicação do protótipo em ambiente organizacional,
com o registro histórico de alocações e de seus desfechos, o que permitiria substituir
a variável sintética por dados observados e reavaliar o modelo sob condições reais.
Indica-se, ainda, a incorporação de mecanismos de realimentação, por meio dos quais as
avaliações do gestor sobre as equipes sugeridas passem a compor o conjunto de
treinamento, e a investigação de estratégias de otimização combinatória para a
composição da equipe como um todo, em complemento ao ranqueamento individual dos
candidatos adotado neste trabalho.

---

## Anexo — Correções pontuais identificadas no texto

| Localização | Ocorrência | Correção sugerida |
|---|---|---|
| Seção II, parágrafo introdutório | "Largue Language Models" | "Large Language Models" |
| Seção II, parágrafo introdutório | "Interfaces Conversasionais" | "Interfaces Conversacionais" |
| Resumo | "àrea de atuação" | "área de atuação" |
| Seções II-E-1 e III | "microserviços" | "microsserviços" (padronizar, ocorre nas duas grafias) |
| Seção III-A | "serviço de Inteligência Computacional – IC" | **Colisão de siglas.** A sigla IC é definida na Seção II-E como Interface Conversacional. Sugere-se renomear este componente para "serviço de predição" ou "motor de predição" |

### Sugestão adicional

A Seção II-F apresenta os três trabalhos correlatos em texto corrido. A inclusão de um
quadro comparativo — contendo, para cada trabalho, o domínio de aplicação, as técnicas
empregadas, as tecnologias utilizadas e a lacuna que o presente trabalho endereça —
tornaria a contribuição desta pesquisa imediatamente visível ao leitor, prática usual
em artigos científicos.
