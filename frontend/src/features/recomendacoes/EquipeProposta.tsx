import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { ROTULO_PAPEL, ROTULO_SENIORIDADE } from '../../api/rotulos'
import type { Recomendacao } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { Painel } from '../../components/Painel'
import { numero, percentual } from '../../texto'

function Dado({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <Box>
      <Typography variant="caption" color="textSecondary" sx={{ display: 'block', lineHeight: 1.3 }}>
        {rotulo}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
        {valor}
      </Typography>
    </Box>
  )
}

/** Cartoes dos integrantes selecionados, na ordem do ranking. */
export function EquipeProposta({ recomendacao }: { recomendacao: Recomendacao }) {
  const selecionados = recomendacao.ranking.filter((m) => m.selecionado)
  const faltantes = recomendacao.tamanhoEquipe - selecionados.length

  return (
    <Painel
      titulo="Equipe proposta"
      acao={
        <Typography variant="body2" color={faltantes > 0 ? 'warning' : 'textSecondary'}>
          {selecionados.length} de {recomendacao.tamanhoEquipe} integrantes
        </Typography>
      }
    >
      {selecionados.length === 0 ? (
        <Typography variant="body2" color="textSecondary">
          Nenhum candidato atendeu às restrições do projeto.
        </Typography>
      ) : (
        <Box
          component="ul"
          sx={{ listStyle: 'none', m: 0, p: 0, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(3, 1fr)' } }}
        >
          {selecionados.map((m) => (
            <Paper component="li" key={m.desenvolvedorId} sx={{ p: 2 }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mb: 1.5 }}>
                <AvatarIniciais nome={m.nome} tamanho={40} />
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 700 }} noWrap>
                    {m.nome}
                  </Typography>
                  <Stack direction="row" spacing={0.75} sx={{ mt: 0.25 }}>
                    <Chip size="small" label={ROTULO_PAPEL[m.papel]} />
                    <Chip size="small" variant="outlined" label={ROTULO_SENIORIDADE[m.senioridade]} />
                  </Stack>
                </Box>
                <Box sx={{ textAlign: 'right' }}>
                  <Typography sx={{ fontSize: '1.4rem', fontWeight: 800, color: 'primary.main', lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
                    {numero(m.scoreAfinidade)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    escore
                  </Typography>
                </Box>
              </Stack>
              <Stack direction="row" spacing={3} sx={{ pt: 1.5, borderTop: 1, borderColor: 'divider' }}>
                <Dado rotulo="Posição" valor={`${m.posicao}º`} />
                <Dado rotulo="Aderência técnica" valor={percentual(m.aderenciaTecnica, true)} />
                <Dado rotulo="Cobertura de horas" valor={percentual(m.coberturaHoras, true)} />
              </Stack>
            </Paper>
          ))}
        </Box>
      )}
    </Painel>
  )
}
