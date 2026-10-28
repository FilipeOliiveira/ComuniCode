import test from 'node:test';
import assert from 'node:assert/strict';
import { validateFile, fileDetails, MAX_UPLOAD_BYTES } from '../src/data/uploads.js';
import { filterMaterials, materialUrl } from '../src/data/materials.js';

test('aceita documentos, vídeos, áudios e imagens e valida tamanho e formato', () => {
  for (const name of ['aula.PDF', 'resumo.docx', 'slides.pptx', 'video.mp4', 'audio.mp3', 'imagem.png', 'arquivos.zip']) assert.equal(validateFile({ name, size: 100 }), '');
  assert.ok(validateFile(null));
  assert.ok(validateFile({ name: 'vazio.pdf', size: 0 }));
  assert.ok(validateFile({ name: 'video.mp4', size: MAX_UPLOAD_BYTES + 1 }));
  assert.ok(validateFile({ name: 'arquivo.exe', size: 100 }));
  assert.equal(validateFile({ name: 'video.mp4', size: MAX_UPLOAD_BYTES }), '');
});

test('classifica vídeos para filtro e mantém endereço local de download', () => {
  const material = { title: 'Vídeo da aula', description: '', discipline: 'PLN', author: 'Ana Luiza', objectUrl: 'blob:local', ...fileDetails({ name: 'aula.mp4', size: 123 }) };
  assert.equal(material.mediaType, 'video');
  assert.equal(materialUrl(material), 'blob:local');
  assert.equal(filterMaterials([material], { format: 3 }).length, 1);
  assert.equal(filterMaterials([material], { format: 1 }).length, 0);
  assert.equal(filterMaterials([material], { format: 4 }).length, 0);
  assert.equal(filterMaterials([{ ...material, ...fileDetails({ name: 'imagem.png', size: 100 }) }], { format: 4 }).length, 1);
});
