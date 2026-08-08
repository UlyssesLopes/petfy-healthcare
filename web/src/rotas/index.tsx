import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl, type IntlShape } from "react-intl";

import { LinhaDoTempo } from "../componentes/LinhaDoTempo.tsx";
import { Pata } from "../componentes/Pata.tsx";
import { Rede } from "../componentes/Rede.tsx";
import { useAnimais, type Animal } from "../dados/animais.ts";
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
import { anosDesde, dataLocalDe, diasAte } from "../i18n/datas.ts";
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
  const animais = useAnimais();
  const sair = useSair();

  const [incluirSilenciadas, setIncluirSilenciadas] = useState(false);
  const [animalEscolhido, setAnimalEscolhido] = useState<string | undefined>(undefined);

  const lista = animais.data ?? [];
  const ativo = lista.find((a) => a.animalId === animalEscolhido) ?? lista[0];

  return (
    // O fundo da pagina e a superficie; o conteiner da aplicacao tem raio de 16 px e
    // contorno de 1 px — o raio escalonado e identidade (DESIGN.md 4), nao decoracao.
    <div className="min-h-dvh bg-superficie p-4 sm:p-6">
      <div className="mx-auto max-w-5xl overflow-hidden rounded-app border border-contorno bg-superficie">
        <header className="flex items-center gap-4 border-b border-contorno bg-superficie-elevada px-5 py-3.5 sm:gap-[18px]">
          {/* Nome de marca nao se traduz: e o unico texto literal da tela. */}
          <span className="text-resumo text-acento">Petfy</span>

          <nav className="flex gap-[18px]">
            <span className="text-interface text-tinta">
              {intl.formatMessage({ id: "home.nav.inicio" })}
            </span>
          </nav>

          <span className="text-apoio ml-auto hidden text-tinta-secundaria sm:inline">
            {contexto.data?.personName ?? intl.formatMessage({ id: "home.contexto.voce" })}
          </span>

          <button
            type="button"
            onClick={sair}
            className="text-interface text-tinta-secundaria hover:underline"
          >
            {intl.formatMessage({ id: "home.sair" })}
          </button>
        </header>

        {lista.length > 0 ? (
          <div className="flex flex-wrap gap-2 px-5 pt-3.5">
            {lista.map((animal) => (
              <BotaoDeAnimal
                key={animal.animalId}
                animal={animal}
                ativo={animal.animalId === ativo?.animalId}
                aoEscolher={() => setAnimalEscolhido(animal.animalId)}
              />
            ))}
          </div>
        ) : null}

        {/*
          Duas colunas, e a segunda esta reservada para a percepcao (5.5): ela vive FORA do
          eixo do tempo, e a posicao existe desde ja para que o dia em que ela chegar nao
          desfaca o layout. Nao ha caixa vazia aqui — a propria 5.5 chama isso de moldura
          vazia.
        */}
        <div className="grid gap-[22px] px-5 pb-6 pt-[18px] lg:grid-cols-[1fr_292px]">
          <main className="min-w-0">
            {ativo !== undefined ? <Heroi animal={ativo} /> : null}

            <div className="mt-6 flex flex-wrap items-baseline gap-3">
              <h2 className="text-rotulo uppercase text-tinta-secundaria">
                {intl.formatMessage({ id: "home.pendencias.titulo" })}
              </h2>

              <button
                type="button"
                onClick={() => setIncluirSilenciadas((atual) => !atual)}
                className="text-apoio ml-auto text-tinta-secundaria hover:underline"
              >
                {intl.formatMessage({
                  id: incluirSilenciadas ? "home.ocultarSilenciadas" : "home.mostrarSilenciadas",
                })}
              </button>
            </div>

            <Feed incluirSilenciadas={incluirSilenciadas} animalId={ativo?.animalId} />

            {/*
              A ordem das secoes e a tese da 5.4 virando layout: quem cuida vem ANTES dos
              dados. O feed fica no topo porque e o centro da area do tutor (9.3), e a
              linha do tempo fecha — ela e o objeto central do produto, e nao a abertura
              da tela.
            */}
            {ativo?.animalId !== undefined && ativo.name !== undefined ? (
              <>
                <Rede animalId={ativo.animalId} animalNome={ativo.name} />
                <LinhaDoTempo animalId={ativo.animalId} animalNome={ativo.name} />
              </>
            ) : null}
          </main>

          <aside />
        </div>
      </div>
    </div>
  );
}

function BotaoDeAnimal({
  animal,
  ativo,
  aoEscolher,
}: {
  animal: Animal;
  ativo: boolean;
  aoEscolher: () => void;
}) {
  return (
    <button
      type="button"
      onClick={aoEscolher}
      aria-pressed={ativo}
      className={`flex items-center gap-2 rounded-pilula py-1.5 pl-1.5 pr-3.5 text-[0.875rem] font-bold ${
        ativo
          ? "bg-fundo-acento text-tinta"
          : "border border-contorno text-tinta-secundaria hover:text-tinta"
      }`}
    >
      <span className="grid size-[26px] place-items-center rounded-circulo bg-fundo-acento text-acento">
        <Pata className="size-[15px]" />
      </span>
      {animal.name}
    </button>
  );
}

/** O animal e o protagonista visual, e a tela abre por ele — nao por dados (DESIGN.md 1). */
function Heroi({ animal }: { animal: Animal }) {
  const intl = useIntl();

  const partes: string[] = [];

  if (animal.breed !== undefined) {
    partes.push(animal.breed);
  } else if (animal.species !== undefined) {
    partes.push(intl.formatMessage({ id: `home.especie.${animal.species}` }));
  }

  if (animal.bornDate !== undefined) {
    partes.push(intl.formatMessage({ id: "home.idade" }, { anos: anosDesde(animal.bornDate) }));
  }

  return (
    <div className="flex items-center gap-4">
      <span className="grid size-[60px] shrink-0 place-items-center rounded-circulo bg-fundo-acento text-acento">
        <Pata className="size-8" />
      </span>

      <div className="min-w-0">
        <h1 className="text-nome-animal text-tinta">{animal.name}</h1>
        {partes.length > 0 ? (
          <p className="text-apoio mt-0.5 text-tinta-secundaria">{partes.join(" · ")}</p>
        ) : null}
      </div>
    </div>
  );
}

function Feed({
  incluirSilenciadas,
  animalId,
}: {
  incluirSilenciadas: boolean;
  animalId: string | undefined;
}) {
  const intl = useIntl();
  const pendencias = usePendencias(incluirSilenciadas);

  if (pendencias.isPending) {
    return (
      <p className="text-apoio mt-3 text-tinta-secundaria">
        {intl.formatMessage({ id: "home.pendencias.carregando" })}
      </p>
    );
  }

  if (pendencias.isError) {
    return (
      <p
        role="alert"
        className="text-registro mt-3 rounded-bloco bg-fundo-urgencia p-4 text-urgencia"
      >
        {intl.formatMessage({ id: chaveDoErro(pendencias.error) })}
      </p>
    );
  }

  /*
   * O seletor filtra o feed pelo animal escolhido, e o que NAO tem animal fica sempre —
   * hoje isso e o consentimento, que bloqueia o resto do produto e nao pode sumir por
   * causa de um filtro. Filtrar nao e montar: a ordem e o conteudo continuam vindo do
   * servidor, que e o que a 9.5 exige deste feed.
   */
  const visiveis = pendencias.data.filter(
    (p) => p.animalId === undefined || animalId === undefined || p.animalId === animalId,
  );

  if (visiveis.length === 0) {
    return (
      <div className="mt-3 rounded-bloco border border-contorno p-5">
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
    <ul className="mt-3 flex flex-col gap-2.5">
      {visiveis.map((pendencia) => (
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
   * O `!` aqui nao e descuido, e a causa dele nao esta nesta tela: o contrato nao declara
   * `required` em nenhum schema, entao todo campo de resposta chega opcional no tipo
   * gerado — inclusive os que o backend sempre preenche. A divida esta no ROADMAP.md.
   */
  const alvo = { kind: pendencia.kind!, sourceId: pendencia.sourceId! };

  return (
    <article className={`rounded-bloco px-[17px] py-[15px] ${FUNDO[estado]}`}>
      <Etiqueta pendencia={pendencia} estado={estado} />

      <p className="text-registro text-tinta">{fatoDe(pendencia, intl)}</p>

      {/* A regra 5.3: nunca cobrar duas pessoas sem dizer que a outra ja fez. */}
      {pendencia.lastFulfilledByName !== undefined && pendencia.lastFulfilledAt !== undefined ? (
        <p className="text-apoio mt-1 text-tinta-secundaria">
          {intl.formatMessage(
            { id: "home.jaFeito" },
            {
              nome: pendencia.lastFulfilledByName,
              quando: quandoDe(pendencia.lastFulfilledAt, intl),
            },
          )}
        </p>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-3.5">
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
            className="text-interface inline-flex min-h-toque items-center rounded-pilula bg-acento px-[18px] font-bold text-sobre-acento disabled:opacity-70"
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
            className="text-interface inline-flex min-h-toque items-center text-tinta-secundaria hover:underline"
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
      <p className="text-rotulo mb-1 normal-case text-tinta-secundaria">
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
    <p
      className={`text-rotulo mb-1 normal-case ${
        estado === "vencida" ? "text-urgencia" : "text-acento"
      }`}
    >
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
