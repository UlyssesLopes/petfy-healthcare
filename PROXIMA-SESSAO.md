# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-11, com o **bloco 2 fechado e mergeado** e a **espinha do bloco 3 de pé**.

## Onde o trabalho está agora

**Branch `feat/por-onde-o-valor-entra`, um commit (`bba76b3`), sem PR aberto.**
A `main` está em `931dac0` — os PRs #53, #54 e #55 já foram mergeados.

**860 casos, 0 falhas, `Skipped: 0`** contra Postgres real. Contrato regenerado, nenhuma operação
renomeada. O front ainda não foi tocado neste bloco.

### O que já está pronto no bloco 3

- `V36__custo_do_animal.sql` — a tabela `animal_costs`, e `monthly_fee` / `due_day` / `daily_rate`
  na `enrollments`
- `AnimalCost`, `AnimalCostKind`, `CostRecurrence`, repositório, serviço e duas rotas
  (`GET` e `POST /animals/{animalId}/costs`)
- A regra de acesso, com cinco testes
- `animal_costs` classificada nas **duas** guardas de cobertura (move na união, some no purge)

### O que já está pronto no bloco 3 (continuação)

- **O combinado da creche, inteiro** — `V37__os_dias_combinados.sql`, `PUT
  /professional/creche/enrollments/{id}/agreement`, e os quatro campos no DTO de matrícula.
- **A diária avulsa entra sozinha no check-in**, com as três recusas a cobrar por suposição.
- **Tela 40** — a caixa "Valor cobrado · opcional" dentro de `pacientes.$animalId.atendimento.tsx`.

**868 casos no backend, 43 no front, tudo verde, `Skipped: 0`.**

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

### O que falta no bloco 3

1. **Tela 42** — "o que você compra por fora": três toques, na área do tutor.

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
| 3 | Por onde o valor entra — 40, 41, 42 | **Espinha de backend pronta**; falta o resto |
| 4 | Custo do cuidado — 37, 38, 39 | pendente |
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
- **O merge de PR é comando do Ulysses, sempre.**
- **Nenhuma tela foi conferida no navegador** em nenhuma destas entregas: a extensão do Chrome não
  esteve conectada.

## Primeiro passo da próxima sessão

**Terminar o bloco 3**, na ordem abaixo. O desenho é
`design/IdentidadeVisual/Telas Petfy - Por onde o valor entra.dc.html`.

1. **A mensalidade na matrícula.** Expor `monthlyFee`, `dueDay` e `dailyRate` no DTO de matrícula
   e aceitá-los na escrita. As colunas já existem.
2. **A diária avulsa no check-in.** Em `CrecheServiceImpl`, no `check-in`: se o dia não é dos
   combinados e a matrícula tem `dailyRate`, criar um `AnimalCost` do tipo `CRECHE_DIARIA` com
   `sourceEnrollmentId`. Usar `existeDiariaNoDia` antes, para não cobrar duas vezes.
3. **Tela 40** — o campo de valor dentro de `pacientes.$animalId.atendimento.tsx`. Um item tem
   descrição, valor e "já foi pago", e há "adicionar outro item". Lançar depois de o atendimento
   gravar, com `sourceHealthRecordId`.
4. **Tela 41** — o combinado, dentro da tela de matrícula.
5. **Tela 42** — "o que você compra por fora": três toques, na área do tutor.

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
