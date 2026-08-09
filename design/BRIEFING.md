# Petfy — briefing de produto para o design

> **Para quem lê isto.** Você vai desenhar o Petfy inteiro, do zero: a identidade, o
> sistema visual e todas as telas, na web e no aplicativo. Este documento é a única
> coisa que você recebe, e é de propósito.

**Não há design anterior para respeitar.** Nenhuma paleta, nenhuma tipografia, nenhuma
referência, nenhuma tela existente. Nada foi decidido sobre a aparência deste produto, e
a decisão é sua. Se você procurar entre as linhas uma dica de estilo, não vai achar — não
porque esteja escondida, mas porque não existe.

**O que este documento dá é a outra metade, e ela é inegociável:** o que o Petfy é, para
quem, o que promete, o que se recusa a fazer, e quais telas precisam existir. É a parte
que você não teria como inventar, e é a parte que decide se o design está certo. Um
sistema visual bonito sobre um entendimento errado do produto é trabalho perdido.

**Leia até o fim antes de desenhar qualquer coisa.**

---

## 1. A tese, e as quatro invariantes

> **O Petfy é o registro único, contínuo e vitalício da vida de um animal — e o fio que
> liga todos que cuidam dele a esse registro.**

Tudo no produto existe para servir um único ser: **o animal**. Pessoas e instituições
entram e saem da vida dele — tutores, veterinários, clínicas, creches, lares
transitórios, abrigos —, mas o registro é do animal e sobrevive a todas elas. Quem cuida
contribui com o registro; **ninguém o possui**.

Guarde essa frase. Ela é o teste de qualquer decisão de tela que você tomar: se um
desenho faz parecer que o histórico pertence a quem o cadastrou, ele está errado, por
mais elegante que seja.

### O animal é o centro, não a conta

O histórico não pertence a quem cadastrou. Acompanha o animal por mudança de tutor,
adoção, passagem por abrigo, troca de clínica. Um produto centrado na conta perde a
história do animal exatamente quando ela fica mais valiosa — **na transferência**.

### Nada se apaga, tudo se sucede

Correção não sobrescreve: **encerra e registra**. O valor de um histórico de saúde é
proporcional à confiança de que ele não foi editado.

Consequência direta no seu trabalho: **não existe "excluir" no registro**, e "editar"
nunca é um campo que se sobrescreve — é um ato novo que se soma, dizendo o que corrigiu,
quem corrigiu e quando. O que estava lá continua visível.

### Acesso é concedido, delimitado, temporário e auditável

Ninguém alcança o registro de um animal por ser profissional, por ter o link ou por ter
estado lá antes. Alcança porque **quem responde pelo animal concedeu**, no escopo que
concedeu, enquanto durar, e com rastro de quem leu o quê.

### Nenhuma custódia termina sem sucessor

Ninguém "solta" um animal. Repasse é a criação do vínculo seguinte, não o encerramento do
anterior: toda transferência tem destino identificado, nenhum período fica sem alguém
respondendo, e a linha do tempo sempre avança.

Na tela: **não existe "abrir mão deste animal"**. Existe "transferir para", e ele exige o
outro lado.

### A consequência que dá nome ao produto

Por manter o registro inteiro, em vez de fragmentos espalhados por cada prestador, o
Petfy é o único ponto do ecossistema capaz de **perceber padrão ao longo da vida do
animal** — e de agir sobre ele antes que o tutor perceba sozinho.

---

## 2. Quem usa, e o que cada um ganha

**Ninguém entra por altruísmo com o ecossistema.** Cada ator precisa de uma razão egoísta
e imediata para usar o Petfy sozinho, mesmo que ninguém mais use.

Isso importa para você porque **a primeira tela de cada ator tem de entregar a razão
egoísta dele**, nunca a promessa do ecossistema. Ninguém se cadastra para "conectar o
cuidado do meu animal": cadastra-se para parar de perder a carteirinha.

### 2.1 Tutor — o único ator obrigatório

**A dor.** A vida do animal está espalhada: carteirinha de papel que desbota, laudo no
WhatsApp da clínica, o resto na memória. Na hora em que a informação vale — emergência,
viagem, matrícula na creche, troca de veterinário, o animal passando mal às 23h — ele não
tem.

**O que o traz.** A carteira no celular e o aviso do que vence. É o que ele já tenta
fazer com foto e alarme no telefone, e faz mal.

**O que ele dá.** O cadastro do animal e, juridicamente decisivo, **o consentimento que
autoriza todo o resto do ecossistema a existir**. Sem tutor, ninguém alcança nada.

**O que ele não pode, e é contraintuitivo:** alterar, apagar **ou contestar** o que um
profissional registrou. O registro clínico é do animal e da responsabilidade de quem o
assinou — **não é uma conversa**. Ele é *informado* do que foi registrado e recebe os
**dados de contato do profissional e da organização**; divergência se resolve com quem
registrou, fora do produto.

Na tela: onde houver registro clínico, não pode haver campo de resposta, nota do tutor,
polegar, avaliação nem "reportar erro". O que pode haver é **quem registrou, e como falar
com essa pessoa**. Ele segue podendo exportar tudo e sair do produto.

### 2.2 Veterinário — a pessoa, não o lugar

**A dor.** Recebe o animal sem histórico e depende do que o tutor lembra — que é pouco e
frequentemente errado. E o que ele registra fica preso no sistema da clínica onde
trabalha: se ele sai, o trabalho dele não vai junto.

**O que o traz.** Ver o histórico real antes de encostar no animal, e registrar o
atendimento sem burocracia.

**O que ele dá.** O dado de maior densidade clínica: diagnóstico, procedimento,
prescrição.

**O que ele não pode.** Alcançar animal sem concessão do tutor, nem levar o histórico
embora ao trocar de clínica.

**Duas formas que o mundo real exige, e que é fácil esquecer ao desenhar:**

- **O autônomo.** Atendimento domiciliar é enorme no Brasil e não tem clínica. Ele
  precisa funcionar inteiro sem pertencer a lugar nenhum — nada pode obrigá-lo a inventar
  uma clínica que não existe.
- **O que atua em vários lugares.** Pode ser veterinário de duas clínicas, voluntário de
  um abrigo e consultor de uma creche ao mesmo tempo.

É identificado pelo **registro profissional** — o que o identifica no mundo real, segue
com ele entre clínicas, e é público.

### 2.3 Clínica — e a dor dela não é a do veterinário

**A dor.** A clínica não sofre de falta de histórico; sofre de **paciente que não volta**.
Reforço de vacina esquecido é receita perdida, e é o vínculo com o cliente esfriando.

**O que a traz.** Saber quem está vencendo, e ver num lugar só os animais que ela atende.

**O que ela dá.** Volume, continuidade e credibilidade — animal cujo histórico veio de
clínica vale mais do que histórico digitado pelo tutor.

**O que ela não pode.** Usar o alcance concedido como lista de marketing. No dia em que um
tutor receber promoção de banho e tosa por ter autorizado acesso ao prontuário do
cachorro, a confiança acaba — e ela é o ativo inteiro do Petfy.

### 2.4 Creche

**A dor, e são três.** Turma para gerir — quem vem que dia, quem está dentro agora.
Comunicação com o tutor, que hoje é um WhatsApp caótico de 40 pessoas com vídeo perdido.
E **responsabilidade**: aceitar animal sem vacina em dia, ou sem saber de uma alergia, é
risco de surto na turma e de processo.

**O que a traz.** A matrícula que exige comprovação de saúde — hoje resolvida pedindo foto
da carteirinha por WhatsApp e conferindo no olho — e o diário que o tutor adora receber.

**O que ela dá, e é o dado mais subestimado do ecossistema.** Observação diária,
sistemática, por gente treinada: comportamento, apetite, energia, sociabilidade,
eliminação. **Nenhum veterinário tem isso.** O veterinário vê o animal 20 minutos por ano;
a creche vê 8 horas por dia, 3 dias por semana. É a série temporal mais rica que vai
existir sobre um animal, e é ela que enxerga *"o Code está mais quieto há duas semanas"*
antes de virar doença.

**O que ela produz para o tutor:** matrícula e turma, com entrada e saída do dia;
avaliação diária; recado e foto do dia; tema de casa — o que treinar até a próxima aula; e
alerta — *"hoje ele não comeu"*, *"mancou da pata direita"*, *"brigou"*.

**Ela opera com limite físico e legal**, e o produto respeita: limite de turmas, alunos por
turma, quantas vagas sobram e para quem isso é visível, e a janela de horário de entrada —
inclusive o que acontece fora dela.

**O que ela não pode, e a fronteira é dura:** escrever no prontuário clínico. **A creche
registra observação; ela não diagnostica.** Sem essa fronteira, em seis meses tem monitor
lançando *"suspeita de displasia"* no histórico médico, e o valor clínico do registro
morre. O alerta da creche é **gancho para o veterinário**.

### 2.5 Lar transitório — dois perfis, uma mecânica

**O eventual** é pessoa comum que topa receber um animal por um período. Nunca vai criar
uma conta de "lar transitório" nem preencher cadastro institucional: liga uma
disponibilidade — período, espécie, porte, quantos — e desliga quando não puder mais.

**O recorrente** faz isso o tempo todo, quase sempre ligado a um abrigo. Mesma mecânica,
com histórico acumulado e reputação.

**A dor do eventual.** Recebe um animal que não conhece: não sabe o que ele tem, se é
vacinado, se é alérgico, se toma remédio, se pode ficar com outro cachorro. E teme a
responsabilidade — se o animal adoecer sob a guarda dele, de quem é a conta.

**A dor do recorrente.** O histórico do período dele evapora no repasse. Viveu com o
animal 4 meses, aprendeu tudo sobre ele, e entrega uma caixa e um *"ele é bonzinho"*.

**O que ele dá.** Continuidade do registro **no período mais frágil da vida do animal**,
que é justamente quando ele hoje não existe.

**O que ele não pode.** Reter o animal ou o registro fora do combinado. Início, prazo e
fim da custódia são eventos registrados, não acordo verbal.

### 2.6 Abrigo e ONG

**A dor.** Dezenas a centenas de animais em planilha, voluntário que troca toda semana, e
a **adoção que volta** — devolução por doença ou comportamento que ninguém contou.

**O que o traz.** Gestão dos animais e da rede de lares transitórios num lugar só.

**O que ele dá, e é o mais valioso do produto inteiro.** Primeiro, **animal que nasce no
sistema sem tutor humano** — resgate, animal de rua, ninhada. Segundo, **o momento da
adoção**, onde o Petfy prova a tese: o registro inteiro — resgate, tratamento, castração,
meses no lar transitório, comportamento observado — passa para o adotante. O abrigo
entrega um animal **com biografia**, e o adotante ganha uma conta que ele não pediu e não
vai querer perder.

Se você desenhar uma única tela com carinho neste briefing, que seja essa.

**O que ele não pode.** Continuar mandando no registro depois de repassar. Vira leitor do
que produziu, não dono do animal.

### 2.7 O que a lista revela

**A cadeia de aquisição é assimétrica** — ninguém traz o próximo por generosidade: a
creche obriga o tutor a comprovar saúde; a clínica quer o paciente de volta; o abrigo
entrega registro na adoção; e o tutor, sendo muitos, puxa clínica e creche por demanda.

**O tutor é o único que precisa existir**; os outros cinco são o motivo pelo qual ele
aparece.

**Um estado fácil de não prever:** *animal sem tutor humano*, sob responsabilidade de uma
organização. Nenhuma tela de animal pode pressupor que existe uma pessoa dona na outra
ponta.

**Fora de propósito:** hotel, banho e tosa, adestrador, pet sitter. Não desenhe para eles.

---

## 3. O vocabulário canônico

Dezessete conceitos. **São os nomes do produto, e você não deve reinventá-los** — nem na
interface, nem na sua documentação. Quando um deles aparecer na tela, aparece com este
nome; quando você precisar de uma palavra nova, é sinal de que o conceito talvez não
exista.

**Nada foi inventado para creche, lar transitório ou abrigo.** As três frentes cabem no
vocabulário abaixo, e é esse o teste de que ele está certo.

### As pessoas e os pontos de vista

**Pessoa** — um ser humano com uma conta. **Sem tipo.** Não existe "conta de tutor" nem
"conta de veterinário": existe uma pessoa, e o que ela pode fazer decorre dos vínculos que
tem, não do formulário que preencheu ao se cadastrar. *Regra dura: a pessoa nunca declara
o próprio papel. Declara quem é; o sistema deriva o que ela alcança.*

**Contexto** — o ponto de vista ativo: ela mesma, ou uma organização em nome de quem age.
Uma pessoa com três vínculos tem três contextos. O que ela vê, pode e registra muda
conforme o contexto, e **o registro guarda em qual contexto foi feito**: *"a Ana
registrou"* e *"a Ana, pela Clínica Vet Norte, registrou"* são fatos diferentes, e só o
segundo tem responsabilidade institucional.

### O animal e quem responde por ele

**Animal** — o centro. Existe por si, independente de qualquer conta. Não é criado *por*
alguém: é **registrado** por alguém, e sempre tem quem responda por ele, o que é outra
coisa. *Regra dura: animal não se apaga. Morte, perda e transferência são eventos, e a
linha do tempo continua existindo depois de cada um.*

**Custódia** — quem responde pelo animal, desde quando, até quando e por quê. Generaliza
tutor, lar transitório e abrigo. Tem natureza (definitiva, transitória, institucional,
resgate), prazo previsto, fim real, motivo de encerramento e sucessor.

**Acesso** — quem pode ler ou escrever no registro, em que escopo e até quando. **Custódia
e acesso são coisas opostas, e confundi-las é o erro mais caro que este produto pode
cometer na tela:**

|  | Custódia | Acesso |
|---|---|---|
| Responde pelo animal | Sim | Não |
| Quantos ao mesmo tempo | Poucos, e sempre há um principal | Muitos |
| Origem | Aquisição, adoção, resgate, repasse | Concessão de quem tem custódia |
| Termina | Só com sucessor | A qualquer momento, sem sucessor |
| Exemplos | Tutor, lar transitório, abrigo | Clínica, creche, o filho adulto, o cuidador |

A clínica não responde pelo animal e nunca vai responder: lê e escreve por concessão. Se
"conceder acesso à clínica" parecer, na sua tela, parente de "transferir titularidade", o
desenho está errado.

### Os coletivos

**Organização** — um coletivo que atua sobre animais, com membros e capacidades. Clínica,
creche e abrigo **não são três entidades**: são a mesma, exercendo conjuntos de
capacidades diferentes.

| Capacidade | Clínica | Creche | Abrigo |
|---|---|---|---|
| Registrar ato clínico | sim | não | sim (com veterinário membro) |
| Registrar observação | sim | sim | sim |
| Comunicar-se com o tutor | sim | sim | sim |
| Deter custódia | não | não | sim |
| Gerir turma e vaga | não | sim | não |
| Manter rede de lares transitórios | não | não | sim |

ONG com clínica própria faz as três; creche que hospeda faz duas. **Desenhar três telas
diferentes para "clínica", "creche" e "abrigo" é reintroduzir o tipo pela porta dos
fundos** — é uma tela só, que mostra o que a organização tem capacidade de fazer.

**Membro** — uma pessoa dentro de uma organização, com função: veterinário, monitor,
voluntário, administrador. A função decide o que ela exerce das capacidades que a
organização tem. Uma pessoa tem vários vínculos em várias organizações. **Organização é
opcional para atuar:** o veterinário autônomo é pessoa com credencial e sem vínculo.

**Vínculo** — relação contínua entre um animal e uma organização, sem transferir custódia.
Paciente da clínica e aluno da creche são a mesma coisa. **Não é acesso:** acesso é
permissão, vínculo é relacionamento. A clínica que atendeu uma vez em 2023 teve acesso; a
clínica onde o animal se trata há 6 anos tem vínculo — e é o vínculo que justifica
"meus pacientes" e "meus alunos". *Vínculo não some do histórico ao encerrar: ter estudado
dois anos naquela creche é biografia.*

### O que acontece

**Evento** — algo que aconteceu com o animal, num instante, registrado por alguém num
contexto: vacina, antiparasitário, atendimento, pesagem, observação, entrada e saída da
creche, mudança de custódia, adoção, óbito.

> **Regra dura, e ela tem consequência visual imediata:** *quando aconteceu* e *quando foi
> registrado* são coisas distintas. A creche registra às 18h o que viu às 9h; o tutor
> cadastra hoje a vacina de 2019 da carteirinha de papel. **Linha do tempo ordenada por
> data de digitação mente.**

**Linha do tempo** — a leitura ordenada dos eventos de um animal, atravessando todas as
custódias, organizações e prestadores da vida dele. **É isto o produto.** *Regra dura: ela
não recomeça na transferência. O adotante recebe a vida inteira, não uma ficha em branco
com a data de hoje.*

**Observação** — o que alguém viu: não comeu, mancou, vomitou, brigou. Qualquer um com
acesso registra.
**Ato clínico** — diagnóstico, prescrição, procedimento. Carrega responsabilidade
profissional, e só quem tem credencial verificada pratica.

*Regra dura: observação nunca vira ato clínico sozinha.* Pode ser **referenciada** por um:
o veterinário lê *"mancou da direita nos últimos 3 dias"*, registrado pela creche, e emite
um diagnóstico que aponta para aquelas observações como evidência. **A creche nunca
escreve no prontuário, e o que ela viu chega a quem pode diagnosticar.**

**Orientação** — instrução dada a quem cuida, com prazo e confirmação de cumprimento. Um
conceito, três usos: prescrição do veterinário, tratamento contínuo, e tema de casa da
creche. *Regra dura: a orientação segue a **custódia**, não a pessoa. Se o animal volta do
lar transitório para o abrigo no meio de um tratamento de 21 dias, o remédio continua — a
instrução passa para quem assumiu, com o já cumprido preservado.*

**Conteúdo** — o que uma organização escreve para o tutor ler: recado, foto, avaliação do
dia. Não é registro clínico e não entra no prontuário, mas **é biografia** — a foto do
primeiro dia de creche faz parte da vida do animal e aparece na linha do tempo.

> **É o único conceito que existe para dar prazer, e não para resolver problema.** É o que
> faz o tutor abrir o produto num dia em que nada vence — e app de saúde que só fala
> quando tem má notícia é app que ninguém abre. Trate-o como tal.

**Pendência** — tudo que reivindica ação de alguém, com prazo: dose vencendo, orientação a
cumprir, tema de casa, matrícula com vacina irregular, convite aguardando resposta,
consentimento pendente. **Não é registrada; é derivada** dos fatos. Existe para dar lugar
único a tudo que o produto vier a cobrar — sem ela, cada funcionalidade nova inventa o
próprio aviso, no próprio canto.

**Percepção** — uma leitura sobre os fatos, produzida pelo Petfy, que ninguém registrou.
*"O Code teve aquele episódio há 6 meses; que tal uma dieta específica?"*

> **Regra dura, não negociável:** percepção nunca é fato e nunca é diagnóstico. Não entra
> na linha do tempo como evento, **sempre mostra a evidência que a gerou**, e sempre pode
> ser dispensada. No dia em que uma percepção do sistema for lida como opinião clínica,
> ela vira risco jurídico e o produto perde o direito de ter uma.

### Os limites físicos

**Disponibilidade** — a oferta de quem pode receber um animal: período, espécie, porte,
quantos, e restrições reais (*"não pode com gato"*, *"tenho criança pequena"*, *"não
recebo animal em tratamento"*). É oferta, não compromisso: liga e desliga a qualquer
momento e não gera custódia.

**Lotação** — o limite físico e legal do que uma organização comporta: turmas, alunos por
turma, vagas abertas, janela de entrada; quantos animais cabem, quantos há. *Vaga
divulgada é decisão da organização, não do sistema.*

> **Capacidade** é o que a organização *pode fazer*; **lotação** é *quantos cabem*. Não
> troque as palavras.

### O vocabulário fechado, em uma frase

**Uma pessoa, agindo num contexto, tem custódia de um animal ou acesso a ele. Organizações
têm membros, capacidades e lotação, e mantêm vínculo com os animais que atendem. Tudo o
que acontece é evento, e a soma ordenada dos eventos é a linha do tempo — de onde saem as
pendências que cobram ação e as percepções que ninguém registrou.**

---

## 4. O que o produto faz, e o que promete

Organizado **por fluxo**, e não por ator: quase toda capacidade importante é exercida por
mais de um ator.

### 4.1 A vida registrada

**Quatro portas de entrada,** uma para cada ator da cadeia: o tutor cadastra (a porta
comum); a organização registra animal sob custódia (o abrigo com um resgate, sem tutor
humano); a clínica registra ao atender (animal que chega em emergência e cujo tutor ainda
não usa o Petfy); e a importação do que existe em papel — a carteirinha fotografada.

**Identificação é múltipla e nenhuma é obrigatória:** microchip, tatuagem, RGA, nome de
casa. O produto não pode exigir o que metade dos animais do Brasil não tem — mas onde há
microchip, ele é a chave que responde *"esse animal já está no Petfy?"* no resgate e na
adoção.

**A linha do tempo se forma sozinha**, ordenada por quando aconteceu: o tutor cadastra
hoje a vacina de 2019 e ela aparece em 2019.

**Regra dura:** todo evento sabe quem o registrou e em que contexto, e isso **nunca é
editável**. É o que separa um registro que um veterinário aceita de um caderno digital.

### 4.2 O cuidado que se cobra

**A quem se cobra: a quem tem custódia agora**, não a quem cadastrou o animal. O tutor que
deixou o cachorro no lar transitório não é quem dá o remédio às 8h.

**Cumprir é evento.** *"Dei o remédio"* entra na linha do tempo, com quem e quando — é o
que transforma orientação em histórico de aderência, o dado que o veterinário nunca tem
quando o tratamento não funciona.

**Três regras duras, e as três são de tela:**

- **Silêncio é funcionalidade.** O tutor precisa poder silenciar sem que o registro pare.
  Produto de saúde que não pode ser calado é desinstalado — e aí para de registrar também.
- **Nunca cobrar a mesma coisa de duas pessoas sem dizer que a outra já fez.** Dois
  tutores dando o mesmo remédio é dano, não incômodo.
- **Urgência é escassa.** Se tudo interrompe o dia, nada interrompe. Você vai precisar de
  uma hierarquia de gravidade que resista à tentação de tornar tudo vermelho.

### 4.3 A custódia que muda de mão

**Cinco movimentos, um mecanismo:** transferência entre tutores, adoção por abrigo,
entrega a lar transitório, devolução, e resgate — custódia nascendo sem antecessor.

| Atravessa a transferência | Não atravessa |
|---|---|
| A linha do tempo inteira | Os acessos concedidos pelo tutor anterior |
| Orientações ativas, com o já cumprido | Preferências e canais de notificação |
| Condições, alergias, anexos | Pendências que eram do antecessor por outro motivo |

**Regra dura, de privacidade: acessos não são herdados.** A clínica do tutor anterior
deixa de enxergar o animal no instante da transferência. O oposto faria o novo tutor
herdar, sem saber, uma plateia que não escolheu. **Isso precisa estar visível na tela de
transferência** — quem vai deixar de ver, e o que o novo tutor terá de conceder de novo.

O tutor anterior mantém leitura do período em que respondeu pelo animal, salvo revogação.

### 4.4 A convivência com organização

**A matrícula na creche é o fluxo mais rico do produto:** a creche pede o animal, o produto
responde se a saúde está regular, e a matrícula é aceita ou fica pendente. Depois: turma,
lotação, janela de entrada, entrada e saída do dia.

**Regra dura, a mais importante deste fluxo: acesso tem escopo.** A creche precisa de
alergia, vacinação, medicação em curso e contato de emergência — **não do prontuário
inteiro**. Acesso tudo-ou-nada significa que matricular o cachorro na creche entrega a ela
o histórico completo de doenças do animal.

Consequência para você: **conceder acesso nunca é um interruptor.** É uma escolha de
escopo, e ela precisa ser compreensível por alguém que não sabe o que é escopo.

### 4.5 O que se conta ao tutor

**O que a organização manda:** recado, foto, avaliação do dia, alerta.

**Regra dura — alerta relata fato, não interpretação.** *"Não comeu hoje"*, *"mancou da
pata direita"*, *"brigou no pátio"* são observações. *"Acho que está com dor"*, *"parece
displasia"* não são. **A linguagem do produto tem de impedir isso, não só a permissão:**
campo de texto livre sem orientação vira diagnóstico de monitor em três meses. Isto é um
problema de design de formulário, e é seu.

**Regra dura — o produto não alarma.** O alerta chega como *o que foi observado* e *o que
fazer a respeito* (*"vale procurar o veterinário"*), nunca como hipótese clínica.

**Comunicação é de mão única.** A organização conta, o tutor lê, e para conversar existe o
contato direto. Não desenhe caixa de resposta, chat nem reação.

### 4.6 O que só o Petfy enxerga

| Forma de percepção | Exemplo |
|---|---|
| Recorrência | Terceiro episódio urinário em 18 meses |
| Tendência | Peso subiu 12% em 6 meses |
| Ausência | Não passa por veterinário há 2 anos; reforço nunca aplicado |
| Sazonalidade | Coceira registrada toda primavera, três anos seguidos |
| Mudança de comportamento | Creche relata queda de energia por três semanas |

As cinco dependem de insumo que nenhum ator isolado tem. **Nunca diagnostica:** descreve o
padrão, mostra a evidência, e aponta para quem pode. É sempre dispensável, e dispensar
ensina o produto a não repetir.

**Percepção não carrega marca.** Sugerir categoria de ação (*"vale conversar com o
veterinário sobre dieta renal"*) é serviço; sugerir marca é publicidade.

### 4.7 As oito promessas, e o preço de cada uma

Uma promessa só vale se tiver preço. Estas são as do Petfy, e **o que cada uma proíbe na
tela**:

| Promessa | O que ela proíbe você de desenhar |
|---|---|
| O registro é do animal, e não se perde | "Começar do zero"; apagar conta que apague o animal |
| Nada se apaga; correção é sucessão | Excluir e editar destrutivos, em qualquer lugar do registro |
| Acesso é concedido, delimitado, temporário e auditável | Acesso permanente, implícito, ou sem escopo |
| Dado de saúde tem regime próprio | Juntar tudo num "histórico" só, sem distinguir o sensível |
| O Petfy não interpreta clinicamente | Qualquer texto com hipótese diagnóstica, inclusive em vazio e erro |
| O Petfy não vende a atenção do tutor | Anúncio, patrocínio, marca sugerida, "parceiros" |
| Responsabilidade tem nome | Registro anônimo; autor ou contexto editáveis |
| Portabilidade real | Retenção por atrito na saída |

**Sobre a mais cara delas:** o produto **não responde** *"o que meu cachorro tem?"*, que é
exatamente o que o usuário mais gostaria de perguntar. Parecer menos inteligente é o preço
de não ser irresponsável — e é seu trabalho fazer com que isso pareça cuidado, e não
limitação.

**Sobre a lei:** o prontuário do animal não é dado pessoal do tutor — o animal não é
titular. O cadastro dele (nome, contato, preferências) **ele corrige livremente**; o
registro clínico do animal, não. O modelo é o do prontuário humano: o paciente não
reescreve o que o profissional registrou; obtém retificação de quem registrou, e ela entra
como sucessão visível. Os direitos que a tela precisa tornar exercíveis: ver tudo, saber
quem registrou, falar com quem registrou, ter o erro corrigido, corrigir o que é dele,
levar embora, e sair.

---

## 5. O que o Petfy não é

**O critério que decide, e vale para qualquer ideia sua:** se serve **o animal e o
registro dele**, é do Petfy. Se serve a **operação de um negócio**, não é — por mais que o
negócio seja cliente nosso.

**Nunca, porque viola uma promessa:** publicidade ou marketplace; rede social de pets
(perfil público, seguidores, curtida — o conteúdo é dirigido ao tutor, não ao público, e a
mesma foto que encanta numa timeline privada vira exposição num feed aberto); telemedicina;
seguro ou plano de saúde pet.

**Não nesta rodada:** sistema de gestão de clínica (faturamento, estoque, comissão, escala,
agenda de sala); sistema de gestão de ONG (doação, prestação de contas, campanha);
mensageria; hotel, banho e tosa, adestrador, pet sitter.

**Fora de alcance:** o Petfy não emite nem valida RGA, microchip ou identificação legal —
guarda e usa esses números. E não verifica credencial profissional enquanto não houver
integração com o conselho: o estado *informado* é dito em voz alta no registro, em vez de
fingir uma garantia que o produto não tem.

> **A tentação que vai voltar** é a gestão de clínica, pela boca de um cliente pagante. A
> resposta: o Petfy não é o sistema da clínica; é **o registro do animal que a clínica
> atende**. O que ela nunca terá é o registro preso ao software dela — e é justamente isso
> que faz o registro valer.

---

## 6. As três superfícies

Existem três formas de alguém alcançar o registro de um animal. **Cada uma se ancora numa
relação diferente do modelo** — não é arrumação de menu.

| Superfície | Ancorada em | Quem é | Volume | Como se navega |
|---|---|---|---|---|
| **Área do tutor** | Custódia, mais os acessos recebidos | Quem responde pelo animal | Um a três animais | Um animal em profundidade |
| **Área de organização** | Vínculo com a organização, mais os acessos concedidos | Quem atua por si ou por uma organização | Dezenas a centenas | Lista, filtro, busca, lote |
| **Cartão** | Um acesso com escopo, sem conta | Quem recebeu o link | Um animal | Leitura, escopo mínimo |

**O que impede uma quarta aparecer:** superfície nova só existe se houver **relação nova**.
Se você sentir falta de uma, a pergunta é *"ancorada em quê"* — e se a resposta for "num
tipo de usuário", é a Pessoa-sem-tipo sendo desfeita pela porta dos fundos.

**O que as três compartilham, e é o ponto:** o mesmo registro. **Não existe "versão
simplificada" da linha do tempo** para uma superfície — existe **escopo**. Entre elas muda
o quanto se alcança e como se navega; nunca muda o que é verdade sobre o animal.

---

## 7. Uma aplicação, duas áreas

**Mesma aplicação, mesmo cadastro, mesma entrada.** As duas áreas não são dois produtos:
são dois pontos de vista sobre o mesmo registro, e quem alterna entre elas é a mesma
pessoa.

> **Regra dura: não existe login de tutor e login de veterinário.** Dois logins desfariam
> a Pessoa-sem-tipo logo na porta. Não desenhe uma tela de entrada que pergunte "você é
> tutor ou profissional?".

**A área não vem de um campo de papel.** A pessoa chega numa área por causa do que ela
**tem** — custódia de um animal, ou vínculo com uma organização —, nunca por causa de um
tipo declarado no cadastro. Se o papel voltar como condicional na tela, ele voltou inteiro.

**A troca entre áreas é explícita e visível.** Quem age precisa saber em nome de quem está
agindo **antes** de agir, porque o registro guarda isso e não é editável depois. Contexto
implícito é ato registrado no lugar errado. **Este é um dos problemas de design mais
importantes deste briefing** — a troca precisa ser óbvia sem ser um pedágio em cada tela.

**A área do tutor** é um animal em profundidade. O centro dela é o **feed de pendências**,
porque é o único formato em que vencimento, orientação, recado e percepção cabem juntos
sem virar quatro abas. É a área que existe todo dia.

**A área de organização** é muitos animais em largura: lista, filtro, busca e ação sobre
vários de uma vez. **Clínica, creche, abrigo e rede de lares transitórios são a mesma
área**, diferindo pelas capacidades. É a área do expediente.

**O profissional autônomo entra na área de organização sem organização nenhuma**, e nada
nessa área pode exigir uma organização para funcionar. Quem o limita é a credencial dele,
não a existência de um coletivo.

**Ter as duas é o caso comum, não o raro:** a veterinária que tem cachorro, o dono de
creche que é tutor, o voluntário de abrigo com dois gatos. Ela cadastra o próprio animal
numa área e atende na outra, e **nenhuma das duas esconde a existência da outra**.

---

## 8. Web primeiro, app depois

**Web primeiro, e o motivo é alcance:** é a única superfície que serve os seis atores sem
exigir nada de ninguém. Quem recebe um link entra — não instala, não atualiza, não depende
de loja aprovar. E os atores pesados são de teclado: clínica, creche e abrigo trabalham em
balcão, cadastram em série e olham lista. **A área de organização não pede app; pede tela
grande.**

Isso não significa desktop-only: o tutor usa o navegador do celular o tempo todo, e a
web precisa ser excelente ali. Significa que **a densidade da área de organização é
projetada para tela grande**, e a da área do tutor, para a mão.

**O app não busca paridade.** Existe para as duas coisas que o navegador faz mal, e as
duas são do tutor:

- **avisar** — a pendência que chega sem o usuário ir procurar;
- **confirmar num toque** — *"dei o remédio"*, que é o cumprimento da orientação virando
  evento.

É o loop diário, e é curto de propósito. Lançar a vacina de 2019 da carteirinha de papel
continua na web — ninguém faz isso no ônibus.

> **Regra dura: o app não é o produto reduzido.** É um recorte com propósito. App que
> tenta ser a web inteira numa tela menor faz as duas coisas mal, e a primeira a quebrar é
> justamente o loop diário, que é a razão de ele existir.

**O cartão é a superfície mais móvel das três — e é a que nunca pode exigir app.** Nasce
de um QR numa coleira, lido por quem socorre o animal: não tem conta, não tem o app e não
vai instalar nada no meio de uma emergência. Navegador, e só.

---

## 9. A entrada, e o onboarding

### O que precisa existir

- **Cadastro.** Pessoa, sem tipo. Nome, e-mail, senha, e o **aceite dos termos**, que é
  condição para a conta nascer — sem base legal não há como tratar dado de saúde. Não
  pergunte se a pessoa é tutora ou profissional.
- **Login.** Um só, para todo mundo.
- **Recuperação de senha**, com o link que chega por e-mail e a tela de definir a nova.
- **Confirmação de e-mail.** A conta funciona antes de confirmar, mas não recebe aviso
  nenhum — e é preciso deixar isso claro sem transformar a tela num bloqueio.
- **Consentimento**, versionado. Quando o texto muda, o aceite anterior vira pendente, e o
  produto precisa pedir de novo sem parecer que algo deu errado.
- **Declarar credencial profissional**, em qualquer momento — não só no cadastro. Quem se
  formou depois precisa de caminho.

### O onboarding, e a decisão que já foi tomada

**O onboarding entra depois do login, e não como porta.** Um passo a passo antes do valor é
fricção antes do valor, e este produto só fica interessante quando existe um animal com
histórico na tela. O cadastro é de um passo.

**O que ele conduz, quando existir:** cadastrar o primeiro animal, convidar quem mais
cuida, e declarar credencial para quem for profissional. São os três cômodos — e a razão
de o onboarding vir depois é que **construir o corredor antes dos cômodos é construir para
trás**.

**Um caso que não é exceção:** o adotante chega com o animal já dentro, entregue pelo
abrigo. Ele não passa por "cadastre seu primeiro animal" — ele passa por *"o Code é seu, e
aqui está a vida dele"*. É o melhor primeiro momento que este produto tem para oferecer, e
merece desenho próprio.

---

## 10. A área do tutor

É um animal em profundidade, e existe todo dia. **O centro é o feed de pendências.**

### O que precisa existir

**O feed.** Tudo que reivindica ação, num lugar só: dose vencendo ou vencida,
antiparasitário no intervalo, orientação a cumprir, tema de casa da creche, convite
aguardando resposta, consentimento pendente. Precisa de hierarquia de gravidade que
sobreviva ao dia em que houver quinze itens, e precisa deixar **silenciar** sem parar o
registro.

Quando duas pessoas cuidam do mesmo animal, o feed diz **o que a outra já fez** — nunca
cobre a mesma coisa de duas pessoas em silêncio.

**Cumprir, ali mesmo.** Registrar a dose, confirmar o remédio, marcar o tema de casa. É o
gesto mais repetido do produto inteiro; se ele custar mais de um toque, o produto falha na
única coisa que faz todo dia.

**A carteira do animal.** Identificação (nome, espécie, raça, nascimento, microchip, RGA,
tatuagem, foto), vacinação, antiparasitário, peso como série, condições e alergias,
anexos. É o que se abre no balcão da creche e na sala do veterinário.

**A linha do tempo.** A vida inteira, ordenada por quando aconteceu, atravessando
custódias e organizações. Mostra quem registrou e em que contexto. Precisa suportar um
animal de 14 anos com centenas de eventos sem virar um rolo infinito — filtro por tipo e
por período são requisito, não enfeite.

**Registrar retroativo.** Lançar a carteirinha de papel, inclusive por foto. É trabalhoso
por natureza; o desenho decide se é tolerável.

**A rede de quem cuida.** Quem tem custódia, quem tem acesso, em que escopo, até quando —
e **conceder, delimitar e revogar**. Aqui mora o problema de design mais difícil da área:
tornar escopo compreensível para quem nunca ouviu a palavra.

**Convidar co-tutor**, e **transferir a titularidade** — com o sucessor identificado, e
dizendo com todas as letras quem deixa de enxergar o animal.

**Compartilhar o cartão:** gerar o link, escolher o escopo, definir validade, revogar, e
ver quem leu.

**O que a organização mandou:** recado, foto, avaliação do dia, alerta. Leitura, sem
resposta.

**Percepções**, quando existirem: sempre com a evidência à vista, e sempre dispensáveis.

**Exportar tudo, e sair.** A saída exige dar destino ao animal, e essa tela precisa ser
honesta em vez de dissuasiva.

---

## 11. A área de organização

Muitos animais em largura. É a área do expediente, e é onde a densidade importa.

**Uma tela só para clínica, creche e abrigo** — o que muda é a capacidade da organização.
E ela precisa funcionar inteira para **o profissional autônomo, sem organização nenhuma**.

### O que precisa existir

**A troca de contexto.** Em nome de quem estou agindo agora. Visível o tempo todo, porque
o registro guarda isso e não se corrige depois.

**A lista de animais alcançados**, com busca por nome, microchip e registro geral, filtro e
ordenação. Dezenas a centenas de linhas: é aqui que a área ganha ou perde.

**O animal, do ponto de vista profissional.** A mesma verdade da área do tutor, no escopo
concedido — com o que foi concedido e o que não foi dito com clareza, em vez de aparecer
vazio como se não existisse.

**Registrar ato clínico** — para quem tem credencial verificada. Diagnóstico, prescrição,
procedimento, com a possibilidade de **referenciar observações como evidência**.

**Registrar observação** — para monitor, voluntário, qualquer um com acesso. O formulário
precisa induzir fato e desencorajar interpretação; é design de campo, não de permissão.

**Corrigir** — registrando a correção, com o original visível ao lado. Nunca sobrescrever.

**Emitir orientação**, com o que fazer, frequência e prazo — e acompanhar o cumprimento.

**Mandar conteúdo ao tutor:** recado, foto, avaliação do dia, alerta.

**Quem está vencendo.** O recall que traz a clínica para o produto: lista de animais com
dose vencida ou vencendo, acionável.

**Membros e convites:** convidar, definir função, remover. E as **capacidades** da
organização, que decidem o que aparece para todo mundo.

**Para a creche:** matrícula com verificação de saúde, turmas, lotação, vagas, janela de
entrada, entrada e saída do dia.

**Para o abrigo:** animais sob custódia (inclusive sem tutor humano), a rede de lares
transitórios com as disponibilidades, a entrega e a devolução, e **a adoção** — que é
onde o registro inteiro passa para o adotante.

---

## 12. O cartão

Um acesso com escopo, **sem conta**. Nasce de um QR na coleira e é lido por quem socorre o
animal — possivelmente às 3h, num celular alheio, por alguém que nunca ouviu falar do
Petfy e não vai instalar nada.

**Só leitura, e só o escopo mínimo:** o que importa em emergência. Alergia, condição
crônica, medicação em curso, vacinação, contato de quem responde pelo animal, e como falar
com o veterinário dele.

**Regras duras:**

- **Nunca indexável nem adivinhável.** Registro de saúde atrás de um link que um buscador
  encontra é registro público, e nenhuma promessa deste produto sobrevive a isso.
- **Tem validade, e pode ser revogado** — o cartão precisa dizer o que fazer quando expira,
  para quem chegou por ele.
- **É a única superfície sem senha atrás**, então o escopo é a defesa inteira.

Desenhe pensando em pressa, luz ruim e uma mão só.

---

## 13. O recorte do app

Duas coisas, e nada mais: **avisar** e **confirmar num toque**.

- A pendência que chega sem o usuário ir procurar.
- *"Dei o remédio"* — o cumprimento virando evento, no menor gesto possível.

Cabe também, porque é o que faz abrir num dia sem pendência: **o recado e a foto que a
creche mandou**.

**Não cabe:** lançar carteirinha retroativa, a área de organização, gestão de acesso e
escopo, matrícula, adoção. Se você sentir que o app está ficando parecido com a web, ele
está errado.

**O aviso não espera o app** — o navegador já notifica. O que o app muda é a
confiabilidade disso.

---

## 14. As regras duras, os estados e o erro

### O que o design não pode quebrar

1. **Não existe excluir no registro.** Correção é sucessão, e o original continua visível.
2. **Todo evento mostra quem registrou e em que contexto**, e isso nunca é editável.
3. **Quando aconteceu ≠ quando foi registrado.** A linha do tempo ordena pelo primeiro.
4. **Nenhuma tela pergunta se a pessoa é tutora ou profissional.**
5. **Nenhuma custódia termina sem sucessor identificado.**
6. **Acesso nunca é interruptor:** tem escopo e prazo.
7. **Nenhum texto do produto contém hipótese diagnóstica** — inclusive vazios, erros,
   dicas e microcópia.
8. **Nenhuma superfície tem anúncio, marca sugerida ou "parceiro".**
9. **O registro clínico não tem resposta, nota do tutor, nota de avaliação nem denúncia** —
   tem o contato de quem registrou.
10. **Silenciar existe em tudo que cobra**, e silenciar nunca para o registro.

### Os estados que toda tela precisa ter desenhados

Não são exceção; são a maior parte da vida do produto:

- **vazio de verdade** — a conta nova, sem animal nenhum;
- **vazio por escopo** — existe, mas você não recebeu acesso a isso. **É diferente de não
  existir, e confundir os dois é mentir sobre o animal**;
- **carregando**, inclusive listas longas;
- **erro de carga**, e erro ao gravar — com o que o usuário escreveu preservado;
- **sem permissão para a ação**, dito antes do gesto e não depois;
- **expirado** — cartão vencido, convite vencido, acesso revogado;
- **conflito** — a outra pessoa já cumpriu isso.

### O erro, e o que a tela tem permissão de dizer

Erro fala do que aconteceu e do que fazer agora. Não pede desculpa duas vezes, não culpa o
usuário, não expõe detalhe técnico, e **nunca especula sobre a saúde do animal**. Quando a
causa é nossa, diz que é nossa. Quando o usuário não pode agir, não sugere que ele tente
de novo.

### Idioma e acessibilidade

**Português do Brasil**, escrito para quem nunca usou um sistema clínico. O texto do
produto é parte do design e vem no seu escopo: nomes de tela, rótulos de campo, vazios,
erros e confirmações.

**Acessibilidade não é camada final.** Contraste, alvo de toque, foco visível, leitura por
teclado e leitor de tela, e **nunca cor como único portador de significado** — vencido,
vencendo e em dia precisam se distinguir sem depender de vermelho, amarelo e verde.

---

## 15. O que entregar

1. **A identidade e o sistema visual.** Cor, tipografia, espaçamento, forma, iconografia,
   movimento — com as regras que os governam, e não só amostras. Precisa funcionar em tema
   claro e escuro.
2. **Os componentes**, no estado em que a vida real os encontra: com foco, desabilitado,
   em erro, carregando, com texto longo demais, e com nome de animal de 3 e de 40
   caracteres.
3. **As telas das seções 9 a 13**, na web, para tela grande e para a mão.
4. **O recorte do app** da seção 13.
5. **O cartão** da seção 12, pensado para emergência.
6. **Os estados da seção 14**, desenhados — não descritos.
7. **O texto do produto**, escrito por você.
8. **As decisões escritas.** Para cada escolha estrutural, uma linha dizendo o que ela
   resolve. É o que permite discordar de você com precisão, em vez de discutir gosto.

### O que não mandar

- Tela que dependa de dado que este documento não descreve.
- Qualquer coisa que responda *"o que o animal tem"*.
- Ilustração de banco de imagens no lugar de decisão de produto.
- Uma segunda porta de entrada por tipo de usuário.
- Fluxo em que apagar algo do registro seja possível.

### O critério pelo qual o trabalho será lido

Uma pessoa que nunca viu este produto abre a área do tutor às 23h com o animal passando
mal, e encontra o que precisa. Uma veterinária abre a área de organização numa segunda de
manhã com 40 animais para ver, e não perde tempo. E um adotante recebe a vida inteira de
um animal que ele acabou de conhecer, e entende que aquilo é dele agora.

Se o design fizer as três, está certo — independentemente do que eu teria desenhado.
