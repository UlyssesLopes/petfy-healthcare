import { createFileRoute } from "@tanstack/react-router";

import { DocumentoLegal } from "../componentes/DocumentoLegal.tsx";
import { PRIVACIDADE } from "../conteudo/documentos.ts";

/** Publica: quem decide se cria conta precisa ler antes de ter uma. */
export const Route = createFileRoute("/privacidade")({
  component: () => <DocumentoLegal documento={PRIVACIDADE} />,
});
