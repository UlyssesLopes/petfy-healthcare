# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-10, no fim da sessão que construiu sete telas novas.

## Onde o trabalho está

**Branch `feat/tela-conceder-acesso`, 9 commits, front verde:** `tsc --noEmit` limpo e
**42 testes**. O PR está aberto e é o CI dele que fecha a única verificação pendente — o
Docker desta máquina passou a sessão inteira fora do ar (a distro `rancher-desktop-data`
está `Stopped`), então **o caso novo do `LeituraForaDeTransacaoContainerTest` não rodou
aqui** e nenhuma das telas novas foi conferida no navegador.

O PR #47 (quatro telas) já está na `main`.

## As telas: 14 das 20 endereçadas

| Tela | Rota | Estado |
|---|---|---|
| 01 · feed de pendências | `/` | Feita |
| 02 · a vida do animal | `/animais/$animalId` | Feita |
| 03 · área de organização | `/pacientes` | **Feita sem a coluna principal** — ver abaixo |
| 07 · criar conta | `/criar-conta` | Feita |
| 08 · três cômodos | `/comecar` | Feita |
| Onboarding · 4 passos | `/animais/novo` | **Passos 1, 2 e 4** — o 3 está trancado |
| 09 · conceder acesso | `/animais/$animalId/conceder-acesso` | Feita |
| 11 · transferir titularidade | `/animais/$animalId/transferir` | Feita, **contrariando o desenho** |
| 14 · os estados | `componentes/Estados.tsx` | Feita — não é rota |
| 15 · criar organização | `/organizacoes/nova` | Feita sem as capacidades |
| 16 · equipe | `/organizacoes/$id/equipe` | **A um quarto** — só convites |
| 22 · quem alcança | `/animais/$animalId/quem-cuida` | Feita |
| 23 · contestar registro | `.../discordar/$registroId` | Feita sem o aviso |
| 28 · a porta | `/entrar` | Feita |
| 29 · a porta na mão | `/entrar` a 390px | Feita |
| 24 · avisos | — | **Não dá para construir** |
| 10, 12, 13, 17, 18 | — | **Onda D: backend novo** |
| 25–27 · operação na mão | — | Idem (o arquivo os traz como faixa, não como telas numeradas) |

## O que a construção descobriu, e é o que importa daqui

### Um 500 novo, o quinto do mesmo defeito — e o primeiro numa escrita

`grant()` não era `@Transactional`. Conceder pela primeira vez funcionava; **reconceder
respondia 500**, porque o grant vem do repositório com a organização como proxy e o
`toResponse` lê o nome dela depois de a transação do `save` fechar. Os quatro anteriores
eram leitura — o que os cinco têm em comum não é ler, é **montar DTO a partir de entidade
fora de transação**. O critério da classe de guarda foi emendado para dizer isso.

### A dívida de contrato mais caras: o multipart não está declarado

`POST /pet-id/import-pet-id-card` **existe e lê carteirinha com Tesseract**, e
`POST /animals/{id}/attachments` também existe. **O contrato não declara o corpo multipart de
nenhum dos dois** — o cliente é gerado dele, então não há por onde mandar arquivo. Isso
tranca sozinho: o passo 3 do onboarding (que o próprio desenho chama de "o mais trabalhoso
e o mais valioso"), a foto do animal e a foto da carteirinha.

**É o primeiro item da próxima sessão se a prioridade for entregar tela.** Provável causa:
`@RequestParam MultipartFile` em vez de `@RequestPart`, que o springdoc documenta.

### O desenho promete três coisas que o backend faz ao contrário (Tela 11)

Ele desenhou "quem deixa de ver o Code" — co-tutora e organizações perdendo acesso no
aceite — com a tese: *"acessos não são herdados"*. Conferido no `PetTutorServiceImpl`, nos
dois caminhos: **nenhuma concessão é revogada**, e o titular anterior ganha `EDITOR`, que é
escrita. A tela mostra a verdade e o `voz.test.ts` trava isso; **fazer o backend obedecer ao
desenho é decisão de produto e está em aberto.**

### O conflito que o desenho protege não é detectado

A Tela 14 desenha a recusa de dose duplicada: *"dois registros da mesma dose viram dose
dobrada no histórico"*. **Não há código de conflito para isso** — as duas gravações passam.
O dano é silencioso: ninguém é avisado.

### Onde o produto emudece

- **Observação não notifica ninguém** (`ObservationServiceImpl` não chama notificador). Por
  isso a Tela 23 não pode avisar quem registrou, e é a maior perda dela.
- **Não há canal de contato com quem registrou:** a autoria é nome em texto, sem id.
- **Não há preferência de aviso:** a única rota é silenciar um item. A Tela 24 cai inteira.
- **Não há listagem de membros de organização, nem função no convite, nem desligamento.**
- **Quem já tem conta não tem como aceitar convite de organização** — só pelo `inviteToken`
  na criação da conta.
- **Nenhuma agregação por organização:** `/due-items` é sempre da pessoa logada, e o
  `VetPetDTO` não traz nada de saúde. É o que tira a coluna "situação" da Tela 03.
- **Capacidade de organização não se declara:** o enum existe, o campo não.
- **Espécie é só `CANINA` e `FELINA`**; RGA e tatuagem não têm campo.
- **`CustodyEndReason` tem `ADOCAO` e `DEVOLUCAO`, e nenhuma rota HTTP os alcança** — o
  domínio da adoção existe, o caminho não.

## A decisão que trava o resto

**A onda D (telas 10, 12, 13, 17, 18 e o grupo 25–27) não é trabalho de tela: é fase de
produto.** Matrícula e turma, rede de lares, agenda de banho e tosa, a operação do dia da
creche e a adoção pela organização precisam de modelo, migração, contrato e teste novos. O
`GERIR_TURMA_E_VAGA` e o `MANTER_REDE_DE_LARES` são flags declaradas e vazias.

Antes disso há trabalho de tela mais barato e com dono claro:

1. **Declarar o multipart no contrato** e destravar o passo 3, a foto do animal e o anexo.
2. **Decidir se acesso é herdado na transferência** (Tela 11).
3. **Detectar dose duplicada** — é a única correção desta lista que evita dano ao histórico.
4. **Rota de membros da organização** e função no convite, que fazem a Tela 16 existir.
5. **Agregação por organização** ("quem está vencendo"), que devolve a Tela 03 inteira.

## Armadilhas desta máquina

- **O Docker não subiu nesta sessão.** `docker ps` responde `timed out dialing Hyper-V
  socket`; o pipe existe e a distro `rancher-desktop` roda, mas a `rancher-desktop-data`
  está `Stopped`. **Não reinicie o Rancher** — a máquina tem k8s com argocd no ar.
- **Sem Docker não há teste de container e não há aplicação local**, então "conferido no
  navegador" ficou impossível. Quando voltar: rodar a suíte inteira e abrir as sete telas.
- **`cd` no Bash persiste entre chamadas** — e rodar `vitest` da raiz em vez de `web/`
  produz duas falhas falsas na `paleta.test.ts` e cria um `node_modules/` vazio na raiz.
- **`gh` perde a credencial**; `gh auth login` é interativo.
- **O merge de PR é bloqueado ao agente pelo classificador** — é comando seu, sempre.
- **O gerador de rotas do TanStack** não tem CLI aqui: quem regenera
  `arvore-de-rotas.gen.ts` é `npx vite build`.

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
```

Os três entram no **mesmo commit** da mudança que os causou.
