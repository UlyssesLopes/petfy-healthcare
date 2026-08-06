-- P3a da Fase 6: clinica vira organizacao, e capacidade deixa de ser implicita.
--
-- Clinica, creche e abrigo nao sao tres entidades: sao a mesma exercendo conjuntos
-- diferentes de capacidades. Modelar como tipo obrigaria a escolher um e mentir
-- sobre o resto - ONG com clinica propria faz as tres, creche que hospeda faz duas.
--
-- O que era especifico de clinica continua na tabela, porque continua sendo verdade
-- de qualquer organizacao que atende: nome, CNPJ, endereco, contato. O que era
-- IMPLICITO - "isto e uma clinica, entao registra ato clinico" - vira explicito.

-- ---------------------------------------------------------------- tabelas
ALTER TABLE clinics        RENAME TO organizations;
ALTER TABLE clinic_invites RENAME TO organization_invites;

ALTER TABLE organizations        RENAME COLUMN clinic_id        TO organization_id;
ALTER TABLE organization_invites RENAME COLUMN clinic_invite_id TO organization_invite_id;
ALTER TABLE organization_invites RENAME COLUMN clinic_id        TO organization_id;

-- ---------------------------------------------------------------- quem aponta para a organizacao
ALTER TABLE persons                   RENAME COLUMN clinic_id               TO organization_id;
ALTER TABLE vaccines                  RENAME COLUMN clinic_id               TO organization_id;
ALTER TABLE health_records            RENAME COLUMN clinic_id               TO organization_id;
ALTER TABLE vaccine_corrections       RENAME COLUMN corrected_in_clinic_id  TO corrected_in_organization_id;
ALTER TABLE health_record_corrections RENAME COLUMN corrected_in_clinic_id  TO corrected_in_organization_id;
ALTER TABLE grants                    RENAME COLUMN grantee_clinic_id       TO grantee_organization_id;
ALTER TABLE custodies                 RENAME COLUMN holder_clinic_id        TO holder_organization_id;

-- O log guarda o nome no momento do acesso, e nao uma chave estrangeira: a
-- organizacao pode ser renomeada depois, e o registro de que ela leu o historico nao
-- pode virar linha com o nome de hoje.
ALTER TABLE sensitive_access_log      RENAME COLUMN clinic_name             TO organization_name;

-- ---------------------------------------------------------------- constraints
ALTER TABLE organizations        RENAME CONSTRAINT pk_clinics                    TO pk_organizations;
ALTER TABLE organization_invites RENAME CONSTRAINT pk_clinic_invites             TO pk_organization_invites;
ALTER TABLE organization_invites RENAME CONSTRAINT uk_clinic_invites_token_hash  TO uk_organization_invites_token_hash;
ALTER TABLE organization_invites RENAME CONSTRAINT fk_clinic_invites_clinic      TO fk_organization_invites_organization;
ALTER TABLE organization_invites RENAME CONSTRAINT fk_clinic_invites_created_by  TO fk_organization_invites_created_by;
ALTER TABLE organization_invites RENAME CONSTRAINT fk_clinic_invites_accepted_by TO fk_organization_invites_accepted_by;
ALTER TABLE persons                   RENAME CONSTRAINT fk_persons_clinic             TO fk_persons_organization;
ALTER TABLE vaccines                  RENAME CONSTRAINT fk_vaccines_clinic            TO fk_vaccines_organization;
ALTER TABLE health_records            RENAME CONSTRAINT fk_health_records_clinic      TO fk_health_records_organization;
ALTER TABLE vaccine_corrections       RENAME CONSTRAINT fk_vaccine_corrections_clinic TO fk_vaccine_corrections_organization;
ALTER TABLE health_record_corrections RENAME CONSTRAINT fk_hr_corrections_clinic      TO fk_hr_corrections_organization;
ALTER TABLE grants                    RENAME CONSTRAINT fk_grants_grantee_clinic      TO fk_grants_grantee_organization;
ALTER TABLE custodies                 RENAME CONSTRAINT fk_custodies_holder_clinic    TO fk_custodies_holder_organization;

-- ---------------------------------------------------------------- indices
ALTER INDEX idx_clinic_invites_clinic  RENAME TO idx_organization_invites_organization;
ALTER INDEX idx_persons_clinic         RENAME TO idx_persons_organization;
ALTER INDEX idx_vaccines_clinic        RENAME TO idx_vaccines_organization;
ALTER INDEX idx_health_records_clinic  RENAME TO idx_health_records_organization;
ALTER INDEX idx_grants_grantee_clinic  RENAME TO idx_grants_grantee_organization;

-- ---------------------------------------------------------------- capacidade, explicita
CREATE TABLE organization_capabilities (
    organization_id uuid        NOT NULL,
    capability      varchar(32) NOT NULL,
    CONSTRAINT pk_organization_capabilities PRIMARY KEY (organization_id, capability),
    CONSTRAINT fk_organization_capabilities_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
);

-- Toda organizacao existente era uma clinica, entao recebe o conjunto de uma
-- clinica: ato clinico, observacao e comunicacao com o tutor.
--
-- DETER_CUSTODIA fica FORA de proposito, e essa e a linha mais importante deste
-- backfill: clinica nunca respondeu por animal nenhum, e conceder a capacidade
-- retroativamente daria a toda clinica cadastrada o direito de virar responsavel por
-- um animal. Abrigo entra por cadastro, nao por migration.
INSERT INTO organization_capabilities (organization_id, capability)
SELECT o.organization_id, c.capability
FROM organizations o
CROSS JOIN (VALUES ('REGISTRAR_ATO_CLINICO'),
                   ('REGISTRAR_OBSERVACAO'),
                   ('COMUNICAR_COM_TUTOR')) AS c(capability);
