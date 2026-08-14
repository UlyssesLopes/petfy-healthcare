import { API } from "./endereco.ts";
import type { components } from "./gerado/api";

type LoginResponseDTO = components["schemas"]["LoginResponseDTO"];

/**
 * A sessao, e ela mora aqui dentro — nenhuma tela le token.
 *
 * <b>O token continua vivendo em memoria</b>, e `localStorage` continua fora: e a exposicao a
 * XSS que o ROADMAP.md recusa por escrito.
 *
 * <b>O QUE MUDOU NA V51: o custo daquela decisao deixou de ser o login a cada recarga.</b>
 * Estava escrito aqui que o `ensureFresh` "hoje desloga; no dia em que o refresh em cookie
 * httpOnly entrar, ele passa a chamar `/auth/refresh`. Nesse dia muda este arquivo, e nenhuma
 * tela". Foi o que aconteceu — e o gatilho previsto tambem: "o login a cada recarga se mostrar
 * insuportavel no uso real". Mostrou-se, no uso real, nesta mesma sessao de trabalho.
 *
 * <b>O refresh nao mora aqui, e esse e o ponto.</b> Ele esta num cookie httpOnly que este
 * arquivo nao le e nao pode ler — nem ele, nem um script injetado. O que este arquivo faz e
 * pedir ao servidor que o troque por um JWT novo.
 */

/** Antes do fim, e nao no fim: requisicao que sai perto do limite volta 401. */
const MARGEM_DE_SEGURANCA_MS = 30_000;

export type MotivoDeEncerramento =
  /** O prazo do token acabou. */
  | "expirada"
  /**
   * O servidor recusou um token que ainda nao tinha expirado. A troca de senha invalida
   * o token por `iat`, entao este caso e esperado e nao e erro — e a mensagem para quem
   * esta lendo a tela e outra.
   */
  | "invalidada"
  /** A pessoa saiu. */
  | "saida";

interface Sessao {
  token: string;
  personId: string;
  profissional: boolean;
  /** Epoch em ms, derivado do `expiresInMinutes` que o login devolve. */
  expiraEm: number;
  /**
   * O contexto ativo (PRODUTO.md 9.5): agir em nome de uma organizacao e algo que a
   * pessoa escolheu ANTES, e nao a cada operacao. Vai em `X-Petfy-Organization`.
   */
  organizacaoAtiva: string | null;
}

export interface EstadoDaSessao {
  autenticada: boolean;
  personId: string | null;
  profissional: boolean;
  organizacaoAtiva: string | null;
  ultimoEncerramento: MotivoDeEncerramento | null;
}

let sessao: Sessao | null = null;
let ultimoEncerramento: MotivoDeEncerramento | null = null;

const ouvintes = new Set<() => void>();

/**
 * Recriado a cada mudanca, e so a cada mudanca: `useSyncExternalStore` compara por
 * identidade, e devolver um objeto novo a cada leitura daria laco infinito.
 */
let instantaneo: EstadoDaSessao = calcularInstantaneo();

function calcularInstantaneo(): EstadoDaSessao {
  return {
    autenticada: sessao !== null,
    personId: sessao?.personId ?? null,
    profissional: sessao?.profissional ?? false,
    organizacaoAtiva: sessao?.organizacaoAtiva ?? null,
    ultimoEncerramento,
  };
}

function avisar() {
  instantaneo = calcularInstantaneo();
  for (const ouvinte of ouvintes) {
    ouvinte();
  }
}

export function assinarSessao(ouvinte: () => void): () => void {
  ouvintes.add(ouvinte);
  return () => {
    ouvintes.delete(ouvinte);
  };
}

export function lerSessao(): EstadoDaSessao {
  return instantaneo;
}

export function iniciarSessao(resposta: LoginResponseDTO): void {
  if (!resposta.token || !resposta.personId || resposta.expiresInMinutes === undefined) {
    throw new Error("O login respondeu sem token, pessoa ou prazo.");
  }

  sessao = {
    token: resposta.token,
    personId: resposta.personId,
    profissional: resposta.professional ?? false,
    expiraEm: Date.now() + resposta.expiresInMinutes * 60_000,
    organizacaoAtiva: null,
  };
  ultimoEncerramento = null;
  avisar();
}

export function encerrarSessao(motivo: MotivoDeEncerramento): void {
  if (sessao === null && ultimoEncerramento === motivo) {
    return;
  }

  sessao = null;
  ultimoEncerramento = motivo;
  avisar();
}

/**
 * Trocar de contexto e explicito e visivel (PRODUTO.md 9.3), e por isso passa por aqui
 * em vez de ser parametro de chamada: o registro guarda em nome de quem foi feito, e
 * contexto nao e editavel depois.
 */
export function definirOrganizacaoAtiva(organizationId: string | null): void {
  if (sessao === null || sessao.organizacaoAtiva === organizationId) {
    return;
  }

  sessao = { ...sessao, organizacaoAtiva: organizationId };
  avisar();
}

/**
 * O ponto unico por onde o token sai para a rede.
 *
 * Devolve o token quando ele ainda vale, e `null` quando nao ha sessao ou quando ela
 * acabou de morrer. <b>E aqui que o `/auth/refresh` entra</b> quando o cookie httpOnly
 * existir: no lugar do `encerrarSessao("expirada")`.
 *
 * `async` desde ja, de proposito: o dia da troca nao pode obrigar toda a cadeia de
 * chamada a virar assincrona de uma vez.
 */
export async function ensureFresh(): Promise<string | null> {
  if (sessao === null) {
    return null;
  }

  if (Date.now() < sessao.expiraEm - MARGEM_DE_SEGURANCA_MS) {
    return sessao.token;
  }

  /*
   * AQUI ESTAVA O `encerrarSessao("expirada")`, e este comentario prometia a troca.
   *
   * O token venceu, e em vez de derrubar a pessoa o cliente pede um novo com o refresh do
   * cookie. Se o servidor recusar — sessao encerrada na Tela 36, senha trocada, ou trinta dias
   * sem usar —, ai sim a sessao acaba, e o motivo continua sendo "expirada": do ponto de vista
   * de quem esta lendo a tela, o prazo acabou.
   */
  return await renovar("expirada");
}

/**
 * Uma renovacao por vez, e as outras esperam a mesma.
 *
 * <b>Sem isto, uma tela com cinco consultas paralelas dispara cinco refresh no mesmo instante</b>
 * — e como cada renovacao ROTACIONA o token, quatro delas apresentariam um refresh que a
 * primeira acabou de invalidar. O resultado seria deslogar exatamente quem esta usando o produto
 * com mais intensidade.
 */
let renovacaoEmCurso: Promise<string | null> | null = null;

function renovar(motivoSeFalhar: MotivoDeEncerramento): Promise<string | null> {
  if (renovacaoEmCurso !== null) {
    return renovacaoEmCurso;
  }

  renovacaoEmCurso = pedirTokenNovo(motivoSeFalhar).finally(() => {
    renovacaoEmCurso = null;
  });

  return renovacaoEmCurso;
}

/**
 * <b>`fetch` cru, e nao o `cliente`.</b> O cliente passa por este arquivo para pegar o token —
 * chama-lo daqui fecharia um ciclo, e o middleware dele tentaria renovar durante a renovacao.
 */
async function pedirTokenNovo(motivoSeFalhar: MotivoDeEncerramento): Promise<string | null> {
  try {
    const resposta = await fetch(`${API}/auth/refresh`, {
      method: "POST",
      // o cookie httpOnly so viaja com isto, e ele e a credencial desta chamada
      credentials: "include",
    });

    if (!resposta.ok) {
      encerrarSessao(motivoSeFalhar);
      return null;
    }

    const corpo = (await resposta.json()) as LoginResponseDTO;
    iniciarSessao(corpo);

    return sessao?.token ?? null;
  } catch {
    /*
     * Rede fora nao e sessao encerrada. Derrubar aqui faria um tunel de metro deslogar a pessoa,
     * e o cookie continua valendo do outro lado — a proxima tentativa reencontra a sessao.
     */
    return null;
  }
}

/**
 * Tenta recuperar a sessao ao abrir o app.
 *
 * <b>E o que faz a recarga deixar de deslogar.</b> A memoria comeca vazia a cada carregamento;
 * esta chamada pergunta ao servidor se o cookie ainda vale. Devolve `false` quando nao ha
 * sessao a recuperar, e ai o `beforeLoad` das rotas manda para o `/entrar` como sempre mandou.
 *
 * <b>Nao encerra a sessao ao falhar</b>, e a diferenca importa: quem abre o app pela primeira
 * vez nao tem sessao para "perder", e escrever "sua sessao expirou" na tela de entrada de quem
 * nunca entrou seria mentira.
 */
export async function recuperarSessao(): Promise<boolean> {
  if (sessao !== null) {
    return true;
  }

  try {
    const resposta = await fetch(`${API}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    });

    if (!resposta.ok) {
      return false;
    }

    iniciarSessao((await resposta.json()) as LoginResponseDTO);
    return true;
  } catch {
    return false;
  }
}

/**
 * Avisa o servidor que esta entrada acabou.
 *
 * <b>Sair passou a ter efeito do outro lado.</b> Antes o cliente esquecia o token e pronto: o
 * JWT seguia valido ate expirar, e o refresh nem existia. Agora a entrada e encerrada e o cookie
 * apagado — sair num computador emprestado passou a significar o que a palavra diz.
 */
export async function encerrarNoServidor(): Promise<void> {
  try {
    await fetch(`${API}/auth/logout`, { method: "POST", credentials: "include" });
  } catch {
    // sair localmente nao pode depender da rede: quem clicou em sair sai, e o cookie expira
  }
}

export function organizacaoAtiva(): string | null {
  return sessao?.organizacaoAtiva ?? null;
}
