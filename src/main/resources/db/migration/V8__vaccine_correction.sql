-- Rastro de correcoes em registro de vacina.
--
-- Existe porque carteira de vacinacao e dado de saude, e dado de saude nao se
-- reescreve em silencio: quem apresenta a carteira precisa poder confiar que o
-- que esta la nao foi trocado sem deixar marca. Guarda os valores ANTERIORES,
-- entao a vacina sempre tem o estado atual e a tabela reconstroi o caminho.
--
-- Quem corrigiu vai em duas colunas separadas, e nao numa coluna generica com
-- papel: sao chaves estrangeiras para tabelas diferentes, e perder integridade
-- referencial para economizar uma coluna nao compensa.

CREATE TABLE vaccine_corrections (
    vaccine_correction_id  uuid      NOT NULL,
    vaccine_id             uuid      NOT NULL,
    corrected_by_vet_id    uuid,
    corrected_by_owner_id  uuid,
    previous_vaccine_name  varchar(255),
    previous_application_date date,
    previous_next_dose_date  date,
    previous_description   varchar(255),
    corrected_at           timestamp NOT NULL,
    CONSTRAINT pk_vaccine_corrections PRIMARY KEY (vaccine_correction_id),
    CONSTRAINT fk_vaccine_corrections_vaccine FOREIGN KEY (vaccine_id) REFERENCES vaccines (vaccine_id),
    CONSTRAINT fk_vaccine_corrections_vet FOREIGN KEY (corrected_by_vet_id) REFERENCES vets (vet_id),
    CONSTRAINT fk_vaccine_corrections_owner FOREIGN KEY (corrected_by_owner_id) REFERENCES owners (owner_id)
);

CREATE INDEX idx_vaccine_corrections_vaccine ON vaccine_corrections (vaccine_id);
