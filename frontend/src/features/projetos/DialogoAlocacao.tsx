import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useId, useState, type FormEvent } from 'react'
import { useAlocar } from '../../api/alocacoes'
import { useDesenvolvedores } from '../../api/desenvolvedores'
import { opcoes, ROTULO_PAPEL, ROTULO_SENIORIDADE } from '../../api/rotulos'
import type { AlocacaoRequest, DesenvolvedorResumo, PapelAtuacao, Projeto } from '../../api/tipos'
import { AvatarIniciais } from '../../components/AvatarIniciais'
import { useNotificar } from '../../components/notificar'
import { useAtrasado } from '../../useAtrasado'

/** Alocacao manual de um desenvolvedor ativo no projeto. Montado so enquanto aberto. */
export function DialogoAlocacao({ projeto, aoFechar }: { projeto: Projeto; aoFechar: () => void }) {
  const idTitulo = useId()
  const notificar = useNotificar()
  const alocar = useAlocar(projeto.id)
  const [busca, setBusca] = useState('')
  const [desenvolvedor, setDesenvolvedor] = useState<DesenvolvedorResumo | null>(null)
  const [papel, setPapel] = useState<PapelAtuacao | ''>('')
  const [horas, setHoras] = useState('')
  const [inicio, setInicio] = useState('')
  const [erroCampo, setErroCampo] = useState<string | null>(null)
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const nome = useAtrasado(busca.trim())
  const candidatos = useDesenvolvedores({ nome: nome || undefined, ativo: true, pagina: 0, tamanho: 10 })

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!desenvolvedor) {
      setErroCampo('Escolha o desenvolvedor.')
      return
    }
    setErroCampo(null)
    setErroGeral(null)
    // So vai o que foi preenchido: o backend completa com os padroes do projeto e do perfil.
    const dados: AlocacaoRequest = { desenvolvedorId: desenvolvedor.id }
    if (papel) dados.papel = papel
    if (horas) dados.horasSemanais = Number(horas)
    if (inicio) dados.dataInicio = inicio
    try {
      await alocar.mutateAsync(dados)
      notificar(`${desenvolvedor.nome} alocado(a) no projeto.`)
      aoFechar()
    } catch (erro) {
      setErroGeral((erro as Error).message)
    }
  }

  return (
    <Dialog open onClose={alocar.isPending ? undefined : aoFechar} aria-labelledby={idTitulo} maxWidth="sm" fullWidth>
      <form onSubmit={enviar} noValidate>
        <DialogTitle id={idTitulo}>Alocar desenvolvedor</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {erroGeral && <Alert severity="error">{erroGeral}</Alert>}
            <Autocomplete
              options={candidatos.data?.conteudo ?? []}
              value={desenvolvedor}
              onChange={(_, valor) => setDesenvolvedor(valor)}
              inputValue={busca}
              onInputChange={(_, valor) => setBusca(valor)}
              filterOptions={(x) => x}
              isOptionEqualToValue={(a, b) => a.id === b.id}
              getOptionLabel={(d) => d.nome}
              loading={candidatos.isFetching}
              noOptionsText="Nenhum desenvolvedor ativo encontrado"
              loadingText="Buscando…"
              renderOption={({ key, ...props }, d) => (
                <Box component="li" key={key} {...props} sx={{ gap: 1.5 }}>
                  <AvatarIniciais nome={d.nome} tamanho={28} />
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {d.nome}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {ROTULO_PAPEL[d.papelPrincipal]} · {ROTULO_SENIORIDADE[d.senioridade]} · {d.horasDisponiveis} h/semana
                    </Typography>
                  </Box>
                </Box>
              )}
              renderInput={(params) => (
                <TextField {...params} label="Desenvolvedor" autoFocus error={erroCampo !== null} helperText={erroCampo ?? 'Busque pelo nome entre os ativos.'} />
              )}
            />
            <TextField select label="Papel no projeto" value={papel} onChange={(e) => setPapel(e.target.value as PapelAtuacao | '')}>
              <MenuItem value="">Papel principal do desenvolvedor</MenuItem>
              {opcoes(ROTULO_PAPEL).map(([valor, rotulo]) => (
                <MenuItem key={valor} value={valor}>
                  {rotulo}
                </MenuItem>
              ))}
            </TextField>
            <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' } }}>
              <TextField
                label="Horas semanais"
                type="number"
                value={horas}
                onChange={(e) => setHoras(e.target.value)}
                placeholder={String(projeto.horasSemanaisPorIntegrante)}
                helperText={`Vazio, usa ${projeto.horasSemanaisPorIntegrante} h do projeto.`}
                slotProps={{
                  inputLabel: { shrink: true },
                  htmlInput: { min: 1, max: 60 },
                  input: { endAdornment: <InputAdornment position="end">h</InputAdornment> },
                }}
              />
              <TextField
                label="Início"
                type="date"
                value={inicio}
                onChange={(e) => setInicio(e.target.value)}
                helperText="Vazio, usa a data de hoje."
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={aoFechar} disabled={alocar.isPending} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" loading={alocar.isPending}>
            Alocar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
