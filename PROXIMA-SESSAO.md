# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-12, no fim de uma sessão que fechou o **bloco 6b inteiro** — backend, frontend
> e a tela que o desenho não desenha.

## Onde o trabalho está agora

**O bloco 6 está mergeado** — PR #60. A `main` estava em `08e7ba6` quando esta branch saiu dela.

**O BLOCO 6b ESTÁ FECHADO — Tela 45, na branch `feat/encaminhamento`**, esperando seu comando.

**947 casos no backend, 51 no front, `Skipped: 0`.** A suíte passa também em
`-Dsurefire.runOrder=reversealphabetical`.

**Quatro telas no ar:** 33, 34, 43, 44 — e agora a 45, mais a tela de decisão que ela exigiu.

## O bloco 6b, e o que ele decidiu

**A regra que governa a tela inteira**, e ela está no desenho: *"Marcelo Dias precisa autorizar.
**Encaminhar é você indicando o caminho; conceder acesso continua sendo dele**, como sempre foi. O
acesso do Roberto vale 90 dias e depois fecha sozinho."*

**O encaminhamento não concede nada.** É um pedido; o que o aceite produz é um `Grant` comum, com
`grantedBy` sendo o tutor e prazo de 90 dias. **A guarda não aprendeu nada sobre encaminhamento, e
nem precisa** — um segundo caminho de alcance seria consultado em toda leitura para sempre.

### O que existe agora (backend)

- **`V42__o_caso_que_passa_adiante.sql`** — `referrals`, `referral_scopes` e
  `professional_credentials.specialty`
- `GET /animals/{id}/referral-options` — as caixas do "o que vai junto", com o número de cada uma
- `GET /animals/{id}/referral-candidates?busca=` — "Buscar outro profissional"
- `POST /animals/{id}/referrals`, `GET` — o pedido, e o que a clínica acompanha
- `GET /referrals/pending`, `GET /referrals/received`
- `POST /referrals/{id}/authorize`, `.../reject`
- `ReferralNotifier` — três momentos, não quatro

### As seis decisões que valem saber

**1. "O que vai junto" é ESCOPO, e não evento.** O desenho seleciona *eventos* — "o raio-X de 2023",
"4 observações da creche entre 02/06 e 05/08" — e a concessão só sabe conceder por *tipo*. O recorte
por evento também mentiria com facilidade: as quatro observações escolhidas hoje não dizem nada sobre
a quinta, escrita amanhã pela mesma creche sobre o mesmo problema, e o especialista tratando o caso
não a veria. **A tela assume o compromisso em voz alta** em vez de escondê-lo.

**2. As contagens das caixas saem da LINHA DO TEMPO** (`TimelineRepository.contagemPorTipo`), e não
de contar cada tabela de origem. Seriam seis consultas para o mesmo, com um risco que a agregada não
tem: o número na caixa viria de uma fonte e o que o especialista abre depois vem da linha do tempo
mascarada por escopo. **Contando ali, a promessa e a entrega são a mesma consulta.**

**3. A união de escopos no aceite — e é o achado da sessão.** Se o especialista já é co-tutor com
acesso permanente, gravar prazo e escopos do encaminhamento por cima **ENCOLHERIA** o acesso dele: o
tutor autorizaria a ver mais e o efeito seria ver menos, e ninguém descobriria antes de o co-tutor
abrir a carteira e não achar a vacina. Então os escopos **somam**, e o prazo mais generoso vence —
**nulo vence de tudo**. Há caso de teste para isso.

**4. A concessão nasce EDITOR, e não VIEWER.** *"O especialista que registra ali passa a devolver o
retorno para a clínica que encaminhou."* Um encaminhamento só de leitura entregaria o caso e não
deixaria o especialista escrever o que concluiu — e o retorno voltaria a ser telefone.

**5. A busca de profissional vive DEBAIXO DO ANIMAL**, e não numa rota de gente. Duas razões, e a
segunda decidiu: uma rota que devolve pessoas por nome parcial é varredura de dado pessoal — a lição
do `POST /found` no bloco 5. Debaixo do animal, quem varre precisa antes ter escrita em um animal, e
a trava passa a ser a guarda que já protege tudo, em vez de um limite por IP.

**6. Quem responde pelo animal encaminhando produz pedido JÁ AUTORIZADO**, com a concessão na hora.
Pedir autorização a si mesmo seria teatro — dois botões para o mesmo efeito.

### A tela que o desenho não desenha, e por que ela entrou no bloco

**`/encaminhamentos`** — o tutor recebe e decide. Sem ela a metade desenhada não funciona: **o
encaminhamento é o único fluxo do produto em que a ação de uma pessoa fica parada esperando outra que
não está na tela.** A clínica clica e sai do consultório; o tutor recebe um e-mail e precisa de um
lugar para onde ir.

Duas listas na mesma página, e não duas rotas: a veterinária que é tutora de um animal decide sobre
ele **e** recebe casos de colegas, e duas rotas fariam essa pessoa descobrir a segunda por acidente.

### Três coisas que a tela diz DIFERENTE do desenho, de propósito

- **Não escreve o nome de quem autoriza.** O desenho diz "Marcelo Dias precisa autorizar"; o nome de
  quem responde pelo animal vive no escopo `CONTATO`, e quem encaminha em geral tem só o clínico.
  Escrever o nome exigiria pedir ao servidor um dado que aquela concessão não alcança.
- **O especialista não é avisado do pendente**, e vê na caixa de entrada dele **sem o nome do
  animal**. Ele precisa saber que há um caso esperando — pode ligar para a clínica —, e o tutor não
  autorizou nada ainda.
- **`CONTATO` aparece marcado como dado pessoal.** Não é pedaço do prontuário, é o telefone do tutor:
  escondê-lo dentro de um recorte clínico entregaria dado pessoal sem que a diferença aparecesse.

### O que a exclusão de conta e o purger aprenderam

- **`referrals` sai ANTES de `grants`**, nos dois caminhos: ele aponta para o animal **e** para a
  concessão que o aceite produziu. Ordem inversa = 500 em todo animal já encaminhado.
- **`referrals` FICA na união de cadastros** (Tela 32), porque `grants` fica: mover um sem o outro
  deixaria um pedido autorizado apontando para acesso de outro cadastro.
- **A exclusão de conta apaga o encaminhamento**, e não desassocia como faz com o anexo: o anexo
  pertence ao animal e o laudo vale sem quem o subiu; um encaminhamento é conversa entre duas pessoas
  nomeadas, e sem uma das duas a linha não diz nada.

## A verificação contra o app rodando, e o que dela ficou de fora

**O fluxo inteiro foi exercitado por HTTP, contra Postgres real** — não só por teste. A `V42` aplicou
sobre um banco que já tinha dado até a `V41`, e os nove passos responderam como a tela promete:

| Passo | Resultado |
|---|---|
| Ana busca `orto` | Roberto Lins · ortopedia · CRMV-SP 54321 · Clínica Anhangabaú |
| Ana busca `or` | `[]` — duas letras não é buscar |
| Ana encaminha | `PENDENTE`, 90 dias, **sem concessão e sem data de fim** |
| Ana tenta autorizar o próprio pedido | **403** — ela tem escrita e não custódia |
| A lista do Marcelo | o pedido, com `canDecide: true` |
| Marcelo autoriza | `AUTORIZADO`, expira `2026-11-10` (90 dias exatos) |
| A caixa do Roberto, antes | `PENDENTE`, **`animalId` e `animalName` nulos** |
| A caixa do Roberto, depois | `AUTORIZADO`, "Code", com a data |
| `GET /animals/{id}` como Roberto | **200 — e não abria antes** |
| Autorizar de novo | **409** |

O `ReferralNotifier` disparou nos três momentos e suprimiu por e-mail não confirmado, com log por
destinatário. Os únicos `ERROR` no log são as três exceções de negócio provocadas de propósito.

**O que ficou de fora: as telas no navegador.** A extensão do Chrome não está conectada, então a
conferência visual continua devendo — é a mesma dívida que custou o `Â·`. O `npx vite build` e o
`tsc` passam, e o dev server serve os três módulos novos sem erro de transformação, mas isso é
compilação, não renderização.

### Como chegar nas telas (o app pode estar de pé)

```
docker start petfy-pg-sessao
$env:DB_PORT="5433"; mvn spring-boot:run "-Dspring-boot.run.profiles=local"
cd web; npm run dev
```

Contas semeadas, senha `Petfy!2026` nas três:

- `marcelo@petfy.test` — responde pelo Code, e é quem autoriza → `/encaminhamentos`
- `ana@petfy.test` — concessão EDITOR no Code, CRMV-SP 12345 → `/animais/e7bf40cc-adbc-4617-be82-3b35f1b5ef45/encaminhar`
- `roberto@petfy.test` — ortopedia, CRMV-SP 54321, Clínica Anhangabaú → `/encaminhamentos`

O animal `Code` é `e7bf40cc-adbc-4617-be82-3b35f1b5ef45`, com 4 eventos: dois pesos (7,5 → 8,6 kg),
duas observações e uma condição crônica. **A concessão da Ana e a especialidade do Roberto foram
inseridas por SQL** — não há tela para declarar especialidade, e conceder acesso a uma *pessoa* só
existe pelo convite de co-tutor.

## O DEFEITO PRÉ-EXISTENTE QUE ESTA SESSÃO MEDIU E NÃO CORRIGIU

**O `Â·` sobreviveu em duas telas, e em TEXTO VISÍVEL.** O bloco 5 pagou essa dívida no `creche.tsx`
e no `animais.$animalId.tsx` e parou ali. Restam:

| Arquivo | Linha | Onde |
|---|---|---|
| `web/src/rotas/pacientes.tsx` | 211, 212, 247 | **texto visível** (Tela 03) |
| `web/src/rotas/organizacoes.$organizationId.equipe.tsx` | 203 | **texto visível** (Tela 16) |
| `web/src/rotas/pacientes.tsx` | 16 | comentário |
| `web/src/rotas/organizacoes.$organizationId.equipe.tsx` | 19 | comentário |

**Quatro ocorrências em tela, e a correção é trocar `Â·` por `·`.** Não foi feita porque pertence às
Telas 03 e 16, e não a este bloco — a decisão é sua.

**E o `PersonServiceImpl` não apaga `group_approvals`.** A tabela nasceu no bloco 6 apontando para
`persons` em quatro colunas, e a exclusão de conta não a limpa: quem pediu uma concordância num animal
que **sobrevive** à exclusão faz o `DELETE /persons/me` responder 500. Não há guarda de schema para
FK que aponta para `persons` — a guarda existe só para `animals`. **Medido, não corrigido.**

## A armadilha nova desta sessão

**`Get-Content -Raw | Set-Content -Encoding utf8` DUPLO-CODIFICA o arquivo** e quebra a compilação de
um jeito que engana: o erro é `class, interface, enum, or record expected` na linha 6, como se o
`package` estivesse errado. É a mesma família do `Â·`. **Para editar arquivo em lote, usar a
ferramenta de escrita, nunca o pipe do PowerShell.**

## A fila, e onde ela está

| # | Bloco | Estado |
|---|---|---|
| 1 | A faixa sai de dentro do cabeçalho sticky | **Fechado** — PR #54 |
| 2 | Núcleo clínico — Telas 30, 31, 32 | **Fechado e mergeado** (PRs #54 e #55) |
| 3 | Por onde o valor entra — 40, 41, 42 | **Fechado e mergeado** — PR #56 |
| 4 | Custo do cuidado — 37, 38, 39 | **Fechado e mergeado** — PR #57 |
| 5 | Fim e reencontro — 33, 34 | **Fechado e mergeado** — PR #59 |
| 6 | Animal comunitário — 43, 44 | **Fechado e mergeado** — PR #60 |
| 6b | Encaminhamento — Tela 45 | **Fechado** — `feat/encaminhamento` |
| 7 | **Apadrinhar, hospedar, o ano — 46, 47, 48** | pendente, e é o próximo |
| 8 | Tela 18 — petshop | pendente (a especificação sempre existiu) |
| 9 | A busca e a conta — 35, 36 | pendente, e o desenho existe |

**Antes de planejar o bloco 7, ler o `.dc.html` inteiro e contar as telas.** O arquivo é
`Telas Petfy - Apadrinhar, hospedar, o ano.dc.html`, e o nome de arquivo já enganou a fila duas vezes
— foi assim que as Telas 35 e 36 apareceram.

## O que continua faltando, e por quê

- **Não há canal de aviso IN-APP.** O e-mail existe e funciona desde o P4. A Tela 03 lista quem está
  vencendo e não avisa ninguém, a Tela 23 não avisa quem registrou, e a Tela 24 continua
  inconstruível — mas **por decisão de produto, e não por falta de canal.**
- **A busca autenticada não tem rota** (Tela 35), e **RGA não é campo de busca**.
- **Capacidade de organização não se declara** na criação (Tela 15), e **espécie é só CANINA e
  FELINA**.
- **A especialidade é texto livre e ninguém a preenche hoje.** Não há tela para declará-la: toda
  credencial existente ficou nula na V42, e a busca da Tela 45 só a mostra para quem a gravar por
  outro caminho. **É a dívida que este bloco criou de propósito.**
- **A contagem por aba da Tela 43 não aparece** ("Todos · 14"): o servidor devolve a lista filtrada.
- **`alcanca()` do `AnimalAccessGuard` só pergunta por pessoa** — é a terceira metade do defeito que
  o bloco 6 encontrou no `requireEscrita`. Ainda não foi olhada.
- **O escopo do evento de união e do óbito é um compromisso**: quem tem concessão só de `OBSERVACOES`
  ou só de `PESO` não vê nenhum dos dois.
- **A edição de vacina não passa pela guarda de dose duplicada.**
- **Nenhuma tela foi conferida no navegador**, e isso já custou o `Â·` — que continua em duas telas.

## Armadilhas desta máquina

- **`vite build` suja o `arvore-de-rotas.gen.ts`**: `git status` mostra modificado, `git diff` vem
  vazio (é `core.autocrlf` sem `.gitattributes`), e **o `git pull` aborta**. `git checkout --` no
  arquivo destrava; ele é gerado. Conferir `git log` DEPOIS do pull. **Nesta sessão a mudança dele
  foi legítima** — duas rotas novas —, então ele entrou no commit.
- **Rota nova exige regenerar a árvore antes do `tsc`**: o `npm run typecheck` reclama que a rota
  "não é assinável a `keyof FileRoutesByPath`", e a saída é `npx vite build` (o `npm run build` roda
  o `tsc` primeiro e falha antes de gerar).
- **O Rancher Desktop precisa estar aberto** — sem ele não há Docker, e sem Docker o
  `OpenApiContractTest` e as guardas de cobertura são **pulados**. Conferir `Skipped: 0`.
- **O container `petfy-pg-sessao` já existe**: `docker start petfy-pg-sessao`, e não `docker run`.
- **O banco de teste NÃO é limpo entre casos.** Uma asserção de "um único resultado" numa busca passa
  no primeiro caso e falha nos catorze seguintes, porque cada `@BeforeEach` cria gente nova. Recortar
  pelo id da rodada, e sortear o que for único no schema — o número de CRMV é.
- **Teste de container não toca coleção preguiçosa fora de transação**: `Grant.scopes` estoura
  `LazyInitializationException`. Ler pelo `JdbcTemplate` afirma o que ficou GRAVADO, que é o que um
  teste de container deveria afirmar.
- **`-Dtest=` com vírgula e `-D` com ponto precisam de aspas no PowerShell**, e
  `Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir. **E `Select-String` num log
  de Maven casa o SQL do Hibernate**: redirecionar para arquivo e filtrar depois.
- **Here-string do PowerShell quebra em aspas duplas**: escrever a mensagem de commit num arquivo
  e usar `git commit -F`.
- **`Get-Content -Raw | Set-Content` duplo-codifica** — ver a armadilha nova, acima.
- **O `cd` do Bash e o do PowerShell se contaminam** — usar caminho absoluto nos dois, sempre.
- **`python` não existe nesta máquina** (o alias abre a Microsoft Store).
- **A conta ativa do `gh` é a de trabalho**, e o erro mente: *"Could not resolve to a Repository"*.
  `gh auth switch --user UlyssesLopes` antes de qualquer `gh`.
- **Commit numa branch cujo PR já mergeou não roda check nenhum**, e nada avisa. Bloco novo, branch
  nova.
- **O merge de PR é comando do Ulysses, sempre.**
- **ICU não aceita chave vazia em `select`.** Quando o texto muda conforme um campo existir ou
  não, são **duas mensagens** concatenadas — o `encaminhar.caixa.desde` é o exemplo novo.
- **Código de erro novo quebra o front**, e é de propósito: o `erroDaApi.test.ts` cobra tradução em
  `pt-BR` para cada valor do `ErrorMessageEnum`. Sete entraram nesta sessão.

## Primeiro passo da próxima sessão

**Decidir o PR do bloco 6b**, e depois sincronizar a `main` e ramificar dela — branch nova é regra
desde a armadilha do bloco 4.

Depois, escolher entre três coisas, e as três são pequenas ao lado de um bloco:

1. **O `Â·` em texto visível** (quatro trocas, duas telas)
2. **`group_approvals` na exclusão de conta** (uma linha e um teste)
3. **O bloco 7**, que é o trabalho grande

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
cd web && npx vite build                             # regenera a arvore de rotas
mvn test "-Dsurefire.runOrder=reversealphabetical"   # caça asserção dependente de ordem
```

Os três primeiros entram no **mesmo commit** da mudança que os causou.
