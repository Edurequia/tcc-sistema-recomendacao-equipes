import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { rotas } from '../rotas'
import { NotificacoesProvider } from '../components/Notificacoes'
import { TemaProvider } from '../tema'

/**
 * Renderiza a aplicacao inteira na rota indicada, com cache de consultas isolado por teste.
 * As paginas sao carregadas sob demanda: espera o roteador terminar a primeira rota.
 */
export async function renderizarRota(caminho: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(rotas, { initialEntries: [caminho] })
  const usuario = userEvent.setup()
  const resultado = render(
    <TemaProvider>
      <QueryClientProvider client={queryClient}>
        <NotificacoesProvider>
          <RouterProvider router={router} />
        </NotificacoesProvider>
      </QueryClientProvider>
    </TemaProvider>,
  )
  // O React aplica a navegacao como transicao: espera o roteador e a tela desenhada.
  await waitFor(() => {
    expect(router.state.initialized).toBe(true)
    expect(router.state.navigation.state).toBe('idle')
    expect(resultado.container.querySelector('main')).not.toBeNull()
  }, { timeout: 5000 }) // o primeiro import de cada pagina e lento no jsdom
  return { ...resultado, router, usuario }
}
