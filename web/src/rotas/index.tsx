import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { MedalhaDoAnimal } from "../componentes/novo/MedalhaDoAnimal.tsx";
import { MolduraDoTutor } from "../componentes/novo/MolduraDoTutor.tsx";
import { RegistrarDose } from "../componentes/novo/RegistrarDose.tsx";
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
import { anosDesde, diasAte } from "../i18n/datas.ts";
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

function Hoje() {
  const intl = useIntl();
  const contexto = useMeuContexto();
  const animais = useAnimais();
  const [incluirSilenciadas, setIncluirSilenciadas] = useState(false);
  const pendencias = usePendencias(incluirSilenciadas);

  const lista = animais.data ?? [];
  const todas = pendencias.data ?? [];
  const pedindo = todas.filter((p) => p.silenced !== true && p.lastFulfilledAt === undefined);
  const primeiroNome = contexto.data?.personName?.split(" ")[0] ?? "";

  return (
    <MolduraDoTutor titulo={intl.formatMessage({ id: "home.hoje" })}>
      <div className="pf-hoje">
        <header className="pf-hoje__cabeca">
          <h1 className="pf-titulo pf-hoje__titulo">
            {intl.formatMessage({ id: `hoje.saudacao.${saudacao()}` }, { nome: primeiroNome })}
          </h1>
          <p className="pf-hoje__resumo">
            {pendencias.isPending
              ? intl.formatMessage({ id: "home.pendencias.carregando" })
              : intl.formatMessage({ id: "hoje.resumo" }, { total: pedindo.length })}
          </p>
        </header>

        <section className="pf-hoje__animais" aria-labelledby="pf-hoje-animais">
          <h2 id="pf-hoje-animais" className="pf-hoje__secao">
            {intl.formatMessage({ id: "hoje.animais" })}
          </h2>
          <ul className="pf-bichos">
            {lista.map((animal) => (
              <li key={animal.animalId}>
                <CartaoDoAnimal
                  animal={animal}
                  pendencias={todas.filter((p) => p.animalId === animal.animalId)}
                />
              </li>
            ))}
            <li>
              <Link to="/animais/novo" className="pf-bicho pf-bicho--novo">
                <span className="pf-bicho__mais" aria-hidden="true">
                  +
                </span>
                <span className="pf-bicho__nome">{intl.formatMessage({ id: "hoje.cadastrar" })}</span>
              </Link>
            </li>
          </ul>
        </section>

        <div className="pf-hoje__grade">
          <section className="pf-hoje__pede" aria-labelledby="pf-hoje-pede">
            <div className="pf-hoje__pede-cabeca">
              <h2 id="pf-hoje-pede" className="pf-hoje__secao">
                {intl.formatMessage({ id: "hoje.pede" })}
              </h2>
              <button
                type="button"
                className="pf-texto-botao"
                aria-pressed={incluirSilenciadas}
                onClick={() => setIncluirSilenciadas((atual) => !atual)}
              >
                {intl.formatMessage({
                  id: incluirSilenciadas ? "home.ocultarSilenciadas" : "home.mostrarSilenciadas",
                })}
              </button>
            </div>

            {pendencias.isError && (
              <p role="alert" className="pf-hoje__erro">
                {intl.formatMessage({ id: chaveDoErro(pendencias.error) })}
              </p>
            )}

            {!pendencias.isPending && todas.length === 0 ? (
              <div className="pf-hoje__vazio">
                <p className="pf-hoje__vazio-titulo">{intl.formatMessage({ id: "home.pendencias.vazio" })}</p>
                <p>{intl.formatMessage({ id: "home.pendencias.vazio.apoio" })}</p>
              </div>
            ) : (
              <ul className="pf-pedidos">
                {todas.map((pendencia) => (
                  <li key={`${pendencia.kind}:${pendencia.sourceId}`}>
                    <CartaoDePendencia
                      pendencia={pendencia}
                      animal={lista.find((a) => a.animalId === pendencia.animalId)}
                    />
                  </li>
                ))}
              </ul>
            )}
            <p className="pf-hoje__nota">{intl.formatMessage({ id: "hoje.silenciarNota" })}</p>
          </section>

          <aside className="pf-hoje__lado">
            <div className="pf-hoje__cartao">
              <h2 className="pf-hoje__cartao-titulo">{intl.formatMessage({ id: "hoje.convite.titulo" })}</h2>
              <p>{intl.formatMessage({ id: "hoje.convite.texto" })}</p>
              <Link to="/convites" className="pf-botao pf-botao--pequeno">
                {intl.formatMessage({ id: "hoje.convite.acao" })}
              </Link>
            </div>

            {/* A porta da área de organização para o autônomo: credencial sem vínculo. */}
            {contexto.data?.professional === true && contexto.data.active?.kind !== "ORGANIZACAO" && (
              <div className="pf-hoje__cartao">
                <h2 className="pf-hoje__cartao-titulo">{intl.formatMessage({ id: "hoje.pacientes.titulo" })}</h2>
                <Link to="/pacientes" className="pf-botao pf-botao--pequeno pf-botao--claro">
                  {intl.formatMessage({ id: "pacientes.titulo" })}
                </Link>
              </div>
            )}

            <QuemJaEsteve />
          </aside>
        </div>
      </div>
    </MolduraDoTutor>
  );
}

function CartaoDoAnimal({ animal, pendencias }: { animal: Animal; pendencias: Pendencia[] }) {
  const intl = useIntl();
  const ativas = pendencias.filter((p) => p.silenced !== true && p.lastFulfilledAt === undefined);
  const vencidas = ativas.filter((p) => estadoDe(p) === "vencida").length;
  const logo = ativas.filter((p) => estadoDe(p) === "venceHoje" || estadoDe(p) === "aVencer").length;

  const sobre = [
    animal.species === undefined ? undefined : intl.formatMessage({ id: `home.especie.${animal.species}` }),
    animal.bornDate === undefined ? undefined : intl.formatMessage({ id: "home.idade" }, { anos: anosDesde(animal.bornDate) }),
  ]
    .filter(Boolean)
    .join(" · ");

  const [tom, chave, n] =
    vencidas > 0
      ? (["perigo", "hoje.animal.vencidas", vencidas] as const)
      : logo > 0
        ? (["alerta", "hoje.animal.logo", logo] as const)
        : ativas.length > 0
          ? (["calmo", "hoje.animal.pelaFrente", ativas.length] as const)
          : (["calmo", "hoje.animal.emDia", 0] as const);

  return (
    <Link to="/animais/$animalId" params={{ animalId: animal.animalId! }} className="pf-bicho">
      <MedalhaDoAnimal animal={animal} className="pf-bicho__medalha" />
      <span className="pf-bicho__nome" translate="no">
        {animal.name}
      </span>
      {sobre !== "" && <span className="pf-bicho__sobre">{sobre}</span>}
      <span className="pf-estado pf-bicho__estado" data-tom={tom}>
        {intl.formatMessage({ id: chave }, { n })}
      </span>
    </Link>
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
