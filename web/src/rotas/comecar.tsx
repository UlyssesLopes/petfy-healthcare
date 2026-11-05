import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { Plaquinha } from "../componentes/novo/Plaquinha.tsx";
import { useSair } from "../dados/autenticacao.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/porta.css";
import "../estilos/novo/primeiro-animal.css";

/**
 * Depois da conta, antes do primeiro animal. Nenhum passo pergunta se a pessoa é tutora ou
 * profissional (PRODUTO.md 9.3): o animal vem primeiro, e o caminho de quem atende fica logo
 * abaixo. Fazer depois e sair estão sempre à mão.
 */
export const Route = createFileRoute("/comecar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Comecar,
});

function Comecar() {
  const intl = useIntl();
  const t = (id: string, valores?: Record<string, string>) => intl.formatMessage({ id }, valores);
  const navegar = useNavigate();
  const contexto = useMeuContexto();
  const sair = useSair();
  const [animal, setAnimal] = useState("");

  const primeiroNome = contexto.data?.personName?.split(" ")[0] ?? "";
  const nomeDoAnimal = animal.trim();

  const cadastrar = (evento: FormEvent) => {
    evento.preventDefault();
    void navegar({ to: "/animais/novo", search: nomeDoAnimal === "" ? {} : { nome: nomeDoAnimal } });
  };

  return (
    <div className="pf pf-pagina pf-porta pf-porta--passos">
      <header className="pf-porta__topo pf-passos__topo">
        <Link to="/" className="pf-porta__marca" translate="no">
          Petfy
        </Link>
        <button
          type="button"
          className="pf-texto-botao pf-passos__sair"
          onClick={() => {
            sair();
            void navegar({ to: "/entrar" });
          }}
        >
          {t("comecar.sair")}
        </button>
      </header>

      <aside className="pf-porta__painel" aria-label={t("comecar.medalha.rotulo")}>
        <Plaquinha
          frente={
            <>
              <span
                className="pf-plaquinha__animal"
                data-longo={nomeDoAnimal.length > 9 ? "" : undefined}
                data-vazio={nomeDoAnimal === "" ? "" : undefined}
                translate="no"
              >
                {nomeDoAnimal === "" ? t("criarConta.medalha.semAnimal") : nomeDoAnimal}
              </span>
              <span className="pf-plaquinha__marca" translate="no">
                Petfy
              </span>
            </>
          }
        />
        <p className="pf-porta__legenda">{t("comecar.medalha.legenda")}</p>
      </aside>

      <main className="pf-porta__folha">
        <div className="pf-porta__corpo">
          <h1 className="pf-titulo pf-porta__titulo">
            {primeiroNome === "" ? t("comecar.titulo.semNome") : t("comecar.titulo", { nome: primeiroNome })}
          </h1>
          <p className="pf-porta__apoio">{t("comecar.apoio")}</p>

          <form className="pf-form" onSubmit={cadastrar}>
            <div className="pf-campo">
              <label htmlFor="comecar-animal">{t("comecar.animal.campo")}</label>
              <input
                id="comecar-animal"
                type="text"
                value={animal}
                maxLength={24}
                autoComplete="off"
                onChange={(e) => setAnimal(e.target.value)}
                aria-describedby="comecar-animal-dica"
              />
              <p id="comecar-animal-dica" className="pf-campo__dica">
                {t("comecar.animal.texto")}
              </p>
            </div>
            <button type="submit" className="pf-botao pf-porta__acao">
              {nomeDoAnimal === "" ? t("comecar.animal.acao") : t("comecar.animal.acaoCom", { animal: nomeDoAnimal })}
            </button>
          </form>

          <section className="pf-atende" aria-labelledby="comecar-atende">
            <h2 id="comecar-atende" className="pf-atende__titulo">
              {t("comecar.profissional.titulo")}
            </h2>
            <p className="pf-atende__texto">{t("comecar.profissional.texto")}</p>
            <ul className="pf-atende__portas">
              <li>
                <Link to="/convites">{t("comecar.profissional.convite")}</Link>
              </li>
              <li>
                <Link to="/conta">{t("comecar.profissional.registro")}</Link>
              </li>
              <li>
                <Link to="/organizacoes/nova">{t("comecar.profissional.acao")}</Link>
              </li>
            </ul>
          </section>

          <p className="pf-porta__ja-tem">
            <Link to="/">{t("comecar.depois")}</Link>
          </p>
        </div>
      </main>
    </div>
  );
}
