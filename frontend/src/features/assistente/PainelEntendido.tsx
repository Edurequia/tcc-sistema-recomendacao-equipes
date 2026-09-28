import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { ROTULO_CRITICIDADE } from '../../api/rotulos'
import type { CriticidadeProjeto, ParametrosEntendidos } from '../../api/tipos'
import { Painel } from '../../components/Painel'

function Linha({ rotulo, children }: { rotulo: string; children: ReactNode }) {
  return (
    <Box sx={{ py: 1.25, '& + &': { borderTop: 1, borderColor: 'divider' } }}>
      <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 0.25 }}>
        {rotulo}
      </Typography>
      {children}
    </Box>
  )
}

function Valor({ valor, obrigatorio }: { valor?: string | number | null; obrigatorio?: boolean }) {
  if (valor !== undefined && valor !== null && valor !== '') {
    return <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }}>{valor}</Typography>
  }
  return obrigatorio ? (
    <Typography variant="body2" color="warning" sx={{ fontWeight: 600 }}>
      Falta informar
    </Typography>
  ) : (
    <Typography variant="body2" color="textSecondary">
      —
    </Typography>
  )
}

function Etiquetas({ itens }: { itens?: string[] }) {
  if (!itens?.length) return <Valor />
  return (
    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75, mt: 0.5 }}>
      {itens.map((item) => (
        <Chip key={item} size="small" label={item} />
      ))}
    </Stack>
  )
}

/** O que o modelo de linguagem ja extraiu do pedido; os tres campos obrigatorios destacam a falta. */
export function PainelEntendido({ entendido }: { entendido?: ParametrosEntendidos }) {
  return (
    <Painel titulo="O que já entendi">
      {!entendido ? (
        <Typography variant="body2" color="textSecondary">
          Conforme a conversa avança, os requisitos reconhecidos aparecem aqui. Nome do projeto, tamanho da equipe e horas semanais são
          obrigatórios; papéis, tecnologias e criticidade refinam a recomendação.
        </Typography>
      ) : (
        <Box>
          <Linha rotulo="Projeto">
            <Valor valor={entendido.nome} obrigatorio />
          </Linha>
          <Linha rotulo="Tamanho da equipe">
            <Valor valor={entendido.tamanhoEquipe ? `${entendido.tamanhoEquipe} integrantes` : undefined} obrigatorio />
          </Linha>
          <Linha rotulo="Horas por integrante">
            <Valor valor={entendido.horasSemanaisPorIntegrante ? `${entendido.horasSemanaisPorIntegrante} h/semana` : undefined} obrigatorio />
          </Linha>
          <Linha rotulo="Criticidade">
            <Valor valor={entendido.criticidade ? ROTULO_CRITICIDADE[entendido.criticidade as CriticidadeProjeto] ?? entendido.criticidade : undefined} />
          </Linha>
          <Linha rotulo="Papéis">
            <Etiquetas itens={entendido.papeis} />
          </Linha>
          <Linha rotulo="Tecnologias">
            <Etiquetas itens={entendido.tecnologias} />
          </Linha>
        </Box>
      )}
    </Painel>
  )
}
