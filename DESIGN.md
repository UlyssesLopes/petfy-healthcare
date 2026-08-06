# Design

O `PRODUTO.md` diz **o que o Petfy é**, o `ROADMAP.md` diz **o que vem e em que ordem**,
o `README.md` diz **o que existe**. Este documento diz **como o produto se apresenta** —
e nasce depois dos três de propósito: direção visual sem produto decidido é gosto pessoal
com nome bonito.

**O que não entra aqui:** componente, biblioteca, arquivo de token, nome de classe. Isso
é código, e vem depois.

**A referência renderizada é `design/home-tutor.html`** — a home do tutor em web e
celular, nos dois temas. Se ela e este documento divergirem, **o documento manda**: ela é
um retrato de uma tela, ele é a regra de todas.

## Estado — 2026-08-06

| # | Seção | Status |
|---|---|---|
| 1 | O que este produto parece, e o que não parece | **Aprovada**, reescrita em 2026-08-06 |
| 2 | A voz | **Aprovada** em 2026-08-06 |
| 3 | Cor | **Aprovada**, reescrita em 2026-08-06 |
| 4 | Tipografia e ritmo | **Aprovada**, reescrita em 2026-08-06 |
| 5 | Os objetos que carregam a identidade | **Aprovada**, revisada em 2026-08-06 |
| 6 | Acessibilidade e idioma | **Aprovada** em 2026-08-06 |

**Documento fechado em 2026-08-06.**

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

## 1. O que este produto parece, e o que não parece

### A direção, em uma frase

> **Um lugar de cuidado, não um sistema de registros — caloroso com quem cuida, rigoroso
> com o que se registra.**

**De onde ela sai.** O Petfy guarda dado clínico, mas não vende software clínico. Quem
abre a tela é alguém que gosta de um animal e quer saber se está tudo bem com ele — e
quem também abre é um profissional que precisa levar o registro a sério. A direção
precisa servir os dois sem virar nem rede social de pet nem prontuário de hospital.

**A fórmula, que a primeira versão tinha ao contrário:**

> **O calor está na forma. O rigor está no conteúdo.**

Superfície macia, canto generoso, botão em pílula, o animal grande na tela — e, dentro
disso, autoria em cada registro, credencial dita como ela é, correção visível, nenhuma
interpretação clínica. A versão anterior tentou o inverso — estrutura austera com
conteúdo afetivo — e o resultado pareceu ferramenta de trabalho.

**O que "lugar de cuidado" significa na prática:**

- **A tela abre por quem cuida, não por dados.** O animal, e as pessoas em volta dele,
  antes de qualquer número.
- **Superfície macia é permitida e desejada.** Blocos com preenchimento suave, raio
  generoso, pílula na ação. Nada de sombra pesada, mas caixa não é mais proibida — ela é
  o que separa afeto de austeridade.
- **Autoria é calor, não burocracia.** O registro assinado com as iniciais de quem
  registrou faz a tela parecer gente cuidando, e cumpre a promessa 5.7 de graça.
- **Os neutros são de pelo.** Areia, creme, castanho. Nunca cinza de framework.
- **O animal é o protagonista visual**, e a coisa mais colorida da tela é sempre a foto
  dele.

### A tensão entre as duas áreas, e como se resolve

A área do tutor é um animal em profundidade, lida no sofá à noite. A de organização é
centenas em largura, lida no balcão às 9h. **Não são dois visuais — é o mesmo sistema em
duas densidades.** A de organização comprime espaço e reduz tamanho; não muda cor, fonte,
raio nem forma. Se as duas parecerem produtos diferentes, a 9.3 do `PRODUTO.md` foi
desfeita na camada visual, do mesmo jeito que um `if (isVet)` a desfaria.

### O que este produto não é, visualmente

| Proibido | Por quê |
|---|---|
| **Foto de banco de imagem** | Toda foto do produto é de um animal real de um usuário real. Cachorro sorridente de stock é a mentira visual mais barata que existe |
| **Mascote** | O protagonista é o animal do usuário. Um mascote nosso compete com ele — e perde |
| **Infantilização visual** | Balão de desenho, tipografia de gibi, ilustração fofa genérica. Caloroso não é infantil, e o veterinário precisa levar a tela a sério |
| **Painel de métricas na área do tutor** | Ele não tem indicador; tem um animal. Cartão de KPI é a assinatura do CRM, que é o que não queremos parecer |
| **Comemoração de ato de saúde** | Confete e "parabéns!" por registrar vacina. Cuidado não é conquista — e a regra de 5.5 vale nos dois sentidos: não alarma, e não comemora |
| **Emoji como ícone** | Envelhece em um ano e some no dia em que alguém precisa de informação séria |
| **Gradiente, índigo/violeta, cinza slate ou zinc** | A assinatura reconhecível de projeto gerado. Não é sobre feio; é sobre não ser de ninguém |
| **Vermelho de erro de sistema no que não é erro** | Vermelho é vencido e urgente. Observação de creche não é nenhum dos dois |

### O teste que decide caso novo

1. **Isto parece um lugar de cuidado, ou um sistema?** Se parece sistema, está errado do
   lado do tutor.
2. **Aponta para uma promessa da seção 5 ou um objeto da seção 3 do `PRODUTO.md`?** Se não
   aponta para nenhum, é decoração, e decoração não entra.
3. **Um print desta tela poderia ser de qualquer outro SaaS?** Se poderia, ainda não é o
   Petfy.

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

## 3. Cor

### O raciocínio de mercado, antes da paleta

**Uma premissa comum, que é falsa:** o vermelho do McDonald's não abre apetite. Isso é
folclore de marketing e nunca se sustentou em evidência. O que o vermelho faz é **ser
visível a distância** e **acelerar a sensação de urgência** — ferramenta de decisão
impulsiva. O Petfy quer o oposto: confiança que dura anos e alguém que volta todo dia sem
ansiedade. Copiar o mecanismo do vermelho seria copiar a ferramenta errada.

**A descoberta que decidiu a paleta: nenhuma cor diz "animal". Quem diz são os neutros e
a foto.** Areia, creme, caramelo, castanho, carvão — é isso que se lê como pelo. Por isso
o fundo inteiro é da família do pelo, e a cor viva fica reservada para duas coisas: a ação
e a urgência. Isso também resolve o problema de todo produto do nicho parecer o mesmo:
eles disputam o mesmo azul e o mesmo laranja, e nenhum usa os neutros do bicho.

**Por que o acento não é azul.** Azul é o default da categoria inteira — Petz, Cobasi,
Chewy, Rover, PetDesk, Petco. O risco não é bater com uma marca específica; é ser mais um
pet azul. O `#125E63` é um **petróleo esverdeado**: conserva a calma e a confiança do
azul, entra no terreno da saúde, e sai da prateleira visual do varejo.

### A paleta, com contraste medido

Os números são WCAG conferidos, não estimados. O mínimo é 4.5:1 para texto.

| Papel | Claro | Escuro |
|---|---|---|
| Superfície | `#FDFBF9` | `#141311` |
| Superfície elevada — barra, cabeçalho | `#F7F1EB` | `#1B1916` |
| Contorno | `#EADFD4` | `#2C2823` |
| Marca neutra — avatar, ícone | `#EFE8E1` | `#211E1A` |
| Tinta | `#22201E` | `#F3EFEA` |
| Tinta secundária | `#7A706A` | `#A79C93` |

| Significado | Claro | Escuro | Reservado a |
|---|---|---|---|
| Acento | `#125E63` | `#4FB6AE` | Identidade, ação, ato clínico |
| Sobre o acento | `#FFFFFF` | `#052422` | Texto dentro de botão e marca cheia |
| Fundo de acento | `#E4F0EF` | `#152827` | Bloco calmo, ato clínico, seleção |
| Urgência | `#A93F2F` | `#FF8B72` | Vencido, e só |
| Fundo de urgência | `#F9E7E2` | `#2B1D1A` | O bloco da pendência vencida |

| Par | Claro | Escuro |
|---|---|---|
| Texto sobre papel | 15.73 | 16.22 |
| Texto secundário sobre papel | 4.67 | 6.91 |
| Acento sobre papel | 7.25 | 7.63 |
| Texto dentro do botão | 7.48 | 6.74 |
| Urgência sobre o fundo dela | 5.11 | 7.10 |
| Texto sobre fundo de acento | 13.92 | 13.44 |

> O vermelho claro começou em `#B84A38` e **reprovou por 4.31**. Subiu para `#A93F2F`, que
> dá 5.11. Fica registrado porque o par mais frágil da paleta é sempre o de urgência sobre
> o próprio fundo, e ele é o que mais tenta passar no olho.

### A distribuição

| Papel | Peso na tela |
|---|---|
| Fundo e superfícies, em neutro de pelo | ~70% |
| Texto e estrutura | ~20% |
| Acento — identidade e ação | ~7% |
| Destaque humano — avatar de quem registrou | ~2% |
| Urgência | ~1% |

### Cinco regras

**1. Cor de estado nunca vira cor de ação.** Urgência aparece em marca pequena, etiqueta e
fundo de bloco — jamais em botão. Botão é acento. Um botão vermelho e um alerta vermelho
na mesma tela ensinam a pessoa a ignorar os dois.

**2. Urgência é escassa, e tem dono.** Só o que está de fato vencido. **O alerta da creche
não é vermelho** — é observação, não emergência nem diagnóstico (3.11, 4.5). Pintá-lo de
vermelho é a tela dando o diagnóstico que o produto se proibiu de dar.

**3. Cor nunca é o único portador de significado.** Todo estado tem também palavra, forma
ou posição. "Vencido" é informação de saúde, e um em doze homens não distingue o vermelho
do resto.

**4. A foto do animal é a coisa mais colorida da tela.** A interface não compete com ela.

**5. Percepção não usa cor semântica.** Ela não é fato (3.15), então não pode se vestir de
alerta. Distingue-se por posição — fora do eixo do tempo —, nunca por cor de urgência.

### O escuro não é inversão

É um segundo papel. Superfície quase-preta **quente** (`#141311`), nunca `#000000`; tinta
que não é branco puro; acento clareado para manter contraste. Inverter o claro produz o
cinza-azulado morto que denuncia tema escuro feito às pressas — e metade do uso do tutor é
à noite, no sofá, que é justamente quando o produto precisa parecer bem-feito.

---

## 4. Tipografia e ritmo

### Uma família só: Figtree

Geométrica humanista, aberta, ótima em tamanho pequeno, variável, e com números que se
comportam em coluna. É amigável **sem ser infantil** — que é exatamente a linha que a
seção 1 pede.

**Por que uma só.** Duas famílias eram necessárias quando a direção era documento, com
serifada para leitura. Aquela direção caiu, e a serifada caiu junto: ela lia como
prontuário. Uma família bem usada em quatro pesos dá toda a hierarquia necessária, e
economiza uma requisição, um arquivo e uma decisão por componente.

**Por que não Inter.** É a fonte assinatura do projeto gerado, e a seção 1 proíbe essa
assinatura. Não é problema da fonte; é problema de ela ser o default de todo mundo.

**Numerais tabulares em tudo que alinha:** peso, data, contagem, série. É o detalhe que
faz o dado parecer registro em vez de texto.

### A escala

| Degrau | Tamanho | Peso | Dono |
|---|---|---|---|
| Nome do animal | 26 px | 700 | O título de tudo |
| Número de resumo | 20–21 px | 800 | Anos, tutores, peso — sempre tabular |
| Texto de registro | 15,5 px | 600 | O fato, na linha do tempo e na pendência |
| Interface | 14–15 px | 600–700 | Botão, link, item de lista |
| Apoio | 12,5–13 px | 400–600 | Autoria, data, contexto — em tinta secundária |
| Rótulo de seção | 12,5 px | 700 | Caixa alta, `letter-spacing` de 0.07em |

**Quatro pesos:** 400, 600, 700, 800. Nada de light — some sobre papel quente. O 800 é
exclusivo de número de resumo.

### O ritmo, e o raio como identidade

**Base de 4 px**, escala de 4, 8, 12, 16, 24, 32, 48.

**O raio faz parte da identidade, e é escalonado** — o oposto do que a primeira versão
dizia:

| Elemento | Raio |
|---|---|
| Contêiner da aplicação | 16 px |
| Bloco — pendência, caixa lateral, cartão de pessoa | 12 px |
| Linha da lista, item da linha do tempo | 10 px |
| Botão, etiqueta, seletor de animal | pílula |
| Avatar | círculo |

**Agrupamento é feito com preenchimento suave e espaço**, não com sombra. Bloco calmo usa
o fundo de acento; bloco urgente usa o fundo de urgência; o resto é contorno de 1 px.

**Duas densidades, um sistema.**

| | Área do tutor | Área de organização |
|---|---|---|
| Texto de registro | 15,5 px | 14 px |
| Espaço entre itens | 9–24 px | 4–8 px |
| Altura de linha em lista | Confortável | Compacta |
| Fonte, cor, raio, forma | **Idênticos** | **Idênticos** |

### O que o idioma cobra

Português é longo, e alemão é mais. **Nada de largura fixa em rótulo, botão ou coluna** —
o que couber em `pt-BR` tem de caber quando crescer 40%. É consequência de i18n ser decisão
de dia um, e é barata agora e cara depois.

---

## 5. Os objetos que carregam a identidade

Se estes cinco estiverem certos, o resto do produto se desenha sozinho. Se algum estiver
errado, nenhuma paleta salva.

### 5.1 A linha do tempo

O objeto central (3.10). Uma coluna, ordenada por **quando aconteceu**, com uma **espinha
contínua** ligando os registros.

- **A espinha não quebra**, e é isso que torna a promessa 5.1 visível: ela atravessa o dia
  em que o animal mudou de mão. A transferência é um marco na linha, nunca um corte.
- **O eixo dos anos fica logo acima da linha, sem caixa** — o primeiro ano de registro de
  um lado, o ano corrente do outro, os marcos entre eles. Ele diz "esta vida tem sete
  anos" antes de qualquer registro ser lido.
- **Os números do resumo moram no cabeçalho da seção**, em uma linha discreta: *7 anos · 2
  tutores · 3 organizações · 41 registros*. Não é painel, é legenda.
- **Silêncio é informação.** Intervalo longo sem registro aparece como intervalo — ausência
  é uma das cinco percepções (4.6), e esconder a lacuna esconde o dado.

**Nunca:** agrupar por tipo como estrutura padrão (tipo é filtro); rolagem infinita sem
âncora de data; ordenar por data de digitação.

### 5.2 O evento assinado

Duas linhas: **o fato**, e **quem o afirmou**. A segunda nunca é opcional, nunca é tooltip,
nunca é "ver detalhes" (5.7).

- **A marca do evento é a pessoa.** As iniciais de quem registrou ocupam o lugar que num
  produto comum teria um ícone de tipo. É a promessa 5.7 virando calor em vez de
  burocracia.
- **Ato clínico tem bloco de acento e linha de credencial.** A distinção com observação é
  imediata, sem precisar ler o autor (3.11).
- **A credencial diz o que é.** CRMV apenas informado aparece como informado, em tinta
  secundária — sem selo de "verificado" que o produto não pode dar (5.10).
- **Os dois instantes, quando divergem.** Se foi registrado em outro dia, o apoio diz.
  Quando coincidem, não se diz nada — ruído não é transparência.
- **Correção é sucessão, e se vê** (5.2). O valor anterior continua legível e a correção
  fica logo abaixo, com autor e data. Nada de aba "histórico", nada de `(editado)`.
- **Série mora dentro do evento.** O registro de peso carrega o próprio gráfico e a
  variação — peso *é* série (3.9), então o lugar dela é a linha do registro, não um painel
  à parte. Vale para qualquer medida que vier a ser série.

### 5.3 A pendência

O centro da área do tutor (decisão 2, 9.3). Cada uma diz **o quê**, **de quem**, **até
quando** e **o que fazer**.

- **Três estados, e só três:** vencido, a vencer, e o resto sem marca. Se tudo tiver marca,
  nada tem.
- **Nunca cobrar duas pessoas sem dizer que a outra já fez.** A pendência de um animal com
  dois responsáveis **sempre** mostra quem cumpriu. Dose dupla é dano, não incômodo.
- **Silenciar mora na pendência**, não em preferências. Silêncio é funcionalidade (4.2), e
  funcionalidade escondida em configuração não é oferecida — é escondida.
- **O que sai do feed entra na linha do tempo.** Confirmar cumprimento cria evento: a
  pendência não some, ela se move.

**Nunca:** contador de badge como pressão, ponto vermelho em tudo, contagem regressiva.

### 5.4 A rede de quem cuida

**As pessoas em volta do animal vêm antes dos dados**, e essa posição é a tese da seção 1
do `PRODUTO.md` virando tela: o registro é o fio que liga quem cuida.

- Cada pessoa aparece com iniciais, papel e **a última contribuição** — "registrou hoje",
  "registrou ontem". É o que mostra que a rede está viva.
- **Conceder acesso é ação de primeira linha**, no meio da rede, e não escondida em
  configurações. Concessão é o mecanismo central do produto (3.5).
- O que aparece aqui é **quem tem alcance de fato** — custódia e acesso —, nunca uma lista
  de contatos.

### 5.5 A fronteira entre observação, ato clínico e percepção

A distinção mais importante do produto (3.11), e ela precisa ser imediata.

| O que é | Onde vive | Como se distingue | Quem pode |
|---|---|---|---|
| **Ato clínico** | Na linha do tempo | Bloco de acento, iniciais em acento cheio, linha de credencial | Só credencial profissional |
| **Observação** | Na linha do tempo | Sem bloco, iniciais em neutro | Qualquer um com acesso |
| **Conteúdo** — recado, foto | Na linha do tempo | O único objeto em que a imagem é grande | Organização com vínculo |
| **Percepção** | **Fora do eixo do tempo** | Vive na coluna lateral, com evidência e "dispensar" | Só o Petfy |

**A regra visual mais forte deste documento:** *percepção não fica na linha do tempo.* Ela
não é fato (3.15), e o lugar de uma coisa na tela afirma o que ela é.

**E o alerta da creche é observação com urgência** — não é ato clínico e não é vermelho
(3.11, 4.5). Ganha peso pela posição no feed, não por cor de emergência.

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

**Contraste conferido, não estimado.** A tabela da seção 3 traz os pares medidos, nos dois
temas. Regra que fica: **nenhuma cor entra na paleta sem o número**, e o par de urgência
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
