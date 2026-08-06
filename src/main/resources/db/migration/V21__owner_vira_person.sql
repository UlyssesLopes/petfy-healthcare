-- P1a da Fase 6: "Owner" vira "Person".
--
-- Renomeacao pura, como a V20. A fusao com "vets" -- que e o que de fato acaba
-- com o papel derivado da tabela -- vem na V22, separada de proposito: esta aqui
-- nao muda comportamento nenhum e se revisa em dois minutos.
--
-- Ainda ha uma tabela "vets" depois desta migration. Ela some na proxima.
--
-- Mesma regra da V20 sobre o que nao se toca: pet_tutors, pet_tutor_invites e
-- pet_clinic_access se dissolvem em custodia e acesso no P2, entao a coluna
-- owner_id delas continua chamando owner_id, apontando para persons.person_id.

-- ---------------------------------------------------------------- tabela
ALTER TABLE owners RENAME TO persons;
ALTER TABLE persons RENAME COLUMN owner_id TO person_id;

-- ---------------------------------------------------------------- quem aponta para a pessoa e sobrevive
ALTER TABLE consent_records            RENAME COLUMN owner_id             TO person_id;
ALTER TABLE password_reset_tokens      RENAME COLUMN owner_id             TO person_id;
ALTER TABLE email_verification_tokens  RENAME COLUMN owner_id             TO person_id;
ALTER TABLE attachments                RENAME COLUMN uploaded_by_owner_id TO uploaded_by_person_id;
ALTER TABLE vaccine_corrections        RENAME COLUMN corrected_by_owner_id TO corrected_by_person_id;
ALTER TABLE health_record_corrections  RENAME COLUMN corrected_by_owner_id TO corrected_by_person_id;

-- ---------------------------------------------------------------- constraints
ALTER TABLE persons                   RENAME CONSTRAINT pk_owners                          TO pk_persons;
ALTER TABLE persons                   RENAME CONSTRAINT uk_owners_email                    TO uk_persons_email;
ALTER TABLE consent_records           RENAME CONSTRAINT fk_consent_records_owner           TO fk_consent_records_person;
ALTER TABLE consent_records           RENAME CONSTRAINT uk_consent_records_owner_document_version
                                                                                           TO uk_consent_records_person_document_version;
ALTER TABLE password_reset_tokens     RENAME CONSTRAINT fk_password_reset_tokens_owner     TO fk_password_reset_tokens_person;
ALTER TABLE email_verification_tokens RENAME CONSTRAINT fk_email_verification_tokens_owner TO fk_email_verification_tokens_person;
ALTER TABLE vaccine_corrections       RENAME CONSTRAINT fk_vaccine_corrections_owner       TO fk_vaccine_corrections_person;
ALTER TABLE health_record_corrections RENAME CONSTRAINT fk_hr_corrections_owner            TO fk_hr_corrections_person;
