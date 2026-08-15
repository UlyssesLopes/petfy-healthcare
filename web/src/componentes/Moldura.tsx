import { useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAvisosNaoLidos } from "../dados/avisos.ts";
import { useSair } from "../dados/autenticacao.ts";
import { useMeuContexto, type MeuContexto } from "../dados/contexto.ts";
import { useReenviarVerificacao } from "../dados/verificacao.ts";
import { definirOrganizacaoAtiva, lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A moldura de toda tela, de `design/IdentidadeVisual/Petfy - Header e Footer.dc.html`.
 *
 * <b>Por que ela existe.</b> O desenho abre dizendo o diagnostico: "o que eu vinha desenhando
 * era um cabecalho por tela — parecido, nunca igual. E isso que da a sensacao de falta na
 * implementacao". Onze rotas desenhavam a propria marca a mao, cada uma com a sua medida, e o
 * produto parecia um conjunto de paginas soltas em vez de uma plataforma.
 *
 * <b>Ela mora na raiz, e nao em cada rota</b>, porque foi exatamente "cada rota se lembra de
 * repetir" que produziu onze cabecalhos diferentes. A rota que quiser sair do padrao declara
 * isso aqui, num lugar so, e nao esquecendo de incluir.
 *
 * <b>AS TRES VARIANTES SAO DO DESENHO, e nao invencao de implementacao</b> (secao 04):
 *
 *   · COMPLETA  — toda tela autenticada: area do tutor, area de organizacao, administracao.
 *   · REDUZIDA  — login e criacao de conta ficam "com a marca no lugar do cabecalho e uma
 *                 linha so de rodape". O stepper do primeiro animal mantem a marca e o
 *                 "fazer isso depois", sem navegacao: sair no meio e botao, nao link de menu.
 *   · MINIMA    — o cartao de emergencia e as telas publicas. "Sem sessao, sem navegacao, sem
 *                 rodape institucional entre o dedo e o telefone do veterinario."
 *
 * <b>DUAS AFORDANCIAS DO CABECALHO NAO TEM BACKEND, e ficam desabilitadas com o motivo</b> —
 * a secao 06 da identidade manda: "o desabilitado nunca aparece mudo — ao lado dele, sempre a
 * frase que diz por que". Sao a busca (nao ha rota de busca, e RGA nem existe como campo) e o
 * marcador de avisos (nao ha canal de aviso nenhum). Um marcador que nunca acende seria pior
 * que a ausencia dele: ele ensinaria a pessoa a nao olhar.
 */

/** As portas. Marca no lugar do cabecalho, e uma linha so de rodape. */
const PORTAS = ["/entrar", "/criar-conta"];

/**
 * O stepper do primeiro animal: marca, sem navegacao — "sair no meio e botao, nao link de menu".
 *
 * Sao duas rotas porque o percurso e um so: `/comecar` e o "depois do login" e `/animais/novo` e
 * o cadastro em quatro passos que ele abre. Dar navegacao a essas telas seria oferecer saidas
 * laterais no meio de um preenchimento — e o "fazer isso depois" que elas ja tem e a saida certa,
 * porque leva a pessoa para o produto em vez de abandona-la num formulario pela metade.
 */
const SEM_NAVEGACAO = ["/comecar", "/animais/novo"];

/**
 * As telas publicas, lidas por quem nao tem conta (Tela 34).
 *
 * <b>Lista propria e nao um item em PORTAS</b>, porque as duas coisas se parecem e nao sao a mesma:
 * uma porta e o comeco de uma sessao, e esta tela nao leva a sessao nenhuma — quem digita um
 * microchip as 23h com um animal no colo nao esta a caminho de criar conta.
 *
 * O efeito e a moldura reduzida, e ela serve pela mesma razao das portas: a tela desenha a
 * propria marca dentro do cartao.
 */
const PUBLICAS = ["/encontrado", "/termos", "/privacidade"];

/**
 * O cartao de emergencia, e a variante MINIMA do desenho.
 *
 * "Quem abre o cartao nao tem sessao, nao tem para onde navegar e esta com pressa. Cabecalho com
 * 'Agindo como' nao faz sentido, e um rodape de quatro colunas seria uma parede entre o dedo e o
 * telefone do veterinario."
 *
 * <b>Prefixo, e nao caminho exato</b>, porque o token viaja na URL.
 */
const MINIMAS = ["/cartao/"];

type Variante = "completa" | "reduzida" | "minima";

export function Moldura({ children }: { children: ReactNode }) {
  const caminho = useRouterState({ select: (estado) => estado.location.pathname });

  const variante: Variante = (() => {
    if (MINIMAS.some((prefixo) => caminho.startsWith(prefixo))) {
      return "minima";
    }
    if (PORTAS.includes(caminho) || SEM_NAVEGACAO.includes(caminho) || PUBLICAS.includes(caminho)) {
      return "reduzida";
    }
    return "completa";
  })();

  /* Nada entre quem le e o telefone: sem cabecalho, sem rodape. A propria tela diz quem gerou o
     cartao, ate quando ele vale e o que nao esta ali. */
  if (variante === "minima") {
    return <div style={{ minHeight: "100dvh" }}>{children}</div>;
  }

  /* As portas desenham a propria marca dentro do cartao, e o desenho delas ja e a moldura
     reduzida. Repetir a marca aqui daria duas. */
  if (variante === "reduzida") {
    return (
      <div style={{ minHeight: "100dvh", display: "flex", flexDirection: "column" }}>
        <div style={{ flex: 1 }}>{children}</div>
        <RodapeDeUmaLinha />
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100dvh", display: "flex", flexDirection: "column", background: "oklch(0.985 0.004 120)" }}>
      <Cabecalho />
      {/*
       * AS FAIXAS FICAM FORA DO CABECALHO, e a diferenca nao e de organizacao de codigo.
       *
       * Elas nasceram dentro do `<header>` sticky, e la herdavam o fundo branco e a borda dele —
       * pareciam parte do chrome e acompanhavam a rolagem, como se fossem estrutura permanente
       * da tela. Uma faixa nao e estrutura: ela e um recado que sai quando for resolvido, e ficar
       * grudada no topo enquanto a pessoa rola dava a ela um peso que o recado nao tem.
       *
       * <b>Continuam sendo faixas DO CABECALHO no sentido que o desenho da</b> ("faixas do
       * cabecalho · abaixo dos 64 px, empilhaveis"): logo abaixo dele, empilhaveis, e sempre
       * aviso de SESSAO — nunca de animal. Pendencia de saude vive no feed, e se subir para ca o
       * produto vira cobrador. O que mudou foi so onde elas pousam.
       */}
      <Faixas />
      <div style={{ flex: 1 }}>{children}</div>
      <Rodape />
      <BarraDeBaixo />
    </div>
  );
}

/**
 * O celular tem outra moldura, e nao a mesma encolhida.
 *
 * `matchMedia` e nao media query pelo mesmo motivo da Tela 29: o estilo aqui e inline,
 * convertido do arquivo do desenho, e `style` nao aceita `@media`.
 */
function useEstreito() {
  const [estreito, setEstreito] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(max-width: 860px)").matches,
  );

  useEffect(() => {
    const consulta = window.matchMedia("(max-width: 860px)");
    const ouvir = (evento: MediaQueryListEvent) => setEstreito(evento.matches);

    consulta.addEventListener("change", ouvir);
    return () => consulta.removeEventListener("change", ouvir);
  }, []);

  return estreito;
}

/* ------------------------------------------------------------------------- o cabecalho */

function Cabecalho() {
  const estreito = useEstreito();
  const contexto = useMeuContexto();

  /* Sem sessao nao ha cabecalho de sessao: o `/me/context` responderia 401, e um cabecalho
     dizendo "Agindo como —" e pior que nenhum. */
  if (!lerSessao().autenticada) {
    return null;
  }

  return (
    <header style={{ background: "oklch(1 0 0)", borderBottom: "1px solid oklch(0.90 0.008 150)", position: "sticky", top: 0, zIndex: 20 }}>
      {estreito ? <CabecalhoEstreito contexto={contexto.data} /> : <CabecalhoLargo contexto={contexto.data} />}
    </header>
  );
}

function CabecalhoLargo({ contexto }: { contexto?: MeuContexto }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "auto 1fr auto", gap: "32px", alignItems: "center", padding: "0 28px", height: "64px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "28px" }}>
        <Marca />
        <AgindoComo contexto={contexto} />
      </div>

      <nav style={{ display: "flex", alignItems: "center", gap: "4px", justifySelf: "center" }}>
        <Destinos contexto={contexto} />
      </nav>

      <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
        <Busca />
        <Avisos />
        <Voce contexto={contexto} />
      </div>
    </div>
  );
}

/**
 * No celular o cabecalho guarda so marca, avisos e voce. Os destinos descem para a barra
 * inferior, onde o polegar alcanca.
 *
 * <b>O "Agindo como" nao desaparece</b>, e o desenho diz por que: "um monitor que registra
 * achando que esta como pessoa fisica e um problema de responsabilidade, nao de interface".
 * Ele sai do cabecalho e vira a primeira linha do menu do avatar.
 */
function CabecalhoEstreito({ contexto }: { contexto?: MeuContexto }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "12px", padding: "0 18px", height: "56px" }}>
      <Marca compacta />
      <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "12px" }}>
        <Avisos compacto />
        <Voce contexto={contexto} />
      </div>
    </div>
  );
}

function Marca({ compacta = false }: { compacta?: boolean }) {
  const anel = compacta ? 24 : 26;

  return (
    <Link to="/" style={{ textDecoration: "none", display: "flex", alignItems: "center", gap: "10px", flex: "none" }}>
      <div aria-hidden style={{ width: `${anel}px`, height: `${anel}px`, borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div style={{ width: compacta ? "7px" : "8px", height: compacta ? "7px" : "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
      </div>
      <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: compacta ? "16px" : "17px", fontWeight: 600, color: "oklch(0.25 0.02 150)" }}>
        Petfy
      </span>
    </Link>
  );
}

/**
 * O seletor "Agindo como" — <b>a peca mais importante do cabecalho</b>, e o desenho diz por
 * que: "a mesma conta atende como pessoa e em nome de organizacoes, e tudo que se registra leva
 * esse nome".
 *
 * <b>Em contexto de organizacao ele ganha contorno musgo e diz "pela".</b> "E a diferenca visual
 * entre registrar como pessoa e registrar em nome de alguem — e ela nunca e sutil."
 */
function AgindoComo({ contexto }: { contexto?: MeuContexto }) {
  const intl = useIntl();
  const consultas = useQueryClient();
  const [aberto, setAberto] = useState(false);
  const caixa = useRef<HTMLDivElement>(null);

  useFechaAoClicarFora(caixa, () => setAberto(false));

  /**
   * Trocar de contexto invalida TODO o cache, e nao so o contexto.
   *
   * O `X-Petfy-Organization` viaja em toda requisicao, entao praticamente toda resposta ja
   * guardada foi lida em nome de outra organizacao. Invalidar so o contexto deixaria a tela
   * dizendo "pela Creche Quintal" no cabecalho enquanto lista os pacientes da clinica anterior
   * — e ninguem olharia duas vezes para perceber.
   */
  const trocarPara = (organizationId: string | null) => {
    definirOrganizacaoAtiva(organizationId);
    void consultas.invalidateQueries();
    setAberto(false);
  };

  if (contexto === undefined) {
    return null;
  }

  const ativo = contexto.active;
  const naOrganizacao = ativo?.kind === "ORGANIZACAO";
  const opcoes = contexto.available ?? [];

  /* Ambiguo e um estado do backend, e nao um erro: com mais de um vinculo e sem header, ele se
     recusa a escolher em silencio. O seletor e exatamente onde essa escolha cabe. */
  const precisaEscolher = contexto.ambiguous === true;

  return (
    <div ref={caixa} style={{ position: "relative" }}>
      <button
        type="button"
        onClick={() => setAberto((antes) => !antes)}
        aria-expanded={aberto}
        style={{
          fontFamily: "inherit",
          display: "flex",
          alignItems: "center",
          gap: "10px",
          border: `1px solid ${naOrganizacao || precisaEscolher ? "oklch(0.46 0.085 150)" : "oklch(0.86 0.008 150)"}`,
          borderRadius: "8px",
          padding: "0 14px",
          height: "44px",
          background: naOrganizacao || precisaEscolher ? "oklch(0.96 0.02 150)" : "oklch(0.975 0.004 150)",
          cursor: "pointer",
        }}
      >
        <span style={{ fontSize: "13px", color: naOrganizacao ? "oklch(0.42 0.05 150)" : "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "moldura.agindoComo" })}
        </span>
        <span style={{ fontSize: "15px", fontWeight: 500 }}>{contexto.personName}</span>
        <span style={{ fontSize: "13px", color: naOrganizacao ? "oklch(0.42 0.05 150)" : "oklch(0.5 0.015 150)" }}>
          {precisaEscolher
            ? intl.formatMessage({ id: "moldura.agindoComo.escolha" })
            : naOrganizacao
              ? intl.formatMessage({ id: "moldura.agindoComo.pela" }, { organizacao: ativo?.organizationName ?? "" })
              : intl.formatMessage({ id: "moldura.agindoComo.voceMesmo" })}
        </span>
        <span aria-hidden style={{ width: "7px", height: "7px", borderRight: "1.5px solid oklch(0.45 0.015 150)", borderBottom: "1.5px solid oklch(0.45 0.015 150)", transform: "rotate(45deg)", marginLeft: "4px", marginTop: "-3px" }}></span>
      </button>

      {aberto && (
        <div style={{ position: "absolute", top: "52px", left: 0, minWidth: "280px", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", boxShadow: "0 8px 24px oklch(0.2 0.02 150 / 0.10)", padding: "6px", zIndex: 30 }}>
          {opcoes.map((opcao) => {
            const eOAtivo =
              opcao.kind === ativo?.kind && opcao.organizationId === ativo?.organizationId;

            return (
              <button
                key={`${opcao.kind}-${opcao.organizationId ?? "pessoa"}`}
                type="button"
                onClick={() => trocarPara(opcao.organizationId ?? null)}
                style={{ fontFamily: "inherit", display: "block", width: "100%", textAlign: "left", border: "none", background: eOAtivo ? "oklch(0.94 0.02 150)" : "transparent", borderRadius: "6px", padding: "11px 13px", minHeight: "44px", cursor: "pointer", fontSize: "15px" }}
              >
                {opcao.kind === "ORGANIZACAO" ? (
                  <>
                    <div style={{ fontWeight: 500 }}>{opcao.organizationName}</div>
                    {opcao.role !== undefined && (
                      <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                        {intl.formatMessage({ id: `equipe.funcao.${opcao.role}` })}
                      </div>
                    )}
                  </>
                ) : (
                  <div style={{ fontWeight: 500 }}>{intl.formatMessage({ id: "moldura.contexto.pessoa" })}</div>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

/**
 * No maximo quatro destinos, e eles mudam conforme o contexto ativo. "Nunca mais de quatro: o
 * produto nao e um portal."
 *
 * <b>So entra destino que existe.</b> O desenho mostra "Meus animais" e "Quem cuida" como
 * destinos proprios, e nenhum dos dois tem rota: a home ja lista os animais, e "quem cuida" e
 * por animal. Um item de menu que leva a lugar nenhum e pior que a ausencia dele.
 */
function Destinos({ contexto }: { contexto?: MeuContexto }) {
  const intl = useIntl();
  const caminho = useRouterState({ select: (estado) => estado.location.pathname });

  const ativo = contexto?.active;
  const capacidades = ativo?.capabilities ?? [];
  const naOrganizacao = ativo?.kind === "ORGANIZACAO";

  const destinos: { para: string; rotulo: string; params?: Record<string, string> }[] = [
    { para: "/", rotulo: intl.formatMessage({ id: "moldura.destino.hoje" }) },
  ];

  if (naOrganizacao && capacidades.includes("REGISTRAR_ATO_CLINICO")) {
    destinos.push({ para: "/pacientes", rotulo: intl.formatMessage({ id: "moldura.destino.pacientes" }) });
  }

  if (naOrganizacao && capacidades.includes("GERIR_TURMA_E_VAGA")) {
    destinos.push({ para: "/creche", rotulo: intl.formatMessage({ id: "moldura.destino.creche" }) });
  }

  if (naOrganizacao && ativo?.organizationId !== undefined) {
    destinos.push({
      para: "/organizacoes/$organizationId/equipe",
      rotulo: intl.formatMessage({ id: "moldura.destino.equipe" }),
      params: { organizationId: ativo.organizationId },
    });
  }

  return (
    <>
      {destinos.map((destino) => {
        const aqui =
          destino.params === undefined
            ? caminho === destino.para
            : caminho.startsWith("/organizacoes/") && caminho.endsWith("/equipe");

        return (
          <Link
            key={destino.para}
            to={destino.para}
            params={destino.params}
            style={{ display: "flex", alignItems: "center", padding: "0 14px", height: "44px", borderRadius: "8px", fontSize: "15px", textDecoration: "none", fontWeight: aqui ? 500 : 400, background: aqui ? "oklch(0.94 0.02 150)" : "transparent", color: aqui ? "oklch(0.38 0.07 150)" : "oklch(0.42 0.015 150)" }}
          >
            {destino.rotulo}
          </Link>
        );
      })}
    </>
  );
}

/**
 * A busca do desenho e "por nome, microchip ou RGA", e <b>nao ha rota de busca nenhuma</b> —
 * nem campo de RGA no modelo. Ela fica desabilitada com o motivo ao lado, que e o que a secao
 * 06 da identidade manda fazer com afordancia sem backend.
 */
function Busca() {
  const intl = useIntl();

  return (
    <div
      title={intl.formatMessage({ id: "moldura.busca.indisponivel" })}
      aria-disabled
      style={{ display: "flex", alignItems: "center", gap: "9px", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "8px", padding: "0 14px", height: "44px", width: "200px", color: "oklch(0.62 0.012 150)", fontSize: "15px", cursor: "not-allowed", background: "oklch(0.98 0.004 150)" }}
    >
      <div aria-hidden style={{ width: "13px", height: "13px", borderRadius: "999px", border: "2px solid oklch(0.72 0.012 150)", flex: "none" }}></div>
      {intl.formatMessage({ id: "moldura.busca" })}
    </div>
  );
}

/**
 * O marcador de avisos, e ele <b>ACENDE</b> desde a V48.
 *
 * Aqui estava escrito: "nao ha canal de aviso nenhum no produto, entao nao ha o que acender — um
 * marcador que nunca muda ensinaria a pessoa a nao olhar para ele, e um que sempre acende seria
 * mentira". As duas frases continuam verdadeiras, e por isso o marcador so existe agora que ha um
 * canal: o ponto acende quando ha aviso por ler, e apaga quando nao ha.
 *
 * <b>Circulo cheio, e nao losango.</b> O desenho manda usar "as mesmas formas de estado do sistema
 * — losango so quando algo venceu", e um aviso nao e um vencimento: "Ana entrou no Code" nao pede
 * acao nem esta atrasado. Pintar de losango faria toda a linguagem de urgencia do produto perder o
 * sentido no dia em que uma vacina vencesse de verdade.
 *
 * <b>E nao ha numero dentro do ponto.</b> Vinte e tres avisos e um aviso pedem a mesma coisa —
 * abrir a lista —, e um contador transformaria uma marca em divida a zerar.
 */
function Avisos({ compacto = false }: { compacto?: boolean }) {
  const intl = useIntl();
  const lado = compacto ? 24 : 44;

  const naoLidos = useAvisosNaoLidos();
  const acende = (naoLidos.data ?? 0) > 0;

  return (
    <Link
      to="/avisos"
      title={intl.formatMessage({ id: acende ? "moldura.avisos.ha" : "moldura.avisos" })}
      aria-label={intl.formatMessage({ id: acende ? "moldura.avisos.ha" : "moldura.avisos" })}
      style={{ width: `${lado}px`, height: `${lado}px`, border: compacto ? "none" : "1px solid oklch(0.90 0.008 150)", borderRadius: "8px", display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none" }}
    >
      <div
        aria-hidden
        style={{
          width: "15px",
          height: "15px",
          borderRadius: "999px",
          border: `2px solid ${acende ? "oklch(0.46 0.085 150)" : "oklch(0.68 0.012 150)"}`,
          background: acende ? "oklch(0.46 0.085 150)" : "transparent",
        }}
      ></div>
    </Link>
  );
}

/** O avatar e o que ele guarda: no celular, o contexto ativo por extenso; sempre, a saida. */
function Voce({ contexto }: { contexto?: MeuContexto }) {
  const intl = useIntl();
  const estreito = useEstreito();
  const sair = useSair();
  const navegar = useNavigate();
  const [aberto, setAberto] = useState(false);
  const caixa = useRef<HTMLDivElement>(null);

  useFechaAoClicarFora(caixa, () => setAberto(false));

  const ativo = contexto?.active;
  const lado = estreito ? 30 : 36;

  return (
    <div ref={caixa} style={{ position: "relative" }}>
      <button
        type="button"
        onClick={() => setAberto((antes) => !antes)}
        aria-expanded={aberto}
        aria-label={intl.formatMessage({ id: "moldura.voce" })}
        style={{ width: `${lado}px`, height: `${lado}px`, borderRadius: "999px", background: "oklch(0.90 0.03 150)", border: "none", cursor: "pointer", padding: 0 }}
      ></button>

      {aberto && (
        <div style={{ position: "absolute", top: `${lado + 10}px`, right: 0, minWidth: "260px", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", boxShadow: "0 8px 24px oklch(0.2 0.02 150 / 0.10)", padding: "6px", zIndex: 30 }}>
          {/* No celular o "Agindo como" mora aqui, por extenso. Ele nao pode desaparecer: um
              monitor que registra achando que esta como pessoa fisica e um problema de
              responsabilidade, nao de interface. */}
          <div style={{ padding: "11px 13px", borderBottom: "1px solid oklch(0.94 0.006 150)", marginBottom: "4px" }}>
            <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "moldura.agindoComo" })}
            </div>
            <div style={{ fontSize: "15px", fontWeight: 500, marginTop: "3px" }}>
              {contexto?.personName ?? ""}
            </div>
            <div style={{ fontSize: "13px", color: "oklch(0.42 0.05 150)", marginTop: "2px" }}>
              {ativo?.kind === "ORGANIZACAO"
                ? intl.formatMessage({ id: "moldura.agindoComo.pela" }, { organizacao: ativo.organizationName ?? "" })
                : intl.formatMessage({ id: "moldura.contexto.pessoa" })}
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setAberto(false);
              sair();
              void navegar({ to: "/entrar" });
            }}
            style={{ fontFamily: "inherit", display: "block", width: "100%", textAlign: "left", border: "none", background: "transparent", borderRadius: "6px", padding: "11px 13px", minHeight: "44px", cursor: "pointer", fontSize: "15px" }}
          >
            {intl.formatMessage({ id: "moldura.sair" })}
          </button>
        </div>
      )}
    </div>
  );
}

/**
 * As faixas, abaixo dos 64 px e empilhaveis.
 *
 * <b>"Faixa e aviso de sessao, nunca de animal."</b> Pendencia de saude vive no feed — "se ela
 * subir para o cabecalho, o produto vira cobrador". Por isso a unica faixa aqui e a do e-mail
 * nao confirmado, que e sobre a conta, e nunca uma vacina vencida.
 */
function Faixas() {
  const intl = useIntl();
  const contexto = useMeuContexto();
  const reenviar = useReenviarVerificacao();

  /* Le o contexto por conta propria em vez de receber do cabecalho: ela deixou de ser filha
     dele. A consulta e a mesma chave do TanStack, entao nao ha uma segunda ida a rede. */
  const dados = contexto.data;

  /* Sem sessao nao ha faixa de sessao. E `!== false` e nao `=== false` invertido de proposito:
     enquanto o contexto nao chegou, `undefined` nao pode acender a faixa e faze-la piscar em
     toda navegacao para quem ja confirmou o e-mail ha meses. */
  if (!lerSessao().autenticada || dados === undefined || dados.emailVerified !== false) {
    return null;
  }

  return (
    <div style={{ padding: "12px 28px 0" }}>
      <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "8px", padding: "13px 16px", display: "flex", alignItems: "center", gap: "12px" }}>
        <div aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", border: "3px solid oklch(0.62 0.11 70)", flex: "none" }}></div>
        <span style={{ fontSize: "15px", flex: 1 }}>
          {intl.formatMessage({ id: "moldura.faixa.email" })}
        </span>
        <button
          type="button"
          disabled={reenviar.isPending || reenviar.isSuccess}
          onClick={() => reenviar.mutate()}
          style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.46 0.085 150)", background: "transparent", border: "none", cursor: reenviar.isSuccess ? "default" : "pointer", padding: "6px 4px", minHeight: "32px" }}
        >
          {intl.formatMessage({
            id: reenviar.isSuccess
              ? "moldura.faixa.email.enviado"
              : reenviar.isPending
                ? "moldura.faixa.email.enviando"
                : "moldura.faixa.email.reenviar",
          })}
        </button>
      </div>
    </div>
  );
}

/* --------------------------------------------------------------------------- o rodape */

/**
 * Quatro colunas e uma linha de fe.
 *
 * <b>"Seus dados" nao vai para dentro de configuracoes</b>, e o desenho escreve o porque: "num
 * produto que pede acesso a saude de um ser vivo, esses dois links sao a garantia — e garantia
 * escondida nao sossega ninguem".
 *
 * <b>Nao ha rodape institucional no celular</b> — "ele viraria uma parede de links acima da
 * barra de navegacao".
 *
 * Os destinos das colunas ainda nao existem como rotas. Eles ficam como texto, e nao como link
 * morto: um link que nao leva a lugar nenhum e a forma mais rapida de ensinar que os links
 * daqui nao valem — e justamente estes precisam valer.
 */
function Rodape() {
  const intl = useIntl();
  const estreito = useEstreito();

  if (estreito) {
    return null;
  }

  return (
    <footer style={{ background: "oklch(0.975 0.008 150)", borderTop: "1px solid oklch(0.90 0.008 150)", marginTop: "40px" }}>
      <div style={{ padding: "44px 40px 36px", maxWidth: "1360px", margin: "0 auto" }}>
        <div style={{ display: "grid", gridTemplateColumns: "1.5fr 1fr 1fr 1fr", gap: "40px", marginBottom: "40px" }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "11px", marginBottom: "16px" }}>
              <div aria-hidden style={{ width: "28px", height: "28px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 600 }}>Petfy</span>
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", maxWidth: "34ch" }}>
              {intl.formatMessage({ id: "moldura.rodape.tese" })}
            </div>
          </div>

          {/*
           * ------------------------------------------- O RODAPE LISTA O QUE ELE CONSEGUE ABRIR
           *
           * Os doze itens eram `<div>`: nenhum clicava, nas tres colunas. Um item de rodape que
           * nao vai a lugar nenhum e uma promessa quebrada repetida em toda pagina do produto —
           * e, pior, ensina que rodape aqui e enfeite, o que estraga tambem os que funcionam.
           *
           * <b>Saiu a coluna "O produto"</b> (quatro paginas institucionais que nao existem) e a
           * coluna "Ajuda" (central, contato, acessibilidade, status — nenhuma existe). Ficam
           * registradas no `ROADMAP.md`, e voltam quando tiverem destino.
           *
           * <b>Fica "Seus dados"</b>, que e a coluna que importa: "num produto que pede acesso a
           * saude de um ser vivo, esses links sao a garantia, e garantia escondida nao sossega
           * ninguem". Garantia que nao abre tambem nao sossega — entao ela lista os dois
           * documentos, que agora existem.
           */}
          <Coluna titulo={intl.formatMessage({ id: "moldura.rodape.dados" })}>
            <ItemDeRodape para="/privacidade">
              {intl.formatMessage({ id: "moldura.rodape.dados.privacidade" })}
            </ItemDeRodape>
            <ItemDeRodape para="/termos">
              {intl.formatMessage({ id: "moldura.rodape.dados.termos" })}
            </ItemDeRodape>
          </Coluna>
        </div>

        {/* A linha de fe. Ela nao e rodape juridico enfiado no fim: e o limite do produto,
            dito onde qualquer pessoa alcanca. */}
        <div style={{ borderTop: "1px solid oklch(0.90 0.008 150)", paddingTop: "26px", display: "grid", gridTemplateColumns: "auto 1fr", gap: "20px", alignItems: "start" }}>
          <div aria-hidden style={{ width: "13px", height: "13px", borderRadius: "999px", border: "3px solid oklch(0.46 0.085 150)", marginTop: "4px" }}></div>
          <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)", maxWidth: "96ch" }}>
            <strong style={{ fontWeight: 500 }}>{intl.formatMessage({ id: "moldura.rodape.fe.titulo" })}</strong>{" "}
            {intl.formatMessage({ id: "moldura.rodape.fe.texto" })}
          </div>
        </div>
      </div>

      <div style={{ padding: "18px 40px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "24px", maxWidth: "1360px", margin: "0 auto", borderTop: "1px solid oklch(0.90 0.008 150)" }}>
        <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "moldura.rodape.assinatura" })}
        </div>
        {/* "Encarregado de dados (LGPD)" saiu junto: o canal ainda nao existe, e a propria
            politica de privacidade declara que falta. Anunciar aqui um contato que nao atende e
            pior que nao anunciar — quem procura o encarregado esta exercendo um direito. */}
        <div style={{ display: "flex", alignItems: "center", gap: "22px", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
          <span style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px" }}>v1.0</span>
        </div>
      </div>
    </footer>
  );
}

/** As portas ficam com "uma linha so de rodape — termos, privacidade e ajuda". */
function RodapeDeUmaLinha() {
  const intl = useIntl();

  return (
    /*
     * TERMOS E PRIVACIDADE VIRARAM LINKS. Eram <span>, e este rodape existe justamente para
     * carrega-los: a porta pede que a pessoa aceite os dois, e ate agora nao havia como ler
     * nenhum.
     *
     * <b>A "Central de ajuda" saiu.</b> A pagina nao existe, e dois links que clicam ao lado de
     * um texto que nao clica parece defeito — alem de ensinar que rodape aqui e enfeite. Ela
     * volta no dia em que tiver para onde levar.
     */
    <footer style={{ padding: "20px 24px 28px", display: "flex", alignItems: "center", justifyContent: "center", gap: "20px", flexWrap: "wrap", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
      <Link to="/termos" style={{ color: "oklch(0.46 0.085 150)" }}>
        {intl.formatMessage({ id: "moldura.rodape.dados.termos" })}
      </Link>
      <Link to="/privacidade" style={{ color: "oklch(0.46 0.085 150)" }}>
        {intl.formatMessage({ id: "moldura.rodape.dados.privacidade" })}
      </Link>
    </footer>
  );
}

/** No celular os destinos descem para onde o polegar alcanca — tres, no maximo. */
function BarraDeBaixo() {
  const estreito = useEstreito();
  const contexto = useMeuContexto();

  if (!estreito || !lerSessao().autenticada) {
    return null;
  }

  return (
    <nav style={{ position: "sticky", bottom: 0, background: "oklch(1 0 0)", borderTop: "1px solid oklch(0.92 0.006 150)", display: "flex", zIndex: 20 }}>
      <div style={{ display: "flex", width: "100%", justifyContent: "space-around" }}>
        <Destinos contexto={contexto.data} />
      </div>
    </nav>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Coluna({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div>
      <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "16px" }}>
        {titulo}
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: "11px", fontSize: "15px" }}>{children}</div>
    </div>
  );
}

/**
 * Item do rodape completo.
 *
 * <b>Vira link quando ha para onde ir, e continua texto quando nao ha.</b> Todos eram `<div>` —
 * as tres colunas inteiras. A diferenca importa mais nesta coluna que em qualquer outra: "num
 * produto que pede acesso a saude de um ser vivo, esses links sao a garantia", e garantia que nao
 * abre nao e garantia.
 */
function ItemDeRodape({ children, para }: { children: ReactNode; para?: "/termos" | "/privacidade" }) {
  if (para === undefined) {
    return <div style={{ color: "oklch(0.45 0.015 150)" }}>{children}</div>;
  }

  return (
    <div>
      <Link to={para} style={{ color: "oklch(0.46 0.085 150)" }}>
        {children}
      </Link>
    </div>
  );
}

/** Menu aberto que so fecha no proprio botao vira menu preso quando a pessoa desiste. */
function useFechaAoClicarFora(caixa: React.RefObject<HTMLDivElement | null>, fechar: () => void) {
  useEffect(() => {
    const ouvir = (evento: MouseEvent) => {
      if (caixa.current !== null && !caixa.current.contains(evento.target as Node)) {
        fechar();
      }
    };

    document.addEventListener("mousedown", ouvir);
    return () => document.removeEventListener("mousedown", ouvir);
  });
}
