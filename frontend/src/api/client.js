export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function createApi(fetcher = (...args) => fetch(...args)) {
  async function request(path, { headers, ...options } = {}) {
    let response;
    try {
      response = await fetcher(`/api${path}`, {
        ...options, credentials: 'include', cache: 'no-store',
        headers: { Accept: 'application/json', ...headers },
      });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      throw new ApiError('Não foi possível conectar ao servidor. Tente novamente.');
    }
    let data = null;
    if (response.status !== 204 && response.headers.get('content-type')?.includes('application/json')) {
      try { data = await response.json(); }
      catch { throw new ApiError('O servidor retornou uma resposta inválida.', response.status); }
    }
    if (!response.ok) {
      const fallback = response.status === 401 ? 'E-mail ou senha inválidos.'
        : response.status === 403 ? 'Sua sessão mudou ou você não tem permissão. Tente novamente.'
        : 'Não foi possível concluir a solicitação. Tente novamente.';
      throw new ApiError(response.status < 500 && typeof data?.mensagem === 'string' ? data.mensagem : fallback, response.status);
    }
    if (response.status !== 204 && data === null) throw new ApiError('O servidor retornou uma resposta inválida.', response.status);
    return data;
  }

  async function post(path, body, contentType) {
    // Login/logout renovam a sessão/CSRF. Cada ação obtém um token atual.
    const csrf = await request('/auth/csrf');
    if (!csrf?.headerName || !csrf?.token) throw new ApiError('Não foi possível iniciar uma sessão segura. Tente novamente.');
    return request(path, { method: 'POST', body,
      headers: { [csrf.headerName]: csrf.token, ...(contentType ? { 'Content-Type': contentType } : {}) } });
  }

  return {
    me: options => request('/auth/me', options),
    async login(email, senha) {
      await post('/auth/login', new URLSearchParams({ email: email.trim(), senha }), 'application/x-www-form-urlencoded');
      return request('/auth/me');
    },
    register: data => post('/auth/cadastro/aluno', JSON.stringify({
      nome: data.nome.trim(), email: data.email.trim(), senha: data.senha,
      matricula: data.matricula.trim(), semestreAtual: Number(data.semestreAtual),
    }), 'application/json'),
    logout: () => post('/auth/logout'),
    disciplines: options => request('/disciplinas', options),
    posts: options => request('/postagens', options),
  };
}

export const api = createApi();
