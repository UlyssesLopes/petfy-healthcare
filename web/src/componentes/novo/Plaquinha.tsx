import type { ReactNode } from "react";

/**
 * A plaquinha da coleira das portas: disco com borda, furo em cima e argola. Com duas faces,
 * gira na argola; a face escondida sai da árvore acessível. O que vai gravado é de quem usa.
 */
export function Plaquinha({
  frente,
  verso,
  face = "frente",
  className,
}: {
  frente: ReactNode;
  verso?: ReactNode;
  face?: "frente" | "verso";
  className?: string;
}) {
  return (
    <div className={`pf-plaquinha ${className ?? ""}`} data-face={face}>
      <span className="pf-plaquinha__argola" aria-hidden="true" />
      <div className="pf-plaquinha__disco">
        <div className="pf-plaquinha__face" aria-hidden={face !== "frente" || undefined}>
          <span className="pf-plaquinha__furo" aria-hidden="true" />
          <div className="pf-plaquinha__gravado">{frente}</div>
        </div>
        {verso !== undefined && (
          <div className="pf-plaquinha__face pf-plaquinha__face--verso" aria-hidden={face !== "verso" || undefined}>
            <span className="pf-plaquinha__furo" aria-hidden="true" />
            <div className="pf-plaquinha__gravado">{verso}</div>
          </div>
        )}
      </div>
    </div>
  );
}
