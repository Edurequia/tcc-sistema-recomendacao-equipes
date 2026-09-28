import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined'
import FolderOutlined from '@mui/icons-material/FolderOutlined'
import PeopleOutlined from '@mui/icons-material/PeopleOutlined'
import SchoolOutlined from '@mui/icons-material/SchoolOutlined'
import type { ReactNode } from 'react'

export interface Area {
  caminho: string
  rotulo: string
  descricao: string
  icone: ReactNode
  /** Outros prefixos de caminho que pertencem a esta area (para o breadcrumb). */
  tambem?: string[]
}

export interface SecaoMenu {
  titulo: string
  areas: Area[]
}

/** Areas do sistema, agrupadas como aparecem no menu lateral. */
export const SECOES_MENU: SecaoMenu[] = [
  {
    titulo: 'Gestão',
    areas: [
      {
        caminho: '/projetos',
        rotulo: 'Projetos',
        descricao: 'Requisitos de cada projeto, recomendações geradas e equipe alocada.',
        icone: <FolderOutlined />,
        tambem: ['/recomendacoes'],
      },
      { caminho: '/desenvolvedores', rotulo: 'Desenvolvedores', descricao: 'Perfis, competências e disponibilidade semanal dos profissionais.', icone: <PeopleOutlined /> },
      { caminho: '/habilidades', rotulo: 'Habilidades', descricao: 'Catálogo de tecnologias e competências comportamentais.', icone: <SchoolOutlined /> },
    ],
  },
  {
    titulo: 'Inteligência',
    areas: [
      { caminho: '/assistente', rotulo: 'Assistente', descricao: 'Descreva a equipe em linguagem natural e receba a recomendação.', icone: <AutoAwesomeOutlined /> },
    ],
  },
]

export const AREAS: Area[] = SECOES_MENU.flatMap((secao) => secao.areas)

export function areaDoCaminho(caminho: string): Area | undefined {
  const pertence = (prefixo: string) => caminho === prefixo || caminho.startsWith(`${prefixo}/`)
  return AREAS.find((area) => [area.caminho, ...(area.tambem ?? [])].some(pertence))
}
