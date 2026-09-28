import { Navigate, type RouteObject } from 'react-router'
import { Layout } from './components/Layout'
import { NaoEncontrada } from './components/NaoEncontrada'

// Cada pagina vira um pedaco proprio do bundle, baixado so quando a rota e aberta.
export const rotas: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/projetos" replace /> },
      { path: 'projetos', lazy: async () => ({ Component: (await import('./features/projetos/PaginaProjetos')).PaginaProjetos }) },
      { path: 'projetos/novo', lazy: async () => ({ Component: (await import('./features/projetos/FormularioProjeto')).FormularioProjeto }) },
      { path: 'projetos/:id', lazy: async () => ({ Component: (await import('./features/projetos/DetalheProjeto')).DetalheProjeto }) },
      { path: 'projetos/:id/editar', lazy: async () => ({ Component: (await import('./features/projetos/FormularioProjeto')).FormularioProjeto }) },
      {
        path: 'recomendacoes/:id',
        lazy: async () => ({ Component: (await import('./features/recomendacoes/PaginaRecomendacao')).PaginaRecomendacao }),
      },
      {
        path: 'desenvolvedores',
        lazy: async () => ({ Component: (await import('./features/desenvolvedores/PaginaDesenvolvedores')).PaginaDesenvolvedores }),
      },
      {
        path: 'desenvolvedores/novo',
        lazy: async () => ({ Component: (await import('./features/desenvolvedores/FormularioDesenvolvedor')).FormularioDesenvolvedor }),
      },
      {
        path: 'desenvolvedores/:id',
        lazy: async () => ({ Component: (await import('./features/desenvolvedores/FormularioDesenvolvedor')).FormularioDesenvolvedor }),
      },
      { path: 'habilidades', lazy: async () => ({ Component: (await import('./features/habilidades/PaginaHabilidades')).PaginaHabilidades }) },
      { path: 'assistente', lazy: async () => ({ Component: (await import('./features/assistente/PaginaAssistente')).PaginaAssistente }) },
      { path: '*', element: <NaoEncontrada /> },
    ],
  },
]
