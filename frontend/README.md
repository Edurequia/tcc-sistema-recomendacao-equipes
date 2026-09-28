# TeamRec — Frontend

Interface web do **Sistema Web de Recomendação para Formação de Equipes em Projetos de
Desenvolvimento de Software** (TFG — Sistemas de Informação — UFN).

O frontend conversa **apenas com o backend Spring**. Nunca acessa o motor de predição
nem a API do modelo de linguagem diretamente.

```
  [ React (este módulo) ] ──/api──▶ Backend Spring Boot ──▶ Motor de predição (FastAPI)
                                            └──────────────▶ Gemini (Spring AI)
```

## Stack

| Item | Versão | Para quê |
|---|---|---|
| React + TypeScript | 19 / 6 | Interface |
| Vite | 8 | Servidor de desenvolvimento, proxy e build |
| Material UI | 9 | Componentes, tema claro/escuro |
| TanStack Query | 5 | Busca, cache e atualização dos dados da API |
| React Router | 8 | Rotas, com cada página carregada sob demanda |
| react-hook-form + zod | 7 / 4 | Formulários e validação |
| Vitest + Testing Library + MSW | 5 / 16 / 2 | Testes com a API simulada |

Fonte: Plus Jakarta Sans (pacote `@fontsource-variable`, servida localmente).

## Como rodar

Requer Node 20 ou superior. O backend precisa estar em `localhost:8080` (veja
[`../backend/README.md`](../backend/README.md)); o Vite repassa `/api` para ele.

```bash
npm install
npm run dev          # http://localhost:5173
```

Para apontar para outro backend: `VITE_BACKEND_URL=http://host:porta npm run dev`.

Com o backend fora do ar, as telas mostram "Não foi possível contatar o servidor".
Sem o motor de predição, só a geração de recomendações falha (503); sem o Gemini
configurado, só a justificativa e o assistente.

| Comando | O quê |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com recarga automática |
| `npm test` | Testes (uma execução) |
| `npm run test:watch` | Testes em modo observação |
| `npm run typecheck` | Checagem de tipos |
| `npm run lint` | Lint (oxlint) |
| `npm run build` | Build de produção em `dist/` |

## Telas

| Rota | Tela |
|---|---|
| `/projetos` | Lista com busca, filtros por status e criticidade, paginação |
| `/projetos/novo`, `/projetos/:id/editar` | Cadastro: identificação, vagas por papel, stack com peso e obrigatoriedade |
| `/projetos/:id` | Detalhe: status, requisitos, recomendações (gerar e histórico), equipe alocada |
| `/recomendacoes/:id` | Restrições, métricas, equipe proposta, justificativa, linha de base, ranking, confirmação |
| `/desenvolvedores` | Lista com filtros, ajuste de horas, inativação |
| `/desenvolvedores/novo`, `/desenvolvedores/:id` | Perfil e competências com proficiência |
| `/habilidades` | Catálogo com filtros, cadastro e inativação |
| `/assistente` | Conversa em linguagem natural que cria o projeto e a recomendação |

## Organização

```
src/
├── api/            cliente HTTP, tipos (espelho dos DTOs), rótulos dos enums e hooks por recurso
├── components/     layout, tema, painéis, filtros, paginação, diálogos, avisos
├── features/       uma pasta por área: habilidades, desenvolvedores, projetos,
│                   recomendacoes, assistente — cada uma com seus testes
├── test/           setup do Vitest, servidor MSW, dados de exemplo e helper de renderização
├── rotas.tsx       rotas (páginas carregadas sob demanda)
├── tema.tsx        tema MUI claro/escuro
└── texto.ts        formatação de números, datas e buscas sem acento
```

Decisões que o código segue:

- **Erros num formato só.** O cliente (`api/cliente.ts`) converte o `ApiError` do backend
  em `ErroApi`. A mensagem vai para um aviso e os erros por campo (`campos[]`) vão para
  o próprio campo do formulário (`components/errosFormulario.ts`).
- **As regras continuam no backend.** O frontend valida o que dá para validar antes de
  enviar (limites, vagas × tamanho da equipe, ordem das datas), mas o backend é quem decide.
- **Sem estado de sessão no chat.** O backend não guarda a conversa; a tela envia o
  histórico inteiro a cada turno, até o limite de 20 mensagens.
- **Testes com a aplicação inteira.** Cada teste renderiza as rotas reais com a API
  simulada pelo MSW; requisição sem simulação faz o teste falhar.
