import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Plaquinha } from "../componentes/novo/Plaquinha.tsx";
import { useCriarAnimal, useIdentificarAnimal, type Especie } from "../dados/animais.ts";
import { useConvidarCoTutor } from "../dados/animal.ts";
import { useDefinirFotoDoAnimal, useEnviarAnexo } from "../dados/carteira.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/porta.css";
import "../estilos/novo/primeiro-animal.css";

/**
 * O primeiro animal, em três passos. Só o primeiro é obrigatório; cada passo grava sozinho, e
 * sair no meio não desfaz nada. O animal nasce no fim do passo 1, a foto sobe depois dele e a
 * falha dela não segura o passo. A carteirinha não entra aqui: o OCR cria outro animal
 * (docs/reconstrucao-do-front.md).
 */
export const Route = createFileRoute("/animais/novo")({
  validateSearch: (search: Record<string, unknown>): { nome?: string } =>
    typeof search.nome === "string" && search.nome.trim() !== ""
      ? { nome: search.nome.trim().slice(0, 24) }
      : {},
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: PrimeiroAnimal,
});

type Passo = 1 | 2 | 3;

/** As espécies atrás do "Outro". Derivada do tipo gerado: espécie nova no backend acusa aqui. */
const AS_OUTRAS = ["AVE", "ROEDORA", "LAGOMORFA", "REPTIL", "EQUINA", "OUTRA"] as const satisfies
  readonly Exclude<Especie, "CANINA" | "FELINA">[];

function ehOutra(especie: Especie): boolean {
  return (AS_OUTRAS as readonly string[]).includes(especie);
}

function PrimeiroAnimal() {
  const intl = useIntl();
  const t = (id: string, valores?: Record<string, string>) => intl.formatMessage({ id }, valores);
  const navegar = useNavigate();
  const contexto = useMeuContexto();

  const criar = useCriarAnimal();
  const identificar = useIdentificarAnimal();
  const convidar = useConvidarCoTutor();
  const enviarFoto = useEnviarAnexo();
  const definirFoto = useDefinirFotoDoAnimal();

  const [passo, setPasso] = useState<Passo>(1);
  const [nome, setNome] = useState(Route.useSearch().nome ?? "");
  const [especie, setEspecie] = useState<Especie>("CANINA");
  const [nascimento, setNascimento] = useState("");
  const [microchip, setMicrochip] = useState("");
  const [email, setEmail] = useState("");
  const [foto, setFoto] = useState<File | undefined>(undefined);
  const [previa, setPrevia] = useState<string | undefined>(undefined);
  const [tocado, setTocado] = useState(false);
  const [face, setFace] = useState<"frente" | "verso">("frente");

  // O object URL é revogado quando a foto troca ou a tela sai.
  useEffect(() => {
    if (foto === undefined) {
      setPrevia(undefined);
      return;
    }
    const url = URL.createObjectURL(foto);
    setPrevia(url);
    return () => URL.revokeObjectURL(url);
  }, [foto]);

  const animalId = criar.data?.animalId;
  const nomeGravado = criar.data?.name ?? nome.trim();
  const nomeVazio = tocado && nome.trim() === "";

  const irPara = (proximo: Passo) => {
    setPasso(proximo);
    setFace(proximo === 2 ? "verso" : "frente");
    window.scrollTo({ top: 0 });
  };

  const irParaOAnimal = async () => {
    if (animalId !== undefined) {
      await navegar({ to: "/animais/$animalId", params: { animalId } });
    } else {
      await navegar({ to: "/" });
    }
  };

  const registrarAnimal = async (evento: FormEvent) => {
    evento.preventDefault();
    setTocado(true);
    if (nome.trim() === "") {
      document.getElementById("novo-nome")?.focus();
      return;
    }

    const animal = await criar.mutateAsync({
      nome: nome.trim(),
      especie,
      nascimento: nascimento === "" ? undefined : nascimento,
    });

    if (foto !== undefined && animal.animalId !== undefined) {
      try {
        const anexo = await enviarFoto.mutateAsync({
          animalId: animal.animalId,
          arquivo: foto,
          descricao: t("onboarding.p1.foto"),
        });
        if (anexo.attachmentId !== undefined) {
          await definirFoto.mutateAsync({ animalId: animal.animalId, attachmentId: anexo.attachmentId });
        }
      } catch {
        // O aviso da foto aparece no passo 2; o animal já está gravado.
      }
    }

    irPara(2);
  };

  const registrarMicrochip = async (evento: FormEvent) => {
    evento.preventDefault();
    if (microchip.trim() === "" || animalId === undefined) return;
    await identificar.mutateAsync({ animalId, nome: nomeGravado, especie, microchip: microchip.trim() });
    irPara(3);
  };

  const enviarConvite = async (evento: FormEvent) => {
    evento.preventDefault();
    if (email.trim() === "" || animalId === undefined) return;
    await convidar.mutateAsync({ animalId, email: email.trim() });
  };

  // O link é a única cópia do convite fora do e-mail, para quando o e-mail não chega.
  const linkDoConvite =
    convidar.data?.token === undefined || convidar.data.token === null
      ? undefined
      : `${window.location.origin}/convites/animal?token=${convidar.data.token}`;

  const PASSOS = [
    { n: 1, rotulo: t("onboarding.trilho.animal"), feito: criar.data?.name },
    { n: 2, rotulo: t("onboarding.trilho.identificacao"), feito: identificar.data?.microchipNumber ?? undefined },
    { n: 3, rotulo: t("onboarding.trilho.quemCuida"), feito: undefined },
  ] as const;

  return (
    <div className="pf pf-pagina pf-porta pf-porta--passos">
      <header className="pf-porta__topo pf-passos__topo">
        <span className="pf-porta__marca" translate="no">
          Petfy
        </span>
        <button type="button" className="pf-texto-botao pf-passos__sair" onClick={() => void irParaOAnimal()}>
          {t("onboarding.depois")}
        </button>
      </header>

      <aside className="pf-porta__painel" aria-label={t("onboarding.medalha.rotulo")}>
        <Plaquinha
          face={face}
          frente={
            <>
              {previa !== undefined && <img className="pf-plaquinha__foto" src={previa} alt="" />}
              <span
                className="pf-plaquinha__animal"
                data-longo={nomeGravado.length > 9 ? "" : undefined}
                data-vazio={nomeGravado === "" ? "" : undefined}
                translate="no"
              >
                {nomeGravado === "" ? t("criarConta.medalha.semAnimal") : nomeGravado}
              </span>
              <span className="pf-plaquinha__marca">{t(`animal.especie.${especie}`)}</span>
            </>
          }
          verso={
            <>
              <span className="pf-plaquinha__aviso">{t("onboarding.medalha.chip")}</span>
              <span
                className="pf-plaquinha__chip"
                data-vazio={microchip.trim() === "" ? "" : undefined}
                translate="no"
              >
                {microchip.trim() === "" ? t("onboarding.medalha.semChip") : microchip.trim()}
              </span>
              {contexto.data?.personName !== undefined && (
                <span className="pf-plaquinha__aviso" translate="no">
                  {t("onboarding.medalha.tutor", { nome: contexto.data.personName.split(" ")[0] ?? "" })}
                </span>
              )}
            </>
          }
        />
        <button
          type="button"
          className="pf-porta__virar"
          onClick={() => setFace((antes) => (antes === "frente" ? "verso" : "frente"))}
        >
          <span className="pf-porta__virar-seta" aria-hidden="true">
            ↻
          </span>
          {t(face === "frente" ? "criarConta.medalha.verVerso" : "criarConta.medalha.verFrente")}
        </button>
      </aside>

      <main className="pf-porta__folha">
        <div className="pf-porta__corpo">
          <ol className="pf-passos" aria-label={t("onboarding.passos")}>
            {PASSOS.map(({ n, rotulo, feito }) => (
              <li
                key={n}
                className="pf-passos__item"
                data-estado={passo === n ? "agora" : passo > n ? "feito" : "depois"}
                aria-current={passo === n ? "step" : undefined}
              >
                <span className="pf-passos__marca" aria-hidden="true" />
                <span className="pf-passos__rotulo">{rotulo}</span>
                {passo > n && feito !== undefined && feito !== "" && (
                  <span className="pf-passos__feito" translate="no">
                    {feito}
                  </span>
                )}
              </li>
            ))}
          </ol>

          {passo === 1 && (
            <>
              <h1 className="pf-titulo pf-porta__titulo">{t("onboarding.p1.titulo")}</h1>
              <p className="pf-porta__apoio">{t("onboarding.p1.apoio")}</p>

              <form className="pf-form" onSubmit={(e) => void registrarAnimal(e)} noValidate>
                <div className="pf-campo">
                  <label htmlFor="novo-nome">{t("onboarding.p1.nome")}</label>
                  <input
                    id="novo-nome"
                    type="text"
                    value={nome}
                    maxLength={60}
                    autoComplete="off"
                    onChange={(e) => setNome(e.target.value)}
                    onFocus={() => setFace("frente")}
                    aria-invalid={nomeVazio || undefined}
                    aria-describedby={nomeVazio ? "novo-nome-erro" : undefined}
                  />
                  {nomeVazio && (
                    <p id="novo-nome-erro" className="pf-campo__erro">
                      {t("onboarding.p1.semNome")}
                    </p>
                  )}
                </div>

                <fieldset className="pf-especie">
                  <legend>{t("onboarding.p1.especie")}</legend>
                  <div className="pf-especie__opcoes">
                    <Opcao valor="CANINA" escolhida={especie === "CANINA"} aoEscolher={() => setEspecie("CANINA")}>
                      {t("animal.especie.CANINA")}
                    </Opcao>
                    <Opcao valor="FELINA" escolhida={especie === "FELINA"} aoEscolher={() => setEspecie("FELINA")}>
                      {t("animal.especie.FELINA")}
                    </Opcao>
                    <Opcao valor="OUTRA" escolhida={ehOutra(especie)} aoEscolher={() => setEspecie(AS_OUTRAS[0])}>
                      {t("onboarding.p1.especie.outro")}
                    </Opcao>
                  </div>
                </fieldset>

                {ehOutra(especie) && (
                  <div className="pf-campo">
                    <label htmlFor="novo-especie-qual">{t("onboarding.p1.especie.qual")}</label>
                    <select
                      id="novo-especie-qual"
                      value={especie}
                      onChange={(e) => setEspecie(e.target.value as Especie)}
                      aria-describedby="novo-especie-nota"
                    >
                      {AS_OUTRAS.map((valor) => (
                        <option key={valor} value={valor}>
                          {t(`animal.especie.${valor}`)}
                        </option>
                      ))}
                    </select>
                    <p id="novo-especie-nota" className="pf-campo__dica">
                      {t("onboarding.p1.especie.semProtocolo")}
                    </p>
                  </div>
                )}

                <div className="pf-campo">
                  <label htmlFor="novo-nascimento">
                    {t("onboarding.p1.nascimento")} <span className="pf-campo__opcional">{t("onboarding.opcional")}</span>
                  </label>
                  <input
                    id="novo-nascimento"
                    type="month"
                    value={nascimento}
                    max={new Date().toISOString().slice(0, 7)}
                    onChange={(e) => setNascimento(e.target.value)}
                    aria-describedby="novo-nascimento-dica"
                  />
                  <p id="novo-nascimento-dica" className="pf-campo__dica">
                    {t("onboarding.p1.nascimento.apoio")}
                  </p>
                </div>

                <div className="pf-foto">
                  <span className="pf-foto__previa" aria-hidden="true">
                    {previa !== undefined && <img src={previa} alt="" />}
                  </span>
                  <span className="pf-foto__texto">
                    <span className="pf-foto__titulo">{t("onboarding.p1.foto")}</span>
                    <span className="pf-foto__dica">{foto === undefined ? t("onboarding.p1.foto.apoio") : foto.name}</span>
                  </span>
                  <label className="pf-foto__escolher">
                    {t(foto === undefined ? "onboarding.p1.foto.escolher" : "onboarding.p1.foto.trocar")}
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      className="pf-foto__arquivo"
                      onChange={(e) => {
                        setFoto(e.target.files?.[0]);
                        setFace("frente");
                      }}
                    />
                  </label>
                </div>

                <div aria-live="polite">
                  {criar.isError && (
                    <p role="alert" className="pf-porta__erro">
                      {t(chaveDoErro(criar.error))}
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  className="pf-botao pf-porta__acao"
                  disabled={criar.isPending}
                  aria-busy={criar.isPending || undefined}
                >
                  {t(criar.isPending ? "onboarding.registrando" : "onboarding.p1.acao")}
                </button>
                <p className="pf-porta__seguinte">{t("onboarding.p1.depois")}</p>
              </form>
            </>
          )}

          {passo === 2 && (
            <>
              <h1 className="pf-titulo pf-porta__titulo">{t("onboarding.p2.titulo", { nome: nomeGravado })}</h1>
              <p className="pf-porta__apoio">{t("onboarding.p2.apoio", { nome: nomeGravado })}</p>

              {enviarFoto.isError && (
                <p role="alert" className="pf-porta__nota">
                  {t("onboarding.p1.foto.falhou", { nome: nomeGravado })}
                </p>
              )}

              <form className="pf-form" onSubmit={(e) => void registrarMicrochip(e)} noValidate>
                <div className="pf-campo">
                  <label htmlFor="novo-microchip">{t("onboarding.p2.microchip")}</label>
                  <input
                    id="novo-microchip"
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    spellCheck={false}
                    maxLength={23}
                    value={microchip}
                    onChange={(e) => setMicrochip(e.target.value)}
                    onFocus={() => setFace("verso")}
                    aria-describedby="novo-microchip-dica"
                    className="pf-campo__numero"
                  />
                  <p id="novo-microchip-dica" className="pf-campo__dica">
                    {t("onboarding.p2.microchip.apoio")} {t("onboarding.p2.naoValida")}
                  </p>
                </div>

                <div aria-live="polite">
                  {identificar.isError && (
                    <p role="alert" className="pf-porta__erro">
                      {t(chaveDoErro(identificar.error))}
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  className="pf-botao pf-porta__acao"
                  disabled={identificar.isPending || microchip.trim() === ""}
                  aria-busy={identificar.isPending || undefined}
                >
                  {t(identificar.isPending ? "onboarding.registrando" : "onboarding.p2.acao")}
                </button>
                <button type="button" className="pf-texto-botao pf-passos__pular" onClick={() => irPara(3)}>
                  {t("onboarding.p2.naoTem", { nome: nomeGravado })}
                </button>
              </form>

              <p className="pf-porta__ja-tem pf-passos__nota">{t("onboarding.p2.semRgaNemTatuagem")}</p>
            </>
          )}

          {passo === 3 && (
            <>
              <h1 className="pf-titulo pf-porta__titulo">{t("onboarding.p4.titulo", { nome: nomeGravado })}</h1>
              <p className="pf-porta__apoio">{t("onboarding.p4.apoio")}</p>

              <div className="pf-quem">
                <Cartao titulo={t("onboarding.p4.pessoa")} texto={t("onboarding.p4.pessoa.apoio")}>
                  {convidar.isSuccess ? (
                    <div role="status" className="pf-quem__feito">
                      <p>{t("onboarding.p4.convidado", { email: convidar.data?.email ?? "" })}</p>
                      {linkDoConvite !== undefined && (
                        <p className="pf-quem__link" translate="no">
                          {linkDoConvite}
                        </p>
                      )}
                    </div>
                  ) : (
                    <form className="pf-quem__convite" onSubmit={(e) => void enviarConvite(e)} noValidate>
                      <div className="pf-campo">
                        <label htmlFor="novo-convite">{t("onboarding.p4.pessoa.campo")}</label>
                        <input
                          id="novo-convite"
                          type="email"
                          inputMode="email"
                          spellCheck={false}
                          autoComplete="off"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                        />
                      </div>
                      <button
                        type="submit"
                        className="pf-botao pf-botao--pequeno"
                        disabled={convidar.isPending || email.trim() === ""}
                        aria-busy={convidar.isPending || undefined}
                      >
                        {t("onboarding.p4.convidar")}
                      </button>
                      {convidar.isError && (
                        <p role="alert" className="pf-porta__erro">
                          {t(chaveDoErro(convidar.error))}
                        </p>
                      )}
                    </form>
                  )}
                </Cartao>

                <Cartao titulo={t("onboarding.p4.organizacao")} texto={t("onboarding.p4.organizacao.apoio")}>
                  {animalId !== undefined && (
                    <Link to="/animais/$animalId/conceder-acesso" params={{ animalId }} className="pf-quem__abrir">
                      {t("onboarding.p4.escolherOQueVe")}
                    </Link>
                  )}
                </Cartao>
              </div>

              <div className="pf-form">
                <button type="button" className="pf-botao pf-porta__acao" onClick={() => void irParaOAnimal()}>
                  {t("onboarding.p4.ir", { nome: nomeGravado })}
                </button>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

function Opcao({
  valor,
  escolhida,
  aoEscolher,
  children,
}: {
  valor: string;
  escolhida: boolean;
  aoEscolher: () => void;
  children: ReactNode;
}) {
  return (
    <label className="pf-especie__opcao" data-escolhida={escolhida ? "" : undefined}>
      <input type="radio" name="especie" value={valor} checked={escolhida} onChange={aoEscolher} />
      {children}
    </label>
  );
}

function Cartao({ titulo, texto, children }: { titulo: string; texto: string; children: ReactNode }) {
  return (
    <section className="pf-quem__cartao">
      <h2 className="pf-quem__titulo">{titulo}</h2>
      <p className="pf-quem__texto">{texto}</p>
      {children}
    </section>
  );
}
