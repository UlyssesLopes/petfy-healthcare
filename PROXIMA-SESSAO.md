# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-07, no fim da sessão que fechou o B0 e a parte de backend do bloco 1.

## Onde o trabalho parou

**Passo 5 da Fase 5 — construir.** O B0 fechou e o bloco 1 fechou do lado do backend.
Nenhuma linha de frontend existe ainda: o próximo passo é o scaffold.

| # | O que | Estado |
|---|---|---|
| B0 | CORS por perfil | **Feito** — PR #40, mergeado |
| B0 | Caminho de auth | **Decidido:** adiado, token em memória, com gatilho escrito |
| 1 | Gerador do cliente OpenAPI | **Decidido:** `openapi-typescript`, só tipos |
| 1 | Contrato versionado com guarda | **Feito** — PR #41 |
| 1 | Scaffold, tokens, i18n, rota, camada de dados | **Não iniciado** |
| 2 | Home do tutor | Depende do 1 |

**Suíte:** 759 testes, 0 falhas, 0 pulados. CI verde nos dois PRs.

## O que ler antes de planejar, e nesta ordem

1. **`ROADMAP.md`, "Passo 5 — construir"** — o plano, os blocos, e as duas seções novas
   de fechamento (`B0 — concluído` e `Bloco 1 — o gerador escolhido`).
2. **`DESIGN.md` inteiro**, mais `design/home-tutor.html`. Divergiram? O documento manda.
   A 5.5 agora registra que `Insight` e `Update` são posteriores: **a v1 carrega quatro
   dos cinco objetos**, e a tela reserva a posição da percepção.
3. **`PRODUTO.md` seção 9** — as três superfícies, as duas áreas, e o que a 9.5 cobra.
4. **`contract/openapi.json`** — agora é o contrato, no repositório. Não precisa mais
   chamar o `dev` para lê-lo. O `README.md` **não** é fonte de rota.

## O primeiro passo da próxima sessão

**O scaffold do front.** O que a stack já decidiu, sem reabrir: Vite + React +
TypeScript (SPA), TanStack Query, TanStack Router, TanStack Table, Tailwind com tokens
próprios, componente headless (Radix ou React Aria), i18n desde a primeira tela.

O que precisa nascer junto, e é o que evita retrabalho:

- **Os tokens saem das seções 3 e 4 do `DESIGN.md`** — a paleta com contraste já medido
  nos dois temas, a escala tipográfica, o raio escalonado, a base de 4 px. Nada de
  inventar cor: nenhuma entra sem o número.
- **A camada de dados é única, e nenhuma tela chama HTTP.** É a regra que mantém o BFF
  possível. Dentro dela mora a sessão, com um `ensureFresh()` que hoje desloga.
- **O token vive em memória.** `localStorage` está fora — é a exposição a XSS que a
  decisão recusa. Login a cada recarga é o custo aceito, e o gatilho do refresh está no
  `ROADMAP.md`.
- **i18n com a tabela de código de erro.** As mensagens da API estão em inglês no
  `ErrorMessageEnum`, mas têm código numérico: o front mapeia por código e nunca exibe a
  mensagem do servidor.

## Duas decisões pequenas que ficaram abertas

Nenhuma trava o começo, mas as duas ficam mais caras depois:

1. **O nome da pasta do front.** `web/`, `front/` ou `client/`.
2. **O filtro por caminho no CI entra junto do scaffold, ou depois da primeira tela?**
   Hoje qualquer push roda os 759 testes Java, e mudança de CSS vai disparar isso.

## Armadilhas desta máquina, confirmadas nesta sessão

- **Testcontainers devolve `false` com o Docker no ar.** Aconteceu **duas vezes
  seguidas** e passou na terceira, sem mexer em nada. Tente de novo antes de
  investigar, e **nunca** use `-Dpetfy.allow-skipping-container-tests=true` para
  declarar algo verificado.
- **Rodar com `-Dtest=` desliga a rede de segurança.** O `ContainerTestsHabilitadosTest`
  falha quando os testes de container são pulados — mas se a seleção o deixar de fora,
  o build fica verde com zero validação. Foi o que aconteceu aqui.
- **PowerShell quebra `-D` com ponto.** `mvn -Dpetfy.openapi.update=true` vira lixo;
  use aspas: `mvn "-Dpetfy.openapi.update=true"`.
- **`Select-Object -First N` corta o pipeline** e devolve exit 255 mesmo com o Maven
  bem-sucedido. Use `Out-String` no fim.
- **`Set-Content -Encoding utf8` escreve BOM** no PowerShell 5.1, e o `javac` recusa com
  `illegal character: ﻿`. Use `UTF8Encoding($false)`.
- **O CI só dispara em `pull_request` para `main`.** Commit numa branch cujo PR já foi
  mergeado não roda nada: abra um PR novo, senão a suíte local verde é tudo que existe.

## Como regenerar o contrato

Quando um controller mudar de forma — e só então:

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
```

O `contract/openapi.json` entra no **mesmo commit** da mudança, para a revisão ver o
delta de contrato ao lado do código que o causou.
