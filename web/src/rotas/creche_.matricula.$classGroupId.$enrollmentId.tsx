import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useEffect, useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useMatriculasDaTurma, type Matricula } from "../dados/creche.ts";
import { DIAS_DA_SEMANA, useGravarCombinado } from "../dados/custo.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 41 · dentro da matricula — A mensalidade da creche", de
 * `design/IdentidadeVisual/Telas Petfy - Por onde o valor entra.dc.html`, do lado de quem ESCREVE.
 *
 * <b>ESCREVE A CRECHE, LE SO QUEM RESPONDE PELO ANIMAL.</b> A creche e quem sabe o que combinou, e
 * e por isso que ela grava: exigir que o tutor digitasse o proprio boleto seria o oposto de "o
 * custo do animal so existe se o dado entrar sem esforco". Ela le de volta o que ela mesma
 * combinou — precisa, para conferir e corrigir —, e nao alcanca o combinado de nenhuma outra
 * organizacao.
 *
 * <b>O DESENHO TEM UM CARTAO QUE ESTA TELA NAO PODE MOSTRAR, e a diferenca e a regra do produto.</b>
 * A direita da Tela 41 esta "o que o Marcelo passa a ver, sem perguntar", com a mensalidade E a
 * diaria de 22/07 ja lancada. Ler custo exige CUSTODIA, e a creche nao tem: "o que a Clinica Vet
 * Norte cobra do Marcelo nao e assunto da creche, do petshop nem de outra clinica". Entao o que
 * esta tela mostra a direita e o que o combinado VAI produzir, derivado dos campos ao lado — e nao
 * a conta do animal, que ela nao le. Perder essa distincao seria abrir o preco de todo mundo para
 * quem tem uma turma.
 *
 * <b>O CONTROLE DE DIAS NAO ESTA NO MOCKUP, e isso e um desvio registrado.</b> O desenho desenha
 * tres campos e escreve, duas vezes, que a diaria e "usada quando o Code vem FORA DOS DIAS
 * COMBINADOS" — e o tutor le "3 dias por semana" no cartao ao lado. Os dias precisam existir para
 * a diaria poder entrar sozinha; sem eles, ela so entraria se alguem digitasse, que e exatamente
 * o que o desenho recusa.
 *
 * <b>NENHUMA COBRANCA SAI DAQUI.</b> "O Petfy nao cobra, nao emite boleto e nao processa
 * pagamento. Ele guarda o que foi combinado, para que o tutor veja o custo real do Code e ninguem
 * precise perguntar por telefone." Gravar a mensalidade nao lanca mensalidade nenhuma.
 */

export const Route = createFileRoute("/creche_/matricula/$classGroupId/$enrollmentId")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: MatriculaDaCreche,
});

/** "R$ 530,00" e "530.00" chegam iguais. Virgula e como se digita dinheiro em pt-BR. */
function emNumero(valor: string): number {
  return Number(valor.replace(/[^\d,.-]/g, "").replace(",", "."));
}

/** Campo de dinheiro vazio some do combinado — e o que "opcional" quer dizer no PUT. */
function valorOuNada(texto: string): number | undefined {
  if (texto.trim() === "") {
    return undefined;
  }

  const numero = emNumero(texto);
  return Number.isNaN(numero) ? undefined : numero;
}

function MatriculaDaCreche() {
  const { classGroupId, enrollmentId } = Route.useParams();
  const intl = useIntl();

  const matriculas = useMatriculasDaTurma(classGroupId);
  const matricula = (matriculas.data ?? []).find(
    (item) => item.enrollmentId === enrollmentId,
  );

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto" }}>
        {matriculas.isError ? (
          <ErroDeCarga
            oQue={intl.formatMessage({ id: "combinado.oQue" })}
            erro={matriculas.error}
            aoTentarDeNovo={() => void matriculas.refetch()}
            carregando={matriculas.isFetching}
          />
        ) : matriculas.isPending ? (
          <Carregando oQue={intl.formatMessage({ id: "combinado.oQue" })} />
        ) : matricula === undefined ? (
          <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "combinado.semMatricula" })}
          </div>
        ) : (
          <Combinado matricula={matricula} />
        )}

        <div style={{ marginTop: "26px" }}>
          <Link to="/creche" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
            {intl.formatMessage({ id: "combinado.voltar" })}
          </Link>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Combinado({ matricula }: { matricula: Matricula }) {
  const intl = useIntl();
  const gravar = useGravarCombinado();

  const [mensalidade, setMensalidade] = useState("");
  const [vencimento, setVencimento] = useState("");
  const [diaria, setDiaria] = useState("");
  const [dias, setDias] = useState<string[]>([]);
  const [gravou, setGravou] = useState(false);

  /*
   * O FORMULARIO NASCE DO QUE JA ESTA COMBINADO, e nao vazio.
   *
   * O PUT substitui o combinado inteiro: um formulario que comecasse em branco e fosse enviado com
   * um campo so apagaria os outros tres. Quem abre a tela para mexer na diaria nao esta pedindo
   * para zerar a mensalidade — e essa diferenca precisa existir ANTES do primeiro clique, e nao no
   * momento de montar o corpo.
   */
  const enrollmentId = matricula.enrollmentId;

  useEffect(() => {
    setMensalidade(matricula.monthlyFee === undefined ? "" : String(matricula.monthlyFee));
    setVencimento(matricula.dueDay === undefined ? "" : String(matricula.dueDay));
    setDiaria(matricula.dailyRate === undefined ? "" : String(matricula.dailyRate));
    setDias(matricula.weekdays ?? []);
  }, [matricula.monthlyFee, matricula.dueDay, matricula.dailyRate, matricula.weekdays]);

  const alternar = (dia: string) =>
    setDias((antes) => (antes.includes(dia) ? antes.filter((item) => item !== dia) : [...antes, dia]));

  const enviar = () => {
    if (enrollmentId === undefined) {
      return;
    }

    setGravou(false);

    gravar.mutate(
      {
        enrollmentId,
        mensalidade: valorOuNada(mensalidade),
        diaDoVencimento: vencimento.trim() === "" ? undefined : Number(vencimento),
        diaria: valorOuNada(diaria),
        /* Na ordem da semana, e nao na ordem em que a creche clicou: os dois sao o mesmo
           combinado, e a segunda ordem faria o tutor achar que algo mudou. */
        dias: DIAS_DA_SEMANA.filter((dia) => dias.includes(dia)),
      },
      { onSuccess: () => setGravou(true) },
    );
  };

  const valorDaDiaria = valorOuNada(diaria);
  const semDias = dias.length === 0;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 700px) minmax(0, 620px)", gap: "24px", alignItems: "start" }}>

      {/* =============================================== o combinado, que a creche escreve */}
      <div style={{ background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "32px 34px 36px" }}>
        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "24px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage(
            { id: "combinado.titulo" },
            { nome: matricula.animalName ?? "", turma: matricula.classGroupName ?? "" },
          )}
        </h1>
        <p style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)", margin: "0 0 26px" }}>
          {intl.formatMessage(
            { id: "combinado.assinatura" },
            { quem: matricula.createdByName ?? "", onde: matricula.organizationName ?? "" },
          )}
        </p>

        <div style={{ border: "1px solid oklch(0.46 0.085 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px", marginBottom: "20px" }}>
          <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "18px" }}>
            {intl.formatMessage({ id: "combinado.caixa" })}
            <span style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 400, color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "combinado.opcional" })}
            </span>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
            <div>
              <label htmlFor="mensalidade" style={rotulo}>
                {intl.formatMessage({ id: "combinado.mensalidade" })}
              </label>
              <input
                id="mensalidade"
                type="text"
                inputMode="decimal"
                value={mensalidade}
                onChange={(evento) => setMensalidade(evento.target.value)}
                placeholder="530,00"
                style={{ ...campo, fontFamily: "'DM Mono', monospace" }}
              />
            </div>

            <div>
              <label htmlFor="vencimento" style={rotulo}>
                {intl.formatMessage({ id: "combinado.vencimento" })}
              </label>
              {/*
               * ATE 28, e nao ate 31: dia 30 nao existe em fevereiro, e um vencimento que some uma
               * vez por ano e um vencimento que ninguem consegue explicar ao tutor. O servidor
               * recusa igual — este `max` e conveniencia, e nao a regra.
               */}
              <input
                id="vencimento"
                type="number"
                min={1}
                max={28}
                value={vencimento}
                onChange={(evento) => setVencimento(evento.target.value)}
                placeholder="05"
                style={{ ...campo, fontFamily: "'DM Mono', monospace" }}
              />
            </div>
          </div>

          <div style={{ marginBottom: "18px" }}>
            <label htmlFor="diaria" style={rotulo}>
              {intl.formatMessage({ id: "combinado.diaria" })}
            </label>
            <input
              id="diaria"
              type="text"
              inputMode="decimal"
              value={diaria}
              onChange={(evento) => setDiaria(evento.target.value)}
              placeholder="88,00"
              style={{ ...campo, fontFamily: "'DM Mono', monospace" }}
            />
            <Nota>
              {intl.formatMessage({ id: "combinado.diaria.nota" }, { nome: matricula.animalName ?? "" })}
            </Nota>
          </div>

          {/* --------------------------------------------------- os dias, que o mockup nao tem */}
          <div>
            <div style={rotulo}>{intl.formatMessage({ id: "combinado.dias" })}</div>

            <div role="group" aria-label={intl.formatMessage({ id: "combinado.dias" })} style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
              {DIAS_DA_SEMANA.map((dia) => {
                const marcado = dias.includes(dia);

                return (
                  <button
                    key={dia}
                    type="button"
                    aria-pressed={marcado}
                    onClick={() => alternar(dia)}
                    style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: marcado ? 500 : 400, minWidth: "56px", minHeight: "48px", padding: "0 14px", borderRadius: "8px", cursor: "pointer", border: `1px solid ${marcado ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, background: marcado ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)", color: marcado ? "oklch(0.38 0.07 150)" : "oklch(0.42 0.015 150)" }}
                  >
                    {intl.formatMessage({ id: `combinado.dia.${dia}` })}
                  </button>
                );
              })}
            </div>

            {/*
             * SEM DIAS, A DIARIA NUNCA ENTRA — e a tela diz isso em vez de deixar a creche
             * descobrir no fim do mes. Nao e um erro: nao declarar dia e comum, e o silencio nao
             * pode virar cobranca ao tutor. Por isso o aviso so aparece quando ha diaria combinada
             * sem dia nenhum, que e o unico caso em que o combinado nao faz o que parece fazer.
             */}
            {semDias && valorDaDiaria !== undefined && (
              <div role="status" style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "8px", padding: "12px 14px", marginTop: "12px", fontSize: "14px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
                {intl.formatMessage({ id: "combinado.semDias" }, { nome: matricula.animalName ?? "" })}
              </div>
            )}
          </div>
        </div>

        <div style={{ border: "1px solid oklch(0.88 0.02 150)", background: "oklch(0.98 0.008 150)", borderRadius: "12px", padding: "20px 22px", fontSize: "15px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)" }}>
          {intl.formatMessage({ id: "combinado.naoCobra" }, { nome: matricula.animalName ?? "" })}
        </div>

        {gravar.error !== null && gravar.error !== undefined && (
          <div style={{ marginTop: "20px" }}>
            <ErroAoGravar erro={gravar.error} oQue={intl.formatMessage({ id: "combinado.oQue" })} />
          </div>
        )}

        <div style={{ display: "flex", alignItems: "center", gap: "16px", marginTop: "24px", flexWrap: "wrap" }}>
          <button
            type="button"
            disabled={gravar.isPending}
            onClick={enviar}
            style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: gravar.isPending ? "oklch(0.62 0.05 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: gravar.isPending ? "wait" : "pointer" }}
          >
            {intl.formatMessage({ id: gravar.isPending ? "combinado.gravando" : "combinado.gravar" })}
          </button>

          {gravou && !gravar.isPending && (
            <span role="status" style={{ fontSize: "15px", color: "oklch(0.38 0.07 150)" }}>
              {intl.formatMessage({ id: "combinado.gravado" })}
            </span>
          )}
        </div>
      </div>

      {/* ================================== o que o combinado vai produzir — e nao a conta */}
      <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "26px 28px" }}>
        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "11px", letterSpacing: "0.06em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "16px" }}>
          {intl.formatMessage({ id: "combinado.oQueOTutorVe" })}
        </div>

        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", overflow: "hidden", marginBottom: "16px" }}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px", padding: "14px 18px", alignItems: "center", fontSize: "15px", minHeight: "56px", borderBottom: "1px solid oklch(0.95 0.005 150)" }}>
            <div>
              {intl.formatMessage(
                { id: "combinado.linha.mensalidade" },
                { onde: matricula.organizationName ?? "" },
              )}
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                {intl.formatMessage(
                  { id: "combinado.linha.mensalidade.detalhe" },
                  { dias: dias.length },
                )}
                {/* o vencimento so entra quando existe: "vence dia" sem numero seria uma
                    promessa pela metade */}
                {vencimento.trim() !== "" &&
                  intl.formatMessage(
                    { id: "combinado.linha.mensalidade.vence" },
                    { vence: vencimento.trim() },
                  )}
              </div>
            </div>
            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px" }}>
              {valorOuNada(mensalidade) === undefined
                ? "—"
                : intl.formatMessage(
                    { id: "combinado.porMes" },
                    {
                      valor: intl.formatNumber(valorOuNada(mensalidade)!, {
                        style: "currency",
                        currency: "BRL",
                      }),
                    },
                  )}
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px", padding: "14px 18px", alignItems: "center", fontSize: "15px", minHeight: "56px" }}>
            <div>
              {intl.formatMessage({ id: "combinado.linha.diaria" })}
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                {intl.formatMessage({ id: "combinado.linha.diaria.detalhe" })}
              </div>
            </div>
            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px" }}>
              {valorDaDiaria === undefined
                ? "—"
                : intl.formatNumber(valorDaDiaria, { style: "currency", currency: "BRL" })}
            </div>
          </div>
        </div>

        <div style={{ fontSize: "15px", lineHeight: 1.7, color: "oklch(0.42 0.015 150)" }}>
          <p style={{ margin: "0 0 14px" }}>
            {intl.formatMessage({ id: "combinado.sozinha" }, { nome: matricula.animalName ?? "" })}
          </p>
          <p style={{ margin: "0 0 14px" }}>{intl.formatMessage({ id: "combinado.contestavel" })}</p>
        </div>

        {/*
         * A CRECHE NAO LE A CONTA DO ANIMAL, e a tela diz isso onde o desenho poe a conta.
         * Escondido, o cartao pareceria uma lista incompleta; escrito, ele ensina a regra.
         */}
        <div style={{ borderTop: "1px solid oklch(0.94 0.006 150)", paddingTop: "16px", fontSize: "14px", lineHeight: 1.65, color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage(
            { id: "combinado.naoLemos" },
            { nome: matricula.animalName ?? "" },
          )}
        </div>
      </div>
    </div>
  );
}

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

const rotulo = {
  display: "block",
  fontSize: "13px",
  fontWeight: 500,
  color: "oklch(0.42 0.015 150)",
  marginBottom: "7px",
} as const;

function Nota({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.55 }}>
      {children}
    </div>
  );
}
