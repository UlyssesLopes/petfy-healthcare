-- P0 da Fase 6: "Pet" vira "Animal" no vocabulario do banco.
--
-- Renomeacao pura: nenhuma coluna nasce, nenhuma morre, nenhum dado se move.
-- O motivo esta no ROADMAP: "Pet" pressupoe dono, e o modelo que vem a seguir
-- tem animal sob custodia de organizacao, sem tutor humano. Nome que mente e a
-- proxima pessoa lendo "Pet" e assumindo que existe dono.
--
-- V1 a V19 NAO sao reescritas: sao log do que aconteceu, nao descricao do
-- estado atual.
--
-- Tres tabelas ficam de fora de proposito -- pet_tutors, pet_tutor_invites e
-- pet_clinic_access. Elas se dissolvem em custodia e acesso no P2, e renomea-las
-- agora e trabalho que se joga fora. Por isso a coluna pet_id delas continua
-- chamando pet_id, apontando para animals.animal_id: e uma chave estrangeira, o
-- nome nao precisa casar, e o descasamento marca justamente o que e o mundo
-- velho.

-- ---------------------------------------------------------------- tabelas
ALTER TABLE pets                  RENAME TO animals;
ALTER TABLE pet_shares            RENAME TO animal_shares;
ALTER TABLE pet_weight_history    RENAME TO animal_weight_history;
ALTER TABLE pet_health_conditions RENAME TO animal_health_conditions;

-- ---------------------------------------------------------------- chaves primarias
ALTER TABLE animals                  RENAME COLUMN pet_id                  TO animal_id;
ALTER TABLE animal_shares            RENAME COLUMN pet_share_id            TO animal_share_id;
ALTER TABLE animal_health_conditions RENAME COLUMN pet_health_condition_id TO animal_health_condition_id;

-- ---------------------------------------------------------------- chaves estrangeiras para o animal
ALTER TABLE vaccines                 RENAME COLUMN pet_id TO animal_id;
ALTER TABLE health_records           RENAME COLUMN pet_id TO animal_id;
ALTER TABLE antiparasitics           RENAME COLUMN pet_id TO animal_id;
ALTER TABLE attachments              RENAME COLUMN pet_id TO animal_id;
ALTER TABLE sensitive_access_log     RENAME COLUMN pet_id TO animal_id;
ALTER TABLE animal_shares            RENAME COLUMN pet_id TO animal_id;
ALTER TABLE animal_weight_history    RENAME COLUMN pet_id TO animal_id;
ALTER TABLE animal_health_conditions RENAME COLUMN pet_id TO animal_id;

-- ---------------------------------------------------------------- constraints
ALTER TABLE animals                  RENAME CONSTRAINT pk_pets                     TO pk_animals;
ALTER TABLE animal_shares            RENAME CONSTRAINT pk_pet_shares               TO pk_animal_shares;
ALTER TABLE animal_shares            RENAME CONSTRAINT uk_pet_shares_token_hash    TO uk_animal_shares_token_hash;
ALTER TABLE animal_shares            RENAME CONSTRAINT fk_pet_shares_pet           TO fk_animal_shares_animal;
ALTER TABLE animal_weight_history    RENAME CONSTRAINT pk_pet_weight_history       TO pk_animal_weight_history;
ALTER TABLE animal_weight_history    RENAME CONSTRAINT fk_pet_weight_history_pet   TO fk_animal_weight_history_animal;
ALTER TABLE animal_health_conditions RENAME CONSTRAINT pk_pet_health_conditions    TO pk_animal_health_conditions;
ALTER TABLE animal_health_conditions RENAME CONSTRAINT fk_pet_health_conditions_pet TO fk_animal_health_conditions_animal;
ALTER TABLE vaccines                 RENAME CONSTRAINT fk_vaccines_pet             TO fk_vaccines_animal;
ALTER TABLE health_records           RENAME CONSTRAINT fk_health_records_pet       TO fk_health_records_animal;
ALTER TABLE antiparasitics           RENAME CONSTRAINT fk_antiparasitics_pet       TO fk_antiparasitics_animal;
ALTER TABLE attachments              RENAME CONSTRAINT fk_attachments_pet          TO fk_attachments_animal;
ALTER TABLE sensitive_access_log     RENAME CONSTRAINT fk_sensitive_access_log_pet TO fk_sensitive_access_log_animal;

-- ---------------------------------------------------------------- indices
ALTER INDEX idx_pets_microchip_number        RENAME TO idx_animals_microchip_number;
ALTER INDEX idx_pet_shares_pet               RENAME TO idx_animal_shares_animal;
ALTER INDEX idx_pet_weight_history_pet       RENAME TO idx_animal_weight_history_animal;
ALTER INDEX idx_pet_weight_history_measured_at RENAME TO idx_animal_weight_history_measured_at;
ALTER INDEX idx_pet_health_conditions_pet    RENAME TO idx_animal_health_conditions_animal;
ALTER INDEX idx_vaccines_pet                 RENAME TO idx_vaccines_animal;
ALTER INDEX idx_health_records_pet           RENAME TO idx_health_records_animal;
ALTER INDEX idx_antiparasitics_pet           RENAME TO idx_antiparasitics_animal;
ALTER INDEX idx_attachments_pet              RENAME TO idx_attachments_animal;
ALTER INDEX idx_sensitive_access_log_pet_accessed RENAME TO idx_sensitive_access_log_animal_accessed;
