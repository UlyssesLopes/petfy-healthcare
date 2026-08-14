# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-14, na sessão que pagou **quatro dívidas antigas** — e descobriu que três
> delas estavam registradas como "não dá para fazer" com justificativas que não se sustentavam.

## Onde o trabalho está agora

**1039 casos no backend, 51 no front, `Skipped: 0`.** Migrations até a **V50**, e as cinquenta
aplicam do zero em menos de 400 ms.

| PR | Conteúdo | Estado |
|---|---|---|
| **#65** | O registro do tutor, a duração do gasto (V47), os convites sem link | **mergeado** |
| **#66** | O aviso in-app (V48), o remédio (V49), os aparelhos (V50), a guarda na edição | **mergeado** |
| **#67** | As três telas do #66, e a correção do canal de aviso | **aberto**, CI verde |

## A lição desta sessão, e ela se repetiu três vezes

**Justificativa escrita não é prova.** Três dívidas estavam registradas como impossíveis, e duas
descreviam custos que **já estavam pagos**:

- A **Tela 36** dizia que o produto não podia saber dos aparelhos conectados porque o JWT é
  stateless. Mas o `TokenFreshness` já consultava o banco em **toda requisição autenticada** desde
  o P1, para derrubar token anterior a uma troca de senha. Faltava uma linha por sessão.
- O **`VaccineFactory`** dizia que a edição ficava fora da guarda de dose duplicada de propósito,
  porque "é raro e tem rastro". **Rastro não é guarda** — a duplicata nascida de uma edição dobra a
  dose no histórico igual à nascida de duas gravações.
- O **remédio como fato de saúde** parecia mudança de modelo. A `CareInstruction` já era
  "prescrição, medicação e tema de casa" desde o P3, e já não exigia credencial. Faltava a ligação.

**Antes de aceitar um "não dá", conferir se o custo que a justificativa teme já não está sendo pago
em outro lugar.**

## O defeito que só o app de pé mostrou

O canal in-app nasceu no #66 para alcançar quem o e-mail não alcança. Mas a supressão de "endereço
não confirmado" morava nos **notificadores do domínio**, antes do dispatcher: seis deles faziam
`continue`, e o canal novo nunca via a mensagem. **Ele estava morto exatamente para as pessoas que
existia para atender**, e nenhum teste dizia o contrário porque nenhum ia do evento até o canal.

A regra mudou de lugar, não de conteúdo: **"não mandamos e-mail para endereço não confirmado" é
propriedade do canal de e-mail**, e não do evento. A `Notification` ganhou `porEmail`.

E desdobrou num sétimo lugar: o `AnimalReach.temAlguemNotificavel` fazia um animal cujos tutores não
confirmaram o e-mail **sair da varredura de vacina inteira**. Era o buraco da Tela 03.

## O QUE FICOU DEVENDO

### 1. A conferência no navegador — continua sendo a maior dívida

**E agora há muito o que conferir.** O app está de pé com dados: `marcelo@petfy.test` (senha
`Petfy!2026`), animal **Code** `a44cad1e-6d94-4f56-8818-f60304fe5fc7`, com aviso não lido, sete
aparelhos conectados, um remédio ligado a tratamento, e a previsão de custo com ração de dois meses.

Telas novas ou tocadas e nunca abertas: `/avisos`, `/convites`, `/animais/{id}/registrar`,
`/conta`, `/animais/{id}/compra`, `/animais/{id}/quem-cuida`, `/animais/novo`,
`/organizacoes/{id}/equipe`.

### 2. O refresh token — e é o que resolve o deslogue ao recarregar

O token vive em memória (decisão contra XSS registrada no `ROADMAP.md`), então **toda recarga
desloga**. O `sessao.ts` nomeia o gatilho da troca: *"o domínio próprio pondo front e API sob o
mesmo site, ou o login a cada recarga se mostrar insuportável no uso real"* — e o segundo aconteceu
nesta sessão, no uso real.

A `person_sessions` da V50 é **pré-requisito** disso, e não substituto.

### 3. Não há URL pública configurada

Por isso o convite vai por **código**, colado na `/convites`. No dia em que existir
`petfy.app.base-url` por ambiente, o `InviteNotifier` manda link em uma linha.

### 4. O aviso in-app não tem tempo real

A marca do sino atualiza a cada 60 s por `refetchInterval`. Quem está com a tela aberta vê o aviso
com até um minuto de atraso. Websocket ou SSE resolveria, e nenhum dos dois existe no produto.

## O que continua faltando no produto, e por quê

- **Não há foto no apadrinhamento nem no petshop** — anexo vive sob escopo, e não existe "anexo
  público".
- **A especialidade é texto livre**, e toda credencial anterior à V42 continua nula.
- **Espécie é só CANINA e FELINA.**
- **O escopo do evento de união e do óbito continua sendo um compromisso.**

## Armadilhas desta máquina

- **Rota ou código de erro novo cobra TRÊS artefatos no mesmo commit:** `contract/openapi.json`
  (`-Dpetfy.openapi.update=true`), `web/src/dados/gerado/api.d.ts` (`npm run gerar:api`) e a
  tradução `erro.<n>` em `pt-BR.ts`. **O CI web tem um passo "O cliente gerado esta atualizado" que
  o `mvn` e o `vitest` não têm** — verde local não prova nada aqui. Custou dois CI vermelhos.
- **O nome do método do controller vira o `operationId`.** Um método `listMine` ou `revoke` colide
  com os que já existem e faz o gerador **renomear o vizinho** para `revoke_1`. Nome único mantém o
  diff aditivo.
- **Tela não fala HTTP:** o `regra-da-camada.test.ts` recusa `cliente.GET` fora de `src/dados/`.
- **O `Set-Location` do PowerShell persiste entre chamadas** — `mvn` depois de um `cd web` falha com
  "no POM in this directory", e `npx tsc` fora de `web/` **instala um pacote `tsc` falso**.
- **`vite build` suja o `arvore-de-rotas.gen.ts`** e trava o `git pull`. Pior: **com o dev server
  rodando, ele regenera o arquivo em loop** e o `checkout` nunca limpa — parar o dev server antes de
  sincronizar.
- **`git checkout main` com o `.gen.ts` sujo reverte a árvore** e o `pull` aborta: dá para ficar com
  o código de antes do merge sem perceber.
- **Editar arquivo do front desloga quem está usando o app**: HMR recarrega a página, e o token vive
  em memória.
- **O banco de teste NÃO é limpo entre casos.**
- **Precisão de timestamp**: o Postgres guarda microssegundos e o `LocalDateTime` tem nanos —
  comparar o retorno do serviço com o que está gravado falha por motivo nenhum. Comparar dois
  valores lidos do banco.
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **Branch cujo PR já mergeou não roda check nenhum**, e nada avisa.
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
