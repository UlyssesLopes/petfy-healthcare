-- Anexos: o arquivo que o tutor tem na mao.
--
-- Ate aqui nao havia campo de arquivo em nenhuma entidade. O que o tutor de fato
-- possui e papel - carteirinha fisica, resultado de exame em PDF, receita -, e sem
-- upload ele digitava o que estava no papel e o papel continuava sendo a fonte de
-- verdade. O produto era um caderno digital paralelo, nao substituto.
--
-- O OCR de RG animal e o exemplo do problema: ele le a imagem e a descarta.

CREATE TABLE attachments (
    attachment_id      uuid          NOT NULL,

    -- pet_id e OBRIGATORIO e faz dois trabalhos ao mesmo tempo.
    --
    -- E a ancora de autorizacao: o PetAccessGuard decide acesso por petId, e toda
    -- pergunta sobre quem pode ver este arquivo se reduz a quem pode ver este pet.
    -- E e a ancora de limpeza: o PetPurger apaga por petId, e o
    -- PetPurgerCoverageContainerTest recusa o build se esta tabela ficar de fora.
    --
    -- A alternativa era uma tabela polimorfica com (tipo_do_dono, id_do_dono), que
    -- nao tem integridade referencial e deixaria as duas perguntas sem resposta
    -- barata.
    pet_id             uuid          NOT NULL,

    -- O que o arquivo documenta. Os dois nulos significa anexo do pet em si - foto,
    -- RG animal. No maximo um dos dois, garantido por CHECK abaixo: um arquivo nao
    -- documenta uma vacina E um atendimento ao mesmo tempo, e permitir os dois faria
    -- a listagem por vacina e a por atendimento devolverem a mesma linha.
    vaccine_id         uuid,
    health_record_id   uuid,

    -- Nome que o cliente enviou, guardado para exibir e para o download devolver.
    -- NUNCA usado para montar caminho - ver storage_key.
    original_filename  varchar(255)  NOT NULL,

    -- Tipo DETECTADO pelo conteudo, e nao o declarado no upload. O cliente pode
    -- mentir no Content-Type, e um .exe renomeado para .pdf passaria.
    content_type       varchar(100)  NOT NULL,

    size_bytes         bigint        NOT NULL,

    -- SHA-256 do conteudo. Serve para conferir integridade do que voltou do storage
    -- e para reconhecer reenvio do mesmo arquivo.
    checksum_sha256    varchar(64)   NOT NULL,

    -- Caminho no storage, GERADO pelo servidor. Nenhum pedaco vem do cliente: nome
    -- de arquivo enviado por terceiro dentro de um caminho e travessia de diretorio
    -- esperando acontecer.
    storage_key        varchar(512)  NOT NULL,

    description        varchar(500),

    uploaded_by_owner_id uuid,

    creation_date      timestamp     NOT NULL,

    CONSTRAINT pk_attachments PRIMARY KEY (attachment_id),
    CONSTRAINT uk_attachments_storage_key UNIQUE (storage_key),

    CONSTRAINT fk_attachments_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    CONSTRAINT fk_attachments_vaccine
        FOREIGN KEY (vaccine_id) REFERENCES vaccines (vaccine_id),
    CONSTRAINT fk_attachments_health_record
        FOREIGN KEY (health_record_id) REFERENCES health_records (health_record_id),
    -- nulo quando quem subiu apagou a conta depois: o arquivo pertence ao pet, que
    -- sobrevive se houver outro tutor
    CONSTRAINT fk_attachments_uploaded_by
        FOREIGN KEY (uploaded_by_owner_id) REFERENCES owners (owner_id),

    -- No maximo um dono especifico. Em codigo esta regra ficaria numa validacao que
    -- alguem contorna com um insert direto; aqui o banco recusa.
    CONSTRAINT ck_attachments_um_dono_no_maximo
        CHECK (vaccine_id IS NULL OR health_record_id IS NULL)
);

CREATE INDEX idx_attachments_pet            ON attachments (pet_id, creation_date DESC);
CREATE INDEX idx_attachments_vaccine        ON attachments (vaccine_id);
CREATE INDEX idx_attachments_health_record  ON attachments (health_record_id);
