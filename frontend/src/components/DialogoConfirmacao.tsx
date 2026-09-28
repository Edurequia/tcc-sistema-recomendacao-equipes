import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import { useId, type ReactNode } from 'react'

interface Props {
  aberto: boolean
  titulo: string
  descricao: ReactNode
  rotuloConfirmar: string
  destrutivo?: boolean
  processando?: boolean
  aoConfirmar: () => void
  aoCancelar: () => void
}

/** Confirmacao de acao que muda dados; substitui o window.confirm do navegador. */
export function DialogoConfirmacao({ aberto, titulo, descricao, rotuloConfirmar, destrutivo, processando, aoConfirmar, aoCancelar }: Props) {
  const idTitulo = useId()
  return (
    <Dialog open={aberto} onClose={processando ? undefined : aoCancelar} aria-labelledby={idTitulo} maxWidth="xs" fullWidth>
      <DialogTitle id={idTitulo}>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText component="div">{descricao}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={aoCancelar} disabled={processando} color="inherit">
          Cancelar
        </Button>
        <Button variant="contained" color={destrutivo ? 'error' : 'primary'} onClick={aoConfirmar} loading={processando}>
          {rotuloConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
