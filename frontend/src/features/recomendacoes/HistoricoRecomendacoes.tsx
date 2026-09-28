import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined'
import ChevronRight from '@mui/icons-material/ChevronRight'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, useNavigate } from 'react-router'
import { projetoAtivo } from '../../api/projetos'
import { useGerarRecomendacao, useRecomendacoesDoProjeto } from '../../api/recomendacoes'
import type { Projeto } from '../../api/tipos'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { useNotificar } from '../../components/notificar'
import { Painel } from '../../components/Painel'
import { dataHora, numero, percentual } from '../../texto'

/** Botao de gerar e historico das recomendacoes do projeto, da mais recente para a mais antiga. */
export function HistoricoRecomendacoes({ projeto }: { projeto: Projeto }) {
  const navigate = useNavigate()
  const notificar = useNotificar()
  const historico = useRecomendacoesDoProjeto(projeto.id)
  const gerar = useGerarRecomendacao(projeto.id)

  async function gerarRecomendacao() {
    try {
      const gerada = await gerar.mutateAsync()
      navigate(`/recomendacoes/${gerada.id}`)
    } catch (erro) {
      notificar((erro as Error).message, 'error')
    }
  }

  return (
    <Painel
      titulo="Recomendações"
      acao={
        projetoAtivo(projeto.status) && (
          <Button variant="contained" size="small" startIcon={<AutoAwesomeOutlined />} onClick={gerarRecomendacao} loading={gerar.isPending}>
            Gerar recomendação
          </Button>
        )
      }
    >
      {gerar.isPending && (
        <Typography variant="body2" color="textSecondary" sx={{ mb: 1.5 }}>
          Pontuando os desenvolvedores ativos no motor de predição e compondo a equipe…
        </Typography>
      )}
      {historico.isError ? (
        <ErroCarregamento erro={historico.error} aoTentarDeNovo={() => historico.refetch()} />
      ) : historico.isPending ? (
        <Skeleton variant="rounded" height={64} />
      ) : historico.data.length === 0 ? (
        <Typography variant="body2" color="textSecondary" sx={{ py: 3, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2 }}>
          Nenhuma recomendação gerada para este projeto.
        </Typography>
      ) : (
        <List disablePadding sx={{ '& > a + a': { borderTop: 1, borderColor: 'divider' } }}>
          {historico.data.map((r) => (
            <ListItemButton key={r.id} component={RouterLink} to={`/recomendacoes/${r.id}`} sx={{ borderRadius: 0, px: 1, py: 1.25 }}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={{ xs: 0.75, md: 2 }} sx={{ flexGrow: 1, alignItems: { md: 'center' } }}>
                <Box sx={{ minWidth: 190 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {dataHora(r.criadoEm)}
                  </Typography>
                </Box>
                <Stack direction="row" spacing={0.75}>
                  <Chip size="small" label={r.origem === 'CHAT' ? 'Assistente' : 'Formulário'} variant="outlined" />
                  {r.observacaoRestricao && <Chip size="small" color="warning" variant="outlined" label="Com restrições" />}
                </Stack>
                <Typography variant="body2" color="textSecondary" sx={{ flexGrow: 1, textAlign: { md: 'right' }, fontVariantNumeric: 'tabular-nums' }}>
                  Papéis {percentual(r.coberturaPapeis)} · Tecnologias {percentual(r.coberturaTecnologias)} · Escore médio {numero(r.scoreMedioEquipe)}
                </Typography>
              </Stack>
              <ChevronRight fontSize="small" sx={{ color: 'text.secondary', ml: 1 }} aria-hidden />
            </ListItemButton>
          ))}
        </List>
      )}
    </Painel>
  )
}
