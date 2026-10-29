import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxy = { '/api': { target: env.API_PROXY_TARGET || 'http://127.0.0.1:8081' } };
  // Mesma origem para a interface, o cookie de sessão e o token CSRF.
  return { server: { proxy }, preview: { proxy } };
});
