import React from 'react';
import Icon from './Icon.jsx';
import { materialDateLabel } from '../data/catalog.js';

const statuses = { RASCUNHO: 'Rascunho', PUBLICADA: 'Publicada', ARQUIVADA: 'Arquivada' };
const roles = { professor: 'Professor', student: 'Aluno', admin: 'Administrador', unknown: 'Autor' };

export default function MaterialCard({ material }) {
  const author = material.author || 'Autor não informado';
  const initials = author.trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('');
  const dateLabel = materialDateLabel(material.uploadedAt);
  const attachments = material.attachments || [];

  return <article className="material-card group relative rounded-2xl bg-surface-container-low hover:bg-surface-container p-space-lg flex flex-col justify-between transition-all duration-200 shadow-lg" aria-labelledby={`title-${material.id}`}>
    <div className="flex flex-col gap-space-md">
      <div className="flex flex-wrap items-center justify-between gap-space-sm">
        <span className="rounded bg-surface-container-highest text-on-surface-variant font-label-sm text-label-sm px-space-sm py-1">{attachments.length ? `${attachments.length} ${attachments.length === 1 ? 'arquivo' : 'arquivos'}` : 'Sem anexo'}</span>
        <span className="rounded-full px-space-sm py-1 text-label-sm text-primary bg-primary/10">{statuses[material.status] || material.status}</span>
      </div>
      <div>
        <p className="text-outline font-label-sm text-label-sm uppercase tracking-wider">{material.discipline || 'Disciplina não informada'}</p>
        <h3 id={`title-${material.id}`} className="font-headline-sm text-headline-sm text-on-surface mt-space-xs">{material.title}</h3>
        <p className="font-body-sm text-body-sm text-on-surface-variant mt-space-sm">{material.description}</p>
      </div>
      <div className="flex items-center gap-space-sm">
        <span className="author-avatar" aria-hidden="true">{initials}</span>
        <div className="min-w-0"><p className="font-label-md text-label-md author-name">{author}</p><p className="text-body-sm text-outline">{roles[material.authorRole]}</p></div>
      </div>
      {material.verified && <span className="text-body-sm text-primary">Material verificado</span>}
      {dateLabel && <div className="upload-date"><Icon name="calendar_today" className="text-base" /><span>Enviado em <time dateTime={material.uploadedAt}>{dateLabel}</time></span></div>}
    </div>
    <div className="attachment-list">
      {attachments.map(file => <div key={file.id}>
        <p className="attachment-name">{file.filename} • {file.format}{Number.isFinite(file.bytes) && ` • ${(file.bytes / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`}</p>
        {file.url ? <a href={file.url} target="_blank" rel="noopener noreferrer" aria-label={`Abrir ${file.filename} (nova aba)`} className="card-action bg-primary-container text-on-primary-container"><Icon name="open_in_new" className="text-base" />Abrir arquivo</a> : <p className="text-body-sm text-outline">Arquivo sem endereço disponível.</p>}
      </div>)}
      {attachments.length === 0 && <p className="text-body-sm text-outline">Esta postagem não possui arquivos anexados.</p>}
    </div>
  </article>;
}
