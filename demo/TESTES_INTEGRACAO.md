# Testes de integracao dos services no Supabase

Configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no ambiente antes de executar.
A configuracao nao contem mais credenciais fixas. Use `sslmode=require` na URL,
sem senha embutida. Para os testes locais da API, veja [README_API.md](README_API.md).

## Cadastro permanente de Joao e Geraldo

Para gravar o aluno Joao e o professor Geraldo e consultar depois no painel:

```powershell
.\mvnw.cmd test "-Dtest=CadastroPermanenteSupabaseIT"
```

Este teste faz COMMIT e mantem os registros. No Table Editor, tabela `usuario`,
procure `joao.teste@example.invalid` e `geraldo.teste@example.invalid`.
Os dados especificos ficam em `aluno` (matricula `TESTE-JOAO-001`, semestre 1)
e `professor` (registro `TESTE-GERALDO-001`, peso 1), com os mesmos IDs de usuario.
As senhas sao aleatorias, armazenadas com BCrypt, e nao sao exibidas.
Ao repetir, os cadastros existentes com esses emails sao conferidos e reutilizados,
sem atualizar seus dados. O teste nao roda no comando comum `mvnw.cmd test`.

## Testes com rollback

Na pasta `demo`, execute:

```powershell
.\mvnw.cmd test "-Dtest=ServicesSupabaseIT"
```

No VS Code, abra `src/test/java/com/example/demo/services/ServicesSupabaseIT.java`
e clique em **Run Test** acima da classe ou de um metodo.

A classe ativa os perfis `supabase` e `integracao-supabase` e utiliza a conexao
configurada em `src/main/resources/application-supabase.properties`.
O teste nao carrega `.env` automaticamente. Se precisar substituir a conexao
no terminal, use as variaveis Spring `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.
Nao inclua senha na URL JDBC.

Os services e repositories sao reais: nao ha Mockito. Hibernate valida o schema
existente, sem criar ou alterar tabelas. Nao e ativado o perfil `teste-banco`,
que insere dados permanentes.

## O que e verificado

- Criacao de disciplina por administrador e leitura pelo repository e SQL.
- Duplicidade de codigo usando uma consulta real, ignorando maiusculas.
- Permissao usando o perfil de aluno persistido no banco.
- Consulta de usuario por email e rejeicao de senha incorreta pelo service.
- Criacao de postagem com autor, conteudo, tag e anexo; leitura dos relacionamentos.
- Exclusao de postagem pelo criador, incluindo anexo e associacao com tag.

Os usuarios de teste sao ficticios e inativos, com emails e matriculas unicos.
As senhas de preparacao sao hashes BCrypt de valores aleatorios. Os testes de cadastro
tambem verificam a gravacao JOINED de aluno e professor e o login com BCrypt.

## O que aparece no console

O console mostra os comandos SQL e mensagens `[INTEGRACAO]`, por exemplo:

```text
[INTEGRACAO] Disciplina gravada e lida: <UUID> | Programacao ...
[INTEGRACAO] Rollback confirmado: usuarios de teste nao permaneceram no banco.
```

Cada teste usa uma transacao revertida automaticamente no final, inclusive quando
uma assercao falha. `flush()` envia o SQL ao banco e `clear()` limpa o cache JPA
antes da releitura. A verificacao apos o rollback consulta se os usuarios de teste
desapareceram. Os registros nao ficam disponiveis no painel do Supabase apos o teste.
Isso testa SQL e relacionamentos dentro da transacao, mas nao confirma um commit
nem visibilidade dos dados por outra conexao.

A classe termina em `IT` para nao acessar o Supabase no comando habitual
`mvnw.cmd test`. Execute-a explicitamente com `-Dtest=ServicesSupabaseIT`.

## Se a conexao falhar

`SocketTimeoutException: Connect timed out` significa que a conexao ao endereco
e porta configurados nao foi estabelecida. Confira o estado do projeto Supabase,
o host e a porta em **Connect**, e se sua rede permite a conexao PostgreSQL.
Uma mensagem posterior sobre nao determinar o dialect pode ser consequencia
dessa falha: primeiro resolva a conexao.

Os seis cenarios originais passaram no Supabase apos a troca de rede. Foram
adicionados dois cenarios de cadastro e login (aluno e professor).
