-- Schema inicial do Petfy HealthCare.
--
-- Reflete o mapeamento das entidades como ele esta hoje. Os tipos foram tirados
-- do DDL que o proprio Hibernate gera para o dialeto Postgres, para que o
-- ddl-auto=validate passe na subida da aplicacao.

CREATE TABLE owners (
    owner_id      uuid         NOT NULL,
    name          varchar(255),
    email         varchar(255) NOT NULL,
    password      varchar(255) NOT NULL,
    phone         varchar(255),
    address       varchar(255),
    creation_date timestamp,
    update_date   timestamp,
    CONSTRAINT pk_owners PRIMARY KEY (owner_id),
    CONSTRAINT uk_owners_email UNIQUE (email)
);

CREATE TABLE clinics (
    clinic_id      uuid NOT NULL,
    name           varchar(255),
    owner_vet_name varchar(255),
    phone          varchar(255),
    email          varchar(255),
    cnpj           varchar(255),
    address        varchar(255),
    city           varchar(255),
    state          varchar(255),
    cep            varchar(255),
    description    varchar(255),
    creation_date  timestamp,
    update_date    timestamp,
    CONSTRAINT pk_clinics PRIMARY KEY (clinic_id)
);

CREATE TABLE pets (
    pet_id           uuid NOT NULL,
    name             varchar(255),
    type             varchar(255),
    breed            varchar(255),
    born_date        date,
    born_local       varchar(255),
    color            varchar(255),
    general_registry varchar(255),
    microchip        boolean,
    weight           float8,
    gender           varchar(255),
    owner_id         uuid NOT NULL,
    creation_date    timestamp,
    update_date      timestamp,
    CONSTRAINT pk_pets PRIMARY KEY (pet_id),
    CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

CREATE TABLE vaccines (
    vaccine_id       uuid NOT NULL,
    vaccine_name     varchar(255),
    application_date date,
    next_dose_date   date,
    description      varchar(255),
    pet_id           uuid NOT NULL,
    clinic_id        uuid,
    creation_date    timestamp,
    update_date      timestamp,
    CONSTRAINT pk_vaccines PRIMARY KEY (vaccine_id),
    CONSTRAINT fk_vaccines_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_vaccines_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);

CREATE TABLE health_records (
    health_record_id uuid NOT NULL,
    event_type       varchar(255),
    event_date       date,
    description      varchar(255),
    pet_id           uuid NOT NULL,
    clinic_id        uuid,
    creation_date    timestamp,
    update_date      timestamp,
    CONSTRAINT pk_health_records PRIMARY KEY (health_record_id),
    CONSTRAINT fk_health_records_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_health_records_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);

-- as chaves estrangeiras nao ganham indice automatico no Postgres, e as
-- leituras do dominio sao justamente por dono, por pet e por clinica
CREATE INDEX idx_pets_owner ON pets (owner_id);
CREATE INDEX idx_vaccines_pet ON vaccines (pet_id);
CREATE INDEX idx_vaccines_clinic ON vaccines (clinic_id);
CREATE INDEX idx_health_records_pet ON health_records (pet_id);
CREATE INDEX idx_health_records_clinic ON health_records (clinic_id);
