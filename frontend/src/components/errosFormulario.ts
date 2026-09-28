import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ErroApi } from '../api/cliente'

/**
 * Leva ao formulario os erros devolvidos pelo backend. Erros de validacao (400) vao
 * para os campos que o formulario conhece; o conflito (409) vai para `campoConflito`.
 * Devolve a mensagem que nao coube em nenhum campo, para exibir no topo do formulario.
 */
export function aplicarErrosDoServidor<T extends FieldValues>(
  erro: unknown,
  setError: UseFormSetError<T>,
  camposConhecidos: readonly Path<T>[],
  campoConflito?: Path<T>,
): string | null {
  if (!(erro instanceof ErroApi)) {
    return 'Ocorreu um erro inesperado. Tente novamente.'
  }
  if (erro.status === 409 && campoConflito) {
    setError(campoConflito, { type: 'server', message: erro.message }, { shouldFocus: true })
    return null
  }

  const naoAtribuidos = erro.campos.filter((campo) => {
    const conhecido = camposConhecidos.find((nome) => nome === campo.campo)
    if (conhecido) {
      setError(conhecido, { type: 'server', message: campo.motivo })
    }
    return !conhecido
  })

  if (erro.campos.length > 0 && naoAtribuidos.length === 0) {
    return null
  }
  return [erro.message, ...naoAtribuidos.map((campo) => `${campo.campo}: ${campo.motivo}`)].join(' ')
}
