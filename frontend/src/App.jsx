import React, { useCallback, useEffect, useState } from 'react';
import { api } from './api/client.js';
import AuthPage from './components/AuthPage.jsx';
import CatalogPage from './components/CatalogPage.jsx';

export default function App({ client = api }) {
  const [session, setSession] = useState({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);
  const signedOut = useCallback((notice = '') => setSession({ status: 'anonymous', notice }), []);

  useEffect(() => {
    const controller = new AbortController();
    setSession({ status: 'loading' });
    client.me({ signal: controller.signal }).then(user => {
      if (!controller.signal.aborted) setSession({ status: 'authenticated', user });
    }).catch(error => {
      if (controller.signal.aborted) return;
      if (error.status === 401) signedOut();
      else setSession({ status: 'error', error: error.message });
    });
    return () => controller.abort();
  }, [client, attempt, signedOut]);

  useEffect(() => {
    if (session.status !== 'authenticated') return;
    const controller = new AbortController();
    const checkSession = () => client.me({ signal: controller.signal }).then(user => {
      if (!controller.signal.aborted) setSession(current => current.status === 'authenticated' ? { ...current, user } : current);
    }).catch(error => {
      if (!controller.signal.aborted && error.status === 401) signedOut('Sua sessão expirou. Entre novamente.');
    });
    window.addEventListener('focus', checkSession);
    return () => { controller.abort(); window.removeEventListener('focus', checkSession); };
  }, [session.status, client, signedOut]);

  if (session.status === 'loading' || session.status === 'error') return <div className="session-screen text-on-surface">
    <div className="auth-brand"><span className="brand-mark" aria-hidden="true">&lt;/&gt;</span><strong>COMUNICODE</strong></div>
    {session.status === 'loading' ? <p role="status">Verificando sua sessão…</p> : <>
      <p role="alert">{session.error}</p>
      <button className="card-action bg-primary-container text-on-primary-container" onClick={() => setAttempt(value => value + 1)}>Tentar novamente</button>
    </>}
  </div>;
  if (session.status === 'anonymous') return <AuthPage client={client} notice={session.notice} onAuthenticated={user => setSession({ status: 'authenticated', user })} />;
  return <CatalogPage key={session.user.id} client={client} user={session.user} onSignedOut={signedOut} />;
}
