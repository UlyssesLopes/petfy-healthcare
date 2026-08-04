-- Autorizacao do tutor para uma clinica acessar o pet.
--
-- E o portao que torna seguro o veterinario escrever no historico de um pet que
-- nao e dele: sem concessao explicita do tutor, nenhum vet alcanca o pet. Isso
-- vale mesmo sem verificacao de identidade profissional - alguem que se cadastre
-- como vet de uma clinica so chega aos pets que aquela clinica ja foi autorizada
-- a atender.
--
-- A concessao e por CLINICA e nao por veterinario: quem atende hoje pode nao ser
-- quem atende no retorno, e o tutor pensa em "levei o Rex na Bicho Feliz", nao
-- em qual profissional o recebeu.

CREATE TABLE pet_clinic_access (
    pet_clinic_access_id uuid      NOT NULL,
    pet_id               uuid      NOT NULL,
    clinic_id            uuid      NOT NULL,
    granted_at           timestamp NOT NULL,
    revoked_at           timestamp,
    CONSTRAINT pk_pet_clinic_access PRIMARY KEY (pet_clinic_access_id),
    -- uma linha por par: reconceder reativa a existente em vez de acumular
    -- historico, que ninguem pediu ainda
    CONSTRAINT uk_pet_clinic_access UNIQUE (pet_id, clinic_id),
    CONSTRAINT fk_pet_clinic_access_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_pet_clinic_access_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);

CREATE INDEX idx_pet_clinic_access_clinic ON pet_clinic_access (clinic_id);
