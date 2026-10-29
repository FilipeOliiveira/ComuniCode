import React, { useEffect, useState } from 'react';
import Icon from './Icon.jsx';
import { filterCatalog, loadCatalog } from '../data/catalog.js';
import MaterialSection from './MaterialSection.jsx';

const formats = [{ id: 0, label: 'Todos' }, { id: 1, label: 'PDF' }, { id: 2, label: 'Word' }, { id: 3, label: 'Vídeos' }, { id: 4, label: 'Outros' }];
const profiles = { ALUNO: 'Aluno', PROFESSOR: 'Professor', ADMIN: 'Administrador' };

export default function CatalogPage({ client, user, onSignedOut }) {
  const [catalog, setCatalog] = useState({ materials: [], disciplines: [], professors: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [leaving, setLeaving] = useState(false);
  const [view, setView] = useState('all');
  const [format, setFormat] = useState(0);
  const [discipline, setDiscipline] = useState('Todas');
  const [professor, setProfessor] = useState('todos');
  const [search, setSearch] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    loadCatalog(client, controller.signal).then(data => {
      if (!controller.signal.aborted) setCatalog(data);
    }).catch(failure => {
      if (controller.signal.aborted) return;
      if (failure.status === 401) onSignedOut('Sua sessão expirou. Entre novamente.');
      else setError(failure.message);
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [client, refresh, onSignedOut]);

  async function logout() {
    if (leaving) return;
    setLeaving(true);
    setActionError('');
    try { await client.logout(); onSignedOut('Você saiu da sua conta.'); }
    catch (failure) {
      if (failure.status === 401) onSignedOut('Sua sessão expirou. Entre novamente.');
      else { setActionError('Não foi possível sair. Tente novamente.'); setLeaving(false); }
    }
  }

  const clearFilters = () => { setFormat(0); setDiscipline('Todas'); setProfessor('todos'); setSearch(''); };
  const changeView = next => { setView(next); clearFilters(); };
  const visible = filterCatalog(catalog.materials, { format, discipline, professor, search, ownerId: view === 'mine' ? user.id : null });
  const disciplines = [{ id: 'Todas', label: 'Todas as disciplinas' }, ...catalog.disciplines];
  const initials = user.nome.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('');

  return <div className="app-shell bg-background font-body-md text-on-surface antialiased min-h-screen">
    <header className="fixed top-0 left-0 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.35)] z-50">
      <div className="h-16 w-full px-gutter flex items-center justify-between gap-space-lg">
        <div className="flex items-center gap-space-sm w-64 shrink-0"><span className="brand-mark" aria-hidden="true">&lt;/&gt;</span><span>COMUNICODE</span><span className="rounded-full bg-secondary-container text-on-secondary-container px-2 py-1">ACADÊMICO</span></div>
        <div className="flex-1 max-w-2xl"><div className="relative flex items-center">
          <Icon name="search" className="absolute left-space-md text-outline" />
          <input value={search} onChange={event => setSearch(event.target.value)} className="w-full h-10 pl-11 pr-4 bg-surface-container-low rounded-lg text-body-sm" aria-label="Pesquisar materiais" placeholder="Pesquisar por títulos, autores, disciplinas…" type="search" />
        </div></div>
        <div className="flex items-center gap-space-md shrink-0">
          <span className="author-avatar" title={`${user.nome} • ${profiles[user.perfil] || user.perfil}`} aria-label={`Conta de ${user.nome}`}>{initials}</span>
          <button onClick={logout} disabled={leaving} className="card-action bg-surface-container-highest" aria-label="Sair da conta"><Icon name="logout" /><span>{leaving ? 'Saindo…' : 'Sair'}</span></button>
        </div>
      </div>
    </header>
    <aside className="fixed left-0 top-16 bottom-0 w-64 bg-surface-container-low z-40 flex flex-col justify-between py-space-md shadow-[1px_0_12px_rgba(0,0,0,0.3)]">
      <div className="flex-1 flex flex-col gap-space-lg px-space-md">
        <p className="font-label-sm text-outline uppercase tracking-wider px-space-sm">Repositório</p>
        <nav className="flex flex-col gap-space-xs" aria-label="Navegação principal">
          {[{ id: 'all', icon: 'explore', label: 'Explorar acervo' }, { id: 'mine', icon: 'folder_shared', label: 'Meus materiais' }].map(item => <a key={item.id} href="#acervo" onClick={() => changeView(item.id)} aria-label={item.label} aria-current={view === item.id ? 'page' : undefined} className={`flex items-center gap-space-md px-space-md py-space-sm rounded-lg ${view === item.id ? 'bg-secondary-container text-on-secondary-container' : 'text-on-surface-variant hover:bg-surface-container-high'}`}><Icon name={item.icon} /><span>{item.label}</span></a>)}
        </nav>
      </div>
      <div className="px-space-lg"><p className="text-body-sm text-outline">{user.nome}<br />{profiles[user.perfil]}</p></div>
    </aside>
    <div className="pl-64">
      <main className="relative min-h-screen w-full px-gutter-lg py-space-xl">
        <section className="mb-space-xl mt-space-lg">
          <div className="rounded-2xl bg-gradient-to-br from-surface-container-high via-surface-container to-surface-container-lowest p-space-xl shadow-xl">
            <p className="text-primary text-label-md uppercase tracking-wider">Comunidade acadêmica</p>
            <h1 className="font-display-lg text-display-lg mt-space-md tracking-tight">Olá, {user.nome.split(' ')[0]}! Vamos aprender algo novo?</h1>
            <p className="text-on-surface-variant mt-space-md">Explore os materiais compartilhados pela comunidade.</p>
            <div className="flex flex-wrap gap-space-md mt-space-lg">
              <a href="#acervo" onClick={() => changeView('all')} className="card-action bg-primary-container text-on-primary-container px-space-lg">Explorar acervo</a>
              <button disabled className="card-action bg-surface-container-highest" aria-describedby="upload-next">Enviar material</button>
            </div>
            <p id="upload-next" className="text-body-sm text-outline mt-space-sm">O envio de arquivos estará disponível em breve.</p>
          </div>
        </section>
        {actionError && <p role="alert" className="upload-notice">{actionError}</p>}
        <section id="acervo" className="mb-space-xl" aria-label="Filtros do acervo">
          <div className="rounded-xl bg-surface-container-low p-space-lg flex flex-col gap-space-md">
            <div className="catalog-filters">
              <label className="upload-field">Professor<select value={professor} onChange={event => setProfessor(event.target.value)}><option value="todos">Todos os professores</option>{catalog.professors.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
              <label className="upload-field">Disciplina<select value={discipline} onChange={event => setDiscipline(event.target.value)}>{disciplines.map(item => <option key={item.id} value={item.id}>{item.label}</option>)}</select></label>
              <div className="flex flex-wrap items-end gap-space-xs" role="group" aria-label="Formato do arquivo">
                {formats.map(item => <button key={item.id} onClick={() => setFormat(item.id)} aria-pressed={format === item.id} className={`format-toggle-btn card-action ${format === item.id ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-highest'}`}>{item.label}</button>)}
              </div>
            </div>
            <div className="flex items-center justify-between flex-wrap gap-space-md">
              <span role="status" aria-live="polite" className="text-primary text-body-sm">{loading ? 'Carregando materiais…' : error ? 'Acervo indisponível' : `${visible.length} ${visible.length === 1 ? 'material encontrado' : 'materiais encontrados'}`}</span>
              <div className="flex gap-space-md"><button className="text-body-sm text-primary" onClick={clearFilters}>Limpar filtros</button><button className="text-body-sm text-primary" disabled={loading} onClick={() => setRefresh(value => value + 1)}>Atualizar acervo</button></div>
            </div>
          </div>
        </section>
        {error && <div className="upload-notice"><p role="alert">{error}</p><button className="card-action bg-primary-container text-on-primary-container mt-space-md" onClick={() => setRefresh(value => value + 1)}>Tentar novamente</button></div>}
        {!loading && !error && <>
          <MaterialSection id="materiais" title={view === 'mine' ? 'Meus materiais' : 'Acervo da comunidade'} description={view === 'mine' ? 'Materiais vinculados à sua conta' : 'Mais recentes primeiro'} icon="auto_stories" items={visible} />
          {visible.length === 0 && <div className="empty-results rounded-xl bg-surface-container-low p-space-xl text-center">
            <Icon name="search_off" className="text-primary text-3xl" />
            <h2 className="font-headline-sm text-headline-sm mt-space-sm">{catalog.materials.length === 0 ? 'O acervo ainda está vazio' : 'Nenhum material encontrado'}</h2>
            <p className="text-on-surface-variant text-body-sm mt-space-sm">{catalog.materials.length === 0 ? 'Os materiais cadastrados aparecerão aqui.' : 'Tente outra disciplina, professor ou termo de pesquisa.'}</p>
            <button onClick={clearFilters} className="card-action bg-primary-container text-on-primary-container mt-space-md mx-auto">Limpar filtros</button>
          </div>}
        </>}
      </main>
    </div>
  </div>;
}
