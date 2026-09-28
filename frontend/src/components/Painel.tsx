import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useId, type ReactNode } from 'react'

/** Painel com titulo que o leitor de tela anuncia como regiao nomeada. */
export function Painel({ titulo, acao, children }: { titulo: string; acao?: ReactNode; children: ReactNode }) {
  const id = useId()
  return (
    <Paper component="section" aria-labelledby={id} sx={{ p: { xs: 2, md: 2.5 } }}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 2, minHeight: 36 }}>
        <Typography id={id} variant="h2">
          {titulo}
        </Typography>
        {acao}
      </Stack>
      {children}
    </Paper>
  )
}
