import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { useJustificar } from '../../api/recomendacoes'
import type { Recomendacao } from '../../api/tipos'
import { Painel } from '../../components/Painel'

/** Texto do modelo de linguagem explicando a equipe; gerado sob demanda e gravado na recomendacao. */
export function PainelJustificativa({ recomendacao }: { recomendacao: Recomendacao }) {
  const justificar = useJustificar(recomendacao.id)
  const [modelo, setModelo] = useState<string | null>(null)
  const texto = recomendacao.justificativaLlm

  async function gerar() {
    try {
      const resposta = await justificar.mutateAsync(Boolean(texto))
      setModelo(resposta.modelo)
    } catch {
      // a mensagem aparece no alerta abaixo, via justificar.error
    }
  }

  return (
    <Painel
      titulo="Justificativa"
      acao={
        texto && (
          <Button size="small" startIcon={<AutoAwesomeOutlined />} onClick={gerar} loading={justificar.isPending}>
            Gerar novamente
          </Button>
        )
      }
    >
      <Stack spacing={2}>
        {justificar.isError && <Alert severity="warning">{justificar.error.message}</Alert>}
        {texto ? (
          <>
            <Stack spacing={1.5} sx={{ maxWidth: 820 }}>
              {texto
                .split(/\n\s*\n/)
                .map((p) => p.trim())
                .filter(Boolean)
                .map((paragrafo, i) => (
                  <Typography key={i} sx={{ lineHeight: 1.7 }}>
                    {paragrafo}
                  </Typography>
                ))}
            </Stack>
            <Typography variant="caption" color="textSecondary">
              Texto gerado por modelo de linguagem{modelo ? ` (${modelo})` : ''} a partir do ranking, das métricas e das restrições desta
              recomendação. Confira os números nos painéis acima.
            </Typography>
          </>
        ) : (
          <Stack spacing={1.5} sx={{ alignItems: 'flex-start' }}>
            <Typography variant="body2" color="textSecondary">
              Peça ao modelo de linguagem uma explicação em texto corrido de por que esta equipe foi sugerida, quem ficou de fora e quais
              restrições pesaram. Pode levar alguns segundos.
            </Typography>
            <Button variant="outlined" startIcon={<AutoAwesomeOutlined />} onClick={gerar} loading={justificar.isPending}>
              Gerar justificativa
            </Button>
          </Stack>
        )}
      </Stack>
    </Painel>
  )
}
