import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

/** Bloco centralizado para listas vazias e areas ainda sem conteudo. */
export function EstadoVazio({ icone, titulo, descricao, acao }: { icone: ReactNode; titulo: string; descricao: string; acao?: ReactNode }) {
  return (
    <Paper sx={{ borderStyle: 'dashed', py: { xs: 6, md: 8 }, px: 3, textAlign: 'center' }}>
      <Box
        aria-hidden
        sx={(t) => ({
          width: 56,
          height: 56,
          mx: 'auto',
          mb: 2,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          color: 'primary.main',
          bgcolor: `rgba(${t.vars!.palette.primary.mainChannel} / 0.1)`,
        })}
      >
        {icone}
      </Box>
      <Typography variant="h2" component="p" sx={{ mb: 1 }}>
        {titulo}
      </Typography>
      <Typography color="textSecondary" sx={{ maxWidth: 440, mx: 'auto' }}>
        {descricao}
      </Typography>
      {acao && <Box sx={{ mt: 3 }}>{acao}</Box>}
    </Paper>
  )
}
