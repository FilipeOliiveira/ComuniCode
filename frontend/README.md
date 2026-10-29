# COMUNICODE — Frontend

Interface React/Vite integrada à API Spring Boot: login, cadastro de aluno, logout,
restauração da sessão, listagem de materiais e filtros combinados. O acervo usa
somente os registros retornados pelo backend, sem inserir dados de exemplo.

## Executar o projeto

Abra dois terminais na pasta `LAB PROG - GRUPO`.

**1. Backend (JDK 25):** configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` em
`demo/.env` ou no ambiente, conforme [README_API.md](../demo/README_API.md).
Executando na pasta `demo`, o perfil Supabase carrega esse arquivo automaticamente.
Também é possível usar a configuração existente do VS Code.

```powershell
cd demo
.\mvnw.cmd spring-boot:run
```

A API inicia na porta **8081**. O banco precisa ter o schema existente; iniciar
esta aplicação não cria usuários nem popula materiais.

**2. Frontend (Node.js e npm):**

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Abra o endereço informado pelo Vite, normalmente **http://localhost:5173**.
Use o mesmo host ao navegar (localhost ou 127.0.0.1) para preservar o cookie.
Crie uma conta de aluno na tela de cadastro e entre com as credenciais escolhidas.
Professores e administradores existentes também podem entrar; o cadastro público
não permite escolher perfil. Um acervo sem postagens exibe uma mensagem de vazio.

## Como a integração funciona

- O navegador chama `/api` na origem do frontend. O proxy do Vite encaminha as
  chamadas ao backend, inclusive cookies e cabeçalhos CSRF. Não é preciso liberar CORS.
- Cada cadastro, login e logout busca um token em `/api/auth/csrf`. O login envia
  `email` e `senha` como formulário; cadastro usa JSON. Respostas 204 não têm corpo.
- `/api/auth/me` restaura a sessão ao recarregar a página. A sessão também é conferida
  ao voltar à aba. Respostas 401 durante consultas limpam o acervo e exibem o login.
- Senhas e tokens não são gravados no localStorage. A sessão usa o cookie HttpOnly
  emitido pelo backend. Reiniciar o backend encerra suas sessões em memória.
- Disciplinas e postagens vêm de `/api/disciplinas` e `/api/postagens`. Os cartões
  exibem autor, perfil, disciplina, status e todos os anexos. O selo de verificação
  se refere ao material, sem atribuir verificação automaticamente a professores.
- Pesquisa, professor, disciplina e formato são combinados no navegador sobre a
  lista carregada. Professores do filtro são os autores desse perfil presentes no
  acervo. “Meus materiais” seleciona o ID do usuário autenticado.
- Carregamento, falha de conexão, erro de consulta e lista vazia têm estados próprios.
  Use “Atualizar acervo” para buscar novos registros.

Para outro endereço do backend, copie `.env.example` para `.env.local`, ajuste
`API_PROXY_TARGET` e reinicie o Vite. Esse valor é usado apenas pelo proxy;
nunca coloque senha do banco em variáveis do frontend.

## Limites desta etapa

Login, cadastro e listagem estão conectados. Publicação, arquivamento e upload real
ficam para a próxima etapa. O botão de envio está desabilitado com essa indicação;
os antigos componentes de upload local e PDFs de referência continuam no projeto,
mas não são usados como conteúdo da conta nem enviados ao servidor.

A interface exibe o status recebido. A política de visibilidade de rascunhos ainda
é a da API existente: as consultas não filtram por status. Não há paginação nem
implementação de favoritos, notificações, comentários, avaliações ou denúncias.
Os anexos abrem a URL já cadastrada; acesso e download dependem do servidor que
hospeda o arquivo. Datas `LocalDateTime` preservam o dia informado pela API;
datas com fuso são exibidas em America/Fortaleza.

## Validar

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run test:e2e
```

Os testes Node verificam o cliente HTTP, contrato de cadastro, CSRF, filtros,
URLs dos anexos, datas, encaminhamento pelo proxy e arquivos de referência.
Os testes Playwright usam o Edge instalado no Windows, em modo headless, e uma
API simulada: cadastro, login, cookie, renovação de CSRF, logout, recarga, sessão
expirada, erros, filtros e layout de celular. Não gravam dados no Supabase.

Em outros sistemas, instale o Chromium com `npx playwright install chromium`.
Para escolher um navegador instalado no Windows, use por exemplo
`$env:PLAYWRIGHT_CHANNEL = 'chrome'` antes de executar os testes.

Os campos novos da API também são cobertos por `ApiIntegracaoTest` com H2:

```powershell
cd ../demo
.\mvnw.cmd test "-Dtest=ApiIntegracaoTest"
```

## Build e hospedagem

`npm.cmd run build` gera `dist/`. `npm.cmd run preview` permite verificar o build
localmente com o mesmo proxy. Em produção, configure o servidor/reverse proxy para
encaminhar `/api` ao backend na mesma origem e use HTTPS com
`SESSION_COOKIE_SECURE=true`. O proxy do Vite não é incorporado aos arquivos estáticos.
As fontes do Google usam fontes locais como alternativa quando não há internet.
Os ícones das telas integradas são SVGs locais.
