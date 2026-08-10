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
  "entrar.ou": "ou",

  // A frase da esquerda nao vende o produto — lembra por que a conta existe. E a mesma
  // tese do simbolo: o animal permanece, as pessoas passam.
  "entrar.tese.titulo": "A vida deles continua registrada, mesmo quando você não está olhando.",
  "entrar.tese.apoio": "Cada dose, cada consulta e cada dia de creche entrou aqui com o nome de quem fez. Nada some, nada é reescrito às escondidas.",

  "entrar.agora.rotulo": "Precisa de algo agora?",
  "entrar.agora.texto": "Se o animal está passando mal e você não lembra a senha, o cartão de emergência dele abre sem login — alergias, remédios em curso e quem chamar.",
  "entrar.cartao.acao": "Abrir cartão de emergência",
  "entrar.cartao.semSenha": "Sem senha, para quando não dá tempo.",

  "entrar.continuarConectado": "Continuar conectado neste aparelho",
  "entrar.link.acao": "Receber um link por e-mail",
  "entrar.link.apoio": "Sem senha. Você clica no link e entra — serve quando a senha não vem à cabeça.",
  "entrar.criarConta": "Ainda não tem conta? {acao}",
  "entrar.criarConta.acao": "Criar uma agora",

  // As quatro frases de "por que esta desabilitado". A secao 06 pede que o desabilitado
  // nunca apareca mudo, e que o motivo venha ANTES do gesto — nao depois.
  "entrar.cartao.porque": "Ainda não dá para abrir daqui: o cartão abre por um link que quem responde pelo animal gera e envia.",
  "entrar.continuarConectado.porque": "Ainda não guardamos a sessão entre visitas — ao recarregar a página é preciso entrar de novo.",
  "entrar.link.porque": "Entrar por link ainda não existe. Por enquanto, só com senha.",
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
  "criarConta.jaTem": "Já tem conta?",
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
  "onboarding.p4.convidado": "Convite enviado para {email}. Ele aparece na rede quando a pessoa aceitar.",
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
  "transferir.continua.rotulo": "Quem continua alcançando o {nome}",
  "transferir.continua.voce": "Você",
  "transferir.continua.voce.texto": "Deixa de responder pelo {nome} e continua alcançando ele como co-tutor, com permissão de registrar. Quem recebe pode revogar isso.",
  "transferir.continua.coTutor": "{quem}, co-tutor",
  "transferir.continua.coTutor.texto": "Continua com o mesmo acesso. Quem recebe pode revogar.",
  "transferir.continua.organizacao.texto": "Continua com o acesso que você concedeu. Quem recebe pode revogar.",
  "transferir.continua.tese": "Hoje os acessos são herdados: quem recebe o animal recebe também quem já alcançava ele, e decide o que fica.",
  "transferir.acao": "Enviar transferência",
  "transferir.acao.enviando": "Enviando…",
  "transferir.acao.cancelar": "Cancelar",
  "transferir.enviado.titulo": "Transferência enviada para {email}",
  "transferir.enviado.apoio": "Enquanto a pessoa não aceitar, o {nome} continua sob sua responsabilidade e nada mudou.",
  "transferir.enviado.prazo": "O convite vale até {data}.",
  "transferir.enviado.voltar": "Voltar para a vida do {nome}",

  // ----------------------------------------------------- a adoção (Tela 13)
  //
  // "O adotante não ganha uma ficha em branco com a data de hoje: ganha onze anos de vida de
  // um animal que ele acabou de conhecer." É a tese do produto inteiro, num gesto só.
  "adocao.titulo": "Adoção do {nome}",
  "adocao.apoio": "A vida registrada passa inteira para quem adota. O {abrigo} continua vendo o que produziu, e deixa de mandar no registro.",
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
  "pacientes.recortes.indisponiveis": "Vencendo, em tratamento e atendidos este mês dependem de uma consulta por organização que ainda não existe",
  "pacientes.coluna.animal": "Animal",
  "pacientes.coluna.tutor": "Tutor",
  "pacientes.coluna.acessoDesde": "Acesso desde",
  "pacientes.atender": "Atender",
  "pacientes.oQue": "os pacientes",
  "pacientes.vazio": "Nenhum tutor concedeu acesso a esta organização ainda.",
  "pacientes.semResultado": "Nenhum paciente com esse nome.",
  "pacientes.mostrando": "Mostrando {quantos} de {total}",
  "pacientes.idade.anos": "{anos, plural, one {# ano} other {# anos}}",
  "pacientes.idade.meses": "{meses, plural, =0 {recém-nascido} one {# mês} other {# meses}}",
  "pacientes.vencendo.titulo": "Quem está vencendo",
  "pacientes.vencendo.falta": "Esta lista não existe ainda: a pendência é sempre da pessoa logada, e não há consulta de quem está vencendo por organização. Montar no cliente exigiria uma leitura por animal — centenas de requisições para desenhar uma coluna.",
  "pacientes.vencendo.aviso": "Quando ela existir, o aviso fala só da dose: acesso concedido não é lista de marketing.",

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
  "equipe.convidar.semFuncao": "Ainda não dá para escolher a função de quem entra: o convite guarda o e-mail e o prazo, e mais nada. Veterinária, monitora e recepção existem no modelo, mas não há por onde atribuí-las.",
  "equipe.aguardando": "{quantos, plural, =0 {Nenhum convite aguardando} one {# convite aguardando} other {# convites aguardando}}",
  "equipe.aguardando.vazio": "Ninguém foi convidado ainda.",
  "equipe.carregando": "Carregando os convites…",
  "equipe.convite.vale": "Vale até {data}",
  "equipe.convite.revogar": "Revogar",
  "equipe.encerrados": "{quantos, plural, one {# convite já encerrado} other {# convites já encerrados}} — aceitos, vencidos ou revogados.",
  "equipe.oQue": "os convites da equipe",
  "equipe.oQue.convite": "o convite",
  "equipe.falta.titulo": "A tabela da equipe não existe ainda",
  "equipe.falta.texto": "Nenhuma rota devolve quem já entrou na organização, então não há como listar a equipe, mostrar a função de cada um nem desligar alguém. Mostrar uma tabela vazia seria pior: os membros existem, e é o produto que ainda não sabe mostrá-los.",
  "equipe.comoAceita": "Quem recebe o convite entra criando a conta com ele. Quem já tem conta no Petfy ainda não tem por onde aceitar.",

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
} as const;

export type ChaveDeMensagem = keyof typeof mensagens;
