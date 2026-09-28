import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderizarRota } from './test/renderizar'

describe('layout e rotas', async () => {
  it('mostra o menu com as areas do sistema', async () => {
    await renderizarRota('/projetos')

    const menu = screen.getByRole('navigation', { name: 'Menu principal' })
    for (const area of ['Projetos', 'Desenvolvedores', 'Habilidades', 'Assistente']) {
      expect(menu).toHaveTextContent(area)
    }
  })

  it('redireciona a raiz para a lista de projetos', async () => {
    const { router } = await renderizarRota('/')

    await waitFor(() => expect(router.state.location.pathname).toBe('/projetos'))
    expect(await screen.findByRole('heading', { level: 1, name: 'Projetos' })).toBeInTheDocument()
  })

  it('navega pelo menu', async () => {
    const { router, usuario } = await renderizarRota('/projetos')

    await usuario.click(screen.getByRole('link', { name: 'Desenvolvedores' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/desenvolvedores'))
    expect(await screen.findByRole('heading', { level: 1, name: 'Desenvolvedores' })).toBeInTheDocument()
  })

  it('marca o item do menu da area atual', async () => {
    await renderizarRota('/habilidades')

    expect(screen.getByRole('link', { name: 'Habilidades' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Projetos' })).not.toHaveAttribute('aria-current')
  })

  it('permite escolher o tema claro, escuro ou o do sistema', async () => {
    const { usuario } = await renderizarRota('/projetos')

    const grupo = screen.getByRole('group', { name: 'Tema' })
    const escuro = screen.getByRole('button', { name: 'Tema escuro' })
    expect(grupo).toContainElement(escuro)
    expect(screen.getByRole('button', { name: 'Tema do sistema' })).toHaveAttribute('aria-pressed', 'true')

    await usuario.click(escuro)

    expect(escuro).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Tema do sistema' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('avisa quando a pagina nao existe', async () => {
    await renderizarRota('/nao-existe')

    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar para os projetos' })).toHaveAttribute('href', '/projetos')
  })
})
