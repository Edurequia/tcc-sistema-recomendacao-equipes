import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { Habilidade } from '../../api/tipos'
import { umaHabilidade } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const JAVA = umaHabilidade()
const LIDERANCA = umaHabilidade({
  id: 2,
  nome: 'Liderança',
  tipo: 'SOFT',
  tipoDescricao: 'Competencia comportamental',
  categoria: 'COMPORTAMENTAL',
  categoriaDescricao: 'Competencia comportamental',
  chaveDataset: undefined,
  usadaNoModelo: false,
})
const COBOL = umaHabilidade({ id: 3, nome: 'COBOL', chaveDataset: undefined, usadaNoModelo: false, ativa: false })

/** Responde a listagem respeitando os filtros e devolve as URLs recebidas. */
function catalogo(habilidades: Habilidade[]) {
  const urls: URL[] = []
  servidor.use(
    http.get('/api/habilidades', ({ request }) => {
      const url = new URL(request.url)
      urls.push(url)
      const tipo = url.searchParams.get('tipo')
      const apenasAtivas = url.searchParams.get('apenasAtivas') !== 'false'
      return HttpResponse.json(habilidades.filter((h) => (!tipo || h.tipo === tipo) && (!apenasAtivas || h.ativa)))
    }),
  )
  return urls
}

async function linhaDe(nome: string) {
  const celula = await screen.findByRole('cell', { name: new RegExp(`^${nome}`) })
  return celula.closest('tr')!
}

describe('pagina de habilidades', async () => {
  it('lista o catalogo de habilidades ativas', async () => {
    const urls = catalogo([JAVA, LIDERANCA, COBOL])
    await renderizarRota('/habilidades')

    const java = await linhaDe('Java')
    expect(java).toHaveTextContent('Técnica')
    expect(java).toHaveTextContent('Linguagem')
    expect(within(java).getByText('No modelo')).toBeInTheDocument()
    expect(await linhaDe('Liderança')).toHaveTextContent('Comportamental')
    expect(screen.queryByText('COBOL')).not.toBeInTheDocument()
    expect(screen.getByText('2 habilidades')).toBeInTheDocument()
    expect(urls[0].searchParams.get('apenasAtivas')).toBe('true')
  })

  it('filtra pelo tipo no servidor', async () => {
    const urls = catalogo([JAVA, LIDERANCA])
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('button', { name: 'Comportamentais' }))

    await waitFor(() => expect(screen.queryByText('Java')).not.toBeInTheDocument())
    expect(await linhaDe('Liderança')).toBeInTheDocument()
    expect(urls.at(-1)!.searchParams.get('tipo')).toBe('SOFT')
  })

  it('busca pelo nome sem diferenciar maiusculas nem acentos', async () => {
    catalogo([JAVA, LIDERANCA])
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.type(screen.getByRole('searchbox', { name: 'Buscar habilidade' }), 'lider')

    expect(screen.queryByText('Java')).not.toBeInTheDocument()
    expect(await linhaDe('Liderança')).toBeInTheDocument()
  })

  it('mostra as inativas quando pedido e permite reativar', async () => {
    catalogo([JAVA, COBOL])
    let reativada: string | undefined
    servidor.use(
      http.post('/api/habilidades/:id/reativacao', ({ params }) => {
        reativada = params.id as string
        return HttpResponse.json({ ...COBOL, ativa: true })
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('switch', { name: 'Mostrar inativas' }))
    const cobol = await linhaDe('COBOL')
    expect(cobol).toHaveTextContent('Inativa')

    await usuario.click(within(cobol).getByRole('button', { name: 'Reativar COBOL' }))

    await waitFor(() => expect(reativada).toBe('3'))
    expect(await screen.findByText('COBOL reativada.')).toBeInTheDocument()
  })

  it('cadastra uma nova habilidade', async () => {
    catalogo([JAVA])
    let corpo: unknown
    servidor.use(
      http.post('/api/habilidades', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(umaHabilidade({ id: 9, nome: 'Kotlin' }), { status: 201 })
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('button', { name: 'Nova habilidade' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nova habilidade' })
    await usuario.type(within(dialogo).getByLabelText(/Nome/), 'Kotlin')
    await usuario.click(within(dialogo).getByRole('combobox', { name: 'Categoria' }))
    await usuario.click(screen.getByRole('option', { name: 'Linguagem' }))
    await usuario.type(within(dialogo).getByLabelText(/Chave no dataset/), 'Kotlin')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(corpo).toEqual({ nome: 'Kotlin', tipo: 'HARD', categoria: 'LINGUAGEM', chaveDataset: 'Kotlin' }))
    expect(await screen.findByText('Kotlin cadastrada.')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('valida o formulario antes de enviar', async () => {
    catalogo([JAVA])
    let enviou = false
    servidor.use(
      http.post('/api/habilidades', async () => {
        enviou = true
        return HttpResponse.json({})
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('button', { name: 'Nova habilidade' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cadastrar' }))

    expect(await within(dialogo).findByText('Informe o nome.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Escolha a categoria.')).toBeInTheDocument()
    expect(enviou).toBe(false)
  })

  it('mostra no campo o conflito de nome devolvido pelo servidor', async () => {
    catalogo([JAVA])
    servidor.use(
      http.post('/api/habilidades', () =>
        HttpResponse.json({ status: 409, mensagem: "Ja existe uma habilidade chamada 'Java'." }, { status: 409 }),
      ),
    )
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('button', { name: 'Nova habilidade' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.type(within(dialogo).getByLabelText(/Nome/), 'Java')
    await usuario.click(within(dialogo).getByRole('combobox', { name: 'Categoria' }))
    await usuario.click(screen.getByRole('option', { name: 'Linguagem' }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cadastrar' }))

    expect(await within(dialogo).findByText("Ja existe uma habilidade chamada 'Java'.")).toBeInTheDocument()
    expect(within(dialogo).getByLabelText(/Nome/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('mostra no topo do formulario o conflito que nao e de nome', async () => {
    catalogo([JAVA])
    servidor.use(
      http.post('/api/habilidades', () =>
        HttpResponse.json(
          { status: 409, mensagem: 'A operacao viola uma restricao de integridade (registro duplicado ou em uso).' },
          { status: 409 },
        ),
      ),
    )
    const { usuario } = await renderizarRota('/habilidades')
    await linhaDe('Java')

    await usuario.click(screen.getByRole('button', { name: 'Nova habilidade' }))
    const dialogo = await screen.findByRole('dialog')
    await usuario.type(within(dialogo).getByLabelText(/Nome/), 'Java Novo')
    await usuario.click(within(dialogo).getByRole('combobox', { name: 'Categoria' }))
    await usuario.click(screen.getByRole('option', { name: 'Linguagem' }))
    await usuario.type(within(dialogo).getByLabelText(/Chave no dataset/), 'Java')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cadastrar' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('registro duplicado')
    expect(within(dialogo).getByLabelText(/Nome/)).toHaveAttribute('aria-invalid', 'false')
  })

  it('edita uma habilidade existente', async () => {
    catalogo([JAVA])
    let corpo: unknown
    servidor.use(
      http.put('/api/habilidades/1', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json({ ...JAVA, nome: 'Java 21' })
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')

    await usuario.click(within(await linhaDe('Java')).getByRole('button', { name: 'Editar Java' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Editar habilidade' })
    const nome = within(dialogo).getByLabelText(/Nome/)
    expect(nome).toHaveValue('Java')
    await usuario.clear(nome)
    await usuario.type(nome, 'Java 21')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(corpo).toEqual({ nome: 'Java 21', tipo: 'HARD', categoria: 'LINGUAGEM', chaveDataset: 'Java' }))
    expect(await screen.findByText('Java 21 atualizada.')).toBeInTheDocument()
  })

  it('pede confirmacao antes de inativar', async () => {
    catalogo([JAVA])
    let inativou = false
    servidor.use(
      http.delete('/api/habilidades/1', async () => {
        inativou = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')

    await usuario.click(within(await linhaDe('Java')).getByRole('button', { name: 'Inativar Java' }))
    const confirmacao = await screen.findByRole('dialog', { name: 'Inativar Java?' })
    expect(inativou).toBe(false)
    await usuario.click(within(confirmacao).getByRole('button', { name: 'Inativar' }))

    await waitFor(() => expect(inativou).toBe(true))
    expect(await screen.findByText('Java inativada.')).toBeInTheDocument()
  })

  it('mostra um estado vazio quando nao ha habilidades', async () => {
    catalogo([])
    await renderizarRota('/habilidades')

    expect(await screen.findByText('Nenhuma habilidade encontrada')).toBeInTheDocument()
  })

  it('mostra o erro de carregamento e permite tentar de novo', async () => {
    let tentativas = 0
    servidor.use(
      http.get('/api/habilidades', async () => {
        tentativas += 1
        return tentativas === 1 ? HttpResponse.error() : HttpResponse.json([JAVA])
      }),
    )
    const { usuario } = await renderizarRota('/habilidades')

    expect(await screen.findByText(/Não foi possível contatar o servidor/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await linhaDe('Java')).toBeInTheDocument()
  })
})
