// Espelho dos DTOs e enums do backend (br.ufn.tfg.teamrec.dto e domain.enums).
// Datas chegam como string ISO; valores BigDecimal chegam como number.

export type Senioridade = 'JUNIOR' | 'PLENO' | 'SENIOR'
export type PapelAtuacao = 'BACKEND' | 'FRONTEND' | 'FULLSTACK' | 'MOBILE' | 'QA' | 'DEVOPS'
export type CriticidadeProjeto = 'BAIXA' | 'MEDIA' | 'ALTA'
export type StatusProjeto = 'PLANEJAMENTO' | 'EM_ANDAMENTO' | 'CONCLUIDO' | 'CANCELADO'
export type TipoHabilidade = 'HARD' | 'SOFT'
export type CategoriaHabilidade =
  | 'LINGUAGEM'
  | 'FRAMEWORK'
  | 'BANCO_DE_DADOS'
  | 'CLOUD'
  | 'DEVOPS'
  | 'MOBILE'
  | 'COMPORTAMENTAL'
  | 'OUTRA'
export type OrigemRecomendacao = 'FORMULARIO' | 'CHAT'
export type AutorMensagem = 'GESTOR' | 'ASSISTENTE'

// --- Erros -------------------------------------------------------------------

export interface CampoInvalido {
  campo: string
  motivo: string
}

export interface ApiErrorBody {
  timestamp?: string
  status: number
  erro?: string
  mensagem: string
  caminho?: string
  campos?: CampoInvalido[]
}

export interface Pagina<T> {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
  primeira: boolean
  ultima: boolean
}

// --- Habilidades -------------------------------------------------------------

export interface HabilidadeRequest {
  nome: string
  tipo: TipoHabilidade
  categoria: CategoriaHabilidade
  chaveDataset?: string | null
}

export interface Habilidade {
  id: number
  nome: string
  tipo: TipoHabilidade
  tipoDescricao: string
  categoria: CategoriaHabilidade
  categoriaDescricao: string
  chaveDataset?: string
  usadaNoModelo: boolean
  ativa: boolean
}

// --- Desenvolvedores ---------------------------------------------------------

export interface VinculoHabilidadeRequest {
  habilidadeId: number
  proficiencia: number
}

export interface DesenvolvedorRequest {
  nome: string
  email: string
  senioridade: Senioridade
  papelPrincipal: PapelAtuacao
  softSkillsScore: number
  horasDisponiveis: number
  habilidades: VinculoHabilidadeRequest[]
}

export interface DesenvolvedorResumo {
  id: number
  nome: string
  email: string
  senioridade: Senioridade
  senioridadeDescricao: string
  papelPrincipal: PapelAtuacao
  papelPrincipalDescricao: string
  softSkillsScore: number
  horasDisponiveis: number
  ativo: boolean
}

export interface VinculoHabilidade {
  habilidadeId: number
  nome: string
  tipo: TipoHabilidade
  categoria: CategoriaHabilidade
  proficiencia: number
}

export interface Desenvolvedor extends DesenvolvedorResumo {
  habilidades: VinculoHabilidade[]
}

// --- Projetos ----------------------------------------------------------------

export interface PapelRequeridoRequest {
  papel: PapelAtuacao
  quantidade: number
}

export interface HabilidadeRequeridaRequest {
  habilidadeId: number
  obrigatoria: boolean
  peso: number
}

export interface ProjetoRequest {
  nome: string
  descricao?: string | null
  criticidade: CriticidadeProjeto
  tamanhoEquipe: number
  horasSemanaisPorIntegrante: number
  dataInicioPrevista?: string | null
  dataFimPrevista?: string | null
  papeisRequeridos: PapelRequeridoRequest[]
  habilidadesRequeridas: HabilidadeRequeridaRequest[]
}

export interface ProjetoResumo {
  id: number
  nome: string
  criticidade: CriticidadeProjeto
  criticidadeDescricao: string
  status: StatusProjeto
  statusDescricao: string
  tamanhoEquipe: number
  horasSemanaisPorIntegrante: number
  dataInicioPrevista?: string
  dataFimPrevista?: string
}

export interface PapelRequerido {
  papel: PapelAtuacao
  papelDescricao: string
  quantidade: number
}

export interface HabilidadeRequerida {
  habilidadeId: number
  nome: string
  categoria: CategoriaHabilidade
  obrigatoria: boolean
  peso: number
  usadaNoModelo: boolean
}

export interface Projeto extends ProjetoResumo {
  descricao?: string
  criticidadeNivel: number
  vagasDeclaradas: number
  vagasSemPapelFixo: number
  papeisRequeridos: PapelRequerido[]
  habilidadesRequeridas: HabilidadeRequerida[]
}

// --- Alocacoes ---------------------------------------------------------------

export interface AlocacaoRequest {
  desenvolvedorId: number
  papel?: PapelAtuacao
  horasSemanais?: number
  dataInicio?: string
}

export interface Alocacao {
  id: number
  desenvolvedorId: number
  desenvolvedorNome: string
  projetoId: number
  projetoNome: string
  papel: PapelAtuacao
  papelDescricao: string
  horasSemanais: number
  dataInicio: string
  dataFim?: string
  ativa: boolean
}

// --- Recomendacoes -----------------------------------------------------------

export interface MetricasEquipe {
  coberturaPapeis: number
  coberturaTecnologias: number
  desvioPadraoSenioridade: number
  desvioPadraoScore: number
  scoreMedioEquipe: number
}

export interface MembroRanking {
  posicao: number
  desenvolvedorId: number
  nome: string
  senioridade: Senioridade
  senioridadeDescricao: string
  papel: PapelAtuacao
  papelDescricao: string
  scoreAfinidade: number
  aderenciaTecnica: number
  coberturaHoras: number
  elegivel: boolean
  selecionado: boolean
}

export interface Recomendacao {
  id: number
  projetoId: number
  projetoNome: string
  origem: OrigemRecomendacao
  origemDescricao: string
  criadoEm: string
  tamanhoEquipe: number
  totalCandidatos: number
  integrantesSelecionados: number
  metricas: MetricasEquipe
  observacaoRestricao?: string
  justificativaLlm?: string
  /** Por que a justificativa nao veio junto; so chega na resposta da geracao. */
  avisoJustificativa?: string
  ranking: MembroRanking[]
}

export interface RecomendacaoResumo {
  id: number
  projetoId: number
  origem: OrigemRecomendacao
  origemDescricao: string
  criadoEm: string
  coberturaPapeis: number
  coberturaTecnologias: number
  scoreMedioEquipe: number
  observacaoRestricao?: string
}

export interface LinhaBase {
  recomendacaoId: number
  projetoId: number
  sorteios: number
  semente: number
  candidatosElegiveis: number
  tamanhoEquipeSorteada: number
  prototipo: MetricasEquipe
  linhaBase: { media: MetricasEquipe; desvioPadrao: MetricasEquipe }
}

export interface Justificativa {
  recomendacaoId: number
  justificativa: string
  modelo: string
  geradaAgora: boolean
}

// --- Conversa ----------------------------------------------------------------

export interface MensagemConversa {
  autor: AutorMensagem
  texto: string
}

export interface ParametrosEntendidos {
  nome?: string
  criticidade?: string
  tamanhoEquipe?: number
  horasSemanaisPorIntegrante?: number
  papeis?: string[]
  tecnologias?: string[]
}

export interface RespostaConversa {
  status: 'PERGUNTA' | 'RECOMENDACAO'
  resposta: string
  faltantes?: string[]
  entendido?: ParametrosEntendidos
  recomendacao?: Recomendacao
}
