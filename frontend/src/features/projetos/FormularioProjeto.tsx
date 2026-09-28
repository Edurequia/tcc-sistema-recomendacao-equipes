import { zodResolver } from '@hookform/resolvers/zod'
import Add from '@mui/icons-material/Add'
import ArrowBack from '@mui/icons-material/ArrowBack'
import CloseRounded from '@mui/icons-material/CloseRounded'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import FormHelperText from '@mui/material/FormHelperText'
import FormLabel from '@mui/material/FormLabel'
import IconButton from '@mui/material/IconButton'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import { useId, useMemo, useState, type ReactNode } from 'react'
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import { useHabilidades } from '../../api/habilidades'
import { useProjeto, useSalvarProjeto } from '../../api/projetos'
import { opcoes, ROTULO_CATEGORIA, ROTULO_CRITICIDADE, ROTULO_PAPEL } from '../../api/rotulos'
import type { CategoriaHabilidade, CriticidadeProjeto, Habilidade, PapelAtuacao, Projeto } from '../../api/tipos'
import { Cabecalho } from '../../components/Cabecalho'
import { ErroCarregamento } from '../../components/ErroCarregamento'
import { aplicarErrosDoServidor } from '../../components/errosFormulario'
import { useNotificar } from '../../components/notificar'

const DESCRICAO_CRITICIDADE: Record<CriticidadeProjeto, string> = {
  BAIXA: 'Foco nas competências técnicas e nos papéis exigidos.',
  MEDIA: 'Equilíbrio entre habilidades técnicas e perfil comportamental.',
  ALTA: 'Projeto complexo e de alto risco: senioridade e soft skills pesam mais.',
}

const numeroInteiro = (campo: string, minimo: number, maximo: number) =>
  z
    .number({ error: `Informe ${campo}.` })
    .int('Use um número inteiro.')
    .min(minimo, `O mínimo é ${minimo}.`)
    .max(maximo, `O máximo é ${maximo}.`)

// Mesmos limites do ProjetoRequest no backend, mais as duas regras do ProjetoService
// que da para conferir antes de enviar (vagas x tamanho e ordem das datas).
const esquema = z
  .object({
    nome: z.string().trim().min(3, 'O nome deve ter entre 3 e 150 caracteres.').max(150, 'O nome deve ter entre 3 e 150 caracteres.'),
    descricao: z.string().trim().max(2000, 'Use no máximo 2000 caracteres.'),
    criticidade: z.enum(['BAIXA', 'MEDIA', 'ALTA']),
    tamanhoEquipe: numeroInteiro('o tamanho da equipe', 1, 50),
    horasSemanaisPorIntegrante: numeroInteiro('as horas semanais', 1, 60),
    dataInicioPrevista: z.string(),
    dataFimPrevista: z.string(),
    papeisRequeridos: z.array(z.object({ papel: z.string().min(1, 'Escolha o papel.'), quantidade: numeroInteiro('a quantidade', 1, 50) })),
    habilidadesRequeridas: z.array(
      z.object({ habilidadeId: z.number(), nome: z.string(), categoria: z.string(), obrigatoria: z.boolean(), peso: z.number().int().min(1).max(5) }),
    ),
  })
  .superRefine((campos, ctx) => {
    const vagas = campos.papeisRequeridos.reduce((soma, p) => soma + (Number.isNaN(p.quantidade) ? 0 : p.quantidade), 0)
    if (!Number.isNaN(campos.tamanhoEquipe) && vagas > campos.tamanhoEquipe) {
      ctx.addIssue({
        code: 'custom',
        path: ['papeisRequeridos'],
        message: `As vagas por papel somam ${vagas}, mas a equipe tem ${campos.tamanhoEquipe} integrantes.`,
      })
    }
    if (campos.dataInicioPrevista && campos.dataFimPrevista && campos.dataFimPrevista < campos.dataInicioPrevista) {
      ctx.addIssue({ code: 'custom', path: ['dataFimPrevista'], message: 'O fim previsto não pode ser anterior ao início.' })
    }
  })

type Campos = z.infer<typeof esquema>

const CAMPOS_SERVIDOR: (keyof Campos)[] = [
  'nome',
  'descricao',
  'criticidade',
  'tamanhoEquipe',
  'horasSemanaisPorIntegrante',
  'dataInicioPrevista',
  'dataFimPrevista',
]

function valoresIniciais(projeto?: Projeto): Campos {
  return {
    nome: projeto?.nome ?? '',
    descricao: projeto?.descricao ?? '',
    criticidade: projeto?.criticidade ?? 'MEDIA',
    tamanhoEquipe: projeto?.tamanhoEquipe ?? 4,
    horasSemanaisPorIntegrante: projeto?.horasSemanaisPorIntegrante ?? 20,
    dataInicioPrevista: projeto?.dataInicioPrevista ?? '',
    dataFimPrevista: projeto?.dataFimPrevista ?? '',
    papeisRequeridos: (projeto?.papeisRequeridos ?? []).map((p) => ({ papel: p.papel, quantidade: p.quantidade })),
    habilidadesRequeridas: (projeto?.habilidadesRequeridas ?? []).map((h) => ({
      habilidadeId: h.habilidadeId,
      nome: h.nome,
      categoria: h.categoria,
      obrigatoria: h.obrigatoria,
      peso: h.peso,
    })),
  }
}

/** Liga um TextField numerico a um campo number do formulario (vazio vira NaN, reprovado pelo zod). */
function propsNumero(field: { value: number; onChange: (valor: number) => void }) {
  return {
    type: 'number' as const,
    value: Number.isNaN(field.value) ? '' : field.value,
    onChange: (e: { target: { value: string } }) => field.onChange(e.target.value === '' ? NaN : Number(e.target.value)),
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

function Formulario({ projeto }: { projeto?: Projeto }) {
  const edicao = projeto !== undefined
  const navigate = useNavigate()
  const notificar = useNotificar()
  const salvar = useSalvarProjeto()
  const catalogo = useHabilidades({ tipo: 'HARD', apenasAtivas: true })
  const idCriticidade = useId()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [busca, setBusca] = useState('')

  const {
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Campos>({ resolver: zodResolver(esquema), defaultValues: valoresIniciais(projeto) })
  const papeis = useFieldArray({ control, name: 'papeisRequeridos', keyName: 'chave' })
  const stack = useFieldArray({ control, name: 'habilidadesRequeridas', keyName: 'chave' })
  const [tamanho, papeisAtuais, criticidade] = useWatch({ control, name: ['tamanhoEquipe', 'papeisRequeridos', 'criticidade'] })

  const vagasDefinidas = papeisAtuais.reduce((soma, p) => soma + (Number.isNaN(p.quantidade) ? 0 : p.quantidade), 0)
  const livres = Number.isNaN(tamanho) ? 0 : tamanho - vagasDefinidas

  const tecnologiasDisponiveis = useMemo(() => {
    const escolhidas = new Set(stack.fields.map((h) => h.habilidadeId))
    return (catalogo.data ?? []).filter((h) => !escolhidas.has(h.id)).sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
  }, [catalogo.data, stack.fields])

  function adicionarTecnologia(habilidade: Habilidade | null) {
    if (!habilidade) return
    stack.append({ habilidadeId: habilidade.id, nome: habilidade.nome, categoria: habilidade.categoria, obrigatoria: true, peso: 3 })
    setBusca('')
  }

  const enviar = handleSubmit(async (campos) => {
    setErroGeral(null)
    try {
      const salvo = await salvar.mutateAsync({
        id: projeto?.id,
        dados: {
          nome: campos.nome,
          descricao: campos.descricao || null,
          criticidade: campos.criticidade,
          tamanhoEquipe: campos.tamanhoEquipe,
          horasSemanaisPorIntegrante: campos.horasSemanaisPorIntegrante,
          dataInicioPrevista: campos.dataInicioPrevista || null,
          dataFimPrevista: campos.dataFimPrevista || null,
          papeisRequeridos: campos.papeisRequeridos.map((p) => ({ papel: p.papel as PapelAtuacao, quantidade: p.quantidade })),
          habilidadesRequeridas: campos.habilidadesRequeridas.map((h) => ({ habilidadeId: h.habilidadeId, obrigatoria: h.obrigatoria, peso: h.peso })),
        },
      })
      notificar(`Projeto ${salvo.nome} ${edicao ? 'atualizado' : 'cadastrado'}.`)
      navigate(`/projetos/${salvo.id}`)
    } catch (erro) {
      setErroGeral(aplicarErrosDoServidor(erro, setError, CAMPOS_SERVIDOR, 'nome'))
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

        <Secao titulo="Identificação" descricao="Nome, contexto e nível de risco do projeto.">
          <Stack spacing={2.5}>
            <Controller
              name="nome"
              control={control}
              render={({ field }) => (
                <TextField {...field} label="Nome do projeto" required error={!!errors.nome} helperText={errors.nome?.message} />
              )}
            />
            <Controller
              name="descricao"
              control={control}
              render={({ field }) => (
                <TextField {...field} label="Descrição (opcional)" multiline minRows={2} error={!!errors.descricao} helperText={errors.descricao?.message} />
              )}
            />
            <Controller
              name="criticidade"
              control={control}
              render={({ field }) => (
                <FormControl>
                  <FormLabel id={idCriticidade} sx={{ mb: 1, fontSize: '0.85rem' }}>
                    Criticidade
                  </FormLabel>
                  <ToggleButtonGroup
                    aria-labelledby={idCriticidade}
                    exclusive
                    size="small"
                    color="primary"
                    value={field.value}
                    onChange={(_, valor: CriticidadeProjeto | null) => valor && field.onChange(valor)}
                    sx={{ maxWidth: 420 }}
                    fullWidth
                  >
                    {opcoes(ROTULO_CRITICIDADE).map(([valor, rotulo]) => (
                      <ToggleButton key={valor} value={valor}>
                        {rotulo}
                      </ToggleButton>
                    ))}
                  </ToggleButtonGroup>
                  <FormHelperText sx={{ mx: 0 }}>{DESCRICAO_CRITICIDADE[criticidade]}</FormHelperText>
                </FormControl>
              )}
            />
            <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' } }}>
              <Controller
                name="dataInicioPrevista"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    type="date"
                    label="Início previsto"
                    error={!!errors.dataInicioPrevista}
                    helperText={errors.dataInicioPrevista?.message}
                    slotProps={{ inputLabel: { shrink: true } }}
                  />
                )}
              />
              <Controller
                name="dataFimPrevista"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    type="date"
                    label="Fim previsto"
                    error={!!errors.dataFimPrevista}
                    helperText={errors.dataFimPrevista?.message}
                    slotProps={{ inputLabel: { shrink: true } }}
                  />
                )}
              />
            </Box>
          </Stack>
        </Secao>

        <Secao titulo="Equipe" descricao="Quantas pessoas o projeto precisa, com que dedicação e em quais papéis.">
          <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, mb: 3 }}>
            <Controller
              name="tamanhoEquipe"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  {...propsNumero(field)}
                  label="Tamanho da equipe"
                  error={!!errors.tamanhoEquipe}
                  helperText={errors.tamanhoEquipe?.message}
                  slotProps={{ htmlInput: { min: 1, max: 50 }, input: { endAdornment: <InputAdornment position="end">pessoas</InputAdornment> } }}
                />
              )}
            />
            <Controller
              name="horasSemanaisPorIntegrante"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  {...propsNumero(field)}
                  label="Horas semanais por integrante"
                  error={!!errors.horasSemanaisPorIntegrante}
                  helperText={errors.horasSemanaisPorIntegrante?.message ?? 'Quem não tiver essas horas livres fica inelegível.'}
                  slotProps={{ htmlInput: { min: 1, max: 60 }, input: { endAdornment: <InputAdornment position="end">h</InputAdornment> } }}
                />
              )}
            />
          </Box>

          <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', mb: 1.5 }}>
            <Typography variant="h3">Vagas por papel</Typography>
            <Typography variant="body2" color={livres < 0 ? 'error' : 'textSecondary'}>
              {vagasDefinidas} de {Number.isNaN(tamanho) ? '–' : tamanho} vagas com papel definido · {Math.max(livres, 0)}{' '}
              {Math.max(livres, 0) === 1 ? 'livre' : 'livres'}
            </Typography>
          </Stack>
          <Stack spacing={1.5}>
            {papeis.fields.map((vaga, indice) => {
              const usadosPorOutras = new Set(papeisAtuais.filter((_, i) => i !== indice).map((p) => p.papel))
              return (
                <Stack key={vaga.chave} role="group" aria-label={`Vaga ${indice + 1}`} direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
                  <Controller
                    name={`papeisRequeridos.${indice}.papel`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        select
                        size="small"
                        label="Papel"
                        sx={{ flexGrow: 1, maxWidth: 280 }}
                        error={!!errors.papeisRequeridos?.[indice]?.papel}
                        helperText={errors.papeisRequeridos?.[indice]?.papel?.message}
                      >
                        {opcoes(ROTULO_PAPEL)
                          .filter(([valor]) => !usadosPorOutras.has(valor))
                          .map(([valor, rotulo]) => (
                            <MenuItem key={valor} value={valor}>
                              {rotulo}
                            </MenuItem>
                          ))}
                      </TextField>
                    )}
                  />
                  <Controller
                    name={`papeisRequeridos.${indice}.quantidade`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        {...propsNumero(field)}
                        size="small"
                        label="Quantidade"
                        sx={{ width: 120 }}
                        error={!!errors.papeisRequeridos?.[indice]?.quantidade}
                        helperText={errors.papeisRequeridos?.[indice]?.quantidade?.message}
                        slotProps={{ htmlInput: { min: 1, max: 50 } }}
                      />
                    )}
                  />
                  <IconButton aria-label={`Remover vaga ${indice + 1}`} onClick={() => papeis.remove(indice)} sx={{ mt: 0.25 }}>
                    <CloseRounded fontSize="small" />
                  </IconButton>
                </Stack>
              )
            })}
          </Stack>
          {errors.papeisRequeridos?.root?.message || errors.papeisRequeridos?.message ? (
            <FormHelperText error sx={{ mt: 1 }}>
              {errors.papeisRequeridos?.root?.message ?? errors.papeisRequeridos?.message}
            </FormHelperText>
          ) : null}
          <Button
            startIcon={<Add />}
            onClick={() => papeis.append({ papel: '', quantidade: 1 })}
            disabled={papeis.fields.length >= opcoes(ROTULO_PAPEL).length}
            sx={{ mt: 1.5 }}
          >
            Adicionar papel
          </Button>
          <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
            Vagas sem papel definido são preenchidas pelos maiores escores de qualquer papel.
          </Typography>
        </Secao>

        <Secao
          titulo="Stack exigida"
          descricao="Tecnologias do projeto. O peso (1 a 5) mede a importância no cálculo da aderência técnica; as obrigatórias ausentes na equipe são apontadas como restrição."
        >
          <Autocomplete
            options={tecnologiasDisponiveis}
            value={null}
            inputValue={busca}
            onInputChange={(_, valor, motivo) => motivo !== 'reset' && setBusca(valor)}
            onChange={(_, habilidade) => adicionarTecnologia(habilidade)}
            getOptionLabel={(h) => h.nome}
            loading={catalogo.isPending}
            noOptionsText="Nenhuma tecnologia disponível"
            loadingText="Carregando catálogo…"
            blurOnSelect
            renderInput={(params) => <TextField {...params} label="Adicionar tecnologia" placeholder="Busque no catálogo" />}
            sx={{ mb: 2, maxWidth: { md: 420 } }}
          />
          {stack.fields.length === 0 ? (
            <Typography variant="body2" color="textSecondary" sx={{ py: 3, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2 }}>
              Nenhuma tecnologia exigida. Sem stack, a aderência técnica não diferencia os candidatos.
            </Typography>
          ) : (
            <Stack divider={<Box sx={{ borderTop: 1, borderColor: 'divider' }} />} sx={{ border: 1, borderColor: 'divider', borderRadius: 2 }}>
              {stack.fields.map((tecnologia, indice) => (
                <Stack
                  key={tecnologia.chave}
                  role="group"
                  aria-label={`Requisito ${tecnologia.nome}`}
                  direction={{ xs: 'column', md: 'row' }}
                  spacing={{ xs: 1, md: 2.5 }}
                  sx={{ px: 2, py: 1.25, alignItems: { md: 'center' } }}
                >
                  <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }}>{tecnologia.nome}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {ROTULO_CATEGORIA[tecnologia.categoria as CategoriaHabilidade]}
                    </Typography>
                  </Box>
                  <Controller
                    name={`habilidadesRequeridas.${indice}.peso`}
                    control={control}
                    render={({ field }) => (
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography variant="body2" color="textSecondary">
                          Peso
                        </Typography>
                        <ToggleButtonGroup
                          size="small"
                          exclusive
                          color="primary"
                          aria-label={`Peso de ${tecnologia.nome}`}
                          value={field.value}
                          onChange={(_, valor: number | null) => valor && field.onChange(valor)}
                        >
                          {[1, 2, 3, 4, 5].map((peso) => (
                            <ToggleButton key={peso} value={peso} sx={{ px: 1.5, py: 0.25 }}>
                              {peso}
                            </ToggleButton>
                          ))}
                        </ToggleButtonGroup>
                      </Stack>
                    )}
                  />
                  <Controller
                    name={`habilidadesRequeridas.${indice}.obrigatoria`}
                    control={control}
                    render={({ field }) => (
                      <FormControlLabel
                        control={
                          <Switch
                            checked={field.value}
                            onChange={(e) => field.onChange(e.target.checked)}
                            slotProps={{ input: { 'aria-label': `${tecnologia.nome} obrigatória` } }}
                          />
                        }
                        label="Obrigatória"
                        sx={{ mr: 0, '& .MuiFormControlLabel-label': { fontSize: '0.875rem' } }}
                      />
                    )}
                  />
                  <IconButton
                    aria-label={`Remover ${tecnologia.nome}`}
                    onClick={() => stack.remove(indice)}
                    size="small"
                    sx={{ alignSelf: { xs: 'flex-end', md: 'center' } }}
                  >
                    <CloseRounded fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
            </Stack>
          )}
        </Secao>

        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end' }}>
          <Button component={RouterLink} to={edicao ? `/projetos/${projeto.id}` : '/projetos'} color="inherit" disabled={salvar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" size="large" loading={salvar.isPending}>
            {edicao ? 'Salvar alterações' : 'Cadastrar projeto'}
          </Button>
        </Stack>
      </Stack>
    </form>
  )
}

export function FormularioProjeto() {
  const { id } = useParams()
  const idNumerico = id ? Number(id) : undefined
  const consulta = useProjeto(idNumerico)
  const voltar = (
    <Button component={RouterLink} to={idNumerico ? `/projetos/${idNumerico}` : '/projetos'} startIcon={<ArrowBack />} color="inherit">
      Voltar
    </Button>
  )

  if (idNumerico === undefined) {
    return (
      <>
        <Cabecalho titulo="Novo projeto" subtitulo="Descreva o projeto e o que a equipe precisa ter." acoes={voltar} />
        <Formulario />
      </>
    )
  }
  if (consulta.isError) {
    return (
      <>
        <Cabecalho titulo="Projeto" acoes={voltar} />
        <ErroCarregamento erro={consulta.error} aoTentarDeNovo={() => consulta.refetch()} />
      </>
    )
  }
  if (!consulta.data) {
    return (
      <>
        <Skeleton variant="text" width={320} height={48} />
        <Skeleton variant="rounded" height={360} sx={{ mt: 3 }} />
      </>
    )
  }
  return (
    <>
      <Cabecalho titulo={`Editar ${consulta.data.nome}`} subtitulo="Papéis e tecnologias salvos substituem os anteriores." acoes={voltar} />
      <Formulario key={consulta.data.id} projeto={consulta.data} />
    </>
  )
}
