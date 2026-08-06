-- P1b da Fase 6: acabam as duas tabelas de conta.
--
-- Ate aqui o papel de uma pessoa era derivado de qual tabela o e-mail dela
-- aparecia. Isso tornava impossivel a veterinaria que tem cachorro, o vet
-- autonomo, a pessoa comum que vira lar transitorio e o voluntario de abrigo que
-- tambem e tutor - e nenhum deles e caso raro: e o caso comum de quem gosta de
-- animal a ponto de usar o Petfy.
--
-- Os UUIDs sao preservados na fusao, entao nenhuma chave estrangeira precisa de
-- remapeamento: elas so passam a apontar para persons.

-- ---------------------------------------------------------------- credencial profissional
-- Tabela, e nao coluna: o registro e por conselho e por estado, entao uma pessoa
-- pode ter mais de um; e ele tem estado proprio, que muda sem a pessoa mudar.
CREATE TABLE professional_credentials (
    professional_credential_id uuid        NOT NULL,
    person_id                  uuid        NOT NULL,
    council                    varchar(16) NOT NULL,
    uf                         varchar(2)  NOT NULL,
    number                     varchar(32) NOT NULL,
    status                     varchar(16) NOT NULL,
    creation_date              timestamp,
    update_date                timestamp,
    CONSTRAINT pk_professional_credentials PRIMARY KEY (professional_credential_id),
    CONSTRAINT fk_professional_credentials_person FOREIGN KEY (person_id) REFERENCES persons (person_id),
    -- o mesmo registro nao pode ser reivindicado por duas pessoas
    CONSTRAINT uk_professional_credentials_registro UNIQUE (council, uf, number)
);

CREATE INDEX idx_professional_credentials_person ON professional_credentials (person_id);

-- ---------------------------------------------------------------- a ponte que morre no P3
-- O lugar certo disto e Membership, porque uma pessoa atua em N organizacoes.
-- Enquanto ele nao existe, a coluna fica aqui - NULLABLE, e e isso que torna o
-- veterinario autonomo possivel: em vets ela era NOT NULL, entao atendimento
-- domiciliar obrigava a inventar uma clinica.
ALTER TABLE persons ADD COLUMN clinic_id uuid;
ALTER TABLE persons ADD CONSTRAINT fk_persons_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id);
CREATE INDEX idx_persons_clinic ON persons (clinic_id);

-- ---------------------------------------------------------------- vets viram persons
-- email_verified_at fica nulo: nenhum veterinario jamais confirmou o proprio
-- e-mail, porque a verificacao so existia para tutor. Herdar como verificado
-- seria inventar um fato. Na pratica eles param de receber notificacao ate
-- confirmar - que e exatamente o que a V12 fez com as contas anteriores a ela.
INSERT INTO persons (person_id, name, email, password, phone, address,
                     creation_date, update_date, password_changed_at, email_verified_at, clinic_id)
SELECT v.vet_id, v.name, v.email, v.password, NULL, NULL,
       v.creation_date, v.update_date, v.password_changed_at, NULL, v.clinic_id
FROM vets v;

-- O CRMV era texto livre, entao a UF sai de heuristica: prefixo de duas letras
-- quando o formato permite, 'NA' quando nao. Imperfeito e assumido, no mesmo
-- espirito do backfill de especie do passo 6 - a base de dev e descartavel, e o
-- profissional corrige. O estado e INFORMADO porque nao ha integracao com
-- conselho, e VERIFICADO aqui seria mentira gravada.
INSERT INTO professional_credentials (professional_credential_id, person_id, council, uf, number,
                                      status, creation_date, update_date)
SELECT gen_random_uuid(), v.vet_id, 'CRMV',
       CASE WHEN v.crmv ~ '^[A-Za-z]{2}[-/ ]' THEN upper(left(v.crmv, 2)) ELSE 'NA' END,
       left(CASE WHEN v.crmv ~ '^[A-Za-z]{2}[-/ ]' THEN substring(v.crmv from 4) ELSE v.crmv END, 32),
       'INFORMADO', v.creation_date, v.update_date
FROM vets v
WHERE v.crmv IS NOT NULL AND btrim(v.crmv) <> '';

-- ---------------------------------------------------------------- quem apontava para vets
ALTER TABLE clinic_invites DROP CONSTRAINT fk_clinic_invites_created_by;
ALTER TABLE clinic_invites DROP CONSTRAINT fk_clinic_invites_accepted_by;
ALTER TABLE clinic_invites ADD CONSTRAINT fk_clinic_invites_created_by
    FOREIGN KEY (created_by_vet_id) REFERENCES persons (person_id);
ALTER TABLE clinic_invites ADD CONSTRAINT fk_clinic_invites_accepted_by
    FOREIGN KEY (accepted_by_vet_id) REFERENCES persons (person_id);

-- ---------------------------------------------------------------- correcao: papel vira contexto
-- Havia duas colunas exclusivas entre si, uma para tutor e outra para vet, e o
-- papel era inferido de qual estava preenchida. Passa a haver um autor - sempre
-- uma pessoa - e, ao lado, em nome de qual organizacao ela agiu.
ALTER TABLE vaccine_corrections       ADD COLUMN corrected_in_clinic_id uuid;
ALTER TABLE health_record_corrections ADD COLUMN corrected_in_clinic_id uuid;

UPDATE vaccine_corrections c
SET corrected_in_clinic_id = v.clinic_id
FROM vets v
WHERE c.corrected_by_vet_id = v.vet_id;

UPDATE health_record_corrections c
SET corrected_in_clinic_id = v.clinic_id
FROM vets v
WHERE c.corrected_by_vet_id = v.vet_id;

UPDATE vaccine_corrections
SET corrected_by_person_id = corrected_by_vet_id
WHERE corrected_by_person_id IS NULL AND corrected_by_vet_id IS NOT NULL;

UPDATE health_record_corrections
SET corrected_by_person_id = corrected_by_vet_id
WHERE corrected_by_person_id IS NULL AND corrected_by_vet_id IS NOT NULL;

-- Linha de auditoria sem autor nenhum nao deveria existir - o codigo sempre
-- preenchia uma das duas colunas. Se existir, e lixo de base descartavel, e sai
-- antes do NOT NULL em vez de travar a migration com um erro obscuro.
DELETE FROM vaccine_corrections       WHERE corrected_by_person_id IS NULL;
DELETE FROM health_record_corrections WHERE corrected_by_person_id IS NULL;

ALTER TABLE vaccine_corrections       DROP CONSTRAINT fk_vaccine_corrections_vet;
ALTER TABLE health_record_corrections DROP CONSTRAINT fk_hr_corrections_vet;
ALTER TABLE vaccine_corrections       DROP COLUMN corrected_by_vet_id;
ALTER TABLE health_record_corrections DROP COLUMN corrected_by_vet_id;

ALTER TABLE vaccine_corrections       ALTER COLUMN corrected_by_person_id SET NOT NULL;
ALTER TABLE health_record_corrections ALTER COLUMN corrected_by_person_id SET NOT NULL;

ALTER TABLE vaccine_corrections ADD CONSTRAINT fk_vaccine_corrections_clinic
    FOREIGN KEY (corrected_in_clinic_id) REFERENCES clinics (clinic_id);
ALTER TABLE health_record_corrections ADD CONSTRAINT fk_hr_corrections_clinic
    FOREIGN KEY (corrected_in_clinic_id) REFERENCES clinics (clinic_id);

-- ---------------------------------------------------------------- e a tabela some
DROP TABLE vets;
