import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { MensagemConversa, RespostaConversa } from '../../api/tipos'
import { umaRecomendacao } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const PERGUNTA: RespostaConversa = {
  status: 'PERGUNTA',
  resposta: 'Para montar a equipe, ainda preciso saber o tamanho da equipe e as horas semanais por integrante.',
  faltantes: ['o tamanho da equipe', 'as horas semanais por integrante'],
  entendido: { nome: 'Portal do Aluno', criticidade: 'ALTA', papeis: ['Backend (2)'], tecnologias: ['Java', 'React'] },
}

const RECOMENDACAO: RespostaConversa = {
  status: 'RECOMENDACAO',
  resposta: 'Criei o projeto "Portal do Aluno" e propus uma equipe de 2 integrante(s). Consulte o ranking completo ou peca a justificativa.',
  faltantes: [],
  entendido: { nome: 'Portal do Aluno', criticidade: 'ALTA', tamanhoEquipe: 2, horasSemanaisPorIntegrante: 20, papeis: ['Backend (2)'], tecnologias: ['Java'] },
  recomendacao: umaRecomendacao({ tamanhoEquipe: 2, observacaoRestricao: undefined }),
}

/** Responde cada turno com a proxima resposta da fila e guarda os historicos recebidos. */
function turnos(...respostas: RespostaConversa[]) {
  const recebidos: MensagemConversa[][] = []
  servidor.use(
    http.post('/api/recomendacoes/conversa/turnos', async ({ request }) => {
      const corpo = (await request.json()) as { mensagens: MensagemConversa[] }
      recebidos.push(corpo.mensagens)
      return HttpResponse.json(respostas[recebidos.length - 1])
    }),
  )
  return recebidos
}

type Usuario = Awaited<ReturnType<typeof renderizarRota>>['usuario']

async function enviar(usuario: Usuario, texto: string) {
  await usuario.type(screen.getByRole('textbox', { name: 'Mensagem' }), texto)
  await usuario.click(screen.getByRole('button', { name: 'Enviar' }))
}

describe('assistente', async () => {
  it('apresenta o assistente com sugestoes de pedido', async () => {
    const { usuario } = await renderizarRota('/assistente')

    expect(screen.getByRole('heading', { level: 1, name: 'Assistente' })).toBeInTheDocument()
    const sugestao = screen.getAllByRole('button', { name: /^Usar sugestão/ })[0]
    await usuario.click(sugestao)

    expect((screen.getByRole('textbox', { name: 'Mensagem' }) as HTMLTextAreaElement).value.length).toBeGreaterThan(20)
  })

  it('pergunta o que falta e mostra o que ja entendeu', async () => {
    const recebidos = turnos(PERGUNTA)
    const { usuario } = await renderizarRota('/assistente')

    await enviar(usuario, 'Preciso de uma equipe para o Portal do Aluno, projeto critico em Java e React.')

    const conversa = screen.getByRole('log', { name: 'Conversa' })
    expect(await within(conversa).findByText(PERGUNTA.resposta)).toBeInTheDocument()
    expect(within(conversa).getByText(/Portal do Aluno, projeto critico/)).toBeInTheDocument()
    expect(recebidos[0]).toEqual([{ autor: 'GESTOR', texto: 'Preciso de uma equipe para o Portal do Aluno, projeto critico em Java e React.' }])

    const entendido = screen.getByRole('region', { name: 'O que já entendi' })
    expect(entendido).toHaveTextContent('Portal do Aluno')
    expect(entendido).toHaveTextContent('Alta')
    expect(entendido).toHaveTextContent('Backend (2)')
    expect(within(entendido).getAllByText('Falta informar')).toHaveLength(2)
    expect(screen.getByRole('textbox', { name: 'Mensagem' })).toHaveValue('')
  })

  it('envia o historico inteiro e mostra a recomendacao gerada', async () => {
    const recebidos = turnos(PERGUNTA, RECOMENDACAO)
    const { usuario } = await renderizarRota('/assistente')

    await enviar(usuario, 'Preciso de uma equipe para o Portal do Aluno.')
    await screen.findByText(PERGUNTA.resposta)
    await enviar(usuario, 'Duas pessoas, 20 horas por semana.')

    const conversa = screen.getByRole('log', { name: 'Conversa' })
    expect(await within(conversa).findByText(RECOMENDACAO.resposta)).toBeInTheDocument()
    expect(recebidos[1]).toEqual([
      { autor: 'GESTOR', texto: 'Preciso de uma equipe para o Portal do Aluno.' },
      { autor: 'ASSISTENTE', texto: PERGUNTA.resposta },
      { autor: 'GESTOR', texto: 'Duas pessoas, 20 horas por semana.' },
    ])
    expect(within(conversa).getByText('Ana Souza')).toBeInTheDocument()
    expect(within(conversa).getByText('Bruno Lima')).toBeInTheDocument()
    expect(within(conversa).getByRole('link', { name: 'Ver recomendação completa' })).toHaveAttribute('href', '/recomendacoes/7')
    expect(screen.getByRole('textbox', { name: 'Mensagem' })).toBeDisabled()
  })

  it('recomeca a conversa', async () => {
    turnos(RECOMENDACAO)
    const { usuario } = await renderizarRota('/assistente')
    await enviar(usuario, 'Portal do Aluno, 2 pessoas, 20 horas, backend Java.')
    await screen.findByText(RECOMENDACAO.resposta)

    await usuario.click(screen.getByRole('button', { name: 'Nova conversa' }))

    expect(screen.queryByText(RECOMENDACAO.resposta)).not.toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Mensagem' })).toBeEnabled()
  })

  it('devolve o texto ao campo quando o turno falha', async () => {
    servidor.use(
      http.post('/api/recomendacoes/conversa/turnos', () =>
        HttpResponse.json({ status: 503, mensagem: 'A integracao com o modelo de linguagem nao esta configurada.' }, { status: 503 }),
      ),
    )
    const { usuario } = await renderizarRota('/assistente')

    await enviar(usuario, 'Preciso de uma equipe.')

    expect(await screen.findByText('A integracao com o modelo de linguagem nao esta configurada.')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Mensagem' })).toHaveValue('Preciso de uma equipe.')
    expect(within(screen.getByRole('log', { name: 'Conversa' })).queryByText('Preciso de uma equipe.')).not.toBeInTheDocument()
  })

  it('envia com Enter e quebra linha com Shift+Enter', async () => {
    const recebidos = turnos(PERGUNTA)
    const { usuario } = await renderizarRota('/assistente')

    const campo = screen.getByRole('textbox', { name: 'Mensagem' })
    await usuario.type(campo, 'Linha um{Shift>}{Enter}{/Shift}linha dois')
    expect(campo).toHaveValue('Linha um\nlinha dois')
    await usuario.keyboard('{Enter}')

    await waitFor(() => expect(recebidos[0]).toEqual([{ autor: 'GESTOR', texto: 'Linha um\nlinha dois' }]))
  })
})
