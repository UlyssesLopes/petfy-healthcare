-- P2a da Fase 6: acesso vira conceito proprio, e ganha escopo.
--
-- Ate aqui havia duas formas de um terceiro alcancar um animal, cada uma com
-- tabela propria e regra propria: pet_clinic_access, para a clinica autorizada, e
-- animal_shares, para o link publico de carteira. As duas respondiam a mesma
-- pergunta - quem, alem de quem responde pelo animal, alcanca o registro dele - e
-- nenhuma das duas sabia dizer "o quanto".
--
-- Escopo e o que faltava. Sem ele, matricular o cachorro numa creche entrega a
-- ela o historico completo de doencas do animal, e o cartao de emergencia da
-- decisao 13 seria o prontuario inteiro dentro de um link.
--
-- A custodia continua no pet_tutors ate o P2b. Este passo nao encosta no
-- AnimalAccessGuard, que e a peca mais sensivel do sistema.

CREATE TABLE grants (
    grant_id             uuid        NOT NULL,
    animal_id            uuid        NOT NULL,
    grantee_person_id    uuid,
    grantee_clinic_id    uuid,
    token_hash           varchar(255),
    level                varchar(16) NOT NULL,
    granted_by_person_id uuid,
    granted_at           timestamp   NOT NULL,
    expires_at           timestamp,
    revoked_at           timestamp,
    CONSTRAINT pk_grants PRIMARY KEY (grant_id),
    CONSTRAINT uk_grants_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_grants_animal FOREIGN KEY (animal_id) REFERENCES animals (animal_id),
    CONSTRAINT fk_grants_grantee_person FOREIGN KEY (grantee_person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_grants_grantee_clinic FOREIGN KEY (grantee_clinic_id) REFERENCES clinics (clinic_id),
    CONSTRAINT fk_grants_granted_by FOREIGN KEY (granted_by_person_id) REFERENCES persons (person_id),
    -- Exatamente um beneficiario por linha. E CHECK no banco, e nao so regra de
    -- servico, pelo mesmo motivo da gravidade de alergia no passo 12: o banco
    -- impede insert direto, o servico impede que o cliente receba erro de
    -- integridade como 500. Duas linhas de defesa para a mesma regra.
    CONSTRAINT ck_grants_um_beneficiario CHECK (
        (CASE WHEN grantee_person_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN grantee_clinic_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN token_hash        IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

CREATE INDEX idx_grants_animal ON grants (animal_id);
CREATE INDEX idx_grants_grantee_clinic ON grants (grantee_clinic_id);
CREATE INDEX idx_grants_grantee_person ON grants (grantee_person_id);

-- Escopo e conjunto, e nao coluna: um acesso alcanca varias coisas, e guardar
-- isso como texto separado por virgula transformaria toda consulta em LIKE.
CREATE TABLE grant_scopes (
    grant_id uuid        NOT NULL,
    scope    varchar(24) NOT NULL,
    CONSTRAINT pk_grant_scopes PRIMARY KEY (grant_id, scope),
    CONSTRAINT fk_grant_scopes_grant FOREIGN KEY (grant_id) REFERENCES grants (grant_id)
);

-- ---------------------------------------------------------------- acesso de clinica
-- Nivel EDITOR porque e o que a clinica ja fazia: registrar vacina e atendimento.
INSERT INTO grants (grant_id, animal_id, grantee_clinic_id, level,
                    granted_at, revoked_at)
SELECT a.pet_clinic_access_id, a.pet_id, a.clinic_id, 'EDITOR',
       a.granted_at, a.revoked_at
FROM pet_clinic_access a;

-- O escopo do backfill e TOTAL, e isso e deliberado: e exatamente o que essas
-- concessoes significavam quando foram dadas. Estreitar agora seria mudar
-- retroativamente o que o tutor autorizou - decidir por ele, em silencio, que ele
-- quis conceder menos. Quem quiser menos revoga e concede de novo.
INSERT INTO grant_scopes (grant_id, scope)
SELECT a.pet_clinic_access_id, s.scope
FROM pet_clinic_access a
CROSS JOIN (VALUES ('CARTEIRA'), ('CONDICOES'), ('PRONTUARIO'),
                   ('PESO'), ('ANEXOS'), ('CONTATO')) AS s(scope);

-- ---------------------------------------------------------------- link de carteira
INSERT INTO grants (grant_id, animal_id, token_hash, level,
                    granted_at, expires_at, revoked_at)
SELECT s.animal_share_id, s.animal_id, s.token_hash, 'VIEWER',
       COALESCE(s.creation_date, s.expires_at), s.expires_at, s.revoked_at
FROM animal_shares s;

-- Só CARTEIRA, porque e literalmente o que o link mostra hoje: o historico de
-- saude fica de fora de proposito - quem pede a carteira precisa saber se as
-- vacinas estao em dia, nao que o animal fez uma cirurgia. O backfill preserva
-- isso em vez de aproveitar a migration para alargar o que ja estava concedido.
INSERT INTO grant_scopes (grant_id, scope)
SELECT s.animal_share_id, 'CARTEIRA'
FROM animal_shares s;

-- ---------------------------------------------------------------- e as duas somem
DROP TABLE pet_clinic_access;
DROP TABLE animal_shares;
