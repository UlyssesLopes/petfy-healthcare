import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { useRedeDeCuidado } from "../dados/carteira.ts";
import { usePedirConcordancia } from "../dados/colonia.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 44 · o gato que virou alguem — Da praca para uma casa", de
 * `design/IdentidadeVisual/Telas Petfy - Animal comunitario e encaminhamento.dc.html`.
 *
 * <b>"Os tres anos de praca vao junto — a vida da Branquinha nao comeca no dia em que ela entra
 * numa casa."</b> E o que o produto entrega aqui, e o que nenhum outro entrega: um gato de rua
 * ganha o mesmo que um cao de apartamento, uma vida documentada que sobrevive a todo mundo que
 * passou por ela.
 *
 * ------------------------------------------------------ o passo que o desenho nao desenha
 *
 * O mockup diz "Paula passa a responder por ela" logo depois da concordancia da Marta. <b>No
 * produto ha um terceiro passo, e ele nao e burocracia:</b> quando alguem do grupo concorda, Paula
 * recebe um CONVITE para responder pelo animal, e a custodia passa quando ela aceita.
 *
 * O produto inteiro exige que quem recebe um animal consinta — e assim na transferencia de
 * titularidade e na adocao do abrigo. Passar a custodia sem o aceite dela colocaria um animal sob
 * a responsabilidade de quem ainda nao disse sim, e este seria o unico ponto do Petfy em que isso
 * aconteceria. A tela diz isso com todas as letras, antes do gesto.
 */

export const Route = createFileRoute("/animais/$animalId_/adotar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Adotar,
});

function Adotar() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const rede = useRedeDeCuidado(animalId);
  const pedir = usePedirConcordancia();

  const [paraQuem, setParaQuem] = useState("");
  const [motivo, setMotivo] = useState("");
  const [enviado, setEnviado] = useState(false);

  const nome = animal.data?.name ?? "";

  /* Quem pode receber o animal: as pessoas que ja alcancam o grupo. Uma busca aberta por qualquer
     pessoa do Petfy daria a quem cuida da colonia um diretorio de usuarios, que ninguem lhe deu. */
  const candidatos = (rede.data ?? []).filter((membro) => membro.holder !== true);

  if (animal.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "fim.oQueE" })} />;
  }

  if (animal.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "fim.oQueE" })}
        erro={animal.error}
        aoTentarDeNovo={() => void animal.refetch()}
        carregando={animal.isFetching}
      />
    );
  }

  const podePedir = paraQuem !== "" && !pedir.isPending;

  const enviar = () => {
    pedir.mutate(
      { tipo: "ADOCAO", animalId, paraQuemId: paraQuem, motivo: motivo.trim() || undefined },
      { onSuccess: () => setEnviado(true) },
    );
  };

  const nomeDoDestino =
    candidatos.find((pessoa) => pessoa.personId === paraQuem)?.name ?? "";

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "700px", margin: "0 auto", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "32px 34px 36px" }}>

        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, margin: "0 0 20px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "adotar.titulo" }, { nome })}
        </h1>

        <p style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", margin: "0 0 22px" }}>
          {intl.formatMessage({ id: "adotar.oQueVaiJunto" })}
        </p>

        {enviado ? (
          <div style={{ border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "22px 24px", fontSize: "15px", lineHeight: 1.65 }}>
            <div style={{ marginBottom: "10px" }}>
              {intl.formatMessage({ id: "adotar.pedido.enviado" })}
            </div>
            <div style={{ color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "adotar.viraConvite" }, { quem: nomeDoDestino })}
            </div>
            <div style={{ marginTop: "16px" }}>
              <Link to="/colonia" style={{ color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "colonia.titulo" })}
              </Link>
            </div>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
            <div>
              <label htmlFor="paraQuem" style={rotulo}>
                {intl.formatMessage({ id: "adotar.paraQuem" })}
              </label>
              <select
                id="paraQuem"
                value={paraQuem}
                onChange={(evento) => setParaQuem(evento.target.value)}
                style={{ ...campo, cursor: "pointer" }}
              >
                <option value="">—</option>
                {candidatos.map((pessoa) => (
                  <option key={pessoa.personId} value={pessoa.personId}>
                    {pessoa.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="motivo" style={rotulo}>
                {intl.formatMessage({ id: "adotar.motivo" })}
              </label>
              <textarea
                id="motivo"
                rows={4}
                value={motivo}
                onChange={(evento) => setMotivo(evento.target.value)}
                style={{ ...campo, minHeight: "96px", lineHeight: 1.6, resize: "vertical" }}
              />
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.55 }}>
                {intl.formatMessage({ id: "adotar.motivo.nota" })}
              </div>
            </div>

            {/*
             * O aviso de que falta outra pessoa vem ANTES do botao, e nao depois do gesto.
             * "O desabilitado nunca aparece mudo, e a frase vem antes do gesto" — aqui o botao
             * nem esta desabilitado: ele funciona, e o que a frase evita e a surpresa de o animal
             * nao mudar de mao no instante do clique.
             */}
            <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "8px", padding: "14px 16px", display: "flex", gap: "12px", alignItems: "flex-start" }}>
              <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", border: "3px solid oklch(0.62 0.11 70)", flex: "none", marginTop: "4px" }}></span>
              <span style={{ fontSize: "15px", lineHeight: 1.55 }}>
                {intl.formatMessage({ id: "adotar.precisaDeOutra" })}
              </span>
            </div>

            <div style={{ fontSize: "14px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "adotar.viraConvite" }, { quem: nomeDoDestino || "quem receber" })}
            </div>

            {pedir.error !== null && pedir.error !== undefined && (
              <ErroAoGravar erro={pedir.error} oQue={nome} />
            )}

            <button
              type="button"
              disabled={!podePedir}
              onClick={enviar}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: podePedir ? "oklch(0.46 0.085 150)" : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "14px 24px", minHeight: "48px", cursor: podePedir ? "pointer" : "not-allowed" }}
            >
              {intl.formatMessage({ id: pedir.isPending ? "adotar.pedindo" : "adotar.pedir" })}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

const rotulo = {
  display: "block",
  fontSize: "13px",
  fontWeight: 500,
  color: "oklch(0.42 0.015 150)",
  marginBottom: "7px",
} as const;

const campo = {
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
  fontFamily: "inherit",
} as const;
