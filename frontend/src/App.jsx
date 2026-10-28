import React, { useState } from 'react';
import { disciplines, professors, materials, filterMaterials } from './data/materials.js';
import MaterialSection from './components/MaterialSection.jsx';
import UploadMaterialModal from './components/UploadMaterialModal.jsx';
import useUploads from './hooks/useUploads.js';

// Acervo local: os PDFs são servidos pelo próprio frontend, sem chamadas ao backend.
export default function App() {
  const [format, setFormat] = useState(0);
  const [discipline, setDiscipline] = useState('Todas');
  const [professor, setProfessor] = useState('todos');
  const [search, setSearch] = useState('');
  const [uploadOpen, setUploadOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const { uploads, addUpload, storageError } = useUploads();
  const visibleMaterials = filterMaterials([...uploads, ...materials], { format, discipline, professor, search });
  const clearFilters = () => { setFormat(0); setDiscipline('Todas'); setProfessor('todos'); setSearch(''); };
  return (<div className="app-shell bg-background font-body-md text-on-surface antialiased min-h-screen">
<header className="fixed top-0 left-0 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.35)] z-50">
<div className="h-16 w-full px-gutter flex items-center justify-between gap-space-lg">
<div className="flex items-center gap-space-sm w-64 shrink-0">
<span className="brand-mark" aria-hidden="true">&lt;/&gt;</span>
<span className="font-headline-sm text-headline-sm text-on-surface tracking-tight">COMUNICODE</span>
<span className="font-label-sm text-label-sm px-space-xs py-0.5 rounded-full bg-secondary-container text-on-secondary-container ml-space-xs font-semibold">ACADÊMICO</span>
</div>
<div className="flex-1 max-w-2xl">
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-space-md text-outline pointer-events-none text-xl">search</span>
<input value={search} onChange={event => setSearch(event.target.value)} className="w-full h-10 pl-11 pr-4 bg-surface-container-low text-on-surface placeholder:text-outline text-body-sm font-body-sm rounded-lg border-0 focus:outline-none focus:ring-1 focus:ring-primary shadow-inner" aria-label="Pesquisar materiais" placeholder="Pesquisar por títulos, professores, disciplinas..." type="search" />
</div>
</div>
<div className="flex items-center gap-space-md shrink-0">
<button aria-label="Notificações" className="relative p-space-sm rounded-lg text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high transition-colors">
<span className="material-symbols-outlined text-2xl">notifications</span>
<span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-tertiary ring-2 ring-surface-container-lowest"></span>
</button>
<button onClick={() => setUploadOpen(true)} aria-label="Enviar material" className="flex items-center gap-space-xs px-space-md py-space-sm rounded-lg bg-primary-container text-on-primary-container font-label-md text-label-md hover:bg-primary transition-all shadow-[0_0_16px_rgba(128,131,255,0.25)]">
<span className="material-symbols-outlined text-lg" aria-hidden="true">upload_file</span>
<span className="">Enviar Material</span>
</button>
<div className="flex items-center gap-space-xs pl-space-xs">
<div className="relative">
<img alt="Profile" className="w-8 h-8 rounded-full object-cover ring-1 ring-primary/40" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCnuQumm44Pm_CqXgy7T5VsvqZRsLmX1u7cA4S0seo6EJOn6Dk45pDPpH9FmfkEL3n5o9bevR-xEOhJV3oGXgwZKJ82vQlvWeIL1affTApSYqATXvsD2apc50m7UvWeCUXgsRc176sjCk_gXuJNhc5cJDHRoklxmpDd05LT8BPSBr4wVx7kmhsfiFN1Yvj-BdD6UL_PZBHgtL9s2Pvo91PhWmrXn6cqPWF7cRLMdAqP" />
<span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-tertiary ring-2 ring-surface-container-lowest"></span>
</div>
</div>
</div>
</div>
</header>
<aside className="fixed left-0 top-16 bottom-0 w-64 bg-surface-container-low z-40 flex flex-col justify-between py-space-md shadow-[1px_0_12px_rgba(0,0,0,0.3)]">
<div className="flex-1 flex flex-col gap-space-lg px-space-md">
<div className="px-space-sm">
<p className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-semibold">Repositório</p>
</div>
<nav className="flex flex-col gap-space-xs" data-active-classes="bg-secondary-container text-on-secondary-container font-semibold shadow-[inset_3px_0_0_#c0c1ff]">
<a aria-current="page" className="flex items-center gap-space-md px-space-md py-space-sm rounded-lg transition-all bg-secondary-container text-on-secondary-container font-semibold shadow-[inset_3px_0_0_#c0c1ff]" data-path="home" href="#">
<span className="material-symbols-outlined text-xl">home</span>
<span className="">Home</span>
</a>
<a className="flex items-center gap-space-md px-space-md py-space-sm rounded-lg text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-all font-body-md text-body-md" data-path="explorar" href="#acervo" onClick={clearFilters}>
<span className="material-symbols-outlined text-xl">explore</span>
<span className="">Explorar</span>
</a>
<a className="flex items-center gap-space-md px-space-md py-space-sm rounded-lg text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-all font-body-md text-body-md" data-path="meus-materiais" href="#">
<span className="material-symbols-outlined text-xl">folder_shared</span>
<span className="">Meus Materiais</span>
</a>
<a className="flex items-center gap-space-md px-space-md py-space-sm rounded-lg text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-all font-body-md text-body-md" data-path="favoritos" href="#">
<span className="material-symbols-outlined text-xl">bookmark</span>
<span className="">Favoritos</span>
</a>
<a className="flex items-center gap-space-md px-space-md py-space-sm rounded-lg text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-all font-body-md text-body-md" data-path="configuracoes" href="#">
<span className="material-symbols-outlined text-xl">settings</span>
<span className="">Configurações</span>
</a>
</nav>
</div>
<div className="px-space-md flex flex-col gap-space-md pt-space-md">
<a className="flex items-center gap-space-sm px-space-md py-space-sm rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors font-body-sm text-body-sm" data-path="suporte" href="#">
<span className="material-symbols-outlined text-lg">help</span>
<span className="">Ajuda &amp; Suporte</span>
</a>
</div>
</aside>
<div className="pl-64">
<main className="relative pt-16 min-h-screen bg-background w-full px-gutter-lg py-space-xl">
<div className="flex flex-col w-full">

<div className="relative w-full overflow-hidden">

<div className="absolute -top-24 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-3xl pointer-events-none">
</div>
<div className="absolute top-1/3 -right-20 w-80 h-80 bg-secondary-container/20 rounded-full blur-3xl pointer-events-none">
</div>

<section className="relative z-10 mb-space-xl">
<div className="relative rounded-2xl bg-gradient-to-br from-surface-container-high/90 via-surface-container/80 to-surface-container-lowest p-space-xl overflow-hidden shadow-2xl">

<div className="absolute -right-12 -bottom-12 opacity-10 pointer-events-none text-primary">
<svg fill="currentColor" height="420" viewBox="0 0 200 200" width="420">
<path d="M40,-68.1C51.6,-62.4,60.8,-51.9,68.4,-39.8C75.9,-27.7,81.9,-13.9,81.8,-0.1C81.6,13.7,75.3,27.4,66.8,38.8C58.3,50.2,47.7,59.3,35.4,66.5C23.1,73.6,9.1,78.8,-5.4,81.9C-19.9,85,-35,86,-47.3,79.5C-59.6,73.1,-69.1,59.2,-74.8,44.7C-80.5,30.2,-82.5,15.1,-80.6,0.9C-78.7,-13.3,-72.9,-26.6,-64.8,-38.3C-56.7,-50,-46.3,-60.1,-34.3,-65.7C-22.3,-71.4,-8.6,-72.6,3.8,-79.1C16.3,-85.7,28.4,-73.8,40,-68.1Z" transform="translate(100 100)">
</path>
</svg>
</div>
<div className="relative flex flex-col lg:flex-row lg:items-center justify-between gap-space-xl">

<div className="max-w-3xl flex flex-col gap-space-md">
<div className="inline-flex items-center gap-space-xs px-space-md py-1 rounded-full bg-surface-container-highest text-primary-fixed w-fit">
<span className="material-symbols-outlined text-base">verified_user</span>
<span className="font-label-md text-label-md tracking-wider uppercase">Portal Oficial de Compartilhamento</span>
</div>
<div>
<h1 className="font-display-lg text-display-lg text-on-surface tracking-tight">
                Olá, Luiza! Pronto para aprofundar seus estudos hoje?
              </h1>

</div>

<div className="flex flex-wrap items-center gap-space-md pt-space-xs">
<a className="inline-flex items-center gap-space-sm px-space-xl py-3 rounded-lg bg-gradient-to-r from-primary-container to-secondary-container text-on-primary-container font-label-md text-label-md hover:brightness-110 shadow-lg shadow-primary-container/20 transition-all cursor-pointer" href="#acervo" onClick={clearFilters}>
<span className="material-symbols-outlined text-lg" aria-hidden="true">auto_stories</span>
<span className="">Explorar Acervo Completo</span>
</a>
<button onClick={() => setUploadOpen(true)} className="inline-flex items-center gap-space-sm px-space-lg py-3 rounded-lg bg-surface-container-highest/60 hover:bg-surface-container-highest text-on-surface font-label-md text-label-md transition-all cursor-pointer" type="button">
<span className="material-symbols-outlined text-lg text-primary" aria-hidden="true">upload_file</span>
<span className="">Fazer Upload de Resumo</span>
</button>
</div>
</div>

<div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-1 gap-space-sm w-full lg:w-72 shrink-0">






</div>
</div>
</div>
</section>

<section className="mb-space-xl" id="acervo">
<div className="rounded-xl bg-surface-container-low/95 p-space-lg shadow-xl flex flex-col gap-space-md backdrop-blur-md">

<div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-space-md">

<div className="flex flex-wrap items-center gap-space-md">

<div className="relative min-w-[240px]">
<label className="sr-only" htmlFor="filter-prof">Filtrar por Professor</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-space-md text-primary text-xl pointer-events-none">school</span>
<select value={professor} onChange={event => setProfessor(event.target.value)} className="w-full h-11 pl-11 pr-10 bg-surface-container-highest text-on-surface text-body-sm font-body-sm rounded-lg focus:outline-none appearance-none cursor-pointer" id="filter-prof">
<option value="todos">Todos os Professores</option>
{professors.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
</select>
<span className="material-symbols-outlined absolute right-space-md text-outline text-xl pointer-events-none">expand_more</span>
</div>
</div>

<div className="flex flex-wrap items-center p-1 rounded-lg bg-surface-container-highest text-body-sm font-body-sm">
<button onClick={() => setFormat(0)} aria-pressed={format === 0} className={`format-toggle-btn px-space-md py-1.5 rounded font-label-md text-label-md transition-all ${format === 0 ? 'bg-primary-container text-on-primary-container shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`} type="button">
                Todos
              </button>
<button onClick={() => setFormat(1)} aria-pressed={format === 1} className={`format-toggle-btn px-space-md py-1.5 rounded font-label-md text-label-md transition-all ${format === 1 ? 'bg-primary-container text-on-primary-container shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`} type="button">
                PDF (Apostilas e Livros)
              </button>
<button onClick={() => setFormat(2)} aria-pressed={format === 2} className={`format-toggle-btn px-space-md py-1.5 rounded font-label-md text-label-md transition-all ${format === 2 ? 'bg-primary-container text-on-primary-container shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`} type="button">
                Word (Resumos Editáveis)
              </button>
{[{ id: 3, label: 'Vídeos' }, { id: 4, label: 'Outros' }].map(item => <button key={item.id} onClick={() => setFormat(item.id)} aria-pressed={format === item.id} className={`format-toggle-btn px-space-md py-1.5 rounded font-label-md text-label-md transition-all ${format === item.id ? 'bg-primary-container text-on-primary-container shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`} type="button">{item.label}</button>)}
</div>
</div>

<div className="flex items-center justify-between xl:justify-end gap-space-lg">
<div className="flex items-center gap-space-xs font-label-md text-label-md text-primary-fixed">
<span className="w-2 h-2 rounded-full bg-primary inline-block"></span>
<span role="status" aria-live="polite">{visibleMaterials.length} {visibleMaterials.length === 1 ? 'material encontrado' : 'materiais encontrados'}</span>
</div>
<button onClick={clearFilters} className="inline-flex items-center gap-1 font-label-md text-label-md text-outline hover:text-on-surface transition-colors cursor-pointer" type="button">
<span className="material-symbols-outlined text-base" aria-hidden="true">restart_alt</span>
<span className="">Limpar Filtros</span>
</button>
</div>
</div>


<div className="discipline-filter">
  <label htmlFor="filter-discipline" className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Disciplina</label>
  <select id="filter-discipline" value={discipline} onChange={event => setDiscipline(event.target.value)} className="h-11 px-space-md bg-surface-container-highest text-on-surface text-body-sm rounded-lg">
    {disciplines.filter((item, index, list) => list.findIndex(other => other.id === item.id) === index).map(item => <option key={item.id} value={item.id}>{item.id === 'pds' ? 'Processo de Desenvolvimento de Software (PDS)' : item.label}</option>)}
  </select>
</div>
<nav className="discipline-bar flex items-center gap-space-xs overflow-x-auto pb-2 pt-space-xs" aria-label="Disciplinas" tabIndex={0}>
  <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider shrink-0 mr-space-xs">Disciplinas:</span>
  {disciplines.map(item => <button key={item.label} onClick={event => { setDiscipline(item.id); event.currentTarget.scrollIntoView({ block: 'nearest', inline: 'nearest' }); }} aria-pressed={discipline === item.id} className={`shrink-0 px-space-md py-1 rounded-full font-label-md text-label-md transition-all ${discipline === item.id ? 'bg-secondary-container text-on-secondary-container' : 'bg-surface-container-highest text-on-surface-variant hover:text-on-surface'}`} type="button">{item.label}</button>)}
</nav>
</div>
</section>


{(notice || storageError) && <p role="status" className="upload-notice">{storageError || notice}</p>}
<MaterialSection id="envios-recentes" title="Seus envios recentes" description="Materiais salvos neste navegador" icon="cloud_done" items={visibleMaterials.filter(item => item.section === 'uploaded')} onShowAll={clearFilters} />
<MaterialSection id="materiais-destaque" title="Materiais em Destaque" description="Conteúdos acadêmicos para aprofundar seus estudos" icon="auto_stories" items={visibleMaterials.filter(item => item.section === 'featured')} onShowAll={clearFilters} />
<MaterialSection id="mais-materiais" title="Mais Lidos & Populares" description="Mais materiais para explorar e estudar" icon="trending_up" items={visibleMaterials.filter(item => item.section === 'popular')} onShowAll={clearFilters} />
{visibleMaterials.length === 0 && <div className="empty-results rounded-xl bg-surface-container-low p-space-xl text-center">
  <span className="material-symbols-outlined text-primary text-3xl" aria-hidden="true">search_off</span>
  <h2 className="font-headline-sm text-headline-sm mt-space-sm">Nenhum material encontrado</h2>
  <p className="text-on-surface-variant text-body-sm mt-space-sm">Tente outra disciplina, professor ou termo de pesquisa.</p>
  <button onClick={clearFilters} className="rounded-lg bg-primary-container text-on-primary-container px-space-lg py-space-sm mt-space-md" type="button">Limpar Filtros</button>
</div>}


</div>
</div>
</main>
</div>


{uploadOpen && <UploadMaterialModal onClose={() => setUploadOpen(false)} onSubmit={async (material, file) => { await addUpload(material, file); clearFilters(); setNotice('Material salvo neste navegador! Ele já está disponível em Seus envios recentes.'); }} />}
</div>);
}
