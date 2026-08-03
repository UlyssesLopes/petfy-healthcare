-- Link de leitura da carteira de vacinacao.
--
-- Guarda o HASH do token, nunca o token. Quem tem o link acessa a carteira sem
-- autenticar, entao o token e credencial: com o hash, um vazamento do banco nao
-- entrega os links ativos. A busca funciona igual porque o hash e deterministico.
--
-- A consequencia de produto e que o token so aparece uma vez, na criacao - nao
-- da para reexibir o que nao esta guardado. Revogar e criar outro cobre o caso
-- de quem perdeu o link.

CREATE TABLE pet_shares (
    pet_share_id  uuid         NOT NULL,
    pet_id        uuid         NOT NULL,
    token_hash    varchar(255) NOT NULL,
    expires_at    timestamp    NOT NULL,
    revoked_at    timestamp,
    creation_date timestamp,
    CONSTRAINT pk_pet_shares PRIMARY KEY (pet_share_id),
    CONSTRAINT uk_pet_shares_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_pet_shares_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id)
);

CREATE INDEX idx_pet_shares_pet ON pet_shares (pet_id);
