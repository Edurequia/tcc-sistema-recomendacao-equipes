/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// O backend e o unico servico que o navegador acessa. Em desenvolvimento, o Vite
// repassa /api para o Spring Boot, e o frontend sempre usa caminhos relativos.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // Formularios longos com user-event e MUI passam de 5 s no jsdom quando a suite roda inteira.
    testTimeout: 15_000,
  },
})
