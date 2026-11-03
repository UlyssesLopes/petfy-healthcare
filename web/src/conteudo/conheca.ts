/**
 * O texto da página pública (`/conheca`). Fica fora do `pt-BR.ts` pelo mesmo motivo do
 * `documentos.ts`: é texto corrido de página, não rótulo de interface.
 *
 * Todo animal, pessoa e organização daqui é exemplo, e nenhum dado finge ser de usuário real.
 * Cada frase sobre o produto foi conferida contra o que ele faz hoje (o cartão tem escopo de
 * contato, de condições e validade; a correção é sucessão; o acesso é revogável).
 */

/** Uma foto com as dimensões dela, para o navegador reservar o espaço antes de carregar. */
export type Foto = { src: string; largura: number; altura: number };

const foto = (arquivo: string, largura: number, altura: number): Foto => ({
  src: `/conheca/${arquivo}`,
  largura,
  altura,
});

export const FOTOS = {
  pipoca: foto("dog-in-yedikule-animal-shelter.jpg", 1920, 2560),
  mia: foto("tabby-cat-with-blue-eyes-3336579.jpg", 1920, 2306),
  veterinaria: foto("close-up-of-a-veterinarian-examining-a.jpg", 1920, 2876),
  petshop: foto("grooming-poodle.jpg", 1920, 1280),
  gatoDoRodape: foto("british-shorthair-img-9095.jpg", 1280, 1520),
};

export const CAPA = {
  titulo: ["A vida dele", "fica com ele"],
  apoio:
    "Vacinas, consultas e tudo o que acontece com o seu animal, num registro que vai com ele a vida inteira.",
  acao: "Criar conta",
  altFoto: "Um cão branco e marrom, deitado de lado, sorrindo de olhos semicerrados, com uma coleira rosa.",
};

/** A mesma vida, mais adiante a cada mão. As datas andam para frente e as contagens fecham. */
export const MAOS = {
  titulo: "De mão em mão",
  animal: { nome: "Pipoca", descricao: "SRD · fêmea · cerca de 2 anos" },
  etapas: [
    {
      frase: "Primeiro, o abrigo que a resgatou.",
      responde: "Abrigo Patas da Vila",
      registros: [{ data: "14 mar 2024", texto: "Resgate", autor: "Abrigo Patas da Vila" }],
    },
    {
      frase: "Depois, um lar temporário.",
      responde: "Marcos Lima, lar temporário",
      registros: [
        { data: "20 abr 2024", texto: "Castração", autor: "Dra. Luíza Prado" },
        { data: "2 abr 2024", texto: "Vacina V10", autor: "Dra. Luíza Prado" },
        { data: "14 mar 2024", texto: "Resgate", autor: "Abrigo Patas da Vila" },
      ],
    },
    {
      frase: "Então, uma família.",
      responde: "Ana Ribeiro",
      registros: [
        { data: "3 ago 2024", texto: "Adoção: passa para Ana", autor: "Abrigo Patas da Vila" },
        { data: "20 abr 2024", texto: "Castração", autor: "Dra. Luíza Prado" },
        { data: "2 abr 2024", texto: "Vacina V10", autor: "Dra. Luíza Prado" },
        { data: "14 mar 2024", texto: "Resgate", autor: "Abrigo Patas da Vila" },
      ],
    },
    {
      frase: "E quem mais cuidar dela.",
      responde: "Ana Ribeiro",
      comAcesso: "Clínica Bom Pasto, Creche Quintal",
      registros: [
        { data: "12 set 2024", texto: "Consulta de rotina", autor: "Clínica Bom Pasto" },
        { data: "3 ago 2024", texto: "Adoção: passa para Ana", autor: "Abrigo Patas da Vila" },
        { data: "20 abr 2024", texto: "Castração", autor: "Dra. Luíza Prado" },
        { data: "2 abr 2024", texto: "Vacina V10", autor: "Dra. Luíza Prado" },
        { data: "14 mar 2024", texto: "Resgate", autor: "Abrigo Patas da Vila" },
      ],
    },
  ],
};

export const NADA_SE_APAGA = {
  titulo: "Nada se apaga",
  texto:
    "Errou um dado? A correção entra como um registro novo, com quem corrigiu e quando. O que estava lá continua à vista, porque um histórico de saúde só vale se ninguém puder reescrevê-lo.",
  altFoto: "Uma gata rajada de cinza e creme, de olhos azuis, olhando para a câmera.",
  animal: "Mia",
  original: { valor: "4,8 kg", autor: "Ana Ribeiro", data: "10 set 2025" },
  correcao: {
    valor: "4,2 kg",
    autor: "Dra. Luíza Prado",
    data: "13 set 2025",
    motivo: "Pesada na clínica. A balança de casa marcava a mais.",
  },
  opcoes: { original: "O registro", correcao: "A correção" },
};

export const ACESSO = {
  titulo: "Quem vê, você decide",
  pecas: {
    partes: {
      nome: "Por partes",
      linha: "A creche vê as vacinas. A consulta, não.",
      controle: "Carteira de vacinas",
      fora: "Consultas",
    },
    tempo: { nome: "Por um tempo", linha: "Todo acesso tem data para acabar.", controle: "até 30 nov" },
    pessoa: { nome: "Por pessoa", linha: "Cada um com o seu acesso.", iniciais: ["AR", "LP", "CQ"] },
    rastro: { nome: "Com rastro", linha: "Você vê quem leu o quê.", controle: ["Leu a carteira", "ontem, 18h40"] },
    agora: { nome: "Na hora", linha: "Tirou o acesso, acabou.", controle: "Tirar o acesso" },
  },
};

export const CARTAO = {
  titulo: "O cartão da coleira",
  texto:
    "Um QR na medalha. Quem achar a Pipoca lê com o celular e vê só o que você escolheu, sem conta e sem aplicativo.",
  controlesRotulo: "O que aparece no cartão",
  /** As partes que o cartão real sabe mostrar: os escopos CONDICOES, CARTEIRA e CONTATO. */
  partes: [
    { id: "condicoes", nome: "Alergias e condições" },
    { id: "vacinas", nome: "Vacinas" },
    { id: "telefone", nome: "Telefone" },
  ],
  previa: {
    nome: "Pipoca",
    descricao: "SRD · fêmea · cerca de 2 anos",
    alerta: "Alergia a frango",
    cuidado: "Dieta sem frango, ração hipoalergênica",
    vacinasTitulo: "Vacinas",
    vacinas: [
      { nome: "V10", situacao: "em dia, até abr 2026" },
      { nome: "Antirrábica", situacao: "em dia, até mai 2026" },
    ],
    contato: { texto: "Ligar para a Ana", tipo: "Tutora" },
    soNome: "Este cartão mostra só o nome. Você escolheu não mostrar mais nada.",
    validade: "Vale até 31 dez 2026",
  },
} as const;

/**
 * Uma semana da Pipoca, contada pelo próprio registro: cada entrada assinada por uma mão
 * diferente, com o que essa mão alcança. Continua a história da página: a Ana adotou, a Clínica
 * Bom Pasto e a Creche Quintal têm acesso, e o reforço da V10 é o que deixa a vacina "em dia até
 * abr 2026" no cartão da coleira.
 */
export const QUEM_CUIDA = {
  titulo: "Quem cuida junto",
  texto: "É o mesmo registro para todos. Cada um assina o que faz e só vê a parte que você deu.",
  semana: "A semana de 7 a 12 de abril de 2025 da Pipoca",
  entradas: [
    {
      autor: "Creche Quintal",
      iniciais: "CQ",
      papel: "Creche",
      alcance: "vê rotina e recados",
      quando: "seg, 7 abr · 17h40",
      tipo: "Recado do dia",
      texto: "Brincou a tarde toda com a turma do quintal e comeu tudo. Chegou um pouco agitada, saiu tranquila.",
    },
    {
      autor: "Dra. Luíza Prado",
      foto: FOTOS.veterinaria,
      papel: "Clínica Bom Pasto",
      alcance: "vê carteira e consultas",
      quando: "qua, 9 abr · 10h15",
      tipo: "Vacina",
      texto: "V10, reforço anual. Próxima dose em abril de 2026.",
      assinatura: "Assinado com o CRMV",
    },
    {
      autor: "Petshop Vila Nova",
      foto: FOTOS.petshop,
      papel: "Banho e tosa",
      alcance: "vê alergias e recados",
      quando: "sex, 11 abr · 9h00",
      tipo: "Banho",
      texto: "Xampu neutro, sem perfume. Por causa da alergia a frango, não demos o petisco da casa.",
    },
    {
      autor: "Ana Ribeiro",
      iniciais: "AR",
      papel: "Tutora",
      alcance: "responde pela Pipoca",
      quando: "sáb, 12 abr · 20h05",
      tipo: "Peso",
      texto: "14,2 kg, pesada em casa.",
      anexo: { foto: FOTOS.pipoca, alt: "A Pipoca deitada de lado, sorrindo, com a coleira rosa." },
    },
  ],
};

export const COMECO = {
  titulo: "Comece o registro",
  antes: "de",
  rotulo: "Nome do seu animal",
  dica: "nome dele",
  acao: "agora",
  nota: (nome: string) =>
    `Primeiro a sua conta. O registro ${nome ? `de ${nome}` : "dele"} vem logo em seguida.`,
};

export const RODAPE = {
  foto: FOTOS.gatoDoRodape,
  altFoto: "Um gato cinza de pelo curto e olhos cor de mel, deitado, olhando para a câmera.",
  creditosRotulo: "Créditos das fotos",
  colunas: [
    { nome: "Petfy", links: [{ texto: "Criar conta", para: "/criar-conta" }, { texto: "Entrar", para: "/entrar" }] },
    { nome: "Ajuda", links: [{ texto: "Achei um animal", para: "/encontrado" }] },
    { nome: "Documentos", links: [{ texto: "Termos de uso", para: "/termos" }, { texto: "Privacidade", para: "/privacidade" }] },
  ],
  direitos: "© 2026 Petfy",
  creditos: [
    { titulo: "Dog in Yedikule Animal Shelter", autor: "Turkish delight2", licenca: "CC BY 4.0", url: "https://commons.wikimedia.org/wiki/File:Dog_in_Yedikule_Animal_Shelter.jpg" },
    { titulo: "Tabby cat with blue eyes", autor: "AdinaVoicu", licenca: "CC0", url: "https://commons.wikimedia.org/wiki/File:Tabby_cat_with_blue_eyes-3336579.jpg" },
    { titulo: "Veterinarian examining a maltese dog", autor: "Shixart1985", licenca: "CC BY 2.0", url: "https://commons.wikimedia.org/wiki/File:Close-up_of_a_veterinarian_examining_a_maltese_dog.jpg" },
    { titulo: "Grooming Poodle", autor: "daveynin", licenca: "CC BY 2.0", url: "https://commons.wikimedia.org/wiki/File:Grooming_Poodle.jpg" },
    { titulo: "British Shorthair IMG 9095", autor: "W2k2", licenca: "CC BY 4.0", url: "https://commons.wikimedia.org/wiki/File:British_Shorthair_IMG_9095.jpg" },
  ],
};
