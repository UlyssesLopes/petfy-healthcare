import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useRef, useState, type FormEvent } from "react";

import {
  ACESSO,
  CAPA,
  CARTAO,
  COMECO,
  FOTOS,
  MAOS,
  NADA_SE_APAGA,
  QUEM_CUIDA,
  RODAPE,
  type Foto,
} from "../conteudo/conheca.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/conheca.css";

/**
 * A página pública: a primeira tela da reconstrução do front (docs/reconstrucao-do-front.md).
 * A identidade nasce aqui: a medalha da coleira é a forma de tudo, em cobalto sobre areia.
 */
export const Route = createFileRoute("/conheca")({
  component: Conheca,
});

function Conheca() {
  const raiz = useRef<HTMLDivElement>(null);
  useEntradas(raiz);

  return (
    <div ref={raiz} className="pf pf-pagina">
      <a className="pf-pular" href="#pf-conteudo">
        Pular para o conteúdo
      </a>
      <Capa />
      <main id="pf-conteudo">
        <DeMaoEmMao />
        <NadaSeApaga />
        <QuemVe />
        <CartaoDaColeira />
        <QuemCuida />
        <Comeco />
      </main>
      <Rodape />
    </div>
  );
}

/** As entradas de cada seção: uma vez, ao entrar na tela, e nunca escondendo nada sem script. */
function useEntradas(raiz: React.RefObject<HTMLDivElement | null>) {
  useEffect(() => {
    const no = raiz.current;
    if (!no || !("IntersectionObserver" in window)) return;
    no.classList.add("js");
    const observador = new IntersectionObserver(
      (entradas) => {
        for (const entrada of entradas) {
          if (entrada.isIntersecting) {
            entrada.target.classList.add("is-in");
            observador.unobserve(entrada.target);
          }
        }
      },
      { rootMargin: "0px 0px -10% 0px", threshold: 0.1 },
    );
    no.querySelectorAll("[data-entra]").forEach((alvo) => observador.observe(alvo));
    return () => observador.disconnect();
  }, [raiz]);
}

/** Toda foto da página: com as dimensões dela, e preguiçosa a menos que seja a da capa. */
function Imagem({
  foto,
  alt = "",
  className,
  prioridade = false,
}: {
  foto: Foto;
  alt?: string;
  className?: string;
  prioridade?: boolean;
}) {
  return (
    <img
      src={foto.src}
      width={foto.largura}
      height={foto.altura}
      alt={alt}
      className={className}
      loading={prioridade ? "eager" : "lazy"}
      fetchPriority={prioridade ? "high" : "auto"}
      decoding="async"
    />
  );
}

/** A medalha: o disco furado da coleira. A foto, quando há, mora dentro dele. */
function Medalha({
  foto,
  alt,
  className,
  prioridade,
}: {
  foto?: Foto;
  alt?: string;
  className?: string;
  prioridade?: boolean;
}) {
  return (
    <div className={`pf-medalha ${className ?? ""}`}>
      {foto && (
        <div className="pf-medalha__foto">
          <Imagem foto={foto} alt={alt} prioridade={prioridade} />
        </div>
      )}
      <span className="pf-medalha__furo" aria-hidden="true" />
      <span className="pf-medalha__argola" aria-hidden="true" />
    </div>
  );
}

function Capa() {
  return (
    <header className="pf-capa">
      <div className="pf-topo">
        <Link to="/conheca" className="pf-marca" translate="no">
          Petfy
        </Link>
        <nav className="pf-topo__links" aria-label="Principal">
          <Link to="/encontrado">Achei um animal</Link>
          <Link to="/entrar">Entrar</Link>
        </nav>
      </div>
      <Medalha foto={FOTOS.pipoca} alt={CAPA.altFoto} className="pf-capa__medalha" prioridade />
      <div className="pf-capa__texto">
        <h1 className="pf-capa__titulo">
          {CAPA.titulo[0]}
          <br />
          {CAPA.titulo[1]}
        </h1>
        <p className="pf-capa__apoio">{CAPA.apoio}</p>
        <Link to="/criar-conta" className="pf-botao pf-capa__acao">
          {CAPA.acao}
        </Link>
      </div>
    </header>
  );
}

function DeMaoEmMao() {
  const total = MAOS.etapas.length;
  return (
    <section className="pf-maos" aria-labelledby="pf-maos-titulo">
      {MAOS.etapas.map((etapa, i) => (
        <div key={etapa.frase} className={`pf-mao pf-mao--${i + 1}`} data-entra>
          {i === 0 && (
            <h2 id="pf-maos-titulo" className="pf-mao__titulo">
              {MAOS.titulo}
            </h2>
          )}
          <p className="pf-mao__frase">{etapa.frase}</p>
          <article className="pf-ficha" data-ui aria-label={`O registro da ${MAOS.animal.nome}, etapa ${i + 1} de ${total}`}>
            <Imagem foto={FOTOS.pipoca} className="pf-ficha__retrato" />
            <p className="pf-ficha__nome">{MAOS.animal.nome}</p>
            <p className="pf-ficha__meta">{MAOS.animal.descricao}</p>
            <dl className="pf-ficha__quem">
              <div>
                <dt>Quem responde</dt>
                <dd>{etapa.responde}</dd>
              </div>
              {etapa.comAcesso && (
                <div>
                  <dt>Com acesso</dt>
                  <dd>{etapa.comAcesso}</dd>
                </div>
              )}
            </dl>
            <ol className="pf-ficha__registros">
              {etapa.registros.slice(0, 2).map((registro) => (
                <li key={registro.data + registro.texto}>
                  <span className="pf-ficha__texto">{registro.texto}</span>
                  <span className="pf-ficha__autor">
                    {registro.autor} · {registro.data}
                  </span>
                </li>
              ))}
            </ol>
            <p className="pf-ficha__total">
              {etapa.registros.length === 1 ? "1 registro" : `${etapa.registros.length} registros`} desde 14 mar 2024
            </p>
          </article>
        </div>
      ))}
    </section>
  );
}

function NadaSeApaga() {
  const [vendo, setVendo] = useState<"original" | "correcao">("correcao");
  const { original, correcao } = NADA_SE_APAGA;

  return (
    <section className="pf-nada" aria-labelledby="pf-nada-titulo">
      <div className="pf-nada__foto" data-entra>
        <Medalha foto={FOTOS.mia} alt={NADA_SE_APAGA.altFoto} />
      </div>
      <div className="pf-nada__coluna">
        <div data-entra>
          <h2 id="pf-nada-titulo" className="pf-titulo">
            {NADA_SE_APAGA.titulo}
          </h2>
          <p className="pf-nada__texto">{NADA_SE_APAGA.texto}</p>
        </div>

        <div className="pf-peso" data-ui aria-live="polite" data-entra>
          <p className="pf-peso__animal">Peso da {NADA_SE_APAGA.animal}</p>
          {vendo === "correcao" ? (
            <>
              <p className="pf-peso__valor">{correcao.valor}</p>
              <p className="pf-peso__quem">
                Corrigido por {correcao.autor} · {correcao.data}
              </p>
              <p className="pf-peso__motivo">“{correcao.motivo}”</p>
              <p className="pf-peso__antes">
                Antes: {original.valor}, registrado por {original.autor} em {original.data}
              </p>
            </>
          ) : (
            <>
              <p className="pf-peso__valor">{original.valor}</p>
              <p className="pf-peso__quem">
                Registrado por {original.autor} · {original.data}
              </p>
              <p className="pf-peso__antes">
                Corrigido em {correcao.data}. As duas versões ficam no histórico.
              </p>
            </>
          )}
        </div>

        <div className="pf-escolha" role="group" aria-label="Qual versão ver" data-entra>
          {(["original", "correcao"] as const).map((opcao) => (
            <button
              key={opcao}
              type="button"
              className="pf-escolha__opcao"
              aria-pressed={vendo === opcao}
              onClick={() => setVendo(opcao)}
            >
              {NADA_SE_APAGA.opcoes[opcao]}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}

function QuemVe() {
  const { pecas } = ACESSO;
  return (
    <section className="pf-acesso" aria-labelledby="pf-acesso-titulo">
      <div className="pf-placa pf-placa--titulo" data-entra>
        <h2 id="pf-acesso-titulo" className="pf-placa__titulo">
          {ACESSO.titulo}
        </h2>
      </div>

      <div className="pf-placa pf-placa--agora" data-entra>
        <div className="pf-ampliado" data-ui aria-hidden="true">
          <span className="pf-ampliado__botao">{pecas.agora.controle}</span>
        </div>
        <Legenda {...pecas.agora} />
      </div>

      <div className="pf-placa pf-placa--partes" data-entra>
        <div className="pf-ampliado pf-ampliado--coluna" data-ui aria-hidden="true">
          <span className="pf-ampliado__linha">
            <span className="pf-ampliado__chave" />
            {pecas.partes.controle}
          </span>
          <span className="pf-ampliado__linha pf-ampliado__linha--fora">
            <span className="pf-ampliado__chave pf-ampliado__chave--fora" />
            {pecas.partes.fora}
          </span>
        </div>
        <Legenda {...pecas.partes} />
      </div>

      <div className="pf-placa pf-placa--tempo" data-entra>
        <div className="pf-ampliado" data-ui aria-hidden="true">
          <span className="pf-ampliado__data">{pecas.tempo.controle}</span>
        </div>
        <Legenda {...pecas.tempo} />
      </div>

      <div className="pf-placa pf-placa--pessoa" data-entra>
        <div className="pf-ampliado" data-ui aria-hidden="true">
          {pecas.pessoa.iniciais.map((iniciais) => (
            <span key={iniciais} className="pf-ampliado__iniciais">
              {iniciais}
            </span>
          ))}
        </div>
        <Legenda {...pecas.pessoa} />
      </div>

      <div className="pf-placa pf-placa--rastro" data-entra>
        <div className="pf-ampliado" data-ui aria-hidden="true">
          <span className="pf-ampliado__rastro">
            {pecas.rastro.controle[0]}
            <br />
            {pecas.rastro.controle[1]}
          </span>
        </div>
        <Legenda {...pecas.rastro} />
      </div>
    </section>
  );
}

function Legenda({ nome, linha }: { nome: string; linha: string }) {
  return (
    <div className="pf-placa__legenda">
      <h3>{nome}</h3>
      <p>{linha}</p>
    </div>
  );
}

type ParteDoCartao = (typeof CARTAO.partes)[number]["id"];

/** Os controles em tamanho de controle; o cartão grande, como o celular de quem achou o mostra. */
function CartaoDaColeira() {
  const [ligadas, setLigadas] = useState<Record<ParteDoCartao, boolean>>({
    condicoes: true,
    vacinas: true,
    telefone: true,
  });
  return (
    <section className="pf-cartao" aria-labelledby="pf-cartao-titulo">
      <div className="pf-cartao__lado" data-entra>
        <h2 id="pf-cartao-titulo" className="pf-titulo">
          {CARTAO.titulo}
        </h2>
        <p className="pf-cartao__apoio">{CARTAO.texto}</p>

        <fieldset className="pf-partes">
          <legend className="pf-partes__rotulo">{CARTAO.controlesRotulo}</legend>
          {CARTAO.partes.map((parte) => (
            <button
              key={parte.id}
              type="button"
              role="switch"
              aria-checked={ligadas[parte.id]}
              className="pf-parte"
              onClick={() => setLigadas((atual) => ({ ...atual, [parte.id]: !atual[parte.id] }))}
            >
              <span className="pf-parte__nome">{parte.nome}</span>
              <span className="pf-parte__trilho" aria-hidden="true">
                <span className="pf-parte__bola" />
              </span>
            </button>
          ))}
        </fieldset>
      </div>

      {/* O palco reserva o tamanho do cartão cheio (a cópia invisível), e o cartão real cresce e
          encolhe dentro dele: muda o cartão, nunca a seção. */}
      <div className="pf-previa__palco" data-entra>
        <PreviaDoCartao ligadas={TODAS_AS_PARTES} reserva />
        <PreviaDoCartao ligadas={ligadas} />
      </div>
    </section>
  );
}

const TODAS_AS_PARTES: Record<ParteDoCartao, boolean> = {
  condicoes: true,
  vacinas: true,
  telefone: true,
};

function PreviaDoCartao({
  ligadas,
  reserva = false,
}: {
  ligadas: Record<ParteDoCartao, boolean>;
  reserva?: boolean;
}) {
  const { previa } = CARTAO;
  const nenhuma = !ligadas.condicoes && !ligadas.vacinas && !ligadas.telefone;

  return (
    <article
      className={reserva ? "pf-previa pf-previa--reserva" : "pf-previa"}
      data-ui
      aria-hidden={reserva || undefined}
      inert={reserva || undefined}
      aria-label={reserva ? undefined : "O cartão, como quem achou vê no celular"}
      aria-live={reserva ? undefined : "polite"}
    >
        <div className="pf-previa__cabeca">
          <Medalha foto={FOTOS.pipoca} className="pf-previa__medalha" />
          <div>
            <p className="pf-previa__nome">{previa.nome}</p>
            <p className="pf-previa__meta">{previa.descricao}</p>
          </div>
        </div>

        {ligadas.condicoes && (
          <div className="pf-previa__bloco">
            <p className="pf-previa__alerta">{previa.alerta}</p>
            <p>{previa.cuidado}</p>
          </div>
        )}

        {ligadas.vacinas && (
          <div className="pf-previa__bloco">
            <p className="pf-previa__subtitulo">{previa.vacinasTitulo}</p>
            <ul className="pf-previa__vacinas">
              {previa.vacinas.map((vacina) => (
                <li key={vacina.nome}>
                  <span>{vacina.nome}</span>
                  <span className="pf-previa__meta">{vacina.situacao}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {ligadas.telefone && (
          <p className="pf-previa__ligar">
            <span className="pf-botao">{previa.contato.texto}</span>
            <span className="pf-previa__meta">{previa.contato.tipo}</span>
          </p>
        )}

        {nenhuma && <p className="pf-previa__so-nome">{previa.soNome}</p>}

        <p className="pf-previa__validade">{previa.validade}</p>
    </article>
  );
}

function QuemCuida() {
  return (
    <section className="pf-cuida" aria-labelledby="pf-cuida-titulo">
      <div className="pf-cuida__cabeca" data-entra>
        <h2 id="pf-cuida-titulo" className="pf-titulo">
          {QUEM_CUIDA.titulo}
        </h2>
        <p className="pf-cuida__texto">{QUEM_CUIDA.texto}</p>
      </div>

      {/* A semana da Pipoca, contada pelo registro: cada entrada assinada por uma mão diferente,
          com o que ela alcança. Duas colunas desencontradas, como um mural. */}
      <div className="pf-semana">
        <p className="pf-semana__titulo">{QUEM_CUIDA.semana}</p>
        <ol className="pf-semana__entradas">
          {QUEM_CUIDA.entradas.map((entrada) => (
            <li key={entrada.autor} className="pf-entrada" data-ui data-entra>
              <div className="pf-entrada__autor">
                {"foto" in entrada && entrada.foto ? (
                  <Medalha foto={entrada.foto} className="pf-entrada__medalha" />
                ) : (
                  <span className="pf-entrada__iniciais" aria-hidden="true">
                    {"iniciais" in entrada ? entrada.iniciais : ""}
                  </span>
                )}
                <div>
                  <h3 className="pf-entrada__nome">{entrada.autor}</h3>
                  <p className="pf-entrada__papel">
                    {entrada.papel} · {entrada.alcance}
                  </p>
                </div>
              </div>
              <p className="pf-entrada__quando">{entrada.quando}</p>
              <p className="pf-entrada__tipo">{entrada.tipo}</p>
              <p className="pf-entrada__texto">{entrada.texto}</p>
              {"anexo" in entrada && entrada.anexo && (
                <Imagem foto={entrada.anexo.foto} alt={entrada.anexo.alt} className="pf-entrada__anexo" />
              )}
              {"assinatura" in entrada && entrada.assinatura && (
                <p className="pf-entrada__assinatura">{entrada.assinatura}</p>
              )}
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

function Comeco() {
  const [nome, setNome] = useState("");
  const navegar = useNavigate();
  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    void navegar({ to: "/criar-conta" });
  };

  return (
    <section className="pf-comeco" aria-labelledby="pf-comeco-titulo">
      <form className="pf-comeco__frase" onSubmit={enviar} data-entra>
        <h2 id="pf-comeco-titulo" className="pf-comeco__titulo">
          {COMECO.titulo}
        </h2>
        <span className="pf-comeco__linha">
          {COMECO.antes}{" "}
          <input
            className="pf-comeco__campo"
            name="nome-do-animal"
            value={nome}
            onChange={(evento) => setNome(evento.target.value.slice(0, 24))}
            placeholder={COMECO.dica}
            aria-label={COMECO.rotulo}
            autoComplete="off"
          />{" "}
          <button
            type="submit"
            className="pf-comeco__botao"
            data-pronto={nome.trim() ? "" : undefined}
          >
            <span className="pf-comeco__furo" aria-hidden="true" />
            {COMECO.acao}
            <span className="pf-comeco__seta" aria-hidden="true">
              →
            </span>
          </button>
        </span>
      </form>
      <p className="pf-comeco__nota">{COMECO.nota(nome.trim())}</p>
    </section>
  );
}

function Rodape() {
  return (
    <footer className="pf-rodape">
      <div className="pf-rodape__marca" aria-hidden="true">
        <Medalha foto={RODAPE.foto} alt={RODAPE.altFoto} className="pf-rodape__medalha" />
        <span className="pf-rodape__nome" translate="no">
          Petfy
        </span>
      </div>
      <nav className="pf-rodape__colunas" aria-label="Rodapé">
        {RODAPE.colunas.map((coluna) => (
          <div key={coluna.nome}>
            <p className="pf-rodape__grupo">{coluna.nome}</p>
            <ul>
              {coluna.links.map((link) => (
                <li key={link.para}>
                  <Link to={link.para}>{link.texto}</Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
      <details className="pf-rodape__creditos">
        <summary>{RODAPE.creditosRotulo}</summary>
        <p>
        {RODAPE.creditos.map((credito, i) => (
          <span key={credito.url}>
            <a href={credito.url} rel="noreferrer">
              {credito.titulo}
            </a>
            , {credito.autor}, {credito.licenca}
            {i < RODAPE.creditos.length - 1 ? "; " : "."}
          </span>
        ))}
        </p>
      </details>
      <div className="pf-rodape__fim">
        <span>{RODAPE.direitos}</span>
        <Link to="/criar-conta" className="pf-botao pf-botao--claro">
          Criar conta
        </Link>
      </div>
    </footer>
  );
}
