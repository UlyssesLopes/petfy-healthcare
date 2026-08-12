# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-12, no fim de uma sessão longa que fechou **os blocos 6b, 7, 8 e 9** — e com
> eles **todas as telas desenhadas**.

## Onde o trabalho está agora

**Não há mais tela desenhada por construir.** As nove que faltavam foram entregues nesta sessão:
45, 46, 47, 48, 18, 19, 20, 21, 35 e 36.

| PR | Conteúdo | Estado |
|---|---|---|
| **#61** | Bloco 6b — Tela 45 | **mergeado** |
| **#62** | Blocos 7, 8 e 9 | **aberto**, esperando seu comando |

**1001 casos no backend, 51 no front, `Skipped: 0`.**

## Os três achados desta sessão, e o segundo é o maior

### 1. A concessão de organização não contava na guarda

**É a terceira metade do mesmo defeito.** A primeira foi na Tela 13 (`requireCustodia` não via
*custódia* de organização); a segunda na Tela 43 (`require` tinha o mesmo buraco). Esta era a mais
larga: o `Grant` aceita `granteeOrganization` desde o P2a — e o tutor concede à **clínica**, não ao
veterinário, justamente porque "quem atende hoje pode não ser quem atende no retorno" — mas a guarda
só sabia perguntar por pessoa.

**O efeito era grande e mudo:** a clínica listava o animal em "meus pacientes" pelo
`findVigentesDaClinica` e respondia **404** quando alguém tentava abrir ou escrever nele. Duas telas
discordando sobre o mesmo acesso, e a que mentia era a que prometia.

Corrigido em `AnimalAccessGuard.concessaoDoAutenticado`. Vale só a organização **declarada**, e a
concessão da pessoa vem primeiro por ser a mais específica. **Nenhum dos 992 casos quebrou.**

### 2. As Telas 19, 20 e 21 não estavam na fila — e bloqueavam fluxo já entregue

Estão no arquivo do petshop, escritas como `Telas 19–21` com travessão — **a terceira vez que esse
formato engana a contagem** (as 35–36 já haviam enganado duas). E não eram cosméticas:

- `POST /pet-tutor-invites/{token}/accept` existia no backend e **o frontend nunca o chamava**
- A adoção da colônia (Tela 44) e a transferência de titularidade **emitem convite**
- Ou seja: **dois fluxos entregues terminavam num convite que ninguém conseguia aceitar**

### 3. O encerramento de conta mudou de contrato

Até aqui, encerrar a conta resolvia sozinho: o animal **sem outro tutor morria com ela**, e o que
tinha co-tutor **passava para o mais antigo deles** — os dois em silêncio. O primeiro destrói anos de
registro de um animal vivo; o segundo entrega a responsabilidade a quem nunca disse sim.

Agora recusa com 409 e devolve a lista. É o "vai numa fatia própria" que o `PROXIMA-SESSAO` anterior
prometia, e o PRODUTO 3.4 já decidia.

**E ele expôs um buraco:** as custódias **já encerradas** nunca eram apagadas na exclusão de conta.
Antes quase não aparecia; agora toda pessoa que encerra a conta passou por uma transferência — e o
`DELETE /persons/me` responderia 500 **justamente para quem seguiu a instrução da tela**.

## O que cada bloco decidiu

### Bloco 6b — Tela 45, encaminhamento

- **O encaminhamento não concede nada.** O aceite produz um `Grant` comum, com `granted_by` sendo o
  tutor. A guarda não aprendeu nada.
- **"O que vai junto" é escopo, não evento** — e a tela assume o compromisso em voz alta.
- **A união de escopos no aceite:** se o especialista já é co-tutor com acesso permanente, gravar por
  cima **encolheria** o acesso dele.
- **A busca de profissional vive debaixo do animal**, e não numa rota de gente.

### Bloco 7 — Telas 46, 47, 48

- **Apadrinhar não move dinheiro.** Não há meio de pagamento em lugar nenhum do produto; o que existe
  é um compromisso registrado. **E o padrinho não vira um `Grant`** — o custo mora fora da linha do
  tempo desde a Tela 40, e é isso que torna a tela possível sem inventar escopo.
- **Hospedagem não precisou de migration.** Uma estadia é uma `Custody` `TRANSITORIA` — campos que
  existiam desde o P2 sem caminho HTTP. **O que ela precisou resolver não está no desenho:** o tutor
  deixaria de responder pelo animal e o guard responderia 404 para ele. A saída é uma concessão que
  ele mesmo concede, revogada na volta.
- **O ano do animal inclui o que deu errado.** Metade do serviço existe para achar os dias em que uma
  vacina esteve vencida e as condições crônicas sem reavaliação. A irregularidade **não está gravada**:
  é a leitura de duas doses.

### Bloco 8 — Telas 18 e 19–21

- **Não há nenhuma regra de petshop em lugar nenhum**: há um tutor que concedeu três escopos e não
  concedeu prontuário.
- **O losango vermelho vem do TIPO da condição, e não da gravidade** — o CHECK
  `gravidade_so_em_alergia` existe desde o P3, e foi o schema que ensinou.
- **Entregar sem texto não escreve nada na linha do tempo.**
- **Uma rota para as três telas de convite**, porque o que muda é o significado, e ele vem do convite.

### Bloco 9 — Telas 35 e 36

- **A busca diz o que você NÃO pode ver.** "Existem outros animais que casam com essa busca. Você não
  tem acesso a eles." Booleano, e não contagem: um número seria um oráculo.
- **A parcial existe aqui e não existe no `POST /found`** — lá servia varredura e não servia a
  ninguém; aqui quem busca já alcança o que a consulta devolve.
- **A V46 não cria nada**, e o registro dela é o que vale: diz onde cada peça já estava.

## O QUE FICOU DEVENDO, e é curto

### 1. "Aparelhos conectados" (Tela 36) — a única linha de tela não construída

O desenho pede *"3 sessões abertas. A mais antiga é de 11/2025"*. **Este produto autentica com JWT sem
estado:** o servidor não sabe quantos tokens válidos existem, e não teria como invalidar um deles.
Entregar exigiria persistir sessão, emitir refresh token e mexer no filtro de autenticação inteiro.

**A tela diz que não sabe**, e explica que trocar a senha derruba todas de uma vez — que é verdade.
Um número estimado seria pior que a ausência: a pessoa clicaria em "encerrar" acreditando ter
encerrado.

### 2. A conferência no navegador

**Continua devendo, e agora é a maior dívida do projeto.** O que foi conferido nesta sessão foi o
fluxo da Tela 45 **por HTTP contra o app de pé** (nove passos, tudo como prometido). As telas em si
nunca foram abertas: a extensão do Chrome não está conectada.

### 3. O `Â·` em texto visível

`pacientes.tsx:211,212,247` e `organizacoes.$organizationId.equipe.tsx:203`. Quatro trocas de
caractere, medidas e não corrigidas — pertencem às Telas 03 e 16.

### 4. `group_approvals` na exclusão de conta

A tabela nasceu no bloco 6 apontando para `persons` em quatro colunas, e o `PersonServiceImpl` não a
limpa. Quem pediu uma concordância num animal que **sobrevive** à exclusão faz o `DELETE /persons/me`
responder 500. **Medido, não corrigido** — e agora vale mais, porque o encerramento passou a ser um
caminho que as pessoas de fato percorrem.

## O que continua faltando no produto, e por quê

- **Não há aviso IN-APP.** O e-mail funciona desde o P4. A Tela 03 lista quem está vencendo e não
  avisa ninguém, e a 47 não promete o "você é avisado na hora" que o desenho escreve.
- **Não há foto no apadrinhamento nem no petshop** — anexo vive sob escopo, e não existe "anexo
  público". Pedir mais escopo para tirar uma foto contrariaria a frase que dá título ao arquivo do
  petshop.
- **A especialidade é texto livre, e agora tem tela** (a conta declara). Toda credencial anterior à
  V42 continua nula.
- **`alcanca()` do guard foi corrigido junto com o resto** — a dívida que o bloco 6 apontou está paga.
- **Espécie é só CANINA e FELINA**, e capacidade de organização não se declara na criação (Tela 15).
- **O escopo do evento de união e do óbito continua sendo um compromisso.**
- **A edição de vacina não passa pela guarda de dose duplicada.**

## A fila, e onde ela está

| # | Bloco | Estado |
|---|---|---|
| 1–5 | Faixa, núcleo clínico, valor, custo, fim e reencontro | **mergeados** |
| 6 | Animal comunitário — 43, 44 | **mergeado** — PR #60 |
| 6b | Encaminhamento — 45 | **mergeado** — PR #61 |
| 7 | Apadrinhar, hospedar, o ano — 46, 47, 48 | **no PR #62** |
| 8 | Petshop e quem recebe — 18, 19, 20, 21 | **no PR #62** |
| 9 | A busca e a conta — 35, 36 | **no PR #62** |

**Não há bloco 10.** O próximo trabalho não é uma tela: é escolher entre a conferência no navegador,
os e2e, e as quatro dívidas acima.

## Armadilhas desta máquina

- **Branch cujo PR já mergeou não roda check nenhum**, e nada avisa. **Aconteceu nesta sessão:** você
  mergeou o #61 enquanto três commits do bloco 7 iam para a mesma branch. A saída foi ramificar e
  abrir o #62. **Bloco novo, branch nova — e conferir se o PR ainda está aberto antes de empurrar.**
- **`vite build` suja o `arvore-de-rotas.gen.ts`** e o `git pull` aborta. Mas **rota nova EXIGE rodar
  `npx vite build` antes do `tsc`** — e aí a mudança do `.gen.ts` é legítima.
- **A rota gerada perde o `_`**: o arquivo é `animais.$animalId_.transferir.tsx` e o `to` do `Link` é
  `/animais/$animalId/transferir`. O `tsc` sugere a forma certa no erro.
- **O Rancher Desktop precisa estar aberto.** Conferir `Skipped: 0`.
- **O container `petfy-pg-sessao` já existe**: `docker start`, e não `docker run`.
- **O banco de teste NÃO é limpo entre casos.** Asserção de "um único resultado" passa no primeiro e
  falha nos seguintes; o que for único no schema (CRMV, microchip) tem de ser sorteado.
- **Teste de container não toca coleção preguiçosa fora de transação** — ler pelo `JdbcTemplate`.
- **`Get-Content -Raw | Set-Content` duplo-codifica** e o erro em `.java` mente: `class, interface,
  enum, or record expected` na linha 6.
- **`Select-String` num log de Maven casa o SQL do Hibernate** — redirecionar e filtrar depois.
- **`Select-Object -First N` corta o pipe e faz o `$LASTEXITCODE` mentir** (exit 255 com build ok).
- **`docker cp` converte caminho no Git Bash**: `cd` para o diretório e usar caminho relativo.
- **`-Dtest=` com vírgula e `-D` com ponto precisam de aspas.**
- **Here-string do PowerShell quebra em aspas duplas**: escrever a mensagem num arquivo e usar
  `git commit -F`.
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **O merge de PR é comando do Ulysses, sempre.**
- **ICU não aceita chave vazia em `select`** — duas mensagens concatenadas.
- **Código de erro novo quebra o front**, de propósito: o `erroDaApi.test.ts` cobra tradução para
  cada valor do enum. Vinte e três entraram nesta sessão.
- **Postgres recusa parâmetro nulo sem tipo** em `:x is null` quando não há coluna a que associar —
  usar sentinela, como o `DESDE_SEMPRE` da linha do tempo.

## Como subir, e como regenerar

```
docker start petfy-pg-sessao
$env:DB_PORT="5433"; mvn spring-boot:run "-Dspring-boot.run.profiles=local"
cd web; npm run dev      # http://localhost:5173
```

Contas semeadas nesta sessão (senha `Petfy!2026`): `marcelo@petfy.test` (tutor do Code),
`ana@petfy.test` (CRMV, concessão no Code), `roberto@petfy.test` (ortopedia). O Code é
`e7bf40cc-adbc-4617-be82-3b35f1b5ef45`.

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true
cd web && npm run gerar:api
cd web && npx vite build                             # regenera a arvore de rotas
mvn test "-Dsurefire.runOrder=reversealphabetical"   # caça asserção dependente de ordem
```

Os três primeiros entram no **mesmo commit** da mudança que os causou.
