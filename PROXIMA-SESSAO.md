# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-10, com a árvore limpa e o CI verde.

## Onde o trabalho está

**Construindo as telas de verdade a partir de `design/IdentidadeVisual/`.** A árvore
compila e passa: front com `tsc --noEmit` limpo e **32 testes**, backend com **780 testes,
0 falhas, `Skipped: 0`** — este último medido no CI, com o `ContainerTestsHabilitadosTest`
verde, então os containers subiram de verdade.

**Branch `feat/tela-do-animal`, PR #47, os dois checks verdes, aguardando merge.** O merge
não foi feito: é comando seu (`gh pr merge 47 --merge`). Cinco commits — telas 28, 02, 01 e
22, mais a limpeza do cache do TanStack que havia entrado por descuido.

## As 20 telas da entrega — 4 feitas, 16 faltam

| Tela | Título | Rota | Estado |
|---|---|---|---|
| 01 | Área do tutor — o feed de pendências | `/` | **Feita** |
| 02 | A vida do Code — carteira e linha do tempo | `/animais/$animalId` | **Feita** |
| 22 | Quem alcança o Code — e o que cada um leu | `…/quem-cuida` | **Feita** |
| 28 | Entrar no Petfy | `/entrar` | **Feita** |
| 07 | Cadastro e login — uma só, para todo mundo | — | Falta |
| 08 + Onboarding | Três cômodos, nenhum corredor (4 passos) | — | Falta |
| 09 | Conceder acesso — escopo sem a palavra "escopo" | — | Falta, backend existe |
| 11 | Transferir a titularidade | — | Falta |
| 23 | Contestar um registro — sem botão de apagar | — | Falta |
| 24 | Avisos — o que interrompe e o que espera | — | Falta |
| 29 | Entrar pelo celular | — | Falta |
| 03 | Área de organização — a segunda-feira da veterinária | — | Falta |
| 14 | Os estados — desenhados, não descritos | — | Falta, é transversal |
| 15, 16 | Criar a creche · equipe, funções e desligamento | — | Falta, **depende da decisão 15** |
| 10, 12, 13, 17, 18 | Matrícula, abrigo, adoção, operação do dia, agenda de banho | — | Falta, **são os buracos** |

O bloco 0 (tokens) fechou no #45. O que resta é bloco 2/3 — e o bloco 4 continua sendo
escopo novo de produto, **não decidido**.

## Os cinco buracos, que não são custo de front

| Tela | O que não existe no backend |
|---|---|
| 17 · Da Creche Quintal, hoje | **Conteúdo** — recado, foto, avaliação do dia |
| 10 · Matrícula do Code | **Vínculo, turma, lotação, janela, check-in.** Só a flag `GERIR_TURMA_E_VAGA`, vazia |
| 12 · Rede de lares | **Disponibilidade.** Idem: `MANTER_REDE_DE_LARES` é flag sem nada atrás |
| — · Percepção ("uma leitura do Petfy") | **Insight.** Zero apoio |
| — · Quem está vencendo | Pendência é da pessoa logada; consulta por organização não existe |

Faltam também dois `DueItemKind`: **tema de casa** e **matrícula irregular**.

## O que aprendemos ao traduzir as quatro telas

**O markup vem do arquivo, convertido por script** (`style="..."` → `style={{...}}`), e o
estilo fica **inline** contra o resto do projeto. Isso é decisão, não descuido: traduzir
para classe foi exatamente o que abriu espaço entre o desenho aprovado e o que sobe — a
primeira Tela 02 foi reescrita à minha maneira e apresentada como tradução, e teve de ser
substituída inteira. O `style-hover` é a única coisa que não atravessa; virou `useHover`.

**Ver a tela no navegador acha o que teste não acha.** Barra de peso plana por escalar sem
piso, pesagem aparecendo como `8.4` cru, microchip em quinze dígitos seguidos — nenhum
apareceria no compilador.

**A leitura fora de transação é o defeito recorrente deste backend: já são quatro defeitos
em três rotas** (feed, agenda de vacinas e acessos, esta última com dois), e o
`LeituraForaDeTransacaoContainerTest` é a guarda — hoje com quatro leituras cobertas, a
quarta preventiva. Ele **serializa** o resultado, não só
chama — a primeira versão só chamava e deixou passar um 500 real. E ele não é
`@Transactional`, ao contrário das vizinhas: a anotação traria os bugs de volta em verde.

## Armadilhas desta máquina, confirmadas de novo

- **O Testcontainers falha na primeira chamada depois de ociosidade e funciona na segunda**
  (`Bad chunk header` no npipe do Rancher). O sintoma é `Skipped: N` com `BUILD SUCCESS`:
  um verde que não testou nada. Quem grita é o `ContainerTestsHabilitadosTest`, mas
  **`-Dtest=` o deixa de fora** — com seleção de teste, conferir `Skipped: 0` sempre.
  **Não reinicie o Rancher:** a máquina tem um cluster k8s com argocd no ar.
- **`cd` dentro do Bash persiste entre chamadas.** Use caminho absoluto.
- **`gh` e `git fetch` perdem a credencial.** `gh auth login` é interativo, tem de ser você.
- **O merge de PR é bloqueado ao Claude pelo classificador.** É comando seu, sempre.
- `clean verify` apaga o `target/` debaixo da aplicação em execução; parar a aplicação exige
  matar o Java à mão.

## Dívida de infra medida e não paga, de propósito

**`.idea/` está versionado** (inclusive `sonarlint/issuestore/`), e por isso cada máquina
gera arquivo novo aparecendo como não rastreado — hoje é o `.idea/aws.xml`. Fica **medido e
registrado**; limpar isso é desvio da prioridade declarada, que é o front.

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

## O primeiro passo da próxima sessão

Depois do merge do #47, **a escolha é sua** e há três caminhos:

1. **Tela 09 (conceder acesso)** — a mais barata das que faltam: o `GrantScope` já existe no
   backend e a Tela 22, que mostra o resultado, já está no ar. Fecha o par.
2. **Telas 07 / 08 + onboarding** — é o caminho de quem chega. Hoje não há como cadastrar um
   animal pela interface.
3. **Decidir o bloco 4** (os cinco buracos) ou a **decisão 15** (quem cria organização e
   como entra o primeiro membro) — sem elas, sete das dezesseis telas não começam.
