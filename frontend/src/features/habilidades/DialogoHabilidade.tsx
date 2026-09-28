import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useId, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErroApi } from '../../api/cliente'
import { useSalvarHabilidade } from '../../api/habilidades'
import { opcoes, ROTULO_CATEGORIA, ROTULO_TIPO_HABILIDADE } from '../../api/rotulos'
import type { CategoriaHabilidade, Habilidade, TipoHabilidade } from '../../api/tipos'
import { aplicarErrosDoServidor } from '../../components/errosFormulario'
import { useNotificar } from '../../components/notificar'

// Mesmos limites do HabilidadeRequest no backend.
const esquema = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(80, 'Use no máximo 80 caracteres.'),
  tipo: z.enum(['HARD', 'SOFT']),
  categoria: z.string().min(1, 'Escolha a categoria.'),
  chaveDataset: z.string().trim().max(120, 'Use no máximo 120 caracteres.'),
})

type Campos = z.infer<typeof esquema>

const CAMPOS: (keyof Campos)[] = ['nome', 'tipo', 'categoria', 'chaveDataset']

function valoresIniciais(habilidade?: Habilidade): Campos {
  return {
    nome: habilidade?.nome ?? '',
    tipo: habilidade?.tipo ?? 'HARD',
    categoria: habilidade?.categoria ?? '',
    chaveDataset: habilidade?.chaveDataset ?? '',
  }
}

interface Props {
  /** Habilidade em edicao; ausente, o dialogo cadastra uma nova. */
  habilidade?: Habilidade
  aoFechar: () => void
}

/** Montado apenas enquanto aberto: cada abertura comeca com o formulario limpo. */
export function DialogoHabilidade({ habilidade, aoFechar }: Props) {
  const idTitulo = useId()
  const edicao = habilidade !== undefined
  const salvar = useSalvarHabilidade()
  const notificar = useNotificar()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    control,
    handleSubmit,
    setError,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<Campos>({ resolver: zodResolver(esquema), defaultValues: valoresIniciais(habilidade) })

  const enviar = handleSubmit(async (campos) => {
    setErroGeral(null)
    try {
      const salva = await salvar.mutateAsync({
        id: habilidade?.id,
        dados: {
          nome: campos.nome,
          tipo: campos.tipo,
          categoria: campos.categoria as CategoriaHabilidade,
          chaveDataset: campos.chaveDataset || null,
        },
      })
      notificar(`${salva.nome} ${edicao ? 'atualizada' : 'cadastrada'}.`)
      aoFechar()
    } catch (erro) {
      // So o conflito de nome vai para o campo; o de chave duplicada chega com a
      // mensagem generica de integridade e fica no topo do formulario.
      const conflitoDeNome = erro instanceof ErroApi && erro.message.startsWith('Ja existe uma habilidade')
      setErroGeral(aplicarErrosDoServidor(erro, setError, CAMPOS, conflitoDeNome ? 'nome' : undefined))
    }
  })

  return (
    <Dialog open onClose={salvar.isPending ? undefined : aoFechar} aria-labelledby={idTitulo} maxWidth="sm" fullWidth>
      <form onSubmit={enviar} noValidate>
        <DialogTitle id={idTitulo}>{edicao ? 'Editar habilidade' : 'Nova habilidade'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {erroGeral && <Alert severity="error">{erroGeral}</Alert>}

            <Controller
              name="nome"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Nome"
                  required
                  autoFocus
                  error={!!errors.nome}
                  helperText={errors.nome?.message ?? 'Como a habilidade aparece nos perfis e projetos.'}
                />
              )}
            />

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <Controller
                name="tipo"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    select
                    label="Tipo"
                    fullWidth
                    onChange={(evento) => {
                      const tipo = evento.target.value as TipoHabilidade
                      field.onChange(tipo)
                      // Competencia comportamental so tem uma categoria possivel.
                      if (tipo === 'SOFT' && !getValues('categoria')) {
                        setValue('categoria', 'COMPORTAMENTAL', { shouldValidate: true })
                      }
                    }}
                  >
                    {opcoes(ROTULO_TIPO_HABILIDADE).map(([valor, rotulo]) => (
                      <MenuItem key={valor} value={valor}>
                        {rotulo}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <Controller
                name="categoria"
                control={control}
                render={({ field }) => (
                  <TextField {...field} select label="Categoria" fullWidth error={!!errors.categoria} helperText={errors.categoria?.message}>
                    {opcoes(ROTULO_CATEGORIA).map(([valor, rotulo]) => (
                      <MenuItem key={valor} value={valor}>
                        {rotulo}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Stack>

            <Controller
              name="chaveDataset"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Chave no dataset (opcional)"
                  error={!!errors.chaveDataset}
                  helperText={
                    errors.chaveDataset?.message ??
                    'Nome exato da coluna correspondente nos dados de treinamento do modelo. Deixe vazio se não houver.'
                  }
                />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={aoFechar} disabled={salvar.isPending} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" loading={salvar.isPending}>
            {edicao ? 'Salvar' : 'Cadastrar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
