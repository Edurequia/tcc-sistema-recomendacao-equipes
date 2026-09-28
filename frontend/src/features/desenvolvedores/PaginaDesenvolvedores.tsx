import Add from '@mui/icons-material/Add'
import EditOutlined from '@mui/icons-material/EditOutlined'
import HideSourceOutlined from '@mui/icons-material/HideSourceOutlined'
import PeopleOutlined from '@mui/icons-material/PeopleOutlined'
import RestoreOutlined from '@mui/icons-material/RestoreOutlined'
import ScheduleOutlined from '@mui/icons-material/ScheduleOutlined'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import LinearProgress from '@mui/material/LinearProgress'
import Link from '@mui/material/Link'
import Paper from '@mui/material/Paper'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { useDesenvolvedores, useInativarDesenvolvedor, useReativarDesenvolvedor } from '../../api/desenvolvedores'
import { opcoes, ROTULO_PAPEL, ROTULO_SENIORIDADE } from '../../api/rotulos'
import type { DesenvolvedorResumo, PapelAtuacao, Senioridade } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { Cabecalho } from '../../components/Cabecalho'
import { DialogoConfirmacao } from '../../components/DialogoConfirmacao'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { EstadoVazio } from '../../components/EstadoVazio'
import { Paginacao } from '../../components/Paginacao'
import { CampoBusca, FiltroSelect } from '../../components/Filtros'
import { CABECALHO_TABELA, SO_EM_TELA_LARGA, visuallyHidden } from '../../components/estilos'
import { useNotificar } from '../../components/notificar'
import { decimal } from '../../texto'
import { useAtrasado } from '../../useAtrasado'
import { DialogoDisponibilidade } from './DialogoDisponibilidade'

type Situacao = 'ATIVOS' | 'INATIVOS' | 'TODOS'

const ATIVO_POR_SITUACAO: Record<Situacao, boolean | undefined> = { ATIVOS: true, INATIVOS: false, TODOS: undefined }

const COR_SENIORIDADE: Record<Senioridade, 'default' | 'info' | 'primary'> = { JUNIOR: 'default', PLENO: 'info', SENIOR: 'primary' }

function LinhasCarregando() {
  return Array.from({ length: 5 }, (_, i) => (
    <TableRow key={i}>
      <TableCell>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Skeleton variant="circular" width={36} height={36} />
          <Box sx={{ flexGrow: 1 }}>
            <Skeleton width="60%" />
            <Skeleton width="40%" />
          </Box>
        </Stack>
      </TableCell>
      {Array.from({ length: 6 }, (_, j) => (
        <TableCell key={j} sx={j > 0 && j < 5 ? SO_EM_TELA_LARGA : undefined}>
          <Skeleton />
        </TableCell>
      ))}
    </TableRow>
  ))
}

export function PaginaDesenvolvedores() {
  const notificar = useNotificar()
  const [busca, setBusca] = useState('')
  const [papel, setPapel] = useState<PapelAtuacao | ''>('')
  const [senioridade, setSenioridade] = useState<Senioridade | ''>('')
  const [situacao, setSituacao] = useState<Situacao>('ATIVOS')
  const [pagina, setPagina] = useState(0)
  const [tamanho, setTamanho] = useState(20)
  const [paraAjustar, setParaAjustar] = useState<DesenvolvedorResumo | null>(null)
  const [paraInativar, setParaInativar] = useState<DesenvolvedorResumo | null>(null)
  const nome = useAtrasado(busca.trim())

  const consulta = useDesenvolvedores({
    nome: nome || undefined,
    papel: papel || undefined,
    senioridade: senioridade || undefined,
    ativo: ATIVO_POR_SITUACAO[situacao],
    pagina,
    tamanho,
  })
  const inativar = useInativarDesenvolvedor()
  const reativar = useReativarDesenvolvedor()

  // Qualquer filtro novo volta para a primeira pagina.
  function filtrar<T>(definir: (valor: T) => void) {
    return (valor: T) => {
      definir(valor)
      setPagina(0)
    }
  }

  async function confirmarInativacao() {
    if (!paraInativar) return
    try {
      await inativar.mutateAsync(paraInativar.id)
      notificar(`${paraInativar.nome} inativado(a).`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    } finally {
      setParaInativar(null)
    }
  }

  async function reativarDesenvolvedor(desenvolvedor: DesenvolvedorResumo) {
    try {
      await reativar.mutateAsync(desenvolvedor.id)
      notificar(`${desenvolvedor.nome} reativado(a).`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    }
  }

  const dados = consulta.data

  return (
    <>
      <Cabecalho
        titulo="Desenvolvedores"
        subtitulo="Profissionais candidatos às equipes, com competências, senioridade e horas semanais disponíveis."
        acoes={
          <Button variant="contained" startIcon={<Add />} component={RouterLink} to="/desenvolvedores/novo">
            Novo desenvolvedor
          </Button>
        }
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <CampoBusca
          rotulo="Buscar desenvolvedor"
          valor={busca}
          aoMudar={(valor) => {
            setBusca(valor)
            setPagina(0)
          }}
        />
        <FiltroSelect rotulo="Papel" valor={papel} aoMudar={filtrar(setPapel)} itens={[['', 'Todos'], ...opcoes(ROTULO_PAPEL)]} />
        <FiltroSelect
          rotulo="Senioridade"
          valor={senioridade}
          aoMudar={filtrar(setSenioridade)}
          itens={[['', 'Todas'], ...opcoes(ROTULO_SENIORIDADE)]}
        />
        <FiltroSelect<Situacao>
          rotulo="Situação"
          valor={situacao}
          aoMudar={filtrar((valor) => setSituacao(valor || 'TODOS'))}
          itens={[
            ['ATIVOS', 'Ativos'],
            ['INATIVOS', 'Inativos'],
            ['TODOS', 'Todos'],
          ]}
        />
      </Stack>

      {consulta.isError ? (
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      ) : dados && dados.totalElementos === 0 ? (
        <EstadoVazio
          icone={<PeopleOutlined />}
          titulo="Nenhum desenvolvedor encontrado"
          descricao={
            nome || papel || senioridade || situacao !== 'ATIVOS'
              ? 'Nenhum profissional corresponde aos filtros escolhidos.'
              : 'Cadastre os profissionais que poderão compor as equipes.'
          }
        />
      ) : (
        <Paper>
          <TableContainer>
            <Table sx={{ '& td': { py: 1.25 } }}>
              <TableHead>
                <TableRow sx={CABECALHO_TABELA}>
                  <TableCell>Desenvolvedor</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Papel</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Senioridade</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Soft skills</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA} align="right">
                    Horas/semana
                  </TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Situação</TableCell>
                  <TableCell align="right">
                    <Box component="span" sx={visuallyHidden}>
                      Ações
                    </Box>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {!dados ? (
                  <LinhasCarregando />
                ) : (
                  dados.conteudo.map((dev) => (
                    <TableRow key={dev.id} hover sx={{ opacity: dev.ativo ? 1 : 0.6, '&:last-child td': { border: 0 } }}>
                      <TableCell>
                        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                          <AvatarIniciais nome={dev.nome} />
                          <Box sx={{ minWidth: 0 }}>
                            <Link
                              component={RouterLink}
                              to={`/desenvolvedores/${dev.id}`}
                              underline="hover"
                              color="inherit"
                              sx={{ fontWeight: 600, fontSize: '0.9rem', display: 'block' }}
                            >
                              {dev.nome}
                            </Link>
                            <Typography variant="caption" color="textSecondary" noWrap sx={{ display: 'block' }}>
                              {dev.email}
                            </Typography>
                          </Box>
                        </Stack>
                      </TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>{ROTULO_PAPEL[dev.papelPrincipal]}</TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>
                        <Chip size="small" variant="outlined" color={COR_SENIORIDADE[dev.senioridade]} label={ROTULO_SENIORIDADE[dev.senioridade]} />
                      </TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums', minWidth: 28 }}>
                            {decimal(dev.softSkillsScore)}
                          </Typography>
                          <LinearProgress
                            variant="determinate"
                            value={(dev.softSkillsScore / 5) * 100}
                            aria-hidden
                            sx={{ width: 56, height: 6, borderRadius: 3 }}
                          />
                        </Stack>
                      </TableCell>
                      <TableCell sx={{ ...SO_EM_TELA_LARGA, fontVariantNumeric: 'tabular-nums' }} align="right">
                        {dev.horasDisponiveis} h
                      </TableCell>
                      <TableCell sx={SO_EM_TELA_LARGA}>
                        <Chip
                          size="small"
                          label={dev.ativo ? 'Ativo' : 'Inativo'}
                          color={dev.ativo ? 'success' : 'default'}
                          variant={dev.ativo ? 'outlined' : 'filled'}
                        />
                      </TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Tooltip title="Editar perfil">
                          <IconButton aria-label={`Editar ${dev.nome}`} component={RouterLink} to={`/desenvolvedores/${dev.id}`}>
                            <EditOutlined fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        {dev.ativo ? (
                          <>
                            <Tooltip title="Ajustar horas">
                              <IconButton aria-label={`Ajustar horas de ${dev.nome}`} onClick={() => setParaAjustar(dev)}>
                                <ScheduleOutlined fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Inativar">
                              <IconButton aria-label={`Inativar ${dev.nome}`} onClick={() => setParaInativar(dev)}>
                                <HideSourceOutlined fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </>
                        ) : (
                          <Tooltip title="Reativar">
                            <IconButton aria-label={`Reativar ${dev.nome}`} onClick={() => reativarDesenvolvedor(dev)} disabled={reativar.isPending}>
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
          <Paginacao
            total={dados?.totalElementos ?? 0}
            pagina={dados ? pagina : 0}
            tamanho={tamanho}
            aoMudarPagina={setPagina}
            aoMudarTamanho={(novo) => {
              setTamanho(novo)
              setPagina(0)
            }}
          />
        </Paper>
      )}

      {paraAjustar && <DialogoDisponibilidade desenvolvedor={paraAjustar} aoFechar={() => setParaAjustar(null)} />}

      <DialogoConfirmacao
        aberto={paraInativar !== null}
        titulo={`Inativar ${paraInativar?.nome ?? ''}?`}
        descricao="O profissional deixa de ser candidato em novas recomendações, mas continua visível nas recomendações e alocações que já o incluem. Ele pode ser reativado depois."
        rotuloConfirmar="Inativar"
        destrutivo
        processando={inativar.isPending}
        aoConfirmar={confirmarInativacao}
        aoCancelar={() => setParaInativar(null)}
      />
    </>
  )
}
