import Add from '@mui/icons-material/Add'
import ChevronRight from '@mui/icons-material/ChevronRight'
import FolderOutlined from '@mui/icons-material/FolderOutlined'
import Button from '@mui/material/Button'
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
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router'
import { useProjetos } from '../../api/projetos'
import { opcoes, ROTULO_CRITICIDADE, ROTULO_STATUS } from '../../api/rotulos'
import type { CriticidadeProjeto, StatusProjeto } from '../../api/tipos'
import { Cabecalho } from '../../components/Cabecalho'
import { ChipCriticidade, ChipStatus } from '../../components/ChipsProjeto'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { EstadoVazio } from '../../components/EstadoVazio'
import { Paginacao } from '../../components/Paginacao'
import { CABECALHO_TABELA, SO_EM_TELA_LARGA } from '../../components/estilos'
import { CampoBusca, FiltroSelect } from '../../components/Filtros'
import { periodo } from '../../texto'
import { useAtrasado } from '../../useAtrasado'

const BOTAO_NOVO = (
  <Button variant="contained" startIcon={<Add />} component={RouterLink} to="/projetos/novo">
    Novo projeto
  </Button>
)

export function PaginaProjetos() {
  const navigate = useNavigate()
  const [busca, setBusca] = useState('')
  const [status, setStatus] = useState<StatusProjeto | ''>('')
  const [criticidade, setCriticidade] = useState<CriticidadeProjeto | ''>('')
  const [pagina, setPagina] = useState(0)
  const [tamanho, setTamanho] = useState(20)
  const nome = useAtrasado(busca.trim())

  const consulta = useProjetos({ nome: nome || undefined, status: status || undefined, criticidade: criticidade || undefined, pagina, tamanho })
  const dados = consulta.data

  function filtrar<T>(definir: (valor: T) => void) {
    return (valor: T) => {
      definir(valor)
      setPagina(0)
    }
  }

  return (
    <>
      <Cabecalho
        titulo="Projetos"
        subtitulo="Projetos que precisam de equipe, com os papéis, a stack e a carga horária exigidos."
        acoes={BOTAO_NOVO}
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <CampoBusca rotulo="Buscar projeto" valor={busca} aoMudar={filtrar(setBusca)} />
        <FiltroSelect rotulo="Status" valor={status} aoMudar={filtrar(setStatus)} itens={[['', 'Todos'], ...opcoes(ROTULO_STATUS)]} />
        <FiltroSelect
          rotulo="Criticidade"
          valor={criticidade}
          aoMudar={filtrar(setCriticidade)}
          itens={[['', 'Todas'], ...opcoes(ROTULO_CRITICIDADE)]}
        />
      </Stack>

      {consulta.isError ? (
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      ) : dados && dados.totalElementos === 0 ? (
        <EstadoVazio
          icone={<FolderOutlined />}
          titulo="Nenhum projeto encontrado"
          descricao={
            nome || status || criticidade
              ? 'Nenhum projeto corresponde aos filtros escolhidos.'
              : 'Cadastre um projeto com seus requisitos para gerar a primeira recomendação de equipe.'
          }
          acao={nome || status || criticidade ? undefined : BOTAO_NOVO}
        />
      ) : (
        <Paper>
          <TableContainer>
            <Table sx={{ '& td': { py: 1.5 } }}>
              <TableHead>
                <TableRow sx={CABECALHO_TABELA}>
                  <TableCell>Projeto</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Criticidade</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Equipe</TableCell>
                  <TableCell sx={SO_EM_TELA_LARGA}>Período previsto</TableCell>
                  <TableCell padding="checkbox" />
                </TableRow>
              </TableHead>
              <TableBody>
                {!dados
                  ? Array.from({ length: 4 }, (_, i) => (
                      <TableRow key={i}>
                        {Array.from({ length: 6 }, (_, j) => (
                          <TableCell key={j} sx={j > 1 && j < 5 ? SO_EM_TELA_LARGA : undefined}>
                            <Skeleton />
                          </TableCell>
                        ))}
                      </TableRow>
                    ))
                  : dados.conteudo.map((projeto) => (
                      <TableRow
                        key={projeto.id}
                        hover
                        onClick={() => navigate(`/projetos/${projeto.id}`)}
                        sx={{ cursor: 'pointer', '&:last-child td': { border: 0 } }}
                      >
                        <TableCell>
                          <Link
                            component={RouterLink}
                            to={`/projetos/${projeto.id}`}
                            underline="hover"
                            color="inherit"
                            onClick={(evento) => evento.stopPropagation()}
                            sx={{ fontWeight: 600, fontSize: '0.9rem' }}
                          >
                            {projeto.nome}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <ChipStatus status={projeto.status} />
                        </TableCell>
                        <TableCell sx={SO_EM_TELA_LARGA}>
                          <ChipCriticidade criticidade={projeto.criticidade} />
                        </TableCell>
                        <TableCell sx={{ ...SO_EM_TELA_LARGA, fontVariantNumeric: 'tabular-nums' }}>
                          {projeto.tamanhoEquipe} × {projeto.horasSemanaisPorIntegrante} h
                        </TableCell>
                        <TableCell sx={SO_EM_TELA_LARGA}>
                          <Typography variant="body2" color={projeto.dataInicioPrevista || projeto.dataFimPrevista ? 'textPrimary' : 'textSecondary'}>
                            {periodo(projeto.dataInicioPrevista, projeto.dataFimPrevista)}
                          </Typography>
                        </TableCell>
                        <TableCell padding="checkbox" sx={{ color: 'text.secondary' }}>
                          <ChevronRight fontSize="small" aria-hidden />
                        </TableCell>
                      </TableRow>
                    ))}
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
    </>
  )
}
