-- Passo 9: antiparasitario como recorrente e peso como serie historica.
--
-- 9a. Antiparasitario
-- Nova entidade paralela a Vaccine. Vermifugo e antipulgas sao o recorrente que
-- o tutor de fato esquece. Manter separado de Vaccine evita breaking change na
-- tabela e no contrato da API. O scheduler de lembretes varre as duas.
--
-- 9b. Peso como serie
-- pet_weight_history guarda cada medicao. Pet.weight permanece como espelho da
-- ultima medicao para leitura rapida em endpoints que ja retornam o pet -
-- evita join adicional no caso mais comum.

-- -------------------------------------------------------------------------
-- Catalogo de antiparasitarios (mantido por migration, somente leitura pela API)
-- -------------------------------------------------------------------------
CREATE TABLE antiparasitic_catalog (
    antiparasitic_catalog_id  uuid         NOT NULL,
    code                      varchar(255) NOT NULL,
    name                      varchar(255) NOT NULL,
    kind                      varchar(50)  NOT NULL, -- AntiparasiticKind: DEWORMER | FLEA_TICK
    species                   varchar(32)  NOT NULL, -- Species: CANINA | FELINA
    default_interval_days     integer,
    description               varchar(255),
    CONSTRAINT pk_antiparasitic_catalog PRIMARY KEY (antiparasitic_catalog_id),
    CONSTRAINT uk_antiparasitic_catalog_code UNIQUE (code)
);

INSERT INTO antiparasitic_catalog
    (antiparasitic_catalog_id, code, name, kind, species, default_interval_days, description)
VALUES
    -- Vermifugos caninos
    ('b1000000-0000-4000-8000-000000000001', 'VERMIFUGO_CAO_3M',
     'Vermifugo canino (trimestral)', 'DEWORMER', 'CANINA', 90,
     'Protocolo de manutencao recomendado para caes adultos'),
    ('b1000000-0000-4000-8000-000000000002', 'VERMIFUGO_FILHOTE_CAO_15D',
     'Vermifugo filhote canino (quinzenal)', 'DEWORMER', 'CANINA', 15,
     'Protocolo inicial para filhotes ate 3 meses'),
    -- Antipulgas caninos
    ('b1000000-0000-4000-8000-000000000003', 'ANTIPULGA_CAO_1M',
     'Antipulgas / carrapatos canino (mensal)', 'FLEA_TICK', 'CANINA', 30,
     'Coleira, pipeta ou comprimido mensal para caes'),
    -- Vermifugos felinos
    ('b1000000-0000-4000-8000-000000000004', 'VERMIFUGO_GATO_3M',
     'Vermifugo felino (trimestral)', 'DEWORMER', 'FELINA', 90,
     'Protocolo de manutencao para gatos adultos'),
    ('b1000000-0000-4000-8000-000000000005', 'VERMIFUGO_FILHOTE_GATO_15D',
     'Vermifugo filhote felino (quinzenal)', 'DEWORMER', 'FELINA', 15,
     'Protocolo inicial para filhotes felinos ate 3 meses'),
    -- Antipulgas felinos
    ('b1000000-0000-4000-8000-000000000006', 'ANTIPULGA_GATO_1M',
     'Antipulgas / carrapatos felino (mensal)', 'FLEA_TICK', 'FELINA', 30,
     'Pipeta mensal para gatos');

-- -------------------------------------------------------------------------
-- Registro de aplicacoes antiparasitarias
-- -------------------------------------------------------------------------
CREATE TABLE antiparasitics (
    antiparasitic_id          uuid         NOT NULL,
    pet_id                    uuid         NOT NULL,
    name                      varchar(255) NOT NULL,
    kind                      varchar(50)  NOT NULL, -- AntiparasiticKind: DEWORMER | FLEA_TICK
    application_date          date,
    next_dose_date            date,
    description               varchar(500),
    antiparasitic_catalog_id  uuid,
    last_reminder_sent_at     timestamp,
    creation_date             timestamp    NOT NULL,
    update_date               timestamp    NOT NULL,
    CONSTRAINT pk_antiparasitics PRIMARY KEY (antiparasitic_id),
    CONSTRAINT fk_antiparasitics_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_antiparasitics_catalog
        FOREIGN KEY (antiparasitic_catalog_id)
        REFERENCES antiparasitic_catalog (antiparasitic_catalog_id)
);

CREATE INDEX idx_antiparasitics_pet        ON antiparasitics (pet_id);
CREATE INDEX idx_antiparasitics_next_dose  ON antiparasitics (next_dose_date);
CREATE INDEX idx_antiparasitics_catalog    ON antiparasitics (antiparasitic_catalog_id);

-- -------------------------------------------------------------------------
-- Historico de peso
-- -------------------------------------------------------------------------
CREATE TABLE pet_weight_history (
    weight_history_id  uuid             NOT NULL,
    pet_id             uuid             NOT NULL,
    weight             double precision NOT NULL,
    measured_at        date             NOT NULL,
    note               varchar(500),
    creation_date      timestamp        NOT NULL,
    CONSTRAINT pk_pet_weight_history PRIMARY KEY (weight_history_id),
    CONSTRAINT fk_pet_weight_history_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id)
);

CREATE INDEX idx_pet_weight_history_pet         ON pet_weight_history (pet_id);
CREATE INDEX idx_pet_weight_history_measured_at ON pet_weight_history (pet_id, measured_at DESC);
