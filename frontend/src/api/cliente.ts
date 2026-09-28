import type { ApiErrorBody, CampoInvalido } from './tipos'

const BASE = '/api'
const SEM_CONEXAO = 'Não foi possível contatar o servidor. Verifique se o backend está em execução.'

/**
 * Erro de chamada a API, ja no formato que as telas exibem. `status` 0 indica que o
 * backend nao respondeu; `campos` traz os erros de validacao por campo, quando houver.
 */
export class ErroApi extends Error {
  readonly status: number
  readonly campos: CampoInvalido[]

  constructor(status: number, mensagem: string, campos: CampoInvalido[] = []) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.campos = campos
  }
}

type ValorParametro = string | number | boolean | null | undefined
export type Parametros = Record<string, ValorParametro>

function montarUrl(caminho: string, parametros?: Parametros): string {
  const busca = new URLSearchParams()
  for (const [chave, valor] of Object.entries(parametros ?? {})) {
    if (valor !== undefined && valor !== null && valor !== '') {
      busca.set(chave, String(valor))
    }
  }
  const query = busca.toString()
  return `${BASE}${caminho}${query ? `?${query}` : ''}`
}

async function lerErro(resposta: Response): Promise<ErroApi> {
  try {
    const corpo = (await resposta.json()) as ApiErrorBody
    if (corpo?.mensagem) {
      return new ErroApi(resposta.status, corpo.mensagem, corpo.campos ?? [])
    }
  } catch {
    // corpo ausente ou fora do formato ApiError: cai na mensagem generica
  }
  // Sem corpo do backend, 502/504 vem do proxy (Vite ou reverso): o backend nao respondeu.
  if (resposta.status === 502 || resposta.status === 504) {
    return new ErroApi(0, SEM_CONEXAO)
  }
  return new ErroApi(resposta.status, `O servidor respondeu com erro (HTTP ${resposta.status}).`)
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown, parametros?: Parametros): Promise<T> {
  let resposta: Response
  try {
    resposta = await fetch(montarUrl(caminho, parametros), {
      method: metodo,
      headers: corpo === undefined ? { Accept: 'application/json' } : { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new ErroApi(0, SEM_CONEXAO)
  }

  if (!resposta.ok) {
    throw await lerErro(resposta)
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  const texto = await resposta.text()
  return (texto ? JSON.parse(texto) : undefined) as T
}

export const api = {
  get: <T>(caminho: string, parametros?: Parametros) => requisitar<T>('GET', caminho, undefined, parametros),
  post: <T>(caminho: string, corpo?: unknown, parametros?: Parametros) => requisitar<T>('POST', caminho, corpo, parametros),
  put: <T>(caminho: string, corpo: unknown) => requisitar<T>('PUT', caminho, corpo),
  patch: <T>(caminho: string, corpo: unknown) => requisitar<T>('PATCH', caminho, corpo),
  delete: <T = void>(caminho: string) => requisitar<T>('DELETE', caminho),
}
