-- Owner Table
CREATE TABLE owners (
    owner_id UUID PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    creation_date TIMESTAMP,
    update_date TIMESTAMP
);

-- Clinic Table
CREATE TABLE clinics (
    clinic_id UUID PRIMARY KEY,
    name VARCHAR(255),
    owner_vet_name VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    cnpj VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    cep VARCHAR(20),
    description TEXT,
    creation_date TIMESTAMP,
    update_date TIMESTAMP
);

-- Pet Table
CREATE TABLE pets (
    pet_id UUID PRIMARY KEY,
    name VARCHAR(255),
    type VARCHAR(50),
    breed VARCHAR(100),
    born_date DATE,
    weight DOUBLE PRECISION,
    gender VARCHAR(10),
    owner_id UUID NOT NULL,
    creation_date TIMESTAMP,
    update_date TIMESTAMP,
    CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

-- Vaccine Table
CREATE TABLE vaccines (
    vaccine_id UUID PRIMARY KEY,
    pet_id UUID NOT NULL,
    clinic_id UUID,
    vaccine_name VARCHAR(255),
    application_date DATE,
    next_dose_date DATE,
    description TEXT,
    creation_date TIMESTAMP,
    update_date TIMESTAMP,
    CONSTRAINT fk_vaccine_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_vaccine_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);

-- Health Record Table
CREATE TABLE health_records (
    health_record_id UUID PRIMARY KEY,
    pet_id UUID NOT NULL,
    clinic_id UUID,
    description TEXT,
    event_date DATE,
    event_type VARCHAR(100),
    creation_date TIMESTAMP,
    update_date TIMESTAMP,
    CONSTRAINT fk_healthrecord_pet FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_healthrecord_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id)
);