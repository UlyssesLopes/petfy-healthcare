import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { MedalhaDoAnimal } from "../componentes/novo/MedalhaDoAnimal.tsx";
import { MolduraDoTutor } from "../componentes/novo/MolduraDoTutor.tsx";
import { RegistrarDose } from "../componentes/novo/RegistrarDose.tsx";
import { useLinhasDoTempo, type EntradaDaLinha } from "../dados/animal.ts";
import { useAnimais, type Animal } from "../dados/animais.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { useQueJaEstiveramComigo } from "../dados/fim.ts";
import {
  podeSilenciar,
  useCumprirOrientacao,
  useDeixarDeSilenciar,
  usePendencias,
  useSilenciar,
  type Pendencia,
} from "../dados/pendencias.ts";
import { lerSessao } from "../dados/sessao.ts";
import { dataLocalDe, diasAte } from "../i18n/datas.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/app.css";
import "../estilos/novo/hoje.css";

/**
 * Hoje: a primeira tela de quem entra. Cumprimenta, mostra os animais com o estado de cada um e
 * lista o que pede a pessoa, de todos eles. Cumprido por outra pessoa não some: conta quem fez.
 */
export const Route = createFileRoute("/")({
  // O token vive em memória: sem a guarda, recarregar mostraria a tela vazia antes do 401.
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Hoje,
});

/** Uma semana: o horizonte em que dá para agir. Marcar tudo que vence em 30 dias marcaria tudo. */
const DIAS_DE_ANTECEDENCIA = 7;

type Estado = "vencida" | "venceHoje" | "aVencer" | "semMarca";

function estadoDe(pendencia: Pendencia): Estado {
  if (pendencia.overdue === true) return "vencida";
  if (pendencia.dueOn === undefined) return "semMarca";
  const dias = diasAte(pendencia.dueOn);
  if (dias <= 0) return "venceHoje";
  return dias <= DIAS_DE_ANTECEDENCIA ? "aVencer" : "semMarca";
}

function saudacao(): "manha" | "tarde" | "noite" {
  const hora = new Date().getHours();
  if (hora >= 5 && hora < 12) return "manha";
  if (hora >= 12 && hora < 18) return "tarde";
  return "noite";
}

/** A ordem de urgência: o que venceu há mais tempo, depois o que vence antes. */
function urgencia(pendencia: Pendencia): number {
  if (pendencia.dueOn === undefined) return 1e6;
  return diasAte(pendencia.dueOn);
}

const TIPO_DE_EVENTO: Record<string, string> = {
  VACINA: "Vacina",
  ANTIPARASITARIO: "Antiparasitário",
  ATENDIMENTO: "Consulta",
  OBSERVACAO: "Recado",
  PESAGEM: "Peso",
  CONDICAO: "Condição",
  ANEXO: "Documento",
  ORIENTACAO: "Orientação",
  CUMPRIMENTO: "Cuidado feito",
  UNIAO: "Cadastros unidos",
  OBITO: "Fim",
};

const RELATIVO = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });
const DATA_CURTA = new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "short" });

function quando(iso: string | undefined): string {
  if (!iso) return "";
  const dia = iso.length <= 10 ? dataLocalDe(iso) : new Date(iso);
  const dias = -diasAte(iso.slice(0, 10));
  return dias >= 0 && dias < 7 ? RELATIVO.format(-dias, "day") : DATA_CURTA.format(dia);
}

const KG = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

/** O resumo do registro; o peso chega como número cru e sai com a unidade. */
function textoDo(entrada: EntradaDaLinha): string {
  if (entrada.eventType === "PESAGEM" && entrada.summary) {
    const kg = Number(entrada.summary.replace(",", "."));
    return Number.isFinite(kg) ? `${KG.format(kg)} kg` : entrada.summary;
  }
  return entrada.summary ?? "";
}

/** Quantos registros o mural mostra: a semana, sem virar a linha do tempo inteira. */
const NO_MURAL = 6;

function Hoje() {
  const intl = useIntl();
  const contexto = useMeuContexto();
  const animais = useAnimais();
  const [incluirSilenciadas, setIncluirSilenciadas] = useState(false);
  const pendencias = usePendencias(incluirSilenciadas);

  const lista = animais.data ?? [];
  const todas = pendencias.data ?? [];
  const pedindo = todas
    .filter((p) => p.silenced !== true && p.lastFulfilledAt === undefined)
    .sort((a, b) => urgencia(a) - urgencia(b));
  const destaque = pedindo[0];
  const resto = todas.filter((p) => p !== destaque);
  const primeiroNome = contexto.data?.personName?.split(" ")[0] ?? "";
  const animalDe = (id: string | undefined) => lista.find((a) => a.animalId === id);

  return (
    <MolduraDoTutor titulo={intl.formatMessage({ id: "home.hoje" })}>
      <div className="pf-hoje">
        <header className="pf-hoje__cabeca">
          <h1 className="pf-hoje__titulo">
            {intl.formatMessage({ id: `hoje.saudacao.${saudacao()}` }, { nome: primeiroNome })}
          </h1>
          <p className="pf-hoje__resumo">
            {pendencias.isPending
              ? intl.formatMessage({ id: "home.pendencias.carregando" })
              : intl.formatMessage({ id: "hoje.resumo" }, { total: pedindo.length })}
          </p>
        </header>

        {pendencias.isError && (
          <p role="alert" className="pf-hoje__erro">
            {intl.formatMessage({ id: chaveDoErro(pendencias.error) })}
          </p>
        )}

        {destaque !== undefined && <Destaque pendencia={destaque} animal={animalDe(destaque.animalId)} />}

        <div className="pf-hoje__grade">
          <Semana animais={lista} />

          <aside className="pf-hoje__lado">
            {(resto.length > 0 || incluirSilenciadas) && (
              <section className="pf-hoje__pede" aria-labelledby="pf-hoje-pede">
                <h2 id="pf-hoje-pede" className="pf-hoje__secao">
                  {intl.formatMessage({ id: destaque === undefined ? "hoje.pede" : "hoje.tambem" })}
                </h2>
                <ul className="pf-pedidos">
                  {resto.map((pendencia) => (
                    <li key={`${pendencia.kind}:${pendencia.sourceId}`}>
                      <CartaoDePendencia pendencia={pendencia} animal={animalDe(pendencia.animalId)} />
                    </li>
                  ))}
                </ul>
              </section>
            )}
            <button
              type="button"
              className="pf-texto-botao pf-hoje__silenciadas"
              aria-pressed={incluirSilenciadas}
              onClick={() => setIncluirSilenciadas((atual) => !atual)}
            >
              {intl.formatMessage({ id: incluirSilenciadas ? "home.ocultarSilenciadas" : "home.mostrarSilenciadas" })}
            </button>

            <ul className="pf-hoje__atalhos">
              <li>
                <Link to="/convites">{intl.formatMessage({ id: "hoje.convite.acao" })}</Link>
              </li>
              {/* A porta da área de organização para o autônomo: credencial sem vínculo. */}
              {contexto.data?.professional === true && contexto.data.active?.kind !== "ORGANIZACAO" && (
                <li>
                  <Link to="/pacientes">{intl.formatMessage({ id: "pacientes.titulo" })}</Link>
                </li>
              )}
            </ul>

            <QuemJaEsteve />
          </aside>
        </div>
      </div>
    </MolduraDoTutor>
  );
}

/** O que mais pede a pessoa agora, sozinho no cobalto, com a ação ali mesmo. */
function Destaque({ pendencia, animal }: { pendencia: Pendencia; animal?: Animal }) {
  const intl = useIntl();
  const silenciar = useSilenciar();
  const cumprir = useCumprirOrientacao();
  const [registrando, setRegistrando] = useState(false);
  const estado = estadoDe(pendencia);
  const alvo = { kind: pendencia.kind!, sourceId: pendencia.sourceId! };

  return (
    <section className="pf-destaque" aria-labelledby="pf-destaque-titulo">
      {animal !== undefined && <MedalhaDoAnimal animal={animal} className="pf-destaque__medalha" prioridade />}
      <div className="pf-destaque__texto">
        <p className="pf-destaque__animal" translate="no">
          {pendencia.animalName}
        </p>
        <h2 id="pf-destaque-titulo" className="pf-destaque__titulo">
          {pendencia.description}
        </h2>
        {pendencia.dueOn !== undefined && (
          <p className="pf-destaque__prazo" data-tom={estado === "vencida" ? "perigo" : "alerta"}>
            {intl.formatMessage(
              { id: `home.estado.${estado === "semMarca" ? "aVencer" : estado}` },
              { dias: Math.abs(diasAte(pendencia.dueOn)) },
            )}
          </p>
        )}
        <div className="pf-destaque__acoes">
          {pendencia.kind === "DOSE_DE_VACINA" && !registrando && (
            <button type="button" className="pf-botao pf-botao--claro" onClick={() => setRegistrando(true)}>
              {intl.formatMessage({ id: "home.acao.registrarDose" })}
            </button>
          )}
          {pendencia.kind === "ORIENTACAO" && (
            <button
              type="button"
              className="pf-botao pf-botao--claro"
              disabled={cumprir.isPending}
              onClick={() => cumprir.mutate({ careInstructionId: pendencia.sourceId!, animalId: pendencia.animalId! })}
            >
              {intl.formatMessage({ id: cumprir.isPending ? "home.acao.cumprindo" : "home.acao.cumprir" })}
            </button>
          )}
          {podeSilenciar(pendencia) && (
            <button type="button" className="pf-texto-botao" onClick={() => silenciar.mutate(alvo)}>
              {intl.formatMessage({ id: "home.acao.silenciar" })}
            </button>
          )}
          {animal?.animalId !== undefined && (
            <Link to="/animais/$animalId" params={{ animalId: animal.animalId }} className="pf-destaque__ver">
              {intl.formatMessage({ id: "hoje.verAnimal" }, { animal: animal.name ?? "" })}
            </Link>
          )}
        </div>
        {registrando && pendencia.animalId !== undefined && pendencia.sourceId !== undefined && (
          <RegistrarDose
            animalId={pendencia.animalId}
            vaccineId={pendencia.sourceId}
            aoFechar={() => setRegistrando(false)}
          />
        )}
      </div>
    </section>
  );
}

/** A vida da semana: os registros mais recentes de todos os animais, assinados. */
function Semana({ animais }: { animais: Animal[] }) {
  const intl = useIntl();
  const linhas = useLinhasDoTempo(animais.map((a) => a.animalId ?? "").filter((id) => id !== ""));
  const carregando = linhas.some((l) => l.isPending);

  const registros = animais
    .flatMap((animal, i) =>
      (linhas[i]?.data ?? [])
        // A foto de perfil é o rosto do animal, e não um acontecimento.
        .filter((e) => !(e.eventType === "ANEXO" && e.eventId === animal.photoAttachmentId))
        .map((entrada) => ({ animal, entrada })),
    )
    .sort((a, b) => (b.entrada.occurredAt ?? "").localeCompare(a.entrada.occurredAt ?? ""))
    .slice(0, NO_MURAL);

  return (
    <section className="pf-semana" aria-labelledby="pf-semana-titulo">
      <h2 id="pf-semana-titulo" className="pf-hoje__secao">
        {intl.formatMessage({ id: "hoje.semana" })}
      </h2>
      {carregando ? (
        <p className="pf-hoje__nota">{intl.formatMessage({ id: "hoje.semana.carregando" })}</p>
      ) : registros.length === 0 ? (
        <div className="pf-hoje__vazio">
          <p className="pf-hoje__vazio-titulo">{intl.formatMessage({ id: "hoje.semana.vazio" })}</p>
        </div>
      ) : (
        <ul className="pf-mural">
          {registros.map(({ animal, entrada }) => (
            <li key={`${animal.animalId}-${entrada.eventType}-${entrada.eventId}-${entrada.recordedAt}`}>
              <Link to="/animais/$animalId" params={{ animalId: animal.animalId! }} className="pf-recado">
                <span className="pf-recado__topo">
                  <MedalhaDoAnimal animal={animal} className="pf-recado__medalha" />
                  <span className="pf-recado__animal" translate="no">
                    {animal.name}
                  </span>
                  <span className="pf-recado__tipo">{TIPO_DE_EVENTO[entrada.eventType ?? ""] ?? "Registro"}</span>
                </span>
                <span className="pf-recado__texto">{textoDo(entrada)}</span>
                <span className="pf-recado__assinatura">
                  <span>{[entrada.recordedByName, entrada.organizationName].filter(Boolean).join(" · ")}</span>
                  <span className="pf-recado__quando">{quando(entrada.occurredAt)}</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function CartaoDePendencia({ pendencia, animal }: { pendencia: Pendencia; animal?: Animal }) {
  const intl = useIntl();
  const silenciar = useSilenciar();
  const deixarDeSilenciar = useDeixarDeSilenciar();
  const cumprir = useCumprirOrientacao();
  const [registrando, setRegistrando] = useState(false);

  const estado = estadoDe(pendencia);
  const cumprida = pendencia.lastFulfilledAt !== undefined;
  const silenciada = pendencia.silenced === true;
  const alvo = { kind: pendencia.kind!, sourceId: pendencia.sourceId! };

  const tom = cumprida || silenciada ? undefined : estado === "vencida" ? "perigo" : estado === "semMarca" ? undefined : "alerta";
  const prazo =
    pendencia.dueOn === undefined
      ? undefined
      : intl.formatMessage(
          { id: `home.estado.${estado === "semMarca" ? "aVencer" : estado}` },
          { dias: Math.abs(diasAte(pendencia.dueOn)) },
        );

  return (
    <article className="pf-pedido" data-apagado={cumprida || silenciada ? "" : undefined}>
      {animal !== undefined ? (
        <MedalhaDoAnimal animal={animal} className="pf-pedido__medalha" />
      ) : (
        <span className="pf-pedido__medalha pf-pedido__medalha--vazia" aria-hidden="true" />
      )}

      <div className="pf-pedido__texto">
        <h3 className="pf-pedido__titulo">{pendencia.description}</h3>
        <p className="pf-pedido__linha">
          {pendencia.animalName !== undefined && (
            <span className="pf-pedido__animal" translate="no">
              {pendencia.animalName}
            </span>
          )}
          {cumprida ? (
            <span>
              {intl.formatMessage(
                { id: "home.jaFeito" },
                {
                  nome: pendencia.lastFulfilledByName ?? "",
                  quando: intl.formatDate(new Date(pendencia.lastFulfilledAt!), { dateStyle: "short", timeStyle: "short" }),
                },
              )}
            </span>
          ) : silenciada ? (
            <span>{intl.formatMessage({ id: "home.silenciada" })}</span>
          ) : prazo !== undefined ? (
            tom !== undefined ? (
              <span className="pf-estado" data-tom={tom}>
                {prazo}
              </span>
            ) : (
              <span>{prazo}</span>
            )
          ) : null}
        </p>
      </div>

      <div className="pf-pedido__acoes">
        {silenciada ? (
          <button type="button" className="pf-texto-botao" onClick={() => deixarDeSilenciar.mutate(alvo)}>
            {intl.formatMessage({ id: "home.acao.voltarACobrar" })}
          </button>
        ) : cumprida ? (
          <span className="pf-pedido__feito">{intl.formatMessage({ id: "home.silenciada.cumprido" })}</span>
        ) : (
          <>
            {podeSilenciar(pendencia) && (
              <button type="button" className="pf-texto-botao" onClick={() => silenciar.mutate(alvo)}>
                {intl.formatMessage({ id: "home.acao.silenciar" })}
              </button>
            )}
            {pendencia.kind === "ORIENTACAO" && (
              <button
                type="button"
                className="pf-botao pf-botao--pequeno"
                disabled={cumprir.isPending}
                aria-busy={cumprir.isPending || undefined}
                onClick={() => cumprir.mutate({ careInstructionId: pendencia.sourceId!, animalId: pendencia.animalId! })}
              >
                {intl.formatMessage({ id: cumprir.isPending ? "home.acao.cumprindo" : "home.acao.cumprir" })}
              </button>
            )}
            {pendencia.kind === "DOSE_DE_VACINA" && !registrando && (
              <button
                type="button"
                className="pf-botao pf-botao--pequeno"
                aria-expanded={registrando}
                onClick={() => setRegistrando(true)}
              >
                {intl.formatMessage({ id: "home.acao.registrarDose" })}
              </button>
            )}
          </>
        )}
      </div>

      {registrando && pendencia.animalId !== undefined && pendencia.sourceId !== undefined && (
        <div className="pf-pedido__dose">
          <RegistrarDose
            animalId={pendencia.animalId}
            vaccineId={pendencia.sourceId}
            aoFechar={() => setRegistrando(false)}
          />
        </div>
      )}
    </article>
  );
}

/** Os animais que já foram da pessoa. Some quando vazia: anunciar o vazio lembraria de uma perda. */
function QuemJaEsteve() {
  const intl = useIntl();
  const anteriores = useQueJaEstiveramComigo();
  const lista = anteriores.data?.content ?? [];
  if (lista.length === 0) return null;

  return (
    <div className="pf-hoje__cartao">
      <h2 className="pf-hoje__cartao-titulo">{intl.formatMessage({ id: "anteriores.titulo" })}</h2>
      <ul className="pf-anteriores">
        {lista.map((animal) => (
          <li key={animal.animalId}>
            <Link to="/animais/$animalId" params={{ animalId: animal.animalId! }}>
              <span translate="no">{animal.name}</span>
              <span className="pf-anteriores__quando">
                {animal.deceasedOn === undefined || animal.deceasedOn === null
                  ? intl.formatMessage({ id: "anteriores.saiu" })
                  : intl.formatMessage(
                      { id: "anteriores.morreuEm" },
                      { data: intl.formatDate(animal.deceasedOn, { month: "2-digit", year: "numeric", timeZone: "UTC" }) },
                    )}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
