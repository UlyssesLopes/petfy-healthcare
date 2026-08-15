# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-14, no fim da sessão que **fechou a tela de login inteira** e descobriu,
> no caminho, que um recurso do produto estava morto dos dois lados.

## Onde o trabalho está agora

**1075 casos no backend, 51 no front, `Skipped: 0`.** Migrations até a **V52**. A `main` está em
`8e3a755`, e **não há PR aberto**.

| PR | Conteúdo |
|---|---|
| **#70** | Continuar conectado vira escolha (V52), a tela do cartão `/cartao/{token}`, o `/share` fora do 500, o login por link removido |
| **#71** | O aviso de entrada em aparelho novo |
| **#72** | `/termos` e `/privacidade`, o rodapé que para de fingir, a saída do `/comecar` |

## COMECE POR AQUI: a próxima tela

O trabalho agora é **tela por tela, uma fechada por vez** — pedido do Ulysses, 2026-08-14:

> *"vamos ir tela a tela, começando ela e terminando ela completamente, com todas as
> funcionalidades e pendências que deve ter."*

A **Tela 28/29 (a porta)** está fechada. A próxima é dele. O método que funcionou:

1. Ler o `.dc.html` **inteiro** e contar as telas e os **estados** antes de planejar.
2. Para cada "ainda não" na tela, descobrir de qual dos três tipos ele é (abaixo).
3. Levantar o que falta, decidir com ele, e só então escrever.

## A lição desta sessão

**"Ainda não" tem três significados, e só um merece ficar na tela.** A porta exibia três, e
eram três coisas diferentes:

- **Texto velho que ninguém apagou.** *"Ainda não guardamos a sessão entre visitas"* era falso
  desde a V51 — o cookie já durava 30 dias e o login sempre o gravava.
- **Recurso morto que ninguém testou.** O cartão de emergência não abria porque **não havia tela**
  no front *e* porque o `/share/{token}` respondia **500 para qualquer token válido, desde
  sempre**.
- **Limitação verdadeira.** O login por link não existe, e sem `petfy.app.base-url` nem seria
  link. Esse saiu da tela e virou item de roadmap.

**Antes de aceitar um "ainda não", conferir qual dos três é.** Vale para todas as telas que faltam.

## O defeito que ninguém via, e por quê

`AnimalShareServiceImpl.viewSharedCard` **nunca foi `@Transactional`**. `Grant.scopes` é lazy, e a
primeira pergunta "este link alcança a carteira?" estourava com `LazyInitializationException`.

**Por que a suíte não pegava:** os testes de unidade montam o `Grant` com mock, e mock não tem
sessão do Hibernate para perder. E **nenhuma tela chamava a rota**, então o app de pé também não
denunciava. O `CartaoCompartilhadoContainerTest` cobre agora — e foi **conferido que ele pega**:
removendo o `@Transactional` de propósito, 5 dos 6 casos quebram com o erro exato de produção.

**Toda rota pública sem tela é candidata ao mesmo defeito.** Vale varrer as outras.

## O QUE FICOU DEVENDO

### 1. O texto jurídico de verdade — e é do Ulysses

`/termos` e `/privacidade` existem e descrevem o que o produto faz, conferido contra o código.
Mas **sete seções estão marcadas como pendentes** e aparecem na tela dizendo o que falta:

- privacidade: retenção por tipo de dado, identificação do controlador, canal do encarregado,
  sub-operadores com país de processamento;
- termos: preço, suspensão de conta, lei aplicável e foro.

Enquanto estiverem marcadas, **o aceite do cadastro aponta para um documento que se declara
incompleto**. É melhor que antes — antes não havia documento nenhum — mas não fecha a base legal.
O texto mora em `web/src/conteudo/documentos.ts`, fora do `pt-BR.ts` de propósito.

### 2. A conferência no navegador continua parcial

Conferidas com Chrome headless nesta sessão: `/privacidade`, `/termos`, `/criar-conta`.
**O `/comecar` não foi visto rodando** — exige sessão e o headless cai na porta. O botão
"Fazer isso depois" está no código e no typecheck, e não em uma tela que alguém olhou.

**Telas novas ou tocadas e nunca abertas:** `/avisos`, `/convites`, `/animais/{id}/registrar`,
`/conta`, `/animais/{id}/compra`, `/animais/{id}/quem-cuida`, `/animais/novo`,
`/organizacoes/{id}/equipe`.

### 3. Uma duplicação criada de propósito

`RotuloDoAparelho` (Java) é gêmeo de `apelidoDoAparelho` (`web/src/dados/aparelhos.ts`): o e-mail
sai do servidor e não pode chamar o do navegador. Os dois têm testes com os mesmos casos. **A
forma de matar isso** é o backend expor o rótulo no DTO e o front parar de calcular — não foi
feito porque muda o contrato e a Tela 36, que é outra tela.

### 4. O que o rodapé prometia e não tinha

Os doze itens eram `<div>`: nenhum clicava. Termos e privacidade viraram links; **o resto saiu** e
está no `ROADMAP.md` — as quatro institucionais, as quatro de ajuda, e o **canal do encarregado de
dados (LGPD)**, que é exigência legal. `Quem lê o registro` e `Levar meus dados embora` saíram por
outro motivo: dependem de tela, e não de texto.

### 5. O banco local está sujo

As provas desta sessão deixaram quatro contas de teste (`teste-conectado@`, `cartao-teste@`,
`cartao2@`, `aparelho-novo@petfy.test`) e dois animais "Code". O Ulysses pediu banco limpo para
percorrer as telas do zero — **zerar antes de retomar**, se for continuar a conferência.

### 6. A branch `docs/o-estado-para-a-proxima-sessao`

O commit `bab7594` continua sozinho lá, sem PR, e **o conteúdo dele foi substituído por este
arquivo**. Pode ser descartada.

## Armadilhas desta máquina

- **`TaskStop` não mata o backend.** `mvn spring-boot:run` roda o app num **JVM filho**; parar a
  tarefa mata só o Maven. O app novo sobe, **aplica o Flyway**, e morre com "Port 8080 was already
  in use" — a migration aparece aplicada e quem responde é o processo velho. **Custou um
  diagnóstico invertido nesta sessão.** Matar pelo PID que segura a porta:
  `Get-NetTCPConnection -LocalPort 8080 -State Listen`.
- **Rota ou código de erro novo cobra TRÊS artefatos no mesmo commit:** `contract/openapi.json`
  (`-Dpetfy.openapi.update=true`), `web/src/dados/gerado/api.d.ts` (`npm run gerar:api`) e a
  tradução `erro.<n>` em `pt-BR.ts`. **O CI web tem um passo que o `mvn` não tem.**
- **O CI é filtrado por caminho:** commit que só toca `src/` não roda o CI web, e vice-versa. Um
  único check verde pode ser o correto — conferir o filtro antes de suspeitar de CI mudo.
- **O nome do método do controller vira o `operationId`**, e um nome repetido faz o gerador
  renomear o vizinho para `_1`.
- **`NoClassDefFoundError` numa classe `$1` é `target/` velho.** `mvn clean` resolve.
- **Tela não fala HTTP:** o `regra-da-camada.test.ts` recusa `cliente.GET`/`fetch` fora de
  `src/dados/`.
- **O `Set-Location` do PowerShell persiste**, e o `cwd` do Bash também deriva entre chamadas —
  `mvn` com `-f` e caminho absoluto evita o "no POM in this directory".
- **`vite build` suja o `arvore-de-rotas.gen.ts`**, e com o dev server rodando ele regenera em
  loop: **parar o dev server antes de sincronizar**.
- **O banco de teste NÃO é limpo entre casos.**
- **`@MockBean`, e não `@MockitoBean`** — esta versão do Spring Boot não tem o segundo.
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **O merge de PR é comando do Ulysses, sempre.**

## Como subir, e como regenerar

```
docker start petfy-pg-sessao
$env:DB_PORT="5433"; mvn spring-boot:run "-Dspring-boot.run.profiles=local"
cd web; npm run dev      # http://localhost:5173
```

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true
cd web && npm run gerar:api
cd web && npx vite build                             # regenera a arvore de rotas
mvn test "-Dsurefire.runOrder=reversealphabetical"   # caça asserção dependente de ordem
```

Os três primeiros entram no **mesmo commit** da mudança que os causou.
