import { useIntl } from "react-intl";

import { iniciaisDe, useAcessosDeOrganizacao, useTutores } from "../dados/animal.ts";

/**
 * A rede de quem cuida (DESIGN.md 5.4).
 *
 * <b>As pessoas em volta do animal vem antes dos dados</b>, e essa posicao na tela e a
 * tese da secao 1 do PRODUTO.md virando layout: o registro e o fio que liga quem cuida.
 * Por isso ela fica acima da linha do tempo, e nao numa aba.
 *
 * <b>Conceder acesso e acao de primeira linha</b>, no meio da rede — concessao e o
 * mecanismo central do produto (3.5), e escondida em configuracoes ela deixa de ser
 * oferecida.
 */
export function Rede({ animalId, animalNome }: { animalId: string; animalNome: string }) {
  const intl = useIntl();
  const tutores = useTutores(animalId);
  const organizacoes = useAcessosDeOrganizacao(animalId);

  const pessoas = tutores.data ?? [];
  const ativas = (organizacoes.data ?? []).filter((acesso) => acesso.active !== false);

  if (pessoas.length === 0 && ativas.length === 0) {
    return null;
  }

  return (
    <section className="mt-6">
      <div className="flex flex-wrap items-baseline gap-3">
        <h2 className="text-rotulo uppercase text-tinta-secundaria">
          {intl.formatMessage({ id: "rede.titulo" }, { animal: animalNome })}
        </h2>
        <span className="text-rotulo ml-auto text-tinta-secundaria">
          {intl.formatMessage(
            { id: "rede.contagem" },
            { quantas: pessoas.length + ativas.length },
          )}
        </span>
      </div>

      <div className="mt-2.5 grid gap-2.5 sm:grid-cols-2">
        {pessoas.map((pessoa) => (
          <Cartao
            key={pessoa.personId}
            iniciais={iniciaisDe(pessoa.personName ?? "")}
            nome={pessoa.personName ?? ""}
            /*
             * O titular responde pelo animal; os demais alcancam por concessao. E a
             * distincao que o P2 separou — custodia nao e acesso —, e a tela usa as
             * palavras da secao 2: ao tutor, "responde pelo", nunca "custodia".
             */
            papel={
              pessoa.holder === true
                ? intl.formatMessage({ id: "rede.responde" }, { animal: animalNome })
                : intl.formatMessage({ id: "rede.acesso" })
            }
          />
        ))}

        {ativas.map((acesso) => (
          <Cartao
            key={acesso.grantId}
            iniciais={iniciaisDe(acesso.organizationName ?? "")}
            nome={acesso.organizationName ?? ""}
            papel={(acesso.scopes ?? []).join(" · ")}
            /* Organizacao e onde o ato clinico acontece: acento cheio, como manda a 5.5. */
            emAcento
          />
        ))}

        <Cartao
          iniciais="+"
          nome={intl.formatMessage({ id: "rede.conceder" })}
          papel={intl.formatMessage({ id: "rede.conceder.apoio" })}
          tracejado
        />
      </div>
    </section>
  );
}

function Cartao({
  iniciais,
  nome,
  papel,
  emAcento = false,
  tracejado = false,
}: {
  iniciais: string;
  nome: string;
  papel: string;
  emAcento?: boolean;
  tracejado?: boolean;
}) {
  return (
    <div
      className={`flex flex-col gap-0.5 rounded-bloco border border-linha px-3.5 py-3 ${
        tracejado ? "border-dashed" : ""
      }`}
    >
      {/*
        Iniciais precisam de nome (secao 6): a marca e decoracao para quem nao enxerga, e
        o nome de quem cuida esta logo abaixo, no texto.
      */}
      <span
        aria-hidden="true"
        className={`grid size-8 place-items-center rounded-ser text-[0.78125rem] font-medium ${
          tracejado
            ? "bg-fundo-musgo text-musgo"
            : emAcento
              ? "bg-musgo text-sobre-musgo"
              : "bg-fundo-musgo text-tinta-secundaria"
        }`}
      >
        {iniciais}
      </span>

      <b className="text-corpo-denso mt-2 font-medium text-tinta">{nome}</b>
      {papel !== "" ? <span className="text-rotulo normal-case text-tinta-secundaria">{papel}</span> : null}
    </div>
  );
}
