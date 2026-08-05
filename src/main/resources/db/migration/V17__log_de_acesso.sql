-- Log de acesso a dado de saude.
--
-- Ate aqui havia rastro de ESCRITA - vaccine_corrections e health_record_corrections
-- registram quem alterou o que. Nao havia rastro de LEITURA: um veterinario podia
-- abrir o historico completo de um pet e o tutor nunca saberia. Para dado de saude,
-- quem leu e tao relevante quanto quem escreveu.
--
-- Registra apenas acesso de TERCEIRO: veterinario de clinica autorizada e link
-- publico de carteira. Leitura do proprio tutor e dos co-tutores nao entra, e a
-- omissao e deliberada:
--
--   * o tutor abrindo o proprio pet gera volume enorme sem informacao nenhuma - o
--     log existe para responder "quem MAIS viu isto", e a resposta nao inclui quem
--     pergunta;
--   * o co-tutor nao e terceiro. Ele responde pelo animal junto, e a V15 fez disso
--     uma relacao explicita. Registrar cada leitura dele transformaria cuidado
--     compartilhado em vigilancia mutua.
--
-- Se um dia valer registrar acesso de co-tutor, o actor_type ja tem espaco para
-- OWNER e o unico trabalho e o gancho na leitura.

CREATE TABLE sensitive_access_log (
    sensitive_access_log_id  uuid         NOT NULL,
    pet_id                   uuid         NOT NULL,
    -- AccessActorType: VET | SHARE_LINK
    actor_type               varchar(16)  NOT NULL,
    -- nulo para SHARE_LINK: quem abre o link nao tem conta
    actor_id                 uuid,
    -- Nome no momento do acesso, e nao chave estrangeira.
    --
    -- Snapshot de proposito: o veterinario pode fechar a conta depois, e o registro
    -- de que ele leu o historico nao pode virar uma linha sem nome. Uma FK para vets
    -- tambem faria a exclusao de conta de veterinario esbarrar neste log - a mesma
    -- familia de bug que travou o DELETE /owners/me duas vezes.
    actor_name               varchar(255),
    -- Qual clinica, porque e o que o tutor autorizou: ele concedeu acesso a uma
    -- clinica, nao a uma pessoa
    clinic_name              varchar(255),
    -- AccessedResource: VACCINES | HEALTH_RECORDS | VACCINE_CORRECTIONS |
    --                   HEALTH_RECORD_CORRECTIONS | SHARED_CARD
    resource                 varchar(32)  NOT NULL,
    accessed_at              timestamp    NOT NULL,
    ip_address               varchar(45),
    user_agent               varchar(512),
    CONSTRAINT pk_sensitive_access_log PRIMARY KEY (sensitive_access_log_id),
    CONSTRAINT fk_sensitive_access_log_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id)
);

-- A consulta e sempre "os acessos deste pet, do mais recente para o mais antigo",
-- entao o indice cobre as duas colunas na ordem em que sao usadas
CREATE INDEX idx_sensitive_access_log_pet_accessed
    ON sensitive_access_log (pet_id, accessed_at DESC);

-- A FK para pets significa que o log morre com o pet, e isso e coerente: ele existe
-- para o tutor poder perguntar quem viu o pet dele, e pet apagado nao tem tutor a
-- quem responder. A limpeza fica no PetPurger, e o PetPurgerCoverageContainerTest
-- recusa o build se esta tabela ficar fora da lista.
