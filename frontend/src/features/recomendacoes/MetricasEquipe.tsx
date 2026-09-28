import Box from '@mui/material/Box'
import LinearProgress from '@mui/material/LinearProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { useId } from 'react'
import type { MetricasEquipe as Metricas } from '../../api/tipos'
import { numero, percentual } from '../../texto'

interface Cartao {
  rotulo: string
  valor: string
  explicacao: string
  /** Preenchimento da barra, de 0 a 100; ausente para metricas sem teto natural. */
  barra?: number
}

function cartoes(m: Metricas): Cartao[] {
  return [
    {
      rotulo: 'Cobertura de papéis',
      valor: percentual(m.coberturaPapeis),
      barra: m.coberturaPapeis,
      explicacao: 'Vagas por papel preenchidas com alguém daquele papel.',
    },
    {
      rotulo: 'Cobertura de tecnologias',
      valor: percentual(m.coberturaTecnologias),
      barra: m.coberturaTecnologias,
      explicacao: 'Peso das tecnologias exigidas que ao menos um integrante domina.',
    },
    {
      rotulo: 'Escore médio',
      valor: numero(m.scoreMedioEquipe),
      barra: m.scoreMedioEquipe,
      explicacao: 'Média do escore de afinidade dos integrantes (0 a 100).',
    },
    {
      rotulo: 'Desvio da senioridade',
      valor: numero(m.desvioPadraoSenioridade, 2),
      explicacao: 'Dispersão dos níveis (1 a 3). Zero indica todos no mesmo nível.',
    },
    {
      rotulo: 'Desvio do escore',
      valor: numero(m.desvioPadraoScore),
      explicacao: 'Dispersão dos escores. Valores baixos indicam equipe homogênea.',
    },
  ]
}

/** Cartoes com as cinco metricas de avaliacao da equipe proposta. */
export function MetricasEquipe({ metricas }: { metricas: Metricas }) {
  const idTitulo = useId()
  return (
    <Box component="section" aria-labelledby={idTitulo}>
      <Typography id={idTitulo} variant="h2" sx={{ mb: 1.5 }}>
        Métricas da equipe
      </Typography>
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(5, 1fr)' } }}>
        {cartoes(metricas).map((c) => (
          <Tooltip key={c.rotulo} title={c.explicacao} placement="top">
            <Paper sx={{ p: 2 }}>
              <Stack spacing={0.5}>
                <Typography variant="caption" color="textSecondary">
                  {c.rotulo}
                </Typography>
                <Typography sx={{ fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.02em', fontVariantNumeric: 'tabular-nums' }}>
                  {c.valor}
                </Typography>
              </Stack>
              {c.barra !== undefined && (
                <LinearProgress
                  variant="determinate"
                  value={Math.min(c.barra, 100)}
                  aria-hidden
                  color={c.barra >= 100 ? 'success' : 'primary'}
                  sx={{ mt: 1, height: 6, borderRadius: 3 }}
                />
              )}
            </Paper>
          </Tooltip>
        ))}
      </Box>
    </Box>
  )
}
