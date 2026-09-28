import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import LinearProgress from '@mui/material/LinearProgress'
import Link from '@mui/material/Link'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { ROTULO_PAPEL, ROTULO_SENIORIDADE } from '../../api/rotulos'
import type { MembroRanking } from '../../api/tipos'
import { CABECALHO_TABELA, SO_EM_TELA_LARGA } from '../../components/estilos'
import { Painel } from '../../components/Painel'
import { numero, percentual } from '../../texto'

type Filtro = 'TODOS' | 'SELECIONADOS' | 'INELEGIVEIS'

const FILTROS: Record<Filtro, (m: MembroRanking) => boolean> = {
  TODOS: () => true,
  SELECIONADOS: (m) => m.selecionado,
  INELEGIVEIS: (m) => !m.elegivel,
}

function Situacao({ membro }: { membro: MembroRanking }) {
  if (membro.selecionado) return <Chip size="small" color="primary" label="Selecionado" />
  if (membro.elegivel) return <Chip size="small" variant="outlined" label="Elegível" />
  return <Chip size="small" color="warning" variant="outlined" label="Sem horas" />
}

/** Todos os candidatos pontuados, na ordem do escore, com o motivo de estar dentro ou fora. */
export function TabelaRanking({ ranking }: { ranking: MembroRanking[] }) {
  const [filtro, setFiltro] = useState<Filtro>('TODOS')
  const visiveis = ranking.filter(FILTROS[filtro])

  return (
    <Painel
      titulo="Ranking completo"
      acao={
        <ToggleButtonGroup size="small" exclusive value={filtro} onChange={(_, valor: Filtro | null) => valor && setFiltro(valor)} aria-label="Filtrar ranking">
          <ToggleButton value="TODOS">Todos</ToggleButton>
          <ToggleButton value="SELECIONADOS">Selecionados</ToggleButton>
          <ToggleButton value="INELEGIVEIS">Inelegíveis</ToggleButton>
        </ToggleButtonGroup>
      }
    >
      <Typography variant="body2" color="textSecondary" sx={{ mb: 1.5 }}>
        Candidatos sem as horas livres exigidas ficam inelegíveis, qualquer que seja o escore. Entre os elegíveis, as vagas por papel vêm
        antes das posições livres.
      </Typography>
      <TableContainer sx={{ mx: { xs: -2, md: -2.5 }, width: 'auto' }}>
        <Table size="small" sx={{ '& td, & th': { px: { xs: 2, md: 2.5 } }, '& td': { py: 1 } }}>
          <TableHead>
            <TableRow sx={CABECALHO_TABELA}>
              <TableCell align="right" sx={{ width: 48 }}>
                #
              </TableCell>
              <TableCell>Desenvolvedor</TableCell>
              <TableCell sx={SO_EM_TELA_LARGA}>Papel</TableCell>
              <TableCell sx={SO_EM_TELA_LARGA}>Senioridade</TableCell>
              <TableCell>Escore</TableCell>
              <TableCell sx={SO_EM_TELA_LARGA} align="right">
                Aderência
              </TableCell>
              <TableCell sx={SO_EM_TELA_LARGA} align="right">
                Horas
              </TableCell>
              <TableCell>Situação</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visiveis.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} sx={{ textAlign: 'center', color: 'text.secondary', py: 3 }}>
                  Nenhum candidato neste filtro.
                </TableCell>
              </TableRow>
            ) : (
              visiveis.map((m) => (
                <TableRow key={m.desenvolvedorId} hover sx={{ opacity: m.elegivel ? 1 : 0.7, '&:last-child td': { border: 0 } }}>
                  <TableCell align="right" sx={{ color: 'text.secondary', fontVariantNumeric: 'tabular-nums' }}>
                    {m.posicao}
                  </TableCell>
                  <TableCell>
                    <Link
                      component={RouterLink}
                      to={`/desenvolvedores/${m.desenvolvedorId}`}
                      underline="hover"
                      color="inherit"
                      sx={{ fontWeight: m.selecionado ? 700 : 500 }}
                    >
                      {m.nome}
                    </Link>
                  </TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>{ROTULO_PAPEL[m.papel]}</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>{ROTULO_SENIORIDADE[m.senioridade]}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Box component="span" sx={{ fontVariantNumeric: 'tabular-nums', minWidth: 36 }}>
                        {numero(m.scoreAfinidade)}
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(m.scoreAfinidade, 100)}
                        aria-hidden
                        sx={{ width: 64, height: 6, borderRadius: 3, display: { xs: 'none', sm: 'block' } }}
                      />
                    </Stack>
                  </TableCell>
                  <TableCell sx={{ ...SO_EM_TELA_LARGA, fontVariantNumeric: 'tabular-nums' }} align="right">
                    {percentual(m.aderenciaTecnica, true)}
                  </TableCell>
                  <TableCell sx={{ ...SO_EM_TELA_LARGA, fontVariantNumeric: 'tabular-nums' }} align="right">
                    {percentual(m.coberturaHoras, true)}
                  </TableCell>
                  <TableCell>
                    <Situacao membro={m} />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Painel>
  )
}
