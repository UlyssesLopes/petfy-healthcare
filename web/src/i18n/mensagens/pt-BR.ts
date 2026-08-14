/**
 * As mensagens em pt-BR.
 *
 * As de erro sao escritas CONTRA a secao 2 do DESIGN.md, nao traduzidas do ingles do
 * `ErrorMessageEnum`: o fato primeiro, sem culpar quem errou, e sem revelar o que a
 * pessoa nao pode ver. A `mensagemDaApi` do `contract/error-codes.json` serve so de
 * referencia para saber qual falha e cada codigo.
 *
 * A chave e `erro.<codigo>`, e o codigo e o do enum. Nunca o status HTTP: 404 e 409
 * tem varios codigos cada um.
 */
export const mensagens = {
  // --------------------------------------------------------------- o que nao achamos
  //
  // "Nao encontramos" e proposital em todos. O backend responde 404 para animal fora
  // do alcance justamente para NAO confirmar que ele existe, e a secao 2 e explicita:
  // a tela nao pode desfazer isso dizendo "esse animal e de outro tutor".
  "erro.101": "Não encontramos esta pessoa.",
  "erro.102": "Não encontramos este animal.",
  "erro.103": "Não encontramos esta organização.",
  "erro.104": "Não encontramos este registro de vacina.",
  "erro.105": "Não encontramos este atendimento.",
  "erro.106": "Não encontramos esta vacina no catálogo.",
  "erro.110": "Não encontramos este acesso.",
  "erro.118": "Não encontramos este antiparasitário no catálogo.",
  "erro.125": "Não encontramos este anexo.",
  "erro.130": "Não encontramos esta condição.",
  "erro.139": "Não encontramos esta orientação.",

  // ------------------------------------------------------------ links e convites
  //
  // O enum e vago de proposito em 115 e 116 — nao distingue inexistente, expirado e ja
  // usado —, e a traducao preserva a vagueza em vez de "corrigi-la": dizer "expirado"
  // contaria a quem tem o link que ele um dia valeu.
  "erro.107": "Este link não está mais valendo.",
  "erro.111": "Este convite não está mais valendo.",
  "erro.115": "Este link de recuperação não está mais valendo. Peça um novo.",
  "erro.116": "Este link de confirmação não está mais valendo. Peça um novo.",
  "erro.120": "Este convite não está mais valendo.",

  // ------------------------------------------------------------------- conta e senha
  "erro.108": "Esse e-mail já tem conta. Você pode entrar ou recuperar a senha.",
  "erro.113": "A senha atual não confere.",
  "erro.114": "A nova senha precisa ser diferente da atual.",

  // -------------------------------------------------- quem alcanca, e com que alcance
  "erro.109": "Só quem é membro desta organização pode fazer isso.",
  "erro.119": "Seu acesso a este animal não permite essa ação.",
  "erro.133": "Esta ação precisa de uma credencial profissional ativa.",
  "erro.134": "Esta credencial já está registrada.",
  "erro.135": "Você não é membro ativo desta organização.",
  "erro.137": "Esta organização não faz esse tipo de registro.",

  // Os dois de contexto sao falta de escolha, e nao falta de permissao — e a tela
  // precisa oferecer a escolha em vez de parecer uma porta fechada (PRODUTO.md 9.5).
  "erro.136": "Você atua por mais de uma organização. Escolha em nome de qual está agindo.",
  "erro.138": "Esta ação é da organização. Escolha em nome de qual está agindo.",

  // --------------------------------------------------------- custodia e quem responde
  "erro.121": "Esta pessoa já é tutora deste animal.",
  "erro.122": "Quem responde pelo animal não pode ser removido. Transfira antes.",
  "erro.123": "Para trocar quem responde pelo animal, use a transferência.",
  "erro.124": "Esta pessoa não é tutora deste animal.",

  // ------------------------------------------------------- o registro, e sua sucessao
  //
  // 112 e 132 dizem a mesma coisa por baixo: registro nao se reescreve, se sucede
  // (DESIGN.md 5.2). O texto oferece o caminho em vez de so recusar.
  "erro.112": "O prazo para corrigir este registro passou.",
  "erro.131": "A gravidade vale só para alergia.",
  "erro.132": "O tipo não muda depois de registrado. Registre uma nova condição.",
  "erro.117": "Este item do catálogo é de outra espécie.",

  // ------------------------------------------------------------------------- anexos
  "erro.126": "O anexo precisa ser JPEG, PNG, WEBP ou PDF.",
  "erro.127": "Este arquivo passa do tamanho máximo.",
  "erro.128": "Não conseguimos guardar o anexo agora. Tente de novo em instantes.",
  "erro.129": "Este arquivo está vazio.",
  // O 148 é vizinho do 129 e diz outra coisa: lá havia um arquivo, e ele estava vazio; aqui não
  // veio arquivo nenhum. Por isso a frase é um pedido, e não um diagnóstico — o que resolve é
  // escolher o arquivo.
  "erro.148": "Escolha um arquivo para enviar.",

  // ---------------------------------------------------------- pendencia e orientacao
  "erro.140": "Esta orientação não estava valendo na data informada.",
  "erro.141": "Esta pendência não está mais na sua lista.",
  "erro.142": "O aceite dos termos não pode ser silenciado.",

  // ------------------------------------------------------------------ corpo nao lido
  //
  // Nao ha campo para marcar, porque o servidor nao chegou a montar o objeto — entao a
  // frase do 400 mentiria aqui. E o unico erro desta tabela que o usuario nao causou e
  // nao conserta: se aparecer, o defeito e nosso, e insistir daria no mesmo. Por isso
  // manda recarregar, que e a unica saida que as vezes funciona (versao velha da tela).
  "erro.143":
    "Algo saiu errado no envio, e não foi você. Recarregue a página e tente de novo.",

  // ------------------------------------------------------------ a operação da creche
  //
  // O 146 é o mais importante da tabela inteira do ponto de vista de quem lê: ele aparece na
  // porta da creche, às 7h30, para uma monitora com quinze cachorros esperando. Diz o fato e
  // a saída, sem culpar o tutor e sem pedir para "tentar de novo" — tentar de novo não
  // resolve; registrar a dose resolve.
  "erro.144": "Não encontramos esta turma.",
  "erro.145": "Esta turma não tem vaga livre. Matrículas guardadas também ocupam vaga.",
  "erro.146": "A comprovação de saúde deste animal está aberta — ele não pode entrar hoje. A matrícula se completa sozinha quando a dose for registrada.",
  "erro.147": "Este animal não tem entrada marcada hoje.",

  // ------------------------------------------------------------------ a dose duplicada
  //
  // O 149 chega para quem acabou de fazer a coisa certa: a clínica registrou a dose, e o tutor
  // está registrando a mesma minutos depois. A frase não pode soar como erro dele — o que ela
  // precisa dizer é que o registro JÁ EXISTE, e que por isso não há nada a fazer.
  "erro.149": "Esta dose já está registrada nesta data. Ela aparece no histórico do animal.",

  // ---------------------------------------------------------------------------- a equipe
  //
  // O 152 é o único dos três que precisa dizer a SAÍDA, e não só o impedimento: quem tenta
  // rebaixar ou desligar o último administrador quase sempre está se organizando, e não errando.
  "erro.150": "Não encontramos esta pessoa na equipe.",
  "erro.151": "Só quem administra a organização pode ajustar funções e desligar.",
  "erro.152": "A organização ficaria sem ninguém para administrá-la. Promova outra pessoa a administradora antes.",
  // O 153 não acusa ninguém: quem chega nele não errou, já está onde queria estar. E ele diz
  // que o convite continua de pé, porque a pergunta seguinte de quem lê é se acabou de queimá-lo.
  "erro.153": "Você já é membro desta organização. O convite não foi usado e continua valendo para quem for entrar.",

  // ------------------------------------------------------- a união de cadastros (Tela 32)
  //
  // O 154 fala de um cadastro que virou apontador. Ele diz onde a vida do animal continua, e não
  // "não encontrado": some com o cadastro seria dizer que o animal nunca existiu.
  "erro.154":
    "Este cadastro foi unido a outro, e é lá que a vida do animal continua. Ele não recebe registro novo.",
  "erro.155": "Não encontramos este pedido de união.",
  // Decidir duas vezes não é falta de idempotência: a segunda decisão viria de quem leu a
  // comparação antes da primeira união, sobre um estado que já não existe.
  "erro.156":
    "Este pedido já foi decidido. Recarregue para ver como os dois cadastros ficaram — o que você está vendo é de antes.",
  "erro.157":
    "Já existe um pedido para unir estes dois cadastros, esperando decisão de quem responde pelo animal.",
  // O dia da semana chegou num formato que o servidor nao entende. Nao e a creche que errou
  // de digitacao — os dias sao botoes —, entao a frase nao a manda "conferir os campos": ela
  // diz que o combinado nao foi gravado, que e a consequencia que importa para quem esta ali.
  "erro.158": "Não conseguimos gravar os dias combinados. O resto do combinado também não foi salvo — tente de novo.",

  // -------------------------------------------------- o fim da linha do tempo (Tela 33)
  //
  // As duas frases mais difíceis de escrever do arquivo, e a regra que valeu é a mesma da
  // seção 2: o fato primeiro, sem culpar quem preencheu. Quem está nesta tela acabou de perder
  // o animal, e um "campo inválido" aqui seria uma grosseria.
  "erro.159": "Confira a data: ela não pode ser no futuro nem antes do nascimento dele.",
  // Encerrar duas vezes quase sempre é a mesma pessoa em duas abas, ou um toque repetido. A
  // frase não trata como erro, e diz onde o registro está.
  "erro.160": "A linha do tempo dele já foi encerrada. O que você preencheu antes está guardado.",

  // --------------------------------------------- a busca de animal encontrado (Tela 34)
  //
  // NÃO É UM ERRO, e a frase não pode soar como um. O número pode estar em outro sistema, ou o
  // chip pode ter sido aplicado e nunca registrado — e quem está lendo isso tem um animal na
  // frente e precisa saber o que fazer em seguida, não que a busca "falhou".
  "erro.161":
    "Este número não está no Petfy. Confira os 15 dígitos e procure uma clínica ou a prefeitura — elas consultam bases que o Petfy não alcança.",

  // ------------------------------------------------ o animal comunitário (Telas 43 e 44)
  "erro.162": "A data não pode ser no futuro — ninguém vê um animal amanhã.",
  "erro.163": "Não encontramos este pedido.",
  "erro.164":
    "Este pedido já foi decidido. Recarregue para ver como ficou — o que você está vendo é de antes.",
  // A ÚNICA RECUSA DESTE BLOCO QUE EXISTE PARA PROTEGER O ANIMAL DE QUEM CUIDA DELE. A frase diz
  // a regra inteira, e não "ação não permitida": quem lê precisa entender que não é um defeito.
  "erro.165":
    "Quem pede não concorda consigo. Num grupo sem dono, é o acordo de duas pessoas que protege o que não se desfaz — peça a alguém do grupo para olhar.",
  "erro.166": "Já existe um pedido igual esperando decisão.",
  "erro.167":
    "Só quem é do grupo pode fazer isso. Não é sobre função — é sobre cuidar destes animais.",

  // ------------------------------------------------------ o encaminhamento (Tela 45)
  "erro.168": "Não encontramos este encaminhamento.",
  "erro.169":
    "Este encaminhamento já foi decidido. Recarregue para ver como ficou — o que você está vendo é de antes.",
  "erro.170":
    "Este animal já foi encaminhado para este profissional, e o pedido espera decisão de quem responde por ele.",
  "erro.171": "Encaminhar é indicar outro profissional. Escolha alguém diferente de você.",
  // A recusa que existe para proteger o tutor, e a frase diz por quê: sem ela, encaminhar seria o
  // caminho mais curto para dar acesso de escrita a qualquer pessoa, com o tutor autorizando na
  // crença de que era um especialista.
  "erro.172":
    "Só quem tem registro profissional ativo pode receber um encaminhamento. Quem responde pelo animal autorizaria acesso ao prontuário achando que é um especialista.",
  "erro.173":
    "Esta pessoa já responde por este animal, e alcança tudo sem precisar de encaminhamento nenhum.",
  "erro.174":
    "Ninguém responde por este animal agora, então não há quem autorize o encaminhamento.",

  // ------------------------------------------------------ o apadrinhamento (Tela 46)
  // Cobre os dois casos, e a frase diz os dois: o abrigo não abriu este animal, ou quem responde
  // por ele é uma pessoa. Apadrinhar o cachorro de alguém seria pagar a conta dessa pessoa.
  "erro.175":
    "Este animal não está aberto a padrinhos. Ou o abrigo não o ofereceu, ou quem responde por ele é uma pessoa — e aí não é apadrinhar, é pagar a conta de alguém.",
  "erro.176": "Não encontramos este apadrinhamento.",
  "erro.177":
    "Você já banca isto para este animal. Para dar mais, use “Outro” e escreva o que você quer bancar.",
  "erro.178": "Este apadrinhamento já está terminando. A data combinada continua valendo.",
  "erro.179": "Este gasto não é deste animal.",
  "erro.180":
    "Sua organização responde por este animal, então ela não pode apadrinhá-lo. Se quiser bancar do seu bolso, saia do contexto da organização primeiro.",

  // ---------------------------------------------------------- a hospedagem (Tela 47)
  "erro.181": "Este animal não está hospedado agora.",
  "erro.182": "Este animal já está hospedado. Registre a volta antes de entregá-lo de novo.",
  "erro.183": "A volta prevista precisa ser depois de hoje.",
  "erro.184":
    "Só quem está com o animal ou quem o entregou pode registrar a volta.",
  "erro.185":
    "Esta organização não pode responder por um animal. Hospedagem passa a responsabilidade, e não só o acesso.",

  // -------------------------------------------------------- a agenda do petshop (Tela 18)
  "erro.186": "Não encontramos este agendamento.",
  "erro.187": "Este animal já tem um horário marcado nesse momento.",
  "erro.188":
    "Este agendamento não está no estado que permite isso. Recarregue para ver como ele está agora.",
  "erro.189":
    "Sua organização não tem acesso a este animal. Peça acesso a quem cuida dele antes de agendar.",

  // ------------------------------------------------------- a busca e a conta (Telas 35 e 36)
  // A recusa que substituiu um desfecho silencioso: antes, o animal sem outro tutor morria com a
  // conta e o que tinha co-tutor passava para o mais antigo — sem ninguém escolher.
  "erro.190":
    "Dê um destino a cada animal antes de encerrar a conta. Passe cada um para outra pessoa — a vida registrada deles vai junto e não se apaga com a sua conta.",
  "erro.191": "Escreva ao menos três letras para buscar.",
  // Os dois dizem "não encontramos", e não "não é seu": o servidor responde 404 para aviso e
  // sessão de outra pessoa justamente para não confirmar que existem. A frase da tela não pode
  // desfazer isso.
  "erro.192": "Não encontramos este aviso.",
  "erro.193": "Não encontramos esta sessão.",

  // -------------------------------------------------------------------- os tres gerais
  //
  // O 400 e o caso que nenhum codigo resolve: o servidor devolve `campo: motivo` com
  // nome de campo em ingles e sem acento, e nada disso e exibivel. Entao a validacao de
  // campo e do front, ANTES de enviar, e o 400 que voltar e a rede de seguranca.
  "erro.400": "Confira os campos marcados — algo ali não está completo.",
  "erro.401": "E-mail ou senha não conferem.",
  "erro.500":
    "Não conseguimos salvar agora. O que você escreveu não se perdeu — tente de novo em instantes.",

  /**
   * Codigo que a tabela nao conhece, e resposta que nem forma de erro tem — queda de
   * rede, HTML de proxy, resposta cortada. O teste ao lado do `erroDaApi.ts` impede o
   * primeiro caso de acontecer por esquecimento; este texto cobre o resto.
   */
  "erro.desconhecido": "Não conseguimos completar agora. Tente de novo em instantes.",

  // ---------------------------------------------------------------------------- entrar
  //
  // A validacao de campo e NOSSA e acontece antes de enviar - o servidor devolve
  // `campo: motivo` com o nome do campo em ingles, e isso nao e exibivel (DESIGN.md 6,
  // emenda de 2026-08-07). As frases seguem a secao 2: dizem o que falta, sem culpar.
  "entrar.titulo": "Que bom te ver",
  "entrar.subtitulo": "Entre para continuar de onde parou.",
  "entrar.email": "E-mail",
  "entrar.senha": "Senha",
  "entrar.acao": "Entrar",
  "entrar.enviando": "Entrando…",
  "entrar.mostrar": "Mostrar",
  "entrar.ocultar": "Ocultar",
  "entrar.esqueci": "Esqueci a senha",
  "entrar.esqueci.enviando": "Enviando…",
  // O servidor responde igual para e-mail existente e inexistente, de propósito. A tela
  // repete a postura: confirma o envio sem afirmar que a conta existe.
  "entrar.esqueci.enviado": "Se {email} tiver conta aqui, o link para trocar a senha já saiu.",
  "entrar.esqueci.precisaEmail": "Escreva seu e-mail acima para receber o link.",

  // A frase da esquerda nao vende o produto — lembra por que a conta existe. E a mesma
  // tese do simbolo: o animal permanece, as pessoas passam.
  "entrar.tese.titulo": "A vida deles continua registrada, mesmo quando você não está olhando.",
  "entrar.tese.apoio": "Cada dose, cada consulta e cada dia de creche entrou aqui com o nome de quem fez. Nada some, nada é reescrito às escondidas.",

  "entrar.agora.rotulo": "Precisa de algo agora?",
  "entrar.agora.texto": "Se o animal está passando mal e você não lembra a senha, o cartão de emergência dele abre sem login — alergias, remédios em curso e quem chamar.",
  "entrar.cartao.semSenha": "Sem senha, para quando não dá tempo.",
  // O botão pede o código porque não há URL pública: o link que o tutor gera carrega um token,
  // e é ele que se cola aqui.
  "entrar.cartao.codigo": "Código do cartão",
  "entrar.cartao.abrir": "Abrir",
  "entrar.cartao.ajuda": "Cole aqui o código que quem responde pelo animal enviou.",

  "entrar.continuarConectado": "Continuar conectado neste aparelho",
  "entrar.continuarConectado.marcado": "Você fica conectado por 30 dias, mesmo fechando o navegador.",
  "entrar.continuarConectado.desmarcado": "Ao fechar o navegador, você sai. Use num computador emprestado.",
  "entrar.criarConta": "Ainda não tem conta? {acao}",
  "entrar.criarConta.acao": "Criar uma agora",
  // O caminho de quem não vem entrar (Tela 34). A frase é a pergunta que a pessoa tem na cabeça,
  // e não o nome da funcionalidade: ninguém procura por "consulta de microchip".
  "entrar.achouUmAnimal": "Achou um animal na rua? Procure pelo microchip, sem criar conta",

  "entrar.email.faltando": "Informe o seu e-mail.",
  "entrar.email.incompleto": "Esse e-mail não parece completo — falta o @",
  "entrar.senha.faltando": "Informe a sua senha.",

  // Os dois motivos de encerramento que a pessoa precisa distinguir. Nenhum e erro: o
  // segundo e a consequencia esperada de trocar a senha, e dizer isso evita que ela
  // pareca uma falha do produto.
  "entrar.sessaoExpirada": "Sua sessão expirou. Entre de novo para continuar.",
  "entrar.sessaoInvalidada": "Sua senha mudou, então a sessão anterior foi encerrada.",

  // ------------------------------------------------------------------- home do tutor
  "home.sair": "Sair",
  "home.agindoComo": "Agindo como",
  "home.contexto.pela": "· pela {org}",
  "home.nav.meusAnimais": "Meus animais",
  "home.nav.quemCuida": "Quem cuida",
  "home.custodia": "Sob sua custódia",
  "home.cadastrarAnimal": "Cadastrar animal",
  // Quem chega com um código na mão acabou de criar a conta para aceitar um convite, e não tem
  // animal nenhum — é para ele que esta linha existe.
  "home.tenhoUmConvite": "Tenho um código de convite",
  "home.hoje": "Hoje",
  // "Três coisas pedem você" — o desenho conta, e a contagem e o que da hierarquia:
  // sem numero, quinze pendencias e uma lista; com numero, e um dia.
  "home.quantas": "{total, plural, =0 {Nada pede você agora.} one {Uma coisa pede você.} other {# coisas pedem você.}} Silenciar não para o registro.",
  "home.pendencia.titulo": "{o_que} do {animal}",
  "home.silenciada.cumprido": "Cumprido",
  "home.quemAlcanca": "Quem alcança",
  "home.quemAlcanca.vazio": "Só você alcança este animal.",
  "home.alcance.custodia": "Custódia",
  "home.alcance.concessao": "Acesso concedido",
  "home.alcance.semPrazo": "sem prazo",
  "home.alcance.ate": "até {data}",

  // "Code, de relance": quatro leituras sobre dado que ja existe, nao campos novos.
  "relance.titulo": "{nome}, de relance",
  "relance.vacinacao.irregular": "Vacinação irregular",
  "relance.vacinacao.emDia": "Vacinação em dia",
  "relance.vacinacao.semRegistro": "Vacinação sem registro",
  "relance.antiparasitario.emDia": "Antiparasitário em dia",
  "relance.antiparasitario.vencido": "Antiparasitário vencido",
  "relance.antiparasitario.semRegistro": "Antiparasitário sem registro",
  // Nao pesar nao e irregularidade: por isso anel tracejado, e nao losango.
  "relance.peso.antigo": "Peso sem registro há {meses} meses",
  "relance.peso.recente": "Peso registrado recentemente",
  "relance.peso.semRegistro": "Peso sem registro",
  "relance.alergia": "{o_que} registrada{extras, plural, =0 {} other { · mais #}}",
  "relance.condicao": "{total, plural, one {# condição registrada} other {# condições registradas}}",
  "home.nav.inicio": "Início",
  "home.contexto.voce": "· você mesmo",

  // A idade em anos, pela regra do idioma. Fica ao lado da raça, separada por ponto
  // medio — sao itens de uma lista curta, e nao uma frase montada por concatenacao.
  "home.idade": "{anos, plural, =0 {menos de um ano} one {# ano} other {# anos}}",
  "home.especie.CANINA": "Cão",
  "home.especie.FELINA": "Gato",
  "home.pendencias.titulo": "Precisa da sua atenção",
  "home.pendencias.carregando": "Carregando o que precisa da sua atenção…",

  // O vazio nao e uma falha, e o texto nao pede desculpa por ele. Tambem nao comemora:
  // a secao 1 proibe comemorar ato de saude, e "tudo em dia!" e a versao disso.
  "home.pendencias.vazio": "Nada precisa da sua atenção agora.",
  "home.pendencias.vazio.apoio": "Quando uma dose ou uma orientação vencer, ela aparece aqui.",

  // Plural pela regra do idioma, e nao por "s" no fim (DESIGN.md 6).
  "home.estado.vencida": "{dias, plural, one {Vencida há # dia} other {Vencida há # dias}}",
  "home.estado.venceHoje": "Vence hoje",
  "home.estado.aVencer": "{dias, plural, one {Vence amanhã} other {Vence em # dias}}",

  // A regra 5.3: nunca cobrar duas pessoas sem dizer que a outra ja fez. Dose dupla e
  // dano, e nao incomodo.
  "home.jaFeito": "Já feito por {nome}, {quando}.",

  // O dia, e nao a hora: o `lastFulfilledAt` viaja como LocalDateTime, sem fuso nenhum.
  // Dizer "às 7h40" seria inventar uma precisao que o dado nao tem — e erraria em horas
  // se o servidor nao estiver no mesmo fuso de quem le.
  "home.quando.hoje": "hoje",
  "home.quando.ontem": "ontem",
  "home.quando.em": "em {data}",

  // O animal vai numa linha propria, e nao dentro do fato ("Antirrábica do Code"), porque
  // o DTO da pendencia nao traz o sexo do animal — e a secao 6 diz que onde o sexo nao
  // for conhecido a frase se reescreve para nao precisar dele, nunca se chuta.
  "home.animal": "{nome}",

  "home.convite.naoAceito": "{email} ainda não aceitou o convite.",
  "home.consentimento": "Você precisa aceitar a versão atual dos termos e da política de privacidade.",

  // ----------------------------------------------------------------- registrar a dose
  "dose.acao": "Registrar dose",
  "dose.titulo": "Registrar a dose de {vacina}",
  "dose.aplicadaEm": "Aplicada em",
  "dose.proximaDose": "Próxima dose",
  "dose.proximaDose.apoio": "Se você souber. Dá para preencher depois.",
  "dose.confirmar": "Registrar",
  "dose.confirmando": "Registrando…",
  "dose.cancelar": "Cancelar",
  "dose.carregando": "Buscando a dose anterior…",
  // A saída do conflito é fechar, e não tentar de novo: a dose já está lá.
  "dose.conflito.fechar": "Fechar",

  // A validacao e nossa e antes de enviar. As frases dizem o que falta, sem culpar.
  "dose.erro.semData": "Informe o dia em que a dose foi aplicada.",
  "dose.erro.futuro": "Essa data ainda não chegou — a dose foi aplicada quando?",
  "dose.erro.proximaAntes": "A próxima dose precisa vir depois da aplicação.",

  "home.acao.silenciar": "Silenciar",
  "home.acao.voltarACobrar": "Voltar a cobrar",
  "home.acao.cumprir": "Já dei",
  "home.acao.registrarDose": "Registrar dose",
  "home.acao.cumprindo": "Confirmando…",
  "home.silenciada": "Silenciada",
  "home.mostrarSilenciadas": "Mostrar as silenciadas",
  "home.ocultarSilenciadas": "Ocultar as silenciadas",

  // -------------------------------------------------------------- quem cuida (5.4)
  //
  // As pessoas em volta do animal vem ANTES dos dados: e a tese da secao 1 do PRODUTO.md
  // virando tela — o registro e o fio que liga quem cuida.
  "rede.titulo": "Quem cuida do {animal}",
  "rede.responde": "Responde pelo {animal}",
  "rede.respondeComVoce": "Responde pelo {animal} com você",
  "rede.acesso": "Tem acesso",
  "rede.conceder": "Conceder acesso",
  "rede.conceder.apoio": "Clínica, creche, alguém de confiança",
  "rede.contagem": "{quantas, plural, one {# pessoa cuida} other {# pessoas cuidam}}",

  // -------------------------------------------------------------- a vida do animal (5.1)
  "linha.titulo": "A vida do {animal}",
  "linha.carregando": "Carregando os registros…",
  "linha.vazia": "Ainda não há registros na vida do {animal}.",
  "linha.vazia.apoio": "Uma vacina, um peso, uma observação — tudo o que for registrado aparece aqui, em ordem.",
  "linha.resumo": "{registros, plural, one {# registro} other {# registros}}",

  // A credencial diz o que e. CRMV apenas informado aparece COMO INFORMADO, em tinta
  // secundaria — sem selo de "verificado" que o produto nao pode dar (5.10).
  "linha.credencial.INFORMADO": "{credencial} · informado",
  "linha.credencial.VERIFICADO": "{credencial} · verificado",
  "linha.credencial.SUSPENSO": "{credencial} · suspenso",

  // Correcao e sucessao, e se ve (5.2). Nada de aba "historico", nada de "(editado)".
  "linha.correcoes": "{quantas, plural, one {# correção} other {# correções}}",

  // Os dois instantes, quando divergem. Quando coincidem, nao se diz nada — ruido nao e
  // transparencia (5.2).
  "linha.registradoEm": "registrado em {data}",

  // A variacao cabe na linha; a curva nao (5.2, emenda de 2026-08-07).
  "linha.peso.variacao": "{sinal}{diferenca} kg desde a última",

  // A orientacao e o cumprimento dela chegam com o MESMO texto — a view usa a descricao
  // da orientacao nos dois. Sem distinguir, a linha mostraria "Amoxicilina, 8h" duas
  // vezes em dias diferentes, e o leitor nao saberia qual e qual. Nao e rotulo de tipo
  // como estrutura (o que a 5.1 recusa): e o fato dizendo o que aconteceu.
  "linha.orientacao.emitida": "Orientação: {o que}",
  "linha.orientacao.cumprida": "Cumprido: {o que}",

  // ------------------------------------------------------------------------ navegacao
  //
  // Nao e erro da API, e por isso nao tem codigo: e um endereco que nao existe. O texto
  // segue a mesma regra — o fato, sem culpar quem digitou.
  // -------------------------------------------------- quem cuida, e o que leu (Tela 22)
  //
  // A tese da tela, na frase dela mesma: "conceder deixa de ser um ato de fé quando você
  // vê o que acontece depois". Por isso o escopo aparece em FRASE, e nunca a palavra
  // "escopo" — a seção 09 da voz proíbe usá-la com o tutor.
  "acesso.voltar": "Início",
  "acesso.titulo": "Quem cuida do {nome}",
  "acesso.apoio": "Cada acesso que você concedeu, com o que foi lido e quando. Conceder deixa de ser um ato de fé quando você vê o que acontece depois.",
  "acesso.carregando": "Carregando os acessos…",
  "acesso.vazio": "Nenhuma organização alcança o {nome} hoje.",
  "acesso.vazio.acao": "Conceder acesso a uma organização",
  "acesso.pessoas": "Pessoas",
  "acesso.pessoas.vazio": "Só você responde por este animal.",
  "acesso.pessoas.transferir": "Transferir a titularidade",
  "acesso.pessoa.titular": "Responde pelo {nome}",
  "acesso.pessoa.coTutor": "Co-tutor · responde pelo {nome} com você",
  "acesso.ultimosAcessos": "Últimos acessos",
  "acesso.ultimosAcessos.vazio": "Ninguém desta organização leu nada ainda.",
  "acesso.leitura.linha": "{quem} {o_que}",
  "acesso.leitura.total": "{total, plural, one {# acesso ao todo} other {# acessos ao todo}}",
  "acesso.leitura.VACCINES": "abriu a vacinação",
  "acesso.leitura.HEALTH_RECORDS": "abriu o histórico clínico",
  "acesso.leitura.VACCINE_CORRECTIONS": "abriu as correções de vacina",
  "acesso.leitura.HEALTH_RECORD_CORRECTIONS": "abriu as correções de atendimento",
  "acesso.leitura.ATTACHMENTS": "abriu os anexos",
  "acesso.leitura.SHARED_CARD": "abriu o cartão compartilhado",
  "acesso.leitura.desconhecida": "abriu um registro",
  "acesso.acao.ajustar": "Ajustar",
  "acesso.acao.revogar": "Revogar",
  "acesso.acao.revogando": "Revogando…",
  "acesso.acao.concederDeNovo": "Conceder de novo",
  "acesso.encerrado.venceu": "Acesso venceu em {data} · não vê mais nada",
  "acesso.encerrado.revogado": "Acesso revogado em {data} · não vê mais nada",
  "acesso.semPrazo": "sem prazo",
  "acesso.ate": "até {data}",
  "acesso.escopo.nenhum": "não vê nada",
  "acesso.escopo.CARTEIRA": "vacinas",
  "acesso.escopo.CONDICOES": "alergias e condições",
  "acesso.escopo.PRONTUARIO": "histórico clínico",
  "acesso.escopo.OBSERVACOES": "observações",
  "acesso.escopo.PESO": "peso",
  "acesso.escopo.ANEXOS": "documentos",
  "acesso.escopo.CONTATO": "seu contato",
  // Revogar não apaga trabalho feito, e dizer isso ANTES do gesto é o que separa a
  // decisão informada do arrependimento.
  "acesso.revogar.titulo": "O que revogar faz",
  "acesso.revogar.texto": "Fecha a porta a partir de agora. O que a organização registrou continua na linha do tempo, assinado por quem registrou — revogar acesso não apaga trabalho feito.",

  // --------------------------------------------- convidar quem cuida (a Tela 11 ganhou a porta)
  //
  // O convite de co-tutor só existia no passo 4 do cadastro do animal: quem passasse dali sem
  // preencher o e-mail nunca mais convidava ninguém para aquele animal. O backend estava inteiro
  // desde o P4 — convidar, listar, revogar, aceitar e recusar. Faltava a porta.
  //
  // O E-MAIL SAI AGORA, e por muito tempo não saía: o `invite` gravava o convite e devolvia o
  // token, e não havia envio em lugar nenhum. O `InviteNotifier` fechou esse buraco.
  //
  // O LINK CONTINUA NA TELA MESMO ASSIM, e não é redundância. O e-mail pode cair na caixa de spam,
  // o endereço pode ter um dedo trocado, e quem convida costuma mandar por mensagem de qualquer
  // jeito. A frase promete só o que o servidor faz — enviamos —, e entrega o link para o caso de
  // ele não chegar.
  "acesso.convidar.rotulo": "Convidar outra pessoa para cuidar",
  "acesso.convidar.oQueElaGanha":
    "Quem aceitar responde pelo animal com você: vê tudo e registra em nome próprio. Uma clínica ou creche, que vê só o que você marcar, entra por “Conceder acesso”.",
  "acesso.convidar.botao": "Convidar",
  "acesso.convidar.enviando": "Convidando…",
  "acesso.convidar.enviado":
    "Mandamos o convite para {email}, com um código para ela colar no Petfy. Se não chegar, este link abre o mesmo convite:",
  "acesso.convidar.copiar": "Copiar o link",
  "acesso.convidar.copiado": "Link copiado",
  "acesso.convidar.oQueE": "o convite",

  // ------------------------------------------------------- conceder acesso (Tela 09)
  //
  // O desenho nomeia o problema: "o problema de design mais difícil da área do tutor é
  // fazer alguém que nunca ouviu a palavra escopo escolher um. A saída é não nomear a
  // abstração — nomear o que a organização vai ver". Então cada linha diz O QUE a
  // organização verá e POR QUE ela precisa, e o rótulo do dado nunca aparece.
  "conceder.voltar": "Quem cuida",
  "conceder.escolher.titulo": "Quem vai poder ver o {nome}",
  "conceder.escolher.apoio": "Escolha a organização. Se ela não estiver aqui, é porque ainda não tem cadastro no Petfy.",
  // O desenho do onboarding (passo 4) escreve o campo assim: "buscar organização".
  "conceder.escolher.busca": "Buscar organização por nome ou cidade",
  "conceder.escolher.carregando": "Carregando as organizações…",
  "conceder.escolher.vazio": "Nenhuma organização cadastrada ainda.",
  "conceder.escolher.semResultado": "Nenhuma organização com esse nome.",
  "conceder.escolher.acao": "Escolher",
  "conceder.organizacao.semCidade": "Organização",
  "conceder.organizacao.cidade": "{cidade}, {estado}",
  "conceder.titulo": "O que a {organizacao} vai poder ver do {nome}",
  "conceder.apoio": "Você escolhe item por item, e pode mudar ou revogar quando quiser. Quem tem acesso não responde pelo {nome} — isso continua sendo seu.",
  // As quatro linhas concedíveis, na ordem do desenho. O título é o que a organização vê;
  // a segunda linha é por que ela precisa — nunca o nome técnico do dado.
  "conceder.item.CONDICOES": "Alergias e condições em curso",
  "conceder.item.CONDICOES.porque": "Para não receitar nada que faça mal ao {nome}.",
  "conceder.item.CARTEIRA": "Vacinação e antiparasitário",
  "conceder.item.CARTEIRA.porque": "Para saber o que já foi aplicado e o que falta.",
  "conceder.item.PRONTUARIO": "Diagnósticos, prescrições e exames",
  "conceder.item.PRONTUARIO.porque": "O histórico clínico completo. Quem trata o {nome} precisa disto.",
  "conceder.item.OBSERVACOES": "Fotos e recados que outras organizações enviaram",
  "conceder.item.OBSERVACOES.porque": "É biografia, não saúde. A clínica não precisa.",
  // A quinta linha do desenho, desabilitada com o motivo ao lado: a seção 06 pede que o
  // desabilitado nunca apareça mudo. Conceder escrita não existe na API — o `GrantLevel`
  // é sempre EDITOR e o escopo só governa leitura.
  "conceder.item.registrar": "Registrar novos atendimentos",
  "conceder.item.registrar.porque": "Sem isto, a clínica só lê. Marque quando o {nome} for se tratar lá.",
  "conceder.item.registrar.indisponivel": "Ainda não é possível escolher isto: o Petfy hoje concede leitura, e quem registra atendimento é a própria organização, pela área dela.",
  "conceder.ate.rotulo": "Até quando",
  "conceder.ate.apoio": "Todo acesso tem prazo. Você pode revogar antes disso, a qualquer momento.",
  "conceder.resumo.rotulo": "Resumo em uma frase",
  "conceder.resumo.frase": "A {organizacao} vai ler {o_que} do {nome} até {data}.",
  "conceder.resumo.semNada": "Escolha ao menos um item acima para a {organizacao} poder ver algo do {nome}.",
  "conceder.resumo.naoVai": "E não vai ver {o_que}, nem registrar nada.",
  "conceder.resumo.naoVaiNada": "E não vai registrar nada.",
  "conceder.depois": "Você vai ver quem leu o quê, e quando, na rede de quem cuida do {nome}.",
  "conceder.acao": "Conceder acesso",
  "conceder.acao.concedendo": "Concedendo…",
  "conceder.acao.cancelar": "Cancelar",

  // --------------------------------------------------------- criar conta (Tela 07)
  //
  // "Uma conta serve para tudo" é a tese da tela, e ela é o oposto de um seletor de perfil:
  // quem responde pela área é o que a pessoa TEM, não o que ela declarou no cadastro.
  "criarConta.titulo": "Criar conta",
  "criarConta.apoio": "Uma conta serve para tudo: cuidar dos seus animais e atender os animais de outras pessoas. Você decide isso depois, e pode mudar quando quiser.",
  "criarConta.nome": "Nome completo",
  "criarConta.email": "E-mail",
  "criarConta.senha": "Senha",
  "criarConta.senha.mostrar": "Mostrar",
  "criarConta.senha.esconder": "Esconder",
  // O desenho escreve 10; o contrato aceita 8. O cliente é mais rigoroso de propósito.
  "criarConta.senha.minimo": "Ao menos {minimo} caracteres.",
  "criarConta.termos": "Li e aceito os termos de uso e a política de privacidade, inclusive o tratamento de dados de saúde dos animais que eu registrar.",
  "criarConta.faltaAceite": "Falta aceitar os termos para criar a conta.",
  "criarConta.faltaCampo": "Preencha nome, e-mail e uma senha de ao menos 10 caracteres.",
  "criarConta.acao": "Criar conta",
  "criarConta.acao.criando": "Criando…",
  "criarConta.acao.comConvite": "Criar conta e entrar na equipe",
  "criarConta.jaTem": "Já tem conta?",
  // Quem chegou por um convite sem ter conta. A tela diz o que sabe — que há um convite em
  // vigor — e não afirma o que não sabe: qual organização convidou. Ver o cabeçalho da tela.
  "criarConta.convite.titulo": "Você está entrando por um convite",
  "criarConta.convite.texto":
    "Ele é conferido no momento em que a conta for criada, e a organização que convidou aparece a partir dali. Use o mesmo e-mail para o qual o convite foi enviado — um convite endereçado a outra pessoa não vale.",
  "criarConta.entrar": "Entrar",
  // O painel "declarar credencial · em qualquer momento". Criar a conta é um deles.
  "criarConta.credencial.abrir": "Sou veterinário e quero declarar meu registro",
  "criarConta.credencial.registro": "Registro profissional (CRMV)",
  "criarConta.credencial.uf": "UF",
  "criarConta.credencial.informado": "O Petfy ainda não consulta o conselho. Seu registro aparece como informado em tudo que você assinar, e quem lê sabe disso.",
  "criarConta.credencial.semClinica": "Não é preciso ter clínica. Atendimento domiciliar funciona inteiro sem organização nenhuma.",

  // ------------------------------------------------ contestar um registro (Tela 23)
  //
  // Não há botão de apagar, e isso é a tese: "quem escreveu é quem corrige — é o que faz o
  // registro valer alguma coisa". O tutor escreve ao lado, e o que ele escreve também fica.
  "discordar.titulo": "Você discorda deste registro",
  "discordar.apoio": "Quem escreveu é quem corrige — é o que faz o registro valer alguma coisa. O que você pode fazer é registrar sua discordância ao lado, e ela também fica para sempre.",
  "discordar.registro": "O registro",
  // A categoria é valor de domínio, e mostrar a constante seria mostrar código na cara de
  // quem lê. O `eventType` que o profissional escreveu ganha da categoria quando existe.
  "animal.categoria.CONSULTA": "Consulta",
  "animal.categoria.RETORNO": "Retorno",
  "animal.categoria.EXAME": "Exame",
  "animal.categoria.CIRURGIA": "Cirurgia",
  "animal.categoria.INTERNACAO": "Internação",
  "animal.categoria.EMERGENCIA": "Emergência",
  "animal.categoria.PROCEDIMENTO": "Procedimento",
  "animal.categoria.OUTRO": "Atendimento",
  "discordar.registro.carregando": "Carregando o registro…",
  "discordar.registro.quando": "Registrado em {data}",
  "discordar.sua": "Sua observação",
  "discordar.campo": "O que você viu de diferente",
  "discordar.campo.apoio": "Escreva o que aconteceu, não o que acha que quem atendeu errou. Sua observação entra na linha do tempo em {data}, ao lado do registro, assinada por você.",
  "discordar.avisar": "Avisar quem registrou. Essa pessoa pode corrigir o registro dela, se concordar com você.",
  "discordar.avisar.porque": "Ainda não dá para avisar: o Petfy não envia mensagem sobre observação, e uma caixa que promete aviso sem mandar nada seria pior que a ausência dela. Sua observação fica no lugar certo do mesmo jeito.",
  "discordar.acao": "Registrar minha observação",
  "discordar.acao.registrando": "Registrando…",
  "discordar.cancelar": "Cancelar",
  "discordar.falar": "Falar direto com quem registrou",
  "discordar.falar.porque": "Ainda não há como: o registro traz o nome de quem atendeu, e não o contato dela. Falar pelo Petfy depende de um canal que ainda não existe.",
  "discordar.seCorrigir": "Se ela corrigir",
  "discordar.seCorrigir.texto": "A correção aparece ao lado do original, com o motivo e a hora. O registro não desaparece — ele passa a ter duas versões, e as duas ficam visíveis.",
  "discordar.seNaoResponder": "Se ela não responder",
  "discordar.seNaoResponder.texto": "Sua observação continua lá, do lado. Todo veterinário que abrir a linha do tempo do {nome} vai ler as duas coisas.",

  // ----------------------------------------------- os três cômodos (Tela 08)
  //
  // "Nenhum passo pergunta se você é tutor ou profissional." Os três ficam abertos, e a
  // área que a pessoa alcança vem do que ela tem — não do que declarou.
  "comecar.agindoComo": "Agindo como",
  "comecar.titulo": "Bem-vindo, {nome}",
  "comecar.apoio": "O Petfy fica útil quando existe um animal com histórico aqui. Comece por onde fizer sentido para você — nada aqui é obrigatório, e nada disso bloqueia o resto.",
  "comecar.animal.titulo": "Cadastrar um animal",
  "comecar.animal.texto": "Nome e espécie bastam para começar. A carteirinha de papel você lança depois, com calma.",
  "comecar.animal.acao": "Começar",
  "comecar.rede.titulo": "Convidar quem mais cuida",
  "comecar.rede.texto": "Quem divide a casa, a clínica que atende, a creche. Você escolhe o que cada um vê.",
  "comecar.rede.acao": "Convidar",
  "comecar.rede.semAnimal": "Convite é sempre para um animal, e você ainda não tem nenhum cadastrado. Comece pelo primeiro cômodo.",
  "comecar.profissional.titulo": "Atender animais de outras pessoas",
  "comecar.profissional.texto": "Se você é veterinário, monitor ou voluntário, declare seu registro profissional ou aceite o convite de uma organização.",
  "comecar.profissional.acao": "Criar uma organização",
  "comecar.profissional.indisponivel": "O registro profissional é declarado na criação da conta. Declarar depois, e aceitar convite de organização com uma conta que já existe, ainda não têm caminho.",
  "comecar.tese": "Nenhum passo pergunta se você é tutor ou profissional. Os três cômodos ficam abertos, e a área que você alcança vem do que você tem — um animal sob sua custódia, ou um vínculo com uma organização.",

  // ------------------------------------------- o primeiro animal, em quatro passos
  //
  // A regra do desenho: "só o passo 1 é obrigatório. Do 2 em diante, 'agora não' é um botão
  // de verdade". E: "cada etapa deixa algo registrado, e sair no meio não desfaz nada".
  "onboarding.depois": "Fazer isso depois",
  "onboarding.continuar": "Continuar",
  "onboarding.registrando": "Registrando…",
  "onboarding.opcional": "· opcional",
  "onboarding.trilho.animal": "O animal",
  "onboarding.trilho.identificacao": "Identificação",
  "onboarding.trilho.carteirinha": "Carteirinha",
  "onboarding.trilho.quemCuida": "Quem mais cuida",
  "onboarding.p1.titulo": "Quem é o animal?",
  "onboarding.p1.apoio": "Nome e espécie bastam. O resto pode entrar a qualquer momento, inclusive anos depois.",
  "onboarding.p1.nome": "Como você chama ele",
  "onboarding.p1.especie": "Espécie",
  "onboarding.p1.especie.outro": "Outro",
  "onboarding.p1.especie.outro.porque": "Hoje o Petfy só registra cão e gato: a espécie é um dado do domínio, e não um texto livre. Outra espécie ficaria registrada errada.",
  "onboarding.p1.nascimento": "Nascimento",
  "onboarding.p1.nascimento.apoio": "Só o mês e o ano servem. Estimativa também.",
  "onboarding.p1.foto": "Foto do animal",
  "onboarding.p1.foto.apoio": "Ajuda quem cuida a reconhecer ele no balcão. Pode ficar para depois.",
  "onboarding.p1.foto.indisponivel": "Ainda não dá para enviar a foto: o contrato da API não descreve o envio de arquivo, e o cliente é gerado a partir dele.",
  "onboarding.p1.aviso": "O {nome} já fica registrado agora. Os próximos passos são opcionais.",
  "onboarding.p2.titulo": "O {nome} tem algum número de identificação?",
  "onboarding.p2.apoio": "Metade dos animais no Brasil não tem nenhum, e o Petfy funciona igual sem. Se tiver, o microchip é o que permite reconhecer o {nome} se ele se perder e for encontrado por outra pessoa.",
  "onboarding.p2.microchip": "Microchip",
  "onboarding.p2.microchip.apoio": "15 dígitos, geralmente na carteirinha ou na nota da aplicação.",
  "onboarding.p2.semRgaNemTatuagem": "RGA e tatuagem ainda não têm onde ser guardados. Só o microchip tem campo próprio, e usar o dele para outro número faria o registro mentir.",
  "onboarding.p2.naoValida": "O Petfy não emite nem valida esses números — guarda e usa o que você informar.",
  "onboarding.p2.naoTem": "O {nome} não tem nenhum",
  "onboarding.p3.titulo": "O que o {nome} já tomou",
  "onboarding.p3.apoio": "É o passo mais trabalhoso e o mais valioso: sem ele, o Petfy não sabe o que vence e quando.",
  "onboarding.p3.indisponivel": "A leitura da carteirinha por foto existe no servidor, mas o contrato da API não descreve o envio do arquivo — e o cliente desta tela é gerado a partir dele. Enquanto isso, cada dose pode ser lançada uma a uma na tela do animal, e cada uma entra na data em que foi aplicada.",
  "onboarding.p3.naoTenho": "Não tenho a carteirinha agora",
  "onboarding.p4.titulo": "Quem mais cuida do {nome}?",
  "onboarding.p4.apoio": "Você escolhe o que cada pessoa ou organização vê, e por quanto tempo. Dá para mudar ou revogar depois, a qualquer momento.",
  "onboarding.p4.pessoa": "Alguém que divide o cuidado",
  "onboarding.p4.pessoa.apoio": "Quem também dá remédio e leva ao veterinário. Vê tudo e registra junto com você.",
  "onboarding.p4.pessoa.campo": "e-mail",
  "onboarding.p4.convidar": "Convidar",
  // "Convite enviado" era mentira quando foi escrito, e passou a ser verdade agora: até o
  // `InviteNotifier`, o backend gravava o convite, devolvia o token e não mandava nada. Quem
  // chegava aqui saía achando que tinha convidado alguém.
  "onboarding.p4.convidado":
    "Mandamos o convite para {email}, com um código para ela colar no Petfy. Se não chegar, este link abre o mesmo convite. Ela aparece na rede quando aceitar.",
  "onboarding.p4.organizacao": "Uma clínica ou creche",
  "onboarding.p4.organizacao.apoio": "Vê só o que você marcar, pelo prazo que você definir. Nada de tudo ou nada.",
  "onboarding.p4.organizacao.semAnimal": "Primeiro cadastre o animal, no passo 1.",
  "onboarding.p4.escolherOQueVe": "Escolher o que ela vê",
  "onboarding.p4.ir": "Ir para o {nome}",
  "onboarding.p4.sozinho": "Cuido sozinho por enquanto",

  // ------------------------------------------------- consentimento atualizado (Tela 07)
  //
  // "Sem alarme e sem tom de erro: o texto mudou, não a conta" — a nota do desenho. Por
  // isso não há "atenção", não há vermelho e "agora não" é saída de verdade.
  "consentimento.titulo": "Atualizamos os termos",
  "consentimento.texto": "Mudou o texto de {documentos}. Você continua usando o Petfy do mesmo jeito.",
  "consentimento.documento.TERMS_OF_SERVICE": "termos de uso",
  "consentimento.documento.PRIVACY_POLICY": "política de privacidade",
  "consentimento.documento.OUTRO": "um documento",
  "consentimento.aceitar": "Aceitar",
  "consentimento.aceitando": "Aceitando…",
  "consentimento.agoraNao": "Agora não",

  // -------------------------------------------------- transferir a titularidade (Tela 11)
  //
  // A regra do desenho: "nenhuma custódia termina sem sucessor". Por isso não existe
  // "abandonar" nem "sair" — existe passar para alguém, e a pessoa precisa aceitar.
  "transferir.titulo": "Transferir o {nome} para outra pessoa",
  "transferir.apoio": "A vida inteira do {nome} vai junto. Quem recebe passa a responder por ele a partir do aceite — não existe deixar o {nome} sem ninguém.",
  "transferir.paraQuem": "Para quem",
  "transferir.paraQuem.apoio": "A pessoa precisa aceitar. Enquanto não aceitar, o {nome} continua sob sua responsabilidade.",
  "transferir.motivo": "Motivo",
  "transferir.motivo.indisponivel": "Ainda não é possível registrar o motivo: o convite guarda o e-mail, o papel e o prazo, e não há campo para ele. Um campo que aceitasse o texto e o jogasse fora seria pior.",
  "transferir.junto.rotulo": "O que vai junto com o {nome}",
  "transferir.junto.linha": "A linha do tempo inteira, desde {ano}",
  "transferir.junto.linhaVazia": "A linha do tempo inteira, do jeito que ela está hoje",
  "transferir.junto.orientacao": "{o_que} em curso",
  // O desenho escreve "com as 6 doses já dadas". A contagem não existe no
  // CareInstructionResponseDTO — ele traz a última, e é isso que a linha diz.
  "transferir.junto.orientacaoComDose": "{o_que} em curso, com a última dose em {data}",
  "transferir.junto.condicoes": "{condicoes, plural, =0 {Nenhuma condição registrada} one {# condição registrada} other {# condições registradas}} e {anexos, plural, =0 {nenhum anexo} one {# anexo} other {# anexos}}",
  // O desenho escreveu "quem deixa de ver o Code". O backend não revoga nada de ninguém
  // na transferência, então a tela diz o que acontece de fato — com o mesmo peso de alerta.
  // O painel virou "quem deixa de ver", como o desenho pede. A mudança é do backend, e não de
  // texto: no aceite, toda concessão do animal é revogada e você fica com leitura.
  "transferir.continua.rotulo": "O que muda no acesso ao {nome}",
  "transferir.continua.voce": "Você",
  "transferir.continua.voce.texto": "Deixa de responder pelo {nome} e passa a apenas ver o histórico dele. Registrar, conceder e transferir passam a ser de quem recebe.",
  "transferir.continua.coTutor": "{quem}, co-tutor",
  "transferir.continua.coTutor.texto": "Perde o acesso no aceite. Quem recebe concede de novo, se quiser.",
  "transferir.continua.organizacao.texto": "Perde o acesso no aceite — inclusive o link compartilhado. Quem recebe concede de novo, se quiser.",
  "transferir.continua.tese": "Acessos não são herdados: quem autorizou foi você, e quem passa a responder pelo animal decide do zero quem alcança ele.",
  "transferir.acao": "Enviar transferência",
  "transferir.acao.enviando": "Enviando…",
  "transferir.acao.cancelar": "Cancelar",
  "transferir.enviado.titulo": "Transferência enviada para {email}",
  "transferir.enviado.apoio": "Enquanto a pessoa não aceitar, o {nome} continua sob sua responsabilidade e nada mudou.",
  "transferir.enviado.prazo": "O convite vale até {data}.",
  "transferir.enviado.voltar": "Voltar para a vida do {nome}",

  // ------------------------------------------------- a matrícula na creche (Tela 10)
  //
  // "O produto responde pela saúde": a creche não julga e o tutor não prova nada. O servidor cruza
  // o que a organização exige com o que a carteira tem, e esta tela só mostra.
  "animal.acao.creches": "Creches",
  "animal.acao.compra": "Lançar uma compra",
  "animal.acao.custo": "Quanto custou",
  // O caminho para a Tela 33. Escrito no infinitivo e sem eufemismo: "encerrar a ficha" ou
  // "arquivar" faria a pessoa clicar sem saber o que vai encontrar do outro lado.
  "animal.acao.encerrar": "Encerrar a linha do tempo do {nome}",
  "matricula.titulo": "As creches do {nome}",
  "matricula.apoio": "A comprovação de saúde é do produto, e não da creche: o Petfy compara o que cada organização exige com o que a carteira tem, e refaz essa conta a cada vez que alguém olha.",
  "matricula.oQue": "as matrículas",
  "matricula.vazio": "O {nome} não está matriculado em nenhuma creche.",
  "matricula.turma": "Turma {turma}",
  "matricula.status.PENDENTE": "Matrícula pendente",
  "matricula.status.ATIVA": "Matrícula ativa",
  "matricula.status.ENCERRADA": "Matrícula encerrada",
  "matricula.comprovacao": "Comprovação de saúde",
  "matricula.comprovacao.semExigencia": "Esta organização não exige vacina nenhuma para matricular.",
  "matricula.linha.emDia": "Em dia até {data}",
  "matricula.linha.emDiaSemPrazo": "Em dia — a dose está registrada e não tem prazo declarado",
  "matricula.linha.vencida": "Venceu em {data} — impede a matrícula",
  "matricula.linha.semRegistro": "Sem registro — impede a matrícula, porque não dá para afirmar o que ninguém viu",
  "matricula.linha.porNome": "Casada pelo nome da vacina, e não pelo catálogo: a prova é mais fraca.",
  "matricula.pendente.titulo": "A matrícula está guardada",
  "matricula.pendente.texto": "Ela se completa sozinha assim que a dose que falta for registrada na carteira. Ninguém precisa refazer o cadastro.",
  "matricula.pendente.semAviso": "Ainda não há como avisar a creche daqui — não existe canal de aviso para matrícula. O que existe é isto: registre a dose, e ela ativa.",
  "matricula.precisaSaber": "O que a creche precisa saber",
  "matricula.precisaSaber.vazio": "Nada registrado sobre o {nome} que a creche precise saber.",
  // A única linha do produto que ensina a diferença entre ausência de dado e ausência de acesso.
  "matricula.naoCompartilhado": "O histórico completo de atendimentos do {nome} existe e não foi compartilhado com a {organizacao}. Isso é diferente de não existir.",
  "matricula.voltar": "Voltar para a vida do {nome}",

  // ------------------------------------------ a operação do dia da creche (Tela 17)
  //
  // A única tela em que a pressa é parte do contexto: 7h34, catorze esperados, quem opera com um
  // cachorro em cada mão. O topo conta em vez de listar, e cada linha tem um gesto só.
  "creche.contagem": "{esperados, plural, one {# esperado} other {# esperados}} · {chegaram, plural, one {# já chegou} other {# já chegaram}}",
  "creche.turma.comLimite": "{nome} · {ocupadas} de {vagas} vagas",
  "creche.turma.semLimite": "{nome} · {ocupadas, plural, one {# animal} other {# animais}}",
  "creche.oQue.turmas": "as turmas",
  "creche.oQue.dia": "o dia da turma",
  "creche.semTurma": "Nenhuma turma criada ainda. A turma é o que dá vaga e organiza o dia.",
  "creche.turmaVazia": "Ninguém matriculado nesta turma.",
  "creche.coluna.animal": "Animal",
  "creche.coluna.hojePrecisa": "Hoje precisa",
  "creche.coluna.entrada": "Entrada",
  "creche.nada": "Nada",
  "creche.impedido": "{motivo} — não pode entrar hoje",
  // O desenho põe "Avisar tutor" aqui. Avisar não existe no backend, e um botão que não avisa
  // ninguém seria pior: o que fica é a frase e o caminho real, que é registrar a dose.
  "creche.avisarNaoDa": "Ainda não dá para avisar o tutor daqui. A entrada libera sozinha quando a dose for registrada na carteira.",
  "creche.estado.PRESENTE": "Chegou",
  "creche.estado.SAIU": "Saiu {hora}",
  "creche.estado.FALTA": "Falta",
  "creche.acao.entrada": "Marcar entrada",
  "creche.acao.saida": "Marcar saída",
  "creche.acao.falta": "Falta",
  // Os três estados da comprovação. "Não sabemos" não é "está ruim" — e as duas impedem.
  "creche.comprovacao.EM_DIA": "Em dia",
  "creche.comprovacao.VENCIDA": "Vencida",
  "creche.comprovacao.SEM_REGISTRO": "Sem registro",

  // ----------------------------------------------------- a adoção (Tela 13)
  //
  // "O adotante não ganha uma ficha em branco com a data de hoje: ganha onze anos de vida de
  // um animal que ele acabou de conhecer." É a tese do produto inteiro, num gesto só.
  "adocao.titulo": "Adoção do {nome}",
  "adocao.apoio": "A vida registrada passa inteira para quem adota. O {abrigo} continua vendo o que produziu, e deixa de mandar no registro.",
  // O caminho para a Tela 39, antes do convite: depois de ele sair já é tarde para a conversa
  // que impede o animal de voltar.
  "adocao.verCusto": "Ver quanto o {nome} custou, para conversar com quem vai adotar",
  "adocao.oQue": "este animal",
  "adocao.oQue.enviar": "a adoção",
  "adocao.recebe": "O que o adotante recebe",
  "adocao.recebe.linha": "A linha do tempo inteira, desde {desde}",
  "adocao.recebe.atendimentos": "{quantos, plural, one {# atendimento} other {# atendimentos}}, por {pessoas, plural, one {# pessoa} other {# pessoas}}",
  "adocao.recebe.condicoes": "{quantas, plural, one {# condição registrada} other {# condições registradas}} — alergias e o que evitar",
  "adocao.recebe.emCurso": "{quantas, plural, one {# tratamento em curso} other {# tratamentos em curso}}, com as doses já dadas",
  "adocao.recebe.vazio": "O {nome} ainda não tem nada registrado além do cadastro.",
  // A devolução de 2019 que o desenho mostra é justamente o que não temos.
  "adocao.recebe.faltaResgate": "O resgate, o tempo em lar transitório e uma devolução anterior não aparecem aqui: não existem como evento no Petfy, e a história de custódias não tem consulta. O desenho faz questão de mostrar a devolução — é biografia, e quem adota tem direito de saber. Ainda não dá.",
  "adocao.adotante": "Adotante",
  "adocao.adotante.campo": "e-mail de quem vai adotar",
  "adocao.adotante.semConta": "Se ela ainda não tiver conta no Petfy, vai criar uma ao aceitar — e o {nome} já estará dentro.",
  "adocao.mudaParaOAbrigo": "O que muda para vocês",
  "adocao.passaALer": "A partir do aceite, o {abrigo} passa a ler o que registrou, e não decide mais nada sobre o {nome}. Quem adotou pode revogar esse acesso quando quiser.",
  "adocao.tese": "Onze anos de vida registrada não recomeçam: quem adota recebe o histórico inteiro, assinado por quem fez cada parte dele.",
  "adocao.acao": "Enviar adoção",
  "adocao.acao.enviando": "Enviando…",
  "adocao.cancelar": "Cancelar",
  "adocao.enviado.titulo": "Adoção enviada para {email}",
  "adocao.enviado.apoio": "Enquanto a pessoa não aceitar, o {nome} continua sob a responsabilidade do {abrigo} e nada mudou.",
  "adocao.enviado.voltar": "Voltar para os animais",

  // --------------------------------------- a área de organização (Tela 03)
  //
  // A tabela do desenho tem quatro colunas; três dependem de agregação que nenhuma rota faz.
  // O que sobra é dito com o nome certo, e a ausência é declarada em vez de disfarçada.
  "pacientes.titulo": "Pacientes",
  "pacientes.meusAnimais": "Meus animais",
  "pacientes.hoje": "Hoje na creche",
  "pacientes.busca": "Buscar por nome, microchip ou RGA",
  // Os dois recortes que uma consulta responde. "Vencendo", "em tratamento" e "atendidos este
  // mês" continuam sem agregação — a nota ao lado diz isso.
  "pacientes.aba.acesso": "Com acesso concedido",
  "pacientes.aba.acesso.contados": "Com acesso concedido · {quantos}",
  "pacientes.aba.custodia": "Sob custódia",
  "pacientes.aba.custodia.contados": "Sob custódia · {quantos}",
  "pacientes.semTutor": "sem tutor humano",
  "pacientes.adotar": "Adoção",
  "pacientes.abrir": "Abrir",
  // Os três recortes que o resumo responde. Eles contam sobre a organização inteira, e por isso
  // não mudam quando a busca filtra a lista.
  "pacientes.recorte.vencendo": "vencendo em 30 dias · {quantos}",
  "pacientes.recorte.tratamento": "em tratamento · {quantos}",
  "pacientes.recorte.atendidos": "atendidos este mês · {quantos}",

  "pacientes.coluna.animal": "Animal",
  "pacientes.coluna.tutor": "Tutor",
  "pacientes.coluna.situacao": "Situação",
  "pacientes.coluna.ultimaVisita": "Última visita",

  // "Sem prazo" não é "em dia": pode ser dose única e pode ser carteira que ninguém registrou.
  // A frase diz o que o produto sabe, e não o que ele gostaria de afirmar.
  "pacientes.situacao.OVERDUE": "Vencida",
  "pacientes.situacao.DUE_SOON": "Vencendo",
  "pacientes.situacao.UP_TO_DATE": "Em dia",
  "pacientes.situacao.NO_NEXT_DOSE": "Sem prazo registrado",
  "pacientes.emTratamento": "Em tratamento",
  "pacientes.semVisita": "sem visita",
  "pacientes.atender": "Atender",
  "pacientes.oQue": "os pacientes",
  "pacientes.vazio": "Nenhum tutor concedeu acesso a esta organização ainda.",
  "pacientes.semResultado": "Nenhum paciente com esse nome.",
  "pacientes.mostrando": "Mostrando {quantos} de {total}",
  "pacientes.idade.anos": "{anos, plural, one {# ano} other {# anos}}",
  "pacientes.idade.meses": "{meses, plural, =0 {recém-nascido} one {# mês} other {# meses}}",
  "pacientes.vencendo.titulo": "Quem está vencendo",
  "pacientes.vencendo.titulo.contados": "Quem está vencendo · {quantos} no total",
  // O gesto do desenho — avisar os tutores — continua não existindo, e a frase diz por quê sem
  // prometer data. Um botão que não avisa ninguém seria pior que a ausência dele.
  "pacientes.vencendo.aviso": "Avisar o tutor daqui ainda não existe: o produto não tem canal de aviso. Quando tiver, o aviso fala só da dose — acesso concedido não é lista de marketing.",

  // ------------------------------------------------- criar a organização (Tela 15)
  //
  // "A creche não assina por ninguém": a organização existe para o trabalho da equipe ficar
  // assinado em nome dela, e quem registra continua tendo nome próprio (seção 10).
  "organizacao.nova.titulo": "Criar uma organização",
  "organizacao.nova.apoio": "Uma organização existe para que o trabalho da equipe fique assinado em nome dela. Você continua sendo {quem} em tudo que registrar — a organização não assina por ninguém.",
  "organizacao.nova.nome": "Nome que os tutores vão ver",
  "organizacao.nova.oQueFazem": "O que vocês fazem",
  "organizacao.nova.oQueFazem.creche": "Creche e hospedagem",
  "organizacao.nova.oQueFazem.clinica": "Clínica veterinária",
  "organizacao.nova.oQueFazem.banhoETosa": "Banho e tosa",
  "organizacao.nova.oQueFazem.abrigo": "Abrigo ou resgate",
  "organizacao.nova.oQueFazem.adestramento": "Adestramento",
  "organizacao.nova.oQueFazem.indisponivel": "Ainda não dá para declarar isso: o cadastro de organização não guarda o que ela faz, e é esse dado que definiria o que a equipe consegue registrar. Marcar aqui não viajaria para lugar nenhum.",
  "organizacao.nova.cnpj": "CNPJ",
  "organizacao.nova.telefone": "Telefone para emergência",
  "organizacao.nova.telefone.apoio": "Aparece para o tutor quando o animal está com vocês.",
  "organizacao.nova.endereco": "Endereço",
  "organizacao.nova.cidade": "Cidade",
  "organizacao.nova.estado": "UF",
  "organizacao.nova.comecaVazia": "Criar uma organização não dá acesso a animal nenhum. Cada tutor concede o que quiser, animal por animal, e pode revogar quando quiser. {nome, select, other {Ela}} começa vazia.",
  "organizacao.nova.acao": "Criar a organização",
  "organizacao.nova.criando": "Criando…",
  "organizacao.nova.responsavel": "Você fica como responsável.",
  "organizacao.nova.voltar": "Voltar para o início",
  "organizacao.nova.oQue": "a organização",

  // ------------------------------------------------- equipe e convites (Tela 16)
  "equipe.titulo": "Equipe e convites",
  "equipe.apoio": "Quem entra passa a registrar em nome da organização, e cada registro continua assinado com o nome de quem fez.",
  "equipe.pela": "· pela {organizacao}",
  "equipe.convidar": "Convidar pessoa",
  "equipe.convidar.campo": "e-mail",
  "equipe.convidar.acao": "Convidar",
  "equipe.convidando": "Convidando…",
  // A frase diz por que a função é escolhida AQUI, e não por quem aceita: sem isso o campo
  // parece burocracia. Com isso, ele é a decisão de quem responde pela organização.
  "equipe.convidar.apoio": "A função é escolhida por você, no convite — quem aceita não escolhe o próprio papel. Dá para ajustar depois.",
  "equipe.funcao.rotulo": "Função",
  "equipe.funcao.VOLUNTARIO": "Voluntária",
  "equipe.funcao.MONITOR": "Monitora",
  "equipe.funcao.VETERINARIO": "Veterinária",
  "equipe.funcao.ADMINISTRADOR": "Administradora",
  "equipe.aguardando": "{quantos, plural, =0 {Nenhum convite aguardando} one {# convite aguardando} other {# convites aguardando}}",
  "equipe.aguardando.vazio": "Ninguém foi convidado ainda.",
  "equipe.carregando": "Carregando os convites…",
  "equipe.convite.vale": "Vale até {data}",
  "equipe.convite.revogar": "Revogar",
  "equipe.encerrados": "{quantos, plural, one {# convite já encerrado} other {# convites já encerrados}} — aceitos, vencidos ou revogados.",
  "equipe.oQue": "os convites da equipe",
  "equipe.oQue.convite": "o convite",
  "equipe.oQue.equipe": "a equipe",
  "equipe.oQue.funcao": "a função",
  "equipe.oQue.desligamento": "o desligamento",

  // A tabela da equipe. O "desde" é mês e ano: dia exato não ajuda ninguém a decidir nada, e
  // sugere uma precisão que a pergunta ("faz tempo que ela está aqui?") não pede.
  "equipe.tabela": "{quantos, plural, =0 {Ninguém na equipe ainda} one {# pessoa na equipe} other {# pessoas na equipe}}",
  "equipe.carregando.membros": "Carregando a equipe…",
  "equipe.membro.desde": "Desde {data}",
  "equipe.membro.ajustar": "Função de {pessoa}",
  "equipe.membro.desligar": "Desligar",

  // Como o convite é aceito, dos dois lados. Deixou de ser uma ressalva sobre o que falta e
  // passou a ser instrução: os dois caminhos existem, e quem convida precisa saber o que dizer
  // a quem convidou.
  "equipe.comoAceita": "Quem ainda não tem conta entra criando a conta com o convite. Quem já é do Petfy abre o link do convite e aceita com a conta que já tem — sem precisar de um segundo cadastro.",

  // O LINK APARECE UMA VEZ SÓ, e a frase precisa dizer isso: o token existe fora do servidor
  // apenas nesta resposta. O e-mail com o código já saiu — o link aqui é para quando ele não
  // chega, e sair da tela sem copiá-lo custa um convite novo.
  "equipe.convite.link":
    "Mandamos o convite para {email}, com um código para colar no Petfy. Se não chegar, este link abre o mesmo convite — e ele aparece uma vez só.",

  // ------------------------------------------------- aceitar o convite (Tela 16, o outro lado)
  //
  // A tela de quem RECEBEU. Ela mostra de qual organização é o convite, com que função e até
  // quando vale, antes de qualquer botão: aceitar às cegas não é aceitar, e o que está em jogo é
  // entrar numa equipe que enxerga a saúde de animais alheios.
  "convite.titulo": "Você foi convidada para uma equipe",
  "convite.carregando": "Lendo o convite…",
  "convite.organizacao": "{organizacao} convidou você",
  "convite.porQuem": "O convite foi feito por {quem}.",
  "convite.funcao": "Você entra como {funcao}.",
  "convite.funcao.semFuncao": "Este convite foi emitido antes de as funções existirem, então você entra com a função que pode menos — um administrador ajusta depois.",
  "convite.vale": "Vale até {data}.",
  "convite.oQueMuda": "Entrar não dá acesso a animal nenhum por si só. Cada tutor concede o que quiser, animal por animal, e pode revogar quando quiser.",
  "convite.aceitar": "Aceitar e entrar na equipe",
  "convite.aceitando": "Entrando…",
  "convite.agora": "Pronto — você agora é da equipe de {organizacao}.",
  "convite.verEquipe": "Ver a equipe",
  "convite.oQue": "o convite",
  "convite.oQue.aceite": "o aceite",
  // O token não vem na URL: sem ele não há o que ler, e a tela diz isso em vez de mostrar um
  // erro de servidor para quem apenas digitou o endereço na mão.
  "convite.semToken.titulo": "Falta o link do convite",
  "convite.semToken.texto": "Esta tela precisa do link completo que você recebeu. Abra o convite pelo link, ou peça um novo a quem administra a organização.",
  // Recusado é um estado só, de propósito: o servidor não distingue expirado de revogado, de já
  // usado, nem de endereçado a outra pessoa — distinguir diria a quem tenta adivinhar qual parte
  // errou. A tela não pode inventar a distinção que o backend recusa a dar.
  "convite.invalido.titulo": "Este convite não vale mais",
  "convite.invalido.texto": "Ele pode ter vencido, sido revogado, já ter sido usado, ou ter sido enviado para outro e-mail. Peça um novo convite a quem administra a organização.",
  "convite.jaMembro.titulo": "Você já é da equipe",
  "convite.jaMembro.texto": "Sua conta já é membro ativo desta organização, então não havia nada para aceitar — e o convite continua de pé para quem for usá-lo.",

  // ------------------------------------------------- os estados (Tela 14)
  //
  // "Desenhados, não descritos." As três partes do erro de carga andam juntas: o que falhou,
  // de quem é a culpa e o que aconteceu com o dado — e a terceira é a que importa para quem
  // está com o animal doente na frente.
  "estado.erroDeCarga.titulo": "Não conseguimos carregar {o_que}",
  "estado.erroDeCarga.nossa": "O problema é nosso. Nada do registro foi perdido.",
  "estado.tentarDeNovo": "Tentar de novo",
  "estado.tentando": "Tentando…",
  "estado.carregando": "Carregando {o_que}…",
  "estado.carregando.quantos": "Carregando {quantos} {o_que}…",
  "estado.erroAoGravar": "Não conseguimos gravar {o_que}. O que você escreveu está aqui, intacto.",
  // O título do conflito afirma o fato e não acusa ninguém: quem chega nele acabou de fazer a
  // coisa certa, e o registro que importa já existe.
  "estado.conflito.titulo": "Já foi feito.",
  // O "o que" de cada tela, para a frase do erro dizer o nome da coisa e não "os dados".
  "acesso.oQue": "quem alcança o {nome}",
  "conceder.oQue": "a lista de organizações",
  "discordar.oQue": "este registro",
  "discordar.oQue.gravar": "sua observação",

  "rota.naoEncontrada.titulo": "Esta página não existe.",
  "rota.naoEncontrada.acao": "Ir para o início",

  // ----------------------------------------------------- a vida do animal (Tela 02)
  //
  // A voz e a da secao 09: o produto relata e orienta, nao opina sobre saude e nao usa
  // a palavra "escopo" com o tutor. Nada de "Ops!", nada de exclamacao, nada de emoji.
  "animal.carregando": "Carregando a vida deste animal…",
  "animal.voltar": "Ir para o início",
  "animal.especie.CANINA": "Cão",
  "animal.especie.FELINA": "Gato",
  "animal.genero.MACHO": "macho",
  "animal.genero.FEMEA": "fêmea",
  "animal.nascido": "Nascido em {data}",
  "animal.microchip": "Microchip {numero}",
  "animal.rga": "RGA {numero}",
  "animal.custodia": "Sob custódia de {nome} desde {desde}",
  "animal.acao.registrar": "Registrar evento",
  "animal.acao.compartilhar": "Compartilhar cartão",

  // A carteira: como o animal esta agora.
  "animal.carteira.vacinacao": "Vacinação",
  "animal.carteira.vacinacao.vazio": "Nenhuma vacina registrada, e o catálogo não sabe o que esperar desta espécie.",
  "animal.carteira.venceuHa": "{dias, plural, one {venceu há # dia} other {venceu há # dias}}",
  "animal.carteira.ate": "até {data}",
  // "Não sabemos" ≠ "não existe" ≠ "irregular" (secao 05). O texto nao pode dizer
  // "nunca tomou": o registro so sabe que ninguem lancou nada aqui.
  "animal.carteira.semRegistro": "sem registro",
  "animal.carteira.semProximaDose": "sem próxima dose prevista",
  "animal.carteira.condicoes": "Condições e alergias",
  "animal.carteira.condicoes.vazio": "Nada registrado.",
  "animal.carteira.desde": "· desde {ano}",
  "animal.carteira.peso": "Peso",
  "animal.carteira.peso.vazio": "Nenhuma pesagem registrada.",
  "animal.carteira.peso.resumo": "{peso} kg em {data} · {total, plural, one {# pesagem} other {# pesagens}}",
  "animal.carteira.anexos": "Anexos",
  "animal.carteira.anexos.vazio": "Nenhum documento anexado.",

  // A linha do tempo: como ele chegou aqui.
  "animal.linha.titulo": "Linha do tempo",
  "animal.linha.carregando": "Carregando a linha do tempo…",
  "animal.linha.recorte.tudo": "Tudo",
  "animal.linha.recorte.vacinas": "Vacinas",
  "animal.linha.recorte.atendimentos": "Atendimentos",
  "animal.linha.recorte.creche": "Creche",
  "animal.linha.faixa": "{de} — {ate}",
  "animal.linha.vazio.tudo": "A linha do tempo deste animal ainda está vazia.",
  "animal.linha.vazio.vacinas": "Nenhuma vacina ou antiparasitário registrado.",
  "animal.linha.vazio.atendimentos": "Nenhum atendimento registrado.",
  "animal.linha.vazio.creche": "Nenhuma observação de organização registrada.",
  "animal.linha.pesagem": "Pesagem: {peso} kg",
  "animal.linha.pesoAnterior": "Pesagem anterior: {peso} kg",
  "animal.linha.autoria": "Registrado por {quem} · lançado em {quando}",
  "animal.linha.autoriaComOrg": "Registrado por {quem}, pela {organizacao} · lançado em {quando}",
  "animal.linha.lancado": "Lançado em {quando}",
  "animal.linha.corrigido": "{vezes, plural, one {# correção} other {# correções}}",
  "animal.linha.discordar": "Discordo deste registro",
  "animal.credencial.INFORMADO": "informado",
  "animal.credencial.VERIFICADO": "verificado",
  "animal.credencial.SUSPENSO": "suspenso",

  // ------------------------------------------------------------ a moldura do produto
  //
  // Cabeçalho e rodapé de toda tela autenticada. O diagnóstico está na primeira frase do
  // desenho: "o que eu vinha desenhando era um cabeçalho por tela — parecido, nunca igual. É
  // isso que dá a sensação de falta na implementação."
  "moldura.agindoComo": "Agindo como",
  "moldura.agindoComo.voceMesmo": "· você mesmo",
  // Em contexto de organização o seletor diz "pela". "É a diferença visual entre registrar como
  // pessoa e registrar em nome de alguém — e ela nunca é sutil."
  "moldura.agindoComo.pela": "pela {organizacao}",
  // Ambíguo não é erro: com mais de um vínculo e sem escolha declarada, o servidor se recusa a
  // decidir em silêncio — porque um registro assinado por uma organização que a pessoa não
  // pretendia não se corrige depois.
  "moldura.agindoComo.escolha": "· escolha por quem",
  "moldura.contexto.pessoa": "Você mesmo",
  "moldura.voce": "Sua conta",
  "moldura.sair": "Sair",

  // No máximo quatro, e só entra destino que existe: um item de menu que leva a lugar nenhum é
  // pior que a ausência dele.
  "moldura.destino.hoje": "Hoje",
  "moldura.destino.pacientes": "Pacientes",
  "moldura.destino.creche": "Creche",
  "moldura.destino.equipe": "Equipe",

  // As duas afordâncias sem backend. A seção 06 da identidade manda: "o desabilitado nunca
  // aparece mudo — ao lado dele, sempre a frase que diz por que".
  "moldura.busca": "Buscar animal",
  "moldura.busca.indisponivel":
    "A busca ainda não existe: não há rota de busca no servidor, e o RGA nem é um campo do cadastro.",
  // O marcador acende desde a V48. Ele ficou visível e desabilitado até aqui, com a frase "não há
  // canal de aviso no produto ainda" — que era verdade, e o dia em que deixou de ser é este.
  "moldura.avisos": "Avisos",
  "moldura.avisos.ha": "Avisos — há coisa por ler",

  // A faixa. "Faixa é aviso de sessão, nunca de animal" — pendência de saúde vive no feed, e se
  // subir para o cabeçalho o produto vira cobrador.
  "moldura.faixa.email": "Confirme seu e-mail para receber avisos de vacina.",
  "moldura.faixa.email.reenviar": "Reenviar",
  "moldura.faixa.email.enviando": "Enviando…",
  "moldura.faixa.email.enviado": "Enviado — veja sua caixa de entrada",

  // O rodapé. A tese é a do produto inteiro, e não uma frase de marketing.
  "moldura.rodape.tese":
    "O registro é do animal. Ele atravessa tutores, clínicas e abrigos, e não recomeça quando a responsabilidade muda de mão.",
  "moldura.rodape.produto": "O produto",
  "moldura.rodape.produto.tutores": "Para tutores",
  "moldura.rodape.produto.clinicas": "Para clínicas",
  "moldura.rodape.produto.creches": "Para creches e petshops",
  "moldura.rodape.produto.abrigos": "Para abrigos",
  // "Seus dados" não vai para dentro de configurações: num produto que pede acesso à saúde de um
  // ser vivo, esses links são a garantia — e garantia escondida não sossega ninguém.
  "moldura.rodape.dados": "Seus dados",
  "moldura.rodape.dados.privacidade": "Política de privacidade",
  "moldura.rodape.dados.termos": "Termos de uso",
  "moldura.rodape.dados.quemLe": "Quem lê o registro dos meus animais",
  "moldura.rodape.dados.exportar": "Levar meus dados embora",
  "moldura.rodape.ajuda": "Ajuda",
  "moldura.rodape.ajuda.central": "Central de ajuda",
  "moldura.rodape.ajuda.contato": "Falar com a gente",
  "moldura.rodape.ajuda.acessibilidade": "Acessibilidade",
  "moldura.rodape.ajuda.status": "Status do sistema",
  // A linha de fé. Não é rodapé jurídico enfiado no fim: é o limite do produto, dito onde
  // qualquer pessoa alcança.
  "moldura.rodape.fe.titulo": "O Petfy não substitui atendimento veterinário.",
  "moldura.rodape.fe.texto":
    "Guardamos e organizamos o que profissionais e cuidadores registram, e não emitimos diagnóstico. Registros profissionais informados por quem se cadastra não são verificados junto aos conselhos. Em emergência, procure um veterinário.",
  "moldura.rodape.assinatura": "© 2026 Petfy · São Paulo, Brasil",
  "moldura.rodape.lgpd": "Encarregado de dados (LGPD)",

  // ------------------------------------------- o paciente pelos olhos da clínica (Tela 30)
  //
  // É o mesmo animal da Tela 02, e não é a mesma tela: aquela é a vida do animal como o tutor
  // a lê, esta é o que a clínica precisa ter na frente ANTES de prescrever.
  "paciente.oQue": "este paciente",
  "paciente.oQue.vacinas": "a vacinação",
  "paciente.oQue.historico": "o histórico clínico",
  "paciente.peso": "{peso} kg",
  "paciente.microchip": "Microchip {numero}",
  "paciente.tutor": "Tutor: {nome}",
  "paciente.registrar": "Registrar atendimento",
  "paciente.imprimir": "Imprimir carteirinha",
  "paciente.imprimir.indisponivel":
    "Não existe rota para gerar a carteirinha ainda. O botão fica visível para não sumir da tela quando passar a funcionar.",

  // A caixa mais importante, e por isso a primeira. Prescrever sem ver alergia e o que já está
  // em uso é o erro que este produto existe para tornar difícil.
  "paciente.antesDePrescrever": "Antes de prescrever",
  "paciente.antesDePrescrever.nada":
    "Nenhuma alergia, condição ativa ou medicação em curso registrada. Ausência de registro não é o mesmo que ausência de condição — pergunte ao tutor.",
  "paciente.emUso": "Em uso: {o_que}{ate, select, undefined {} other { · até {ate}}}",
  "paciente.vacinacao": "Vacinação",
  "paciente.peso.titulo":
    "{quantos, plural, =0 {Peso · sem registro} one {Peso · # registro} other {Peso · # registros}}",
  "paciente.peso.vazio": "Nenhuma pesagem registrada.",
  "paciente.peso.variacao": "{de} kg → {para} kg",

  // "Existe e não foi compartilhado com a clínica." A saída é humana e não técnica de propósito:
  // quem pode liberar é o tutor, e não um botão daqui.
  "paciente.foraDoAlcance": "Fora do seu alcance",
  "paciente.foraDoAlcance.texto":
    "{quantos, plural, one {# registro deste animal existe} other {# registros deste animal existem}} e não {quantos, plural, one {foi} other {foram}} compartilhado{quantos, plural, one {} other {s}} com esta organização. Se for necessário para o caso, peça a {quem}.",

  "paciente.historico": "Histórico clínico",
  "paciente.recorte.tudo": "Tudo",
  "paciente.recorte.desta-clinica": "Só desta clínica",
  "paciente.recorte.meus": "Só o que eu registrei",
  // Vazio com recorte marcado não é o mesmo que vazio: "esta clínica nunca registrou nada deste
  // animal" é um fato, e não a ausência de histórico.
  "paciente.historico.vazio.tudo": "Nada foi registrado para este animal ainda.",
  "paciente.historico.vazio.desta-clinica":
    "Esta organização nunca registrou nada deste animal. Isso não quer dizer que ele não tenha histórico — tire o filtro para ver o resto.",
  "paciente.historico.vazio.meus":
    "Você nunca registrou nada deste animal. Tire o filtro para ver o que a equipe e outras clínicas registraram.",

  // O rótulo que impede um relato de creche de parecer um diagnóstico.
  "paciente.naoClinico": "Observação, não clínico",
  "paciente.corrigido": "{quantas, plural, one {# correção} other {# correções}}",
  "paciente.pela": "pela {organizacao}",
  "paciente.credencial": "{registro}, {estado}",
  "paciente.semCredencial": "sem registro profissional",
  // No topo e não numa caixa lateral: quem prescreve sem saber que existe outro cadastro
  // prescreve contra metade do histórico.
  "paciente.duplicado.titulo": "Este microchip está em outro cadastro",
  "paciente.duplicado.pedido": "Pedem para unir este cadastro a outro",
  "paciente.duplicado.texto":
    "Provavelmente é o mesmo animal, visto por duas pessoas diferentes — e metade do histórico dele pode estar do outro lado. Abra para comparar.",

  // ------------------------------------------ registrar atendimento e prescrever (Tela 31)
  //
  // O próprio desenho a chama de "o loop central". É a única tela que grava três coisas de uma
  // vez: o prontuário, a pesagem do dia e cada medicamento.
  "atendimento.titulo": "Atendimento do {nome}",
  // A assinatura aparece ANTES do primeiro campo. É o que separa um registro que um veterinário
  // aceita de um caderno digital, e quem digita precisa saber em nome de quem antes de escrever.
  "atendimento.assinatura": "Será assinado por {quem}, {onde}. A autoria não muda depois.",
  "atendimento.porMim": "por você mesmo",
  "atendimento.quando": "Quando aconteceu",
  "atendimento.quando.nota":
    "Pode ser retroativo. A data de lançamento fica registrada à parte — o histórico mostra as duas.",
  "atendimento.peso": "Peso hoje",
  "atendimento.constatacao": "O que você constatou",
  "atendimento.diagnostico": "Diagnóstico",
  "atendimento.diagnostico.nota":
    "Campo próprio, e não enterrado na descrição: é por ele que o histórico destaca o diagnóstico anos depois.",

  // "Referenciar não transforma observação em diagnóstico. Ela continua sendo o que é, assinada
  // por quem escreveu."
  "atendimento.evidencias": "Evidências que outros registraram",
  "atendimento.evidencias.nota":
    "Referenciar não transforma observação em diagnóstico. Ela continua sendo o que é, assinada por quem escreveu — vai para o prontuário entre aspas, com o nome de quem disse.",
  "atendimento.referencia": "“{texto}” — {quem}, {onde}",
  "atendimento.rotulo": "Consulta",

  "atendimento.prescricao": "Prescrição",
  "atendimento.prescricao.nota": "Vira pendência na casa do tutor, uma por dia.",
  "atendimento.medicamento": "Medicamento e dose",
  "atendimento.intervalo": "Intervalo, em horas",
  "atendimento.duracao": "Por quantos dias",
  "atendimento.comoDar": "Como dar",
  "atendimento.outroMedicamento": "Adicionar outro medicamento",
  // O aviso NÃO recusa: sobrepor dois medicamentos às vezes é exatamente o que se quer. O número
  // de dias é a informação — um dia é ruído, duas semanas é outra conversa.
  "atendimento.sobreposicao":
    "O {nome} está em uso de {o_que} até {ate}. Os dois vão se sobrepor por {dias, plural, one {# dia} other {# dias}} — confirme que é intencional.",

  "atendimento.oQue": "o atendimento",
  "atendimento.gravar": "Registrar atendimento",
  "atendimento.gravando": "Registrando…",
  "atendimento.soCorrecao": "Depois disso, só correção — com motivo, ao lado do original.",
  "atendimento.voltar": "Voltar ao paciente",

  "atendimento.oQueRecebe": "O que o tutor vai receber",
  "atendimento.oQueRecebe.nada":
    "Nenhuma prescrição ainda. Um atendimento sem prescrição é válido — nem toda consulta termina em remédio.",
  "atendimento.pendencia": "Dia 1 de {total} · orientação de {quem}",
  "atendimento.somemSozinhas":
    "Uma pendência por dia, no feed do tutor e de quem mais cuidar do animal. Somem sozinhas quando o tratamento acaba.",
  // "Você prescreve o que o animal precisa. Quem dá cada dose é o tutor quem decide, porque só
  // ele sabe quem estará em casa."
  "atendimento.naoConcedeAcesso":
    "Prescrever não concede acesso a ninguém. Quem dá cada dose é o tutor quem decide — a creche só recebe a dose se já tiver acesso e se ele atribuir.",

  // ---------------------------------------------------------- Tela 40 · o valor cobrado
  //
  // "O campo é opcional, e um evento sem valor é NORMAL — nunca um erro, nunca um alerta."
  // Nenhuma destas frases cobra o preenchimento, e a de baixo diz o contrário com todas as
  // letras: se informar valor virar obrigação, a clínica para de registrar o atendimento.
  "valor.titulo": "Valor cobrado",
  "valor.opcional": " · opcional",
  "valor.soOTutorVe": "Só o tutor do {nome} vê",
  "valor.oQue": "O que foi cobrado",
  "valor.quanto": "Valor",
  "valor.jaFoiPago": "Já foi pago",
  "valor.outroItem": "Adicionar outro item",
  "valor.emBranco":
    "Deixe em branco se o valor não faz parte do que você quer registrar. O atendimento fica igual, e o custo do {nome} simplesmente não conta este evento.",

  // ------------------------------------------------- Tela 41 · o combinado com o tutor
  //
  // "O Petfy não cobra, não emite boleto e não processa pagamento. Ele guarda o que foi
  // combinado" — e nenhuma frase daqui pode sugerir o contrário. Gravar a mensalidade não
  // lança mensalidade nenhuma; o único valor que entra sozinho é a diária, e ela nasce da
  // entrada do animal, e não deste formulário.
  "combinado.oQue": "o combinado",
  "combinado.semMatricula":
    "Não encontramos esta matrícula nesta turma. Ela pode ter sido encerrada — volte ao dia da creche para ver as que estão valendo.",
  "combinado.voltar": "Voltar ao dia da creche",
  "combinado.titulo": "Matrícula do {nome} · turma {turma}",
  "combinado.assinatura": "{quem}, pela {onde}",
  "combinado.caixa": "Combinado com o tutor",
  "combinado.opcional": " · opcional",
  "combinado.mensalidade": "Mensalidade",
  "combinado.vencimento": "Vence todo dia",
  "combinado.diaria": "Diária avulsa",
  "combinado.diaria.nota": "Usada quando o {nome} vem fora dos dias combinados.",
  "combinado.dias": "Dias combinados",
  // As iniciais, como se lê um calendário. Segunda a domingo, na ordem da semana.
  "combinado.dia.MONDAY": "Seg",
  "combinado.dia.TUESDAY": "Ter",
  "combinado.dia.WEDNESDAY": "Qua",
  "combinado.dia.THURSDAY": "Qui",
  "combinado.dia.FRIDAY": "Sex",
  "combinado.dia.SATURDAY": "Sáb",
  "combinado.dia.SUNDAY": "Dom",
  // Não é um erro: não declarar dia é comum. O aviso existe só quando há diária combinada
  // sem dia nenhum — o único caso em que o combinado não faz o que parece fazer.
  "combinado.semDias":
    "Sem dias combinados, a diária nunca entra sozinha: não há como saber que o {nome} veio fora do combinado. Marque os dias em que ele é esperado.",
  "combinado.naoCobra":
    "O Petfy não cobra, não emite boleto e não processa pagamento. Ele guarda o que foi combinado, para que o tutor veja o custo real do {nome} e ninguém precise perguntar por telefone.",
  "combinado.gravar": "Gravar o combinado",
  "combinado.gravando": "Gravando…",
  "combinado.gravado": "Combinado gravado.",

  "combinado.oQueOTutorVe": "O que o tutor passa a ver, sem perguntar",
  "combinado.linha.mensalidade": "{onde} · mensalidade",
  // Duas mensagens e nao um `select` sobre string vazia: chave vazia nao é ICU válido, e o
  // erro só apareceria em tela, no dia em que alguém combinasse mensalidade sem vencimento.
  "combinado.linha.mensalidade.detalhe":
    "{dias, plural, =0 {Nenhum dia combinado} one {# dia por semana} other {# dias por semana}}",
  "combinado.linha.mensalidade.vence": ", vence dia {vence}",
  "combinado.porMes": "{valor}/mês",
  "combinado.linha.diaria": "Diária avulsa",
  "combinado.linha.diaria.detalhe": "Registrada pela creche no dia, sem ninguém digitar",
  "combinado.sozinha":
    "A diária avulsa entra sozinha: quando a creche marca a entrada do {nome} num dia fora da combinação, o evento de entrada carrega o valor. Ninguém digita nada.",
  "combinado.contestavel":
    "Cada valor tem um evento por trás, com autor e data — e por isso pode ser contestado como qualquer outro registro.",
  // A creche não lê custo: ler exige custódia, e nenhum escopo de acesso substitui.
  "combinado.naoLemos":
    "Esta é a sua parte do combinado, e não a conta do {nome}. O que outras organizações cobram, e o que o tutor lança por fora, só quem responde pelo animal lê.",

  // O mesmo combinado, lido por quem responde pelo animal. Vem nulo para quem só alcança o
  // animal por concessão, e aí a caixa nem existe.
  "matricula.combinado": "O que foi combinado",
  "matricula.combinado.mensalidade": "Mensalidade",
  "matricula.combinado.detalhe":
    "{dias, plural, =0 {Sem dias combinados} one {# dia por semana} other {# dias por semana}}",
  "matricula.combinado.vence": ", vence dia {vence}",
  "matricula.combinado.porMes": "{valor}/mês",
  "matricula.combinado.diaria": "Diária avulsa",
  "matricula.combinado.diaria.detalhe":
    "Entra sozinha quando a creche marca a entrada num dia fora do combinado",
  "matricula.combinado.naoCobra":
    "O Petfy não cobra e não processa pagamento — ele guarda o que foi combinado, para você não precisar perguntar por telefone.",

  // ------------------------------------------------ Tela 42 · o que você compra por fora
  //
  // "Este é o único formulário de dinheiro em todo o Petfy, e ele cabe em três toques.
  // Quanto mais campos, menos gente lança, e menos verdadeiro fica o custo." Nenhuma frase
  // aqui cobra o lançamento: a última diz o contrário, e é dela que a tela depende.
  "compra.titulo": "Lançar uma compra",
  "compra.oQue": "O que foi",
  "compra.oQue.racao": "Ração",
  "compra.oQue.remedio": "Remédio",
  "compra.oQue.outro": "Outro",
  "compra.valor": "Valor",

  // QUANDO FOI: o campo não existia, e a tela sempre lançava agora. Quem lança a nota do mercado à
  // noite registrava hoje; quem lança a de sábado na segunda registrava errado — e a previsão, que
  // lê "quando foi a última vez", herdava o erro.
  "compra.quando": "Quando foi",

  // DE QUANTO EM QUANTO TEMPO, e não uma caixinha.
  //
  // "Dura cerca de um mês" gravava `recurrence = MENSAL`, e com isso "a ração dura um mês" e "a
  // mensalidade chega todo mês" viraram o mesmo fato. Enquanto só existia um mês, os dois
  // coincidiam por acidente. A ração que dura DOIS meses não tinha resposta: marcar dizia "todo
  // mês", que é o dobro do que o tutor gasta — e a previsão planejava para cima.
  "compra.dura": "De quanto em quanto tempo isto se repete",
  "compra.dura.naoSeRepete": "Não se repete — foi uma vez só",
  "compra.dura.meses": "{meses, plural, one {Dura cerca de um mês} other {Dura cerca de # meses}}",
  "compra.dura.nota":
    "{vezes, plural, one {A previsão do ano conta uma compra dessas} other {A previsão do ano conta # compras dessas}}.",
  "compra.dura.nota.avulsa": "Entra no que já foi gasto, e não na previsão do ano.",

  // O REMÉDIO QUE O ANIMAL ESTÁ TOMANDO.
  //
  // Até a V49 remédio virava custo e nada mais: quem cuidasse do animal naquela semana não tinha
  // como saber que havia um comprimido às 8h. É opcional de propósito — um antipulgas de dose
  // única não é tratamento a acompanhar, e obrigar transformaria os três toques num formulário.
  "compra.tratamento.declarar": "O {nome} está tomando isto",
  "compra.tratamento.nota":
    "Marque só se for um tratamento a acompanhar. Ele entra na vida do animal com prazo, e quem cuida dele vê o que fazer e quando.",
  "compra.tratamento.comoDar": "O que fazer",
  "compra.tratamento.exemplo": "Meio comprimido de manhã, com comida",
  // O nome do remédio e o "como dar" no mesmo campo: quem lê a pendência precisa ver "junto com a
  // comida" ao lado do nome, e separá-los faria a instrução chegar pela metade.
  "compra.tratamento.comoDar.nota":
    "Escreva como quem vai dar precisa ler — o nome do remédio e o jeito de dar, na mesma frase.",
  "compra.tratamento.aCada": "De quanto em quanto tempo",
  "compra.tratamento.aCada.dias": "{dias, plural, one {Todo dia} other {A cada # dias}}",
  "compra.tratamento.por": "Por quanto tempo",
  "compra.tratamento.por.dias": "{dias, plural, one {Um dia} other {# dias}}",
  "compra.tratamento.oQueE": "o tratamento",

  "compra.lancar": "Lançar",
  "compra.lancando": "Lançando…",
  "compra.lancado": "{valor} entrou no custo do {nome}.",
  "compra.oQueE": "a compra",
  "compra.tresToques":
    "Três toques. Se você não lançar, o custo do {nome} fica incompleto — e tudo bem, ele continua servindo.",
  "compra.voltar": "Voltar para o {nome}",

  "compra.unico.titulo": "O único lançamento manual do produto",
  "compra.unico.p1":
    "Tudo que acontece numa organização entra sozinho, porque alguém já estava registrando o evento. Ração e coisas de mercado não têm organização por trás — só existem se você lançar.",
  "compra.unico.p2":
    "Por isso este é o único formulário de dinheiro em todo o Petfy, e ele cabe em três toques. Quanto mais campos, menos gente lança, e menos verdadeiro fica o custo.",

  "compra.mensal.titulo": "Durar não é repetir",
  "compra.mensal.texto":
    "Este é o campo que transforma uma compra avulsa em custo previsível — e é ele que permite a um abrigo dizer ao adotante quanto a ração custa por mês. Uma ração de R$ 190 que dura dois meses custa R$ 95 por mês, e volta daqui a dois meses. A caixinha que existia aqui antes só sabia dizer “todo mês”, e planejava o dobro para quem compra a cada dois.",

  "compra.naoExiste.titulo": "O que não existe aqui",
  "compra.naoExiste.banco":
    "Nenhuma integração com banco ou cartão. O Petfy não olha sua conta.",
  "compra.naoExiste.orcamento":
    "Nenhum orçamento mensal, nenhuma meta, nenhum aviso de que você passou do limite.",
  "compra.naoExiste.loja":
    "Nenhuma loja, nenhum link de compra, nenhuma sugestão de ração mais barata.",
  // ------------------------------------------------------- Tela 37 · o custo do animal
  //
  // "Montado a partir do que já está registrado. Você não digitou nada disto duas vezes."
  // Nenhuma frase daqui compara com ninguém, e isso é decisão: "nenhuma tela aqui diz que
  // você gasta mais ou menos que outros tutores, nem sugere trocar de clínica por preço.
  // Quem cuida de um animal doente já tem o suficiente na cabeça."
  "custo.titulo": "Quanto o {nome} custou",
  "custo.apoio":
    "Montado a partir do que já está registrado. Você não digitou nada disto duas vezes — cada valor veio junto com o evento que o gerou.",
  "custo.oQue": "o custo",
  "custo.oQue.lista": "os valores",
  "custo.voltar": "Voltar para o {nome}",
  "custo.verPrevisao": "O que vem pela frente para o {nome}",

  "custo.janela": "O período",
  "custo.janela.dozeMeses": "Últimos 12 meses",
  "custo.janela.desde": "Desde {ano}",
  "custo.janela.sempre": "Desde sempre",

  "custo.indicador.dozeMeses": "Nos últimos 12 meses",
  "custo.indicador.noPeriodo": "No período todo",
  "custo.indicador.porMes": "Por mês, em média",
  "custo.indicador.desde": "Desde {ano}",
  "custo.indicador.sempre": "Desde sempre",

  "custo.ondeFoi": "Onde foi",
  // Não é erro nem pendência: um animal sem valor registrado é um animal cuidado por gente
  // que não informou preço, e o produto não cobra isso de ninguém.
  "custo.ondeFoi.vazio":
    "Nenhum valor foi informado para o {nome} neste período. Não é uma pendência: a clínica e a creche informam se quiserem, e você lança o que compra por fora quando quiser.",
  // A frase depende da ordem, e só aparece quando saúde é de fato a menor fatia — escrita
  // fixa, ela mentiria no mês em que a saúde fosse o maior gasto, que é justamente o mês em
  // que o animal está doente.
  "custo.saudeEMenor":
    "Saúde é o menor pedaço do gasto do {nome} — e é o único que cresce sozinho quando é adiado. {quantos, plural, one {O outro é escolha sua} other {Os outros {quantos} são escolha sua}}.",

  "custo.categoria.SAUDE": "Saúde · consultas, vacinas e remédios",
  "custo.categoria.ALIMENTACAO": "Alimentação",
  "custo.categoria.CRECHE": "Creche",
  "custo.categoria.HIGIENE": "Banho e tosa",
  "custo.categoria.OUTRO": "Outro",
  // A versão curta, para caber na linha de "quem pagou o quê"
  "custo.categoria.curta.SAUDE": "saúde",
  "custo.categoria.curta.ALIMENTACAO": "alimentação",
  "custo.categoria.curta.CRECHE": "creche",
  "custo.categoria.curta.HIGIENE": "banho e tosa",
  "custo.categoria.curta.OUTRO": "outros",

  "custo.cadaValor": "Cada valor veio de um evento",
  "custo.cadaValor.vazio": "Nenhum valor registrado neste período.",
  "custo.origem.voce": "Lançado por você",

  "custo.quemPagou": "Quem pagou o quê",
  // O produto não sabe quem pagou uma consulta: quem registrou foi a veterinária, e ela
  // informou o valor, não o pagador. A linha existe para a conta fechar com o total.
  "custo.quemPagou.semNome": "Não informado",
  "custo.quemPagou.semNome.detalhe": "O que a clínica e a creche registraram",
  "custo.quemPagou.soMostra":
    "Dois co-tutores dividem o cuidado e quase sempre perdem a conta de quem pagou o quê. O Petfy só mostra o que alguém informou — não cobra ninguém, não faz acerto, e não vai pedir que você preencha o resto depois.",

  // ------------------------------------------ Tela 39 · a conversa honesta antes da adoção
  //
  // "O abrigo tem um problema que custa vidas: o animal volta. Quase sempre porque alguém
  // adotou sem saber que um cão de 11 anos com artrose custa R$ 270 por mês, todo mês, até o
  // fim." Nenhuma frase daqui desanima nem desqualifica quem vai adotar — a última linha do
  // cartão da direita diz por que: "dizer isso antes é o oposto de dificultar a adoção".
  "adocaoCusto.oQue": "o que este animal custou",
  "adocaoCusto.anos": "{anos, plural, one {# ano} other {# anos}}",
  "adocaoCusto.abertura":
    "Antes de você decidir, o abrigo quer que você saiba quanto o {nome} custou nos últimos doze meses. Não para desanimar — para que ele não volte para cá.",
  "adocaoCusto.custou": "Custou nos últimos 12 meses",
  "adocaoCusto.porMes": "{valor} por mês",
  // NÃO se chama "previsto": a previsão cobre só o que tem data ou se repete, e chamar isso de
  // previsto na frente de quem está decidindo adotar subestimaria o custo do animal.
  "adocaoCusto.jaMarcado": "Já marcado para os próximos 12",
  "adocaoCusto.jaMarcado.falta":
    "Só o que tem data ou se repete. A consulta que o {nome} vier a precisar não está aqui — este número é um piso, e não um teto.",
  "adocaoCusto.todoMes": "O que o {nome} precisa todo mês",
  "adocaoCusto.todoMes.vazio":
    "Ainda não há nada registrado como mensal para o {nome}. Ração e remédio de uso contínuo aparecem aqui quando alguém os lançar marcando “dura cerca de um mês”.",
  "adocaoCusto.origem.CRECHE_MENSALIDADE": "Mensalidade combinada com a creche",
  "adocaoCusto.origem.COMPRA_MENSAL": "Lançado como compra que dura cerca de um mês",
  "adocaoCusto.origem.DOSE_DE_VACINA": "Dose com data de reforço",
  "adocaoCusto.origem.ANTIPARASITARIO": "Antiparasitário no intervalo de reforço",
  "adocaoCusto.realmentePagou":
    "Estes valores são o que o abrigo realmente pagou, evento por evento, e não uma estimativa de mercado. Onde você mora e onde você trata podem mudar tudo.",
  "adocaoCusto.voltar": "Voltar para a adoção do {nome}",

  "adocaoCusto.porQue.titulo": "Por que esta é a tela mais valiosa do conjunto",
  "adocaoCusto.porQue.p1":
    "O abrigo tem um problema que custa vidas: o animal volta. Quase sempre porque alguém adotou sem saber que um cão idoso com artrose custa a mesma quantia por mês, todo mês, até o fim.",
  "adocaoCusto.porQue.p2":
    "Nenhum questionário de adoção resolve isso, porque o abrigo também não tinha o número. Agora tem — está em cada evento registrado do {nome}.",
  "adocaoCusto.porQue.p3":
    "Dizer isso antes é o oposto de dificultar a adoção. É o que faz a adoção durar, e é a mesma tese do produto: o que interessa é o animal, não a transação.",

  "adocaoCusto.foraDeProposito.titulo": "O que fica de fora, de propósito",
  "adocaoCusto.foraDeProposito.triagem":
    "Nenhuma triagem por renda. O abrigo mostra o custo; quem decide se dá conta é o adotante.",
  "adocaoCusto.foraDeProposito.orcamento":
    "Nenhum “animais dentro do seu orçamento”. Isso transformaria um ser vivo em item de catálogo por faixa de preço.",
  "adocaoCusto.foraDeProposito.plano":
    "Nenhuma oferta de plano de saúde animal nesta tela. O produto não vende no momento em que alguém está decidindo adotar.",

  // -------------------------------------- Tela 38 · o que vem pela frente, e o custo de adiar
  //
  // "Isto não é previsão de gasto: é o que já está marcado no registro dele." Nenhuma frase
  // aqui projeta, estima ou aconselha clinicamente — o limite está escrito no cartão do lado.
  "previsao.titulo": "Os próximos 12 meses do {nome}",
  "previsao.apoio":
    "Isto não é previsão de gasto: é o que já está marcado no registro dele. Vacinas com data de reforço, a mensalidade que se repete e o que você disse que dura um mês.",
  "previsao.oQue": "o que vem pela frente",
  "previsao.vazia":
    "Não há nada marcado para os próximos 12 meses do {nome} — nenhuma dose com data de reforço, nenhuma mensalidade e nenhuma compra que se repete. Não é uma pendência.",
  "previsao.verOQueCustou": "Ver quanto o {nome} já custou",

  "previsao.quando.esteMes": "este mês",
  "previsao.quando.todoMes": "todo mês",
  "previsao.linha.vencida": "{nome} · vencida há {dias, plural, one {# dia} other {# dias}}",
  "previsao.linha.proxima": "{nome} · próxima dose em {data}",
  "previsao.linha.mensalidade": "{onde} · mensalidade",
  "previsao.marcador.vencida": "Vencida",
  "previsao.marcador.chegando": "Chegando",
  "previsao.marcador.distante": "Mais adiante",
  "previsao.marcador.todoMes": "Todo mês",

  "previsao.total": "Total previsto em 12 meses",
  // Um total que fingisse cobrir tudo seria um número autoritário e menor que a verdade.
  "previsao.total.parcial":
    "{quantas, plural, one {Uma linha ainda não tem valor informado e não entrou nesta conta} other {{quantas} linhas ainda não têm valor informado e não entraram nesta conta}}.",

  "previsao.leitura": "Uma leitura do Petfy",
  "previsao.leitura.comPreco":
    "A {dose} do {nome} custa {valor} hoje. Enquanto ela estiver vencida, a {creche} não pode recebê-lo.",
  "previsao.leitura.semPreco":
    "A {dose} do {nome} está vencida, e enquanto estiver a {creche} não pode recebê-lo.",
  // A aritmética é sobre o que este tutor já pode ver: o valor de um dia combinado do próprio
  // animal. O número do desenho — o que aconteceu com outro animal da turma — é custo de outro
  // animal, e ler custo exige custódia dele.
  "previsao.leitura.diaPerdido":
    "Cada dia combinado que ele perder custa {valor} da mensalidade que você já paga.",
  "previsao.leitura.oQueGerou": "O que gerou esta leitura",
  "previsao.leitura.fonte.dose": "{dose} vencida em {data}",
  "previsao.leitura.fonte.exigencia": "exigência de vacinação da {creche}",
  "previsao.leitura.fonte.combinado": "mensalidade de {valor} para {dias} dias por semana",
  "previsao.leitura.registrar": "Registrar a dose",
  // No lugar do "Dispensar" do desenho: guardar a dispensa pediria uma tabela que não existe, e
  // um botão que esquece no recarregamento é pior do que nenhum botão. O gesto existe no feed.
  "previsao.leitura.semDispensar":
    "Para parar de ser cobrado por esta dose, silencie a pendência no seu feed — aqui a leitura só reflete o que está registrado.",

  "previsao.diferente.titulo": "Por que isto é diferente de um app de gastos",
  "previsao.diferente.texto":
    "Um aplicativo de finanças pede que você digite tudo, e por isso ninguém mantém. Aqui o valor entra de carona no evento que a clínica já ia registrar de qualquer jeito. E a previsão não é estatística: é a data de reforço que já está escrita na carteirinha do animal.",
  "previsao.limite.titulo": "O limite da leitura de custo",
  "previsao.limite.texto":
    "Ela pode dizer que adiar a vacina custa dias de creche perdidos, porque isso é aritmética sobre fatos registrados. Nunca vai dizer que tratar a displasia agora sai mais barato que operar depois — isso é prognóstico clínico, e o Petfy não faz prognóstico.",
  "previsao.paraQuemRegistra.titulo": "Para a clínica e a creche",
  "previsao.paraQuemRegistra.texto":
    "Informar o valor é opcional e leva um campo. Em troca, elas param de ser cobradas por telefone sobre o que já foi pago, e o tutor chega sabendo o que vem pela frente. Abrigo não informa valor nenhum — o produto é grátis para eles.",

  // O valor da dose, dentro do registro que já estava sendo feito. Opcional como em toda
  // tela que o oferece: "um evento sem valor é normal — nunca um erro, nunca um alerta".
  "dose.valor": "Valor · opcional",
  "dose.valor.apoio": "Informe e o Petfy saberá quanto o próximo reforço deve custar.",

  "custo.semJulgamento.titulo": "Sem comparação, sem julgamento",
  "custo.semJulgamento.texto":
    "Nenhuma tela aqui diz que você gasta mais ou menos que outros tutores, nem sugere trocar de clínica por preço. Quem cuida de um animal doente já tem o suficiente na cabeça.",
  "custo.deOndeVem.titulo": "De onde vêm os valores",
  "custo.deOndeVem.texto":
    "A clínica e a creche informam o valor junto com o que registram — é a mesma tela de sempre, com um campo a mais. O que você compra por fora, como ração, você lança quando quiser. Se ninguém informou, o evento aparece sem valor, e isso não é erro.",

  "compra.naoExiste.fecho":
    "O custo existe para responder “o que vem pela frente” e “quanto este animal realmente custa”. Tudo além disso seria outro produto morando dentro deste.",

  // ------------------------------- o mesmo animal, cadastrado duas vezes (Tela 32)
  //
  // Uma tela, dois lados: quem percebe a duplicata é quase sempre a clínica, e quem decide é quem
  // responde pelo animal. A comparação é a mesma para os dois — duas telas com a mesma tabela
  // divergiriam, e a que o tutor lê é a que precisa estar certa.
  "duplicado.oQue": "os cadastros parecidos",
  "duplicado.oQue.pedido": "o pedido",
  "duplicado.oQue.decisao": "a decisão",
  "duplicado.titulo": "Este microchip já está em outro cadastro",
  "duplicado.apoio":
    "Você está cadastrando um animal com o microchip {microchip}, e ele já existe no Petfy. Provavelmente é o mesmo animal, visto por duas pessoas diferentes. Unir duas linhas do tempo não apaga nem reescreve nada — mas é irreversível, e por isso quem decide é quem responde pelo animal.",
  "duplicado.varios":
    "{quantos, plural, other {# cadastros parecidos}} — cada um é uma decisão própria",
  "duplicado.lado.existe": "O cadastro que já existe",
  "duplicado.lado.novo": "O que você está cadastrando",
  "duplicado.responsavel": "Responsável",
  "duplicado.semResponsavel": "nenhum ainda",
  "duplicado.eventos": "Eventos",
  "duplicado.eventos.desde": "{quantos}, desde {desde}",
  "duplicado.eventos.semData": "{quantos}",
  "duplicado.quemRegistrou": "Quem registrou",
  "duplicado.quemRegistrou.valor":
    "{pessoas, plural, one {# pessoa} other {# pessoas}}, {organizacoes, plural, =0 {nenhuma organização} one {# organização} other {# organizações}}",

  // As quatro promessas, e nenhuma delas é decorativa.
  "duplicado.oQueAcontece": "O que acontece se forem unidos",
  "duplicado.acontece.ordem":
    "As duas linhas do tempo viram uma só, ordenada por quando cada coisa aconteceu. Nenhum evento é descartado.",
  "duplicado.acontece.assinatura":
    "Cada evento continua assinado por quem o registrou, com a data de lançamento original.",
  "duplicado.acontece.evento":
    "A união em si vira um evento na linha do tempo, com seu nome e o motivo — quem ler daqui a cinco anos vai entender por que existem dois nomes no histórico.",
  "duplicado.acontece.custodia":
    "Quem responde pelo animal continua sendo quem já respondia. Unir não transfere custódia, e o acesso da sua organização continua sendo o que foi concedido.",

  // Vem ANTES do botão, e não depois de um 403: quem clica e lê "proibido" aprende que o produto
  // é hostil; quem lê antes entende a regra.
  "duplicado.naoSozinha.titulo": "Você não pode unir sozinha",
  "duplicado.naoSozinha.texto":
    "Quem responde pelo animal é {quem}. Essa pessoa recebe o pedido, vê exatamente esta comparação e decide. É a mesma regra da transferência: ninguém mexe na vida registrada de um animal sem quem responde por ele.",

  "duplicado.motivo": "Por que você acha que é o mesmo animal",
  "duplicado.motivo.exemplo": "Mesmo microchip. Chegou com a Juliana, que disse ser co-tutora.",
  "duplicado.motivo.nota":
    "Sem motivo, quem decide recebe um pedido que só diz “una” e não tem como julgar. Ele vai junto para o evento da união.",
  "duplicado.pedir": "Pedir a união a {quem}",
  "duplicado.pedindo": "Enviando…",
  "duplicado.pedido.enviado":
    "Pedido enviado. {quem} decide, e nada muda até lá — os dois cadastros continuam como estão.",

  // Acontece na hora: não mexe em nada, só acende a marca.
  "duplicado.diferentes": "São animais diferentes",
  "duplicado.diferentes.nota":
    "“São animais diferentes” acontece na hora e não precisa de aprovação — não muda nada, só marca o microchip repetido nos dois cadastros. Provavelmente há um erro de digitação em algum lugar, e alguém vai precisar saber disso.",
  "duplicado.marcado":
    "Marcado nos dois cadastros. O produto não adivinha qual dos dois microchips está errado — quem tem o animal na frente é quem sabe.",

  // O lado de quem decide.
  "duplicado.pedido.titulo": "Pedem para unir este cadastro a outro",
  "duplicado.pedido.dequem": "{quem}, {onde}, acha que são o mesmo animal:",
  "duplicado.porSi": "por si mesma",
  "duplicado.unir": "Unir os dois cadastros",
  "duplicado.unindo": "Unindo…",
  "duplicado.irreversivel":
    "Unir é irreversível. Nada é apagado, mas as duas linhas do tempo não voltam a se separar.",

  "duplicado.nenhuma.titulo": "Nenhum cadastro parecido",
  "duplicado.nenhuma.texto":
    "Nenhum outro cadastro tem este microchip. Se o animal tem microchip e ele não está preenchido aqui, vale registrar — é a única pista que permite reconhecer o mesmo bicho visto por duas pessoas.",
  "duplicado.voltar": "Voltar ao paciente",

  // ============================================================ o fim da linha do tempo (Tela 33)
  //
  // "Sentimos muito" é a única concessão emocional da tela, e ela está no desenho. Depois disso
  // o produto fica em silêncio: não manda condolências, não sugere adotar outro, não pergunta a
  // causa da morte.
  "fim.titulo": "O {nome} morreu",
  "fim.apoio":
    "Sentimos muito. Quando você conseguir, preencha o que souber. Nada aqui tem pressa, e você pode fechar esta tela e voltar depois.",

  // O "o que é" dos estados de carga e de erro. Fala do animal, e não do formulário: quem está
  // esperando aqui quer ver a ficha dele, não "a tela de encerramento".
  "fim.oQueE": "a ficha do animal",

  "fim.quando": "Quando foi",
  "fim.onde": "Onde",
  "fim.onde.exemplo": "Em casa",
  "fim.despedida": "Se quiser dizer alguma coisa",
  "fim.despedida.nota": "Fica na linha do tempo dele, no lugar de quem escreveu.",
  "fim.opcional": "opcional",

  // O bloco "o que acontece". As três primeiras linhas são efeitos que o servidor produz de
  // fato; a quarta é o que muda na tela de quem preenche.
  "fim.acontece": "O que acontece",
  "fim.acontece.avisos": "Os avisos param hoje. Ninguém mais vai te cobrar uma vacina do {nome}.",
  "fim.acontece.organizacoes":
    "Quem cuida dele é avisado, sem que você precise ligar para cada um. A matrícula na creche é encerrada.",
  "fim.acontece.historico":
    "O que foi registrado continua aqui, inteiro, para você abrir quando quiser.",
  "fim.acontece.lista": "O {nome} sai da sua lista de animais e passa a ficar em “quem já esteve com você”.",

  // As duas caixas ao lado do formulário. A primeira responde a pergunta que a tela levanta
  // sozinha: por que a clínica que atendeu na última noite não fez isso.
  "fim.porQueSoVoce": "Por que só você encerra",
  "fim.porQueSoVoce.texto":
    "A veterinária que atendeu na última noite pode registrar o óbito como ato clínico dela — isso é o trabalho dela. Mas fechar a linha do tempo é de quem responde pelo animal, e não pode acontecer sem essa pessoa. Ninguém deve descobrir que perdeu o animal por uma notificação do sistema.",
  "fim.oQueNaoFazemos": "O que o Petfy não faz",
  "fim.oQueNaoFazemos.texto":
    "Não manda condolências automáticas. Não sugere adotar outro. Não pergunta a causa da morte — se você quiser contar, o campo aberto está aí. Depois disso, o Petfy fica em silêncio sobre ele.",

  "fim.registrar": "Registrar",
  "fim.registrando": "Registrando…",
  "fim.agoraNao": "Agora não",

  // A ficha fechada. "Sem tarja preta, sem laço, sem memorial."
  "fim.ficha.periodo": "{inicio} — {fim}",
  "fim.ficha.tempoComVoce": "{anos, plural, one {# ano} other {# anos}} com você",
  "fim.ficha.eventos": "Eventos registrados",
  "fim.ficha.quemCuidou": "Quem cuidou dele",
  "fim.ficha.quemCuidou.valor":
    "{pessoas, plural, one {# pessoa} other {# pessoas}}, {organizacoes, plural, one {# organização} other {# organizações}}",
  "fim.ficha.verVida": "Ver a vida do {nome}",
  "fim.ficha.encerradaEm": "Linha do tempo encerrada em {data}.",
  "fim.ficha.onde": "Onde: {onde}",

  // A lista que recebe o animal que saiu da outra.
  "anteriores.titulo": "Quem já esteve com você",
  "anteriores.vazia":
    "Nenhum animal saiu da sua lista até agora. Aqui ficam os que você cuidou e não cuida mais.",
  "anteriores.morreuEm": "Morreu em {data}",
  // O transferido também está nesta lista, e o produto não sabe para quem foi — dizer "morreu"
  // sobre ele seria mentira, e é por isso que a data vem do óbito e não do fim da custódia.
  "anteriores.saiu": "Não está mais com você",

  // ==================================================== achei um animal na rua (Tela 34)
  "encontrado.titulo": "Achou um animal?",
  "encontrado.apoio":
    "Se ele tem microchip, um veterinário, uma clínica ou uma ONG podem ler o número em segundos. Digite aqui e encontre quem responde por ele.",
  "encontrado.numero": "Número do microchip",
  "encontrado.procurar": "Procurar",
  "encontrado.procurando": "Procurando…",
  // A promessa do desenho é "não guardamos quem fez a busca", e ela é verdade: o log registra o
  // acesso sem ator. A segunda metade — "e por onde" — não existe, e a frase não a promete.
  "encontrado.semConta":
    "Não precisa criar conta. Não guardamos quem fez a busca. Quem responde pelo animal vê que ele foi procurado.",

  "encontrado.cartao.marca": "Encontrado · só leitura",
  "encontrado.rodape":
    "Quem responde pelo {nome} foi avisado de que ele foi procurado agora. É o mesmo cartão de emergência dele — nada a mais.",

  // O vazio que diz o que fazer em seguida. O texto mora em `erro.161`, e este é o título.
  "encontrado.vazio.titulo": "Este número não está no Petfy",

  // ============================================ o cartão do animal, dividido por duas telas
  // A Tela 04 (link compartilhado) e a Tela 34 (achei na rua) mostram a mesma ficha. O que
  // muda é a marca do topo e o rodapé, e por isso só esses dois moram fora daqui.
  "cartao.ligar": "Ligar para {quem}",
  // Cadastro sem telefone: o nome aparece de todo jeito, porque saber que existe uma clínica
  // cuidando dele já ajuda quem está com o animal.
  "cartao.semTelefone": "{quem} · sem telefone cadastrado",
  "cartao.alergias": "Alergias",
  "cartao.condicoes": "Condições",
  "cartao.medicacao": "Medicação em curso",
  "cartao.vacinacao": "Vacinação",
  "cartao.vacina.vencida": "{nome} vencida",
  "cartao.vacina.emDia": "{nome} em dia",
  "cartao.vacina.chegando": "{nome} · próxima dose em {data}",
  "cartao.vacina.semProxima": "{nome} · sem próxima dose marcada",
  "cartao.nada": "Nada registrado",

  // ------------------------------------------ o link compartilhado (Tela 04) e o vencimento
  "cartao.marca": "Cartão do animal · só leitura",
  "cartao.carregando": "Abrindo o cartão…",
  "cartao.idade": "{anos, plural, =0 {menos de 1 ano} one {# ano} other {# anos}}",
  "cartao.rodape":
    "Este cartão vale até {data}. Depois disso, peça um novo link a quem responde pelo {nome}. Não é preciso criar conta.",
  "cartao.semPrazo": "Este cartão não tem prazo. Quem responde pelo {nome} pode revogá-lo quando quiser.",
  // O que o link NÃO alcança não fica em silêncio: sem isto, um cartão sem alergias parece
  // dizer "não tem alergia", quando o que houve foi o tutor não conceder esse pedaço.
  "cartao.foraDoEscopo": "Este link não mostra {o_que}. Quem o gerou escolheu o que ele alcança.",
  "cartao.escopo.condicoes": "alergias, condições e medicação",
  "cartao.escopo.contato": "para quem ligar",
  "cartao.escopo.carteira": "a carteira de vacinação",

  // O cartão vencido (Tela 14). Diz o que fazer, e não só que acabou.
  "cartao.expirado.marca": "Cartão expirado · sem conta, sem senha",
  "cartao.expirado.titulo": "Este cartão venceu",
  "cartao.expirado.texto":
    "Quem responde por este animal pode gerar um novo link. Se for uma emergência, procure um veterinário — ele saberá o que fazer sem esta ficha.",

  // ===================================================== o animal comunitário (Telas 43 e 44)
  "colonia.titulo": "Os animais do grupo",
  "colonia.vazia":
    "Nenhum animal registrado ainda. Quem cadastra o primeiro é quem já alimenta a colônia.",
  "colonia.carregando": "os animais do grupo",

  // A caixa que explica o gesto. Ela está na tela porque "marcar que viu" parece pequeno demais
  // para ser o mais importante — e é.
  "colonia.sinalVital.titulo": "“Visto por último” é o sinal vital daqui",
  "colonia.sinalVital.texto":
    "Um animal de rua não falta à creche nem deixa de comer em casa: ele some. Marcar que você viu hoje é o gesto mais frequente desta tela, e é o que permite ao grupo perceber, em vez de descobrir tarde, que alguém desapareceu.",

  "colonia.filtro.TODOS": "Todos",
  "colonia.filtro.FALTA_CASTRAR": "Falta castrar",
  "colonia.filtro.EM_TRATAMENTO": "Em tratamento",
  "colonia.filtro.SUMIDOS": "Sumidos há mais de 15 dias",

  "colonia.coluna.gato": "Animal",
  "colonia.coluna.situacao": "Situação",
  "colonia.coluna.visto": "Visto por último",

  // A situação, em três estados. O terceiro é o que o desenho desenha tracejado.
  "colonia.castrado": "Castrado em {data}",
  "colonia.castracaoMarcada": "Castração marcada, {data}",
  "colonia.semInformacao": "Sem informação",

  // "Visto por último" — o sinal vital. "Nunca" é diferente de "há muito tempo", e a tela diz os
  // dois de formas diferentes: quem nunca foi marcado não está sumido, está sem registro.
  "colonia.visto.hoje": "hoje, por {quem}",
  "colonia.visto.ontem": "ontem, por {quem}",
  "colonia.visto.dias": "há {dias} dias, por {quem}",
  "colonia.visto.nunca": "ninguém marcou ainda",
  "colonia.marcarQueVi": "Vi hoje",
  "colonia.marcando": "Marcando…",

  // O painel lateral: o que qualquer um faz, e o que precisa de duas pessoas.
  "colonia.qualquerUm": "O que qualquer um pode fazer",
  "colonia.qualquerUm.registrar": "Marcar que viu o animal, registrar ferida, foto, comportamento",
  "colonia.qualquerUm.veterinario": "Levar ao veterinário e registrar o que foi feito",
  "colonia.duasPessoas": "O que precisa de mais de uma pessoa",
  "colonia.duasPessoas.adocao": "Dar um animal para adoção",
  "colonia.duasPessoas.obito": "Encerrar a linha do tempo de um animal",
  "colonia.duasPessoas.remocao": "Tirar alguém do grupo",
  "colonia.duasPessoas.porque":
    "Duas pessoas do grupo precisam concordar. Sem dono, a proteção contra o gesto irreversível de uma pessoa só é o acordo de duas.",

  // Os pedidos esperando decisão.
  "colonia.pedidos": "Esperando concordância",
  "colonia.pedidos.vazio": "Nada esperando decisão agora.",
  "colonia.pedido.ADOCAO": "Passar {animal} para {quem}",
  "colonia.pedido.OBITO": "Encerrar a linha do tempo de {animal}",
  "colonia.pedido.REMOCAO_DE_MEMBRO": "Tirar {quem} do grupo",
  "colonia.pedido.pedidoPor": "Pedido por {quem}",
  "colonia.pedido.semMotivo": "Sem motivo escrito.",
  "colonia.pedido.concordar": "Concordar",
  "colonia.pedido.recusar": "Recusar",
  // Quem pediu vê o próprio pedido e vê que não pode decidi-lo. A frase diz o porquê, e não só
  // "indisponível": é a regra do produto, não uma limitação da tela.
  "colonia.pedido.euPedi":
    "Você pediu isto. Quem pede não concorda consigo — falta outra pessoa do grupo olhar.",

  // ------------------------------------------------------ Tela 44: da praça para uma casa
  "adotar.titulo": "Passar {nome} para uma casa",
  "adotar.paraQuem": "Para quem",
  "adotar.motivo": "Por que",
  "adotar.motivo.nota":
    "Quem concorda lê isto e nada mais. Um pedido que só diz “concorde” não dá à outra pessoa nada com que decidir.",
  "adotar.oQueVaiJunto":
    "A vida registrada vai junto — ela não começa no dia em que o animal entra numa casa.",
  // Sem nome de pessoa: a regra é "qualquer outra do grupo", e nomear alguém a transformaria em
  // "peça à Marta" — o pedido pararia no dia em que a Marta viajasse.
  "adotar.precisaDeOutra":
    "Outra pessoa do grupo precisa concordar. Duas pessoas decidem juntas o que é irreversível.",
  "adotar.pedir": "Pedir concordância do grupo",
  "adotar.pedindo": "Enviando…",
  // O que acontece DEPOIS da concordância, e que o desenho não diz: quem recebe precisa aceitar.
  "adotar.viraConvite":
    "Quando alguém concordar, {quem} recebe um convite para responder pelo animal. A custódia passa quando essa pessoa aceitar — ninguém recebe um animal sem dizer sim.",
  "adotar.pedido.enviado":
    "Pedido enviado. Nada muda até que outra pessoa do grupo concorde.",

  // ------------------------------------------------ Tela 45: encaminhar para o especialista
  "encaminhar.oQueE": "o encaminhamento",
  "encaminhar.titulo": "Encaminhar {nome}",
  "encaminhar.oCasoVaiInteiro":
    "O caso vai inteiro: seu diagnóstico, os exames e o que quem convive com o animal observou. Nada de foto de prontuário no WhatsApp.",
  "encaminhar.paraQuem": "Para quem",
  "encaminhar.buscarOutro": "Buscar outro profissional",
  "encaminhar.busca.dica": "Nome ou especialidade",
  // A trava de três letras é do servidor, e a tela diz o porquê em vez de não responder nada.
  "encaminhar.busca.curta": "Escreva ao menos três letras.",
  "encaminhar.busca.vazia":
    "Nenhum profissional com registro ativo encontrado. Só quem tem credencial ativa pode receber um encaminhamento.",
  // "Ele já registrou o raio-X do Code em 2023." É a diferença entre encaminhar para um nome numa
  // lista e encaminhar para quem já conhece o caso.
  "encaminhar.candidato.jaRegistrou": "Já registrou algo sobre {nome} em {ano}.",
  "encaminhar.candidato.novo": "Nunca registrou nada sobre {nome}.",
  "encaminhar.motivo": "Por que você está encaminhando",
  // Obrigatório, e a nota diz por que: duas pessoas leem isto com perguntas diferentes.
  "encaminhar.motivo.nota":
    "Quem responde pelo animal decide com base neste texto, e o especialista descobre por ele o que está sendo perguntado.",
  "encaminhar.oQueVaiJunto": "O que vai junto",
  // A frase que assume o compromisso do bloco, em vez de escondê-lo: o recorte é por tipo, e o que
  // motivou o encaminhamento vai no campo do motivo.
  "encaminhar.oQueVaiJunto.porEscopo":
    "O acesso é por tipo de registro, e não por evento escolhido — {total, plural, =0 {este animal ainda não tem nada registrado} one {há 1 evento na vida dele} other {há # eventos na vida dele}}. O que motivou o encaminhamento vai no campo acima.",
  "encaminhar.caixa.eventos":
    "{quantos, plural, =0 {nada registrado ainda} one {1 registro} other {# registros}}",
  // Mensagem separada, e não um `select` com chave vazia: o ICU não aceita chave vazia, e "desde"
  // só existe quando há algo registrado.
  "encaminhar.caixa.desde": " · desde {ano}",
  "encaminhar.caixa.dadoPessoal": "· não é prontuário: é o contato de quem responde pelo animal",
  "encaminhar.escopo.CARTEIRA": "Carteira de vacinação e antiparasitários",
  "encaminhar.escopo.CONDICOES": "Alergias e condições crônicas",
  "encaminhar.escopo.PRONTUARIO": "Prontuário: atendimentos, diagnósticos e orientações",
  "encaminhar.escopo.OBSERVACOES": "Observações de quem convive com o animal",
  "encaminhar.escopo.PESO": "Histórico de peso",
  "encaminhar.escopo.ANEXOS": "Laudos, exames e anexos",
  "encaminhar.escopo.CONTATO": "Nome e telefone de quem responde pelo animal",
  // Sem o nome de quem responde pelo animal, e a diferença com o desenho é de acesso: o nome vive
  // no escopo CONTATO, e quem encaminha em geral tem só o clínico.
  "encaminhar.precisaAutorizar":
    "Quem responde pelo animal precisa autorizar. Encaminhar é você indicando o caminho; conceder acesso continua sendo dele, como sempre foi. O acesso vale {dias} dias e depois fecha sozinho.",
  "encaminhar.enviar": "Enviar encaminhamento",
  "encaminhar.enviando": "Enviando…",
  "encaminhar.tutorDecide": "Quem responde pelo animal recebe e decide.",
  "encaminhar.faltaPreencher":
    "Falta escolher para quem, escrever o motivo e marcar ao menos um tipo de registro.",
  "encaminhar.enviado.titulo": "Encaminhamento enviado",
  "encaminhar.enviado.esperandoDecisao":
    "{quem} só vai poder abrir o caso se quem responde pelo animal autorizar. Ele não foi avisado ainda.",
  // Quem responde pelo animal encaminhando não pede autorização a si mesmo.
  "encaminhar.enviado.jaAutorizado":
    "Você responde por este animal, então o acesso de {quem} já está liberado — por {dias} dias, e depois fecha sozinho.",
  "encaminhar.voltarAoAnimal": "Voltar para {nome}",

  // -------------------------------------------- o outro lado da Tela 45: receber e decidir
  "encaminhamentos.oQueE": "os encaminhamentos",
  "encaminhamentos.titulo": "Encaminhamentos",
  "encaminhamentos.oQueEstaTelaE":
    "Quando uma clínica encaminha o caso de um animal seu, a decisão de liberar o acesso é sua — e é aqui.",
  "encaminhamentos.esperandoVoce": "Esperando sua decisão",
  "encaminhamentos.nadaEsperando": "Nenhum encaminhamento espera sua decisão.",
  "encaminhamentos.quemEncaminhou": "{quem} encaminhou {animal} para {para}",
  "encaminhamentos.oQueIriaJunto": "O que iria junto: {escopos}",
  "encaminhamentos.prazo":
    "O acesso valeria {dias} dias e depois fecharia sozinho. Você pode revogar antes, a qualquer momento.",
  "encaminhamentos.autorizar": "Autorizar o acesso",
  "encaminhamentos.recusar": "Não autorizar",
  "encaminhamentos.paraMim": "Encaminharam para você",
  "encaminhamentos.nadaParaMim": "Nenhum caso foi encaminhado para você.",
  "encaminhamentos.recebido.titulo": "{quem} encaminhou {animal} para você",
  // O pendente não traz o nome do animal, e a frase diz o porquê em vez de deixar um espaço vazio.
  "encaminhamentos.recebido.pendente":
    "{quem} encaminhou um caso para você, e quem responde pelo animal ainda não autorizou",
  "encaminhamentos.recebido.esperando":
    "Você ainda não pode abrir o caso. Enquanto não houver autorização, nem o nome do animal aparece aqui.",
  "encaminhamentos.recebido.recusado":
    "Quem responde pelo animal não autorizou. O caso não foi compartilhado.",
  "encaminhamentos.acessoAte":
    "Acesso liberado por quem responde pelo animal até {quando}.",

  // ------------------------------------------------------------- Tela 46: apadrinhar
  "apadrinhar.oQueE": "o apadrinhamento",
  "apadrinhar.identidade": "{especie} · {raca} · no {abrigo}",
  "apadrinhar.desdeQuando": "Lá desde {ano}",
  "apadrinhar.oQueCusta": "O que o abrigo gasta com ele por mês",
  "apadrinhar.custoReal": "Custo real, todo mês",
  // O vazio dirige, e não anuncia falha: sem custo lançado não há o que bancar de concreto.
  "apadrinhar.semCusto":
    "O {abrigo} ainda não lançou os gastos mensais deste animal. Você pode bancar um valor livre — e o que for comprado aparece para você.",
  "apadrinhar.quantoBancar": "Quanto você quer bancar",
  "apadrinhar.outro": "Outro",
  "apadrinhar.outro.nota": "valor livre",
  "apadrinhar.livre.oQue": "O que você quer bancar",
  "apadrinhar.livre.quanto": "Quanto por mês",
  "apadrinhar.coisaConcreta":
    "Você banca uma coisa concreta, não uma cota abstrata. Quando ela for comprada, você vai ver o evento — com data, valor e quem comprou.",
  "apadrinhar.bancar": "Passar a bancar",
  "apadrinhar.enviando": "Registrando…",
  "apadrinhar.podeParar":
    "Pode parar quando quiser, sem justificar. O abrigo é avisado com 30 dias para se organizar.",
  // Um número, e não nomes: nenhum ranking, nenhuma barra de meta, nenhuma urgência fabricada.
  "apadrinhar.quantosBancam":
    "{quantos, plural, one {Uma pessoa já banca} other {# pessoas já bancam}} algo dele.",
  "apadrinhar.faltaEscolher": "Escolha o que você quer bancar.",
  "apadrinhar.pronto.titulo": "Você passou a bancar o cuidado do {nome}",
  "apadrinhar.pronto.oQueAcontece":
    "O {abrigo} foi avisado. A partir de agora, o que for comprado do que você banca aparece para você — com data, valor e quem comprou.",
  // A frase que evita a pergunta mais previsível desta tela.
  "apadrinhar.pronto.oPetfyNaoCobra":
    "O Petfy não cobra esse valor de você e não o repassa: quem combina o pagamento são vocês dois. O que o produto faz é registrar o compromisso e mostrar onde o dinheiro foi.",

  // --------------------------------------------- o lado do padrinho, depois do gesto
  "apadrinhamentos.oQueE": "o que você banca",
  "apadrinhamentos.titulo": "O que você banca",
  "apadrinhamentos.oQueEstaTelaE":
    "A vida registrada de quem você ajuda, filtrada no que você banca. Nenhum relatório, nenhuma newsletter.",
  "apadrinhamentos.vazio": "Você ainda não banca o cuidado de nenhum animal.",
  "apadrinhamentos.oQueVoceBanca": "{oQue} · {animal}",
  "apadrinhamentos.porMes": " por mês",
  "apadrinhamentos.desde": "Desde {quando}.",
  "apadrinhamentos.terminaEm":
    "Você pediu para parar. Continua bancando até {quando} — são os 30 dias que o abrigo tem para se organizar.",
  "apadrinhamentos.terminou": "Terminou.",
  "apadrinhamentos.verOQueRecebo": "Ver o que foi comprado",
  "apadrinhamentos.fechar": "Fechar",
  "apadrinhamentos.parar": "Parar de bancar",
  "apadrinhamentos.carregandoEventos": "Carregando…",
  // O vazio aqui é comum e não é falha: o abrigo ainda não lançou nada desde que a pessoa começou.
  "apadrinhamentos.aindaSemEvento":
    "Nada foi lançado ainda desde que você começou. O que o abrigo registrar aparece aqui.",
  "apadrinhamentos.evento": "{oQue} em {quando} · {valor}",
  "apadrinhamentos.evento.porQuem": " · {quem}",
  // O desenho promete a foto e avisa que ela não é garantida. O produto não tem como entregá-la
  // sem decidir quais anexos são públicos, então diz o que faz em vez de simular.
  "apadrinhamentos.semFoto":
    "Aparecem aqui os gastos do que você banca. Fotos do dia a dia não são prometidas — o que o Petfy garante é o registro.",

  // ------------------------------------------------------------ Tela 47: hospedagem
  "hospedagem.oQueE": "a hospedagem",
  "hospedagem.entregar.titulo": "Deixar o animal hospedado",
  "hospedagem.entregar.oQueE":
    "Hospedagem é custódia temporária: quem recebe passa a responder pelo animal, com prazo, e devolve no dia combinado.",
  "hospedagem.entregar.onde": "Onde ele fica",
  "hospedagem.entregar.onde.nota":
    "Só aparecem organizações que podem responder por um animal. Uma clínica que apenas registra atendimento não passa a responder por ele durante uma semana.",
  "hospedagem.entregar.volta": "Volta prevista",
  "hospedagem.entregar.volta.nota":
    "Prevista, e não automática: a volta acontece quando alguém registra. A data serve para vocês dois combinarem, e ninguém devolve um animal porque o relógio virou.",
  // O que muda de verdade, dito antes do gesto.
  "hospedagem.entregar.oQueMuda":
    "Enquanto ele estiver lá, quem responde por ele é a organização — ela pode levá-lo ao veterinário sem esperar você. Você continua vendo tudo, e a responsabilidade volta para você no dia da devolução.",
  "hospedagem.entregar": "Entregar para hospedagem",
  "hospedagem.entregando": "Registrando…",
  "hospedagem.voltarAoAnimal": "Voltar para o animal",
  "hospedagem.titulo": "O {animal} está na {onde}",
  "hospedagem.diaDe": "Dia {dia} de {total}",
  "hospedagem.periodo": "Entrou em {entrada} · volta prevista para {volta}",
  "hospedagem.oQueAconteceu": "Isto é o que aconteceu com ele desde que saiu de casa.",
  "hospedagem.carregando": "Carregando…",
  // O vazio é comum e não é falha: ninguém registrou nada desde a entrada.
  "hospedagem.aindaNada":
    "Nada foi registrado desde a entrada. Quando alguém da {onde} anotar algo, aparece aqui.",
  // Evento fora do escopo aparece opaco em vez de sumir — sumir diria que nada aconteceu.
  "hospedagem.evento.opaco": "Um registro que você não alcança",
  "hospedagem.enquantoEleEstaLa": "Enquanto ele está lá",
  "hospedagem.regra.responde":
    "A {onde} responde por ele até {volta}, e pode levá-lo ao veterinário sem esperar você.",
  "hospedagem.regra.voceContinuaLendo":
    "A responsabilidade volta para {quem} quando a devolução for registrada. Até lá, você continua vendo tudo que acontece com ele.",
  "hospedagem.devolver": "Registrar a volta",
  "hospedagem.devolvendo": "Registrando…",

  // -------------------------------------------------------- Tela 48: o ano do animal
  "ano.oQueE": "o ano do animal",
  "ano.titulo": "O ano do {nome}",
  "ano.periodo": "{de} a {ate}",
  "ano.anoDeVida": " · {ano}º ano dele",
  "ano.imprimir": "Imprimir",
  "ano.registros": "Registros no ano",
  "ano.registros.nota":
    "por {pessoas, plural, =0 {ninguém} one {1 pessoa} other {# pessoas}} e {organizacoes, plural, =0 {nenhuma organização} one {1 organização} other {# organizações}}",
  "ano.creche": "Dias na creche",
  "ano.creche.nota":
    "{dias, plural, =0 {nenhum de hospedagem} one {e 1 de hospedagem} other {e # de hospedagem}}",
  "ano.peso": "Peso",
  "ano.peso.valor": "{kg} kg",
  "ano.peso.antes": "era {kg} kg no começo do período",
  "ano.peso.semAnterior": "não houve outra pesagem no período",
  "ano.saude": "O que aconteceu de saúde",
  "ano.saude.consultas":
    "{quantas, plural, =0 {Nenhum atendimento registrado} one {1 atendimento} other {# atendimentos}}",
  "ano.saude.doses":
    "{vacinas, plural, =0 {Nenhuma vacina} one {1 vacina} other {# vacinas}} e {antiparasitarios, plural, =0 {nenhum antiparasitário} one {1 antiparasitário} other {# antiparasitários}}",
  // O QUE DEU ERRADO. É a parte que nenhum resumo automático costuma ter.
  "ano.saude.esteveVencida":
    "A {vacina} ficou {dias, plural, one {1 dia} other {# dias}} vencida em {quando}.",
  // O ainda-vencido é outra frase: ele não é história, é uma pendência de hoje.
  "ano.saude.vencidaAgora":
    "A {vacina} está vencida desde {desde} — {dias, plural, one {1 dia} other {# dias}} até agora.",
  "ano.saude.naoReavaliada": "A {condicao} não foi reavaliada desde {ano}.",
  // O vazio aqui é notícia boa, e a frase diz isso em vez de deixar a seção sem nada.
  "ano.saude.nadaPendente":
    "Nenhuma vacina esteve vencida e nenhuma condição crônica ficou sem reavaliação no período.",
  "ano.quemCuidou": "Quem cuidou dele este ano",
  "ano.quemCuidou.pelaOrganizacao": "{quem}, pela {organizacao}",
  "ano.quemCuidou.registros": "{quantos, plural, one {1 registro} other {# registros}}",
  "ano.quemCuidou.ninguem": "Ninguém registrou nada sobre ele no período.",
  "ano.paraQueServe":
    "Este é o documento que você leva à próxima consulta, e o que você entrega junto com ele se algum dia ele passar para outra pessoa. Um ano de vida dele, em uma página.",

  // ------------------------------------------------------ Tela 18: a agenda do petshop
  "agenda.oQueE": "a agenda",
  "agenda.hoje": "Hoje",
  "agenda.quantos": "{quantos, plural, =0 {nada marcado} one {1 banho hoje} other {# banhos hoje}}",
  // O vazio dirige, e não anuncia falha: um dia sem banho marcado é um dia normal.
  "agenda.vazia": "Nada marcado para hoje.",
  "agenda.abrir": "Abrir",
  "agenda.fechar": "Fechar",
  "agenda.marcarEntrada": "Marcar entrada",
  "agenda.entregue": "Entregue às {quando}",
  "agenda.faltou": "Não veio",
  "agenda.naoVeio": "Marcar que não veio",
  "agenda.oQuePrecisaSaber": "O que você precisa saber antes de encostar nele",
  // O estado que a maioria dos produtos esconderia. Um cartão sem as linhas parece um animal sem
  // restrição nenhuma.
  "agenda.noEscuro": "O acesso a este animal venceu — você está no escuro",
  "agenda.semLinhas":
    "Você não tem acesso às informações deste animal. Peça de novo antes de dar o banho: um cartão vazio não quer dizer que ele não tem restrição.",
  "agenda.vacinacaoEmDia": "Vacinação em dia",
  "agenda.vacinacaoAtrasada": "Há vacina vencida na carteira dele",
  "agenda.eTudoQueOTutorCompartilhou":
    "É tudo o que quem cuida dele compartilhou, e é tudo o que o banho exige. O histórico clínico existe e não está aqui.",
  "agenda.algoQueNotou": "Alguma coisa que você notou",
  // A frase mais importante da tela: observação nunca vira ato clínico sozinha.
  "agenda.descrevaOQueViu":
    "Descreva o que viu, não o que acha que é. Isso entra na linha do tempo do {animal} como observação sua, e o veterinário decide o resto.",
  "agenda.entregarEAvisar": "Entregar e avisar quem cuida dele",
  "agenda.oQueNuncaVe": "O que você nunca vê",
  "agenda.oQueNuncaVe.lista":
    "Diagnósticos, prescrições e exames · histórico de atendimentos · recados e fotos de outras organizações.",
  "agenda.quatroLinhasBastam":
    "Quatro linhas de saúde bastam para o banho ser seguro. Pedir mais que isso seria pedir o que não se usa.",

  // -------------------------------------- Telas 19–21: quem recebe o convite de animal
  "conviteDeAnimal.oQueE": "o convite",
  "conviteDeAnimal.tipo.coTutoria": "Convite de co-tutoria",
  "conviteDeAnimal.tipo.transferencia": "Transferência de titularidade",
  "conviteDeAnimal.tipo.adocao": "Adoção",
  "conviteDeAnimal.coTutoria.titulo": "{quem} quer dividir o cuidado do {animal} com você",
  "conviteDeAnimal.coTutoria.resumo":
    "Você vai ver tudo sobre o {animal} e registrar junto: remédio dado, consulta, o que notou no dia.",
  "conviteDeAnimal.transferencia.titulo": "{quem} quer passar o {animal} para você",
  "conviteDeAnimal.transferencia.resumo":
    "Você passa a responder pelo {animal}. A vida registrada dele vai junto — ela não recomeça no dia em que ele muda de mão.",
  "conviteDeAnimal.adocao.titulo": "{quem} quer que você adote o {animal}",
  "conviteDeAnimal.adocao.resumo":
    "Você passa a responder pelo {animal}. Tudo que o abrigo registrou sobre ele vai junto — você não recebe uma ficha em branco.",
  "conviteDeAnimal.oQueSignifica": "O que isso significa",
  "conviteDeAnimal.significa.avisos": "Você passa a receber os avisos de vacina e remédio",
  "conviteDeAnimal.significa.autoria": "O que você registrar leva o seu nome, para sempre",
  "conviteDeAnimal.significa.eleContinuaRespondendo":
    "{quem} continua respondendo pelo {animal}. Você pode sair quando quiser.",
  "conviteDeAnimal.significa.voceResponde":
    "Quem responde pelo animal passa a ser você. {quem} continua vendo a vida dele, e deixa de decidir sobre ele.",
  "conviteDeAnimal.aceitar": "Aceitar",
  "conviteDeAnimal.aceitando": "Aceitando…",
  "conviteDeAnimal.recusar": "Recusar",
  "conviteDeAnimal.recusando": "Recusando…",
  "conviteDeAnimal.seRecusar": "Se recusar, {quem} é avisado e nada muda para o {animal}.",
  "conviteDeAnimal.aceito.titulo": "Pronto — o {animal} é seu agora também",
  "conviteDeAnimal.aceito.agoraVoceAcompanha":
    "Você já vê tudo sobre ele e pode registrar junto com {quem}.",
  "conviteDeAnimal.aceito.agoraVoceResponde":
    "Você responde pelo {animal} a partir de agora. Toda a vida registrada dele veio junto.",
  "conviteDeAnimal.recusado.titulo": "Convite recusado",
  "conviteDeAnimal.recusado.nadaMuda":
    "{quem} foi avisado, e nada muda para o {animal}: quem respondia por ele continua respondendo.",
  "conviteDeAnimal.verAnimais": "Ver meus animais",
  "conviteDeAnimal.irParaInicio": "Ir para o início",
  "conviteDeAnimal.invalido.titulo": "Este convite não vale mais",
  // A recusa é um estado só, e a tela não inventa a distinção que o backend recusa a dar.
  "conviteDeAnimal.invalido.oQuePodeSer":
    "Ele pode ter expirado, já ter sido usado, ter sido cancelado por quem o enviou, ou ter sido endereçado a outro e-mail. Peça um convite novo a quem cuida do animal.",

  // ------------------------------------------------------------------ Tela 35: a busca
  "buscar.titulo": "Buscar",
  "buscar.dica": "Nome, microchip ou RGA",
  "buscar.curta": "Escreva ao menos três letras.",
  "buscar.procurando": "Procurando…",
  "buscar.seusAnimais": "Seus animais",
  "buscar.nenhumSeu": "Nenhum animal seu com esse nome ou número.",
  "buscar.pelaOrganizacao": "Animais que você alcança pela {organizacao}",
  "buscar.nenhumDaOrganizacao": "Nenhum animal da organização com esse nome ou número.",
  "buscar.tutor": " · tutor {quem}",
  // A frase que quase nenhum produto teria: dizer que existe o que você não pode ver.
  "buscar.existemOutros":
    "Existem outros animais que casam com essa busca no Petfy. Você não tem acesso a eles, e por isso não aparecem aqui. Se você encontrou um animal na rua,",
  "buscar.useAEncontrado": "use a busca de animal encontrado.",

  // ------------------------------------------------------------------ Tela 36: a conta
  "conta.oQueE": "sua conta",
  "conta.titulo": "Sua conta",
  "conta.alterar": "Alterar",
  "conta.nomeETelefone": "Nome e telefone",
  "conta.nomeETelefone.nota":
    "Aparecem para quem cuida dos seus animais, e no cartão de emergência.",
  "conta.senha": "Senha",
  "conta.senha.alteradaEm": "Alterada em {quando}.",
  // Nulo significa "nunca desde que a conta existe", e a tela diz isso em vez de mostrar a data do
  // cadastro como se fosse troca.
  "conta.senha.nuncaTrocada": "Você nunca trocou desde que criou a conta.",
  "conta.registroProfissional": "Registro profissional",
  "conta.registro.semNenhum":
    "Você não declarou nenhum. Sem ele, você não registra diagnóstico nem prescrição.",
  "conta.declarar": "Declarar",
  "conta.registro.numero": "CRMV",
  "conta.registro.uf": "UF",
  "conta.registro.especialidade": "Especialidade (opcional)",
  "conta.registro.salvar": "Salvar registro",
  "conta.registro.informado":
    "Entra como informado: o Petfy não consulta o conselho, e o registro carrega essa informação em vez de fingir uma garantia que não tem.",
  // A ÚNICA LINHA DE TELA QUE FALTAVA, e ela passou a existir na V50.
  //
  // Aqui estava escrito que o Petfy não guardava a lista, e a justificativa era o JWT sem estado.
  // A premissa estava errada: o filtro já consultava o banco em toda requisição autenticada.
  "conta.aparelhos": "Aparelhos conectados",
  "conta.aparelhos.quantos":
    "{quantos, plural, =0 {Nenhuma entrada aberta} one {# entrada aberta} other {# entradas abertas}} na sua conta.",
  "conta.aparelhos.carregando": "Carregando…",
  // Sem palpite: user agent que não dá para reconhecer vira "não identificado", e não um chute que
  // faria a pessoa encerrar a sessão errada.
  "conta.aparelhos.desconhecido": "Aparelho não identificado",
  "conta.aparelhos.este": "· este aqui",
  "conta.aparelhos.desde": "Desde {data}",
  "conta.aparelhos.encerradoEm": "Encerrado em {data}",
  "conta.aparelhos.encerrar": "Encerrar",
  // A frase vem ANTES do gesto: cair na tela de entrada sem aviso pareceria defeito.
  "conta.aparelhos.esteEhSair":
    "Este é o aparelho que você está usando agora. Encerrar aqui é sair do Petfy neste aparelho. Quer continuar?",
  "conta.aparelhos.oQueE": "o encerramento",
  "conta.aparelhos.trocarSenha":
    "Não guardamos de onde você entrou — um endereço de rede não ajudaria você a reconhecer o próprio aparelho. Se não reconhecer alguma entrada, trocar a senha derruba todas de uma vez.",
  "conta.levarDados": "Levar meus dados embora",
  "conta.levarDados.nota":
    "Baixa a vida registrada dos seus animais em arquivo legível, sem pedir motivo.",
  "conta.baixar": "Baixar",
  "conta.encerrar": "Encerrar a conta",
  // A regra que esta tela trouxe: ninguém sai deixando um animal sem quem responda por ele.
  "conta.encerrar.precisamDeAlguem":
    "{quantos, plural, one {O {animais} precisa} other {Os animais {animais} precisam}} de alguém antes que você saia. Passe cada um para outra pessoa — a vida registrada deles vai junto e não se apaga com a sua conta.",
  "conta.encerrar.transferir": "Transferir {animal}",
  "conta.encerrar.nadaPendente":
    "Nenhum animal está sob a sua responsabilidade. Encerrar apaga a sua conta e os seus dados, e é irreversível.",
  "conta.encerrar.botao": "Encerrar conta",
  "conta.encerrar.confirmar": "Confirmar — isto não tem volta",
  "conta.encerrar.disponivelQuando":
    "Disponível quando nenhum animal estiver sob sua responsabilidade.",

  // ------------------------------------------------- o que o tutor registra no próprio animal
  //
  // <b>O buraco mais largo do produto, e ele estava à vista.</b> O botão "Registrar evento" ficava
  // `disabled` no cabeçalho do animal — sem condição, sem destino, sem handler. Os hooks
  // `useRegistrarDose`, `useDoseAnterior` e `useCorrecoes` existiam e nenhuma rota os chamava: o
  // único lugar que registrava vacina ou peso era a tela da CLÍNICA. O tutor tinha um produto que
  // promete "cada dose entrou aqui com o nome de quem fez", e nenhum jeito de fazer.
  //
  // ATENDIMENTO NÃO ESTÁ AQUI, e a ausência é a regra mais importante da tela: diagnóstico e
  // prescrição são ato clínico, e ato clínico exige credencial. O tutor registra o que VIU.
  "registrar.titulo": "O que aconteceu com o {nome}",
  "registrar.apoio":
    "Vacina, peso e observação são o que você registra sozinho, sem organização por trás. Diagnóstico e prescrição são ato clínico — quem os registra assina com o próprio registro profissional.",
  "registrar.carregando": "o animal",
  "registrar.oQueE": "o registro",
  "registrar.oQue": "O que aconteceu",
  "registrar.oQue.vacina": "Vacina",
  "registrar.oQue.peso": "Peso",
  "registrar.oQue.observacao": "Observação",

  // A data vem antes do conteúdo, e nunca "agora" implícito: quem lança a carteirinha de papel de
  // 2019 está registrando 2019, e um formulário que assume hoje transformaria a vida inteira do
  // animal num único dia.
  "registrar.quando.vacina": "Quando foi aplicada",
  "registrar.quando.peso": "Quando foi pesado",
  "registrar.quando.observacao": "Quando aconteceu",

  "registrar.vacina.qual": "Qual vacina",
  "registrar.vacina.nota":
    "A lista vem do que a carteira já acompanha. Escrever o mesmo nome de outro jeito cria uma segunda série, e o mesmo reforço passa a aparecer duas vezes.",
  "registrar.vacina.proxima": "Próxima dose (opcional)",
  "registrar.vacina.proxima.nota":
    "Sem ela o Petfy não tem como avisar que a dose está chegando, e o lembrete é metade do que ele faz.",

  "registrar.peso.quanto": "Quanto pesou, em quilos",
  "registrar.peso.nota": "Vírgula para os gramas: 8,6 são oito quilos e seiscentos.",

  "registrar.observacao.oQue": "O que você viu",
  // A mesma frase da Tela 18, e pela mesma razão: "a distinção entre o que alguém viu e o que
  // alguém concluiu é a mais importante do produto".
  "registrar.observacao.nota":
    "Observação é o que você viu, e não uma conclusão: “mancou depois do parque” é registro; “tem displasia” é diagnóstico, e quem diagnostica assina.",

  "registrar.gravar": "Registrar",
  "registrar.autoria":
    "O que você registrar leva o seu nome, para sempre — quem cuida deste animal vê o registro e vê quem o fez.",
  "registrar.voltar": "Voltar para o {nome}",
  "registrar.feito.vacina": "{vacina} entrou na carteira.",
  "registrar.feito.peso": "{peso} kg entrou no acompanhamento de peso.",
  "registrar.feito.observacao": "A observação entrou na linha do tempo.",

  // ------------------------------------------------------- o que o Petfy te contou (a V48)
  //
  // O produto avisa por e-mail desde o P4 e nunca avisou aqui dentro. O sino da moldura existia
  // desabilitado, com "não há canal de aviso" escrito ao lado — esta tela é o destino dele.
  "avisos.titulo": "Avisos",
  "avisos.oQueE": "os avisos",
  "avisos.apoio":
    "O que aconteceu nos seus animais, com o mesmo texto que foi para o seu e-mail. Fica guardado aqui mesmo que o e-mail não chegue.",
  "avisos.marcarTodos": "Marcar todos como lidos",
  "avisos.marcarLido": "Marcar como lido",
  // O VAZIO AQUI É BOA NOTÍCIA, e a frase diz isso. "Nada encontrado" ensinaria a pessoa a temer
  // a tela — num produto de saúde animal, nenhum aviso significa que nada mudou de mão.
  "avisos.vazio":
    "Nada por aqui, e isso é uma boa notícia: ninguém entrou nos seus animais e nada mudou de mão.",
  "avisos.inicio": "Voltar para o início",

  // ---------------------------------------------------- onde se cola o código de um convite
  //
  // Existe porque o e-mail de convite manda um CÓDIGO, e não um link: nenhuma notificação deste
  // produto carrega link, e a confirmação de e-mail já manda "Código: ...". Sem esta tela o código
  // não teria onde ser colado.
  //
  // UMA PORTA, E NÃO DUAS: "convite de animal" e "convite de organização" é vocabulário de quem
  // escreveu o backend. Quem recebe um código não sabe de que tipo ele é, e não deveria precisar
  // saber — a tela pergunta ao servidor e leva a pessoa ao lugar certo.
  "colarConvite.titulo": "Cole o código do convite",
  "colarConvite.apoio":
    "Quem convidou você recebeu um código e mandou junto com o convite. Cole aqui e nós abrimos o convite certo.",
  "colarConvite.rotulo": "Código",
  "colarConvite.exemplo": "cole aqui",
  "colarConvite.abrir": "Abrir o convite",
  "colarConvite.procurando": "Procurando…",
  // O mesmo estado único das duas telas de aceite: o servidor responde igual para código
  // inexistente, expirado, revogado, já usado e endereçado a outra pessoa. Distinguir diria a quem
  // tenta adivinhar qual parte errou.
  "colarConvite.naoAchou":
    "Não encontramos um convite com este código. Ele pode ter expirado, já ter sido usado, ou ser de outro endereço de e-mail. Peça um novo a quem convidou você.",
  "colarConvite.lerNaoAceita":
    "Abrir não aceita nada: você vê de que convite se trata e o que ele muda antes de decidir.",
  "colarConvite.inicio": "Voltar para o início",
} as const;

export type ChaveDeMensagem = keyof typeof mensagens;
