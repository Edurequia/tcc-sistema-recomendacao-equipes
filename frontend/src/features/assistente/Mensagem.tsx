import AutoAwesome from '@mui/icons-material/AutoAwesome'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router'
import { ROTULO_PAPEL } from '../../api/rotulos'
import type { AutorMensagem, Recomendacao } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { numero, percentual } from '../../texto'

function AvatarAssistente() {
  return (
    <Box
      aria-hidden
      sx={{
        width: 32,
        height: 32,
        borderRadius: '50%',
        flexShrink: 0,
        display: 'grid',
        placeItems: 'center',
        color: '#fff',
        background: 'linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)',
      }}
    >
      <AutoAwesome sx={{ fontSize: 16 }} />
    </Box>
  )
}

/** Balao de uma fala: o gestor a direita, o assistente a esquerda. */
export function Mensagem({ autor, children }: { autor: AutorMensagem; children: ReactNode }) {
  const gestor = autor === 'GESTOR'
  return (
    <Stack component="li" direction="row" spacing={1.25} sx={{ justifyContent: gestor ? 'flex-end' : 'flex-start', alignItems: 'flex-start' }}>
      {!gestor && <AvatarAssistente />}
      <Box
        sx={{
          maxWidth: { xs: '88%', md: '78%' },
          px: 2,
          py: 1.25,
          borderRadius: 3,
          borderTopRightRadius: gestor ? 4 : undefined,
          borderTopLeftRadius: gestor ? undefined : 4,
          bgcolor: gestor ? 'primary.main' : 'action.hover',
          color: gestor ? 'primary.contrastText' : 'text.primary',
          whiteSpace: 'pre-wrap',
          overflowWrap: 'anywhere',
        }}
      >
        <Box component="span" sx={visuallyHiddenAutor}>
          {gestor ? 'Você: ' : 'Assistente: '}
        </Box>
        {children}
      </Box>
    </Stack>
  )
}

const visuallyHiddenAutor = { position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)', whiteSpace: 'nowrap' } as const

export function Digitando() {
  return (
    <Stack component="li" direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
      <AvatarAssistente />
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', px: 2, py: 1.25, borderRadius: 3, bgcolor: 'action.hover' }}>
        <CircularProgress size={14} />
        <Typography variant="body2" color="textSecondary">
          Analisando o pedido…
        </Typography>
      </Stack>
    </Stack>
  )
}

/** Resumo da recomendacao criada pela conversa, com o atalho para a tela completa. */
export function CartaoRecomendacao({ recomendacao }: { recomendacao: Recomendacao }) {
  const selecionados = recomendacao.ranking.filter((m) => m.selecionado)
  return (
    <Paper sx={{ mt: 1.5, p: 1.5, bgcolor: 'background.paper', color: 'text.primary', whiteSpace: 'normal' }}>
      <Stack spacing={1}>
        {selecionados.map((m) => (
          <Stack key={m.desenvolvedorId} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <AvatarIniciais nome={m.nome} tamanho={24} />
            <Typography variant="body2" sx={{ fontWeight: 600, flexGrow: 1 }}>
              {m.nome}
            </Typography>
            <Typography variant="caption" color="textSecondary">
              {ROTULO_PAPEL[m.papel]} · {numero(m.scoreAfinidade)}
            </Typography>
          </Stack>
        ))}
      </Stack>
      <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mt: 1.25 }}>
        Papéis {percentual(recomendacao.metricas.coberturaPapeis)} · Tecnologias {percentual(recomendacao.metricas.coberturaTecnologias)} ·
        Escore médio {numero(recomendacao.metricas.scoreMedioEquipe)}
      </Typography>
      <Button component={RouterLink} to={`/recomendacoes/${recomendacao.id}`} variant="contained" size="small" sx={{ mt: 1.5 }}>
        Ver recomendação completa
      </Button>
    </Paper>
  )
}
