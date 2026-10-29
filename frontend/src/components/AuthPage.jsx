import React, { useState } from 'react';

export default function AuthPage({ client, onAuthenticated, notice = '' }) {
  const [registering, setRegistering] = useState(false);
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setError('');
    if (registering && values.senha !== values.confirmacao) { setError('As senhas não coincidem.'); return; }
    if (registering && new TextEncoder().encode(values.senha).length > 72) { setError('A senha é muito longa. Tente uma senha menor.'); return; }
    setBusy(true);
    try {
      if (registering) {
        await client.register(values);
        setRegistering(false);
        setSuccess('Cadastro concluído! Entre com seu e-mail e senha.');
      } else {
        onAuthenticated(await client.login(values.email, values.senha));
      }
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }

  return <div className="auth-shell text-on-surface">
    <div className="auth-intro">
      <div className="auth-brand"><span className="brand-mark" aria-hidden="true">&lt;/&gt;</span><strong>COMUNICODE</strong></div>
      <p className="text-primary font-label-md uppercase tracking-wider">Seu espaço de conhecimento</p>
      <h1>Aprender fica melhor quando a gente compartilha.</h1>
      <p>Acesse os materiais da comunidade acadêmica e encontre conteúdo para seus estudos.</p>
    </div>
    <section className="auth-panel" aria-labelledby="auth-heading">
      <h2 id="auth-heading">{registering ? 'Crie sua conta de aluno' : 'Entre na sua conta'}</h2>
      <p className="text-on-surface-variant">{registering ? 'Preencha seus dados para acessar o acervo.' : 'Use o e-mail e a senha do seu cadastro.'}</p>
      {notice && !success && <p role="status" className="upload-notice">{notice}</p>}
      {success && <p role="status" className="upload-notice">{success}</p>}
      <form key={registering ? 'register' : 'login'} onSubmit={submit} className="upload-form" aria-busy={busy}>
        <fieldset disabled={busy}>
          {registering && <label className="upload-field">Nome completo<input name="nome" autoComplete="name" required maxLength={255} /></label>}
          <label className="upload-field">E-mail<input name="email" type="email" autoComplete="username" required maxLength={255} value={email} onChange={event => setEmail(event.target.value)} /></label>
          {registering && <div className="auth-fields">
            <label className="upload-field">Matrícula<input name="matricula" required maxLength={255} /></label>
            <label className="upload-field">Semestre atual<input name="semestreAtual" type="number" required min="0" max="2147483647" step="1" defaultValue="1" /></label>
          </div>}
          <label className="upload-field">Senha<input name="senha" type="password" autoComplete={registering ? 'new-password' : 'current-password'} required /></label>
          {registering && <label className="upload-field">Confirmar senha<input name="confirmacao" type="password" autoComplete="new-password" required /></label>}
          {error && <p role="alert" className="upload-error">{error}</p>}
          <button type="submit" className="card-action bg-primary-container text-on-primary-container auth-submit">{busy ? 'Aguarde…' : registering ? 'Criar conta' : 'Entrar'}</button>
        </fieldset>
      </form>
      <button className="auth-switch text-primary" disabled={busy} onClick={() => { setRegistering(!registering); setError(''); setSuccess(''); }}>
        {registering ? 'Já tenho conta — entrar' : 'Ainda não tenho conta — cadastrar'}
      </button>
    </section>
  </div>;
}
