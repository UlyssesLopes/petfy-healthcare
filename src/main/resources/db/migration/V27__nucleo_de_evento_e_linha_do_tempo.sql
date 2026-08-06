-- P4 da Fase 6: todo evento passa a saber quem o registrou, e nasce a linha do tempo.
--
-- Nenhum evento sabia disso ate aqui. So o anexo tinha autor, e vacina e atendimento
-- tinham a organizacao - um contexto embrionario sem pessoa dentro. E o que separa um
-- registro que um veterinario aceita de um caderno digital.

-- ---------------------------------------------------------------- autoria
ALTER TABLE vaccines                  ADD COLUMN recorded_by_person_id uuid;
ALTER TABLE antiparasitics            ADD COLUMN recorded_by_person_id uuid;
ALTER TABLE health_records            ADD COLUMN recorded_by_person_id uuid;
ALTER TABLE animal_weight_history     ADD COLUMN recorded_by_person_id uuid;
ALTER TABLE animal_health_conditions  ADD COLUMN recorded_by_person_id uuid;

-- O anexo ja tinha autor com outro nome. Renomear em vez de criar coluna nova evita
-- as duas respostas para "quem registrou" que a V15 evitou em pets.owner_id.
ALTER TABLE attachments RENAME COLUMN uploaded_by_person_id TO recorded_by_person_id;
ALTER TABLE attachments RENAME CONSTRAINT fk_attachments_uploaded_by TO fk_attachments_recorded_by;

-- Contexto onde faltava. Vacina e atendimento ja tinham.
ALTER TABLE antiparasitics            ADD COLUMN organization_id uuid;
ALTER TABLE animal_weight_history     ADD COLUMN organization_id uuid;
ALTER TABLE animal_health_conditions  ADD COLUMN organization_id uuid;
ALTER TABLE attachments               ADD COLUMN organization_id uuid;

ALTER TABLE vaccines ADD CONSTRAINT fk_vaccines_recorded_by
    FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id);
ALTER TABLE antiparasitics ADD CONSTRAINT fk_antiparasitics_recorded_by
    FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id);
ALTER TABLE health_records ADD CONSTRAINT fk_health_records_recorded_by
    FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id);
ALTER TABLE animal_weight_history ADD CONSTRAINT fk_animal_weight_history_recorded_by
    FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id);
ALTER TABLE animal_health_conditions ADD CONSTRAINT fk_animal_health_conditions_recorded_by
    FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id);

ALTER TABLE antiparasitics ADD CONSTRAINT fk_antiparasitics_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id);
ALTER TABLE animal_weight_history ADD CONSTRAINT fk_animal_weight_history_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id);
ALTER TABLE animal_health_conditions ADD CONSTRAINT fk_animal_health_conditions_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id);
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id);

-- SEM BACKFILL DE AUTOR, e isso e deliberado.
--
-- Nao havia coluna: ninguem foi registrado como autor de nada. Preencher com o
-- titular atual afirmaria um fato que nao aconteceu - e pior, um fato de
-- responsabilidade, porque autoria de ato clinico e o que 5.7 promete que nunca e
-- editavel. E o mesmo raciocinio da V16, que se recusou a inventar consentimento.
--
-- Na pratica a linha do tempo mostra "registrado por: nao informado" nos eventos
-- anteriores a esta migration, o que e honesto sobre o que o produto sabe.

-- ---------------------------------------------------------------- a linha do tempo
--
-- VIEW, e nao tabela-indice. A alternativa seria uma tabela alimentada por quem grava
-- cada evento, e ela PODE DIVERGIR da fonte: uma escrita nova que esquecesse de
-- inserir o indice deixaria o evento fora da linha do tempo em silencio. Divergencia
-- silenciosa foi a familia dos seis bugs da Fase 4 - e a unica das duas opcoes que
-- nao tem como divergir e esta, porque nao existe segunda escrita.
--
-- Se o volume um dia cobrar, vira materializada sem mudar o contrato.
--
-- occurred_at vem da coluna de fato de cada tabela, e nao de uma coluna nova ao lado
-- dela: duas respostas para "quando aconteceu" fariam a linha do tempo mentir sobre a
-- ordem. is_health_data e derivado do TIPO, e nao guardado por linha - senao duas
-- vacinas poderiam discordar sobre serem dado de saude.
CREATE VIEW animal_timeline AS
SELECT v.vaccine_id                      AS event_id,
       'VACINA'                          AS event_type,
       v.animal_id,
       v.application_date::timestamp     AS occurred_at,
       v.creation_date                   AS recorded_at,
       v.recorded_by_person_id,
       v.organization_id,
       true                              AS is_health_data,
       v.vaccine_name                    AS summary
FROM vaccines v
WHERE v.application_date IS NOT NULL

UNION ALL

SELECT a.antiparasitic_id, 'ANTIPARASITARIO', a.animal_id,
       a.application_date::timestamp, a.creation_date,
       a.recorded_by_person_id, a.organization_id, true, a.name
FROM antiparasitics a
WHERE a.application_date IS NOT NULL

UNION ALL

SELECT h.health_record_id, 'ATENDIMENTO', h.animal_id,
       h.event_date::timestamp, h.creation_date,
       h.recorded_by_person_id, h.organization_id, true,
       COALESCE(h.diagnosis, h.event_type)
FROM health_records h

UNION ALL

SELECT w.weight_history_id, 'PESAGEM', w.animal_id,
       w.measured_at::timestamp, w.creation_date,
       w.recorded_by_person_id, w.organization_id, true,
       w.weight::text
FROM animal_weight_history w

UNION ALL

SELECT c.animal_health_condition_id, 'CONDICAO', c.animal_id,
       COALESCE(c.since::timestamp, c.creation_date), c.creation_date,
       c.recorded_by_person_id, c.organization_id, true,
       c.description
FROM animal_health_conditions c

UNION ALL

-- O anexo nao tem data de fato propria: o que ele documenta e datado pelo registro a
-- que aponta. Entao o instante do fato e o do registro, e dizer isso na view e melhor
-- que inventar uma coluna que ninguem preenche.
SELECT t.attachment_id, 'ANEXO', t.animal_id,
       t.creation_date, t.creation_date,
       t.recorded_by_person_id, t.organization_id, true,
       COALESCE(t.description, t.original_filename)
FROM attachments t;
