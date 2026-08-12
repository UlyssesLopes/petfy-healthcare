import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  usePacientes,
  useResumoDosPacientes,
  useSobCustodia,
  type Paciente,
} from "../dados/pacientes.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 03 · web, tela grande — Area de organizacao, a segunda-feira da veterinaria", de
 * `design/IdentidadeVisual/Telas Petfy.dc.html`.
 *
 * <b>A COLUNA MAIS IMPORTANTE DELA EXISTE AGORA.</b> O desenho pede quatro colunas — animal,
 * tutor, <b>situacao</b> e <b>ultima visita</b> —, quatro recortes contados no topo e um painel
 * "quem esta vencendo". Ate a rodada passada nada disso vinha do backend: o `VetPetDTO` tinha
 * nome, tutor, raca, sexo, nascimento e peso, e zero sobre saude.
 *
 * O que destravou foi a agregacao por organizacao: `healthStatus`, `lastVisitAt` e
 * `underTreatment` viajam por animal, e `/professional/animals/summary` conta o conjunto
 * inteiro. <b>Tudo em lote, no servidor</b> — era justamente a leitura por animal (318
 * requisicoes para desenhar uma tabela) que tornava isso impossivel no cliente.
 *
 * <b>A TELA NAO RECALCULA NADA.</b> A situacao vem pronta, como na Tela 10: no dia em que o
 * cliente divergisse do servidor, seria o dia em que um animal com antirrabica vencida
 * apareceria em dia para quem decide a quem ligar.
 *
 * <b>O que AINDA nao existe e o GESTO:</b> "avisar os doze tutores". Nao ha canal de aviso no
 * produto, e um botao que nao avisa ninguem seria pior que a ausencia dele — entao o painel
 * lista quem esta vencendo e diz, em vez de desenhar, que avisar dali nao existe.
 *
 * <b>A busca e do servidor</b> (`q`), e nao do cliente: 318 pacientes nao caberiam numa
 * pagina para filtrar em memoria.
 */

export const Route = createFileRoute("/pacientes")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Pacientes,
});

function Pacientes() {
  const intl = useIntl();
  const [busca, setBusca] = useState("");

  /*
   * DUAS LISTAS, e nao um filtro: "quem eu alcanco porque alguem me concedeu" e "por quem eu
   * respondo" sao perguntas diferentes. O abrigo precisa da segunda para decidir uma adocao —
   * e a Tela 12 inteira mora nela, quando tiver onde-esta e saude.
   */
  const [aba, setAba] = useState<"acesso" | "custodia">("acesso");

  const porAcesso = usePacientes(busca);
  const porCustodia = useSobCustodia(busca);
  const resumo = useResumoDosPacientes();
  const pacientes = aba === "acesso" ? porAcesso : porCustodia;

  const lista = pacientes.data?.content ?? [];
  const total = pacientes.data?.totalElements;

  /*
   * Quem esta vencendo, na pagina carregada. Vencida vem antes de vencendo: o painel existe
   * para dizer a quem ligar primeiro, e quem passou do prazo e o primeiro.
   */
  const vencendo = lista
    .filter((paciente) => paciente.healthStatus === "OVERDUE" || paciente.healthStatus === "DUE_SOON")
    .sort((a, b) => (a.healthStatus === b.healthStatus ? 0 : a.healthStatus === "OVERDUE" ? -1 : 1));

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>

        <div style={{ padding: "28px 32px 36px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "16px", marginBottom: "18px", flexWrap: "wrap" }}>
            <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, margin: 0 }}>
              {intl.formatMessage({ id: "pacientes.titulo" })}
            </h1>

            <input
              type="search"
              value={busca}
              onChange={(evento) => setBusca(evento.target.value)}
              placeholder={intl.formatMessage({ id: "pacientes.busca" })}
              aria-label={intl.formatMessage({ id: "pacientes.busca" })}
              style={{ fontFamily: "inherit", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "10px 14px", minHeight: "44px", fontSize: "15px", width: "300px", background: "oklch(1 0 0)" }}
            />
          </div>

          {/*
           * Os quatro recortes do desenho. As duas abas contam pelo `totalElements` de cada
           * lista — sao dois conjuntos diferentes, e nao dois filtros do mesmo. Os outros tres
           * vem do resumo, que conta sobre a organizacao INTEIRA: numero que muda ao buscar nao
           * e resumo.
           */}
          <div style={{ display: "flex", gap: "8px", marginBottom: "18px", flexWrap: "wrap" }}>
            <Aba escolhida={aba === "acesso"} aoEscolher={() => setAba("acesso")}>
              {porAcesso.data?.totalElements === undefined
                ? intl.formatMessage({ id: "pacientes.aba.acesso" })
                : intl.formatMessage(
                    { id: "pacientes.aba.acesso.contados" },
                    { quantos: porAcesso.data.totalElements },
                  )}
            </Aba>

            <Aba escolhida={aba === "custodia"} aoEscolher={() => setAba("custodia")}>
              {porCustodia.data?.totalElements === undefined
                ? intl.formatMessage({ id: "pacientes.aba.custodia" })
                : intl.formatMessage(
                    { id: "pacientes.aba.custodia.contados" },
                    { quantos: porCustodia.data.totalElements },
                  )}
            </Aba>
            {resumo.data !== undefined && (
              <>
                <Recorte
                  texto={intl.formatMessage(
                    { id: "pacientes.recorte.vencendo" },
                    { quantos: resumo.data.dueIn30Days ?? 0 },
                  )}
                  atencao={(resumo.data.dueIn30Days ?? 0) > 0}
                />
                <Recorte
                  texto={intl.formatMessage(
                    { id: "pacientes.recorte.tratamento" },
                    { quantos: resumo.data.underTreatment ?? 0 },
                  )}
                />
                <Recorte
                  texto={intl.formatMessage(
                    { id: "pacientes.recorte.atendidos" },
                    { quantos: resumo.data.seenThisMonth ?? 0 },
                  )}
                />
              </>
            )}
          </div>

          {pacientes.isError ? (
            <ErroDeCarga
              oQue={intl.formatMessage({ id: "pacientes.oQue" })}
              erro={pacientes.error}
              aoTentarDeNovo={() => void pacientes.refetch()}
              carregando={pacientes.isFetching}
            />
          ) : pacientes.isPending ? (
            <Carregando oQue={intl.formatMessage({ id: "pacientes.oQue" })} quantos={total} />
          ) : lista.length === 0 ? (
            <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({
                id: busca.trim() === "" ? "pacientes.vazio" : "pacientes.semResultado",
              })}
            </div>
          ) : (
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
              <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr 0.9fr 0.8fr auto", gap: "16px", padding: "12px 22px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)" }}>
                <div>{intl.formatMessage({ id: "pacientes.coluna.animal" })}</div>
                <div>{intl.formatMessage({ id: "pacientes.coluna.tutor" })}</div>
                <div>{intl.formatMessage({ id: "pacientes.coluna.situacao" })}</div>
                <div>{intl.formatMessage({ id: "pacientes.coluna.ultimaVisita" })}</div>
                <div></div>
              </div>

              {lista.map((paciente) => (
                <Linha key={paciente.animalId} paciente={paciente} sobCustodia={aba === "custodia"} />
              ))}

              <div style={{ padding: "12px 22px", fontSize: "13px", color: "oklch(0.5 0.015 150)", borderTop: "1px solid oklch(0.95 0.005 150)" }}>
                {total === undefined
                  ? ""
                  : intl.formatMessage(
                      { id: "pacientes.mostrando" },
                      { quantos: lista.length, total },
                    )}
              </div>
            </div>
          )}

          {/*
            "QUEM ESTA VENCENDO". A lista existe agora; o GESTO do desenho — "avisar os doze
            tutores" — continua nao existindo, e por isso continua escrito em vez de desenhado:
            nao ha canal de aviso no produto, e um botao que nao avisa ninguem seria pior que a
            ausencia dele.

            A lista mostra quem esta vencendo NA PAGINA CARREGADA, e o numero ao lado e o da
            organizacao inteira. Os dois juntos, porque um deles sozinho mente: so a pagina
            esconderia o tamanho do problema, e so o numero nao diria a quem ligar.
          */}
          {vencendo.length > 0 && (
            <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "22px 24px", marginTop: "20px", maxWidth: "640px" }}>
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
                {intl.formatMessage(
                  { id: "pacientes.vencendo.titulo.contados" },
                  { quantos: resumo.data?.dueIn30Days ?? vencendo.length },
                )}
              </div>

              <ul style={{ listStyle: "none", margin: "0 0 12px", padding: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
                {vencendo.map((paciente) => (
                  <li key={paciente.animalId} style={{ fontSize: "15px", color: "oklch(0.35 0.018 150)" }}>
                    <b style={{ fontWeight: 500 }}>{paciente.name}</b>
                    {paciente.personName === undefined
                      ? ""
                      : ` · ${paciente.personName}`}
                    {" · "}
                    <Situacao status={paciente.healthStatus} />
                  </li>
                ))}
              </ul>

              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage({ id: "pacientes.vencendo.aviso" })}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/** Linha de 56 px, densidade compacta, e o alvo de toque preservado no controle. */
function Linha({ paciente, sobCustodia }: { paciente: Paciente; sobCustodia: boolean }) {
  const intl = useIntl();

  const identidade = [
    paciente.type,
    paciente.bornDate === undefined ? undefined : idadeDe(paciente.bornDate, intl),
  ]
    .filter((parte): parte is string => parte !== undefined && parte !== "")
    .join(", ");

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr 0.9fr 0.8fr auto", gap: "16px", alignItems: "center", padding: "8px 22px", minHeight: "56px", borderTop: "1px solid oklch(0.95 0.005 150)" }}>
      <div>
        <span style={{ fontSize: "16px", fontWeight: 500 }}>{paciente.name}</span>
        {identidade !== "" && (
          <span style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}> · {identidade}</span>
        )}
      </div>

      {/*
       * SEM TUTOR HUMANO e uma informacao, e nao um campo vazio. O animal sob custodia do
       * abrigo nao tem tutor — escrever o nome do abrigo aqui faria a coluna mentir, e deixar
       * em branco faria parecer defeito.
       */}
      <div style={{ fontSize: "15px", color: paciente.personName === undefined ? "oklch(0.5 0.015 150)" : "oklch(0.35 0.018 150)" }}>
        {paciente.personName ?? intl.formatMessage({ id: "pacientes.semTutor" })}
      </div>

      {/*
        A COLUNA QUE A TELA NAO TINHA. O servidor manda a dose mais urgente da carteira pronta —
        vencida vence vencendo, que vence em dia. A tela nao recalcula nada: no dia em que ela
        divergisse do servidor, seria o dia em que um animal com antirrabica vencida apareceria
        em dia para quem decide a quem ligar.

        "Em tratamento" viaja junto do estado da carteira porque sao coisas diferentes: um animal
        pode estar em dia de vacina E em tratamento, e a linha precisa dizer as duas.
      */}
      <div style={{ fontSize: "14px", display: "flex", flexDirection: "column", gap: "2px" }}>
        <Situacao status={paciente.healthStatus} />
        {paciente.underTreatment === true && (
          <span style={{ fontSize: "13px", color: "oklch(0.45 0.09 250)" }}>
            {intl.formatMessage({ id: "pacientes.emTratamento" })}
          </span>
        )}
      </div>

      {/*
        Nunca visitou NAO e "—": e "sem visita registrada", e a diferenca importa para quem le.
        O tracinho diz "nao sei"; a frase diz o que o produto sabe.
      */}
      <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", fontFamily: "'DM Mono', monospace" }}>
        {paciente.lastVisitAt === undefined
          ? intl.formatMessage({ id: "pacientes.semVisita" })
          : intl.formatDate(new Date(paciente.lastVisitAt), { dateStyle: "short" })}
      </div>

      {/*
       * "Atender" leva para a vida do animal: e de la que o registro sai, e o profissional
       * com acesso le a mesma tela do tutor. Uma tela de atendimento propria e a 05/06 da
       * entrega, que nao esta nesta rodada.
       */}
      {paciente.animalId === undefined ? (
        <span></span>
      ) : (
        <div style={{ display: "flex", gap: "10px" }}>
          {/* Adotar so aparece para quem o abrigo RESPONDE — e a Tela 13. */}
          {sobCustodia && (
            <Link
              to="/animais/$animalId/adocao"
              params={{ animalId: paciente.animalId }}
              style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", display: "flex", alignItems: "center", textDecoration: "none" }}
            >
              {intl.formatMessage({ id: "pacientes.adotar" })}
            </Link>
          )}

          {/*
           * <b>Leva a Tela 30, e nao mais a Tela 02.</b> As duas mostram o mesmo animal e
           * respondem perguntas diferentes: a Tela 02 e a vida dele como o TUTOR a le, e a 30 e
           * o que a clinica precisa ter na frente antes de prescrever — alergia e o que ja esta
           * em uso primeiro, e o historico com "so desta clinica" ao lado. Quem chega por aqui
           * esta atendendo, e nao visitando.
           */}
          <Link
            to="/pacientes/$animalId"
            params={{ animalId: paciente.animalId }}
            style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", display: "flex", alignItems: "center", textDecoration: "none" }}
          >
            {intl.formatMessage({ id: sobCustodia ? "pacientes.abrir" : "pacientes.atender" })}
          </Link>
        </div>
      )}
    </div>
  );
}

/**
 * A situacao da carteira, em palavra e cor.
 *
 * <b>`NO_NEXT_DOSE` nao e "em dia", e por isso tem frase propria.</b> Pode ser dose unica e pode
 * ser carteira que ninguem registrou — dizer "em dia" ali seria o produto afirmando saude a
 * partir de ausencia de dado, que e exatamente o que ele existe para nao fazer.
 */
function Situacao({ status }: { status: Paciente["healthStatus"] }) {
  const intl = useIntl();

  const cor =
    status === "OVERDUE"
      ? "oklch(0.42 0.13 30)"
      : status === "DUE_SOON"
        ? "oklch(0.45 0.10 70)"
        : status === "UP_TO_DATE"
          ? "oklch(0.42 0.06 150)"
          : "oklch(0.5 0.015 150)";

  return (
    <span style={{ color: cor, fontWeight: status === "OVERDUE" ? 500 : 400 }}>
      {intl.formatMessage({ id: `pacientes.situacao.${status ?? "NO_NEXT_DOSE"}` })}
    </span>
  );
}

/** Um numero do cabecalho. */
function Recorte({ texto, atencao = false }: { texto: string; atencao?: boolean }) {
  return (
    <div
      style={{
        fontSize: "14px",
        padding: "8px 14px",
        minHeight: "40px",
        display: "flex",
        alignItems: "center",
        border: `1px solid ${atencao ? "oklch(0.86 0.03 70)" : "oklch(0.88 0.008 150)"}`,
        background: atencao ? "oklch(0.985 0.012 70)" : "oklch(1 0 0)",
        color: "oklch(0.35 0.018 150)",
        borderRadius: "8px",
      }}
    >
      {texto}
    </div>
  );
}

/** Os recortes do topo: só existem os dois que uma consulta responde. */
function Aba({
  escolhida,
  aoEscolher,
  children,
}: {
  escolhida: boolean;
  aoEscolher: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={aoEscolher}
      aria-pressed={escolhida}
      style={{ fontFamily: "inherit", fontSize: "14px", padding: "8px 14px", minHeight: "40px", display: "flex", alignItems: "center", border: `1px solid ${escolhida ? "oklch(0.46 0.085 150)" : "oklch(0.84 0.012 150)"}`, color: escolhida ? "oklch(0.46 0.085 150)" : "oklch(0.42 0.015 150)", background: "oklch(1 0 0)", borderRadius: "8px", cursor: "pointer" }}
    >
      {children}
    </button>
  );
}

/** "6a" do desenho: idade em anos, e em meses no primeiro ano. */
function idadeDe(nascimento: string, intl: ReturnType<typeof useIntl>): string {
  const nasceu = new Date(`${nascimento}T12:00:00`);
  const meses =
    (new Date().getFullYear() - nasceu.getFullYear()) * 12 +
    (new Date().getMonth() - nasceu.getMonth());

  if (meses < 12) {
    return intl.formatMessage({ id: "pacientes.idade.meses" }, { meses: Math.max(meses, 0) });
  }

  return intl.formatMessage({ id: "pacientes.idade.anos" }, { anos: Math.floor(meses / 12) });
}
