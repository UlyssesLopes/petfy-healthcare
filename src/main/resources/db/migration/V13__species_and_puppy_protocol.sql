-- Especie como dado, nao mais texto livre em Pet.type, e protocolo de filhote
-- no catalogo. Ver ROADMAP passos 6 e 7.
--
-- 1. Pet ganha coluna 'species'. Backfill heuristico para nao quebrar o pouco
--    dado que existe em dev: tudo comeca CANINA, e o que parece gato pelo
--    'type' vira FELINA. NOT NULL entra depois do backfill.
--
-- 2. Catalogo ganha o protocolo:
--    - initial_dose_count: quantas doses do esquema inicial (adulto = 1)
--    - initial_dose_interval_days: intervalo entre doses do esquema inicial
--      (nulo quando count = 1)
--    - mandatory: entra automaticamente no schedule do filhote. Nem toda vacina
--      do catalogo e obrigatoria - a V3 e a V4 felinas cobrem o mesmo pet, so
--      uma delas deve virar schedule; a antirrabica sim, obrigatoria por lei.

ALTER TABLE pets ADD COLUMN species varchar(32);

UPDATE pets SET species = CASE
    WHEN lower(coalesce(type, '')) LIKE '%gat%' THEN 'FELINA'
    WHEN lower(coalesce(type, '')) LIKE '%cat%' THEN 'FELINA'
    ELSE 'CANINA'
END;

ALTER TABLE pets ALTER COLUMN species SET NOT NULL;

ALTER TABLE vaccine_catalog ADD COLUMN initial_dose_count       integer NOT NULL DEFAULT 1;
ALTER TABLE vaccine_catalog ADD COLUMN initial_dose_interval_days integer;
ALTER TABLE vaccine_catalog ADD COLUMN mandatory                boolean NOT NULL DEFAULT false;

-- Protocolos padrao. Fonte: recomendacoes do CFMV/SBMV para caes e gatos.
-- 3 doses de 21 dias entre 45 e 90 dias, reforco anual (o default_interval_days
-- ja cobre o reforco anual).
UPDATE vaccine_catalog
   SET initial_dose_count = 3,
       initial_dose_interval_days = 21,
       mandatory = true
 WHERE code IN ('V10', 'V8');

-- Uma unica polivalente felina vira mandatory. Escolhida a V4 por cobrir mais
-- que a V3 sem exigir acesso a rua (diferente da V5 com FeLV). Duas doses
-- iniciais aos 60 e 90 dias.
UPDATE vaccine_catalog
   SET initial_dose_count = 2,
       initial_dose_interval_days = 30,
       mandatory = true
 WHERE code IN ('V4_FELINA');

-- Antirrabica: dose unica no esquema inicial (aos 4 meses), reforco anual.
UPDATE vaccine_catalog
   SET initial_dose_count = 1,
       mandatory = true
 WHERE code IN ('ANTIRRABICA_C', 'ANTIRRABICA_F');
