import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { umaPagina } from './fixtures'

// Listagens vazias por padrao, para que qualquer pagina renderize sem backend.
// Cada teste sobrescreve o que precisa com `servidor.use`.
const handlersPadrao = [
  http.get('/api/habilidades', () => HttpResponse.json([])),
  http.get('/api/desenvolvedores', () => HttpResponse.json(umaPagina([]))),
  http.get('/api/projetos', () => HttpResponse.json(umaPagina([]))),
]

export const servidor = setupServer(...handlersPadrao)
