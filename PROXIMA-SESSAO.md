# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-12, no fim de uma sessão que fechou o **bloco 5** e deixou o **bloco 6 pela
> metade** — backend inteiro, frontend nenhum.

## Onde o trabalho está agora

**O bloco 5 está mergeado** — PR #59, CI verde. A `main` está em `4524050`.

**O BLOCO 6 ESTÁ FECHADO — Telas 43 e 44, no PR #60**, esperando seu comando. A Tela 45
(encaminhamento) ficou de fora de propósito e virou o bloco 6b.

**932 casos no backend, 51 no front, `Skipped: 0`.**

**Duas telas no ar desde o bloco 5:** 33 (encerrar a linha do tempo) e 34 (achei um animal na rua).

**O conflito que os dois PRs produziram, e que vai voltar:** #58 e #59 reescreveram esta página. A
resolução foi "a versão mais nova vence", nunca mesclando as duas. **Enquanto houver dois PRs
abertos ao mesmo tempo, este arquivo conflita.**

## O bloco 6, e o que falta dele

**Escopo escolhido: 43 e 44 agora, 45 depois.** O arquivo do desenho traz três telas, e a 45
(encaminhar ao especialista) **é independente das outras duas** — caiu ali por ser "o caso que
passa adiante", e vira bloco próprio sem perder nada.

### O que já está pronto (backend, em `347e681`)

- **`V41__o_animal_que_e_de_todos.sql`** — `animal_sightings`, `group_approvals`,
  `memberships.contribution` e `animals.neutering_scheduled_for`
- `POST /animals/{id}/sightings` — idempotente por pessoa, animal e dia
- `POST /group-approvals`, `GET`, `.../agree`, `.../reject`
- Os três atos executam de verdade: adoção vira convite de titularidade, óbito reusa o fluxo da
  Tela 33 inteiro, remoção desliga a `membership`

### O defeito que o teste encontrou, e que é o achado da sessão

**O `requireEscrita` não enxergava custódia de organização** — só o `requireCustodia` enxergava,
desde a Tela 13. O efeito era mudo e grande: um animal cuja custódia é de uma organização, sem
tutor humano, **não podia receber registro nenhum de quem cuida dele**. O abrigo não lançava peso
no animal que resgatou.

É a mesma linha que faltava no `requireCustodia` e trancava a adoção inteira, agora na outra
metade da guarda. **Vale procurar a terceira**: `alcanca()` também só pergunta por pessoa.

### O frontend, e o que ele decidiu

- **`/colonia`** — a lista com os quatro filtros, "visto por último" e o painel do que qualquer um
  faz contra o que precisa de duas pessoas
- **`/animais/{id}/adotar`** — a Tela 44
- **`GET /group/animals?filtro=`** — a listagem enriquecida, que não existia. Três consultas para
  a lista inteira, e não três por linha

**"Vi hoje" é um botão**, e não link nem menu: cada camada entre o dedo e o registro reduz o
número de marcações — que é a única coisa que faz a coluna do lado significar algo.

**"Sem informação" não é "sumido"**, e o filtro respeita: um gato cadastrado ontem, que ninguém
marcou ainda, não está desaparecido. Contá-lo entre os sumidos mandaria o grupo procurar um animal
que está na praça.

**O passo que o desenho não desenha:** quando alguém concorda com a adoção, quem vai receber o
animal recebe um **convite**, e a custódia passa quando essa pessoa aceita. A tela diz isso antes
do gesto.

### O que NÃO foi feito no bloco 6, e vale saber

- **A Tela 45 (encaminhamento)** — bloco 6b, independente
- **Não há tela de cadastrar animal no grupo nem de convidar quem cuida.** Os dois botões existem
  no desenho da 43 e não foram construídos: cadastrar reusa `/animais/novo`, e convidar reusa o
  fluxo de membro de organização — mas nenhum dos dois está ligado a partir daqui
- **A contagem por aba não aparece** ("Todos · 14", "Falta castrar · 5"). O servidor devolve a
  lista filtrada, e os números exigiriam ou quatro chamadas ou um endpoint de contagem

## O bloco 5, e as decisões que ele tomou

**Telas 33 e 34.** O desenho é `Telas Petfy - Fim, reencontro e conta.dc.html`, e ele cobre
**quatro** telas, não duas: **35 e 36 (a busca e a conta) existem lá** e ficaram de fora do bloco
por escolha de escopo — o índice antigo dava as duas como nunca desenhadas, e isso estava errado.

### Tela 33 — encerrar a linha do tempo

O produto não tinha isto, e **o que ele tinha no lugar era pior que nada**: o
`DELETE /animals/{id}`, que apaga a carteira, o prontuário, o peso, os anexos e os arquivos no
disco. Quem perdia o animal escolhia entre destruir sete anos de registro e conviver com um
cadastro que continua cobrando vacina.

- `V40__o_fim_da_linha.sql` — `animal_deaths`, com o **animal como chave primária**: morre-se
  uma vez, e a PK garante isso sem CHECK nenhum. Tabela, e não cinco colunas anuláveis em
  `animals` — colunas soltas não prometem que, se há data, há também quem a registrou
- `TimelineEventType.OBITO`, na view, `is_health_data = false`
- `POST` e `GET /animals/{id}/death`, e `GET /animals/former`
- Encerra a custódia com `OBITO` **sem sucessor**, encerra as matrículas vivas, põe o fim na
  linha do tempo e avisa quem cuidava

**A GUARDA MUDOU, e é a alteração mais sensível do bloco.** Quem respondia até o óbito continua
**lendo** a vida do animal, para sempre. Sem isso a promessa da tela — *"os sete anos de vida
dele continuam aqui, inteiros"* — se quebraria no dia seguinte ao encerramento: a custódia
acabou, óbito não tem sucessor que conceda nada, e o guard responderia 404.

Não virou concessão gravada porque **toda concessão tem quem a concedeu e quem pode revogá-la**,
e aqui não há nem um nem outro. Uma linha em `grants` "concedida por ninguém" apareceria na tela
de quem cuida como se alguém tivesse autorizado.

### A premissa que estava errada: HÁ canal de aviso

**A versão anterior deste arquivo dizia "não há canal de aviso".** Isso vale para aviso in-app,
mas **o e-mail funciona desde o P4** — `ResendNotifier`, `AsyncNotificationDispatcher`, e o
`OrganizationActivityNotifier` já manda e-mail a cada vacina e atendimento registrado.

A decisão do aviso na Tela 33 foi tomada duas vezes por causa disso: primeiro "entregar o efeito
e não prometer o aviso", depois refeita para **avisar de verdade**, como o desenho escreveu.
Faltava só o destinatário de organização, que a `findVigentesDeOrganizacoesNoAnimal` resolveu.

**Vale reler essa linha antes de decidir qualquer coisa nas Telas 03, 23 e 24.**

### Tela 34 — achei um animal na rua

- `POST /found`, pública e sem conta. **POST e não GET por uma razão concreta:** o
  `RateLimitFilter` só intercepta POST, e uma rota pública que devolve nome e telefone a partir
  de um número não pode ficar sem trava de varredura. Grupo próprio de limite, por IP
- DTO próprio, e não o do `/share/{token}`: lá o tutor escolheu os escopos, aqui não houve
  escolha de ninguém e o conjunto é sempre o mesmo
- **Busca parcial não existe** — "9810" acharia todo animal de uma fabricante de chip
- `AccessActorType.MICROCHIP_SEARCH` **sem ator**, pela promessa da própria tela
- Animal com a linha do tempo encerrada responde como **número inexistente**: mostrar o cartão
  produziria uma ligação para quem acabou de perder o animal

**Dois pedaços do desenho que não têm de onde sair:** *"avisamos (...) e **por onde**"* — o
produto não sabe onde a pessoa está, e pedir a região a quem está na calçada com um animal é
atrito no pior momento — e o **nome de cada organização** avisada na Tela 33.

### O duplicado, e qual cadastro responde

O microchip aparece em mais de um cadastro com frequência — é a razão de a Tela 32 existir. O
critério de escolha é o mesmo que ela já usa: **quem tem alguém respondendo por ele agora vem
primeiro; entre iguais, o mais antigo.** "O primeiro que vier" daria a quem está com o animal na
mão o cadastro que a clínica abriu às 22h com três campos preenchidos.

### A dívida do texto duplo-codificado foi paga

O `creche.tsx` **e** o `animais.$animalId.tsx`. No segundo havia **quatro ocorrências em texto
visível**: o separador entre nome e espécie aparecia como `Â·` em tela desde a entrega da Tela 02,
e ninguém tinha visto porque **nenhuma tela foi conferida no navegador em nenhuma entrega**.

## A fila, e onde ela está

| # | Bloco | Estado |
|---|---|---|
| 1 | A faixa sai de dentro do cabeçalho sticky | **Fechado** — PR #54 |
| 2 | Núcleo clínico — Telas 30, 31, 32 | **Fechado e mergeado** (PRs #54 e #55) |
| 3 | Por onde o valor entra — 40, 41, 42 | **Fechado e mergeado** — PR #56 |
| 4 | Custo do cuidado — 37, 38, 39 | **Fechado e mergeado** — PR #57 |
| 5 | Fim e reencontro — 33, 34 | **Fechado e mergeado** — PR #59 |
| 6 | Animal comunitário — 43, 44 | **Fechado** — PR #60 |
| 6b | **Encaminhamento — Tela 45** | pendente, e independente das outras duas |
| 7 | Apadrinhar, hospedar, o ano — 46, 47, 48 | pendente |
| 8 | Tela 18 — petshop | pendente (a especificação sempre existiu) |
| 9 | **A busca e a conta — 35, 36** | pendente, e o desenho existe |

**A 35 é quase só frontend:** as consultas de busca por microchip já existem no
`CustodyRepository` e no `GrantRepository`. **A 36 é o maior pedaço** — sessões abertas,
exportação de dados e encerramento de conta, e mexe em autenticação.

## O que continua faltando, e por quê

- **Não há canal de aviso IN-APP.** O e-mail existe e funciona. A Tela 03 lista quem está
  vencendo e não avisa ninguém, a Tela 23 não avisa quem registrou, e a Tela 24 continua
  inconstruível — mas agora **por decisão de produto, e não por falta de canal.**
- **A busca autenticada não tem rota** (Tela 35), e **RGA não é campo de busca**.
- **Capacidade de organização não se declara** na criação (Tela 15), e **espécie é só CANINA e
  FELINA**.
- **O escopo do evento de união é um compromisso**, e agora o do óbito também: quem tem concessão
  só de `OBSERVACOES` ou só de `PESO` não vê nenhum dos dois.
- **A edição de vacina não passa pela guarda de dose duplicada.**
- **Nenhuma tela foi conferida no navegador**, e isso já custou o defeito do `Â·` acima.

## Armadilhas desta máquina

- **`vite build` suja o `arvore-de-rotas.gen.ts`**: `git status` mostra modificado, `git diff` vem
  vazio (é `core.autocrlf` sem `.gitattributes`), e **o `git pull` aborta**. `git checkout --` no
  arquivo destrava; ele é gerado. Conferir `git log` DEPOIS do pull.
- **O Rancher Desktop precisa estar aberto** — sem ele não há Docker, e sem Docker o
  `OpenApiContractTest` e as guardas de cobertura são **pulados**. Conferir `Skipped: 0`.
  **Nesta sessão isso travou o frontend por meia hora:** sem container não há contrato, e sem
  contrato não há tipos gerados.
- **O container `petfy-pg-sessao` já existe**: `docker start petfy-pg-sessao`, e não `docker run`.
- **`-Dtest=` com vírgula e `-D` com ponto precisam de aspas no PowerShell**, e
  `Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir.
- **Here-string do PowerShell quebra em aspas duplas**: escrever a mensagem de commit num arquivo
  e usar `git commit -F`.
- **O `cd` do Bash e o do PowerShell se contaminam** — usar caminho absoluto nos dois, sempre.
- **`python` não existe nesta máquina** (o alias abre a Microsoft Store). Para manipular arquivo
  em lote, PowerShell com `[System.IO.File]::ReadAllText`.
- **A conta ativa do `gh` é a de trabalho**, e o erro mente: *"Could not resolve to a Repository"*.
  `gh auth switch --user UlyssesLopes` antes de qualquer `gh`.
- **Commit numa branch cujo PR já mergeou não roda check nenhum**, e nada avisa: o `git push`
  funciona e o `gh pr view` mostra o `headRefOid` antigo para sempre. Bloco novo, branch nova.
- **O merge de PR é comando do Ulysses, sempre.**
- **ICU não aceita chave vazia em `select`.** Quando o texto muda conforme um campo existir ou
  não, são **duas mensagens** concatenadas.

## Primeiro passo da próxima sessão

**Decidir o PR #60**, e depois sincronizar a `main` e ramificar dela — branch nova é regra desde a
armadilha do bloco 4.

O próximo trabalho é o **bloco 6b — Tela 45, encaminhar ao especialista**, cujo desenho já está
lido (mesmo arquivo do bloco 6). Ele é o menor dos que sobraram e reusa concessão com prazo, que
já existe: *"o acesso do Roberto vale 90 dias e depois fecha sozinho"*. A regra central dele já
está escrita no desenho — **encaminhar é indicar o caminho; conceder acesso continua sendo do
tutor**.

**Ler o `.dc.html` inteiro antes de planejar, e contar as telas.** O nome do arquivo cobre mais
do que o bloco declarado — foi assim que as Telas 35 e 36 apareceram.

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
