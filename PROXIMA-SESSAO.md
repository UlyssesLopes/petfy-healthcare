# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-10, no fim da sessão que fechou a dívida de backend das telas.

## Onde o trabalho está

**Branch `feat/creche-turma-e-dia`, 14 commits, PR #50 aberto contra a `main`.**

O CI passou em todos os pushes até o penúltimo commit. **O último commit (`4e448c9`, a Tela
11) foi commitado mas NÃO foi empurrado, e portanto não tem CI.** É o primeiro passo de
amanhã: `git push`, esperar os dois jobs e conferir.

Verde localmente no último commit: **backend 829 casos, 0 falhas, Skipped 0** (contra
Postgres real), **front `tsc` limpo e 43 testes**.

## O que esta sessão fez

Ela pegou a lista de dívidas que a sessão anterior deixou escrita e **fechou as cinco**, cada
uma com backend e tela no mesmo passo.

| # | Dívida | Estado |
|---|---|---|
| 1 | multipart não declarado no contrato | **Fechada** — destrava o passo 3 do onboarding, a foto do animal e a foto da carteirinha |
| 2 | dose duplicada não detectada | **Fechada** — 409/149, e a Tela 14 ganhou o sexto estado |
| 3 | equipe da organização não existia | **Fechada** — listar, função no convite, ajustar e desligar; Tela 16 completa |
| 4 | nenhuma agregação por organização | **Fechada** — situação, última visita e resumo; Tela 03 com as quatro colunas |
| 5 | acesso herdado na transferência | **Fechada** — decisão tomada: o backend obedece ao desenho |

Antes disso, dois `fix:` que o CI e a ordem invertida cobraram (ver abaixo).

## O que a sessão descobriu, e é o que importa daqui

### Duas asserções mediam o BANCO INTEIRO num container compartilhado

O CI reprovou o `SchemaMigrationContainerTest.catalogoDeveVirSemeado`: esperava 11 itens de
catálogo e viu 23. **As duas execuções rodaram os mesmos 797 casos** — a diferença era a
ORDEM. O container do Postgres é compartilhado entre as classes e ninguém limpa a tabela; o
`CrecheContainerTest` cria um item de catálogo por caso, e são 12.

Rodar a suíte com **`-Dsurefire.runOrder=reversealphabetical`** achou a segunda antes do CI: o
`UuidQueriesContainerTest.rotinaDeLembretesPegaAsCertas` varre o banco inteiro e recolhia
vacinas de outras classes. **Essa flag vale como ferramenta de rotina** — ela mostra a
fragilidade antes de a próxima classe nova mudar a ordem por acidente.

### O teste que faltava era o da LIGAÇÃO HTTP, e foi ele que achou um 500

O upload de anexo e a leitura de carteirinha tinham serviço testado e armazenamento testado —
e **nenhum cliente conseguia mandar arquivo**, porque `@RequestParam MultipartFile` não vira
`requestBody` no contrato. Os testes de serviço continuariam verdes com a rota inalcançável.

O `MultipartUploadBindingTest`, que entra pelo MockMvc, cobrou também que **requisição sem a
parte do arquivo respondia 500** — erro de cliente voltando como erro de servidor. Virou 400
com código próprio (148).

### O springdoc RENOMEIA operação alheia

Com os métodos da equipe chamados `list` e `changeRole`, o `openapi.json` saiu com `list_1`
virando `list_2` numa rota de OUTRO controller e `changeRole` virando `changeRole_1` no
pet-tutor. O `operationId` vem do nome do método e o desempate é sufixo numérico, **que
depende da ordem de varredura**. Método com nome próprio (`listMembers`) manteve o contrato
estável: 130 inserções e zero remoções.

### Convidar exigia credencial profissional

E trancava a Tela 16 para exatamente quem ela serve: a administradora do abrigo e a da creche
não têm CRMV. Era `requireContext` onde cabia `organizacaoDeclarada` — **a terceira vez que
esse mesmo defeito aparece** (a creche cobrou as outras duas). O convite também saiu do
`professionalAccessManager` na cadeia de filtros.

## O que continua faltando, e por quê

- **Quem já tem conta não consegue aceitar convite de organização.** O único caminho de aceite
  é o `inviteToken` na criação da conta. É a única parte da Tela 16 que ainda depende de
  backend, e a tela diz isso.
- **Não há canal de aviso.** Por isso a Tela 03 lista quem está vencendo mas não avisa
  ninguém, a Tela 23 não avisa quem registrou, e a Tela 24 (avisos) continua inconstruível.
  Um botão que não avisa seria pior que a ausência dele.
- **A edição de vacina não passa pela guarda de dose duplicada** — mudar a data de um registro
  para bater com outro cria a duplicata. Foi decisão consciente: é raro e deixa rastro no
  `VaccineCorrectionLog`, enquanto a gravação dupla é comum e silenciosa.
- **Telas 12, 18 e 25–27 não têm especificação no repositório.** O arquivo com as telas
  numeradas do Claude Design nunca foi versionado — em `design/` só existem o `BRIEFING.md`, o
  `home-tutor.html` e quatro capturas. Sem ele não dá para construir nenhuma delas.
- **Capacidade de organização não se declara** na criação (Tela 15), **espécie é só CANINA e
  FELINA**, e **RGA e tatuagem não têm campo**.

## Armadilhas desta máquina

- **O Docker subiu e ficou de pé a sessão inteira** (`petfy-pg-sessao` no ar). A distro
  `rancher-desktop-data` já esteve `Stopped` em sessões passadas; **não reinicie o Rancher** —
  a máquina tem k8s com argocd no ar.
- **`-Dtest=` com vírgula e `-D` com ponto precisam de aspas no PowerShell**, e
  `Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir (dá 255 com BUILD
  SUCCESS).
- **Reescrever arquivo com `Set-Content -Encoding utf8` corrompe acento** (dupla codificação).
  Use a ferramenta de escrita, ou `[IO.File]::WriteAllText`.
- **O scratchpad da sessão pode não existir**: `Out-File` para um diretório inexistente mata o
  pipe e o comando anterior nem roda. Criar antes.
- **`cd` no Bash persiste entre chamadas** — e o `git add` falha com pathspec se o cwd for
  `web/`.
- **O merge de PR é bloqueado ao agente pelo classificador** — é comando seu, sempre.
- **O gerador de rotas do TanStack** não tem CLI aqui: quem regenera `arvore-de-rotas.gen.ts`
  é `npx vite build`.
- **Nenhuma tela foi conferida no navegador nesta sessão.**

## Primeiro passo de amanhã

```
git push                      # o commit da Tela 11 ainda não subiu
gh pr checks 50               # os dois jobs
```

Depois, se estiver verde, o merge é seu.

## Como subir, e como regenerar

```
docker run -d --name petfy-pg-sessao -e POSTGRES_DB=petfy -e POSTGRES_USER=petfy -e POSTGRES_PASSWORD=petfy -p 5433:5432 postgres:14
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
