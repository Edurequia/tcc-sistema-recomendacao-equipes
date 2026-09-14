"""Camada que envolve o pipeline treinado no PyCaret.

Concentra tudo que depende do formato do modelo: nomes e ordem das colunas,
montagem do DataFrame e a checagem de que o arquivo carregado e de fato o
modelo que este contrato espera.
"""

from __future__ import annotations

import logging
from importlib.metadata import PackageNotFoundError, version
from pathlib import Path
from typing import Sequence

import joblib
import pandas as pd

log = logging.getLogger(__name__)

PAPEIS = [
    "DevOps specialist",
    "Developer, QA or test",
    "Developer, back-end",
    "Developer, front-end",
    "Developer, full-stack",
    "Developer, mobile",
]

# Ordem exata usada no treinamento (script de geracao da base simulada).
COLUNAS = [
    "Nivel_Senioridade",
    *PAPEIS,
    "Soft_Skills_Score",
    "Proj_Criticidade",
    "Proj_Requisito_Backend",
    "Proj_Requisito_Frontend",
    "Aderencia_Tecnica",
    "Cobertura_Horas",
]

# O pipeline do PyCaret guarda tambem o nome da variavel alvo.
COLUNA_ALVO = "Escore_Afinidade"


class MotorPredicao:
    """Pipeline treinado + conversao do contrato da API para o vetor do modelo."""

    def __init__(self, pipeline, arquivo: str):
        colunas_modelo = [c for c in pipeline.feature_names_in_ if c != COLUNA_ALVO]
        if colunas_modelo != COLUNAS:
            faltando = [c for c in COLUNAS if c not in colunas_modelo]
            sobrando = [c for c in colunas_modelo if c not in COLUNAS]
            raise ValueError(
                "O modelo carregado nao corresponde ao contrato da API. "
                f"Colunas ausentes: {faltando}; colunas inesperadas: {sobrando}."
            )

        estimador = pipeline.steps[-1][1]
        # O LightGBM repete avisos de apelidos de parametros a cada predicao;
        # silencia-los nao altera o resultado.
        if hasattr(estimador, "get_params") and "verbose" in estimador.get_params():
            estimador.set_params(verbose=-1)

        self._pipeline = pipeline
        self.arquivo = arquivo
        self.colunas = colunas_modelo
        self.algoritmo = type(estimador).__name__

    @classmethod
    def carregar(cls, caminho: Path) -> "MotorPredicao":
        log.info("Carregando modelo de %s", caminho)
        return cls(joblib.load(caminho), arquivo=caminho.name)

    def prever(self, projeto, candidatos: Sequence) -> list[float]:
        """Devolve um escore por candidato, na mesma ordem recebida."""
        linhas = [self._vetor(projeto, candidato) for candidato in candidatos]
        dados = pd.DataFrame(linhas, columns=self.colunas)
        return [round(float(score), 3) for score in self._pipeline.predict(dados)]

    def metadados(self) -> dict:
        return {
            "algoritmo": self.algoritmo,
            "arquivo": self.arquivo,
            "colunas": self.colunas,
            "papeisAceitos": PAPEIS,
            "versoes": {pacote: _versao(pacote)
                        for pacote in ("pycaret", "scikit-learn", "lightgbm")},
        }

    @staticmethod
    def _vetor(projeto, candidato) -> dict:
        linha = dict.fromkeys(COLUNAS, 0)
        linha[candidato.papel] = 1
        linha.update({
            "Nivel_Senioridade": candidato.nivel_senioridade,
            "Soft_Skills_Score": candidato.soft_skills_score,
            "Proj_Criticidade": projeto.criticidade,
            "Proj_Requisito_Backend": int(projeto.requisito_backend),
            "Proj_Requisito_Frontend": int(projeto.requisito_frontend),
            "Aderencia_Tecnica": candidato.aderencia_tecnica,
            "Cobertura_Horas": candidato.cobertura_horas,
        })
        return linha


def _versao(pacote: str) -> str | None:
    try:
        return version(pacote)
    except PackageNotFoundError:
        return None
