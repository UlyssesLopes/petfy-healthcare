# Produto

O que o Petfy é, para quem, e o que promete. Este documento é **anterior** aos
outros dois: o `README.md` descreve o que existe e por quê, o `ROADMAP.md`
descreve o que vem e em que ordem, e é aqui que se decide o que faz sentido
existir.

**O que não entra aqui:** tela, stack, endpoint, entidade, prazo. Se uma frase só
faz sentido sabendo que o backend é Java, ela é de outro documento.

**O que sai daqui:** o levantamento do que o backend precisa mudar para comportar
o produto descrito — que vira uma fase nova no `ROADMAP.md` — e só depois disso o
frontend.

## Estado — 2026-08-06

Documento em construção, aprovado **seção a seção**. Nenhuma seção é escrita antes
de a anterior fechar.

| # | Seção | Status |
|---|---|---|
| 1 | A tese | **Aprovada** em 2026-08-05 |
| 2 | Quem usa, e o que cada um ganha | **Aprovada** em 2026-08-05, com quatro ajustes do titular |
| 3 | O vocabulário canônico | **Aprovada** em 2026-08-06, em duas partes |
| 4 | O que o produto faz | **Aprovada** em 2026-08-06, em duas partes |
| 5 | As promessas | **Aprovada** em 2026-08-06 |
| 6 | O que o Petfy não é | **Aprovada** em 2026-08-06 |
| 7 | Horizonte | **Aprovada** em 2026-08-06 |
| 8 | Decisões em aberto | **Consolidada** em 2026-08-06 — 12 de 14 resolvidas |
| 9 | O lado do cliente | **Aprovada** em 2026-08-06, em cinco partes |

**As oito primeiras seções fecharam em 2026-08-06**, e nenhuma delas mudou desde
então. A seção 9 é posterior ao fechamento e não o contradiz — o porquê está na 9.1.
Ela abriu as decisões 15, 16 e 17, que vivem na 9.5 e não na seção 8.

**Retomar em:** a Fase 5 do `ROADMAP.md`, reescrita sobre esta seção.

---

## 1. A tese

> **O Petfy é o registro único, contínuo e vitalício da vida de um animal — e o
> fio que liga todos que cuidam dele a esse registro.**

Tudo no produto existe para servir um único ser: o animal. Pessoas e instituições
entram e saem da vida dele — tutores, veterinários, clínicas, creches, lares
transitórios, abrigos —, mas o registro é do animal e sobrevive a todas elas. Quem
cuida contribui com o registro; ninguém o possui.

**Três invariantes, que valem para qualquer decisão futura:**

**O animal é o centro, não a conta.** O histórico não pertence a quem cadastrou.
Ele acompanha o animal por mudança de tutor, adoção, passagem por abrigo ou troca
de clínica. Um produto centrado na conta perde a história do animal exatamente
quando ela fica mais valiosa — na transferência.

**Nada se apaga, tudo se sucede.** Correção não sobrescreve: encerra e registra.
Já é assim em vacina, atendimento e condição de saúde, e passa a ser regra geral
do produto. O valor de um histórico de saúde é proporcional à confiança de que ele
não foi editado.

**Acesso é concedido, temporário e auditável.** Ninguém alcança o registro de um
animal por ser profissional, por ter o link ou por ter estado lá antes. Alcança
porque quem responde pelo animal concedeu, enquanto durar, e com rastro de quem
leu o quê.

**Nenhuma custódia termina sem sucessor.** Ninguém "solta" um animal: repasse é a
criação do vínculo seguinte, não o encerramento do anterior. Toda transferência
tem destino identificado, nenhum período fica sem alguém respondendo, e a linha do
tempo do animal sempre avança. Vale para todos que detêm custódia — abrigo, lar
transitório e também o tutor comum.

> Elevado a invariante em 2026-08-06. Nasceu como regra do lar transitório (2.5) e
> se mostrou geral ao ser definida a custódia (3.4).

**A consequência que dá nome ao produto:** por manter o registro inteiro em vez de
fragmentos por prestador, o Petfy é o único ponto do ecossistema capaz de perceber
padrão ao longo da vida do animal — e de agir sobre ele antes que o tutor perceba
sozinho.

---

## 2. Quem usa, e o que cada um ganha

Um princípio antes da lista: **nenhum ator entra por altruísmo com o ecossistema.**
Cada um precisa de uma razão egoísta e imediata para usar o Petfy sozinho, mesmo
que ninguém mais use. O que liga os seis é consequência, não pré-condição.

### 2.1 Tutor — o único ator obrigatório

**A dor.** A vida do animal está espalhada: carteirinha de papel que desbota, laudo
no WhatsApp da clínica, o resto na memória. Na hora que a informação vale —
emergência, viagem, matrícula na creche, troca de veterinário, o animal passando
mal às 23h — ele não tem.

**Gancho de entrada.** A carteira no celular e o aviso do que vence. É o que ele já
tenta fazer com foto e alarme no telefone, e faz mal.

**O que ele dá.** O cadastro do animal e — juridicamente decisivo — **o
consentimento que autoriza todo o resto do ecossistema a existir**. Sem tutor,
ninguém alcança nada.

**O que ele não pode.** Alterar, apagar **ou contestar** o que um profissional
registrou. O registro clínico é do animal e da responsabilidade de quem o
assinou — não é uma conversa. O tutor é **informado** do que foi registrado e
recebe **os dados de contato do profissional e da organização**; qualquer
divergência se resolve com quem registrou, fora do registro. Ele segue podendo
exportar tudo e sair do produto.

> Ajuste do titular, 2026-08-05. A versão anterior previa contestação e anexação
> de versão do tutor; foi recusada. Não há canal de contestação dentro do
> registro.

### 2.2 Veterinário — a pessoa, não o lugar

**A dor.** Recebe o animal sem histórico e depende do que o tutor lembra — que é
pouco e frequentemente errado. E o que ele registra fica preso no sistema da
clínica onde trabalha: se ele sai, o trabalho dele não vai junto.

**Gancho de entrada.** Ver o histórico real antes de encostar no animal, e
registrar o atendimento sem burocracia.

**O que ele dá.** O dado de maior densidade clínica: diagnóstico, procedimento,
prescrição.

**O que ele não pode.** Alcançar animal sem concessão do tutor, nem levar o
histórico embora ao trocar de clínica — o registro é do animal.

**Identidade profissional pelo CRMV** *(sugestão do titular, 2026-08-05, a fechar
na seção 3).* O veterinário seria identificado pelo registro profissional, e não
só por e-mail. Faz sentido de domínio — o CRMV é o que o identifica no mundo real,
segue com ele entre clínicas e é público. Duas consequências a decidir junto: se
o CRMV vira **chave de identidade** (unicidade, e possivelmente login) ou apenas
**atributo verificado**; e que fazer quando ele não puder ser conferido com o
conselho, já que hoje o campo existe e não é verificado por ninguém.

**Duas coisas que o backend hoje impede e o mundo real exige:**

- **O veterinário autônomo.** Atendimento domiciliar é enorme no Brasil e não tem
  clínica. Hoje o cadastro exige convite de uma clínica ou criar uma nova — o
  autônomo é obrigado a inventar uma clínica que não existe.
- **O veterinário em mais de uma clínica.** Já registrado como dívida no roadmap.
  Com o ecossistema aberto, ele pode ainda ser voluntário de um abrigo e consultor
  de uma creche.

### 2.3 Clínica — organização, e a dor dela não é a do vet

**A dor.** A clínica não sofre de falta de histórico; ela sofre de **paciente que
não volta**. Reforço de vacina esquecido é receita perdida e é o vínculo com o
cliente esfriando.

**Gancho de entrada.** Recall automático de quem está vencendo, e os animais que
ela atende num lugar só.

**O que ela dá.** Volume, continuidade e credibilidade — pet cujo histórico veio de
clínica vale mais que histórico digitado pelo tutor.

**O que ela não pode.** Usar o alcance concedido como lista de marketing. Essa é
uma promessa que o produto faz por escrito, porque no dia em que um tutor receber
promoção de banho e tosa por ter autorizado acesso ao prontuário do cachorro, a
confiança acaba — e ela é o ativo inteiro do Petfy.

### 2.4 Creche

**A dor, e são três.** Turma para gerir (quem vem que dia, quem está dentro agora).
Comunicação com tutor que hoje é um WhatsApp caótico de 40 pessoas e vídeo perdido.
E **responsabilidade**: aceitar animal sem vacina em dia, ou sem saber de uma
alergia, é risco de surto na turma e de processo.

**Gancho de entrada.** A matrícula que exige comprovação de saúde — que ela hoje
resolve pedindo foto da carteirinha por WhatsApp e conferindo no olho — e o diário
que o tutor adora receber.

**O que ela dá — e é o dado mais subestimado do ecossistema.** Observação diária,
sistemática, por gente treinada, sobre comportamento, apetite, energia,
sociabilidade e eliminação. **Nenhum veterinário tem isso.** O vet vê o animal 20
minutos por ano; a creche vê 8 horas por dia, 3 dias por semana. É a série temporal
mais rica que vai existir sobre um animal — e é ela que enxerga "o Code está mais
quieto há duas semanas" antes de virar doença.

**O que ela produz para o tutor:**

- matrícula e turma, com check-in e check-out do dia
- **avaliação diária** — comportamento, apetite, energia, interação, eliminação
- **recado e foto** do dia
- **tema de casa** — o que treinar em casa até a próxima aula
- **alerta** — "hoje ele não comeu", "mancou da pata direita", "brigou"

**Gestão de capacidade** *(acréscimo do titular, 2026-08-05, a detalhar na seção
4).* A creche opera com limite físico e legal, e o produto precisa respeitá-lo:

- **limite de turmas** que a unidade comporta
- **limite de alunos por turma**
- **divulgação de vagas** — quantas sobram, e para quem isso é visível
- **tempo de check-in** — a janela de entrada, e o que acontece fora dela

**O que ela não pode.** Escrever no prontuário clínico. **A creche registra
observação; ela não diagnostica.** Essa fronteira precisa ser dura no modelo, senão
em seis meses tem monitor de creche lançando "suspeita de displasia" no histórico
médico do animal, e o valor clínico do registro morre. O caminho certo é o alerta
da creche **virar gancho para o veterinário** — e aí o diagnóstico vem de quem
pode dar.

### 2.5 Lar transitório — dois perfis, uma mecânica

**O eventual.** Pessoa comum que topa receber um animal por um período. Nunca vai
criar uma conta de "lar transitório", nunca vai preencher cadastro institucional.
Ela liga uma disponibilidade — período, espécie, porte, quantos — e desliga quando
não puder mais.

**O recorrente.** Faz isso o tempo todo, quase sempre ligado a um abrigo ou ONG.
Mesma mecânica, com histórico acumulado e reputação — quantos animais passaram,
quanto tempo, como saíram.

**A dor do eventual.** Recebe um animal que não conhece: não sabe o que ele tem, se
é vacinado, se é alérgico, se toma remédio, se pode ficar com outro cachorro. E tem
medo de responsabilidade — se o animal adoecer sob a guarda dele, de quem é a conta.

**A dor do recorrente.** O histórico do período dele evapora no repasse. Ele viveu
com o animal 4 meses, aprendeu tudo sobre ele, e entrega uma caixa e um "ele é
bonzinho".

**Gancho de entrada.** Receber o animal com ficha completa, prazo definido e
responsabilidade explícita — e devolver com o que aconteceu registrado.

**O que ele dá.** Continuidade do registro **no período mais frágil da vida do
animal**, que é justamente quando ele hoje não existe.

**A linha do tempo nunca para** *(regra do titular, 2026-08-05).* Não existe saída
de custódia para lugar nenhum. **A ONG, o abrigo ou o lar transitório só repassa o
animal cadastrando o novo tutor** — o repasse é a criação do vínculo seguinte, não
o encerramento do anterior. Um animal não pode ficar órfão de registro, e a linha
do tempo dele sempre avança: toda transferência tem destino identificado, e nenhum
período fica sem alguém respondendo.

> Isso eleva a regra ao nível dos invariantes da seção 1, e vale para todos os
> atores que detêm custódia — não só o lar transitório.

**O que ele não pode.** Reter o animal ou o registro fora do combinado. Início,
prazo e fim da custódia são eventos registrados, não acordo verbal.

### 2.6 Abrigo e ONG

**A dor.** Dezenas a centenas de animais em planilha, voluntário que troca toda
semana, e a **adoção que volta** — devolução por doença ou comportamento que
ninguém contou ao adotante.

**Gancho de entrada.** Gestão dos animais e da rede de lares transitórios num lugar
só.

**O que ele dá — e é o mais valioso do produto inteiro.** Duas coisas:

1. **Animal que nasce no sistema sem tutor humano.** Resgate, animal de rua,
   ninhada. Hoje o backend não permite isso: pet exige dono.
2. **O momento da adoção.** É onde o Petfy prova a tese: o registro inteiro —
   resgate, tratamento, castração, meses no lar transitório, comportamento
   observado — passa para o adotante. O abrigo entrega um animal com biografia, e o
   adotante ganha uma conta no Petfy que ele não pediu e não quer perder.

**O que ele não pode.** Continuar mandando no registro depois de repassar. Ele vira
leitor do que produziu, não dono do animal.

### 2.7 O que a lista revela

**A cadeia de aquisição existe e é assimétrica.** Ninguém aqui traz o próximo por
generosidade:

- A **creche** obriga o tutor a comprovar saúde → o tutor entra.
- A **clínica** quer o paciente de volta → o tutor entra.
- O **abrigo** entrega registro na adoção → o adotante entra já com o animal dentro.
- O **tutor**, sendo muitos, puxa clínica e creche por demanda ("vocês estão no
  Petfy?").

O tutor é o único que **precisa** existir; os outros cinco são o motivo pelo qual
ele aparece. Isso tem consequência direta na ordem de construção — seção 7.

**Um estado novo que a lista exige:** *animal sem tutor humano*, sob
responsabilidade de organização. Abrigo e resgate dependem dele, e o modelo atual
não o comporta.

**Adjacentes que a mesma mecânica já cobriria, e que não estão sendo abertos:**
hotel, banho e tosa, adestrador, pet sitter. Registrado na seção 6 como delimitação
consciente, não esquecimento — nenhum deles muda o modelo, o que é bom sinal sobre
o modelo.

---

## 3. O vocabulário canônico

Dezessete conceitos, aprovados em 2026-08-06. Cada um traz, entre parênteses, o
nome que teria no código — o backend é em inglês, e essa ponte é o que torna o
levantamento de delta mecânico em vez de interpretativo.

**Nenhuma entidade foi inventada para creche, lar transitório ou abrigo.** As três
frentes novas cabem no vocabulário abaixo, e é esse o teste de que ele está certo.

### 3.1 Pessoa (`Person`)

**Um ser humano com uma conta. Sem tipo.**

Não existe "conta de tutor" nem "conta de veterinário". Existe uma pessoa, e o que
ela pode fazer decorre dos vínculos que ela tem — não do formulário que preencheu
ao se cadastrar.

**Por quê:** a seção 2 produziu quatro pessoas que o modelo atual não representa —
a veterinária que tem cachorro, o vet autônomo, a pessoa comum que vira lar
transitório num verão, o voluntário de abrigo que também é tutor. Nenhuma é caso
raro; é o caso comum de quem gosta de animal a ponto de usar o Petfy.

**O que encerra:** as duas tabelas (`Owner`, `Vet`) e o papel derivado de qual
delas o e-mail aparece. O namespace único de e-mail entre elas, conferido hoje no
cadastro para o login não ficar ambíguo, deixa de ser necessário — não há dois
lados.

**Regra dura:** a pessoa nunca declara o próprio papel. Declara quem é; o sistema
deriva o que ela alcança.

### 3.2 Contexto (`Context`)

**O ponto de vista ativo da pessoa: ela mesma, ou uma organização em nome de quem
age.**

Uma pessoa com três vínculos tem três contextos. O que ela vê, pode e registra muda
conforme o contexto, e **o registro guarda em qual contexto foi feito** — "a Ana
registrou" e "a Ana, pela Clínica Vet Norte, registrou" são fatos diferentes, e só
o segundo tem responsabilidade institucional.

### 3.3 Animal (`Animal`)

**O centro. Existe por si, independente de qualquer conta.**

O animal não é criado *por* alguém e não pertence a ninguém — é **registrado** por
alguém e sempre tem quem responda por ele, o que é outra coisa.

**O que destrava:** o animal sem tutor humano que a 2.6 exige — resgate, animal de
rua, ninhada nascida no abrigo. Hoje `Pet` exige dono por chave estrangeira, e é
por isso que apagar conta com pet é recusado pelo banco.

**Regra dura:** animal não se apaga. Morte, perda e transferência são eventos, e a
linha do tempo continua existindo depois de cada um.

**Renomeação aprovada:** `Pet` passa a ser `Animal` no vocabulário. "Pet" pressupõe
dono, e metade dos atores da seção 2 lida com animal que naquele momento não é pet
de ninguém.

### 3.4 Custódia (`Custody`)

**Quem responde pelo animal, desde quando, até quando, e por quê.** Generaliza
tutor, lar transitório e abrigo.

Tem: quem (pessoa ou organização), início, fim previsto (pode ser indefinido), fim
real, natureza (definitiva, transitória, institucional, resgate), motivo de
encerramento e sucessor.

**Regra dura — é o quarto invariante da seção 1:** nenhuma custódia termina sem
sucessor.

**Consequência para o tutor comum:** ele não "sai" do produto com o animal. Exporta
e apaga a conta, mas o animal precisa de destino — outro tutor, uma organização, ou
encerramento explícito (óbito, perda, entrega). Isso resolve a limitação conhecida
de apagar conta com pet: passa a ser recusada por regra de produto, com caminho de
saída, em vez de por chave estrangeira sem explicação.

### 3.5 Acesso (`Grant`)

**Quem pode ler ou escrever no registro do animal, e até quando.**

Custódia e acesso são hoje a mesma coisa no backend — o papel no `PetTutor` decide
o alcance. **Passam a ser separados:**

|  | Custódia | Acesso |
|---|---|---|
| Responde pelo animal | Sim | Não |
| Quantos ao mesmo tempo | Poucos, e sempre há um principal | Muitos |
| Origem | Aquisição, adoção, resgate, repasse | Concessão de quem tem custódia |
| Termina | Só com sucessor | A qualquer momento, sem sucessor |
| Exemplos | Tutor, lar transitório, abrigo | Clínica, creche, o filho adulto, o cuidador, o vet autônomo |

**Por quê:** a clínica não responde pelo animal e nunca vai responder — lê e
escreve por concessão. Misturar as duas coisas faz "conceder acesso à clínica"
parecer parente de "transferir titularidade", quando são opostos, e obrigaria a
creche a virar co-tutora de 40 animais.

Os três níveis atuais continuam válidos: `HOLDER` migra para custódia; `EDITOR` e
`VIEWER` viram níveis de acesso.

**Regra dura:** todo acesso é concedido, tem prazo ou revogação, e é auditável. Já
é assim em `PetClinicAccess` e `SensitiveAccessLog` — vira regra geral.

### 3.6 Organização (`Organization`)

**Um coletivo que atua sobre animais, com membros e capacidades.**

Clínica, creche e abrigo não são três entidades: são a mesma exercendo conjuntos de
capacidades diferentes.

| Capacidade | Clínica | Creche | Abrigo |
|---|---|---|---|
| Registrar ato clínico | ✅ | ❌ | ✅ (com vet membro) |
| Registrar observação | ✅ | ✅ | ✅ |
| Comunicar-se com o tutor | ✅ | ✅ | ✅ |
| Deter custódia | ❌ | ❌ | ✅ |
| Gerir turma e vaga | ❌ | ✅ | ❌ |
| Manter rede de lares transitórios | ❌ | ❌ | ✅ |

**Por quê:** ONG com clínica própria faz as três; creche que hospeda faz duas.
Modelar como tipos obriga a escolher um e mentir sobre o resto.

**O que encerra:** `Clinic` como entidade específica — vira organização com perfil
de capacidades clínicas.

### 3.7 Membro (`Membership`)

**Uma pessoa dentro de uma organização, com função.**

Veterinário, monitor, voluntário, administrador. A função decide o que a pessoa
exerce das capacidades que a organização tem: a creche registra observação, mas
quem registra é o monitor; o abrigo registra ato clínico, mas só através do vet
membro.

**O que encerra:** `Vet.clinic` como `@ManyToOne` singular. Uma pessoa tem N
vínculos em N organizações — resolve de uma vez o vet em duas clínicas, o vet
voluntário de abrigo e o dono de creche que também é tutor.

**Organização é opcional para atuar.** O veterinário autônomo é uma pessoa com
credencial profissional que recebe acesso direto do tutor. Quem atende é o
profissional; a clínica é onde ele atende.

### 3.8 Vínculo (`Enrollment`)

**Relação contínua entre um animal e uma organização, sem transferência de
custódia.** Paciente da clínica e aluno da creche são a mesma coisa; o tutor
continua tutor o tempo todo.

**Por que não é acesso:** acesso é permissão, vínculo é relacionamento. A clínica
que atendeu uma vez em 2023 teve acesso; a clínica onde o animal se trata há 6 anos
tem vínculo. É o vínculo que justifica recall, matrícula, turma, mensalidade e
diário — sem ele, "meus pacientes" e "meus alunos" viram "a lista de quem me deu
permissão", que some quando o acesso expira.

**Regra dura:** vínculo tem estado (ativo, suspenso, encerrado) e não some do
histórico ao encerrar — ter estudado dois anos naquela creche é biografia.

### 3.9 Evento (`Event`)

**Algo que aconteceu com o animal, num instante, registrado por alguém num
contexto.** Vacina, antiparasitário, atendimento, pesagem, observação, check-in,
mudança de custódia, adoção, óbito.

**O que não é:** uma tabela genérica com campo JSON. Isso destruiria o que o
backend já sabe — que vacina tem validade e próxima dose, que condição tem gravidade
e encerramento, que peso é série.

**O que é:** todo evento tem um **núcleo comum** — identidade, tipo, instante em que
aconteceu, instante em que foi registrado, quem registrou, em que contexto, e se é
dado de saúde — e cada tipo mantém estrutura e regras próprias. O núcleo permite
ordenar, filtrar e auditar tudo junto; a especialização preserva o valor clínico.

**Regra dura:** *quando aconteceu* e *quando foi registrado* são campos distintos.
A creche registra às 18h o que viu às 9h; o tutor cadastra hoje a vacina de 2019 da
carteirinha de papel. Linha do tempo ordenada por data de digitação mente.

### 3.10 Linha do tempo (`Timeline`)

**A leitura ordenada dos eventos de um animal, atravessando todas as custódias,
organizações e prestadores da vida dele.** É isto o produto.

**Regra dura:** a linha do tempo não recomeça na transferência. O adotante recebe a
vida inteira — resgate, tratamento, os quatro meses no lar transitório —, não uma
ficha em branco com a data de hoje.

**O que encerra:** a ideia de o cliente montar a cronologia chamando seis endpoints
e ordenando em memória. A linha do tempo é do backend, porque é regra de domínio —
o que entra, o que quem vê pode ver, como se ordena —, não formatação de tela.

### 3.11 Observação e ato clínico

**Observação:** o que alguém viu — não comeu, mancou, vomitou, brigou.
**Ato clínico:** diagnóstico, prescrição, procedimento; carrega responsabilidade
profissional.

**Quem pode:** observação, qualquer um com acesso — tutor, monitor, lar transitório,
voluntário. Ato clínico, só pessoa com credencial profissional — por isso o CRMV é
atributo da pessoa (3.7), não do vínculo com a clínica.

**Regra dura:** observação nunca vira ato clínico sozinha. Pode ser **referenciada**
por um: o veterinário lê "mancou da direita nos últimos 3 dias" registrado pela
creche e emite um diagnóstico que aponta para aquelas observações como evidência.

**O alerta da creche** é uma observação com sinalização de urgência: chega ao tutor
e a quem tem acesso clínico, entra na linha do tempo como observação, e pode ser
puxado como evidência por um atendimento posterior. A creche nunca escreve no
prontuário, e o que ela viu chega a quem pode diagnosticar.

Isso também dá o critério objetivo de **dado de saúde** para efeito de LGPD: ato
clínico sempre é; observação quase sempre é; recado e foto não são. O
`SensitiveAccessLog` passa a ter regra clara sobre o que cobre.

#### Construída em 2026-08-07, e o "quase sempre" foi fechado

Observação estava descrita aqui desde o documento fundador e **não existia no
backend** — descoberto ao levantar o delta de contrato do passo 4. A ausência
deixava três coisas desta seção sem lugar nenhum: a creche não tinha onde registrar
o que viu, o ato clínico não tinha o que referenciar como evidência, e o alerta da
creche não existia porque o que ele qualifica não existia.

Duas decisões que o texto acima não tomava, e que a construção obrigou a fechar:

**Observação é *sempre* dado de saúde.** O *"quase sempre"* era honesto como
descrição e inviável como regra: ele abre uma classificação que teria de ser
decidida por registro. Pelo autor, e a mesma frase muda de regime conforme quem
digitou. Por um campo, e a classificação de dado sensível fica na mão de quem está
com pressa no balcão. **Sempre** é mais restritivo do que o necessário em "brincou
muito hoje", e nunca vaza por classificação errada — que é o erro que não tem
conserto.

**Observação tem escopo de concessão próprio**, e não o do prontuário. Quem mais
escreve observação é a creche; colocá-la no prontuário faria com que dar à creche
acesso ao que **ela mesma escreve** entregasse junto todo atendimento clínico do
animal — exatamente o que a 3.5 e o escopo existem para impedir, e o caso que os
originou. Consequência: concessão anterior a 2026-08-07 não tem esse escopo e não
vê observação, o que é o padrão correto — ninguém concedeu acesso a algo que não
existia quando concedeu.

### 3.12 Orientação (`CareInstruction`)

**Instrução dada a quem cuida do animal, com prazo e confirmação de cumprimento.**

Um conceito, três usos hoje separados: prescrição do veterinário, medicação e
tratamento contínuo (item parado em *Fica para depois do frontend*, no roadmap) e
tema de casa da creche.

Tem: quem emitiu (pessoa + contexto), o que fazer, frequência, até quando, e o
registro de cada confirmação.

**Regra dura:** a orientação segue a **custódia**, não a pessoa. Se o animal volta
do lar transitório para o abrigo no meio de um tratamento de 21 dias, o remédio
continua — a instrução passa para quem assumiu, com o que já foi cumprido
preservado.

### 3.13 Conteúdo (`Update`)

**O que uma organização escreve para o tutor ler.** Recado, foto, avaliação do dia.

**Regra dura:** conteúdo não é registro clínico e não entra no prontuário. Mas é
biografia — a foto do primeiro dia de creche faz parte da vida do animal e aparece
na linha do tempo.

**Por que é do produto e não enfeite:** é o único conceito que existe para dar
prazer em vez de resolver problema. É o que faz o tutor abrir o app num dia em que
nada vence — e app de saúde que só fala quando tem má notícia é app que ninguém
abre.

### 3.14 Pendência (`DueItem`)

**Tudo que reivindica ação de alguém, com prazo.** Dose vencendo, orientação a
cumprir, tema de casa, matrícula com vacina irregular, convite aguardando resposta,
consentimento pendente.

**Não é registrada, é derivada.** A agenda de vacinas é o primeiro caso, e o roadmap
já a reconheceu como "a única parte do sistema que produz informação em vez de
devolver o que foi gravado".

**Por que precisa de nome:** dá lugar único a tudo que o produto vier a cobrar do
usuário. Sem ela, cada funcionalidade nova inventa o próprio aviso, no próprio
canto.

### 3.15 Percepção (`Insight`)

**Uma leitura sobre os fatos, produzida pelo Petfy, que ninguém registrou.** É o
"o Code teve aquele episódio há 6 meses, que tal uma dieta específica?" — a promessa
que fecha a seção 1.

**Regra dura, não negociável:** percepção nunca é fato e nunca é diagnóstico. Não
entra na linha do tempo como evento, sempre mostra a evidência que a gerou, e sempre
pode ser dispensada pelo tutor. No dia em que uma percepção do sistema for lida como
opinião clínica, ela vira risco jurídico e o produto perde o direito de ter uma.

**De onde nasce:** da linha do tempo atravessando custódias e organizações. Nenhum
ator do ecossistema tem esse insumo — a clínica vê os atendimentos dela, a creche vê
a turma dela, o tutor vê o que lembra. Só o Petfy vê o animal inteiro. A percepção
não é feature: é a consequência da tese.

### 3.16 Disponibilidade (`Availability`)

**A oferta de quem pode receber um animal.** Do lar transitório, principalmente:
período, espécie, porte, quantos, e restrições reais ("não pode com gato", "tenho
criança pequena", "não recebo animal em tratamento").

**Regra dura:** é oferta, não compromisso. Liga e desliga a qualquer momento e não
gera custódia — a custódia nasce quando um animal é de fato recebido. É o que
permite a pessoa comum da 2.5 entrar e sair sem virar instituição.

### 3.17 Lotação (`Occupancy`)

**O limite físico e legal do que uma organização comporta.** Da creche: turmas,
alunos por turma, vagas abertas, janela de check-in. Do abrigo: quantos animais
cabem, quantos há.

**Regra dura:** vaga divulgada é decisão da organização, não do sistema. A creche
que quer aparecer com "3 vagas" escolhe isso; a que não quer, não aparece.

> Nota de nomenclatura: **capacidade** (3.6) é o que a organização *pode fazer*;
> **lotação** (3.17) é *quantos cabem*. As duas palavras disputavam o mesmo termo.

### O vocabulário fechado, em uma frase

**Uma pessoa, agindo num contexto, tem custódia de um animal ou acesso a ele.
Organizações têm membros, capacidades e lotação, e mantêm vínculo com os animais que
atendem. Tudo o que acontece é evento, e a soma ordenada dos eventos é a linha do
tempo — de onde saem as pendências que cobram ação e as percepções que ninguém
registrou.**

## 4. O que o produto faz

Aprovada em 2026-08-06. Organizada **por fluxo**, e não por ator: quase toda
capacidade importante do Petfy é exercida por mais de um ator, e uma lista por ator
repetiria a seção 2 escondendo isso. Como efeito colateral, esta lista **é** a lista
de capacidades para o levantamento de backend.

### 4.1 A vida registrada

**Quatro portas de entrada**, uma para cada ator da cadeia de aquisição (2.7):

1. **O tutor cadastra** — a porta comum.
2. **A organização registra animal sob custódia** — o abrigo com um resgate, sem
   tutor humano (3.3).
3. **A clínica registra ao atender** — animal que chega em emergência e cujo tutor
   ainda não usa o Petfy.
4. **Importação do que existe em papel** — a carteirinha por OCR, que já existe.

**Identificação é múltipla e nenhuma é obrigatória:** microchip, tatuagem, RGA,
nome de casa. O produto não pode exigir o que metade dos animais do Brasil não tem
— mas onde há microchip, ele é a chave que responde "esse animal já está no Petfy?"
no resgate e na adoção.

**A linha do tempo se forma sozinha**, ordenada por *quando aconteceu* (3.9): o
tutor cadastra hoje a vacina de 2019 e ela aparece em 2019.

| Ator | Contribui com | Não pode |
|---|---|---|
| Tutor | Cadastro, histórico retroativo, observação, peso, anexo | Ato clínico |
| Veterinário | Ato clínico, prescrição, observação | Registrar sem acesso concedido |
| Clínica / abrigo (via membro) | O que a capacidade da organização permitir (3.6) | Exceder a capacidade da organização |
| Creche | Observação, check-in, conteúdo | Ato clínico |
| Lar transitório | Observação, peso, anexo, o que ocorreu na estadia | Ato clínico |

**Regra dura:** todo evento sabe quem o registrou e em que contexto (3.2), e isso
nunca é editável. É o que separa um registro que um veterinário aceita de um caderno
digital.

**Falta no backend:** óbito como evento — a linha do tempo encerra, não some, e
cessam os lembretes; e **animal perdido** como estado, diferente de custódia
encerrada, e onde o microchip vale mais.

### 4.2 O cuidado que se cobra

**O que gera pendência:** dose vencendo ou vencida, antiparasitário no intervalo,
orientação com prazo, tema de casa, matrícula com saúde irregular, convite
aguardando resposta, consentimento pendente.

**A quem se cobra:** a **quem tem custódia agora**, não a quem cadastrou o animal.
O tutor que deixou o cachorro no lar transitório não é quem dá o remédio às 8h.
Decorre de 3.12 e não é opcional — sem isso o produto cobra a pessoa errada
exatamente quando a informação vale mais.

**Cumprir é evento.** "Dei o remédio" entra na linha do tempo, com quem e quando —
é o que transforma orientação em histórico de aderência, o dado que o veterinário
nunca tem quando o tratamento não funciona.

**Regras duras:**

- **Silêncio é funcionalidade.** O tutor precisa poder silenciar sem que o registro
  pare. Produto de saúde que não pode ser calado é desinstalado, e aí para de
  registrar também.
- **Nunca cobrar a mesma coisa de duas pessoas sem dizer que a outra já fez.** Dois
  tutores dando o mesmo remédio é dano, não incômodo.
- **Urgência é escassa.** Se tudo interrompe o dia, nada interrompe.

**Falta no backend:** orientação; agenda além de vacina; confirmação de cumprimento;
canal além de e-mail; preferência de notificação por usuário.

### 4.3 A custódia que muda de mão

**Cinco movimentos, um mecanismo:** transferência entre tutores, adoção por abrigo,
entrega a lar transitório, devolução, e resgate (custódia nascendo sem antecessor).

**Todo movimento tem sucessor identificado.** A ONG não "dá" o animal: ela cadastra
o novo tutor, e é isso que encerra a custódia dela.

| Atravessa a transferência | Não atravessa |
|---|---|
| A linha do tempo inteira | Os acessos concedidos pelo tutor anterior |
| Orientações ativas, com o já cumprido | Preferências e canais de notificação |
| Condições, alergias, anexos | Pendências que eram do antecessor por outro motivo |

**Regra dura, de privacidade:** **acessos não são herdados.** A clínica do tutor
anterior deixa de enxergar o animal no instante da transferência; quem quiser
mantê-la, o novo tutor concede de novo. O oposto faria o novo tutor herdar, sem
saber, uma plateia que não escolheu.

**O tutor anterior** segue a regra do abrigo (decisão 7): mantém leitura do período
em que respondeu pelo animal, salvo revogação do novo. *Casos de conflito real —
separação, venda mal resolvida, denúncia de maus-tratos — merecem revisão antes de
virar implementação.*

**Falta no backend:** custódia como conceito (hoje só `HOLDER` com
`transfer-holder`); prazo e natureza; revogação em cascata dos acessos na
transferência; adoção com cadastro obrigatório do sucessor; resgate sem antecessor.

### 4.4 A convivência com organização

**Uma organização alcança um animal de dois jeitos:** por concessão de quem tem
custódia, ou por ter custódia (só o abrigo).

**O vínculo nasce depois do acesso e vive mais que ele:** o animal vira paciente ou
aluno, e isso não expira junto com uma permissão pontual.

**A matrícula na creche é o fluxo mais rico do produto**, e usa quase tudo que já
existe: a creche pede o animal, o produto responde se a saúde está regular (derivado
da agenda), e a matrícula é aceita ou fica pendente. É a carteira compartilhada e a
agenda de vencimento sendo usadas por quem tem motivo profissional para conferir.
Depois: turma, lotação, janela de check-in, check-in e check-out do dia.

**Regra dura, a mais importante deste fluxo:** **acesso tem escopo.** A creche
precisa de alergia, vacinação, medicação em curso e contato de emergência — não do
prontuário clínico inteiro. Hoje o acesso é tudo ou nada, e manter assim significa
que matricular o cachorro na creche entrega a ela o histórico completo de doenças do
animal. Acesso é concedido, temporário, auditável — e agora também **delimitado**.

**Falta no backend:** escopo de acesso; vínculo; turma, lotação e janela; check-in e
check-out; regularidade de saúde como resposta do produto, e não como leitura manual
da carteirinha.

### 4.5 O que se conta ao tutor

**O que a organização manda:** recado, foto, avaliação do dia, alerta.

**Regra dura — alerta relata fato, não interpretação.** "Não comeu hoje", "mancou da
pata direita", "brigou no pátio" são observações. "Acho que está com dor", "parece
displasia" não são. A linguagem do produto tem de impedir isso, não só a permissão:
campo de texto livre sem orientação vira diagnóstico de monitor em três meses.

**Regra dura — o produto não alarma.** O alerta chega como *o que foi observado* e
*o que fazer a respeito* ("vale procurar o veterinário"), nunca como hipótese
clínica.

**Comunicação é de mão única, por ora.** A organização conta, o tutor lê, e para
conversar existe o contato direto — coerente com o que ficou decidido em 2.1. Abrir
mensageria bidirecional é abrir moderação, não lida, histórico e responsabilidade
sobre o que foi dito: é produto novo, não campo novo.

**Falta no backend:** conteúdo não existe; mídia existe só como anexo de prontuário;
não há push; não há preferência de notificação.

### 4.6 O que só o Petfy enxerga

| Forma de percepção | Exemplo |
|---|---|
| **Recorrência** | Terceiro episódio urinário em 18 meses |
| **Tendência** | Peso subiu 12% em 6 meses |
| **Ausência** | Não passa por veterinário há 2 anos; reforço nunca aplicado |
| **Sazonalidade** | Coceira registrada toda primavera, três anos seguidos |
| **Mudança de comportamento** | Creche relata queda de energia por três semanas |

As cinco dependem de insumo que nenhum ator isolado tem: a tendência de peso vem do
tutor e da clínica, a mudança de comportamento vem da creche, a recorrência
atravessa duas clínicas e um lar transitório.

**Regras duras:** nunca diagnostica — descreve o padrão, mostra a evidência e sugere
procurar quem pode; é sempre dispensável, e dispensar ensina o produto a não
repetir; nunca é fato na linha do tempo.

**Percepção não carrega marca.** Sugerir categoria de ação ("vale conversar com o
veterinário sobre dieta renal") é serviço; sugerir marca é publicidade. No dia em
que uma percepção for paga por um fabricante, ela deixa de ser percepção, e o ativo
do Petfy — confiança — começa a ser gasto. Monetização, se existir, mora em outro
lugar do produto.

**Falta no backend:** tudo. É o único conceito da seção 3 sem nenhum ponto de apoio
no que existe hoje.

## 5. As promessas

Aprovada em 2026-08-06. Uma promessa só vale se tiver **preço** — por isso cada uma
traz o que o Petfy deixa de poder fazer por causa dela, e o que a torna verificável.
Promessa que ninguém consegue conferir é slogan.

### 5.1 O registro é do animal, e não se perde

**Dizemos:** a história do animal atravessa mudança de tutor, adoção, abrigo e troca
de clínica. Ninguém a leva embora e ninguém a apaga.

**Custa:** não existe "começar do zero". Apagar a conta não apaga o animal, e sair
do produto exige dar destino a ele (3.4).

**Verifica-se:** o adotante recebe a vida inteira, não uma ficha nova.

### 5.2 Nada se apaga; correção é sucessão

**Dizemos:** erro se corrige registrando a correção, nunca sobrescrevendo. O que
estava lá continua visível, com quem corrigiu e quando.

**Custa:** ninguém "limpa" o histórico — nem o tutor constrangido, nem o
profissional que errou.

**Verifica-se:** já é assim em `VaccineCorrection`, `HealthRecordCorrection` e na
condição de saúde, que encerra com `resolvedAt` em vez de `DELETE`.

### 5.3 Acesso é concedido, delimitado, temporário e auditável

**Dizemos:** ninguém alcança o registro por ser profissional, por ter o link ou por
ter estado lá antes. Alcança porque quem responde concedeu, no escopo que concedeu,
enquanto durar — e fica rastro de quem leu.

**Custa:** fricção real. O veterinário não vê nada antes de o tutor autorizar, e a
clínica perde acesso quando o animal muda de mão (4.3).

**Verifica-se:** `PetClinicAccess`, `SensitiveAccessLog` e o `PetAccessGuard` como
ponto único já existem; falta o escopo (decisão 10).

### 5.4 Dado de saúde tem regime próprio

**Dizemos:** ato clínico e observação são tratados como dado sensível — base legal
registrada, log de leitura e exportação completa. Recado e foto não são, e não
carregam esse peso.

**Custa:** o produto não pode simplificar juntando tudo num "histórico" só. O
critério de 3.11 vira classificação obrigatória em cada evento.

**Verifica-se:** consentimento versionado e log de acesso já no ar; a exportação
devolve o que existe.

### 5.5 O Petfy não interpreta clinicamente

**Dizemos:** nem a percepção do sistema nem o alerta da creche dizem o que o animal
tem. Descrevem o observado, mostram a evidência, e apontam para quem pode
diagnosticar.

**Custa:** caro, e de propósito. O produto **não responde** "o que meu cachorro
tem?", que é o que o usuário mais gostaria de perguntar. Parecer menos inteligente é
o preço de não ser irresponsável.

**Verifica-se:** toda percepção exibe a evidência que a gerou; nenhum texto do
produto contém hipótese diagnóstica.

### 5.6 O Petfy não vende a atenção do tutor

**Dizemos:** percepção não carrega marca (4.6), e acesso concedido a uma organização
nunca vira lista de marketing (2.3).

**Custa:** fecha a via de receita mais óbvia. Restringe o cardápio de monetização, e
a consequência é que ela terá de vir de quem recebe valor operacional —
organizações —, não de quem recebe cuidado.

**Verifica-se:** ausência de anunciante no produto. É a única promessa que se prova
por não existir.

### 5.7 Responsabilidade tem nome

**Dizemos:** todo ato clínico carrega quem o praticou, com que credencial e em nome
de qual organização — e isso nunca é editável.

**Custa:** o profissional não age anonimamente, e o registro dele o segue.

**Verifica-se:** autor e contexto imutáveis em cada evento (3.2, 4.1).

### 5.8 Portabilidade real

**Dizemos:** o que o Petfy guarda, o titular leva — inclusive os arquivos.

**Custa:** facilita a saída, o que é o ponto. Produto que retém por aprisionamento
não precisaria das outras sete promessas.

**Verifica-se:** a exportação existe; falta o ZIP com os arquivos dentro, já
registrado no roadmap.

### 5.9 A tensão com a LGPD, resolvida

Pendente de 2.1: se o tutor não altera nem contesta o registro clínico, como fica o
direito de correção que a lei dá ao titular?

**Primeira camada: o prontuário do animal não é dado pessoal do tutor.** Titular de
dado pessoal é pessoa natural, e o animal não é. Dado pessoal do tutor é o cadastro
dele — nome, e-mail, contato, e o vínculo com o animal —, e **isso ele corrige
livremente**. O que ele não edita é o registro clínico do animal, que não é sobre
ele.

**Segunda camada: mesmo o que o toca, o caminho existe e não é o botão de editar.**
O modelo é o do prontuário humano — o paciente não reescreve o que o profissional
registrou; obtém retificação junto de quem registrou, e ela entra como sucessão
visível. É o que o backend já faz.

| Direito | Como se exerce |
|---|---|
| Ver tudo | Acesso integral à linha do tempo do animal |
| Saber quem registrou | Autor, credencial e organização em cada evento (5.7) |
| Falar com quem registrou | Contato do profissional e da organização (2.1) |
| Ter o erro corrigido | Quem assinou corrige, e a correção fica visível (5.2) |
| Corrigir o que é dele | Cadastro, contato, preferências — direto, sem intermediário |
| Levar embora | Exportação completa (5.8) |
| Sair | Apagar a conta, dando destino ao animal (3.4) |

*Ressalva devida: isto é raciocínio de produto, não parecer jurídico. Antes de `prd`
com usuário real, vale validação de advogado — o custo de errar aqui não é técnico.*

### 5.10 O CRMV — decisão 4, resolvida

**Credencial verificável da pessoa, não chave de identidade.** A conta segue
identificada por e-mail; o CRMV é atributo da pessoa (3.7), com estado próprio:
*informado*, *verificado*, *suspenso*.

**Por que não chave de identidade:**

- Credencial muda de estado — pode ser suspensa ou cassada. Se fosse identidade, um
  profissional suspenso perderia a conta, e a linha do tempo perderia o autor de
  atos que continuam válidos.
- Uma pessoa pode ter mais de um (o registro é por estado) e pode não ter ainda
  (recém-formado, estudante).
- A veterinária que usa o Petfy só como tutora não deveria precisar de CRMV para
  existir.

**O que a credencial governa:** só pratica **ato clínico** quem tem CRMV verificado
(3.11). Observação e conteúdo seguem abertos a quem tem acesso. É a credencial
decidindo capacidade, não identidade.

**Enquanto não há integração com o conselho** — hoje não há —, o estado é *informado*
e o registro carrega essa informação. Ato clínico assinado por credencial não
verificada é honesto sobre isso, em vez de fingir garantia que o produto não tem.

## 6. O que o Petfy não é

Aprovada em 2026-08-06.

**O critério que decide, e vale para qualquer proposta futura:** se a funcionalidade
serve **o animal e o registro dele**, é do Petfy. Se serve a **operação de um
negócio**, não é — por mais que o negócio seja cliente nosso.

### 6.1 Nunca — viola uma promessa

**Não é publicidade nem marketplace.** A percepção é o ponto onde a monetização
seria mais eficaz e mais destrutiva; 5.6 fecha isso.

**Não é rede social de pets.** Perfil público, seguidores, curtida. O conteúdo
(3.13) é dirigido ao tutor, não ao público — a mesma foto que encanta numa timeline
privada vira exposição num feed aberto. A privacidade é o ativo.

**Não é telemedicina.** Atividade regulada, e o produto não interpreta clinicamente
(5.5). O Petfy leva informação ao veterinário; não o substitui.

**Não é seguro nem plano de saúde pet.** O adjacente de receita mais óbvia e
conflito mais grave: quem paga a conta do tratamento tem interesse financeiro no que
o registro diz. Com esse interesse, nenhuma das oito promessas se sustenta.

### 6.2 Não nesta rodada — a mecânica cabe, o foco não

**Não é sistema de gestão de clínica.** Faturamento, estoque, comissão, escala,
agenda de sala. É o que a clínica pediria primeiro, e é outro produto — centrado na
operação dela, não no animal.

**Não é sistema de gestão de ONG.** Doação, prestação de contas, campanha, escala de
voluntário. O Petfy cuida dos animais do abrigo, não da administração do abrigo.

**Não é mensageria.** Decidido em 4.5.

**Não abre hotel, banho e tosa, adestrador e pet sitter.** Todos cabem no vocabulário
da seção 3 sem entidade nova — o que é o melhor sinal possível sobre o modelo, e
exatamente por isso não há pressa.

### 6.3 Fora de alcance — não depende de nós

**Não é registro oficial do animal.** RGA, microchip e identificação legal são
emitidos por órgãos públicos e entidades de registro. O Petfy guarda e usa esses
números; não os emite nem os valida.

**Não verifica CRMV enquanto não houver integração.** O estado *informado* é dito em
voz alta no registro (5.10).

### 6.4 A tentação que merece nome próprio

Das oito acima, **a gestão de clínica é a única que vai voltar** — pela boca de um
cliente pagante dizendo "eu usaria o Petfy se ele também fizesse X". A resposta
pronta:

O Petfy não é o sistema da clínica; é **o registro do animal que a clínica atende**.
Se ela precisar dos dois, terá os dois. O que ela nunca terá é o registro do animal
preso ao software dela — e é justamente isso que faz o registro valer.

## 7. Horizonte

Aprovada em 2026-08-06.

### 7.1 O critério

O mesmo que o `ROADMAP.md` já usa: separar **o que muda a forma do dado que a tela
mostra** do **que é funcionalidade nova**. Aplicado às seis seções anteriores, ele
produz a regra de fatiamento:

> **Modelo inteiro, produto fatiado.** O que muda identidade, custódia, acesso ou a
> forma do evento nasce completo, antes de qualquer tela — mesmo sem interface que o
> exercite. O que é capacidade de um ator chega por fatia, quando aquele ator entrar.

É a decisão 3 virando plano.

### 7.2 Horizonte 1 — a fundação

Nada aqui tem tela; tudo aqui é pré-requisito de qualquer tela.

| O que | Substitui | Peso |
|---|---|---|
| **Pessoa única, sem tipo** (3.1) + contexto (3.2) | `Owner` e `Vet` separados, papel derivado da tabela | Grande |
| **Animal sem exigência de dono** (3.3) | `Pet` com FK obrigatória para `owners` | Médio |
| **Custódia** separada de **acesso** (3.4, 3.5) | `PetTutor` com papel fazendo as duas coisas | Grande |
| **Organização com capacidades** + **membro** (3.6, 3.7) | `Clinic` específica, `Vet.clinic` singular | Grande |
| **Evento com núcleo comum** + **linha do tempo** (3.9, 3.10) | Seis leituras que não conversam | Grande |
| **Escopo de acesso** (decisão 10) | Acesso binário | Médio |
| **Classificação de dado de saúde** (5.4) | Implícita hoje | Pequeno |
| **Óbito e animal perdido** como estados (4.1) | Não existem | Pequeno |

**Isto é refatoração do núcleo, não incremento.** Toda checagem de propriedade que
existe hoje passa por identidade e por `PetTutor`, e os dois mudam. Não vai parecer
progresso enquanto acontece.

**A razão de fazer assim mesmo — a janela.** Ela está aberta agora e fecha quando o
frontend nascer. O precedente está registrado no roadmap: a paginação mudou `List`
para `Page` no dia seguinte ao contrato ser publicado e *"como não existe cliente
ainda, a mudança saiu de graça"*. Aqui é o mesmo, multiplicado — com cliente
existindo, cada linha da tabela acima deixa de ser refatoração e vira migração com
dois lados para coordenar.

### 7.3 Horizonte 2 — v1 utilizável: tutor, veterinário, clínica

Critério de pronto, o mesmo do roadmap: **alguém que nunca viu `curl` cadastra um
animal e vê a próxima dose.**

| Capacidade | Estado hoje |
|---|---|
| Cadastro, login, consentimento, recuperação | Existe |
| Animal, carteira, agenda de vencimento | Existe |
| Prontuário, alergia, condição, peso, anexo | Existe |
| Multi-tutor, convite, transferência | Existe — migra para custódia |
| Concessão de acesso à clínica, log de leitura | Existe — ganha escopo |
| Compartilhamento por link | Existe |
| **Linha do tempo do animal** | Nasce no H1 |
| **Orientação com confirmação** (3.12) | Nova — pequena, e é o que traz o tutor de volta todo dia |
| **Pendência unificada** (3.14) | Generaliza a agenda que já existe |
| **Push como canal** | Novo — e-mail não sustenta lembrete no Brasil |
| **Acesso de emergência** (decisão 13) | Novo |

Mais o que já está no roadmap e é pré-requisito de gente real: domínio próprio
(decisão 9), `stg` e `prd`.

### 7.4 Horizonte 3 — os ecossistemas

Cada um é fatia independente, e **nenhum exige mudança no modelo** — é o teste da
seção 3 se pagando.

| Fatia | O que entra | Por que nesta ordem |
|---|---|---|
| **Creche** | Vínculo, turma, lotação, check-in, avaliação diária, conteúdo, tema de casa | Primeira: **traz tutor** (matrícula exige comprovação) e produz o dado que alimenta a percepção |
| **Abrigo** | Custódia institucional, animal sem tutor, adoção com cadastro do sucessor | Segunda: traz tutor já com animal dentro, mas depende de custódia madura |
| **Lar transitório** | Disponibilidade, custódia temporária, devolução | Terceira: é a menor, e depende do abrigo para ter volume |

### 7.5 Horizonte 4 — a percepção

**A promessa central é a última a ser construída, e isso é da natureza dela.**
Percepção precisa de história acumulada: com três eventos não há recorrência,
tendência nem sazonalidade.

O que se pode fazer desde já, e é barato: garantir que o H1 registre o que ela vai
precisar — instante do fato separado do instante do registro, autoria, contexto e
classificação. **Percepção não se constrói cedo; ela se torna possível cedo.**

### 7.6 Declarado, sem data

Espécie além de cão e gato; raça como dado; ZIP no export; leitura de anexo pelo
veterinário; consulta agendada; verificação de CRMV por integração. Todos já estão
no `ROADMAP.md`, e nenhum bloqueia os quatro horizontes.

## 8. Decisões em aberto

Consolidada em 2026-08-06. Das quatorze levantadas ao longo do documento, **doze
estão resolvidas**; as duas restantes são operacionais e se decidem na execução.

**Em aberto:**

| # | Decisão | Origem | Consequência |
|---|---|---|---|
| 8 | **Token longo por link, ou OTP de 6 dígitos?** | Roadmap, decisão pré-existente | Com app no horizonte, OTP entra em jogo — exige limite de tentativas. Decide-se ao construir o fluxo |
| 9 | **Domínio próprio.** | Roadmap, decisão pré-existente | Bloqueio absoluto para usuário real: o Resend só entrega para o e-mail dono da conta |

**Dependem de terceiro:**

- **Validação jurídica de 5.9** antes de `prd` com usuário real.
- **Integração com o conselho** para o CRMV sair de *informado* para *verificado*
  (5.10).

**Marcado para revisão antes de virar implementação:**

- **Transferência em conflito** (4.3): separação, venda mal resolvida, denúncia de
  maus-tratos. A regra "o tutor anterior mantém leitura do período dele" não foi
  pensada para esses casos.

**Resolvidas:**

| # | Decisão | Resolução | Onde |
|---|---|---|---|
| 1 | Identidade única, ou contas segregadas por tipo? | **Pessoa única, sem tipo.** O que ela pode decorre dos vínculos, não do cadastro | 3.1 |
| 3 | Ecossistema desenhado agora, ou v2 declarado? | **Conceito agora, implementação depois.** O vocabulário comporta as três frentes novas sem entidade nova; quando cada uma é construída é a seção 7 | Seção 3 inteira |
| 4 | CRMV: chave de identidade ou atributo verificado? | **Credencial verificável da pessoa.** A conta segue por e-mail; o CRMV tem estado (informado, verificado, suspenso) e governa quem pratica ato clínico | 5.10 |
| 5 | Veterinário autônomo entra como o quê? | **Pessoa com credencial, sem organização.** Organização passa a ser opcional para atuar | 3.7 |
| 6 | Onde mora o alerta da creche? | **Observação com sinalização de urgência.** Entra na linha do tempo, chega a quem tem acesso clínico, e pode ser referenciado por um ato clínico posterior | 3.11 |
| 7 | O abrigo mantém leitura depois da adoção? | **Mantém acesso de leitura ao que ele mesmo produziu**, se o novo tutor não revogar. Perde a custódia, não some do registro | 3.5 |
| 2 | Feed de ações ou carteira do animal como centro? | **Feed de pendências.** É o único formato onde percepção, orientação, recado e vencimento cabem juntos; cada coisa futura é um card, não uma aba | 3.14, 4.2 |
| 10 | Acesso com escopo, ou tudo-ou-nada? | **Com escopo.** Quem concede escolhe o quanto: a creche recebe alergia, vacinação, medicação e contato; a clínica recebe o prontuário | 4.4, 5.3 |
| 11 | Comunicação bidirecional entra? | **Não nesta rodada.** Mão única mais contato direto; conversa abre moderação, não lida e responsabilidade sobre o dito | 4.5, 6.2 |
| 12 | Percepção pode carregar marca? | **Nunca.** Categoria de ação sim, marca não | 4.6, 5.6 |
| 13 | Existe acesso de emergência (quebra-vidro)? | **Não existe.** Ninguém alcança sem concessão, sem exceção — ver a consequência registrada abaixo | 5.3 |
| 14 | Como atacar o H1? | **Fundação inteira antes de qualquer tela**, pelo argumento da janela | 7.2 |

### A consequência da decisão 13, registrada

Sem quebra-vidro, a promessa 5.3 fica absoluta — **ninguém alcança o registro sem
concessão de quem tem custódia, em nenhuma circunstância.** É mais simples de
defender, mais fácil de auditar e não abre porta que precise ser vigiada.

O preço é o caso da madrugada: animal inconsciente, tutor sem atender, clínico sem
saber de alergia ou medicação em curso.

**A compensação não é uma porta dos fundos — é preparo do tutor**, e três coisas que
o produto já tem ou terá cobrem a maior parte do caso:

- **Cartão de emergência preparado antes.** O tutor gera de véspera um
  compartilhamento com escopo mínimo (alergia, medicação em curso, condições, contato
  do veterinário de confiança). O link público já existe; o escopo chega na decisão
  10.
- **QR na coleira ou na plaquinha**, apontando para esse cartão. Quem socorre lê sem
  precisar do tutor — porque o tutor já autorizou, antes, para essa finalidade.
- **Concessão em dois toques**, para quando o tutor está acordado e do outro lado da
  linha.

A diferença é de princípio e não de resultado: **quem autoriza continua sendo o
tutor**, antecipadamente, em vez de o sistema decidir por ele no susto.

*A validar no levantamento: o cartão de emergência com escopo é item de v1, e não
uma consequência que descobrimos depois.*

## 9. O lado do cliente

> Aberta em 2026-08-06, depois de o documento ter sido fechado com oito seções. O
> fechamento continua verdadeiro sobre o que ele dizia: as oito seções estão
> aprovadas e nenhuma mudou. Esta é seção nova, e o motivo dela está na 9.1.

### 9.1 O critério

**Esta seção existe apesar da regra do documento, e não contra ela.** O cabeçalho diz
que tela e stack não entram aqui, e nenhuma das duas entra. O que se decide na seção 9
é **quantas superfícies o produto tem, quem usa cada uma, e a que relação do modelo
cada uma se ancora** — que é a mesma natureza de pergunta da seção 2, feita do outro
lado. Tela é o que se desenha depois. Stack é o `ROADMAP.md`.

**Por que ela vem antes do contrato.** A ordem intuitiva seria a inversa: o modelo está
de pé, então especifique a API e desenhe a tela contra ela. Está errada, porque **a
forma do cliente muda o que a API precisa responder**. Três perguntas que só a
estrutura do cliente responde, e que o contrato não tem como responder sozinho:

- **Quantas coisas uma tela pede de uma vez.** Um animal em profundidade e uma lista
  longa de animais não têm a mesma resposta certa.
- **Se agir em nome de uma organização é escolha por operação, ou estado de quem
  entrou.** Uma pessoa com três vínculos (3.2) decide o contexto a cada ação, ou decide
  uma vez ao entrar? São dois produtos diferentes sobre o mesmo modelo.
- **Se uma lista é de três ou de duzentos.** O tutor responde por um a três animais; o
  abrigo, por centenas. A mesma leitura serve para um e é inviável para o outro.

Especificar o contrato antes é responder às três por adivinhação, e depois brigar com a
própria resposta.

**Seis atores, e não seis superfícies.** A seção 2 listou seis; esta seção chega em
três. A redução não é economia — é o que o modelo já afirmava: clínica, creche, abrigo
e rede de lares diferem por capacidade (3.6), não por natureza. Uma superfície por ator
seria a mesma coisa que uma tabela por tipo de pessoa, que é o erro que a 3.1 desfez.

**O que esta seção não decide:** tela, navegação, nome de rota, tecnologia, prazo. Onde
o argumento precisou de um exemplo com nome de rota, ele está no `ROADMAP.md`, que é
onde esse tipo de frase pode existir.

### 9.2 As três superfícies

**Três, e cada uma se ancora numa relação diferente do modelo.** Não é arrumação de
produto: são as três formas que existem de alguém alcançar o registro de um animal, e a
seção 3 já as havia separado antes de existir cliente.

| Superfície | Ancorada em | Quem é | Volume | Granularidade |
|---|---|---|---|---|
| **Área do tutor** | Custódia (3.4), mais os acessos recebidos | Quem responde pelo animal | Um a três animais | Um animal em profundidade |
| **Área de organização** | Membro (3.7), mais os acessos concedidos (3.5) | Quem atua por si ou em nome de uma organização | Dezenas a centenas | Lista, filtro, busca, lote |
| **Cartão** | Um acesso com escopo (3.5), sem conta | Quem recebeu o link | Um animal | Leitura, escopo mínimo |

**O teste que impede uma quarta aparecer:** superfície nova só existe se houver
**relação nova no modelo**. Se alguém propuser uma, a pergunta é "ancorada em quê" — e
se a resposta for "num tipo de usuário", é a 3.1 sendo desfeita pela porta dos fundos.

**O cartão já existe, e não é hipótese.** É o compartilhamento por link sem conta, e é
ele que a decisão 13 nomeou como compensação por não haver quebra-vidro: o tutor
prepara de véspera, com escopo mínimo, e quem socorre lê sem depender de alguém acordar
às 3h. É a única superfície sem conta — e por isso a única em que o escopo é a defesa
inteira, sem senha atrás dele.

**Regra dura:** o cartão nunca é indexável nem adivinhável. Registro de saúde atrás de
um link que um buscador encontra é registro público, e nenhuma das promessas da seção 5
sobrevive a isso.

**O que as três compartilham, e é o ponto:** o mesmo registro. Não existe "versão
simplificada" da linha do tempo para uma superfície — existe **escopo**. Entre elas muda
o quanto se alcança e como se navega; nunca muda o que é verdade sobre o animal.

### 9.3 Uma aplicação, duas áreas

**Mesma aplicação, mesmo cadastro, mesma entrada.** As duas áreas não são dois
produtos: são dois pontos de vista sobre o mesmo registro, e quem alterna entre elas é a
mesma pessoa. A 3.1 decidiu que não existe conta de tutor nem conta de veterinário —
dois logins desfariam isso logo na porta.

**Regra dura: a área não vem de um campo de papel.** A pessoa chega numa área por causa
do que ela **tem** — custódia de um animal, ou vínculo com uma organização —, nunca por
causa de um tipo declarado no cadastro. O papel deixou de existir no modelo; se ele
voltar como condicional na tela, ele voltou inteiro, e a 3.1 vira decoração.

**A troca entre áreas é explícita e visível.** É o mesmo princípio do contexto (3.2): o
registro guarda em nome de quem foi feito, e quem age precisa saber em nome de quem está
agindo **antes** de agir. Área implícita é ato registrado no contexto errado — e contexto
não é editável depois (5.7).

**A área do tutor** é um animal em profundidade. O centro dela é o feed de pendências
(decisão 2), porque é o único formato em que vencimento, orientação, recado e percepção
cabem juntos sem virar quatro abas. É a área que existe todo dia.

**A área de organização** é muitos animais em largura: lista, filtro, busca e ação sobre
vários de uma vez. Clínica, creche, abrigo e rede de lares transitórios são **a mesma
área**, diferindo pelas capacidades da organização (3.6) — quem gere turma e vaga vê
turma; quem detém custódia vê os animais pelos quais responde; quem registra ato clínico
tem onde registrar. É a área do expediente.

**O profissional autônomo entra na área de organização sem organização nenhuma.** Ele é
pessoa com credencial e sem vínculo (3.7), e **nada nessa área pode exigir uma
organização para funcionar**. Quem o limita é a credencial dele, não a capacidade de um
coletivo — foi isso que a decisão 5 resolveu.

**Ter as duas é o caso comum, não o raro.** A veterinária que tem cachorro, o dono de
creche que é tutor, o voluntário de abrigo com dois gatos. Ela cadastra o próprio animal
numa área e atende na outra, e nenhuma das duas esconde a existência da outra.

### 9.4 Web primeiro, app depois

**Web primeiro, e o motivo é alcance:** é a única superfície que serve os seis atores da
seção 2 sem exigir nada de ninguém. Quem recebe um link entra — não instala, não
atualiza, não depende de loja aprovar.

**E os atores pesados são de teclado.** Clínica, creche e abrigo trabalham em balcão,
cadastram em série e olham lista. A área de organização não pede app; pede tela grande.

**O app vem depois de uma v1 web robusta, e não busca paridade.** Ele existe para as
duas coisas que o navegador faz mal, e as duas são do tutor:

- **avisar** — a pendência (3.14) que chega sem o usuário ir procurar;
- **confirmar num toque** — "dei o remédio", que é o cumprimento da orientação (3.12)
  virando evento.

É o loop diário do tutor, e é curto de propósito. Lançar a vacina de 2019 da carteirinha
de papel continua na web — ninguém faz isso no ônibus.

**Regra dura: o app não é o produto reduzido.** Ele é um recorte com propósito. App que
tenta ser a web inteira numa tela menor faz as duas coisas mal, e a primeira a quebrar é
justamente o loop diário, que é a razão de ele existir.

**O cartão é a superfície mais móvel das três — e é a que nunca pode exigir app.** Ele
nasce de um QR numa coleira, lido por quem socorre o animal, não tem conta, não tem o
app e não vai instalar nada no meio de uma emergência. Navegador, e só.

**Sobre o aviso, para não soar como adiamento:** notificação não espera o app — o
navegador já notifica, e a 4.2 registra o canal além do e-mail como falta desde antes
desta seção existir. O que o app muda é a **confiabilidade** disso. Enquanto ele não
existe, o aviso funciona; depois dele, o aviso deixa de ser frágil.

### 9.5 O que a estrutura cobra, e o que fica em aberto

Nada aqui é implementação — é o que as quatro seções anteriores tornam obrigatório. O
detalhamento, rota por rota, é do `ROADMAP.md`.

**Leitura em volume.** Tudo que existe hoje foi desenhado para quem responde por três
animais. A área de organização lê centenas, e precisa de recorte, busca e ordem — não
por conforto, mas porque sem isso ela é inutilizável para o ator que a justifica.

**Contexto ativo como estado, e não como escolha a cada ação.** A 9.3 decidiu que a
troca de área é explícita e visível. A consequência é que agir em nome de uma
organização passa a ser algo que a pessoa **escolheu antes** — a ambiguidade deixa de ser
erro devolvido no meio de uma operação e vira decisão tomada na entrada.

**Nada na área de organização pode exigir organização.** O autônomo (9.3) atravessa a
área inteira, e isso precisa ser afirmação verificada, não coincidência de quais
funcionalidades foram construídas primeiro.

**O cartão preparado, com escopo.** A decisão 13 prometeu-o como compensação por não
haver quebra-vidro, e a 9.2 o transformou em superfície. Ele deixa de ser consequência
descoberta depois e passa a ser item de v1, como a própria seção 8 já suspeitava.

**O feed de pendências como leitura, e não como soma feita na tela.** A 3.14 já dizia
que pendência é derivada; a 9.3 a coloca no centro da área do tutor. Se o cliente montar
esse feed juntando fontes por conta própria, cada cliente novo — o app, depois — remonta
a mesma regra e erra diferente.

**Três decisões em aberto, todas da área de organização:**

| # | Decisão | Por que não se decide aqui |
|---|---|---|
| 15 | **Quem cria uma organização, e como entra o primeiro membro?** | O convite de membro existe no modelo, mas o começo da cadeia nunca foi exercitado. É a primeira coisa que a área de organização encontra, e não tem resposta ainda |
| 16 | **Como o adotante sem e-mail à mão recebe o convite de custódia?** | A adoção acontece no balcão. Decidiu-se que a organização convida em vez de criar conta pela pessoa (2.6, 4.3), e a fricção disso é real — a resposta é de fluxo, e o fluxo ainda não foi desenhado |
| 17 | **O que compõe o escopo mínimo do cartão?** | Alergia, medicação em curso, condições e contato são o candidato óbvio. Fechar a lista é decisão clínica, e merece ser tomada com um veterinário na frente |

---

## Anexo — insumos já levantados para o delta de backend

Nada aqui é decisão de produto. É o que a leitura do código em 2026-08-05
apontou como restrição ou lacuna, guardado para o levantamento que vem depois da
seção 8.

> ⚠️ **Este anexo está vencido, e planejar por ele é planejar para trás.** Ele é de
> 2026-08-05, **anterior à Fase 6**, e cinco das oito lacunas que ele lista já estão
> fechadas — entre elas *"pet exige dono, não há animal sem tutor humano"*, que a
> `Custody` resolveu, e *"não há linha do tempo do animal"*, que a view `animal_timeline`
> resolveu. A conferência item por item, feita contra o contrato publicado em 2026-08-07,
> está no `ROADMAP.md`, na seção do passo 4. **Leia lá antes de usar qualquer coisa
> daqui.**

**Restrições que o backend já impõe a qualquer cliente:**

- **Papel manda no que aparece.** `HOLDER`/`EDITOR`/`VIEWER` no `PetTutor`, com o
  `PetAccessGuard` como ponto único. O cliente precisa receber o próprio papel
  junto do recurso, ou reimplementa autorização por conta própria.
- **Pet fora do alcance responde `404`, não `403`.** Muda mensagem de erro e
  comportamento de cache no cliente.
- **Download de anexo passa pela API autenticada**, não por URL assinada.
- **Consentimento pendente é estado, não erro.** Mudança de versão da política
  exige aceite antes de seguir.
- **Troca de senha invalida token por `iat`.** O cliente recebe `401` em token não
  expirado, e precisa distinguir isso de expiração.
- **Contrato misto nas listagens.** `Page` em `/pets`, `/vaccines`,
  `/health-records` e `/clinics`; array nas listagens escopadas por pet e na agenda.

**Lacunas estruturais que o produto descrito aqui vai cobrar:**

- `Clinic` é específica demais para comportar creche e abrigo.
- `Vet.clinic` é `@ManyToOne` singular — vet em duas clínicas precisa de duas
  contas, e o namespace único de e-mail impede.
- Pet exige dono; não há estado de *animal sem tutor humano*.
- Não existe conceito de conteúdo não clínico dirigido ao tutor.
- Não existe orientação atribuída com prazo e confirmação.
- Vacina, antiparasitário, peso, atendimento, condição e anexo são seis leituras
  separadas — não há linha do tempo do animal.
- Espécie tem dois valores (`Species`), e o catálogo depende dela.
- Raça é texto livre.
