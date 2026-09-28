import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { Alocacao, Projeto } from '../../api/tipos'
import { umaAlocacao, umaPagina, umaRecomendacao, umaRecomendacaoResumo, umDesenvolvedorResumo, umProjeto } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

function projeto(dados: Partial<Projeto> = {}, alocacoes: Alocacao[] = []) {
  let atual = umProjeto(dados)
  servidor.use(
    http.get('/api/projetos/1', () => HttpResponse.json(atual)),
    http.get('/api/projetos/1/alocacoes', () => HttpResponse.json(alocacoes)),
    http.get('/api/projetos/1/recomendacoes', () => HttpResponse.json([])),
  )
  return { alterar: (novos: Partial<Projeto>) => (atual = { ...atual, ...novos }) }
}

describe('detalhe do projeto', async () => {
  it('mostra os dados e os requisitos do projeto', async () => {
    projeto()
    await renderizarRota('/projetos/1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })).toBeInTheDocument()
    expect(screen.getByText('Reescrita do portal do aluno.')).toBeInTheDocument()
    const papeis = screen.getByRole('region', { name: 'Papéis exigidos' })
    expect(papeis).toHaveTextContent('Backend')
    expect(papeis).toHaveTextContent('2')
    expect(papeis).toHaveTextContent('Sem papel definido')
    const stack = screen.getByRole('region', { name: 'Stack exigida' })
    expect(within(stack).getByText('Java').closest('li')).toHaveTextContent('Obrigatória')
    expect(within(stack).getByText('React').closest('li')).toHaveTextContent('Peso 3')
  })

  it('altera o status do projeto', async () => {
    const estado = projeto()
    let corpo: unknown
    servidor.use(
      http.patch('/api/projetos/1/status', async ({ request }) => {
        corpo = await request.json()
        estado.alterar({ status: 'EM_ANDAMENTO', statusDescricao: 'Em andamento' })
        return HttpResponse.json(umProjeto({ status: 'EM_ANDAMENTO' }))
      }),
    )
    const { usuario } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('combobox', { name: 'Status do projeto' }))
    await usuario.click(screen.getByRole('option', { name: 'Em andamento' }))

    await waitFor(() => expect(corpo).toEqual({ status: 'EM_ANDAMENTO' }))
    expect(await screen.findByText('Status alterado para Em andamento.')).toBeInTheDocument()
  })

  it('cancela o projeto depois de confirmar', async () => {
    projeto()
    let cancelou = false
    servidor.use(
      http.delete('/api/projetos/1', async () => {
        cancelou = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('button', { name: 'Cancelar projeto' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar Portal do Aluno?' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar projeto' }))

    await waitFor(() => expect(cancelou).toBe(true))
    expect(await screen.findByText('Projeto Portal do Aluno cancelado.')).toBeInTheDocument()
  })

  it('lista a equipe alocada e encerra uma alocacao', async () => {
    projeto({}, [umaAlocacao()])
    let encerrou = false
    servidor.use(
      http.delete('/api/alocacoes/10', async () => {
        encerrou = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = await renderizarRota('/projetos/1')

    const equipe = await screen.findByRole('region', { name: 'Equipe alocada' })
    const ana = (await within(equipe).findByText('Ana Souza')).closest('li')!
    expect(ana).toHaveTextContent('Backend')
    expect(ana).toHaveTextContent('20 h/semana')
    expect(equipe).toHaveTextContent('1 de 4 integrantes')

    await usuario.click(within(ana).getByRole('button', { name: 'Encerrar alocação de Ana Souza' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Encerrar' }))

    await waitFor(() => expect(encerrou).toBe(true))
    expect(await screen.findByText('Alocação de Ana Souza encerrada.')).toBeInTheDocument()
  })

  it('aloca um desenvolvedor manualmente', async () => {
    projeto()
    let corpo: unknown
    const buscas: (string | null)[] = []
    servidor.use(
      http.get('/api/desenvolvedores', ({ request }) => {
        buscas.push(new URL(request.url).searchParams.get('nome'))
        return HttpResponse.json(umaPagina([umDesenvolvedorResumo()]))
      }),
      http.post('/api/projetos/1/alocacoes', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(umaAlocacao(), { status: 201 })
      }),
    )
    const { usuario } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('button', { name: 'Alocar desenvolvedor' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Alocar desenvolvedor' })
    await usuario.type(within(dialogo).getByRole('combobox', { name: 'Desenvolvedor' }), 'Ana')
    await waitFor(() => expect(buscas).toContain('Ana'))
    await usuario.click(await screen.findByRole('option', { name: /Ana Souza/ }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Alocar' }))

    await waitFor(() => expect(corpo).toEqual({ desenvolvedorId: 1 }))
    expect(await screen.findByText('Ana Souza alocado(a) no projeto.')).toBeInTheDocument()
  })

  it('mostra na janela a alocacao recusada por falta de horas', async () => {
    projeto()
    servidor.use(
      http.get('/api/desenvolvedores', () => HttpResponse.json(umaPagina([umDesenvolvedorResumo()]))),
      http.post('/api/projetos/1/alocacoes', () =>
        HttpResponse.json(
          { status: 422, mensagem: "Conflito de alocacao: 'Ana Souza' tem 10 horas semanais livres, mas a alocacao exige 20." },
          { status: 422 },
        ),
      ),
    )
    const { usuario } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('button', { name: 'Alocar desenvolvedor' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.type(within(dialogo).getByRole('combobox', { name: 'Desenvolvedor' }), 'Ana')
    await usuario.click(await screen.findByRole('option', { name: /Ana Souza/ }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Alocar' }))

    expect(await within(dialogo).findByText(/tem 10 horas semanais livres/)).toBeInTheDocument()
  })

  it('nao oferece alocacao nem cancelamento em projeto encerrado', async () => {
    projeto({ status: 'CANCELADO', statusDescricao: 'Cancelado' })
    await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    expect(screen.queryByRole('button', { name: 'Alocar desenvolvedor' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar projeto' })).not.toBeInTheDocument()
  })

  it('mostra o historico de recomendacoes do projeto', async () => {
    projeto()
    servidor.use(
      http.get('/api/projetos/1/recomendacoes', () =>
        HttpResponse.json([
          umaRecomendacaoResumo({ id: 8, origem: 'CHAT', criadoEm: '2026-09-21T10:15:00-03:00', observacaoRestricao: 'Equipe parcial.' }),
          umaRecomendacaoResumo(),
        ]),
      ),
    )
    await renderizarRota('/projetos/1')

    const historico = await screen.findByRole('region', { name: 'Recomendações' })
    const itens = await within(historico).findAllByRole('link')
    expect(itens[0]).toHaveAttribute('href', '/recomendacoes/8')
    expect(itens[0]).toHaveTextContent('21/09/2026')
    expect(itens[0]).toHaveTextContent('Assistente')
    expect(itens[0]).toHaveTextContent('Com restrições')
    expect(itens[1]).toHaveTextContent('Formulário')
    expect(itens[1]).toHaveTextContent('67%')
  })

  it('gera uma recomendacao e abre o resultado', async () => {
    projeto()
    servidor.use(
      http.post('/api/projetos/1/recomendacoes', () => HttpResponse.json(umaRecomendacao(), { status: 201 })),
      http.get('/api/recomendacoes/7', () => HttpResponse.json(umaRecomendacao())),
      http.get('/api/recomendacoes/7/linha-base', () => HttpResponse.json({})),
    )
    const { usuario, router } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('button', { name: 'Gerar recomendação' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/recomendacoes/7'))
  })

  it('avisa quando o motor de predicao esta fora do ar', async () => {
    projeto()
    servidor.use(
      http.post('/api/projetos/1/recomendacoes', () =>
        HttpResponse.json({ status: 503, mensagem: 'O motor de predicao esta indisponivel no momento.' }, { status: 503 }),
      ),
    )
    const { usuario } = await renderizarRota('/projetos/1')
    await screen.findByRole('heading', { level: 1, name: 'Portal do Aluno' })

    await usuario.click(screen.getByRole('button', { name: 'Gerar recomendação' }))

    expect(await screen.findByText('O motor de predicao esta indisponivel no momento.')).toBeInTheDocument()
  })
})
