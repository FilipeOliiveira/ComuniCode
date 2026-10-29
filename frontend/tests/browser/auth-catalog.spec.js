import { test, expect } from '@playwright/test';

const user = { id: 'aluno1', nome: 'Ana Silva', email: 'ana@example.invalid', perfil: 'ALUNO', ativo: true };
const posts = [
  { id: 'p1', titulo: 'Árvores em Java', descricao: 'Algoritmos e coleções', autorId: 'prof1', autorNome: 'Geraldo Braz', autorPerfil: 'PROFESSOR', disciplinaId: 'd1', disciplinaNome: 'Programação', conteudoTitulo: 'Coleções', status: 'PUBLICADA', dataCriacao: '2026-10-29T10:00:00', verificada: false, tags: [{ nome: 'Java' }], anexos: [
    { id: 'a1', nome: 'Resumo.pdf', tipo: 'PDF', url: 'https://example.invalid/resumo.pdf', tamanho: 1024 },
    { id: 'a2', nome: 'Aula.mp4', tipo: 'VIDEO', url: 'https://example.invalid/aula.mp4', tamanho: 2048 },
  ] },
  { id: 'p2', titulo: 'Meu resumo de cálculo', descricao: 'Derivadas', autorId: user.id, autorNome: user.nome, autorPerfil: 'ALUNO', disciplinaId: 'd2', disciplinaNome: 'Cálculo', conteudoTitulo: 'Derivadas', status: 'RASCUNHO', dataCriacao: '2026-10-29T11:00:00', verificada: false, tags: [], anexos: [] },
];

async function mockApi(page, authenticated = false) {
  const state = { authenticated, token: 'antes', posts, unavailable: false, catalogError: false, logoutError: false, registration: null };
  if (authenticated) await page.context().addCookies([{ name: 'JSESSIONID', value: 'logged', url: 'http://127.0.0.1:4173', httpOnly: true, sameSite: 'Lax' }]);
  await page.route(/https:\/\/fonts\.(googleapis|gstatic)\.com\//, route => route.abort());
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const headers = await request.allHeaders();
    const reply = (status, data, extra = {}) => route.fulfill({ status, headers: { 'content-type': 'application/json', ...extra }, body: status === 204 ? '' : JSON.stringify(data) });
    const hasSession = state.authenticated && headers.cookie?.includes('JSESSIONID=logged');
    if (state.unavailable) return reply(503, {});
    if (path === '/api/auth/csrf') return reply(200, { headerName: 'X-CSRF-TOKEN', token: state.token });
    if (request.method() === 'POST' && headers['x-csrf-token'] !== state.token) return reply(403, { mensagem: 'CSRF inválido' });
    if (path === '/api/auth/login') {
      const data = new URLSearchParams(request.postData());
      if (data.get('email') !== user.email || data.get('senha') !== 'senha-teste') return reply(401, { mensagem: 'Credenciais invalidas.' });
      expect(headers['content-type']).toContain('application/x-www-form-urlencoded');
      state.authenticated = true;
      state.token = 'depois';
      return reply(204, null, { 'set-cookie': 'JSESSIONID=logged; Path=/; HttpOnly; SameSite=Lax' });
    }
    if (path === '/api/auth/cadastro/aluno') {
      const data = request.postDataJSON();
      if (data.email === 'existente@example.invalid') return reply(409, { mensagem: 'Ja existe um usuario cadastrado com este e-mail.' });
      state.registration = data;
      return reply(201, user);
    }
    if (path === '/api/auth/logout') {
      if (state.logoutError) return reply(503, {});
      state.authenticated = false;
      state.token = 'apos-logout';
      return reply(204, null, { 'set-cookie': 'JSESSIONID=; Path=/; Max-Age=0' });
    }
    if (!hasSession) return reply(401, { mensagem: 'Autenticacao necessaria.' });
    if (path === '/api/auth/me') return reply(200, user);
    if (state.catalogError) return reply(503, {});
    if (path === '/api/disciplinas') return reply(200, [{ id: 'd1', nome: 'Programação' }, { id: 'd2', nome: 'Cálculo' }]);
    if (path === '/api/postagens') return reply(200, state.posts);
    return reply(404, {});
  });
  return state;
}

async function login(page, password = 'senha-teste') {
  await page.getByLabel('E-mail', { exact: true }).fill(user.email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
}

test('login, filtros combinados, meus materiais, recarga e logout com cookie/CSRF', async ({ page }) => {
  await mockApi(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
  await login(page, 'incorreta');
  await expect(page.getByRole('alert')).toContainText('Credenciais invalidas');
  await login(page);
  await expect(page.getByRole('heading', { name: /Olá, Ana/ })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(2);
  await page.screenshot({ path: 'test-results/catalog-desktop.png', fullPage: true });
  await expect(page.getByRole('link', { name: 'Abrir Resumo.pdf (nova aba)', exact: true })).toHaveAttribute('href', 'https://example.invalid/resumo.pdf');
  await expect(page.getByText('Rascunho', { exact: true })).toBeVisible();
  await page.getByLabel('Pesquisar materiais').fill('arvores programacao');
  await page.getByRole('combobox', { name: 'Professor', exact: true }).selectOption('prof1');
  await page.getByRole('combobox', { name: 'Disciplina', exact: true }).selectOption('d1');
  await page.getByRole('button', { name: 'Vídeos', exact: true }).click();
  await expect(page.getByRole('article')).toHaveCount(1);
  await page.getByRole('button', { name: 'Word', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Nenhum material encontrado' })).toBeVisible();
  await page.getByRole('link', { name: 'Meus materiais', exact: true }).click();
  await expect(page.getByRole('article')).toHaveCount(1);
  await expect(page.getByRole('article')).toContainText('Meu resumo de cálculo');
  await page.reload();
  await expect(page.getByRole('article')).toHaveCount(2);
  await page.getByRole('button', { name: 'Sair da conta' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  expect((await page.context().cookies()).some(cookie => cookie.name === 'JSESSIONID')).toBe(false);
});

test('cadastro valida confirmação, informa conflito e retorna ao login', async ({ page }) => {
  const state = await mockApi(page);
  await page.goto('/');
  await page.getByRole('button', { name: /Ainda não tenho conta/ }).click();
  await page.getByLabel('Nome completo').fill(user.nome);
  await page.getByLabel('E-mail', { exact: true }).fill('existente@example.invalid');
  await page.getByLabel('Matrícula').fill('20261234');
  await page.getByLabel('Semestre atual').fill('3');
  await page.getByLabel('Senha', { exact: true }).fill('senha-teste');
  await page.getByLabel('Confirmar senha').fill('diferente');
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click();
  await expect(page.getByRole('alert')).toHaveText('As senhas não coincidem.');
  expect(state.registration).toBe(null);
  await page.getByLabel('Confirmar senha').fill('senha-teste');
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Ja existe');
  await page.getByLabel('E-mail', { exact: true }).fill(user.email);
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await expect(page.getByRole('status')).toContainText('Cadastro concluído');
  expect(state.registration).toEqual({ nome: user.nome, email: user.email, matricula: '20261234', semestreAtual: 3, senha: 'senha-teste' });
  await expect(page.getByLabel('Senha', { exact: true })).toHaveValue('');
  await login(page);
  await expect(page.getByRole('article')).toHaveCount(2);
});

test('sessão expirada limpa o acervo e solicita novo login', async ({ page }) => {
  const state = await mockApi(page, true);
  await page.goto('/');
  await expect(page.getByRole('article')).toHaveCount(2);
  state.authenticated = false;
  await page.getByRole('button', { name: 'Atualizar acervo' }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await expect(page.getByRole('status')).toContainText('Sua sessão expirou');
  await expect(page.getByRole('article')).toHaveCount(0);
});

test('diferencia servidor indisponível, falha de listagem e acervo vazio', async ({ page }) => {
  const state = await mockApi(page, true);
  state.unavailable = true;
  await page.goto('/');
  await expect(page.getByRole('alert')).toBeVisible();
  state.unavailable = false;
  state.catalogError = true;
  await page.getByRole('button', { name: 'Tentar novamente' }).click();
  await expect(page.getByRole('heading', { name: /Olá, Ana/ })).toBeVisible();
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByText('O acervo ainda está vazio')).toHaveCount(0);
  state.catalogError = false;
  state.posts = [];
  await page.getByRole('button', { name: 'Tentar novamente' }).click();
  await expect(page.getByRole('heading', { name: 'O acervo ainda está vazio' })).toBeVisible();
  state.logoutError = true;
  await page.getByRole('button', { name: 'Sair da conta' }).click();
  await expect(page.getByRole('alert')).toContainText('Não foi possível sair');
  await expect(page.getByRole('heading', { name: /Olá, Ana/ })).toBeVisible();
});

test('login e catálogo cabem na tela de celular', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockApi(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: 'test-results/login-mobile.png', fullPage: true });
  await login(page);
  await expect(page.getByRole('article')).toHaveCount(2);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: 'test-results/catalog-mobile.png', fullPage: true });
});
