import type { AlertColor } from '@mui/material/Alert'
import { createContext, useContext } from 'react'

export type Notificar = (mensagem: string, gravidade?: AlertColor) => void

export const ContextoNotificacoes = createContext<Notificar>(() => {})

/** Mostra um aviso curto no canto da tela (ver NotificacoesProvider). */
export function useNotificar(): Notificar {
  return useContext(ContextoNotificacoes)
}
