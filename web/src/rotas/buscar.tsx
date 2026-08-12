import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { useBuscaDeAnimais, type AnimalEncontrado } from "../dados/conta.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 35 · rotina — A busca", de
 * `design/IdentidadeVisual/Telas Petfy - Fim, reencontro e conta.dc.html`.
 *
 * <b>Um campo so para nome, microchip e RGA</b>, porque quem digita nao sabe em qual esta digitando.
 * Obrigar a escolher o tipo antes de buscar seria pedir a pessoa a resposta que ela veio procurar.
 *
 * ------------------------------------------------------------ a frase que quase nenhum produto teria
 *
 * <b>"Existem outros animais com microchip comecando em 9810 no Petfy. Voce nao tem acesso a eles, e
 * por isso nao aparecem aqui."</b>
 *
 * A saida obvia seria mostrar a lista curta e calar sobre o resto — e o efeito seria a pessoa concluir
 * que o animal que ela procura nao esta no produto. Dizer que ele existe e que ela nao o alcanca e a
 * unica resposta verdadeira, e e ela que torna util o caminho seguinte: a busca de animal encontrado,
 * que e publica e existe desde a Tela 34.
 */

export const Route = createFileRoute("/buscar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Buscar,
});

function Buscar() {
  const intl = useIntl();

  const [termo, setTermo] = useState("");
  const resultado = useBuscaDeAnimais(termo);

  const meus = resultado.data?.mine ?? [];
  const pelaOrganizacao = resultado.data?.throughOrganization ?? [];
  const curto = termo.trim().length > 0 && termo.trim().length < 3;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "640px", margin: "0 auto" }}>
        <h1 style={titulo}>{intl.formatMessage({ id: "buscar.titulo" })}</h1>

        <input
          type="search"
          value={termo}
          onChange={(evento) => setTermo(evento.target.value)}
          placeholder={intl.formatMessage({ id: "buscar.dica" })}
          aria-label={intl.formatMessage({ id: "buscar.dica" })}
          style={{
            border: "1px solid oklch(0.82 0.012 150)",
            borderRadius: "8px",
            padding: "14px 16px",
            fontSize: "17px",
            minHeight: "52px",
            width: "100%",
            background: "oklch(1 0 0)",
            fontFamily: "inherit",
            marginBottom: "8px",
          }}
        />

        {curto && <div style={nota}>{intl.formatMessage({ id: "buscar.curta" })}</div>}

        {resultado.isFetching && (
          <div style={nota}>{intl.formatMessage({ id: "buscar.procurando" })}</div>
        )}

        {resultado.data !== undefined && (
          <div style={{ marginTop: "24px", display: "flex", flexDirection: "column", gap: "26px" }}>
            <Grupo
              titulo={intl.formatMessage({ id: "buscar.seusAnimais" })}
              itens={meus}
              vazio={intl.formatMessage({ id: "buscar.nenhumSeu" })}
            />

            {/* O segundo grupo só existe quando a pessoa está agindo por uma organização. */}
            {resultado.data.organizationName !== null &&
              resultado.data.organizationName !== undefined && (
                <Grupo
                  titulo={intl.formatMessage(
                    { id: "buscar.pelaOrganizacao" },
                    { organizacao: resultado.data.organizationName },
                  )}
                  itens={pelaOrganizacao}
                  vazio={intl.formatMessage({ id: "buscar.nenhumDaOrganizacao" })}
                />
              )}

            {/*
             * A frase mais incomum da tela. Ela não mostra os outros, e não diz quantos são: diz que
             * existem, e para onde ir.
             */}
            {resultado.data.othersExist === true && (
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "18px 20px", fontSize: "15px", lineHeight: 1.65, color: CINZA }}>
                {intl.formatMessage({ id: "buscar.existemOutros" })}{" "}
                <Link to="/encontrado" style={{ color: VERDE }}>
                  {intl.formatMessage({ id: "buscar.useAEncontrado" })}
                </Link>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function Grupo({
  titulo: rotulo,
  itens,
  vazio,
}: {
  titulo: string;
  itens: AnimalEncontrado[];
  vazio: string;
}) {
  const intl = useIntl();

  return (
    <section>
      <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "12px" }}>
        {rotulo}
      </div>

      {itens.length === 0 ? (
        <div style={{ fontSize: "15px", color: CINZA, lineHeight: 1.6 }}>{vazio}</div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {itens.map((animal) => (
            <Link
              key={animal.animalId}
              to="/animais/$animalId"
              params={{ animalId: animal.animalId ?? "" }}
              style={{
                display: "block",
                border: "1px solid oklch(0.90 0.008 150)",
                borderRadius: "12px",
                padding: "14px 16px",
                background: "oklch(1 0 0)",
                textDecoration: "none",
                color: "inherit",
                minHeight: "48px",
              }}
            >
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 500 }}>
                {animal.name}
                {/* "Amora · tutor Ricardo Alves" — duas mensagens, porque o ICU não aceita chave
                    vazia em `select` e o tutor só aparece no grupo da organização. */}
                {animal.holderName !== null && animal.holderName !== undefined
                  ? intl.formatMessage({ id: "buscar.tutor" }, { quem: animal.holderName })
                  : ""}
              </div>
              {animal.microchipNumber !== null && animal.microchipNumber !== undefined && (
                <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px", color: CINZA, marginTop: "3px" }}>
                  {animal.microchipNumber}
                </div>
              )}
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "30px",
  fontWeight: 500,
  margin: "0 0 18px",
  letterSpacing: "-0.02em",
} as const;

const nota = {
  fontSize: "13px",
  color: CINZA,
  lineHeight: 1.55,
} as const;
