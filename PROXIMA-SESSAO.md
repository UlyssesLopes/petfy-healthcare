# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-11, no fim de uma sessão que fechou **dois blocos, 3 e 4, os dois mergeados**.

## Onde o trabalho está agora

**A `main` está em `05b7b0f`, e não há trabalho pendente.** Os PRs **#56 (bloco 3)** e
**#57 (bloco 4)** foram mergeados, os dois com CI verde. Nenhuma branch de feature aberta.

**892 casos no backend, 43 no front, `Skipped: 0`.** Contrato regenerado, nenhuma operação
renomeada. `npm run build` limpo.

**Seis telas novas no ar:** 40, 41, 42 (por onde o valor entra) e 37, 38, 39 (custo do cuidado).

**Uma armadilha de fluxo que custou tempo, e que vai voltar:** o bloco 4 começou na branch do
bloco 3 (a decisão da sessão foi essa, porque PR empilhado não roda CI). Mas o **#56 foi mergeado
antes** de os commits do bloco 4 serem empurrados — e aí eles ficaram numa branch cujo PR estava
fechado, **sem CI nenhum e sem aviso**. O sintoma é mudo: o `git push` funciona, e o
`gh pr view` mostra o `headRefOid` antigo para sempre.

**Regra: commit numa branch cujo PR já mergeou não roda check nenhum.** Bloco novo, branch nova,
tirada de uma `main` recém-sincronizada.

**892 casos no backend, 43 no front, tudo verde, `Skipped: 0`.** Contrato regenerado, nenhuma
operação renomeada. `npm run build` limpo.

### O bloco 3, inteiro

- `V36__custo_do_animal.sql` — a tabela `animal_costs`, e `monthly_fee` / `due_day` / `daily_rate`
  na `enrollments`
- `V37__os_dias_combinados.sql` — a `enrollment_weekdays`, sem a qual a diária não tinha como entrar
- `AnimalCost`, `AnimalCostKind`, `CostRecurrence`, repositório, serviço e duas rotas
  (`GET` e `POST /animals/{animalId}/costs`)
- `PUT /professional/creche/enrollments/{id}/agreement` — o combinado inteiro, e não campo a campo
- **A diária avulsa entra sozinha no check-in**, com as três recusas a cobrar por suposição
- `AnimalAccessGuard.respondePor` — a guarda que impede o combinado de vazar por concessão
- **Tela 40** — "Valor cobrado · opcional" dentro de `pacientes.$animalId.atendimento.tsx`
- **Tela 41** — `/creche/matricula/{turma}/{matricula}` (creche escreve) e a caixa "O que foi
  combinado" em `animais.$animalId_.matricula.tsx` (tutor lê)
- **Tela 42** — `/animais/{id}/compra`, o único lançamento manual do produto
- `animal_costs` e `enrollment_weekdays` classificadas nas **duas** guardas de cobertura

### O DIA COMBINADO NÃO EXISTIA, e essa foi a decisão desta sessão

O desenho diz *"diária avulsa: usada quando o Code vem **fora dos dias combinados**"* — e não havia,
em lugar nenhum do modelo, o dia em que o animal é esperado. Sem isso o encadeamento central do
bloco não tinha como existir sem alguém digitar, que é o que o desenho recusa.

Duas saídas sem coluna foram olhadas e recusadas: *"diária só quando não há mensalidade"* contradiz
o próprio exemplo do desenho (R$ 530/mês **e** uma diária de 22/07 na mesma matrícula), e *"a creche
marca avulso no check-in"* apaga o *"ninguém digitou nada"*.

O custo: a Tela 41 ganhou **um controle que o mockup não desenhou** — a linha de dias dentro da
caixa "Combinado com o tutor". Está registrado aqui porque é um desvio do FE aprovado, e foi
aprovado em sessão.

**Conjunto vazio é "não sei", e nunca "nenhum dia"** — sem dias declarados a diária nunca entra.
O silêncio da creche não vira cobrança.

### Tela 41, e o cartão que ela NÃO pode mostrar

O desenho põe, à direita, "o que o Marcelo passa a ver": a mensalidade **e** a diária de 22/07 já
lançada. **A creche não pode mostrar isso** — ler custo exige custódia. Então a tela da creche
mostra o que o combinado *vai produzir*, derivado dos campos ao lado, e diz com todas as letras que
aquela é a parte dela e não a conta do animal. A conta mora na tela do tutor, onde a caixa "O que
foi combinado" **some sozinha** para quem alcança o animal só por concessão: os quatro campos vêm
nulos do servidor.

Rota nova: `/creche/matricula/{classGroupId}/{enrollmentId}`, alcançada pelo **nome do animal** na
Tela 17 — um nome não disputa com o gesto das 7h30, um botão disputaria.

### Tela 42, e o preço que ela paga de propósito

"O que foi" são **três botões e nenhum campo de texto**, como o desenho desenha. A consequência é
real e foi aceita: uma compra marcada como "Outro" chega ao custo dizendo só "Outro". O desenho
aceita isso de olhos abertos — *"quanto mais campos, menos gente lança, e menos verdadeiro fica o
custo"*. Uma compra lançada vale mais do que uma compra bem descrita que ninguém lançou.

**A caixinha "dura cerca de um mês" é o campo mais importante da tela**, e não parece. É ela que
transforma compra avulsa em custo mensal previsível — e é dela que o bloco 4 vai depender para
dizer "a ração do Teco custa R$ 190 por mês, todo mês".

## O bloco 4, e o que ele já tem

- `V38__onde_o_dinheiro_foi.sql` — `AnimalCostCategory` (SAUDE, ALIMENTACAO, CRECHE, HIGIENE,
  OUTRO), retroativa pelo `kind` e `NOT NULL` só **depois** do retroativo
- `V39__o_valor_da_dose.sql` — `source_vaccine_id` e `source_antiparasitic_id`, sem os quais a
  previsão não teria preço
- `GET /animals/{id}/costs/summary?window=DOZE_MESES|SEMPRE` — os três números, as fatias e os
  pagadores num payload só
- `GET /animals/{id}/costs/forecast` — as quatro fontes do que vem pela frente
- **Tela 37** — `/animais/{id}/custo`
- **Tela 38** — `/animais/{id}/previsao`
- **Tela 39** — `/animais/{id}/custo-da-adocao`, alcançada da tela de adoção

### Duas decisões desta parte

**A categoria não é o `kind` com outro nome.** O `kind` diz de que evento o valor saiu; a categoria
diz em que ele foi gasto. Divergem no caso que decide o gráfico: **ração e remédio saem os dois de
uma `COMPRA`**. Os três botões da Tela 42 passaram a carregar a categoria — o que também conserta o
"Outro" que o bloco 3 tinha aceito como preço.

**"Quem pagou" é `organization == null`, e não o `kind`.** Num atendimento o autor é a veterinária:
ela informou o valor e não disse quem o pagou. Há a linha **sem nome** para a conta fechar, e o
produto não vai pedir que o tutor atribua depois.

### A paleta do desenho falha no validador, e eu mantive

Os quatro tons de fatia do desenho são uma **rampa de luminosidade na mesma matiz**. Rodados no
validador de paleta: **passam** em separação para daltonismo (ΔE 11,5), mas **falham** o piso de
visão normal — `#8fae94` ↔ `#bdd0c0` a ΔE 12, abaixo de 15 —, e os dois tons mais claros ficam
abaixo de 3:1 de contraste contra o branco.

**Mantive a paleta** (é o FE aprovado) e paguei o alívio que o validador exige: **o nome de cada
fatia aparece sempre ao lado da cor**, e no lista de eventos o quadradinho tem `title`. A cor é
reforço; a identidade é o rótulo.

**Dois defeitos que o mockup não podia mostrar:**

1. **A cor seguia a posição, e agora segue a categoria.** As fatias são ordenadas por valor, e a
   ordem muda ao trocar a janela — pintar por posição faria a creche sair do verde-escuro e a saúde
   entrar nele **entre dois cliques da mesma tela**.
2. **`OUTRO` não pode ser um quinto tom plano.** A rampa está cheia em quatro: minha primeira
   tentativa (um cinza) ficou a **ΔE 3,5** de Alimentação em deuteranopia. Virou **hachura** — canal
   que sobrevive a daltonismo e a impressão, e que já é vocabulário do desenho.

**E a frase "saúde é o menor pedaço" só aparece quando saúde é de fato a menor fatia.** Escrita
fixa, ela mentiria no mês em que a saúde fosse o maior gasto — que é justamente o mês em que o
animal está doente.

### A Tela 38, e o preço que não tinha de onde sair

O desenho põe "antirrábica R$ 90" em cada linha, e **nenhum desses valores tinha fonte**. A
`animal_costs` ligava-se a atendimento e a matrícula; uma dose de vacina não é nenhum dos dois.
Precificar pelo último custo de `SAUDE` cobraria a antirrábica com o preço de uma consulta
dermatológica — pior que não ter preço.

**V39: `source_vaccine_id` e `source_antiparasitic_id`.** O reforço do ano que vem é precificado
pela dose **do mesmo item de catálogo daquele animal**. O campo de valor entrou no `RegistrarDose`,
opcional; se o lançamento falhar, **a dose fica registrada**. Tipo novo `AnimalCostKind.VACINA` —
uma dose não é um atendimento, e o `kind` existe para ler agrupado.

**A conta mora no `CostForecastBuilder`, sem repositório e sem mock**, com 12 casos: um erro ali não
aparece como exceção, aparece como número plausível e errado, e número plausível ninguém confere.

**O total não finge ser tudo:** `itemsWithoutAmount` diz quantas linhas ficaram de fora.

**Dois números do desenho não existem, e por razões diferentes:**

1. *"foram R$ 176 (...) com **outro animal da turma**"* — é custo de outro animal, e ler custo exige
   custódia dele. No lugar entrou aritmética sobre o que este tutor já vê: **o valor de um dia
   combinado** (mensalidade ÷ dias combinados ÷ 4,348). Não é a diária avulsa — essa é o que se paga
   por um dia *extra*, e o que se perde ao ser barrado é um dia que a mensalidade já cobriu.
2. **O botão "Dispensar" não existe.** Guardar a dispensa pediria uma tabela, e um botão que esquece
   no recarregamento é pior que nenhum. No lugar, a tela diz onde o gesto existe: silenciar a
   pendência no feed.

**A leitura só aparece quando os fatos a sustentam** — dose atrasada **e** matrícula viva cuja
comprovação impede a entrada. Genérica, ela viraria conselho, e conselho genérico é o que faz a
pessoa parar de ler.

### A Tela 39, e o rótulo que mudou

Ela é **frontend puro**: reusa os dois endpoints, e o abrigo lê o custo do Teco sem regra de acesso
nova — `requireCustodia` já conta custódia de organização desde a Tela 13.

**O desenho chama o segundo número de "Previsto para os próximos 12", e eu não pude.** A previsão
deste produto cobre só **o que está marcado**; a consulta que vai aparecer no meio do ano não está
nela, e não pode estar (seria estimativa). Logo o número vem **menor** que os doze meses que
passaram — e chamá-lo de "previsto" na frente de quem está decidindo adotar **subestimaria o custo
do animal**, o oposto exato do que a tela existe para fazer.

Virou **"Já marcado para os próximos 12"**, com a falta dita ao lado do número e não num rodapé:
*"este número é um piso, e não um teto."*

E **"tende a subir: ele tem 11 anos" o produto não afirma.** Definir a partir de que idade um animal
é idoso é conhecimento veterinário que este código não tem, e varia por espécie e porte. A idade
aparece como fato na ficha; o julgamento fica com quem conversa.

## O bloco 4 está fechado e mergeado (PR #57)

## O ERRO QUE O DOCUMENTO ANTERIOR CONTINHA

A versão anterior deste arquivo afirmava:

> *"Telas 12, 18 e 25–27 não têm especificação no repositório. O arquivo com as telas numeradas
> do Claude Design nunca foi versionado — em `design/` só existem o `BRIEFING.md`, o
> `home-tutor.html` e quatro capturas."*

**Isso era falso, e travou trabalho à toa.** Havia 8 arquivos de tela versionados em
`design/IdentidadeVisual/` desde antes, e as **Telas 12, 13 e 18 sempre tiveram especificação**.

O que de fato nunca existiu são as **Telas 25, 26 e 27** — e também 04–06, 19–21, 35–36. Elas não
foram desenhadas, e não é o repositório que as perdeu.

## A fila, e onde ela está

Em 2026-08-11 o Ulysses acrescentou **7 arquivos novos** em `design/IdentidadeVisual/`, com 19
telas. Eles estão versionados (entraram no PR #53).

| # | Bloco | Estado |
|---|---|---|
| 1 | A faixa sai de dentro do cabeçalho sticky | **Fechado** — PR #54 |
| 2 | Núcleo clínico — Telas 30, 31, 32 | **Fechado e mergeado** (PRs #54 e #55) |
| 3 | Por onde o valor entra — 40, 41, 42 | **Fechado e mergeado** — PR #56 |
| 4 | Custo do cuidado — 37, 38, 39 | **Fechado e mergeado** — PR #57 |
| 5 | Fim e reencontro — 33, 34 | pendente |
| 6 | Animal comunitário — 43, 44, 45 | pendente |
| 7 | Apadrinhar, hospedar, o ano — 46, 47, 48 | pendente |
| 8 | Tela 18 — petshop | pendente (a especificação sempre existiu) |

A ordem foi escolhida por dependência: a 40 é literalmente "dentro da tela 31", e o custo (37–39)
só tem o que dizer depois que 40–42 gravarem custo.

## O que esta sessão fechou, e o que importa daqui

### A moldura do produto (PR #53)

Onze rotas desenhavam a própria marca à mão. Agora há **um** cabeçalho e **um** rodapé, na raiz —
porque foi "cada rota se lembra de repetir" que produziu os onze. Três variantes, todas do desenho:
completa nas telas autenticadas, reduzida nas portas e no stepper, nenhuma no cartão público.

Backend ganhou **um** campo: `emailVerified` no `/me/context`, para a faixa.

**Duas afordâncias ficam desabilitadas com o motivo** — a busca (não há rota de busca, nem campo
de RGA) e o marcador de avisos (não há canal). Um marcador que nunca acende ensina a pessoa a não
olhar para ele.

### O núcleo clínico (PR #54)

As três origens da Tela 30 **já vinham inteiras do servidor** — `organizationName`,
`credentialLabel`/`credentialStatus`, `eventType`. Faltava a tela distinguir.

Backend novo: os dois recortes do histórico (`onlyMyOrganization`, `onlyMine`). **Não é filtro de
cliente** — a página tem vinte itens e a vida de um animal de dez anos tem centenas.

**O aviso de sobreposição de medicamento é do cliente**, e é decisão registrada: guardas que
**recusam** moram no servidor (dose duplicada = 409/149); este só pede confirmação. O custo está
escrito no arquivo — outro cliente que prescreva não vai avisar.

### A união de cadastros (esta branch)

**Evento move, vínculo não** — o critério inteiro do `AnimalMerger`. Custódia, concessão,
matrícula, convite e log de acesso **ficam**, cada um por uma razão escrita na guarda de cobertura.

**A guarda de cobertura pegou um defeito antes de eu escrever a linha seguinte:** o
`AnimalPurgerCoverageContainerTest` reprovou porque `animal_merge_requests` aponta para `animals` e
o purger não a cobria. **Quarta vez que essa guarda pega a mesma classe de defeito.**

O `AnimalMergerCoverageContainerTest` nasce fazendo o mesmo pela união. Ali o sintoma seria pior
que um 500 — **silencioso**: a união terminaria "com sucesso" deixando eventos para trás.

## O que continua faltando, e por quê

- **Não há canal de aviso.** A Tela 03 lista quem está vencendo e não avisa ninguém, a Tela 23 não
  avisa quem registrou, e a Tela 24 continua inconstruível. É decisão de produto, não de código.
- **A busca não existe** — nem rota, nem campo de RGA no modelo.
- **Capacidade de organização não se declara** na criação (Tela 15), e **espécie é só CANINA e
  FELINA**.
- **O escopo do evento de união é um compromisso**: quem tem concessão só de `OBSERVACOES` ou só de
  `PESO` não verá a união, e para essa pessoa o histórico vai parecer ter dobrado sozinho.
  Resolver de verdade pede um escopo que não existe hoje.
- **A edição de vacina não passa pela guarda de dose duplicada.**

## Armadilhas desta máquina

- **`vite build` suja o `arvore-de-rotas.gen.ts`**: `git status` mostra modificado, `git diff` vem
  vazio (é `core.autocrlf` sem `.gitattributes`), e **o `git pull` aborta**. Já me fez ramificar de
  uma `main` desatualizada. Conferir `git log` DEPOIS do pull; `git reset --hard origin/main`
  destrava com segurança porque o arquivo é gerado.
- **O Rancher Desktop precisa estar aberto** — sem ele não há Docker, e sem Docker o
  `OpenApiContractTest` é **pulado** em silêncio. Conferir `Skipped: 0`.
- **O container `petfy-pg-sessao` já existe**: `docker start petfy-pg-sessao`, e não `docker run`.
- **`-Dtest=` com vírgula e `-D` com ponto precisam de aspas no PowerShell**, e
  `Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir.
- **Here-string do PowerShell quebra em aspas duplas** dentro da mensagem: escrever a mensagem de
  commit num arquivo e usar `git commit -F`.
- **`cd` no Bash contamina o cwd do PowerShell** — rodar `mvn -f <pom absoluto>`.
- **A conta ativa do `gh` é a de trabalho**, e ela não enxerga este repositório. O erro mente:
  *"Could not resolve to a Repository with the name"*, como se ele não existisse. E só aparece na
  hora de abrir o PR — o `git push` funciona, porque usa credencial do git e não do `gh`.
  `gh auth switch --user UlyssesLopes`.
- **Commit numa branch cujo PR já mergeou não roda check nenhum**, e nada avisa: o `git push`
  funciona e o `gh pr view` mostra o `headRefOid` antigo para sempre. Bloco novo, branch nova.
- **O merge de PR é comando do Ulysses, sempre.**
- **Nenhuma tela foi conferida no navegador** em nenhuma destas entregas: a extensão do Chrome não
  esteve conectada. **E isso já custou um defeito:** o `creche.tsx` estava com texto
  **duplo-codificado** — `Â·` entre o nome do animal e a espécie, `â€”` na coluna de entrada —, e
  isso apareceria assim em tela desde que a Tela 17 foi entregue. Os dois foram corrigidos; **os
  comentários daquele arquivo continuam corrompidos**, e nenhum teste pega isso.
- **ICU não aceita chave vazia em `select`.** `{x, select, {} {} other {…}}` compila e só explode
  em tela, no caso raro. Quando o texto muda conforme um campo existir ou não, são **duas
  mensagens** concatenadas — não um `select` sobre string vazia.

## Primeiro passo da próxima sessão

**Sincronizar a `main` e ramificar dela** — ela está em `05b7b0f` e não há nada pendente. Depois,
**o bloco 5 — Fim e reencontro, Telas 33 e 34**. O desenho é
`design/IdentidadeVisual/Telas Petfy - Fim, reencontro e conta.dc.html`, que **ainda não foi lido**.

E antes de qualquer `gh`: **`gh auth switch --user UlyssesLopes`.** A conta ativa é a de trabalho,
que não enxerga este repositório — e o erro finge que o repositório não existe.

**Duas coisas para conferir antes de planejar o bloco 5:** o `CustodyEndReason` já traz os motivos
de fim (a adoção usa `ADOCAO`), e a Tela 34 do índice antigo era "a conta" — vale abrir o arquivo e
ver quais das três telas dele são 33 e 34, porque o nome do arquivo cobre mais do que o bloco.

**E há uma dívida barata pendente, do bloco 3:** os **comentários** do `web/src/rotas/creche.tsx`
continuam duplo-codificados (`Â·`, `â€”`). As strings visíveis foram corrigidas; os comentários não,
e nenhum teste pega isso. É um `sed` e uma revisão.

### O que essas telas NÃO têm, e o desenho diz com todas as letras

- Nenhuma integração com banco ou cartão. "O Petfy não olha sua conta."
- Nenhum orçamento mensal, nenhuma meta, nenhum aviso de que passou do limite.
- Nenhum losango e nenhum "valor não informado" em cinza: **a ausência de preço não é pendência**,
  é um evento clínico completo.

## O que a Tela 32 deixou pronto, e vale saber antes de mexer

Cinco rotas: `GET /animals/{id}/duplicates`, `POST /animals/{id}/merge-requests`,
`GET /animals/{id}/merge-requests`, `POST /merge-requests/{id}/accept`, `.../reject`, e
`POST /animals/{id}/duplicates/{outro}/distinct`.

**O desenho tinha um botão que o backend não cobria:** "são animais diferentes" aparece na tela da
clínica, sem pedido nenhum no meio. Virou rota própria — pedir a união mexe na vida registrada e
precisa de quem responde; dizer "são outros bichos" só acende uma marca.

**A tela não adivinha quem pode decidir.** Mostra os botões e deixa o servidor responder 403.
Esconder o botão exigiria o cliente recalcular custódia — uma segunda verdade sobre quem manda.

## Como subir, e como regenerar

```
docker start petfy-pg-sessao
DB_PORT=5433 mvn spring-boot:run -Dspring-boot.run.profiles=local
cd web && npm run dev      # http://localhost:5173
```

Um PostgreSQL nativo do Windows ocupa a 5432 — por isso a 5433.

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true
cd web && npm run gerar:api
mvn test "-Dsurefire.runOrder=reversealphabetical"   # caça asserção dependente de ordem
```

Os três primeiros entram no **mesmo commit** da mudança que os causou.
