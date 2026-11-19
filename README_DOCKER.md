# Executar com Docker

Com Docker não é preciso instalar JDK, Maven ou Node na máquina: as versões
ficam fixadas nas imagens (Java 25, Maven 3.9, Node 24, Nginx).

| Serviço    | Imagem de build                  | Imagem final           | Porta no host |
|------------|----------------------------------|------------------------|---------------|
| `backend`  | `maven:3.9-eclipse-temurin-25`   | `eclipse-temurin:25-jre` | 8081        |
| `frontend` | `node:24-alpine`                 | `nginx:1.27-alpine`    | 5173          |

O banco continua no Supabase; nenhum PostgreSQL local é criado.

## 1. Pré-requisito

Docker Desktop instalado e **aberto** (o ícone da baleia precisa estar ativo).

## 2. Configurar credenciais (uma vez)

Copie `demo/.env.example` para `demo/.env` e preencha os valores reais
(`CHAVE=valor`, sem aspas). O Compose injeta esse arquivo no container do backend.
O `.env` nunca entra na imagem nem no Git.

## 3. Subir

Na pasta raiz do projeto (onde está `docker-compose.yml`):

```powershell
docker compose up --build
```

Abra **http://localhost:5173**. O Nginx serve o frontend e encaminha `/api`
para o backend, mantendo a mesma origem para o cookie de sessão e o CSRF.
O frontend só inicia depois que `/actuator/health` do backend responde.

## Comandos úteis

```powershell
docker compose up --build -d     # em segundo plano
docker compose logs -f backend   # acompanhar logs da API
docker compose ps                # estado e health dos serviços
docker compose down              # parar e remover os containers
docker compose up --build backend  # recompilar só um serviço após mudanças
```

Após alterar código, rode `docker compose up --build` de novo: as imagens são
recompiladas (dependências Maven/npm ficam em cache, então é rápido).

## Observações

- Os testes automatizados não rodam no build da imagem. Execute-os normalmente
  (`.\mvnw.cmd test` em `demo`, `npm.cmd test` em `frontend`).
- O desenvolvimento com hot reload (`npm run dev`) continua funcionando fora do
  Docker; o proxy do Vite aponta para `http://127.0.0.1:8081`, que é a porta
  publicada pelo container do backend.
- Em produção com HTTPS, defina `SESSION_COOKIE_SECURE=true` no `demo/.env`.
