import { createFileRoute } from "@tanstack/react-router";

import { DocumentoLegal } from "../componentes/DocumentoLegal.tsx";
import { TERMOS } from "../conteudo/documentos.ts";

/** Publica: quem decide se cria conta precisa ler antes de ter uma. */
export const Route = createFileRoute("/termos")({
  component: () => <DocumentoLegal documento={TERMOS} />,
});
