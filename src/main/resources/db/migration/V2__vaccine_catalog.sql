-- Catalogo de vacinas.
--
-- Existe para que o cliente escolha a vacina de uma lista em vez de digitar
-- texto livre, e para que a data da proxima dose seja calculada a partir do
-- intervalo padrao em vez de estimada de cabeca pelo tutor.
--
-- E somente leitura pela API: incluir ou corrigir vacina e uma migration nova.
-- Assim o catalogo fica versionado e nao depende de um papel de administrador,
-- que o sistema ainda nao tem.

CREATE TABLE vaccine_catalog (
    vaccine_catalog_id    uuid         NOT NULL,
    code                  varchar(255) NOT NULL,
    name                  varchar(255) NOT NULL,
    species               varchar(255) NOT NULL,
    default_interval_days integer,
    description           varchar(255),
    CONSTRAINT pk_vaccine_catalog PRIMARY KEY (vaccine_catalog_id),
    CONSTRAINT uk_vaccine_catalog_code UNIQUE (code)
);

-- default_interval_days e o intervalo de REFORCO, nao o protocolo completo.
-- O esquema inicial de filhote (varias doses de 21 a 30 dias) nao esta
-- modelado: exigiria representar protocolo com numero de doses e intervalos
-- diferentes entre elas, o que fica para quando houver necessidade real.
INSERT INTO vaccine_catalog (vaccine_catalog_id, code, name, species, default_interval_days, description) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'V8',            'V8 (Polivalente canina)',      'CANINA', 365, 'Cinomose, parvovirose, hepatite, leptospirose e outras'),
    ('a1000000-0000-4000-8000-000000000002', 'V10',           'V10 (Polivalente canina)',     'CANINA', 365, 'Cobertura da V8 com sorovares adicionais de leptospirose'),
    ('a1000000-0000-4000-8000-000000000003', 'ANTIRRABICA_C', 'Antirrabica canina',           'CANINA', 365, 'Obrigatoria por lei na maior parte do pais'),
    ('a1000000-0000-4000-8000-000000000004', 'GRIPE_CANINA',  'Gripe canina (tosse dos canis)', 'CANINA', 365, 'Recomendada para caes com convivio coletivo'),
    ('a1000000-0000-4000-8000-000000000005', 'GIARDIA',       'Giardia',                      'CANINA', 365, NULL),
    ('a1000000-0000-4000-8000-000000000006', 'LEISHMANIOSE',  'Leishmaniose',                 'CANINA', 365, 'Indicada em regioes endemicas'),
    ('a1000000-0000-4000-8000-000000000007', 'V3_FELINA',     'V3 (Triplice felina)',         'FELINA', 365, 'Panleucopenia, rinotraqueite e calicivirose'),
    ('a1000000-0000-4000-8000-000000000008', 'V4_FELINA',     'V4 (Quadrupla felina)',        'FELINA', 365, 'Triplice felina com clamidiose'),
    ('a1000000-0000-4000-8000-000000000009', 'V5_FELINA',     'V5 (Quintupla felina)',        'FELINA', 365, 'Quadrupla felina com leucemia felina'),
    ('a1000000-0000-4000-8000-00000000000a', 'ANTIRRABICA_F', 'Antirrabica felina',           'FELINA', 365, NULL),
    ('a1000000-0000-4000-8000-00000000000b', 'FELV',          'Leucemia felina (FeLV)',       'FELINA', 365, 'Indicada para gatos com acesso a rua');

-- vacina registrada pode apontar para o catalogo; segue nula para os registros
-- antigos e para vacina digitada em texto livre
ALTER TABLE vaccines ADD COLUMN vaccine_catalog_id uuid;

ALTER TABLE vaccines ADD CONSTRAINT fk_vaccines_catalog
    FOREIGN KEY (vaccine_catalog_id) REFERENCES vaccine_catalog (vaccine_catalog_id);

CREATE INDEX idx_vaccines_catalog ON vaccines (vaccine_catalog_id);

-- a agenda varre as vacinas do tutor procurando proxima dose vencida ou
-- proxima de vencer
CREATE INDEX idx_vaccines_next_dose ON vaccines (next_dose_date);
