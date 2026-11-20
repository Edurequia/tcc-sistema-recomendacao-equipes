import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { useJustificar } from '../../api/recomendacoes'
import type { Recomendacao } from '../../api/tipos'
import { Painel } from '../../components/Painel'

/**
 * Texto do modelo de linguagem explicando a equipe. Chega pronto com a
 * recomendacao; o botao so aparece para repetir quando o modelo falhou ou para
 * pedir uma nova versao.
 */
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
            {!justificar.isError && (
              <Alert severity="info" role="status">
                {recomendacao.avisoJustificativa ?? 'A justificativa não foi gerada junto com esta recomendação.'}
              </Alert>
            )}
            <Button variant="outlined" startIcon={<AutoAwesomeOutlined />} onClick={gerar} loading={justificar.isPending}>
              Tentar novamente
            </Button>
          </Stack>
        )}
      </Stack>
    </Painel>
  )
}
