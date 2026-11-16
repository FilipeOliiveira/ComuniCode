// Regras do envio de materiais. Espelham o ValidadorArquivos do backend, que confere tudo de novo.
export const MAX_FILES = 5;
export const MAX_BYTES = 25 * 1024 * 1024;
export const EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg', 'webp', 'mp4', 'doc', 'docx', 'ppt', 'pptx', 'odt', 'txt'];
export const FILE_ACCEPT = EXTENSIONS.map(extension => `.${extension}`).join(',');

const extensionOf = name => (String(name || '').match(/\.([^.]+)$/)?.[1] || '').toLowerCase();

export function formatBytes(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
}

export function validateFiles(files) {
  if (!files?.length) return 'Escolha pelo menos um arquivo.';
  if (files.length > MAX_FILES) return `Envie no máximo ${MAX_FILES} arquivos por material.`;
  for (const file of files) {
    if (!EXTENSIONS.includes(extensionOf(file.name))) return `Formato não permitido: ${file.name}. Use ${EXTENSIONS.join(', ')}.`;
    if (!file.size) return `O arquivo ${file.name} está vazio.`;
    if (file.size > MAX_BYTES) return `O arquivo ${file.name} passa do limite de 25 MB.`;
  }
  return '';
}

export function validatePublication({ title, contentId, tagIds, files }) {
  if (!title?.trim()) return 'Informe o título do material.';
  if (title.trim().length > 255) return 'O título deve ter até 255 caracteres.';
  if (!contentId) return 'Escolha a disciplina e o conteúdo.';
  if (!tagIds?.length) return 'Escolha pelo menos uma tag.';
  return validateFiles(files);
}

/** Junta arquivos sem repetir o mesmo arquivo (mesmo nome, tamanho e data). */
export function mergeFiles(current, added) {
  const key = file => `${file.name}|${file.size}|${file.lastModified}`;
  const seen = new Set(current.map(key));
  return [...current, ...[...added].filter(file => !seen.has(key(file)) && seen.add(key(file)))];
}

export function buildPublicationForm({ title, description, contentId, tagIds }, files) {
  const form = new FormData();
  const data = { titulo: title.trim(), descricao: description?.trim() || null, conteudoId: contentId, tagsIds: tagIds };
  form.append('dados', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  for (const file of files) form.append('arquivos', file, file.name);
  return form;
}
