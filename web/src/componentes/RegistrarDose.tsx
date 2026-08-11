import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { ehRespostaDeErro } from "../dados/RespostaDeErro.ts";
import { useDoseAnterior, useRegistrarDose } from "../dados/vacinas.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";
import { Conflito } from "./Estados.tsx";

/** O 149 do `error-codes.json`: mesma dose, mesmo animal, mesma data. */
const DOSE_JA_REGISTRADA = 149;

function ehDoseDuplicada(erro: unknown): boolean {
  return ehRespostaDeErro(erro) && erro.code === DOSE_JA_REGISTRADA;
}

/**
 * Registrar a dose que estava vencendo.
 *
 * <b>Fica dentro da propria pendencia</b>, e nao numa tela a parte: e o gesto que a
 * pendencia pede, e o que sai do feed entra na linha do tempo (DESIGN.md 5.3).
 *
 * <b>O nome e o catalogo vem da dose anterior</b>, e nao do que a pessoa digitar. Alem de
 * poupar digitacao, e o que garante que a dose nova caia na MESMA serie — sem isso, a
 * anterior continuaria cobrando, que e o defeito que a construcao desta tela revelou.
 */
export function RegistrarDose({
  animalId,
  vaccineId,
  aoFechar,
}: {
  animalId: string;
  vaccineId: string;
  aoFechar: () => void;
}) {
  const intl = useIntl();
  const anterior = useDoseAnterior(vaccineId);
  const registrar = useRegistrarDose();

  const hoje = new Date().toISOString().slice(0, 10);

  const [aplicadaEm, setAplicadaEm] = useState(hoje);
  const [proximaDose, setProximaDose] = useState("");
  const [erro, setErro] = useState<string | undefined>(undefined);

  if (anterior.isPending) {
    return (
      <p className="text-rotulo mt-3 text-tinta-secundaria">
        {intl.formatMessage({ id: "dose.carregando" })}
      </p>
    );
  }

  if (anterior.isError) {
    return (
      <p role="alert" className="text-rotulo mt-3 text-telha-texto">
        {intl.formatMessage({ id: chaveDoErro(anterior.error) })}
      </p>
    );
  }

  const vacina = anterior.data;

  function enviar(evento: FormEvent) {
    evento.preventDefault();

    if (aplicadaEm === "") {
      setErro("dose.erro.semData");
      return;
    }
    if (aplicadaEm > hoje) {
      setErro("dose.erro.futuro");
      return;
    }
    if (proximaDose !== "" && proximaDose <= aplicadaEm) {
      setErro("dose.erro.proximaAntes");
      return;
    }

    setErro(undefined);

    registrar.mutate(
      {
        animalId,
        vaccineName: vacina.vaccineName ?? "",
        vaccineCatalogId: vacina.vaccineCatalogId,
        applicationDate: aplicadaEm,
        nextDoseDate: proximaDose === "" ? undefined : proximaDose,
      },
      { onSuccess: aoFechar },
    );
  }

  return (
    <form onSubmit={enviar} noValidate className="mt-3 border-t border-linha pt-3">
      <h3 className="text-rotulo normal-case text-tinta-secundaria">
        {intl.formatMessage({ id: "dose.titulo" }, { vacina: vacina.vaccineName ?? "" })}
      </h3>

      <div className="mt-2.5 flex flex-wrap gap-4">
        <Data
          id={`aplicada-${vaccineId}`}
          rotulo={intl.formatMessage({ id: "dose.aplicadaEm" })}
          valor={aplicadaEm}
          aoMudar={setAplicadaEm}
          maximo={hoje}
        />

        <Data
          id={`proxima-${vaccineId}`}
          rotulo={intl.formatMessage({ id: "dose.proximaDose" })}
          apoio={intl.formatMessage({ id: "dose.proximaDose.apoio" })}
          valor={proximaDose}
          aoMudar={setProximaDose}
        />
      </div>

      {erro !== undefined ? (
        <p role="alert" className="text-rotulo mt-2 text-telha-texto">
          {intl.formatMessage({ id: erro })}
        </p>
      ) : null}

      {/*
        O conflito nao entra na linha de erro. A dose duplicada e o unico caso em que a
        gravacao falha e nao ha nada de errado: a clinica registrou primeiro, e o registro que
        importa existe. A saida e ver e fechar — nao "tentar de novo", que daria o mesmo
        resultado e deve dar.
      */}
      {registrar.isError && ehDoseDuplicada(registrar.error) ? (
        <div className="mt-2">
          <Conflito
            oQue={intl.formatMessage({ id: chaveDoErro(registrar.error) })}
            saida={
              <button
                type="button"
                onClick={aoFechar}
                className="text-corpo-denso inline-flex min-h-toque items-center text-tinta-secundaria hover:underline"
              >
                {intl.formatMessage({ id: "dose.conflito.fechar" })}
              </button>
            }
          />
        </div>
      ) : registrar.isError ? (
        <p role="alert" className="text-rotulo mt-2 text-telha-texto">
          {intl.formatMessage({ id: chaveDoErro(registrar.error) })}
        </p>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-3.5">
        <button
          type="submit"
          disabled={registrar.isPending}
          className="text-corpo-denso inline-flex min-h-toque items-center rounded-controle bg-musgo px-[18px] font-medium text-sobre-musgo disabled:opacity-70"
        >
          {intl.formatMessage({ id: registrar.isPending ? "dose.confirmando" : "dose.confirmar" })}
        </button>

        <button
          type="button"
          onClick={aoFechar}
          className="text-corpo-denso inline-flex min-h-toque items-center text-tinta-secundaria hover:underline"
        >
          {intl.formatMessage({ id: "dose.cancelar" })}
        </button>
      </div>
    </form>
  );
}

function Data({
  id,
  rotulo,
  apoio,
  valor,
  aoMudar,
  maximo,
}: {
  id: string;
  rotulo: string;
  apoio?: string;
  valor: string;
  aoMudar: (valor: string) => void;
  maximo?: string;
}) {
  return (
    <div className="flex flex-col gap-1">
      {/* Rotulo sempre visivel: placeholder some quando se digita, e e onde o erro nasce. */}
      <label htmlFor={id} className="text-rotulo font-medium text-tinta">
        {rotulo}
      </label>

      <input
        id={id}
        type="date"
        value={valor}
        max={maximo}
        onChange={(evento) => aoMudar(evento.target.value)}
        className="text-corpo min-h-toque rounded-bloco border border-linha bg-superficie px-3 text-tinta"
      />

      {apoio !== undefined ? (
        <span className="text-rotulo normal-case text-tinta-secundaria">{apoio}</span>
      ) : null}
    </div>
  );
}
