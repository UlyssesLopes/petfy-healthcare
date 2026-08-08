import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { useEntrar } from "../dados/autenticacao.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

export const Route = createFileRoute("/entrar")({
  beforeLoad: () => {
    if (lerSessao().autenticada) {
      throw redirect({ to: "/" });
    }
  },
  component: Entrar,
});

/** Erros por campo, e a chave e a mensagem — nunca a frase montada na hora. */
type ErrosDeCampo = { email?: string; senha?: string };

function validar(email: string, senha: string): ErrosDeCampo {
  const erros: ErrosDeCampo = {};

  if (email.trim() === "") {
    erros.email = "entrar.email.faltando";
  } else if (!email.includes("@")) {
    // Deliberadamente frouxo: a checagem existe para pegar o esquecimento obvio, e nao
    // para decidir se um endereco existe - quem decide isso e o servidor de e-mail.
    erros.email = "entrar.email.incompleto";
  }

  if (senha === "") {
    erros.senha = "entrar.senha.faltando";
  }

  return erros;
}

function Entrar() {
  const intl = useIntl();
  const navegar = useNavigate();
  const entrar = useEntrar();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erros, setErros] = useState<ErrosDeCampo>({});

  /*
   * Lido uma vez, na montagem: depois de entrar a sessao passa a existir, e o aviso
   * some sozinho. Se fosse lido a cada render, ele piscaria durante a navegacao.
   */
  const [encerramentoAnterior] = useState(() => lerSessao().ultimoEncerramento);

  function enviar(evento: FormEvent) {
    evento.preventDefault();

    const encontrados = validar(email, senha);
    setErros(encontrados);

    if (encontrados.email !== undefined || encontrados.senha !== undefined) {
      return;
    }

    entrar.mutate(
      { email: email.trim(), senha },
      { onSuccess: () => void navegar({ to: "/" }) },
    );
  }

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center p-6">
      <h1 className="text-nome-animal text-tinta">
        {intl.formatMessage({ id: "entrar.titulo" })}
      </h1>

      {encerramentoAnterior === "expirada" || encerramentoAnterior === "invalidada" ? (
        <p className="text-apoio mt-3 rounded-bloco bg-fundo-acento p-3 text-tinta">
          {intl.formatMessage({
            id:
              encerramentoAnterior === "expirada"
                ? "entrar.sessaoExpirada"
                : "entrar.sessaoInvalidada",
          })}
        </p>
      ) : null}

      <form onSubmit={enviar} noValidate className="mt-6 flex flex-col gap-5">
        <Campo
          id="email"
          rotulo={intl.formatMessage({ id: "entrar.email" })}
          tipo="email"
          valor={email}
          aoMudar={setEmail}
          erro={erros.email}
          autoComplete="email"
        />

        <Campo
          id="senha"
          rotulo={intl.formatMessage({ id: "entrar.senha" })}
          tipo="password"
          valor={senha}
          aoMudar={setSenha}
          erro={erros.senha}
          autoComplete="current-password"
        />

        {/*
          O erro do servidor vive fora dos campos: ele nao pertence a nenhum deles - o 401
          e sobre o par, e dizer "senha errada" contaria a quem tentou que o e-mail existe.
        */}
        {entrar.isError ? (
          <p
            role="alert"
            className="text-apoio rounded-bloco bg-fundo-urgencia p-3 text-urgencia"
          >
            {intl.formatMessage({ id: chaveDoErro(entrar.error) })}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={entrar.isPending}
          className="text-interface min-h-toque rounded-pilula bg-acento px-6 font-bold text-sobre-acento disabled:opacity-70"
        >
          {intl.formatMessage({ id: entrar.isPending ? "entrar.enviando" : "entrar.acao" })}
        </button>
      </form>
    </main>
  );
}

function Campo({
  id,
  rotulo,
  tipo,
  valor,
  aoMudar,
  erro,
  autoComplete,
}: {
  id: string;
  rotulo: string;
  tipo: "email" | "password";
  valor: string;
  aoMudar: (valor: string) => void;
  erro: string | undefined;
  autoComplete: string;
}) {
  const intl = useIntl();
  const idDoErro = `${id}-erro`;

  return (
    <div className="flex flex-col gap-1.5">
      {/* Rotulo sempre visivel: placeholder some quando se digita, e e onde o erro nasce. */}
      <label htmlFor={id} className="text-apoio font-semibold text-tinta">
        {rotulo}
      </label>

      <input
        id={id}
        name={id}
        type={tipo}
        value={valor}
        onChange={(evento) => aoMudar(evento.target.value)}
        autoComplete={autoComplete}
        aria-invalid={erro !== undefined}
        aria-describedby={erro !== undefined ? idDoErro : undefined}
        className="text-registro min-h-toque rounded-linha border border-contorno bg-superficie px-3 text-tinta"
      />

      {erro !== undefined ? (
        <p id={idDoErro} className="text-apoio text-urgencia">
          {intl.formatMessage({ id: erro })}
        </p>
      ) : null}
    </div>
  );
}
