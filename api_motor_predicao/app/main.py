"""Aplicacao FastAPI do motor de predicao.

O modelo e carregado uma unica vez, na inicializacao. Se o arquivo nao existir
ou nao corresponder ao contrato, o servico nem sobe: falhar cedo e melhor do
que responder escores de um modelo errado.
"""

from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request

from app.motor import MotorPredicao
from app.schemas import HealthResponse, ModeloResponse, PredicaoRequest, PredicaoResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

MODELO_PADRAO = Path(__file__).resolve().parent.parent / "motor_predicao_tcc.pkl"


def create_app(caminho_modelo: Path | None = None) -> FastAPI:
    caminho = caminho_modelo or Path(os.getenv("MODELO_PATH", MODELO_PADRAO))

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.motor = MotorPredicao.carregar(caminho)
        yield

    app = FastAPI(
        title="TeamRec - Motor de Predicao",
        version="1.0.0",
        description="Calcula o escore de afinidade entre desenvolvedores e um projeto "
                    "a partir do modelo LightGBM treinado no PyCaret.",
        lifespan=lifespan,
    )

    @app.get("/health", response_model=HealthResponse)
    def health(request: Request):
        return HealthResponse(status="UP", modelo_carregado=hasattr(request.app.state, "motor"))

    @app.get("/modelo", response_model=ModeloResponse)
    def modelo(request: Request):
        return request.app.state.motor.metadados()

    @app.post("/predicoes", response_model=PredicaoResponse)
    def predicoes(corpo: PredicaoRequest, request: Request):
        # Rota sincrona de proposito: o FastAPI a executa em thread separada,
        # sem bloquear o loop de eventos durante o calculo do modelo.
        motor: MotorPredicao = request.app.state.motor
        scores = motor.prever(corpo.projeto, corpo.candidatos)
        return PredicaoResponse(
            algoritmo=motor.algoritmo,
            predicoes=[{"id": c.id, "score": s} for c, s in zip(corpo.candidatos, scores)],
        )

    return app


app = create_app()
