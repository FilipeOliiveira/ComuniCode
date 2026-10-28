import React, { useEffect, useRef, useState } from 'react';
import { disciplines, professors } from '../data/materials.js';
import { FILE_ACCEPT, fileDetails, validateFile } from '../data/uploads.js';

export default function UploadMaterialModal({ onClose, onSubmit }) {
  const dialog = useRef(null);
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [role, setRole] = useState('student');
  const options = disciplines.filter((item, index, list) => item.id !== 'Todas' && list.findIndex(other => other.id === item.id) === index);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    dialog.current.showModal();
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previousOverflow; };
  }, []);

  function selectFile(selected) {
    const problem = validateFile(selected);
    setError(problem);
    setFile(problem ? null : selected);
  }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const problem = validateFile(file);
    if (problem) { setError(problem); return; }
    const data = new FormData(event.currentTarget);
    const title = data.get('title').trim();
    const description = data.get('description').trim();
    const author = data.get('author')?.trim();
    if (!title || !description || (role === 'student' && !author)) { setError('Preencha título, descrição e nome do autor.'); return; }
    const discipline = options.find(item => item.id === data.get('discipline'));
    const professor = professors.find(item => item.id === data.get('professor'));
    const material = {
      id: crypto.randomUUID(), section: 'uploaded', title, description,
      disciplineId: discipline.id,
      discipline: discipline.id === 'pds' ? 'Processo de Desenvolvimento de Software (PDS)' : discipline.label,
      authorRole: role,
      author: role === 'student' ? author : professor.name,
      professorId: role === 'professor' ? professor.id : null,
      // O envio local não concede verificação ao autor.
      verified: false,
      ...(role === 'student' ? { course: data.get('course').trim(), institution: data.get('institution').trim(), semester: data.get('semester').trim() } : {}),
      uploadedAt: new Date().toISOString(), demoDate: false, ...fileDetails(file),
    };
    setBusy(true);
    setError('');
    try {
      await onSubmit(material, file);
      onClose();
    } catch {
      setError('Não foi possível salvar neste navegador. Verifique o espaço disponível ou tente em uma janela normal.');
      setBusy(false);
    }
  }

  return (
    <dialog ref={dialog} className="upload-dialog" aria-labelledby="upload-title" aria-describedby="upload-scope" onCancel={event => { if (busy) event.preventDefault(); }} onClose={onClose} onClick={event => { if (event.target === event.currentTarget && !busy) { const bounds = dialog.current.getBoundingClientRect(); if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose(); } }}>
      <div className="upload-dialog-header">
        <div><p className="text-primary text-label-md font-label-md">COMPARTILHE CONHECIMENTO</p><h2 id="upload-title" className="font-headline-lg text-headline-lg mt-1">Enviar material</h2></div>
        <button type="button" className="modal-close" aria-label="Fechar envio de material" disabled={busy} onClick={onClose}>×</button>
      </div>
      <p id="upload-scope" className="text-body-sm text-on-surface-variant">Envie documentos, vídeos, áudios ou imagens. Seus materiais ficam salvos neste navegador e disponíveis na página inicial.</p>
      <form onSubmit={submit} className="upload-form" aria-busy={busy}>
        <fieldset disabled={busy}>
          <div className={`upload-dropzone ${dragging ? 'is-dragging' : ''}`} onDragOver={event => { event.preventDefault(); setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={event => { event.preventDefault(); setDragging(false); if (event.dataTransfer.files.length !== 1) { setError('Envie um arquivo por vez.'); return; } selectFile(event.dataTransfer.files[0]); }}>
            <span className="material-symbols-outlined text-3xl text-primary" aria-hidden="true">cloud_upload</span>
            <label htmlFor="upload-file" className="font-semibold">{file ? file.name : 'Escolha ou arraste seu arquivo'}</label>
            <p className="text-body-sm text-outline">PDF, Word, slides, planilhas, vídeo, áudio, imagem ou ZIP • até 100 MB</p>
            <input id="upload-file" name="file" type="file" accept={FILE_ACCEPT} onChange={event => { const selected = event.target.files[0]; if (selected) selectFile(selected); }} aria-describedby="upload-error" />
            {file && <span className="text-body-sm text-primary">{(file.size / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB • Pronto para enviar</span>}
          </div>
          <label className="upload-field">Título do material<input name="title" required maxLength={160} placeholder="Ex.: Resumo de Estruturas de Dados" /></label>
          <label className="upload-field">Disciplina<select aria-label="Disciplina" name="discipline" required defaultValue=""><option value="" disabled>Selecione uma disciplina</option>{options.map(item => <option key={item.id} value={item.id}>{item.id === 'pds' ? 'Processo de Desenvolvimento de Software (PDS)' : item.label}</option>)}</select></label>
          <label className="upload-field">Descrição<textarea name="description" required maxLength={1500} rows={3} placeholder="Conte o que seus colegas vão encontrar neste material." /></label>
          <label className="upload-field">Autoria<select aria-label="Autoria" value={role} onChange={event => setRole(event.target.value)}><option value="student">Aluno</option><option value="professor">Professor</option></select></label>
          {role === 'student' ? <>
            <label className="upload-field">Nome do aluno<input name="author" required maxLength={120} defaultValue="Ana Luiza Oliveira Montelo" /></label>
            <div className="upload-field-grid">
              <label className="upload-field">Curso<input name="course" required maxLength={100} defaultValue="Ciência da Computação" /></label>
              <label className="upload-field">Instituição<input name="institution" required maxLength={100} defaultValue="UFMA" /></label>
              <label className="upload-field">Período<input name="semester" required maxLength={30} defaultValue="8º período" /></label>
            </div>
          </> : <label className="upload-field">Professor<select aria-label="Professor" name="professor" required>{professors.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
        </fieldset>
        <p id="upload-error" role="alert" className="upload-error">{error}</p>
        <div className="upload-dialog-actions">
          <button type="button" className="card-action bg-surface-container-highest" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="submit" disabled={busy} className="card-action bg-primary-container text-on-primary-container">{busy ? 'Salvando…' : 'Enviar material'}</button>
        </div>
      </form>
    </dialog>
  );
}
