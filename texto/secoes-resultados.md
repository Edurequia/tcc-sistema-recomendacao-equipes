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
> do treinamento (tabela do `compare_models()`, métricas obtidas, parâmetros das
> distribuições sintéticas). **Nenhum valor foi estimado ou inventado** — preencher
> esses campos com dados reais é indispensável.
>
> Os valores das seções 3 e 4 provêm de uma execução real do protótipo, reproduzível
> pelo roteiro `avaliacao/cenarios.py` do repositório, com 100 sorteios por cenário e
> semente 42.
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
organizada em cinco conjuntos de operações. O primeiro compreende a manutenção do
catálogo de competências, atribuição do perfil Administrador. O segundo compreende o
cadastro e a atualização dos perfis de desenvolvedores, incluindo a declaração das
competências dominadas e o ajuste da disponibilidade semanal — operação acessível ao
próprio profissional, conforme previsto no diagrama de casos de uso. O terceiro
compreende o cadastro dos projetos e a definição de seus requisitos, atribuição do
perfil Gestor.

O quarto conjunto reúne as operações de recomendação: a geração de uma nova
recomendação para um projeto, a consulta ao histórico de recomendações já produzidas, o
cálculo da linha de base aleatória utilizada na avaliação e a confirmação da equipe
proposta. O quinto trata das alocações, permitindo alocar um profissional
individualmente, consultar a equipe efetivamente alocada em um projeto e encerrar uma
alocação, liberando as horas correspondentes.

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
base relacional são convertidos nas variáveis de entrada esperadas pelo modelo e como o
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

O orquestrador recupera os requisitos do projeto e o conjunto de profissionais ativos,
constrói as variáveis de entrada correspondentes a cada candidato e submete o conjunto,
em um único lote, ao serviço de predição. Este carrega o modelo previamente treinado,
verifica se todos os valores recebidos pertencem ao domínio utilizado no treinamento,
calcula o escore de afinidade de cada candidato e devolve os resultados em formato
JSON. De posse dos escores, o orquestrador ordena os candidatos, aplica as regras de
composição da equipe e calcula as métricas de avaliação. O resultado consolidado é
então encaminhado ao modelo de linguagem, que redige a justificativa qualitativa
apresentada ao gestor.

A verificação de domínio realizada pelo serviço de predição merece registro. Modelos
baseados em árvores de decisão não acusam erro diante de valores inesperados: um nível
de senioridade igual a 4 seria tratado silenciosamente como o extremo mais próximo,
devolvendo um escore plausível, porém sem respaldo no treinamento. Por isso, valores
fora das faixas observadas na base de treinamento são recusados de forma explícita, e o
serviço não entra em operação caso o arquivo do modelo não apresente exatamente as
treze variáveis esperadas.

### Construção das variáveis de entrada

A conversão entre a estrutura relacional e as treze variáveis esperadas pelo modelo é
apresentada na Tabela I. A correspondência reproduz, em tempo de execução, as mesmas
transformações de engenharia de atributos aplicadas durante o treinamento.

**Tabela I — Correspondência entre a base relacional e as variáveis do modelo**

| Variáveis | Origem na base relacional | Transformação aplicada |
|:---:|---|---|
| 1 | `Desenvolvedor.senioridade` | Escala ordinal crescente: júnior = 1, pleno = 2, sênior = 3 |
| 2–7 | `Desenvolvedor.papelPrincipal` | Codificação binária: valor 1 na coluna correspondente ao papel e 0 nas demais |
| 8 | `Desenvolvedor.softSkillsScore` | Valor direto, em escala de 0 a 5 |
| 9 | `Projeto.criticidade` | Escala ordinal: baixa = 1, média = 2, alta = 3 |
| 10 | `ProjetoPapelRequerido` | Requisito de backend: 1 se o projeto exige backend ou full-stack |
| 11 | `ProjetoPapelRequerido` | Requisito de frontend: 1 se o projeto exige frontend ou full-stack |
| 12 | `DesenvolvedorHabilidade` × `ProjetoHabilidadeRequerida` | Aderência técnica: soma dos pesos das tecnologias exigidas dominadas ÷ soma dos pesos exigidos |
| 13 | `Desenvolvedor.horasDisponiveis` menos as horas confirmadas em `Alocacao` | Cobertura de horas: horas livres ÷ horas exigidas pelo projeto, limitada a 1 |

As trinta colunas de tecnologias da matriz de treinamento não são submetidas
individualmente ao modelo: elas são resumidas na aderência técnica, que expressa o
quanto o conhecimento do profissional atende à necessidade específica daquele projeto.
Duas convenções completam a tabela. Quando o projeto não declara papéis exigidos, os
dois requisitos recebem valor 1, situação equivalente à de um projeto que demanda as
duas frentes. Quando não declara tecnologias, a aderência técnica recebe valor 1, de
modo que a ausência de exigência não penalize candidato algum.

As duas últimas linhas da Tabela I merecem destaque metodológico, por serem as únicas
variáveis relacionais, calculadas para o par formado pelo desenvolvedor e pelo projeto.
A cobertura de horas não corresponde à disponibilidade declarada no cadastro, mas à
diferença entre esse valor e o total de horas semanais já comprometidas em projetos
ativos. Dessa forma, um profissional já alocado em outras frentes apresenta cobertura
reduzida e tende a ocupar posições inferiores no ranqueamento. Esse mecanismo constitui
o tratamento adotado para o cenário de disputa por profissionais entre projetos
simultâneos.

### Regras de composição da equipe

O escore avalia cada candidato isoladamente, de modo que a formação da equipe exige
regras adicionais, aplicadas pelo orquestrador após o ranqueamento, em três etapas.
Na primeira, a **elegibilidade**, participam da equipe apenas os profissionais com
horas livres suficientes para atender à carga semanal exigida pelo projeto; os demais
permanecem no ranqueamento, identificados como inelegíveis. Na segunda, as **vagas por
papel**, cada vaga declarada é preenchida pelo candidato elegível de maior escore que
possui exatamente aquele papel. Na terceira, as **posições livres**, as vagas restantes
até o tamanho da equipe são preenchidas pelos maiores escores ainda não escolhidos,
independentemente do papel.

Quando não há elegíveis suficientes, o sistema propõe a melhor equipe possível e
registra as restrições encontradas, em vez de não apresentar resultado. A recomendação
não reduz a disponibilidade de ninguém: o desconto ocorre apenas na confirmação da
equipe pelo gestor, aplicada de forma integral — ou todos os selecionados são alocados,
ou nenhum o é.

### Exemplo de aplicação

Para ilustrar o percurso completo de uma recomendação, apresenta-se a execução
realizada no Cenário 1, descrito na seção seguinte. O projeto Sistema de Matrícula
Online, de criticidade média, exige cinco integrantes com 20 horas semanais cada,
distribuídos em duas vagas de backend, duas de frontend e uma de QA. A stack requerida
reúne Java com peso 5, Spring Boot e React com peso 4 e PostgreSQL e Docker com peso 3,
totalizando 19 pontos de peso.

A Tabela II apresenta as variáveis submetidas ao modelo para três dos vinte candidatos
avaliados, escolhidos por ilustrarem situações distintas.

**Tabela II — Variáveis de entrada e escores de três candidatos do Cenário 1**

| Variável | Iara Teixeira | Ana Souza | Quezia Amaral |
|---|:---:|:---:|:---:|
| Senioridade | 3 | 3 | 2 |
| Papel de atuação | full-stack | backend | QA |
| Competências comportamentais | 4,40 | 4,60 | 4,00 |
| Criticidade do projeto | 2 | 2 | 2 |
| Requisito de backend | 1 | 1 | 1 |
| Requisito de frontend | 1 | 1 | 1 |
| Aderência técnica | 0,6316 | 0,7895 | 0,4211 |
| Cobertura de horas | 1,0 | 1,0 | 1,0 |
| **Escore de afinidade** | **88,03** | **86,66** | **45,98** |
| Posição no ranqueamento | 1ª | 2ª | 18ª |
| Selecionado | Não | Sim | Sim |

Os valores de aderência técnica decorrem diretamente da fórmula apresentada na Tabela
I. Ana Souza domina Java, Spring Boot, PostgreSQL e Docker, o que soma 15 dos 19 pontos
de peso exigidos, resultando em 0,7895. Iara Teixeira domina Java, React e PostgreSQL,
somando 12 pontos, ou 0,6316. Quezia Amaral domina Java e PostgreSQL, somando 8 pontos,
ou 0,4211. Como todos os candidatos do cenário possuíam as 20 horas semanais livres, a
cobertura de horas foi igual a 1 para todos, e a diferença entre os escores decorre dos
demais atributos.

O desfecho evidencia a precedência entre as regras de composição e o escore individual.
Iara Teixeira obteve o maior escore de todo o ranqueamento e não integrou a equipe,
enquanto Quezia Amaral, na décima oitava posição, foi selecionada. Como as cinco vagas
estavam integralmente vinculadas a papéis específicos, não restou posição livre para a
candidata full-stack, e a vaga de QA precisava ser ocupada por alguém daquele papel. Ao
todo, treze candidatos com escore superior ao de Quezia Amaral ficaram de fora da
equipe. Esse é precisamente o tipo de decisão que demanda justificativa ao gestor, razão
pela qual o ranqueamento completo, e não apenas a equipe escolhida, é encaminhado ao
modelo de linguagem.

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
aleatória de equipes**. Em cada cenário, foram sorteadas 100 equipes com o mesmo
tamanho definido para o projeto, escolhidas apenas entre os profissionais elegíveis,
isto é, entre os mesmos candidatos a partir dos quais o protótipo forma a sua equipe.
Cada composição sorteada foi avaliada pelas mesmas métricas, e a média dos 100 sorteios
constitui o valor de referência apresentado nas tabelas a seguir, acompanhada do
respectivo desvio padrão. O sorteio utilizou semente fixa, o que permite reproduzir
exatamente os mesmos resultados.

A escolha da composição aleatória como linha de base decorre da ausência de registros
históricos de alocações reais. Trata-se, portanto, de uma referência conservadora:
demonstra que o desempenho do protótipo não é atribuível ao acaso, embora não permita
compará-lo diretamente ao desempenho de gestores humanos.

As métricas empregadas são as três definidas na Seção III-B: cobertura de papéis,
cobertura de tecnologias e dispersão interna da equipe, esta última expressa pelo
desvio padrão da senioridade dos integrantes. Nas duas primeiras, valores mais
elevados indicam melhor desempenho; na terceira, valores menores indicam equipes mais
niveladas. De forma complementar, registrou-se também o escore de afinidade médio.

Os quatro cenários foram executados sobre o protótipo em funcionamento, com o serviço
de predição carregando o modelo efetivamente treinado. Os profissionais e os projetos
foram cadastrados pela própria interface de programação do sistema, por meio de um
roteiro automatizado, e cada cenário utilizou uma base isolada, de modo que os perfis
de um cenário não influenciassem o ranqueamento do seguinte. Os perfis são fictícios e
foram construídos para representar as situações descritas, uma vez que a pesquisa não
dispõe de dados reais de profissionais.

O dimensionamento das bases seguiu um critério: manter o número de elegíveis entre três
e quatro vezes maior que o tamanho da equipe. Quando os elegíveis são em número igual
ao das vagas, todo sorteio devolve a mesma composição, o desvio padrão da linha de base
é nulo e a comparação perde o poder de discriminação. A Tabela III resume a
configuração de cada cenário.

**Tabela III — Configuração dos cenários de avaliação**

| Cenário | Projeto | Criticidade | Equipe | Horas | Base (elegíveis) |
|:---:|---|:---:|:---:|:---:|:---:|
| 1 | Sistema de Matrícula Online | Média | 5 | 20 | 20 (20) |
| 2 | Plataforma de Telemedicina | Alta | 4 | 20 | 20 (20) |
| 3 | Migração de Sistema Legado | Alta | 5 | 30 | 14 (3) |
| 4 | Portal do Paciente e Aplicativo de Agendamento | Alta e Média | 4 e 4 | 20 | 12 (12) |

Os quatro cenários compartilham a mesma stack exigida: Java e Spring Boot, com pesos 5
e 4, ambas obrigatórias, React com peso 4, também obrigatória, além de PostgreSQL e
Docker, com peso 3 e não obrigatórias. A inclusão de uma tecnologia dominada por apenas
parte dos perfis é deliberada: em uma base na qual todos dominam todas as tecnologias
exigidas, a cobertura de tecnologias atinge o valor máximo em qualquer composição e
deixa de distinguir as abordagens.

### Cenário 1 — Formação de equipes restritas a papéis mínimos obrigatórios

Neste cenário, avalia-se a capacidade do protótipo de compor equipes que atendam
integralmente às posições exigidas pelo escopo do projeto. O projeto exige cinco
integrantes, distribuídos em duas vagas de backend, duas de frontend e uma de QA, com
20 horas semanais por integrante. A base reúne 20 profissionais, todos elegíveis,
distribuídos de forma desbalanceada entre os papéis: oito de backend, cinco
full-stack, três de frontend, dois de QA e dois de DevOps. O desbalanceamento reproduz
uma situação comum, na qual os papéis menos numerosos são justamente os exigidos pelo
projeto.

**Tabela IV — Resultados do Cenário 1**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | 100,00 | 59,60 ± 16,97 |
| Cobertura de tecnologias (%) | 100,00 | 93,84 ± 8,52 |
| Desvio padrão da senioridade | 0,490 | 0,702 ± 0,148 |
| Escore médio da equipe | 69,674 | 62,882 ± 5,690 |

O protótipo preencheu as cinco vagas com profissionais do papel exigido, alcançando
cobertura de papéis de 100%, enquanto a composição aleatória cobriu, em média, 59,60%
das vagas — o equivalente a três das cinco posições. A diferença corresponde a
aproximadamente 2,4 desvios padrão acima da média dos sorteios, o que indica que o
resultado não é atribuível ao acaso. A equipe proposta reuniu dois profissionais
sêniores de backend, um sênior e um pleno de frontend e um pleno de QA. O detalhamento
dessa execução, incluindo as variáveis submetidas ao modelo, encontra-se no exemplo de
aplicação da seção anterior.

### Cenário 2 — Balanceamento de experiência entre os integrantes

Neste cenário, verifica-se o nivelamento da equipe recomendada, com atenção à
possibilidade de composições formadas majoritariamente por profissionais de um mesmo
nível de senioridade. O projeto é de criticidade alta, com quatro integrantes, duas
vagas de backend e uma de frontend, restando uma posição livre. A base reúne 20
profissionais elegíveis, com predominância de perfis menos experientes: dez júniores,
seis plenos e quatro sêniores.

**Tabela V — Resultados do Cenário 2**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | 100,00 | 78,33 ± 20,21 |
| Cobertura de tecnologias (%) | 100,00 | 88,37 ± 11,05 |
| Desvio padrão da senioridade | 0,000 | 0,631 ± 0,251 |
| Escore médio da equipe | 86,491 | 61,474 ± 7,625 |

Em um projeto de criticidade alta, no qual a senioridade e as competências
comportamentais recebem multiplicadores de peso mais elevados, o modelo posicionou os
quatro profissionais sêniores no topo do ranqueamento, e a equipe resultante foi
integralmente composta por eles. O desvio padrão da senioridade foi nulo, contra 0,631
da média dos sorteios, e o escore médio superou a linha de base em cerca de 3,3 desvios
padrão.

O resultado confirma o comportamento esperado, mas expõe um efeito que merece registro:
como a métrica de dispersão premia equipes homogêneas, uma equipe inteiramente sênior
obtém o melhor valor possível. A métrica mede o nivelamento interno, e não a
diversidade de experiência, de modo que, em organizações nas quais a formação de
profissionais menos experientes seja um objetivo, ela precisaria ser complementada por
outro critério.

### Cenário 3 — Recomendação em contexto de escassez e disponibilidade desbalanceada

Neste cenário, o número de profissionais com disponibilidade suficiente é inferior ao
necessário para a composição integral da equipe. Avalia-se a capacidade do protótipo
de propor a melhor composição possível e de sinalizar explicitamente a restrição
encontrada, em vez de retornar ausência de resultado. O projeto exige cinco integrantes
com dedicação de 30 horas semanais, distribuídos em três vagas de backend, uma de
frontend e uma de QA. A base reúne 14 profissionais, dos quais apenas três possuem as
30 horas livres exigidas.

**Tabela VI — Resultados do Cenário 3**

| Métrica | Protótipo | Linha de base (aleatória) |
|---|:---:|:---:|
| Cobertura de papéis (%) | 60,00 | 60,00 ± 0,00 |
| Cobertura de tecnologias (%) | 100,00 | 100,00 ± 0,00 |
| Desvio padrão da senioridade | 0,471 | 0,471 ± 0,00 |
| Escore médio da equipe | 73,958 | 73,958 ± 0,00 |
| Integrantes alocados / previstos | 3/5 | 3/5 |

O protótipo propôs uma equipe parcial, com três dos cinco integrantes previstos, e
registrou a restrição encontrada: equipe parcial de três de cinco integrantes, onze
candidatos sem as 30 horas semanais livres exigidas e duas vagas sem profissional do
papel exigido, uma de backend e uma de QA. A cobertura de papéis de 60% corresponde às
três vagas efetivamente preenchidas.

A coincidência entre o protótipo e a linha de base decorre da própria natureza do
cenário. Com apenas três profissionais elegíveis para cinco vagas, existe uma única
composição possível, e o sorteio devolve sempre essa mesma equipe, razão pela qual
todos os desvios padrão são nulos. Em um contexto de escassez, portanto, não há
critério de escolha a ser comparado: o que o cenário avalia é a capacidade do sistema
de entregar uma proposta útil, acompanhada da descrição precisa do que faltou, em vez
de recusar a solicitação.

### Cenário 4 — Conflito de alocação entre projetos simultâneos

Neste cenário, dois projetos disputam o mesmo conjunto de profissionais. Avalia-se o
efeito do desconto das horas já comprometidas sobre o ranqueamento dos candidatos.
Ambos os projetos exigem quatro integrantes com 20 horas semanais, duas vagas de
backend e uma de frontend, sobre uma base comum de 12 profissionais com 30 horas
disponíveis cada. O Portal do Paciente, de criticidade alta, é tratado como Projeto A e
tem sua equipe confirmada primeiro; o Aplicativo de Agendamento, de criticidade média,
é o Projeto B.

**Tabela VII — Resultados do Cenário 4**

| Métrica | Projeto A | Projeto B |
|---|:---:|:---:|
| Cobertura de papéis (%) | 100,00 | 100,00 |
| Cobertura de tecnologias (%) | 100,00 | 84,21 |
| Desvio padrão da senioridade | 0,000 | 0,000 |
| Escore médio da equipe | 85,952 | 68,518 |
| Profissionais em sobreposição, antes da confirmação | 4 | 4 |
| Profissionais em sobreposição, após a confirmação | 0 | 0 |

Antes de qualquer confirmação, as equipes propostas para os dois projetos eram
idênticas: os mesmos quatro profissionais ocupavam as quatro posições em ambas, o que
caracteriza sobreposição integral. Confirmada a equipe do Projeto A, cada um de seus
integrantes passou a ter 20 das 30 horas comprometidas, restando 10 horas livres, valor
inferior às 20 exigidas pelo Projeto B. Esses profissionais tornaram-se inelegíveis, e
a nova recomendação do Projeto B foi composta por quatro profissionais distintos, sem
nenhuma sobreposição com a equipe já confirmada.

A tentativa de alocar manualmente ao Projeto B um profissional já comprometido com o
Projeto A foi recusada pelo sistema, com a indicação das horas ainda livres e das horas
exigidas pela alocação. O mecanismo atua, portanto, nas duas vias: na recomendação
automática, removendo o candidato do conjunto elegível, e na alocação manual,
bloqueando a operação.

O Projeto B manteve cobertura de papéis de 100%, o que demonstra que o desconto de
horas não impediu a formação de uma equipe completa. A diferença aparece nas demais
métricas: a cobertura de tecnologias caiu de 100% para 84,21% e o escore médio, de
85,952 para 68,518, porque os perfis mais experientes — os mesmos que dominavam a
tecnologia de menor difusão na base — já estavam comprometidos. O resultado evidencia
que a ordem de confirmação tem consequência real sobre a qualidade das equipes
subsequentes, informação que o sistema torna visível ao gestor em vez de mantê-la
implícita.

### Discussão dos resultados

Nos três cenários em que havia escolha a ser feita, o protótipo superou a composição
aleatória em todas as métricas avaliadas, e em nenhum cenário apresentou desempenho
inferior ao da linha de base. As diferenças mais expressivas concentram-se na cobertura
de papéis, de 100% contra 59,60% no Cenário 1 e contra 78,33% no Cenário 2, e no escore
médio da equipe, cuja distância chegou a 3,3 desvios padrão no Cenário 2. Como ambas as
abordagens partiram do mesmo conjunto de candidatos elegíveis, essas diferenças
refletem apenas o critério de seleção.

Os Cenários 3 e 4 avaliam comportamentos que não se reduzem a uma comparação numérica.
No primeiro, o sistema entregou uma equipe parcial acompanhada da descrição das
restrições, em vez de recusar a solicitação — conduta adequada a um instrumento de
apoio à decisão, no qual a informação sobre o que falta é tão relevante quanto a
sugestão em si. No segundo, o desconto das horas confirmadas eliminou por completo a
sobreposição entre equipes de projetos concorrentes, sem que isso impedisse a formação
integral da segunda equipe.

Em conjunto, os resultados indicam que a integração entre o motor preditivo e as regras
de composição produziu recomendações consistentes com os requisitos declarados,
rastreáveis até os valores que as originaram, e superiores a alocações não orientadas
por dados. Essa conclusão está condicionada a três limitações. A primeira é a natureza
sintética da variável alvo: o escore aproxima a função de pontuação proposta neste
trabalho, não um histórico real de sucesso de equipes. A segunda é o caráter fictício
dos perfis utilizados nos cenários. A terceira é a própria linha de base: a composição
aleatória demonstra que o desempenho do protótipo não decorre do acaso, mas não
equivale ao desempenho de um gestor experiente, que é o ponto de comparação relevante
em um ambiente organizacional.

---

## Complementos à subseção *2) Modelo de Aprendizagem*

> *Os trechos abaixo complementam a subseção já redigida. Recomenda-se inseri-los
> nos pontos indicados.*

### Complemento A — Dimensionamento do conjunto de dados

> *Inserir após o parágrafo que descreve a filtragem dos profissionais.*

Após a filtragem por área de atuação, o conjunto de dados resultante reuniu 33.462
registros de profissionais. A aplicação das técnicas de engenharia de atributos
produziu uma matriz de 13 variáveis independentes, assim distribuídas: uma variável
ordinal correspondente ao nível de senioridade, seis variáveis binárias
correspondentes aos papéis de atuação, uma variável contínua correspondente ao escore
de competências comportamentais, uma variável ordinal correspondente à criticidade do
projeto, duas variáveis binárias correspondentes aos requisitos de backend e de
frontend, e duas variáveis contínuas relacionais — a aderência técnica e a cobertura de
horas — calculadas para cada par de desenvolvedor e projeto.

As trinta colunas binárias de tecnologias extraídas da pesquisa não integram
diretamente a matriz: elas são resumidas na aderência técnica. Essa decisão reduz a
dimensionalidade do problema e, sobretudo, preserva o significado de cada variável, uma
vez que o domínio de uma tecnologia só é informativo quando confrontado com a exigência
concreta de um projeto.

### Complemento B — Parâmetros dos dados sintéticos

> *Inserir após o parágrafo que descreve a injeção de dados sintéticos.*

A geração dos atributos ausentes na base pública seguiu distribuições normais com os
parâmetros apresentados na Tabela `[[ AJUSTAR NUMERAÇÃO ]]`. Os valores gerados foram
truncados aos limites de cada escala, de modo a preservar a validade do domínio.

| Atributo | Distribuição | Média | Desvio padrão | Limites |
|---|:---:|:---:|:---:|:---:|
| Escore de competências comportamentais | Normal | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | 0 a 5 |
| Disponibilidade semanal (horas) | Normal | `[[ PREENCHER ]]` | `[[ PREENCHER ]]` | 0 a 60 |

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
S(d, p) = w_p · P(d, p) + w_t(c) · T(d, p) + w_s(c) · N(d) + w_c(c) · C(d) + w_h · H(d, p) + ε
```

na qual `S(d, p)` representa o escore de afinidade do desenvolvedor `d` em relação ao
projeto `p`; `P(d, p)` a aderência do papel de atuação, que vale 1 para o papel exato,
0,7 para papéis próximos — como um full-stack em um projeto exclusivamente de backend —
e 0 para os demais; `T(d, p)` a aderência técnica, isto é, a proporção dos pesos das
tecnologias exigidas que o profissional domina; `N(d)` o nível de senioridade
normalizado; `C(d)` o escore de competências comportamentais normalizado; `H(d, p)` a
cobertura de horas; e `ε` um termo de ruído aleatório, incorporado para representar a
imprevisibilidade inerente ao comportamento humano.

Todos os componentes foram colocados na mesma escala, de 0 a 1, de modo que cada peso
represente a pontuação máxima que o atributo pode somar. Os coeficientes `w_t`, `w_s` e
`w_c` são função do nível de criticidade `c` do projeto e, somados aos pesos fixos do
papel e da disponibilidade, totalizam 100 pontos. Essa dependência operacionaliza a
definição dos três níveis de criticidade: à medida que a criticidade aumenta, cresce o
peso atribuído à senioridade e às competências comportamentais, em detrimento do peso
atribuído à aderência estritamente técnica.

| Nível de criticidade | `w_p` (papel) | `w_t` (técnica) | `w_s` (senioridade) | `w_c` (comportamental) | `w_h` (disponibilidade) |
|---|:---:|:---:|:---:|:---:|:---:|
| Baixa (Nível 1) | 25 | 30 | 15 | 15 | 15 |
| Média (Nível 2) | 25 | 25 | 20 | 15 | 15 |
| Alta (Nível 3) | 25 | 15 | 25 | 20 | 15 |

O termo de ruído `ε` foi amostrado de uma distribuição normal de média zero e desvio
padrão de 4 pontos.

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
sobre conjuntos de treinamento e teste divididos na proporção 80/20, com validação
cruzada em 10 partições. A Tabela `[[ AJUSTAR NUMERAÇÃO ]]` apresenta o desempenho dos
algoritmos avaliados, ordenados pelo Erro Quadrático Médio.

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

Como o ruído inserido na geração da variável alvo possui desvio padrão de 4 pontos, um
RMSE próximo desse valor indica que o erro restante corresponde ao próprio ruído, que
por definição não pode ser previsto. Para confirmar esse comportamento, as previsões do
modelo foram comparadas com a fórmula sem ruído em 3.000 casos sorteados, obtendo-se
erro médio absoluto de 1,08 ponto, o que demonstra que o algoritmo aprendeu a lógica de
pontuação em vez de memorizar os exemplos.

O procedimento foi executado em ambiente Python 3.10, com as bibliotecas PyCaret 3.3.2,
scikit-learn 1.4.2 e LightGBM 4.6.0, em
`[[ PREENCHER: Google Colab ou execução local ]]`.

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

A avaliação foi conduzida por meio de quatro cenários controlados, comparando as
equipes recomendadas a composições aleatórias formadas entre os mesmos candidatos
elegíveis. Nos cenários em que havia escolha a ser feita, o protótipo superou a linha
de base em todas as métricas: a cobertura de papéis alcançou 100%, contra 59,60% e
78,33% das composições aleatórias, e o escore médio da equipe chegou a 3,3 desvios
padrão acima da média dos sorteios. Nos demais cenários, verificou-se que o sistema
propõe equipes parciais acompanhadas da descrição das restrições, em vez de recusar a
solicitação, e que o desconto das horas confirmadas elimina a sobreposição de
profissionais entre projetos simultâneos, sem impedir a formação integral da segunda
equipe.

Entre as contribuições do trabalho, destaca-se a articulação entre duas abordagens
usualmente tratadas de forma isolada: o processamento analítico, responsável pelo
ranqueamento quantitativo dos candidatos, e a geração de linguagem natural,
responsável por tornar esse ranqueamento interpretável pelo gestor. Essa combinação
busca responder à limitação apontada na introdução, segundo a qual recomendações
produzidas exclusivamente por algoritmos preditivos carecem de transparência
metodológica.

Reconhecem-se, contudo, as limitações discutidas na Seção `[[ AJUSTAR REFERÊNCIA ]]`,
sobretudo a natureza sintética da variável dependente e de parte dos atributos
utilizados no treinamento, além do caráter fictício dos perfis empregados nos cenários
de avaliação. Tais limitações restringem o alcance das conclusões à validação técnica
da solução proposta, sem permitir inferências sobre o desempenho de equipes em contexto
organizacional real.

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
