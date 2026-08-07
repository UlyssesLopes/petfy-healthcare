-- A pesagem passa a carregar a pesagem anterior, e com isso a variacao.
--
-- "Serie mora dentro do evento. O registro de peso carrega o proprio grafico e a
-- variacao - peso E serie (3.9), entao o lugar dela e a linha do registro, nao um painel
-- a parte." (DESIGN 5.2)
--
-- ------------------------------------------------------------------- por que so o anterior
--
-- O grafico precisa de N pontos, e a linha do tempo nao e lugar de servir N pontos por
-- entrada. Quem serve a curva inteira e GET /animals/{id}/weights, que ja existe.
--
-- O que a ENTRADA precisa e outra coisa: "12,5 kg" sozinho nao diz se e boa ou ma
-- noticia. Com o valor anterior o cliente calcula a variacao e a linha fica honesta, em
-- uma linha, sem virar painel - que e o que o DESIGN recusa em cinco lugares.
--
-- ------------------------------------------------------------- por que data estritamente anterior
--
-- A comparacao e por DIA, e duas pesagens no mesmo dia nao produzem variacao entre si.
-- Nao e limitacao esquecida: variacao de peso e leitura de tendencia, e diferenca entre
-- duas medidas da mesma tarde e ruido de balanca - a mesma logica que faz a linha do
-- tempo ordenar por quando aconteceu e nao por quando foi digitado.
--
-- O desempate por creation_date existe para a escolha ser deterministica quando ha duas
-- medidas no mesmo dia anterior: sem ele o Postgres devolveria qualquer uma das duas, e a
-- mesma tela mostraria variacoes diferentes entre duas leituras.

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
       END                                       AS correction_count,

       -- Nulo em tudo que nao e pesagem, e nulo tambem na primeira pesagem do animal:
       -- nao ha anterior, e zero seria mentira - diria "nao variou".
       CASE WHEN b.event_type = 'PESAGEM' THEN (
           SELECT w2.weight
           FROM animal_weight_history w2
           WHERE w2.animal_id = b.animal_id
             AND w2.measured_at < b.occurred_at::date
           ORDER BY w2.measured_at DESC, w2.creation_date DESC NULLS LAST
           LIMIT 1
       ) END                                     AS previous_weight

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
