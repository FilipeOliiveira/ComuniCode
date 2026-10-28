import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { createServer } from 'vite';
import React from 'react';
import { renderToString } from 'react-dom/server';
import { materials, professors, disciplines, filterMaterials, materialUrl } from '../src/data/materials.js';

test('mantém apenas os sete professores solicitados com o título PROF. DR.', () => {
  for (const id of ['carlos', 'geraldo', 'mario', 'anselmo', 'luciano', 'simara', 'luis']) {
    assert.ok(professors.some(professor => professor.id === id), id);
  }
  assert.equal(professors.length, 7);
  assert.ok(professors.every(professor => professor.name.startsWith('PROF. DR. ')));
  assert.equal(professors.find(professor => professor.id === 'mario').name, 'PROF. DR. Mário Meireles');
});

test('preserva disciplinas e resolve as quatro novas disciplinas', () => {
  for (const id of ['Todas', 'calculo', 'dados-ii', 'ia', 'algoritmos', 'dados-i', 'pds', 'pln', 'laboratorio', 'teste']) {
    assert.ok(disciplines.some(discipline => discipline.id === id), id);
  }
  assert.equal(disciplines.filter(discipline => discipline.id === 'pds').length, 2);
  for (const [discipline, title] of [['pln', 'Rotulagem de Sequências'], ['laboratorio', 'Componentizando a Interface'], ['pds', 'Diagrama de Sequência'], ['teste', 'Gerenciamento de Testes']]) {
    assert.ok(filterMaterials(materials, { discipline }).some(item => item.title === title));
  }
});

test('combina disciplina, professor, formato e pesquisa sem acentuação', () => {
  assert.equal(filterMaterials(materials, { discipline: 'pln', professor: 'anselmo', format: 1, search: 'ROTULAGEM sequencias' }).length, 1);
  assert.equal(filterMaterials(materials, { discipline: 'pln', professor: 'simara' }).length, 0);
  assert.equal(filterMaterials(materials, { search: 'simara uml' })[0].id, 'diagrama-sequencia');
  assert.equal(filterMaterials(materials, { format: 2 })[0].id, 'estruturas-avancadas');
  assert.equal(filterMaterials(materials, { format: 1 }).length, 5);
  assert.equal(filterMaterials(materials, { professor: 'carlos' }).length, 0);
  assert.equal(filterMaterials(materials, { search: 'inexistente' }).length, 0);
  assert.equal(filterMaterials(materials).length, 6);
  assert.equal(materials[1].professorId, 'geraldo');
  assert.equal(materials[1].professor, 'PROF. DR. Geraldo Braz');
});

test('distingue alunos e professores e apresenta autoria e datas', () => {
  for (const material of materials.slice(0, 4)) {
    assert.equal(material.authorRole, 'professor');
    assert.equal(material.verified, true);
    assert.ok(Number.isFinite(new Date(material.uploadedAt).getTime()));
    assert.equal(material.demoDate, true);
  }
  assert.equal(materials[4].author, 'João Victor de Oliveira Carneiro');
  assert.equal(materials[5].author, 'Ana Luiza Oliveira Montelo');
  for (const material of materials.slice(4)) {
    assert.equal(material.authorRole, 'student');
    assert.equal(material.course, 'Ciência da Computação');
    assert.equal(material.institution, 'UFMA');
    assert.equal(material.semester, '8º período');
    assert.equal(material.professorId, null);
  }
  assert.equal(filterMaterials(materials, { search: 'joao victor ufma' })[0].id, 'estruturas-avancadas');
});

test('renderiza React sem erros e entrega os PDFs estáticos por HTTP', async () => {
  const server = await createServer({ server: { port: 0, host: '127.0.0.1' } });
  try {
    const { default: App } = await server.ssrLoadModule('/src/App.jsx');
    const html = renderToString(React.createElement(App));
    assert.ok(html.includes('COMUNICODE'));
    assert.ok(html.replace(/<!--[\s\S]*?-->/g, '').includes('6 materiais encontrados'));
    assert.equal((html.match(/<article /g) ?? []).length, 6);
    assert.equal((html.match(/ download=/g) ?? []).length, 4);
    assert.equal((html.match(/aria-label="Professor verificado"/g) ?? []).length, 4);
    assert.equal((html.match(/>Material de aluno</g) ?? []).length, 2);
    assert.equal((html.match(/<time /g) ?? []).length, 6);
    for (const material of materials.slice(0, 4)) assert.ok(html.includes(material.title));
    await server.listen();
    const base = `http://127.0.0.1:${server.httpServer.address().port}`;
    for (const material of materials.slice(0, 4)) {
      const file = await fs.readFile(new URL(`../public/materials/${material.filename}`, import.meta.url));
      assert.equal(file.subarray(0, 5).toString(), '%PDF-');
      assert.equal(file.length, material.bytes);
      const response = await fetch(`${base}${materialUrl(material)}`);
      assert.equal(response.status, 200);
      assert.match(response.headers.get('content-type'), /application\/pdf/);
      const served = Buffer.from(await response.arrayBuffer());
      assert.equal(createHash('sha256').update(served).digest('hex'), createHash('sha256').update(file).digest('hex'));
    }
    assert.equal(materialUrl(materials[4]), null);
  } finally {
    await server.close();
  }
});
