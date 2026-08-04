-- Conta de veterinario.
--
-- Tabela separada de owners de proposito. Tutor e veterinario sao papeis
-- diferentes, com dados diferentes (o vet tem CRMV e clinica) e ciclos de vida
-- diferentes. Juntar os dois numa tabela de conta generica seria mais elegante,
-- mas obrigaria a reescrever autenticacao e todas as checagens de propriedade
-- que ja existem - custo alto para um ganho que ninguem cobra ainda.
--
-- O preco dessa escolha e que a unicidade de email entre as duas tabelas nao e
-- garantida pelo banco: fica por conta do service, no cadastro.

CREATE TABLE vets (
    vet_id        uuid         NOT NULL,
    clinic_id     uuid         NOT NULL,
    name          varchar(255),
    email         varchar(255) NOT NULL,
    password      varchar(255) NOT NULL,
    crmv          varchar(255),
    creation_date timestamp,
    update_date   timestamp,
    CONSTRAINT pk_vets PRIMARY KEY (vet_id),
    CONSTRAINT uk_vets_email UNIQUE (email),
    CONSTRAINT fk_vets_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);

CREATE INDEX idx_vets_clinic ON vets (clinic_id);
