import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { useMeuContexto } from "../dados/contexto.ts";
import { usePacientes, useSobCustodia, type Paciente } from "../dados/pacientes.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 03 · web, tela grande — Area de organizacao, a segunda-feira da veterinaria", de
 * `design/IdentidadeVisual/Telas Petfy.dc.html`.
 *
 * <b>O QUE ESTA TELA PERDE E A COLUNA MAIS IMPORTANTE DELA.</b> O desenho mostra uma tabela
 * de pacientes com quatro colunas — animal, tutor, <b>situacao</b> e <b>ultima visita</b> —,
 * quatro recortes contados no topo ("vencendo em 30 dias · 12", "em tratamento · 7",
 * "atendidos este mes · 41", "todos · 318") e um painel "quem esta vencendo" com o gesto de
 * avisar os doze tutores. <b>Nada disso existe no backend.</b>
 *
 * O `VetPetDTO` devolve nome, tutor, raca, sexo, nascimento, peso e desde quando o acesso
 * existe. Zero sobre saude. E a pendencia, no modelo de hoje, e SEMPRE da pessoa logada:
 * `/due-items` responde pelo tutor autenticado, e nao existe consulta de "quem esta vencendo"
 * por organizacao. Montar a coluna no cliente exigiria uma leitura por animal — 318
 * requisicoes para desenhar uma tabela — e ainda assim `atendidos este mes` nao sairia de
 * lugar nenhum.
 *
 * Entao a tabela mostra as colunas que existem, o cabecalho diz quantos sao de verdade, e o
 * painel da direita <b>declara a ausencia em vez de fingir uma lista vazia</b>. E o mesmo
 * critério das outras telas: vazio nao e a mesma coisa que nao existe, e aqui os dados
 * existem — o que falta e rota que os agregue.
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
  const contexto = useMeuContexto();
  const [busca, setBusca] = useState("");

  /*
   * DUAS LISTAS, e nao um filtro: "quem eu alcanco porque alguem me concedeu" e "por quem eu
   * respondo" sao perguntas diferentes. O abrigo precisa da segunda para decidir uma adocao —
   * e a Tela 12 inteira mora nela, quando tiver onde-esta e saude.
   */
  const [aba, setAba] = useState<"acesso" | "custodia">("acesso");

  const porAcesso = usePacientes(busca);
  const porCustodia = useSobCustodia(busca);
  const pacientes = aba === "acesso" ? porAcesso : porCustodia;

  const quem = contexto.data?.personName ?? "";
  const ativo = contexto.data?.active;
  const lista = pacientes.data?.content ?? [];
  const total = pacientes.data?.totalElements;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div aria-hidden style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
            </div>
            <Link to="/" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "pacientes.meusAnimais" })}
            </Link>
          </div>

          {/*
           * "Agindo como" com a organizacao ao lado: e o chip que a Tela 01 ja usa, e aqui ele
           * e obrigatorio — quem atende precisa saber em nome de quem esta registrando.
           */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", padding: "8px 14px", minHeight: "44px", background: "oklch(0.975 0.004 150)" }}>
            <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "comecar.agindoComo" })}
            </span>
            <span style={{ fontSize: "15px", fontWeight: 500 }}>{quem}</span>
            {ativo?.organizationName !== undefined && (
              <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                {intl.formatMessage({ id: "equipe.pela" }, { organizacao: ativo.organizationName })}
              </span>
            )}
          </div>
        </div>

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
           * Os quatro recortes do desenho viram UM: o unico numero que existe e o total, e ele
           * vem do `totalElements` da pagina. Os outros tres — vencendo, em tratamento,
           * atendidos este mes — dependem de agregacao que nenhuma rota faz.
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
            <div style={{ fontSize: "14px", padding: "8px 14px", minHeight: "40px", display: "flex", alignItems: "center", border: "1px dashed oklch(0.88 0.008 150)", color: "oklch(0.5 0.015 150)", borderRadius: "8px" }}>
              {intl.formatMessage({ id: "pacientes.recortes.indisponiveis" })}
            </div>
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
              <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr 1fr auto", gap: "16px", padding: "12px 22px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)" }}>
                <div>{intl.formatMessage({ id: "pacientes.coluna.animal" })}</div>
                <div>{intl.formatMessage({ id: "pacientes.coluna.tutor" })}</div>
                <div>{intl.formatMessage({ id: "pacientes.coluna.acessoDesde" })}</div>
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

          {/* --------------------------------------------- o painel que nao da para montar */}
          <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "22px 24px", marginTop: "20px", maxWidth: "640px" }}>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
              {intl.formatMessage({ id: "pacientes.vencendo.titulo" })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "10px" }}>
              {intl.formatMessage({ id: "pacientes.vencendo.falta" })}
            </div>
            <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "pacientes.vencendo.aviso" })}
            </div>
          </div>
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
    <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr 1fr auto", gap: "16px", alignItems: "center", padding: "8px 22px", minHeight: "56px", borderTop: "1px solid oklch(0.95 0.005 150)" }}>
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

      <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", fontFamily: "'DM Mono', monospace" }}>
        {paciente.accessGrantedAt === undefined
          ? "—"
          : intl.formatDate(new Date(paciente.accessGrantedAt), { dateStyle: "short" })}
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

          <Link
            to="/animais/$animalId"
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
