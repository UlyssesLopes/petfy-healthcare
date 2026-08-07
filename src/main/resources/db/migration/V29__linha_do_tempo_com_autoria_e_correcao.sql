-- P3 do passo 4: a linha do tempo passa a carregar o que a tela precisa para desenhar
-- um evento assinado (DESIGN.md 5.2).
--
-- Nenhuma tabela muda. Sao cinco colunas novas na view, e todas ja existiam no banco -
-- o que faltava era chegarem juntas ao cliente.
--
-- ------------------------------------------------------------------ o que a tela cobrava
--
-- "Duas linhas: o fato, e quem o afirmou. A segunda nunca e opcional, nunca e tooltip,
-- nunca e 'ver detalhes'." Para cumprir isso o cliente precisava de tres coisas que a
-- view nao dava:
--
--   organization_name  - "a Ana, pela Clinica Norte, registrou" carrega responsabilidade
--                        institucional; "a Ana registrou" nao. A view tinha o id e o
--                        cliente nao tem como resolve-lo sem uma chamada por evento.
--   credential_*       - "a credencial diz o que e. CRMV apenas informado aparece como
--                        informado, sem selo de verificado que o produto nao pode dar"
--                        (5.10). Sem o status, a tela ou omite a credencial ou mente
--                        sobre ela.
--   correction_count   - "correcao e sucessao, e se ve". Sem isso a tela so descobre que
--                        um evento foi corrigido chamando a rota de correcoes de cada
--                        um, ou seja, nunca descobre.
--
-- ------------------------------------------------------------- e uma correcao de desempenho
--
-- recorded_by_name entra pelo mesmo motivo, e resolve um N+1 que ja existia: o
-- TimelineServiceImpl fazia personRepository.findById() por entrada, entao uma pagina de
-- vinte eventos custava vinte consultas. Acrescentar organizacao e credencial no mesmo
-- padrao teria feito disso N+3.
--
-- --------------------------------------------------------------- por que CTE e nao repetir
--
-- Os ramos da uniao continuam identicos, agrupados num CTE, e os JOINs entram UMA vez do
-- lado de fora. Repeti-los nos oito ramos seria a mesma regra escrita oito vezes, e a
-- proxima entrada da linha do tempo esqueceria um deles em silencio.
--
-- CREATE OR REPLACE continua valendo: o Postgres exige que as colunas existentes
-- mantenham nome, tipo e ordem, e permite acrescentar no fim. E exatamente o que isto
-- faz - as nove primeiras saem do CTE na ordem original.

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

       -- LEFT JOIN, e nao JOIN: evento anterior ao P4 nao tem autor, e a migration nao
       -- inventou um. Nulo aqui e a resposta honesta - mostrar o titular atual como se
       -- ele tivesse registrado seria pior que nao dizer nada.
       p.name                                    AS recorded_by_name,

       o.name                                    AS organization_name,

       cred.label                                AS credential_label,
       cred.status                               AS credential_status,

       -- Contagem, e nao a cadeia inteira: a tela precisa saber QUE houve sucessao para
       -- marcar o evento, e o conteudo do valor anterior continua em
       -- /vaccines/{id}/corrections e /health-records/{id}/corrections. Trazer toda
       -- correcao de toda entrada aqui pesaria a leitura mais quente do produto para
       -- servir o caso raro.
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

-- A credencial de quem registrou, uma so e escolhida de forma deterministica.
--
-- A tabela admite mais de uma por pessoa (conselhos e UFs diferentes), e a tela mostra
-- uma linha. A ordem prefere credencial nao suspensa - porque e a que autoriza ato
-- clinico, ver CredentialStatus.autorizaAtoClinico() - e depois a mais recente. Sem o
-- ORDER BY a escolha seria a que o Postgres devolvesse primeiro, e a mesma tela mudaria
-- de credencial entre duas leituras.
LEFT JOIN LATERAL (
    SELECT pc.council || '-' || pc.uf || ' ' || pc.number AS label,
           pc.status
    FROM professional_credentials pc
    WHERE pc.person_id = b.recorded_by_person_id
    ORDER BY (pc.status <> 'SUSPENSO') DESC, pc.creation_date DESC NULLS LAST
    LIMIT 1
) cred ON true;
