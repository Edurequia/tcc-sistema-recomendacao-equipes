import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { ErroApi } from './api/cliente'
import { rotas } from './rotas'
import { NotificacoesProvider } from './components/Notificacoes'
import { TemaProvider } from './tema'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Erro de regra (4xx) nao melhora com nova tentativa; so falha de rede ou 5xx.
      retry: (tentativas, erro) => tentativas < 2 && (!(erro instanceof ErroApi) || erro.status === 0 || erro.status >= 500),
    },
  },
})

const router = createBrowserRouter(rotas)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <TemaProvider>
      <QueryClientProvider client={queryClient}>
        <NotificacoesProvider>
          <RouterProvider router={router} />
        </NotificacoesProvider>
      </QueryClientProvider>
    </TemaProvider>
  </StrictMode>,
)
