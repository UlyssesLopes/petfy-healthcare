import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useCriarAnimal, useIdentificarAnimal, type Especie } from "../dados/animais.ts";
import { useConvidarCoTutor } from "../dados/animal.ts";
import { useEnviarAnexo } from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O onboarding de `design/IdentidadeVisual/Onboarding Petfy.dc.html` — "o primeiro animal,
 * em quatro passos" —, que e para onde a Tela 08 aponta.
 *
 * A regra que o proprio arquivo escreve, e que manda em tudo aqui: <b>"so o passo 1 e
 * obrigatorio. Do 2 em diante, 'agora nao' e um botao de verdade"</b>. E: "cada etapa deixa
 * algo registrado, e sair no meio nao desfaz nada". Por isso o animal e criado no fim do
 * passo 1 e cada passo seguinte grava por conta propria — nao existe rascunho, nao existe
 * "salvar tudo no final", e fechar o navegador no passo 3 nao perde o passo 2.
 *
 * ------------------------------------------------------- o que mudou em 2026-08-15, e por que
 *
 * <b>Tres "ainda nao" desta tela eram falsos, e um era verdadeiro.</b> A conferencia veio de
 * fora: o Ulysses abriu a tela, clicou em "Outro" e listou o que nao funcionava. Vale a regra
 * que a Tela 08 ja tinha ensinado — "ainda nao" tem tres significados, e a maioria e texto
 * velho que ninguem apagou.
 *
 * <ul>
 *   <li><b>"Outro" como especie: PASSOU A FUNCIONAR.</b> O {@code Species} tinha CANINA e
 *       FELINA, e a opcao ficava desabilitada com um paragrafo explicando por que o animal da
 *       pessoa nao cabia. Agora o enum tem oito valores e "Outro" abre um seletor. A coluna
 *       sempre foi {@code varchar(32)} sem CHECK — nao houve migration.</li>
 *   <li><b>A foto: o contrato SEMPRE declarou o multipart.</b> Estava escrito aqui que nao, e
 *       era falso — {@code POST /animals/{animalId}/attachments} e {@code multipart/form-data}
 *       no contrato e no cliente gerado. O que faltava era a funcao de envio no front.</li>
 *   <li><b>O passo 3 saiu do stepper.</b> A rota de OCR existe, mas <b>ela CRIA um animal
 *       novo</b> ({@code importAnimalFromIdCard}) — no passo 3 o animal ja existe desde o passo
 *       1, e chama-la ali criaria um segundo. E ela nao devolve as doses para conferencia, que
 *       e o que o desenho pede ("Confira o que lemos · 4 doses"). Esse e backend novo, e o
 *       passo nao fica na tela prometendo o que nao faz.</li>
 *   <li><b>RGA e tatuagem.</b> So {@code microchipNumber} existe. Ver {@code useIdentificarAnimal}.
 *       Continua verdadeiro.</li>
 *   <li><b>"O que sobra vira pendencia".</b> O {@code DueItemKind} nao tem "lancar a
 *       carteirinha". Continua verdadeiro, e a tela nao promete que gera.</li>
 * </ul>
 */

export const Route = createFileRoute("/animais/novo")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: PrimeiroAnimal,
});

type Passo = 1 | 2 | 3;

/**
 * As especies que o botao "Outro" abre — todas menos as duas que ja tem botao proprio.
 *
 * <b>Derivada do tipo gerado, e nao escrita a mao</b>: quando o backend ganhar uma especie, o
 * `tsc` acusa aqui em vez de a tela esquecer de oferecer a nova. A ordem e a de quem cadastra,
 * e `OUTRA` fica por ultimo porque e o fim da lista, nao o comeco dela.
 */
const AS_OUTRAS = ["AVE", "ROEDORA", "LAGOMORFA", "REPTIL", "EQUINA", "OUTRA"] as const satisfies
  readonly Exclude<Especie, "CANINA" | "FELINA">[];

/** Se a especie escolhida e uma das que moram atras do "Outro" — e portanto se o seletor abre. */
function ehOutra(especie: Especie): boolean {
  return (AS_OUTRAS as readonly string[]).includes(especie);
}

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

function PrimeiroAnimal() {
  const intl = useIntl();
  const navegar = useNavigate();

  const criar = useCriarAnimal();
  const identificar = useIdentificarAnimal();
  const convidar = useConvidarCoTutor();
  const enviarFoto = useEnviarAnexo();

  const [passo, setPasso] = useState<Passo>(1);

  const [nome, setNome] = useState("");
  const [especie, setEspecie] = useState<Especie>("CANINA");
  const [nascimento, setNascimento] = useState("");
  const [microchip, setMicrochip] = useState("");
  const [email, setEmail] = useState("");
  const [foto, setFoto] = useState<File | undefined>(undefined);

  /*
   * A previa e um object URL, e ele e revogado quando a foto troca ou a tela sai — sem isso o
   * blob fica preso na memoria da aba ate a navegacao dura.
   */
  const [previaDaFoto, setPreviaDaFoto] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (foto === undefined) {
      setPreviaDaFoto(undefined);
      return;
    }

    const url = URL.createObjectURL(foto);
    setPreviaDaFoto(url);

    return () => URL.revokeObjectURL(url);
  }, [foto]);

  /* O animal existe a partir do fim do passo 1, e o id e o que os passos seguintes usam. */
  const animalId = criar.data?.animalId;

  /* O link do convite, que e a unica copia que existe dele — o backend nao envia e-mail. */
  const linkDoConvite =
    convidar.data?.token === undefined || convidar.data.token === null
      ? undefined
      : `${window.location.origin}/convites/animal?token=${convidar.data.token}`;

  const irParaOAnimal = async () => {
    if (animalId !== undefined) {
      await navegar({ to: "/animais/$animalId", params: { animalId } });
      return;
    }

    await navegar({ to: "/" });
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1000px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <div aria-hidden style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
            </div>
            <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
          </div>

          {/* "Fazer isso depois" leva para onde a pessoa ja esta indo: o produto. */}
          <button
            type="button"
            onClick={() => void irParaOAnimal()}
            style={{ fontFamily: "inherit", fontSize: "15px", color: "oklch(0.46 0.085 150)", background: "transparent", border: "none", cursor: "pointer", textDecoration: "underline", textUnderlineOffset: "3px" }}
          >
            {intl.formatMessage({ id: "onboarding.depois" })}
          </button>
        </div>

        <div style={{ padding: "28px 56px 0" }}>
          {/*
           * TRES trilhos, e nao quatro: "Carteirinha" saiu junto com o passo dela.
           *
           * A barra "mostra o que ja ficou registrado em cada passo, e nao so quantos faltam" —
           * um trilho para um passo que nao existe mostraria o contrario.
           */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "12px" }}>
            <Trilho ativo={passo >= 1} feito={passo > 1} rotulo={intl.formatMessage({ id: "onboarding.trilho.animal" })} detalhe={criar.data?.name} />
            <Trilho ativo={passo >= 2} feito={passo > 2} rotulo={intl.formatMessage({ id: "onboarding.trilho.identificacao" })} detalhe={identificar.data?.microchipNumber ?? undefined} />
            <Trilho ativo={passo >= 3} feito={false} rotulo={intl.formatMessage({ id: "onboarding.trilho.quemCuida" })} />
          </div>
        </div>

        <div style={{ padding: "40px 56px 44px" }}>
          {passo === 1 && (
            <>
              <Titulo>{intl.formatMessage({ id: "onboarding.p1.titulo" })}</Titulo>
              <Apoio>{intl.formatMessage({ id: "onboarding.p1.apoio" })}</Apoio>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px 24px", maxWidth: "640px" }}>
                <div style={{ gridColumn: "1 / -1" }}>
                  <Rotulo para="novo-nome">{intl.formatMessage({ id: "onboarding.p1.nome" })}</Rotulo>
                  <input
                    id="novo-nome"
                    type="text"
                    value={nome}
                    onChange={(evento) => setNome(evento.target.value)}
                    style={estiloDoCampo}
                  />
                </div>

                <div>
                  <Rotulo>{intl.formatMessage({ id: "onboarding.p1.especie" })}</Rotulo>
                  <div style={{ display: "flex", gap: "10px" }}>
                    <Opcao
                      escolhida={especie === "CANINA"}
                      aoEscolher={() => setEspecie("CANINA")}
                      texto={intl.formatMessage({ id: "animal.especie.CANINA" })}
                    />
                    <Opcao
                      escolhida={especie === "FELINA"}
                      aoEscolher={() => setEspecie("FELINA")}
                      texto={intl.formatMessage({ id: "animal.especie.FELINA" })}
                    />
                    <Opcao
                      escolhida={ehOutra(especie)}
                      aoEscolher={() => setEspecie(AS_OUTRAS[0])}
                      texto={intl.formatMessage({ id: "onboarding.p1.especie.outro" })}
                    />
                  </div>

                  {/*
                   * "Outro" nao e uma especie: e a porta para as que nao cabem em tres botoes.
                   * Escolher revela o seletor, e o valor entra no dominio com o nome certo —
                   * calopsita vira AVE, e nao um CANINA que registraria cachorro.
                   */}
                  {ehOutra(especie) && (
                    <div style={{ marginTop: "12px" }}>
                      <Rotulo para="novo-especie-qual">
                        {intl.formatMessage({ id: "onboarding.p1.especie.qual" })}
                      </Rotulo>
                      <select
                        id="novo-especie-qual"
                        value={especie}
                        onChange={(evento) => setEspecie(evento.target.value as Especie)}
                        style={estiloDoCampo}
                      >
                        {AS_OUTRAS.map((valor) => (
                          <option key={valor} value={valor}>
                            {intl.formatMessage({ id: `animal.especie.${valor}` })}
                          </option>
                        ))}
                      </select>
                      <Nota>{intl.formatMessage({ id: "onboarding.p1.especie.semProtocolo" })}</Nota>
                    </div>
                  )}
                </div>

                <div>
                  <Rotulo para="novo-nascimento">
                    {intl.formatMessage({ id: "onboarding.p1.nascimento" })}
                    <span style={{ fontWeight: 400, color: "oklch(0.55 0.015 150)" }}>
                      {" "}
                      {intl.formatMessage({ id: "onboarding.opcional" })}
                    </span>
                  </Rotulo>
                  <input
                    id="novo-nascimento"
                    type="month"
                    value={nascimento}
                    max={new Date().toISOString().slice(0, 7)}
                    onChange={(evento) => setNascimento(evento.target.value)}
                    style={{ ...estiloDoCampo, fontFamily: "'DM Mono', monospace" }}
                  />
                  <Nota>{intl.formatMessage({ id: "onboarding.p1.nascimento.apoio" })}</Nota>
                </div>

                {/*
                 * A moldura da foto do desenho, agora com o botao que ela sempre pediu.
                 *
                 * <b>A foto e escolhida aqui e enviada depois</b>, no "Continuar": anexo pertence
                 * a um animal, e o animal so passa a existir no fim deste passo. Guardar o
                 * `File` em estado e o unico jeito de honrar a ordem sem inventar um rascunho.
                 *
                 * <b>Falha no envio nao desfaz o passo.</b> O animal ja foi gravado quando a foto
                 * sobe; perder o cadastro inteiro porque uma imagem falhou seria trocar o que
                 * importa pelo que pode ficar para depois — e o proprio desenho diz que a foto
                 * pode. O aviso aparece e o stepper segue.
                 */}
                <div style={{ gridColumn: "1 / -1", display: "flex", alignItems: "center", gap: "20px", border: "1px dashed oklch(0.84 0.012 150)", borderRadius: "12px", padding: "20px 22px" }}>
                  <div aria-hidden style={{ width: "64px", height: "64px", borderRadius: "999px", overflow: "hidden", background: "repeating-linear-gradient(135deg, oklch(0.94 0.006 150) 0 8px, oklch(0.96 0.004 150) 8px 16px)", flex: "none" }}>
                    {previaDaFoto !== undefined && (
                      <img src={previaDaFoto} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                    )}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: "16px", marginBottom: "4px" }}>
                      {intl.formatMessage({ id: "onboarding.p1.foto" })}
                    </div>
                    <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.5 }}>
                      {foto === undefined
                        ? intl.formatMessage({ id: "onboarding.p1.foto.apoio" })
                        : foto.name}
                    </div>
                  </div>

                  <label style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "12px 18px", minHeight: "44px", display: "flex", alignItems: "center", cursor: "pointer" }}>
                    {intl.formatMessage({ id: "onboarding.p1.foto.escolher" })}
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      style={{ display: "none" }}
                      onChange={(evento) => setFoto(evento.target.files?.[0])}
                    />
                  </label>
                </div>

                {enviarFoto.isError && (
                  <div style={{ gridColumn: "1 / -1" }}>
                    <Alerta>{intl.formatMessage({ id: chaveDoErro(enviarFoto.error) })}</Alerta>
                  </div>
                )}
              </div>

              {criar.isError && <Alerta>{intl.formatMessage({ id: chaveDoErro(criar.error) })}</Alerta>}

              <Rodape>
                <Principal
                  pendente={criar.isPending}
                  desabilitado={nome.trim() === ""}
                  aoClicar={async () => {
                    const animal = await criar.mutateAsync({
                      nome: nome.trim(),
                      especie,
                      nascimento: nascimento === "" ? undefined : nascimento,
                    });

                    /* A foto sobe depois do animal, e a falha dela nao segura o passo. */
                    if (foto !== undefined && animal.animalId !== undefined) {
                      try {
                        await enviarFoto.mutateAsync({
                          animalId: animal.animalId,
                          arquivo: foto,
                          descricao: intl.formatMessage({ id: "onboarding.p1.foto" }),
                        });
                      } catch {
                        setPasso(2);
                        return;
                      }
                    }

                    setPasso(2);
                  }}
                >
                  {intl.formatMessage({ id: criar.isPending ? "onboarding.registrando" : "onboarding.continuar" })}
                </Principal>
                <span style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
                  {intl.formatMessage({ id: "onboarding.p1.aviso" }, { nome: nome.trim() })}
                </span>
              </Rodape>
            </>
          )}

          {passo === 2 && (
            <>
              <Titulo>{intl.formatMessage({ id: "onboarding.p2.titulo" }, { nome: criar.data?.name ?? "" })}</Titulo>
              <Apoio>{intl.formatMessage({ id: "onboarding.p2.apoio" }, { nome: criar.data?.name ?? "" })}</Apoio>

              <div style={{ maxWidth: "420px" }}>
                <Rotulo para="novo-microchip">{intl.formatMessage({ id: "onboarding.p2.microchip" })}</Rotulo>
                <input
                  id="novo-microchip"
                  type="text"
                  value={microchip}
                  inputMode="numeric"
                  onChange={(evento) => setMicrochip(evento.target.value)}
                  style={{ ...estiloDoCampo, fontFamily: "'DM Mono', monospace" }}
                />
                <Nota>{intl.formatMessage({ id: "onboarding.p2.microchip.apoio" })}</Nota>
                <Nota>{intl.formatMessage({ id: "onboarding.p2.semRgaNemTatuagem" })}</Nota>
                <Nota>{intl.formatMessage({ id: "onboarding.p2.naoValida" })}</Nota>
              </div>

              {identificar.isError && <Alerta>{intl.formatMessage({ id: chaveDoErro(identificar.error) })}</Alerta>}

              <Rodape>
                <Principal
                  pendente={identificar.isPending}
                  desabilitado={microchip.trim() === "" || animalId === undefined}
                  aoClicar={async () => {
                    await identificar.mutateAsync({
                      animalId: animalId!,
                      nome: criar.data?.name ?? nome.trim(),
                      especie,
                      microchip: microchip.trim(),
                    });
                    setPasso(3);
                  }}
                >
                  {intl.formatMessage({ id: "onboarding.continuar" })}
                </Principal>
                <Secundario aoClicar={() => setPasso(3)}>
                  {intl.formatMessage({ id: "onboarding.p2.naoTem" }, { nome: criar.data?.name ?? "" })}
                </Secundario>
              </Rodape>
            </>
          )}

          {passo === 3 && (
            <>
              <Titulo>{intl.formatMessage({ id: "onboarding.p4.titulo" }, { nome: criar.data?.name ?? "" })}</Titulo>
              <Apoio>{intl.formatMessage({ id: "onboarding.p4.apoio" })}</Apoio>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", maxWidth: "820px" }}>
                <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
                  <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "8px" }}>
                    {intl.formatMessage({ id: "onboarding.p4.pessoa" })}
                  </div>
                  <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", marginBottom: "16px" }}>
                    {intl.formatMessage({ id: "onboarding.p4.pessoa.apoio" })}
                  </div>

                  <input
                    type="email"
                    value={email}
                    onChange={(evento) => setEmail(evento.target.value)}
                    placeholder={intl.formatMessage({ id: "onboarding.p4.pessoa.campo" })}
                    aria-label={intl.formatMessage({ id: "onboarding.p4.pessoa.campo" })}
                    style={{ ...estiloDoCampo, marginBottom: "12px" }}
                  />

                  {convidar.isSuccess ? (
                    <div style={{ fontSize: "15px", color: "oklch(0.38 0.07 150)" }}>
                      {intl.formatMessage({ id: "onboarding.p4.convidado" }, { email: convidar.data?.email ?? "" })}

                      {/*
                       * O LINK, para quando o e-mail nao chega.
                       *
                       * Esta caixa dizia "Convite enviado para {email}" desde o primeiro dia, e nada
                       * era enviado: o `invite` gravava o convite e devolvia o token — "unico
                       * momento em que o token existe fora do cliente". Quem seguia o passo 4 ate o
                       * fim saia daqui achando que tinha convidado alguem. O `InviteNotifier` fez a
                       * frase virar verdade; o link fica porque spam e dedo trocado existem.
                       */}
                      {linkDoConvite !== undefined && (
                        <div
                          style={{ marginTop: "10px", padding: "12px 14px", background: "oklch(0.975 0.008 150)", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "6px", fontFamily: "'DM Mono', monospace", fontSize: "13px", lineHeight: 1.5, wordBreak: "break-all", color: "oklch(0.3 0.02 150)" }}
                        >
                          {linkDoConvite}
                        </div>
                      )}
                    </div>
                  ) : (
                    <Principal
                      pendente={convidar.isPending}
                      desabilitado={email.trim() === "" || animalId === undefined}
                      aoClicar={async () => {
                        await convidar.mutateAsync({ animalId: animalId!, email: email.trim() });
                      }}
                    >
                      {intl.formatMessage({ id: "onboarding.p4.convidar" })}
                    </Principal>
                  )}

                  {convidar.isError && <Alerta>{intl.formatMessage({ id: chaveDoErro(convidar.error) })}</Alerta>}
                </div>

                <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
                  <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "8px" }}>
                    {intl.formatMessage({ id: "onboarding.p4.organizacao" })}
                  </div>
                  <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", marginBottom: "16px" }}>
                    {intl.formatMessage({ id: "onboarding.p4.organizacao.apoio" })}
                  </div>

                  {/* O "escolher o que ela ve" do desenho e a Tela 09, que ja existe. */}
                  {animalId === undefined ? (
                    <Nota>{intl.formatMessage({ id: "onboarding.p4.organizacao.semAnimal" })}</Nota>
                  ) : (
                    <Link
                      to="/animais/$animalId/conceder-acesso"
                      params={{ animalId }}
                      style={{ display: "inline-flex", alignItems: "center", minHeight: "48px", padding: "13px 18px", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", textDecoration: "none" }}
                    >
                      {intl.formatMessage({ id: "onboarding.p4.escolherOQueVe" })}
                    </Link>
                  )}
                </div>
              </div>

              <Rodape>
                <Principal pendente={false} desabilitado={false} aoClicar={irParaOAnimal}>
                  {intl.formatMessage({ id: "onboarding.p4.ir" }, { nome: criar.data?.name ?? "" })}
                </Principal>
                <Secundario aoClicar={() => void irParaOAnimal()}>
                  {intl.formatMessage({ id: "onboarding.p4.sozinho" })}
                </Secundario>
              </Rodape>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

const estiloDoCampo = {
  fontFamily: "inherit",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
} as const;

/** Uma coluna do stepper: a barra, o ponto e o que aquele passo deixou registrado. */
function Trilho({
  ativo,
  feito,
  rotulo,
  detalhe,
}: {
  ativo: boolean;
  feito: boolean;
  rotulo: string;
  detalhe?: string;
}) {
  return (
    <div>
      <div style={{ height: "4px", borderRadius: "2px", background: ativo ? "oklch(0.46 0.085 150)" : "oklch(0.90 0.008 150)", marginBottom: "10px" }}></div>
      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
        <div
          aria-hidden
          style={
            ativo
              ? { width: "11px", height: "11px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none" }
              : { width: "11px", height: "11px", borderRadius: "999px", border: "2px solid oklch(0.80 0.012 150)", flex: "none" }
          }
        ></div>
        <span style={{ fontSize: "14px", fontWeight: ativo ? 500 : 400, color: ativo ? "oklch(0.25 0.02 150)" : "oklch(0.5 0.015 150)" }}>
          {rotulo}
        </span>
      </div>
      {/* O desenho troca o rotulo pelo que ficou registrado — "Code", "Microchip", "4 doses". */}
      {feito && detalhe !== undefined && detalhe !== "" && (
        <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "4px", paddingLeft: "19px" }}>
          {detalhe}
        </div>
      )}
    </div>
  );
}

function Titulo({ children }: { children: ReactNode }) {
  return (
    <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "32px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
      {children}
    </h1>
  );
}

function Apoio({ children }: { children: ReactNode }) {
  return (
    <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "54ch" }}>
      {children}
    </p>
  );
}

function Rotulo({ children, para }: { children: ReactNode; para?: string }) {
  return (
    <label htmlFor={para} style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
      {children}
    </label>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.5 }}>
      {children}
    </div>
  );
}

function Alerta({ children }: { children: ReactNode }) {
  return (
    <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", marginTop: "20px", marginBottom: 0 }}>
      {children}
    </p>
  );
}

function Rodape({ children }: { children: ReactNode }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "14px", marginTop: "36px", flexWrap: "wrap" }}>
      {children}
    </div>
  );
}

function Opcao({
  escolhida,
  aoEscolher,
  texto,
  indisponivel = false,
}: {
  escolhida: boolean;
  aoEscolher?: () => void;
  texto: string;
  indisponivel?: boolean;
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={escolhida}
      disabled={indisponivel}
      onClick={aoEscolher}
      style={{
        flex: 1,
        fontFamily: "inherit",
        fontSize: "16px",
        fontWeight: escolhida ? 500 : 400,
        border: `1px solid ${escolhida ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`,
        background: escolhida ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)",
        color: escolhida ? "oklch(0.38 0.07 150)" : "oklch(0.42 0.015 150)",
        borderRadius: "8px",
        padding: "13px",
        minHeight: "48px",
        cursor: indisponivel ? "not-allowed" : "pointer",
        opacity: indisponivel ? 0.55 : 1,
      }}
    >
      {texto}
    </button>
  );
}

function Principal({
  children,
  pendente,
  desabilitado,
  aoClicar,
}: {
  children: ReactNode;
  pendente: boolean;
  desabilitado: boolean;
  aoClicar: () => Promise<void> | void;
}) {
  const hover = useHover();

  return (
    <button
      type="button"
      disabled={pendente || desabilitado}
      {...hover.props}
      onClick={() => void aoClicar()}
      style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: pendente || desabilitado ? "oklch(0.62 0.05 150)" : hover.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: desabilitado ? "not-allowed" : "pointer" }}
    >
      {children}
    </button>
  );
}

function Secundario({ children, aoClicar }: { children: ReactNode; aoClicar: () => void }) {
  return (
    <button
      type="button"
      onClick={aoClicar}
      style={{ fontFamily: "inherit", fontSize: "15px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "none", cursor: "pointer", textDecoration: "underline", textUnderlineOffset: "3px", minHeight: "44px" }}
    >
      {children}
    </button>
  );
}
