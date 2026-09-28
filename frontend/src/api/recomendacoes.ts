import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CHAVE_ALOCACOES } from './alocacoes'
import { api } from './cliente'
import type { Alocacao, Justificativa, LinhaBase, Recomendacao, RecomendacaoResumo } from './tipos'

export const CHAVE_RECOMENDACOES = ['recomendacoes'] as const

export function useRecomendacoesDoProjeto(projetoId: number) {
  return useQuery({
    queryKey: [...CHAVE_RECOMENDACOES, 'projeto', projetoId],
    queryFn: () => api.get<RecomendacaoResumo[]>(`/projetos/${projetoId}/recomendacoes`),
  })
}

export function useRecomendacao(id: number) {
  return useQuery({
    queryKey: [...CHAVE_RECOMENDACOES, 'detalhe', id],
    queryFn: () => api.get<Recomendacao>(`/recomendacoes/${id}`),
  })
}

/** Pontua os ativos no motor de predicao e compoe a equipe; pode levar alguns segundos. */
export function useGerarRecomendacao(projetoId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<Recomendacao>(`/projetos/${projetoId}/recomendacoes`),
    onSuccess: (gerada) => {
      queryClient.setQueryData([...CHAVE_RECOMENDACOES, 'detalhe', gerada.id], gerada)
      return queryClient.invalidateQueries({ queryKey: [...CHAVE_RECOMENDACOES, 'projeto', projetoId] })
    },
  })
}

export function useLinhaBase(id: number, sorteios: number, semente: number) {
  return useQuery({
    queryKey: [...CHAVE_RECOMENDACOES, 'linha-base', id, sorteios, semente],
    queryFn: () => api.get<LinhaBase>(`/recomendacoes/${id}/linha-base`, { sorteios, semente }),
    placeholderData: keepPreviousData,
    // O resultado so muda com outros parametros: a semente fixa o sorteio.
    staleTime: Infinity,
  })
}

export function useJustificar(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (regerar: boolean) => api.post<Justificativa>(`/recomendacoes/${id}/justificativa`, undefined, { regerar }),
    onSuccess: (resposta) =>
      queryClient.setQueryData<Recomendacao>([...CHAVE_RECOMENDACOES, 'detalhe', id], (atual) =>
        atual ? { ...atual, justificativaLlm: resposta.justificativa } : atual,
      ),
  })
}

/** Aloca todos os selecionados de uma vez (tudo ou nada). */
export function useConfirmarRecomendacao(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<Alocacao[]>(`/recomendacoes/${id}/confirmacao`),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: CHAVE_ALOCACOES }),
        queryClient.invalidateQueries({ queryKey: ['desenvolvedores'] }),
      ]),
  })
}
