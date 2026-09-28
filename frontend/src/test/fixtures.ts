import type {
  Alocacao,
  Desenvolvedor,
  DesenvolvedorResumo,
  Habilidade,
  LinhaBase,
  MembroRanking,
  Pagina,
  Projeto,
  ProjetoResumo,
  Recomendacao,
  RecomendacaoResumo,
} from '../api/tipos'

export function umaHabilidade(dados: Partial<Habilidade> = {}): Habilidade {
  return {
    id: 1,
    nome: 'Java',
    tipo: 'HARD',
    tipoDescricao: 'Competencia tecnica',
    categoria: 'LINGUAGEM',
    categoriaDescricao: 'Linguagem de programacao',
    chaveDataset: 'Java',
    usadaNoModelo: true,
    ativa: true,
    ...dados,
  }
}

export function umaPagina<T>(conteudo: T[], dados: Partial<Pagina<T>> = {}): Pagina<T> {
  return {
    conteudo,
    pagina: 0,
    tamanho: 20,
    totalElementos: conteudo.length,
    totalPaginas: conteudo.length ? 1 : 0,
    primeira: true,
    ultima: true,
    ...dados,
  }
}

export function umDesenvolvedorResumo(dados: Partial<DesenvolvedorResumo> = {}): DesenvolvedorResumo {
  return {
    id: 1,
    nome: 'Ana Souza',
    email: 'ana.souza@ufn.edu.br',
    senioridade: 'SENIOR',
    senioridadeDescricao: 'Senior',
    papelPrincipal: 'BACKEND',
    papelPrincipalDescricao: 'Backend',
    softSkillsScore: 4.2,
    horasDisponiveis: 30,
    ativo: true,
    ...dados,
  }
}

export function umDesenvolvedor(dados: Partial<Desenvolvedor> = {}): Desenvolvedor {
  return {
    ...umDesenvolvedorResumo(),
    habilidades: [{ habilidadeId: 1, nome: 'Java', tipo: 'HARD', categoria: 'LINGUAGEM', proficiencia: 5 }],
    ...dados,
  }
}

export function umProjetoResumo(dados: Partial<ProjetoResumo> = {}): ProjetoResumo {
  return {
    id: 1,
    nome: 'Portal do Aluno',
    criticidade: 'ALTA',
    criticidadeDescricao: 'Projeto complexo e de alto risco',
    status: 'PLANEJAMENTO',
    statusDescricao: 'Em planejamento',
    tamanhoEquipe: 4,
    horasSemanaisPorIntegrante: 20,
    dataInicioPrevista: '2026-10-01',
    dataFimPrevista: '2027-03-31',
    ...dados,
  }
}

export function umProjeto(dados: Partial<Projeto> = {}): Projeto {
  return {
    ...umProjetoResumo(),
    descricao: 'Reescrita do portal do aluno.',
    criticidadeNivel: 3,
    vagasDeclaradas: 3,
    vagasSemPapelFixo: 1,
    papeisRequeridos: [
      { papel: 'BACKEND', papelDescricao: 'Backend', quantidade: 2 },
      { papel: 'FRONTEND', papelDescricao: 'Frontend', quantidade: 1 },
    ],
    habilidadesRequeridas: [
      { habilidadeId: 1, nome: 'Java', categoria: 'LINGUAGEM', obrigatoria: true, peso: 5, usadaNoModelo: true },
      { habilidadeId: 2, nome: 'React', categoria: 'FRAMEWORK', obrigatoria: false, peso: 3, usadaNoModelo: true },
    ],
    ...dados,
  }
}

export function umaAlocacao(dados: Partial<Alocacao> = {}): Alocacao {
  return {
    id: 10,
    desenvolvedorId: 1,
    desenvolvedorNome: 'Ana Souza',
    projetoId: 1,
    projetoNome: 'Portal do Aluno',
    papel: 'BACKEND',
    papelDescricao: 'Backend',
    horasSemanais: 20,
    dataInicio: '2026-10-01',
    ativa: true,
    ...dados,
  }
}

export function umMembro(dados: Partial<MembroRanking> = {}): MembroRanking {
  return {
    posicao: 1,
    desenvolvedorId: 1,
    nome: 'Ana Souza',
    senioridade: 'SENIOR',
    senioridadeDescricao: 'Senior',
    papel: 'BACKEND',
    papelDescricao: 'Backend',
    scoreAfinidade: 88.058,
    aderenciaTecnica: 0.7895,
    coberturaHoras: 1,
    elegivel: true,
    selecionado: true,
    ...dados,
  }
}

export function umaRecomendacao(dados: Partial<Recomendacao> = {}): Recomendacao {
  return {
    id: 7,
    projetoId: 1,
    projetoNome: 'Portal do Aluno',
    origem: 'FORMULARIO',
    origemDescricao: 'Via de fluxo estruturado (formularios)',
    criadoEm: '2026-09-20T20:54:41.273-03:00',
    tamanhoEquipe: 3,
    totalCandidatos: 4,
    integrantesSelecionados: 2,
    metricas: { coberturaPapeis: 66.667, coberturaTecnologias: 100, desvioPadraoSenioridade: 0.5, desvioPadraoScore: 4.21, scoreMedioEquipe: 83.85 },
    observacaoRestricao: 'Equipe parcial: 2 de 3 integrantes. 1 candidato(s) sem as 20 horas semanais livres exigidas.',
    ranking: [
      umMembro(),
      umMembro({ posicao: 2, desenvolvedorId: 2, nome: 'Bruno Lima', papel: 'FRONTEND', papelDescricao: 'Frontend', senioridade: 'PLENO', scoreAfinidade: 79.642, aderenciaTecnica: 0.4211 }),
      umMembro({ posicao: 3, desenvolvedorId: 3, nome: 'Carla Dias', scoreAfinidade: 75.1, coberturaHoras: 0.5, elegivel: false, selecionado: false }),
      umMembro({ posicao: 4, desenvolvedorId: 4, nome: 'Davi Rocha', papel: 'QA', papelDescricao: 'QA / Testes', scoreAfinidade: 60.2, selecionado: false }),
    ],
    ...dados,
  }
}

export function umaRecomendacaoResumo(dados: Partial<RecomendacaoResumo> = {}): RecomendacaoResumo {
  return {
    id: 7,
    projetoId: 1,
    origem: 'FORMULARIO',
    origemDescricao: 'Via de fluxo estruturado (formularios)',
    criadoEm: '2026-09-20T20:54:41.273-03:00',
    coberturaPapeis: 66.667,
    coberturaTecnologias: 100,
    scoreMedioEquipe: 83.85,
    ...dados,
  }
}

export function umaLinhaBase(dados: Partial<LinhaBase> = {}): LinhaBase {
  return {
    recomendacaoId: 7,
    projetoId: 1,
    sorteios: 100,
    semente: 42,
    candidatosElegiveis: 3,
    tamanhoEquipeSorteada: 3,
    prototipo: { coberturaPapeis: 66.667, coberturaTecnologias: 100, desvioPadraoSenioridade: 0.5, desvioPadraoScore: 4.21, scoreMedioEquipe: 83.85 },
    linhaBase: {
      media: { coberturaPapeis: 41.2, coberturaTecnologias: 88.5, desvioPadraoSenioridade: 0.61, desvioPadraoScore: 9.8, scoreMedioEquipe: 74.3 },
      desvioPadrao: { coberturaPapeis: 12.5, coberturaTecnologias: 9.1, desvioPadraoSenioridade: 0.2, desvioPadraoScore: 3.3, scoreMedioEquipe: 4.4 },
    },
    ...dados,
  }
}
