# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-11-03 e atualizado em 2026-11-04, depois da segunda tela da reconstrução:
> **a vida do animal** (`/animais/{id}`), e as duas portas: `/criar-conta` e `/entrar`.

## Onde o trabalho está agora

Backend: **1090 casos, migrations até a V53** (a foto do animal). O front mudou de rumo.

**O Ulysses reprovou o front inteiro**, em 2026-11-03: *"não consegui dar cara de aplicativo de
pet pro meu produto, parece que não consegui dar vida pra ele"*. A reconstrução é **do zero**:

- **A identidade do Claude Design caiu inteira**: paleta musgo/ocre/telha, tipografia, pílulas.
  As telas de `design/IdentidadeVisual/` **deixaram de ser a especificação**.
- **Nenhum print ou modelo do projeto serve de base**: nem `design/`, nem o front atual
  renderizado. Do código atual sai só **o que cada tela faz**, nunca como ela é.
- **Ficam de pé**: a tese e as quatro invariantes (são produto), `src/dados/`, o contrato
  gerado, o i18n, as guardas de rota e o endereço `/cartao/{token}` (está impresso no QR).

## O método

O mesmo que deu certo na loja `japanese-commerce`: as skills de `.claude/skills/`
(`design-taste-frontend`, `web-design-guidelines`, `playwright-cli`) mais um processo.

1. **Ordem de autoridade**: invariantes e promessas do `PRODUTO.md` → acessibilidade (WCAG AA)
   → o `DESIGN.md` novo → a Taste → o gosto do modelo.
2. **Três regimes de tela**: página (a porta, o cartão, o ano do animal), app com rosto (a área
   do tutor) e app puro (a área de organização, modo `--app` da Taste).
3. **O ciclo de cada tela**: texto antes do markup → tokens → `inspect.mjs` em 1440/768/390 →
   **olhar as capturas** e corrigir os 3 piores (no máximo 3 rodadas) → auditoria da
   `web-design-guidelines` → clicar de verdade com o `playwright-cli` → mostrar ao Ulysses.
4. **Tela a tela, um PR por tela.** O CSS antigo convive isolado até a última tela velha sair.

**A análise completa (seis seções) foi aprovada nesta sessão e ainda não está escrita em
documento**: é o primeiro item da Fase 0 que falta.

## O que foi feito: a página pública

`/conheca`, aprovada pelo Ulysses ("por hora aprovado"), depois de seis rodadas de ajuste dele.

- **A ideia**: a medalha da coleira, o disco furado que leva o nome do animal, é a forma de tudo.
  Direção `one-motif`, seed `375332394`; hero e paleta ressorteados uma vez, com motivo.
- **Cobalto sobre areia**, Chillax (Fontshare) como família única. Os tokens moram em
  `web/src/estilos/novo/tokens.css`, com o contraste medido ao lado de cada cor, e só valem
  dentro de `.pf`.
- **Uma história só, a da Pipoca**: os dados de exemplo fecham entre as seções (a alergia do
  cartão é a do banho no petshop; o reforço da V10 é o que deixa a vacina "em dia até abr 2026").
- **O que o Ulysses corrigiu, e vale como gosto dele**: seção que muda de tamanho ao clicar é
  defeito (o cartão cresce dentro de um espaço reservado); informação "jogada no fundo" sem
  objeto que a amarre é defeito; botão tem de parecer botão (furo, seta, movimento); fotos de
  animal com rosto centrado e fundo limpo; lista vertical solta não serve.

**O `/` continua levando ao login.** A página só existe em `/conheca`.

## O que foi feito: a vida do animal

Branch `feat/a-foto-do-animal`, a primeira tela do regime "app com rosto".

- **A foto do animal virou dado**: V53 (`animals.photo_attachment_id`), `PUT /animals/{id}/photo`
  (só anexo de imagem do próprio animal), e o cadastro já define a foto que sobe.
- **Três defeitos de produção da mesma família**, que só apareceram com dado de verdade:
  `LazyInitializationException` em `Organization.capabilities` e em `Grant.scopes`, e organização
  nascendo sem capacidade nenhuma. Cada um com teste de container que o reproduzia antes.
- **A semente** `docker-local/semente/semear.mjs` cria pela API ana, luiza (CRMV), joana e rafael
  (senha `Petfy#2026`), três organizações e Pipoca, Mia e Nino com foto, vacinas, condições,
  peso, registros e acessos.
- **A tela**: cabeçalho com a medalha e o fato do dia (vacina vencida ou perto de vencer),
  vacinas, condições, peso com gráfico, a linha da vida e quem cuida. O menu do tutor
  (`MolduraDoTutor`) é um painel cobalto da altura da tela, com os animais e suas fotos.
- **O que o Ulysses corrigiu no menu**: letra de título em menu é defeito; painel estreito
  "embutido" no fundo pareceu desconexo. Ficou letra de texto, painel cheio, grupos com rótulo.

## COMECE POR AQUI

1. **Fechar a Fase 0**:
   - escrever `docs/design-ferramentas.md` e `docs/reconstrucao-do-front.md` (a análise
     aprovada, e o inventário e a ideia que hoje estão em `.taste-skill/notas.md`, fora do git);
   - apagar `design/` e os comentários "Tela NN" do código;
   - tirar do `DESIGN.md` as seções 1, 3, 4 e 5, com a nota de que reabriram (a 6, acessibilidade,
     fica; a 2, a voz, fica como ponto de partida);
2. **As portas estão feitas** na branch `feat/criar-conta` (empilhada no #76: o PR dela só
   depois do merge do #76). Folha de areia com o formulário e painel cobalto com a plaquinha
   (`componentes/novo/Plaquinha.tsx`, estilos em `novo/porta.css`). No cadastro a plaquinha tem
   frente (o nome do animal, que agora vem da `/conheca` e chega preenchido em `/animais/novo`)
   e verso (primeiro nome e e-mail), e gira. No entrar, o painel é a saída de emergência: o
   cartão sem senha e o "achei um animal".
   **O que o Ulysses corrigiu**: medalha tem de parecer medalha (borda, furo, argola); falar de
   frente e verso exige poder virar; o verso leva contato em letra pequena, não um nome gigante.
3. **A próxima tela é o cadastro do animal** (`/animais/novo`), para onde as duas portas levam.

## O que ficou devendo

- **Os 5 avisos do inspector na `/conheca`**: `card-rows` e `palette-cream` são defensáveis;
  `no-footage` foi uma escolha; `size-count` dá para enxugar; **`off-centre` (36 contra 86 px)
  não tem a origem achada**.
- **As fotos pesam 3,1 MB** e não foram otimizadas; a fonte vem do CDN da Fontshare.
- **A seta do "agora" se move sem parar.** A `web-design-guidelines` pede controle para movimento
  que repete por mais de 5 s; ficou assim por pedido do Ulysses.
- **O texto jurídico**: sete seções de `/termos` e `/privacidade` seguem marcadas como pendentes.
- **`RotuloDoAparelho` (Java) e `apelidoDoAparelho` (TS)** continuam gêmeos.
- **A branch `docs/o-estado-para-a-proxima-sessao`** pode ser descartada.

## Armadilhas desta máquina

- **O `!` do prompt roda Bash, e não PowerShell.** Comando para o Ulysses rodar vai em Bash.
- **Escrever em `.claude/` é bloqueado** para o agente; instalar ou atualizar skill é comando do
  Ulysses. Ao atualizar a Taste, reler os scripts antes de commitar: a permissão do
  `settings.json` vale para o que estiver na pasta.
- **Heredoc longo com crase no Bash quebra.** Para trocar um bloco grande de arquivo, gravar o
  bloco novo em arquivo e emendar por marcador.
- **`.pf-pagina p { margin: 0 }` atropela as margens de cada seção** por especificidade; o reset
  é `:where(...)`, de peso zero.
- **A 8080 e a 5433 são da loja `japanese-commerce`.** Nunca parar o java da 8080 nem tocar no
  `zivora-db-local`. O Petfy local roda na 8081 com o banco `petfy-pg-local` na 5434.
- **`TaskStop` não mata o backend**: matar pelo PID que segura a 8081.
- **Rota ou código de erro novo cobra três artefatos no mesmo commit**: `contract/openapi.json`,
  `web/src/dados/gerado/api.d.ts` e a tradução `erro.<n>`.
- **`vite build` suja o `arvore-de-rotas.gen.ts`**; parar o dev server antes de sincronizar.
- **O banco de teste não é limpo entre casos.**
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **O merge de PR é comando do Ulysses, sempre.**

## Como subir, inspecionar e conferir

```
docker start petfy-pg-local
DB_PORT=5434 SERVER_PORT=8081 CORS_ALLOWED_ORIGINS=http://localhost:4173 mvn spring-boot:run "-Dspring-boot.run.profiles=local"
PG_CONTAINER=petfy-pg-local node docker-local/semente/semear.mjs   # só com o banco vazio
cd web; VITE_API_URL=http://localhost:8081 npx vite build; npx vite preview --port 4173
```

```
node .claude/skills/design-taste-frontend/scripts/lint.mjs web/src/rotas/conheca.tsx web/src/estilos/novo
node .claude/skills/design-taste-frontend/scripts/inspect.mjs http://localhost:4173/conheca --out .taste-skill/inspect
node .claude/skills/design-taste-frontend/scripts/draw.mjs --show     # a folha da direcao
playwright-cli open http://localhost:4173/conheca                     # clicar e fotografar
```

O inspector tem de rodar sobre o **build de produção**, e não sobre o dev server.
