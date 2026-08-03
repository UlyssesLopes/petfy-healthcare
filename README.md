# Petfy HealthCare

Sistema para gestão de vacinação digital de pets, clínicas veterinárias, donos e
histórico de saúde animal. Java 17 + Spring Boot 2.7 + PostgreSQL, com uma
feature de importação de RG animal por OCR.

## Stack

- Java 17, Spring Boot 2.7.18
- PostgreSQL, Hibernate/JPA, Flyway
- Spring Security + JWT (HS256, via jjwt)
- Tesseract OCR (tess4j)
- Docker Compose para o banco local
- JUnit 5 + Mockito + AssertJ, cobertura por JaCoCo
- UUIDs como identificadores

## Subindo o projeto

```bash
# banco
docker compose -f docker-local/docker-compose.yaml up -d

# aplicação (perfil local é o default)
mvn spring-boot:run
```

O perfil `local` já traz defaults de desenvolvimento para banco, segredo do JWT e
caminho do Tesseract. Nenhuma variável de ambiente é necessária para rodar na
máquina.

### Configuração

| Variável | Default no perfil `local` | Obrigatória fora dele |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `petfy` | não |
| `DB_USER` / `DB_PASSWORD` | `petfy` / `petfy` | não |
| `JWT_SECRET` | valor de desenvolvimento | **sim** — a aplicação não sobe sem ele |
| `JWT_EXPIRATION_MINUTES` | `120` | não |
| `TESSDATA_PATH` | caminho do Tesseract no Windows | não (no container vem do `TESSDATA_PREFIX`) |

Perfis: `local` (app na máquina) e `docker` (app em container, banco no host).

## Autenticação

A API é stateless. O cadastro de owner e o login são públicos; todo o resto exige
`Authorization: Bearer <token>`.

```bash
# cadastro
curl -X POST localhost:8080/owners/include \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ulysses","email":"ulysses@exemplo.com","password":"s3nhaForte"}'

# login
curl -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ulysses@exemplo.com","password":"s3nhaForte"}'

# rota protegida
curl localhost:8080/pets/all -H "Authorization: Bearer $TOKEN"
```

## Endpoints

Todos os domínios seguem o mesmo formato: `POST /{recurso}/include`,
`GET /{recurso}/{id}`, `GET /{recurso}/all`, `PUT /{recurso}/{id}`,
`DELETE /{recurso}/{id}`.

| Recurso | Base | Extras |
|---|---|---|
| Autenticação | `/auth` | `POST /auth/login` |
| Owners | `/owners` | cadastro é público |
| Pets | `/pets` | |
| Clínicas | `/clinics` | |
| Vacinas | `/vaccines` | listagem em `GET /vaccines` |
| Histórico de saúde | `/health-records` | `GET /health-records/pet/{petId}` |
| Importação por OCR | `/pet-id` | `POST /pet-id/import-pet-id-card?ownerId={uuid}` (multipart) |

O `PUT` é parcial de propósito: campos ausentes no payload são preservados. Por
isso a validação de payload (`@Valid`) vale apenas nos `POST` — os dois
compartilham o mesmo DTO.

Erros são padronizados por `ErrorMessageEnum` e tratados no
`GlobalExceptionHandler`. Campos de auditoria (`creationDate`, `updateDate`)
existem em todas as entidades.

## Banco

O schema é versionado com Flyway em `src/main/resources/db/migration`. O
Hibernate roda com `ddl-auto=validate`: ele confere que o mapeamento bate com o
banco, mas não altera nada. Toda mudança de schema é uma migration nova.

## Testes

```bash
mvn verify          # testes + relatório de cobertura em target/site/jacoco
```

Alguns testes existem por motivos específicos e vale saber antes de mexer:

- **`ControllerPathVariableTest`** — o nome do path variable precisa bater com o
  nome do parâmetro do método. Quando divergem o endpoint quebra em runtime e a
  compilação não acusa.
- **`SecurityFilterChainTest`** — os demais testes de controller usam
  `standaloneSetup`, que não monta a cadeia de filtros e passaria mesmo com a
  segurança desligada. Este sobe a cadeia de verdade.
- **`HealthRecordRepositoryTest`** — queries derivadas só são traduzidas na
  subida do contexto; um nome de propriedade errado derruba a aplicação no boot,
  não no teste.

## Limitações conhecidas

- **Filtro por UUID não verificado contra banco real.** O
  `HealthRecordRepositoryTest` roda em H2, onde o Hibernate 5.6 gera
  `BINARY(255)` para UUID. O H2 2.x trata `BINARY` como tamanho fixo, então o
  valor gravado é preenchido até 255 bytes e nunca casa com um parâmetro de 16
  bytes — qualquer consulta por UUID volta vazia. Comparação entre colunas casa,
  o que confirma que os dados estão corretos. É artefato do H2 e não deve afetar
  o Postgres, onde UUID é tipo nativo. Fechar isso pede Testcontainers.
- **A migration V1 não foi validada contra um Postgres real.** Os tipos vieram do
  DDL que o próprio Hibernate gera para o dialeto Postgres, mas o `validate` só
  será exercitado na primeira subida com banco.
- **O OCR em container não foi testado.** O Dockerfile instala o Tesseract e
  descobre o `tessdata` no build, mas isso não foi exercitado.
- **Troca de senha não existe.** O `PUT /owners/{id}` ignora o campo `password`
  de propósito; trocar senha merece endpoint próprio, com confirmação da atual.
- **Não há autorização, só autenticação.** Qualquer usuário autenticado enxerga
  os dados de todos — nada amarra um pet ao owner que fez a requisição.

## Arquitetura

Hoje é um monólito organizado por domínio. A intenção registrada no início do
projeto era quebrar em serviços independentes (Auth, Owner, Pet, Vaccine, Clinic,
HealthRecord) atrás de um API Gateway, com comunicação por eventos via RabbitMQ —
daí o JWT stateless, que não exige sessão compartilhada. Nada disso existe ainda.
