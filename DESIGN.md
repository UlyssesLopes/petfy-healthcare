# Design

O `PRODUTO.md` diz **o que o Petfy é**, o `ROADMAP.md` diz **o que vem e em que ordem**,
o `README.md` diz **o que existe**. Este documento diz **como o produto se apresenta** —
e nasce depois dos três de propósito: direção visual sem produto decidido é gosto pessoal
com nome bonito.

**O que não entra aqui:** componente, biblioteca, arquivo de token, nome de classe. Isso
é código, e vem depois.

## Estado — 2026-08-06

Documento em construção, aprovado **seção a seção**, no mesmo regime do `PRODUTO.md`.

| # | Seção | Status |
|---|---|---|
| 1 | O que este produto parece, e o que não parece | **Aprovada** em 2026-08-06 |
| 2 | A voz | **Aprovada** em 2026-08-06 |
| 3 | Cor | **Aprovada** em 2026-08-06 |
| 4 | Tipografia e ritmo | **Aprovada** em 2026-08-06 |
| 5 | Os quatro objetos que carregam a identidade | **Aprovada** em 2026-08-06 |
| 6 | Acessibilidade e idioma | **Aprovada** em 2026-08-06 |

**Documento fechado em 2026-08-06.** As seis seções estão aprovadas.

**Retomar em:** o passo 4 da Fase 5 — o delta de contrato.

As referências que originaram este documento estão na Fase 5 do `ROADMAP.md`, e migram
para cá conforme cada seção as usa.

---

## 1. O que este produto parece, e o que não parece

### A direção, em uma frase

> **Um documento, não um painel — sério sobre o fato, caloroso sobre o animal.**

**De onde ela sai, e não é gosto.** O objeto central do produto é a linha do tempo:
eventos com autor, instante e responsabilidade, que não se apagam e se corrigem por
sucessão visível (3.9, 3.10, 5.2). Isso é a natureza de um **documento**, não de um
painel de métricas. Um produto cujo centro é um registro e que se apresenta como
dashboard mente sobre si mesmo na primeira tela.

**O que "documento" significa na prática:**

- **A estrutura é lista e régua**, não cartão flutuante. Hierarquia vem de tipografia,
  espaço e linha — os três recursos que um documento sempre teve.
- **Cada fato mostra quem o afirmou.** Autoria não é metadado escondido num tooltip: é
  parte do evento, porque é o que separa registro de caderno (5.7).
- **O tempo é o eixo**, e é o tempo do fato, não o da digitação (3.9).
- **Nada some.** Correção aparece ao lado do corrigido, não no lugar dele — a promessa
  5.2 tem forma visual, e é essa.

**O calor não vem da forma — vem da foto e da fala.** Isso importa: calor por decoração
(pastel, canto redondo, ilustração) barateia o registro clínico, e é exatamente o que faz
um veterinário não levar o produto a sério. O que aquece é o animal na tela — a foto que
o tutor pôs, a foto que a creche mandou — e um português que fala com gente. **A
estrutura é séria; o conteúdo é que é afetivo.**

### A tensão entre as duas áreas, e como se resolve

A área do tutor é um animal em profundidade, lida no sofá à noite. A de organização é
centenas em largura, lida no balcão às 9h. **Não são dois visuais — é o mesmo sistema em
duas densidades.** A área de organização comprime espaço e aumenta informação por linha;
não muda cor, tipo nem forma. Se as duas parecerem produtos diferentes, a 9.3 foi
desfeita na camada visual, do mesmo jeito que um `if (isVet)` a desfaria.

### O que este produto não é, visualmente

A lista é dura de propósito — é ela que impede a cara de template.

| Proibido | Por quê |
|---|---|
| **Cartão branco flutuante em grade de três** | É a estrutura padrão de todo projeto gerado, e fragmenta o que é contínuo. A linha do tempo não é um mural |
| **Sombra como hierarquia** | Hierarquia é tipo, espaço e régua. Sombra é decoração fingindo função |
| **Gradiente** | Nenhum, em lugar nenhum |
| **Cinza puro de framework** (slate, zinc) | Neutro é decisão, e a decisão está na seção 3 |
| **Canto arredondado como assinatura** | Raio existe, é pequeno e é constante. Não é personalidade |
| **Ilustração genérica e emoji como ícone** | Envelhecem em um ano e somem no dia em que alguém precisa de uma informação séria |
| **Mascote** | O protagonista é o animal do usuário. Um mascote nosso compete com ele — e perde |
| **Foto de banco de imagem** | Toda foto do produto é de um animal real de um usuário real. Cachorro sorridente de stock é a mentira visual mais barata que existe |
| **Celebração de ato de saúde** (confete, "parabéns!") | Registrar uma vacina não é conquista, é cuidado. A regra de 5.5 vale nos dois sentidos: o produto não alarma, e também não comemora |
| **Métrica grande sem dono na área do tutor** | Ele não tem indicador. Ele tem um animal |

### O teste que decide caso novo

Duas perguntas, nesta ordem:

1. **Isto aponta para uma promessa da seção 5 ou para um objeto da seção 3?** Se não
   aponta para nenhum dos dois, é decoração — e decoração não entra.
2. **Um print desta tela poderia ser de qualquer outro SaaS?** Se poderia, ainda não é o
   Petfy.

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

## 3. Cor

### A decisão

**Papel quente e tinta escura, com um verde-arquivo como única cor de identidade.** Nada
de branco puro, nada de cinza de framework, nada de azul de SaaS.

**Por que papel e não branco.** `#FFFFFF` com cinza `slate` é o par que todo projeto novo
herda, e é frio como prontuário de hospital — exatamente a sensação que a seção 1 quer
evitar. Um off-white quente faz a mesma tela parecer documento cuidado em vez de
formulário. O calor entra pela superfície, não pela decoração.

**Por que verde, e qual verde.** Não o verde-menta de farmácia nem o verde-neon de saúde
digital: um **verde escuro, dessaturado, de encadernação**. Diz vida e cuidado sem gritar
clínica, e sobrevive ao lado de qualquer foto de animal. E descarta de saída as três
assinaturas mais reconhecíveis do genérico — o índigo/violeta que é a cara de projeto
gerado, o azul corporativo e o roxo de fintech.

### Os âncoras

Direção, não arquivo de token. A conferência de contraste é condição para virarem token.

| Papel | Claro | Escuro | Uso |
|---|---|---|---|
| Superfície | `#FAF7F2` | `#14120F` | O fundo de tudo |
| Superfície elevada | `#F3EEE6` | `#1D1A16` | Faixa, cabeçalho fixo, linha destacada |
| Régua | `#E2DAD0` | `#2E2A24` | A linha que separa — o recurso estrutural principal |
| Tinta | `#191512` | `#F2EDE5` | Texto |
| Tinta secundária | `#6B625A` | `#A79E93` | Autoria, data, o que acompanha o fato |

| Significado | Claro | Escuro | Reservado a |
|---|---|---|---|
| Verde-arquivo | `#1F4A3D` | `#6FA88F` | Identidade e ação primária |
| Âmbar | `#A9701A` | `#D9A441` | A vencer, pendente, aguardando resposta |
| Vermelho-óxido | `#9B2C1E` | `#D9705C` | Vencido, urgente de verdade |

O vermelho é óxido e não `#EF4444` de propósito: o vermelho puro de framework é
estridente sobre papel quente, e estridência é o oposto de "não alarmar".

### Cinco regras

**1. Cor de estado nunca vira cor de ação.** Âmbar e vermelho aparecem em marca pequena —
ponto, etiqueta, régua lateral —, jamais em botão. Botão é verde-arquivo ou tinta. Um
botão vermelho e um alerta vermelho na mesma tela ensinam a pessoa a ignorar os dois.

**2. Vermelho é escasso, e tem dono.** Só o que está de fato vencido ou urgente. **O
alerta da creche não é vermelho** — é observação, não emergência nem diagnóstico (3.11,
4.5). Pintá-lo de vermelho é a tela fazendo a interpretação clínica que o produto se
proibiu de fazer.

**3. Cor nunca é o único portador de significado.** Todo estado tem também palavra, forma
ou posição. Sem isso, oito por cento dos homens não enxergam o produto — e "vencido" é
informação de saúde.

**4. A foto do animal é a coisa mais colorida da tela.** A interface não compete com ela.
É o que faz o calor vir do conteúdo, como a seção 1 decidiu.

**5. Percepção não usa cor semântica.** Ela não é fato (3.15), então não pode se vestir de
alerta. Distingue-se por tipografia e régua, nunca por âmbar ou vermelho.

### O escuro não é inversão

É um segundo papel. Superfície quase-preta **quente**, nunca `#000000`; tinta que não é
branco puro; verde clareado para manter contraste. Inverter o claro produz aquele
cinza-azulado morto que denuncia tema escuro feito às pressas — e metade do uso do tutor
é à noite, no sofá, que é justamente quando o produto precisa parecer bem-feito.

## 4. Tipografia e ritmo

### O par, e o motivo

**Serifada para o registro, sem serifa para a interface.** Uma superfamília, para as duas
conversarem sem esforço: **Source Serif 4** e **Source Sans 3**.

| Onde | Fonte | Por quê |
|---|---|---|
| O conteúdo do registro — texto do evento, nome do animal, títulos | **Source Serif 4** | Ler uma vida inteira em lista é leitura de verdade, e serifada é o que se lê. É também o que faz a tela parecer documento e não painel |
| A interface — rótulo, botão, tabela, formulário, navegação | **Source Sans 3** | Neutra, alta legibilidade em tamanho pequeno, e não disputa atenção com o registro |
| Identificador — microchip, RGA, tatuagem | **Source Code Pro**, e só aí | Sequência que se confere caractere a caractere. Em nenhum outro lugar |

**Por que não Inter.** É a fonte assinatura do projeto gerado — a seção 1 proíbe
justamente essa assinatura. Não é problema da fonte; é problema de ela ser o default de
todo mundo.

**Numerais tabulares em tudo que alinha:** data, peso, dose, contagem. É detalhe pequeno
e é o que separa registro de post — série de peso com números que dançam de linha em
linha não parece dado, parece texto.

### A escala

Sete degraus, e não mais. Cada um com dono.

| Degrau | Tamanho | Entrelinha | Dono |
|---|---|---|---|
| Nome do animal | 32 px | 1.2 | Serifada. É o título de todo o registro |
| Título de seção | 24 px | 1.25 | Serifada |
| Subtítulo | 19 px | 1.35 | Serifada |
| Texto do registro | 17 px | 1.55 | Serifada. É o que se lê de verdade |
| Interface | 15 px | 1.45 | Sem serifa. Rótulo, botão, célula |
| Apoio | 13 px | 1.4 | Autoria, data, contexto — em tinta secundária |
| Etiqueta | 12 px | 1.3 | Estado, marca, unidade. Nunca texto corrido |

**Pesos: três.** Regular, medium, semibold. Nada de light — ilegível sobre papel quente
—, nada de black. Se uma hierarquia precisar de um quarto peso, ela está errada em outro
lugar.

**Medida de leitura: 60 a 75 caracteres.** A linha do tempo é leitura, e linha longa
demais é onde o olho se perde no retorno.

### O ritmo

**Base de 4 px**, e uma escala curta: 4, 8, 12, 16, 24, 32, 48, 64.

**Regra dura: agrupamento é feito com espaço e régua, nunca com sombra ou caixa.** É a
seção 1 virando mecânica. O que pertence junto fica perto; o que separa é uma linha de
1 px na cor de régua. Um documento bem composto nunca precisou de cartão flutuante para
dizer o que anda junto.

**Duas densidades, um sistema.**

| | Área do tutor | Área de organização |
|---|---|---|
| Texto de registro | 17 px | 15 px |
| Espaço entre itens | 16–24 px | 8–12 px |
| Altura de linha em lista | Confortável | Compacta |
| Fonte, cor, régua, raio | **Idênticos** | **Idênticos** |

Muda o espaço e o tamanho. Não muda mais nada — se mudar, viraram dois produtos, e a 9.3
se desfaz.

### Alinhamento

- **Texto alinhado à esquerda**, sempre. Justificado nunca, centralizado só em estado
  vazio e no cartão, que é objeto único lido no celular.
- **Número alinhado à direita** em tabela, com numeral tabular.
- **Um eixo vertical por tela.** Rótulo, texto e ação começam na mesma coluna — é o que
  dá a sensação de documento composto em vez de tela montada.
- **Raio de 4 px**, constante. Existe para não ter canto vivo, não para ter personalidade.

### O que o idioma cobra

Português é longo, e alemão é mais. **Nada de largura fixa em rótulo, botão ou coluna** —
o que couber em `pt-BR` tem de caber quando crescer 40%. É a consequência de i18n na
primeira tela ser decisão de dia um, e é barata agora e cara depois.

## 5. Os quatro objetos que carregam a identidade

Se estes quatro estiverem certos, o resto do produto se desenha sozinho. Se algum estiver
errado, nenhuma paleta salva.

### 5.1 A linha do tempo

O objeto central (3.10). Uma coluna, um eixo, ordenada por **quando aconteceu**.

```
2026
──── agosto ────────────────────────────────────
 6   Vacina antirrábica aplicada
     Dra. Ana Lima · CRMV-SP 12345 · Clínica Vet Norte

 2   Mancou da pata direita, de manhã
     Marina · Creche Quintal

──── julho ─────────────────────────────────────
28   Peso: 12,4 kg
     Você

──── março ─────────────────────────────────────
14   O Code passou a ser seu
     Transferido por Abrigo Boa Sorte
```

**A transferência é uma marca na linha, nunca um corte.** A promessa 5.1 — o adotante
recebe a vida inteira — só é verdade visualmente se rolar acima daquela marca continuar
mostrando o abrigo, o lar transitório e o resgate.

**Silêncio é informação.** Um intervalo longo sem registro aparece como intervalo, e não
é colapsado sem aviso: "ausência" é uma das cinco percepções (4.6), e mentir sobre a
lacuna esconde o dado.

**Nunca:** agrupar por tipo como estrutura padrão — tipo é filtro; rolagem infinita sem
âncora de data; ordenar por data de digitação.

### 5.2 O evento com autoria

Duas linhas: **o fato**, e **quem o afirmou**. A segunda nunca é opcional, nunca é
tooltip, nunca é "ver detalhes" (5.7).

- **Contexto junto do autor.** "Dra. Ana Lima, pela Clínica Vet Norte" e "Dra. Ana Lima"
  são fatos diferentes (3.2), e a linha mostra qual dos dois é.
- **A credencial diz o que é.** CRMV apenas informado aparece como informado — em tinta
  secundária, sem selo verde de "verificado" que o produto não pode dar (5.10).
  Honestidade discreta, não alarde.
- **Os dois instantes, quando divergem.** Se foi registrado em outro dia, o apoio diz:
  *"registrado em 6/8"*. Quando coincidem, não se diz nada — ruído não é transparência.
- **Correção é sucessão, e se vê** (5.2). O valor anterior continua legível, a correção
  fica logo abaixo com autor e data. Nada de aba "histórico", nada de sobrescrever com um
  `(editado)`.

```
 6   Vacina antirrábica — lote 4471   ← corrigido
     Dra. Ana Lima · Clínica Vet Norte

     ↳ Lote correto: 4417
       Dra. Ana Lima · 7 de agosto
```

### 5.3 A pendência

O centro da área do tutor (decisão 2, 9.3). Cada uma diz **o quê**, **de quem**, **até
quando** e **o que fazer** — nessa ordem, uma linha para o fato e uma para a ação.

```
● Vencida há 3 dias
  Antirrábica do Code
  Registrar dose · Falar com a clínica · Silenciar

○ Até domingo
  Vermífugo da Nina — 2 de 3 doses dadas
  Confirmar · Silenciar

  Amoxicilina do Code, 8h
  Já dado hoje por Rafael, às 7h40
```

**Três estados, e só três:** vencido (marca vermelho-óxido), a vencer (âmbar), o resto
(sem marca). Urgência é escassa (4.2) — se tudo tiver marca, nada tem.

**Nunca cobrar duas pessoas sem dizer que a outra já fez.** É regra de produto (4.2) e
vira exigência de tela: a pendência de um animal com dois responsáveis **sempre** mostra
quem cumpriu. Dose dupla é dano, não incômodo.

**Silenciar mora na pendência**, não em preferências. Silêncio é funcionalidade (4.2), e
funcionalidade escondida em configuração não é oferecida — é escondida.

**O que sai do feed entra na linha do tempo.** Confirmar cumprimento é criar evento
(4.2): a pendência não some, ela **se move** — e o produto mostra isso, porque é o que
transforma cobrança em histórico de aderência.

**Nunca:** contador de badge como pressão ("47"), ponto vermelho em tudo, contagem
regressiva.

### 5.4 A diferença entre observação e ato clínico

A fronteira mais importante do produto (3.11) — e ela precisa ser **imediata**, sem
depender de ler quem assinou.

| O que é | Onde vive | Como se distingue | Quem pode |
|---|---|---|---|
| **Ato clínico** | Na linha do tempo | Régua vertical sólida em tinta, à esquerda, e a linha de credencial | Só credencial profissional |
| **Observação** | Na linha do tempo | Sem régua. Mesmo peso de texto, autoria simples | Qualquer um com acesso |
| **Conteúdo** — recado, foto | Na linha do tempo | É o único objeto em que a imagem é grande. Sem régua, sem marca de estado | Organização com vínculo |
| **Percepção** | **Fora do eixo do tempo** | Nunca aparece entre os eventos | Só o Petfy |

**A regra visual mais forte do documento:** *percepção não fica na linha do tempo.* Ela
não é fato (3.15), e o lugar de uma coisa na tela afirma o que ela é. Percepção mora
junto das pendências, sempre com a evidência ao lado e sempre com "dispensar" visível.

**E o alerta da creche é observação com urgência, não é ato clínico e não é vermelho**
(3.11, 4.5). Ele ganha peso pela posição no feed, não por cor de emergência — porque
pintar de vermelho o que um monitor observou é a tela dando o diagnóstico que o produto
se proibiu de dar.

**Nunca:** etiqueta colorida escrita "CLÍNICO"; ícone de tipo em cada linha; percepção
parecendo evento.

## 6. Acessibilidade e idioma

### O piso, e por que não é opcional aqui

**WCAG 2.2 nível AA, em toda tela, desde a primeira.** Não é conformidade por
conformidade — é consequência de quem usa:

- **O tutor de 70 anos** com o cachorro de 14 é o usuário mais fiel que este produto vai
  ter, e o que menos enxerga letra pequena.
- **A área de organização é usada oito horas por dia**, no teclado, por gente com as mãos
  ocupadas em outra coisa.
- **O cartão é lido por um estranho, numa emergência**, no celular dele, talvez no sol,
  com pressa. É a tela mais exigente do produto e a que roda no pior contexto possível —
  e a única sem conta para dar contexto (9.2).

### O que isso obriga

**Contraste.** 4.5:1 em texto, 3:1 em elemento de interface e texto grande. Os âncoras da
seção 3 só viram token depois de conferidos, nos dois temas — e é o par tinta-secundária
sobre papel que mais tem chance de reprovar, porque é justamente onde mora a autoria.

**Cor nunca sozinha.** Repetido de propósito: todo estado tem palavra, forma ou posição
além da cor. "Vencido" é informação de saúde, e um em doze homens não distingue o
vermelho do resto.

**Foco visível, e ele é nosso.** Anel de 2 px em tinta, com 2 px de deslocamento — nunca
o azul padrão do navegador, nunca removido, nunca só uma mudança de fundo. Como decidido
na seção 3, o foco não introduz cor nova: usa a tinta, que contrasta em qualquer
superfície.

**Teclado em tudo, e a área de organização é teclado primeiro.** Toda ação alcançável sem
mouse; ordem de tabulação seguindo o eixo do documento (seção 4). Cadastro em série no
balcão é digitação, não clique.

**Estrutura real para leitor de tela.** A linha do tempo é lista com títulos de verdade,
datas em elemento de tempo, e a autoria lida **junto** do fato — não pulada como enfeite.
É o principal motivo de o componente ser headless (Fase 5): sopa de `div` estilizada não
tem como ser corrigida depois.

**Alvo de toque de 44 px.** Confirmar cumprimento é o gesto mais repetido do produto, e
vai acontecer com uma mão, com o cachorro na outra.

**Movimento discreto e respeitando preferência.** Nenhuma animação carrega significado, e
quem pede movimento reduzido recebe. O produto não comemora (seção 1), então quase não há
o que animar.

**Formulário com rótulo sempre visível.** Placeholder não é rótulo — some quando se
digita, e é onde o erro nasce. Mensagem de erro amarrada ao campo, escrita como manda a
seção 2.

### O que o idioma cobra

**Texto é dado.** Nunca dentro de imagem, nunca montado por concatenação. Frase quebrada
em pedaços não sobrevive a nenhuma tradução.

**Data é uma coisa; instante é outra.** A vacina foi aplicada num **dia**; a observação da
creche aconteceu num **instante**. Data se mostra como data; instante se apresenta no fuso
de quem lê. Confundir os dois faz a linha do tempo trocar a ordem dos fatos para quem
viaja — e ordem é o produto.

**Concordância com o animal.** *"O Code passou a ser seu"* e *"A Nina passou a ser sua"*.
O produto sabe o sexo do animal e precisa acertar o artigo — em português, errar isso é a
diferença entre um produto que conhece o Code e um formulário que preenche variável. Onde
o sexo não for conhecido, a frase se reescreve para não precisar dele; nunca se chuta.

**Plural e contagem** passam pela regra do idioma, não por `s` no fim.

**Espécie, raça e catálogo são conteúdo traduzível**, não literal de código.

**Direção de texto da direita para a esquerda não entra agora** — mas nada trava o
caminho: onde existir propriedade lógica de início e fim, ela é usada, em vez de esquerda
e direita fixas.

### O que fecha uma tela

Nenhuma tela é dada por pronta sem isto — é a lista que o passo 5 vai usar:

- [ ] Contraste conferido, tema claro **e** escuro
- [ ] Navegável só com teclado, com foco visível o tempo todo
- [ ] Lida por leitor de tela, com autoria e data no lugar certo
- [ ] Alvos de toque a partir de 44 px
- [ ] Suporta 40% de crescimento de texto sem quebrar
- [ ] Nenhum significado transmitido só por cor
- [ ] Passa o teste do print da seção 1
