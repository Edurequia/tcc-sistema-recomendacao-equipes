import ExploreOffOutlined from '@mui/icons-material/ExploreOffOutlined'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import { Link } from 'react-router'
import { EstadoVazio } from './EstadoVazio'

export function NaoEncontrada() {
  return (
    <>
      <Typography variant="h1" sx={{ mb: 3 }}>
        Página não encontrada
      </Typography>
      <EstadoVazio
        icone={<ExploreOffOutlined />}
        titulo="Nada por aqui"
        descricao="O endereço acessado não corresponde a nenhuma área do sistema."
        acao={
          <Button variant="contained" component={Link} to="/projetos">
            Voltar para os projetos
          </Button>
        }
      />
    </>
  )
}
