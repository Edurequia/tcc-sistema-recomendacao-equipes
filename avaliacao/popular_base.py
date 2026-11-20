"""Popula o backend com desenvolvedores e projetos ficticios para uso manual.

Diferente de cenarios.py, este script nao isola bases nem grava resultados:
apenas cadastra perfis e projetos variados pela API para que a tela de
recomendacao tenha com o que trabalhar. Pode ser executado de novo: e-mails e
nomes de projeto ja cadastrados sao ignorados.

Pre-requisito: backend em localhost:8080.

    python popular_base.py
"""

import sys

from cenarios import ErroApi, catalogo, chamar

BACK_SR = [("Java", 5), ("Spring Boot", 5), ("PostgreSQL", 4), ("Docker", 3)]
BACK = [("Java", 4), ("Spring Boot", 4), ("PostgreSQL", 3)]
BACK_JR = [("Java", 2), ("Spring Boot", 2)]
BACK_PY = [("Python", 4), ("FastAPI", 4), ("PostgreSQL", 3), ("Redis", 2)]
BACK_NODE = [("Node.js", 4), ("TypeScript", 4), ("MongoDB", 3)]
FRONT = [("React", 4), ("TypeScript", 4), ("HTML/CSS", 4)]
FRONT_VUE = [("Vue.js", 4), ("JavaScript", 4), ("HTML/CSS", 4)]
FRONT_JR = [("React", 2), ("HTML/CSS", 3), ("JavaScript", 2)]
FULL = [("Java", 3), ("Spring Boot", 3), ("React", 4), ("PostgreSQL", 3)]
FULL_JS = [("Node.js", 4), ("React", 4), ("Next.js", 3), ("TypeScript", 4)]
MOBILE = [("Flutter", 4), ("Dart", 4)]
MOBILE_NATIVO = [("Kotlin", 4), ("Swift", 3), ("React Native", 2)]
QA = [("Java", 2), ("Python", 3), ("PostgreSQL", 2)]
OPS = [("Docker", 4), ("Kubernetes", 3), ("AWS", 3)]
OPS_SR = [("Docker", 5), ("Kubernetes", 4), ("AWS", 4), ("Microsoft Azure", 3)]

# (nome, senioridade, papel, softSkills, horasDisponiveis, competencias)
DESENVOLVEDORES = [
    ("Amanda Siqueira",   "SENIOR", "BACKEND",   4.5, 40, BACK_SR),
    ("Bernardo Lopes",    "PLENO",  "BACKEND",   3.8, 40, BACK),
    ("Camila Fontes",     "PLENO",  "BACKEND",   4.1, 30, BACK_PY),
    ("Daniel Moreira",    "JUNIOR", "BACKEND",   3.3, 40, BACK_JR),
    ("Eduarda Pires",     "PLENO",  "BACKEND",   3.6, 20, BACK_NODE),
    ("Fernando Castro",   "SENIOR", "BACKEND",   4.0, 10, BACK_SR),
    ("Gabriela Neves",    "SENIOR", "FRONTEND",  4.6, 40, FRONT),
    ("Heitor Campos",     "PLENO",  "FRONTEND",  3.9, 40, FRONT),
    ("Isadora Melo",      "PLENO",  "FRONTEND",  4.2, 30, FRONT_VUE),
    ("Joaquim Teles",     "JUNIOR", "FRONTEND",  3.1, 40, FRONT_JR),
    ("Leticia Rangel",    "SENIOR", "FULLSTACK", 4.4, 40, FULL),
    ("Matheus Vidal",     "PLENO",  "FULLSTACK", 3.7, 40, FULL_JS),
    ("Nicole Batista",    "JUNIOR", "FULLSTACK", 3.4, 30, FULL),
    ("Otavio Correia",    "PLENO",  "MOBILE",    3.8, 40, MOBILE),
    ("Priscila Antunes",  "SENIOR", "MOBILE",    4.3, 30, MOBILE_NATIVO),
    ("Renato Guimaraes",  "JUNIOR", "MOBILE",    3.0, 40, MOBILE),
    ("Sabrina Coelho",    "PLENO",  "QA",        4.1, 40, QA),
    ("Thiago Peixoto",    "JUNIOR", "QA",        3.5, 40, QA),
    ("Ursula Machado",    "SENIOR", "DEVOPS",    4.2, 40, OPS_SR),
    ("Vinicius Cunha",    "PLENO",  "DEVOPS",    3.6, 20, OPS),
]

# stack: (tecnologia, obrigatoria, peso de 1 a 5)
PROJETOS = [
    {
        "nome": "Portal do Aluno",
        "descricao": "Portal web para consulta de notas, frequencia e rematricula.",
        "criticidade": "MEDIA", "tamanhoEquipe": 4, "horasSemanaisPorIntegrante": 20,
        "dataInicioPrevista": "2026-11-03", "dataFimPrevista": "2027-04-30",
        "papeis": [("BACKEND", 1), ("FRONTEND", 1), ("QA", 1)],
        "stack": [("Java", True, 5), ("Spring Boot", True, 4), ("React", True, 4),
                  ("PostgreSQL", False, 3), ("Docker", False, 2)],
    },
    {
        "nome": "Sistema de Prontuario Eletronico",
        "descricao": "Registro clinico de pacientes com requisitos de seguranca e auditoria.",
        "criticidade": "ALTA", "tamanhoEquipe": 5, "horasSemanaisPorIntegrante": 30,
        "dataInicioPrevista": "2026-11-17", "dataFimPrevista": "2027-08-31",
        "papeis": [("BACKEND", 2), ("FRONTEND", 1), ("QA", 1), ("DEVOPS", 1)],
        "stack": [("Java", True, 5), ("Spring Boot", True, 5), ("PostgreSQL", True, 4),
                  ("React", False, 3), ("Kubernetes", False, 3)],
    },
    {
        "nome": "Aplicativo de Delivery",
        "descricao": "Aplicativo mobile de pedidos com API propria e painel do restaurante.",
        "criticidade": "MEDIA", "tamanhoEquipe": 4, "horasSemanaisPorIntegrante": 20,
        "dataInicioPrevista": "2026-12-01", "dataFimPrevista": "2027-05-31",
        "papeis": [("MOBILE", 2), ("BACKEND", 1)],
        "stack": [("Flutter", True, 5), ("Dart", True, 4), ("Node.js", False, 3),
                  ("MongoDB", False, 2)],
    },
    {
        "nome": "Landing Page Institucional",
        "descricao": "Site institucional com blog e formulario de contato.",
        "criticidade": "BAIXA", "tamanhoEquipe": 2, "horasSemanaisPorIntegrante": 10,
        "dataInicioPrevista": "2026-10-26", "dataFimPrevista": "2026-12-18",
        "papeis": [("FRONTEND", 1)],
        "stack": [("Next.js", False, 4), ("React", True, 4), ("HTML/CSS", True, 3)],
    },
    {
        "nome": "API de Analise de Dados",
        "descricao": "Servico de ingestao e consulta de indicadores para paineis gerenciais.",
        "criticidade": "ALTA", "tamanhoEquipe": 3, "horasSemanaisPorIntegrante": 30,
        "dataInicioPrevista": "2027-01-11", "dataFimPrevista": "2027-06-30",
        "papeis": [("BACKEND", 2), ("DEVOPS", 1)],
        "stack": [("Python", True, 5), ("FastAPI", True, 4), ("PostgreSQL", True, 4),
                  ("Redis", False, 2), ("AWS", False, 3)],
    },
]


def popular_desenvolvedores(hab):
    _, pagina = chamar("GET", "/api/desenvolvedores?size=500", esperado=200)
    existentes = {dev["email"] for dev in pagina["conteudo"]}

    criados = 0
    for indice, (nome, senioridade, papel, soft, horas, competencias) in enumerate(DESENVOLVEDORES, 1):
        email = f"demo.dev{indice:02d}@teamrec.local"
        if email in existentes:
            continue
        chamar("POST", "/api/desenvolvedores", {
            "nome": nome,
            "email": email,
            "senioridade": senioridade,
            "papelPrincipal": papel,
            "softSkillsScore": soft,
            "horasDisponiveis": horas,
            "habilidades": [{"habilidadeId": hab[n], "proficiencia": p} for n, p in competencias],
        }, esperado=201)
        criados += 1
        print(f"  + {nome} ({senioridade}, {papel}, {horas} h)")

    print(f"{criados} desenvolvedores criados; {len(DESENVOLVEDORES) - criados} ja existiam.")


def popular_projetos(hab):
    _, pagina = chamar("GET", "/api/projetos?size=500", esperado=200)
    existentes = {projeto["nome"].lower() for projeto in pagina["conteudo"]}

    criados = 0
    for spec in PROJETOS:
        if spec["nome"].lower() in existentes:
            continue
        chamar("POST", "/api/projetos", {
            "nome": spec["nome"], "descricao": spec["descricao"],
            "criticidade": spec["criticidade"], "tamanhoEquipe": spec["tamanhoEquipe"],
            "horasSemanaisPorIntegrante": spec["horasSemanaisPorIntegrante"],
            "dataInicioPrevista": spec["dataInicioPrevista"], "dataFimPrevista": spec["dataFimPrevista"],
            "papeisRequeridos": [{"papel": p, "quantidade": q} for p, q in spec["papeis"]],
            "habilidadesRequeridas": [{"habilidadeId": hab[n], "obrigatoria": o, "peso": w}
                                      for n, o, w in spec["stack"]],
        }, esperado=201)
        criados += 1
        print(f"  + {spec['nome']} ({spec['criticidade']}, equipe de {spec['tamanhoEquipe']})")

    print(f"{criados} projetos criados; {len(PROJETOS) - criados} ja existiam.")


def main():
    try:
        hab = catalogo()
        popular_desenvolvedores(hab)
        popular_projetos(hab)
    except (ErroApi, KeyError) as erro:
        print(f"Falha: {erro}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
