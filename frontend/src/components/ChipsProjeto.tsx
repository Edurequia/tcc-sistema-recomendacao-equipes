import Chip, { type ChipProps } from '@mui/material/Chip'
import { ROTULO_CRITICIDADE, ROTULO_STATUS } from '../api/rotulos'
import type { CriticidadeProjeto, StatusProjeto } from '../api/tipos'

const COR_CRITICIDADE: Record<CriticidadeProjeto, ChipProps['color']> = { BAIXA: 'success', MEDIA: 'warning', ALTA: 'error' }
const COR_STATUS: Record<StatusProjeto, ChipProps['color']> = {
  PLANEJAMENTO: 'info',
  EM_ANDAMENTO: 'primary',
  CONCLUIDO: 'success',
  CANCELADO: 'default',
}

export function ChipCriticidade({ criticidade }: { criticidade: CriticidadeProjeto }) {
  return <Chip size="small" variant="outlined" color={COR_CRITICIDADE[criticidade]} label={ROTULO_CRITICIDADE[criticidade]} />
}

export function ChipStatus({ status }: { status: StatusProjeto }) {
  return <Chip size="small" color={COR_STATUS[status]} variant={status === 'CANCELADO' ? 'filled' : 'outlined'} label={ROTULO_STATUS[status]} />
}
