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
| `DB_HOST` / `DB_NAME` | `localhost` / `petfy` | **sim no `prod`** — sem default, para não subir apontando para o lugar errado |
| `DB_PORT` / `DB_USER` / `DB_PASSWORD` | `5432` / `petfy` / `petfy` | não |
| `DB_SSLMODE` | não se aplica | não — default `require` no `prod` |
| `JWT_SECRET` | valor de desenvolvimento | **sim** — a aplicação não sobe sem ele |
| `JWT_EXPIRATION_MINUTES` | `120` | não |
| `TESSDATA_PATH` | caminho do Tesseract no Windows | não (no container vem do `TESSDATA_PREFIX`) |
| `SPRING_PROFILES_ACTIVE` | `local` | **sim no deploy** — a imagem traz `docker`, que aponta o banco para a própria máquina |

Perfis:

| Perfil | Onde |
|---|---|
| `local` | app na máquina, banco no container do `docker-local` |
| `docker` | app em container, banco no host |
| `dev` / `stg` / `prd` | ambientes hospedados, banco gerenciado e conexão cifrada |

Os três ambientes hospedados compartilham a mesma configuração de
infraestrutura e se diferenciam por **valor**, não por estrutura. Por isso o que
é comum mora em `application-hosted.properties`, e `dev`, `stg` e `prd` são
*profile groups* que o incluem — subir com `SPRING_PROFILES_ACTIVE=dev` ativa
`dev` e `hosted` juntos. Só o `dev` tem arquivo próprio, para religar o
`show-sql`; `stg` e `prd` se resolvem por variável de ambiente.

### Health check

`GET /actuator/health` é **público** e responde apenas o status, sem detalhe de
componente — quem chama é o provedor de hospedagem, sem credencial, e a resposta
não deve contar a um desconhecido qual peça caiu. Os demais endpoints do
actuator exigem token como qualquer outra rota, e só `health` e `info` são
expostos sobre HTTP.

## Autenticação e autorização

Existem **dois tipos de conta**: `OWNER` (tutor) e `VET` (veterinário, vinculado
a uma clínica). Cada um tem sua tabela — tutor e veterinário têm dados e ciclos
de vida diferentes, e uni-los numa conta genérica obrigaria a reescrever
autenticação e todas as checagens de propriedade que já existem.

Os dois entram pelo **mesmo** `POST /auth/login`. O papel sai de qual tabela o
e-mail aparece, nunca de um campo do request — deixar o cliente declarar o
próprio papel seria deixá-lo escolher a própria permissão. O token carrega o
papel, e o `LoginResponseDTO` devolve `role` para o cliente saber que tela abrir.

O preço de duas tabelas é que a unicidade de e-mail entre elas não é garantida
pelo banco: o cadastro de vet checa os dois lados, senão o login ficaria ambíguo.

A API é stateless. Cadastro de owner, cadastro de vet, login e a carteira
compartilhada são públicos; todo o resto exige `Authorization: Bearer <token>`.

**Trocar a senha derruba as sessões abertas.** Ser stateless significa que não há
sessão no servidor para encerrar, então o token emitido antes da troca
continuaria valendo até expirar — e quem troca a senha por suspeita de acesso
indevido não expulsaria justamente quem quer expulsar. Por isso a troca carimba
`password_changed_at` na conta, e o filtro recusa token emitido antes desse
instante. É o único ponto em que uma rota autenticada consulta o banco antes de
autenticar, e por isso a consulta traz só a coluna, não a entidade inteira.

Token do **mesmo segundo** da troca é recusado junto: o `iat` do JWT tem precisão
de segundo e a troca é gravada com fração de segundo, então o empate é ambíguo.
Recusar custa um login novo; aceitar deixaria viva a sessão que a troca deveria
ter derrubado.

Autenticar não basta: **cada owner só enxerga os próprios dados**. Pets são
filtrados pelo dono, e vacinas e histórico de saúde pelo dono do pet. Recurso de
outra pessoa responde `404`, e não `403` — um `403` confirmaria que aquele id
existe, o que permitiria varrer ids para descobrir o que há na base.

Onde o dono é obrigatório, ele vem sempre do token, nunca do payload: por isso
`PetRequestDTO` não tem `ownerId` e a importação por OCR não recebe o dono como
parâmetro. E como um owner só acessa a si mesmo, não existem `GET /owners/{id}`
nem `GET /owners/all` — só `/owners/me`.

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
| Autenticação | `/auth` | `POST /auth/login`, `/auth/password-reset`, `/auth/password-reset/confirm`, `/auth/email-verification/resend` e `/auth/email-verification/confirm` (todos públicos) |
| Owners | `/owners` | `POST /owners/include` (público), `GET`/`PUT`/`DELETE /owners/me` e `PUT /owners/me/password` |
| Pets | `/pets` | escopado ao dono autenticado |
| Veterinários | `/vets` | `POST /vets/include` (público, exige convite ou clínica nova) e `GET /vets/me` |
| Convites de clínica | `/vet/clinic-invites` | vet emite, lista e revoga |
| Acesso de clínicas | `/pets/{petId}/clinic-access` | tutor concede, lista e revoga |
| Área do veterinário | `/vet/pets` | pets autorizados, vacinas e atendimentos |
| Clínicas | `/clinics` | leitura e criação abertas; **editar e remover exigem ser vet da clínica** |
| Vacinas | `/vaccines` | listagem em `GET /vaccines`, escopada pelo dono do pet |
| Agenda de vacinas | `/vaccines/agenda` | `?windowDays=30` — o que está vencido ou vencendo |
| Catálogo de vacinas | `/vaccine-catalog` | somente leitura, mantido por migration |
| Histórico de saúde | `/health-records` | `GET /health-records/pet/{petId}` |
| Importação por OCR | `/pet-id` | `POST /pet-id/import-pet-id-card` (multipart) |
| Compartilhamento | `/pets/{petId}/shares` | criar e listar links; `DELETE /shares/{id}` revoga |
| Carteira compartilhada | `/share/{token}` | **público** — não exige autenticação |

O `PUT` é parcial de propósito: campos ausentes no payload são preservados. Por
isso a validação de payload (`@Valid`) vale apenas nos `POST` — os dois
compartilham o mesmo DTO.

Erros são padronizados por `ErrorMessageEnum` e tratados no
`GlobalExceptionHandler`. Campos de auditoria (`creationDate`, `updateDate`)
existem em todas as entidades.

## Agenda de vacinas

É a única parte do sistema que produz informação em vez de devolver o que foi
digitado. `GET /vaccines/agenda` cruza as vacinas de todos os pets do tutor e
classifica pela próxima dose:

| Status | Critério |
|---|---|
| `OVERDUE` | próxima dose antes de hoje |
| `DUE_SOON` | próxima dose entre hoje e o fim da janela (inclusive) |
| `UP_TO_DATE` | próxima dose depois da janela |
| `NO_NEXT_DOSE` | sem próxima dose — dose única, ou não preenchido |

A lista traz só o que pede ação (`OVERDUE` e `DUE_SOON`), da mais atrasada para
a menos urgente, com o nome do pet junto para a tela não precisar de outra
chamada. Os contadores cobrem os quatro status, para mostrar "3 em dia" sem uma
segunda requisição. A janela é ajustável via `?windowDays=`.

Para que isso seja confiável, a próxima dose precisa ser confiável — daí o
catálogo. Ao registrar uma vacina com `vaccineCatalogId`, o nome e o
`nextDoseDate` saem do catálogo (`applicationDate + defaultIntervalDays`) em vez
de serem estimados pelo tutor. Data enviada explicitamente sempre vence, para o
caso de orientação diferente do veterinário. Vacina em texto livre continua
aceita, só sem cálculo automático.

O catálogo é somente leitura na API e mantido por migration: incluir ou corrigir
vacina é uma migration nova. Isso o mantém versionado sem exigir um papel de
administrador, que o sistema não tem.

## Entrando numa clínica

Há **duas** portas de entrada para um veterinário, e nenhuma delas é apontar
para uma clínica existente:

1. **Cadastrar a clínica junto** — vira o primeiro vet dela.
2. **Apresentar um convite** emitido por quem já está lá.

```bash
# vet de dentro emite (opcionalmente endereçado a um e-mail)
curl -X POST localhost:8080/vet/clinic-invites \
  -H "Authorization: Bearer $TOKEN_VET" -H 'Content-Type: application/json' \
  -d '{"email":"nova@vet.com.br","expiresInDays":3}'

# a pessoa convidada se cadastra
curl -X POST localhost:8080/vets/include \
  -H 'Content-Type: application/json' \
  -d '{"name":"Dra. Marina","email":"nova@vet.com.br","password":"s3nhaForte",
       "crmv":"SP-12345","inviteToken":"..."}'
```

**Por que isso importa:** antes bastava saber o `clinicId` — que aparece em
qualquer listagem — para se cadastrar como vet de qualquer clínica. E quem entra
numa clínica alcança **todos os pets que ela já foi autorizada a atender**. Era o
buraco mais sério do modelo.

Isso não substitui validação de CRMV, que dependeria de integrar com registro
externo. Mas troca *"qualquer um entra"* por *"alguém de dentro respondeu por
essa pessoa"*.

O convite é de **uso único** (aceitar consome), tem validade curta (7 dias por
padrão, teto de 30) e pode ser endereçado a um e-mail específico — vale
preencher, porque um link sem dono encaminhado a terceiros vira porta de entrada.
Token inexistente, expirado, revogado, já usado e destinado a outro e-mail
respondem igual, para não dizer a quem tenta adivinhar qual parte errou.

## Veterinário atendendo um pet

O tutor autoriza uma **clínica** (não um veterinário específico — quem atende
hoje pode não ser quem atende no retorno) a acessar o pet. Qualquer vet daquela
clínica passa então a ver e registrar vacinas ali, carimbadas com a clínica.

```bash
# tutor concede
curl -X POST localhost:8080/pets/$PET_ID/clinic-access \
  -H "Authorization: Bearer $TOKEN_TUTOR" -H 'Content-Type: application/json' \
  -d "{\"clinicId\":\"$CLINIC_ID\"}"

# vet lista os pets que a clínica dele atende
curl localhost:8080/vet/pets -H "Authorization: Bearer $TOKEN_VET"

# vet registra a vacina
curl -X POST localhost:8080/vet/pets/$PET_ID/vaccines \
  -H "Authorization: Bearer $TOKEN_VET" -H 'Content-Type: application/json' \
  -d '{"vaccineCatalogId":"...","applicationDate":"2026-08-01"}'

# tutor revoga quando quiser
curl -X DELETE localhost:8080/pets/$PET_ID/clinic-access/$CLINIC_ID \
  -H "Authorization: Bearer $TOKEN_TUTOR"
```

**A concessão do tutor é o portão.** É ela que torna seguro um veterinário
escrever no histórico de um pet que não é dele — e é o que limita o dano de não
haver verificação de identidade profissional: quem se cadastra como vet de uma
clínica só alcança os pets que aquela clínica já foi autorizada a atender.

A clínica da vacina vem **do vet autenticado, nunca do payload** — mesmo
princípio do `ownerId` no pet. O `petId` vem do path; o que vier no corpo é
ignorado. Pet sem concessão ativa responde `404`, não `403`: para o veterinário,
um pet que sua clínica não atende é indistinguível de um pet que não existe.

As rotas `/vet/**` exigem `ROLE_VET` na cadeia de filtros, além da checagem do
`CurrentVetProvider` — a autorização não depende só de a busca falhar na tabela
certa.

O veterinário registra **vacinas e atendimentos** (consulta, cirurgia, exame) —
`POST /vet/pets/{petId}/vaccines` e `POST /vet/pets/{petId}/health-records`. Os
dois seguem a mesma regra: clínica do vet autenticado, `petId` do path, tutor
notificado.

### Corrigir um registro

`PUT /vet/pets/{petId}/vaccines/{vaccineId}` — correção, não reescrita:

- **só registro da própria clínica.** Vacina lançada pelo tutor não é da clínica
  corrigir.
- **só dentro de uma janela curta** (7 dias, configurável). Depois disso o
  registro congela: corrigir um lançamento de meses atrás não é conserto de
  digitação, e o tutor é quem decide o que fica na carteira dele.
- **o estado anterior fica gravado** em `vaccine_corrections`. Carteira de
  vacinação é dado de saúde, e dado de saúde não se reescreve em silêncio — quem
  apresenta a carteira precisa poder confiar que o que está lá não mudou sem
  deixar marca.
- **o tutor é notificado** da correção.
- **não há DELETE.** Apagar um registro de vacina não é correção; se foi lançado
  no pet errado, isso é outro problema. O tutor pode remover pelo endpoint dele.

O tutor também deixa rastro ao editar, mas **não tem janela** — a carteira é
dele. Se só o veterinário fosse auditado, o histórico contaria meia verdade, o
que é pior que não ter histórico: daria impressão de completude.

O rastro é legível pelos dois lados:

```bash
# tutor, sobre uma vacina dele
curl localhost:8080/vaccines/$VACCINE_ID/corrections -H "Authorization: Bearer $TOKEN_TUTOR"

# vet, sobre uma vacina de um pet que a clínica atende
curl localhost:8080/vet/pets/$PET_ID/vaccines/$VACCINE_ID/corrections \
  -H "Authorization: Bearer $TOKEN_VET"
```

Cada entrada traz **o que a vacina era** antes daquela alteração, mais quem
mudou (`OWNER`/`VET`, nome, e a clínica quando for vet) e quando. Comparando com
o registro atual, quem lê reconstrói o que mudou. O vet enxerga o rastro de
qualquer vacina do pet, não só das que sua clínica registrou — ele já lê a
carteira inteira, e saber que um registro foi alterado faz parte de lê-lo.

## Compartilhar a carteira

O momento de uso de uma carteira de vacinação é apresentá-la: hotelzinho,
creche, banho e tosa, vet novo. O tutor gera um link de leitura com validade e
manda para quem pediu — quem recebe **não precisa ter conta**.

```bash
# gerar (autenticado). expiresInDays é opcional, default 30, teto 365
curl -X POST localhost:8080/pets/$PET_ID/shares \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"expiresInDays":7}'

# abrir (público)
curl localhost:8080/share/$SHARE_TOKEN

# listar e revogar
curl localhost:8080/pets/$PET_ID/shares -H "Authorization: Bearer $TOKEN"
curl -X DELETE localhost:8080/shares/$SHARE_ID -H "Authorization: Bearer $TOKEN"
```

**O link mostra a carteira, não o prontuário.** O histórico de saúde fica de fora
de propósito: quem pede a carteira precisa saber se as vacinas estão em dia, não
que o pet fez uma cirurgia. Do tutor sai só o nome — e-mail, telefone e endereço
não aparecem. Cada vacina vem classificada pela mesma regra da agenda
(`VaccineStatusCalculator`), para o link e o app não discordarem sobre o que
está vencido.

**O token é guardado como hash.** Ele é a única credencial do link, então um
vazamento do banco não pode entregar as carteiras ativas. A consequência de
produto é que o token **só aparece uma vez, na criação** — não dá para reexibir o
que não está guardado. Quem perdeu o link revoga e cria outro. Se o incômodo
superar o ganho, é trocar o `hash()` por armazenamento direto no
`PetShareServiceImpl`.

Token inexistente, expirado e revogado respondem igual, para não confirmar a
quem tem um link velho que aquele pet existe.

## Confirmação de e-mail

O e-mail é ao mesmo tempo a chave do login e o canal por onde saem lembrete de
vacina e aviso de vacina registrada. Sem confirmar que o endereço é de quem se
cadastrou, o sistema pode passar a mandar nome do pet e do tutor para a caixa de
um estranho.

**E-mail não confirmado não bloqueia o login — suspende a notificação.** Bloquear
criaria atrito no cadastro para proteger contra outra coisa: o risco não é a
pessoa entrar, é o aviso sair para o endereço errado. Quem digitou o próprio
e-mail errado continua conseguindo entrar e corrigir.

```bash
# reenviar (publico). Responde 202 exista ou nao a conta, e tambem para quem ja confirmou
curl -X POST localhost:8080/auth/email-verification/resend \
  -H 'Content-Type: application/json' -d '{"email":"ulysses@exemplo.com"}'

# confirmar (publico)
curl -X POST localhost:8080/auth/email-verification/confirm \
  -H 'Content-Type: application/json' -d '{"token":"<token>"}'
```

O token vale 24 horas — bem mais que os 30 minutos da recuperação de senha,
porque o que está em jogo é diferente: este confirma um endereço, aquele troca a
senha da conta.

**Falha de envio no cadastro não desfaz a conta.** É a mesma escolha do aviso de
vacina registrada: o efeito principal é a conta criada, e perdê-la porque o canal
caiu seria trocar um problema pequeno — reenviar — por um grande.

**As contas que existiam antes desta regra ficam como não confirmadas**, porque
não há como afirmar que aqueles endereços foram checados. Na prática elas param
de receber notificação até verificarem.

## Notificações

Há dois avisos ao tutor, no mesmo canal:

1. **Lembrete de vacina** — rotina diária, **desligada por padrão** (ver o porquê
   abaixo).
2. **Vacina registrada por uma clínica** — sai na hora do registro e **não
   depende do agendador**, porque não é varredura.

```properties
petfy.notifications.channel=log       # log | email
petfy.notifications.from=             # default: o proprio MAIL_USERNAME

petfy.reminders.enabled=true          # default false
petfy.reminders.window-days=30        # antecedência do aviso
petfy.reminders.cooldown-days=7       # intervalo mínimo entre avisos da mesma dose
petfy.reminders.cron=0 0 9 * * *      # 9h, America/Sao_Paulo
```

**O canal não conhece o domínio.** Quem monta o texto é quem tem o contexto
(`VaccineReminderService`, `VaccineRecordedNotifier`); `Notifier` só transporta
uma `Notification` pronta. Foi o que permitiu somar o segundo aviso sem duplicar
a infraestrutura de canal — e um push futuro não mexe em nenhum dos dois.

**As duas notificações tratam falha de envio de forma oposta, de propósito:**

| | Lembrete | Vacina registrada |
|---|---|---|
| Quando sai | rotina agendada, fora de requisição | dentro da requisição do vet, mas **o envio vai para outra thread** |
| Falha no envio | exceção sobe, rollback | captura e loga |
| Por quê | o envio é o único efeito; não avisar = não ter feito nada | o efeito principal é a vacina gravada — perdê-la porque o SMTP caiu seria trocar um problema pequeno por um grande |

**Só o envio é assíncrono, não a montagem da mensagem.** Montar passa por
associações lazy — o pet, o tutor — que só existem dentro da transação de quem
chamou; em outra thread elas estariam fechadas e o aviso morreria com
`LazyInitializationException`. Então o `ClinicActivityNotifier` monta a
`Notification` na hora e entrega pronta ao `AsyncNotificationDispatcher`.

O pool é próprio e limitado (`AsyncConfig`), não o executor padrão do Spring, que
cria uma thread por chamada sem teto: uma clínica registrando vacina em lote não
pode virar uma thread por registro. Quando a fila enche, o envio volta a rodar na
thread de quem chamou — devolve latência ao veterinário no pico, o que é ruim,
mas melhor do que descartar um aviso ao tutor em silêncio.

**A rotina de lembretes continua síncrona de propósito.** Ela já roda fora de
requisição e depende da exceção subir: é o rollback que desmarca
`last_reminder_sent_at` e mantém a dose elegível na próxima varredura.

**Um lembrete por tutor, não um por dose.** Quem tem três pets atrasados recebe
uma mensagem com três linhas, não três mensagens.

**O cooldown existe porque dose vencida continua vencida.** Sem ele, o mesmo
lembrete sairia todo dia até a pessoa vacinar o pet — o caminho mais curto para
o aviso ser ignorado. O envio é marcado em `vaccines.last_reminder_sent_at`,
sempre **depois** do envio: se o canal falhar, o rollback deixa a dose elegível
na próxima execução, em vez de registrar como avisada uma dose que ninguém
recebeu.

**Canais.** `log` é o padrão e escreve no log da aplicação — não é um stub: a
rotina roda inteira (varre, agrupa, monta a mensagem, marca o envio) sem
depender de credencial de SMTP. `email` exige SMTP configurado:

| Variável | Para quê |
|---|---|
| `NOTIFICATIONS_CHANNEL=email` | troca o canal; sem isso nada é enviado de verdade |
| `MAIL_USERNAME` | a conta que autentica no SMTP |
| `MAIL_PASSWORD` | **senha de app**, não a senha da conta — exige 2FA ligado |
| `MAIL_HOST` / `MAIL_PORT` | default `smtp.gmail.com` / `587`, que serve para Gmail e Google Workspace |

**O remetente é o próprio `MAIL_USERNAME` por default.** Gmail e Workspace
recusam `From` diferente da conta autenticada, então definir `NOTIFICATIONS_FROM`
com outro endereço faz o envio ser rejeitado — o default evita esse erro.

Os timeouts de SMTP são explícitos porque o default do JavaMail é esperar para
sempre: um servidor que aceita a conexão e não responde prenderia uma thread do
pool de envio até a aplicação reiniciar.

**Por que vem desligada:** com mais de uma instância no ar, todas disparariam a
rotina e o tutor receberia o lembrete repetido. Ligar exige decidir quem executa
— uma instância só, ou um agendador externo chamando a rotina. Enquanto essa
decisão não existe, o default seguro é não enviar.

## Banco

O schema é versionado com Flyway em `src/main/resources/db/migration`. O
Hibernate roda com `ddl-auto=validate`: ele confere que o mapeamento bate com o
banco, mas não altera nada. Toda mudança de schema é uma migration nova.

## Testes

```bash
mvn verify          # testes + relatório de cobertura em target/site/jacoco
```

Os testes terminados em **`ContainerTest`** sobem um Postgres real via
Testcontainers e **são pulados onde não há Docker** (`disabledWithoutDocker`) —
quem roda a suíte sem Docker não fica bloqueado, e o pipeline executa de
verdade. São eles que cobrem o que o H2 não alcança:

- **`SchemaMigrationContainerTest`** — o Flyway aplica as migrations num Postgres
  limpo e o Hibernate roda `ddl-auto=validate` contra o resultado. Se algum tipo
  ou nome divergir do mapeamento, o contexto não sobe — a mesma falha que
  aconteceria na primeira subida em produção.
- **`UuidQueriesContainerTest`** — as consultas por UUID, que no H2 voltam sempre
  vazias. São a base do escopo por dono e do acesso do veterinário: se uma
  trouxer de menos, o tutor deixa de ver os próprios dados; de mais, vê os de
  outro.

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

## Recuperação de senha

```bash
# pedir (publico). Responde 202 exista ou nao a conta
curl -X POST localhost:8080/auth/password-reset \
  -H 'Content-Type: application/json' \
  -d '{"email":"ulysses@exemplo.com"}'

# concluir (publico), com o token que chegou no canal de notificacao
curl -X POST localhost:8080/auth/password-reset/confirm \
  -H 'Content-Type: application/json' \
  -d '{"token":"<token>","newPassword":"s3nhaNovaForte"}'
```

**O pedido responde igual exista ou não a conta.** Distinguir transformaria um
endpoint público em verificador de quem tem cadastro aqui — para uma base de
tutores de pet isso já é exposição e, cruzado com o vazamento de outro serviço,
vira lista de alvos. Pelo mesmo motivo, token inexistente, expirado e já usado
respondem igual na confirmação.

**Há cooldown entre pedidos** (`petfy.password-reset.cooldown-minutes`, padrão 5).
Sem ele, bastaria repetir a chamada com o endereço de alguém para o endpoint
virar máquina de enviar e-mail a terceiros.

**Pedir de novo invalida o pedido anterior.** Dois links vivos ao mesmo tempo
dobram a janela de quem interceptou o e-mail antigo, sem ajudar em nada quem
esqueceu a senha.

**O token é guardado como hash**, como no compartilhamento da carteira, então ele
só existe uma vez — no envio. E concluir a recuperação carimba
`password_changed_at`, o que derruba as sessões abertas: se a conta foi tomada,
quem recupera precisa expulsar quem entrou, não apenas voltar a entrar junto.

## Limitações conhecidas

- **O OCR em container não foi testado.** O Dockerfile instala o Tesseract e
  descobre o `tessdata` no build, mas isso não foi exercitado.
- **Os testes de container não rodam sem Docker.** Localmente eles pulam; quem
  precisa de garantia sobre migrations e consultas por UUID depende do pipeline
  ou de ter Docker no ar.
- **A recuperação de senha nunca foi exercitada contra um SMTP real.** O fluxo
  está completo e testado, mas o canal padrão é `log`: o token sai no log da
  aplicação, o que serve para desenvolver e não serve para um tutor de verdade.
- **A recuperação vale só para tutor.** O veterinário não recupera nem troca a
  própria senha. A coluna
  `password_changed_at` já existe em `vets` e o filtro já a consulta, mas não há
  endpoint que a preencha.
- **O envio por e-mail nunca foi exercitado contra um SMTP real.** O
  `EmailReminderNotifier` é coberto só pela montagem da mensagem; não houve
  entrega de verdade.
- **A rotina de lembretes não é segura para múltiplas instâncias.** Não há lock
  distribuído: se duas instâncias rodarem com `reminders.enabled=true`, as duas
  varrem e o tutor recebe repetido. Daí o default desligado.
- **O protocolo de filhote não está modelado.** O `defaultIntervalDays` do
  catálogo é o intervalo de reforço (anual na maioria). O esquema inicial de
  várias doses a cada 21–30 dias exigiria representar protocolo com número de
  doses e intervalos distintos entre elas.
- **A espécie do catálogo é informativa.** `Pet.type` é texto livre, então não há
  filtro automático entre o pet e as vacinas aplicáveis a ele.
- **Link de compartilhamento não tem limite de acesso nem rastro.** Quem tem a
  URL abre quantas vezes quiser, e não há registro de quem abriu. Para uma
  carteira de vacinação isso é aceitável; se um dia guardar dado mais sensível,
  vale limitar por número de acessos e registrar os acessos.
- **O CRMV não é verificado.** Entrar numa clínica já exige convite de quem está
  lá, mas o registro profissional informado no cadastro não é conferido com
  nenhum órgão. Fechar isso depende de integrar com o CRMV.
- **Quem cria uma clínica não é verificado.** A porta do convite protege clínicas
  existentes, mas qualquer pessoa ainda cadastra uma clínica nova e vira o
  primeiro vet dela. O dano é menor (uma clínica sem pets autorizados não alcança
  nada), mas permite poluir o diretório.
- **O rastro cobre vacinas e atendimentos.** Pet e clínica seguem editáveis sem
  registro do que mudou.
- **A notificação de vacina registrada é síncrona.** Sai dentro da requisição do
  veterinário, então um SMTP lento adiciona latência ao registro. Falha não
  quebra nada (é capturada), mas o envio devia sair do caminho da requisição —
  fila ou `@Async`.
- **O escopo por dono nas listagens depende de queries não verificadas contra
  banco real** (`findByOwnerOwnerId` e afins) — mesma limitação de UUID no H2
  descrita acima. As checagens de propriedade item a item, essas sim, estão
  cobertas por teste.

## Arquitetura

Hoje é um monólito organizado por domínio. A intenção registrada no início do
projeto era quebrar em serviços independentes (Auth, Owner, Pet, Vaccine, Clinic,
HealthRecord) atrás de um API Gateway, com comunicação por eventos via RabbitMQ —
daí o JWT stateless, que não exige sessão compartilhada. Nada disso existe ainda.
