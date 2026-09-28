import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import type { Habilidade, HabilidadeRequest, TipoHabilidade } from './tipos'

export interface FiltroHabilidades {
  tipo?: TipoHabilidade
  apenasAtivas: boolean
}

const CHAVE = ['habilidades'] as const

export function useHabilidades(filtro: FiltroHabilidades) {
  return useQuery({
    queryKey: [...CHAVE, filtro],
    queryFn: () => api.get<Habilidade[]>('/habilidades', { tipo: filtro.tipo, apenasAtivas: filtro.apenasAtivas }),
    placeholderData: keepPreviousData,
  })
}

/** Cria (sem id) ou atualiza (com id) uma habilidade. */
export function useSalvarHabilidade() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dados }: { id?: number; dados: HabilidadeRequest }) =>
      id === undefined ? api.post<Habilidade>('/habilidades', dados) : api.put<Habilidade>(`/habilidades/${id}`, dados),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useInativarHabilidade() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/habilidades/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useReativarHabilidade() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.post<Habilidade>(`/habilidades/${id}/reativacao`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}
