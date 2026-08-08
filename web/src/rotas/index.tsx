import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl, type IntlShape } from "react-intl";

import { useSair } from "../dados/autenticacao.ts";
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
import { dataLocalDe, diasAte } from "../i18n/datas.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

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

function Inicio() {
  const intl = useIntl();
  const contexto = useMeuContexto();
  const sair = useSair();

  const [incluirSilenciadas, setIncluirSilenciadas] = useState(false);

  return (
    <div className="min-h-dvh bg-superficie">
      <header className="flex items-center gap-4 border-b border-contorno bg-superficie-elevada px-5 py-3.5">
        {/* Nome de marca nao se traduz, e por isso e o unico texto literal da tela. */}
        <span className="text-resumo text-acento">Petfy</span>

        <span className="text-apoio ml-auto text-tinta-secundaria">
          {contexto.data?.personName ?? ""}
        </span>

        <button
          type="button"
          onClick={sair}
          className="text-interface min-h-toque rounded-pilula px-3 text-tinta-secundaria underline"
        >
          {intl.formatMessage({ id: "home.sair" })}
        </button>
      </header>

      {/*
        Duas colunas em tela larga, e a segunda esta reservada para a percepcao (5.5):
        ela vive FORA do eixo do tempo, e a posicao existe desde ja para que o dia em que
        ela chegar nao desfaca o layout. Nao ha caixa vazia aqui de proposito — a propria
        5.5 chama isso de moldura vazia.
      */}
      <div className="mx-auto grid max-w-5xl gap-6 p-5 lg:grid-cols-[1fr_292px]">
        <main>
          <div className="flex flex-wrap items-baseline gap-3">
            <h1 className="text-rotulo uppercase text-tinta-secundaria">
              {intl.formatMessage({ id: "home.pendencias.titulo" })}
            </h1>

            <button
              type="button"
              onClick={() => setIncluirSilenciadas((atual) => !atual)}
              className="text-apoio ml-auto text-acento underline"
            >
              {intl.formatMessage({
                id: incluirSilenciadas ? "home.ocultarSilenciadas" : "home.mostrarSilenciadas",
              })}
            </button>
          </div>

          <Feed incluirSilenciadas={incluirSilenciadas} />
        </main>

        <aside />
      </div>
    </div>
  );
}

function Feed({ incluirSilenciadas }: { incluirSilenciadas: boolean }) {
  const intl = useIntl();
  const pendencias = usePendencias(incluirSilenciadas);

  if (pendencias.isPending) {
    return (
      <p className="text-apoio mt-4 text-tinta-secundaria">
        {intl.formatMessage({ id: "home.pendencias.carregando" })}
      </p>
    );
  }

  if (pendencias.isError) {
    return (
      <p role="alert" className="text-registro mt-4 rounded-bloco bg-fundo-urgencia p-4 text-urgencia">
        {intl.formatMessage({ id: chaveDoErro(pendencias.error) })}
      </p>
    );
  }

  if (pendencias.data.length === 0) {
    return (
      <div className="mt-4 rounded-bloco border border-contorno p-5">
        <p className="text-registro text-tinta">
          {intl.formatMessage({ id: "home.pendencias.vazio" })}
        </p>
        <p className="text-apoio mt-1 text-tinta-secundaria">
          {intl.formatMessage({ id: "home.pendencias.vazio.apoio" })}
        </p>
      </div>
    );
  }

  return (
    <ul className="mt-4 flex flex-col gap-2.5">
      {pendencias.data.map((pendencia) => (
        <li key={`${pendencia.kind}-${pendencia.sourceId}`}>
          <ItemDePendencia pendencia={pendencia} />
        </li>
      ))}
    </ul>
  );
}

/** O fundo diz o estado, e a palavra tambem: cor nunca e o unico portador (DESIGN.md 3). */
const FUNDO: Record<Estado, string> = {
  vencida: "bg-fundo-urgencia",
  venceHoje: "bg-fundo-acento",
  aVencer: "bg-fundo-acento",
  semMarca: "border border-contorno",
};

function ItemDePendencia({ pendencia }: { pendencia: Pendencia }) {
  const intl = useIntl();
  const silenciar = useSilenciar();
  const deixarDeSilenciar = useDeixarDeSilenciar();
  const cumprir = useCumprirOrientacao();

  const estado = estadoDe(pendencia);

  /*
   * O `!` aqui nao e descuido, e a causa dele nao esta nesta tela: <b>o contrato nao
   * declara `required` em nenhum schema</b>, entao todo campo de resposta chega opcional
   * no tipo gerado — inclusive os que o backend sempre preenche. Enquanto for assim, ou
   * se afirma o que se sabe, ou toda tela enche de `?? ""` e de ramo morto.
   *
   * A correcao e do lado do contrato, e esta registrada como divida no ROADMAP.md.
   */
  const alvo = { kind: pendencia.kind!, sourceId: pendencia.sourceId! };

  return (
    <article className={`rounded-bloco p-4 ${FUNDO[estado]}`}>
      <Etiqueta pendencia={pendencia} estado={estado} />

      <p className="text-registro text-tinta">{fatoDe(pendencia, intl)}</p>

      {pendencia.animalName !== undefined ? (
        <p className="text-apoio mt-0.5 text-tinta-secundaria">
          {intl.formatMessage({ id: "home.animal" }, { nome: pendencia.animalName })}
        </p>
      ) : null}

      {/* A regra 5.3: nunca cobrar duas pessoas sem dizer que a outra ja fez. */}
      {pendencia.lastFulfilledByName !== undefined && pendencia.lastFulfilledAt !== undefined ? (
        <p className="text-apoio mt-1.5 text-tinta-secundaria">
          {intl.formatMessage(
            { id: "home.jaFeito" },
            {
              nome: pendencia.lastFulfilledByName,
              quando: quandoDe(pendencia.lastFulfilledAt, intl),
            },
          )}
        </p>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-4">
        {pendencia.kind === "ORIENTACAO" ? (
          <button
            type="button"
            disabled={cumprir.isPending}
            onClick={() =>
              cumprir.mutate({
                animalId: pendencia.animalId!,
                careInstructionId: pendencia.sourceId!,
              })
            }
            className="text-interface min-h-toque rounded-pilula bg-acento px-5 font-bold text-sobre-acento disabled:opacity-70"
          >
            {intl.formatMessage({
              id: cumprir.isPending ? "home.acao.cumprindo" : "home.acao.cumprir",
            })}
          </button>
        ) : null}

        {podeSilenciar(pendencia) ? (
          <button
            type="button"
            onClick={() =>
              pendencia.silenced === true
                ? deixarDeSilenciar.mutate(alvo)
                : silenciar.mutate(alvo)
            }
            className="text-interface min-h-toque text-tinta-secundaria underline"
          >
            {intl.formatMessage({
              id: pendencia.silenced === true ? "home.acao.voltarACobrar" : "home.acao.silenciar",
            })}
          </button>
        ) : null}
      </div>
    </article>
  );
}

function Etiqueta({ pendencia, estado }: { pendencia: Pendencia; estado: Estado }) {
  const intl = useIntl();

  if (pendencia.silenced === true) {
    return (
      <p className="text-apoio mb-1 font-bold text-tinta-secundaria">
        {intl.formatMessage({ id: "home.silenciada" })}
      </p>
    );
  }

  if (estado === "semMarca") {
    return null;
  }

  const dias = pendencia.dueOn !== undefined ? diasAte(pendencia.dueOn) : 0;

  const texto =
    estado === "vencida"
      ? intl.formatMessage({ id: "home.estado.vencida" }, { dias: Math.abs(dias) })
      : estado === "venceHoje"
        ? intl.formatMessage({ id: "home.estado.venceHoje" })
        : intl.formatMessage({ id: "home.estado.aVencer" }, { dias });

  return (
    <p className={`text-apoio mb-1 font-bold ${estado === "vencida" ? "text-urgencia" : "text-acento"}`}>
      {texto}
    </p>
  );
}

/**
 * O fato, por tipo.
 *
 * Dois dos cinco NAO usam o `description` do servidor, e por motivos diferentes: o do
 * consentimento e frase de sistema escrita em portugues sem acento no backend, e o do
 * convite e o e-mail de quem foi convidado — dado, e nao frase. Nos dois a tela escreve,
 * porque a mensagem do servidor nunca vira texto de tela.
 */
function fatoDe(pendencia: Pendencia, intl: IntlShape): string {
  if (pendencia.kind === "CONSENTIMENTO_PENDENTE") {
    return intl.formatMessage({ id: "home.consentimento" });
  }

  if (pendencia.kind === "CONVITE_PENDENTE") {
    return intl.formatMessage(
      { id: "home.convite.naoAceito" },
      { email: pendencia.description ?? "" },
    );
  }

  return pendencia.description ?? "";
}

/**
 * O dia em que foi feito, sem hora.
 *
 * Le so a parte da data da string e nao constroi instante nenhum: o `lastFulfilledAt`
 * viaja como LocalDateTime, <b>sem fuso</b>, entao qualquer conversao seria um chute com
 * cara de precisao.
 */
function quandoDe(instante: string, intl: IntlShape): string {
  const dia = instante.slice(0, 10);
  const dias = diasAte(dia);

  if (dias === 0) {
    return intl.formatMessage({ id: "home.quando.hoje" });
  }
  if (dias === -1) {
    return intl.formatMessage({ id: "home.quando.ontem" });
  }

  return intl.formatMessage(
    { id: "home.quando.em" },
    { data: intl.formatDate(dataLocalDe(dia), { day: "numeric", month: "long" }) },
  );
}
