import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import InputAdornment from '@mui/material/InputAdornment'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useId, useState, type FormEvent } from 'react'
import { useAtualizarDisponibilidade } from '../../api/desenvolvedores'
import type { DesenvolvedorResumo } from '../../api/tipos'
import { useNotificar } from '../../components/notificar'

/** Ajuste pontual das horas semanais, sem tocar no restante do perfil. Montado so enquanto aberto. */
export function DialogoDisponibilidade({ desenvolvedor, aoFechar }: { desenvolvedor: DesenvolvedorResumo; aoFechar: () => void }) {
  const idTitulo = useId()
  const atualizar = useAtualizarDisponibilidade()
  const notificar = useNotificar()
  const [horas, setHoras] = useState(String(desenvolvedor.horasDisponiveis))
  const [erroCampo, setErroCampo] = useState<string | null>(null)
  const [erroGeral, setErroGeral] = useState<string | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const valor = Number(horas)
    if (horas.trim() === '' || !Number.isInteger(valor) || valor < 0 || valor > 60) {
      setErroCampo('Informe um número inteiro de 0 a 60.')
      return
    }
    setErroCampo(null)
    setErroGeral(null)
    try {
      await atualizar.mutateAsync({ id: desenvolvedor.id, horasDisponiveis: valor })
      notificar(`Disponibilidade de ${desenvolvedor.nome} atualizada para ${valor} h.`)
      aoFechar()
    } catch (erro) {
      setErroGeral((erro as Error).message)
    }
  }

  return (
    <Dialog open onClose={atualizar.isPending ? undefined : aoFechar} aria-labelledby={idTitulo} maxWidth="xs" fullWidth>
      <form onSubmit={enviar} noValidate>
        <DialogTitle id={idTitulo}>Disponibilidade de {desenvolvedor.nome}</DialogTitle>
        <DialogContent>
          <Stack spacing={2}>
            <DialogContentText>
              Horas semanais que o profissional pode dedicar a projetos. Não pode ficar abaixo das horas já comprometidas em alocações ativas.
            </DialogContentText>
            {erroGeral && <Alert severity="error">{erroGeral}</Alert>}
            <TextField
              label="Horas por semana"
              type="number"
              autoFocus
              value={horas}
              onChange={(evento) => setHoras(evento.target.value)}
              error={erroCampo !== null}
              helperText={erroCampo}
              slotProps={{
                htmlInput: { min: 0, max: 60, step: 1 },
                input: { endAdornment: <InputAdornment position="end">h</InputAdornment> },
              }}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={aoFechar} disabled={atualizar.isPending} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" loading={atualizar.isPending}>
            Salvar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
