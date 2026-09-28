import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { ProjetoResumo } from '../../api/tipos'
import { umaPagina, umProjetoResumo } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const PORTAL = umProjetoResumo()
const APP = umProjetoResumo({
  id: 2,
  nome: 'App de Agendamento',
  criticidade: 'MEDIA',
  status: 'EM_ANDAMENTO',
  tamanhoEquipe: 3,
  horasSemanaisPorIntegrante: 30,
  dataInicioPrevista: undefined,
  dataFimPrevista: undefined,
})

function base(projetos: ProjetoResumo[]) {
  const urls: URL[] = []
  servidor.use(
    http.get('/api/projetos', ({ request }) => {
      const url = new URL(request.url)
      urls.push(url)
      return HttpResponse.json(umaPagina(projetos))
    }),
  )
  return urls
}

describe('pagina de projetos', async () => {
  it('lista os projetos com criticidade, status, equipe e periodo', async () => {
    base([PORTAL, APP])
    await renderizarRota('/projetos')

    const portal = (await screen.findByText('Portal do Aluno')).closest('tr')!
    expect(portal).toHaveTextContent('Alta')
    expect(portal).toHaveTextContent('Em planejamento')
    expect(portal).toHaveTextContent('4 × 20 h')
    expect(portal).toHaveTextContent('01/10/2026 – 31/03/2027')
    const app = screen.getByText('App de Agendamento').closest('tr')!
    expect(app).toHaveTextContent('Média')
    expect(app).toHaveTextContent('Em andamento')
    expect(app).toHaveTextContent('Sem datas')
  })

  it('envia os filtros de status, criticidade e nome', async () => {
    const urls = base([PORTAL])
    const { usuario } = await renderizarRota('/projetos')
    await screen.findByText('Portal do Aluno')

    await usuario.click(screen.getByRole('combobox', { name: 'Status' }))
    await usuario.click(screen.getByRole('option', { name: 'Em andamento' }))
    await usuario.click(screen.getByRole('combobox', { name: 'Criticidade' }))
    await usuario.click(screen.getByRole('option', { name: 'Alta' }))
    await usuario.type(screen.getByRole('searchbox', { name: 'Buscar projeto' }), 'portal')

    await waitFor(() => {
      const ultima = urls.at(-1)!.searchParams
      expect(ultima.get('status')).toBe('EM_ANDAMENTO')
      expect(ultima.get('criticidade')).toBe('ALTA')
      expect(ultima.get('nome')).toBe('portal')
    })
  })

  it('leva ao detalhe do projeto e ao cadastro', async () => {
    base([PORTAL])
    servidor.use(http.get('/api/projetos/1', () => HttpResponse.json({})))
    const { usuario, router } = await renderizarRota('/projetos')

    await usuario.click(await screen.findByRole('link', { name: 'Portal do Aluno' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/projetos/1'))
  })

  it('mostra um estado vazio com atalho para o cadastro', async () => {
    base([])
    await renderizarRota('/projetos')

    expect(await screen.findByText('Nenhum projeto encontrado')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Novo projeto/ })[0]).toHaveAttribute('href', '/projetos/novo')
  })
})
