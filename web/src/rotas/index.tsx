import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { DeRelance } from "../componentes/DeRelance.tsx";
import { RegistrarDose } from "../componentes/RegistrarDose.tsx";
import { useAnimais, type Animal } from "../dados/animais.ts";
import { useSair } from "../dados/autenticacao.ts";
import { useRedeDeCuidado } from "../dados/carteira.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import {
  podeSilenciar,
  useCumprirOrientacao,
  useDeixarDeSilenciar,
  usePendencias,
  useSilenciar,
  type Pendencia,
} from "../dados/pendencias.ts";
import { lerSessao } from "../dados/sessao.ts";
import { anosDesde, diasAte } from "../i18n/datas.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 01 · web, tela grande — Area do tutor, o feed de pendencias", de
 * `design/IdentidadeVisual/Telas Petfy.dc.html`, com o backend ligado nela.
 *
 * O markup vem do arquivo: os estilos foram convertidos por script, entao as tres colunas
 * (264 / 1fr / 320), os cartoes de 12 px e os quatro marcadores sao os do desenho.
 *
 * <b>DOIS BLOCOS DO DESENHO NAO ESTAO AQUI, e a ausencia e escolha.</b> "Da Creche
 * Quintal, hoje" (foto, recado e avaliacao do dia) e "Uma leitura do Petfy" (a percepcao)
 * <b>nao tem backend nenhum</b> — nao e uma rota vazia, e um modelo que nao existe.
 * Desenhar a moldura deles agora criaria uma caixa que nunca preenche, e a secao 06 e
 * explicita sobre nao confundir "vazio" com "nao existe". Eles entram junto com o modelo.
 */

export const Route = createFileRoute("/")({
  /*
   * O token vive em memoria, entao recarregar a pagina derruba a sessao - e a home so
   * existe autenticada. Sem esta guarda, o recarregamento mostraria a tela vazia
   * piscando antes de qualquer 401 voltar.
   */
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Inicio,
});

/**
 * Quantos dias antes uma pendencia ganha a marca de "a vencer".
 *
 * <b>E decisao de tela, e nao do documento</b> — fica registrado para nao virar numero
 * orfao. O feed ja chega filtrado em 30 dias pelo servidor; marcar as 30 faria toda linha
 * ter marca, e a 5.3 e explicita: "se tudo tiver marca, nada tem". Uma semana e o
 * horizonte em que da para agir — marcar consulta, comprar o remedio.
 */
const DIAS_DE_ANTECEDENCIA = 7;

type Estado = "vencida" | "venceHoje" | "aVencer" | "semMarca";

function estadoDe(pendencia: Pendencia): Estado {
  if (pendencia.overdue === true) {
    return "vencida";
  }
  if (pendencia.dueOn === undefined) {
    // O consentimento nao tem data. Ele ja vem primeiro na ordem do servidor, e o que o
    // distingue e o proprio texto — nao uma etiqueta de prazo que ele nao tem.
    return "semMarca";
  }

  const dias = diasAte(pendencia.dueOn);

  if (dias <= 0) {
    return "venceHoje";
  }
  return dias <= DIAS_DE_ANTECEDENCIA ? "aVencer" : "semMarca";
}

function useHover() {
  const [sobre, setSobre] = useState(false);
  return {
    sobre,
    props: {
      onMouseEnter: () => setSobre(true),
      onMouseLeave: () => setSobre(false),
      onFocus: () => setSobre(true),
      onBlur: () => setSobre(false),
    },
  };
}

function Inicio() {
  const intl = useIntl();
  const contexto = useMeuContexto();
  const animais = useAnimais();
  const sair = useSair();

  const [incluirSilenciadas, setIncluirSilenciadas] = useState(false);
  const [animalEscolhido, setAnimalEscolhido] = useState<string | undefined>(undefined);

  const pendencias = usePendencias(incluirSilenciadas);

  const lista = animais.data ?? [];
  const ativo = lista.find((a) => a.animalId === animalEscolhido) ?? lista[0];

  const cadastrar = useHover();

  const doAtivo = (pendencias.data ?? []).filter(
    (p) => ativo?.animalId === undefined || p.animalId === ativo.animalId,
  );
  const pedindo = doAtivo.filter((p) => p.silenced !== true);

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>

        {/* ------------------------------------------------------------------------ topo */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "28px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              {/* Nome de marca nao se traduz: e o unico texto literal da tela. */}
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "10px", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", padding: "8px 14px", minHeight: "44px", background: "oklch(0.975 0.004 150)" }}>
              <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                {intl.formatMessage({ id: "home.agindoComo" })}
              </span>
              <span style={{ fontSize: "15px", fontWeight: 500 }}>
                {contexto.data?.personName ?? ""}
              </span>
              <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                {contexto.data?.active?.organizationName === undefined
                  ? intl.formatMessage({ id: "home.contexto.voce" })
                  : intl.formatMessage(
                      { id: "home.contexto.pela" },
                      { org: contexto.data.active.organizationName },
                    )}
              </span>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "20px", fontSize: "15px" }}>
            {/* "Meus animais" e o `href="#carteira"` do desenho: ele leva a Tela 02. Sem
                ele a vida do animal fica inalcancavel — o token vive em memoria, entao
                colar a URL cai no /entrar e volta para ca. */}
            {ativo?.animalId !== undefined ? (
              <Link
                to="/animais/$animalId"
                params={{ animalId: ativo.animalId }}
                style={{ color: "oklch(0.46 0.085 150)" }}
              >
                {intl.formatMessage({ id: "home.nav.meusAnimais" })}
              </Link>
            ) : null}

            {ativo?.animalId !== undefined ? (
              <Link
                to="/animais/$animalId/quem-cuida"
                params={{ animalId: ativo.animalId }}
                style={{ color: "oklch(0.46 0.085 150)" }}
              >
                {intl.formatMessage({ id: "home.nav.quemCuida" })}
              </Link>
            ) : null}

            <button type="button" onClick={sair} style={{ fontFamily: "inherit", background: "none", border: "none", cursor: "pointer", fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "home.sair" })}
            </button>
            <div aria-hidden style={{ width: "34px", height: "34px", borderRadius: "999px", background: "oklch(0.90 0.03 150)" }}></div>
          </div>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "264px 1fr 320px", minHeight: "720px" }}>

          {/* ------------------------------------------------------ trilho: sob sua custodia */}
          <div style={{ borderRight: "1px solid oklch(0.90 0.008 150)", padding: "24px 20px", display: "flex", flexDirection: "column", gap: "8px" }}>
            <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", padding: "0 12px 10px" }}>
              {intl.formatMessage({ id: "home.custodia" })}
            </div>

            {lista.map((animal) => (
              <ItemDeAnimal
                key={animal.animalId}
                animal={animal}
                ativo={animal.animalId === ativo?.animalId}
                pendencias={(pendencias.data ?? []).filter((p) => p.animalId === animal.animalId)}
                aoEscolher={() => setAnimalEscolhido(animal.animalId)}
              />
            ))}

            <div style={{ height: "1px", background: "oklch(0.92 0.006 150)", margin: "12px 0" }}></div>

            {/* Deixou de ser desabilitado quando o onboarding passou a existir. */}
            <Link
              to="/animais/novo"
              {...cadastrar.props}
              style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: cadastrar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: `1px solid ${cadastrar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.84 0.012 150)"}`, borderRadius: "8px", padding: "12px", minHeight: "44px", textAlign: "left", textDecoration: "none", display: "block" }}
            >
              {intl.formatMessage({ id: "home.cadastrarAnimal" })}
            </Link>
          </div>

          {/* ---------------------------------------------------------------- centro: hoje */}
          <div style={{ padding: "32px 36px" }}>
            <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", marginBottom: "8px" }}>
              <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: 0, letterSpacing: "-0.02em" }}>
                {intl.formatMessage({ id: "home.hoje" })}
              </h1>
              <button
                type="button"
                onClick={() => setIncluirSilenciadas((atual) => !atual)}
                style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.45 0.015 150)", background: "transparent", border: "none", cursor: "pointer", minHeight: "44px" }}
              >
                {intl.formatMessage({
                  id: incluirSilenciadas ? "home.ocultarSilenciadas" : "home.mostrarSilenciadas",
                })}
              </button>
            </div>

            <p style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)", margin: "0 0 24px" }}>
              {pendencias.isPending
                ? intl.formatMessage({ id: "home.pendencias.carregando" })
                : intl.formatMessage({ id: "home.quantas" }, { total: pedindo.length })}
            </p>

            {pendencias.isError ? (
              <Aviso>{intl.formatMessage({ id: chaveDoErro(pendencias.error) })}</Aviso>
            ) : null}

            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {doAtivo.length === 0 && !pendencias.isPending ? (
                <Vazio />
              ) : (
                doAtivo.map((pendencia) => (
                  <CartaoDePendencia
                    key={`${pendencia.kind}:${pendencia.sourceId}`}
                    pendencia={pendencia}
                  />
                ))
              )}
            </div>
          </div>

          {/* ------------------------------------------------------------- trilho: de relance */}
          <div style={{ borderLeft: "1px solid oklch(0.90 0.008 150)", padding: "32px 24px", display: "flex", flexDirection: "column", gap: "24px" }}>
            {ativo?.animalId !== undefined && ativo.name !== undefined ? (
              <>
                <DeRelance animalId={ativo.animalId} nome={ativo.name} />
                <QuemAlcanca animalId={ativo.animalId} />
              </>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Aviso({ children }: { children: ReactNode }) {
  return (
    <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: "0 0 12px" }}>
      {children}
    </p>
  );
}

/** O vazio de verdade da secao 06: linha tracejada, e nao fundo cheio. */
function Vazio() {
  const intl = useIntl();

  return (
    <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
      <div style={{ marginBottom: "4px" }}>{intl.formatMessage({ id: "home.pendencias.vazio" })}</div>
      <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
        {intl.formatMessage({ id: "home.pendencias.vazio.apoio" })}
      </div>
    </div>
  );
}

/** O pior estado entre as pendencias do animal e o que vira marcador no trilho. */
function marcadorDoAnimal(pendencias: Pendencia[]) {
  const ativas = pendencias.filter((p) => p.silenced !== true);

  if (ativas.some((p) => estadoDe(p) === "vencida")) {
    return <div aria-hidden style={{ marginLeft: "auto", width: "12px", height: "12px", background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)", flex: "none" }} />;
  }
  if (ativas.some((p) => estadoDe(p) === "venceHoje" || estadoDe(p) === "aVencer")) {
    return <div aria-hidden style={{ marginLeft: "auto", width: "12px", height: "12px", borderRadius: "999px", border: "2px solid oklch(0.62 0.11 70)", flex: "none" }} />;
  }
  return <div aria-hidden style={{ marginLeft: "auto", width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none" }} />;
}

function ItemDeAnimal({
  animal,
  ativo,
  pendencias,
  aoEscolher,
}: {
  animal: Animal;
  ativo: boolean;
  pendencias: Pendencia[];
  aoEscolher: () => void;
}) {
  const intl = useIntl();

  const especie =
    animal.species === undefined
      ? undefined
      : intl.formatMessage({ id: `home.especie.${animal.species}` });

  const idade =
    animal.bornDate === undefined
      ? undefined
      : intl.formatMessage({ id: "home.idade" }, { anos: anosDesde(animal.bornDate) });

  return (
    <button
      type="button"
      onClick={aoEscolher}
      aria-pressed={ativo}
      style={{ fontFamily: "inherit", textAlign: "left", width: "100%", display: "flex", alignItems: "center", gap: "12px", padding: "12px", borderRadius: "8px", background: ativo ? "oklch(0.94 0.02 150)" : "transparent", border: "none", minHeight: "44px", cursor: "pointer" }}
    >
      <div aria-hidden style={{ width: "38px", height: "38px", borderRadius: "999px", background: ativo ? "oklch(0.86 0.03 150)" : "oklch(0.90 0.012 150)", flex: "none" }}></div>

      <div style={{ minWidth: 0 }}>
        <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 500, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "130px" }}>
          {animal.name}
        </div>
        <div style={{ fontSize: "13px", color: "oklch(0.45 0.015 150)" }}>
          {[especie, idade].filter(Boolean).join(" · ")}
        </div>
      </div>

      {marcadorDoAnimal(pendencias)}
    </button>
  );
}

/**
 * O cartao de pendencia — "o componente mais usado do produto" (secao 06).
 *
 * <b>O terceiro estado e a regra virando componente:</b> quando outra pessoa cumpriu, a
 * pendencia NAO some — ela conta quem fez e quando. E o "nunca cobrar a mesma coisa de
 * duas pessoas em silencio" deixando de ser principio e virando pixel.
 */
function CartaoDePendencia({ pendencia }: { pendencia: Pendencia }) {
  const intl = useIntl();

  const silenciar = useSilenciar();
  const deixarDeSilenciar = useDeixarDeSilenciar();
  const cumprir = useCumprirOrientacao();
  const acao = useHover();
  const [registrando, setRegistrando] = useState(false);

  const estado = estadoDe(pendencia);
  const cumprida = pendencia.lastFulfilledAt !== undefined;
  const silenciada = pendencia.silenced === true;

  const alvo = { kind: pendencia.kind!, sourceId: pendencia.sourceId! };

  const borda =
    estado === "vencida" && !cumprida ? "oklch(0.86 0.03 30)" : "oklch(0.90 0.008 150)";
  const fundo =
    estado === "vencida" && !cumprida
      ? "oklch(0.985 0.008 30)"
      : cumprida || silenciada
        ? "oklch(0.975 0.004 150)"
        : "oklch(1 0 0)";

  return (
    <div style={{ border: `1px solid ${borda}`, background: fundo, borderRadius: "12px", padding: "20px 22px", display: "grid", gridTemplateColumns: "auto 1fr auto", gap: "18px", alignItems: "center" }}>
      {cumprida ? (
        <div aria-hidden style={{ width: "13px", height: "13px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }} />
      ) : estado === "vencida" ? (
        <div aria-hidden style={{ width: "13px", height: "13px", background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)" }} />
      ) : (
        <div aria-hidden style={{ width: "13px", height: "13px", borderRadius: "999px", border: "3px solid oklch(0.62 0.11 70)" }} />
      )}

      <div>
        <div style={{ fontSize: "17px", fontWeight: 500, marginBottom: "4px", color: cumprida ? "oklch(0.42 0.015 150)" : "oklch(0.25 0.02 150)" }}>
          {pendencia.animalName === undefined
            ? pendencia.description
            : intl.formatMessage(
                { id: "home.pendencia.titulo" },
                { o_que: pendencia.description ?? "", animal: pendencia.animalName },
              )}
        </div>

        <div style={{ fontSize: "14px", color: "oklch(0.45 0.015 150)" }}>
          {cumprida
            ? intl.formatMessage(
                { id: "home.jaFeito" },
                {
                  nome: pendencia.lastFulfilledByName ?? "",
                  quando: intl.formatDate(new Date(pendencia.lastFulfilledAt!), {
                    dateStyle: "short",
                    timeStyle: "short",
                  }),
                },
              )
            : pendencia.dueOn !== undefined
              ? intl.formatMessage(
                  { id: `home.estado.${estado === "semMarca" ? "aVencer" : estado}` },
                  { dias: Math.abs(diasAte(pendencia.dueOn)) },
                )
              : ""}
        </div>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
        {silenciada ? (
          <button
            type="button"
            onClick={() => deixarDeSilenciar.mutate(alvo)}
            style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.45 0.015 150)", background: "transparent", border: "none", cursor: "pointer", minHeight: "44px" }}
          >
            {intl.formatMessage({ id: "home.acao.voltarACobrar" })}
          </button>
        ) : cumprida ? (
          <span style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
            {intl.formatMessage({ id: "home.silenciada.cumprido" })}
          </span>
        ) : (
          <>
            {podeSilenciar(pendencia) && (
              <button
                type="button"
                onClick={() => silenciar.mutate(alvo)}
                style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.45 0.015 150)", background: "transparent", border: "none", cursor: "pointer", minHeight: "44px" }}
              >
                {intl.formatMessage({ id: "home.acao.silenciar" })}
              </button>
            )}

            {pendencia.kind === "ORIENTACAO" ? (
              <button
                type="button"
                onClick={() =>
                  cumprir.mutate({
                    careInstructionId: pendencia.sourceId!,
                    animalId: pendencia.animalId!,
                  })
                }
                disabled={cumprir.isPending}
                {...acao.props}
                style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: acao.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: `1px solid ${acao.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, borderRadius: "8px", padding: "12px 20px", minHeight: "44px", cursor: "pointer" }}
              >
                {intl.formatMessage({ id: cumprir.isPending ? "home.acao.cumprindo" : "home.acao.cumprir" })}
              </button>
            ) : pendencia.kind === "DOSE_DE_VACINA" ? (
              /* A acao principal do cartao vencido: musgo cheio, e nao contorno. E a
                 unica cor de acao do produto, e aqui ela esta sobre fundo de telha —
                 a gravidade e do marcador e do fundo, nunca do botao (secao 02). */
              <button
                type="button"
                onClick={() => setRegistrando(true)}
                style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "12px 20px", minHeight: "44px", cursor: "pointer" }}
              >
                {intl.formatMessage({ id: "home.acao.registrarDose" })}
              </button>
            ) : null}
          </>
        )}
      </div>

      {registrando && pendencia.animalId !== undefined && pendencia.sourceId !== undefined ? (
        <div style={{ gridColumn: "1 / -1" }}>
          <RegistrarDose
            animalId={pendencia.animalId}
            vaccineId={pendencia.sourceId}
            aoFechar={() => setRegistrando(false)}
          />
        </div>
      ) : null}
    </div>
  );
}

/** "Quem alcanca o Code" — custodia e concessao, nunca uma lista de contatos (5.4). */
function QuemAlcanca({ animalId }: { animalId: string }) {
  const intl = useIntl();
  const rede = useRedeDeCuidado(animalId);

  const membros = rede.data ?? [];

  return (
    <div>
      <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
        {intl.formatMessage({ id: "home.quemAlcanca" })}
      </div>

      {membros.length === 0 ? (
        <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "home.quemAlcanca.vazio" })}
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "14px" }}>
          {membros.map((membro) => (
            <div key={membro.personId ?? membro.organizationId}>
              <div style={{ fontWeight: 500, fontSize: "15px" }}>{membro.name}</div>
              <div style={{ color: "oklch(0.5 0.015 150)" }}>{alcanceDe(membro, intl)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function alcanceDe(
  membro: NonNullable<ReturnType<typeof useRedeDeCuidado>["data"]>[number],
  intl: ReturnType<typeof useIntl>,
): string {
  const comoAlcanca = intl.formatMessage({
    id: membro.reach === "CUSTODIA" ? "home.alcance.custodia" : "home.alcance.concessao",
  });

  const prazo =
    membro.expiresAt === undefined
      ? intl.formatMessage({ id: "home.alcance.semPrazo" })
      : intl.formatMessage(
          { id: "home.alcance.ate" },
          { data: intl.formatDate(new Date(membro.expiresAt), { dateStyle: "short" }) },
        );

  return `${comoAlcanca} · ${prazo}`;
}
