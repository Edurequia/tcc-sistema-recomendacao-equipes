import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { DesenvolvedorResumo } from '../../api/tipos'
import { umaPagina, umDesenvolvedorResumo } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const ANA = umDesenvolvedorResumo()
const BRUNO = umDesenvolvedorResumo({
  id: 2,
  nome: 'Bruno Lima',
  email: 'bruno@ufn.edu.br',
  senioridade: 'JUNIOR',
  senioridadeDescricao: 'Junior',
  papelPrincipal: 'FRONTEND',
  papelPrincipalDescricao: 'Frontend',
  horasDisponiveis: 20,
})
const CARLA = umDesenvolvedorResumo({ id: 3, nome: 'Carla Dias', email: 'carla@ufn.edu.br', ativo: false })

/** Responde a listagem e devolve as URLs recebidas, para conferir os filtros enviados. */
function base(desenvolvedores: DesenvolvedorResumo[], pagina: Partial<Parameters<typeof umaPagina>[1]> = {}) {
  const urls: URL[] = []
  servidor.use(
    http.get('/api/desenvolvedores', ({ request }) => {
      const url = new URL(request.url)
      urls.push(url)
      const ativo = url.searchParams.get('ativo')
      const filtrados = desenvolvedores.filter((d) => ativo === null || String(d.ativo) === ativo)
      return HttpResponse.json(umaPagina(filtrados, pagina))
    }),
  )
  return urls
}

async function linhaDe(nome: string) {
  const texto = await screen.findByText(nome)
  return texto.closest('tr')!
}

describe('pagina de desenvolvedores', async () => {
  it('lista os desenvolvedores ativos com papel, senioridade e horas', async () => {
    const urls = base([ANA, BRUNO, CARLA])
    await renderizarRota('/desenvolvedores')

    const ana = await linhaDe('Ana Souza')
    expect(ana).toHaveTextContent('ana.souza@ufn.edu.br')
    expect(ana).toHaveTextContent('Backend')
    expect(ana).toHaveTextContent('Sênior')
    expect(ana).toHaveTextContent('30 h')
    expect(ana).toHaveTextContent('4,2')
    expect(screen.queryByText('Carla Dias')).not.toBeInTheDocument()
    expect(urls[0].searchParams.get('ativo')).toBe('true')
    expect(urls[0].searchParams.get('page')).toBe('0')
  })

  it('envia os filtros de papel, senioridade e nome ao servidor', async () => {
    const urls = base([ANA])
    const { usuario } = await renderizarRota('/desenvolvedores')
    await linhaDe('Ana Souza')

    await usuario.click(screen.getByRole('combobox', { name: 'Papel' }))
    await usuario.click(screen.getByRole('option', { name: 'Frontend' }))
    await usuario.click(screen.getByRole('combobox', { name: 'Senioridade' }))
    await usuario.click(screen.getByRole('option', { name: 'Júnior' }))
    await usuario.type(screen.getByRole('searchbox', { name: 'Buscar desenvolvedor' }), 'bru')

    await waitFor(() => {
      const ultima = urls.at(-1)!.searchParams
      expect(ultima.get('papel')).toBe('FRONTEND')
      expect(ultima.get('senioridade')).toBe('JUNIOR')
      expect(ultima.get('nome')).toBe('bru')
    })
  })

  it('pagina no servidor', async () => {
    const urls = base([ANA], { totalElementos: 45, totalPaginas: 3, ultima: false })
    const { usuario } = await renderizarRota('/desenvolvedores')
    await linhaDe('Ana Souza')

    await usuario.click(screen.getByRole('button', { name: /próxima página/i }))

    await waitFor(() => expect(urls.at(-1)!.searchParams.get('page')).toBe('1'))
  })

  it('mostra os inativos e permite reativar', async () => {
    const urls = base([ANA, CARLA])
    let reativado: string | undefined
    servidor.use(
      http.post('/api/desenvolvedores/:id/reativacao', ({ params }) => {
        reativado = params.id as string
        return HttpResponse.json({ ...CARLA, ativo: true, habilidades: [] })
      }),
    )
    const { usuario } = await renderizarRota('/desenvolvedores')
    await linhaDe('Ana Souza')

    await usuario.click(screen.getByRole('combobox', { name: 'Situação' }))
    await usuario.click(screen.getByRole('option', { name: 'Inativos' }))
    const carla = await linhaDe('Carla Dias')
    expect(urls.at(-1)!.searchParams.get('ativo')).toBe('false')

    await usuario.click(within(carla).getByRole('button', { name: 'Reativar Carla Dias' }))

    await waitFor(() => expect(reativado).toBe('3'))
    expect(await screen.findByText('Carla Dias reativado(a).')).toBeInTheDocument()
  })

  it('pede confirmacao antes de inativar', async () => {
    base([ANA])
    let inativou = false
    servidor.use(
      http.delete('/api/desenvolvedores/1', async () => {
        inativou = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = await renderizarRota('/desenvolvedores')

    await usuario.click(within(await linhaDe('Ana Souza')).getByRole('button', { name: 'Inativar Ana Souza' }))
    const confirmacao = await screen.findByRole('dialog', { name: 'Inativar Ana Souza?' })
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Inativar' }))

    await waitFor(() => expect(inativou).toBe(true))
    expect(await screen.findByText('Ana Souza inativado(a).')).toBeInTheDocument()
  })

  it('ajusta a disponibilidade semanal', async () => {
    base([ANA])
    let corpo: unknown
    servidor.use(
      http.patch('/api/desenvolvedores/1/disponibilidade', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...ANA, horasDisponiveis: 24, habilidades: [] })
      }),
    )
    const { usuario } = await renderizarRota('/desenvolvedores')

    await usuario.click(within(await linhaDe('Ana Souza')).getByRole('button', { name: 'Ajustar horas de Ana Souza' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Disponibilidade de Ana Souza' })
    const horas = within(dialogo).getByLabelText(/Horas por semana/)
    await usuario.clear(horas)
    await usuario.type(horas, '24')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(corpo).toEqual({ horasDisponiveis: 24 }))
    expect(await screen.findByText('Disponibilidade de Ana Souza atualizada para 24 h.')).toBeInTheDocument()
  })

  it('explica quando as horas ficam abaixo do ja comprometido', async () => {
    base([ANA])
    servidor.use(
      http.patch('/api/desenvolvedores/1/disponibilidade', () =>
        HttpResponse.json(
          {
            status: 422,
            mensagem:
              'Nao e possivel reduzir a disponibilidade para 10 horas: o desenvolvedor ja possui 20 horas semanais comprometidas em projetos ativos.',
          },
          { status: 422 },
        ),
      ),
    )
    const { usuario } = await renderizarRota('/desenvolvedores')

    await usuario.click(within(await linhaDe('Ana Souza')).getByRole('button', { name: 'Ajustar horas de Ana Souza' }))
    const dialogo = await screen.findByRole('dialog')
    const horas = within(dialogo).getByLabelText(/Horas por semana/)
    await usuario.clear(horas)
    await usuario.type(horas, '10')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Salvar' }))

    expect(await within(dialogo).findByText(/20 horas semanais comprometidas/)).toBeInTheDocument()
  })

  it('leva ao formulario de cadastro e de edicao', async () => {
    base([ANA])
    servidor.use(http.get('/api/habilidades', () => HttpResponse.json([])))
    const { usuario, router } = await renderizarRota('/desenvolvedores')

    await usuario.click(screen.getByRole('link', { name: 'Novo desenvolvedor' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/desenvolvedores/novo'))
  })

  it('mostra um estado vazio quando nao ha resultados', async () => {
    base([])
    await renderizarRota('/desenvolvedores')

    expect(await screen.findByText('Nenhum desenvolvedor encontrado')).toBeInTheDocument()
  })
})
