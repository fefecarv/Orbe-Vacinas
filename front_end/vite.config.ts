import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  server: {
    allowedHosts: [
      'lending-strengthening-creator-finding.trycloudflare.com',
      'coleman-cabinets-advice-deposit.trycloudflare.com',
    ],
    proxy: {
      '/orbe-backend': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  plugins: [svelte()],
});
