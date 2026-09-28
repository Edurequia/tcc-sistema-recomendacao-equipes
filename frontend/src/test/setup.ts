import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { servidor } from './servidor'

// Toda requisicao sem handler falha o teste: nenhum teste depende do backend real.
beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  servidor.resetHandlers()
  localStorage.clear()
  cleanup()
})
afterAll(() => servidor.close())
