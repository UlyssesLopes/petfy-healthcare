# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-12, no fim de uma sessão que fechou o **bloco 5**.

## Onde o trabalho está agora

**O bloco 5 está mergeado** — PR #59, com CI verde nos dois workflows. O #58, que era só
documentação, entrou antes dele (`520e5f2`). **Não há branch de feature aberta.**

**922 casos no backend, 51 no front, `Skipped: 0`.** Contrato regenerado, nenhuma operação
renomeada. `npm run build` limpo.

**Duas telas novas no ar:** 33 (encerrar a linha do tempo) e 34 (achei um animal na rua).

**O conflito que os dois PRs produziram, e que vai voltar:** os dois reescreveram esta página. A
resolução foi ficar com esta versão, que já continha o conteúdo do #58. **Enquanto houver dois PRs
abertos ao mesmo tempo, este arquivo conflita** — e a saída é resolver por "a versão mais nova
vence", nunca mesclando as duas.

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
| 6 | Animal comunitário — 43, 44, 45 | pendente |
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

**Sincronizar a `main` e ramificar dela** — não há nada pendente, e branch nova é regra desde a
armadilha do bloco 4.

Depois, **o bloco 6 — Animal comunitário, Telas 43, 44 e 45**, cujo desenho é
`design/IdentidadeVisual/Telas Petfy - Animal comunitário e encaminhamento.dc.html` e **ainda não
foi lido**. Ou o bloco 9 (35 e 36), se a preferência for fechar o arquivo do bloco 5 inteiro.

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
