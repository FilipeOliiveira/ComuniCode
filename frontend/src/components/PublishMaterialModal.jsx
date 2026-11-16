import React, { useEffect, useRef, useState } from 'react';
import Icon from './Icon.jsx';
import { FILE_ACCEPT, MAX_FILES, buildPublicationForm, formatBytes, mergeFiles, validateFiles, validatePublication } from '../data/publication.js';

const byName = (a, b) => a.label.localeCompare(b.label, 'pt-BR');

export default function PublishMaterialModal({ client, disciplines, onClose, onPublished, onSignedOut }) {
  const dialog = useRef(null);
  const fileInput = useRef(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [disciplineId, setDisciplineId] = useState('');
  const [contents, setContents] = useState({ items: [], loading: false });
  const [contentId, setContentId] = useState('');
  const [tags, setTags] = useState({ items: [], loading: true });
  const [tagIds, setTagIds] = useState([]);
  const [files, setFiles] = useState([]);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleFailure = failure => {
    if (failure.name === 'AbortError') return;
    if (failure.status === 401) onSignedOut('Sua sessão expirou. Entre novamente.');
    else setError(failure.message);
  };

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    dialog.current.showModal();
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previousOverflow; };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    client.tags({ signal: controller.signal })
      .then(items => setTags({ items: items.map(tag => ({ id: tag.id, label: tag.nome })).sort(byName), loading: false }))
      .catch(failure => { if (!controller.signal.aborted) { setTags({ items: [], loading: false }); handleFailure(failure); } });
    return () => controller.abort();
  }, [client]);

  useEffect(() => {
    setContentId('');
    if (!disciplineId) { setContents({ items: [], loading: false }); return undefined; }
    const controller = new AbortController();
    setContents({ items: [], loading: true });
    client.contents(disciplineId, { signal: controller.signal })
      .then(items => setContents({ items: items.map(item => ({ id: item.id, label: item.titulo, order: item.ordem ?? 0 }))
        .sort((a, b) => a.order - b.order || byName(a, b)), loading: false }))
      .catch(failure => { if (!controller.signal.aborted) { setContents({ items: [], loading: false }); handleFailure(failure); } });
    return () => controller.abort();
  }, [client, disciplineId]);

  function addFiles(added) {
    const next = mergeFiles(files, added);
    const problem = validateFiles(next);
    setError(problem);
    if (!problem) setFiles(next);
    if (fileInput.current) fileInput.current.value = '';
  }

  const removeFile = target => { setFiles(files.filter(file => file !== target)); setError(''); };
  const toggleTag = id => setTagIds(current => current.includes(id) ? current.filter(item => item !== id) : [...current, id]);
  const close = () => { if (!busy) onClose(); };

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const data = { title, description, contentId, tagIds };
    const problem = validatePublication({ ...data, files });
    if (problem) { setError(problem); return; }
    setBusy(true);
    setError('');
    try {
      const post = await client.publish(buildPublicationForm(data, files));
      onPublished(post);
    } catch (failure) {
      handleFailure(failure);
      setBusy(false);
    }
  }

  const noTags = !tags.loading && tags.items.length === 0;
  const noContents = disciplineId && !contents.loading && contents.items.length === 0;

  return (
    <dialog ref={dialog} className="upload-dialog" aria-labelledby="publish-title" aria-describedby="publish-scope"
      onCancel={event => { event.preventDefault(); close(); }}
      onClick={event => {
        if (event.target !== event.currentTarget) return;
        const bounds = dialog.current.getBoundingClientRect();
        if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) close();
      }}>
      <div className="upload-dialog-header">
        <div><p className="text-primary text-label-md font-label-md">COMPARTILHE CONHECIMENTO</p><h2 id="publish-title" className="font-headline-lg text-headline-lg mt-1">Enviar material</h2></div>
        <button type="button" className="modal-close" aria-label="Fechar envio de material" disabled={busy} onClick={close}>×</button>
      </div>
      <p id="publish-scope" className="text-body-sm text-on-surface-variant">O material é publicado no acervo assim que o envio terminar. Só usuários com login conseguem abrir os arquivos.</p>
      <form onSubmit={submit} className="upload-form" aria-busy={busy} noValidate>
        <fieldset disabled={busy}>
          <div className={`upload-dropzone ${dragging ? 'is-dragging' : ''}`}
            onDragOver={event => { event.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={event => { event.preventDefault(); setDragging(false); addFiles(event.dataTransfer.files); }}>
            <Icon name="cloud_upload" className="text-3xl text-primary" />
            <label htmlFor="publish-files" className="font-semibold">Escolha ou arraste até {MAX_FILES} arquivos</label>
            <p className="text-body-sm text-outline">PDF, imagens (PNG, JPG, WEBP), vídeo MP4, Word, PowerPoint, ODT ou TXT • até 25 MB cada</p>
            <input ref={fileInput} id="publish-files" type="file" multiple accept={FILE_ACCEPT} aria-describedby="publish-error"
              onChange={event => addFiles(event.target.files)} />
          </div>
          {files.length > 0 && <ul className="flex flex-col gap-space-xs" aria-label="Arquivos selecionados">
            {files.map(file => <li key={`${file.name}|${file.size}|${file.lastModified}`} className="flex items-center justify-between gap-space-sm rounded-lg bg-surface-container-high px-space-md py-space-sm text-body-sm">
              <span className="min-w-0 truncate">{file.name} • {formatBytes(file.size)}</span>
              <button type="button" className="text-primary shrink-0" aria-label={`Remover ${file.name}`} onClick={() => removeFile(file)}>Remover</button>
            </li>)}
          </ul>}
          <label className="upload-field">Título do material<input value={title} onChange={event => setTitle(event.target.value)} required maxLength={255} placeholder="Ex.: Lista de exercícios de árvores" /></label>
          <label className="upload-field">Descrição<textarea value={description} onChange={event => setDescription(event.target.value)} maxLength={1500} rows={3} placeholder="Conte o que seus colegas vão encontrar neste material." /></label>
          <label className="upload-field">Disciplina<select value={disciplineId} onChange={event => setDisciplineId(event.target.value)} required>
            <option value="">Selecione uma disciplina</option>
            {[...disciplines].sort(byName).map(item => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select></label>
          <label className="upload-field">Conteúdo<select value={contentId} onChange={event => setContentId(event.target.value)} required disabled={!disciplineId || contents.loading || noContents}>
            <option value="">{!disciplineId ? 'Escolha a disciplina primeiro' : contents.loading ? 'Carregando conteúdos…' : noContents ? 'Nenhum conteúdo cadastrado' : 'Selecione um conteúdo'}</option>
            {contents.items.map(item => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select></label>
          {noContents && <p className="text-body-sm text-outline">Esta disciplina ainda não tem conteúdos. Um professor ou administrador precisa cadastrá-los.</p>}
          <div className="upload-field" role="group" aria-labelledby="publish-tags-label">
            <span id="publish-tags-label">Tags</span>
            {tags.loading ? <p className="text-body-sm text-outline">Carregando tags…</p>
              : noTags ? <p className="text-body-sm text-outline">Nenhuma tag cadastrada. Um professor ou administrador precisa criar as tags antes do primeiro envio.</p>
              : <div className="flex flex-wrap gap-space-xs">{tags.items.map(tag => <button type="button" key={tag.id} aria-pressed={tagIds.includes(tag.id)} onClick={() => toggleTag(tag.id)}
                  className={`card-action ${tagIds.includes(tag.id) ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-highest'}`}>{tag.label}</button>)}</div>}
          </div>
        </fieldset>
        <p id="publish-error" role="alert" className="upload-error">{error}</p>
        <div className="upload-dialog-actions">
          <button type="button" className="card-action bg-surface-container-highest" disabled={busy} onClick={close}>Cancelar</button>
          <button type="submit" disabled={busy || noTags} className="card-action bg-primary-container text-on-primary-container">{busy ? 'Enviando…' : 'Publicar material'}</button>
        </div>
      </form>
    </dialog>
  );
}
