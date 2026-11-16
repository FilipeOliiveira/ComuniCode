# ComuniCode

> Portal acadêmico colaborativo para alunos e professores de Ciência da Computação (UFMA) compartilharem materiais de estudo.

Este documento descreve o estado atual da branch `main`.

---

## 1. O que é o projeto

O **ComuniCode** é um repositório de materiais acadêmicos. Alunos e professores publicam, encontram e avaliam conteúdos ligados às disciplinas do curso, como slides, PDFs, documentos, imagens e vídeos.

| Funcionalidade | Situação |
|----------------|----------|
| Cadastro de aluno, login, logout e sessão | ✅ Implementado (backend + frontend) |
| Cadastro de professor (feito por admin) | ✅ Backend |
| Disciplinas, conteúdos e tags | ✅ Backend (listagem consumida pelo frontend) |
| Postagens com anexos e tags (criar, editar, excluir) | ✅ Backend |
| Acervo com busca e filtros combinados | ✅ Frontend integrado à API |
| Upload real de arquivos / publicação pela interface | ⏳ Próxima etapa |
| Comentários, avaliações, denúncias, ranking, notificações | ⏳ Modelados no banco, ainda sem API |

## 2. Arquitetura

```
ComuniCode/
├── demo/                         # Backend: Spring Boot + PostgreSQL (Supabase)
│   ├── database/criar_banco_portal.sql
│   ├── src/main/java/com/example/demo/
│   │   ├── config/               # SecurityConfig, TesteBancoConfig
│   │   ├── controller/           # Endpoints REST + tratamento de erros
│   │   ├── dto/                  # Objetos de entrada/saída da API
│   │   ├── exception/            # Exceções de negócio
│   │   ├── model/                # Entidades JPA e enums
│   │   ├── repository/           # Spring Data JPA
│   │   ├── security/             # Usuário autenticado e UserDetailsService
│   │   └── services/             # Regras de negócio e permissões
│   ├── src/test/java/...         # Testes unitários, H2 e integração com Supabase
│   ├── README_API.md             # Referência da API
│   ├── README_SUPABASE.md        # Configuração do banco
│   └── TESTES_INTEGRACAO.md      # Testes contra o Supabase
├── frontend/                     # Interface: React + Vite + Tailwind CSS
│   ├── src/api/client.js         # Cliente HTTP (sessão + CSRF)
│   ├── src/components/           # AuthPage, CatalogPage, MaterialCard...
│   ├── tests/                    # node:test e Playwright (tests/browser)
│   └── vite.config.js            # Proxy /api → backend
├── .vscode/launch.json           # Configurações de execução do backend
└── docs/                         # Esta documentação
```

```
Navegador ──► Vite (5173) ──proxy /api──► Spring Boot (8081) ──JDBC──► PostgreSQL (Supabase)
             React + Tailwind            Security + JPA                 14 tabelas, RLS ativo
```

### 2.1 Backend (`demo/`)

| Item | Tecnologia |
|------|------------|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Actuator) |
| Banco | PostgreSQL no Supabase (`ddl-auto=validate`: o Hibernate não altera o schema) |
| Autenticação | Spring Security com login por formulário, sessão HTTP e CSRF |
| Senhas | Hash BCrypt |
| Testes | JUnit + Mockito + H2. Classes `*SupabaseIT` usam o banco real |
| Porta | `8081` |

**Camadas**

- **model**: `Usuario` (abstrata, herança `JOINED`) → `Aluno`, `Professor`, `Administrador`; além de `Disciplina`, `Conteudo`, `Postagem`, `Anexo` e `Tag`. Enums:
  - `StatusPostagem` (RASCUNHO, PUBLICADA, ARQUIVADA)
  - `StatusComentario` (ATIVO, OCULTO, DELETADO)
  - `StatusDenuncia` (PENDENTE, ANALISADA, REJEITADA)
  - `MotivoDenuncia` (SPAM, OFENSIVO, PLAGIO, OUTRO)
  - `TipoAnexo` (PDF, IMAGEM, VIDEO, DOCUMENTO)
- **services**: `UsuarioService`, `DisciplinaService`, `ConteudoService`, `TagService`, `PostagemService` e `PostagemApiService`. Aplicam as regras de negócio e de permissão (por exemplo, só o autor ou um admin edita uma postagem).
- **controller**: `AuthController`, `DisciplinaController`, `ConteudoController`, `TagController` e `PostagemController`. O `ApiExceptionHandler` padroniza os erros como `{ "status", "mensagem" }`.
- **dto**: as entradas rejeitam campos desconhecidos, o que impede, por exemplo, enviar `perfil` ou `senhaHash`. As respostas nunca expõem senha nem e-mail do autor.

**Principais rotas** (referência completa em [`demo/README_API.md`](../demo/README_API.md))

| Rota | Acesso |
|------|--------|
| `GET /api/auth/csrf`, `POST /api/auth/login`, `POST /api/auth/cadastro/aluno` | Público |
| `GET /api/auth/me`, `POST /api/auth/logout` | Sessão |
| `POST /api/auth/cadastro/professor`, `POST /api/disciplinas` | Admin |
| `POST /api/conteudos`, `POST /api/tags` | Professor ou admin |
| `GET /api/disciplinas`, `/api/conteudos`, `/api/tags`, `/api/postagens` | Autenticado |
| `POST /api/postagens` | Autenticado (a postagem começa como RASCUNHO) |
| `PATCH` / `DELETE /api/postagens/{id}` | Autor ou admin |
| `GET /actuator/health` | Público |

A sessão fica em memória e expira após 30 minutos sem uso. O cookie é HttpOnly com SameSite=Lax. Não existe rota pública para criar administrador: o primeiro admin precisa ser cadastrado direto no banco.

### 2.2 Banco de dados

O script `demo/database/criar_banco_portal.sql` cria **14 tabelas** no schema `public`:

| Grupo | Tabelas |
|-------|---------|
| Usuários (herança JOINED) | `usuario`, `aluno`, `professor`, `administrador` |
| Estrutura acadêmica | `disciplina`, `conteudo`, `professor_disciplina` |
| Publicações | `postagem`, `anexo`, `tag`, `postagem_tag` |
| Interação e moderação | `comentario`, `avaliacao`, `denuncia` |

- Chaves primárias são `UUID`, e as subclasses de usuário compartilham a PK com `usuario`.
- O **RLS está habilitado** e o acesso das roles `anon`/`authenticated` foi revogado. Todo acesso passa pelo backend.
- A autenticação é feita pelo Spring Security, não pelo Supabase Auth.

### 2.3 Frontend (`frontend/`)

| Item | Tecnologia |
|------|------------|
| UI | React 19 |
| Build/dev server | Vite 6, com proxy de `/api` para `http://127.0.0.1:8081` |
| Estilo | Tailwind CSS 3, tema escuro |
| Testes | `node --test` (unitários) e Playwright (ponta a ponta, com API simulada) |

Fluxo da interface:

1. **`App.jsx`** consulta `/api/auth/me` para restaurar a sessão. Se a resposta for 401, mostra a tela de login. A sessão é verificada de novo sempre que a aba volta ao foco.
2. **`AuthPage`** cuida do login e do cadastro de aluno, buscando um token CSRF antes de cada envio.
3. **`CatalogPage`** carrega disciplinas e postagens da API e oferece busca e filtros por professor, disciplina e formato, além de "Meus materiais", logout e "Atualizar acervo". Também tem estados próprios para carregamento, erro e lista vazia.
4. **`api/client.js`** centraliza as chamadas com `credentials: 'include'` e o cabeçalho CSRF.

Senhas e tokens não são gravados no `localStorage`; a sessão depende apenas do cookie emitido pelo backend. O botão "Enviar Material" está desabilitado até a próxima etapa. Os componentes antigos de upload local (IndexedDB) e os PDFs de exemplo ainda estão no projeto, mas não são usados no acervo.

## 3. Como executar

São necessários **dois terminais**.

**Backend** (JDK 25, com o schema já criado no Supabase):

```powershell
cd demo
copy .env.example .env    # preencha DB_URL, DB_USERNAME e DB_PASSWORD
.\mvnw.cmd spring-boot:run
```

**Frontend** (Node.js):

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev           # abre em http://localhost:5173
```

Use sempre o mesmo host (`localhost` **ou** `127.0.0.1`) para que o cookie de sessão funcione.

**Testes**

```powershell
cd demo;     .\mvnw.cmd test        # unitários + H2 (não acessa o Supabase)
cd frontend; npm.cmd test           # unitários
cd frontend; npm.cmd run test:e2e   # Playwright
```

Os testes `ServicesSupabaseIT` (com rollback) e `CadastroPermanenteSupabaseIT` (grava dados de verdade) rodam só quando chamados explicitamente. Veja [`demo/TESTES_INTEGRACAO.md`](../demo/TESTES_INTEGRACAO.md).

## 4. Histórico do repositório

Repositório remoto: `github.com/FilipeOliiveira/ComuniCode`

| Data | Autor | Commit |
|------|-------|--------|
| 2026-09-17 | João | Configura projeto Spring Boot com Supabase |
| 2026-09-17 | João Victor | Add database connection string and agent skills info |
| 2026-09-20 | Hyago | Criação das classes do projeto |
| 2026-09-29 | Hyago | Adiciona serviços e repositórios de usuário, disciplina e postagem |
| 2026-09-30 | João | Atualiza services e testes de integração |
| 2026-09-30 | João | Implementa conteúdos, tags, consultas e edição de postagens |
| 2026-10-01 | João | Implementa autenticação por sessão e API com permissões |
| 2026-10-28 | Ana Luiza | Implementa primeira versão da interface *(branch `feature/interface-comunicode`)* |
| 2026-10-29 | João | Integra frontend com autenticação e acervo da API |
| 2026-10-29 | João | Atualiza branch com a versão local |
| 2026-11-16 | — | Remove logs gerados e adiciona `.gitignore` |

A `main` foi atualizada por fast-forward até a `minha-nova-versao`, que já inclui e expande a interface criada em `feature/interface-comunicode`.

## 5. Próximos passos

- Upload real de arquivos (Supabase Storage) e publicação de materiais pela interface.
- API e telas para comentários, avaliações, denúncias e moderação.
- Política de visibilidade de rascunhos e paginação das listagens.
- Ranking por relevância e regra para verificar materiais.
- Migrações versionadas do banco, CI e deploy com HTTPS (`SESSION_COOKIE_SECURE=true`).
- Uma role dedicada no banco para o backend em produção, em vez de `postgres`.

## 6. Equipe

Contribuidores no histórico de commits: João Victor de Oliveira Carneiro, Ana Luiza Oliveira Montelo, Hyago e Filipe Oliveira.
