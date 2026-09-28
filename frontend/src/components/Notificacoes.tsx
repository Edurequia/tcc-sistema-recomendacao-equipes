import Alert, { type AlertColor } from '@mui/material/Alert'
import Snackbar from '@mui/material/Snackbar'
import { useCallback, useState, type ReactNode } from 'react'
import { ContextoNotificacoes, type Notificar } from './notificar'

interface Notificacao {
  id: number
  mensagem: string
  gravidade: AlertColor
}

/** Avisos curtos no canto da tela, para confirmar acoes concluidas ou falhas pontuais. */
export function NotificacoesProvider({ children }: { children: ReactNode }) {
  const [atual, setAtual] = useState<Notificacao | null>(null)

  const notificar = useCallback<Notificar>((mensagem, gravidade = 'success') => {
    setAtual({ id: Date.now(), mensagem, gravidade })
  }, [])

  return (
    <ContextoNotificacoes.Provider value={notificar}>
      {children}
      <Snackbar
        key={atual?.id}
        open={atual !== null}
        autoHideDuration={5000}
        onClose={(_, motivo) => motivo !== 'clickaway' && setAtual(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        {atual ? (
          <Alert severity={atual.gravidade} variant="filled" onClose={() => setAtual(null)} sx={{ minWidth: 280 }}>
            {atual.mensagem}
          </Alert>
        ) : undefined}
      </Snackbar>
    </ContextoNotificacoes.Provider>
  )
}

