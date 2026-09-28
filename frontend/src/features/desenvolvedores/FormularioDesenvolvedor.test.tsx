import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { umaHabilidade, umaPagina, umDesenvolvedor } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const JAVA = umaHabilidade()
const REACT = umaHabilidade({ id: 2, nome: 'React', categoria: 'FRAMEWORK' })
const LIDERANCA = umaHabilidade({ id: 3, nome: 'Liderança', tipo: 'SOFT', categoria: 'COMPORTAMENTAL', usadaNoModelo: false })

function catalogoAtivo() {
  servidor.use(
    http.get('/api/habilidades', () => HttpResponse.json([JAVA, REACT, LIDERANCA])),
    http.get('/api/desenvolvedores', () => HttpResponse.json(umaPagina([]))),
  )
}

async function preencherPerfil(usuario: Awaited<ReturnType<typeof renderizarRota>>['usuario']) {
  await usuario.type(screen.getByLabelText(/Nome completo/), 'Diego Alves')
  await usuario.type(screen.getByLabelText(/E-mail/), 'diego@ufn.edu.br')
  await usuario.click(screen.getByRole('combobox', { name: 'Papel principal' }))
  await usuario.click(screen.getByRole('option', { name: 'Full-stack' }))
  await usuario.click(screen.getByRole('button', { name: 'Pleno' }))
  const horas = screen.getByLabelText(/Horas disponíveis/)
  await usuario.clear(horas)
  await usuario.type(horas, '25')
}

async function adicionarCompetencia(usuario: Awaited<ReturnType<typeof renderizarRota>>['usuario'], nome: string) {
  await usuario.click(screen.getByRole('combobox', { name: 'Adicionar competência' }))
  await usuario.click(await screen.findByRole('option', { name: nome }))
}

describe('formulario de desenvolvedor', async () => {
  it('cadastra um desenvolvedor com suas competencias', async () => {
    catalogoAtivo()
    let corpo: unknown
    servidor.use(
      http.post('/api/desenvolvedores', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(umDesenvolvedor({ id: 7, nome: 'Diego Alves' }), { status: 201 })
      }),
    )
    const { usuario, router } = await renderizarRota('/desenvolvedores/novo')
    expect(await screen.findByRole('heading', { level: 1, name: 'Novo desenvolvedor' })).toBeInTheDocument()

    await preencherPerfil(usuario)
    fireEvent.change(screen.getByRole('slider', { name: 'Soft skills' }), { target: { value: 4.5 } })
    await adicionarCompetencia(usuario, 'Java')
    await adicionarCompetencia(usuario, 'Liderança')
    const java = screen.getByRole('group', { name: 'Proficiência em Java' })
    // O Rating decide o valor pela posicao do ponteiro, que o jsdom nao tem; o clique direto no radio equivale ao teclado.
    fireEvent.click(within(java).getByRole('radio', { name: /^4 de 5/ }))
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() =>
      expect(corpo).toEqual({
        nome: 'Diego Alves',
        email: 'diego@ufn.edu.br',
        papelPrincipal: 'FULLSTACK',
        senioridade: 'PLENO',
        softSkillsScore: 4.5,
        horasDisponiveis: 25,
        habilidades: [
          { habilidadeId: 1, proficiencia: 4 },
          { habilidadeId: 3, proficiencia: 3 },
        ],
      }),
    )
    expect(await screen.findByText('Diego Alves cadastrado(a).')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/desenvolvedores'))
  })

  it('nao oferece de novo uma competencia ja adicionada', async () => {
    catalogoAtivo()
    const { usuario } = await renderizarRota('/desenvolvedores/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo desenvolvedor' })

    await adicionarCompetencia(usuario, 'Java')
    await usuario.click(screen.getByRole('combobox', { name: 'Adicionar competência' }))

    expect(await screen.findByRole('option', { name: 'React' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Java' })).not.toBeInTheDocument()
  })

  it('valida os campos antes de enviar', async () => {
    catalogoAtivo()
    let enviou = false
    servidor.use(
      http.post('/api/desenvolvedores', async () => {
        enviou = true
        return HttpResponse.json({})
      }),
    )
    const { usuario } = await renderizarRota('/desenvolvedores/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo desenvolvedor' })

    await usuario.type(screen.getByLabelText(/Nome completo/), 'Di')
    await usuario.type(screen.getByLabelText(/E-mail/), 'diego')
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('O nome deve ter entre 3 e 120 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(screen.getByText('Escolha o papel principal.')).toBeInTheDocument()
    expect(enviou).toBe(false)
  })

  it('mostra no campo o e-mail ja cadastrado', async () => {
    catalogoAtivo()
    servidor.use(
      http.post('/api/desenvolvedores', () =>
        HttpResponse.json({ status: 409, mensagem: "Ja existe um desenvolvedor com o e-mail 'diego@ufn.edu.br'." }, { status: 409 }),
      ),
    )
    const { usuario } = await renderizarRota('/desenvolvedores/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo desenvolvedor' })

    await preencherPerfil(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText("Ja existe um desenvolvedor com o e-mail 'diego@ufn.edu.br'.")).toBeInTheDocument()
    expect(screen.getByLabelText(/E-mail/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('edita o perfil substituindo as competencias', async () => {
    catalogoAtivo()
    const ana = umDesenvolvedor({
      habilidades: [
        { habilidadeId: 1, nome: 'Java', tipo: 'HARD', categoria: 'LINGUAGEM', proficiencia: 5 },
        { habilidadeId: 2, nome: 'React', tipo: 'HARD', categoria: 'FRAMEWORK', proficiencia: 2 },
      ],
    })
    let corpo: unknown
    servidor.use(
      http.get('/api/desenvolvedores/1', () => HttpResponse.json(ana)),
      http.put('/api/desenvolvedores/1', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(ana)
      }),
    )
    const { usuario } = await renderizarRota('/desenvolvedores/1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Ana Souza' })).toBeInTheDocument()
    expect(screen.getByLabelText(/E-mail/)).toHaveValue('ana.souza@ufn.edu.br')
    await usuario.click(screen.getByRole('button', { name: 'Remover React' }))
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() =>
      expect(corpo).toMatchObject({
        nome: 'Ana Souza',
        senioridade: 'SENIOR',
        papelPrincipal: 'BACKEND',
        softSkillsScore: 4.2,
        horasDisponiveis: 30,
        habilidades: [{ habilidadeId: 1, proficiencia: 5 }],
      }),
    )
    expect(await screen.findByText('Ana Souza atualizado(a).')).toBeInTheDocument()
  })

  it('mostra no topo as regras recusadas pelo servidor', async () => {
    catalogoAtivo()
    servidor.use(
      http.post('/api/desenvolvedores', () =>
        HttpResponse.json({ status: 422, mensagem: 'Habilidade(s) inativa(s) nao podem ser vinculadas: [COBOL].' }, { status: 422 }),
      ),
    )
    const { usuario } = await renderizarRota('/desenvolvedores/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo desenvolvedor' })

    await preencherPerfil(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Habilidade(s) inativa(s) nao podem ser vinculadas: [COBOL].')
  })

  it('avisa quando o desenvolvedor nao existe', async () => {
    catalogoAtivo()
    servidor.use(
      http.get('/api/desenvolvedores/99', () =>
        HttpResponse.json({ status: 404, mensagem: 'Desenvolvedor de id 99 nao encontrado(a).' }, { status: 404 }),
      ),
    )
    await renderizarRota('/desenvolvedores/99')

    expect(await screen.findByText('Desenvolvedor de id 99 nao encontrado(a).')).toBeInTheDocument()
  })
})
