import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './cliente'
import { CHAVE_PROJETOS } from './projetos'
import { CHAVE_RECOMENDACOES } from './recomendacoes'
import type { MensagemConversa, RespostaConversa } from './tipos'

/** Limite de mensagens por conversa aceito pelo backend (ConversaRequest). */
export const LIMITE_MENSAGENS = 20

/** Um turno do assistente: envia o historico inteiro, pois o backend nao guarda sessao. */
export function useTurnoConversa() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (mensagens: MensagemConversa[]) => api.post<RespostaConversa>('/recomendacoes/conversa/turnos', { mensagens }),
    onSuccess: (resposta) => {
      if (resposta.recomendacao) {
        // O turno final cria um projeto e uma recomendacao: as listagens mudaram.
        queryClient.setQueryData([...CHAVE_RECOMENDACOES, 'detalhe', resposta.recomendacao.id], resposta.recomendacao)
        return Promise.all([
          queryClient.invalidateQueries({ queryKey: CHAVE_PROJETOS }),
          queryClient.invalidateQueries({ queryKey: [...CHAVE_RECOMENDACOES, 'projeto'] }),
        ])
      }
    },
  })
}
