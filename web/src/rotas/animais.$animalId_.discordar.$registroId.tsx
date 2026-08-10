import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAnimal } from "../dados/carteira.ts";
import {
  LIMITE_DA_OBSERVACAO,
  useRegistrarObservacao,
  useRegistro,
} from "../dados/registros.ts";
import { ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 23 · area do tutor — Contestar um registro, sem botao de apagar", de
 * `design/IdentidadeVisual/Telas Petfy - Confianca e operacao.dc.html`.
 *
 * <b>A tese e o titulo:</b> nao ha botao de apagar, e nao ha como editar o registro de outra
 * pessoa. "Quem escreveu e quem corrige — e o que faz o registro valer alguma coisa." O que o
 * tutor pode e escrever ao lado, assinado por ele, na data do atendimento — e isso tambem
 * fica para sempre.
 *
 * <b>A CAIXA "AVISAR ANA FERREIRA" FICA DESABILITADA, e essa e a maior perda desta tela.</b>
 * O desenho oferece avisar quem registrou, para ela poder corrigir se concordar. Nao existe:
 * o `ObservationServiceImpl` nao chama notificador nenhum, e o `urgent` do DTO e uma marca
 * guardada, nao um aviso. Marcar a caixa nao acordaria ninguem — e uma caixa que promete
 * mensagem e nao manda mensagem e pior que a ausencia dela.
 *
 * <b>"FALAR DIRETO COM QUEM REGISTROU" TAMBEM NAO DA</b>, e pelo motivo que a Tela 02 ja
 * havia registrado: a autoria vem como nome em texto, sem id e sem contato. O telefone do
 * desenho nao existe em lugar nenhum do modelo.
 *
 * As duas explicacoes da direita ficam, porque as duas sao verdade: a correcao guarda o valor
 * anterior (`previousDescription` no DTO de correcao), e a observacao nao sai da linha do
 * tempo depois.
 */

export const Route = createFileRoute("/animais/$animalId_/discordar/$registroId")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Discordar,
});

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

function Discordar() {
  const { animalId, registroId } = Route.useParams();
  const intl = useIntl();
  const navegar = useNavigate();

  const animal = useAnimal(animalId);
  const registro = useRegistro(registroId);
  const observar = useRegistrarObservacao();
  const acao = useHover();

  const [texto, setTexto] = useState("");
  const nome = animal.data?.name ?? "";
  const restam = LIMITE_DA_OBSERVACAO - texto.length;

  const enviar = async () => {
    await observar.mutateAsync({
      animalId,
      texto: texto.trim(),
      /*
       * Meio-dia da data do atendimento. O que importa e o DIA — e a linha do tempo ordena
       * por quando aconteceu, entao a observacao encosta no registro em vez de aparecer hoje,
       * meses depois, longe do que ela contesta.
       */
      quando: registro.data?.eventDate === undefined ? undefined : `${registro.data.eventDate}T12:00:00`,
    });

    await navegar({ to: "/animais/$animalId", params: { animalId } });
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "40px 48px 44px" }}>
        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "discordar.titulo" })}
        </h1>
        <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "64ch" }}>
          {intl.formatMessage({ id: "discordar.apoio" })}
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 380px", gap: "32px", alignItems: "start" }}>
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* ------------------------------------------------------------ o registro */}
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
              <Rotulo>{intl.formatMessage({ id: "discordar.registro" })}</Rotulo>

              {registro.isError ? (
                <ErroDeCarga
                  oQue={intl.formatMessage({ id: "discordar.oQue" })}
                  erro={registro.error}
                  aoTentarDeNovo={() => void registro.refetch()}
                  carregando={registro.isFetching}
                />
              ) : registro.isPending ? (
                <Nota>{intl.formatMessage({ id: "discordar.registro.carregando" })}</Nota>
              ) : (
                <>
                  <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "8px" }}>
                    {/* O que o profissional escreveu ganha da categoria do enum. */}
                    {registro.data?.eventType ??
                      intl.formatMessage({
                        id: `animal.categoria.${registro.data?.category ?? "OUTRO"}`,
                      })}
                  </div>
                  {registro.data?.description !== undefined && (
                    <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "10px" }}>
                      {registro.data.description}
                    </div>
                  )}
                  {registro.data?.diagnosis !== undefined && (
                    <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "10px" }}>
                      {registro.data.diagnosis}
                    </div>
                  )}
                  <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                    {registro.data?.eventDate === undefined
                      ? ""
                      : intl.formatMessage(
                          { id: "discordar.registro.quando" },
                          {
                            data: intl.formatDate(new Date(`${registro.data.eventDate}T12:00:00`), {
                              dateStyle: "short",
                            }),
                          },
                        )}
                  </div>
                </>
              )}
            </div>

            {/* ------------------------------------------------------- a sua observacao */}
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
              <Rotulo>{intl.formatMessage({ id: "discordar.sua" })}</Rotulo>

              <label htmlFor="discordar-texto" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
                {intl.formatMessage({ id: "discordar.campo" })}
              </label>
              <textarea
                id="discordar-texto"
                value={texto}
                onChange={(evento) => setTexto(evento.target.value.slice(0, LIMITE_DA_OBSERVACAO))}
                rows={5}
                style={{ fontFamily: "inherit", width: "100%", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", lineHeight: 1.6, background: "oklch(1 0 0)", resize: "vertical" }}
              />
              <div style={{ display: "flex", justifyContent: "space-between", gap: "16px", marginTop: "8px" }}>
                <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.55, flex: 1 }}>
                  {intl.formatMessage(
                    { id: "discordar.campo.apoio" },
                    {
                      data:
                        registro.data?.eventDate === undefined
                          ? ""
                          : intl.formatDate(new Date(`${registro.data.eventDate}T12:00:00`), {
                              day: "2-digit",
                              month: "2-digit",
                            }),
                    },
                  )}
                </div>
                <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px", color: restam < 80 ? "oklch(0.45 0.13 30)" : "oklch(0.55 0.015 150)", flex: "none" }}>
                  {restam}
                </div>
              </div>

              {/* A caixa que o desenho pede, desabilitada com o motivo (secao 06). */}
              <div style={{ display: "flex", gap: "12px", alignItems: "flex-start", marginTop: "20px", opacity: 0.55 }}>
                <div aria-hidden style={{ width: "20px", height: "20px", borderRadius: "4px", border: "1px solid oklch(0.78 0.012 150)", flex: "none", marginTop: "2px" }}></div>
                <div style={{ fontSize: "15px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)" }}>
                  {intl.formatMessage({ id: "discordar.avisar" })}
                </div>
              </div>
              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "8px" }}>
                {intl.formatMessage({ id: "discordar.avisar.porque" })}
              </div>

              {observar.isError && (
                <div style={{ marginTop: "16px" }}>
                  <ErroAoGravar erro={observar.error} oQue={intl.formatMessage({ id: "discordar.oQue.gravar" })} />
                </div>
              )}

              <div style={{ display: "flex", gap: "12px", marginTop: "24px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  disabled={observar.isPending || texto.trim() === ""}
                  {...acao.props}
                  onClick={() => void enviar()}
                  style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: observar.isPending || texto.trim() === "" ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 22px", minHeight: "52px", cursor: texto.trim() === "" ? "not-allowed" : "pointer" }}
                >
                  {intl.formatMessage({ id: observar.isPending ? "discordar.acao.registrando" : "discordar.acao" })}
                </button>

                <Link
                  to="/animais/$animalId"
                  params={{ animalId }}
                  style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "15px 22px", minHeight: "52px", display: "flex", alignItems: "center", textDecoration: "none" }}
                >
                  {intl.formatMessage({ id: "discordar.cancelar" })}
                </Link>
              </div>
            </div>
          </div>

          {/* -------------------------------------------------------------- a direita */}
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
              <Rotulo>{intl.formatMessage({ id: "discordar.falar" })}</Rotulo>
              <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage({ id: "discordar.falar.porque" })}
              </div>
            </div>

            <Cartao
              titulo={intl.formatMessage({ id: "discordar.seCorrigir" })}
              texto={intl.formatMessage({ id: "discordar.seCorrigir.texto" })}
            />
            <Cartao
              titulo={intl.formatMessage({ id: "discordar.seNaoResponder" })}
              texto={intl.formatMessage({ id: "discordar.seNaoResponder.texto" }, { nome })}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
      {children}
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

function Cartao({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "22px 24px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>{texto}</div>
    </div>
  );
}
