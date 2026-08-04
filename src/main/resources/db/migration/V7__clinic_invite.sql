-- Convite para entrar numa clinica.
--
-- Ate aqui bastava saber o clinicId - que aparece em qualquer listagem publica -
-- para se cadastrar como veterinario de qualquer clinica. Era o buraco mais
-- serio do modelo de vet, porque quem entra na clinica alcanca todos os pets que
-- ela ja foi autorizada a atender.
--
-- Agora entrar exige convite emitido por quem ja esta la. Nao substitui
-- validacao de CRMV, que dependeria de integrar com registro externo, mas troca
-- "qualquer um entra" por "alguem de dentro respondeu por essa pessoa".

CREATE TABLE clinic_invites (
    clinic_invite_id   uuid         NOT NULL,
    clinic_id          uuid         NOT NULL,
    created_by_vet_id  uuid         NOT NULL,
    token_hash         varchar(255) NOT NULL,
    email              varchar(255),
    expires_at         timestamp    NOT NULL,
    accepted_at        timestamp,
    accepted_by_vet_id uuid,
    revoked_at         timestamp,
    creation_date      timestamp,
    CONSTRAINT pk_clinic_invites PRIMARY KEY (clinic_invite_id),
    CONSTRAINT uk_clinic_invites_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_clinic_invites_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id),
    CONSTRAINT fk_clinic_invites_created_by FOREIGN KEY (created_by_vet_id) REFERENCES vets (vet_id),
    CONSTRAINT fk_clinic_invites_accepted_by FOREIGN KEY (accepted_by_vet_id) REFERENCES vets (vet_id)
);

CREATE INDEX idx_clinic_invites_clinic ON clinic_invites (clinic_id);
