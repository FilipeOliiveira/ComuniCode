export function safeFileUrl(value) {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password ? url.href : null;
  } catch { return null; }
}

export function materialDateLabel(value) {
  if (!value) return null;
  // LocalDateTime do backend não tem fuso: preserva sua data de calendário.
  const local = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/.test(value);
  const date = new Date(local ? `${value}Z` : value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString('pt-BR', { timeZone: local ? 'UTC' : 'America/Fortaleza', day: '2-digit', month: '2-digit', year: 'numeric' });
}

function attachmentFromApi(file) {
  const url = safeFileUrl(file.url);
  const path = url ? new URL(url).pathname : '';
  const extension = (file.nome?.match(/\.([a-z0-9]+)$/i)?.[1] || path.match(/\.([a-z0-9]+)$/i)?.[1] || '').toUpperCase();
  return {
    id: file.id, filename: file.nome, url, bytes: file.tamanho,
    format: file.tipo === 'PDF' ? 'PDF' : extension || file.tipo,
    mediaType: file.tipo === 'VIDEO' ? 'video' : file.tipo === 'IMAGEM' ? 'image'
      : ['MP3', 'WAV', 'OGG'].includes(extension) ? 'audio' : 'document',
  };
}

export function materialFromApi(post) {
  return {
    id: post.id, title: post.titulo, description: post.descricao || '',
    authorId: post.autorId, author: post.autorNome,
    authorRole: ({ PROFESSOR: 'professor', ADMIN: 'admin', ALUNO: 'student' })[post.autorPerfil] || 'unknown',
    professorId: post.autorPerfil === 'PROFESSOR' ? post.autorId : null,
    disciplineId: post.disciplinaId, discipline: post.disciplinaNome,
    content: post.conteudoTitulo, uploadedAt: post.dataCriacao, status: post.status,
    verified: post.verificada === true, tags: (post.tags || []).map(tag => tag.nome),
    attachments: (post.anexos || []).map(attachmentFromApi),
  };
}

const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export function filterCatalog(items, { format = 0, discipline = 'Todas', professor = 'todos', search = '', ownerId } = {}) {
  const terms = normalize(search).trim().split(/\s+/).filter(Boolean);
  return items.filter(material => {
    const matchesFormat = format === 0 || material.attachments.some(file =>
      (format === 1 && file.format === 'PDF') || (format === 2 && ['DOC', 'DOCX'].includes(file.format))
      || (format === 3 && file.mediaType === 'video')
      || (format === 4 && file.format !== 'PDF' && !['DOC', 'DOCX'].includes(file.format) && file.mediaType !== 'video'));
    const text = normalize([material.title, material.description, material.discipline, material.content, material.author, ...material.tags].join(' '));
    return matchesFormat && (discipline === 'Todas' || material.disciplineId === discipline)
      && (professor === 'todos' || material.professorId === professor)
      && (!ownerId || material.authorId === ownerId) && terms.every(term => text.includes(term));
  });
}

export async function loadCatalog(client, signal) {
  const [disciplines, posts] = await Promise.all([client.disciplines({ signal }), client.posts({ signal })]);
  if (!Array.isArray(disciplines) || !Array.isArray(posts)) throw new Error('O servidor retornou um acervo inválido. Tente novamente.');
  const materials = posts.map(materialFromApi).sort((a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt));
  const professors = [...new Map(materials.filter(item => item.professorId).map(item => [item.professorId, { id: item.professorId, name: item.author }])).values()]
    .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
  return { materials, professors, disciplines: disciplines.map(item => ({ id: item.id, label: item.nome })).sort((a, b) => a.label.localeCompare(b.label, 'pt-BR')) };
}
