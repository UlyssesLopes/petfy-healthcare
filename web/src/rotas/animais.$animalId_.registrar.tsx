import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar } from "../componentes/Estados.tsx";
import { useAnimal, useCarteira } from "../dados/carteira.ts";
import { useRegistrarPeso } from "../dados/atendimento.ts";
import { useRegistrarObservacao } from "../dados/registros.ts";
import { useRegistrarDose } from "../dados/vacinas.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O QUE O TUTOR REGISTRA NO PROPRIO ANIMAL — e ate agora ele nao registrava NADA.
 *
 * <b>Este e o buraco mais largo que a conferencia no navegador encontrou.</b> O botao "Registrar
 * evento", no cabecalho do animal, estava `disabled` no codigo — sem condicao, sem destino, sem
 * handler. Nao era descuido de estilo: era a porta de um caminho que nunca foi construido.
 *
 * A prova estava nos hooks. `useRegistrarDose`, `useDoseAnterior` e `useCorrecoes` existiam em
 * `dados/` e <b>nao eram usados por rota nenhuma</b>; o unico lugar do produto que registrava vacina
 * ou peso era `pacientes.$animalId.atendimento.tsx`, que e a tela da CLINICA. O tutor tinha um
 * produto que promete "cada dose, cada consulta e cada dia de creche entrou aqui com o nome de quem
 * fez" — e nenhum jeito de fazer.
 *
 * ------------------------------------------------------------ tres coisas, e nao um menu
 *
 * Vacina, peso e observacao. Sao as tres que o tutor faz sozinho, em casa, sem organizacao por tras —
 * o mesmo criterio que a Tela 42 usa para o dinheiro: "so existem se o tutor lancar".
 *
 * <b>ATENDIMENTO NAO ESTA AQUI, e a ausencia e a regra mais importante da tela.</b> Diagnostico e
 * prescricao sao ato clinico, e ato clinico exige credencial — e a distincao entre "o que alguem viu"
 * e "o que alguem concluiu" e a que o DESIGN chama de "a mais importante do produto". O tutor que
 * voltou do veterinario registra o que VIU: "mancou depois do parque". Quem diagnostica assina.
 *
 * ------------------------------------------------------------ a data vem antes do conteudo
 *
 * Os tres campos comecam com "quando", e nunca com "agora" implicito. E o mesmo defeito que a Tela 42
 * tinha: quem lanca a carteirinha de papel de 2019 esta registrando 2019, e um formulario que assume
 * hoje transformaria a vida inteira do animal num unico dia.
 */

export const Route = createFileRoute("/animais/$animalId_/registrar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Registrar,
});

type OQue = "vacina" | "peso" | "observacao";

function Registrar() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const carteira = useCarteira(animalId);

  const registrarDose = useRegistrarDose();
  const registrarPeso = useRegistrarPeso();
  const registrarObservacao = useRegistrarObservacao();

  const hoje = new Date().toISOString().slice(0, 10);

  const [oQue, setOQue] = useState<OQue>("vacina");
  const [quando, setQuando] = useState(hoje);
  const [feito, setFeito] = useState<string | undefined>(undefined);

  // vacina
  const [vacina, setVacina] = useState("");
  const [proximaDose, setProximaDose] = useState("");

  // peso
  const [peso, setPeso] = useState("");

  // observacao
  const [texto, setTexto] = useState("");

  if (animal.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "registrar.carregando" })} />;
  }

  const nome = animal.data?.name ?? "";

  /*
   * As vacinas que ESTE animal pode receber saem do catalogo da especie dele, que a carteira ja
   * carrega para a Tela 02. Um campo de texto livre aqui faria "V10", "v10" e "Vacina V10" virarem
   * tres series diferentes — e a carteira, que agrupa por serie, mostraria o mesmo reforco tres vezes.
   */
  const series = (carteira.linhas ?? []).map((linha) => linha.nome);

  const pesoNumero = Number(peso.replace(",", "."));

  const podeGravar =
    !registrarDose.isPending &&
    !registrarPeso.isPending &&
    !registrarObservacao.isPending &&
    (oQue === "vacina"
      ? vacina.trim() !== ""
      : oQue === "peso"
        ? peso.trim() !== "" && !Number.isNaN(pesoNumero) && pesoNumero > 0
        : texto.trim() !== "");

  const erro = registrarDose.error ?? registrarPeso.error ?? registrarObservacao.error;

  const gravar = () => {
    if (!podeGravar) {
      return;
    }

    setFeito(undefined);

    if (oQue === "vacina") {
      registrarDose.mutate(
        {
          animalId,
          vaccineName: vacina.trim(),
          vaccineCatalogId: undefined,
          applicationDate: quando,
          nextDoseDate: proximaDose === "" ? undefined : proximaDose,
        },
        {
          onSuccess: () => {
            setFeito(intl.formatMessage({ id: "registrar.feito.vacina" }, { vacina: vacina.trim() }));
            setVacina("");
            setProximaDose("");
          },
        },
      );
      return;
    }

    if (oQue === "peso") {
      registrarPeso.mutate(
        { animalId, peso: pesoNumero, medidoEm: quando },
        {
          onSuccess: () => {
            setFeito(intl.formatMessage({ id: "registrar.feito.peso" }, { peso: pesoNumero }));
            setPeso("");
          },
        },
      );
      return;
    }

    registrarObservacao.mutate(
      { animalId, texto: texto.trim(), quando: quando + "T12:00:00" },
      {
        onSuccess: () => {
          setFeito(intl.formatMessage({ id: "registrar.feito.observacao" }));
          setTexto("");
        },
      },
    );
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "620px", margin: "0 auto", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "30px 32px 34px" }}>

        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "registrar.titulo" }, { nome })}
        </h1>
        <p style={{ fontSize: "15px", lineHeight: 1.6, color: CINZA, margin: "0 0 22px" }}>
          {intl.formatMessage({ id: "registrar.apoio" })}
        </p>

        {/* --------------------------------------------------------------- o que aconteceu */}
        <div role="group" aria-label={intl.formatMessage({ id: "registrar.oQue" })} style={{ display: "flex", gap: "8px", marginBottom: "20px", flexWrap: "wrap" }}>
          {(["vacina", "peso", "observacao"] as const).map((opcao) => {
            const marcada = opcao === oQue;

            return (
              <button
                key={opcao}
                type="button"
                aria-pressed={marcada}
                onClick={() => {
                  setOQue(opcao);
                  setFeito(undefined);
                }}
                style={{
                  fontFamily: "inherit",
                  fontSize: "15px",
                  fontWeight: 500,
                  padding: "12px 18px",
                  minHeight: "44px",
                  borderRadius: "8px",
                  cursor: "pointer",
                  border: marcada ? `1px solid ${VERDE}` : "1px solid oklch(0.82 0.012 150)",
                  background: marcada ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)",
                  color: marcada ? "oklch(0.34 0.07 150)" : "oklch(0.25 0.02 150)",
                }}
              >
                {intl.formatMessage({ id: `registrar.oQue.${opcao}` })}
              </button>
            );
          })}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
          {/*
           * QUANDO VEM PRIMEIRO, e nunca "agora" implicito: quem lanca a carteirinha de papel de 2019
           * esta registrando 2019, e um formulario que assume hoje transformaria a vida inteira do
           * animal num unico dia.
           */}
          <div>
            <label htmlFor="quando" style={rotulo}>
              {intl.formatMessage({ id: `registrar.quando.${oQue}` })}
            </label>
            <input
              id="quando"
              type="date"
              value={quando}
              max={hoje}
              onChange={(evento) => setQuando(evento.target.value)}
              style={campo}
            />
          </div>

          {oQue === "vacina" && (
            <>
              <div>
                <label htmlFor="vacina" style={rotulo}>
                  {intl.formatMessage({ id: "registrar.vacina.qual" })}
                </label>
                <input
                  id="vacina"
                  list="series-de-vacina"
                  value={vacina}
                  onChange={(evento) => setVacina(evento.target.value)}
                  placeholder={series[0] ?? ""}
                  style={campo}
                />
                {/*
                 * O catalogo da especie deste animal, que a carteira ja carrega. Sem ele, "V10",
                 * "v10" e "Vacina V10" virariam tres series diferentes — e a carteira, que agrupa por
                 * serie, mostraria o mesmo reforco tres vezes.
                 */}
                <datalist id="series-de-vacina">
                  {series.map((serie) => (
                    <option key={serie} value={serie} />
                  ))}
                </datalist>
                <div style={nota}>{intl.formatMessage({ id: "registrar.vacina.nota" })}</div>
              </div>

              <div>
                <label htmlFor="proxima" style={rotulo}>
                  {intl.formatMessage({ id: "registrar.vacina.proxima" })}
                </label>
                <input
                  id="proxima"
                  type="date"
                  value={proximaDose}
                  onChange={(evento) => setProximaDose(evento.target.value)}
                  style={campo}
                />
                {/*
                 * Opcional, e a frase diz o que se perde ao deixar em branco: sem a proxima dose o
                 * produto nao tem como avisar, e o lembrete e metade do que ele promete.
                 */}
                <div style={nota}>{intl.formatMessage({ id: "registrar.vacina.proxima.nota" })}</div>
              </div>
            </>
          )}

          {oQue === "peso" && (
            <div>
              <label htmlFor="peso" style={rotulo}>
                {intl.formatMessage({ id: "registrar.peso.quanto" })}
              </label>
              <input
                id="peso"
                inputMode="decimal"
                value={peso}
                onChange={(evento) => setPeso(evento.target.value)}
                placeholder="8,6"
                style={{ ...campo, fontFamily: "'DM Mono', monospace" }}
              />
              <div style={nota}>{intl.formatMessage({ id: "registrar.peso.nota" })}</div>
            </div>
          )}

          {oQue === "observacao" && (
            <div>
              <label htmlFor="texto" style={rotulo}>
                {intl.formatMessage({ id: "registrar.observacao.oQue" })}
              </label>
              <textarea
                id="texto"
                rows={4}
                value={texto}
                onChange={(evento) => setTexto(evento.target.value)}
                style={{ ...campo, minHeight: "96px", lineHeight: 1.6, resize: "vertical" }}
              />
              {/*
               * A MESMA FRASE DA TELA 18, e pela mesma razao: observacao nunca vira ato clinico
               * sozinha. "A distincao entre o que alguem viu e o que alguem concluiu e a mais
               * importante do produto."
               */}
              <div style={nota}>{intl.formatMessage({ id: "registrar.observacao.nota" })}</div>
            </div>
          )}

          {/* `oQue` entra na frase "Nao conseguimos gravar {o_que}" — e um substantivo, e nao o
              nome do animal, que faria a frase dizer que nao conseguimos gravar o bicho. */}
          {erro !== null && erro !== undefined && (
            <ErroAoGravar erro={erro} oQue={intl.formatMessage({ id: "registrar.oQueE" })} />
          )}

          {feito !== undefined && (
            <div style={{ border: "1px solid oklch(0.86 0.008 150)", background: "oklch(0.975 0.008 150)", borderRadius: "8px", padding: "14px 16px", fontSize: "15px", lineHeight: 1.6 }}>
              {feito}
            </div>
          )}

          <button
            type="button"
            disabled={!podeGravar}
            onClick={gravar}
            style={{
              fontFamily: "inherit",
              fontSize: "16px",
              fontWeight: 500,
              color: "oklch(1 0 0)",
              background: podeGravar ? VERDE : "oklch(0.72 0.02 150)",
              border: "none",
              borderRadius: "8px",
              padding: "15px 26px",
              minHeight: "52px",
              cursor: podeGravar ? "pointer" : "not-allowed",
            }}
          >
            {intl.formatMessage({ id: "registrar.gravar" })}
          </button>

          {/*
           * "O que voce registrar leva o seu nome, para sempre" — a mesma promessa que o convite de
           * co-tutoria faz. Ela vale aqui porque e aqui que a autoria nasce.
           */}
          <div style={nota}>{intl.formatMessage({ id: "registrar.autoria" })}</div>

          <Link to="/animais/$animalId" params={{ animalId }} style={{ color: VERDE, fontSize: "15px" }}>
            {intl.formatMessage({ id: "registrar.voltar" }, { nome })}
          </Link>
        </div>
      </div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

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

const nota = {
  fontSize: "13px",
  color: CINZA,
  marginTop: "7px",
  lineHeight: 1.55,
} as const;
