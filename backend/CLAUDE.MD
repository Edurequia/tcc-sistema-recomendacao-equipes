# TeamRec — Hub do Vault

Sistema orquestrador Web para formação de equipes de software, unindo motor preditivo (PyCaret/FastAPI) e justificativas gerativas (Gemini).

> [!IMPORTANT] Regra de ouro: documente SEMPRE
> **Toda** alteração — código, infra, config ou banco — **deve** ser documentada antes de concluir a tarefa. Não existe mudança "pequena demais" para registrar.
> - Adicione uma entrada em [[Tecnico/changelog]] com a data (`YYYY-MM-DD`) e o que mudou e por quê.
> - Atualize a nota afetada (projeto/técnica): status, tarefas (`- [x]`), decisões.
> - Atualize o `atualizado:` do frontmatter das notas tocadas.
> - Nunca finalize sem o vault refletir o estado real do código.

---

## Fluxo de desenvolvimento

> [!IMPORTANT] Regra permanente — vale pra toda feature, sem precisar repetir no prompt

- Toda feature nova segue o **Superpowers**: brainstorm → plano → implementação → revisão.
- **TDD obrigatório**: escrever o teste antes da implementação (já enforced pelo Superpowers).
- Antes de considerar **qualquer** feature pronta:
    1. Rodar a skill **`semgrep`** nos arquivos alterados, corrigir os achados **sem mudar comportamento** e validar com um novo scan.
    2. Se a feature introduzir **dependência nova**, rodar o **`supply-chain-risk-auditor`** nela.
- **Não avançar de etapa** (plano → código, código → revisão) sem confirmação explícita.

Ferramental recomendado: `superpowers@claude-plugins-official`, `supply-chain-risk-auditor@trailofbits` e as skills do `semgrep/skills` (`semgrep`, `code-security`, `llm-security`).

---

## Skills que valem a pena usar (Claude Code)

| Skill | Quando usar |
|---|---|
| `init` | Gerar/atualizar o CLAUDE.md a partir da codebase existente |
| `tdd` | Toda feature: teste antes da implementação |
| `code-review` | Revisar mudanças desde um ponto fixo (branch, commit, PR) |
| `domain-modeling` | Fixar terminologia do domínio e registrar decisões arquiteturais |
| `obsidian-vault` / `obsidian-markdown` | Gerenciar as notas do vault |
| `run` | Subir e ver o app funcionando de verdade, não só passar teste |

---

## Mapa do Vault

| Pasta | Conteúdo |
|---|---|
| `Projetos/planejamento-atualizacoes/` | Ajustes e melhorias do que já está no ar. |
| `Projetos/funcionalidades/` | Features com código já implementado (`desenvolvendo`, `testando`, `concluido`). |
| `Projetos/planejamento/<feature>-AAAA-MM-DD/` | Plano fechado de uma feature ainda sem código — `plano.md`. |
| `Projetos/planejamento/` (raiz) | Notas de escopo solto, ainda sem plano fechado. |
| `Tecnico/` | Arquitetura, API, banco de dados (9 entidades), decisões técnicas. |
| `Diario/` | Notas de sessão de desenvolvimento (`YYYY-MM-DD.md`). |

---

## Como Executar

```bash
# Perfil LOCAL (padrao, PostgreSQL persistente). Credenciais no backend/.env
cd backend
docker compose up -d          # PostgreSQL em localhost:5433
./mvnw spring-boot:run

# Perfil DEV (H2 em memória, limpo a cada inicialização)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Rodar os testes automatizados do backend (atualmente 107 testes verdes, sempre em perfil dev)
./mvnw test

# Motor de predicao (Python 3.10) - necessario para gerar recomendacoes
cd ../api_motor_predicao
.venv/Scripts/python.exe -m uvicorn app.main:app --port 8000
.venv/Scripts/python.exe -m pytest   # 20 testes, usam o modelo real