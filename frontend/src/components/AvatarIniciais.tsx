import Avatar from '@mui/material/Avatar'

// Tons com contraste suficiente para texto branco nos dois temas.
const CORES = ['#2563eb', '#7c3aed', '#db2777', '#0891b2', '#059669', '#d97706', '#4f46e5', '#0d9488']

function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}

/** Avatar com as iniciais do nome e uma cor estavel derivada dele. */
export function AvatarIniciais({ nome, tamanho = 36 }: { nome: string; tamanho?: number }) {
  const indice = [...nome].reduce((soma, letra) => soma + letra.charCodeAt(0), 0) % CORES.length
  return (
    <Avatar aria-hidden sx={{ width: tamanho, height: tamanho, bgcolor: CORES[indice], color: '#fff', fontSize: tamanho * 0.38, fontWeight: 700 }}>
      {iniciais(nome)}
    </Avatar>
  )
}
