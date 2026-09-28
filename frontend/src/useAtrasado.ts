import { useEffect, useState } from 'react'

/** Devolve `valor` so depois de `ms` sem mudancas; evita uma consulta por tecla digitada. */
export function useAtrasado<T>(valor: T, ms = 300): T {
  const [atrasado, setAtrasado] = useState(valor)
  useEffect(() => {
    const temporizador = setTimeout(() => setAtrasado(valor), ms)
    return () => clearTimeout(temporizador)
  }, [valor, ms])
  return atrasado
}
