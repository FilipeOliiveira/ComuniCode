import React from 'react';
import { materialUrl } from '../data/materials.js';

export default function MaterialCard({ material }) {
  const url = materialUrl(material);
  const author = material.author || material.professor;
  const initials = author.replace(/\b(?:profa?|dra?)\.?/gi, '').trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('');
  const isProfessor = material.authorRole === 'professor';
  const uploadDate = new Date(material.uploadedAt);
  const dateLabel = uploadDate.toLocaleDateString('pt-BR', { timeZone: 'America/Fortaleza', day: '2-digit', month: '2-digit', year: 'numeric' });
  const size = material.bytes ? `${(material.bytes / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB` : null;

  return (
    <article className="material-card group relative rounded-2xl bg-surface-container-low hover:bg-surface-container p-space-lg flex flex-col justify-between transition-all duration-200 shadow-lg hover:-translate-y-1" aria-labelledby={`title-${material.id}`}>
      <div className="flex flex-col gap-space-md">
        <div className="flex flex-wrap items-center justify-between gap-space-sm">
          <span className="inline-flex items-center gap-1.5 px-space-sm py-0.5 rounded bg-surface-container-highest text-on-surface-variant font-label-sm text-label-sm">
            <span className="material-symbols-outlined text-sm" aria-hidden="true">{material.mediaType === 'video' ? 'videocam' : material.mediaType === 'audio' ? 'headphones' : material.format === 'PDF' ? 'picture_as_pdf' : 'description'}</span>
            {material.format}{size && ` • ${size}`}
          </span>
          <span className={`rounded-full px-space-sm py-1 font-label-sm text-label-sm ${isProfessor ? 'bg-primary/10 text-primary' : 'bg-secondary-container text-on-secondary-container'}`}>{isProfessor ? 'Material de aula' : 'Material de aluno'}</span>
        </div>
        <div>
          <p className="text-outline font-label-sm text-label-sm uppercase font-semibold tracking-wider">{material.discipline}</p>
          <h3 id={`title-${material.id}`} className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors mt-space-xs">{material.title}</h3>
          <p className="font-body-sm text-body-sm text-on-surface-variant mt-space-sm">{material.description}</p>
        </div>
        <div className="flex items-center gap-space-sm pt-space-xs">
          <span className="author-avatar" aria-hidden="true">{initials}</span>
          <div className="min-w-0">
            <p className="font-label-md text-label-md text-on-surface author-name">{author}{isProfessor && material.verified && <span className="verified-mark" role="img" aria-label="Professor verificado" title="Professor verificado"><svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="m12 1 3 2.1 3.7.3 1.2 3.5 2.8 2.4-.8 3.6.8 3.6-2.8 2.4-1.2 3.5-3.7.3L12 25l-3-2.3-3.7-.3-1.2-3.5-2.8-2.4.8-3.6-.8-3.6 2.8-2.4 1.2-3.5L9 3.1Z" transform="translate(1 0) scale(.92)"/><path d="m7 12 3 3 7-7" fill="none" stroke="#131313" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg></span>}</p>
            <p className="font-body-sm text-body-sm text-outline">{isProfessor ? 'Professor • Conteúdo de aula' : [material.course, material.institution, material.semester].filter(Boolean).join(' • ')}</p>
          </div>
        </div>
        <div className="upload-date">
          <span className="material-symbols-outlined text-base" aria-hidden="true">calendar_today</span>
          <div><span>Enviado em </span><time dateTime={material.uploadedAt}>{dateLabel}</time>{material.demoDate && <small>Data ilustrativa do exemplo</small>}</div>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-space-sm pt-space-lg">
        {url ? <>
          <a href={url} target="_blank" rel="noopener noreferrer" aria-label={`Visualizar ${material.title} (nova aba)`} className="card-action bg-surface-container-highest hover:bg-surface-bright text-on-surface">
            <span className="material-symbols-outlined text-base" aria-hidden="true">visibility</span> Visualizar
          </a>
          <a href={url} download={material.filename} aria-label={`Baixar ${material.title}`} className="card-action bg-primary-container hover:bg-primary text-on-primary-container">
            <span className="material-symbols-outlined text-base" aria-hidden="true">download</span> Baixar
          </a>
        </> : <p className="col-span-2 font-body-sm text-body-sm text-outline">Exemplo de contribuição de aluno • Arquivo não disponibilizado.</p>}
      </div>
    </article>
  );
}
