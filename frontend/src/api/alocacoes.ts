import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import type { Alocacao, AlocacaoRequest } from './tipos'

export const CHAVE_ALOCACOES = ['alocacoes'] as const

/** Equipe alocada (apenas alocacoes ativas) do projeto. */
export function useAlocacoesDoProjeto(projetoId: number) {
  return useQuery({
    queryKey: [...CHAVE_ALOCACOES, 'projeto', projetoId],
    queryFn: () => api.get<Alocacao[]>(`/projetos/${projetoId}/alocacoes`),
  })
}

// Alocar ou encerrar muda as horas livres dos desenvolvedores, que alimentam as recomendacoes.
function useInvalidarAposAlocacao() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: CHAVE_ALOCACOES }),
      queryClient.invalidateQueries({ queryKey: ['desenvolvedores'] }),
    ])
}

export function useAlocar(projetoId: number) {
  const invalidar = useInvalidarAposAlocacao()
  return useMutation({
    mutationFn: (dados: AlocacaoRequest) => api.post<Alocacao>(`/projetos/${projetoId}/alocacoes`, dados),
    onSuccess: invalidar,
  })
}

export function useEncerrarAlocacao() {
  const invalidar = useInvalidarAposAlocacao()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/alocacoes/${id}`),
    onSuccess: invalidar,
  })
}
