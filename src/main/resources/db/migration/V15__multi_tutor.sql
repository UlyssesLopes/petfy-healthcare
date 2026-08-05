-- Passo 8: um pet passa a ter varios tutores.
--
-- Ate aqui pets.owner_id era o dono unico. Casal e familia dividem o mesmo pet,
-- e adocao e venda nao tinham caminho nenhum - o segundo tutor so conseguia um
-- link de leitura, que expira.
--
-- A coluna pets.owner_id e REMOVIDA no fim desta migration, e nao mantida ao
-- lado da tabela nova. Com as duas, "quem e o dono deste pet" teria duas
-- respostas, e o dia em que divergissem seria um vazamento: alguem enxergando
-- pet que nao e seu, ou perdendo o proprio. O titular passa a morar em
-- pet_tutors como HOLDER, junto dos demais.

-- -------------------------------------------------------------------------
-- Vinculo entre pet e quem cuida dele
-- -------------------------------------------------------------------------
CREATE TABLE pet_tutors (
    pet_tutor_id          uuid         NOT NULL,
    pet_id                uuid         NOT NULL,
    owner_id              uuid         NOT NULL,
    -- PetTutorRole: HOLDER | EDITOR | VIEWER
    role                  varchar(16)  NOT NULL,
    -- nulo nos vinculos criados por esta migration: eram o dono unico de antes,
    -- nao ha convite atras deles
    invited_by_owner_id   uuid,
    creation_date         timestamp    NOT NULL,
    update_date           timestamp,
    CONSTRAINT pk_pet_tutors PRIMARY KEY (pet_tutor_id),
    CONSTRAINT fk_pet_tutors_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_pet_tutors_owner
        FOREIGN KEY (owner_id) REFERENCES owners (owner_id),
    CONSTRAINT fk_pet_tutors_invited_by
        FOREIGN KEY (invited_by_owner_id) REFERENCES owners (owner_id),
    -- a mesma pessoa nao entra duas vezes no mesmo pet
    CONSTRAINT uk_pet_tutors_pet_owner UNIQUE (pet_id, owner_id)
);

CREATE INDEX idx_pet_tutors_pet   ON pet_tutors (pet_id);
CREATE INDEX idx_pet_tutors_owner ON pet_tutors (owner_id);

-- Exatamente um titular por pet, garantido pelo banco e nao so pelo servico.
-- Indice parcial: a restricao vale sobre as linhas HOLDER, e os demais papeis
-- se repetem a vontade.
CREATE UNIQUE INDEX uk_pet_tutors_um_holder_por_pet
    ON pet_tutors (pet_id) WHERE role = 'HOLDER';

-- -------------------------------------------------------------------------
-- Backfill: todo dono de hoje vira o titular do seu pet
-- -------------------------------------------------------------------------
-- gen_random_uuid() vem do pgcrypto, embutido no Postgres desde a 13. A base
-- do projeto e a 14 - ver docker-local e o Testcontainers.
INSERT INTO pet_tutors (pet_tutor_id, pet_id, owner_id, role, creation_date)
SELECT gen_random_uuid(), p.pet_id, p.owner_id, 'HOLDER', COALESCE(p.creation_date, now())
FROM pets p;

-- -------------------------------------------------------------------------
-- Convite para entrar num pet
-- -------------------------------------------------------------------------
-- Mesmo desenho do clinic_invites: token como hash, uso unico, expiracao curta.
-- A diferenca e o email ser NOT NULL aqui - convite de pet da acesso a historico
-- de saude, entao link solto encaminhado por engano entrega dado pessoal.
CREATE TABLE pet_tutor_invites (
    pet_tutor_invite_id    uuid         NOT NULL,
    pet_id                 uuid         NOT NULL,
    created_by_owner_id    uuid         NOT NULL,
    token_hash             varchar(255) NOT NULL,
    email                  varchar(255) NOT NULL,
    -- papel oferecido. HOLDER significa transferencia de titularidade
    role                   varchar(16)  NOT NULL,
    expires_at             timestamp    NOT NULL,
    accepted_at            timestamp,
    accepted_by_owner_id   uuid,
    revoked_at             timestamp,
    creation_date          timestamp    NOT NULL,
    CONSTRAINT pk_pet_tutor_invites PRIMARY KEY (pet_tutor_invite_id),
    CONSTRAINT uk_pet_tutor_invites_token UNIQUE (token_hash),
    CONSTRAINT fk_pet_tutor_invites_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_pet_tutor_invites_created_by
        FOREIGN KEY (created_by_owner_id) REFERENCES owners (owner_id),
    CONSTRAINT fk_pet_tutor_invites_accepted_by
        FOREIGN KEY (accepted_by_owner_id) REFERENCES owners (owner_id)
);

CREATE INDEX idx_pet_tutor_invites_pet   ON pet_tutor_invites (pet_id);
CREATE INDEX idx_pet_tutor_invites_email ON pet_tutor_invites (email);

-- -------------------------------------------------------------------------
-- A coluna antiga sai
-- -------------------------------------------------------------------------
-- Depois do backfill, e so depois: enquanto ela existir e for lida em algum
-- lugar, ha duas fontes de verdade sobre a mesma coisa.
DROP INDEX IF EXISTS idx_pets_owner;
ALTER TABLE pets DROP CONSTRAINT fk_pets_owner;
ALTER TABLE pets DROP COLUMN owner_id;
