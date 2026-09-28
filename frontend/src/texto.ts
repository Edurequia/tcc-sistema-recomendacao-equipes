/** Remove acentos e caixa, para buscas tolerantes ("lider" encontra "Liderança"). */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim()
}

/** "1 habilidade", "2 habilidades". */
export function contar(quantidade: number, singular: string, plural: string): string {
  return `${quantidade} ${quantidade === 1 ? singular : plural}`
}

const DECIMAL = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 2 })

/** 4.2 -> "4,2"; 4 -> "4,0". */
export function decimal(valor: number): string {
  return DECIMAL.format(valor)
}

/** "2026-10-01" -> "01/10/2026", sem passar por Date (evita deslocamento de fuso). */
export function data(iso: string): string {
  const [ano, mes, dia] = iso.slice(0, 10).split('-')
  return `${dia}/${mes}/${ano}`
}

/** Periodo previsto de um projeto, tolerando datas ausentes. */
export function periodo(inicio?: string | null, fim?: string | null): string {
  if (inicio && fim) return `${data(inicio)} – ${data(fim)}`
  if (inicio) return `A partir de ${data(inicio)}`
  if (fim) return `Até ${data(fim)}`
  return 'Sem datas'
}

/** "2026-09-21T10:15:00-03:00" -> "21/09/2026 às 10:15", no horario em que o servidor gravou. */
export function dataHora(iso: string): string {
  return `${data(iso)} às ${iso.slice(11, 16)}`
}

/** Numero com casas decimais fixas no formato brasileiro: 83.85 -> "83,9". */
export function numero(valor: number, casas = 1): string {
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas })
}

/** Percentual inteiro. `fracao` indica valor de 0 a 1 (senao, de 0 a 100): 0.7895 -> "79%". */
export function percentual(valor: number, fracao = false): string {
  return `${Math.round(fracao ? valor * 100 : valor)}%`
}
