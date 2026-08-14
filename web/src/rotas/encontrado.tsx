import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { CartaoDoAnimal } from "../componentes/CartaoDoAnimal.tsx";
import { useProcurarPorMicrochip, type CartaoDeEncontrado } from "../dados/fim.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 34 · publica, sem conta — Achei um animal na rua", de
 * `design/IdentidadeVisual/Telas Petfy - Fim, reencontro e conta.dc.html`.
 *
 * <b>O desenho a chama de "a que so existe quando o registro salva uma vida".</b> E ela e a unica
 * tela do produto que responde a quem nao tem conta e nao vai criar uma: "quem encontrou pode ser a
 * unica pessoa com o animal nas proximas horas".
 *
 * ------------------------------------------------------------------- por que e o cartao inteiro
 *
 * "Se ele estiver em tratamento, ou se comer algo que lhe faz mal, esconder isso nao protege
 * ninguem. O que o cartao nunca traz — historico clinico, endereco, diagnosticos — continua fora,
 * aqui como em qualquer outro lugar."
 *
 * <b>O conjunto nao e escolhido pelo tutor, ao contrario do link compartilhado.</b> La ele decide
 * os escopos; aqui nao houve escolha de ninguem, e o produto responde sempre o mesmo. E por isso
 * que o cartao vem num DTO proprio, e nao no do `/share/{token}`.
 *
 * ------------------------------------------------------------- o que o desenho pede e nao existe
 *
 * O rodape do mockup diz "Marcelo foi avisado de que o Code foi procurado agora, NA REGIAO DA VILA
 * MADALENA". A segunda metade nao tem de onde sair: o produto nao sabe onde a pessoa esta, e pedir
 * a regiao a quem esta na calcada com um animal seria atrito no pior momento. O que existe e
 * verdadeiro — quem responde pelo animal ve, no log de leitura, que o cartao foi aberto por uma
 * busca de microchip — e e isso que o rodape diz.
 *
 * ------------------------------------------------------------------------------ a busca parcial
 *
 * <b>Ela nao existe, e a Tela 35 e que a tem.</b> "9810" acharia todo animal de uma fabricante de
 * chip e transformaria esta rota numa listagem de tutores com telefone. Aqui o numero e a
 * credencial: inteiro, ou nada.
 */

export const Route = createFileRoute("/encontrado")({
  /* Sem `beforeLoad`: e a unica tela do produto que NAO pede sessao. */
  component: Encontrado,
});

function Encontrado() {
  const intl = useIntl();
  const [numero, setNumero] = useState("");
  const procurar = useProcurarPorMicrochip();

  const cartao = procurar.data;
  const podeProcurar = numero.trim() !== "" && !procurar.isPending;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1300px", margin: "0 auto", display: "grid", gridTemplateColumns: "minmax(0, 390px) minmax(0, 390px) minmax(0, 480px)", gap: "24px", alignItems: "start" }}>

        {/* ======================================================================= a busca */}
        <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
          <div style={{ padding: "28px 24px 24px", borderBottom: "1px solid oklch(0.92 0.006 150)", background: "oklch(0.975 0.008 150)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "20px" }}>
              <span aria-hidden style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <span style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></span>
              </span>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
            </div>

            <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "24px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
              {intl.formatMessage({ id: "encontrado.titulo" })}
            </h1>
            <p style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", margin: 0 }}>
              {intl.formatMessage({ id: "encontrado.apoio" })}
            </p>
          </div>

          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              if (podeProcurar) {
                procurar.mutate(numero.trim());
              }
            }}
            style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "16px" }}
          >
            <div>
              <label htmlFor="microchip" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
                {intl.formatMessage({ id: "encontrado.numero" })}
              </label>
              <input
                id="microchip"
                type="text"
                inputMode="numeric"
                autoComplete="off"
                maxLength={32}
                value={numero}
                onChange={(evento) => setNumero(evento.target.value)}
                placeholder="981020 0034 5127"
                style={{ fontFamily: "'DM Mono', monospace", border: "1px solid oklch(0.46 0.085 150)", borderRadius: "4px", padding: "15px 14px", fontSize: "18px", minHeight: "56px", width: "100%", background: "oklch(1 0 0)" }}
              />
            </div>

            <button
              type="submit"
              disabled={!podeProcurar}
              style={{ fontFamily: "inherit", fontSize: "17px", fontWeight: 500, color: "oklch(1 0 0)", background: podeProcurar ? "oklch(0.46 0.085 150)" : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "17px", minHeight: "58px", cursor: podeProcurar ? "pointer" : "not-allowed" }}
            >
              {intl.formatMessage({ id: procurar.isPending ? "encontrado.procurando" : "encontrado.procurar" })}
            </button>

            <p style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.6, margin: 0 }}>
              {intl.formatMessage({ id: "encontrado.semConta" })}
            </p>
          </form>
        </div>

        {/* ====================================================================== o cartao */}
        {cartao !== undefined && <Cartao cartao={cartao} />}

        {/*
         * O VAZIO DIZ O QUE FAZER EM SEGUIDA, e essa e a especificacao: "'nenhum resultado
         * encontrado' deixaria a pessoa e o animal parados na calcada".
         *
         * A mensagem vem do `erro.161`, traduzida por codigo como todas as outras — e nao escrita
         * aqui. Escreve-la nesta tela criaria uma segunda fonte para a mesma frase.
         */}
        {procurar.isError && (
          <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "24px 26px" }}>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "10px" }}>
              {intl.formatMessage({ id: "encontrado.vazio.titulo" })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: chaveDoErro(procurar.error) })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * O cartao de emergencia, aberto pelo numero.
 *
 * <b>A ordem dos blocos e a do desenho, e ela nao e cosmetica:</b> os contatos primeiro, porque a
 * primeira coisa que quem achou o animal precisa e ligar para alguem. Alergia vem antes de
 * condicao pela mesma logica — e o que nao se pode dar ao animal nas proximas horas.
 */
function Cartao({ cartao }: { cartao: CartaoDeEncontrado }) {
  const intl = useIntl();
  const nome = cartao.animalName ?? "";

  return (
    <CartaoDoAnimal
      marca={intl.formatMessage({ id: "encontrado.cartao.marca" })}
      nome={nome}
      descricao={descricao(cartao, intl)}
      contatos={(cartao.contacts ?? []).map((contato) => ({
        nome: contato.name ?? "",
        telefone: contato.phone,
      }))}
      alergias={cartao.allergies ?? []}
      condicoes={cartao.conditions ?? []}
      medicacao={cartao.ongoingCare ?? []}
      vacinas={(cartao.vaccines ?? []).map((vacina) => ({
        nome: vacina.vaccineName ?? "",
        status: vacina.status,
        proximaDose: vacina.nextDoseDate,
      }))}
      rodape={intl.formatMessage({ id: "encontrado.rodape" }, { nome })}
    />
  );
}

/**
 * "Cao · SRD · 6 anos · 8,4 kg" — montada com o que existe, e sem buraco onde falta.
 *
 * <b>Duas mensagens concatenadas, e nao um `select` sobre campo vazio:</b> o ICU nao aceita chave
 * vazia, e um `{x, select, {} {} other {…}}` compila e so explode em tela, no caso raro. Aqui o
 * caso raro e o comum: metade dos animais resgatados nao tem data de nascimento nem peso.
 */
function descricao(cartao: CartaoDeEncontrado, intl: ReturnType<typeof useIntl>): string {
  const partes: string[] = [];

  if (cartao.animalType !== undefined && cartao.animalType !== null) {
    partes.push(cartao.animalType);
  }
  if (cartao.animalBreed !== undefined && cartao.animalBreed !== null) {
    partes.push(cartao.animalBreed);
  }
  if (cartao.animalWeight !== undefined && cartao.animalWeight !== null) {
    partes.push(`${intl.formatNumber(cartao.animalWeight, { maximumFractionDigits: 1 })} kg`);
  }

  return partes.join(" · ");
}

