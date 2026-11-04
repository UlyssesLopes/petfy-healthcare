import type { Animal } from "../../dados/animais.ts";
import { useFotoDoAnimal } from "../../dados/carteira.ts";

/**
 * O animal na medalha da coleira: a foto dele, ou, sem foto, um desenho da espécie. Nunca uma
 * bolinha de iniciais: o protagonista da tela é o animal, e não uma sigla.
 */
export function MedalhaDoAnimal({
  animal,
  className,
  prioridade = false,
}: {
  animal: Pick<Animal, "name" | "species" | "photoAttachmentId">;
  className?: string;
  prioridade?: boolean;
}) {
  const { endereco } = useFotoDoAnimal(animal.photoAttachmentId ?? undefined);

  return (
    <div className={`pf-medalha ${className ?? ""}`}>
      <div className="pf-medalha__foto">
        {endereco ? (
          <img
            src={endereco}
            alt={`Foto de ${animal.name ?? "animal"}`}
            loading={prioridade ? "eager" : "lazy"}
            decoding="async"
          />
        ) : (
          <DesenhoDaEspecie especie={animal.species} />
        )}
      </div>
      <span className="pf-medalha__furo" aria-hidden="true" />
      <span className="pf-medalha__argola" aria-hidden="true" />
    </div>
  );
}

/** O rosto da espécie em formas chapadas, na paleta. Só aparece quando não há foto. */
function DesenhoDaEspecie({ especie }: { especie: Animal["species"] }) {
  const orelhas =
    especie === "FELINA" ? (
      <path d="M22 40 L30 14 L44 32 Z M78 40 L70 14 L56 32 Z" />
    ) : especie === "CANINA" ? (
      <path d="M18 30 Q14 58 26 64 L34 38 Z M82 30 Q86 58 74 64 L66 38 Z" />
    ) : especie === "LAGOMORFA" ? (
      <path d="M36 34 Q30 4 40 4 Q48 4 46 34 Z M64 34 Q70 4 60 4 Q52 4 54 34 Z" />
    ) : null;

  return (
    <svg className="pf-desenho" viewBox="0 0 100 100" role="img" aria-label="Sem foto">
      <rect width="100" height="100" className="pf-desenho__chao" />
      <g className="pf-desenho__rosto">
        {orelhas}
        <circle cx="50" cy="56" r="28" />
      </g>
      <g className="pf-desenho__olhos">
        <circle cx="40" cy="52" r="3.5" />
        <circle cx="60" cy="52" r="3.5" />
        <ellipse cx="50" cy="64" rx="4.5" ry="3.2" />
      </g>
    </svg>
  );
}
