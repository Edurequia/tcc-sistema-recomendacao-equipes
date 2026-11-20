#!/usr/bin/env python3
"""
Carga e execucao dos quatro cenarios de avaliacao do protótipo.

Cada cenario e uma configuracao controlada de base (profissionais) e projeto,
descrita em CENARIOS abaixo. O script cadastra a base pela propria API REST,
gera a recomendacao, consulta a linha de base aleatoria e grava os resultados
em `resultados/`, ja no formato das tabelas do texto.

Pre-requisitos: motor de predicao na porta 8000 e backend na 8080, com a base
vazia (perfil `dev`, H2 em memoria, recem-iniciado). Ver README.md.

Uso:  python cenarios.py
Dependencias: nenhuma alem da biblioteca padrao.
"""

import json
import pathlib
import sys
import urllib.error
import urllib.request

BACKEND = "http://localhost:8080"
MOTOR = "http://localhost:8000"
SORTEIOS = 100
SEMENTE = 42

SAIDA = pathlib.Path(__file__).parent / "resultados"


# ---------------------------------------------------------------------------
# Perfis de competencia reutilizados pelos cenarios
# ---------------------------------------------------------------------------
BACK = [("Java", 4), ("Spring Boot", 4), ("PostgreSQL", 3)]
BACK_SR = [("Java", 5), ("Spring Boot", 5), ("PostgreSQL", 4), ("Docker", 3)]
BACK_JR = [("Java", 2), ("Spring Boot", 2)]
FRONT = [("React", 4), ("TypeScript", 4), ("HTML/CSS", 4)]
FRONT_JR = [("React", 2), ("HTML/CSS", 3)]
FULL = [("Java", 3), ("React", 4), ("PostgreSQL", 3)]
QA = [("Java", 2), ("PostgreSQL", 2)]
OPS = [("Docker", 4), ("Kubernetes", 3), ("AWS", 3)]

# (nome, senioridade, papel, softSkills, horasDisponiveis, competencias)
POOL_C1 = [
    ("Ana Souza",       "SENIOR", "BACKEND",   4.6, 40, BACK_SR),
    ("Bruno Lima",      "PLENO",  "BACKEND",   3.9, 40, BACK),
    ("Carla Reis",      "PLENO",  "BACKEND",   4.0, 40, BACK),
    ("Diego Alves",     "JUNIOR", "BACKEND",   3.2, 40, BACK_JR),
    ("Eva Martins",     "JUNIOR", "BACKEND",   3.0, 40, BACK_JR),
    ("Fabio Nunes",     "SENIOR", "BACKEND",   4.3, 40, BACK_SR),
    ("Gisele Prado",    "PLENO",  "BACKEND",   3.7, 40, BACK),
    ("Hugo Barros",     "JUNIOR", "BACKEND",   2.9, 40, BACK_JR),
    ("Iara Teixeira",   "SENIOR", "FULLSTACK", 4.4, 40, FULL),
    ("Joao Pedroso",    "PLENO",  "FULLSTACK", 3.8, 40, FULL),
    ("Karina Dutra",    "PLENO",  "FULLSTACK", 4.1, 40, FULL),
    ("Lucas Ferraz",    "JUNIOR", "FULLSTACK", 3.1, 40, FULL),
    ("Marina Costa",    "JUNIOR", "FULLSTACK", 3.3, 40, FULL),
    ("Nelson Rocha",    "SENIOR", "FRONTEND",  4.2, 40, FRONT),
    ("Olivia Ramos",    "PLENO",  "FRONTEND",  3.9, 40, FRONT),
    ("Paulo Vieira",    "JUNIOR", "FRONTEND",  3.0, 40, FRONT_JR),
    ("Quezia Amaral",   "PLENO",  "QA",        4.0, 40, QA),
    ("Rafael Moura",    "JUNIOR", "QA",        3.4, 40, QA),
    ("Sandra Vitoria",  "PLENO",  "DEVOPS",    3.6, 40, OPS),
    ("Tiago Bastos",    "SENIOR", "DEVOPS",    4.1, 40, OPS),
]

POOL_C2 = [
    ("Alice Bender",    "SENIOR", "BACKEND",   4.5, 40, BACK_SR),
    ("Breno Cardoso",   "SENIOR", "BACKEND",   4.4, 40, BACK_SR),
    ("Cecilia Duarte",  "SENIOR", "FRONTEND",  4.3, 40, FRONT),
    ("Davi Esteves",    "SENIOR", "FULLSTACK", 4.6, 40, FULL),
    ("Elisa Faria",     "PLENO",  "BACKEND",   3.9, 40, BACK),
    ("Felipe Gomes",    "PLENO",  "BACKEND",   3.8, 40, BACK),
    ("Giovana Horta",   "PLENO",  "FRONTEND",  4.0, 40, FRONT),
    ("Henrique Iglesias", "PLENO", "FRONTEND", 3.7, 40, FRONT),
    ("Isabela Jardim",  "PLENO",  "FULLSTACK", 3.9, 40, FULL),
    ("Juliano Klein",   "PLENO",  "QA",        3.6, 40, QA),
    ("Larissa Muniz",   "JUNIOR", "BACKEND",   3.2, 40, BACK_JR),
    ("Marcelo Nunes",   "JUNIOR", "BACKEND",   3.0, 40, BACK_JR),
    ("Natalia Osorio",  "JUNIOR", "BACKEND",   3.1, 40, BACK_JR),
    ("Otavio Pires",    "JUNIOR", "BACKEND",   2.8, 40, BACK_JR),
    ("Priscila Quadros", "JUNIOR", "FRONTEND", 3.3, 40, FRONT_JR),
    ("Renan Sobral",    "JUNIOR", "FRONTEND",  3.0, 40, FRONT_JR),
    ("Silvia Tavares",  "JUNIOR", "FRONTEND",  3.2, 40, FRONT_JR),
    ("Thiago Urbano",   "JUNIOR", "FULLSTACK", 2.9, 40, FULL),
    ("Ursula Vargas",   "JUNIOR", "FULLSTACK", 3.1, 40, FULL),
    ("Vinicius Weber",  "JUNIOR", "QA",        3.0, 40, QA),
]

# Escassez: so tres profissionais tem as 30 horas semanais exigidas.
POOL_C3 = [
    ("Adriana Lacerda", "SENIOR", "BACKEND",   4.5, 40, BACK_SR),
    ("Bernardo Maia",   "PLENO",  "BACKEND",   3.9, 35, BACK),
    ("Clarice Neves",   "PLENO",  "FRONTEND",  4.0, 30, FRONT),
    ("Douglas Ortiz",   "SENIOR", "BACKEND",   4.4, 25, BACK_SR),
    ("Erica Paiva",     "SENIOR", "FRONTEND",  4.2, 20, FRONT),
    ("Flavio Queiroz",  "PLENO",  "BACKEND",   3.8, 20, BACK),
    ("Gabriela Rangel", "PLENO",  "FULLSTACK", 3.9, 18, FULL),
    ("Heitor Sales",    "PLENO",  "QA",        3.7, 15, QA),
    ("Ines Tavora",     "JUNIOR", "BACKEND",   3.1, 15, BACK_JR),
    ("Jonas Uchoa",     "JUNIOR", "FRONTEND",  3.0, 12, FRONT_JR),
    ("Kelly Vasques",   "JUNIOR", "FULLSTACK", 3.2, 10, FULL),
    ("Leandro Xavier",  "JUNIOR", "QA",        2.9, 10, QA),
    ("Miriam Yamada",   "PLENO",  "DEVOPS",    3.6, 10, OPS),
    ("Norton Zilli",    "JUNIOR", "BACKEND",   2.8, 8,  BACK_JR),
]

# Conflito: todos com 30h. Quem for alocado em A (20h) fica com 10h livres
# e, portanto, inelegivel para B, que tambem exige 20h por integrante.
POOL_C4 = [
    ("Aurora Bilac",    "SENIOR", "BACKEND",   4.5, 30, BACK_SR),
    ("Caio Drummond",   "SENIOR", "BACKEND",   4.3, 30, BACK_SR),
    ("Dalva Eiras",     "PLENO",  "BACKEND",   3.9, 30, BACK),
    ("Elio Freyre",     "PLENO",  "BACKEND",   3.8, 30, BACK),
    ("Fatima Guedes",   "JUNIOR", "BACKEND",   3.1, 30, BACK_JR),
    ("Gustavo Holanda", "SENIOR", "FRONTEND",  4.2, 30, FRONT),
    ("Helena Iglesias", "PLENO",  "FRONTEND",  3.9, 30, FRONT),
    ("Ivo Jobim",       "JUNIOR", "FRONTEND",  3.0, 30, FRONT_JR),
    ("Julia Krause",    "SENIOR", "FULLSTACK", 4.4, 30, FULL),
    ("Kleber Lisboa",   "PLENO",  "FULLSTACK", 3.7, 30, FULL),
    ("Laura Menezes",   "PLENO",  "QA",        3.8, 30, QA),
    ("Murilo Nobre",    "JUNIOR", "QA",        3.0, 30, QA),
]

# Docker entra na stack de proposito: e o requisito que nem todo perfil da base
# domina. Sem um item assim, quase qualquer equipe sorteada cobriria a stack
# inteira e a cobertura de tecnologias nao separaria nada.
STACK_WEB = [("Java", True, 5), ("Spring Boot", True, 4), ("React", True, 4),
             ("PostgreSQL", False, 3), ("Docker", False, 3)]

CRITICIDADE = {"BAIXA": "baixa", "MEDIA": "média", "ALTA": "alta"}

CENARIOS = {
    1: {
        "titulo": "Formacao de equipes restritas a papeis minimos obrigatorios",
        "pool": POOL_C1,
        "projeto": {
            "nome": "Sistema de Matricula Online",
            "descricao": "Reescrita do modulo de matricula, com portal do aluno e area administrativa.",
            "criticidade": "MEDIA", "tamanhoEquipe": 5, "horasSemanaisPorIntegrante": 20,
            "dataInicioPrevista": "2026-10-01", "dataFimPrevista": "2027-03-31",
            "papeis": [("BACKEND", 2), ("FRONTEND", 2), ("QA", 1)],
            "stack": STACK_WEB,
        },
    },
    2: {
        "titulo": "Balanceamento de experiencia entre os integrantes",
        "pool": POOL_C2,
        "projeto": {
            "nome": "Plataforma de Telemedicina",
            "descricao": "Prontuario eletronico e teleconsulta, com requisitos regulatorios.",
            "criticidade": "ALTA", "tamanhoEquipe": 4, "horasSemanaisPorIntegrante": 20,
            "dataInicioPrevista": "2026-10-01", "dataFimPrevista": "2027-06-30",
            "papeis": [("BACKEND", 2), ("FRONTEND", 1)],
            "stack": STACK_WEB,
        },
    },
    3: {
        "titulo": "Recomendacao em contexto de escassez e disponibilidade desbalanceada",
        "pool": POOL_C3,
        "projeto": {
            "nome": "Migracao de Sistema Legado",
            "descricao": "Migracao de sistema critico, com dedicacao semanal elevada.",
            "criticidade": "ALTA", "tamanhoEquipe": 5, "horasSemanaisPorIntegrante": 30,
            "dataInicioPrevista": "2026-10-01", "dataFimPrevista": "2027-09-30",
            "papeis": [("BACKEND", 3), ("FRONTEND", 1), ("QA", 1)],
            "stack": STACK_WEB,
        },
    },
    4: {
        "titulo": "Conflito de alocacao entre projetos simultaneos",
        "pool": POOL_C4,
        "projetoA": {
            "nome": "Portal do Paciente",
            "descricao": "Projeto A do cenario de conflito: equipe formada e confirmada primeiro.",
            "criticidade": "ALTA", "tamanhoEquipe": 4, "horasSemanaisPorIntegrante": 20,
            "dataInicioPrevista": "2026-10-01", "dataFimPrevista": "2027-03-31",
            "papeis": [("BACKEND", 2), ("FRONTEND", 1)],
            "stack": STACK_WEB,
        },
        "projetoB": {
            "nome": "Aplicativo de Agendamento",
            "descricao": "Projeto B do cenario de conflito: disputa os mesmos profissionais.",
            "criticidade": "MEDIA", "tamanhoEquipe": 4, "horasSemanaisPorIntegrante": 20,
            "dataInicioPrevista": "2026-11-01", "dataFimPrevista": "2027-04-30",
            "papeis": [("BACKEND", 2), ("FRONTEND", 1)],
            "stack": STACK_WEB,
        },
    },
}


# ---------------------------------------------------------------------------
# Cliente HTTP
# ---------------------------------------------------------------------------
class ErroApi(RuntimeError):
    pass


def chamar(metodo, caminho, corpo=None, base=BACKEND, esperado=None):
    dados = json.dumps(corpo).encode() if corpo is not None else (b"" if metodo == "POST" else None)
    req = urllib.request.Request(base + caminho, data=dados, method=metodo,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resposta:
            bruto = resposta.read().decode()
            status, corpo_resposta = resposta.status, (json.loads(bruto) if bruto else None)
    except urllib.error.HTTPError as erro:
        bruto = erro.read().decode()
        status, corpo_resposta = erro.code, (json.loads(bruto) if bruto else bruto)
    except urllib.error.URLError as erro:
        raise ErroApi(f"{metodo} {caminho}: servico inacessivel ({erro.reason}).") from erro

    if esperado is not None and status != esperado:
        raise ErroApi(f"{metodo} {caminho} devolveu {status}, esperado {esperado}:\n"
                      f"{json.dumps(corpo_resposta, indent=2, ensure_ascii=False)}")
    return status, corpo_resposta


def conferir_ambiente():
    _, saude = chamar("GET", "/health", base=MOTOR, esperado=200)
    print(f"  motor de predicao: {saude}")
    chamar("GET", "/actuator/health", esperado=200)
    _, pagina = chamar("GET", "/api/desenvolvedores?size=1", esperado=200)
    if pagina.get("totalElementos", 0) > 0:
        raise ErroApi("A base ja tem desenvolvedores cadastrados. Reinicie o backend no perfil "
                      "`dev` (H2 em memoria) para rodar os cenarios em uma base limpa.")


def catalogo():
    _, pagina = chamar("GET", "/api/habilidades?tipo=HARD", esperado=200)
    itens = pagina["conteudo"] if isinstance(pagina, dict) else pagina
    return {item["nome"]: item["id"] for item in itens}


# ---------------------------------------------------------------------------
# Montagem de um cenario
# ---------------------------------------------------------------------------
def cadastrar_pool(numero, pool, hab):
    ids = []
    for indice, (nome, senioridade, papel, soft, horas, competencias) in enumerate(pool, start=1):
        _, dev = chamar("POST", "/api/desenvolvedores", {
            "nome": nome,
            "email": f"c{numero}.dev{indice:02d}@teamrec.local",
            "senioridade": senioridade,
            "papelPrincipal": papel,
            "softSkillsScore": soft,
            "horasDisponiveis": horas,
            "habilidades": [{"habilidadeId": hab[n], "proficiencia": p} for n, p in competencias],
        }, esperado=201)
        ids.append(dev["id"])
    return ids


def isolar(ids_do_cenario):
    """Mantem ativos apenas os profissionais do cenario corrente.

    A recomendacao avalia todos os desenvolvedores ativos; sem isso, a base de
    um cenario contaminaria o seguinte.
    """
    _, pagina = chamar("GET", "/api/desenvolvedores?size=500", esperado=200)
    for dev in pagina["conteudo"]:
        if dev["id"] not in ids_do_cenario and dev.get("ativo", True):
            chamar("DELETE", f"/api/desenvolvedores/{dev['id']}", esperado=204)


def cadastrar_projeto(spec, hab):
    _, projeto = chamar("POST", "/api/projetos", {
        "nome": spec["nome"], "descricao": spec["descricao"],
        "criticidade": spec["criticidade"], "tamanhoEquipe": spec["tamanhoEquipe"],
        "horasSemanaisPorIntegrante": spec["horasSemanaisPorIntegrante"],
        "dataInicioPrevista": spec["dataInicioPrevista"], "dataFimPrevista": spec["dataFimPrevista"],
        "papeisRequeridos": [{"papel": p, "quantidade": q} for p, q in spec["papeis"]],
        "habilidadesRequeridas": [{"habilidadeId": hab[n], "obrigatoria": o, "peso": w}
                                  for n, o, w in spec["stack"]],
    }, esperado=201)
    return projeto


def recomendar(projeto_id):
    _, rec = chamar("POST", f"/api/projetos/{projeto_id}/recomendacoes", esperado=201)
    return rec


def linha_base(recomendacao_id):
    _, lb = chamar("GET", f"/api/recomendacoes/{recomendacao_id}/linha-base"
                          f"?sorteios={SORTEIOS}&semente={SEMENTE}", esperado=200)
    return lb


def selecionados(rec):
    return [m for m in rec["ranking"] if m["selecionado"]]


def elegiveis(rec):
    return [m for m in rec["ranking"] if m["elegivel"]]


def resumo_equipe(rec):
    return [f"{m['nome']} ({m['papelDescricao']}, {m['senioridadeDescricao']}, "
            f"score {m['scoreAfinidade']})" for m in selecionados(rec)]


# ---------------------------------------------------------------------------
# Execucao dos cenarios
# ---------------------------------------------------------------------------
def cenario_simples(numero, hab):
    spec = CENARIOS[numero]
    print(f"\n=== Cenario {numero} — {spec['titulo']} ===")
    ids = cadastrar_pool(numero, spec["pool"], hab)
    isolar(set(ids))
    projeto = cadastrar_projeto(spec["projeto"], hab)
    rec = recomendar(projeto["id"])
    lb = linha_base(rec["id"])

    print(f"  base: {len(ids)} profissionais · elegiveis: {len(elegiveis(rec))} · "
          f"equipe: {rec['integrantesSelecionados']}/{rec['tamanhoEquipe']}")
    for linha in resumo_equipe(rec):
        print(f"    - {linha}")
    print(f"  restricao: {rec.get('observacaoRestricao')}")
    return {"cenario": numero, "titulo": spec["titulo"], "projeto": projeto,
            "totalBase": len(ids), "recomendacao": rec, "linhaBase": lb}


def cenario_conflito(hab):
    spec = CENARIOS[4]
    print(f"\n=== Cenario 4 — {spec['titulo']} ===")
    ids = cadastrar_pool(4, spec["pool"], hab)
    isolar(set(ids))
    projeto_a = cadastrar_projeto(spec["projetoA"], hab)
    projeto_b = cadastrar_projeto(spec["projetoB"], hab)

    rec_a = recomendar(projeto_a["id"])
    rec_b_antes = recomendar(projeto_b["id"])
    antes_a = {m["desenvolvedorId"] for m in selecionados(rec_a)}
    antes_b = {m["desenvolvedorId"] for m in selecionados(rec_b_antes)}
    sobreposicao_antes = antes_a & antes_b

    chamar("POST", f"/api/recomendacoes/{rec_a['id']}/confirmacao", esperado=200)
    rec_b_depois = recomendar(projeto_b["id"])
    depois_b = {m["desenvolvedorId"] for m in selecionados(rec_b_depois)}
    sobreposicao_depois = antes_a & depois_b

    nomes = {m["desenvolvedorId"]: m["nome"] for m in rec_a["ranking"]}
    print(f"  equipe A: {[nomes[i] for i in sorted(antes_a)]}")
    print(f"  equipe B antes da confirmacao: {[nomes[i] for i in sorted(antes_b)]}")
    print(f"  sobreposicao antes: {len(sobreposicao_antes)} "
          f"{[nomes[i] for i in sorted(sobreposicao_antes)]}")
    print(f"  equipe B depois da confirmacao: {[nomes[i] for i in sorted(depois_b)]}")
    print(f"  sobreposicao depois: {len(sobreposicao_depois)}")

    # Tentativa manual de alocar em B alguem ja comprometido em A: deve dar 422.
    disputado = sorted(antes_a)[0]
    status, erro = chamar("POST", f"/api/projetos/{projeto_b['id']}/alocacoes",
                          {"desenvolvedorId": disputado, "horasSemanais":
                              spec["projetoB"]["horasSemanaisPorIntegrante"]})
    print(f"  alocacao manual de '{nomes[disputado]}' em B -> HTTP {status}: "
          f"{erro.get('mensagem') if isinstance(erro, dict) else erro}")

    return {"cenario": 4, "titulo": spec["titulo"], "totalBase": len(ids),
            "projetoA": projeto_a, "projetoB": projeto_b,
            "recomendacaoA": rec_a, "recomendacaoBAntes": rec_b_antes,
            "recomendacaoBDepois": rec_b_depois,
            "sobreposicaoAntes": sorted(nomes[i] for i in sobreposicao_antes),
            "sobreposicaoDepois": sorted(nomes[i] for i in sobreposicao_depois),
            "alocacaoManual": {"status": status, "corpo": erro},
            "linhaBaseA": linha_base(rec_a["id"]),
            "linhaBaseBDepois": linha_base(rec_b_depois["id"])}


# ---------------------------------------------------------------------------
# Tabelas
# ---------------------------------------------------------------------------
METRICAS = [
    ("Cobertura de papeis (%)", "coberturaPapeis", 2),
    ("Cobertura de tecnologias (%)", "coberturaTecnologias", 2),
    ("Desvio padrao da senioridade", "desvioPadraoSenioridade", 3),
    ("Escore medio da equipe", "scoreMedioEquipe", 3),
]


def fmt(valor, casas):
    return f"{valor:.{casas}f}".replace(".", ",")


def equipe_parcial(resultado):
    """A linha de integrantes so informa quando a equipe nao fecha."""
    rec = resultado["recomendacao"]
    return rec["integrantesSelecionados"] < rec["tamanhoEquipe"]


def tabela_md(resultado):
    rec, lb = resultado["recomendacao"], resultado["linhaBase"]
    linhas = ["| Metrica | Prototipo | Linha de base (aleatoria) |", "|---|:---:|:---:|"]
    for rotulo, chave, casas in METRICAS:
        linhas.append(f"| {rotulo} | {fmt(rec['metricas'][chave], casas)} | "
                      f"{fmt(lb['linhaBase']['media'][chave], casas)} ± "
                      f"{fmt(lb['linhaBase']['desvioPadrao'][chave], casas)} |")
    if equipe_parcial(resultado):
        linhas.append(f"| Integrantes alocados / previstos | "
                      f"{rec['integrantesSelecionados']}/{rec['tamanhoEquipe']} | "
                      f"{lb['tamanhoEquipeSorteada']}/{rec['tamanhoEquipe']} |")
    return "\n".join(linhas)


def tabela_tex(resultado, numero):
    rec, lb = resultado["recomendacao"], resultado["linhaBase"]
    corpo = []
    for rotulo, chave, casas in METRICAS:
        rotulo_tex = (rotulo.replace("papeis", "papéis").replace("Metrica", "Métrica")
                      .replace("Desvio padrao", "Desvio padrão").replace("medio", "médio"))
        corpo.append(f"{rotulo_tex} & {fmt(rec['metricas'][chave], casas)} & "
                     f"{fmt(lb['linhaBase']['media'][chave], casas)} $\\pm$ "
                     f"{fmt(lb['linhaBase']['desvioPadrao'][chave], casas)} \\\\")
    if equipe_parcial(resultado):
        corpo.append(f"Integrantes alocados / previstos & "
                     f"{rec['integrantesSelecionados']}/{rec['tamanhoEquipe']} & "
                     f"{lb['tamanhoEquipeSorteada']}/{rec['tamanhoEquipe']} \\\\")
    return ("\\begin{table}[htbp]\n\\centering\n"
            f"\\caption{{Resultados do Cenário {numero}}}\n"
            f"\\label{{tab:cenario{numero}}}\n"
            "\\begin{tabular}{lcc}\n\\hline\n"
            "\\textbf{Métrica} & \\textbf{Protótipo} & \\textbf{Linha de base} \\\\\n\\hline\n"
            + "\n".join(corpo) +
            "\n\\hline\n\\end{tabular}\n\\end{table}")


def tabela_conflito_md(r):
    a, b = r["recomendacaoA"], r["recomendacaoBDepois"]
    linhas = ["| Metrica | Projeto A | Projeto B |", "|---|:---:|:---:|"]
    for rotulo, chave, casas in METRICAS:
        linhas.append(f"| {rotulo} | {fmt(a['metricas'][chave], casas)} | "
                      f"{fmt(b['metricas'][chave], casas)} |")
    linhas.append(f"| Profissionais em sobreposicao (antes da confirmacao) | "
                  f"{len(r['sobreposicaoAntes'])} | {len(r['sobreposicaoAntes'])} |")
    linhas.append(f"| Profissionais em sobreposicao (apos a confirmacao) | "
                  f"{len(r['sobreposicaoDepois'])} | {len(r['sobreposicaoDepois'])} |")
    return "\n".join(linhas)


def configuracao_md(resultado):
    projeto = resultado["projeto"]
    papeis = ", ".join(f"{p['papelDescricao']} ({p['quantidade']})"
                       for p in projeto["papeisRequeridos"])
    stack = ", ".join(f"{h['nome']} (peso {h['peso']}"
                      f"{', obrigatória' if h['obrigatoria'] else ''})"
                      for h in projeto["habilidadesRequeridas"])
    rec = resultado["recomendacao"]
    return (f"Projeto **{projeto['nome']}**, criticidade {CRITICIDADE[projeto['criticidade']]}, "
            f"equipe de {projeto['tamanhoEquipe']} integrantes a "
            f"{projeto['horasSemanaisPorIntegrante']} h semanais. "
            f"Papéis exigidos: {papeis}. Stack: {stack}. "
            f"Base de {resultado['totalBase']} profissionais, "
            f"{len(elegiveis(rec))} deles elegíveis.")


def escrever_saidas(resultados):
    SAIDA.mkdir(exist_ok=True)
    for numero, resultado in resultados.items():
        (SAIDA / f"cenario{numero}.json").write_text(
            json.dumps(resultado, indent=2, ensure_ascii=False), encoding="utf-8")

    md = ["# Resultados dos cenarios de avaliacao",
          "",
          f"Gerado por `cenarios.py` · {SORTEIOS} sorteios por cenario · semente {SEMENTE}.",
          "Os valores saem de `GET /api/recomendacoes/{id}/linha-base`.",
          ""]
    tex = []
    for numero in (1, 2, 3):
        resultado = resultados[numero]
        md += [f"## Cenario {numero} — {resultado['titulo']}", "",
               configuracao_md(resultado), "",
               tabela_md(resultado), "",
               f"Equipe proposta: {'; '.join(resumo_equipe(resultado['recomendacao']))}.", "",
               f"Restricao registrada: "
               f"{(resultado['recomendacao'].get('observacaoRestricao') or 'nenhuma').rstrip('.')}.",
               ""]
        tex.append(tabela_tex(resultado, numero))

    conflito = resultados[4]
    md += [f"## Cenario 4 — {conflito['titulo']}", "",
           f"Projetos **{conflito['projetoA']['nome']}** (A, confirmado primeiro) e "
           f"**{conflito['projetoB']['nome']}** (B), ambos com equipe de "
           f"{conflito['projetoA']['tamanhoEquipe']} integrantes a "
           f"{conflito['projetoA']['horasSemanaisPorIntegrante']} h semanais, sobre a mesma base de "
           f"{conflito['totalBase']} profissionais com 30 h disponiveis cada.", "",
           tabela_conflito_md(conflito), "",
           f"Antes da confirmacao de A, as duas equipes propostas tinham "
           f"{len(conflito['sobreposicaoAntes'])} profissionais em comum: "
           f"{', '.join(conflito['sobreposicaoAntes']) or 'nenhum'}. "
           f"Apos a confirmacao, a sobreposicao caiu para "
           f"{len(conflito['sobreposicaoDepois'])}.", "",
           f"Alocacao manual de um profissional ja comprometido em A: HTTP "
           f"{conflito['alocacaoManual']['status']} — "
           f"{conflito['alocacaoManual']['corpo'].get('mensagem')}", ""]

    (SAIDA / "tabelas.md").write_text("\n".join(md), encoding="utf-8")
    (SAIDA / "tabelas.tex").write_text("\n\n".join(tex) + "\n", encoding="utf-8")
    print(f"\nGravado em {SAIDA}:")
    for arquivo in sorted(SAIDA.iterdir()):
        print(f"  {arquivo.name}")


def main():
    try:
        print("Conferindo ambiente...")
        conferir_ambiente()
        hab = catalogo()
        print(f"  catalogo: {len(hab)} tecnologias")

        resultados = {n: cenario_simples(n, hab) for n in (1, 2, 3)}
        resultados[4] = cenario_conflito(hab)
        escrever_saidas(resultados)
    except ErroApi as erro:
        print(f"\nFALHOU: {erro}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
