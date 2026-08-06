-- P5 da Fase 6: orientacao com prazo e confirmacao de cumprimento.
--
-- Um conceito, tres usos que estavam separados: prescricao do veterinario, medicacao
-- e tratamento continuo - que estava parado em "Fica para depois do frontend" - e tema
-- de casa da creche. Os tres tem a mesma forma: alguem manda, alguem cumpre, e o
-- cumprimento precisa ficar registrado.

CREATE TABLE care_instructions (
    care_instruction_id   uuid         NOT NULL,
    animal_id             uuid         NOT NULL,
    description           varchar(500) NOT NULL,
    interval_days         integer      NOT NULL,
    starts_on             date         NOT NULL,
    ends_on               date,
    revoked_at            timestamp,
    -- Quem encerrou. Encerrar um tratamento e decisao clinica, e "o tutor parou" e "o
    -- veterinario suspendeu" nao sao o mesmo fato para quem ler o historico depois.
    revoked_by_person_id  uuid,
    -- o nucleo de evento do P4: quem registrou, e em nome de quem
    recorded_by_person_id uuid,
    organization_id       uuid,
    creation_date         timestamp,
    CONSTRAINT pk_care_instructions PRIMARY KEY (care_instruction_id),
    CONSTRAINT fk_care_instructions_animal
        FOREIGN KEY (animal_id) REFERENCES animals (animal_id),
    CONSTRAINT fk_care_instructions_recorded_by
        FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_care_instructions_revoked_by
        FOREIGN KEY (revoked_by_person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_care_instructions_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),

    -- Intervalo tem de ser positivo: zero significaria pendencia que renasce no mesmo
    -- instante em que e cumprida, e negativo nao significa nada.
    CONSTRAINT ck_care_instructions_intervalo_positivo CHECK (interval_days > 0),

    -- Prazo que termina antes de comecar nao e tratamento, e erro de digitacao. O banco
    -- recusa porque o servico pode esquecer, e uma orientacao assim nunca apareceria
    -- como pendencia - falharia em silencio, que e o pior modo de falhar.
    CONSTRAINT ck_care_instructions_prazo_coerente CHECK (ends_on IS NULL OR ends_on >= starts_on)
);

CREATE INDEX idx_care_instructions_animal ON care_instructions (animal_id);

CREATE TABLE care_instruction_fulfillments (
    care_instruction_fulfillment_id uuid         NOT NULL,
    care_instruction_id             uuid         NOT NULL,
    confirmed_by_person_id          uuid         NOT NULL,
    fulfilled_at                    timestamp    NOT NULL,
    recorded_at                     timestamp    NOT NULL,
    note                            varchar(500),
    CONSTRAINT pk_care_instruction_fulfillments PRIMARY KEY (care_instruction_fulfillment_id),
    CONSTRAINT fk_care_instruction_fulfillments_instruction
        FOREIGN KEY (care_instruction_id) REFERENCES care_instructions (care_instruction_id),
    CONSTRAINT fk_care_instruction_fulfillments_person
        FOREIGN KEY (confirmed_by_person_id) REFERENCES persons (person_id)
);

CREATE INDEX idx_care_instruction_fulfillments_instruction
    ON care_instruction_fulfillments (care_instruction_id);

-- ---------------------------------------------------------------- a linha do tempo cresce
--
-- Duas entradas novas, e a distincao entre elas e o ponto:
--
-- ORIENTACAO e o fato de alguem ter mandado - "o veterinario prescreveu isto em marco".
-- CUMPRIMENTO e o fato de alguem ter feito - "o remedio foi dado no dia 3". Sao dois
-- fatos diferentes, e juntar os dois numa entrada perderia o historico de aderencia,
-- que e o dado que o veterinario nunca tem quando o tratamento nao funciona.
--
-- CREATE OR REPLACE em vez de DROP e CREATE: a lista de colunas nao muda, so a uniao
-- ganha ramos. DROP exigiria recriar tambem qualquer coisa que dependesse da view.
CREATE OR REPLACE VIEW animal_timeline AS
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

SELECT t.attachment_id, 'ANEXO', t.animal_id,
       t.creation_date, t.creation_date,
       t.recorded_by_person_id, t.organization_id, true,
       COALESCE(t.description, t.original_filename)
FROM attachments t

UNION ALL

SELECT i.care_instruction_id, 'ORIENTACAO', i.animal_id,
       i.starts_on::timestamp, i.creation_date,
       i.recorded_by_person_id, i.organization_id, true,
       i.description
FROM care_instructions i

UNION ALL

-- O cumprimento herda a organizacao de quem EMITIU a orientacao, e nao de quem
-- cumpriu: quem da o remedio em casa e o tutor, e ele nao age por organizacao nenhuma.
-- O que a linha do tempo precisa dizer e de qual tratamento aquele cumprimento faz
-- parte.
SELECT f.care_instruction_fulfillment_id, 'CUMPRIMENTO', i.animal_id,
       f.fulfilled_at, f.recorded_at,
       f.confirmed_by_person_id, i.organization_id, true,
       i.description
FROM care_instruction_fulfillments f
JOIN care_instructions i ON i.care_instruction_id = f.care_instruction_id;
