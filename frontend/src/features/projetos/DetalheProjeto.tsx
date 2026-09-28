import ArrowBack from '@mui/icons-material/ArrowBack'
import EditOutlined from '@mui/icons-material/EditOutlined'
import EventOutlined from '@mui/icons-material/EventOutlined'
import GroupsOutlined from '@mui/icons-material/GroupsOutlined'
import LogoutOutlined from '@mui/icons-material/LogoutOutlined'
import PersonAddAlt1Outlined from '@mui/icons-material/PersonAddAlt1Outlined'
import ScheduleOutlined from '@mui/icons-material/ScheduleOutlined'
import ShieldOutlined from '@mui/icons-material/ShieldOutlined'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import Link from '@mui/material/Link'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { useState, type ReactNode } from 'react'
import { Link as RouterLink, useParams } from 'react-router'
import { useAlocacoesDoProjeto, useEncerrarAlocacao } from '../../api/alocacoes'
import { projetoAtivo, useAlterarStatusProjeto, useCancelarProjeto, useProjeto } from '../../api/projetos'
import { opcoes, ROTULO_CRITICIDADE, ROTULO_PAPEL, ROTULO_STATUS } from '../../api/rotulos'
import type { Alocacao, Projeto, StatusProjeto } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { Cabecalho } from '../../components/Cabecalho'
import { ChipCriticidade, ChipStatus } from '../../components/ChipsProjeto'
import { DialogoConfirmacao } from '../../components/DialogoConfirmacao'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { Painel } from '../../components/Painel'
import { useNotificar } from '../../components/notificar'
import { data, periodo } from '../../texto'
import { HistoricoRecomendacoes } from '../recomendacoes/HistoricoRecomendacoes'
import { DialogoAlocacao } from './DialogoAlocacao'

function Indicador({ icone, rotulo, valor }: { icone: ReactNode; rotulo: string; valor: ReactNode }) {
  return (
    <Paper sx={{ p: 2, display: 'flex', gap: 1.5, alignItems: 'center' }}>
      <Box
        aria-hidden
        sx={(t) => ({
          width: 40,
          height: 40,
          borderRadius: 2,
          display: 'grid',
          placeItems: 'center',
          flexShrink: 0,
          color: 'primary.main',
          bgcolor: `rgba(${t.vars!.palette.primary.mainChannel} / 0.1)`,
        })}
      >
        {icone}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="caption" color="textSecondary">
          {rotulo}
        </Typography>
        <Typography sx={{ fontWeight: 700 }}>{valor}</Typography>
      </Box>
    </Paper>
  )
}

const LISTA = { listStyle: 'none', m: 0, p: 0, '& > li + li': { borderTop: 1, borderColor: 'divider' } } as const

function Requisitos({ projeto }: { projeto: Projeto }) {
  return (
    <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '1fr 1.4fr' } }}>
      <Painel titulo="Papéis exigidos">
        <Box component="ul" sx={LISTA}>
          {projeto.papeisRequeridos.map((p) => (
            <Stack component="li" key={p.papel} direction="row" sx={{ justifyContent: 'space-between', py: 1.25 }}>
              <Typography variant="body2">{ROTULO_PAPEL[p.papel]}</Typography>
              <Typography variant="body2" sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                {p.quantidade}
              </Typography>
            </Stack>
          ))}
          {projeto.vagasSemPapelFixo > 0 && (
            <Stack component="li" direction="row" sx={{ justifyContent: 'space-between', py: 1.25 }}>
              <Typography variant="body2" color="textSecondary">
                Sem papel definido
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                {projeto.vagasSemPapelFixo}
              </Typography>
            </Stack>
          )}
        </Box>
      </Painel>
      <Painel titulo="Stack exigida">
        {projeto.habilidadesRequeridas.length === 0 ? (
          <Typography variant="body2" color="textSecondary">
            Nenhuma tecnologia exigida.
          </Typography>
        ) : (
          <Box component="ul" sx={LISTA}>
            {[...projeto.habilidadesRequeridas]
              .sort((a, b) => b.peso - a.peso || a.nome.localeCompare(b.nome, 'pt-BR'))
              .map((h) => (
                <Stack component="li" key={h.habilidadeId} direction="row" spacing={1.5} sx={{ alignItems: 'center', py: 1 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600, flexGrow: 1 }}>
                    {h.nome}
                  </Typography>
                  {h.obrigatoria && <Chip size="small" label="Obrigatória" color="primary" variant="outlined" />}
                  <Typography variant="caption" color="textSecondary" sx={{ width: 52, textAlign: 'right' }}>
                    Peso {h.peso}
                  </Typography>
                </Stack>
              ))}
          </Box>
        )}
      </Painel>
    </Box>
  )
}

function EquipeAlocada({ projeto }: { projeto: Projeto }) {
  const notificar = useNotificar()
  const alocacoes = useAlocacoesDoProjeto(projeto.id)
  const encerrar = useEncerrarAlocacao()
  const [alocando, setAlocando] = useState(false)
  const [paraEncerrar, setParaEncerrar] = useState<Alocacao | null>(null)
  const ativo = projetoAtivo(projeto.status)

  async function confirmarEncerramento() {
    if (!paraEncerrar) return
    try {
      await encerrar.mutateAsync(paraEncerrar.id)
      notificar(`Alocação de ${paraEncerrar.desenvolvedorNome} encerrada.`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    } finally {
      setParaEncerrar(null)
    }
  }

  const lista = alocacoes.data ?? []

  return (
    <Painel
      titulo="Equipe alocada"
      acao={
        ativo && (
          <Button size="small" variant="outlined" startIcon={<PersonAddAlt1Outlined />} onClick={() => setAlocando(true)}>
            Alocar desenvolvedor
          </Button>
        )
      }
    >
      {alocacoes.isError ? (
        <ErroCarregamento erro={alocacoes.error} aoTentarDeNovo={() => alocacoes.refetch()} />
      ) : alocacoes.isPending ? (
        <Skeleton variant="rounded" height={64} />
      ) : (
        <>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 1 }}>
            {lista.length} de {projeto.tamanhoEquipe} integrantes
          </Typography>
          {lista.length === 0 ? (
            <Typography variant="body2" color="textSecondary" sx={{ py: 3, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2 }}>
              Ninguém alocado ainda. Confirme uma recomendação ou aloque manualmente.
            </Typography>
          ) : (
            <Box component="ul" sx={LISTA}>
              {lista.map((a) => (
                <Stack component="li" key={a.id} direction="row" spacing={1.5} sx={{ alignItems: 'center', py: 1.25 }}>
                  <AvatarIniciais nome={a.desenvolvedorNome} />
                  <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    <Link
                      component={RouterLink}
                      to={`/desenvolvedores/${a.desenvolvedorId}`}
                      underline="hover"
                      color="inherit"
                      sx={{ fontWeight: 600, fontSize: '0.9rem' }}
                    >
                      {a.desenvolvedorNome}
                    </Link>
                    <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                      {ROTULO_PAPEL[a.papel]} · {a.horasSemanais} h/semana · desde {data(a.dataInicio)}
                    </Typography>
                  </Box>
                  {ativo && (
                    <Tooltip title="Encerrar alocação">
                      <IconButton aria-label={`Encerrar alocação de ${a.desenvolvedorNome}`} onClick={() => setParaEncerrar(a)}>
                        <LogoutOutlined fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                </Stack>
              ))}
            </Box>
          )}
        </>
      )}

      {alocando && <DialogoAlocacao projeto={projeto} aoFechar={() => setAlocando(false)} />}
      <DialogoConfirmacao
        aberto={paraEncerrar !== null}
        titulo={`Encerrar alocação de ${paraEncerrar?.desenvolvedorNome ?? ''}?`}
        descricao="As horas semanais voltam a ficar livres para outros projetos. O histórico da alocação é mantido."
        rotuloConfirmar="Encerrar"
        destrutivo
        processando={encerrar.isPending}
        aoConfirmar={confirmarEncerramento}
        aoCancelar={() => setParaEncerrar(null)}
      />
    </Painel>
  )
}

function Acoes({ projeto }: { projeto: Projeto }) {
  const notificar = useNotificar()
  const alterarStatus = useAlterarStatusProjeto()
  const cancelar = useCancelarProjeto()
  const [confirmandoCancelamento, setConfirmandoCancelamento] = useState(false)

  async function mudarStatus(status: StatusProjeto) {
    try {
      await alterarStatus.mutateAsync({ id: projeto.id, status })
      notificar(`Status alterado para ${ROTULO_STATUS[status]}.`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    }
  }

  async function confirmarCancelamento() {
    try {
      await cancelar.mutateAsync(projeto.id)
      notificar(`Projeto ${projeto.nome} cancelado.`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    } finally {
      setConfirmandoCancelamento(false)
    }
  }

  return (
    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', rowGap: 1, alignItems: 'center' }}>
      <TextField
        select
        size="small"
        label="Status do projeto"
        value={projeto.status}
        onChange={(e) => mudarStatus(e.target.value as StatusProjeto)}
        disabled={alterarStatus.isPending}
        sx={{ minWidth: 180 }}
      >
        {opcoes(ROTULO_STATUS).map(([valor, rotulo]) => (
          <MenuItem key={valor} value={valor}>
            {rotulo}
          </MenuItem>
        ))}
      </TextField>
      <Button variant="outlined" startIcon={<EditOutlined />} component={RouterLink} to={`/projetos/${projeto.id}/editar`}>
        Editar
      </Button>
      {projetoAtivo(projeto.status) && (
        <Button color="error" onClick={() => setConfirmandoCancelamento(true)}>
          Cancelar projeto
        </Button>
      )}
      <DialogoConfirmacao
        aberto={confirmandoCancelamento}
        titulo={`Cancelar ${projeto.nome}?`}
        descricao="O projeto para de disputar desenvolvedores e as horas das alocações deixam de ser consumidas. Ele e as recomendações geradas continuam registrados."
        rotuloConfirmar="Cancelar projeto"
        destrutivo
        processando={cancelar.isPending}
        aoConfirmar={confirmarCancelamento}
        aoCancelar={() => setConfirmandoCancelamento(false)}
      />
    </Stack>
  )
}

export function DetalheProjeto() {
  const { id } = useParams()
  const consulta = useProjeto(Number(id))
  const voltar = (
    <Button component={RouterLink} to="/projetos" startIcon={<ArrowBack />} color="inherit" size="small" sx={{ mb: 1, ml: -1 }}>
      Projetos
    </Button>
  )

  if (consulta.isError) {
    return (
      <>
        {voltar}
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      </>
    )
  }
  if (!consulta.data) {
    return (
      <>
        <Skeleton variant="text" width={320} height={48} />
        <Skeleton variant="rounded" height={96} sx={{ mt: 3 }} />
        <Skeleton variant="rounded" height={240} sx={{ mt: 3 }} />
      </>
    )
  }

  const projeto = consulta.data
  return (
    <>
      {voltar}
      <Cabecalho titulo={projeto.nome} acoes={<Acoes projeto={projeto} />} />
      <Stack direction="row" spacing={1} sx={{ mt: -1.5, mb: 2 }}>
        <ChipStatus status={projeto.status} />
        <ChipCriticidade criticidade={projeto.criticidade} />
      </Stack>
      {projeto.descricao && (
        <Typography color="textSecondary" sx={{ mb: 3, maxWidth: 820 }}>
          {projeto.descricao}
        </Typography>
      )}

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' }, mb: 3 }}>
        <Indicador icone={<GroupsOutlined />} rotulo="Equipe" valor={`${projeto.tamanhoEquipe} integrantes`} />
        <Indicador icone={<ScheduleOutlined />} rotulo="Dedicação" valor={`${projeto.horasSemanaisPorIntegrante} h/semana`} />
        <Indicador icone={<ShieldOutlined />} rotulo="Criticidade" valor={ROTULO_CRITICIDADE[projeto.criticidade]} />
        <Indicador icone={<EventOutlined />} rotulo="Período previsto" valor={periodo(projeto.dataInicioPrevista, projeto.dataFimPrevista)} />
      </Box>

      <Stack spacing={3}>
        <HistoricoRecomendacoes projeto={projeto} />
        <EquipeAlocada projeto={projeto} />
        <Requisitos projeto={projeto} />
      </Stack>
    </>
  )
}
