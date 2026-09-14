# TeamRec — Motor de Predição

Serviço FastAPI que expõe o modelo treinado no PyCaret (`motor_predicao_tcc.pkl`) e
calcula o **escore de afinidade** entre desenvolvedores e um projeto. É consumido
apenas pelo backend Spring; o frontend nunca o acessa diretamente.

## O modelo

| Item | Valor |
|---|---|
| Algoritmo | LightGBM (`LGBMRegressor`), otimizado com `tune_model` |
| Pipeline | PyCaret 3.3.2 (imputação + estimador) |
| Entradas | 13 colunas |
| Saída | escore de 0 a 100 (aprox.) |

Colunas, na ordem do treinamento:

| Coluna | Domínio | Origem no backend |
|---|---|---|
| `Nivel_Senioridade` | 1 a 3 | `Senioridade.valorOrdinal` |
| 6 papéis (`Developer, back-end`, ...) | 0/1, one-hot | `PapelAtuacao.chaveDataset` |
| `Soft_Skills_Score` | 0 a 5 | `Desenvolvedor.softSkillsScore` |
| `Proj_Criticidade` | 1 a 3 | `CriticidadeProjeto.nivel` |
| `Proj_Requisito_Backend` | 0/1 | papel exigido BACKEND ou FULLSTACK |
| `Proj_Requisito_Frontend` | 0/1 | papel exigido FRONTEND ou FULLSTACK |
| `Aderencia_Tecnica` | 0 a 1 | Σ pesos da stack que o dev domina ÷ Σ pesos exigidos |
| `Cobertura_Horas` | 0 a 1 | min((horas disponíveis − alocadas em outros projetos) ÷ horas exigidas, 1) |

Validação feita sobre este arquivo (3.000 casos sorteados, comparados à fórmula
geradora sem ruído): **MAE 1,08 · RMSE 1,42**. Cobertura de horas de 0 → 1 soma
~15 pontos; aderência técnica 0 → 1 soma ~16 pontos (criticidade alta).

## Como rodar

Requer **Python 3.10** e o arquivo `motor_predicao_tcc.pkl` nesta pasta. O modelo **não é
versionado** (`*.pkl` está no `.gitignore`): copie-o do local onde o treinamento foi salvo.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app.main:app --port 8000
```

> **Não atualize o LightGBM.** A versão 4.7.0, que o `pip install pycaret` instala por
> padrão, derruba o processo (`access violation`) ao prever com este modelo. O
> `requirements.txt` fixa a 4.6.0.

| Endereço | O quê |
|---|---|
| http://localhost:8000/docs | Swagger do serviço |
| `GET /health` | `{"status": "UP", "modeloCarregado": true}` |
| `GET /modelo` | colunas, papéis aceitos e versões das bibliotecas |
| `POST /predicoes` | escores de um lote de candidatos |

O caminho do modelo pode ser trocado pela variável `MODELO_PATH`. Se o arquivo não
tiver exatamente as 13 colunas acima, o serviço **não sobe** — melhor falhar na
inicialização do que responder escores de um modelo errado.

Testes: `.\.venv\Scripts\python.exe -m pytest`

## Contrato

```http
POST /predicoes
Content-Type: application/json

{
  "projeto": { "criticidade": 3, "requisitoBackend": true, "requisitoFrontend": false },
  "candidatos": [
    { "id": 1, "nivelSenioridade": 3, "papel": "Developer, back-end",
      "softSkillsScore": 4.0, "aderenciaTecnica": 0.75, "coberturaHoras": 0.9 }
  ]
}
```

```json
{ "algoritmo": "LGBMRegressor", "predicoes": [ { "id": 1, "score": 91.015 } ] }
```

- As predições voltam **na mesma ordem** dos candidatos.
- Valores fora do domínio de treinamento, papel desconhecido, projeto sem nenhum
  requisito, lista vazia ou `id` repetido → **422**. Árvores de decisão não falham com
  entradas fora da faixa; tratam como o extremo mais próximo, em silêncio. Por isso a
  recusa acontece antes do modelo.

## Organização

```
app/
├── main.py      FastAPI: ciclo de vida (carrega o modelo uma vez) e rotas
├── motor.py     contrato de colunas, montagem do DataFrame, checagem do .pkl
└── schemas.py   modelos Pydantic com os limites de cada campo
tests/
├── test_api.py    contrato HTTP contra o modelo real
└── test_motor.py  checagem de colunas com pipeline falso
```
