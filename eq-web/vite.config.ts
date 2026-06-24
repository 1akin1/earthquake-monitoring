import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies /api to the standalone monolith (default :8081) so the SPA
// can talk to the backend without CORS friction during local development.
// In production set VITE_API_BASE to the deployed API origin instead.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY || 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
