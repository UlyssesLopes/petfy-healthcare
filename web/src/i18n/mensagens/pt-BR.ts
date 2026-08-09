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
  "entrar.ou": "ou",

  // A frase da esquerda nao vende o produto — lembra por que a conta existe. E a mesma
  // tese do simbolo: o animal permanece, as pessoas passam.
  "entrar.tese.titulo": "A vida deles continua registrada, mesmo quando você não está olhando.",
  "entrar.tese.apoio": "Cada dose, cada consulta e cada dia de creche entrou aqui com o nome de quem fez. Nada some, nada é reescrito às escondidas.",

  "entrar.agora.rotulo": "Precisa de algo agora?",
  "entrar.agora.texto": "Se o animal está passando mal e você não lembra a senha, o cartão de emergência dele abre sem login — alergias, remédios em curso e quem chamar.",
  "entrar.cartao.acao": "Abrir cartão de emergência",

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
  "home.nav.inicio": "Início",
  "home.contexto.voce": "Você",

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
  "home.acao.cumprir": "Confirmar que dei",
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
  "animal.credencial.INFORMADO": "informado",
  "animal.credencial.VERIFICADO": "verificado",
  "animal.credencial.SUSPENSO": "suspenso",
} as const;

export type ChaveDeMensagem = keyof typeof mensagens;
