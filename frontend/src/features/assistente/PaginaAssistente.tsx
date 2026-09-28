import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined'
import RestartAltOutlined from '@mui/icons-material/RestartAltOutlined'
import SendRounded from '@mui/icons-material/SendRounded'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { LIMITE_MENSAGENS, useTurnoConversa } from '../../api/conversa'
import type { AutorMensagem, ParametrosEntendidos, Recomendacao } from '../../api/tipos'
import { Cabecalho } from '../../components/Cabecalho'
import { CartaoRecomendacao, Digitando, Mensagem } from './Mensagem'
import { PainelEntendido } from './PainelEntendido'

interface Fala {
  autor: AutorMensagem
  texto: string
  recomendacao?: Recomendacao
}

const SUGESTOES = [
  'Preciso de uma equipe de 4 pessoas para o Portal do Aluno, um projeto crítico em Java, Spring Boot e React, com 20 horas semanais por integrante. São necessários dois desenvolvedores de backend e um de frontend.',
  'Monte uma equipe de 3 pessoas para o App de Agendamento, criticidade média, 30 horas por semana, com um desenvolvedor mobile e um de QA.',
]

export function PaginaAssistente() {
  const turno = useTurnoConversa()
  const [falas, setFalas] = useState<Fala[]>([])
  const [entendido, setEntendido] = useState<ParametrosEntendidos>()
  const [texto, setTexto] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const fim = useRef<HTMLDivElement>(null)
  const campo = useRef<HTMLTextAreaElement>(null)

  const concluida = falas.some((f) => f.recomendacao)
  const semEspaco = falas.length + 1 > LIMITE_MENSAGENS
  const bloqueado = concluida || semEspaco || turno.isPending

  useEffect(() => {
    fim.current?.scrollIntoView?.({ behavior: 'smooth', block: 'end' })
  }, [falas.length, turno.isPending])

  async function enviar(evento?: FormEvent) {
    evento?.preventDefault()
    const mensagem = texto.trim()
    if (!mensagem || bloqueado) return

    const historico: Fala[] = [...falas, { autor: 'GESTOR', texto: mensagem }]
    setFalas(historico)
    setTexto('')
    setErro(null)
    try {
      const resposta = await turno.mutateAsync(historico.map(({ autor, texto: t }) => ({ autor, texto: t })))
      setEntendido(resposta.entendido)
      setFalas([...historico, { autor: 'ASSISTENTE', texto: resposta.resposta, recomendacao: resposta.recomendacao }])
    } catch (e) {
      // A fala nao entrou na conversa: volta para o campo, para corrigir ou reenviar.
      setFalas(falas)
      setTexto(mensagem)
      setErro((e as Error).message)
    }
  }

  function aoTeclar(evento: KeyboardEvent) {
    if (evento.key === 'Enter' && !evento.shiftKey) {
      evento.preventDefault()
      enviar()
    }
  }

  function recomecar() {
    setFalas([])
    setEntendido(undefined)
    setTexto('')
    setErro(null)
    campo.current?.focus()
  }

  return (
    <>
      <Cabecalho
        titulo="Assistente"
        subtitulo="Descreva em linguagem natural a equipe de que você precisa. O assistente pergunta o que faltar, cria o projeto e gera a recomendação."
        acoes={
          falas.length > 0 && (
            <Button startIcon={<RestartAltOutlined />} onClick={recomecar} color="inherit" disabled={turno.isPending}>
              Nova conversa
            </Button>
          )
        }
      />

      <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 320px' }, alignItems: 'start' }}>
        <Paper sx={{ display: 'flex', flexDirection: 'column', height: { xs: 'auto', lg: 'calc(100vh - 220px)' }, minHeight: 520 }}>
          <Box sx={{ flexGrow: 1, overflowY: 'auto', p: { xs: 2, md: 3 } }}>
            {falas.length === 0 && (
              <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center', py: 4, maxWidth: 560, mx: 'auto' }}>
                <Box
                  aria-hidden
                  sx={(t) => ({
                    width: 56,
                    height: 56,
                    borderRadius: '50%',
                    display: 'grid',
                    placeItems: 'center',
                    color: 'primary.main',
                    bgcolor: `rgba(${t.vars!.palette.primary.mainChannel} / 0.1)`,
                  })}
                >
                  <AutoAwesomeOutlined />
                </Box>
                <Typography variant="h2" component="p">
                  Que equipe você precisa montar?
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Informe ao menos o nome do projeto, o tamanho da equipe e as horas semanais. Papéis, tecnologias e criticidade deixam a
                  recomendação mais precisa.
                </Typography>
                <Stack spacing={1} sx={{ width: '100%', pt: 1 }}>
                  {SUGESTOES.map((sugestao) => (
                    <Button
                      key={sugestao}
                      variant="outlined"
                      color="inherit"
                      aria-label={`Usar sugestão: ${sugestao}`}
                      onClick={() => {
                        setTexto(sugestao)
                        campo.current?.focus()
                      }}
                      sx={{ justifyContent: 'flex-start', textAlign: 'left', fontWeight: 400, borderColor: 'divider', py: 1.25 }}
                    >
                      {sugestao}
                    </Button>
                  ))}
                </Stack>
              </Stack>
            )}
            <Stack component="ol" role="log" aria-label="Conversa" aria-live="polite" spacing={2} sx={{ listStyle: 'none', m: 0, p: 0 }}>
              {falas.map((fala, i) => (
                <Mensagem key={i} autor={fala.autor}>
                  {fala.texto}
                  {fala.recomendacao && <CartaoRecomendacao recomendacao={fala.recomendacao} />}
                </Mensagem>
              ))}
              {turno.isPending && <Digitando />}
            </Stack>
            <div ref={fim} />
          </Box>

          <Box component="form" onSubmit={enviar} sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
            {erro && (
              <Alert severity="error" sx={{ mb: 1.5 }} onClose={() => setErro(null)}>
                {erro}
              </Alert>
            )}
            {concluida && (
              <Alert severity="success" sx={{ mb: 1.5 }}>
                Recomendação gerada. Para pedir outra equipe, comece uma nova conversa.
              </Alert>
            )}
            {!concluida && semEspaco && (
              <Alert severity="info" sx={{ mb: 1.5 }}>
                A conversa atingiu o limite de mensagens. Comece uma nova, descrevendo o pedido completo.
              </Alert>
            )}
            <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-end' }}>
              <TextField
                inputRef={campo}
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                onKeyDown={aoTeclar}
                placeholder="Descreva o projeto e a equipe… (Enter envia, Shift+Enter quebra a linha)"
                multiline
                maxRows={6}
                fullWidth
                disabled={concluida || semEspaco}
                slotProps={{ htmlInput: { 'aria-label': 'Mensagem', maxLength: 2000 } }}
              />
              <IconButton
                type="submit"
                aria-label="Enviar"
                color="primary"
                disabled={bloqueado || !texto.trim()}
                sx={{ bgcolor: 'action.hover', width: 48, height: 48 }}
              >
                <SendRounded />
              </IconButton>
            </Stack>
          </Box>
        </Paper>

        <PainelEntendido entendido={entendido} />
      </Box>
    </>
  )
}
