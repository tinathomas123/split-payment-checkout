import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development, the browser calls /api/... on the Vite server (port 5173), and Vite forwards
// the request to the right Spring Boot service. Because the browser only ever talks to one
// origin, no CORS settings are needed in the services. In Docker and Kubernetes, nginx does
// the same job (see nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/orders': {
        target: 'http://localhost:8081',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api/payments': {
        target: 'http://localhost:8082',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
