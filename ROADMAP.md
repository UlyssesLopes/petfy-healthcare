# Roadmap

Plano de execução do Petfy HealthCare. Complementa o `README.md`: lá está o que
existe e por quê, aqui está o que vem depois e em que ordem.

**Objetivo desta rodada:** sair do estado "API que ninguém consegue usar" e
chegar a tutores reais usando — mesmo que sejam poucos. Toda a ordem abaixo
deriva disso: o que destrava uso real vem antes, e profundidade de modelo vem
depois, guiada pelo que doer no uso.

## Status

Passo 1 em andamento: as tarefas de código estão feitas (perfil `prod`, actuator,
`show-sql`), falta provisionar o PaaS e o Postgres gerenciado e setar as
variáveis. O CI publica a imagem em `ghcr.io/ulysseslopes/petfy-healthcare`, mas
nada consome ela ainda.

## Decisões já tomadas

| Decisão | Consequência |
|---|---|
| Hospedagem em PaaS de container (Fly/Railway/Render), imagem puxada do ghcr | Postgres gerenciado, HTTPS e domínio vêm do provedor; não há IAM/VPC para montar |
| **Uma instância**, escala vertical | O lembrete de vacina dispensa lock distribuído. Escalar para duas reintroduz o problema — está registrado no passo 3 |
| Monólito organizado por domínio | Os microserviços com RabbitMQ que o README cita como intenção original ficam fora desta rodada; não servem o objetivo de usuários reais |

## Fase 1 — pôr no ar e virar o usuário zero

### 1. Ambiente acessível de fora

A configuração já é toda dirigida por variável de ambiente
(`${VAR:default}` em `application.properties`), então o trabalho aqui é menos
código e mais provisionar. As duas exceções estão na lista.

- [x] Criar `application-prod.properties`. O perfil `docker` não serve: o
      datasource dele aponta para `host.docker.internal`, que só existe na
      máquina de desenvolvimento.
- [x] Desligar `spring.jpa.show-sql` fora de desenvolvimento — despejava SQL com
      dados no log. Agora `false` no compartilhado e `true` só no `local`.
- [x] Adicionar `spring-boot-starter-actuator` e expor apenas `health` e `info`.
      `GET /actuator/health` precisou entrar como rota pública no
      `SecurityConfig`, senão o provedor lê 401 e reinicia a instância em loop —
      coberto no `SecurityFilterChainTest`.
- [ ] Provisionar o Postgres gerenciado e apontar `DB_*`. O Flyway aplica as 9
      migrations no primeiro boot — é o mesmo caminho que o
      `SchemaMigrationContainerTest` já exercita no CI.
- [ ] Definir `JWT_SECRET` no cofre do provedor (mínimo 32 caracteres). Sem ele
      a aplicação não sobe fora do perfil `local`, de propósito.
- [ ] Definir `SPRING_PROFILES_ACTIVE=prod` no ambiente. A imagem traz `docker`
      no Dockerfile; esquecer disso faz a instância tentar
      `host.docker.internal` e entrar em loop de reinício.
- [ ] Estender o job `docker` do pipeline com o deploy, ou disparar pelo webhook
      do provedor a partir da tag `:latest` que já é publicada.

**Pronto quando:** você acessa a API de outro dispositivo, `/actuator/health`
responde, e as 9 migrations foram aplicadas num Postgres de verdade.

### 2. Ciclo de vida da conta

Hoje quem perde a senha perde o histórico do pet: `PUT /owners/{id}` ignora o
campo `password` de propósito e não existe recuperação. É o bloqueio mais barato
de remover e o que impede qualquer usuário que não seja você.

- [ ] Configurar `spring.mail.*` — **não existe em nenhum arquivo hoje**, só é
      citado num comentário. A dependência `spring-boot-starter-mail` já está no
      `pom.xml`, mas sem host configurado o canal `email` não funciona.
- [ ] Escolher provedor de envio e validar entrega real numa caixa de verdade. O
      `README` registra que o envio nunca foi exercitado contra SMTP real.
- [ ] Migration da tabela de token de recuperação: uso único, expiração curta e
      **token guardado como hash** — mesmo padrão já adotado em `PetShare`.
- [ ] `POST /auth/password-reset` (solicita) e `POST /auth/password-reset/confirm`
      (troca com o token).
- [ ] Resposta genérica no solicitar, independente de o e-mail existir ou não —
      coerente com a escolha já feita de responder `404` em vez de `403` para
      recurso de terceiro, para não permitir varredura da base.
- [ ] Rate limit no endpoint de solicitação, senão ele vira máquina de enviar
      e-mail para terceiros.
- [ ] `PUT /owners/me/password`, exigindo a senha atual.
- [ ] Verificação de e-mail no cadastro. Decidir explicitamente se e-mail não
      verificado bloqueia login ou apenas suspende notificação — o e-mail é a
      chave do login e o canal do lembrete, então a escolha tem consequência.

**Pronto quando:** você perde a senha, volta sozinho, e o e-mail chega numa caixa
real.

### 3. Lembrete de vacina ligado

A proposta de valor do produto está desligada em produção
(`petfy.reminders.enabled` default `false`). Com uma instância só, o motivo
original da trava — varredura duplicada — deixa de existir.

- [ ] `@EnableAsync` e `@Async` **apenas** no aviso de vacina registrada, que sai
      dentro da requisição do veterinário e hoje soma latência de SMTP a ela.
- [ ] **Não** transformar a rotina de lembretes em assíncrona. Ela já roda fora
      de requisição, por ser agendada, e depende da semântica atual de falha: a
      exceção sobe, o rollback desfaz a marcação em `last_reminder_sent_at`, e a
      dose volta a ser elegível na próxima execução. Async quebraria isso.
- [ ] Ligar em produção: `REMINDERS_ENABLED=true` e `NOTIFICATIONS_CHANNEL=email`.
- [ ] Log do resultado de cada varredura (quantos tutores, quantas doses), senão
      não há como saber se a rotina rodou e não tinha nada a enviar, ou se
      falhou silenciosamente.
- [ ] Registrar no `README` que escalar para duas instâncias reintroduz o
      lembrete duplicado, e que o caminho nesse dia é lock distribuído ou
      agendador externo chamando a rotina.

**Pronto quando:** uma dose vencendo gera um e-mail no dia certo, uma vez só, e o
cooldown impede o reenvio no dia seguinte.

## Fase 2 — dar rosto

### 4. Contrato, antes do cliente

- [ ] Expor OpenAPI. Atenção à versão: **springdoc 1.6.x** para Spring Boot 2.7 —
      a linha 2.x exige Boot 3. Ver a dívida do Boot no fim deste arquivo, porque
      as duas decisões se cruzam.
- [ ] Revisar os nomes antes de um cliente fossilizar o contrato:
      `POST /{recurso}/include` e `GET /{recurso}/all` são difíceis de defender.
- [ ] Decidir o que fazer com o `PUT` parcial. Hoje campo ausente é preservado e
      `@Valid` vale só no `POST`, porque os dois compartilham DTO — funciona, mas
      é surpreendente para quem consome de fora.

**Pronto quando:** o Swagger UI descreve a API inteira e os nomes definitivos
estão decididos.

### 5. Cliente web enxuto

Três telas, nada além: carteira do pet, agenda do que vence, registrar dose. A
agenda é a única parte do sistema que produz informação em vez de devolver o que
foi gravado — é ela que justifica o app existir.

**Pronto quando:** alguém que nunca viu `curl` cadastra um pet e vê a próxima
dose.

## Fase 3 — o domínio que o uso real cobra

### 6. Espécie como dado, não texto livre

`Pet.type` é `String` e a espécie do catálogo é apenas informativa, então nada
impede registrar vacina de cão num gato. Virar enum ou tabela e filtrar o
catálogo por espécie. É pré-requisito do passo 7.

**Pronto quando:** o catálogo devolvido depende da espécie do pet.

### 7. Protocolo de filhote

`defaultIntervalDays` do catálogo representa só o reforço (anual, na maioria). O
esquema inicial — várias doses a cada 21–30 dias — exige modelar protocolo com
número de doses e intervalos distintos entre elas. É o momento de maior
necessidade do tutor, e hoje é o que o sistema não sabe fazer.

**Pronto quando:** cadastrar um filhote de 45 dias gera o esquema inicial
completo, não apenas "próxima em um ano".

## Fase 4 — quando o usuário não é você

### 8. Multi-tutor e transferência de titularidade

`Pet` tem um `owner` só (`@ManyToOne`). Casal e família dividem o mesmo pet, e
hoje o segundo tutor só consegue um link de leitura; adoção e venda não têm
caminho. Mexe no escopo por dono, a parte mais sensível do sistema — as consultas
por UUID já têm cobertura contra Postgres real (`UuidQueriesContainerTest`) para
apoiar a mudança.

**Pronto quando:** dois logins distintos editam o mesmo pet e nenhum dos dois
alcança pet de terceiro.

### 9. Além da vacina: antiparasitário e peso como série

Vermífugo e antipulgas são o recorrente que o tutor de fato esquece, e
reaproveitam agenda e lembrete já construídos. `weight` hoje é um `Double`, um
valor único — virar histórico dá curva de crescimento do filhote.

**Pronto quando:** um lembrete de vermífugo sai pelo mesmo caminho do de vacina,
sem código novo de canal.

### 10. LGPD e confiança

- Exportação e exclusão a pedido do titular — é dado pessoal somado a dado de
  saúde.
- Limite de acesso e rastro nos links públicos de carteira: hoje quem tem a URL
  abre quantas vezes quiser e não há registro de quem abriu.
- Decidir o que fazer com CRMV não verificado e com criação de clínica não
  verificada. A porta do convite protege clínicas existentes, mas qualquer pessoa
  cadastra uma clínica nova e vira o primeiro vet dela.

**Pronto quando:** você atende um pedido de exclusão sem abrir o banco na mão.

## Dívidas com relógio

- **Spring Boot 2.7.18 saiu do suporte OSS.** Para dado de saúde com usuário
  real, o relógio de patch de segurança passa a contar. Não é valor de produto,
  então não ocupa um passo — mas o encaixe natural é **entre o 3 e o 4**: migrar
  para Boot 3 mexe em `javax`→`jakarta` e sai muito mais barato antes de existir
  um cliente dependendo da API. É também o que libera o springdoc 2.x no passo 4,
  em vez de entrar na 1.6.x e trocar depois.
- **Actions com Node 20 deprecado** (`checkout@v4`, `setup-java@v4`,
  `upload/download-artifact@v4`, `login-action@v3`, `build-push-action@v5`). O
  runner está forçando Node 24 e apenas avisa. Um bump numa passada só resolve.
- **OCR em container nunca foi exercitado.** O build instala o Tesseract e
  descobre o `tessdata`, mas ninguém chamou o endpoint de importação de dentro do
  container. A base mudou de Debian slim para Ubuntu 22.04 na correção da imagem,
  então a versão do Tesseract também mudou.

## Fora do escopo desta rodada

- **Anexos e storage de documento** (foto da carteirinha, exames, receitas). O
  OCR fica meio órfão sem isso, mas é dívida, não valor imediato.
- **Microserviços, API Gateway e RabbitMQ**, a intenção registrada no início do
  projeto. O monólito por domínio aguenta muito mais do que o volume desta
  rodada.
- **SonarQube.** O passo existe no pipeline e é pulado quando falta o
  `SONAR_TOKEN`; ligar é questão de configurar o secret.
- **Cobrança, multi-tenancy e verificação de CRMV como produto.** Só fazem
  sentido se o objetivo virar comercial.
