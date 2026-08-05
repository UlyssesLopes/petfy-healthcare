-- Registro de consentimento.
--
-- A LGPD nao pede so que o titular possa sair (passo 10, feito) - pede que a base
-- legal do tratamento seja **registrada e demonstravel**. Ate aqui nao havia nada:
-- nenhum aceite, nenhuma versao de politica, nenhum instante. Na pratica a
-- aplicacao tratava dado de saude sem poder provar que alguem consentiu.
--
-- Guarda a VERSAO do documento, e nao um booleano. Politica de privacidade muda; um
-- "aceitou = true" de 2026 nao diz com o que a pessoa concordou depois da mudanca, e
-- e justamente isso que uma auditoria pergunta.

CREATE TABLE consent_records (
    consent_record_id  uuid         NOT NULL,
    owner_id           uuid         NOT NULL,
    -- ConsentDocument: TERMS_OF_SERVICE | PRIVACY_POLICY
    document           varchar(32)  NOT NULL,
    document_version   varchar(32)  NOT NULL,
    accepted_at        timestamp    NOT NULL,
    -- Evidencia do aceite. E dado pessoal, entao sai junto com a conta na exclusao:
    -- guardar prova de consentimento de quem pediu para ser esquecido inverteria o
    -- proposito da prova. Nao ha o que demonstrar sobre um titular que nao existe.
    -- varchar(45) cabe IPv6 em forma textual completa.
    ip_address         varchar(45),
    user_agent         varchar(512),
    CONSTRAINT pk_consent_records PRIMARY KEY (consent_record_id),
    CONSTRAINT fk_consent_records_owner
        FOREIGN KEY (owner_id) REFERENCES owners (owner_id),
    -- aceitar a mesma versao duas vezes nao cria duas linhas: o primeiro aceite e o
    -- que vale, e o servico trata a repeticao como no-op
    CONSTRAINT uk_consent_records_owner_document_version UNIQUE (owner_id, document, document_version)
);

CREATE INDEX idx_consent_records_owner ON consent_records (owner_id);

-- Nao ha backfill, e isso e deliberado.
--
-- Nenhuma migration pode inventar consentimento: quem se cadastrou antes desta
-- tabela nunca foi perguntado, e escrever uma linha dizendo que foi seria fabricar a
-- prova que a tabela existe para guardar. Essas contas aparecem como pendentes em
-- GET /consents/me, e o aceite acontece quando a pessoa voltar.
