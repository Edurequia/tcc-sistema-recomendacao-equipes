import type {
  CategoriaHabilidade,
  CriticidadeProjeto,
  PapelAtuacao,
  Senioridade,
  StatusProjeto,
  TipoHabilidade,
} from './tipos'

// Rotulos dos enums para selects e filtros. As respostas da API ja trazem a
// descricao de cada valor; estes mapas cobrem os formularios, que precisam de
// todas as opcoes antes de haver resposta.

export const ROTULO_SENIORIDADE: Record<Senioridade, string> = {
  JUNIOR: 'Júnior',
  PLENO: 'Pleno',
  SENIOR: 'Sênior',
}

export const ROTULO_PAPEL: Record<PapelAtuacao, string> = {
  BACKEND: 'Backend',
  FRONTEND: 'Frontend',
  FULLSTACK: 'Full-stack',
  MOBILE: 'Mobile',
  QA: 'QA / Testes',
  DEVOPS: 'DevOps',
}

export const ROTULO_CRITICIDADE: Record<CriticidadeProjeto, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
}

export const ROTULO_STATUS: Record<StatusProjeto, string> = {
  PLANEJAMENTO: 'Em planejamento',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

export const ROTULO_TIPO_HABILIDADE: Record<TipoHabilidade, string> = {
  HARD: 'Técnica',
  SOFT: 'Comportamental',
}

export const ROTULO_CATEGORIA: Record<CategoriaHabilidade, string> = {
  LINGUAGEM: 'Linguagem',
  FRAMEWORK: 'Framework ou biblioteca',
  BANCO_DE_DADOS: 'Banco de dados',
  CLOUD: 'Nuvem',
  DEVOPS: 'DevOps',
  MOBILE: 'Mobile',
  COMPORTAMENTAL: 'Comportamental',
  OUTRA: 'Outra',
}

/** Lista [valor, rotulo] na ordem de declaracao, para montar selects. */
export function opcoes<T extends string>(rotulos: Record<T, string>): [T, string][] {
  return Object.entries(rotulos) as [T, string][]
}
