import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { servidor } from '../test/servidor'
import { api, ErroApi } from './cliente'

describe('cliente da API', () => {
  it('devolve o corpo JSON de uma resposta de sucesso', async () => {
    servidor.use(http.get('/api/habilidades', () => HttpResponse.json([{ id: 1, nome: 'Java' }])))

    await expect(api.get('/habilidades')).resolves.toEqual([{ id: 1, nome: 'Java' }])
  })

  it('monta a query string ignorando parametros vazios', async () => {
    let urlRecebida = ''
    servidor.use(
      http.get('/api/desenvolvedores', ({ request }) => {
        urlRecebida = request.url
        return HttpResponse.json({})
      }),
    )

    await api.get('/desenvolvedores', { nome: 'ana', papel: undefined, ativo: false, pagina: null, page: 0 })

    const params = new URL(urlRecebida).searchParams
    expect(Object.fromEntries(params)).toEqual({ nome: 'ana', ativo: 'false', page: '0' })
  })

  it('envia o corpo como JSON no POST', async () => {
    let corpo: unknown
    servidor.use(
      http.post('/api/habilidades', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ id: 7 }, { status: 201 })
      }),
    )

    await expect(api.post('/habilidades', { nome: 'Go' })).resolves.toEqual({ id: 7 })
    expect(corpo).toEqual({ nome: 'Go' })
  })

  it('devolve undefined em resposta sem conteudo', async () => {
    servidor.use(http.delete('/api/alocacoes/3', () => new HttpResponse(null, { status: 204 })))

    await expect(api.delete('/alocacoes/3')).resolves.toBeUndefined()
  })

  it('converte o ApiError do backend em ErroApi com os campos invalidos', async () => {
    servidor.use(
      http.post('/api/desenvolvedores', () =>
        HttpResponse.json(
          {
            status: 400,
            erro: 'Erro de validacao',
            mensagem: 'Ha campos invalidos.',
            campos: [{ campo: 'email', motivo: 'Informe um e-mail valido.' }],
          },
          { status: 400 },
        ),
      ),
    )

    const erro = await api.post('/desenvolvedores', {}).catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({
      status: 400,
      message: 'Ha campos invalidos.',
      campos: [{ campo: 'email', motivo: 'Informe um e-mail valido.' }],
    })
  })

  it('usa uma mensagem generica quando o erro nao traz corpo', async () => {
    servidor.use(http.get('/api/projetos/9', () => new HttpResponse('falhou', { status: 500 })))

    const erro = await api.get('/projetos/9').catch((e: unknown) => e)

    expect(erro).toMatchObject({ status: 500, message: 'O servidor respondeu com erro (HTTP 500).', campos: [] })
  })

  it('trata o 502 do proxy como backend fora do ar', async () => {
    // O proxy do Vite responde 502 sem corpo quando o backend nao esta em execucao.
    servidor.use(http.get('/api/habilidades', () => new HttpResponse(null, { status: 502 })))

    const erro = await api.get('/habilidades').catch((e: unknown) => e)

    expect(erro).toMatchObject({
      status: 0,
      message: 'Não foi possível contatar o servidor. Verifique se o backend está em execução.',
    })
  })

  it('mantem a mensagem do backend num 503 com corpo', async () => {
    servidor.use(
      http.post('/api/projetos/1/recomendacoes', () =>
        HttpResponse.json({ status: 503, mensagem: 'Motor de predicao indisponivel.' }, { status: 503 }),
      ),
    )

    const erro = await api.post('/projetos/1/recomendacoes').catch((e: unknown) => e)

    expect(erro).toMatchObject({ status: 503, message: 'Motor de predicao indisponivel.' })
  })

  it('explica quando o backend esta fora do ar', async () => {
    servidor.use(http.get('/api/projetos', () => HttpResponse.error()))

    const erro = await api.get('/projetos').catch((e: unknown) => e)

    expect(erro).toMatchObject({
      status: 0,
      message: 'Não foi possível contatar o servidor. Verifique se o backend está em execução.',
    })
  })
})
