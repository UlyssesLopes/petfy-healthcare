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

**Próximo: passo 8 (multi-tutor), e o passo 10 logo atrás.** O 8 vem primeiro por
dependência real, e não por preferência: ele muda quem é titular de um pet, e a
exportação LGPD do passo 10 precisa exportar os dados do titular. Escrever o
export antes seria escrevê-lo assumindo dono único, para reescrever depois.

**Depois do 8, o backend cumpre o que esta rodada prometeu** e a Fase 5 abre. O 8
é o último item que muda a *forma* do dado que a tela mostra — é essa a linha que
separa "falta backend" de "falta frontend".

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

### 8. Multi-tutor e transferência de titularidade

`Pet` tem um `owner` só (`@ManyToOne`). Casal e família dividem o mesmo pet, e
hoje o segundo tutor só consegue um link de leitura; adoção e venda não têm
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

#### Estado do 8a em 2026-08-05 (branch `feat/passo-8-multi-tutor`, sem PR)

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

### 10. LGPD e confiança — parcialmente concluído

- [x] **Exclusão a pedido do titular.** `DELETE /owners/me` agora apaga em
      cascata todo o rastro do tutor: correções → vacinas/histórico/shares/
      acessos por clínica → pets → tokens → owner. **Política escolhida: apagar,
      não anonimizar nem transferir.** Anonimizar deixaria dado de saúde
      associado a um "ex-tutor fantasma" que o vet ainda enxerga — contraria o
      pedido de sair. Transferir pressupõe multi-tutor (passo 8), que não
      existe. Cascata é o que atende ao pedido sem meio-termo. Cobertura por
      teste de mock (`OwnerServiceImplTest.deveApagarEmCascataNaOrdemCerta`) e
      pelo `OwnerDeletionContainerTest` no CI. Concluído em 2026-08-04.
- [ ] **Exportação a pedido do titular** — não entrou nesta rodada. Endpoint
      agregador `GET /owners/me/export` (JSON com owner + pets + vaccines +
      health records + shares) é o próximo passo natural, mecânico.
- [ ] **Rastro nos links públicos de carteira.** Não entrou. Precisa de tabela
      nova `pet_share_access_log` e hook em `SharedCardController` gravando IP
      e timestamp a cada `GET /share/{token}`. Também vale limite de acessos
      por link.
- [ ] **CRMV/clínica não verificada.** Decisão pendente — mistura produto e
      compliance (validação de CRMV exige integração externa).

**Pronto quando:** você atende um pedido de exclusão sem abrir o banco na mão.
**Exclusão feita; export e rastro ainda em aberto.**

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

## O que restou em aberto do passo 10

Nunca esteve nas branches WIP, e continua pendente:

- Export LGPD `GET /owners/me/export` (mecânico).
- Rastro em `SharedCardController` (nova tabela + hook + endpoint de leitura).
- Decisão sobre CRMV/clínica não verificada.

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
