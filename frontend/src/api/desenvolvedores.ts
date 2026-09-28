import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import type { Desenvolvedor, DesenvolvedorRequest, DesenvolvedorResumo, Pagina, PapelAtuacao, Senioridade } from './tipos'

export interface FiltroDesenvolvedores {
  nome?: string
  senioridade?: Senioridade
  papel?: PapelAtuacao
  /** Ausente, traz ativos e inativos. */
  ativo?: boolean
  pagina: number
  tamanho: number
}

const CHAVE = ['desenvolvedores'] as const

export function useDesenvolvedores(filtro: FiltroDesenvolvedores) {
  return useQuery({
    queryKey: [...CHAVE, 'lista', filtro],
    queryFn: () =>
      api.get<Pagina<DesenvolvedorResumo>>('/desenvolvedores', {
        nome: filtro.nome,
        senioridade: filtro.senioridade,
        papel: filtro.papel,
        ativo: filtro.ativo,
        page: filtro.pagina,
        size: filtro.tamanho,
      }),
    placeholderData: keepPreviousData,
  })
}

export function useDesenvolvedor(id: number | undefined) {
  return useQuery({
    queryKey: [...CHAVE, 'detalhe', id],
    queryFn: () => api.get<Desenvolvedor>(`/desenvolvedores/${id}`),
    enabled: id !== undefined,
  })
}

/** Cria (sem id) ou atualiza o perfil completo (com id). */
export function useSalvarDesenvolvedor() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dados }: { id?: number; dados: DesenvolvedorRequest }) =>
      id === undefined
        ? api.post<Desenvolvedor>('/desenvolvedores', dados)
        : api.put<Desenvolvedor>(`/desenvolvedores/${id}`, dados),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useAtualizarDisponibilidade() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, horasDisponiveis }: { id: number; horasDisponiveis: number }) =>
      api.patch<Desenvolvedor>(`/desenvolvedores/${id}/disponibilidade`, { horasDisponiveis }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useInativarDesenvolvedor() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/desenvolvedores/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}

export function useReativarDesenvolvedor() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.post<Desenvolvedor>(`/desenvolvedores/${id}/reativacao`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAVE }),
  })
}
