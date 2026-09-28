import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'

/** Falha ao buscar dados, com a opcao de repetir a consulta. */
export function ErroCarregamento({ erro, aoTentarDeNovo }: { erro: Error; aoTentarDeNovo: () => void }) {
  return (
    <Alert
      severity="error"
      action={
        <Button color="inherit" size="small" onClick={aoTentarDeNovo}>
          Tentar novamente
        </Button>
      }
    >
      {erro.message}
    </Alert>
  )
}
