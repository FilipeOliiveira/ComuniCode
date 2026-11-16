import test from 'node:test';
import assert from 'node:assert/strict';
import { MAX_BYTES, buildPublicationForm, formatBytes, mergeFiles, validateFiles, validatePublication } from '../src/data/publication.js';

const file = (name, size = 10, lastModified = 1) => ({ name, size, lastModified });

test('aceita de 1 a 5 arquivos permitidos de até 25 MB', () => {
  assert.equal(validateFiles([file('aula.PDF'), file('foto.jpeg'), file('video.mp4'), file('lista.docx'), file('notas.txt')]), '');
  assert.equal(validateFiles([]), 'Escolha pelo menos um arquivo.');
  assert.match(validateFiles(Array.from({ length: 6 }, (_, index) => file(`${index}.pdf`))), /no máximo 5/);
  assert.match(validateFiles([file('programa.exe')]), /Formato não permitido: programa\.exe/);
  assert.match(validateFiles([file('semextensao')]), /Formato não permitido/);
  assert.match(validateFiles([file('vazio.pdf', 0)]), /está vazio/);
  assert.equal(validateFiles([file('limite.pdf', MAX_BYTES)]), '');
  assert.match(validateFiles([file('grande.pdf', MAX_BYTES + 1)]), /25 MB/);
});

test('exige título, conteúdo e ao menos uma tag antes dos arquivos', () => {
  const ok = { title: 'Lista', contentId: 'c1', tagIds: ['t1'], files: [file('a.pdf')] };
  assert.equal(validatePublication(ok), '');
  assert.match(validatePublication({ ...ok, title: '  ' }), /título/);
  assert.match(validatePublication({ ...ok, title: 'x'.repeat(256) }), /255/);
  assert.match(validatePublication({ ...ok, contentId: '' }), /conteúdo/);
  assert.match(validatePublication({ ...ok, tagIds: [] }), /tag/);
  assert.match(validatePublication({ ...ok, files: [] }), /arquivo/);
});

test('não repete o mesmo arquivo ao adicionar de novo', () => {
  const first = file('a.pdf', 10, 1);
  const merged = mergeFiles([first], [file('a.pdf', 10, 1), file('a.pdf', 11, 1), file('b.pdf')]);
  assert.deepEqual(merged.map(item => `${item.name}:${item.size}`), ['a.pdf:10', 'a.pdf:11', 'b.pdf:10']);
});

test('monta o multipart com os dados em JSON e os arquivos', async () => {
  const pdf = new File(['%PDF-1.7'], 'lista.pdf', { type: 'application/pdf' });
  const form = buildPublicationForm({ title: ' Lista ', description: '  ', contentId: 'c1', tagIds: ['t1', 't2'] }, [pdf]);
  const dados = form.get('dados');
  assert.equal(dados.type, 'application/json');
  assert.deepEqual(JSON.parse(await dados.text()), { titulo: 'Lista', descricao: null, conteudoId: 'c1', tagsIds: ['t1', 't2'] });
  assert.deepEqual(form.getAll('arquivos').map(item => item.name), ['lista.pdf']);
});

test('formata tamanhos em KB e MB', () => {
  assert.equal(formatBytes(10), '1 KB');
  assert.equal(formatBytes(2048), '2 KB');
  assert.equal(formatBytes(1.5 * 1024 * 1024), '1,5 MB');
});
