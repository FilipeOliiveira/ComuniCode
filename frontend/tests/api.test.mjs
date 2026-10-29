import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi } from '../src/api/client.js';

const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });

test('login usa formulário, cookie e CSRF; logout busca um token novo', async () => {
  const calls = [];
  const replies = [json({ headerName: 'X-CSRF-TOKEN', token: 'antes' }), new Response(null, { status: 204, headers: { 'Content-Type': 'application/json' } }), json({ id: 'aluno', nome: 'Ana' }), json({ headerName: 'X-CSRF-TOKEN', token: 'depois' }), new Response(null, { status: 204 })];
  const api = createApi(async (url, options) => { calls.push({ url, ...options }); return replies.shift(); });
  assert.equal((await api.login(' ana@example.invalid ', 'a&b+ç= ')).id, 'aluno');
  await api.logout();
  assert.deepEqual(calls.map(call => call.url), ['/api/auth/csrf', '/api/auth/login', '/api/auth/me', '/api/auth/csrf', '/api/auth/logout']);
  assert.ok(calls.every(call => call.credentials === 'include' && call.cache === 'no-store'));
  assert.equal(calls[1].headers['Content-Type'], 'application/x-www-form-urlencoded');
  assert.equal(calls[1].headers['X-CSRF-TOKEN'], 'antes');
  assert.equal(calls[1].body.get('senha'), 'a&b+ç= ');
  assert.equal(calls[1].body.get('email'), 'ana@example.invalid');
  assert.equal(calls[4].headers['X-CSRF-TOKEN'], 'depois');
});

test('cadastro envia apenas os campos permitidos, com semestre numérico', async () => {
  let body;
  const api = createApi(async (url, options) => {
    if (url.endsWith('/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'token' });
    assert.equal(options.headers['X-CSRF-TOKEN'], 'token');
    body = JSON.parse(options.body);
    return json({ id: 'novo' }, 201);
  });
  await api.register({ nome: ' Ana ', email: ' ana@example.invalid ', senha: ' senha ', matricula: ' 42 ', semestreAtual: '3', perfil: 'ADMIN', confirmacao: ' senha ' });
  assert.deepEqual(body, { nome: 'Ana', email: 'ana@example.invalid', senha: ' senha ', matricula: '42', semestreAtual: 3 });
});

test('preserva status 401/409 e rejeita HTML, rede indisponível e CSRF inválido', async () => {
  for (const status of [401, 409]) {
    await assert.rejects(createApi(async () => json({ mensagem: 'Erro esperado' }, status)).me(), error => error.status === status && error.message === 'Erro esperado');
  }
  await assert.rejects(createApi(async () => new Response('<html>fallback</html>', { headers: { 'Content-Type': 'text/html' } })).me(), /resposta inválida/);
  await assert.rejects(createApi(async () => { throw new TypeError('fetch failed'); }).me(), /conectar ao servidor/);
  let requests = 0;
  await assert.rejects(createApi(async () => { requests++; return json({}); }).login('a@b.com', 'senha'), /sessão segura/);
  assert.equal(requests, 1);
});

test('não repete automaticamente operações recusadas por CSRF', async () => {
  let requests = 0;
  const api = createApi(async url => {
    requests++;
    return url.endsWith('/csrf') ? json({ headerName: 'X-CSRF-TOKEN', token: 'token' }) : json({ mensagem: 'CSRF inválido' }, 403);
  });
  await assert.rejects(api.login('a@b.com', 'senha'), error => error.status === 403);
  assert.equal(requests, 2);
});
