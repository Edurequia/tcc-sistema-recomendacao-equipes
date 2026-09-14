"""Testes da camada que envolve o pipeline treinado, sem HTTP."""

import pytest

from app.motor import COLUNAS, MotorPredicao


class PipelineFalso:
    """Imita o minimo do pipeline PyCaret usado pelo motor."""

    def __init__(self, colunas):
        self.feature_names_in_ = colunas
        self.steps = [("actual_estimator", object())]

    def predict(self, dados):
        return [float(i) for i in range(len(dados))]


def test_recusa_modelo_com_colunas_diferentes_das_esperadas():
    colunas_erradas = COLUNAS[:-1] + ["Horas_Disponiveis", "Escore_Afinidade"]

    with pytest.raises(ValueError, match="Cobertura_Horas"):
        MotorPredicao(PipelineFalso(colunas_erradas), arquivo="falso.pkl")


def test_aceita_pipeline_que_inclui_a_coluna_alvo():
    motor = MotorPredicao(PipelineFalso(COLUNAS + ["Escore_Afinidade"]), arquivo="falso.pkl")

    assert motor.colunas == COLUNAS
