"""Testes de contrato do motor de predicao.

Usam o modelo real (motor_predicao_tcc.pkl): o objetivo e garantir que o
servico entrega ao backend exatamente o que o modelo treinado responde,
e que entradas fora do dominio de treinamento sao recusadas antes de
chegar a ele.
"""

import copy

import pytest
from fastapi.testclient import TestClient

from app.main import create_app

# Exemplo usado na documentacao do treinamento: formula sem ruido = 90,75.
ANA = {
    "id": 1,
    "nivelSenioridade": 3,
    "papel": "Developer, back-end",
    "softSkillsScore": 4.0,
    "aderenciaTecnica": 0.75,
    "coberturaHoras": 0.9,
}

PROJETO_BACKEND_CRITICO = {
    "criticidade": 3,
    "requisitoBackend": True,
    "requisitoFrontend": False,
}


@pytest.fixture(scope="module")
def client():
    with TestClient(create_app()) as cliente:
        yield cliente


def corpo(candidatos=None, projeto=None):
    return {
        "projeto": projeto or dict(PROJETO_BACKEND_CRITICO),
        "candidatos": candidatos if candidatos is not None else [dict(ANA)],
    }


def score_de(client, **alteracoes):
    candidato = {**ANA, **alteracoes}
    resposta = client.post("/predicoes", json=corpo([candidato]))
    assert resposta.status_code == 200, resposta.text
    return resposta.json()["predicoes"][0]["score"]


# ---------------------------------------------------------------------------
# Saude e metadados
# ---------------------------------------------------------------------------

def test_health_informa_modelo_carregado(client):
    resposta = client.get("/health")

    assert resposta.status_code == 200
    assert resposta.json() == {"status": "UP", "modeloCarregado": True}


def test_modelo_expoe_colunas_e_papeis_aceitos(client):
    corpo_resposta = client.get("/modelo").json()

    assert corpo_resposta["algoritmo"] == "LGBMRegressor"
    assert len(corpo_resposta["colunas"]) == 13
    assert corpo_resposta["colunas"][-2:] == ["Aderencia_Tecnica", "Cobertura_Horas"]
    assert "Developer, full-stack" in corpo_resposta["papeisAceitos"]
    assert corpo_resposta["versoes"]["lightgbm"] == "4.6.0"


# ---------------------------------------------------------------------------
# Predicao
# ---------------------------------------------------------------------------

def test_predicao_reproduz_o_escore_do_modelo_treinado(client):
    resposta = client.post("/predicoes", json=corpo())

    assert resposta.status_code == 200
    json = resposta.json()
    assert json["algoritmo"] == "LGBMRegressor"
    assert json["predicoes"][0]["id"] == 1
    assert json["predicoes"][0]["score"] == pytest.approx(91.02, abs=0.05)


def test_predicao_preserva_a_ordem_dos_candidatos(client):
    candidatos = [
        {**ANA, "id": 30, "papel": "Developer, QA or test"},
        {**ANA, "id": 10},
        {**ANA, "id": 20, "nivelSenioridade": 1, "softSkillsScore": 1.0},
    ]

    predicoes = client.post("/predicoes", json=corpo(candidatos)).json()["predicoes"]

    assert [p["id"] for p in predicoes] == [30, 10, 20]
    # id 10 (papel exato) > id 30 (perde o papel) > id 20 (perde senioridade e soft skills)
    assert predicoes[1]["score"] > predicoes[0]["score"] > predicoes[2]["score"]


def test_cobertura_de_horas_influencia_o_escore(client):
    sem_tempo = score_de(client, coberturaHoras=0.0)
    cobre_tudo = score_de(client, coberturaHoras=1.0)

    assert cobre_tudo - sem_tempo > 10


def test_aderencia_tecnica_influencia_o_escore(client):
    assert score_de(client, aderenciaTecnica=1.0) - score_de(client, aderenciaTecnica=0.0) > 10


def test_papel_exato_supera_papel_adjacente(client):
    projeto_full = {"criticidade": 3, "requisitoBackend": True, "requisitoFrontend": True}
    candidatos = [
        {**ANA, "id": 1, "papel": "Developer, full-stack"},
        {**ANA, "id": 2, "papel": "Developer, back-end"},
    ]

    predicoes = client.post("/predicoes", json=corpo(candidatos, projeto_full)).json()["predicoes"]

    assert predicoes[0]["score"] > predicoes[1]["score"]


# ---------------------------------------------------------------------------
# Validacao: nada fora do dominio de treinamento chega ao modelo
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("campo, valor", [
    ("nivelSenioridade", 4),
    ("nivelSenioridade", 0),
    ("softSkillsScore", 5.5),
    ("aderenciaTecnica", 1.2),
    ("coberturaHoras", -0.1),
    ("papel", "Developer, embedded"),
])
def test_candidato_fora_do_dominio_e_recusado(client, campo, valor):
    candidato = {**ANA, campo: valor}

    resposta = client.post("/predicoes", json=corpo([candidato]))

    assert resposta.status_code == 422
    assert campo in resposta.text


@pytest.mark.parametrize("criticidade", [0, 4])
def test_criticidade_fora_do_dominio_e_recusada(client, criticidade):
    projeto = {**PROJETO_BACKEND_CRITICO, "criticidade": criticidade}

    assert client.post("/predicoes", json=corpo(projeto=projeto)).status_code == 422


def test_projeto_sem_nenhum_requisito_e_recusado(client):
    projeto = {"criticidade": 2, "requisitoBackend": False, "requisitoFrontend": False}

    resposta = client.post("/predicoes", json=corpo(projeto=projeto))

    assert resposta.status_code == 422
    assert "requisito" in resposta.text.lower()


def test_lista_vazia_de_candidatos_e_recusada(client):
    assert client.post("/predicoes", json=corpo([])).status_code == 422


def test_ids_repetidos_sao_recusados(client):
    candidatos = [dict(ANA), copy.deepcopy(ANA)]

    resposta = client.post("/predicoes", json=corpo(candidatos))

    assert resposta.status_code == 422
    assert "repetido" in resposta.text.lower()
