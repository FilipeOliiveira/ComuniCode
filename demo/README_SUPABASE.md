# Projeto adaptado ao Supabase

Mantidos Spring Boot 4.1.1, Java 25, Maven Wrapper, pacote com.example.demo,
porta 8081, DemoApplication e OlaController. O objetivo desta entrega é testar
conexão, escrita e leitura no schema entregue anteriormente. Não implementa login
nem CRUD HTTP completo; o Spring Security existente continua ativo.

## 1. Preparar o banco

Se ainda não criou as tabelas, execute `database/criar_banco_portal.sql` inteiro
no SQL Editor do seu Supabase. Se já executou o mesmo script, não execute de novo.
O SQL tem 14 tabelas e documentação das decisões de modelagem; a aplicação não
executa esse arquivo automaticamente. Ele foi incluído sem alterações em relação
à entrega anterior. Para configurações/comandos deste projeto, siga este README,
pois os comentários do SQL descrevem também outras formas de integração.

## 2. Configurar conexão

No Supabase, abra Connect > JDBC. Em rede IPv4, use Session pooler (porta 5432).
Copie host, porta e usuário exatamente: o usuário do pooler normalmente inclui
o identificador do projeto. Para conexão direta, a rede deve alcançar o endereço
do banco (normalmente IPv6) e o usuário geralmente é postgres.

Variáveis necessárias:

- DB_URL: `jdbc:postgresql://HOST:PORTA/postgres?sslmode=require`
- DB_USERNAME: usuário de banco informado na conexão escolhida.
- DB_PASSWORD: senha do banco, não a senha da conta nem uma chave da API.

O SQL anterior habilita RLS sem liberar acesso pela Data API. Para o teste inicial,
use as credenciais JDBC postgres do projeto apenas no backend. Um usuário de banco
personalizado precisa de grants e políticas RLS compatíveis. Não desative RLS para
resolver erro de permissão. Em produção, planeje um papel específico para o backend.

### VS Code

1. Abra a pasta `LAB PROG - GRUPO` ou a subpasta `demo`.
2. Copie `demo/.env.example` para `demo/.env` e preencha os valores reais.
3. Confirme que o JDK 25 está configurado e recarregue o projeto Maven.
4. Em Executar e Depurar, selecione **Demo - Testar banco (insere 5 registros)**.
5. Execute. Para inicialização posterior sem carga, use **Demo - Supabase (sem carga)**.

As duas configurações de launch.json apontam ao mesmo demo/.env, inclusive quando
a subpasta demo é aberta diretamente. O arquivo .env é lido pelo depurador Java;
Spring Boot/Maven não carregam .env automaticamente no terminal.

### Terminal PowerShell

Na pasta `demo`, que contém pom.xml e mvnw.cmd:

```powershell
java -version
$env:DB_URL="jdbc:postgresql://SEU_HOST:5432/postgres?sslmode=require"
$env:DB_USERNAME="USUARIO_COPIADO_DO_SUPABASE"
$senhaBanco = Read-Host "Senha do banco" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaBanco).Password
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=teste-banco"
```

A senha é solicitada sem aparecer na tela/histórico do comando. O Java deve mostrar
versão 25. `JAVA_HOME`, se definido, também deve apontar ao JDK 25.

Depois de parar com Ctrl+C, para iniciar sem inserir dados:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS: configure as mesmas variáveis de ambiente e execute
`./mvnw spring-boot:run -Dspring-boot.run.profiles=teste-banco`.

## 3. Resultado do teste manual

O perfil teste-banco inclui supabase por meio de um grupo de perfis.
A classe config/TesteBancoConfig faz:

1. SELECT current_database(), confirmando a conexão.
2. INSERT em usuario (fictício, inativo, senha aleatória com hash BCrypt).
3. INSERT em aluno usando o mesmo UUID de usuario, conforme herança JOINED.
4. INSERT em disciplina, conteudo e postagem respeitando as FKs.
5. Commit da transação e SELECT com JOIN para ler o conjunto persistido.

Se um INSERT falhar, os cinco INSERTs da execução são revertidos. Depois do commit,
uma eventual falha na consulta de confirmação não desfaz os registros já gravados.
Cada execução com esse perfil cria um novo conjunto de cinco registros.
Não há carga automática quando o perfil teste-banco está desativado.

Mensagem esperada:

```text
Conexao PostgreSQL estabelecida. Banco: postgres
TESTE CONCLUIDO: 5 registros inseridos e leitura confirmada apos commit.
```

O console informa os UUIDs. Confira também no SQL Editor:

```sql
SELECT p.id, u.nome AS aluno, a.matricula, d.nome AS disciplina,
       c.titulo AS conteudo, p.titulo AS postagem, p.status
FROM public.postagem p
JOIN public.usuario u ON u.id = p.usuario_id
JOIN public.aluno a ON a.id = u.id
JOIN public.conteudo c ON c.id = p.conteudo_id
JOIN public.disciplina d ON d.id = c.disciplina_id
WHERE u.email LIKE 'teste.banco.%@example.invalid'
ORDER BY p.data_criacao DESC;
```

O status RASCUNHO é um valor de exemplo aceito pelo SQL fornecido. Ajuste a constante
STATUS_POSTAGEM se futuramente definir enums/CHECKs com valores diferentes.
O usuário inativo não serve para login. Testar banco não requer abrir o navegador.
A rota `/` continua sob a segurança original; uma tela de login ou 401 não significa
que a conexão JDBC falhou. Não foram criadas rotas que exponham credenciais.

## 4. Testes automatizados

```powershell
.\mvnw.cmd test
```

O teste contextLoads existente foi isolado com o perfil test e banco H2 em memória.
Não exige credenciais, não acessa Supabase e não executa a carga de teste. H2 verifica
a inicialização do contexto, não a compatibilidade do schema PostgreSQL. A validação
real de escrita/leitura no Supabase é a execução manual descrita acima.

## 5. Alterações por arquivo

| Arquivo | Alteração e motivo |
| --- | --- |
| pom.xml | Removida duplicação do Actuator. Adicionados starter-data-jpa (JPA, JDBC e transações), driver PostgreSQL runtime e H2 somente para testes. Spring Security já fornece BCrypt, sem dependência duplicada. Versões originais preservadas. |
| src/main/TesteBancoConfig.java | Removido arquivo vazio fora do diretório de fontes Java do Maven. |
| src/main/java/com/example/demo/config/TesteBancoConfig.java | Classe implementada no pacote correto, com perfil explícito, SQL parametrizado, transação e leitura após commit. |
| src/main/resources/application.properties | Preservados nome e porta. Definido perfil padrão supabase, grupo teste-banco e desativada inicialização automática por SQL. |
| src/main/resources/application-supabase.properties | Credenciais por variáveis de ambiente, pool limitado a cinco conexões, Hibernate validate, UTC e open-in-view desativado. |
| src/main/resources/application-teste-banco.properties | Configuração exclusiva do teste JDBC, sem DDL Hibernate nem migrações automáticas. |
| src/test/java/com/example/demo/DemoApplicationTests.java | Adicionado @ActiveProfiles("test") para isolar o teste de inicialização. |
| src/test/resources/application-test.properties | H2 em memória exclusivo dos testes, sem criar schema de produção ou consultar Supabase. |
| .env.example | Modelo de configuração sem credenciais reais. |
| .gitignore | Proteção para arquivos .env, mantendo .env.example versionável. |
| ../.vscode/launch.json | Duas opções de execução, com cwd demo e leitura de demo/.env. |
| .vscode/launch.json | Mesmas opções para quem abre apenas a pasta demo no VS Code. |
| database/criar_banco_portal.sql | Cópia do SQL já entregue para manter a referência junto ao código. |
| README_SUPABASE.md | Guia de execução, resultado esperado, explicação das alterações e resolução de erros. |

Arquivos compilados antigos em target/ foram excluídos do ZIP de entrega; Maven
os recria. Os demais arquivos originais foram preservados.

## 6. Erros comuns

- `release version 25 not supported`: Maven está usando JDK anterior ao 25.
- `Could not resolve placeholder DB_URL`: preencha as variáveis no ambiente; no
  VS Code, confira o demo/.env. No terminal, apenas criar .env não é suficiente.
- `password authentication failed`: confira senha DO BANCO e usuário da conexão.
- `UnknownHost`, `Network is unreachable` ou timeout: confira host, rede e opção
  Session pooler para IPv4. Nunca use a URL HTTPS da API como URL JDBC.
- `relation public.usuario does not exist`: execute o SQL no mesmo projeto/banco.
- `permission denied` ou erro RLS: confira papel JDBC e seus privilégios.
- Porta 8081 ocupada: pare a instância anterior antes de iniciar outra.
- Ausência da mensagem de carga: ative teste-banco, não apenas supabase.

Fontes oficiais:
- https://docs.spring.io/spring-boot/reference/data/sql.html
- https://docs.spring.io/spring-boot/reference/features/profiles.html
- https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html
- https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot

## 7. Verificação desta entrega

Foram conferidos o XML do pom.xml, ausência de dependências duplicadas, JSON das
configurações do VS Code, correspondência entre pacotes Java e diretórios e a
preservação de DemoApplication/OlaController. O SQL incluído já havia sido validado
em PostgreSQL embarcado (PGlite) na entrega anterior.

A tentativa de `clean test` neste ambiente parou no download do parent do Spring
Boot: primeiro resolução de rede e depois falha de confiança do certificado do
proxy (PKIX). Portanto, compilação Java e execução do teste automatizado NÃO foram
confirmadas aqui. Execute `mvnw.cmd test` com JDK 25 e acesso ao Maven Central no
seu computador. Não houve conexão ao seu Supabase e nenhum dado foi inserido nele.
