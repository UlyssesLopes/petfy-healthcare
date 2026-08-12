-- O animal morreu, e a linha do tempo encerra sem sumir (Tela 33).
--
-- O `CustodyEndReason.OBITO` existe desde o P2b e nunca teve fluxo: o enum sabia que uma custodia
-- pode terminar sem sucessor, mas nao havia rota nenhuma que a terminasse assim. Na pratica o
-- tutor que perdeu o animal so tinha o `DELETE /animals/{id}` — apagar a carteira, o prontuario,
-- o peso, os anexos e os arquivos no disco. O produto oferecia destruir sete anos de registro a
-- quem acabou de perder o bicho, e chamava isso de encerramento.
--
-- ------------------------------------------------------------------ por que uma tabela, e nao colunas
--
-- Cinco colunas anulaveis em `animals` — data, local, despedida, quem registrou, quando —
-- guardariam o mesmo fato e nao conseguiriam garantir a coisa mais simples sobre ele: que se ha
-- data ha tambem quem a registrou. Cada linha desta tabela e um obito inteiro ou nao existe.
--
-- E a chave primaria e o `animal_id`, o que da de graca o invariante que um CHECK nao daria:
-- morre-se uma vez. Nao ha correcao de obito porque nao ha o que corrigir depois — o que se
-- corrige e a data, e para isso a linha e a mesma.
CREATE TABLE animal_deaths (
    animal_id              uuid      NOT NULL,

    -- A data que o tutor sabe, e nao a que o servidor viu. "Quando foi" e o unico campo
    -- obrigatorio da tela, e e o que faz "2019 — 2026" existir na ficha fechada.
    --
    -- SEM CHECK DE DATA FUTURA, e nao por esquecimento: `CURRENT_DATE` nao e IMMUTABLE e o
    -- Postgres recusa a expressao dentro de CHECK. A recusa mora no servico, junto com a
    -- mensagem que explica.
    deceased_on            date      NOT NULL,

    -- "Em casa", "na clinica", "na estrada". Opcional na tela e opcional aqui: quem preenche
    -- este formulario acabou de perder o animal, e o produto nao vai barrar o encerramento
    -- porque faltou um detalhe que nao muda nada no registro.
    place                  varchar(120),

    -- O que o tutor quis dizer. Vai para a linha do tempo assinada por ele — "fica na linha do
    -- tempo dele, no lugar de quem escreveu".
    --
    -- NAO E UMA `observation`, e a distincao e a mesma que o produto ja faz em outro lugar:
    -- observacao e o que alguem VIU do animal, com escopo `OBSERVACOES`, e serve de evidencia
    -- para quem diagnostica. Uma despedida nao e achado clinico, e cair naquele escopo a
    -- entregaria a toda creche que tem `OBSERVACOES` concedido.
    farewell_note          text,

    -- Quem encerrou. Sempre quem respondia pelo animal: o desenho e explicito em que fechar a
    -- linha do tempo e do tutor e nao pode acontecer sem ele. "Ninguem deve descobrir que perdeu
    -- o animal por uma notificacao do sistema."
    recorded_by_person_id  uuid      NOT NULL,

    recorded_at            timestamp NOT NULL,

    CONSTRAINT pk_animal_deaths PRIMARY KEY (animal_id),
    CONSTRAINT fk_animal_deaths_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_animal_deaths_recorded_by FOREIGN KEY (recorded_by_person_id)
        REFERENCES persons (person_id)
);

-- ------------------------------------------------------------- o obito vira um evento da linha do tempo
--
-- Pelo mesmo motivo da uniao: sem ele, quem abrir a vida do animal daqui a tres anos ve 153
-- eventos que param de repente em agosto de 2026 e nao sabe se o animal morreu, se mudou de
-- tutor ou se alguem parou de registrar.
--
-- O EVENTO NAO E UMA TABELA NOVA: ele E a linha do obito, do mesmo jeito que o evento de uniao
-- e o pedido aceito. Duas guardas do mesmo fato acabam discordando, e a que aparece na tela e a
-- que ninguem confere.
--
-- `organization_id` E NULO de proposito. A veterinaria que atendeu na ultima noite pode
-- registrar o obito como ato clinico dela, e isso e o trabalho dela — mas este evento nao e
-- aquele: e o tutor fechando a linha do tempo, e ele nao age por organizacao nenhuma.
--
-- `is_health_data` e FALSE, pela razao ja escrita na uniao: quem tem escopo restrito precisa
-- ver que a vida terminou sem ver o prontuario. Esconder o obito de quem so tem CARTEIRA faria
-- a creche continuar esperando o animal na segunda-feira.
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

    UNION ALL

    -- A uniao, no cadastro que SOBREVIVEU.
    --
    -- Ela nao aparece no absorvido de proposito: aquele cadastro parou de ter vida propria no
    -- instante da uniao, e a linha do tempo dele agora e a do outro.
    --
    -- `occurred_at` e `recorded_at` sao os dois a data da DECISAO, e nao a do pedido: o que
    -- aconteceu com o animal foi a uniao, e ela aconteceu quando alguem disse sim. O pedido e
    -- anterior e nao e fato — e uma pergunta que ficou em aberto.
    --
    -- `is_health_data` e FALSE: unir cadastros e ato administrativo, e nao dado de saude. Quem
    -- tem escopo restrito ve que houve uniao sem ver o prontuario, que e o certo — a uniao muda
    -- o que ele esta lendo, e esconde-la faria o historico parecer inventado.
    SELECT m.animal_merge_request_id, 'UNIAO', m.surviving_animal_id,
           m.decided_at, m.decided_at,
           m.decided_by_person_id, m.organization_id, false,
           m.reason
    FROM animal_merge_requests m
    WHERE m.status = 'ACEITO'

    UNION ALL

    -- O fim.
    --
    -- `occurred_at` e a data que o tutor informou; `recorded_at` e quando ele conseguiu vir
    -- preencher. Os dois separados porque a distancia entre eles e comum e e o assunto da tela:
    -- "nada aqui tem pressa, e voce pode fechar esta tela e voltar depois".
    --
    -- O `summary` e a despedida, que pode ser nula — e a ausencia nao e pendencia. Quem nao quis
    -- escrever nada nao deixou um campo em branco: nao quis escrever nada.
    SELECT d.animal_id, 'OBITO', d.animal_id,
           d.deceased_on::timestamp, d.recorded_at,
           d.recorded_by_person_id, NULL::uuid, false,
           d.farewell_note
    FROM animal_deaths d
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
