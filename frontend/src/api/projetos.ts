import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import type { CriticidadeProjeto, Pagina, Projeto, ProjetoRequest, ProjetoResumo, StatusProjeto } from './tipos'

export interface FiltroProjetos {
  nome?: string
  status?: StatusProjeto
  criticidade?: CriticidadeProjeto
  pagina: number
  tamanho: number
}

export const CHAVE_PROJETOS = ['projetos'] as const

/** Status em que o projeto ainda disputa desenvolvedores e aceita alocacoes. */
export function projetoAtivo(status: StatusProjeto): boolean {
  return status === 'PLANEJAMENTO' || status === 'EM_ANDAMENTO'
}

export function useProjetos(filtro: FiltroProjetos) {
  return useQuery({
    queryKey: [...CHAVE_PROJETOS, 'lista', filtro],
    queryFn: () =>
      api.get<Pagina<ProjetoResumo>>('/projetos', {
        nome: filtro.nome,
        status: filtro.status,
        criticidade: filtro.criticidade,
        page: filtro.pagina,
        size: filtro.tamanho,
      }),
    placeholderData: keepPreviousData,
  })
}

export function useProjeto(id: number | undefined) {
  return useQuery({
    queryKey: [...CHAVE_PROJETOS, 'detalhe', id],
    queryFn: () => api.get<Projeto>(`/projetos/${id}`),
    enabled: id !== undefined,
  })
}

/** Cria (sem id) ou atualiza o cadastro completo (com id). */
export function useSalvarProjeto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dados }: { id?: number; dados: ProjetoRequest }) =>
      id === undefined ? api.post<Projeto>('/projetos', dados) : api.put<Projeto>(`/projetos/${id}`, dados),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE_PROJETOS }),
  })
}

export function useAlterarStatusProjeto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: StatusProjeto }) => api.patch<Projeto>(`/projetos/${id}/status`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE_PROJETOS }),
  })
}

export function useCancelarProjeto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/projetos/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE_PROJETOS }),
  })
}
