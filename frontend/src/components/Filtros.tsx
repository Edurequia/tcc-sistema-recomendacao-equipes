import Search from '@mui/icons-material/Search'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'

/** Select compacto das barras de filtro; o valor '' representa "todos". */
export function FiltroSelect<T extends string>({
  rotulo,
  valor,
  aoMudar,
  itens,
}: {
  rotulo: string
  valor: T | ''
  aoMudar: (valor: T | '') => void
  itens: [T | '', string][]
}) {
  return (
    <TextField select size="small" label={rotulo} value={valor} onChange={(e) => aoMudar(e.target.value as T | '')} sx={{ minWidth: 150 }}>
      {itens.map(([v, r]) => (
        <MenuItem key={v || 'todos'} value={v}>
          {r}
        </MenuItem>
      ))}
    </TextField>
  )
}

/** Campo de busca por texto das barras de filtro. */
export function CampoBusca({ rotulo, valor, aoMudar }: { rotulo: string; valor: string; aoMudar: (valor: string) => void }) {
  return (
    <TextField
      type="search"
      size="small"
      placeholder="Buscar pelo nome"
      value={valor}
      onChange={(evento) => aoMudar(evento.target.value)}
      sx={{ minWidth: { md: 260 }, flexGrow: { md: 1 }, maxWidth: { md: 360 } }}
      slotProps={{
        htmlInput: { 'aria-label': rotulo },
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <Search fontSize="small" />
            </InputAdornment>
          ),
        },
      }}
    />
  )
}
