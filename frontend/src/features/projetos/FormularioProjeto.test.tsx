import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { umaHabilidade, umProjeto } from '../../test/fixtures'
import { renderizarRota } from '../../test/renderizar'
import { servidor } from '../../test/servidor'

const JAVA = umaHabilidade()
const REACT = umaHabilidade({ id: 2, nome: 'React', categoria: 'FRAMEWORK' })
const LIDERANCA = umaHabilidade({ id: 3, nome: 'Liderança', tipo: 'SOFT', categoria: 'COMPORTAMENTAL' })

type Usuario = Awaited<ReturnType<typeof renderizarRota>>['usuario']

/** Catalogo ativo que respeita o filtro por tipo, como o backend. */
function catalogo() {
  servidor.use(
    http.get('/api/habilidades', ({ request }) => {
      const tipo = new URL(request.url).searchParams.get('tipo')
      return HttpResponse.json([JAVA, REACT, LIDERANCA].filter((h) => !tipo || h.tipo === tipo))
    }),
  )
}

async function preencherIdentificacao(usuario: Usuario, nome = 'Portal do Aluno') {
  await usuario.type(screen.getByLabelText(/Nome do projeto/), nome)
  await usuario.click(screen.getByRole('button', { name: 'Alta' }))
}

async function definirNumero(usuario: Usuario, rotulo: RegExp | string, valor: string) {
  const campo = screen.getByLabelText(rotulo)
  await usuario.clear(campo)
  await usuario.type(campo, valor)
}

async function adicionarPapel(usuario: Usuario, papel: string, quantidade: string) {
  await usuario.click(screen.getByRole('button', { name: 'Adicionar papel' }))
  const vagas = screen.getAllByRole('group', { name: /^Vaga / }).at(-1)!
  await usuario.click(within(vagas).getByRole('combobox', { name: 'Papel' }))
  await usuario.click(screen.getByRole('option', { name: papel }))
  const campo = within(vagas).getByLabelText('Quantidade')
  await usuario.clear(campo)
  await usuario.type(campo, quantidade)
}

describe('formulario de projeto', async () => {
  it('cadastra um projeto com vagas por papel e stack exigida', async () => {
    catalogo()
    let corpo: unknown
    servidor.use(
      http.post('/api/projetos', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(umProjeto({ id: 5 }), { status: 201 })
      }),
      http.get('/api/projetos/5', () => HttpResponse.json(umProjeto({ id: 5 }))),
      http.get('/api/projetos/5/alocacoes', () => HttpResponse.json([])),
      http.get('/api/projetos/5/recomendacoes', () => HttpResponse.json([])),
    )
    const { usuario, router } = await renderizarRota('/projetos/novo')
    expect(await screen.findByRole('heading', { level: 1, name: 'Novo projeto' })).toBeInTheDocument()

    await preencherIdentificacao(usuario)
    await definirNumero(usuario, /Tamanho da equipe/, '4')
    await definirNumero(usuario, /Horas semanais por integrante/, '20')
    await adicionarPapel(usuario, 'Backend', '2')
    await adicionarPapel(usuario, 'Frontend', '1')
    expect(screen.getByText('3 de 4 vagas com papel definido · 1 livre')).toBeInTheDocument()

    await usuario.click(screen.getByRole('combobox', { name: 'Adicionar tecnologia' }))
    await usuario.click(await screen.findByRole('option', { name: 'Java' }))
    const java = screen.getByRole('group', { name: 'Requisito Java' })
    await usuario.click(within(java).getByRole('button', { name: '5' }))
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar projeto' }))

    await waitFor(() =>
      expect(corpo).toEqual({
        nome: 'Portal do Aluno',
        descricao: null,
        criticidade: 'ALTA',
        tamanhoEquipe: 4,
        horasSemanaisPorIntegrante: 20,
        dataInicioPrevista: null,
        dataFimPrevista: null,
        papeisRequeridos: [
          { papel: 'BACKEND', quantidade: 2 },
          { papel: 'FRONTEND', quantidade: 1 },
        ],
        habilidadesRequeridas: [{ habilidadeId: 1, obrigatoria: true, peso: 5 }],
      }),
    )
    expect(await screen.findByText('Projeto Portal do Aluno cadastrado.')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/projetos/5'))
  })

  it('oferece so tecnologias na stack e nao repete papeis', async () => {
    catalogo()
    const { usuario } = await renderizarRota('/projetos/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo projeto' })

    await adicionarPapel(usuario, 'Backend', '1')
    await usuario.click(screen.getByRole('button', { name: 'Adicionar papel' }))
    const segunda = screen.getAllByRole('group', { name: /^Vaga / }).at(-1)!
    await usuario.click(within(segunda).getByRole('combobox', { name: 'Papel' }))
    // O menu do primeiro papel pode ainda estar em transicao de saida: confere so o aberto agora.
    const menu = screen.getAllByRole('listbox').at(-1)!
    expect(within(menu).getByRole('option', { name: 'Frontend' })).toBeInTheDocument()
    expect(within(menu).queryByRole('option', { name: 'Backend' })).not.toBeInTheDocument()
    await usuario.keyboard('{Escape}')

    await usuario.click(screen.getByRole('combobox', { name: 'Adicionar tecnologia' }))
    expect(await screen.findByRole('option', { name: 'React' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Liderança' })).not.toBeInTheDocument()
  })

  it('recusa vagas acima do tamanho da equipe e datas invertidas', async () => {
    catalogo()
    let enviou = false
    servidor.use(
      http.post('/api/projetos', async () => {
        enviou = true
        return HttpResponse.json({})
      }),
    )
    const { usuario } = await renderizarRota('/projetos/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo projeto' })

    await preencherIdentificacao(usuario)
    await definirNumero(usuario, /Tamanho da equipe/, '2')
    await adicionarPapel(usuario, 'Backend', '3')
    await usuario.type(screen.getByLabelText(/Início previsto/), '2027-01-10')
    await usuario.type(screen.getByLabelText(/Fim previsto/), '2026-12-01')
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar projeto' }))

    expect(await screen.findByText('As vagas por papel somam 3, mas a equipe tem 2 integrantes.')).toBeInTheDocument()
    expect(screen.getByText('O fim previsto não pode ser anterior ao início.')).toBeInTheDocument()
    expect(enviou).toBe(false)
  })

  it('mostra no campo o nome ja usado por outro projeto', async () => {
    catalogo()
    servidor.use(
      http.post('/api/projetos', () =>
        HttpResponse.json({ status: 409, mensagem: "Ja existe um projeto chamado 'Portal do Aluno'." }, { status: 409 }),
      ),
    )
    const { usuario } = await renderizarRota('/projetos/novo')
    await screen.findByRole('heading', { level: 1, name: 'Novo projeto' })

    await preencherIdentificacao(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar projeto' }))

    expect(await screen.findByText("Ja existe um projeto chamado 'Portal do Aluno'.")).toBeInTheDocument()
    expect(screen.getByLabelText(/Nome do projeto/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('edita um projeto mantendo os requisitos cadastrados', async () => {
    catalogo()
    const portal = umProjeto()
    let corpo: unknown
    servidor.use(
      http.get('/api/projetos/1', () => HttpResponse.json(portal)),
      http.get('/api/projetos/1/alocacoes', () => HttpResponse.json([])),
      http.get('/api/projetos/1/recomendacoes', () => HttpResponse.json([])),
      http.put('/api/projetos/1', async ({ request }) => {
        corpo = await request.json()
        return HttpResponse.json(portal)
      }),
    )
    const { usuario } = await renderizarRota('/projetos/1/editar')

    expect(await screen.findByRole('heading', { level: 1, name: 'Editar Portal do Aluno' })).toBeInTheDocument()
    expect(screen.getByLabelText(/Nome do projeto/)).toHaveValue('Portal do Aluno')
    await usuario.click(screen.getByRole('switch', { name: 'React obrigatória' }))
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() =>
      expect(corpo).toEqual({
        nome: 'Portal do Aluno',
        descricao: 'Reescrita do portal do aluno.',
        criticidade: 'ALTA',
        tamanhoEquipe: 4,
        horasSemanaisPorIntegrante: 20,
        dataInicioPrevista: '2026-10-01',
        dataFimPrevista: '2027-03-31',
        papeisRequeridos: [
          { papel: 'BACKEND', quantidade: 2 },
          { papel: 'FRONTEND', quantidade: 1 },
        ],
        habilidadesRequeridas: [
          { habilidadeId: 1, obrigatoria: true, peso: 5 },
          { habilidadeId: 2, obrigatoria: true, peso: 3 },
        ],
      }),
    )
    expect(await screen.findByText('Projeto Portal do Aluno atualizado.')).toBeInTheDocument()
  })
})
