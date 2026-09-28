import ArrowBack from '@mui/icons-material/ArrowBack'
import TaskAltOutlined from '@mui/icons-material/TaskAltOutlined'
import Alert from '@mui/material/Alert'
import AlertTitle from '@mui/material/AlertTitle'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import { useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router'
import { useRecomendacao } from '../../api/recomendacoes'
import { Cabecalho } from '../../components/Cabecalho'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { dataHora } from '../../texto'
import { DialogoConfirmarEquipe } from './DialogoConfirmarEquipe'
import { EquipeProposta } from './EquipeProposta'
import { MetricasEquipe } from './MetricasEquipe'
import { PainelJustificativa } from './PainelJustificativa'
import { PainelLinhaBase } from './PainelLinhaBase'
import { TabelaRanking } from './TabelaRanking'

const ORIGEM = { FORMULARIO: 'pelo formulário', CHAT: 'pelo assistente' } as const

export function PaginaRecomendacao() {
  const id = Number(useParams().id)
  const consulta = useRecomendacao(id)
  const [confirmando, setConfirmando] = useState(false)

  if (consulta.isError) {
    return <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
  }
  if (!consulta.data) {
    return (
      <>
        <Skeleton variant="text" width={320} height={48} />
        <Skeleton variant="rounded" height={110} sx={{ mt: 3 }} />
        <Skeleton variant="rounded" height={260} sx={{ mt: 3 }} />
      </>
    )
  }

  const recomendacao = consulta.data
  return (
    <>
      <Button component={RouterLink} to={`/projetos/${recomendacao.projetoId}`} startIcon={<ArrowBack />} color="inherit" size="small" sx={{ mb: 1, ml: -1 }}>
        {recomendacao.projetoNome}
      </Button>
      <Cabecalho
        titulo="Equipe recomendada"
        subtitulo={`Gerada em ${dataHora(recomendacao.criadoEm)} ${ORIGEM[recomendacao.origem]} · ${recomendacao.totalCandidatos} candidatos avaliados`}
        acoes={
          recomendacao.integrantesSelecionados > 0 && (
            <Button variant="contained" startIcon={<TaskAltOutlined />} onClick={() => setConfirmando(true)}>
              Confirmar equipe
            </Button>
          )
        }
      />

      <Stack spacing={3}>
        {recomendacao.observacaoRestricao && (
          <Alert severity="warning" role="alert">
            <AlertTitle>Restrições encontradas</AlertTitle>
            {recomendacao.observacaoRestricao}
          </Alert>
        )}
        <MetricasEquipe metricas={recomendacao.metricas} />
        <EquipeProposta recomendacao={recomendacao} />
        <PainelJustificativa recomendacao={recomendacao} />
        <PainelLinhaBase recomendacaoId={recomendacao.id} />
        <TabelaRanking ranking={recomendacao.ranking} />
      </Stack>

      {confirmando && <DialogoConfirmarEquipe recomendacao={recomendacao} aoFechar={() => setConfirmando(false)} />}
    </>
  )
}
