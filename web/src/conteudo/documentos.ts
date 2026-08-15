/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O TEXTO dos dois documentos que o cadastro obriga a aceitar.
 *
 * <b>Mora aqui, e nao no `pt-BR.ts`</b>, porque nao e string de interface: e conteudo, muda por
 * cadencia propria — juridica, e nao de produto — e o backend ja versiona esse aceite pelo
 * `petfy.consent.privacy-version`. Enfiar um documento inteiro no arquivo de mensagens misturaria
 * "Entrar" com clausula de tratamento de dado de saude.
 *
 * <b>O QUE ESTA PRONTO E O QUE NAO ESTA.</b> As secoes marcadas com `pendente` descrevem o que o
 * produto FAZ hoje, verificado contra o codigo, e dizem em seguida o que falta um humano decidir.
 * Nenhuma delas inventa texto juridico: um documento que parece pronto e nao esta e pior que um
 * que se declara incompleto, porque a pessoa aceita achando que leu algo real.
 */

/** A versao que o backend cobra no aceite (`petfy.consent.privacy-version`). */
export const VERSAO_VIGENTE = "2026-08-05";

export type Secao = {
  titulo: string;
  paragrafos: string[];
  /** O que ainda falta nesta secao, e por que so um humano resolve. */
  pendente?: string;
};

export type Documento = {
  titulo: string;
  resumo: string;
  secoes: Secao[];
};

export const PRIVACIDADE: Documento = {
  titulo: "Política de privacidade",
  resumo:
    "O que o Petfy guarda sobre você e sobre os animais que você registra, quem consegue ler, "
    + "e o que você pode fazer a respeito.",
  secoes: [
    {
      titulo: "O que guardamos",
      paragrafos: [
        "Sobre você: nome, e-mail e, se você informar, telefone e endereço. A senha é guardada "
        + "apenas como resumo criptográfico — ninguém no Petfy consegue lê-la, nem nós.",
        "Sobre os animais: nome, espécie, raça, nascimento, peso, microchip e castração; vacinas "
        + "e antiparasitários aplicados; condições de saúde e alergias; orientações de cuidado e "
        + "medicação; observações; anexos que você enviar, como laudos e carteirinhas.",
        "Sobre o uso: cada entrada na conta, com data e o aparelho declarado pelo navegador. "
        + "Cada aceite deste documento, com a versão aceita. E cada leitura de dado de saúde "
        + "feita por terceiro, com o endereço de rede de quem leu.",
      ],
    },
    {
      titulo: "O que não guardamos",
      paragrafos: [
        "Não guardamos o endereço de rede das suas entradas na conta, nem localização. A lista de "
        + "aparelhos conectados mostra o navegador e o sistema — o suficiente para você reconhecer "
        + "o seu — e nada além disso.",
        "Não guardamos quem consultou um microchip na busca pública de animal encontrado.",
      ],
    },
    {
      titulo: "Por que podemos tratar esses dados",
      paragrafos: [
        "Pelo seu consentimento, dado no cadastro. Dado de saúde tem proteção reforçada na LGPD, e "
        + "sem esse aceite não haveria base legal para tratá-lo — por isso a caixa é obrigatória.",
        "Guardamos qual versão deste documento você aceitou, e quando. Se o texto mudar, o aceite "
        + "anterior passa a constar como pendente e pedimos um novo: um aceite de janeiro não diz "
        + "com o que você concordou depois de uma mudança de agosto.",
      ],
    },
    {
      titulo: "Quem consegue ler",
      paragrafos: [
        "Você, e quem você tornou co-tutor do animal.",
        "As organizações a que você concedeu acesso — clínica, creche, abrigo, petshop — e apenas "
        + "dentro do escopo que você escolheu ao conceder. Você revoga quando quiser.",
        "Quem abrir um link de compartilhamento que você gerou, pelo prazo e pelo escopo que você "
        + "definiu nele. O link não pede conta: quem o tem, lê.",
        "Toda leitura de terceiro fica registrada, e você vê essa lista na página do animal. "
        + "Leitura sua e de co-tutores não entra — o registro responde quem mais viu, e cuidado "
        + "compartilhado não é vigilância mútua.",
      ],
    },
    {
      titulo: "O que sai do Petfy",
      paragrafos: [
        "E-mails que avisam você: lembrete de vacina, correção feita por um profissional, entrada "
        + "de um aparelho novo. Só vão para endereços confirmados.",
        "O conteúdo de um link de compartilhamento, para quem você enviar o link.",
        "Não vendemos dado a ninguém, e não usamos o que você registra para publicidade.",
      ],
      pendente:
        "Falta listar nominalmente os serviços que operam por nós — provedor de e-mail e "
        + "hospedagem —, com país de processamento. É informação de infraestrutura contratada, e "
        + "precisa refletir o ambiente real de produção quando ele existir.",
    },
    {
      titulo: "O que você pode fazer",
      paragrafos: [
        "Ver quem leu o registro de saúde de cada animal seu.",
        "Levar tudo embora: a exportação traz você, seus consentimentos e todos os animais em que "
        + "você é tutor, e declara as próprias limitações dentro do arquivo.",
        "Sair. Como não há base legal sem consentimento, revogar o consentimento é encerrar a "
        + "conta.",
      ],
      pendente:
        "O encerramento de conta ainda não resolve o caso de quem tem animal cadastrado: apagar "
        + "junto, transferir para outro tutor ou anonimizar são decisões diferentes, e nenhuma "
        + "delas pode acontecer como efeito colateral silencioso. Enquanto não estiver decidido, "
        + "o pedido é atendido por contato direto.",
    },
    {
      titulo: "Por quanto tempo guardamos",
      paragrafos: [],
      pendente:
        "Falta definir prazo de retenção por tipo de dado — histórico de saúde do animal, registro "
        + "de leitura, aceites e anexos têm razões diferentes para durar. É decisão de negócio com "
        + "efeito jurídico, e não uma escolha de implementação.",
    },
    {
      titulo: "Quem responde por isso",
      paragrafos: [],
      pendente:
        "Falta a identificação do controlador — razão social, CNPJ e endereço — e o canal do "
        + "encarregado pelo tratamento de dados, que a LGPD exige que seja público.",
    },
  ],
};

export const TERMOS: Documento = {
  titulo: "Termos de uso",
  resumo: "O que o Petfy faz, o que ele não faz, e o que se espera de quem registra aqui.",
  secoes: [
    {
      titulo: "O que o Petfy é",
      paragrafos: [
        "Um lugar onde o histórico de saúde de um animal fica guardado e organizado, com o nome de "
        + "quem registrou cada coisa. Nada é apagado às escondidas: correção fica como correção, "
        + "com o valor anterior visível.",
        "A mesma conta serve para tudo. Não existe conta de tutor e conta de veterinário — o que "
        + "você alcança vem do que você tem: um animal sob sua responsabilidade, ou um vínculo com "
        + "uma organização.",
      ],
    },
    {
      titulo: "O que o Petfy não é",
      paragrafos: [
        "O Petfy não substitui atendimento veterinário. Guardamos e organizamos o que "
        + "profissionais e cuidadores registram, e não emitimos diagnóstico.",
        "Registros profissionais informados por quem se cadastra não são verificados junto aos "
        + "conselhos. Ver um CRMV declarado aqui não é o mesmo que confirmá-lo.",
        "Em emergência, procure um veterinário.",
      ],
    },
    {
      titulo: "O que é seu, e o que é sua responsabilidade",
      paragrafos: [
        "O que você registra continua seu, e você pode levar embora a qualquer momento.",
        "Você responde pelo que registra e por a quem concede acesso. Um link de compartilhamento "
        + "que você gera é lido por quem o tiver, pelo prazo que você definir.",
        "Ao registrar dado de um animal, você declara que responde por ele ou que foi autorizado "
        + "por quem responde.",
      ],
    },
    {
      titulo: "Conta, senha e acesso",
      paragrafos: [
        "A senha é sua responsabilidade. Avisamos por e-mail quando alguém entra de um aparelho "
        + "que a conta ainda não conhecia, e você pode encerrar qualquer entrada em Conta.",
        "Trocar a senha derruba todas as sessões abertas.",
      ],
    },
    {
      titulo: "Preço",
      paragrafos: [],
      pendente:
        "Falta dizer se o uso é gratuito, o que muda se deixar de ser, e o que acontece com o "
        + "histórico já registrado nesse caso. É decisão de produto, e ela precisa vir antes do "
        + "texto.",
    },
    {
      titulo: "Suspensão e encerramento",
      paragrafos: [],
      pendente:
        "Falta definir em que casos uma conta pode ser suspensa, com que aviso, e o que acontece "
        + "com o histórico dos animais ligados a ela — é o mesmo nó do encerramento de conta na "
        + "política de privacidade.",
    },
    {
      titulo: "Lei aplicável e foro",
      paragrafos: [],
      pendente: "Falta a cláusula, e ela depende de onde o controlador estiver constituído.",
    },
  ],
};
