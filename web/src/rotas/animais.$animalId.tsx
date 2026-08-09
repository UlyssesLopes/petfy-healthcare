import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import {
  BlocoDeAnexos,
  BlocoDeCondicoes,
  BlocoDePeso,
  BlocoDeVacinacao,
} from "../componentes/Carteira.tsx";
import { LinhaDaVida } from "../componentes/LinhaDaVida.tsx";
import { useLinhaDoTempo } from "../dados/animal.ts";
import {
  useAnexos,
  useAnimal,
  useCarteira,
  useCondicoes,
  usePesagens,
  useRedeDeCuidado,
} from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

export const Route = createFileRoute("/animais/$animalId")({
  // Mesma guarda da home: o token vive em memoria, e esta tela so existe autenticada.
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: VidaDoAnimal,
});

/**
 * A vida do animal — carteira a esquerda, linha do tempo a direita (Tela 02).
 *
 * <b>A tese do produto esta no layout.</b> A carteira responde "como ele esta agora"; a
 * linha do tempo responde "como ele chegou aqui". As duas na mesma tela, porque separa-las
 * em abas faria a segunda virar arquivo — e o registro que sobrevive as pessoas e
 * justamente a segunda.
 */
function VidaDoAnimal() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const rede = useRedeDeCuidado(animalId);
  const carteira = useCarteira(animalId);
  const condicoes = useCondicoes(animalId);
  const pesagens = usePesagens(animalId);
  const anexos = useAnexos(animalId);
  const linha = useLinhaDoTempo(animalId);

  if (animal.isPending) {
    return (
      <main className="mx-auto max-w-[1360px] p-10">
        <p className="text-corpo-denso text-tinta-secundaria">
          {intl.formatMessage({ id: "animal.carregando" })}
        </p>
      </main>
    );
  }

  if (animal.error !== null || animal.data === undefined) {
    return (
      <main className="mx-auto max-w-[1360px] p-10">
        <p className="text-corpo-denso text-tinta">
          {intl.formatMessage({ id: chaveDoErro(animal.error) })}
        </p>
        <Link to="/" className="text-corpo-denso mt-4 inline-flex min-h-toque items-center text-musgo underline">
          {intl.formatMessage({ id: "animal.voltar" })}
        </Link>
      </main>
    );
  }

  const bicho = animal.data;
  const custodia = (rede.data ?? []).find((membro) => membro.holder === true);

  return (
    <main className="mx-auto max-w-[1360px] p-10">
      <div className="border-linha-media rounded-bloco overflow-hidden border bg-papel">
        <Cabecalho animal={bicho} custodia={custodia} />

        <div className="grid grid-cols-1 lg:grid-cols-[340px_1fr]">
          <div className="border-linha flex flex-col gap-6.5 border-b px-7 pt-7 pb-10 lg:border-r lg:border-b-0">
            <BlocoDeVacinacao linhas={carteira.linhas} />
            <BlocoDeCondicoes condicoes={condicoes.data ?? []} />
            <BlocoDePeso pesagens={pesagens.data ?? []} />
            <BlocoDeAnexos anexos={anexos.data ?? []} />
          </div>

          <div>
            {linha.isPending ? (
              <p className="text-corpo-denso p-10 text-tinta-secundaria">
                {intl.formatMessage({ id: "animal.linha.carregando" })}
              </p>
            ) : (
              <LinhaDaVida entradas={linha.data ?? []} />
            )}
          </div>
        </div>
      </div>
    </main>
  );
}

type Animal = NonNullable<ReturnType<typeof useAnimal>["data"]>;
type Membro = ReturnType<typeof useRedeDeCuidado>["data"] extends (infer T)[] | undefined
  ? T
  : never;

function Cabecalho({ animal, custodia }: { animal: Animal; custodia: Membro | undefined }) {
  const intl = useIntl();

  const identidade = [
    animal.species === undefined
      ? undefined
      : intl.formatMessage({ id: `animal.especie.${animal.species}` }),
    animal.breed,
    animal.gender === undefined
      ? undefined
      : intl.formatMessage({ id: `animal.genero.${animal.gender}` }),
  ].filter((parte): parte is string => parte !== undefined && parte !== "");

  return (
    <header className="border-linha grid grid-cols-1 items-center gap-7 border-b bg-superficie px-10 pt-9 pb-7 sm:grid-cols-[auto_1fr] lg:grid-cols-[auto_1fr_auto]">
      {/* Circulo porque e um ser vivo — a regra de forma da secao 04. Sem foto, o anel
          fica listrado em vez de mostrar iniciais: iniciais sao de pessoa. */}
      <div
        aria-hidden
        className="rounded-ser size-24 bg-[repeating-linear-gradient(135deg,var(--petfy-fundo-musgo)_0_8px,var(--petfy-fundo-sutil)_8px_16px)]"
      />

      <div>
        <h1 className="text-nome-animal font-nome mb-2">{animal.name}</h1>

        <div className="text-corpo-denso flex flex-wrap gap-x-5 gap-y-1.5 text-tinta-media">
          {identidade.length > 0 && <span>{identidade.join(" · ")}</span>}

          {animal.bornDate !== undefined && (
            <span>
              {intl.formatMessage(
                { id: "animal.nascido" },
                {
                  data: intl.formatDate(animal.bornDate, {
                    month: "2-digit",
                    year: "numeric",
                    timeZone: "UTC",
                  }),
                },
              )}
            </span>
          )}

          {animal.microchipNumber !== undefined && (
            <span className="font-dado">
              {intl.formatMessage({ id: "animal.microchip" }, { numero: animal.microchipNumber })}
            </span>
          )}

          {animal.generalRegistry !== undefined && (
            <span className="font-dado">
              {intl.formatMessage({ id: "animal.rga" }, { numero: animal.generalRegistry })}
            </span>
          )}
        </div>

        {custodia !== undefined && (
          <p className="text-apoio mt-2.5 text-tinta-media">
            {intl.formatMessage(
              { id: "animal.custodia" },
              {
                nome: <strong className="font-medium text-tinta">{custodia.name}</strong>,
                desde:
                  custodia.since === undefined
                    ? ""
                    : intl.formatDate(new Date(custodia.since), { dateStyle: "short" }),
              },
            )}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-2.5">
        <button
          type="button"
          disabled
          className="text-corpo-denso rounded-controle min-h-toque bg-musgo px-5 py-3 font-medium text-sobre-musgo disabled:opacity-60"
        >
          {intl.formatMessage({ id: "animal.acao.registrar" })}
        </button>
        <button
          type="button"
          disabled
          className="text-corpo-denso rounded-controle border-linha-forte min-h-toque border bg-superficie px-5 py-3 font-medium text-tinta disabled:opacity-60"
        >
          {intl.formatMessage({ id: "animal.acao.compartilhar" })}
        </button>
      </div>
    </header>
  );
}
