import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

/** Titulo da pagina (o unico h1) com acoes opcionais alinhadas a direita. */
export function Cabecalho({ titulo, subtitulo, acoes }: { titulo: string; subtitulo?: string; acoes?: ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3, alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}>
      <div>
        <Typography variant="h1">{titulo}</Typography>
        {subtitulo && (
          <Typography variant="body2" color="textSecondary" sx={{ mt: 0.5 }}>
            {subtitulo}
          </Typography>
        )}
      </div>
      {acoes && <Stack direction="row" spacing={1}>{acoes}</Stack>}
    </Stack>
  )
}
