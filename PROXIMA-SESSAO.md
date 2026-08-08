# Próxima sessão

> **Este arquivo é descartável e sobrescrito a cada sessão.** Ele é o estado de agora,
> não registro histórico — o que vale para sempre mora no `ROADMAP.md`, no `PRODUTO.md`
> e no `DESIGN.md`. Se este arquivo divergir dos três, **eles mandam**.
>
> Escrito em 2026-08-08, no fim da sessão que pôs a home do tutor no ar.

## Onde o trabalho parou

**Passo 5 da Fase 5 — construir.** O `pronto quando` da fase **fechou**: um tutor entra
pelo navegador, vê o que precisa fazer hoje, registra uma dose e abre a linha do tempo.

| # | Bloco | Estado |
|---|---|---|
| B0 | CORS e caminho de auth | **Feito** — PR #40 |
| 1 | Fundação do front | **Feito** — PR #42 |
| 2 | Home do tutor, com login | **Feito** |
| 3 | Linha do tempo | **Feito** |
| 4 | Rede de quem cuida | **Feito** (leitura; conceder acesso ainda não age) |
| 5 | Área de organização | **Não iniciado** — depende da decisão 15 |

**Suíte:** 765 testes, 0 falhas, 0 pulados. Front: 23 testes.

## O que ler antes de planejar, e nesta ordem

1. **`ROADMAP.md`, "Passo 5 — construir"**, com as três seções de fechamento novas:
   *Blocos 2, 3 e 4*, *Duas decisões de produto* e *As dívidas que a construção levantou*.
2. **`DESIGN.md`** inteiro, mais `design/home-tutor.html`. A emenda da seção 6 registra que
   **a validação de campo é do cliente**, porque a do servidor não é exibível.
3. **`PRODUTO.md` seção 9** — as três superfícies e as duas áreas.
4. **`contract/openapi.json`** e **`contract/error-codes.json`**, os dois versionados e com
   guarda dos dois lados.

## O primeiro passo da próxima sessão, e há três candidatos

**Nenhum é obviamente o certo — a escolha é sua.**

1. **Pagar as dívidas de contrato** (tabela no `ROADMAP.md`). A mais barata e a que mais
   rende: `spring.jackson.default-property-inclusion=non_null` faz o tipo gerado deixar de
   mentir sobre nulo. Hoje a mentira para no `corpoDe()` do front, o que é um remendo bom
   e no lugar errado.
2. **Fechar o que a tela ainda não faz.** *Conceder acesso* aparece na rede mas não age;
   não há como cadastrar um animal nem convidar co-tutor. São as três ações que faltam
   para a área do tutor ser autossuficiente — e são o destino do onboarding da v2.
3. **A área de organização** (bloco 5), que **depende da decisão 15** — quem cria uma
   organização e como entra o primeiro membro. Sem resposta, não começa.

## O que fica no ar, e como subir

O ambiente local **não sobe sozinho**, e a máquina tem duas armadilhas próprias:

- **Um PostgreSQL nativo do Windows já ocupa a 5432.** A aplicação fala com ele e toma
  `password authentication failed`. Suba o banco noutra porta:
  `docker run -d --name petfy-pg-sessao -e POSTGRES_DB=petfy -e POSTGRES_USER=petfy -e POSTGRES_PASSWORD=petfy -p 5433:5432 postgres:14`
  e rode a aplicação com `DB_PORT=5433`.
- **O volume `postgres_data` do `docker-local` tem um cluster antigo**, sem os roles
  `petfy` nem `postgres`: o Postgres só aplica `POSTGRES_USER` na primeira inicialização,
  com o diretório vazio. **Não foi apagado** — é decisão de quem for limpar.

```
docker run -d --name petfy-pg-sessao ... -p 5433:5432 postgres:14
DB_PORT=5433 mvn spring-boot:run -Dspring-boot.run.profiles=local
cd web && npm run dev      # http://localhost:5173
```

**Parar a aplicação exige matar o processo Java à mão.** Encerrar o Maven deixa o filho
vivo segurando a 8080, e a subida seguinte falha com *"Port 8080 was already in use"*.

## Armadilhas confirmadas nesta sessão

- **O `clean verify` apaga o `target/` embaixo da aplicação em execução.** Derrube antes.
- **Acento pelo Git Bash sai em Latin-1** e o backend responde **500** — que é outra
  dívida: JSON malformado devia ser `400`. Mande corpo com `--data-binary @arquivo`.
- **PowerShell quebra `-D` com ponto.** Use aspas: `mvn "-Dpetfy.openapi.update=true"`.
- **`Select-Object -First N` corta o pipeline** e devolve exit 255 com o Maven bem-sucedido.
- **`Set-Content -Encoding utf8` escreve BOM** e o `javac` recusa.
- **Em `pull_request`, o filtro por caminho olha o diff do PR inteiro**, e não o último
  commit: num PR misto os dois pipelines correm a cada push, inclusive num commit só de
  `.md`. O ganho aparece em PR focado.

## Como regenerar o que é gerado

```
mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true
mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true
cd web && npm run gerar:api
```

Os três entram no **mesmo commit** da mudança que os causou.
