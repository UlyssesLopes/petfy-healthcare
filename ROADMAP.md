# Roadmap

Plano de execução do Petfy HealthCare. Complementa o `README.md`: lá está o que
existe e por quê, aqui está o que vem depois e em que ordem.

**Objetivo desta rodada:** levar o **backend** a um produto maduro — completo,
seguro e operável — e só então construir o frontend em cima do que existir.
Dentro do backend, a ordem continua sendo a de uso real: o que destrava um tutor
de verdade vem antes, e profundidade de modelo vem depois.

**A consequência de sequenciar assim, dita em voz alta:** tutor real só chega
quando o frontend existir, porque ninguém usa `curl`. Até lá, "usuário zero" é
você, e a validação de cada passo é por API. Foi uma escolha deliberada, não um
esquecimento — ver a seção Frontend.

## Decisões em aberto

- **Token longo por link, ou OTP de 6 dígitos, na recuperação de senha e na
  confirmação de e-mail?** Hoje é um token de 32 bytes, pensado para ir dentro de
  um link — por isso pode valer 30 min ou 24 h sem risco. OTP de 6 dígitos é
  igualmente padrão de mercado e melhor para digitar no celular, mas exige pacote
  completo: expiração de ~10 min, **limite de tentativas erradas** (sem ele, 1
  milhão de combinações caem rápido) e o cooldown que já existe. A escolha
  depende de onde o fluxo vai viver, o que só se decide junto com o frontend. A
  mudança é localizada: geração e validação do token, sem tocar tabela nem
  endpoint.
- **Domínio próprio.** Sem ele, o Resend só envia de `onboarding@resend.dev` e
  **só entrega para o e-mail dono da conta Resend**. Basta para `dev`; é bloqueio
  absoluto para qualquer usuário real. `petfy-healthcare.com` estava livre em
  2026-08-04.

## Status

**Passos 1 e 2 concluídos em 2026-08-04**, ambos validados contra o `dev` em
`petfy-healthcare-development.up.railway.app`.

**Próximo: fechar o passo 3.** Restam duas tarefas, e as duas são de operação, não
de código: exercitar o lembrete com envio real e então ligar
`REMINDERS_ENABLED=true`. O canal de e-mail já funciona, então nada mais bloqueia.

O Railway constrói a partir do repositório, não da imagem do ghcr — o registry
privado não tem onde receber credencial na UI dele. A imagem continua sendo
publicada pelo pipeline como artefato de deploy, mas nada a consome hoje.

## Decisões já tomadas

| Decisão | Consequência |
|---|---|
| Hospedagem em PaaS de container (Fly/Railway/Render), imagem puxada do ghcr | Postgres gerenciado, HTTPS e domínio vêm do provedor; não há IAM/VPC para montar |
| **Uma instância**, escala vertical | O lembrete de vacina dispensa lock distribuído. Escalar para duas reintroduz o problema — está registrado no passo 3 |
| Monólito organizado por domínio | Os microserviços com RabbitMQ que o README cita como intenção original ficam fora desta rodada; não servem o objetivo de usuários reais |
| Três ambientes, criados em ordem: `dev`, depois `stg`, depois `prd` | Nenhum cliente real chega antes de existir um lugar para homologar. Detalhe abaixo |
| **Backend maduro primeiro, frontend depois** | Nada de frontend está escolhido — nem stack, nem telas, nem mecânicas. A API madura vira a especificação da tela. Ver a Fase 5 |

## Ambientes

A ordem é deliberada e cada ambiente só nasce quando há o que fazer nele:

| Ambiente | Para quê | Quando nasce |
|---|---|---|
| `dev` | Onde o desenvolvimento de tudo que falta acontece, já hospedado — não na sua máquina | **Agora**, no passo 1. É pré-requisito de todo o resto |
| `stg` | Homologação: validar o fluxo completo com dado de mentira antes de qualquer pessoa de fora | Quando o backend estiver fechado o bastante para ser homologado ponta a ponta por API — não depende do frontend existir |
| `prd` | Clientes de verdade | Depois de o fluxo passar por `stg` **e** de existir frontend. Sem tela não há cliente real, então `prd` sem frontend seria ambiente parado |

Os três compartilham a mesma estrutura de configuração e diferem por valor, por
isso o comum mora em `application-hosted.properties` e cada ambiente é um
*profile group* que o inclui. Consequências práticas:

- **Nenhum dado real fora do `prd`.** Copiar base de `prd` para `dev` transforma
  o `show-sql` religado do `dev` em vazamento, e faz o lembrete enviar e-mail
  para tutor de verdade a partir de um ambiente de teste.
- **Envio de e-mail em `dev` e `stg` vai para caixa de captura**, nunca para um
  SMTP que entrega. Vale principalmente no passo 2, que é todo sobre e-mail.
- **`prd` é o único que decide sozinho.** O que quebrar em `dev` ou `stg` não
  gera incidente, então é lá que os passos abaixo são exercitados primeiro.

## Fase 1 — pôr no ar e virar o usuário zero

### 1. Ambiente de dev no ar

A configuração já é toda dirigida por variável de ambiente
(`${VAR:default}` em `application.properties`), então o trabalho aqui é menos
código e mais provisionar. As duas exceções estão na lista.

- [x] Criar o perfil `hosted`, comum aos três ambientes, e os grupos `dev`, `stg`
      e `prd` que o incluem. O perfil `docker` não serve para nenhum deles: o
      datasource dele aponta para `host.docker.internal`, que só existe na
      máquina de desenvolvimento.
- [x] Desligar `spring.jpa.show-sql` fora de desenvolvimento — despejava SQL com
      dados no log. Agora `false` no compartilhado, `true` em `local` e `dev`.
- [x] Adicionar `spring-boot-starter-actuator` e expor apenas `health` e `info`.
      `GET /actuator/health` precisou entrar como rota pública no
      `SecurityConfig`, senão o provedor lê 401 e reinicia a instância em loop —
      coberto no `SecurityFilterChainTest`.
- [x] Provisionar o Postgres gerenciado do `dev` e apontar `DB_*`, por referência
      às variáveis do serviço de banco em vez de cópia de senha. O Flyway aplicou
      as 9 migrations no primeiro boot.
- [x] Definir `JWT_SECRET` no cofre do provedor (mínimo 32 caracteres), **um por
      ambiente**. Sem ele a aplicação não sobe fora do perfil `local`, de
      propósito. Segredo compartilhado entre ambientes faria token de `dev` valer
      em `prd`.
- [x] Definir `SPRING_PROFILES_ACTIVE=dev` no ambiente — **em minúsculo**. Perfil
      no Spring é case-sensitive: com `DEV` o grupo não casa, o `hosted` não
      carrega, e sem `spring.datasource.url` o Spring cai no H2 em memória (o H2
      está com escopo `runtime` no `pom.xml`, então vai junto no jar). O erro que
      aparece é o driver do Postgres recusando uma URL de H2, que não parece nem
      de longe com "faltou ativar o perfil".
- [ ] Decidir o gatilho do deploy de `dev`. A tag `:latest` só é publicada em push
      na `main`, então ou o `dev` acompanha a `main`, ou o pipeline passa a
      publicar tag por branch. Vale escolher agora: é o que define se `dev` serve
      para testar branch antes do merge.

**Pronto quando:** você acessa a API do `dev` de outro dispositivo,
`/actuator/health` responde, e as 9 migrations foram aplicadas num Postgres de
verdade.

### 1b. `stg` e `prd`, quando chegar a hora

Não são passos separados de trabalho: com o `hosted` pronto, subir cada um é
repetir o provisionamento com outros valores. Ficam registrados aqui só para não
virarem surpresa.

- [ ] `stg` ao fim da Fase 2, com dado de mentira e caixa de captura de e-mail.
- [ ] `prd` depois que o fluxo completo passar por `stg`. É aqui que
      `REMINDERS_ENABLED=true` e `NOTIFICATIONS_CHANNEL=email` apontam para envio
      de verdade.

### 2. Ciclo de vida da conta

Hoje quem perde a senha perde o histórico do pet: `PUT /owners/{id}` ignora o
campo `password` de propósito e não existe recuperação. É o bloqueio mais barato
de remover e o que impede qualquer usuário que não seja você.

- [ ] Configurar `spring.mail.*` — **não existe em nenhum arquivo hoje**, só é
      citado num comentário. A dependência `spring-boot-starter-mail` já está no
      `pom.xml`, mas sem host configurado o canal `email` não funciona.
- [ ] Escolher provedor de envio e validar entrega real numa caixa de verdade. O
      `README` registra que o envio nunca foi exercitado contra SMTP real.
- [x] Migration da tabela de token de recuperação (`V11`): uso único, expiração
      curta e **token guardado como hash** — mesmo padrão já adotado em
      `PetShare`. Só para tutor: uma tabela com duas chaves estrangeiras
      opcionais só faz sentido quando o vet também trocar senha.
- [x] `POST /auth/password-reset` (solicita) e `POST /auth/password-reset/confirm`
      (troca com o token). Concluir carimba `password_changed_at`, derrubando as
      sessões abertas — aqui isso importa mais que na troca comum: se a conta foi
      tomada, recuperar precisa expulsar quem entrou.
- [x] Resposta genérica no solicitar, independente de o e-mail existir ou não —
      coerente com a escolha já feita de responder `404` em vez de `403` para
      recurso de terceiro, para não permitir varredura da base. Vale também na
      confirmação: inexistente, expirado e já usado respondem igual.
- [x] Cooldown entre pedidos, senão o endpoint vira máquina de enviar e-mail para
      terceiros. Pedido novo também invalida os anteriores em aberto.
- [x] `PUT /owners/me/password`, exigindo a senha atual. Recusa também nova senha
      igual à atual, senão a troca passa como sucesso sem ter rodado credencial
      nenhuma. É o único `PUT` do projeto com `@Valid`, o que tem teste próprio.
- [x] Invalidar os tokens já emitidos ao trocar a senha (`V10`,
      `password_changed_at` em `owners` e `vets`). A checagem ficou no filtro, e
      não nos providers de owner e vet: assim vale para toda rota autenticada,
      inclusive as que não são escopadas por dono e nunca passariam por um
      provider. A recuperação por e-mail vai reusar o mesmo carimbo — quem
      recupera a conta precisa derrubar quem estava dentro.
- [x] Verificação de e-mail no cadastro. **Decisão: não bloqueia login, suspende
      notificação.** Bloquear criaria atrito no cadastro para proteger contra
      outra coisa — o risco não é a pessoa entrar, é o aviso com nome do pet e do
      tutor sair para o endereço errado. A guarda ficou dentro do `enviar` do
      `ClinicActivityNotifier`, que passou a receber o tutor: assim um aviso novo
      não compila sem passar por ela.

- [x] Provedor de envio escolhido e entrega validada de verdade. **Resend, por
      API HTTP** — o Railway bloqueia saída em porta de SMTP, então Gmail e
      qualquer outro SMTP são inviáveis ali. Custou uma classe: o `Notifier` já
      isolava o transporte do domínio.

**Passo 2 concluído em 2026-08-04**, validado contra o `dev` de ponta a ponta:
cadastro → e-mail de confirmação recebido → confirmado (`204`) → reuso do mesmo
token recusado (`400`) → recuperação pedida (`202`, igual para conta existente e
inexistente) → e-mail recebido → senha trocada → **token de login anterior
passou a responder `401`** e a senha antiga parou de funcionar.

### 3. Lembrete de vacina ligado

A proposta de valor do produto está desligada em produção
(`petfy.reminders.enabled` default `false`). Com uma instância só, o motivo
original da trava — varredura duplicada — deixa de existir.

- [x] `@EnableAsync` e `@Async` **apenas** nos avisos do `ClinicActivityNotifier`,
      que saem dentro da requisição do veterinário e somavam latência de SMTP a
      ela. O que foi para outra thread é só o **envio**: montar a mensagem passa
      por associações lazy que só existem na transação de quem chamou. Pool
      próprio e limitado, com `CallerRunsPolicy` — devolver latência no pico é
      melhor que descartar aviso em silêncio.
- [x] **Não** transformar a rotina de lembretes em assíncrona. Ela já roda fora
      de requisição, por ser agendada, e depende da semântica atual de falha: a
      exceção sobe, o rollback desfaz a marcação em `last_reminder_sent_at`, e a
      dose volta a ser elegível na próxima execução. Async quebraria isso.
- [ ] Exercitar em `dev` e `stg` com caixa de captura, e só então ligar em `prd`:
      `REMINDERS_ENABLED=true` e `NOTIFICATIONS_CHANNEL=email`. Um lembrete errado
      em `prd` chega na caixa de um tutor de verdade e não tem como ser desfeito.
- [x] Log do resultado de cada varredura, **inclusive quando não há nada a
      enviar**. Sem essa linha, uma varredura que rodou e não achou dose fica
      indistinguível de uma que não rodou ou morreu no meio — e a diferença só
      apareceria como tutor reclamando de lembrete que nunca chegou.
- [x] Registrar no `README` que escalar para duas instâncias reintroduz o
      lembrete duplicado, e que o caminho nesse dia é lock distribuído ou
      agendador externo chamando a rotina.

**Pronto quando:** uma dose vencendo gera um e-mail no dia certo, uma vez só, e o
cooldown impede o reenvio no dia seguinte.

## Fase 2 — fechar o contrato da API

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

**O passo 5, o cliente, saiu daqui.** Ele agora abre a Fase 5, depois de o
backend estar maduro — ver a seção Frontend.

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

## Fase 5 — Frontend

### 5. O cliente, quando o backend estiver maduro

**Nada do frontend está decidido.** Tecnologia, telas, o que aparece em cada uma,
navegação, mecânicas de interação — tudo será decidido do zero, provavelmente com
apoio do Claude para a parte de design. Não há stack escolhida, não há protótipo,
e nenhuma decisão anterior deste roadmap presume uma.

**A ordem é deliberada:** primeiro o backend chega a produto maduro, e só então o
frontend é desenhado **em cima do que existir de fato**. O motivo é que a API
madura é a melhor especificação possível para a tela — ela já terá respondido o
que é um pet, o que é uma dose vencendo, quem enxerga o quê e o que pode ser
corrigido. Desenhar tela antes disso significaria adivinhar essas respostas e
depois brigar com elas.

O que já se sabe que a primeira versão precisa mostrar, e mesmo isso está sujeito
a mudar: a carteira do pet, a agenda do que vence e o registro de uma dose. A
agenda é a única parte do sistema que **produz** informação em vez de devolver o
que foi gravado — é ela que justifica um app existir, e provavelmente é o centro
da primeira tela.

**Consequência prática:** enquanto esta fase não começa, tutor real não usa o
Petfy, e `prd` não tem por que existir. A validação de tudo que vem antes é por
API, em `dev` e `stg`.

**Pronto quando:** alguém que nunca viu `curl` cadastra um pet e vê a próxima
dose.

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
