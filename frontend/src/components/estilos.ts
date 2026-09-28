/** Esconde visualmente mantendo o texto para leitores de tela. */
export const visuallyHidden = {
  border: 0,
  clip: 'rect(0 0 0 0)',
  height: 1,
  margin: -1,
  overflow: 'hidden',
  padding: 0,
  position: 'absolute',
  whiteSpace: 'nowrap',
  width: 1,
} as const

/** Colunas secundarias de tabela somem em telas estreitas. */
export const SO_EM_TELA_LARGA = { display: { xs: 'none', md: 'table-cell' } } as const

/** Cabecalho de tabela discreto, padrao das listagens. */
export const CABECALHO_TABELA = { '& th': { color: 'text.secondary', fontWeight: 600, fontSize: '0.8rem' } } as const
