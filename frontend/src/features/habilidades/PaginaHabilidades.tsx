import Add from '@mui/icons-material/Add'
import EditOutlined from '@mui/icons-material/EditOutlined'
import HideSourceOutlined from '@mui/icons-material/HideSourceOutlined'
import InsightsOutlined from '@mui/icons-material/InsightsOutlined'
import RestoreOutlined from '@mui/icons-material/RestoreOutlined'
import SchoolOutlined from '@mui/icons-material/SchoolOutlined'
import Search from '@mui/icons-material/Search'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import FormControlLabel from '@mui/material/FormControlLabel'
import IconButton from '@mui/material/IconButton'
import InputAdornment from '@mui/material/InputAdornment'
import Paper from '@mui/material/Paper'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { useMemo, useState } from 'react'
import { useHabilidades, useInativarHabilidade, useReativarHabilidade } from '../../api/habilidades'
import { ROTULO_CATEGORIA, ROTULO_TIPO_HABILIDADE } from '../../api/rotulos'
import type { Habilidade, TipoHabilidade } from '../../api/tipos'
import { Cabecalho } from '../../components/Cabecalho'
import { DialogoConfirmacao } from '../../components/DialogoConfirmacao'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { EstadoVazio } from '../../components/EstadoVazio'
import { CABECALHO_TABELA, SO_EM_TELA_LARGA, visuallyHidden } from '../../components/estilos'
import { useNotificar } from '../../components/notificar'
import { contar, normalizar } from '../../texto'
import { DialogoHabilidade } from './DialogoHabilidade'

type FiltroTipo = TipoHabilidade | 'TODAS'


function LinhasCarregando() {
  return Array.from({ length: 5 }, (_, i) => (
    <TableRow key={i}>
      {Array.from({ length: 6 }, (_, j) => (
        <TableCell key={j} sx={j > 1 && j < 5 ? SO_EM_TELA_LARGA : undefined}>
          <Skeleton />
        </TableCell>
      ))}
    </TableRow>
  ))
}

export function PaginaHabilidades() {
  const notificar = useNotificar()
  const [tipo, setTipo] = useState<FiltroTipo>('TODAS')
  const [mostrarInativas, setMostrarInativas] = useState(false)
  const [busca, setBusca] = useState('')
  const [dialogo, setDialogo] = useState<{ habilidade?: Habilidade } | null>(null)
  const [paraInativar, setParaInativar] = useState<Habilidade | null>(null)

  const consulta = useHabilidades({ tipo: tipo === 'TODAS' ? undefined : tipo, apenasAtivas: !mostrarInativas })
  const inativar = useInativarHabilidade()
  const reativar = useReativarHabilidade()

  const visiveis = useMemo(() => {
    const termo = normalizar(busca)
    return (consulta.data ?? []).filter((h) => !termo || normalizar(h.nome).includes(termo))
  }, [consulta.data, busca])

  async function confirmarInativacao() {
    if (!paraInativar) return
    try {
      await inativar.mutateAsync(paraInativar.id)
      notificar(`${paraInativar.nome} inativada.`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    } finally {
      setParaInativar(null)
    }
  }

  async function reativarHabilidade(habilidade: Habilidade) {
    try {
      await reativar.mutateAsync(habilidade.id)
      notificar(`${habilidade.nome} reativada.`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    }
  }

  return (
    <>
      <Cabecalho
        titulo="Habilidades"
        subtitulo="Catálogo de tecnologias e competências comportamentais usado nos perfis e nos requisitos dos projetos."
        acoes={
          <Button variant="contained" startIcon={<Add />} onClick={() => setDialogo({})}>
            Nova habilidade
          </Button>
        }
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2, alignItems: { md: 'center' } }}>
        <TextField
          type="search"
          size="small"
          placeholder="Buscar pelo nome"
          value={busca}
          onChange={(evento) => setBusca(evento.target.value)}
          sx={{ minWidth: { md: 280 } }}
          slotProps={{
            htmlInput: { 'aria-label': 'Buscar habilidade' },
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <Search fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
        <ToggleButtonGroup
          size="small"
          exclusive
          aria-label="Filtrar por tipo"
          value={tipo}
          onChange={(_, valor: FiltroTipo | null) => valor && setTipo(valor)}
        >
          <ToggleButton value="TODAS">Todas</ToggleButton>
          <ToggleButton value="HARD">Técnicas</ToggleButton>
          <ToggleButton value="SOFT">Comportamentais</ToggleButton>
        </ToggleButtonGroup>
        <FormControlLabel
          control={<Switch checked={mostrarInativas} onChange={(evento) => setMostrarInativas(evento.target.checked)} />}
          label="Mostrar inativas"
          sx={{ ml: { md: 'auto' }, '& .MuiFormControlLabel-label': { fontSize: '0.875rem' } }}
        />
      </Stack>

      {consulta.isError ? (
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      ) : consulta.isSuccess && visiveis.length === 0 ? (
        <EstadoVazio
          icone={<SchoolOutlined />}
          titulo="Nenhuma habilidade encontrada"
          descricao={busca ? 'Nenhuma habilidade corresponde à busca e aos filtros escolhidos.' : 'Cadastre a primeira habilidade do catálogo.'}
        />
      ) : (
        <Paper>
          <TableContainer>
            <Table size="medium" sx={{ '& td': { py: 1.25 } }}>
              <TableHead>
                <TableRow sx={CABECALHO_TABELA}>
                  <TableCell>Nome</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Categoria</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Modelo</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Situação</TableCell>
                  <TableCell align="right">
                    <Box component="span" sx={visuallyHidden}>
                      Ações
                    </Box>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {consulta.isPending ? (
                  <LinhasCarregando />
                ) : (
                  visiveis.map((habilidade) => (
                    <TableRow key={habilidade.id} hover sx={{ opacity: habilidade.ativa ? 1 : 0.6, '&:last-child td': { border: 0 } }}>
                      <TableCell>
                        <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }}>{habilidade.nome}</Typography>
                        {habilidade.chaveDataset && habilidade.chaveDataset !== habilidade.nome && (
                          <Typography variant="caption" color="textSecondary">
                            {habilidade.chaveDataset}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          variant="outlined"
                          color={habilidade.tipo === 'HARD' ? 'primary' : 'secondary'}
                          label={ROTULO_TIPO_HABILIDADE[habilidade.tipo]}
                        />
                      </TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>{ROTULO_CATEGORIA[habilidade.categoria]}</TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>
                        {habilidade.usadaNoModelo && (
                          <Tooltip title="Tem coluna correspondente nos dados de treinamento do modelo.">
                            <Chip size="small" icon={<InsightsOutlined />} label="No modelo" sx={{ fontWeight: 500 }} />
                          </Tooltip>
                        )}
                      </TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>
                        <Chip
                          size="small"
                          label={habilidade.ativa ? 'Ativa' : 'Inativa'}
                          color={habilidade.ativa ? 'success' : 'default'}
                          variant={habilidade.ativa ? 'outlined' : 'filled'}
                        />
                      </TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Tooltip title="Editar">
                          <IconButton aria-label={`Editar ${habilidade.nome}`} onClick={() => setDialogo({ habilidade })}>
                            <EditOutlined fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        {habilidade.ativa ? (
                          <Tooltip title="Inativar">
                            <IconButton aria-label={`Inativar ${habilidade.nome}`} onClick={() => setParaInativar(habilidade)}>
                              <HideSourceOutlined fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        ) : (
                          <Tooltip title="Reativar">
                            <IconButton
                              aria-label={`Reativar ${habilidade.nome}`}
                              onClick={() => reativarHabilidade(habilidade)}
                              disabled={reativar.isPending}
                            >
                              <RestoreOutlined fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          {consulta.isSuccess && (
            <Typography variant="body2" color="textSecondary" sx={{ px: 2, py: 1.5, borderTop: 1, borderColor: 'divider' }}>
              {contar(visiveis.length, 'habilidade', 'habilidades')}
            </Typography>
          )}
        </Paper>
      )}

      {dialogo && <DialogoHabilidade habilidade={dialogo.habilidade} aoFechar={() => setDialogo(null)} />}

      <DialogoConfirmacao
        aberto={paraInativar !== null}
        titulo={`Inativar ${paraInativar?.nome ?? ''}?`}
        descricao="A habilidade deixa de aparecer nas listagens e nos novos cadastros. Perfis, projetos e recomendações que já a usam continuam intactos, e ela pode ser reativada depois."
        rotuloConfirmar="Inativar"
        destrutivo
        processando={inativar.isPending}
        aoConfirmar={confirmarInativacao}
        aoCancelar={() => setParaInativar(null)}
      />
    </>
  )
}

