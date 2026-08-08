import type { components } from "./gerado/api";

type LoginResponseDTO = components["schemas"]["LoginResponseDTO"];

/**
 * A sessao, e ela mora aqui dentro — nenhuma tela le token.
 *
 * <b>O token vive em memoria</b>, e `localStorage` esta fora: e a exposicao a XSS que o
 * ROADMAP.md recusa por escrito. O custo aceito e login a cada recarga da pagina.
 *
 * <b>O `ensureFresh` e a peca que torna o custo baixo.</b> Hoje ele desloga; no dia em
 * que o refresh em cookie httpOnly entrar, ele passa a chamar `/auth/refresh`. Nesse dia
 * muda este arquivo, e nenhuma tela — que e a razao de a sessao morar numa camada so.
 * O gatilho da troca esta no ROADMAP.md: o dominio proprio pondo front e API sob o mesmo
 * site, ou o login a cada recarga se mostrar insuportavel no uso real.
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

  encerrarSessao("expirada");
  return null;
}

export function organizacaoAtiva(): string | null {
  return sessao?.organizacaoAtiva ?? null;
}
