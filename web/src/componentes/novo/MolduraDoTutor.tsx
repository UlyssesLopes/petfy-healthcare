import { Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type ReactNode } from "react";

import { useAnimais } from "../../dados/animais.ts";
import { useSair } from "../../dados/autenticacao.ts";
import { useMeuContexto } from "../../dados/contexto.ts";
import { MedalhaDoAnimal } from "./MedalhaDoAnimal.tsx";

/**
 * A moldura das telas do tutor na reconstrução: o menu lateral lista os animais dele, cada um
 * com a própria foto, e os poucos destinos que existem. No telefone, vira uma barra com o nome
 * da tela e um menu de tela cheia.
 */
export function MolduraDoTutor({
  titulo,
  animalAtivo,
  children,
}: {
  titulo: string;
  animalAtivo?: string;
  children: ReactNode;
}) {
  const [menuAberto, setMenuAberto] = useState(false);

  useEffect(() => {
    if (!menuAberto) return;
    const fechar = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") setMenuAberto(false);
    };
    window.addEventListener("keydown", fechar);
    return () => window.removeEventListener("keydown", fechar);
  }, [menuAberto]);

  return (
    <div className="pf pf-pagina pf-app">
      <a className="pf-pular" href="#pf-tela">
        Pular para o conteúdo
      </a>

      <header className="pf-app__barra">
        <Link to="/" className="pf-app__marca" translate="no">
          Petfy
        </Link>
        <span className="pf-app__barra-titulo">{titulo}</span>
        <button
          type="button"
          className="pf-app__abrir"
          aria-expanded={menuAberto}
          aria-controls="pf-menu"
          onClick={() => setMenuAberto(true)}
        >
          Menu
        </button>
      </header>

      <nav
        id="pf-menu"
        className="pf-app__menu"
        data-aberto={menuAberto ? "" : undefined}
        aria-label="Navegação"
      >
        <div className="pf-app__menu-topo">
          <Link to="/" className="pf-app__marca" translate="no">
            Petfy
          </Link>
          <button type="button" className="pf-app__fechar" onClick={() => setMenuAberto(false)}>
            Fechar
          </button>
        </div>
        <Menu animalAtivo={animalAtivo} aoNavegar={() => setMenuAberto(false)} />
      </nav>

      <main id="pf-tela" className="pf-app__tela">
        {children}
      </main>
    </div>
  );
}

function Menu({ animalAtivo, aoNavegar }: { animalAtivo?: string; aoNavegar: () => void }) {
  const animais = useAnimais();
  const contexto = useMeuContexto();
  const sair = useSair();
  const navegar = useNavigate();

  return (
    <>
      <p className="pf-app__grupo">Seus animais</p>
      <ul className="pf-app__lista">
        {(animais.data ?? []).map((animal) => (
          <li key={animal.animalId}>
            <Link
              to="/animais/$animalId"
              params={{ animalId: animal.animalId! }}
              className="pf-app__animal"
              aria-current={animal.animalId === animalAtivo ? "page" : undefined}
              onClick={aoNavegar}
            >
              <MedalhaDoAnimal animal={animal} className="pf-app__animal-medalha" />
              <span>{animal.name}</span>
            </Link>
          </li>
        ))}
        <li>
          <Link to="/animais/novo" className="pf-app__item pf-app__novo" onClick={aoNavegar}>
            <span className="pf-app__mais-um" aria-hidden="true">
              +
            </span>
            Cadastrar animal
          </Link>
        </li>
      </ul>

      <ul className="pf-app__lista">
        <li>
          <Link to="/" className="pf-app__item" onClick={aoNavegar}>
            Início
          </Link>
        </li>
        <li>
          <Link to="/avisos" className="pf-app__item" onClick={aoNavegar}>
            Avisos
          </Link>
        </li>
        <li>
          <Link to="/convites" className="pf-app__item" onClick={aoNavegar}>
            Convites
          </Link>
        </li>
      </ul>

      <div className="pf-app__pessoa">
        <Link to="/conta" className="pf-app__item" onClick={aoNavegar}>
          <span className="pf-app__sinal" aria-hidden="true">
            {iniciais(contexto.data?.personName)}
          </span>
          {contexto.data?.personName ?? "Sua conta"}
        </Link>
        <button
          type="button"
          className="pf-app__item pf-app__sair"
          onClick={() => {
            sair();
            void navegar({ to: "/entrar" });
          }}
        >
          Sair
        </button>
      </div>
    </>
  );
}

function iniciais(nome: string | undefined): string {
  if (!nome) return "";
  return nome
    .split(" ")
    .filter(Boolean)
    .map((parte) => parte[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}
