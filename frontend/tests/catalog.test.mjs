import test from 'node:test';
import assert from 'node:assert/strict';
import { materialFromApi, materialDateLabel, filterCatalog, loadCatalog, safeFileUrl } from '../src/data/catalog.js';

const post = { id: 'p1', titulo: 'Árvores em Java', descricao: 'Estruturas de dados', autorId: 'prof1', autorNome: 'Geraldo', autorPerfil: 'PROFESSOR', disciplinaId: 'd1', disciplinaNome: 'Programação', conteudoTitulo: 'Coleções', dataCriacao: '2026-10-29T10:00:00', status: 'PUBLICADA', verificada: false, tags: [{ nome: 'algoritmos' }], anexos: [
  { id: 'a1', nome: 'Resumo.pdf', url: 'https://example.invalid/resumo.pdf', tipo: 'PDF', tamanho: 2000 },
  { id: 'a2', nome: 'Aula.mp4', url: 'https://example.invalid/aula.mp4', tipo: 'VIDEO', tamanho: 4000 },
] };

test('combina filtros reais, pesquisa sem acento e todos os anexos da postagem', () => {
  const materials = [materialFromApi(post), materialFromApi({ ...post, id: 'p2', autorId: 'aluno1', autorPerfil: 'ALUNO', anexos: [] })];
  assert.equal(filterCatalog(materials, { discipline: 'd1', professor: 'prof1', format: 3, search: 'arvores programacao algoritmos' }).length, 1);
  assert.equal(filterCatalog(materials, { discipline: 'outra' }).length, 0);
  assert.equal(filterCatalog(materials, { format: 2 }).length, 0);
  assert.equal(filterCatalog(materials, { ownerId: 'aluno1' })[0].id, 'p2');
  assert.equal(materials[0].verified, false);
  assert.equal(materials[0].attachments.length, 2);
});

test('usa apenas URLs HTTP/HTTPS sem credenciais nos links de arquivos', () => {
  for (const value of ['javascript:alert(1)', 'data:text/html,oi', '//example.invalid/a', 'https://user:senha@example.invalid/a', null, '']) assert.equal(safeFileUrl(value), null);
  assert.equal(safeFileUrl('https://example.invalid/a.pdf'), 'https://example.invalid/a.pdf');
});

test('preserva a data local da API e converte datas com fuso para Fortaleza', () => {
  assert.equal(materialDateLabel('2026-10-29T00:30:00'), '29/10/2026');
  assert.equal(materialDateLabel('2026-10-29T00:30:00Z'), '28/10/2026');
  assert.equal(materialDateLabel(null), null);
});

test('carrega somente dados da API, ordena por data e deduplica professores', async () => {
  const result = await loadCatalog({ disciplines: async () => [{ id: 'd1', nome: 'Programação' }], posts: async () => [post, { ...post, id: 'p2', dataCriacao: '2026-10-30T10:00:00' }] });
  assert.deepEqual(result.materials.map(item => item.id), ['p2', 'p1']);
  assert.deepEqual(result.professors, [{ id: 'prof1', name: 'Geraldo' }]);
  assert.deepEqual(result.disciplines, [{ id: 'd1', label: 'Programação' }]);
  const empty = await loadCatalog({ disciplines: async () => [], posts: async () => [] });
  assert.deepEqual(empty, { materials: [], professors: [], disciplines: [] });
  await assert.rejects(loadCatalog({ disciplines: async () => [], posts: async () => { throw new Error('indisponível'); } }), /indisponível/);
});
