# Upload real de arquivos e publicação pela interface

> Branch `feature/upload-arquivos`. Este documento explica o que a funcionalidade faz, as decisões tomadas e como configurá-la e testá-la.

## 1. O que muda para o usuário

Até esta etapa, o botão **Enviar material** ficava desabilitado e a API só aceitava anexos como links para arquivos que já estavam em outro lugar. Agora:

1. Qualquer usuário logado (aluno, professor ou admin) clica em **Enviar material** no acervo.
2. Preenche título, descrição, disciplina, conteúdo e tags, e escolhe (ou arrasta) **de 1 a 5 arquivos**.
3. Ao clicar em **Publicar material**, os arquivos vão para o **Supabase Storage**. A postagem é gravada no banco **já publicada** e aparece no acervo na hora.
4. No cartão do material, **Abrir arquivo** passa pelo backend, que confere a sessão e manda o navegador para um link temporário do arquivo.
5. Quando o autor (ou um admin) exclui a postagem, os arquivos também são apagados do Storage.

## 2. Decisões

| Tema | Decisão | Motivo |
|------|---------|--------|
| Onde guardar | Bucket **privado** no Supabase Storage | O SQL do projeto pede uma URL permanente em `anexo.url` e diz que upload e download são responsabilidade do backend. Num bucket privado, só quem tem login abre os arquivos. |
| Fluxo de envio | **Um único** `POST` multipart (dados + arquivos) | Ou tudo é gravado, ou nada fica pela metade: se o banco recusar a postagem, os arquivos já enviados são apagados. |
| Status | Publicação pela interface cria a postagem **PUBLICADA** | O rascunho continua existindo pela API JSON, como antes. |
| Limites | **Até 5 arquivos de 25 MB** cada | Cabe no limite de 50 MB por arquivo do plano grátis do Supabase. |
| Formatos | pdf, png, jpg/jpeg, webp, mp4, doc, docx, ppt, pptx, odt, txt | O tipo é conferido pelo **conteúdo** do arquivo (assinatura dos primeiros bytes), não só pela extensão. |
| Tags | Continuam **obrigatórias** (pelo menos 1) | Mantém a regra do `PostagemService`. Se não houver tags, o formulário explica que um professor ou admin precisa criá-las. |
| Download | `GET /api/anexos/{id}/arquivo` → **302** para um link assinado de **60 s** | O arquivo não passa pelo backend, e o link vazado expira rápido. |
| Exclusão | Arquivos apagados do Storage **depois do commit** | Se o Storage falhar nesse momento, a postagem continua excluída e a falha vai para o log. |

## 3. Como funciona

```
Navegador                     Spring Boot (8081)                         Supabase
─────────                     ──────────────────                         ────────
Modal "Enviar material"
  │ POST /api/postagens
  │ multipart: dados + arquivos ─► PostagemController.publicar
  │ (cookie + X-CSRF-TOKEN)          │
  │                                  ▼
  │                           PublicacaoService
  │                             1. valida título, conteúdo, tags
  │                             2. ValidadorArquivos: quantidade, tamanho,
  │                                extensão e assinatura dos bytes
  │                             3. para cada arquivo ───────────────────► Storage: POST /object/materiais/postagens/<uuid>.<ext>
  │                             4. transação: grava postagem PUBLICADA ─► Postgres: postagem, anexo, postagem_tag
  │                                + anexos com url = storage://materiais/postagens/<uuid>.<ext>
  │                             ✗ se 4 falhar: apaga os objetos do passo 3
  │ ◄── 201 + PostagemResponse
  │     (anexo.url = /api/anexos/<id>/arquivo)
  │
Clique em "Abrir arquivo"
  │ GET /api/anexos/<id>/arquivo ─► AnexoController → AnexoService
  │                                  confere sessão, lê anexo.url ─────────► Storage: POST /object/sign/... (60 s)
  │ ◄── 302 Location: link assinado
  └──────────────────────────────────────────────────────────────────────► baixa o arquivo direto do Storage
```

### O que fica gravado em `anexo.url`

- **Arquivo enviado pelo portal:** `storage://<bucket>/postagens/<uuid>.<ext>`. Essa referência é **interna**: a API nunca a devolve. No JSON de resposta, o campo `url` vira `/api/anexos/<id>/arquivo`.
- **Link externo antigo** (criado pela rota JSON): continua como `https://...` e é devolvido sem mudança.
- A rota JSON aceita só `http`/`https`. Assim, ninguém consegue gravar uma referência `storage://` que aponte para o arquivo de outra pessoa.
- O nome do objeto no Storage é um UUID gerado pelo servidor. O nome original só fica na coluna `anexo.nome`, depois de limpo (sem caminho de pastas nem caracteres de controle).

**Não houve mudança no schema do banco.** As colunas de `anexo` já existiam, e `ddl-auto=validate` continua.

## 4. Arquivos

### Backend (`demo/src/main/java/com/example/demo`)

| Arquivo | O que faz |
|---------|-----------|
| `storage/ArmazenamentoArquivos.java` | Interface: `enviar`, `excluir` e `linkTemporario`. Nos testes, é substituída por um mock. |
| `storage/SupabaseStorage.java` | Implementação que chama a API REST do Supabase Storage com a chave secreta do `.env`. |
| `storage/ReferenciaArmazenamento.java` | Monta e interpreta `storage://bucket/caminho`. |
| `services/ValidadorArquivos.java` | Regras de quantidade, tamanho, extensão e assinatura dos bytes. |
| `services/PublicacaoService.java` | Orquestra validação → upload → transação → limpeza em caso de erro. |
| `services/AnexoService.java` | Gera o link temporário de um anexo hospedado pelo portal. |
| `services/PostagemApiService.java` | `excluir` agora agenda a remoção dos arquivos depois do commit. |
| `controller/PostagemController.java` | Novo `POST /api/postagens` com `consumes = multipart/form-data`. A rota JSON não mudou. |
| `controller/AnexoController.java` | `GET /api/anexos/{id}/arquivo`. |
| `controller/ApiExceptionHandler.java` | 413 (arquivo grande), 400 (multipart inválido) e 503 (Storage indisponível). |
| `dto/PublicacaoRequest.java`, `dto/AnexoResponse.java` | Dados do envio; a resposta esconde a referência interna. |
| `repository/AnexoRepository.java` | Busca do anexo pelo ID. |
| `exception/ArmazenamentoIndisponivelException.java` | Falha ou falta de configuração do Storage. |
| `resources/application.properties` | Limites de multipart: 25 MB por arquivo, 130 MB por requisição. |

### Frontend (`frontend/src`)

| Arquivo | O que faz |
|---------|-----------|
| `components/PublishMaterialModal.jsx` | Formulário de envio: arquivos (seleção e arrastar), título, descrição, disciplina → conteúdo e tags. |
| `components/CatalogPage.jsx` | Habilita **Enviar material**, mostra o aviso de sucesso e recarrega o acervo. |
| `data/publication.js` | Validação no navegador (mesmas regras do backend) e montagem do `FormData`. |
| `api/client.js` | `contents`, `tags` e `publish` (multipart com CSRF). |
| `data/catalog.js` | Aceita a rota `/api/anexos/<uuid>/arquivo` como link seguro. |

O modal antigo (`UploadMaterialModal`, `useUploads`, `data/uploads.js`, com IndexedDB) continua no projeto, mas fora de uso.

## 5. Configuração (uma vez por projeto Supabase)

1. **Criar o bucket:** no painel do Supabase, abra **Storage → New bucket**. Use o nome `materiais`, deixe **Public bucket desligado** e, se quiser, defina *File size limit* = 25 MB.
2. **Pegar a URL e a chave:** em **Project Settings → API**, copie a **Project URL** (`https://<ref>.supabase.co`) e a chave secreta (**service_role** ou `sb_secret_...`).
3. **Preencher o `demo/.env`** (veja o `demo/.env.example`):
   ```
   SUPABASE_URL=https://<ref>.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=<chave secreta>
   SUPABASE_STORAGE_BUCKET=materiais
   ```
4. **Reiniciar o backend.**

⚠️ A chave secreta ignora o RLS. Ela fica **só** no `demo/.env` (que não é versionado), nunca no frontend nem em commits.

Sem essas variáveis, o backend sobe normalmente. Só o envio e a abertura de arquivos respondem 503 com a mensagem "Envio de arquivos nao configurado no servidor".

**Pré-requisitos no banco:** para publicar, precisam existir uma disciplina (criada por admin), um conteúdo nela (professor ou admin) e pelo menos uma tag (professor ou admin).

## 6. Como testar

### Testes automáticos (não acessam o Supabase)

```powershell
cd demo;     .\mvnw.cmd test         # inclui ValidadorArquivosTest e PublicacaoArquivosTest
cd frontend; npm.cmd test            # inclui publication.test.mjs
cd frontend; npm.cmd run test:e2e    # inclui o fluxo de publicação no Playwright
```

- `PublicacaoArquivosTest` usa H2 e um Storage simulado. Ele cobre publicação, login e CSRF, arquivo falso, campos extras, limpeza quando o banco recusa, Storage fora do ar (503), redirecionamento do download, exclusão e a recusa de `storage://` pela rota JSON.
- O teste Playwright cobre a validação no formulário, a escolha de disciplina e conteúdo, as tags, o erro do servidor, o sucesso e o link do arquivo no acervo.

### Teste manual (com o Supabase configurado)

1. Suba o backend e o frontend. Entre com uma conta.
2. Clique em **Enviar material** e publique um PDF.
3. No Supabase:
   - Em **Storage → materiais → postagens**, o arquivo aparece com um nome UUID.
   - No **SQL Editor**:
     ```sql
     SELECT p.titulo, p.status, a.nome, a.tipo, a.tamanho, a.url
     FROM postagem p JOIN anexo a ON a.postagem_id = p.id
     ORDER BY p.data_criacao DESC LIMIT 5;
     ```
     O `status` deve ser `PUBLICADA` e a `url` deve começar com `storage://materiais/`.
4. No acervo, clique em **Abrir arquivo**: o PDF abre numa nova aba, num endereço `...supabase.co/storage/v1/object/sign/...`. Depois de 60 s, esse link deixa de funcionar.
5. Exclua a postagem (`DELETE /api/postagens/{id}`) e confira que o arquivo sumiu do bucket.

## 7. Limites conhecidos e próximos passos

- Os arquivos são lidos inteiros na memória do backend antes do envio (até 5 × 25 MB por requisição). Para arquivos maiores, o próximo passo seria o upload direto do navegador com URL assinada de upload.
- A interface ainda não tem botões para editar ou excluir postagens. A exclusão existe só pela API.
- Não há barra de progresso por arquivo. O botão mostra "Enviando…" até a resposta.
- Não há antivírus. A validação confere o tipo pelos bytes, mas não analisa o conteúdo.
- Se o Storage falhar ao apagar arquivos (na exclusão ou depois de um erro), o objeto fica órfão e só aparece no log.
