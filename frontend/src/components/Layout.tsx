import ChevronRight from '@mui/icons-material/ChevronRight'
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined'
import Groups2Outlined from '@mui/icons-material/Groups2Outlined'
import LightModeOutlined from '@mui/icons-material/LightModeOutlined'
import MenuIcon from '@mui/icons-material/Menu'
import SettingsBrightnessOutlined from '@mui/icons-material/SettingsBrightnessOutlined'
import Box from '@mui/material/Box'
import Drawer from '@mui/material/Drawer'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import { useColorScheme, useTheme } from '@mui/material/styles'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import useMediaQuery from '@mui/material/useMediaQuery'
import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import { areaDoCaminho, SECOES_MENU } from './areas'

const LARGURA_MENU = 256
const ALTURA_TOPO = 64

function Marca() {
  return (
    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
      <Box
        aria-hidden
        sx={{
          width: 36,
          height: 36,
          borderRadius: 2.5,
          display: 'grid',
          placeItems: 'center',
          color: '#fff',
          background: 'linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)',
          boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
        }}
      >
        <Groups2Outlined fontSize="small" />
      </Box>
      <Box>
        <Typography sx={{ fontWeight: 800, fontSize: '1.05rem', letterSpacing: '-0.02em', lineHeight: 1.2 }}>TeamRec</Typography>
        <Typography variant="caption" color="textSecondary" sx={{ lineHeight: 1.2, display: 'block' }}>
          Formação de equipes
        </Typography>
      </Box>
    </Stack>
  )
}

function SeletorTema() {
  const { mode, setMode } = useColorScheme()
  const opcoes = [
    { valor: 'light', rotulo: 'Tema claro', icone: <LightModeOutlined fontSize="small" /> },
    { valor: 'dark', rotulo: 'Tema escuro', icone: <DarkModeOutlined fontSize="small" /> },
    { valor: 'system', rotulo: 'Tema do sistema', icone: <SettingsBrightnessOutlined fontSize="small" /> },
  ] as const

  return (
    <ToggleButtonGroup
      aria-label="Tema"
      size="small"
      fullWidth
      exclusive
      value={mode ?? 'system'}
      onChange={(_, valor: 'light' | 'dark' | 'system' | null) => valor && setMode(valor)}
    >
      {opcoes.map((opcao) => (
        <Tooltip key={opcao.valor} title={opcao.rotulo}>
          <ToggleButton value={opcao.valor} aria-label={opcao.rotulo}>
            {opcao.icone}
          </ToggleButton>
        </Tooltip>
      ))}
    </ToggleButtonGroup>
  )
}

function ConteudoMenu({ aoNavegar }: { aoNavegar?: () => void }) {
  return (
    <Stack sx={{ height: '100%' }}>
      <Box sx={{ px: 2.5, height: ALTURA_TOPO, display: 'flex', alignItems: 'center' }}>
        <Marca />
      </Box>

      <Box component="nav" aria-label="Menu principal" sx={{ flexGrow: 1, overflowY: 'auto', px: 1.5, pt: 1 }}>
        {SECOES_MENU.map((secao) => (
          <Box key={secao.titulo} sx={{ mb: 2 }}>
            <Typography component="p" variant="overline" color="textSecondary" sx={{ px: 1.5, fontSize: '0.68rem' }}>
              {secao.titulo}
            </Typography>
            <List disablePadding>
              {secao.areas.map((area) => (
                <ListItemButton
                  key={area.caminho}
                  component={NavLink}
                  to={area.caminho}
                  onClick={aoNavegar}
                  sx={(t) => ({
                    mb: 0.5,
                    py: 1,
                    color: 'text.secondary',
                    '& .MuiListItemIcon-root': { color: 'inherit', minWidth: 36 },
                    '&:hover': { bgcolor: 'action.hover', color: 'text.primary' },
                    '&.active': {
                      bgcolor: `rgba(${t.vars!.palette.primary.mainChannel} / 0.1)`,
                      color: 'primary.main',
                    },
                  })}
                >
                  <ListItemIcon>{area.icone}</ListItemIcon>
                  <ListItemText primary={area.rotulo} slotProps={{ primary: { sx: { fontWeight: 600, fontSize: '0.9rem' } } }} />
                </ListItemButton>
              ))}
            </List>
          </Box>
        ))}
      </Box>

      <Stack spacing={1.5} sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
        <SeletorTema />
        <Typography variant="caption" color="textSecondary" sx={{ textAlign: 'center' }}>
          TFG · Sistemas de Informação · UFN
        </Typography>
      </Stack>
    </Stack>
  )
}

function BarraSuperior({ telaPequena, aoAbrirMenu }: { telaPequena: boolean; aoAbrirMenu: () => void }) {
  const area = areaDoCaminho(useLocation().pathname)

  return (
    <Box
      component="header"
      sx={(t) => ({
        position: 'sticky',
        top: 0,
        zIndex: t.zIndex.appBar,
        height: ALTURA_TOPO,
        px: { xs: 2, md: 4 },
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: `rgba(${t.vars!.palette.background.defaultChannel} / 0.8)`,
        backdropFilter: 'blur(12px)',
      })}
    >
      {telaPequena && (
        <IconButton edge="start" aria-label="Abrir menu" onClick={aoAbrirMenu}>
          <MenuIcon />
        </IconButton>
      )}
      <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', color: 'text.secondary' }}>
        <Typography variant="body2">TeamRec</Typography>
        {area && (
          <>
            <ChevronRight fontSize="small" aria-hidden />
            <Typography variant="body2" color="textPrimary" sx={{ fontWeight: 600 }}>
              {area.rotulo}
            </Typography>
          </>
        )}
      </Stack>
    </Box>
  )
}

export function Layout() {
  const tema = useTheme()
  const telaPequena = useMediaQuery(tema.breakpoints.down('md'))
  const [menuAberto, setMenuAberto] = useState(false)

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Drawer
        variant={telaPequena ? 'temporary' : 'permanent'}
        open={telaPequena ? menuAberto : true}
        onClose={() => setMenuAberto(false)}
        sx={{
          width: { md: LARGURA_MENU },
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: LARGURA_MENU, boxSizing: 'border-box', borderColor: 'divider', bgcolor: 'background.paper' },
        }}
      >
        <ConteudoMenu aoNavegar={telaPequena ? () => setMenuAberto(false) : undefined} />
      </Drawer>

      <Box sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <BarraSuperior telaPequena={telaPequena} aoAbrirMenu={() => setMenuAberto(true)} />
        <Box component="main" sx={{ flexGrow: 1, width: '100%', maxWidth: 1240, mx: 'auto', px: { xs: 2, md: 4 }, py: { xs: 3, md: 4 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
