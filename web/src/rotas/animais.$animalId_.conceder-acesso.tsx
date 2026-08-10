import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useMemo, useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAnimal } from "../dados/carteira.ts";
import {
  FRASE_DO_ESCOPO,
  useConcederAcesso,
  useOrganizacoes,
  type Escopo,
  type Organizacao,
} from "../dados/acessos.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 09 · area do tutor — Conceder acesso, a escolha de escopo sem a palavra escopo",
 * de `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * O markup vem do arquivo. O problema que ela resolve esta nomeado no proprio desenho: o
 * mais difícil da area do tutor e fazer alguem que nunca ouviu a palavra "escopo" escolher
 * um — e a saida e nao nomear a abstracao, e sim o que a organizacao vai ver.
 *
 * <b>A ESCOLHA DA ORGANIZACAO E UM PASSO A MAIS, e ela nao esta nesta tela do desenho.</b>
 * O desenho comeca com a "Clinica Vet Norte" ja escolhida, e quem escolhe e o passo 4 do
 * onboarding — la o campo se chama "buscar organizacao" e o botao "escolher o que ela ve".
 * Como o onboarding ainda nao existe, o passo vive aqui, com o campo daquele desenho: sem
 * ele a tela seria inalcancavel por quem chega pela rede de quem cuida.
 *
 * <b>A QUINTA LINHA DO DESENHO FICA DESABILITADA, com o motivo ao lado.</b> "Registrar
 * novos atendimentos" e escrita, e o `GrantScope` da API so governa LEITURA — o
 * `GrantLevel` e sempre EDITOR e nao ha campo para conceder escrita. Marcar essa linha
 * seria prometer o que o servidor nao cumpre; esconde-la seria fingir que o desenho nao
 * pediu. A secao 06 pede exatamente o meio: "o desabilitado nunca aparece mudo, e a frase
 * vem antes do gesto".
 *
 * <b>TRES ESCOPOS DA API NAO TEM LINHA NO DESENHO:</b> `PESO`, `ANEXOS` e `CONTATO`. Nao
 * inventei linha para eles — o desenho escolheu quatro, e juntar `ANEXOS` dentro de
 * "diagnosticos, prescricoes e exames" seria conceder anexo sem dizer. Fica registrado: por
 * esta tela, esses tres nao sao concediveis.
 */

export const Route = createFileRoute("/animais/$animalId_/conceder-acesso")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: ConcederAcesso,
});

/** As quatro linhas concediveis, na ordem do desenho. */
const LINHAS: Escopo[] = ["CONDICOES", "CARTEIRA", "PRONTUARIO", "OBSERVACOES"];

/** O desenho mostra as tres de saude marcadas e a de biografia desmarcada. */
const MARCADOS_AO_ABRIR: Escopo[] = ["CONDICOES", "CARTEIRA", "PRONTUARIO"];

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

function ConcederAcesso() {
  const { animalId } = Route.useParams();

  const animal = useAnimal(animalId);
  const nome = animal.data?.name ?? "";

  const [escolhida, setEscolhida] = useState<Organizacao | undefined>(undefined);

  if (escolhida === undefined) {
    return <Escolher animalId={animalId} nome={nome} aoEscolher={setEscolhida} />;
  }

  return <OQueElaVe animalId={animalId} nome={nome} organizacao={escolhida} />;
}

/* ------------------------------------------------ o passo que o onboarding ainda nao faz */

function Escolher({
  animalId,
  nome,
  aoEscolher,
}: {
  animalId: string;
  nome: string;
  aoEscolher: (organizacao: Organizacao) => void;
}) {
  const intl = useIntl();
  const organizacoes = useOrganizacoes();
  const [busca, setBusca] = useState("");

  const achadas = useMemo(() => {
    const alvo = busca.trim().toLowerCase();
    const lista = organizacoes.data ?? [];

    if (alvo === "") {
      return lista;
    }

    return lista.filter((organizacao) =>
      [organizacao.name, organizacao.city, organizacao.state]
        .filter((campo): campo is string => campo !== undefined)
        .some((campo) => campo.toLowerCase().includes(alvo)),
    );
  }, [busca, organizacoes.data]);

  return (
    <Moldura>
      <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
        {intl.formatMessage({ id: "conceder.escolher.titulo" }, { nome })}
      </h1>
      <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "62ch" }}>
        {intl.formatMessage({ id: "conceder.escolher.apoio" })}
      </p>

      <input
        type="search"
        value={busca}
        onChange={(evento) => setBusca(evento.target.value)}
        placeholder={intl.formatMessage({ id: "conceder.escolher.busca" })}
        aria-label={intl.formatMessage({ id: "conceder.escolher.busca" })}
        style={{ fontFamily: "inherit", fontSize: "15px", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "10px 14px", minHeight: "44px", width: "320px", marginBottom: "18px", background: "oklch(1 0 0)" }}
      />

      {organizacoes.isError ? (
        <Alerta>{intl.formatMessage({ id: chaveDoErro(organizacoes.error) })}</Alerta>
      ) : organizacoes.isPending ? (
        <Nota>{intl.formatMessage({ id: "conceder.escolher.carregando" })}</Nota>
      ) : achadas.length === 0 ? (
        <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
          {intl.formatMessage({
            id:
              (organizacoes.data ?? []).length === 0
                ? "conceder.escolher.vazio"
                : "conceder.escolher.semResultado",
          })}
        </div>
      ) : (
        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
          {achadas.map((organizacao, indice) => (
            <div
              key={organizacao.organizationId}
              style={{ display: "grid", gridTemplateColumns: "40px 1fr auto", gap: "14px", alignItems: "center", padding: "16px 22px", borderTop: indice === 0 ? "none" : "1px solid oklch(0.95 0.005 150)" }}
            >
              <div aria-hidden style={{ width: "40px", height: "40px", borderRadius: "999px", background: "oklch(0.90 0.03 150)" }}></div>
              <div>
                <div style={{ fontSize: "16px", fontWeight: 500 }}>{organizacao.name}</div>
                <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                  {organizacao.city === undefined || organizacao.state === undefined
                    ? intl.formatMessage({ id: "conceder.organizacao.semCidade" })
                    : intl.formatMessage(
                        { id: "conceder.organizacao.cidade" },
                        { cidade: organizacao.city, estado: organizacao.state },
                      )}
                </div>
              </div>
              <button
                type="button"
                onClick={() => aoEscolher(organizacao)}
                style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", cursor: "pointer" }}
              >
                {intl.formatMessage({ id: "conceder.escolher.acao" })}
              </button>
            </div>
          ))}
        </div>
      )}

      <div style={{ marginTop: "24px" }}>
        <Voltar animalId={animalId} />
      </div>
    </Moldura>
  );
}

/* --------------------------------------------------------------- a Tela 09 propriamente */

function OQueElaVe({
  animalId,
  nome,
  organizacao,
}: {
  animalId: string;
  nome: string;
  organizacao: Organizacao;
}) {
  const intl = useIntl();
  const navegar = useNavigate();
  const conceder = useConcederAcesso();
  const acao = useHover();

  const [marcados, setMarcados] = useState<Escopo[]>(MARCADOS_AO_ABRIR);

  /*
   * O desenho escreve 31/12/2027, que e exemplo e nao regra. A regra que ele escreve em
   * texto e "todo acesso tem prazo", entao o campo nasce preenchido com um ano a frente —
   * um padrao que o tutor ve e muda, e nao um vazio que ele precisa entender.
   */
  const [ate, setAte] = useState(() => {
    const daqui = new Date();
    daqui.setFullYear(daqui.getFullYear() + 1);
    return daqui.toISOString().slice(0, 10);
  });

  const naoMarcados = LINHAS.filter((linha) => !marcados.includes(linha));

  const alternar = (linha: Escopo) =>
    setMarcados((atuais) =>
      atuais.includes(linha) ? atuais.filter((item) => item !== linha) : [...atuais, linha],
    );

  const frase = (escopos: Escopo[]) =>
    new Intl.ListFormat("pt-BR", { style: "long", type: "conjunction" }).format(
      // A ordem e a do desenho, e nao a de clique: a frase precisa ler igual todas as vezes.
      LINHAS.filter((linha) => escopos.includes(linha)).map((linha) =>
        intl.formatMessage({ id: FRASE_DO_ESCOPO[linha] }),
      ),
    );

  return (
    <Moldura>
      <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
        {intl.formatMessage({ id: "conceder.titulo" }, { organizacao: organizacao.name, nome })}
      </h1>
      <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "62ch" }}>
        {intl.formatMessage({ id: "conceder.apoio" }, { nome })}
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: "32px", alignItems: "start" }}>
        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "14px", padding: "18px 22px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)" }}>
            <div aria-hidden style={{ width: "40px", height: "40px", borderRadius: "999px", background: "oklch(0.90 0.03 150)", flex: "none" }}></div>
            <div>
              <div style={{ fontSize: "16px", fontWeight: 500 }}>{organizacao.name}</div>
              <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                {organizacao.city === undefined || organizacao.state === undefined
                  ? intl.formatMessage({ id: "conceder.organizacao.semCidade" })
                  : intl.formatMessage(
                      { id: "conceder.organizacao.cidade" },
                      { cidade: organizacao.city, estado: organizacao.state },
                    )}
              </div>
            </div>
          </div>

          <div style={{ padding: "8px 22px 20px" }}>
            {LINHAS.map((linha) => (
              <Linha
                key={linha}
                marcado={marcados.includes(linha)}
                aoAlternar={() => alternar(linha)}
                titulo={intl.formatMessage({ id: `conceder.item.${linha}` })}
                porque={intl.formatMessage({ id: `conceder.item.${linha}.porque` }, { nome })}
              />
            ))}

            <Linha
              marcado={false}
              indisponivel={intl.formatMessage({ id: "conceder.item.registrar.indisponivel" })}
              titulo={intl.formatMessage({ id: "conceder.item.registrar" })}
              porque={intl.formatMessage({ id: "conceder.item.registrar.porque" }, { nome })}
              ultima
            />
          </div>

          <div style={{ borderTop: "1px solid oklch(0.90 0.008 150)", padding: "20px 22px", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", alignItems: "end" }}>
            <div>
              <label
                htmlFor="conceder-ate"
                style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}
              >
                {intl.formatMessage({ id: "conceder.ate.rotulo" })}
              </label>
              <input
                id="conceder-ate"
                type="date"
                value={ate}
                min={new Date().toISOString().slice(0, 10)}
                onChange={(evento) => setAte(evento.target.value)}
                style={{ border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "12px 14px", fontSize: "15px", minHeight: "44px", fontFamily: "'DM Mono', monospace", width: "100%", background: "oklch(1 0 0)" }}
              />
            </div>
            <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.5, paddingBottom: "4px" }}>
              {intl.formatMessage({ id: "conceder.ate.apoio" })}
            </div>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
            <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "12px" }}>
              {intl.formatMessage({ id: "conceder.resumo.rotulo" })}
            </div>
            <div style={{ fontSize: "16px", lineHeight: 1.6 }}>
              {marcados.length === 0
                ? intl.formatMessage(
                    { id: "conceder.resumo.semNada" },
                    { organizacao: organizacao.name, nome },
                  )
                : `${intl.formatMessage(
                    { id: "conceder.resumo.frase" },
                    {
                      organizacao: organizacao.name,
                      nome,
                      o_que: frase(marcados),
                      data:
                        ate === ""
                          ? intl.formatMessage({ id: "acesso.semPrazo" })
                          : intl.formatDate(new Date(`${ate}T00:00:00`), { dateStyle: "short" }),
                    },
                  )} ${
                    naoMarcados.length === 0
                      ? intl.formatMessage({ id: "conceder.resumo.naoVaiNada" })
                      : intl.formatMessage(
                          { id: "conceder.resumo.naoVai" },
                          { o_que: frase(naoMarcados) },
                        )
                  }`}
            </div>
          </div>

          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "22px 24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "conceder.depois" }, { nome })}
          </div>

          {conceder.isError && <Alerta>{intl.formatMessage({ id: chaveDoErro(conceder.error) })}</Alerta>}

          <button
            type="button"
            disabled={conceder.isPending || marcados.length === 0 || organizacao.organizationId === undefined}
            {...acao.props}
            onClick={async () => {
              await conceder.mutateAsync({
                animalId,
                organizationId: organizacao.organizationId!,
                escopos: marcados,
                ate: ate === "" ? undefined : ate,
              });

              await navegar({ to: "/animais/$animalId/quem-cuida", params: { animalId } });
            }}
            style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: conceder.isPending || marcados.length === 0 ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: marcados.length === 0 ? "not-allowed" : "pointer" }}
          >
            {intl.formatMessage({ id: conceder.isPending ? "conceder.acao.concedendo" : "conceder.acao" })}
          </button>

          <Link
            to="/animais/$animalId/quem-cuida"
            params={{ animalId }}
            style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: "pointer", textAlign: "center", textDecoration: "none" }}
          >
            {intl.formatMessage({ id: "conceder.acao.cancelar" })}
          </Link>
        </div>
      </div>
    </Moldura>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/** O cartao de 1100 px do desenho, centrado na pagina. */
function Moldura({ children }: { children: ReactNode }) {
  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "40px 48px 44px" }}>
        {children}
      </div>
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

function Alerta({ children }: { children: ReactNode }) {
  return (
    <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: 0 }}>
      {children}
    </p>
  );
}

function Voltar({ animalId }: { animalId: string }) {
  const intl = useIntl();

  return (
    <Link
      to="/animais/$animalId/quem-cuida"
      params={{ animalId }}
      style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}
    >
      {intl.formatMessage({ id: "conceder.voltar" })}
    </Link>
  );
}

/**
 * Uma linha do que a organizacao vai ver.
 *
 * <b>E `role="checkbox"` num botao, e nao um `input`.</b> A caixa do desenho tem 22 px com
 * canto de 4 e um quadrado branco de 9 px dentro — reproduzir isso sobre um `input` nativo
 * exige esconde-lo e desenhar outro por cima, o que da duas caixas para o teclado achar. O
 * botao com `role` e `aria-checked` e uma caixa so, e o rotulo inteiro e clicavel.
 *
 * Quando `indisponivel` vem preenchido a linha nao alterna, e o motivo aparece embaixo —
 * nunca so o cinza.
 */
function Linha({
  marcado,
  aoAlternar,
  titulo,
  porque,
  indisponivel,
  ultima = false,
}: {
  marcado: boolean;
  aoAlternar?: () => void;
  titulo: string;
  porque: string;
  indisponivel?: string;
  ultima?: boolean;
}) {
  const bloqueada = indisponivel !== undefined;

  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={marcado}
      aria-disabled={bloqueada}
      disabled={bloqueada}
      onClick={aoAlternar}
      style={{ fontFamily: "inherit", textAlign: "left", width: "100%", background: "transparent", border: "none", display: "flex", alignItems: "flex-start", gap: "14px", padding: "16px 0", borderBottom: ultima ? "none" : "1px solid oklch(0.95 0.005 150)", cursor: bloqueada ? "not-allowed" : "pointer", opacity: bloqueada ? 0.55 : 1 }}
    >
      <div
        aria-hidden
        style={
          marcado
            ? { width: "22px", height: "22px", borderRadius: "4px", background: "oklch(0.46 0.085 150)", flex: "none", marginTop: "1px", display: "flex", alignItems: "center", justifyContent: "center" }
            : { width: "22px", height: "22px", borderRadius: "4px", border: "1px solid oklch(0.78 0.012 150)", flex: "none", marginTop: "1px" }
        }
      >
        {marcado && <div style={{ width: "9px", height: "9px", borderRadius: "2px", background: "oklch(1 0 0)" }}></div>}
      </div>

      <div>
        <div style={{ fontSize: "16px" }}>{titulo}</div>
        <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "3px" }}>{porque}</div>
        {bloqueada && (
          <div style={{ fontSize: "14px", color: "oklch(0.42 0.015 150)", marginTop: "8px", lineHeight: 1.5 }}>
            {indisponivel}
          </div>
        )}
      </div>
    </button>
  );
}
