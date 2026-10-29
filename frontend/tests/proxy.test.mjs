import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { createServer } from 'vite';

test('proxy encaminha cookies, formulário e CSRF para a API e preserva Set-Cookie', async () => {
  const backend = http.createServer(async (request, response) => {
    let body = '';
    for await (const part of request) body += part;
    response.writeHead(200, { 'Content-Type': 'application/json', 'Set-Cookie': 'JSESSIONID=renovada; Path=/; HttpOnly; SameSite=Lax' });
    response.end(JSON.stringify({ url: request.url, cookie: request.headers.cookie, csrf: request.headers['x-csrf-token'], body }));
  });
  backend.listen(0, '127.0.0.1');
  await once(backend, 'listening');
  const previousTarget = process.env.API_PROXY_TARGET;
  process.env.API_PROXY_TARGET = `http://127.0.0.1:${backend.address().port}`;
  let vite;
  try {
    vite = await createServer({ server: { host: '127.0.0.1', port: 0 } });
    await vite.listen();
    const response = await fetch(`http://127.0.0.1:${vite.httpServer.address().port}/api/auth/login`, {
      method: 'POST', headers: { Cookie: 'JSESSIONID=anterior', 'X-CSRF-TOKEN': 'seguro' },
      body: new URLSearchParams({ email: 'ana@example.invalid', senha: 'senha-teste' }),
    });
    assert.match(response.headers.get('set-cookie'), /JSESSIONID=renovada/);
    const data = await response.json();
    assert.equal(data.url, '/api/auth/login');
    assert.equal(data.cookie, 'JSESSIONID=anterior');
    assert.equal(data.csrf, 'seguro');
    assert.equal(new URLSearchParams(data.body).get('email'), 'ana@example.invalid');
  } finally {
    if (previousTarget === undefined) delete process.env.API_PROXY_TARGET;
    else process.env.API_PROXY_TARGET = previousTarget;
    await vite?.close();
    backend.closeAllConnections();
    await new Promise(resolve => backend.close(resolve));
  }
});
