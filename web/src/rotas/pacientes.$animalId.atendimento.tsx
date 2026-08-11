import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar } from "../componentes/Estados.tsx";
import { useOrientacoes } from "../dados/animal.ts";
import {
  useObservacoes,
  usePrescrever,
  useRegistrarAtendimento,
  useRegistrarPeso,
} from "../dados/atendimento.ts";
import { useAnimal } from "../dados/carteira.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 31 · o loop central — Registrar atendimento e prescrever", de
 * `design/IdentidadeVisual/Telas Petfy - Nucleo clinico.dc.html`. O proprio desenho a chama de
 * loop central, e e a unica tela do produto que grava tres coisas de uma vez.
 *
 * <b>A ASSINATURA APARECE ANTES DO PRIMEIRO CAMPO.</b> "Sera assinado por Ana Ferreira, pela
 * Clinica Vet Norte. A autoria nao muda depois." Nao e enfeite: e o que separa um registro que
 * um veterinario aceita de um caderno digital, e quem esta digitando precisa saber em nome de
 * quem, ANTES de escrever — nao depois de gravar.
 *
 * <b>SAO TRES ESCRITAS, e elas existem separadas de proposito:</b> o prontuario, a pesagem do
 * dia e cada medicamento. A pesagem entra na serie de peso do animal; a prescricao vira pendencia
 * diaria na casa do tutor. Enfia-las todas no texto do prontuario faria o feed do tutor nao ter
 * como saber que ha remedio para dar hoje.
 *
 * <b>A PRESCRICAO SAI SEM DONO</b>, e o desenho e explicito: "voce prescreve o que o animal
 * precisa. Quem da cada dose — o tutor, a co-tutora ou a creche — e o tutor quem decide, porque
 * so ele sabe quem estara em casa". E prescrever NAO CONCEDE ACESSO a ninguem: a creche so
 * recebe a dose se ja tiver acesso e se o tutor atribuir.
 *
 * <b>O AVISO DE SOBREPOSICAO E DO CLIENTE, e essa e uma decisao e nao um esquecimento.</b> Ele
 * nao recusa nada — o desenho escreve "confirme que e intencional", e sobrepor dois medicamentos
 * e as vezes exatamente o que se quer. Guardas que RECUSAM moram no servidor (a dose duplicada
 * responde 409/149); um aviso que so pede confirmacao e informacao de tela, e todo o dado de que
 * ele precisa — as orientacoes vigentes com a data de fim — ja vem do `/care-instructions`.
 * Registrado: outro cliente que prescreva nao vai avisar, e a decisao de subir isso para o
 * servidor fica em aberto.
 */

export const Route = createFileRoute("/pacientes/$animalId/atendimento")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Atendimento,
});

/** Um medicamento do formulario. O desenho permite "adicionar outro". */
type Receita = {
  descricao: string;
  intervaloEmDias: string;
  dias: string;
  comoDar: string;
};

const RECEITA_VAZIA: Receita = { descricao: "", intervaloEmDias: "24", dias: "7", comoDar: "" };

function hoje(): string {
  return new Date().toISOString().slice(0, 10);
}

function emDias(base: string, dias: number): string {
  const data = new Date(`${base}T12:00:00`);
  data.setDate(data.getDate() + dias);
  return data.toISOString().slice(0, 10);
}

function Atendimento() {
  const { animalId } = Route.useParams();
  const intl = useIntl();
  const navegar = useNavigate();

  const contexto = useMeuContexto();
  const animal = useAnimal(animalId);
  const observacoes = useObservacoes(animalId);
  const orientacoes = useOrientacoes(animalId);

  const registrar = useRegistrarAtendimento();
  const prescrever = usePrescrever();
  const pesar = useRegistrarPeso();

  const [quando, setQuando] = useState(hoje());
  const [peso, setPeso] = useState("");
  const [constatacao, setConstatacao] = useState("");
  const [diagnostico, setDiagnostico] = useState("");
  const [referenciadas, setReferenciadas] = useState<string[]>([]);
  const [receitas, setReceitas] = useState<Receita[]>([{ ...RECEITA_VAZIA }]);
  const [gravando, setGravando] = useState(false);
  const [erro, setErro] = useState<unknown>(undefined);

  const bicho = animal.data;
  const ativo = contexto.data?.active;
  const emUso = (orientacoes.data ?? []).filter((orientacao) => orientacao.vigente !== false);

  /*
   * A sobreposicao, contada em dias. O desenho diz "os dois vao se sobrepor por tres dias —
   * confirme que e intencional": o numero e a informacao, porque um dia de sobreposicao e ruido
   * e duas semanas e outra conversa.
   */
  const sobreposicoes = emUso
    .filter((orientacao) => orientacao.endsOn !== undefined && orientacao.endsOn >= quando)
    .map((orientacao) => ({
      oQue: orientacao.description ?? "",
      ate: orientacao.endsOn!,
      dias:
        Math.floor(
          (new Date(`${orientacao.endsOn!}T12:00:00`).getTime() -
            new Date(`${quando}T12:00:00`).getTime()) /
            86_400_000,
        ) + 1,
    }));

  const receitasPreenchidas = receitas.filter((receita) => receita.descricao.trim() !== "");
  const podeGravar = constatacao.trim() !== "" && !gravando;

  /*
   * A ORDEM DAS TRES ESCRITAS IMPORTA, e nao e alfabetica: o atendimento primeiro. Se a
   * prescricao gravasse antes e o prontuario falhasse, o tutor receberia sete pendencias de um
   * tratamento que nao existe em lugar nenhum — e ninguem saberia dizer por que ele esta dando
   * prednisolona ao animal.
   */
  const enviar = async () => {
    setGravando(true);
    setErro(undefined);

    try {
      const constatacaoFinal = [
        constatacao.trim(),
        ...referenciadas.map((id) => {
          const observacao = (observacoes.data ?? []).find((item) => item.observationId === id);
          return observacao === undefined
            ? ""
            : intl.formatMessage(
                { id: "atendimento.referencia" },
                {
                  texto: observacao.description ?? "",
                  quem: observacao.recordedByName ?? "",
                  onde: observacao.organizationName ?? "",
                },
              );
        }),
      ]
        .filter((parte) => parte !== "")
        .join("\n\n");

      await registrar.mutateAsync({
        animalId,
        categoria: "CONSULTA",
        rotulo: intl.formatMessage({ id: "atendimento.rotulo" }),
        constatacao: constatacaoFinal,
        diagnostico: diagnostico.trim(),
        quando,
      });

      if (peso.trim() !== "") {
        await pesar.mutateAsync({
          animalId,
          peso: Number(peso.replace(",", ".")),
          medidoEm: `${quando}T12:00:00`,
        });
      }

      for (const receita of receitasPreenchidas) {
        const dias = Number(receita.dias) || 1;

        await prescrever.mutateAsync({
          animalId,
          descricao: [receita.descricao.trim(), receita.comoDar.trim()]
            .filter((parte) => parte !== "")
            .join(" · "),
          intervaloEmDias: Math.max(1, Math.round((Number(receita.intervaloEmDias) || 24) / 24)),
          comecaEm: quando,
          terminaEm: emDias(quando, dias - 1),
        });
      }

      await navegar({ to: "/pacientes/$animalId", params: { animalId } });
    } catch (falha) {
      setErro(falha);
    } finally {
      setGravando(false);
    }
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1180px", margin: "0 auto", display: "grid", gridTemplateColumns: "1fr 360px", gap: "24px", alignItems: "start" }}>

        <div style={{ border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "30px 32px 36px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "atendimento.titulo" }, { nome: bicho?.name ?? "" })}
          </h1>

          {/* A assinatura, antes do primeiro campo. "A autoria nao muda depois." */}
          <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", margin: "0 0 26px" }}>
            {intl.formatMessage(
              { id: "atendimento.assinatura" },
              {
                quem: contexto.data?.personName ?? "",
                onde:
                  ativo?.kind === "ORGANIZACAO"
                    ? intl.formatMessage(
                        { id: "paciente.pela" },
                        { organizacao: ativo.organizationName ?? "" },
                      )
                    : intl.formatMessage({ id: "atendimento.porMim" }),
              },
            )}
          </div>

          <Campo rotulo={intl.formatMessage({ id: "atendimento.quando" })} para="quando">
            <input
              id="quando"
              type="date"
              value={quando}
              max={hoje()}
              onChange={(evento) => setQuando(evento.target.value)}
              style={campo}
            />
            <Nota>{intl.formatMessage({ id: "atendimento.quando.nota" })}</Nota>
          </Campo>

          <Campo rotulo={intl.formatMessage({ id: "atendimento.peso" })} para="peso">
            <input
              id="peso"
              type="text"
              inputMode="decimal"
              value={peso}
              onChange={(evento) => setPeso(evento.target.value)}
              placeholder="8,6"
              style={{ ...campo, width: "160px" }}
            />
          </Campo>

          <Campo rotulo={intl.formatMessage({ id: "atendimento.constatacao" })} para="constatacao">
            <textarea
              id="constatacao"
              value={constatacao}
              onChange={(evento) => setConstatacao(evento.target.value)}
              rows={5}
              style={{ ...campo, minHeight: "120px", resize: "vertical" }}
            />
          </Campo>

          <Campo rotulo={intl.formatMessage({ id: "atendimento.diagnostico" })} para="diagnostico">
            <input
              id="diagnostico"
              type="text"
              value={diagnostico}
              onChange={(evento) => setDiagnostico(evento.target.value)}
              style={campo}
            />
            <Nota>{intl.formatMessage({ id: "atendimento.diagnostico.nota" })}</Nota>
          </Campo>

          {/* ------------------------------------------- evidencias que outros registraram
           *
           * <b>Referenciar nao transforma observacao em diagnostico.</b> Ela continua sendo o
           * que e, assinada por quem escreveu — e por isso vai para o prontuario entre aspas,
           * com o nome de quem disse e de onde. O que a veterinaria constata e o campo dela.
           */}
          {(observacoes.data ?? []).length > 0 && (
            <div style={{ marginTop: "26px" }}>
              <Rotulo>{intl.formatMessage({ id: "atendimento.evidencias" })}</Rotulo>

              {(observacoes.data ?? []).slice(0, 6).map((observacao) => {
                const id = observacao.observationId ?? "";
                const marcada = referenciadas.includes(id);

                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() =>
                      setReferenciadas((antes) =>
                        marcada ? antes.filter((item) => item !== id) : [...antes, id],
                      )
                    }
                    style={{ fontFamily: "inherit", display: "block", width: "100%", textAlign: "left", border: `1px solid ${marcada ? "oklch(0.46 0.085 150)" : "oklch(0.90 0.008 150)"}`, background: marcada ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)", borderRadius: "8px", padding: "14px 16px", marginTop: "8px", cursor: "pointer" }}
                  >
                    <div style={{ fontSize: "15px", lineHeight: 1.55, color: "oklch(0.3 0.02 150)" }}>
                      “{observacao.description}”
                    </div>
                    <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "6px" }}>
                      {[observacao.recordedByName, observacao.organizationName]
                        .filter((parte) => parte !== undefined && parte !== "")
                        .join(", ")}
                    </div>
                  </button>
                );
              })}

              <Nota>{intl.formatMessage({ id: "atendimento.evidencias.nota" })}</Nota>
            </div>
          )}

          {/* ------------------------------------------------------------------ prescricao */}
          <div style={{ marginTop: "32px", borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "26px" }}>
            <Rotulo>{intl.formatMessage({ id: "atendimento.prescricao" })}</Rotulo>
            <Nota>{intl.formatMessage({ id: "atendimento.prescricao.nota" })}</Nota>

            {receitas.map((receita, indice) => (
              <div key={indice} style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px", marginTop: "14px" }}>
                <Campo rotulo={intl.formatMessage({ id: "atendimento.medicamento" })} para={`medicamento-${indice}`}>
                  <input
                    id={`medicamento-${indice}`}
                    type="text"
                    value={receita.descricao}
                    onChange={(evento) =>
                      setReceitas((antes) =>
                        antes.map((item, i) => (i === indice ? { ...item, descricao: evento.target.value } : item)),
                      )
                    }
                    style={campo}
                  />
                </Campo>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                  <Campo rotulo={intl.formatMessage({ id: "atendimento.intervalo" })} para={`intervalo-${indice}`}>
                    <input
                      id={`intervalo-${indice}`}
                      type="number"
                      min={1}
                      value={receita.intervaloEmDias}
                      onChange={(evento) =>
                        setReceitas((antes) =>
                          antes.map((item, i) => (i === indice ? { ...item, intervaloEmDias: evento.target.value } : item)),
                        )
                      }
                      style={campo}
                    />
                  </Campo>

                  <Campo rotulo={intl.formatMessage({ id: "atendimento.duracao" })} para={`dias-${indice}`}>
                    <input
                      id={`dias-${indice}`}
                      type="number"
                      min={1}
                      value={receita.dias}
                      onChange={(evento) =>
                        setReceitas((antes) =>
                          antes.map((item, i) => (i === indice ? { ...item, dias: evento.target.value } : item)),
                        )
                      }
                      style={campo}
                    />
                  </Campo>
                </div>

                <Campo rotulo={intl.formatMessage({ id: "atendimento.comoDar" })} para={`como-${indice}`}>
                  <input
                    id={`como-${indice}`}
                    type="text"
                    value={receita.comoDar}
                    onChange={(evento) =>
                      setReceitas((antes) =>
                        antes.map((item, i) => (i === indice ? { ...item, comoDar: evento.target.value } : item)),
                      )
                    }
                    style={campo}
                  />
                </Campo>
              </div>
            ))}

            <button
              type="button"
              onClick={() => setReceitas((antes) => [...antes, { ...RECEITA_VAZIA }])}
              style={{ fontFamily: "inherit", fontSize: "15px", color: "oklch(0.46 0.085 150)", background: "transparent", border: "none", cursor: "pointer", padding: "12px 0", minHeight: "44px" }}
            >
              {intl.formatMessage({ id: "atendimento.outroMedicamento" })}
            </button>

            {/* O aviso de sobreposicao. Ele NAO recusa: "confirme que e intencional". */}
            {receitasPreenchidas.length > 0 &&
              sobreposicoes.map((sobreposicao) => (
                <div key={sobreposicao.oQue} role="status" style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "8px", padding: "14px 16px", marginTop: "10px", fontSize: "15px", lineHeight: 1.6 }}>
                  {intl.formatMessage(
                    { id: "atendimento.sobreposicao" },
                    {
                      nome: bicho?.name ?? "",
                      o_que: sobreposicao.oQue,
                      ate: intl.formatDate(sobreposicao.ate, { day: "2-digit", month: "2-digit" }),
                      dias: sobreposicao.dias,
                    },
                  )}
                </div>
              ))}
          </div>

          {erro !== undefined && (
            <div style={{ marginTop: "22px" }}>
              <ErroAoGravar erro={erro} oQue={intl.formatMessage({ id: "atendimento.oQue" })} />
            </div>
          )}

          <div style={{ display: "flex", alignItems: "center", gap: "16px", marginTop: "28px", flexWrap: "wrap" }}>
            <button
              type="button"
              disabled={!podeGravar}
              onClick={() => void enviar()}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: podeGravar ? "oklch(0.46 0.085 150)" : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: podeGravar ? "pointer" : "not-allowed" }}
            >
              {intl.formatMessage({ id: gravando ? "atendimento.gravando" : "atendimento.gravar" })}
            </button>

            <span style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "atendimento.soCorrecao" })}
            </span>
          </div>

          <div style={{ marginTop: "22px" }}>
            <Link to="/pacientes/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "atendimento.voltar" })}
            </Link>
          </div>
        </div>

        {/* ============================================= o que o tutor vai receber */}
        <div style={{ border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "24px 26px" }}>
          <Rotulo>{intl.formatMessage({ id: "atendimento.oQueRecebe" })}</Rotulo>

          {receitasPreenchidas.length === 0 ? (
            <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "atendimento.oQueRecebe.nada" })}
            </div>
          ) : (
            <>
              {receitasPreenchidas.map((receita, indice) => (
                <div key={indice} style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "8px", padding: "14px 16px", marginTop: "10px" }}>
                  <div style={{ fontSize: "15px", fontWeight: 500 }}>{receita.descricao}</div>
                  <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "4px" }}>
                    {intl.formatMessage(
                      { id: "atendimento.pendencia" },
                      { total: Number(receita.dias) || 1, quem: contexto.data?.personName ?? "" },
                    )}
                  </div>
                </div>
              ))}

              <div style={{ fontSize: "14px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", marginTop: "16px" }}>
                {intl.formatMessage({ id: "atendimento.somemSozinhas" })}
              </div>

              {/* "Prescrever nao concede acesso a ninguem." */}
              <div style={{ fontSize: "14px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)", marginTop: "14px", borderTop: "1px solid oklch(0.90 0.008 150)", paddingTop: "14px" }}>
                {intl.formatMessage({ id: "atendimento.naoConcedeAcesso" })}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

const campo = {
  fontFamily: "inherit",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
} as const;

function Campo({ rotulo, para, children }: { rotulo: string; para: string; children: ReactNode }) {
  return (
    <div style={{ marginTop: "18px" }}>
      <label htmlFor={para} style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
        {rotulo}
      </label>
      {children}
    </div>
  );
}

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "10px" }}>
      {children}
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.55 }}>
      {children}
    </div>
  );
}
