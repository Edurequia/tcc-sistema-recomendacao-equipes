import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useState, type FormEvent } from 'react'
import { useLinhaBase } from '../../api/recomendacoes'
import type { MetricasEquipe } from '../../api/tipos'
import { CABECALHO_TABELA } from '../../components/estilos'
import { Painel } from '../../components/Painel'
import { numero } from '../../texto'

// "maior" marca as metricas em que um valor mais alto e melhor; nos desvios nao ha
// direcao certa (dispersao pode ser desejada ou nao), entao a diferenca fica neutra.
const LINHAS: { chave: keyof MetricasEquipe; rotulo: string; casas: number; maior?: boolean }[] = [
  { chave: 'coberturaPapeis', rotulo: 'Cobertura de papéis', casas: 1, maior: true },
  { chave: 'coberturaTecnologias', rotulo: 'Cobertura de tecnologias', casas: 1, maior: true },
  { chave: 'scoreMedioEquipe', rotulo: 'Escore médio', casas: 1, maior: true },
  { chave: 'desvioPadraoSenioridade', rotulo: 'Desvio da senioridade', casas: 2 },
  { chave: 'desvioPadraoScore', rotulo: 'Desvio do escore', casas: 1 },
]

function comSinal(valor: number, casas: number): string {
  const texto = numero(valor, casas)
  return valor > 0 ? `+${texto}` : texto
}

/** Compara a equipe proposta com equipes sorteadas ao acaso entre os mesmos elegiveis. */
export function PainelLinhaBase({ recomendacaoId }: { recomendacaoId: number }) {
  const [parametros, setParametros] = useState({ sorteios: 100, semente: 42 })
  const [sorteios, setSorteios] = useState('100')
  const [semente, setSemente] = useState('42')
  const [erroCampo, setErroCampo] = useState<string | null>(null)
  const consulta = useLinhaBase(recomendacaoId, parametros.sorteios, parametros.semente)

  function recalcular(evento: FormEvent) {
    evento.preventDefault()
    const n = Number(sorteios)
    if (!Number.isInteger(n) || n < 1 || n > 10_000 || !Number.isInteger(Number(semente)) || semente.trim() === '') {
      setErroCampo('Use de 1 a 10.000 sorteios e uma semente inteira.')
      return
    }
    setErroCampo(null)
    setParametros({ sorteios: n, semente: Number(semente) })
  }

  const dados = consulta.data

  return (
    <Painel titulo="Comparação com a linha de base">
      <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
        Mede se a equipe proposta é melhor que uma escolha ao acaso: sorteia equipes do mesmo tamanho entre os candidatos elegíveis e
        compara as métricas. A semente fixa torna o resultado reproduzível.
      </Typography>

      <Stack component="form" onSubmit={recalcular} noValidate direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mb: 2, alignItems: { sm: 'flex-start' } }}>
        <TextField
          size="small"
          type="number"
          label="Sorteios"
          value={sorteios}
          onChange={(e) => setSorteios(e.target.value)}
          error={erroCampo !== null}
          helperText={erroCampo}
          slotProps={{ htmlInput: { min: 1, max: 10000 } }}
          sx={{ width: { sm: 140 } }}
        />
        <TextField size="small" type="number" label="Semente" value={semente} onChange={(e) => setSemente(e.target.value)} sx={{ width: { sm: 140 } }} />
        <Button type="submit" variant="outlined" loading={consulta.isFetching}>
          Recalcular
        </Button>
      </Stack>

      {consulta.isError ? (
        <Alert severity="info">{consulta.error.message}</Alert>
      ) : !dados ? (
        <Skeleton variant="rounded" height={200} />
      ) : (
        <>
          <TableContainer>
            <Table size="small" sx={{ '& td': { py: 1.1 } }}>
              <TableHead>
                <TableRow sx={CABECALHO_TABELA}>
                  <TableCell>Métrica</TableCell>
                  <TableCell align="right">Protótipo</TableCell>
                  <TableCell align="right">Aleatório (média ± dp)</TableCell>
                  <TableCell align="right">Diferença</TableCell>
                  <TableCell align="right" sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                    Em desvios
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {LINHAS.map(({ chave, rotulo, casas, maior }) => {
                  const prototipo = dados.prototipo[chave]
                  const media = dados.linhaBase.media[chave]
                  const dp = dados.linhaBase.desvioPadrao[chave]
                  const diferenca = prototipo - media
                  const cor = !maior || Math.abs(diferenca) < 0.05 ? 'text.primary' : diferenca > 0 ? 'success.main' : 'error.main'
                  return (
                    <TableRow key={chave} sx={{ '&:last-child td': { border: 0 }, '& td': { fontVariantNumeric: 'tabular-nums' } }}>
                      <TableCell>{rotulo}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700 }}>
                        {numero(prototipo, casas)}
                      </TableCell>
                      <TableCell align="right">
                        {numero(media, casas)} ± {numero(dp, casas)}
                      </TableCell>
                      <TableCell align="right" sx={{ color: cor, fontWeight: 600 }}>
                        {comSinal(diferenca, casas)}
                      </TableCell>
                      <TableCell align="right" sx={{ display: { xs: 'none', sm: 'table-cell' }, color: 'text.secondary' }}>
                        {dp > 0 ? `${comSinal(diferenca / dp, 1)} dp` : '—'}
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
          <Box sx={{ mt: 1.5 }}>
            <Typography variant="caption" color="textSecondary">
              {dados.sorteios} equipes de {dados.tamanhoEquipeSorteada} sorteadas entre {dados.candidatosElegiveis} candidatos elegíveis · semente{' '}
              {dados.semente}
              {dados.candidatosElegiveis <= dados.tamanhoEquipeSorteada &&
                ' · com elegíveis em número igual ou menor que as vagas, todo sorteio repete a mesma equipe.'}
            </Typography>
          </Box>
        </>
      )}
    </Painel>
  )
}
