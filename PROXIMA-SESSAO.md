# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-14, na sessão que fechou a **primeira tela que não veio de um desenho** e
> corrigiu **três convites que ninguém tinha como aceitar**.

## Onde o trabalho está agora

Não há mais tela desenhada por construir — isso não mudou. O que esta sessão fez foi o que a
conferência encontrou **depois** de todas elas existirem.

**1005 casos no backend, 51 no front, `Skipped: 0`.** A `main` continua em `bb4df76`; o trabalho
desta sessão está na branch `fix/o-ponto-e-a-concordancia`, **ainda não commitado**.

## O achado que reordenou a fila

**O tutor não tinha como registrar nada no próprio animal.** Nem vacina, nem peso, nem observação —
só "discordar" de um registro que já existia.

A prova estava nos hooks: `useRegistrarDose`, `useDoseAnterior` e `useCorrecoes` existiam em `dados/`
e **nenhuma rota os chamava**. O único lugar do produto que registrava vacina ou peso era
`pacientes.$animalId.atendimento.tsx`, que é a tela da **clínica**. O botão "Registrar evento", no
cabeçalho do animal, estava `disabled` sem condição, sem destino e sem handler — não era descuido de
estilo, era a porta de um caminho que nunca foi construído.

Um produto que promete "cada dose, cada consulta e cada dia de creche entrou aqui com o nome de quem
fez", e o tutor sem jeito de fazer.

## O que esta sessão entregou

### 1. A tela de registrar do tutor — `/animais/{id}/registrar`

**A primeira tela que não veio de um `.dc.html`.** Vacina, peso e observação: as três coisas que o
tutor faz sozinho, em casa, sem organização por trás — o mesmo critério que a Tela 42 usa para o
dinheiro.

- **Atendimento NÃO está aqui, e a ausência é a regra mais importante da tela.** Diagnóstico e
  prescrição são ato clínico, e ato clínico exige credencial. O tutor registra o que **viu**:
  "mancou depois do parque". Quem conclui assina.
- **A data vem antes do conteúdo**, e nunca "agora" implícito: quem lança a carteirinha de papel de
  2019 está registrando 2019, e um formulário que assume hoje transformaria a vida inteira do animal
  num único dia.
- **A vacina sai do catálogo da espécie**, que a carteira já carrega. Texto livre faria "V10", "v10"
  e "Vacina V10" virarem três séries, e o mesmo reforço apareceria três vezes.
- **O botão do cabeçalho abriu**: `animais.$animalId.tsx` deixou de ter `<button disabled>` e agora é
  um `Link` para a rota.

**Conferido por HTTP com conta sem credencial (`professional: false`):** as três escritas passam —
`POST /vaccines`, `POST /animals/{id}/weights` e `POST /animals/{id}/observations`.

### 2. Duração da despesa — a V47, e ela separa dois fatos

**COBRIR e REPETIR eram a mesma coisa, e ninguém via.** A caixinha "dura cerca de um mês" gravava
`recurrence = MENSAL`, e o `CostRecurrence` tem exatamente um valor. Com isso:

- a **mensalidade da creche REPETE**: chega todo mês, e cada mês custa o valor cheio;
- a **ração COBRE um período**: R$ 190 que atendem dois meses custam R$ 95 por mês, e voltam daqui a
  dois meses.

Enquanto só existia "um mês", os dois coincidiam por acidente. A pergunta que o tutor faz — "e a
ração que dura dois meses?" — não tinha resposta: marcar a caixinha dizia "todo mês", que é **o dobro
do que ele gasta**.

`covers_months` é **um número em meses, e não mais um valor no enum**: a ração do gato dura 45 dias,
a do cachorro grande dura 20, e `BIMESTRAL/TRIMESTRAL` obrigaria cada um a mentir para o vizinho mais
próximo. Em meses e não em dias porque a previsão pensa em doze meses, e quem compra ração não sabe
se ela dura 28 ou 31.

O `CostForecastBuilder` deixou de assumir doze vezes por ano: agora é `12 / intervalo`, **divisão
para baixo de propósito** — algo que dura cinco meses cabe duas vezes no ano com folga, e arredondar
para cima prometeria uma terceira compra que não acontece. A previsão erra por menos.

**O backfill afirma o que a pessoa já tinha afirmado:** toda `COMPRA` com `recurrence = MENSAL` vira
`covers_months = 1`. A mensalidade da creche **não entra**, e é esse o ponto da separação.

**Medido ao vivo:** ração de R$ 190 que dura 2 meses → 6 vezes no ano; creche de R$ 300 → 12 vezes;
total R$ 4.740. Antes da V47 o mesmo animal teria previsão de R$ 5.880 — **R$ 1.140 a mais**.

### 3. Campo de data no lançamento de compra

O backend aceitava `occurredAt` desde sempre; o formulário não tinha campo e lançava agora. Quem
lança a nota do mercado à noite registrava hoje; quem lança a de sábado na segunda registrava errado
— **e a previsão, que lê "quando foi a última vez", herdava o erro.**

### 4. Convidar quem cuida — e os três convites que ninguém aceitava

**O convite de co-tutor só existia no passo 4 do cadastro do animal.** Quem passasse dali sem
preencher o e-mail nunca mais convidava ninguém para aquele animal. Agora vive em "Quem cuida", que é
exatamente a pergunta que a tela responde.

**E ao construí-lo apareceu o defeito de verdade, e ele é de família:**

> O `invite` grava o convite e devolve o token — "único momento em que o token existe fora do
> cliente" —, e **não há envio de e-mail em lugar nenhum**.

Três telas criavam convite e **nenhuma mostrava o link**. O token morria no recarregamento, e o
convite ficava de pé esperando alguém que nunca soube dele:

| onde | o que dizia | o que faz agora |
|---|---|---|
| cadastro do animal, passo 4 | *"Convite enviado para {email}"* — **mentira desde o primeiro dia** | mostra o link |
| equipe da organização (Tela 16) | mostrava "convite aguardando" na lista, e mais nada | mostra o link, com "aparece uma vez só" |
| quem cuida (Tela 11) | não existia | mostra o link, com botão de copiar |

Nenhuma frase diz "enviamos um e-mail". Elas dizem que **quem convida é quem entrega o link**.

## O QUE FICOU DEVENDO

### 1. A conferência no navegador — continua a maior dívida

**E agora há dados para fazê-la.** O app está de pé com banco populado: conta `marcelo@petfy.test`
(senha `Petfy!2026`), animal **Code** `a44cad1e-6d94-4f56-8818-f60304fe5fc7`, com uma vacina V10, uma
pesagem, uma observação, uma ração de dois meses e uma mensalidade de creche.

As telas que esta sessão mexeu e ninguém abriu ainda: `/animais/{id}/registrar` (nova),
`/animais/{id}/compra`, `/animais/{id}/quem-cuida`, `/animais/novo` e
`/organizacoes/{id}/equipe`.

### 2. O e-mail de convite não existe

As telas agora dizem a verdade, mas **a verdade é ruim**: entregar o link é trabalho manual de quem
convida. O Resend funciona desde o P4 e o `AnimalDeathNotifier` prova que dá para mandar e-mail — o
convite simplesmente nunca foi ligado nele. É a próxima coisa que vale mais que uma tela.

### 3. "Aparelhos conectados" (Tela 36)

O JWT sem estado não sabe quantas sessões existem. A tela diz que não sabe, e isso continua sendo a
resposta certa.

### 4. Remédio e ração viram custo, e nada mais

Um remédio que o animal **está tomando** deveria ser fato de saúde além de despesa, e hoje não é. O
custo fora da linha do tempo é deliberado — "nenhum escopo de acesso concede preço junto com saúde" —
mas o remédio é a exceção que o modelo ainda não tem.

## O que continua faltando no produto, e por quê

- **Não há aviso IN-APP.** O e-mail funciona desde o P4; a Tela 03 lista quem está vencendo e não
  avisa ninguém.
- **Não há foto no apadrinhamento nem no petshop** — anexo vive sob escopo, e não existe "anexo
  público".
- **A especialidade é texto livre**, e toda credencial anterior à V42 continua nula.
- **Espécie é só CANINA e FELINA.**
- **A edição de vacina não passa pela guarda de dose duplicada.**

## Armadilhas desta máquina

- **`vite build` suja o `arvore-de-rotas.gen.ts`** e o `git pull` aborta. Mas **rota nova EXIGE rodar
  `npx vite build` antes do `tsc`** — e aí a mudança do `.gen.ts` é legítima.
- **A rota gerada perde o `_`**: o arquivo é `animais.$animalId_.registrar.tsx` e o `to` do `Link` é
  `/animais/$animalId/registrar`.
- **Chave de mensagem que falta não quebra o `tsc` sozinha**, mas o `mensagens` é `as const` e o
  `ChaveDeMensagem` é `keyof typeof` — a tela nova só compilou depois das 33 chaves entrarem.
- **Os hooks usam os nomes do CONTRATO, e não português**: `vaccineName`, `applicationDate`. Só
  `useRegistrarPeso` e `useRegistrarObservacao` traduzem.
- **O `oQue` do `ErroAoGravar` entra na frase "Não conseguimos gravar {o_que}"** — é substantivo
  ("o registro"), e passar o nome do animal faz a frase dizer que não conseguimos gravar o bicho.
- **`kind` da despesa é `CRECHE_MENSALIDADE`**, e não `MENSALIDADE`.
- **`POST /persons` exige `acceptedTerms`**, senão 400 mudo.
- **O `Set-Location` do PowerShell PERSISTE entre chamadas** — `mvn` rodado depois de um `cd web`
  falha com "no POM in this directory". Usar `mvn -f <caminho>/pom.xml`.
- **`npx tsc` fora de `web/` instala um pacote `tsc` falso do npm** e diz "this is not the tsc
  command you are looking for".
- **O Rancher Desktop precisa estar aberto.** Conferir `Skipped: 0`.
- **O container `petfy-pg-sessao` já existe**: `docker start`, e não `docker run`.
- **O banco de teste NÃO é limpo entre casos.**
- **`Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir.**
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **Branch cujo PR já mergeou não roda check nenhum**, e nada avisa.
- **O merge de PR é comando do Ulysses, sempre.**
- **Código de erro novo quebra o front**, de propósito: o `erroDaApi.test.ts` cobra tradução para
  cada valor do enum.

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
