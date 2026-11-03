# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-11-03, na sessão que **decidiu refazer o front do zero** e fechou a primeira
> tela da reconstrução: a página pública `/conheca`.

## Onde o trabalho está agora

O backend não mudou: **1075 casos, migrations até a V52**. O front mudou de rumo.

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

## COMECE POR AQUI

1. **Fechar a Fase 0**:
   - escrever `docs/design-ferramentas.md` e `docs/reconstrucao-do-front.md` (a análise
     aprovada, e o inventário e a ideia que hoje estão em `.taste-skill/notas.md`, fora do git);
   - apagar `design/` e os comentários "Tela NN" do código;
   - tirar do `DESIGN.md` as seções 1, 3, 4 e 5, com a nota de que reabriram (a 6, acessibilidade,
     fica; a 2, a voz, fica como ponto de partida);
   - **zerar o banco e semear dados de exemplo honestos**: a Pipoca e mais dois ou três animais,
     com foto, vacinas, registros assinados por pessoas diferentes e acessos concedidos.
2. **A próxima tela é "a vida do animal"** (`/animais/{id}`), regime app com rosto. É ela que
   prova se a identidade sobrevive dentro do app. O cadastro vem depois, na Fase 2.

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
- **`TaskStop` não mata o backend**: matar pelo PID que segura a 8080.
- **Rota ou código de erro novo cobra três artefatos no mesmo commit**: `contract/openapi.json`,
  `web/src/dados/gerado/api.d.ts` e a tradução `erro.<n>`.
- **`vite build` suja o `arvore-de-rotas.gen.ts`**; parar o dev server antes de sincronizar.
- **O banco de teste não é limpo entre casos.**
- **A conta ativa do `gh` é a de trabalho**: `gh auth switch --user UlyssesLopes`.
- **O merge de PR é comando do Ulysses, sempre.**

## Como subir, inspecionar e conferir

```
docker start petfy-pg-sessao
$env:DB_PORT="5433"; mvn spring-boot:run "-Dspring-boot.run.profiles=local"
cd web; npx vite build; npx vite preview --port 4173      # http://localhost:4173/conheca
```

```
node .claude/skills/design-taste-frontend/scripts/lint.mjs web/src/rotas/conheca.tsx web/src/estilos/novo
node .claude/skills/design-taste-frontend/scripts/inspect.mjs http://localhost:4173/conheca --out .taste-skill/inspect
node .claude/skills/design-taste-frontend/scripts/draw.mjs --show     # a folha da direcao
playwright-cli open http://localhost:4173/conheca                     # clicar e fotografar
```

O inspector tem de rodar sobre o **build de produção**, e não sobre o dev server.
