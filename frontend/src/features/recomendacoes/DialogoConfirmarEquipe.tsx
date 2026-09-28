import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useId } from 'react'
import { useNavigate } from 'react-router'
import { useConfirmarRecomendacao } from '../../api/recomendacoes'
import { ROTULO_PAPEL } from '../../api/rotulos'
import type { Recomendacao } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { useNotificar } from '../../components/notificar'

/** Confirma a equipe proposta, criando as alocacoes de uma so vez. Montado so enquanto aberto. */
export function DialogoConfirmarEquipe({ recomendacao, aoFechar }: { recomendacao: Recomendacao; aoFechar: () => void }) {
  const idTitulo = useId()
  const navigate = useNavigate()
  const notificar = useNotificar()
  const confirmar = useConfirmarRecomendacao(recomendacao.id)
  const selecionados = recomendacao.ranking.filter((m) => m.selecionado)

  async function confirmarEquipe() {
    try {
      // A resposta e a equipe inteira alocada no projeto (quem ja estava alocado e mantido).
      const equipe = await confirmar.mutateAsync()
      notificar(`Equipe confirmada: ${equipe.length} ${equipe.length === 1 ? 'integrante alocado' : 'integrantes alocados'} no projeto.`)
      navigate(`/projetos/${recomendacao.projetoId}`)
    } catch {
      // a mensagem aparece no alerta do proprio dialogo
    }
  }

  return (
    <Dialog open onClose={confirmar.isPending ? undefined : aoFechar} aria-labelledby={idTitulo} maxWidth="sm" fullWidth>
      <DialogTitle id={idTitulo}>Confirmar a equipe recomendada?</DialogTitle>
      <DialogContent>
        <Stack spacing={2}>
          <DialogContentText>
            Cada integrante será alocado em {recomendacao.projetoNome} no papel indicado. É tudo ou nada: se alguém não tiver mais as horas
            livres, nenhuma alocação é criada.
          </DialogContentText>
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0, border: 1, borderColor: 'divider', borderRadius: 2 }}>
            {selecionados.map((m, i) => (
              <Stack
                component="li"
                key={m.desenvolvedorId}
                direction="row"
                spacing={1.5}
                sx={{ alignItems: 'center', px: 2, py: 1, borderTop: i > 0 ? 1 : 0, borderColor: 'divider' }}
              >
                <AvatarIniciais nome={m.nome} tamanho={28} />
                <Typography variant="body2" sx={{ fontWeight: 600, flexGrow: 1 }}>
                  {m.nome}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  {ROTULO_PAPEL[m.papel]}
                </Typography>
              </Stack>
            ))}
          </Box>
          {confirmar.isError && (
            <Alert severity="error">
              {confirmar.error.message} Gere uma nova recomendação para refletir a disponibilidade atual.
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={aoFechar} disabled={confirmar.isPending} color="inherit">
          Cancelar
        </Button>
        <Button variant="contained" onClick={confirmarEquipe} loading={confirmar.isPending}>
          Confirmar e alocar
        </Button>
      </DialogActions>
    </Dialog>
  )
}
