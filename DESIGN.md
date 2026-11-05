# Design

O `PRODUTO.md` diz **o que o Petfy é**, o `ROADMAP.md` diz **o que vem e em que ordem**,
o `README.md` diz **o que existe**. Este documento diz **como o produto se apresenta** —
e nasce depois dos três de propósito: direção visual sem produto decidido é gosto pessoal
com nome bonito.

**O que não entra aqui:** componente, biblioteca, arquivo de token, nome de classe. Isso
é código, e vem depois.

**O visual reabriu em 2026-11-03.** O front está sendo refeito do zero, tela por tela, e as
seções de cor, tipografia, objetos e direção (1, 3, 4 e 5) saíram deste documento: valiam para
uma identidade que caiu. A nova mora no código, em `web/src/estilos/novo/tokens.css` e nas
telas já refeitas. Ficam a voz (2), como ponto de partida, e a acessibilidade (6).

## Estado — 2026-08-06

| # | Seção | Status |
|---|---|---|
| 1 | O que este produto parece, e o que não parece | **Reaberta** em 2026-11-03, fora daqui |
| 2 | A voz | **Aprovada** em 2026-08-06; ponto de partida da reconstrução |
| 3 | Cor | **Reaberta** em 2026-11-03, fora daqui |
| 4 | Tipografia e ritmo | **Reaberta** em 2026-11-03, fora daqui |
| 5 | Os objetos que carregam a identidade | **Reaberta** em 2026-11-03, fora daqui |
| 6 | Acessibilidade e idioma | **Aprovada** em 2026-08-06; a validação de campo precisada em 2026-08-07 |

**Documento fechado em 2026-08-06**, e emendado três vezes em 2026-08-07: na regra da série
(5.2), no escopo da v1 (5.5), onde `Insight` e `Update` ficaram registrados como
posteriores, e na validação de formulário (6), quando a construção do i18n mostrou que o
erro de campo não tem como vir do servidor.

**Documento fechado não é documento imutável, e a emenda registra a diferença.** A regra
da série dizia *"o registro de peso carrega o próprio gráfico e a variação"*, e ao ser
construída mostrou que as duas coisas não cabem no mesmo lugar. O que mudou está escrito
no próprio 5.2, com o motivo — a regra antiga fica legível ao lado da nova, que é a mesma
postura que este produto exige dos registros dele (`PRODUTO.md` 5.2: correção é sucessão).

### A primeira versão estava errada, e o erro fica registrado

As seções 1, 3, 4 e 5 nasceram numa direção que foi **reprovada ao ser renderizada**. A
direção original era *"um documento, não um painel — sério sobre o fato, caloroso sobre o
animal"*, com papel quente, serifada para leitura e proibição de cartões. Ela era
coerente com o modelo e **errada para o mercado**: puxava o produto para prontuário e
para ferramenta de gestão, que é exatamente o que o titular não quer vender.

**Duas lições, e as duas custaram uma rodada:**

- **Aprovar cor e tipografia lendo texto não vale.** As seis seções foram aprovadas
  escritas e reprovadas na primeira tela renderizada. Prova visual deixa de ser etapa
  opcional.
- **Referência de fora do nicho não substitui referência de dentro.** NHS e GOV.UK
  resolvem clareza e confiança, e continuam valendo para conteúdo e acessibilidade. Mas
  quem define a **sensação** de um produto de animal são os produtos de animal.

---

## 2. A voz

### A regra que gera todas as outras

> **Fato antes de interpretação. Sempre.**

Não é preferência de escrita: é a promessa 5.5 virando texto. O produto **não diz o que o
animal tem** — diz o que foi observado, quem observou, quando, e o que se pode fazer a
respeito. Toda frase difícil do Petfy se resolve nessa ordem.

### Os três casos difíceis

**O alerta que não alarma** (4.5). A creche viu algo e o tutor precisa saber, sem entrar
em pânico no trabalho.

| Não | Sim |
|---|---|
| ⚠️ Atenção! O Code pode estar com dor! | O Code mancou da pata direita hoje de manhã. *Registrado por Marina, Creche Quintal, às 9h20.* |
| Suspeita de problema alimentar | O Code não comeu hoje. É a segunda vez esta semana. **Vale procurar o veterinário.** |

**A percepção que não diagnostica** (3.15, 5.5). O produto viu um padrão que ninguém
registrou.

| Não | Sim |
|---|---|
| O Code pode estar com problema renal | Este é o terceiro episódio urinário do Code em 18 meses. **Ver os três** · **Dispensar** |

Toda percepção mostra a evidência e pode ser dispensada, e o texto diz as duas coisas.
Percepção sem evidência à mão é opinião — e opinião o Petfy não tem.

**O erro que não culpa.** Quem errou já está frustrado; a cópia não aumenta isso.

| Não | Sim |
|---|---|
| E-mail inválido | Esse e-mail não parece completo — falta o `@` |
| Acesso negado | Você não tem acesso a este animal. Quem responde por ele pode conceder |
| Erro 500 | Não conseguimos salvar agora. O que você escreveu não se perdeu — tente de novo em instantes |

**E uma regra de segurança que é de texto, não de código:** a mensagem nunca revela o que
a pessoa não pode ver. O backend responde `404` para animal fora do alcance justamente
para não confirmar que ele existe — e a tela não pode desfazer isso dizendo "esse animal
é de outro tutor".

### Como o produto chama as coisas

**O animal tem nome, e o produto usa.** "O Code", nunca "seu pet". É a coisa mais barata
que existe para o produto parecer que conhece o animal — e ele conhece.

**Proibido infantilizar.** Nada de "peludinho", "au-au", "mamãe do Rex", "anjinho de
quatro patas". O tutor é adulto, o veterinário está trabalhando, e o assunto é saúde.
Ternura vem de chamar o Code pelo nome, não de diminutivo.

**Você, sempre.** Nas duas áreas. A voz não muda entre tutor e profissional — o que muda
é o vocabulário, porque o do profissional é mais preciso e ele merece precisão.

**O vocabulário canônico é o do modelo, e a tela traduz sem distorcer.** A seção 3 do
`PRODUTO.md` é a fonte; onde a palavra exata pesar para o tutor, traduz-se — nunca se
inventa sinônimo que mude o sentido.

| Conceito (3.x) | Ao tutor | Ao profissional |
|---|---|---|
| Custódia | Você responde pelo Code | Custódia |
| Acesso concedido | Quem pode ver o Code | Acesso |
| Vínculo | Onde o Code é atendido | Vínculo / paciente / aluno |
| Orientação | O que fazer | Orientação |
| Pendência | O que precisa da sua atenção | Pendência |
| Ato clínico | Registro do veterinário | Ato clínico |

**Regra dura:** a mesma coisa nunca tem dois nomes no produto. Se a tela chamar de
"compartilhamento" o que o modelo chama de acesso, o suporte, a documentação e o código
passam a falar três idiomas.

### Como se dá uma notícia ruim

Vencido, alerta, adoção que não foi aceita, animal perdido. Em todos:

1. **O fato, primeiro, em uma linha.** *A vacina antirrábica do Code venceu há 3 dias.*
2. **A consequência, se houver, sem drama.** *A matrícula na creche fica pendente até a
   atualização.*
3. **O que fazer, com o caminho.** *Registrar a dose · Falar com a clínica.*

**Nunca:** maiúscula gritando, exclamação, contagem regressiva, vermelho em texto
corrido. Urgência é escassa (4.2) — e o produto que grita todo dia é o produto que é
silenciado na primeira semana.

### O que a voz nunca faz

- **Não promete o que não pode.** Nada de "seu pet sempre protegido".
- **Não vende.** Nem produto nosso, nem de terceiro (5.6).
- **Não usa jargão jurídico onde cabe português.** Consentimento e exportação se explicam
  em uma frase que a pessoa entende; o texto legal existe, e fica ao lado, não no lugar.
- **Não faz piada nem trocadilho.** Além de envelhecer mal, não sobrevive à tradução — e
  i18n é decisão de dia um (Fase 5).
- **Não usa texto dentro de imagem.** Texto é dado: precisa ser traduzível, buscável e
  legível por leitor de tela.

---

## 6. Acessibilidade e idioma

### O piso, e por que não é opcional aqui

**WCAG 2.2 nível AA, em toda tela, desde a primeira.** Não é conformidade por
conformidade — é consequência de quem usa:

- **O tutor de 70 anos** com o cachorro de 14 é o usuário mais fiel que este produto vai
  ter, e o que menos enxerga letra pequena.
- **A área de organização é usada oito horas por dia**, no teclado, por gente com as mãos
  ocupadas em outra coisa.
- **O cartão é lido por um estranho, numa emergência**, no celular dele, talvez no sol,
  com pressa. É a tela mais exigente do produto e a única sem conta para dar contexto
  (9.2).

### O que isso obriga

**Contraste conferido, não estimado.** Os tokens trazem o par medido ao lado de cada cor
(`web/src/estilos/novo/tokens.css`). Regra que fica: **nenhuma cor entra na paleta sem o número**, e o par de urgência
sobre o próprio fundo é sempre o primeiro a conferir — foi o único que reprovou na
primeira medição.

**Cor nunca sozinha.** Repetido de propósito: todo estado tem palavra, forma ou posição
além da cor.

**Foco visível, e ele é nosso.** Anel de 2 px em tinta, com 2 px de deslocamento — nunca o
azul padrão do navegador, nunca removido, nunca só mudança de fundo.

**Teclado em tudo, e a área de organização é teclado primeiro.** Toda ação alcançável sem
mouse; ordem de tabulação seguindo a leitura. Cadastro em série no balcão é digitação, não
clique.

**Estrutura real para leitor de tela.** A linha do tempo é lista com títulos de verdade,
datas em elemento de tempo, e a autoria lida **junto** do fato — não pulada como enfeite.
É o principal motivo de o componente ser headless (Fase 5): sopa de `div` estilizada não
tem como ser corrigida depois.

**Iniciais precisam de nome.** O avatar com "AL" é decoração para quem não enxerga: o nome
de quem registrou está no texto, e a marca é `aria-hidden`.

**Alvo de toque de 44 px.** Confirmar cumprimento é o gesto mais repetido do produto, e vai
acontecer com uma mão, com o cachorro na outra.

**Movimento discreto e respeitando preferência.** Nenhuma animação carrega significado.

**Formulário com rótulo sempre visível.** Placeholder não é rótulo — some quando se digita,
e é onde o erro nasce. Mensagem de erro amarrada ao campo, escrita como manda a seção 2.

> **Acrescentado em 2026-08-07, ao construir a camada de i18n.** A mensagem por campo tem
> de ser **nossa, e antes de enviar** — não é escolha de sofisticação. Os erros da API
> viajam com código numérico, e o front traduz por ele; mas o erro de **validação de
> campo** não tem código: o servidor devolve `campo: motivo`, com o nome do campo em
> inglês. Não é exibível nem pela regra de nunca mostrar a mensagem do servidor, nem por
> esta seção. Então a validação de campo é do cliente, e o erro que voltar do servidor
> vira uma frase genérica — rede de segurança, não a mensagem que a pessoa lê.

**O gráfico de série tem texto ao lado.** A variação é dita em palavras — "subiu 12% em 6
meses" —, porque a linha sozinha não é legível por leitor de tela nem por quem não
distingue o traço.

### O que o idioma cobra

**Texto é dado.** Nunca dentro de imagem, nunca montado por concatenação. Frase quebrada em
pedaços não sobrevive a nenhuma tradução.

**Data é uma coisa; instante é outra.** A vacina foi aplicada num **dia**; a observação da
creche aconteceu num **instante**. Data se mostra como data; instante se apresenta no fuso
de quem lê. Confundir os dois faz a linha do tempo trocar a ordem dos fatos para quem
viaja — e ordem é o produto.

**Concordância com o animal.** *"O Code passou a ser seu"* e *"A Nina passou a ser sua"*. O
produto sabe o sexo do animal e precisa acertar o artigo. Onde o sexo não for conhecido, a
frase se reescreve para não precisar dele; nunca se chuta.

**Plural e contagem** passam pela regra do idioma, não por `s` no fim.

**Espécie, raça e catálogo são conteúdo traduzível**, não literal de código.

**Direção da direita para a esquerda não entra agora** — mas nada trava o caminho: onde
existir propriedade lógica de início e fim, ela é usada, em vez de esquerda e direita
fixas.

### O que fecha uma tela

- [ ] Contraste conferido, tema claro **e** escuro
- [ ] Navegável só com teclado, com foco visível o tempo todo
- [ ] Lida por leitor de tela, com autoria e data no lugar certo
- [ ] Alvos de toque a partir de 44 px
- [ ] Suporta 40% de crescimento de texto sem quebrar
- [ ] Nenhum significado transmitido só por cor
- [ ] Passa os três testes da seção 1
