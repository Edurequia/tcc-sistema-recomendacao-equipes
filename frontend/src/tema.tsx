import '@fontsource-variable/plus-jakarta-sans'
import CssBaseline from '@mui/material/CssBaseline'
import { createTheme, ThemeProvider } from '@mui/material/styles'
import type { ReactNode } from 'react'

// Paleta neutra (slate) com azul de destaque; o modo claro/escuro segue o sistema
// ate o usuario escolher outro no seletor do menu, e a escolha fica salva no navegador.
const FONTE = "'Plus Jakarta Sans Variable', system-ui, -apple-system, 'Segoe UI', sans-serif"

const tema = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#2563eb', dark: '#1d4ed8', light: '#60a5fa' },
        secondary: { main: '#7c3aed' },
        success: { main: '#15803d' },
        warning: { main: '#b45309' },
        error: { main: '#dc2626' },
        background: { default: '#f8fafc', paper: '#ffffff' },
        text: { primary: '#0f172a', secondary: '#475569' },
        divider: '#e2e8f0',
      },
    },
    dark: {
      palette: {
        primary: { main: '#60a5fa', dark: '#3b82f6', light: '#93c5fd' },
        secondary: { main: '#a78bfa' },
        success: { main: '#4ade80' },
        warning: { main: '#fbbf24' },
        error: { main: '#f87171' },
        background: { default: '#0b1120', paper: '#111827' },
        text: { primary: '#e2e8f0', secondary: '#94a3b8' },
        divider: 'rgba(148, 163, 184, 0.16)',
      },
    },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: FONTE,
    h1: { fontSize: '1.75rem', fontWeight: 700, letterSpacing: '-0.02em', lineHeight: 1.25 },
    h2: { fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.01em' },
    h3: { fontSize: '1.05rem', fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
    overline: { fontWeight: 700, letterSpacing: '0.08em', lineHeight: 2 },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { WebkitFontSmoothing: 'antialiased', MozOsxFontSmoothing: 'grayscale' },
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': { transitionDuration: '0.01ms !important', animationDuration: '0.01ms !important' },
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 10, paddingInline: 16 } },
    },
    MuiPaper: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: { root: { backgroundImage: 'none' } },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: 8,
          transition: theme.transitions.create(['background-color', 'color'], { duration: 150 }),
          '&.Mui-focusVisible': { outline: `2px solid ${theme.vars!.palette.primary.main}`, outlineOffset: 2 },
        }),
      },
    },
    MuiToggleButton: {
      styleOverrides: { root: ({ theme }) => ({ textTransform: 'none', borderRadius: 8, color: theme.vars!.palette.text.secondary }) },
    },
  },
})

export function TemaProvider({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider theme={tema} defaultMode="system">
      <CssBaseline />
      {children}
    </ThemeProvider>
  )
}
