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

**Passos 1, 2 e 3 concluídos em 2026-08-04**, todos validados contra o `dev` em
`petfy-healthcare-development.up.railway.app`. O passo 3 fica **ligado em `dev`**;
a virada em `prd` só faz sentido depois que `prd` existir, o que só acontece
depois da Fase 5.

**Passos 4, 6, 7 e 10-parcial concluídos em 2026-08-04**, e **9 mais as dívidas
operacionais em 2026-08-05**. As Fases 1, 2 e 3 estão fechadas.

**Passo 8 concluído em 2026-08-05**, fatiado em 8a (estrutural, PRs #23 e #24) e
8b (comportamental). Ele vinha primeiro por dependência real, e não por
preferência: mudou quem é titular de um pet, e a exportação LGPD do passo 10
precisa exportar os dados do titular — escrever o export antes seria escrevê-lo
assumindo dono único, para reescrever depois.

**Passo 10 concluído em 2026-08-05**, com consentimento, exportação e log de acesso
(PRs #29 a #34). Só a decisão de CRMV segue aberta, e ela não é de backend.

### A frase que estava errada, e por que importa

Este documento afirmava, até 2026-08-05:

> *"O 8 era o último item que muda a **forma** do dado que a tela mostra — é essa a
> linha que separa 'falta backend' de 'falta frontend'."*

**Não era verdade.** Duas coisas ainda mudavam a forma da tela de maneira profunda, e
as duas foram descobertas ao inventariar o que a aplicação de fato tinha:

- **Não existia campo de arquivo em nenhuma entidade.** O que o tutor possui é papel —
  carteirinha física, laudo, exame em PDF. Sem upload ele digitava o que estava no
  papel e o papel seguia sendo a fonte de verdade: o produto era um caderno digital
  paralelo, não substituto.
- **O histórico de saúde era uma lista de texto livre.** `eventType` e `description`
  como String, sem diagnóstico, sem categoria, sem alergia nem condição crônica.

Anexo é um componente novo em cinco telas, e prontuário muda a leitura *e* a escrita
do histórico. Construir frontend antes deles significaria retrabalhar as telas
centrais.

O passo 8 tinha fechado o que era **mais arriscado** — privacidade e escopo por dono —,
não o que era último em forma de dado. A lição fica registrada porque o critério é
bom: o erro foi aplicá-lo sem inventariar antes.

**Agora a frase vale.** Com anexos e prontuário estruturado, a forma do dado que a tela
mostra está fechada, e **a Fase 5 abre**.

**Próximo: a Fase 5.** O backend cumpriu o que esta rodada prometeu. O que restou de
backend está em *Fica para depois do frontend*, mais abaixo, e é deliberado: são itens
que a tela define melhor do que o modelo.

**Corrigido em 2026-08-06: a Fase 6 entra na frente.** O `PRODUTO.md` fechou nesta
data e reabriu o backend por um motivo que não era visível quando a frase acima foi
escrita — a forma do dado estava fechada, mas o **modelo por trás dela** não era o do
produto que se decidiu construir. Pessoa única sem tipo, custódia separada de acesso e
organização por capacidades mudam o que a tela mostra tanto quanto anexo e prontuário
mudaram. A decisão 14 do `PRODUTO.md` é explícita: fundação inteira antes de qualquer
tela. **É a terceira vez que este documento descobre que "falta frontend" estava
adiantado**, e as três descobertas vieram do mesmo lugar: inventariar antes de afirmar.

**Fase 6 concluída em 2026-08-06**, nos seis cortes planejados — `037f789` é o último.
O modelo agora é o do `PRODUTO.md`: pessoa única sem tipo, custódia separada de acesso
com escopo, organização por capacidades, núcleo de evento com linha do tempo, e
orientação com histórico de cumprimento. **A Fase 5 volta a ser a próxima**, e desta
vez sobre a fundação que a decisão 14 exigia. O que ficou de fora de propósito está
listado no fechamento da fase, mais abaixo.

**Fase 5 aberta em 2026-08-06, pelos três primeiros passos:** a estrutura do cliente virou
a seção 9 do `PRODUTO.md` — três superfícies, uma aplicação com duas áreas, web antes do
app; a stack foi decidida aqui, com o BFF adiado e o gatilho dele escrito; e a direção
visual fechou no `DESIGN.md`, com a tela de referência em `design/home-tutor.html`.
**O próximo passo é o delta de contrato.**

**A Fase 6 ainda não mergeou:** o PR #36 está aguardando o CI, parado por incidente do
GitHub Actions em 2026-08-06, não por código.

**Ordem em que a Fase 4 foi fechada, e por quê.** A sequência não foi por facilidade:

| # | Passo | Veio nesta posição porque |
|---|---|---|
| 1 | Consentimento (V16) | é pré-requisito de compliance, não feature: sem ele a aplicação tratava dado de saúde sem base legal registrada |
| 2 | Anexos (V18) | maior alavanca de produto por unidade de trabalho, e o que mais muda a tela — antes do frontend, não depois |
| 3 | Exportação | **depois** dos anexos, para já incluir os arquivos. Antes, seria reescrita quando eles chegassem |
| 4 | Prontuário estruturado (V19) | último porque é o que mexe em dado que já existe |
| 5 | Log de acesso (V17) | fecha a auditoria dos dois lados: havia rastro de escrita, nenhum de leitura |

O consentimento e o log de acesso vieram antes na prática por serem pequenos e
independentes; a dependência real era só **anexos antes de exportação**.

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
| **Backend maduro primeiro, frontend depois** | A ordem se cumpriu. Desde 2026-08-06 **estrutura e stack do cliente estão decididas** — seção 9 do `PRODUTO.md` e Fase 5 aqui. Tela e mecânica continuam em aberto |

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

**Decisão de 2026-08-05: `stg` não sobe agora.** A versão anterior deste
documento o previa "ao fim da Fase 2", o que já teria vencido. Fica adiado de
propósito: todo o desenvolvimento continua no `dev` até existir um **MVP maduro,
já com frontend**, e é esse MVP que estreia o `stg` como ambiente de
homologação. Homologar antes de haver o que homologar ponta a ponta seria manter
um ambiente parado — e o `dev` hoje cumpre o papel de validar cada passo por API.

Consequência a não esquecer: enquanto isso, `dev` acumula os dois papéis, então
**nenhum dado real entra nele** e o envio de e-mail continua limitado ao endereço
dono da conta Resend.

- [ ] `stg` quando o MVP com frontend estiver pronto para ser homologado, com
      dado de mentira e caixa de captura de e-mail.
- [ ] `prd` depois que o fluxo completo passar por `stg`. É aqui que
      `REMINDERS_ENABLED=true` e `NOTIFICATIONS_CHANNEL=email` apontam para envio
      de verdade.

### 2. Ciclo de vida da conta

Hoje quem perde a senha perde o histórico do pet: `PUT /owners/{id}` ignora o
campo `password` de propósito e não existe recuperação. É o bloqueio mais barato
de remover e o que impede qualquer usuário que não seja você.

- [x] Configurar o canal de e-mail. Terminou **sem `spring.mail.*`**: o Railway
      bloqueia saída em porta de SMTP, então o envio é por API HTTP do Resend.
- [x] Escolher provedor de envio e validar entrega real numa caixa de verdade.
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
- [x] Exercitar em `dev` com envio real e ligar `REMINDERS_ENABLED=true` lá.
      Validado em 2026-08-04: dose vencida gerou um e-mail no dia certo, e o
      cooldown de 7 dias impediu o reenvio na varredura seguinte. `stg` e `prd`
      seguem pendentes por não existirem; a virada de `NOTIFICATIONS_CHANNEL` em
      `prd` só faz sentido quando houver tutor real chegando pelo frontend.
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

- [x] Expor OpenAPI. Entrou o **springdoc 2.6.0**, e não a linha 1.6.x que este
      documento previa: a migração para o Boot 3 foi feita antes, no mesmo dia,
      justamente para não entrar na linha velha e trocar depois.
- [x] Revisar os nomes antes de um cliente fossilizar o contrato.
      `POST /{recurso}/include` virou `POST /{recurso}` e `GET /{recurso}/all`
      virou `GET /{recurso}` — os dois indefensáveis foram embora.
- [x] Decidir o que fazer com o `PUT` parcial. **Fica parcial**, e passa a ser
      documentado como tal em vez de ser surpresa: campo ausente preserva o valor,
      e `@Valid` vale só no `POST` porque os dois compartilham DTO.

**Passo 4 concluído em 2026-08-04.** Uma consequência que só apareceu depois:
as listagens deixaram de devolver array e passaram a devolver `Page` no dia
seguinte, com as dívidas operacionais. O contrato mudou um dia após ser
publicado — sem custo porque não há cliente, e é exatamente esse o motivo de a
Fase 5 vir por último.

**O passo 5, o cliente, saiu daqui.** Ele agora abre a Fase 5, depois de o
backend estar maduro — ver a seção Frontend.

## Fase 3 — o domínio que o uso real cobra

### 6. Espécie como dado, não texto livre ✅

Concluído em 2026-08-04. `Pet.species` virou enum `{CANINA, FELINA}` NOT NULL;
`Pet.type` continua como sub-classificação livre (raça, cor, o que o tutor
quiser). O catálogo já tinha `species` como texto — passou também a enum, mesmos
valores. `GET /vaccine-catalog?petId=<uuid>` devolve só as vacinas da espécie
daquele pet, e `VaccineFactory` recusa com 409 (`SPECIES_MISMATCH`) se o
catálogo escolhido não bater com a espécie do pet.

Backfill dos pets existentes: default CANINA, com heurística `type LIKE '%gat%'`
→ FELINA. Consciente que é imperfeito, mas dev tem pouco dado e o tutor pode
corrigir.

### 7. Protocolo de filhote ✅

Concluído em 2026-08-04. Catálogo ganhou `initial_dose_count`,
`initial_dose_interval_days` e `mandatory`. Ao cadastrar pet com idade ≤ 120
dias (`petfy.puppy.max-age-days`), o `PuppyProtocolService` gera doses
planejadas (`application_date = null`, `next_dose_date` a partir de hoje) para
cada vacina `mandatory` da espécie. Só vacinas marcadas mandatory disparam o
schedule — evita duplicar (V3 e V4 felinas cobrem o mesmo pet, só a V4 entra;
antirrábica sim, obrigatória por lei).

Escolha registrada: doses são contadas a partir de HOJE, não da data de
nascimento. Se o pet já passou de alguma dose recomendada, calcular
retroativamente criaria vacinas com data passada e sem aplicação — confunde a
agenda e não ajuda o tutor. Mais útil assumir que a próxima dose começa agora.

## Fase 4 — quando o usuário não é você

### 8. Multi-tutor e transferência de titularidade — concluído

`Pet` tinha um `owner` só (`@ManyToOne`). Casal e família dividem o mesmo pet, e
o segundo tutor só conseguia um link de leitura; adoção e venda não tinham
caminho. Mexe no escopo por dono, a parte mais sensível do sistema — as consultas
por UUID já têm cobertura contra Postgres real (`UuidQueriesContainerTest`) para
apoiar a mudança.

**Pronto quando:** dois logins distintos editam o mesmo pet e nenhum dos dois
alcança pet de terceiro.

#### O passo foi fatiado em 8a e 8b

Ao executar, ficou claro que sao duas mudancas com riscos diferentes dentro de um
numero so, e junta-las num PR dificultaria revisar justamente a parte que mexe em
privacidade:

- **8a — estrutural.** O pet passa a ter tutores e a autorizacao vira uma peca
  unica. O comportamento externo muda pouco: quem cadastra o pet continua sendo o
  unico tutor dele.
- **8b — comportamental.** Convite de co-tutor, transferencia de titularidade e
  os endpoints de gestao. E o que entrega o valor visivel do passo.

#### 8a — concluido em 2026-08-05 (PR #23 e #24)

**Pronto e compilando:**

- `PetTutor` e `PetTutorRole` (HOLDER, EDITOR, VIEWER — hierarquia pela ordem da
  declaracao). Tres papeis, e nao dois: quem cuida junto precisa registrar
  vacina, quem so acompanha nao precisa alterar nada.
- `PetTutorInvite`, no molde do `ClinicInvite`, ja modelado — usado no 8b.
- `V15`: cria `pet_tutors`, faz o backfill de todo dono atual como `HOLDER`,
  garante **um titular por pet** com indice unico parcial, e **remove
  `pets.owner_id`**. A coluna nao foi mantida ao lado da tabela de proposito: com
  as duas, "quem e o dono" teria duas respostas, e o dia em que divergissem seria
  um vazamento.
- `PetAccessGuard`: as seis copias de
  `pet.getOwner().getOwnerId().equals(...)` viraram uma regra so, com tres
  niveis. Pet inalcancavel responde 404; falta de nivel responde 403, que so
  acontece com quem ja e tutor e por isso nao revela nada novo.
- **Lembrete e aviso de clinica vao para todos os tutores** com e-mail
  confirmado. A dose e marcada como avisada **uma vez so**, fora do laco: um pet
  compartilhado aparece no lembrete de mais de uma pessoa.
- **Exclusao de conta deixou de ser cascata cega.** Pet sem outro tutor morre
  junto; pet com outro tutor sobrevive e so perde o vinculo. Se quem sai era o
  titular, a titularidade passa ao vinculo mais antigo — o indice do banco exige
  exatamente um `HOLDER`, e um pet sem titular ficaria sem ninguem que pudesse
  convida-lo ou apaga-lo.

**Os testes, fechados depois:** a suite passou de **392 com 78 falhando** para
**436 verdes**, com os container tests executando. As falhas nao eram regressao —
eram testes que verificavam o escopo por dono **dentro de cada servico**, e essa
regra mudou de lugar. Adapta-los mecanicamente para voltarem a passar seria
mentir sobre o que eles cobrem, entao:

1. **`PetAccessGuardTest`** (19 casos) — os tres niveis contra os tres papeis, e
   as duas fronteiras que importam: quem nao e tutor recebe **404**, nunca 403; o
   403 so aparece para quem ja e tutor e falta nivel. Cobre tambem o pet apagado
   entre o vinculo e a leitura, que responde igual a quem nunca alcancou.
2. **Os testes de servico perderam a comparacao de dono** e ganharam um nested
   `nivel exigido do guard` por servico: e a tabela "operacao → nivel", a unica
   coisa que sobrou sob responsabilidade do servico. Pedir leitura onde precisa de
   escrita nao quebra nenhum teste de comportamento, mas deixa um leitor editar o
   pet — esses casos existem para pegar exatamente isso.
3. **`SchemaMigrationContainerTest` aprendeu a V15**, incluindo o que ela promete
   e nao dava para afirmar pelo mapeamento: `pets.owner_id` sumiu, e o **banco**
   recusa um segundo `HOLDER` e a mesma pessoa duas vezes no mesmo pet.
4. **`UuidQueriesContainerTest`** passou a gravar o vinculo de verdade. Ele
   montava `Pet.tutors` so em memoria, e como a relacao e `mappedBy` sem cascade,
   toda consulta por tutor voltava vazia — o teste acusava o escopo por dono
   quando o que faltava era a linha no banco. Ganhou tambem o caso que so existe
   depois da V15: pet com dois tutores aparece para os dois, e para mais ninguem.

**Dois achados fora do combinado, corrigidos no caminho:**

- **Listar o historico de saude exigia EDITOR.** Passava por `findPet`, que pede
  escrita. O tutor VIEWER — a avo que so acompanha — nao conseguiria *ler* o
  historico, que e a razao de o papel existir. Virou `requireLeitura`.
- **Sobrou dependencia morta do refactor.** Cinco servicos ainda resolviam
  `currentOwnerProvider.require().getOwnerId()` numa variavel que ninguem lia, e
  carregavam `PetRepository` sem usar. Saiu, junto com o nome `buscarDoOwner-
  Autenticado`, que mentia depois que o dono unico deixou de existir
  (`buscarAlcancavel`). Concessao de clinica e link de compartilhamento ficaram
  como estavam, exigindo escrita: nao sao "a carteira e a agenda" que o VIEWER
  acompanha, e mexer nisso seria decisao de produto, nao limpeza.

#### 8b — concluído em 2026-08-05

A tabela de tutores que o 8a criou finalmente ganha um segundo tutor. **O passo 8
fecha aqui:** dois logins distintos editam o mesmo pet, e nenhum dos dois alcança
pet de terceiro — verificado contra Postgres real, não contra mock.

**Convite, e não vínculo direto.** O caso comum é o cônjuge que **ainda não tem
conta**: exigir cadastro prévio mataria o fluxo onde ele começa. E vincular
alguém sem que aceite faria a pessoa passar a receber lembrete que não pediu.

- `POST /pets/{petId}/tutors/invites` — só o titular. Um EDITOR que pudesse
  convidar contornaria a regra: bastaria convidar um comparsa como HOLDER para
  tomar o pet de quem o cadastrou.
- `POST /pet-tutor-invites/{token}/accept` — fora de `/pets/{petId}` de propósito:
  quem aceita ainda não alcança o pet, e pedir o petId na URL daria de graça um
  jeito de testar se um id existe.
- `GET`/`DELETE` dos convites, `GET` dos tutores, `PATCH` do papel, `DELETE` do
  tutor e `POST .../transfer-holder`.

**O e-mail do convite é obrigatório, ao contrário do de clínica.** Ele trava quem
aceita. Sem isso o link viraria portador, e quem o recebesse encaminhado entraria
no histórico de saúde de um animal alheio. O sistema **não envia** o convite: o
token volta uma vez na resposta e quem convidou entrega, igual ao `ClinicInvite`.

**Duas saídas pela mesma porta.** O titular remove um co-tutor; um co-tutor remove
a si mesmo, sem depender de ninguém — exigir autorização para sair prenderia a
pessoa a notificações de um pet que não é dela. O titular nunca sai por aqui, nem
por vontade própria: transfere primeiro, ou apaga o pet.

**Transferência preserva o antigo titular como EDITOR**, em vez de removê-lo: quem
cuidou do animal até ontem continua enxergando a carteira, e o novo titular decide
se remove. `HOLDER` não passa pelo `PATCH` de papel — promover alguém rebaixa o
titular atual, então não é editar um tutor, é a transferência.

**O risco do 8b não era regra, era ordem de comandos.** Toda troca de titularidade
passa por um instante com dois candidatos a HOLDER, contra um índice único parcial.
Rebaixar vem antes de promover, com `flush` explícito, nos dois caminhos (aceite de
convite HOLDER e `transfer-holder`). Mock não tem índice — por isso o
`PetTutorFlowContainerTest` exercita o serviço contra Postgres.

**O convite trouxe três FKs novas** (`pet_id`, `created_by`, `accepted_by`), e o
schema não tem `ON DELETE CASCADE` em lugar nenhum: apagar o pet e apagar a conta
passaram a limpar convites antes, senão a FK segura o delete. Mesma família do bug
corrigido no PR #25.

**Cobertura:** 500 testes verdes. `PetTutorServiceImplTest` (36) para as regras e a
tabela operação → nível; `PetTutorFlowContainerTest` (19) para o que só o banco
recusa; `ControllerPathVariableTest` ganhou as rotas de tutor, as únicas com duas
path variables do mesmo tipo — trocar `petId` por `ownerId` compila e passa por
revisão sem chamar atenção.

#### 8c — aviso de mudança de tutores, concluído em 2026-08-05

Ficou fora do 8b de propósito, para não inflar o PR que precisava de revisão
cuidadosa, e entrou logo depois. `PetTutorActivityNotifier`, irmão do
`ClinicActivityNotifier`: lá o aviso é sobre o que uma clínica escreveu no pet, aqui
é sobre **quem passou a poder escrever**.

Três eventos, todos de privacidade:

- **Alguém entrou no pet** — os outros tutores sabem que uma pessoa nova lê o
  histórico de saúde, e em que papel.
- **A titularidade mudou** — vai para todos os tutores, não só para os dois
  envolvidos: quem é titular define quem pode convidar, remover e apagar o pet.
- **Um tutor saiu** — e **quem foi removido também é avisado**, quando não saiu por
  conta própria. Perder acesso ao histórico de um animal que se cuidava e descobrir
  tentando abrir a carteira é a pior versão deste evento.

**Quem age não recebe o aviso** do que acabou de fazer, e aceitar convite de `HOLDER`
gera **um** aviso, não dois — entrar no pet e virar titular são o mesmo fato.

Não avisam: convidar (ninguém entrou ainda) e trocar `EDITOR`↔`VIEWER` (muda o que a
pessoa pode fazer, não quem alcança o pet).

**Os destinatários vêm de consulta, não de `Pet.getTutorOwners()`.** A coleção é lazy
e acabou de ser mexida — vínculo inserido, papel alterado —, então ler dela daria uma
lista que pode não refletir o que foi gravado, e o aviso iria para o conjunto errado
de pessoas. Que é a única forma de este recurso piorar a privacidade em vez de
melhorar. No caso da remoção há um `flush` explícito entre o delete e a consulta, pelo
mesmo motivo.

Herdou do `ClinicActivityNotifier` o que já estava decidido: envio assíncrono fora do
caminho da requisição, falha só logada — o vínculo já está gravado e perdê-lo porque o
e-mail caiu seria trocar um problema pequeno por um grande —, `try` por destinatário
para que falhar com um tutor não cale os outros, e **nada sai para e-mail não
confirmado**.

### 9. Além da vacina: antiparasitário e peso como série — concluído

Vermífugo e antipulgas são o recorrente que o tutor de fato esquece, e
reaproveitam agenda e lembrete já construídos. `weight` hoje é um `Double`, um
valor único — virar histórico dá curva de crescimento do filhote.

- [x] `Antiparasitic` e `AntiparasiticCatalog`, entidades paralelas a `Vaccine`.
      Separadas em vez de um campo em `Vaccine`: evita breaking change na tabela
      e no contrato, e o scheduler varre as duas sem canal novo.
- [x] `pet_weight_history` guarda cada medição. `Pet.weight` continua existindo
      como espelho da mais recente, para os endpoints que já devolvem o pet não
      ganharem um join. **O espelho não regride** quando o tutor lança uma
      pesagem antiga esquecida — quem define o espelho é a maior `measured_at`,
      não a última inserção.
- [x] `kind` e `species` do antiparasitário são enums (`AntiparasiticKind`,
      `Species`), não texto livre. Escrito antes do passo 6 entrar, o código
      nascia com `String` nos dois e reintroduzia exatamente o furo que o passo 6
      tinha acabado de fechar: dava para registrar vermífugo canino num gato.
      Agora recusa com o mesmo `409` do `VaccineFactory`, na criação e também ao
      mover o registro para um pet de outra espécie.
- [x] `GET /antiparasitics/catalog?petId=` filtra pela espécie do pet, mesmo
      contrato do `/vaccine-catalog`.

**Passo 9 concluído em 2026-08-05.** Validado por 26 testes novos de serviço e
pela `V14` aplicada num Postgres real — ver a nota sobre os testes de container
abaixo, que era pré-requisito silencioso para essa validação existir.

### 10. LGPD e confiança — concluído em 2026-08-05

- [x] **Exclusão a pedido do titular.** `DELETE /owners/me` agora apaga em
      cascata todo o rastro do tutor: correções → vacinas/histórico/shares/
      acessos por clínica → pets → tokens → owner. **Política escolhida: apagar,
      não anonimizar nem transferir.** Anonimizar deixaria dado de saúde
      associado a um "ex-tutor fantasma" que o vet ainda enxerga — contraria o
      pedido de sair. Transferir pressupõe multi-tutor (passo 8), que não
      existe. Cascata é o que atende ao pedido sem meio-termo. Cobertura por
      teste de mock (`OwnerServiceImplTest.deveApagarEmCascataNaOrdemCerta`) e
      pelo `OwnerDeletionContainerTest` no CI. Concluído em 2026-08-04.
- [x] **Registro de consentimento** (`V16`, PR #29). **Faltava antes dos outros dois e
      não estava neste roadmap:** havia exclusão e havia export planejado, mas nenhum
      aceite, nenhuma versão de política, nenhum instante. A LGPD não pede só que o
      titular possa sair — pede que a base legal seja **registrada e demonstrável**, e
      não havia como provar que alguém consentiu com nada.

      Guarda a **versão** do documento, não um booleano: política muda, e
      `aceitou = true` de janeiro não diz com o que a pessoa concordou depois de agosto.
      Trocar `CONSENT_PRIVACY_VERSION` faz os aceites anteriores virarem pendentes, sem
      deploy. Sem backfill — nenhuma migration pode inventar consentimento. Sem rota de
      revogação: revogar sem sair criaria um estado em que a aplicação guarda dado sem
      poder tratá-lo, então revogar é `DELETE /owners/me`.
- [x] **Exportação a pedido do titular** (`GET /owners/me/export`, PR #33). Veio
      **depois dos anexos**, para já incluir os arquivos. Dado de terceiro entra
      reduzido — co-tutor por nome e papel, sem e-mail —, e o documento **declara as
      próprias limitações** no campo `limitacoes`, porque quem o abre meses depois não
      tem o Swagger ao lado. Metade dos testes é sobre o que ele *não* carrega: senha,
      hash de token de share, chave de storage.
- [x] **Rastro de acesso a dado de saúde** (`V17`, PR #30) — mais amplo do que este
      roadmap previa. A previsão era só o link público; o buraco maior era que **havia
      rastro de escrita e nenhum de leitura**: um veterinário abria o histórico completo
      e o tutor nunca saberia. `GET /pets/{petId}/access-log` cobre clínica autorizada
      **e** link público. Leitura de tutor e de co-tutor fica fora de propósito — o log
      responde "quem *mais* viu isto".

      **Falha ao gravar derruba a leitura**, ao contrário da política de notificação:
      ali o aviso era acessório, aqui o log *é* a garantia, e log de auditoria que falha
      em silêncio é pior que log nenhum.
- [ ] **CRMV/clínica não verificada.** Decisão pendente — mistura produto e
      compliance (validação de CRMV exige integração externa). **Não é item de
      backend:** o que falta é a decisão, não a implementação.
- [ ] **Limite de acessos por link de carteira.** Não entrou. Com o log de acesso
      existindo, agora é contar linhas por `petShareId` e recusar acima do teto — o
      trabalho que sobrou é decidir o número.

**Pronto quando:** você atende um pedido de exclusão sem abrir o banco na mão.
**Atendido**, e mais: exclusão, exportação, consentimento registrado e rastro de quem
lê. Só a decisão de CRMV segue aberta.

### 11. Anexos — concluído em 2026-08-05

**Não existia campo de arquivo em nenhuma entidade.** O que o tutor possui é papel, e sem
upload ele digitava o que estava no papel — o produto era caderno digital paralelo, não
substituto. O OCR de RG animal é o retrato: lê a imagem e a descarta.

Uma tabela ancorada em `pet_id` **obrigatório**, com `vaccine_id`/`health_record_id`
opcionais dizendo o que o arquivo documenta. O `pet_id` faz dois trabalhos: é a âncora de
**autorização** — toda pergunta sobre quem vê o arquivo se reduz a quem vê o pet, que o
`PetAccessGuard` já responde — e a de **limpeza**, porque o `PetPurger` apaga por `petId`.

**Tipo detectado por magic bytes**, não pelo `Content-Type`: o declarado vem do cliente, e
um executável renomeado para `.pdf` chega anunciado como PDF. O que fica **gravado** é o
detectado. SVG fica fora apesar de ser imagem — é XML com script dentro.

**Sem URL assinada, deliberadamente.** É o padrão para arquivo público, mas troca "checar
autorização a cada download" por "quem tiver o link entra até expirar", e link encaminhado
por engano é o caso que o passo 8 inteiro tentou evitar. Efeito colateral bom: a interface
de storage virou armazém de bytes puro, e trocar filesystem por S3/R2 não encosta em regra
de negócio.

**Storage em filesystem serve `local` e `dev`, não produção com mais de uma instância** —
disco local não é compartilhado. É coerente com "uma instância, escala vertical" nas
decisões tomadas; o dia em que deixar de ser, a substituição é uma classe nova.

Na exclusão **os bytes saem junto**: o storage não participa da transação, então as chaves
são lidas antes de qualquer delete — arquivo órfão com laudo dentro é dado pessoal não
apagado.

### 12. Prontuário estruturado — concluído em 2026-08-05

O histórico de saúde era `eventType` e `description` como String livre. Não havia como
perguntar quais pets tiveram dermatite no último ano, nem montar uma tela que um
veterinário reconheça como prontuário.

- **`category` e `diagnosis`** no atendimento. A categoria **não substitui** `eventType`:
  trocar String por enum exigiria mapear todo valor gravado, e o que não casasse seria
  descartado. O rótulo livre continua ao lado da classificação.
- **Alergias e condições crônicas** viram tabela, não campo de texto — porque precisam
  aparecer em **destaque**. Alergia a anestésico perdida num texto corrido é o tipo de
  informação que só se descobre que faltava depois de um procedimento. As duas na mesma
  tabela: têm a mesma forma e a mesma razão de existir.
- **Gravidade só em alergia**, por `CHECK` no banco *e* validação no serviço — o banco
  impede insert direto, o serviço impede que o cliente receba erro de integridade como 500.
- **Condição é encerrada, não apagada.** `resolvedAt` tira das ativas sem esconder que
  existiu.
- **Número do microchip**, e não só o booleano: é o identificador legal do animal e o que
  liga o Petfy a registro de animal perdido. Índice parcial, porque busca por chip de
  animal perdido não pode varrer a tabela. Mais **castração com data**.

**Sem backfill inteligente**, e isso foi uma simplificação deliberada depois de a base de
`dev` ser declarada descartável: classificar o texto antigo por `LIKE` seria adivinhação
sobre dado que vai ser zerado. A coluna entra `NOT NULL DEFAULT 'OUTRO'` e o default sai
logo depois — roda em base cheia ou vazia, e `OUTRO` não vira o valor silencioso de quem
esqueceu.

## Como a suíte deixou de mentir

Vale registrar separado, porque foi o fio condutor de toda a Fase 4 e a lição não é sobre
nenhum passo em particular.

**Seis bugs de produção foram encontrados nesta rodada, todos da mesma família:** chave
estrangeira ou índice único que **só o banco recusa**, com o código coberto apenas por
mock. Dois deles no caminho de exclusão da LGPD, que este roadmap dava como concluído.

| O que quebrava | Como passou |
|---|---|
| `DELETE /owners/me` com pet compartilhado | mock não tem índice único parcial |
| `DELETE /pets/{id}` com qualquer vacina | mock não tem FK |
| `DELETE /owners/me` com pesagem ou antiparasitário | o passo 9 adicionou tabelas e não atualizou a cascata |
| FKs novas de convite, log de acesso, anexo e condição | cada tabela nova repetia o problema |

A causa raiz não era falta de `ON DELETE CASCADE` — era **duas listas de deletes
duplicadas** que divergiram. Virou o `PetPurger`, uma lista só.

E porque uma lista única ainda pode ficar **desatualizada**, o
`PetPurgerCoverageContainerTest` pergunta ao **próprio schema** quem alcança `pets` —
transitivamente, para pegar netas — e **quebra o build** se alguma tabela ficar fora. Ele
já pegou duas tabelas novas em flagrante durante esta rodada. É a proteção que o cascade
daria, sem perder a exclusão visível em código nem a regra condicional de que um pet com
outro tutor sobrevive.

**E a rede de segurança já foi desligada em silêncio duas vezes.** As classes de container
são *puladas* — não falham — quando o Testcontainers conclui que não há Docker, e essa
conclusão não é confiável: `isDockerAvailable()` engole qualquer `Throwable` e devolve
`false`. Nos dois casos o resultado foi **build verde com zero migration validada contra
Postgres**. Duas mitigações pontuais para o mesmo sintoma indicavam que faltava o guarda:
o `ContainerTestsHabilitadosTest` agora **falha** em vez de deixar passar. Máquina sem
Docker roda com `-Dpetfy.allow-skipping-container-tests=true` — escolha explícita, que
aparece no comando em vez de num número que ninguém lê.

**A causa da segunda ocorrência segue não identificada.** Fixar o heap no surefire faz o
sintoma desaparecer de forma reproduzível, mas isso é evidência, não explicação — e está
dito assim no `pom`, em vez de uma causa inventada documentada como fato.

## Fase 5 — o cliente

### 5. O cliente — reaberto em 2026-08-06, sobre a seção 9 do `PRODUTO.md`

A frase de 2026-08-05 dizia que *"nada do frontend está decidido — nem stack, nem
telas, nem mecânicas"*. Continua valendo para tela e mecânica; deixou de valer para
estrutura e stack, e é isto que esta seção passa a registrar.

#### A sequência

| # | Passo | Estado | Por que nesta posição |
|---|---|---|---|
| 1 | **Estrutura do cliente** | Feito — seção 9 do `PRODUTO.md` | É insumo dos dois seguintes. Sem ela escrita, "de que área é esta tela?" volta a ser decidido em cada tela |
| 2 | **Stack** | Decidida em 2026-08-06 | Antes do design: metade das decisões de design se amarra a ela — componente, token, tema |
| 3 | **Direção visual** | Feito — `DESIGN.md`, seis seções, mais `design/home-tutor.html` | Antes do contrato, porque a tela define quais leituras o backend precisa |
| 4 | **Delta de contrato** | **Concluído em 2026-08-07** — P1 a P6 | Só depois de saber **quais** telas existem é que se sabe quais leituras o backend precisa. A única cobrança já certa é a leitura em volume (9.5) |
| 5 | **Construir** | **Planejado em 2026-08-07, não iniciado** — plano no fim desta fase | Começa por dois pré-requisitos de backend que este roadmap decidiu e nunca construiu: CORS e o refresh em cookie `httpOnly` |

**Por que o delta de contrato vem depois do design, e não antes.** O OpenAPI é gerado
das anotações, então **ele está correto** — quem mente é o `README.md`. O design pode se
apoiar no contrato real desde já, e a reescrita do README deixa de ser bloqueio para
virar entrega do passo 4, junto do que o design tiver cobrado.

#### A stack decidida

| Peça | Escolha | Por quê |
|---|---|---|
| Base | **Vite + React + TypeScript**, SPA | Bundle estático distribuído por CDN, e caminho direto para o app em Expo |
| Estado de servidor | **TanStack Query** | Cache, revalidação e estado de erro que não se escreve à mão |
| Rota | **TanStack Router** | Tipado ponta a ponta: rota inexistente não compila |
| Contrato | **Cliente gerado do OpenAPI** | Fonte única. Se um endpoint muda de forma, o build do front quebra — a divergência que aconteceu com o README não tem como se repetir |
| Tabela | **TanStack Table** | É a área de organização inteira (9.3) |
| Estilo | **Tailwind com tokens próprios** | Ver as referências, abaixo |
| Componente | **Headless — Radix ou React Aria** | Comportamento, foco, teclado e acessibilidade deles; visual inteiramente nosso |
| i18n | **Desde a primeira tela** | Retrofit de i18n é das piores dívidas que existem, e a linha do tempo é toda timestamp |

#### O que foi descartado, e por quê

| Opção | Motivo |
|---|---|
| **Next.js** | O presente dele é o BFF embutido, e o BFF foi adiado. Sobra SSR que a área logada não usa e SEO que o cartão **não pode ter** (9.2). E o BFF dele nasce casado com a web: quando o app chegar, extrai-se outro de qualquer forma |
| **Angular** | Segundo lugar de verdade, não consolo: modelo mental de Spring, opinionado, e a área de organização é o terreno dele. Perdeu pelo caminho do app — Capacitor é webview, e nada se reaproveita |
| **Java na tela** (Vaadin, Thymeleaf + HTMX) | Entrega tela na primeira semana, num idioma só. Não sobrevive ao plano: microserviço, BFF e app pressupõem cliente desacoplado, e o Vaadin ainda guarda estado de UI no servidor — o oposto de escalar horizontalmente |

#### O BFF, adiado — com gatilho escrito

**BFF é por superfície, não do sistema.** Um por cliente: web e app têm necessidades
opostas — a web pede lista de duzentos com filtro, o app pede o mínimo para uma
notificação e um toque. Um BFF que sirva os dois vira um segundo monólito com outro
nome.

**O argumento a favor dele é a virada para microserviços.** Quando o monólito se
dividir, quem re-agrega é o BFF, e o contrato que o cliente enxerga não muda. Sem BFF,
cada quebra de serviço vira mudança de tela.

**Por que não agora:** há um cliente e um backend. BFF hoje é um deploy a mais, um
contrato a mais e um salto de latência sem contrapartida. O único argumento de dia um
seria autenticação — SPA guardando JWT em `localStorage` é exposição a XSS —, e isso sai
mais barato resolvido no Spring que já existe, com refresh em cookie `httpOnly`, do que
construindo um serviço inteiro.

**O gatilho, escrito para não depender de memória.** O primeiro dos três que acontecer:

1. o app entrar;
2. o monólito começar a se dividir;
3. uma tela precisar de três chamadas em sequência para existir.

**A regra que mantém o BFF possível, e é de dia um:** nenhuma tela chama HTTP. Todo
acesso passa por uma camada de dados única, gerada do contrato. Quando o BFF entrar,
mudam a base e as assinaturas geradas — não as telas.

#### Repositório único

**Mesmo repositório, e não como provisório.** O cliente gerado do OpenAPI só protege de
divergência se contrato e consumidor forem versionados juntos; em repos separados, "qual
versão do front fala com qual versão da API" volta a ser problema de gente lembrar.

Dois custos, ambos de configuração:

- **CI com filtro por caminho.** Hoje qualquer push roda os 705 testes Java. Mudança de
  CSS não pode disparar isso, e mudança de controller precisa disparar a regeneração do
  cliente.
- **Um segundo serviço no Railway**, com *root directory* apontando para a pasta do
  front.

Separar depois é barato, e só se justifica com time próprio ou cadência de release
própria. Não existe nenhum dos dois.

#### As referências, e a correção que elas sofreram

| Eixo | Referência | O que se aproveita |
|---|---|---|
| Arquitetura | **SoundCloud** | Onde o BFF nasceu, e pelo mesmo motivo que o nosso vai nascer: monólito virando serviços, com clientes de necessidades diferentes |
| Arquitetura | **Netflix** | Backend JVM com cliente desacoplado e camada por dispositivo, em escala |
| Arquitetura | **Mercado Livre** | A condição mais parecida com a nossa: Java pesado, escala continental, produto de duas pontas |
| Estrutura | **Airbnb** | Uma conta, duas áreas, troca explícita — é a 9.3 rodando em produção há anos |
| Produto | **Apple Health** | Registro longitudinal, compartilhamento com escopo e prazo, e a postura de que o dado é do titular |
| Produto | **Oura, Whoop** | A régua da promessa 5.5: descrever padrão, mostrar evidência, nunca diagnosticar |
| **Sensação** | **Chewy, Petlove, Rover, PetDesk, Digitail, Zee.Dog** | **O nicho.** É daqui que sai o que o produto deve *parecer*: cuidado, acolhimento, segurança — e não plataforma de dados |
| Conteúdo | **NHS Design System** | Linguagem de saúde que informa **sem alarmar** (4.5), e acessibilidade levada a sério |
| Conteúdo | **GOV.UK** | Clareza sem decoração — vale para texto, não para forma |
| Tom | **Nubank** | Português real, sem jargão, numa categoria de baixa confiança |
| Tom | **Monzo** | Tom de voz documentado, e transparência como decisão de design |
| Densidade | **Linear, Stripe** | A régua da área de organização |

**A correção, registrada porque custou uma rodada.** A primeira leitura destas referências
deu peso demais ao NHS e ao GOV.UK e produziu uma direção austera — *"documento, não
painel"* —, que ao ser renderizada leu como prontuário e ferramenta de gestão. O titular
reprovou, com a razão certa: **quem define a sensação de um produto de animal são os
produtos de animal.** NHS e GOV.UK continuam valendo para **conteúdo e acessibilidade**, e
saíram da definição de forma. A lição maior é de método: **cor e tipografia aprovadas em
texto não valem** — só valem renderizadas.

**E o critério negativo, que é exigência do titular.** O visual de projeto gerado tem
assinatura reconhecível — cinza slate, Inter, gradiente, herói centralizado, card branco
com sombra em grade de três. É por isso que o componente é **headless** e não
pré-estilizado: biblioteca que entrega componente já vestido entrega junto a cara dela.
A direção fechada, a paleta com contraste medido e a lista de proibições estão no
`DESIGN.md`; a tela de referência, em `design/home-tutor.html`.

#### O que a Fase 5 antiga dizia e não vale mais

| Dizia | Agora |
|---|---|
| *"Papel manda no que aparece"* — `VIEWER` lê, `EDITOR` escreve, só `HOLDER` convida e transfere | Papel morreu na Fase 6. Quem decide alcance é **custódia** e **acesso com escopo**, e o alcance vem junto do recurso |
| *"A carteira do pet, a agenda e o registro de uma dose"* como centro da primeira tela | O centro da área do tutor é o **feed de pendências** (decisão 2, e 9.3). A agenda é uma das fontes dele, não o todo |
| *"Nada de frontend está decidido"* | Estrutura e stack estão. Tela e mecânica, não |

**Continua valendo, e a tela precisa respeitar:**

- **Download de anexo passa pela API**, não por URL assinada — cada download é
  autenticado, e não um `src` colado.
- **Consentimento pendente é estado**, não erro: política nova exige aceite antes de
  seguir.
- Enquanto esta fase não terminar, tutor real não usa o Petfy e **`prd` não tem por que
  existir**.

**Pronto quando:** alguém que nunca viu `curl` cadastra um animal e vê a próxima
dose.

### Passo 4 — o delta de contrato, levantado em 2026-08-07

O levantamento foi feito **contra o contrato publicado**, não contra o `README.md`:
`GET /v3/api-docs` do `dev`, que é gerado das anotações e portanto é a verdade.
**60 caminhos, 90 operações.** Cada item abaixo tem a evidência ao lado, e a ordem
é de prioridade — o que trava mais tela vem primeiro.

#### O anexo do `PRODUTO.md` venceu, e isto corrige o registro

O anexo *"insumos já levantados para o delta de backend"* é de 2026-08-05,
**anterior à Fase 6**. Metade das lacunas que ele lista já não existe, e quem
planejar por ele planeja para trás:

| Lacuna que o anexo lista | Hoje, verificado no contrato e no código |
|---|---|
| `Clinic` específica demais para creche e abrigo | **Fechada** — `Organization` com capacidades |
| Não existe orientação atribuída com prazo e confirmação | **Fechada** — `/animals/{id}/care-instructions` e `/fulfillments` |
| Seis leituras separadas, não há linha do tempo | **Fechada** — `GET /animals/{id}/timeline`, paginada |
| `Vet.clinic` singular, vet em duas clínicas precisa de duas contas | **Fechada** — `Membership` |
| Pet exige dono; não há animal sem tutor humano | **Fechada** — `Custody.holderPerson` é exclusivo com `holderOrganization`, com `CHECK` no banco |
| Não existe conteúdo não clínico dirigido ao tutor (`Update`) | **Aberta** — nenhum endpoint, nenhum schema |
| Espécie tem dois valores | **Aberta**, e fica fora deste passo |
| Raça é texto livre | **Aberta**, e fica fora deste passo |

**Espécie e raça saem deste passo de propósito:** são mudança de modelo com
migration, não delta de contrato, e já estão em *"Ainda não levantado com você"*.

#### P1 — Leitura em volume na área de organização

**A única cobrança que o 9.5 já dava como certa, e é a pior.**

`GET /professional/animals` **retorna array e não aceita um único parâmetro.** Sem
página, sem busca, sem ordem, sem filtro. A 9.3 define essa área como *"muitos
animais em largura: lista, filtro, busca e ação sobre vários de uma vez"*, e a 9.5
diz que sem recorte ela é *"inutilizável para o ator que a justifica"*.

O contraste com as vizinhas mostra que é esquecimento, não decisão: `/animals`,
`/organizations`, `/vaccines` e o próprio `/timeline` já são `Page` com `pageable`.
A rota da área que lê centenas é a única que ficou array.

**Entra:** `Page`, `q` de busca por nome e registro geral, ordem, e o filtro que a
tela pedir. **Teto de 100 por página**, como as outras.

**Cuidado registrado:** isto muda o contrato de uma rota já publicada, de `List`
para `Page` — exatamente a colisão que a paginação causou no passo 4 anterior. Sai
de graça porque **não existe cliente ainda**, e é por isso que tem de ser agora.

#### P2 — Contexto ativo como estado

**Não existe nenhuma rota de contexto ativo.** Nem para ler, nem para trocar —
procurado por `context`, `active`, `switch` e `me/org` no contrato, zero.

A 9.3 é dura sobre isso: *"a troca entre áreas é explícita e visível… quem age
precisa saber em nome de quem está agindo antes de agir. Área implícita é ato
registrado no contexto errado"*. E a 5.7 diz que **contexto não é editável depois**.
Ou seja: sem isto, o primeiro registro feito em nome errado é permanente.

**Entra:** ler o contexto ativo com as opções disponíveis da pessoa, e trocá-lo.
**A regra que precisa ser verificada, não presumida:** *nada na área de organização
pode exigir organização* (9.3) — o autônomo atravessa a área inteira com contexto
próprio, sem vínculo. Isso é teste, não comentário.

#### P3 — A linha do tempo que a tela 5.2 exige

A rota existe e é paginada. O que falta é o **conteúdo do registro**:
`TimelineEntryResponseDTO` tem `eventId`, `eventType`, `occurredAt`, `recordedAt`,
`recordedByName`, `summary`, `visivel` e `healthData`. A seção 5.2 do `DESIGN.md`
cobra quatro coisas que não estão lá:

| A tela exige | Hoje |
|---|---|
| **Ato clínico com bloco de acento**, distinto de observação "sem precisar ler o autor" | Só `eventType` como string. A natureza (3.11) não vem no DTO |
| **Linha de credencial**, e o CRMV apenas informado aparecendo *como informado* (5.10) | Não existe campo de credencial |
| **Correção é sucessão, e se vê** — valor anterior legível, correção abaixo, com autor e data | Correção mora em `/vaccines/{id}/corrections` e `/health-records/{id}/corrections`, **separadas**. A linha do tempo custaria **N+1 chamadas** |
| **Série mora dentro do evento** — peso carrega o próprio gráfico e a variação | A série vem de `/animals/{id}/weights`, fora do evento |

**Por que P3 e não P1:** é a tela central do produto, mas ela **se desenha
degradada** — dá para mostrar a espinha e o fato sem a credencial. A área de
organização, sem P1, não abre.

#### P4 — A rede de quem cuida

A 5.4 diz *"as pessoas em volta do animal vêm antes dos dados"* e que o que aparece
é **quem tem alcance de fato**. Hoje isso são **três rotas** que o cliente teria de
juntar: `/animals/{id}/tutors`, `/animals/{id}/shares` e
`/animals/{id}/organization-access`.

E falta o campo que dá vida à tela: *"cada pessoa aparece com iniciais, papel e **a
última contribuição** — registrou hoje, registrou ontem"*. Nenhuma das três traz
última contribuição.

**Mesmo argumento do feed de pendências (9.5):** se o cliente montar a rede
juntando fontes por conta própria, **o app remonta a mesma regra e erra diferente**.

#### P5 — Silenciar pendência

`DueItemResponseDTO` já atende a 5.3 quase inteira: `dueOn`, `overdue`, `kind`,
`description`, e — o item mais importante — `lastFulfilledByName`, que é o *"nunca
cobrar duas pessoas sem dizer que a outra já fez. Dose dupla é dano, não
incômodo"*. **Isso já está pronto, e é a parte difícil.**

Falta uma coisa: *"silenciar mora na pendência, não em preferências. Silêncio é
funcionalidade (4.2), e funcionalidade escondida em configuração não é oferecida —
é escondida."* Não há rota de silenciar.

#### P6 — O `README.md`, e as 68 operações sem `summary`

**A dívida mais urgente das três, e ainda assim é P6.** O motivo é ordem, não
importância: o README precisa descrever o contrato **final**, e reescrevê-lo antes
de P1 a P5 é reescrevê-lo duas vezes. O que o desbloqueia enquanto isso é o
OpenAPI, que está correto.

Ele documenta `/vet/pets`, `/vet/clinic-invites`, `ROLE_VET`, `Owner` e `Pet` — API
que morreu na Fase 6 — e não menciona `Person`, `Animal`, `Custody`, `Grant`,
`Organization`, `Membership`, `Timeline`, `CareInstruction` nem `DueItem`.

**E um achado que não estava previsto: 68 das 90 operações não têm `summary`.** Com
a stack decidida gerando **o cliente a partir do OpenAPI**, isso deixa de ser
capricho de documentação e passa a ser a ergonomia do código do front — o nome e a
doc de cada método saem daí.

#### O que fica bloqueado, e por quem

Não é dívida: é decisão de produto que o backend não tem como tomar sozinho.

| # | Decisão em aberto | O que ela trava |
|---|---|---|
| 15 | Quem cria uma organização, e como entra o primeiro membro | O começo da área de organização. `POST /organizations` e `/organizations/invites` existem, mas a cadeia nunca foi exercitada |
| 16 | Como o adotante sem e-mail à mão recebe o convite de custódia | O fluxo de adoção no balcão |
| 17 | O que compõe o escopo mínimo do cartão | `GET /share/{token}` existe **sem escopo**. Fechar a lista é decisão clínica, e a 9.5 pede um veterinário na frente |

#### A tensão entre o `DESIGN.md` e o backend, registrada

A 5.5 do `DESIGN.md` elege cinco objetos que carregam a identidade, e **dois deles
não existem no contrato**: `Insight` (percepção, na coluna lateral, fora do eixo do
tempo) e `Update` (conteúdo — recado e foto da creche, o único objeto em que a
imagem é grande).

Isso é coerente com o `PRODUTO.md` — percepção é Horizonte 4 — mas significa que
**a tela de referência descreve algo que a v1 não entrega**. Ou o design registra
que os dois são posteriores, ou eles entram no escopo. **É decisão de produto, e
não se resolve aqui.**

#### P1, P2 e P3 — concluídos em 2026-08-07

| # | O que entrou | Onde |
|---|---|---|
| **P1** | `/professional/animals` vira `Page`, com busca por nome, registro geral e microchip, e ordem pelo `Pageable` | `GrantRepository`, `VetPetService` |
| **P2** | `GET /me/context` — contexto ativo, opções disponíveis, capacidades e `ambiguous` | `ActiveContextController` |
| **P3** | `V29`: nome de quem registrou, organização, credencial com estado, e contagem de correção na linha do tempo | view `animal_timeline` |
| **P3b** | `V30`: **observação passa a existir**, com escopo próprio e alerta de urgência | `Observation`, `GrantScope.OBSERVACOES` |
| **P3c** | `V31`: a pesagem carrega a anterior, e a linha ganha a variação | view `animal_timeline` |

**O P2 mudou de forma ao ser lido, e o registro importa.** O plano dizia *"ler o
contexto ativo e trocá-lo"* — **a troca já existia** desde a Fase 6, no header
`X-Petfy-Organization`, com a regra certa e sem escolher em silêncio. Faltava só a
leitura, e o 409 `AMBIGUOUS_CONTEXT` era literalmente *"a ambiguidade devolvida no
meio de uma operação"* que o 9.5 proíbe. Por isso **não há rota de troca**: um
contexto ativo guardado no servidor seria uma segunda fonte de verdade, e a que
perde é sempre a que o registro não usou.

**O P3 cresceu porque faltava um objeto, não um campo.** O levantamento dizia que o
DTO da linha do tempo era magro. Era — mas a causa de fundo é que
`TimelineEventType` não tinha `OBSERVACAO`, embora o `PRODUTO.md` 3.11 a definisse
desde o documento fundador e o `DESIGN.md` 5.5 chame essa distinção de *"a mais
importante do produto"*. As duas decisões de produto que a construção obrigou a
fechar — observação é **sempre** dado de saúde, e tem **escopo próprio** — estão
registradas no `PRODUTO.md` 3.11, não aqui, porque são de produto.

**Um N+1 consertado de passagem, e ele já existia:** o `TimelineServiceImpl`
resolvia o nome do autor com `personRepository.findById()` **por entrada** — vinte
eventos, vinte consultas, na leitura que mais cresce no produto. Acrescentar
organização e credencial no mesmo padrão teria feito disso N+3.

**A rede de segurança cobrou duas coisas que teriam passado**, e as duas existem por
causa da família de seis bugs da Fase 4: o `AnimalPurgerCoverageContainerTest` leu o
`information_schema` e apontou `observations` como tabela nova apontando para
`animals` sem cobertura no purger — apagar animal ou conta responderia **500 na
primeira observação registrada**. E o `AnimalPurgerTest` quebrou por falta do mock.

**O que ficou de fora do P3, por decisão:** a **natureza do evento** como campo
derivado. Com `OBSERVACAO` existindo como tipo e a credencial já no DTO, o cliente
tem como fazer a distinção da 5.5 — e mapear `ORIENTACAO` ou `VACINA` para *"ato
clínico"* ainda depende de quem registrou, o que continua sendo decisão de produto.

**Suíte:** 731 testes, 0 falhas, 0 pulados, com os de container contra Postgres real.

#### P4, P5 e P6 — concluídos em 2026-08-07, fechando o passo

| # | O que entrou | Onde |
|---|---|---|
| **P4** | `GET /animals/{id}/care-network` — quem alcança o animal, com a última contribuição | `CareNetworkController` |
| **P5** | `V32`: silenciar pendência, por pessoa e por item, sem parar o registro | `DueItemSilence` |
| **P6** | `summary` em todas as 90 operações, e o `README.md` deixando de duplicar o contrato | 19 controllers, `README.md` |

**O P4 não era só juntar três rotas.** A de tutores filtra `holderPerson`, então um
animal sob custódia de uma ONG aparecia **sem ninguém respondendo por ele** — e a
3.4 admite custódia de organização desde o P2b. E a *última contribuição* não
existia em nenhuma das três: são duas agregações sobre a view, uma por pessoa e uma
por organização, e não uma consulta por membro.

**Duas ausências deliberadas no P4:** sem e-mail nem telefone, porque a 5.4 diz
*"quem tem alcance de fato, nunca uma lista de contatos"*; e sem link de
compartilhamento, porque link é alcance anônimo e o **token dele é a credencial** —
devolvê-lo numa leitura de tela entregaria a chave.

**O P5 fechou uma pergunta que o documento não respondia:** silenciar é por pessoa e
por **item**, não por tipo. Por pessoa porque dois tutores dividem o cuidado e
dividem a cobrança, e um silenciar não pode calar o outro. Por item porque
*"silenciar todas as doses"* seria preferência global disfarçada, que é o que a 5.3
recusa — e como a pendência é derivada, o silêncio se resolve sozinho: registrada a
próxima dose, nasce outra pendência e ela cobra. **Consentimento não se silencia**,
porque bloqueia o resto do produto.

**O P6 corrigiu a causa, e não só o sintoma.** O `README.md` mantinha à mão uma
tabela de endpoints que o OpenAPI já gera das anotações — duas fontes para a mesma
pergunta, e a que perde é sempre a escrita à mão. A tabela saiu; o que ficou é o
vocabulário, as regras que atravessam rotas, e as leituras que o cliente não deve
montar sozinho.

**A dívida que sobra do P6, dita em voz alta:** os tutoriais de fluxo do README
tiveram os caminhos corrigidos, mas a prosa ainda fala em *"vet"* e *"clínica"* onde
o modelo diz pessoa com credencial e organização com capacidades. Não mente mais
sobre rota, e ainda usa vocabulário velho.

**Suíte no fim do passo:** 749 testes, 0 falhas.

#### Ordem, e o critério de pronto

P1 → P2 → P3 → P4 → P5 → P6, e foi essa a ordem executada. Os cinco primeiros são
aditivos e independentes entre si; **P1 foi o único que mudou contrato existente**, e
por isso veio primeiro, enquanto não havia cliente para quebrar.

**Pronto:** o contrato tem as leituras que a home do tutor e a lista da organização
exigem, toda operação está documentada, e o `README.md` descreve o que existe. **O
passo 5 — construir — está liberado.**

**Pronto quando:** o cliente gerado do OpenAPI compila com as leituras que a home
do tutor e a lista da organização exigem, e o `README.md` descreve o que existe.

### Passo 5 — construir, planejado em 2026-08-07

**Aberto e não iniciado.** Nenhuma linha de frontend existe. Este é o plano de
execução, e ele começa com dois itens de **backend** — porque os dois foram
decididos e nenhum foi construído.

#### B0 — os dois pré-requisitos, levantados contra o código

**Não são dívida encontrada por acaso: são decisões deste roadmap que ficaram sem
implementação, e as duas aparecem na primeira tela.**

**CORS não existe.** Zero ocorrências em `config/`. Um SPA do Vite em
`localhost:5173` chamando a API em `localhost:8080` é *cross-origin*, e sem CORS **a
primeira requisição da primeira tela falha no navegador** — antes de qualquer código
de produto. Precisa ser por perfil: `local` libera a origem do Vite, `dev`/`stg`/`prd`
liberam a origem do próprio front, e nunca `*`, porque a API responde com credencial.

**O refresh em cookie `httpOnly` não existe.** A decisão está escrita neste documento
na seção do BFF adiado: *"o único argumento de dia um seria autenticação — SPA
guardando JWT em `localStorage` é exposição a XSS —, e isso sai mais barato resolvido
no Spring que já existe, com refresh em cookie `httpOnly`"*. Hoje `/auth/login`
devolve o JWT no corpo e não há refresh nenhum.

**São dois caminhos, e a escolha é de produto, não de código:**

| Caminho | O que custa | O que fica devendo |
|---|---|---|
| **Construir agora** | Endpoint de refresh, cookie `httpOnly` + `Secure` + `SameSite`, e rotação | Nada. A tela de login nasce definitiva |
| **Adiar, com token em memória** | Zero backend. O token vive em memória do JS e morre no *refresh* da página | Login a cada recarga, e a tela de login será refeita quando o refresh chegar |

**Guardar em `localStorage` não é opção**, e isso já está decidido: é a exposição a
XSS que o documento nomeia. Token em memória é pior de usar e não é insegura; é a
única forma de adiar sem contrariar a decisão.

#### A ordem, e o que cada bloco entrega

| # | Bloco | Entrega | Depende de |
|---|---|---|---|
| **B0** | Pré-requisitos | CORS por perfil, e o caminho de auth escolhido | — |
| **1** | Fundação | Scaffold, tokens das seções 3 e 4 do `DESIGN.md`, cliente gerado do OpenAPI, i18n, rota tipada | B0 |
| **2** | **Home do tutor** | O feed de pendências com silenciar. `GET /due-items` e `GET /me/context`. **É a tela que existe todo dia** (9.3), e a única com referência renderizada | 1 |
| **3** | A linha do tempo | O objeto central (5.1 e 5.2): espinha contínua, evento assinado, credencial como informada, correção como sucessão | 1 |
| **4** | A rede de quem cuida | 5.4, com **conceder acesso como ação de primeira linha** e a última contribuição | 1 |
| **5** | Área de organização | A leitura em largura, com `/professional/animals` e a troca de contexto explícita | **Decisão 15** |

**Por que a home do tutor primeiro, e não a área de organização.** É a área que
existe todo dia (9.3), é a única com tela de referência aprovada
(`design/home-tutor.html`), e o backend dela está inteiro — o feed já traz
`lastFulfilledByName`, que é a parte difícil. A área de organização depende da
decisão 15, que não tem resposta.

#### O que o plano assume, e precisa ser conferido na hora

- **O cliente é gerado, e o gerador não está escolhido.** A decisão de stack diz
  *"cliente gerado do OpenAPI"* e para aí. Escolher é do bloco 1, e o critério é um
  só: mudança de controller tem de quebrar o build do front.
- **As mensagens de erro da API estão em inglês**, no `ErrorMessageEnum`, e o produto
  é pt-BR. Elas têm **código numérico**, então o front mapeia por código em vez de
  exibir a mensagem — e o i18n do bloco 1 já nasce com essa tabela.
- **`Insight` e `Update` não existem no contrato**, e são dois dos cinco objetos que
  a 5.5 do `DESIGN.md` elege como portadores da identidade. Ou o design registra que
  são posteriores, ou eles entram no escopo. **É decisão de produto**, e a tela 3 vai
  encostar nela. **Decidido em 2026-08-07: os dois são posteriores**, e o motivo de
  cada um está na emenda da 5.5 do `DESIGN.md`. A v1 carrega quatro dos cinco.

#### B0 — concluído em 2026-08-07

**CORS existe, por perfil.** `local` traz a origem do Vite, os hospedados recebem a
sua por `CORS_ALLOWED_ORIGINS`, e **vazio não libera ninguém** — fecha por omissão.
`*` derruba a subida, pelo mesmo critério do `JWT_SECRET`: a regra deixou de depender
de quem configura lembrar dela.

Três coisas que só apareceram construindo, e as três viraram teste:

- **`X-Petfy-Organization` precisa estar entre os headers aceitos.** É header
  customizado, então toda requisição que o carrega vira preflight — e é por ele que a
  área de organização age em nome de uma organização.
- **`Content-Disposition` precisa ser exposto**, senão todo anexo baixado salva com
  nome errado: o download passa pela API autenticada (passo 11), e o navegador não
  entrega esse header ao JS por padrão.
- **A posição do `CorsFilter` é o que impede o preflight de tomar 401.** O navegador
  nunca manda credencial no preflight; se ele chegasse à autorização, a requisição de
  verdade nem sairia, e o sintoma seria erro de CORS com o token correto o tempo todo.

**O refresh fica adiado, com token em memória.** `localStorage` segue fora: é a
exposição a XSS que este documento recusa.

O motivo do adiamento não é custo de backend — é que **construir agora entregaria um
mecanismo incompleto**. Front e API em domínios diferentes tornam o cookie
*cross-site*, o que obriga `SameSite=None`, que Safari e Firefox já bloqueiam por
padrão: o refresh falharia calado num navegador inteiro. Ele só fica correto quando
front e API forem *same-site*, e isso depende do **domínio próprio**, que segue em
aberto nas decisões deste documento.

**O gatilho, no molde do BFF.** O refresh em cookie `httpOnly` entra no primeiro dos
dois que acontecer:

1. o domínio próprio entrar, pondo front e API sob o mesmo site;
2. o login a cada recarga se mostrar insuportável no uso real.

**A regra de dia um que mantém o custo baixo:** nenhuma tela lê token. A sessão mora
numa camada só, com um `ensureFresh()` que hoje desloga e amanhã chama
`/auth/refresh` — no dia da troca muda essa camada, não as telas. É irmã da regra que
mantém o BFF possível.

**Suíte:** 756 testes, 0 falhas, 0 pulados, com os de container contra Postgres real.

#### Bloco 1 — o gerador escolhido, e a guarda que faz o critério valer

**O gerador é o `openapi-typescript`, e o cliente é só tipo.** Ele gera um arquivo de
tipos com as 96 operações e os 84 schemas; a camada de dados é escrita por nós, fina,
sobre `openapi-fetch`.

**Por que não um gerador de SDK e hooks** (Orval, `@hey-api`), que economizaria
escrita e já entregaria hooks de TanStack Query: nesse desenho o que a tela importa é
o **hook gerado**, ou seja, a camada de dados única passa a ser gerada. No dia do BFF
ela é regenerada com outra forma, e a mudança encosta em toda tela — que é
exatamente o que a regra *"nenhuma tela chama HTTP"* existe para impedir. Com o
gerador de tipos, o gerado é só o tipo, e a costura continua sendo nossa.

**A parte que nenhum gerador entrega, e sem a qual o critério é intenção.** O
`/v3/api-docs` só existe com a aplicação no ar, então o front geraria contra um
contrato velho — ou nem geraria — e compilaria verde enquanto a API já responde outra
coisa. Por isso o contrato virou **`contract/openapi.json`, versionado**, com o
`OpenApiContractTest` quebrando o build de quem muda controller e não regenera.
Regenerar é explícito:

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
```

Duas normalizações, as duas contra falha intermitente: **chaves ordenadas**, porque a
ordem do springdoc segue a descoberta dos handlers e `getDeclaredMethods` não promete
ordem entre execuções; e o nó **`servers` removido**, porque ele carrega a URL de quem
gerou, enquanto o contrato descreve a forma da API e não onde ela está.

**O contrato deixou de ser omisso sobre autenticação:** entra o esquema `bearer-jwt`,
a exigência global, e `security: []` nas sete rotas públicas. Não é daqui que o token
sai para a rede — o cliente é só tipo, e quem põe o header é a camada de dados. A
lista das públicas virou `RotasPublicas`, lida pelo `SecurityConfig` **e** pelo
`OpenApiConfig`: duas listas escritas à mão divergem, e a que perde é sempre a
documentação. Rota declarada pública que não exista no contrato derruba a subida.

**Suíte:** 759 testes, 0 falhas, 0 pulados.

**O que falta do bloco 1, e é tudo front:** scaffold do Vite, tokens das seções 3 e 4
do `DESIGN.md`, i18n com a tabela de código de erro do `ErrorMessageEnum`, rota
tipada, e a camada de dados sobre os tipos gerados. Mais os dois custos do
repositório único que este documento já listou: o filtro por caminho no CI e o
segundo serviço no Railway.

~~**Duas decisões pequenas em aberto, e as duas são de convenção:** o nome da pasta do
front, e se o filtro por caminho no CI entra junto do scaffold ou depois da primeira
tela.~~ **Decididas em 2026-08-07:** a pasta é **`web/`** — a 9.4 do `PRODUTO.md` decide
"web primeiro, app depois", então o nome já diz qual superfície é e deixa `app/` livre;
`client/` colidiria com dois sentidos já ocupados (o cliente gerado do OpenAPI, e quem
usa o produto). O **filtro por caminho entrou junto do scaffold**, e o que decidiu foi um
fato conferido: a `main` **não tem branch protection** (repositório privado em plano
free), então o risco clássico — *required check* filtrado por caminho ficando pendente
para sempre — não existe hoje.

#### Bloco 1 — concluído em 2026-08-07, e uma ressalva ao critério que este documento escreveu

O scaffold está de pé em `web/`: Vite, React, TypeScript, tokens, i18n, camada de dados
e rota tipada. **761 testes no backend, 0 falhas, 0 pulados**, e o pipeline do front
verde. PR #42.

**A ressalva, e ela corrige uma afirmação deste documento.** A seção acima diz que o
critério do gerador é um só — *"mudança de controller tem de quebrar o build do front"* —
e apresenta o contrato versionado como o que faz esse critério valer. **Vale pela
metade**, e a outra metade só apareceu ao construir: o contrato descreve **apenas o
caminho feliz**. `ErrorResponse` não está entre os 84 schemas, e nenhuma rota declara
resposta de erro. Ou seja, mudar a forma do corpo de erro **não quebra build nenhum**, e
o tipo dele é cópia escrita à mão no front.

**O que fechou o buraco por ora**, e o que continua aberto:

- **Fechou a tradução.** `contract/error-codes.json` é versionado, com o
  `ErrorCodesContractTest` quebrando o build de quem mexe no `ErrorMessageEnum` sem
  regenerar, e um teste do lado do front recusando código sem tradução. Regenerar é
  explícito, no mesmo molde do contrato:

  ```
  mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true
  ```

- **Continua aberta a forma do corpo.** `RespostaDeErro` no front é cópia sem guarda.
  Fechá-la é anotar resposta de erro nas 96 operações — caro, e não bloqueia tela
  nenhuma. Fica registrado para não ser redescoberto.

**O `500` estava fora do enum**, escrito à mão dentro do `handleGenericException` — uma
segunda fonte para a mesma pergunta, e um código que a tabela do front não teria como
cobrir. Virou `INTERNAL_ERROR`, sem mudar código, mensagem nem status.

**O `400` não é traduzível por código, e isso vira requisito de tela.** A validação de
campo devolve `campo: motivo`, com nome de campo em inglês e sem acento — não é exibível
nem pela regra de nunca mostrar a mensagem do servidor, nem pela seção 2 do `DESIGN.md`.
A consequência é que **validar antes de enviar deixou de ser conveniência e passou a ser
obrigação da camada de formulário**: o `400` que voltar é rede de segurança, e vira uma
frase genérica nossa.

**O TypeScript ficou em 5.9, e não no 7.** O 7 não expõe `createPrinter` nem `factory` —
a API clássica do compilador saiu, e o que existe está sob `./unstable/*` —, e o
`openapi-typescript` emite o `.d.ts` por ela. Não é preferência: com o 7, o gerador
escolhido neste documento não roda.

**Cinco guardas nasceram, e nenhuma entrou só escrita.** Todas foram vistas reprovando
antes de serem commitadas — código sem tradução, chave órfã, mensagem em inglês copiada,
tela chamando `fetch` cru, e endereço de rota que não existe. É a lição do teste de
container que passava sem executar, aplicada antes de custar.

**Dívida com relógio:** o `openapi-typescript` puxa `js-yaml` com CVE de CPU quadrático
em `!!omap`. Só em `devDependencies` — `npm audit --omit=dev` dá zero —, e o gerador lê
um JSON do próprio repositório, sem entrada de terceiro. Não há correção disponível; some
quando o `@redocly/openapi-core` atualizar.

#### Blocos 2, 3 e 4 — concluídos em 2026-08-08, e o que a tela achou no modelo

**A home do tutor está de pé, com dados reais.** Login, feed de pendências com silenciar,
cumprir orientação, registrar dose, a rede de quem cuida e a linha do tempo. **765 testes
no backend, 0 falhas, 0 pulados.**

**O "pronto quando" desta fase fechou** — e quase não fechou, porque a construção
descobriu que o gesto central dele não funcionava.

**O defeito que a tela revelou, e ele era de produto.** Registrar uma dose **não tirava a
cobrança do feed**. O `DueItemServiceImpl` olhava cada registro de vacina isoladamente,
filtrando por `nextDoseDate`: a dose de 2025 seguia dizendo *"próxima em 2026-08-04"* para
sempre, e o tutor que registrasse a dose de hoje veria a pendência intacta. Medido contra
a API antes e depois, na mesma base: 4 pendências continuavam 4.

**A correção é derivação, não schema.** As aplicações passam a ser agrupadas em **série**
— animal mais identidade da vacina —, e só a mais recente cobra. Vale igual para
antiparasitário, que tinha o mesmo furo. Nenhuma migration, nenhum dado reescrito, e o
histórico das doses continua inteiro na linha do tempo, que é onde ele vale.

**Uma consequência que a tela teve de respeitar:** a série é identificada pelo catálogo
quando ele existe. Registrar a dose nova como texto livre a poria em outra série, e a
anterior voltaria a cobrar para sempre — então o formulário lê a dose que está vencendo e
herda o catálogo dela.

#### Duas decisões de produto tomadas em 2026-08-08

**O onboarding em passos é v2, e o motivo é sequência.** A ideia é boa — um *stepper* que
guia quem chega — mas as etapas dele apontam para telas que não existem: cadastrar o
primeiro animal, convidar quem mais cuida, declarar credencial. Onboarding é o caminho
entre cômodos, e construí-lo antes dos cômodos é construir para trás. **Quando entrar,
entra depois do login**, e não como porta: um *stepper* antes do valor é fricção antes do
valor, e este produto só fica interessante quando existe um animal com histórico na tela.
O cadastro fica de um passo, com os quatro campos que o `POST /persons` já exige.

**Verificação de telefone fica fora**, e não por custo de tela: ela não existe no backend
— nem rota, nem provider de SMS — e nenhum documento a pediu. O telefone segue campo de
contato opcional. Coletá-lo sem verificar seria guardar um dado que parece conferido e não
é, que é o oposto do que a 5.10 faz com o CRMV.

#### As dívidas que a construção do cliente levantou

Nenhuma delas bloqueia tela, e as cinco são de contrato ou de modelo — não de front.

| # | O que | Por que dói |
|---|---|---|
| 1 | **O contrato não declara `required` em resposta** | 19 dos 84 schemas têm `required`, e os 19 são `*RequestDTO`: quem os gera é o `@NotNull` da validação de entrada. Então todo campo de resposta chega opcional no tipo gerado |
| 2 | **E o backend manda `null` em vez de omitir** | O schema também não diz `nullable`. Os dois juntos fazem o TypeScript **aprovar código que quebra em runtime** — foi assim que a home caiu na primeira vez que abriu com dados de verdade. A mentira para hoje no `corpoDe()` do front |
| 3 | **`LocalDateTime` viaja sem fuso** | A tela mostra *"ontem"* em vez da hora, porque dizer "às 7h40" seria inventar precisão que o dado não tem |
| 4 | **`Pageable` é documentado como um parâmetro** | O springdoc o descreve como `pageable`; o Spring lê `page`, `size` e `sort` soltos. Seguir o contrato ao pé da letra manda `?pageable=…`, que o servidor ignora — funciona por acidente na área do tutor e vai falhar calado na de organização |
| 5 | **JSON malformado responde `500`** | O `HttpMessageNotReadableException` cai no handler genérico. Corpo inválido é erro do cliente, e hoje o front o traduz como *"não conseguimos salvar agora"*, que mente sobre a causa |

**A correção de raiz das duas primeiras é uma linha de cada vez:**
`spring.jackson.default-property-inclusion=non_null` faz o campo nulo deixar de ser
enviado, e o tipo `campo?: T` passa a ser verdadeiro; ou `required` declarado nos DTOs de
resposta, que é mais explícito e mais trabalhoso.

**E uma lacuna de modelo, que não é dívida de contrato:** a **credencial profissional só
pode ser declarada no `POST /persons`**. Quem não marcou na hora — ou se formou depois —
não tem como declarar. Pede `POST /persons/me/credentials`, é pequeno, e destrava o
onboarding da v2.

**Pronto quando:** um tutor que nunca viu `curl` entra pelo navegador, vê o que
precisa fazer hoje, registra uma dose e abre a linha do tempo do animal.
**Cumprido em 2026-08-08.**

#### Dívidas 1, 2 e 5 — pagas em 2026-08-08

**As duas primeiras saíram na linha prevista**, `spring.jackson.default-property-inclusion=non_null`,
e o efeito interessante é o que **não** aconteceu: o `contract/openapi.json` ficou
idêntico. O springdoc não lê a configuração do Jackson, então o tipo gerado continua
`campo?: T` — e é esse o ponto. **O tipo não mudou; ele passou a ser verdade.**

Com isso o `semNulos()` saiu do front, e com ele os três testes que o cobriam. Ele era um
conserto no lugar errado, e tinha um custo escondido além do óbvio: **enquanto existisse,
apagar a linha do `application.properties` não quebraria nada** — o remendo absorveria a
volta do defeito, e a mentira só reapareceria numa tela, no navegador de alguém.

**Por isso a garantia virou teste, e não confiança.** O `RespostaOmiteNulosTest` afirma
sobre o `ObjectMapper` **injetado** — o mesmo que serializa toda resposta da API —, e não
sobre a string da propriedade: ler o `Environment` provaria apenas que alguém escreveu a
linha, não que o JSON sai sem nulo. Conferido ao contrário também, que é o que dá valor à
guarda: com `default-property-inclusion=always`, os três testes falham.

**A dívida 5 revelou uma escolha que a tabela acima não previa.** Reusar `INVALID_REQUEST`
teria corrigido o status e criado outra mentira: o front traduz **por código**, e o texto
do 400 é *"Confira os campos marcados"* — só que num corpo ilegível não houve objeto, logo
não há campo marcado. Daí `MALFORMED_REQUEST_BODY(143)`, que existe pelo mesmo motivo que
o `INTERNAL_ERROR` existe no enum: mensagem distinta exige código distinto.

Dois detalhes que o teste fixa. **A mensagem do Jackson não vai no corpo** — ela carrega
trecho do payload recebido, e no `POST /persons` esse trecho inclui a senha. E o caso do
**acento em Latin-1**, a armadilha que esta máquina produz pelo Git Bash, agora responde
`400` com *"Invalid UTF-8 middle byte"* no log, em vez do `500` que fez parecer defeito de
servidor durante a construção do cliente.

**A dívida 4 foi corrigida em 2026-08-09, e a correção coube numa propriedade.**
`springdoc.default-flat-param-object=true` faz o springdoc descrever o `Pageable` como os
três parâmetros que o Spring realmente lê: os 7 `pageable` do contrato viraram 7 trios
`page`/`size`/`sort`, e o schema `Pageable` deixou de existir. **O que mais doía era o
`required: true`** — o cliente gerado era obrigado a emitir um parâmetro que o servidor
ignora, e recebia sempre a primeira página. Acidentalmente certo na área do tutor, que lê
um a três animais; calado e errado na de organização, que lê centenas.

Conferido antes de aplicar: a propriedade mexe **só** nos 7 `pageable`, porque o
`Pageable` é o único parâmetro-objeto desta API. O `web/src/dados/animais.ts` e o
`animal.ts` perderam o remendo que emitia `pageable: {}`, e o `pageable.test.ts` trocou de
premissa — ele afirmava a divergência, e agora fixa o formato: `page`, `size` e `sort`
soltos na URL, e nenhum `pageable` nela.

**O que continua em aberto, e por quê:**

| # | Estado |
|---|---|
| 3 | `LocalDateTime` sem fuso — adiada por decisão: mexe em DTO, contrato e tela, e merece PR próprio |

## Fase 6 — a remodelagem que o `PRODUTO.md` cobra

**Aberta em 2026-08-06.** O número vem depois da Fase 5, mas **a execução vem
antes**: o `PRODUTO.md` fechou com a decisão 14 — *fundação inteira antes de
qualquer tela* —, e renumerar um documento cheio de referência cruzada custaria
mais do que esta frase. A Fase 5 continua sendo a próxima a **entregar valor**; a
Fase 6 é a que precisa estar de pé embaixo dela.

**O argumento que sustenta a ordem é a janela**, e o precedente está neste
documento: a paginação mudou `List` para `Page` no dia seguinte ao contrato ser
publicado, e *"como não existe cliente ainda, a mudança saiu de graça"*. Cada
linha desta fase é a mesma coisa, multiplicada. Com cliente existindo, deixam de
ser refatoração e viram migração com dois lados para coordenar.

**Isto é refatoração do núcleo, não incremento.** Não vai parecer progresso
enquanto acontece.

### O levantamento, feito contra o código em 2026-08-06

Antes de fatiar, o raio de alcance real de cada mudança de fundação — é ele que
decidiu a ordem, e não a preferência:

| Ponto de acoplamento | Arquivos em `src/main` |
|---|---|
| `Pet` | 54 |
| `Owner` | 31 |
| `PetTutor*` | 28 |
| `PetAccessGuard` | 17 |
| `Vet` | 16 |
| `Clinic` | 14 |
| `CurrentOwnerProvider` | 14 |
| `UserRole` / `ROLE_VET` | 8 |
| `CurrentVetProvider` | 7 |
| **Owner/Vet/PetTutor em teste** | **46 dos 54 arquivos de teste** |

**Quatro achados que mudaram o plano em relação ao que o `PRODUTO.md` supunha:**

1. **`pets.owner_id` já não existe** — a V15 removeu. Animal sem tutor humano é
   mais barato do que o documento previa: falta a custódia poder apontar para uma
   organização, não desatar chave estrangeira.
2. **`Vet.clinic` é `nullable = false` no banco.** O veterinário autônomo é
   impossível hoje por *constraint*, não por regra de serviço — o que muda onde a
   correção mora.
3. **Nenhum evento sabe quem o registrou.** Só `Attachment` tem `uploadedBy`.
   `Vaccine` e `HealthRecord` têm `clinic` nullable, que é um contexto
   embrionário sem pessoa dentro.
4. **A separação fato/registro já existe, sem nome.** `applicationDate`,
   `eventDate` e `measuredAt` são o fato; `creationDate` é o registro. O núcleo
   comum uniformiza e nomeia o que já está lá — não inventa.

**E um buraco que nenhum documento tinha registrado:** `Vet` não tem
`emailVerifiedAt`. A guarda de notificação vive em `Owner.podeReceberNotificacao()`,
então o veterinário está fora dela por acidente de modelagem, e não por decisão.
Some sozinho no P1.

### Regra de fatiamento

**Nenhum passo deixa o sistema em estado que precise do passo seguinte para
compilar.** Cada um é um PR que fecha verde, com os `ContainerTest` executando de
verdade — não pulados.

| # | Passo | Migration | Bloqueia o frontend? |
|---|---|---|---|
| P0 | Rename `Pet` → `Animal` | V20 | sim, só por vocabulário |
| P1 | Pessoa única, sem tipo, e credencial profissional | V21, V22 | **sim** |
| P2 | Custódia separada de acesso, com escopo | V23, V24 | **sim** |
| P3 | Organização com capacidades, e membro | V25, V26 | **sim** |
| P4 | Núcleo de evento e linha do tempo | V27 | **sim** |
| P5 | Orientação e pendência unificada | V28 | não — é v1, anda ao lado da tela |

*A coluna de migration foi corrigida no fechamento: cada passo com rename mecânico
mais mudança de comportamento gastou duas, como o 8a/8b já tinha ensinado.*

**P0 a P4 são o Horizonte 1 do `PRODUTO.md`.** P5 já é v1.

**A base de `dev` é descartável, e isso é decisão registrada:** enquanto não
houver produto homologado em `stg`, nenhuma migration desta fase precisa de
backfill inteligente. É a mesma escolha do passo 12, e é o que torna os seis
passos viáveis — reescrever schema custa uma migration, não um projeto de
migração de dado.

### P0 — `Pet` vira `Animal`, e nada mais muda

**Zero comportamento.** Diff enorme, revisão de dois minutos, justamente porque
não há uma linha de regra dentro dele.

**Por que renomear, já que nome não é comportamento:** o precedente é o 8a, que
matou `buscarDoOwnerAutenticado` porque o nome *"mentia depois que o dono único
deixou de existir"*. `Pet` é a mesma mentira uma camada acima — a classe se chama
"animal de estimação de alguém" num modelo onde o animal do abrigo não é de
ninguém. Nome que mente não é dívida de estilo: é a próxima pessoa lendo `Pet` e
assumindo que existe dono.

**Por que primeiro, e não junto do P2:** hoje a suíte está verde e o código
estável. Um rename mecânico agora é risco zero. Junto de outro passo, ele vira
diff misto — e a parte que precisa de revisão cuidadosa fica escondida no meio de
cinquenta arquivos renomeados. É o mesmo motivo que fatiou o 8 em 8a e 8b.

- [ ] `Pet`→`Animal`, `PetAccessGuard`→`AnimalAccessGuard`,
      `PetPurger`→`AnimalPurger`, `PetWeightHistory`, `PetHealthCondition`,
      `PetShare` e os repositórios, serviços e controllers correspondentes.
- [ ] `/pets` → `/animals`, e `petId` → `animalId` nos path variables. O
      `ControllerPathVariableTest` é a rede aqui: nome de path variable divergindo
      do parâmetro quebra em runtime e compila.
- [ ] `V20`: `ALTER TABLE ... RENAME`. **`V1`–`V19` não se reescrevem** — são log
      do que aconteceu, não descrição do estado atual.
- [ ] **Não renomear** `PetTutor`, `PetTutorRole`, `PetTutorInvite` e
      `PetClinicAccess`: os quatro se dissolvem no P2, e renomeá-los agora é
      trabalho que se joga fora.
- [ ] **Não renomear** `/pet-id`: é o OCR do RG animal, e "carteira de identidade
      do pet" é o nome da coisa no mundo real.

**Pronto quando:** a suíte inteira passa sem nenhuma alteração de asserção de
comportamento, e o `SchemaMigrationContainerTest` valida a V20 num Postgres real.

### P1 — Pessoa única, sem tipo

`owners` e `vets` viram `persons`. Acaba o papel derivado de qual tabela o e-mail
aparece.

- [ ] `V21`: funde as duas tabelas. O e-mail passa a ser `UNIQUE` **no banco** —
      hoje a unicidade entre os dois lados é conferida no serviço de cadastro de
      vet, porque não havia como o banco garanti-la. Uma constraint substitui uma
      checagem que podia ser esquecida.
- [ ] `emailVerifiedAt` e `passwordChangedAt` passam a valer para toda pessoa.
      Fecha de graça duas limitações do `README`: o vet que não recupera senha e o
      vet que estava fora da guarda de notificação.
- [ ] **CRMV vira tabela própria** (`professional_credentials`), e não coluna:
      5.10 diz que uma pessoa pode ter mais de um registro, e que cada um tem
      estado — *informado*, *verificado*, *suspenso*. Coluna única não comporta
      nenhuma das duas coisas.
- [ ] `UserRole` e `ROLE_VET` saem do token e da `SecurityFilterChain`.

**A decisão difícil deste passo, registrada:** matar o papel deixa `/vet/**` sem
guarda, e a substituição definitiva — contexto e membro — só chega no P3. A ponte
**não é andaime**: `/vet/**` passa a exigir **credencial profissional na pessoa**,
que é exatamente o que 5.10 diz governar ato clínico. O P3 só troca de onde vem o
alcance; a regra que entra aqui é a regra final.

**Pronto quando:** a mesma conta faz login, cadastra um animal como tutora e
registra um ato clínico como profissional, sem duas contas e sem dois e-mails.

### P2 — Custódia separada de acesso

`PetTutor` se parte em dois, e é o passo que mexe em privacidade — o mais sensível
dos seis, como o 8b foi da Fase 4.

- [ ] `Custody`: animal, quem responde (**pessoa ou organização**), início, fim
      previsto, fim real, natureza (`DEFINITIVA`, `TRANSITORIA`, `INSTITUCIONAL`,
      `RESGATE`), motivo de encerramento e **sucessor**.
- [ ] `Grant`: animal, a quem, nível, **escopo**, quem concedeu, expiração e
      revogação.
- [ ] `HOLDER` migra para custódia; `EDITOR` e `VIEWER` viram níveis de acesso.
- [ ] `PetClinicAccess` e `PetShare` deixam de ser tabelas próprias e viram
      `Grant` — a de clínica com beneficiário organização, a de link com token e
      escopo. **É aqui que nasce o cartão de emergência da decisão 13**, e não
      num passo futuro: ele é a compensação de não existir quebra-vidro, e o
      `PRODUTO.md` o declara item de v1.
- [ ] **O escopo entra neste passo, não no seguinte.** Adicionar escopo a um
      `Grant` já em uso é reescrever o `AnimalAccessGuard` duas vezes.
- [ ] **O quarto invariante vira constraint:** nenhuma custódia termina sem
      sucessor. `CHECK` no banco *e* validação no serviço — o mesmo par já usado
      em `severity` de alergia, pelo mesmo motivo: o banco impede insert direto, o
      serviço impede que o cliente receba erro de integridade como 500.
- [ ] **Acessos não são herdados na transferência** (4.3). A revogação em cascata
      é regra de privacidade, não limpeza: sem ela o novo tutor herda uma plateia
      que não escolheu.

**Pronto quando:** uma clínica com acesso concedido enxerga o animal no escopo
concedido e nada além dele; e uma transferência de custódia derruba os acessos do
antecessor sem tocar na linha do tempo.

### P3 — Organização com capacidades, e membro

- [ ] `Clinic` → `Organization`, com **tabela de capacidades** — registrar ato
      clínico, registrar observação, comunicar-se com o tutor, deter custódia,
      gerir turma e vaga, manter rede de lares transitórios. Clínica, creche e
      abrigo passam a ser conjuntos de capacidades, não três entidades.
- [ ] `Membership` mata `Vet.clinic` singular, e com ele a `nullable = false` que
      torna o veterinário autônomo impossível hoje. Resolve de uma vez o vet em
      duas clínicas — dívida já registrada em *Ainda não levantado com você*.
- [ ] `ClinicInvite` vira convite de membro, com função.
- [ ] `/vet/**` se dissolve: o alcance passa a vir de contexto ativo + capacidade
      da organização + credencial da pessoa.
- [ ] **Organização é opcional para atuar** (3.7). O autônomo é pessoa com
      credencial que recebe acesso direto de quem tem custódia.

### P4 — Núcleo de evento e linha do tempo

- [ ] As seis tabelas de evento ganham o núcleo comum por `@MappedSuperclass`:
      `occurredAt`, `recordedAt`, `recordedByPersonId`, `recordedInOrganizationId`
      e `isHealthData`. **Nenhuma cirurgia de chave primária** — a especialização
      é o que preserva o valor clínico, e o `PRODUTO.md` recusa explicitamente a
      tabela genérica com JSON.
- [ ] **A linha do tempo é uma view SQL `UNION ALL`** sobre as seis tabelas,
      mapeada como entidade imutável de leitura. O motivo é o histórico desta
      base: uma tabela-índice paralela pode divergir da fonte, e divergência
      silenciosa foi a família dos seis bugs da Fase 4. **Uma view não tem como
      divergir** — não existe segunda escrita. Se o volume um dia cobrar, vira
      materializada sem mudar o contrato.
- [ ] `GET /animals/{animalId}/timeline`, ordenada por **quando aconteceu**, e
      filtrada pelo escopo de quem lê. A cronologia é regra de domínio, não
      formatação de tela — encerra a ideia de o cliente chamar seis endpoints e
      ordenar em memória.
- [ ] **Óbito e animal perdido** como estado do animal, com evento
      correspondente. Óbito encerra a linha do tempo sem apagá-la e cessa os
      lembretes; perdido é estado distinto de custódia encerrada, e é onde o
      microchip vale mais.
- [ ] **Classificação de dado de saúde** por evento, pelo critério de 3.11 — ato
      clínico sempre é, observação quase sempre é, recado e foto não são. É o que
      dá regra clara ao `SensitiveAccessLog`, que hoje cobre por convenção.

### P5 — Orientação e pendência

Já é v1, não fundação. Não bloqueia a tela, e provavelmente é desenhado com ela.

- [ ] `CareInstruction` — quem emitiu (pessoa + contexto), o que fazer,
      frequência, até quando, e o registro de cada confirmação. Unifica prescrição
      do veterinário, medicação contínua (hoje parada em *Fica para depois do
      frontend*) e tema de casa da creche.
- [ ] **A orientação segue a custódia, não a pessoa** (3.12). Animal que volta do
      lar transitório no meio de um tratamento de 21 dias continua o remédio, com
      o já cumprido preservado.
- [ ] `DueItem` generaliza a agenda de vacinas — hoje a única parte do sistema que
      produz informação em vez de devolver o que foi gravado.
- [ ] **Cumprir é evento**, e entra na linha do tempo: é o que transforma
      orientação em histórico de aderência, o dado que o veterinário nunca tem
      quando o tratamento não funciona.

### Fase 6 — fechada em 2026-08-06

Os seis cortes entraram na ordem planejada, cada um verde nas duas metades da suíte
antes do seguinte começar. O que cada um deixou no código:

| # | Commit | O que nasceu | O que morreu |
|---|---|---|---|
| P0 | `dedf3ab` | `Animal` (174 arquivos, 28 renomeados) | — |
| P1a | `62965b9` | `Person` (123 arquivos) | `Owner` |
| P1b | `bfc1f7e` | `ProfessionalCredential`, `CredentialStatus`, `ProfessionalAccessManager` | `Vet`, `UserRole`, `CurrentVetProvider`, `VetController`, o `role` no JWT |
| P2a | `8a1b8af` | `Grant`, `GrantScope`, `GrantLevel` | `AnimalShare`, `PetOrganizationAccess` |
| P2b | `fea7e85` | `Custody`, `CustodyNature`, `CustodyEndReason`, `AnimalReach` | `PetTutor` |
| P3a | `d51474b` | `Organization`, `OrganizationCapability` | `Clinic`, `Vet.clinic` singular |
| P3b | `cc1d646` | `Membership`, `ProfessionalContext`, `X-Petfy-Organization` | `Person.organization`, `/vet/**` |
| P4 | `8fe6fb9` | `AnimalEvent`, view `animal_timeline`, `TimelineEntry` | — |
| P5 | `037f789` | `CareInstruction`, `CareInstructionFulfillment`, `DueItem` derivada | — |

**O que ficou de fora, e não por esquecimento:**

- **Óbito e animal perdido** (era item do P4). É estado do animal com evento
  correspondente, e o núcleo de evento não é pré-requisito dele — pode entrar
  isolado, sem tocar em nada do que esta fase moveu. Ficou fora para o P4 não
  crescer para dois assuntos.
- **`occurredAt` e `isHealthData` no núcleo comum.** Cada evento já tem o seu
  instante — `applicationDate`, `eventDate`, `measuredAt`, `since` — e um
  `occurredAt` ao lado criaria duas respostas para "quando aconteceu"; o dia em que
  divergissem, a linha do tempo mentiria sobre a ordem dos fatos. Quem uniformiza é
  a view. `isHealthData` é propriedade do **tipo** de evento, não da linha: guardar
  por linha abriria a porta para duas vacinas discordarem sobre serem dado de saúde.
  A view deriva do tipo, e hoje deriva `true` para os oito ramos — a classificação
  fina de 3.11 (recado e foto não são) entra quando existir evento que não seja.
- **Cartão preparado pelo tutor com escopo** — o substituto do acesso de
  emergência. `Grant` com escopo é a fundação dele, e o cartão é v1 com tela.

**Como a suíte foi verificada, e uma afirmação que eu tinha escrito errada aqui**

O número que vale é o do comando do CI, em uma tacada:

```
mvn -B clean verify   # 705 testes, Failures: 0, Skipped: 0 — 53s
```

Dos 705, **97 são `ContainerTest`** contra Postgres real, mais a guarda
`ContainerTestsHabilitadosTest`. `Skipped: 0` é a condição de fechamento.

Durante a fase eu rodei em duas metades — `-Dtest='!**/*ContainerTest'` e
`-Dtest='**/*ContainerTest'` — porque o Testcontainers ficou várias vezes sem achar
Docker na suíte completa. **Registrei isso aqui como se fosse regra permanente da
máquina, e não é:** o `clean verify` depois passou com tudo junto, repetidamente. A
condição era transitória, provavelmente o cluster Kubernetes do Docker Desktop
disputando memória. A divisão continua sendo um contorno válido quando o sintoma volta,
mas **a verificação que fecha um passo é o `clean verify` inteiro** — o mesmo que o CI
roda.

Dois detalhes que custaram tempo e ficam registrados:

- **O glob precisa do `**/`.** Com `-Dtest='!*ContainerTest'` o surefire carrega as
  classes de container e as *pula* em vez de excluí-las, e a guarda quebra o build — o
  que é o comportamento certo dela.
- **Rodada filtrada infla a contagem.** Com `-Dtest=!…` o surefire conta classes
  `@Nested` duas vezes: a metade não-container aparecia como 682 quando os testes
  distintos são 607. Os números que ficaram na mensagem do commit `037f789` vêm daí e
  estão superestimados; 705 é o total real.

### A regra de saída do `PRODUTO.md` 3.4 — resolvida em 2026-08-06, contra o documento

O `PRODUTO.md` decidiu que apagar a conta passaria a ser **recusado** até o titular
dar destino ao animal. **Isso não entra, e o motivo é jurídico:** a LGPD dá o direito
de exclusão ao titular, e um produto que recusa o pedido até ele fazer outra coisa
está condicionando o exercício de um direito. O que 3.4 podia legitimamente exigir é
o destino **junto do pedido** — nunca o bloqueio.

**O comportamento que fica** já satisfaz o invariante sem condicionar nada: a
custódia passa para quem tem a concessão mais antiga, e o animal só morre com a conta
quando ninguém mais o alcança. Nenhum animal fica órfão de registro, e nenhum pedido
de exclusão é negado.

O que continua valendo de 3.4 é a exportação antes de sair, que já existe. Se um dia
o produto quiser pedir destino explícito, é campo no request de exclusão — não um
`409` na cara de quem pediu para ser esquecido.

*Isto contradiz o `PRODUTO.md` de propósito, e o documento não foi alterado: ele é o
porquê, e a ressalva dele mesmo em 5.9 diz que raciocínio de produto não é parecer
jurídico. Este é um dos casos em que a diferença apareceu.*

### O que esta fase não faz

Vínculo, turma, lotação, check-in, conteúdo, disponibilidade e percepção **não
entram**. Cabem no vocabulário da seção 3 do `PRODUTO.md` sem entidade nova — é o
teste do modelo se pagando — e são os Horizontes 3 e 4. Construí-los agora seria
modelar para um ator que ainda não existe.

### A rede de segurança desta fase

O que impede um passo de passar verde sem ter rodado:

- **`SchemaMigrationContainerTest`** valida cada migration nova contra Postgres.
- **`AnimalPurgerCoverageContainerTest`** (hoje `PetPurgerCoverageContainerTest`)
  pergunta ao próprio schema quem alcança `animals` e quebra o build se uma tabela
  nova ficar fora da exclusão. Ele já pegou duas em flagrante na Fase 4, e esta
  fase cria pelo menos cinco tabelas.
- **`ContainerTestsHabilitadosTest`** falha em vez de deixar passar, porque a rede
  já foi desligada em silêncio duas vezes neste projeto.

**Rebasear na `main` e rodar `mvn test` de verdade antes de dar qualquer passo por
fechado.** É a lição das branches WIP de 2026-08-04, e ela vale em dobro aqui:
seis PRs em sequência sobre o mesmo núcleo é exatamente a situação em que uma
branch sai de base velha e os testes verdes não provam nada.

## Dívidas com relógio

- ~~**O CI/CD estourou a cota de armazenamento do GitHub Actions.**~~ **Remodelado em
  2026-08-07, e a hipótese deste documento estava errada.** A cota não chegou a 90%: ela
  **estourou**, e o GitHub passou a recusar upload com *"Artifact storage quota has been
  hit"*, pintando de vermelho corridas com 749 testes verdes.

  **Medido, em vez de suposto:**

  | Bucket | Tamanho | Quem consumia |
  |---|---|---|
  | `app-jar` — 129 artefatos | **9.027 MB** | **Ninguém** |
  | Cache do Actions — 210 entradas | **10.658 MB**, contra teto de 10.000 | O build da imagem |
  | ghcr — 87 versões | ~2,2 GB (camadas deduplicam) | Ninguém |
  | `test-reports` — 135 artefatos | 135 MB | Só no dia da falha |

  **O `app-jar` era 98,5% dos 9,2 GB**, e o documento não o mencionava. Ele sobrou de
  quando o pipeline entregava jar pronto para a imagem consumir; hoje o `Dockerfile` é
  multi-stage e compila de dentro do build. Eram **79 MB por push, em toda branch**, com
  a retenção default de 90 dias.

  **Nada estava configurado errado, e isso é o que vale registrar.** Publicar o build
  output é o padrão do starter workflow de Java do próprio GitHub; `cache-to: mode=max` é
  o que a doc do `build-push-action` recomenda; os 90 dias são o default da plataforma.
  O que colide é a aritmética: conta pessoal com **500 MB**, fat jar de 79 MB — **seis
  pushes esgotam a cota inteira** — e **143 corridas em quatro dias**, 76 num só dia.

  **O pipeline foi remodelado para um job e um propósito** (PR #37 e a rodada seguinte):
  saiu o `app-jar`, saiu o job `docker` que publicava imagem que ninguém puxa, saiu o
  upload de relatório que virou a fonte do vermelho, e o gatilho deixou de ser
  `branches: ["**"]` para ser PR e `main`. O que ficou é `mvn verify` com os testes de
  container — que é a única coisa entre um commit ruim e o `dev`, porque o Railway sobe a
  `main` sozinho.

  **Uma armadilha encontrada na medição, para quem for limpar o acumulado:** o `:latest`
  no ghcr não é uma imagem, é um índice OCI com dois filhos — a imagem amd64 e um
  `attestation-manifest`. Essa attestation aparece na listagem **como "sem tag"** (56 sem
  tag contra 31 com tag). A receita comum *"apague todas as versões sem tag"*
  **quebraria o `:latest`**. O critério tem de ser idade, não ausência de tag.

  **O acumulado não foi apagado**, por decisão: os 9 GB vencem sozinhos pela retenção de
  90 dias, no começo de novembro.

- ~~**O `README.md` descreve uma API que morreu na Fase 6.**~~ **Corrigido em 2026-08-07,
  no P6 do passo 4** — e a correção foi na causa, não no sintoma. O README mantinha à mão
  uma tabela de endpoints que o OpenAPI já gera das anotações: duas fontes para a mesma
  pergunta, e a que perde é sempre a escrita à mão. **A tabela saiu.** O que ficou é o
  que o OpenAPI não sabe dizer — o vocabulário, as regras que atravessam rotas, e as
  leituras que o cliente não deve montar sozinho. Junto, `summary` nas 90 operações,
  porque é de lá que o cliente do frontend é gerado.

  **O que sobra dessa dívida:** os tutoriais de fluxo tiveram os caminhos corrigidos, mas
  a prosa ainda fala em *"vet"* e *"clínica"* onde o modelo diz pessoa com credencial e
  organização com capacidades. Não mente mais sobre rota, e ainda usa vocabulário velho.
- ~~**Spring Boot 2.7.18 saiu do suporte OSS.**~~ Migrado para Boot 3.3.5 em
  2026-08-04 (PR #16).
- ~~**Actions com Node 20 deprecado.**~~ Bump para v7/v5/v4/v7/v4/v7 feito na
  higiene lateral (PR #17), 2026-08-04.
- **OCR em container nunca foi exercitado.** O build instala o Tesseract e
  descobre o `tessdata`, mas ninguém chamou o endpoint de importação de dentro do
  container. A base mudou de Debian slim para Ubuntu 22.04 na correção da imagem,
  então a versão do Tesseract também mudou.
- ~~**Os testes de container não rodavam, e o build não dizia.**~~ Corrigido em
  2026-08-05, junto com o passo 9. O `docker-java` assume a API do Docker na
  versão 1.32 quando a negociação pelo named pipe do Windows falha, e daemon
  moderno recusa abaixo de 1.41. O Testcontainers trata isso como "sem Docker" e
  **pula** os testes em vez de falhar: 14 testes verdes que nunca executaram,
  entre eles os que validam as migrations contra Postgres de verdade. Ou seja, as
  `V10` a `V13` foram mergeadas sem nunca terem sido aplicadas num Postgres real.
  A correção é `api.version` no surefire, com a versão do Testcontainers acima da
  que o Boot gerencia. **O que fica de lição:** teste que pula em silêncio é pior
  que teste que falha, porque some do radar exatamente quando mais se confia nele.

## As branches WIP de 2026-08-04 — retomadas e fechadas

Três subagentes foram lançados em paralelo em 2026-08-04. Dois morreram por cota
antes de commitar, mas deixaram o worktree com código escrito, subido como WIP.
**Os dois foram retomados e fechados em 2026-08-05**, e nenhuma branch WIP
continua viva: o passo 9 está registrado na seção própria acima, e as dívidas
operacionais logo abaixo.

**O que os dois casos ensinaram, e vale mais que o código que entregaram:**
código de subagente que nunca rodou parece pronto e não está. O passo 9
reintroduzia o furo de espécie que o passo 6 tinha fechado horas antes; as
dívidas operacionais tinham uma classe que não compilava, um rate limit valendo
metade do configurado e seis testes quebrados. **Nos dois casos o erro só
apareceu depois de rebasear na `main` e rodar `mvn test` de verdade** — e em
nenhum dos dois o rebase era opcional, porque a branch tinha saído de uma base
anterior aos merges do mesmo dia.

### Dívidas operacionais — concluído em 2026-08-05

Rate limit, paginação e observabilidade. As três eram dívida de operação, não
valor de produto — mas a primeira é a que impede que os endpoints de e-mail
virem máquina de mandar mensagem para terceiro.

- [x] **Rate limit por IP** nos públicos: 10/min no login, 5/min em cadastro,
      recuperação de senha e reenvio de confirmação. Roda antes do
      `JwtAuthenticationFilter` — não faz sentido validar token de um flood que
      seria bloqueado de qualquer jeito.
- [x] **Paginação** em `/pets`, `/vaccines`, `/health-records` e `/clinics`,
      com teto de 100 por página. As listagens escopadas por pet continuam array,
      por serem naturalmente pequenas.
- [x] **`/actuator/prometheus`** exposto e **protegido por token**, ao contrário
      do `health`. Métrica diz quanto o sistema é usado e quando alguém está
      tentando entrar; o health é público só porque o provedor o chama sem
      credencial.

**Três defeitos que só apareceram ao rodar o que o WIP nunca rodou:**

1. `PageableConfig` chamava `setDefaultPageable`, que **não existe** — não
   compilava. Substituído por `spring.data.web.pageable.*`, que é como o Boot
   expõe isso, e a classe deixou de ser necessária.
2. `RateLimitFilter` implementava `Filter` cru sendo `@Component`. Isso o
   registra **duas vezes** — na cadeia do servlet container e na
   `SecurityFilterChain`, a segunda rodando dentro da primeira. Cada requisição
   consumiria dois tokens do balde, e o limite real seria **metade** do
   configurado: 5 logins por minuto onde a property diz 10. Corrigido com
   `OncePerRequestFilter`, como o `JwtAuthenticationFilter` vizinho já fazia, e
   travado por teste.
3. `AuthServiceImpl` ganhou `PetfyMetrics` sem que `AuthServiceImplTest`
   soubesse — seis testes quebravam com `NullPointerException`.

**O choque com o passo 4, que era o risco registrado:** o OpenAPI publicado no
mesmo dia descrevia as listagens como array, e agora elas são `Page`. Como não
existe cliente ainda, a mudança saiu de graça — foi exatamente por isso que
valeu fazer agora e não depois. Está documentada no `README`.

**Atenção antes de mergear, descoberto em 2026-08-05:** esta branch saiu do PR
#18 e colide com o passo 4, que entrou no mesmo dia. O OpenAPI recém-publicado
descreve as listagens como `List`, e a paginação muda isso para `Page` — o
contrato mudaria no dia seguinte ao de ser publicado. Ou a paginação entra antes
de alguém consumir o contrato, ou vira versionamento de endpoint. **Rebasear na
`main` antes de qualquer coisa:** foi o que o passo 9 exigiu, e é o que revela o
conflito de verdade — os testes rodados sobre a base velha não provam nada.

Vale fatiar: o **rate limit** é a parte que protege recuperação de senha e
reenvio de confirmação, é aditiva e não mexe em contrato. A paginação é a parte
cara. Não precisam entrar juntas.

## Fica para depois do frontend

Não é dívida esquecida: são itens que **a tela define melhor do que o modelo**.
Especificá-los agora, sem interface, é convite a refazer.

- **Medicação e tratamento contínuo.** Pet com diabetes, cardiopatia ou epilepsia toma
  remédio em horário, com posologia, por tempo determinado ou indefinido. É justamente a
  população que mais precisa de lembrete, e o antiparasitário já é um caso particular
  disso — mas o formato de "quando avisar" e "como marcar como tomado" é decidido pela
  tela.
- **Consulta agendada.** A agenda de hoje é derivada de data de próxima dose: é lista de
  pendências, não calendário. Marcar, confirmar, cancelar e horário de clínica é
  mecânica de interface.
- **Notificação além de e-mail.** No Brasil, lembrete de vacina por e-mail tem abertura
  baixa; WhatsApp e push são o canal real. A arquitetura já ajuda — `Notifier` é
  interface e não conhece o domínio, então é canal novo e não refatoração. Depende de
  saber onde o usuário está.
- **Entrar sem senha.** A Tela 28 desenhou "Receber um link por e-mail", e o botão ficou
  desabilitado desde que a tela subiu — saiu em 2026-08-14, junto com a frase que pedia
  desculpa por ele. **Não seria link:** sem `petfy.app.base-url` por ambiente, o que chega a
  quem precisa entrar é um código, como já acontece com o convite e com o cartão. Então é
  recurso próprio, e não um botão: tabela de token de uso único, expiração curta, limite de
  tentativa e a tela de digitar o código. Quem esquece a senha já tem saída — a recuperação
  resolve na própria porta, sem trocar de tela.
- **As páginas que o rodapé prometia.** Em 2026-08-14 os doze itens do rodapé eram `<div>`:
  nenhum clicava. Termos e privacidade passaram a existir; o resto **saiu do rodapé** e volta
  quando tiver para onde levar — as quatro institucionais (`Para tutores`, `Para clínicas`,
  `Para creches e petshops`, `Para abrigos`), as quatro de ajuda (`Central de ajuda`,
  `Falar com a gente`, `Acessibilidade`, `Status do sistema`) e o canal do **encarregado de
  dados (LGPD)**, que é exigência legal e não conteúdo de marketing — a política de privacidade
  já declara que ele falta. Duas outras dependem de tela, e não de texto: `Quem lê o registro
  dos meus animais` hoje é por animal, e `Levar meus dados embora` só existe na API
  (`/persons/me/export`), sem interface.
- **O texto jurídico de verdade.** As páginas `/termos` e `/privacidade` descrevem o que o
  produto faz, conferido contra o código, e marcam as seções que dependem de decisão humana:
  retenção por tipo de dado, identificação do controlador e do encarregado, sub-operadores com
  país de processamento, preço, suspensão de conta e foro. Enquanto elas estiverem marcadas, o
  aceite do cadastro aponta para um documento que se declara incompleto.
- **ZIP no export**, com os arquivos dentro em vez de só os links.
- **Leitura de anexo pelo veterinário.** É o que fecha o ciclo do laudo. O
  `AccessedResource.ATTACHMENTS` já existe no enum esperando o gancho.

### Ainda não levantado com você

- **Espécie além de cão e gato.** O enum tem dois valores por decisão consciente, e o
  catálogo depende dele. Ave, roedor e coelho são fatia relevante de clínica.
- **Veterinário em mais de uma clínica.** `Vet.clinic` é `@ManyToOne` singular, e quem
  atende em duas precisaria de duas contas — com dois e-mails, o que o namespace único
  impede.
- **Raça como dado**, não String livre. Importa para risco por raça e curva de peso
  esperada.

## Fora do escopo desta rodada
- **Microserviços, API Gateway e RabbitMQ**, a intenção registrada no início do
  projeto. O monólito por domínio aguenta muito mais do que o volume desta
  rodada.
- **SonarQube.** O passo existe no pipeline e é pulado quando falta o
  `SONAR_TOKEN`; ligar é questão de configurar o secret.
- **Cobrança, multi-tenancy e verificação de CRMV como produto.** Só fazem
  sentido se o objetivo virar comercial.
