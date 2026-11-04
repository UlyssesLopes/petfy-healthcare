import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { ehRespostaDeErro } from "../../dados/RespostaDeErro.ts";
import { useLancarCusto } from "../../dados/custo.ts";
import { useDoseAnterior, useRegistrarDose } from "../../dados/vacinas.ts";
import { chaveDoErro } from "../../i18n/erroDaApi.ts";

/** O 149 do `error-codes.json`: mesma dose, mesmo animal, mesma data. */
const DOSE_JA_REGISTRADA = 149;

/**
 * Registrar a dose que venceu, ali mesmo no cartão. Mesma regra do componente antigo: o valor é
 * opcional e entra depois da dose, ligado a ela; se o valor falhar, a dose fica.
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
  const t = (id: string, valores?: Record<string, string>) => intl.formatMessage({ id }, valores);
  const anterior = useDoseAnterior(vaccineId);
  const registrar = useRegistrarDose();
  const lancarCusto = useLancarCusto();

  const hoje = new Date().toISOString().slice(0, 10);
  const [aplicadaEm, setAplicadaEm] = useState(hoje);
  const [proximaDose, setProximaDose] = useState("");
  const [valor, setValor] = useState("");
  const [erro, setErro] = useState<string | undefined>(undefined);

  if (anterior.isPending) {
    return <p className="pf-dose-form__nota">{t("dose.carregando")}</p>;
  }
  if (anterior.isError) {
    return (
      <p role="alert" className="pf-dose-form__erro">
        {t(chaveDoErro(anterior.error))}
      </p>
    );
  }

  const vacina = anterior.data;
  const duplicada = registrar.isError && ehRespostaDeErro(registrar.error) && registrar.error.code === DOSE_JA_REGISTRADA;

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (aplicadaEm === "") return setErro("dose.erro.semData");
    if (aplicadaEm > hoje) return setErro("dose.erro.futuro");
    if (proximaDose !== "" && proximaDose <= aplicadaEm) return setErro("dose.erro.proximaAntes");
    setErro(undefined);

    registrar.mutate(
      {
        animalId,
        vaccineName: vacina.vaccineName ?? "",
        vaccineCatalogId: vacina.vaccineCatalogId,
        applicationDate: aplicadaEm,
        nextDoseDate: proximaDose === "" ? undefined : proximaDose,
      },
      {
        onSuccess: (doseGravada) => {
          const quanto = Number(valor.replace(/[^\d,.-]/g, "").replace(",", "."));
          if (valor.trim() !== "" && !Number.isNaN(quanto) && doseGravada?.vaccineId !== undefined) {
            lancarCusto.mutate({
              animalId,
              descricao: vacina.vaccineName ?? "",
              valor: quanto,
              tipo: "VACINA",
              doseId: doseGravada.vaccineId,
            });
          }
          aoFechar();
        },
      },
    );
  }

  return (
    <form className="pf-dose-form" onSubmit={enviar} noValidate>
      <p className="pf-dose-form__titulo">{t("dose.titulo", { vacina: vacina.vaccineName ?? "" })}</p>

      <div className="pf-dose-form__campos">
        <div className="pf-campo">
          <label htmlFor={`aplicada-${vaccineId}`}>{t("dose.aplicadaEm")}</label>
          <input
            id={`aplicada-${vaccineId}`}
            type="date"
            value={aplicadaEm}
            max={hoje}
            onChange={(e) => setAplicadaEm(e.target.value)}
          />
        </div>
        <div className="pf-campo">
          <label htmlFor={`proxima-${vaccineId}`}>{t("dose.proximaDose")}</label>
          <input
            id={`proxima-${vaccineId}`}
            type="date"
            value={proximaDose}
            onChange={(e) => setProximaDose(e.target.value)}
            aria-describedby={`proxima-${vaccineId}-apoio`}
          />
          <p id={`proxima-${vaccineId}-apoio`} className="pf-dose-form__nota">
            {t("dose.proximaDose.apoio")}
          </p>
        </div>
        <div className="pf-campo">
          <label htmlFor={`valor-${vaccineId}`}>{t("dose.valor")}</label>
          <input
            id={`valor-${vaccineId}`}
            type="text"
            inputMode="decimal"
            autoComplete="off"
            value={valor}
            onChange={(e) => setValor(e.target.value)}
            placeholder="90,00"
            aria-describedby={`valor-${vaccineId}-apoio`}
          />
          <p id={`valor-${vaccineId}-apoio`} className="pf-dose-form__nota">
            {t("dose.valor.apoio")}
          </p>
        </div>
      </div>

      <div aria-live="polite">
        {erro !== undefined && (
          <p role="alert" className="pf-dose-form__erro">
            {t(erro)}
          </p>
        )}
        {/* A duplicada não é falha: a clínica registrou primeiro. A saída é fechar. */}
        {registrar.isError && (
          <p role="alert" className={duplicada ? "pf-dose-form__aviso" : "pf-dose-form__erro"}>
            {t(chaveDoErro(registrar.error))}
          </p>
        )}
      </div>

      <div className="pf-dose-form__acoes">
        {!duplicada && (
          <button
            type="submit"
            className="pf-botao pf-botao--pequeno"
            disabled={registrar.isPending}
            aria-busy={registrar.isPending || undefined}
          >
            {t(registrar.isPending ? "dose.confirmando" : "dose.confirmar")}
          </button>
        )}
        <button type="button" className="pf-texto-botao" onClick={aoFechar}>
          {t(duplicada ? "dose.conflito.fechar" : "dose.cancelar")}
        </button>
      </div>
    </form>
  );
}
