import { zodResolver } from '@hookform/resolvers/zod'
import ArrowBack from '@mui/icons-material/ArrowBack'
import CloseRounded from '@mui/icons-material/CloseRounded'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import FormControl from '@mui/material/FormControl'
import FormHelperText from '@mui/material/FormHelperText'
import FormLabel from '@mui/material/FormLabel'
import IconButton from '@mui/material/IconButton'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Rating from '@mui/material/Rating'
import Skeleton from '@mui/material/Skeleton'
import Slider from '@mui/material/Slider'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import { useId, useMemo, useState, type ReactNode } from 'react'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import { useDesenvolvedor, useSalvarDesenvolvedor } from '../../api/desenvolvedores'
import { useHabilidades } from '../../api/habilidades'
import { opcoes, ROTULO_PAPEL, ROTULO_SENIORIDADE, ROTULO_TIPO_HABILIDADE } from '../../api/rotulos'
import type { Desenvolvedor, Habilidade, PapelAtuacao, Senioridade, TipoHabilidade } from '../../api/tipos'
import { Cabecalho } from '../../components/Cabecalho'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { aplicarErrosDoServidor } from '../../components/errosFormulario'
import { useNotificar } from '../../components/notificar'
import { decimal } from '../../texto'

const NIVEIS_PROFICIENCIA = ['', 'Iniciante', 'Básico', 'Intermediário', 'Avançado', 'Especialista']
const PROFICIENCIA_INICIAL = 3

// Mesmos limites do DesenvolvedorRequest no backend.
const TEXTO_NOME = 'O nome deve ter entre 3 e 120 caracteres.'
const esquema = z.object({
  nome: z.string().trim().min(3, TEXTO_NOME).max(120, TEXTO_NOME),
  email: z.string().trim().max(150, 'Use no máximo 150 caracteres.').pipe(z.email('Informe um e-mail válido.')),
  papelPrincipal: z.string().min(1, 'Escolha o papel principal.'),
  senioridade: z.string().min(1, 'Escolha a senioridade.'),
  softSkillsScore: z.number().min(0).max(5),
  horasDisponiveis: z
    .number({ error: 'Informe as horas disponíveis.' })
    .int('Use um número inteiro.')
    .min(0, 'As horas não podem ser negativas.')
    .max(60, 'O limite é 60 horas semanais.'),
  habilidades: z.array(
    z.object({ habilidadeId: z.number(), nome: z.string(), tipo: z.string(), proficiencia: z.number().int().min(1).max(5) }),
  ),
})

type Campos = z.infer<typeof esquema>

const CAMPOS_SERVIDOR: (keyof Campos)[] = ['nome', 'email', 'papelPrincipal', 'senioridade', 'softSkillsScore', 'horasDisponiveis']

function valoresIniciais(dev?: Desenvolvedor): Campos {
  return {
    nome: dev?.nome ?? '',
    email: dev?.email ?? '',
    papelPrincipal: dev?.papelPrincipal ?? '',
    senioridade: dev?.senioridade ?? '',
    softSkillsScore: dev?.softSkillsScore ?? 3,
    horasDisponiveis: dev?.horasDisponiveis ?? 40,
    habilidades: (dev?.habilidades ?? []).map((h) => ({ habilidadeId: h.habilidadeId, nome: h.nome, tipo: h.tipo, proficiencia: h.proficiencia })),
  }
}

function Secao({ titulo, descricao, children }: { titulo: string; descricao: string; children: ReactNode }) {
  return (
    <Paper component="section" sx={{ p: { xs: 2, md: 3 } }}>
      <Typography variant="h2" sx={{ mb: 0.5 }}>
        {titulo}
      </Typography>
      <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
        {descricao}
      </Typography>
      {children}
    </Paper>
  )
}

function Formulario({ desenvolvedor }: { desenvolvedor?: Desenvolvedor }) {
  const edicao = desenvolvedor !== undefined
  const navigate = useNavigate()
  const notificar = useNotificar()
  const salvar = useSalvarDesenvolvedor()
  const catalogo = useHabilidades({ apenasAtivas: true })
  const idSenioridade = useId()
  const idSoftSkills = useId()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [busca, setBusca] = useState('')

  const {
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Campos>({ resolver: zodResolver(esquema), defaultValues: valoresIniciais(desenvolvedor) })
  const competencias = useFieldArray({ control, name: 'habilidades', keyName: 'chave' })

  const disponiveis = useMemo(() => {
    const escolhidas = new Set(competencias.fields.map((c) => c.habilidadeId))
    return (catalogo.data ?? [])
      .filter((h) => !escolhidas.has(h.id))
      .sort((a, b) => a.tipo.localeCompare(b.tipo) || a.nome.localeCompare(b.nome, 'pt-BR'))
  }, [catalogo.data, competencias.fields])

  function adicionar(habilidade: Habilidade | null) {
    if (!habilidade) return
    competencias.append({ habilidadeId: habilidade.id, nome: habilidade.nome, tipo: habilidade.tipo, proficiencia: PROFICIENCIA_INICIAL })
    setBusca('')
  }

  const enviar = handleSubmit(async (campos) => {
    setErroGeral(null)
    try {
      const salvo = await salvar.mutateAsync({
        id: desenvolvedor?.id,
        dados: {
          nome: campos.nome,
          email: campos.email,
          papelPrincipal: campos.papelPrincipal as PapelAtuacao,
          senioridade: campos.senioridade as Senioridade,
          softSkillsScore: campos.softSkillsScore,
          horasDisponiveis: campos.horasDisponiveis,
          habilidades: campos.habilidades.map((h) => ({ habilidadeId: h.habilidadeId, proficiencia: h.proficiencia })),
        },
      })
      notificar(`${salvo.nome} ${edicao ? 'atualizado(a)' : 'cadastrado(a)'}.`)
      navigate('/desenvolvedores')
    } catch (erro) {
      setErroGeral(aplicarErrosDoServidor(erro, setError, CAMPOS_SERVIDOR, 'email'))
    }
  })

  return (
    <form onSubmit={enviar} noValidate>
      <Stack spacing={3}>
        {erroGeral && (
          <Alert severity="error" role="alert">
            {erroGeral}
          </Alert>
        )}

        <Secao titulo="Perfil" descricao="Dados usados pelo motor de predição para calcular o escore de afinidade.">
          <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
            <Controller
              name="nome"
              control={control}
              render={({ field }) => (
                <TextField {...field} label="Nome completo" required error={!!errors.nome} helperText={errors.nome?.message} />
              )}
            />
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <TextField {...field} type="email" label="E-mail" required error={!!errors.email} helperText={errors.email?.message} />
              )}
            />
            <Controller
              name="papelPrincipal"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  label="Papel principal"
                  error={!!errors.papelPrincipal}
                  helperText={errors.papelPrincipal?.message ?? 'Área técnica em que o profissional atua.'}
                >
                  {opcoes(ROTULO_PAPEL).map(([valor, rotulo]) => (
                    <MenuItem key={valor} value={valor}>
                      {rotulo}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <Controller
              name="horasDisponiveis"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  value={Number.isNaN(field.value) ? '' : field.value}
                  onChange={(e) => field.onChange(e.target.value === '' ? NaN : Number(e.target.value))}
                  type="number"
                  label="Horas disponíveis por semana"
                  error={!!errors.horasDisponiveis}
                  helperText={errors.horasDisponiveis?.message ?? 'De 0 a 60. As alocações em projetos ativos consomem estas horas.'}
                  slotProps={{
                    htmlInput: { min: 0, max: 60, step: 1 },
                    input: { endAdornment: <InputAdornment position="end">h</InputAdornment> },
                  }}
                />
              )}
            />
            <Controller
              name="senioridade"
              control={control}
              render={({ field }) => (
                <FormControl error={!!errors.senioridade}>
                  <FormLabel id={idSenioridade} sx={{ mb: 1, fontSize: '0.85rem' }}>
                    Senioridade
                  </FormLabel>
                  <ToggleButtonGroup
                    aria-labelledby={idSenioridade}
                    exclusive
                    fullWidth
                    size="small"
                    color="primary"
                    value={field.value || null}
                    onChange={(_, valor: Senioridade | null) => valor && field.onChange(valor)}
                  >
                    {opcoes(ROTULO_SENIORIDADE).map(([valor, rotulo]) => (
                      <ToggleButton key={valor} value={valor}>
                        {rotulo}
                      </ToggleButton>
                    ))}
                  </ToggleButtonGroup>
                  {errors.senioridade && <FormHelperText>{errors.senioridade.message}</FormHelperText>}
                </FormControl>
              )}
            />
            <Controller
              name="softSkillsScore"
              control={control}
              render={({ field }) => (
                <FormControl>
                  <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}>
                    <FormLabel id={idSoftSkills} sx={{ fontSize: '0.85rem' }}>
                      Soft skills
                    </FormLabel>
                    <Typography sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{decimal(field.value)} / 5</Typography>
                  </Stack>
                  <Slider
                    value={field.value}
                    onChange={(_, valor) => field.onChange(valor as number)}
                    min={0}
                    max={5}
                    step={0.1}
                    slotProps={{ input: { 'aria-label': 'Soft skills' } }}
                    aria-labelledby={idSoftSkills}
                  />
                  <FormHelperText sx={{ mx: 0 }}>Avaliação das competências comportamentais, de 0 a 5.</FormHelperText>
                </FormControl>
              )}
            />
          </Box>
        </Secao>

        <Secao
          titulo="Competências"
          descricao="Tecnologias e competências comportamentais que o profissional domina, com o nível de proficiência de cada uma."
        >
          <Autocomplete
            options={disponiveis}
            value={null}
            inputValue={busca}
            onInputChange={(_, valor, motivo) => motivo !== 'reset' && setBusca(valor)}
            onChange={(_, habilidade) => adicionar(habilidade)}
            groupBy={(h) => (h.tipo === 'HARD' ? 'Técnicas' : 'Comportamentais')}
            getOptionLabel={(h) => h.nome}
            loading={catalogo.isPending}
            noOptionsText="Nenhuma habilidade disponível"
            loadingText="Carregando catálogo…"
            blurOnSelect
            renderInput={(params) => <TextField {...params} label="Adicionar competência" placeholder="Busque no catálogo" />}
            sx={{ mb: 2, maxWidth: { md: 420 } }}
          />

          {competencias.fields.length === 0 ? (
            <Typography variant="body2" color="textSecondary" sx={{ py: 3, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2 }}>
              Nenhuma competência adicionada. Sem competências técnicas, a aderência técnica do profissional será zero.
            </Typography>
          ) : (
            <Stack divider={<Box sx={{ borderTop: 1, borderColor: 'divider' }} />} sx={{ border: 1, borderColor: 'divider', borderRadius: 2 }}>
              {competencias.fields.map((competencia, indice) => (
                <Stack
                  key={competencia.chave}
                  role="group"
                  aria-label={`Proficiência em ${competencia.nome}`}
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={{ xs: 1, sm: 2 }}
                  sx={{ px: 2, py: 1.25, alignItems: { sm: 'center' } }}
                >
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexGrow: 1, minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }} noWrap>
                      {competencia.nome}
                    </Typography>
                    <Chip
                      size="small"
                      variant="outlined"
                      color={competencia.tipo === 'HARD' ? 'primary' : 'secondary'}
                      label={ROTULO_TIPO_HABILIDADE[competencia.tipo as TipoHabilidade]}
                    />
                  </Stack>
                  <Controller
                    name={`habilidades.${indice}.proficiencia`}
                    control={control}
                    render={({ field }) => (
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                        <Rating
                          name={`proficiencia-${competencia.chave}`}
                          value={field.value}
                          onChange={(_, valor) => valor && field.onChange(valor)}
                          getLabelText={(valor) => `${valor} de 5: ${NIVEIS_PROFICIENCIA[valor]}`}
                        />
                        <Typography variant="body2" color="textSecondary" sx={{ width: 96 }}>
                          {NIVEIS_PROFICIENCIA[field.value]}
                        </Typography>
                      </Stack>
                    )}
                  />
                  <IconButton
                    aria-label={`Remover ${competencia.nome}`}
                    onClick={() => competencias.remove(indice)}
                    size="small"
                    sx={{ alignSelf: { xs: 'flex-end', sm: 'center' } }}
                  >
                    <CloseRounded fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
            </Stack>
          )}
        </Secao>

        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end' }}>
          <Button component={RouterLink} to="/desenvolvedores" color="inherit" disabled={salvar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" size="large" loading={salvar.isPending}>
            {edicao ? 'Salvar alterações' : 'Cadastrar'}
          </Button>
        </Stack>
      </Stack>
    </form>
  )
}

const VOLTAR = (
  <Button component={RouterLink} to="/desenvolvedores" startIcon={<ArrowBack />} color="inherit">
    Voltar
  </Button>
)

export function FormularioDesenvolvedor() {
  const { id } = useParams()
  const idNumerico = id && id !== 'novo' ? Number(id) : undefined
  const consulta = useDesenvolvedor(idNumerico)

  if (idNumerico === undefined) {
    return (
      <>
        <Cabecalho titulo="Novo desenvolvedor" subtitulo="Cadastre o perfil e as competências do profissional." acoes={VOLTAR} />
        <Formulario />
      </>
    )
  }

  if (consulta.isError) {
    return (
      <>
        <Cabecalho titulo="Desenvolvedor" acoes={VOLTAR} />
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      </>
    )
  }

  if (!consulta.data) {
    return (
      <>
        <Skeleton variant="text" width={280} height={48} />
        <Skeleton variant="rounded" height={320} sx={{ mt: 3 }} />
      </>
    )
  }

  return (
    <>
      <Cabecalho titulo={consulta.data.nome} subtitulo="Edite o perfil e as competências do profissional." acoes={VOLTAR} />
      <Formulario key={consulta.data.id} desenvolvedor={consulta.data} />
    </>
  )
}
