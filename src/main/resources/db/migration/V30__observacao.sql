-- Observacao: o que alguem viu.
--
-- O 3.11 do PRODUTO.md a define desde o documento fundador, e ela nunca existiu no
-- backend. A ausencia deixava tres coisas sem lugar:
--
--   1. A creche registra "nao comeu, mancou, vomitou, brigou". Hoje ela so pode anexar
--      arquivo, ou nada.
--   2. O 3.11 diz que observacao pode ser REFERENCIADA por um ato clinico como
--      evidencia - "o veterinario le 'mancou da direita nos ultimos 3 dias' registrado
--      pela creche e emite um diagnostico que aponta para aquelas observacoes". Sem
--      observacao, nao ha o que citar.
--   3. O alerta da creche (4.5) e definido como "observacao com sinalizacao de
--      urgencia". Nao existindo observacao, o alerta nao existe.
--
-- ------------------------------------------------------- por que escopo proprio, e nao PRONTUARIO
--
-- Observacao vai em GrantScope.OBSERVACOES, novo. Coloca-la em PRONTUARIO faria com que
-- dar a creche acesso ao que ela mesma escreve entregasse tambem todo atendimento
-- clinico do animal - exatamente o que o escopo existe para impedir, e o exemplo que a
-- propria doc do GrantScope usa: "matricular o cachorro numa creche entregava a ela o
-- historico completo de doencas".
--
-- Escopo novo e aditivo: concessao existente simplesmente nao o tem, e passa a nao ver
-- observacao. Isso e o default correto - ninguem concedeu acesso a uma coisa que nao
-- existia quando concedeu.
--
-- --------------------------------------------------------------- observacao e dado de saude
--
-- is_health_data = true, sempre. O 3.11 diz "observacao quase sempre e", e o quase abre
-- uma classificacao que teria de ser decidida por linha - ou pelo autor, e a mesma frase
-- mudaria de regime conforme quem digitou, ou por um campo, e ai a classificacao de dado
-- sensivel fica na mao de quem esta com pressa no balcao. Tratar sempre como dado de
-- saude e mais restritivo do que o necessario em "brincou muito hoje", e nunca vaza por
-- classificacao errada.

CREATE TABLE observations (
    observation_id        uuid          NOT NULL,
    animal_id             uuid          NOT NULL,
    description           varchar(1000) NOT NULL,

    -- Quando foi visto, e nao quando foi digitado. A creche registra as 18h o que viu as
    -- 9h, e a linha do tempo ordena pelo fato (3.9).
    observed_at           timestamp     NOT NULL,

    -- O alerta da creche (4.5): observacao com urgencia. NAO e ato clinico e nao e
    -- emergencia medica - ganha peso pela posicao no feed, e nao por cor de alarme
    -- (DESIGN 5.5). Fica na propria observacao porque urgencia e atributo do que se viu,
    -- nao um segundo tipo de registro.
    urgent                boolean       NOT NULL DEFAULT false,

    -- o nucleo de evento do P4: quem registrou, e em nome de quem
    recorded_by_person_id uuid,
    organization_id       uuid,

    recorded_at           timestamp     NOT NULL,
    creation_date         timestamp,

    CONSTRAINT pk_observations PRIMARY KEY (observation_id),
    CONSTRAINT fk_observations_animal
        FOREIGN KEY (animal_id) REFERENCES animals (animal_id),
    CONSTRAINT fk_observations_recorded_by
        FOREIGN KEY (recorded_by_person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_observations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),

    -- Observacao vazia nao e observacao. O banco recusa porque o servico pode esquecer, e
    -- uma linha em branco na linha do tempo de um animal e ruido permanente - nada se
    -- apaga aqui (promessa 5.2).
    CONSTRAINT ck_observations_descricao_nao_vazia CHECK (btrim(description) <> '')
);

-- A consulta e sempre "as observacoes deste animal, mais recentes primeiro", tanto na
-- rota propria quanto na view.
CREATE INDEX idx_observations_animal ON observations (animal_id, observed_at DESC);

-- ---------------------------------------------------------------- a linha do tempo cresce
--
-- Um ramo novo no CTE. As colunas nao mudam, entao CREATE OR REPLACE continua valendo.
CREATE OR REPLACE VIEW animal_timeline AS
WITH base AS (
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
    -- cumpriu: quem da o remedio em casa e o tutor, e ele nao age por organizacao
    -- nenhuma. O que a linha do tempo precisa dizer e de qual tratamento aquele
    -- cumprimento faz parte.
    SELECT f.care_instruction_fulfillment_id, 'CUMPRIMENTO', i.animal_id,
           f.fulfilled_at, f.recorded_at,
           f.confirmed_by_person_id, i.organization_id, true,
           i.description
    FROM care_instruction_fulfillments f
    JOIN care_instructions i ON i.care_instruction_id = f.care_instruction_id

    UNION ALL

    SELECT ob.observation_id, 'OBSERVACAO', ob.animal_id,
           ob.observed_at, ob.recorded_at,
           ob.recorded_by_person_id, ob.organization_id, true,
           ob.description
    FROM observations ob
)
SELECT b.event_id,
       b.event_type,
       b.animal_id,
       b.occurred_at,
       b.recorded_at,
       b.recorded_by_person_id,
       b.organization_id,
       b.is_health_data,
       b.summary,
       p.name                                    AS recorded_by_name,
       o.name                                    AS organization_name,
       cred.label                                AS credential_label,
       cred.status                               AS credential_status,
       CASE b.event_type
           WHEN 'VACINA' THEN (SELECT count(*) FROM vaccine_corrections vc
                               WHERE vc.vaccine_id = b.event_id)
           WHEN 'ATENDIMENTO' THEN (SELECT count(*) FROM health_record_corrections hc
                                    WHERE hc.health_record_id = b.event_id)
           ELSE 0
       END                                       AS correction_count
FROM base b
LEFT JOIN persons p ON p.person_id = b.recorded_by_person_id
LEFT JOIN organizations o ON o.organization_id = b.organization_id
LEFT JOIN LATERAL (
    SELECT pc.council || '-' || pc.uf || ' ' || pc.number AS label,
           pc.status
    FROM professional_credentials pc
    WHERE pc.person_id = b.recorded_by_person_id
    ORDER BY (pc.status <> 'SUSPENSO') DESC, pc.creation_date DESC NULLS LAST
    LIMIT 1
) cred ON true;
