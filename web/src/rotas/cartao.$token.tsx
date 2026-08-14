import { createFileRoute } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import { CartaoDoAnimal } from "../componentes/CartaoDoAnimal.tsx";
import { useCartaoCompartilhado, type CartaoCompartilhado } from "../dados/cartao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 04 · na mao — o cartao de emergencia", de `design/IdentidadeVisual/Telas Petfy.dc.html`
 * (bloco "Telas 04–06"), com o estado vencido da Tela 14 ("Abrigo e estados").
 *
 * <b>Publica, e e o ponto.</b> Quem abre este link pode ser o veterinario de plantao as 3h da
 * manha com o animal na mesa. Nao ha sessao, nao ha cadastro, e a moldura e a minima — o
 * desenho do rodape e explicito: "sem rodape institucional entre o dedo e o telefone do
 * veterinario".
 *
 * ------------------------------------------------------------------- vazio nao e negado
 *
 * O tutor escolhe o que o link alcanca. <b>Um bloco vazio e um bloco negado parecem iguais na
 * tela e sao coisas opostas</b>: "nao ha alergia registrada" contra "o tutor nao autorizou este
 * pedaco". Por isso o DTO manda os `scopes` junto, e a tela diz qual dos dois aconteceu.
 */

export const Route = createFileRoute("/cartao/$token")({
  /* Sem `beforeLoad`: quem abre este cartao nao tem conta, e nao vai criar uma agora. */
  component: Cartao,
});

function Cartao() {
  const { token } = Route.useParams();
  const intl = useIntl();
  const consulta = useCartaoCompartilhado(token);

  if (consulta.isPending) {
    return (
      <Centro>
        <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "cartao.carregando" })}
        </div>
      </Centro>
    );
  }

  /*
   * TOKEN INVALIDO, REVOGADO E VENCIDO RESPONDEM IGUAL, e o backend faz isso de proposito:
   * distinguir diria a quem tem um link velho que aquele animal existe. Entao a tela mostra o
   * cartao vencido para os tres — e a saida que ela oferece serve nos tres casos.
   */
  if (consulta.isError || consulta.data === undefined) {
    return <Vencido />;
  }

  return <Ficha cartao={consulta.data} />;
}

function Ficha({ cartao }: { cartao: CartaoCompartilhado }) {
  const intl = useIntl();

  const nome = cartao.animalName ?? "";
  const escopos = cartao.scopes ?? [];
  const condicoes = cartao.conditions ?? [];

  /* Encerrada nao entra: dizer a quem socorre que o animal tem uma doenca que ele ja nao tem
     faria a pessoa agir sobre um fato que deixou de valer. */
  const vigentes = condicoes.filter((condicao) => condicao.resolvedAt === undefined || condicao.resolvedAt === null);

  const alergias = vigentes
    .filter((condicao) => condicao.kind === "ALERGIA")
    .map((condicao) => condicao.description ?? "");

  const outras = vigentes
    .filter((condicao) => condicao.kind !== "ALERGIA")
    .map((condicao) => condicao.description ?? "");

  return (
    <Centro>
      <CartaoDoAnimal
        marca={intl.formatMessage({ id: "cartao.marca" })}
        nome={nome}
        descricao={descricao(cartao, intl)}
        contatos={(cartao.contacts ?? []).map((contato) => ({
          nome: contato.name ?? "",
          telefone: contato.phone,
        }))}
        alergias={alergias}
        condicoes={outras}
        medicacao={cartao.ongoingCare ?? []}
        vacinas={(cartao.vaccines ?? []).map((vacina) => ({
          nome: vacina.vaccineName ?? "",
          status: vacina.status,
          proximaDose: vacina.nextDoseDate,
        }))}
        rodape={
          cartao.expiresAt === undefined || cartao.expiresAt === null
            ? intl.formatMessage({ id: "cartao.semPrazo" }, { nome })
            : intl.formatMessage(
                { id: "cartao.rodape" },
                {
                  nome,
                  data: intl.formatDate(cartao.expiresAt, {
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  }),
                },
              )
        }
      />

      <ForaDoEscopo escopos={escopos} />
    </Centro>
  );
}

/**
 * O que este link NAO alcanca, dito uma vez so no fim.
 *
 * Sem isto, um cartao sem o bloco de alergias parece afirmar que o animal nao tem nenhuma — e
 * quem esta socorrendo agiria sobre essa leitura.
 */
function ForaDoEscopo({ escopos }: { escopos: string[] }) {
  const intl = useIntl();

  const faltando = (
    [
      ["CONDICOES", "cartao.escopo.condicoes"],
      ["CONTATO", "cartao.escopo.contato"],
      ["CARTEIRA", "cartao.escopo.carteira"],
    ] as const
  )
    .filter(([escopo]) => !escopos.includes(escopo))
    .map(([, chave]) => intl.formatMessage({ id: chave }));

  if (faltando.length === 0) {
    return null;
  }

  return (
    <div style={{ marginTop: "16px", fontSize: "13px", lineHeight: 1.55, color: "oklch(0.5 0.015 150)" }}>
      {intl.formatMessage(
        { id: "cartao.foraDoEscopo" },
        { o_que: intl.formatList(faltando, { type: "conjunction" }) },
      )}
    </div>
  );
}

/** "Cao · SRD · 6 anos" — montada com o que existe, e sem buraco onde falta. */
function descricao(cartao: CartaoCompartilhado, intl: ReturnType<typeof useIntl>): string {
  const partes: string[] = [];

  if (cartao.animalType !== undefined && cartao.animalType !== null) {
    partes.push(cartao.animalType);
  }
  if (cartao.animalBreed !== undefined && cartao.animalBreed !== null) {
    partes.push(cartao.animalBreed);
  }

  const anos = idadeEmAnos(cartao.animalBornDate);
  if (anos !== undefined) {
    partes.push(intl.formatMessage({ id: "cartao.idade" }, { anos }));
  }

  return partes.join(" · ");
}

function idadeEmAnos(nascimento: string | undefined | null): number | undefined {
  if (nascimento === undefined || nascimento === null || nascimento === "") {
    return undefined;
  }

  const data = new Date(nascimento);
  const hoje = new Date();

  let anos = hoje.getFullYear() - data.getFullYear();
  const antesDoAniversario =
    hoje.getMonth() < data.getMonth() ||
    (hoje.getMonth() === data.getMonth() && hoje.getDate() < data.getDate());

  if (antesDoAniversario) {
    anos -= 1;
  }

  return anos < 0 ? undefined : anos;
}

/**
 * O cartao vencido (Tela 14).
 *
 * <b>Diz o que fazer, e nao so que acabou.</b> Quem esta lendo isto pode estar com o animal
 * passando mal — mandar embora sem saida seria o pior defeito desta tela.
 */
function Vencido() {
  const intl = useIntl();

  return (
    <Centro>
      <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ background: "oklch(0.62 0.02 150)", padding: "20px 22px", color: "oklch(1 0 0)" }}>
          <span style={{ fontSize: "13px", letterSpacing: "0.04em", textTransform: "uppercase" }}>
            {intl.formatMessage({ id: "cartao.expirado.marca" })}
          </span>
        </div>
        <div style={{ padding: "24px 22px" }}>
          <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, marginBottom: "12px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "cartao.expirado.titulo" })}
          </div>
          <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "cartao.expirado.texto" })}
          </div>
        </div>
      </div>
    </Centro>
  );
}

/** 390 px, como o desenho: este cartao nasceu para ser lido na mao. */
function Centro({ children }: { children: React.ReactNode }) {
  return (
    <main style={{ minHeight: "100dvh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "24px" }}>
      <div style={{ width: "100%", maxWidth: "390px" }}>{children}</div>
    </main>
  );
}
