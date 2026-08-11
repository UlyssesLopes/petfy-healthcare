import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";
import {
  useAceitarUniao,
  useDuplicatas,
  useMarcarComoDiferentes,
  usePedidosDeUniao,
  usePedirUniao,
  useRecusarUniao,
  type LadoDaUniao,
  type PedidoDeUniao,
} from "../dados/uniao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 32 · o caso delicado — O mesmo animal, cadastrado duas vezes", de
 * `design/IdentidadeVisual/Telas Petfy - Nucleo clinico.dc.html`.
 *
 * <b>UMA TELA, DOIS LADOS.</b> Quem percebe a duplicata e quase sempre a clinica — ela tem o
 * leitor de microchip na mao. Quem decide e quem responde pelo animal. A mesma rota serve os dois
 * porque a COMPARACAO e identica: "ele recebe o pedido, ve exatamente esta comparacao e decide".
 * Duas telas com a mesma tabela divergiriam, e a que o tutor le e a que precisa estar certa.
 *
 * <b>A TELA NAO ADIVINHA QUEM PODE DECIDIR.</b> Ela mostra os botoes de decisao quando ha pedido
 * pendente, e o servidor responde 403 a quem nao responde pelo animal. Esconder o botao exigiria
 * o cliente recalcular custodia — uma segunda verdade sobre quem manda, e a que aparece na tela
 * seria a que ninguem confere.
 *
 * <b>"VOCE NAO PODE UNIR SOZINHA" NAO E UM ERRO, E A PRIMEIRA COISA QUE A TELA DIZ.</b> Ela vem
 * antes do botao, e nao depois de um 403 — quem clica e le "proibido" aprende que o produto e
 * hostil; quem le antes entende a regra: "e a mesma regra da transferencia: ninguem mexe na vida
 * registrada de um animal sem quem responde por ele".
 *
 * <b>OS DOIS BOTOES NAO PEDEM A MESMA COISA.</b> "Pedir a uniao" vai para o tutor e espera. "Sao
 * animais diferentes" acontece na hora, porque nao mexe em nada — so acende uma marca no microchip
 * dos dois cadastros. Fazer o segundo tambem esperar aprovacao obrigaria a clinica a pedir
 * permissao para relatar o que ela acabou de constatar.
 */

export const Route = createFileRoute("/pacientes/$animalId/duplicado")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Duplicado,
});

function Duplicado() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const duplicatas = useDuplicatas(animalId);
  const pedidos = usePedidosDeUniao(animalId);

  const pedir = usePedirUniao();
  const diferentes = useMarcarComoDiferentes();
  const aceitar = useAceitarUniao();
  const recusar = useRecusarUniao();

  const [motivo, setMotivo] = useState("");
  const [escolhido, setEscolhido] = useState<string | undefined>(undefined);

  const bicho = animal.data;
  const candidatos = duplicatas.data ?? [];
  const pendentes = pedidos.data ?? [];

  const alvo = candidatos.find((c) => c.animalId === escolhido) ?? candidatos[0];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1000px", margin: "0 auto", display: "flex", flexDirection: "column", gap: "20px" }}>

        {/* ================================================ o lado de quem DECIDE, quando ha */}
        {pendentes.map((pedido) => (
          <PedidoParaDecidir
            key={pedido.animalMergeRequestId}
            pedido={pedido}
            aoAceitar={() => aceitar.mutate(pedido.animalMergeRequestId!)}
            aoRecusar={() => recusar.mutate(pedido.animalMergeRequestId!)}
            gravando={aceitar.isPending || recusar.isPending}
            erro={aceitar.error ?? recusar.error}
          />
        ))}

        {/* ================================================ o lado de quem PERCEBEU */}
        {duplicatas.isPending && <Carregando oQue={intl.formatMessage({ id: "duplicado.oQue" })} />}

        {!duplicatas.isPending && candidatos.length === 0 && pendentes.length === 0 && (
          <Caixa>
            <Titulo>{intl.formatMessage({ id: "duplicado.nenhuma.titulo" })}</Titulo>
            <Texto>{intl.formatMessage({ id: "duplicado.nenhuma.texto" })}</Texto>
            <div style={{ marginTop: "20px" }}>
              <Link to="/pacientes/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "duplicado.voltar" })}
              </Link>
            </div>
          </Caixa>
        )}

        {alvo !== undefined && (
          <>
            <Caixa destaque>
              <Titulo>{intl.formatMessage({ id: "duplicado.titulo" })}</Titulo>
              <Texto>
                {intl.formatMessage(
                  { id: "duplicado.apoio" },
                  { microchip: bicho?.microchipNumber ?? "" },
                )}
              </Texto>
            </Caixa>

            {/* Mais de uma duplicata acontece com o gato de rua visto por tres pessoas. Cada par
                e uma decisao propria, e a tela deixa escolher de qual se fala. */}
            {candidatos.length > 1 && (
              <Caixa>
                <Rotulo>{intl.formatMessage({ id: "duplicado.varios" }, { quantos: candidatos.length })}</Rotulo>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                  {candidatos.map((candidato) => (
                    <button
                      key={candidato.animalId}
                      type="button"
                      onClick={() => setEscolhido(candidato.animalId)}
                      style={{ fontFamily: "inherit", fontSize: "15px", border: `1px solid ${candidato.animalId === alvo.animalId ? "oklch(0.46 0.085 150)" : "oklch(0.90 0.008 150)"}`, background: candidato.animalId === alvo.animalId ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", cursor: "pointer" }}
                    >
                      {candidato.name}
                    </button>
                  ))}
                </div>
              </Caixa>
            )}

            <Comparacao
              sobrevivente={alvo}
              absorvido={{
                animalId,
                name: bicho?.name,
                species: bicho?.species,
                breed: bicho?.breed,
                microchipNumber: bicho?.microchipNumber,
                eventCount: undefined,
                holderName: undefined,
                firstEventAt: undefined,
                peopleCount: undefined,
                organizationCount: undefined,
              }}
            />

            <OQueAcontece />

            {/* "Você não pode unir sozinha" — antes do botao, e nao depois de um 403. */}
            <Caixa destaque>
              <Titulo>{intl.formatMessage({ id: "duplicado.naoSozinha.titulo" })}</Titulo>
              <Texto>
                {intl.formatMessage(
                  { id: "duplicado.naoSozinha.texto" },
                  { quem: alvo.holderName ?? intl.formatMessage({ id: "duplicado.semResponsavel" }) },
                )}
              </Texto>
            </Caixa>

            <Caixa>
              <Rotulo>{intl.formatMessage({ id: "duplicado.motivo" })}</Rotulo>
              <textarea
                value={motivo}
                onChange={(evento) => setMotivo(evento.target.value)}
                rows={3}
                placeholder={intl.formatMessage({ id: "duplicado.motivo.exemplo" })}
                aria-label={intl.formatMessage({ id: "duplicado.motivo" })}
                style={{ fontFamily: "inherit", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", width: "100%", minHeight: "88px", resize: "vertical", background: "oklch(1 0 0)" }}
              />
              <Nota>{intl.formatMessage({ id: "duplicado.motivo.nota" })}</Nota>

              {(pedir.isError || diferentes.isError) && (
                <div style={{ marginTop: "16px" }}>
                  <ErroAoGravar
                    erro={pedir.error ?? diferentes.error}
                    oQue={intl.formatMessage({ id: "duplicado.oQue.pedido" })}
                  />
                </div>
              )}

              {pedir.isSuccess && (
                <div role="status" style={{ marginTop: "16px", border: "1px solid oklch(0.86 0.05 150)", background: "oklch(0.975 0.012 150)", borderRadius: "8px", padding: "14px 16px", fontSize: "15px", lineHeight: 1.6 }}>
                  {intl.formatMessage(
                    { id: "duplicado.pedido.enviado" },
                    { quem: alvo.holderName ?? "" },
                  )}
                </div>
              )}

              {diferentes.isSuccess && (
                <div role="status" style={{ marginTop: "16px", border: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", borderRadius: "8px", padding: "14px 16px", fontSize: "15px", lineHeight: 1.6 }}>
                  {intl.formatMessage({ id: "duplicado.marcado" })}
                </div>
              )}

              <div style={{ display: "flex", gap: "12px", marginTop: "22px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  disabled={motivo.trim() === "" || pedir.isPending || pedir.isSuccess}
                  onClick={() =>
                    pedir.mutate({
                      sobreviventeId: alvo.animalId!,
                      absorvidoId: animalId,
                      motivo: motivo.trim(),
                    })
                  }
                  style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: motivo.trim() === "" || pedir.isSuccess ? "oklch(0.62 0.05 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 26px", minHeight: "52px", cursor: motivo.trim() === "" ? "not-allowed" : "pointer" }}
                >
                  {intl.formatMessage(
                    { id: pedir.isPending ? "duplicado.pedindo" : "duplicado.pedir" },
                    { quem: alvo.holderName ?? intl.formatMessage({ id: "duplicado.semResponsavel" }) },
                  )}
                </button>

                {/* Acontece na hora: nao mexe em nada, so acende a marca. */}
                <button
                  type="button"
                  disabled={diferentes.isPending || diferentes.isSuccess}
                  onClick={() =>
                    diferentes.mutate({ animalId, outroAnimalId: alvo.animalId! })
                  }
                  style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "15px 26px", minHeight: "52px", cursor: "pointer" }}
                >
                  {intl.formatMessage({ id: "duplicado.diferentes" })}
                </button>
              </div>

              <Nota>{intl.formatMessage({ id: "duplicado.diferentes.nota" })}</Nota>
            </Caixa>

            <div>
              <Link to="/pacientes/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "duplicado.voltar" })}
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/** O pedido na tela de quem responde pelo animal — com a MESMA comparacao. */
function PedidoParaDecidir({
  pedido,
  aoAceitar,
  aoRecusar,
  gravando,
  erro,
}: {
  pedido: PedidoDeUniao;
  aoAceitar: () => void;
  aoRecusar: () => void;
  gravando: boolean;
  erro: unknown;
}) {
  const intl = useIntl();

  return (
    <Caixa destaque>
      <Titulo>{intl.formatMessage({ id: "duplicado.pedido.titulo" })}</Titulo>
      <Texto>
        {intl.formatMessage(
          { id: "duplicado.pedido.dequem" },
          {
            quem: pedido.requestedByName ?? "",
            onde: pedido.organizationName ?? intl.formatMessage({ id: "duplicado.porSi" }),
          },
        )}
      </Texto>

      <div style={{ marginTop: "16px", borderLeft: "3px solid oklch(0.62 0.11 70)", paddingLeft: "16px", fontSize: "16px", lineHeight: 1.65, color: "oklch(0.3 0.02 150)" }}>
        “{pedido.reason}”
      </div>

      {pedido.surviving !== undefined && pedido.absorbed !== undefined && (
        <div style={{ marginTop: "20px" }}>
          <Comparacao sobrevivente={pedido.surviving} absorvido={pedido.absorbed} />
        </div>
      )}

      <div style={{ marginTop: "20px" }}>
        <OQueAcontece />
      </div>

      {erro !== undefined && erro !== null && (
        <div style={{ marginTop: "18px" }}>
          <ErroAoGravar erro={erro} oQue={intl.formatMessage({ id: "duplicado.oQue.decisao" })} />
        </div>
      )}

      <div style={{ display: "flex", gap: "12px", marginTop: "22px", flexWrap: "wrap" }}>
        <button
          type="button"
          disabled={gravando}
          onClick={aoAceitar}
          style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: gravando ? "oklch(0.62 0.05 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 26px", minHeight: "52px", cursor: "pointer" }}
        >
          {intl.formatMessage({ id: gravando ? "duplicado.unindo" : "duplicado.unir" })}
        </button>

        <button
          type="button"
          disabled={gravando}
          onClick={aoRecusar}
          style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "15px 26px", minHeight: "52px", cursor: "pointer" }}
        >
          {intl.formatMessage({ id: "duplicado.diferentes" })}
        </button>
      </div>

      {/* A irreversibilidade fica ao lado do botao, e nao num aviso que se fecha. */}
      <Nota>{intl.formatMessage({ id: "duplicado.irreversivel" })}</Nota>
    </Caixa>
  );
}

/**
 * Os dois lados, e o que faz a comparacao valer: <b>o TAMANHO de cada linha do tempo</b>.
 *
 * "147 eventos, desde 14/02/2019" contra "1, hoje" e o que faz quem decide entender qual dos dois
 * e o cadastro real do animal — sem isso, a tela mostraria dois nomes e duas racas e a pergunta
 * "qual devo manter" ficaria sem a resposta que ela realmente tem.
 */
function Comparacao({
  sobrevivente,
  absorvido,
}: {
  sobrevivente: LadoDaUniao;
  absorvido: LadoDaUniao;
}) {
  const intl = useIntl();

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
      <Lado lado={sobrevivente} rotulo={intl.formatMessage({ id: "duplicado.lado.existe" })} />
      <Lado lado={absorvido} rotulo={intl.formatMessage({ id: "duplicado.lado.novo" })} />
    </div>
  );
}

function Lado({ lado, rotulo }: { lado: LadoDaUniao; rotulo: string }) {
  const intl = useIntl();

  const identidade = [
    lado.species === undefined ? undefined : intl.formatMessage({ id: `animal.especie.${lado.species}` }),
    lado.breed,
  ].filter((parte) => parte !== undefined && parte !== "");

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "20px 22px" }}>
      <Rotulo>{rotulo}</Rotulo>

      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, marginBottom: "6px" }}>
        {lado.name}
      </div>

      {identidade.length > 0 && (
        <div style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)" }}>{identidade.join(" · ")}</div>
      )}

      <Linha
        rotulo={intl.formatMessage({ id: "duplicado.responsavel" })}
        valor={lado.holderName ?? intl.formatMessage({ id: "duplicado.semResponsavel" })}
      />

      {/* Nulo quando a tela ainda nao sabe o tamanho — o lado que o proprio cliente montou, no
          fluxo de quem acabou de cadastrar. Zero seria mentira: diria "nao tem nada". */}
      {lado.eventCount !== undefined && (
        <Linha
          rotulo={intl.formatMessage({ id: "duplicado.eventos" })}
          valor={intl.formatMessage(
            { id: lado.firstEventAt === undefined ? "duplicado.eventos.semData" : "duplicado.eventos.desde" },
            {
              quantos: lado.eventCount,
              desde:
                lado.firstEventAt === undefined
                  ? ""
                  : intl.formatDate(lado.firstEventAt, { dateStyle: "short" }),
            },
          )}
        />
      )}

      {lado.peopleCount !== undefined && lado.peopleCount > 0 && (
        <Linha
          rotulo={intl.formatMessage({ id: "duplicado.quemRegistrou" })}
          valor={intl.formatMessage(
            { id: "duplicado.quemRegistrou.valor" },
            { pessoas: lado.peopleCount, organizacoes: lado.organizationCount ?? 0 },
          )}
        />
      )}
    </div>
  );
}

/** As quatro promessas do desenho, e nenhuma delas e decorativa. */
function OQueAcontece() {
  const intl = useIntl();

  return (
    <Caixa>
      <Rotulo>{intl.formatMessage({ id: "duplicado.oQueAcontece" })}</Rotulo>
      {["ordem", "assinatura", "evento", "custodia"].map((qual) => (
        <div key={qual} style={{ display: "flex", gap: "12px", marginTop: "10px", alignItems: "flex-start" }}>
          <div aria-hidden style={{ width: "6px", height: "6px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", marginTop: "9px", flex: "none" }}></div>
          <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)" }}>
            {intl.formatMessage({ id: `duplicado.acontece.${qual}` })}
          </div>
        </div>
      ))}
    </Caixa>
  );
}

function Caixa({ children, destaque = false }: { children: ReactNode; destaque?: boolean }) {
  return (
    <div style={{ border: `1px solid ${destaque ? "oklch(0.86 0.03 70)" : "oklch(0.90 0.008 150)"}`, background: destaque ? "oklch(0.985 0.012 70)" : "oklch(1 0 0)", borderRadius: "12px", padding: "24px 26px" }}>
      {children}
    </div>
  );
}

function Titulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, marginBottom: "10px" }}>
      {children}
    </div>
  );
}

function Texto({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "16px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)", maxWidth: "72ch" }}>
      {children}
    </div>
  );
}

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "12px" }}>
      {children}
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "12px", lineHeight: 1.6 }}>
      {children}
    </div>
  );
}

function Linha({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", marginTop: "12px", fontSize: "15px" }}>
      <span style={{ color: "oklch(0.5 0.015 150)" }}>{rotulo}</span>
      <span style={{ textAlign: "right", color: "oklch(0.3 0.02 150)" }}>{valor}</span>
    </div>
  );
}
