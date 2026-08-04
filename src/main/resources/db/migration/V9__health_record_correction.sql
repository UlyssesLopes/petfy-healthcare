-- Rastro de correcoes em registro de historico de saude.
--
-- Espelha vaccine_corrections pelo mesmo motivo: dado de saude nao se reescreve
-- em silencio. Vale ainda mais aqui - consulta e cirurgia sao registros mais
-- clinicos que uma dose de vacina.
--
-- Tabela propria em vez de uma generica com tipo de entidade: mantem chave
-- estrangeira de verdade para health_records, o que uma coluna polimorfica nao
-- permitiria.

CREATE TABLE health_record_corrections (
    health_record_correction_id uuid      NOT NULL,
    health_record_id            uuid      NOT NULL,
    corrected_by_vet_id         uuid,
    corrected_by_owner_id       uuid,
    previous_event_type         varchar(255),
    previous_event_date         date,
    previous_description        varchar(255),
    corrected_at                timestamp NOT NULL,
    CONSTRAINT pk_health_record_corrections PRIMARY KEY (health_record_correction_id),
    CONSTRAINT fk_hr_corrections_record FOREIGN KEY (health_record_id) REFERENCES health_records (health_record_id),
    CONSTRAINT fk_hr_corrections_vet FOREIGN KEY (corrected_by_vet_id) REFERENCES vets (vet_id),
    CONSTRAINT fk_hr_corrections_owner FOREIGN KEY (corrected_by_owner_id) REFERENCES owners (owner_id)
);

CREATE INDEX idx_hr_corrections_record ON health_record_corrections (health_record_id);
