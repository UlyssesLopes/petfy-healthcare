# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-14, no fim de uma sessão longa que fechou **seis dívidas antigas** em quatro
> PRs — e descobriu que várias delas estavam registradas como "não dá para fazer".

## Onde o trabalho está agora

**1047 casos no backend, 51 no front, `Skipped: 0`.** Migrations até a **V51**. A `main` está em
`ecaae70`, e **não há PR aberto**.

| PR | Conteúdo |
|---|---|
| **#65** | O registro do tutor, a duração do gasto (V47), os convites que ninguém aceitava |
| **#66** | O aviso in-app (V48), o remédio como saúde (V49), os aparelhos (V50), a guarda na edição |
| **#67** | As três telas do #66, e a correção do canal de aviso |
| **#68** | O refresh (V51) e o aviso em tempo real |

## COMECE POR AQUI: a higienização dos comentários

Pedido do Ulysses, 2026-08-14:

> *"A cada linha de código você adiciona uma bíblia de texto em qualquer lugar. Não, aqui não é um
> livro de receitas."*

**É a próxima tarefa, e é dele a prioridade.** O código acumulou trechos com dez linhas de
justificativa para duas de lógica. O trabalho tem duas partes:

1. **Avaliar cada comentário**: precisa existir ali? O código não se explica sozinho?
2. **O que for aviso de verdade sai do arquivo** para um documento próprio, que liga o texto ao
   fluxo / trecho / classe / decisão a que pertence.

**Duas decisões ainda em aberto, e são do Ulysses:**

- o documento é **um arquivo só ou um por bloco**?
- o link é feito **por nome de classe, por migration, ou por fluxo**?

**Por onde começar:** os arquivos mais inchados são os mais recentes — `RefreshCookie`,
`SessionRenewalImpl`, `V51`, `V48`, `V50`, `InAppNotifier` e as migrations do bloco de custo. O
`AvisoStream` e o cliente de SSE já nasceram no padrão novo (uma linha por decisão) e servem de
referência do que é "suficiente".

## A lição desta sessão, e ela se repetiu quatro vezes

**Justificativa escrita não é prova.** Estava registrado no código que não dava para fazer, e não
era verdade:

- a **Tela 36** dizia que o JWT stateless impedia saber dos aparelhos conectados — e o
  `TokenFreshness` já consultava o banco em **toda requisição autenticada** desde o P1;
- o **`VaccineFactory`** dizia que a edição ficava fora da guarda de dose duplicada porque "é raro e
  tem rastro" — e **rastro não é guarda**;
- o **remédio como fato de saúde** parecia mudança de modelo — e a `CareInstruction` já era
  "prescrição, medicação e tema de casa" desde o P3, sem exigir credencial;
- o **`sessao.ts`** dizia que o login a cada recarga era "o custo aceito" — e ele mesmo nomeava o
  gatilho para deixar de ser.

**Antes de aceitar um "não dá", conferir se o custo que a justificativa teme já não está sendo pago
em outro lugar.**

## Dois defeitos que só apareceram com o app de pé

**O aviso in-app nasceu morto para quem ele servia.** Seis notificadores checavam "e-mail
confirmado?" e faziam `continue` *antes* de montar a mensagem — o canal novo nunca a via. A regra
mudou de lugar, não de conteúdo: *"não mandamos e-mail para endereço não confirmado"* é propriedade
do **canal de e-mail**. A `Notification` ganhou `porEmail`.

**A Tela 36 mentia depois de uma troca de senha.** Os tokens paravam de autenticar pelo `iat` e as
sessões seguiam listadas como abertas. Descobri porque um teste meu caiu: eu tinha posto a revogação
dentro do `renovar()`, que encerrava e depois lançava — e o rollback desfazia tudo.

## O QUE FICOU DEVENDO

### 1. A conferência no navegador — nunca foi feita, e é a maior dívida

O app sobe com dados: `marcelo@petfy.test` / `Petfy!2026`, animal **Code**
`a44cad1e-6d94-4f56-8818-f60304fe5fc7` — com aviso não lido, aparelhos conectados, remédio ligado a
tratamento, e previsão de custo com ração de dois meses.

**Telas novas ou tocadas e nunca abertas:** `/avisos`, `/convites`, `/animais/{id}/registrar`,
`/conta`, `/animais/{id}/compra`, `/animais/{id}/quem-cuida`, `/animais/novo`,
`/organizacoes/{id}/equipe`.

**O que mudou de comportamento e vale conferir primeiro:** recarregar a página **não desloga mais**.

### 2. Não há URL pública configurada

Por isso o convite vai por **código**, colado na `/convites`. No dia em que existir
`petfy.app.base-url` por ambiente, o `InviteNotifier` manda link em uma linha.

### 3. `SameSite` do cookie em produção

Localmente `Lax` funciona porque front e API são o mesmo site (a porta não conta). Se em produção
eles ficarem em sites diferentes, só `None` funciona — e `None` exige `Secure`, que exige HTTPS. Já
é configurável: `petfy.refresh.cookie-same-site` e `petfy.refresh.cookie-secure`.

### 4. O que continua faltando no produto

- **Não há foto** no apadrinhamento nem no petshop — anexo vive sob escopo.
- **A especialidade é texto livre**, e toda credencial anterior à V42 continua nula.
- **Espécie é só CANINA e FELINA.**
- **O escopo do evento de união e do óbito continua sendo um compromisso.**

## Armadilhas desta máquina

- **Rota ou código de erro novo cobra TRÊS artefatos no mesmo commit:** `contract/openapi.json`
  (`-Dpetfy.openapi.update=true`), `web/src/dados/gerado/api.d.ts` (`npm run gerar:api`) e a
  tradução `erro.<n>` em `pt-BR.ts`. **O CI web tem um passo "O cliente gerado esta atualizado" que
  o `mvn` e o `vitest` não têm** — verde local não prova nada aqui. Custou dois CI vermelhos.
- **O nome do método do controller vira o `operationId`.** `listMine` ou `revoke` colide com os que
  já existem e faz o gerador **renomear o vizinho** para `revoke_1`. Nome único mantém o diff
  aditivo.
- **`NoClassDefFoundError` numa classe `$1` é `target/` velho, e não código.** Um `switch` alterado
  deixa a classe sintética fora de sincronia e a suíte estoura com centenas de erros que não são
  reais. `mvn clean` resolve.
- **Tela não fala HTTP:** o `regra-da-camada.test.ts` recusa `cliente.GET`/`fetch` fora de
  `src/dados/`.
- **O `Set-Location` do PowerShell persiste entre chamadas** — `mvn` depois de um `cd web` falha com
  "no POM in this directory", e `npx tsc` fora de `web/` **instala um pacote `tsc` falso**.
- **`vite build` suja o `arvore-de-rotas.gen.ts`** e trava o `git pull`. Com o dev server rodando,
  ele **regenera em loop** e o `checkout` nunca limpa — parar o dev server antes de sincronizar.
- **`git checkout main` com o `.gen.ts` sujo reverte a árvore** e o `pull` aborta: dá para ficar com
  o código de antes do merge sem perceber.
- **Editar arquivo do front desloga quem está usando o app** (HMR recarrega). Isso doía mais antes do
  refresh; agora a sessão sobrevive, mas a página ainda recarrega.
- **O banco de teste NÃO é limpo entre casos.**
- **Precisão de timestamp**: Postgres guarda microssegundos, `LocalDateTime` tem nanos — comparar o
  retorno do serviço com o que está gravado falha por motivo nenhum. Comparar dois valores lidos.
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **Branch cujo PR já mergeou não roda check nenhum**, e nada avisa.
- **Empurrar depois de o PR ser mergeado perde o commit** — aconteceu com um `docs:` nesta sessão.
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
