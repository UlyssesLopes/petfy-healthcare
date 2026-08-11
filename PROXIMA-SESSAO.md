# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-11, com o backend da Tela 32 fechado e a tela dela por construir.

## Onde o trabalho está agora

**Branch `feat/cadastro-duplicado`, dois commits, sem PR aberto ainda.**

Backend da Tela 32 completo e verde: **855 casos, 0 falhas, `Skipped: 0`** contra Postgres real.
Contrato regenerado — 306 inserções, zero remoções, nenhuma operação renomeada.

**O que falta neste bloco: a tela da 32.** O backend inteiro está de pé e testado.

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
| 2 | Núcleo clínico — Telas 30, 31, 32 | **30 e 31 fechadas** (PR #54); **32: backend fechado, tela pendente** |
| 3 | Por onde o valor entra — 40, 41, 42 | pendente |
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

**A tela da 32**, em `web/src/rotas/`. O backend está pronto e o desenho é
`design/IdentidadeVisual/Telas Petfy - Núcleo clínico.dc.html`, seção "Tela 32".

As cinco rotas: `GET /animals/{id}/duplicates`, `POST /animals/{id}/merge-requests`,
`GET /animals/{id}/merge-requests`, `POST /merge-requests/{id}/accept` e `.../reject`.

O que a tela precisa dizer, e que o backend já entrega: a comparação lado a lado com o tamanho de
cada linha do tempo, "o que acontece se forem unidos", "onde os dois discordam", e — o mais
importante — **"você não pode unir sozinha"**, com o pedido indo para quem responde pelo animal.

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
