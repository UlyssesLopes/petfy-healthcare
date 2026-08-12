import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { useEncerrarLinhaDoTempo } from "../dados/fim.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 33 · area do tutor — Encerrar a linha do tempo", de
 * `design/IdentidadeVisual/Telas Petfy - Fim, reencontro e conta.dc.html`.
 *
 * <b>O desenho a chama de "a tela que nenhum produto do setor desenha e que todos precisam".</b> E
 * o que este produto tinha no lugar dela era pior que nada: o `DELETE /animals/{id}`, que apaga a
 * carteira, o prontuario, o peso, os anexos e os arquivos no disco. Quem perdia o animal escolhia
 * entre destruir sete anos de registro e conviver para sempre com um cadastro que continua cobrando
 * vacina.
 *
 * -------------------------------------------------------------- por que so quem responde encerra
 *
 * "A veterinaria que atendeu o Code na ultima noite pode registrar o obito como ato clinico dela —
 * isso e o trabalho dela. Mas fechar a linha do tempo e do Marcelo, e nao pode acontecer sem ele.
 * NINGUEM DEVE DESCOBRIR QUE PERDEU O ANIMAL POR UMA NOTIFICACAO DO SISTEMA."
 *
 * A rota exige custodia, e nenhum nivel de concessao chega la. Esta tela nao reimplementa a regra:
 * ela pede, e o servidor responde 403.
 *
 * ------------------------------------------------------------------ o que o produto NAO faz aqui
 *
 * "Nao manda condolencias automaticas. Nao sugere adotar outro. Nao pergunta a causa da morte — se
 * voce quiser contar, o campo aberto esta la. Depois disso, o Petfy fica em silencio sobre o Code."
 *
 * <b>Um campo obrigatorio, e so um.</b> "Quando voce conseguir, preencha o que souber. Nada aqui
 * tem pressa." Exigir o local transformaria o pior dia do tutor num formulario que o recusa.
 *
 * ------------------------------------------------------- a linha do "o que acontece" que mudou
 *
 * O desenho escreve "a Clinica Vet Norte e a Creche Quintal sao avisadas, sem que voce precise
 * ligar para cada uma", e a frase e VERDADE: o `AnimalDeathNotifier` avisa quem alcanca o animal —
 * pessoas e organizacoes, por e-mail. O que a tela nao diz e o nome de cada uma: montar essa lista
 * exigiria ler a rede de cuidado inteira aqui, e errar nela seria prometer um aviso que nao sai.
 */

export const Route = createFileRoute("/animais/$animalId_/fim")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Fim,
});

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

function Fim() {
  const { animalId } = Route.useParams();
  const intl = useIntl();
  const navegar = useNavigate();

  const animal = useAnimal(animalId);
  const encerrar = useEncerrarLinhaDoTempo();

  const [quando, setQuando] = useState("");
  const [onde, setOnde] = useState("");
  const [despedida, setDespedida] = useState("");

  const nome = animal.data?.name ?? "";

  /* O unico campo que barra o envio. Os outros dois sao opcionais na tela e opcionais no
     servidor, e a tela nao inventa exigencia que a rota nao faz. */
  const podeRegistrar = quando !== "" && !encerrar.isPending;

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

  const registrar = () => {
    encerrar.mutate(
      {
        animalId,
        quando,
        onde: onde.trim() === "" ? undefined : onde.trim(),
        despedida: despedida.trim() === "" ? undefined : despedida.trim(),
      },
      {
        /*
         * Vai para a ficha do animal, e nao para a lista.
         *
         * A lista e onde ele deixou de estar, e cair nela seria a primeira coisa que o produto
         * mostra depois do registro: a ausencia. A ficha fechada e o que o desenho poe no cartao
         * "depois" — os sete anos dele, inteiros.
         */
        onSuccess: () => navegar({ to: "/animais/$animalId", params: { animalId } }),
      },
    );
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1300px", margin: "0 auto", display: "grid", gridTemplateColumns: "minmax(0, 800px) minmax(0, 440px)", gap: "24px", alignItems: "start" }}>

        {/* ==================================================================== o formulario */}
        <div style={{ background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "44px 48px 46px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 12px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "fim.titulo" }, { nome })}
          </h1>

          <p style={{ fontSize: "17px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", margin: "0 0 32px", maxWidth: "56ch" }}>
            {intl.formatMessage({ id: "fim.apoio" })}
          </p>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", marginBottom: "24px" }}>
            <div>
              <label htmlFor="quando" style={rotulo}>
                {intl.formatMessage({ id: "fim.quando" })}
              </label>
              <input
                id="quando"
                type="date"
                value={quando}
                /* O maximo e hoje: o servidor recusa data futura, e deixar o campo aceitar para
                   depois responder erro faria a pessoa preencher duas vezes. */
                max={new Date().toISOString().slice(0, 10)}
                onChange={(evento) => setQuando(evento.target.value)}
                style={{ ...campo, fontFamily: "'DM Mono', monospace" }}
              />
            </div>

            <div>
              <label htmlFor="onde" style={rotulo}>
                {intl.formatMessage({ id: "fim.onde" })}{" "}
                <span style={{ fontWeight: 400, color: "oklch(0.5 0.015 150)" }}>
                  · {intl.formatMessage({ id: "fim.opcional" })}
                </span>
              </label>
              <input
                id="onde"
                type="text"
                maxLength={120}
                value={onde}
                onChange={(evento) => setOnde(evento.target.value)}
                placeholder={intl.formatMessage({ id: "fim.onde.exemplo" })}
                style={campo}
              />
            </div>
          </div>

          <div style={{ marginBottom: "24px" }}>
            <label htmlFor="despedida" style={rotulo}>
              {intl.formatMessage({ id: "fim.despedida" })}{" "}
              <span style={{ fontWeight: 400, color: "oklch(0.5 0.015 150)" }}>
                · {intl.formatMessage({ id: "fim.opcional" })}
              </span>
            </label>
            <textarea
              id="despedida"
              rows={4}
              value={despedida}
              onChange={(evento) => setDespedida(evento.target.value)}
              placeholder={intl.formatMessage({ id: "fim.despedida.nota" })}
              style={{ ...campo, minHeight: "96px", lineHeight: 1.6, resize: "vertical" }}
            />
          </div>

          {/* ============================================================= o que acontece
            *
            * As tres primeiras linhas sao efeitos que a rota produz de fato — custodia encerrada,
            * aviso enviado, matricula encerrada — e por isso podem ser afirmadas. A quarta e o
            * que muda na tela de quem preenche, e vem com a marca tracejada do desenho.
            */}
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px", marginBottom: "24px" }}>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "16px" }}>
              {intl.formatMessage({ id: "fim.acontece" })}
            </div>

            <ul style={{ display: "flex", flexDirection: "column", gap: "14px", fontSize: "15px", lineHeight: 1.6, listStyle: "none", margin: 0, padding: 0 }}>
              {[
                { chave: "fim.acontece.avisos", firme: true },
                { chave: "fim.acontece.organizacoes", firme: true },
                { chave: "fim.acontece.historico", firme: true },
                { chave: "fim.acontece.lista", firme: false },
              ].map((linha) => (
                <li key={linha.chave} style={{ display: "flex", gap: "12px", color: linha.firme ? "inherit" : "oklch(0.45 0.015 150)" }}>
                  <span
                    aria-hidden
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "999px",
                      flex: "none",
                      marginTop: "5px",
                      background: linha.firme ? "oklch(0.46 0.085 150)" : "transparent",
                      border: linha.firme ? "none" : "2px dashed oklch(0.6 0.015 150)",
                    }}
                  ></span>
                  <span>{intl.formatMessage({ id: linha.chave }, { nome })}</span>
                </li>
              ))}
            </ul>
          </div>

          {encerrar.error !== null && encerrar.error !== undefined && (
            <div style={{ marginBottom: "16px" }}>
              <ErroAoGravar erro={encerrar.error} oQue={intl.formatMessage({ id: "fim.titulo" }, { nome })} />
            </div>
          )}

          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <button
              type="button"
              disabled={!podeRegistrar}
              onClick={registrar}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: podeRegistrar ? "oklch(0.46 0.085 150)" : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: podeRegistrar ? "pointer" : "not-allowed" }}
            >
              {intl.formatMessage({ id: encerrar.isPending ? "fim.registrando" : "fim.registrar" })}
            </button>

            {/*
             * "Agora nao" e um link de volta, e nao um botao que descarta.
             *
             * "Voce pode fechar esta tela e voltar depois" — nada foi gravado, entao nao ha o que
             * confirmar nem o que perder. Um dialogo de "descartar o que voce escreveu?" aqui
             * seria o produto insistindo no assunto.
             */}
            <Link
              to="/animais/$animalId"
              params={{ animalId }}
              style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "15px 24px", minHeight: "52px", display: "flex", alignItems: "center", textDecoration: "none" }}
            >
              {intl.formatMessage({ id: "fim.agoraNao" })}
            </Link>
          </div>
        </div>

        {/* ======================================================= por que so quem responde */}
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "24px 26px" }}>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "12px" }}>
              {intl.formatMessage({ id: "fim.porQueSoVoce" })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "fim.porQueSoVoce.texto" })}
            </div>
          </div>

          <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "24px 26px" }}>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "12px" }}>
              {intl.formatMessage({ id: "fim.oQueNaoFazemos" })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "fim.oQueNaoFazemos.texto" })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
