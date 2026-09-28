import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { Recomendacao } from '../../api/tipos'
import { umaAlocacao, umaLinhaBase, umaRecomendacao, umProjeto } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

/** Registra a recomendacao 7 e devolve as URLs pedidas a linha de base. */
function recomendacao(dados: Partial<Recomendacao> = {}) {
  const urlsLinhaBase: URL[] = []
  servidor.use(
    http.get('/api/recomendacoes/7', () => HttpResponse.json(umaRecomendacao(dados))),
    http.get('/api/recomendacoes/7/linha-base', ({ request }) => {
      urlsLinhaBase.push(new URL(request.url))
      return HttpResponse.json(umaLinhaBase())
    }),
  )
  return urlsLinhaBase
}

describe('pagina da recomendacao', async () => {
  it('mostra a equipe proposta, as metricas e as restricoes', async () => {
    recomendacao()
    await renderizarRota('/recomendacoes/7')

    expect(await screen.findByRole('heading', { level: 1, name: 'Equipe recomendada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Portal do Aluno/ })).toHaveAttribute('href', '/projetos/1')
    expect(screen.getByRole('alert')).toHaveTextContent('Equipe parcial: 2 de 3 integrantes.')

    const metricas = screen.getByRole('region', { name: 'Métricas da equipe' })
    expect(within(metricas).getByText('Cobertura de papéis').closest('div')!.parentElement).toHaveTextContent('67%')
    expect(metricas).toHaveTextContent('100%')
    expect(metricas).toHaveTextContent('83,9')

    const equipe = screen.getByRole('region', { name: 'Equipe proposta' })
    expect(equipe).toHaveTextContent('2 de 3 integrantes')
    const ana = within(equipe).getByText('Ana Souza').closest('li')!
    expect(ana).toHaveTextContent('Backend')
    expect(ana).toHaveTextContent('88,1')
    expect(ana).toHaveTextContent('79%')
    expect(within(equipe).queryByText('Davi Rocha')).not.toBeInTheDocument()
  })

  it('lista o ranking completo e filtra os inelegiveis', async () => {
    recomendacao()
    const { usuario } = await renderizarRota('/recomendacoes/7')

    const ranking = await screen.findByRole('region', { name: 'Ranking completo' })
    expect(within(ranking).getByText('Carla Dias').closest('tr')).toHaveTextContent('Sem horas')
    expect(within(ranking).getByText('Davi Rocha').closest('tr')).toHaveTextContent('Elegível')
    expect(within(ranking).getByText('Ana Souza').closest('tr')).toHaveTextContent('Selecionado')

    await usuario.click(within(ranking).getByRole('button', { name: 'Inelegíveis' }))

    expect(within(ranking).queryByText('Ana Souza')).not.toBeInTheDocument()
    expect(within(ranking).getByText('Carla Dias')).toBeInTheDocument()
  })

  it('compara com a linha de base aleatoria e permite mudar os parametros', async () => {
    const urls = recomendacao()
    const { usuario } = await renderizarRota('/recomendacoes/7')

    const painel = await screen.findByRole('region', { name: 'Comparação com a linha de base' })
    const papeis = (await within(painel).findByText('Cobertura de papéis')).closest('tr')!
    expect(papeis).toHaveTextContent('66,7')
    expect(papeis).toHaveTextContent('41,2 ± 12,5')
    expect(papeis).toHaveTextContent('+25,5')
    expect(urls[0].searchParams.get('sorteios')).toBe('100')
    expect(urls[0].searchParams.get('semente')).toBe('42')

    const sorteios = within(painel).getByLabelText('Sorteios')
    await usuario.clear(sorteios)
    await usuario.type(sorteios, '500')
    await usuario.click(within(painel).getByRole('button', { name: 'Recalcular' }))

    await waitFor(() => expect(urls.at(-1)!.searchParams.get('sorteios')).toBe('500'))
  })

  it('pede a justificativa ao modelo de linguagem', async () => {
    recomendacao({ justificativaLlm: undefined })
    let regerar: string | null = 'nao pedido'
    servidor.use(
      http.post('/api/recomendacoes/7/justificativa', ({ request }) => {
        regerar = new URL(request.url).searchParams.get('regerar')
        return HttpResponse.json({
          recomendacaoId: 7,
          justificativa: 'A equipe cobre os papeis principais.\n\nAna lidera o backend.',
          modelo: 'gemini-2.5-flash',
          geradaAgora: true,
        })
      }),
    )
    const { usuario } = await renderizarRota('/recomendacoes/7')

    const painel = await screen.findByRole('region', { name: 'Justificativa' })
    await usuario.click(within(painel).getByRole('button', { name: 'Gerar justificativa' }))

    expect(await within(painel).findByText('A equipe cobre os papeis principais.')).toBeInTheDocument()
    expect(within(painel).getByText('Ana lidera o backend.')).toBeInTheDocument()
    expect(within(painel).getByText(/gemini-2.5-flash/)).toBeInTheDocument()
    expect(regerar).toBe('false')
  })

  it('regera uma justificativa ja gravada', async () => {
    recomendacao({ justificativaLlm: 'Texto antigo.' })
    let regerar: string | null = null
    servidor.use(
      http.post('/api/recomendacoes/7/justificativa', ({ request }) => {
        regerar = new URL(request.url).searchParams.get('regerar')
        return HttpResponse.json({ recomendacaoId: 7, justificativa: 'Texto novo.', modelo: 'gemini-2.5-flash', geradaAgora: true })
      }),
    )
    const { usuario } = await renderizarRota('/recomendacoes/7')

    const painel = await screen.findByRole('region', { name: 'Justificativa' })
    expect(within(painel).getByText('Texto antigo.')).toBeInTheDocument()
    await usuario.click(within(painel).getByRole('button', { name: 'Gerar novamente' }))

    expect(await within(painel).findByText('Texto novo.')).toBeInTheDocument()
    expect(regerar).toBe('true')
  })

  it('explica quando o modelo de linguagem nao esta configurado', async () => {
    recomendacao({ justificativaLlm: undefined })
    servidor.use(
      http.post('/api/recomendacoes/7/justificativa', () =>
        HttpResponse.json(
          { status: 503, mensagem: 'A integracao com o modelo de linguagem nao esta configurada. Defina GEMINI_API_KEY.' },
          { status: 503 },
        ),
      ),
    )
    const { usuario } = await renderizarRota('/recomendacoes/7')

    const painel = await screen.findByRole('region', { name: 'Justificativa' })
    await usuario.click(within(painel).getByRole('button', { name: 'Gerar justificativa' }))

    expect(await within(painel).findByText(/Defina GEMINI_API_KEY/)).toBeInTheDocument()
  })

  it('confirma a equipe alocando os selecionados', async () => {
    recomendacao()
    let confirmou = false
    servidor.use(
      http.post('/api/recomendacoes/7/confirmacao', async () => {
        confirmou = true
        return HttpResponse.json([umaAlocacao(), umaAlocacao({ id: 11, desenvolvedorId: 2, desenvolvedorNome: 'Bruno Lima' })])
      }),
      http.get('/api/projetos/1', () => HttpResponse.json(umProjeto())),
      http.get('/api/projetos/1/alocacoes', () => HttpResponse.json([])),
      http.get('/api/projetos/1/recomendacoes', () => HttpResponse.json([])),
    )
    const { usuario, router } = await renderizarRota('/recomendacoes/7')

    await usuario.click(await screen.findByRole('button', { name: 'Confirmar equipe' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar a equipe recomendada?' })
    expect(dialogo).toHaveTextContent('Ana Souza')
    expect(dialogo).toHaveTextContent('Bruno Lima')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar e alocar' }))

    await waitFor(() => expect(confirmou).toBe(true))
    expect(await screen.findByText('Equipe confirmada: 2 integrantes alocados no projeto.')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/projetos/1'))
  })

  it('avisa quando a confirmacao e recusada', async () => {
    recomendacao()
    servidor.use(
      http.post('/api/recomendacoes/7/confirmacao', () =>
        HttpResponse.json(
          { status: 422, mensagem: "Conflito de alocacao: 'Ana Souza' tem 0 horas semanais livres, mas a alocacao exige 20." },
          { status: 422 },
        ),
      ),
    )
    const { usuario } = await renderizarRota('/recomendacoes/7')

    await usuario.click(await screen.findByRole('button', { name: 'Confirmar equipe' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar e alocar' }))

    expect(await within(dialogo).findByText(/tem 0 horas semanais livres/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/Gere uma nova recomendação/)).toBeInTheDocument()
  })
})
