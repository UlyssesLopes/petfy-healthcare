import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useRef, useState } from "react";

import { MedalhaDoAnimal } from "../componentes/novo/MedalhaDoAnimal.tsx";
import { MolduraDoTutor } from "../componentes/novo/MolduraDoTutor.tsx";
import type { EntradaDaLinha } from "../dados/animal.ts";
import { useLinhaDoTempo } from "../dados/animal.ts";
import type { Animal } from "../dados/animais.ts";
import type { Condicao, LinhaDaCarteira, MembroDaRede, Pesagem } from "../dados/carteira.ts";
import {
  useAnexos,
  useAnimal,
  useBaixarAnexo,
  useCarteira,
  useCondicoes,
  useEnviarAnexo,
  usePesagens,
  useRedeDeCuidado,
} from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";
import { anosDesde, dataLocalDe } from "../i18n/datas.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/app.css";
import "../estilos/novo/vida-do-animal.css";

/**
 * A vida do animal, na reconstrução (docs/reconstrucao-do-front.md): a tela que o tutor abre
 * todo dia. Responde primeiro "está tudo em dia com ele?", e o que precisa de ação fica ao lado
 * do botão que a resolve.
 */
export const Route = createFileRoute("/animais/$animalId")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: VidaDoAnimal,
});

// ------------------------------------------------------------------ formatos

const DATA = new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "short", year: "numeric" });
const MES_ANO = new Intl.DateTimeFormat("pt-BR", { month: "short", year: "numeric" });
const DIA_MES = new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "long" });
const KG = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const DIFERENCA = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
  signDisplay: "exceptZero",
});

/** Data sem hora ("2026-04-08") ou com hora; nunca deslocada pelo fuso. */
function data(iso: string | undefined, formato = DATA): string {
  if (!iso) return "—";
  return formato.format(iso.length <= 10 ? dataLocalDe(iso) : new Date(iso));
}

const ESPECIE: Record<string, string> = {
  CANINA: "Cão",
  FELINA: "Gato",
  AVE: "Ave",
  ROEDORA: "Roedor",
  LAGOMORFA: "Coelho",
  REPTIL: "Réptil",
  EQUINA: "Equino",
  OUTRA: "Outro",
};

const ESCOPO: Record<string, string> = {
  CARTEIRA: "carteira",
  CONDICOES: "alergias",
  PRONTUARIO: "consultas",
  OBSERVACOES: "recados",
  PESO: "peso",
  ANEXOS: "documentos",
  CONTATO: "contato",
};

const TIPO_DE_EVENTO: Record<string, string> = {
  VACINA: "Vacina",
  ANTIPARASITARIO: "Antiparasitário",
  ATENDIMENTO: "Consulta",
  OBSERVACAO: "Recado",
  PESAGEM: "Peso",
  CONDICAO: "Condição",
  ANEXO: "Documento",
  ORIENTACAO: "Orientação",
  CUMPRIMENTO: "Cuidado feito",
  UNIAO: "Cadastros unidos",
  OBITO: "Fim",
};

function idade(nascimento: string | undefined): string | undefined {
  if (!nascimento) return undefined;
  const anos = anosDesde(nascimento);
  if (anos >= 1) return anos === 1 ? "1 ano" : `${anos} anos`;
  return "menos de 1 ano";
}

// ------------------------------------------------------------------ a tela

function VidaDoAnimal() {
  const { animalId } = Route.useParams();
  const animal = useAnimal(animalId);
  const carteira = useCarteira(animalId);
  const nome = animal.data?.name ?? "";

  return (
    <MolduraDoTutor titulo={nome || "Animal"} animalAtivo={animalId}>
      {animal.isPending ? (
        <p className="pf-vida__estado">Carregando…</p>
      ) : animal.isError || !animal.data ? (
        <div className="pf-vida__estado">
          <h1 className="pf-titulo">Não deu para abrir este animal</h1>
          <p>Ele pode ter saído da sua lista, ou a conexão caiu. Tente de novo em instantes.</p>
          <Link to="/" className="pf-botao">
            Voltar ao início
          </Link>
        </div>
      ) : (
        <div className="pf-vida">
          <Cabecalho animal={animal.data} linhas={carteira.linhas} />
          <div className="pf-vida__grade">
            <Vacinas linhas={carteira.linhas} carregando={carteira.carregando} erro={!!carteira.erro} />
            <Condicoes animalId={animalId} />
            <Peso animalId={animalId} />
            <LinhaDaVida
              animalId={animalId}
              nome={nome}
              encerrada={!!animal.data.deceasedOn}
              fotoDePerfil={animal.data.photoAttachmentId ?? undefined}
            />
            <QuemCuida animalId={animalId} encerrada={!!animal.data.deceasedOn} />
          </div>
          <Documentos animalId={animalId} encerrada={!!animal.data.deceasedOn} />
        </div>
      )}
    </MolduraDoTutor>
  );
}

/** O fato do dia: a dose que mais pede atenção, ou que está tudo em dia. */
function fatoDoDia(linhas: LinhaDaCarteira[]): { texto: string; tom: "perigo" | "alerta" | "calmo" } {
  const vencidas = linhas
    .filter((l) => l.estado === "vencida")
    .sort((a, b) => (a.diasAteProximaDose ?? 0) - (b.diasAteProximaDose ?? 0));
  const vencida = vencidas[0];
  if (vencida) {
    const dias = Math.abs(vencida.diasAteProximaDose ?? 0);
    const outras = vencidas.length - 1;
    const mais = outras === 0 ? "" : outras === 1 ? ", e mais 1 vencida" : `, e mais ${outras} vencidas`;
    return { texto: `${vencida.nome} venceu há ${dias} ${dias === 1 ? "dia" : "dias"}${mais}.`, tom: "perigo" };
  }
  const vencendo = linhas
    .filter((l) => l.estado === "vencendo")
    .sort((a, b) => (a.diasAteProximaDose ?? 0) - (b.diasAteProximaDose ?? 0))[0];
  if (vencendo?.proximaDose) {
    const dias = vencendo.diasAteProximaDose ?? 0;
    return {
      texto: `${vencendo.nome} vence em ${dias} ${dias === 1 ? "dia" : "dias"}, em ${data(vencendo.proximaDose, DIA_MES)}.`,
      tom: "alerta",
    };
  }
  if (linhas.some((l) => l.estado === "emDia")) {
    return { texto: "As vacinas estão em dia.", tom: "calmo" };
  }
  return { texto: "Nenhuma vacina registrada ainda.", tom: "calmo" };
}

function Cabecalho({ animal, linhas }: { animal: Animal; linhas: LinhaDaCarteira[] }) {
  const encerrada = !!animal.deceasedOn;
  const fato = fatoDoDia(linhas);
  const sobre = [
    animal.breed || ESPECIE[animal.species ?? "OUTRA"],
    animal.gender?.toLowerCase(),
    idade(animal.bornDate),
    animal.castrated ? (animal.gender?.toLowerCase().startsWith("f") ? "castrada" : "castrado") : undefined,
    animal.weight !== undefined ? `${KG.format(animal.weight)} kg` : undefined,
  ].filter(Boolean);

  return (
    <section className="pf-vida__cabeca" aria-labelledby="pf-vida-nome">
      <MedalhaDoAnimal animal={animal} className="pf-vida__medalha" prioridade />
      <div className="pf-vida__quem">
        <h1 id="pf-vida-nome" className="pf-vida__nome">
          {animal.name}
        </h1>
        <p className="pf-vida__sobre">{sobre.join(" · ")}</p>
        {encerrada ? (
          <p className="pf-vida__fato" data-tom="calmo">
            A linha do tempo de {animal.name} foi encerrada em {data(animal.deceasedOn)}. Tudo o que foi
            registrado continua aqui.
          </p>
        ) : (
          <p className="pf-vida__fato" data-tom={fato.tom}>
            {fato.texto}
          </p>
        )}
      </div>
      {!encerrada && (
        <div className="pf-vida__acoes">
          <Link to="/animais/$animalId/registrar" params={{ animalId: animal.animalId! }} className="pf-botao">
            Registrar
          </Link>
          <details className="pf-mais">
            <summary>Mais</summary>
            <ul>
              <li>
                <Link to="/animais/$animalId/custo" params={{ animalId: animal.animalId! }}>
                  Quanto custa
                </Link>
              </li>
              <li>
                <Link to="/animais/$animalId/compra" params={{ animalId: animal.animalId! }}>
                  Lançar uma compra
                </Link>
              </li>
              <li>
                <Link to="/animais/$animalId/matricula" params={{ animalId: animal.animalId! }}>
                  Matrícula na creche
                </Link>
              </li>
              <li>
                <Link to="/animais/$animalId/fim" params={{ animalId: animal.animalId! }}>
                  Encerrar a linha do tempo
                </Link>
              </li>
            </ul>
          </details>
        </div>
      )}
    </section>
  );
}

// ------------------------------------------------------------------ vacinas

function estadoDaDose(linha: LinhaDaCarteira): { texto: string; tom: "perigo" | "alerta" | "calmo" } {
  const dias = linha.diasAteProximaDose ?? 0;
  switch (linha.estado) {
    case "vencida":
      return { texto: `venceu há ${Math.abs(dias)} ${Math.abs(dias) === 1 ? "dia" : "dias"}`, tom: "perigo" };
    case "vencendo":
      return { texto: `vence em ${dias} ${dias === 1 ? "dia" : "dias"}`, tom: "alerta" };
    case "emDia":
      return { texto: `em dia até ${data(linha.proximaDose, MES_ANO)}`, tom: "calmo" };
    case "semProximaDose":
      return { texto: "sem próxima dose marcada", tom: "calmo" };
    default:
      return { texto: "sem registro", tom: "calmo" };
  }
}

function Vacinas({ linhas, carregando, erro }: { linhas: LinhaDaCarteira[]; carregando: boolean; erro: boolean }) {
  const registradas = linhas.filter((l) => l.estado !== "semRegistro");
  const semRegistro = linhas.length - registradas.length;

  return (
    <section className="pf-painel pf-painel--vacinas" aria-labelledby="pf-vacinas">
      <h2 id="pf-vacinas" className="pf-painel__titulo">
        Vacinas
      </h2>
      {carregando ? (
        <p className="pf-painel__vazio">Carregando…</p>
      ) : erro ? (
        <p className="pf-painel__vazio">Não deu para ler a carteira agora.</p>
      ) : registradas.length === 0 ? (
        <p className="pf-painel__vazio">Nenhuma vacina registrada ainda.</p>
      ) : (
        <ul className="pf-doses">
          {registradas.map((linha) => {
            const estado = estadoDaDose(linha);
            return (
              <li key={linha.nome} className="pf-dose">
                <span className="pf-dose__nome">{linha.nome}</span>
                <span className="pf-estado" data-tom={estado.tom}>
                  {estado.texto}
                </span>
              </li>
            );
          })}
        </ul>
      )}
      {semRegistro > 0 && !carregando && (
        <p className="pf-painel__nota">
          {semRegistro === 1
            ? "1 vacina do calendário da espécie ainda sem registro."
            : `${semRegistro} vacinas do calendário da espécie ainda sem registro.`}
        </p>
      )}
    </section>
  );
}

// ------------------------------------------------------------------ alergias e condições

function Condicoes({ animalId }: { animalId: string }) {
  const condicoes = useCondicoes(animalId);
  const ativas = (condicoes.data ?? []).filter((c: Condicao) => c.ativa !== false);

  return (
    <section className="pf-painel" aria-labelledby="pf-condicoes">
      <h2 id="pf-condicoes" className="pf-painel__titulo">
        Alergias e condições
      </h2>
      {condicoes.isPending ? (
        <p className="pf-painel__vazio">Carregando…</p>
      ) : ativas.length === 0 ? (
        <p className="pf-painel__vazio">Nenhuma registrada.</p>
      ) : (
        <ul className="pf-condicoes">
          {ativas.map((condicao) => (
            <li key={condicao.animalHealthConditionId} className="pf-condicao">
              <span className="pf-condicao__tipo">
                {condicao.kind === "ALERGIA" ? "Alergia" : "Condição crônica"}
                {condicao.severity ? ` · ${condicao.severity.toLowerCase()}` : ""}
              </span>
              <span className="pf-condicao__nome">{condicao.description}</span>
              {condicao.since && <span className="pf-painel__nota">desde {data(condicao.since, MES_ANO)}</span>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

// ------------------------------------------------------------------ peso

function Peso({ animalId }: { animalId: string }) {
  const pesagens = usePesagens(animalId);
  const serie = [...(pesagens.data ?? [])]
    .filter((p: Pesagem) => p.weight !== undefined && p.measuredAt)
    .sort((a, b) => (a.measuredAt! < b.measuredAt! ? -1 : 1));
  const ultima = serie.at(-1);
  const anterior = serie.at(-2);

  return (
    <section className="pf-painel" aria-labelledby="pf-peso">
      <h2 id="pf-peso" className="pf-painel__titulo">
        Peso
      </h2>
      {pesagens.isPending ? (
        <p className="pf-painel__vazio">Carregando…</p>
      ) : !ultima ? (
        <p className="pf-painel__vazio">Nenhuma pesagem ainda.</p>
      ) : (
        <>
          <p className="pf-peso__valor">
            {KG.format(ultima.weight!)}
            <span className="pf-peso__unidade"> kg</span>
          </p>
          <p className="pf-painel__nota">
            {anterior
              ? `${DIFERENCA.format(ultima.weight! - anterior.weight!)} kg desde ${data(anterior.measuredAt, MES_ANO)}`
              : `pesada em ${data(ultima.measuredAt)}`}
          </p>
          {serie.length >= 2 && <GraficoDePeso serie={serie} />}
        </>
      )}
    </section>
  );
}

/** Uma linha em segmentos retos, no tempo de verdade, com a escala nos dois extremos. */
function GraficoDePeso({ serie }: { serie: Pesagem[] }) {
  const largura = 240;
  const altura = 64;
  const tempos = serie.map((p) => dataLocalDe(p.measuredAt!).getTime());
  const pesos = serie.map((p) => p.weight!);
  const inicio = tempos[0] ?? 0;
  const fim = tempos.at(-1) ?? inicio;
  const primeiro = pesos[0] ?? 0;
  const ultimo = pesos.at(-1) ?? primeiro;
  const minimo = Math.floor(Math.min(...pesos));
  const maximo = Math.ceil(Math.max(...pesos));
  const x = (t: number) => ((t - inicio) / Math.max(1, fim - inicio)) * largura;
  const y = (kg: number) => altura - ((kg - minimo) / Math.max(1, maximo - minimo)) * (altura - 8) - 4;
  const pontos = serie.map((p, i) => `${x(tempos[i] ?? inicio).toFixed(1)},${y(p.weight!).toFixed(1)}`).join(" ");

  return (
    <figure className="pf-grafico">
      <svg viewBox={`0 0 ${largura} ${altura}`} role="img" aria-label={`Peso de ${KG.format(primeiro)} a ${KG.format(ultimo)} kg`}>
        <line x1="0" x2={largura} y1={y(minimo)} y2={y(minimo)} className="pf-grafico__base" />
        <line x1="0" x2={largura} y1={y(maximo)} y2={y(maximo)} className="pf-grafico__guia" />
        <polyline points={pontos} className="pf-grafico__linha" />
      </svg>
      <figcaption className="pf-grafico__eixo">
        <span>{data(serie[0]?.measuredAt, MES_ANO)}</span>
        <span>{data(serie.at(-1)!.measuredAt, MES_ANO)}</span>
      </figcaption>
    </figure>
  );
}

// ------------------------------------------------------------------ a linha do tempo

function resumo(entrada: EntradaDaLinha): string {
  if (entrada.eventType === "PESAGEM" && entrada.summary) {
    const kg = Number(entrada.summary.replace(",", "."));
    return Number.isFinite(kg) ? `${KG.format(kg)} kg` : entrada.summary;
  }
  return entrada.summary ?? "";
}

function LinhaDaVida({
  animalId,
  nome,
  encerrada,
  fotoDePerfil,
}: {
  animalId: string;
  nome: string;
  encerrada: boolean;
  fotoDePerfil?: string;
}) {
  const linha = useLinhaDoTempo(animalId);
  const [tudo, setTudo] = useState(false);
  // A foto de perfil e o rosto do animal, e nao um acontecimento da vida dele.
  const entradas = (linha.data ?? []).filter(
    (e) => !(e.eventType === "ANEXO" && fotoDePerfil !== undefined && e.eventId === fotoDePerfil),
  );
  const visiveis = tudo ? entradas : entradas.slice(0, 6);

  return (
    <section className="pf-painel pf-painel--vida" aria-labelledby="pf-vida-linha">
      <div className="pf-painel__cabeca">
        <h2 id="pf-vida-linha" className="pf-painel__titulo">
          A vida de {nome}
        </h2>
        {entradas.length > 0 && (
          <span className="pf-painel__nota">
            {entradas.length === 1 ? "1 registro" : `${entradas.length} registros`}
          </span>
        )}
      </div>
      {linha.isPending ? (
        <p className="pf-painel__vazio">Carregando…</p>
      ) : linha.isError ? (
        <p className="pf-painel__vazio">Não deu para ler a linha do tempo agora.</p>
      ) : entradas.length === 0 ? (
        <p className="pf-painel__vazio">Nada registrado ainda. O primeiro registro começa a história.</p>
      ) : (
        <ol className="pf-linha">
          {visiveis.map((entrada) => (
            <li key={`${entrada.eventType}-${entrada.eventId}-${entrada.recordedAt}`} className="pf-registro">
              <div className="pf-registro__topo">
                <span className="pf-registro__tipo">{TIPO_DE_EVENTO[entrada.eventType ?? ""] ?? "Registro"}</span>
                <span className="pf-registro__quando">{data(entrada.occurredAt)}</span>
              </div>
              <p className="pf-registro__texto">{resumo(entrada)}</p>
              <p className="pf-registro__autor">
                {[entrada.recordedByName, entrada.organizationName, entrada.credentialLabel]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
              {(entrada.correctionCount ?? 0) > 0 && (
                <p className="pf-registro__correcao">
                  {entrada.correctionCount === 1
                    ? "Corrigido 1 vez. O original continua no histórico."
                    : `Corrigido ${entrada.correctionCount} vezes. O original continua no histórico.`}
                </p>
              )}
              {!encerrada && entrada.eventType === "ATENDIMENTO" && entrada.eventId && (
                <Link
                  to="/animais/$animalId/discordar/$registroId"
                  params={{ animalId, registroId: entrada.eventId }}
                  className="pf-registro__acao"
                >
                  Contestar esta consulta
                </Link>
              )}
            </li>
          ))}
        </ol>
      )}
      {entradas.length > 6 && (
        <button type="button" className="pf-painel__mais" onClick={() => setTudo((atual) => !atual)}>
          {tudo ? "Mostrar só os recentes" : `Ver os ${entradas.length} registros`}
        </button>
      )}
    </section>
  );
}

// ------------------------------------------------------------------ quem cuida

function papel(membro: MembroDaRede): string {
  if (membro.reach === "CUSTODIA") return membro.holder ? "Responde pelo animal" : "Cuida junto";
  const partes = (membro.scopes ?? []).map((s) => ESCOPO[s] ?? s.toLowerCase());
  const ate = membro.expiresAt ? `até ${data(membro.expiresAt, MES_ANO)}` : "sem prazo";
  return `Vê ${partes.join(", ")} · ${ate}`;
}

function QuemCuida({ animalId, encerrada }: { animalId: string; encerrada: boolean }) {
  const rede = useRedeDeCuidado(animalId);
  const membros = [...(rede.data ?? [])].sort((a, b) => Number(b.reach === "CUSTODIA") - Number(a.reach === "CUSTODIA"));

  return (
    <section className="pf-painel pf-painel--rede" aria-labelledby="pf-rede">
      <h2 id="pf-rede" className="pf-painel__titulo">
        Quem cuida
      </h2>
      {rede.isPending ? (
        <p className="pf-painel__vazio">Carregando…</p>
      ) : rede.isError ? (
        <p className="pf-painel__vazio">Não deu para ler quem cuida agora.</p>
      ) : (
        <ul className="pf-rede">
          {membros.map((membro) => (
            <li key={`${membro.kind}-${membro.personId ?? membro.organizationId}`} className="pf-membro">
              <span className="pf-membro__sinal" data-kind={membro.kind} aria-hidden="true">
                {(membro.name ?? "?")
                  .split(" ")
                  .map((p) => p[0])
                  .slice(0, 2)
                  .join("")}
              </span>
              <span className="pf-membro__texto">
                <span className="pf-membro__nome">{membro.name}</span>
                <span className="pf-painel__nota">{papel(membro)}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
      <div className="pf-painel__rodape">
        {!encerrada && (
          <Link to="/animais/$animalId/conceder-acesso" params={{ animalId }} className="pf-botao pf-botao--claro">
            Dar acesso
          </Link>
        )}
        <Link to="/animais/$animalId/quem-cuida" params={{ animalId }} className="pf-painel__link">
          Quem leu o quê
        </Link>
      </div>
    </section>
  );
}

// ------------------------------------------------------------------ documentos

function Documentos({ animalId, encerrada }: { animalId: string; encerrada: boolean }) {
  const anexos = useAnexos(animalId);
  const baixar = useBaixarAnexo();
  const enviar = useEnviarAnexo();
  const campo = useRef<HTMLInputElement>(null);
  const [falha, setFalha] = useState<string | undefined>(undefined);
  const lista = anexos.data ?? [];

  const abrir = async (attachmentId: string, nome: string) => {
    const conteudo = await baixar.mutateAsync(attachmentId);
    const url = URL.createObjectURL(conteudo);
    const ancora = document.createElement("a");
    ancora.href = url;
    ancora.download = nome;
    ancora.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="pf-documentos" aria-labelledby="pf-documentos">
      <div className="pf-painel__cabeca">
        <h2 id="pf-documentos" className="pf-painel__titulo">
          Documentos
        </h2>
        {!encerrada && (
          <>
            <button type="button" className="pf-painel__mais" onClick={() => campo.current?.click()}>
              {enviar.isPending ? "Enviando…" : "Anexar documento"}
            </button>
            <input
              ref={campo}
              type="file"
              accept="image/jpeg,image/png,image/webp,application/pdf"
              className="pf-oculto"
              aria-label="Escolher o documento"
              onChange={async (evento) => {
                const arquivo = evento.target.files?.[0];
                evento.target.value = "";
                if (!arquivo) return;
                setFalha(undefined);
                try {
                  await enviar.mutateAsync({ animalId, arquivo });
                } catch {
                  setFalha("O documento não subiu. Aceitamos JPEG, PNG, WEBP e PDF de até 10 MB.");
                }
              }}
            />
          </>
        )}
      </div>
      {falha && (
        <p className="pf-painel__vazio" role="alert">
          {falha}
        </p>
      )}
      {lista.length === 0 ? (
        <p className="pf-painel__vazio">Nenhum documento anexado.</p>
      ) : (
        <ul className="pf-anexos">
          {lista.map((anexo) => (
            <li key={anexo.attachmentId} className="pf-anexo">
              <span className="pf-anexo__nome">{anexo.description || anexo.originalFilename}</span>
              <span className="pf-painel__nota">
                {[anexo.uploadedByPersonName, data(anexo.creationDate)].filter(Boolean).join(" · ")}
              </span>
              <button
                type="button"
                className="pf-painel__link"
                onClick={() => void abrir(anexo.attachmentId!, anexo.originalFilename ?? "documento")}
              >
                Baixar
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
