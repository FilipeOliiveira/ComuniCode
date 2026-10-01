# API: autenticacao e operacoes principais

## Executar

Use JDK 25 e configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no ambiente.
O `.env.example` contem apenas placeholders. O VS Code pode carregar `.env` pelas
configuracoes de depuracao; Maven nao carrega esse arquivo automaticamente.

```powershell
.\mvnw.cmd spring-boot:run
```

A API usa a porta 8081. O schema existente deve estar criado no Supabase;
Hibernate apenas valida as tabelas. Nenhuma migracao ou carga e executada por estas rotas.
As senhas anteriormente presentes em arquivos devem ser trocadas no Supabase:
remover do codigo nao revoga a credencial nem remove o historico do Git.

## Login por sessao

1. `GET /api/auth/csrf` retorna `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`.
   Preserve o cookie de sessao recebido.
2. `POST /api/auth/login` recebe **application/x-www-form-urlencoded** com campos
   `email` e `senha`, cookie e cabecalho CSRF. Sucesso: 204; credenciais invalidas
   ou usuario inativo: 401 com a mesma mensagem.
3. Busque um novo token em `/api/auth/csrf` depois do login: o token anterior expira.
4. Envie o cookie em todas as chamadas e o token nos metodos POST, PATCH e DELETE.
5. `GET /api/auth/me` retorna id, nome, email, perfil e ativo; nunca senha ou hash.
6. `POST /api/auth/logout` com CSRF invalida a sessao e retorna 204.
   Busque outro token antes de um novo login/cadastro.

Exemplo JavaScript com frontend servido na mesma origem (ou proxy de desenvolvimento):

```javascript
let csrf = await fetch('/api/auth/csrf', { credentials: 'include' }).then(r => r.json());
const login = await fetch('/api/auth/login', {
  method: 'POST',
  credentials: 'include',
  headers: { [csrf.headerName]: csrf.token },
  body: new URLSearchParams({ email, senha })
});
if (!login.ok) throw new Error('Falha no login');
csrf = await fetch('/api/auth/csrf', { credentials: 'include' }).then(r => r.json());
const usuario = await fetch('/api/auth/me', { credentials: 'include' }).then(r => r.json());
```

Sessao em memoria, expirada apos 30 minutos de inatividade. Reiniciar a aplicacao
encerra as sessoes. Cookies HttpOnly e SameSite=Lax; em HTTPS de producao use
`SESSION_COOKIE_SECURE=true`. CORS entre origens ainda nao foi habilitado: configure
uma origem explicita quando houver um frontend separado. As permissoes sao
carregadas no login; alteracoes de perfil exigem nova sessao.

## Rotas

Salvo o login, os corpos de requisicao sao JSON. Campos desconhecidos sao rejeitados
com 400, incluindo tentativas de enviar `criador`, `senhaHash`, `perfil` ou pontuacao.
Todo POST requer CSRF, inclusive o cadastro publico.

| Metodo e rota | Acesso | Corpo ou filtros |
| --- | --- | --- |
| GET /api/auth/csrf | Publico | Token CSRF |
| POST /api/auth/cadastro/aluno | Publico | nome, email, senha, matricula, semestreAtual |
| POST /api/auth/cadastro/professor | Admin | nome, email, senha, registro, titulacao opcional |
| POST /api/auth/login | Publico | Formulario email e senha |
| GET /api/auth/me | Autenticado | Usuario atual |
| POST /api/auth/logout | Sessao | Sem corpo |
| GET /api/disciplinas | Autenticado | Filtro opcional nome |
| GET /api/disciplinas/{id} | Autenticado | UUID |
| POST /api/disciplinas | Admin | nome, codigo, descricao, cargaHoraria, periodo |
| GET /api/conteudos | Autenticado | disciplinaId obrigatorio |
| GET /api/conteudos/{id} | Autenticado | UUID |
| POST /api/conteudos | Professor ou admin | titulo, descricao, ordem opcional, disciplinaId |
| GET /api/tags | Autenticado | Lista |
| GET /api/tags/busca | Autenticado | nome obrigatorio |
| POST /api/tags | Professor ou admin | nome |
| GET /api/postagens | Autenticado | Um filtro opcional: conteudoId, autorId ou tagId |
| GET /api/postagens/{id} | Autenticado | UUID |
| POST /api/postagens | Autenticado | Exemplo abaixo |
| PATCH /api/postagens/{id} | Autor ou admin | titulo obrigatorio, descricao opcional |
| DELETE /api/postagens/{id} | Autor ou admin | Sem corpo |
| GET /actuator/health | Publico | Saude, sem detalhes internos |

Criacoes retornam 201; exclusao e logout, 204. Erros de negocio retornam
`{ "status": 400, "mensagem": "..." }`, com 400 para dados invalidos, 401 para
autenticacao, 403 para permissao/CSRF, 404 para recurso ausente e 409 para conflito.

```json
{
  "titulo": "Material de Java",
  "descricao": "Resumo da aula",
  "conteudoId": "UUID_DO_CONTEUDO",
  "tagsIds": ["UUID_DA_TAG"],
  "anexos": [{
    "nome": "Resumo.pdf",
    "url": "https://exemplo.com/resumo.pdf",
    "tipo": "PDF",
    "tamanho": 1024
  }]
}
```

Anexos sao opcionais e representam metadados de arquivos ja hospedados; upload,
autorizacao de download e exclusao de objetos no Storage ainda nao existem.
O servidor define o autor pela sessao e inicia a postagem como RASCUNHO. As consultas
preservam a regra atual dos services, sem filtro de visibilidade por status; a politica
de publicacao/rascunhos ainda precisa ser definida. As listas ainda nao sao paginadas.

Para evitar autoatribuicao de privilegios, professores sao cadastrados por admin
e recebem peso inicial 1. O primeiro administrador deve ser provisionado por um
responsavel no banco (usuario ativo com hash BCrypt e registro correspondente em
administrador); nao existe rota publica nem senha padrao para criar administrador.

## Testes e limites desta etapa

```powershell
.\mvnw.cmd test
```

A suite usa mocks e H2 isolado. `ApiIntegracaoTest` exercita filtros de seguranca,
login real, sessao, CSRF, permissoes, DTOs e persistencia sem transacao envolvendo
o teste, para detectar problemas de serializacao com `open-in-view=false`.
Classes `*SupabaseIT` continuam fora da execucao comum. Nao rode o cadastro permanente
para validar a API: ele grava registros reais. Veja `TESTES_INTEGRACAO.md`.

Ainda ficam para as proximas etapas: migracoes do banco existente, CI, deploy,
revogacao de sessoes ao alterar contas, paginacao e funcionalidades restantes.

Referencias da implementacao:
- [Login por formulario e sessao](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html)
- [Protecao CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
