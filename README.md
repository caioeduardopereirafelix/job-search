# Job Search

Aplicacao que cruza as tecnologias e o curriculo de cada usuario com vagas buscadas na [Adzuna](https://developer.adzuna.com/) e mostra as vagas compativeis.

## Como funciona

1. O usuario se cadastra, faz login (JWT) e informa as tecnologias que domina.
2. Ele pode enviar o curriculo em PDF (ate 5 MB); o texto e extraido com PDFBox.
3. Um scheduler busca vagas na Adzuna periodicamente para cada tecnologia cadastrada, remove duplicadas e gera os *matches*.
4. O match procura a tecnologia como palavra inteira no titulo/descricao da vaga (ex.: "Java" nao casa com "JavaScript").

## Stack

- Java 21, Spring Boot 3.4 (Web, Security, Data JPA, Validation)
- PostgreSQL 16 + Flyway (migrations em `src/main/resources/db/migration`)
- JWT (jjwt), PDFBox, springdoc-openapi
- Front estatico em TypeScript puro, servido pelo Spring (`src/main/resources/static`)

## Requisitos

- JDK 21
- Docker (para o PostgreSQL)
- Node.js (apenas para recompilar o TypeScript)
- Conta gratuita na Adzuna (App ID e App Key)

## Como rodar

1. Configure as variaveis de ambiente:

   ```bash
   cp .env.example .env
   ```

   Edite o `.env` (o `docker-compose.yaml` e o Spring leem este arquivo). Variaveis:

   | Variavel | Descricao |
   |---|---|
   | `DB_URL` | URL JDBC do Postgres |
   | `DB_USER` / `DB_PASSWORD` | Credenciais do banco |
   | `ADZUNA_APP_ID` / `ADZUNA_APP_KEY` | Credenciais da API da Adzuna |
   | `JWT_SECRET` | Segredo de assinatura do JWT |
   | `RESUME_STORAGE_PATH` | Pasta dos curriculos (padrao `./uploads/resumes`) |
   | `FLYWAY_VALIDATE_ON_MIGRATE` | Padrao `true`. So mude pra `false` localmente se editar o conteudo de uma migration ja aplicada no seu banco (o checksum muda e a validacao do Flyway trava a aplicacao) - nunca desative em producao |

2. Suba o banco:

   ```bash
   docker compose up -d
   ```

3. Suba a aplicacao (as migrations rodam sozinhas):

   ```bash
   ./mvnw spring-boot:run
   ```

4. Acesse `http://localhost:8080`.

### Front-end (TypeScript)

Os `.js` compilados ficam versionados em `static/js`. Ao editar os `.ts`:

```bash
npm install
npm run build
```

## API

Documentacao interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`
(OpenAPI em `/v3/api-docs`). Para testar rotas protegidas, faca login, copie o token e use **Authorize**.

| Metodo | Rota | Auth | Descricao |
|---|---|---|---|
| POST | `/api/users` | publica | Cadastra usuario |
| POST | `/api/auth/login` | publica | Retorna o JWT |
| GET | `/api/technologies` | publica | Lista tecnologias |
| GET | `/api/users/{id}` | JWT (dono) | Dados do usuario |
| POST | `/api/users/{id}/technologies` | JWT (dono) | Adiciona tecnologias |
| DELETE | `/api/users/{id}/technologies?technologyName=X` | JWT (dono) | Remove tecnologia |
| GET | `/api/users/{id}/matches` | JWT (dono) | Vagas compativeis |
| POST | `/api/users/{id}/resume` | JWT (dono) | Envia curriculo (PDF) |
| GET | `/api/users/{id}/resume` | JWT (dono) | Metadados e texto do curriculo |
| GET | `/api/users/{id}/resume/download` | JWT (dono) | Baixa o PDF |

## Testes

```bash
./mvnw test
```

## Licenca

[MIT](LICENSE)
