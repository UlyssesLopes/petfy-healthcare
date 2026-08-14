import type { ReactNode } from "react";
import { useIntl } from "react-intl";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O cartao da "Tela 04 · na mao", de `design/IdentidadeVisual/Telas Petfy.dc.html`.
 *
 * <b>Duas telas mostram o MESMO cartao</b>, e o desenho e explicito nisso: quem acha um animal
 * na rua (Tela 34) e quem abre um link compartilhado (Tela 04) leem a mesma ficha. O que muda
 * e a origem — la o produto decide o conjunto, aqui o tutor escolheu os escopos —, e a origem
 * mora na rota. O que se ve mora aqui.
 */

export type ContatoDoCartao = {
  nome: string;
  telefone?: string | null;
};

export type VacinaDoCartao = {
  nome: string;
  status?: string;
  proximaDose?: string;
};

export type CartaoDoAnimalProps = {
  /** "Encontrado · so leitura" ou "Cartao do animal · so leitura". */
  marca: string;
  nome: string;
  /** "Cao · SRD · 8,4 kg", montada por quem chama com o que o DTO dele tem. */
  descricao: string;
  contatos: ContatoDoCartao[];
  alergias: string[];
  condicoes: string[];
  medicacao: string[];
  vacinas: VacinaDoCartao[];
  rodape: ReactNode;
};

/**
 * A ordem e do desenho, e nao e cosmetica.
 *
 * Os contatos vem primeiro porque a primeira coisa que quem esta com o animal precisa e ligar
 * para alguem. Alergia vem antes de condicao pela mesma logica: e o que nao se pode dar ao
 * animal nas proximas horas.
 */
export function CartaoDoAnimal({
  marca,
  nome,
  descricao,
  contatos,
  alergias,
  condicoes,
  medicacao,
  vacinas,
  rodape,
}: CartaoDoAnimalProps) {
  const intl = useIntl();

  return (
    <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
      <div style={{ background: "oklch(0.46 0.085 150)", padding: "20px 22px", color: "oklch(1 0 0)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "9px", marginBottom: "14px" }}>
          <span aria-hidden style={{ width: "20px", height: "20px", borderRadius: "999px", border: "2px solid oklch(1 0 0)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <span style={{ width: "6px", height: "6px", borderRadius: "999px", background: "oklch(1 0 0)" }}></span>
          </span>
          <span style={{ fontSize: "13px", letterSpacing: "0.04em", textTransform: "uppercase" }}>{marca}</span>
        </div>

        <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, letterSpacing: "-0.02em" }}>
          {nome}
        </div>
        <div style={{ fontSize: "15px", opacity: 0.9, marginTop: "4px" }}>{descricao}</div>
      </div>

      <div style={{ padding: "20px 22px", display: "flex", flexDirection: "column", gap: "18px" }}>
        {contatos.map((contato, indice) => (
          <Contato key={`${contato.nome}-${indice}`} contato={contato} destaque={indice === 0} />
        ))}

        <Bloco titulo={intl.formatMessage({ id: "cartao.alergias" })} linhas={alergias} forte />
        <Bloco titulo={intl.formatMessage({ id: "cartao.condicoes" })} linhas={condicoes} />
        <Bloco titulo={intl.formatMessage({ id: "cartao.medicacao" })} linhas={medicacao} />

        <Separador />
        <div>
          <Rotulo>{intl.formatMessage({ id: "cartao.vacinacao" })}</Rotulo>
          {vacinas.length === 0 ? (
            <Nada />
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "8px", fontSize: "16px" }}>
              {vacinas.map((vacina, indice) => (
                <Vacina key={`${vacina.nome}-${indice}`} vacina={vacina} />
              ))}
            </div>
          )}
        </div>

        <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "14px", fontSize: "13px", color: "oklch(0.5 0.015 150)", lineHeight: 1.55 }}>
          {rodape}
        </div>
      </div>
    </div>
  );
}

function Contato({ contato, destaque }: { contato: ContatoDoCartao; destaque: boolean }) {
  const intl = useIntl();

  /*
   * Sem telefone o contato nao vira botao morto: vira uma linha de texto.
   *
   * Um `tel:` sem numero e um botao que nao faz nada — e neste cartao um botao que nao faz nada
   * e o pior defeito possivel, porque quem toca nele esta contando com ele.
   */
  if (contato.telefone === undefined || contato.telefone === null || contato.telefone === "") {
    return (
      <div style={{ fontSize: "15px", color: "oklch(0.42 0.015 150)" }}>
        {intl.formatMessage({ id: "cartao.semTelefone" }, { quem: contato.nome })}
      </div>
    );
  }

  return (
    <a
      href={`tel:${contato.telefone}`}
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        textDecoration: "none",
        fontSize: "17px",
        fontWeight: 500,
        borderRadius: "8px",
        padding: "17px",
        minHeight: "58px",
        color: destaque ? "oklch(1 0 0)" : "oklch(0.25 0.02 150)",
        background: destaque ? "oklch(0.46 0.085 150)" : "transparent",
        border: destaque ? "none" : "1px solid oklch(0.82 0.012 150)",
      }}
    >
      {intl.formatMessage({ id: "cartao.ligar" }, { quem: contato.nome })}
    </a>
  );
}

function Vacina({ vacina }: { vacina: VacinaDoCartao }) {
  const intl = useIntl();
  const nome = vacina.nome;
  const vencida = vacina.status === "OVERDUE";

  const texto = (() => {
    if (vencida) {
      return intl.formatMessage({ id: "cartao.vacina.vencida" }, { nome });
    }
    if (vacina.status === "DUE_SOON" && vacina.proximaDose !== undefined) {
      return intl.formatMessage(
        { id: "cartao.vacina.chegando" },
        { nome, data: intl.formatDate(vacina.proximaDose, { day: "2-digit", month: "2-digit" }) },
      );
    }
    if (vacina.status === "NO_NEXT_DOSE") {
      return intl.formatMessage({ id: "cartao.vacina.semProxima" }, { nome });
    }
    return intl.formatMessage({ id: "cartao.vacina.emDia" }, { nome });
  })();

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
      {/*
       * O losango da vencida e o circulo da em dia, como em todo o produto — a forma carrega o
       * estado, e nao so a cor. Aqui isso vale duplamente: este cartao e lido na rua, no
       * celular, possivelmente no sol.
       */}
      <span
        aria-hidden
        style={{
          width: "12px",
          height: "12px",
          flex: "none",
          background: vencida ? "oklch(0.55 0.14 30)" : "oklch(0.46 0.085 150)",
          borderRadius: vencida ? 0 : "999px",
          transform: vencida ? "rotate(45deg)" : "none",
        }}
      ></span>
      {texto}
    </div>
  );
}

function Bloco({ titulo, linhas, forte = false }: { titulo: string; linhas: string[]; forte?: boolean }) {
  /* Bloco sem nada nao aparece: uma lista vazia de alergias diria "nao tem alergia", e o que o
     produto sabe e "ninguem registrou alergia" — que e outra coisa, e perigosa de confundir. */
  if (linhas.length === 0) {
    return null;
  }

  return (
    <>
      <Separador />
      <div>
        <Rotulo>{titulo}</Rotulo>
        <div style={{ fontSize: forte ? "18px" : "17px", fontWeight: forte ? 500 : 400, display: "flex", flexDirection: "column", gap: "4px" }}>
          {linhas.map((linha) => (
            <span key={linha}>{linha}</span>
          ))}
        </div>
      </div>
    </>
  );
}

function Rotulo({ children }: { children: string }) {
  return (
    <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "8px" }}>
      {children}
    </div>
  );
}

function Nada() {
  const intl = useIntl();
  return (
    <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
      {intl.formatMessage({ id: "cartao.nada" })}
    </div>
  );
}

function Separador() {
  return <div aria-hidden style={{ height: "1px", background: "oklch(0.92 0.006 150)" }}></div>;
}
