"""Contrato HTTP do motor de predicao.

Os limites abaixo sao o dominio visto no treinamento. Arvores de decisao nao
falham com valores fora dele: tratam silenciosamente como o extremo mais
proximo. Por isso a recusa acontece aqui, antes do modelo.
"""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator
from pydantic.alias_generators import to_camel

Papel = Literal[
    "DevOps specialist",
    "Developer, QA or test",
    "Developer, back-end",
    "Developer, front-end",
    "Developer, full-stack",
    "Developer, mobile",
]

MAX_CANDIDATOS = 5000


class _Contrato(BaseModel):
    """Campos em camelCase no JSON, snake_case no Python."""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ProjetoEntrada(_Contrato):
    criticidade: int = Field(ge=1, le=3, description="1 = baixa, 2 = media, 3 = alta.")
    requisito_backend: bool
    requisito_frontend: bool

    @model_validator(mode="after")
    def exige_ao_menos_um_requisito(self) -> "ProjetoEntrada":
        # O treinamento nunca gerou projeto sem demanda de backend nem de frontend.
        if not (self.requisito_backend or self.requisito_frontend):
            raise ValueError("O projeto deve ter ao menos um requisito: backend ou frontend.")
        return self


class CandidatoEntrada(_Contrato):
    id: int = Field(description="Identificador do desenvolvedor, devolvido na resposta.")
    nivel_senioridade: int = Field(ge=1, le=3, description="1 = junior, 2 = pleno, 3 = senior.")
    papel: Papel = Field(description="Rotulo do papel na matriz de treinamento.")
    soft_skills_score: float = Field(ge=0, le=5)
    aderencia_tecnica: float = Field(ge=0, le=1,
                                     description="Proporcao dos pesos da stack exigida que o candidato domina.")
    cobertura_horas: float = Field(ge=0, le=1,
                                   description="Horas efetivas / horas exigidas pelo projeto, limitado a 1.")


class PredicaoRequest(_Contrato):
    projeto: ProjetoEntrada
    candidatos: list[CandidatoEntrada] = Field(min_length=1, max_length=MAX_CANDIDATOS)

    @model_validator(mode="after")
    def ids_unicos(self) -> "PredicaoRequest":
        vistos, repetidos = set(), set()
        for candidato in self.candidatos:
            (repetidos if candidato.id in vistos else vistos).add(candidato.id)
        if repetidos:
            raise ValueError(f"Identificador de candidato repetido: {sorted(repetidos)}.")
        return self


class Predicao(_Contrato):
    id: int
    score: float = Field(description="Escore de afinidade, aproximadamente de 0 a 100.")


class PredicaoResponse(_Contrato):
    algoritmo: str
    predicoes: list[Predicao]


class HealthResponse(_Contrato):
    status: str
    modelo_carregado: bool


class ModeloResponse(_Contrato):
    algoritmo: str
    arquivo: str
    colunas: list[str]
    papeis_aceitos: list[str]
    versoes: dict[str, str | None]
