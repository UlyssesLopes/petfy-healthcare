// Dados de exemplo honestos para o banco local: a Ana, a Pipoca, a Mia e o Nino, e quem cuida deles.
//
// Tudo entra pela API, para passar pelas mesmas regras do produto (autoria, linha do tempo,
// acesso). A unica excecao sao as capacidades das organizacoes, que nao tem rota: entram por
// INSERT no banco do container. Rode com o backend de pe e o banco recem-criado:
//
//   node docker-local/semente/semear.mjs
//
// Variaveis: API (padrao http://localhost:8081) e PG_CONTAINER (padrao petfy-pg-sessao).

import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";

const API = process.env.API ?? "http://localhost:8081";
const PG_CONTAINER = process.env.PG_CONTAINER ?? "petfy-pg-sessao";
const SENHA = "Petfy#2026";
const FOTOS = new URL("../../web/public/conheca/", import.meta.url);

async function chamar(sessao, metodo, caminho, corpo, organizacao) {
  const cabecalhos = {};
  if (sessao?.token) cabecalhos.Authorization = `Bearer ${sessao.token}`;
  if (organizacao) cabecalhos["X-Petfy-Organization"] = organizacao;

  let body;
  if (corpo instanceof FormData) {
    body = corpo;
  } else if (corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
    body = JSON.stringify(corpo);
  }

  const resposta = await fetch(API + caminho, { method: metodo, headers: cabecalhos, body });
  const texto = await resposta.text();
  if (!resposta.ok) {
    throw new Error(`${metodo} ${caminho} respondeu ${resposta.status}: ${texto.slice(0, 400)}`);
  }
  return texto ? JSON.parse(texto) : undefined;
}

async function pessoa({ nome, email, telefone, crmv, uf }) {
  await chamar(undefined, "POST", "/persons", {
    name: nome,
    email,
    password: SENHA,
    phone: telefone,
    acceptedTerms: true,
    ...(crmv ? { crmv, crmvUf: uf } : {}),
  });
  const login = await chamar(undefined, "POST", "/auth/login", { email, password: SENHA });
  const sessao = { token: login.token, personId: login.personId, nome };
  try {
    await chamar(sessao, "POST", "/consents/accept");
  } catch {
    // o aceite do cadastro pode ja ter registrado o consentimento
  }
  return sessao;
}

function capacidades(organizationId, lista) {
  const valores = lista.map((c) => `('${organizationId}', '${c}')`).join(", ");
  execFileSync("docker", [
    "exec", PG_CONTAINER, "psql", "-U", "petfy", "-d", "petfy", "-q", "-c",
    `INSERT INTO organization_capabilities (organization_id, capability) VALUES ${valores} ON CONFLICT DO NOTHING;`,
  ]);
}

async function foto(sessao, animalId, arquivo, tipo) {
  const forma = new FormData();
  forma.append("file", new Blob([readFileSync(new URL(arquivo, FOTOS))], { type: tipo }), arquivo);
  const anexo = await chamar(sessao, "POST", `/animals/${animalId}/attachments?description=Foto`, forma);
  await chamar(sessao, "PUT", `/animals/${animalId}/photo`, { attachmentId: anexo.attachmentId });
}

const meioDia = (data) => `${data}T12:00:00`;

// ------------------------------------------------------------------ as pessoas

const ana = await pessoa({ nome: "Ana Ribeiro", email: "ana@petfy.test", telefone: "11987654321" });
const luiza = await pessoa({
  nome: "Luíza Prado", email: "luiza@petfy.test", telefone: "11976543210", crmv: "41207", uf: "SP",
});
const joana = await pessoa({ nome: "Joana Lima", email: "joana@petfy.test", telefone: "11965432109" });
const rafael = await pessoa({ nome: "Rafael Souza", email: "rafael@petfy.test", telefone: "11954321098" });

// ------------------------------------------------------------------ as organizacoes

const clinica = await chamar(luiza, "POST", "/organizations", {
  name: "Clínica Bom Pasto", city: "São Paulo", state: "SP", phone: "1133334444",
  ownerVetName: "Luíza Prado",
});
const creche = await chamar(joana, "POST", "/organizations", {
  name: "Creche Quintal", city: "São Paulo", state: "SP", phone: "1133335555",
});
const petshop = await chamar(rafael, "POST", "/organizations", {
  name: "Petshop Vila Nova", city: "São Paulo", state: "SP", phone: "1133336666",
});

capacidades(clinica.organizationId, ["REGISTRAR_ATO_CLINICO", "REGISTRAR_OBSERVACAO", "COMUNICAR_COM_TUTOR"]);
capacidades(creche.organizationId, ["REGISTRAR_OBSERVACAO", "COMUNICAR_COM_TUTOR", "GERIR_TURMA_E_VAGA"]);
capacidades(petshop.organizationId, ["REGISTRAR_OBSERVACAO"]);

// ------------------------------------------------------------------ os animais da Ana

const pipoca = await chamar(ana, "POST", "/animals", {
  name: "Pipoca", species: "CANINA", breed: "SRD", gender: "Fêmea",
  bornDate: "2022-03-01", castrated: true, castratedAt: "2024-04-20", weight: 14.0,
});
const mia = await chamar(ana, "POST", "/animals", {
  name: "Mia", species: "FELINA", breed: "SRD", gender: "Fêmea", bornDate: "2021-06-15",
  castrated: true, castratedAt: "2022-01-10", weight: 4.2,
});
const nino = await chamar(ana, "POST", "/animals", {
  name: "Nino", species: "FELINA", breed: "British Shorthair", gender: "Macho",
  bornDate: "2018-09-02", castrated: true, castratedAt: "2019-05-14", weight: 5.6,
});

await foto(ana, pipoca.animalId, "dog-in-yedikule-animal-shelter.jpg", "image/jpeg");
await foto(ana, mia.animalId, "tabby-cat-with-blue-eyes-3336579.jpg", "image/jpeg");
await foto(ana, nino.animalId, "british-shorthair-img-9095.jpg", "image/jpeg");

// ------------------------------------------------------------------ quem alcanca cada um

const ate = (data) => `${data}T23:59:00`;
const acesso = (animal, organizacao, scopes, expira) =>
  chamar(ana, "POST", `/animals/${animal.animalId}/organization-access`, {
    organizationId: organizacao.organizationId, scopes, expiresAt: ate(expira),
  });

for (const animal of [pipoca, mia, nino]) {
  await acesso(animal, clinica, ["CARTEIRA", "CONDICOES", "PRONTUARIO", "PESO", "ANEXOS"], "2027-12-31");
}
await acesso(pipoca, creche, ["CARTEIRA", "CONDICOES", "OBSERVACOES"], "2027-02-28");
await acesso(pipoca, petshop, ["CONDICOES", "OBSERVACOES"], "2026-12-31");

// ------------------------------------------------------------------ o que a Ana registra

const condicao = (animal, corpo) => chamar(ana, "POST", `/animals/${animal.animalId}/conditions`, corpo);
await condicao(pipoca, {
  kind: "ALERGIA", description: "Proteína de frango", severity: "MODERADA", since: "2024-09-10",
  notes: "Coceira e vermelhidão na barriga. Ração hipoalergênica, sem petisco de frango.",
});
await condicao(nino, {
  kind: "CONDICAO_CRONICA", description: "Doença renal crônica, estágio inicial",
  since: "2025-08-20", notes: "Ração renal e água sempre fresca.",
});

const peso = (animal, measuredAt, weight, note) =>
  chamar(ana, "POST", `/animals/${animal.animalId}/weights`, { measuredAt, weight, note });
for (const [data, kg] of [["2024-04-20", 12.1], ["2024-09-10", 13.0], ["2025-04-09", 13.8], ["2026-04-08", 14.2], ["2026-10-30", 14.0]]) {
  await peso(pipoca, data, kg);
}
for (const [data, kg] of [["2025-02-15", 4.4], ["2026-02-15", 4.2]]) await peso(mia, data, kg);
for (const [data, kg] of [["2025-08-20", 6.1], ["2026-02-10", 5.8], ["2026-09-25", 5.6]]) await peso(nino, data, kg);

// ------------------------------------------------------------------ o que a clinica assina

const vacina = (animal, vaccineName, applicationDate, nextDoseDate) =>
  chamar(luiza, "POST", `/professional/animals/${animal.animalId}/vaccines`, {
    animalId: animal.animalId, vaccineName, applicationDate, nextDoseDate,
    organizationId: clinica.organizationId,
  }, clinica.organizationId);

await vacina(pipoca, "V10", "2026-04-08", "2027-04-08");
await vacina(pipoca, "Antirrábica", "2025-11-20", "2026-11-20");
await vacina(mia, "V4 felina", "2026-02-15", "2027-02-15");
await vacina(mia, "Antirrábica", "2026-02-15", "2027-02-15");
await vacina(nino, "V4 felina", "2025-08-20", "2026-08-20");
await vacina(nino, "Antirrábica", "2025-06-01", "2026-06-01");

const consulta = (animal, eventDate, description, diagnosis) =>
  chamar(luiza, "POST", `/professional/animals/${animal.animalId}/health-records`, {
    animalId: animal.animalId, category: "CONSULTA", eventDate, description, diagnosis,
    organizationId: clinica.organizationId,
  }, clinica.organizationId);

await consulta(pipoca, "2026-04-08", "Consulta de rotina e reforço da V10.", "Saudável. Manter a dieta sem frango.");
await consulta(nino, "2026-09-25", "Retorno renal: exame de sangue e pressão.", "Doença renal estável. Manter a ração renal.");

// ------------------------------------------------------------------ os recados de quem cuida

const recado = (sessao, organizacao, animal, observedAt, description) =>
  chamar(sessao, "POST", `/animals/${animal.animalId}/observations`, { description, observedAt },
    organizacao.organizationId);

await recado(joana, creche, pipoca, meioDia("2026-10-28"),
  "Brincou a tarde toda com a turma do quintal e comeu tudo.");
await recado(rafael, petshop, pipoca, meioDia("2026-10-31"),
  "Banho com xampu neutro. Por causa da alergia a frango, não demos o petisco da casa.");
await recado(joana, creche, pipoca, meioDia("2026-11-03"),
  "Chegou um pouco agitada, saiu tranquila. Dormiu depois do almoço.");

console.log(`Pronto. Entre com ana@petfy.test, luiza@petfy.test, joana@petfy.test ou rafael@petfy.test, senha ${SENHA}.`);
console.log(`Pipoca: /animais/${pipoca.animalId}`);
