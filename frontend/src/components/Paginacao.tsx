import TablePagination from '@mui/material/TablePagination'

const ROTULOS_NAVEGACAO = { first: 'Primeira página', last: 'Última página', next: 'Próxima página', previous: 'Página anterior' } as const

/** Rodape de paginacao das listagens paginadas no servidor. */
export function Paginacao({
  total,
  pagina,
  tamanho,
  aoMudarPagina,
  aoMudarTamanho,
}: {
  total: number
  pagina: number
  tamanho: number
  aoMudarPagina: (pagina: number) => void
  aoMudarTamanho: (tamanho: number) => void
}) {
  return (
    <TablePagination
      component="div"
      count={total}
      page={pagina}
      rowsPerPage={tamanho}
      rowsPerPageOptions={[10, 20, 50]}
      onPageChange={(_, nova) => aoMudarPagina(nova)}
      onRowsPerPageChange={(evento) => aoMudarTamanho(Number(evento.target.value))}
      labelRowsPerPage="Por página"
      labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
      getItemAriaLabel={(tipo) => ROTULOS_NAVEGACAO[tipo]}
      sx={{ borderTop: 1, borderColor: 'divider' }}
    />
  )
}
