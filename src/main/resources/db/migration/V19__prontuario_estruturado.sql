-- Passo 4: o historico de saude deixa de ser uma lista de texto.
--
-- Ate aqui health_records tinha event_type e description como String livre. "Historico
-- de saude" era uma lista de texto com data e clinica: nao havia como perguntar quais
-- pets tiveram dermatite no ultimo ano, nem alertar recorrencia, nem montar uma tela que
-- um veterinario reconheca como prontuario.
--
-- E o pet era so cadastro: microchip era booleano - tem ou nao tem, sem o numero, que e
-- o identificador legal do animal - e nao havia onde registrar alergia, condicao cronica
-- nem castracao.

-- -------------------------------------------------------------------------
-- 1. Categoria do atendimento, sem perder o texto que ja existe
-- -------------------------------------------------------------------------
-- A coluna nova NAO substitui event_type. Trocar String por enum exigiria mapear todo
-- valor ja gravado, e o que nao casasse seria descartado - dado de saude que alguem
-- digitou, perdido para caber num enum. Entao event_type continua sendo o rotulo livre e
-- category passa a ser a classificacao ao lado dele.
-- Entra NOT NULL com default, e o default sai em seguida.
--
-- Nao ha backfill inteligente de proposito. A tentativa obvia seria classificar o texto
-- ja gravado por LIKE ('%cirurg%' -> CIRURGIA e assim por diante), e isso seria
-- adivinhacao sobre dado que hoje so existe em desenvolvimento e que sera zerado antes
-- de homologacao. Adivinhacao custa manutencao e nao acerta: quem digitou "Antiga" no
-- event_type nao quis dizer categoria nenhuma.
--
-- O DEFAULT e o que faz esta migration rodar tanto numa base com linhas quanto numa
-- vazia - e ela vai rodar nas duas: aqui com dado descartavel, em stg e prd no primeiro
-- boot, com a tabela vazia. Sai depois porque atendimento novo tem de declarar a
-- categoria; deixar o default de pe faria OUTRO virar o valor silencioso de quem esqueceu.
ALTER TABLE health_records ADD COLUMN category varchar(24) NOT NULL DEFAULT 'OUTRO';
ALTER TABLE health_records ALTER COLUMN category DROP DEFAULT;

-- Diagnostico como campo proprio, e nao enterrado na descricao. E o que permite depois
-- responder "o que este animal ja teve" sem alguem ler texto corrido.
ALTER TABLE health_records ADD COLUMN diagnosis varchar(500);

-- -------------------------------------------------------------------------
-- 2. O pet ganha o que um prontuario pergunta
-- -------------------------------------------------------------------------
-- O numero, e nao apenas o booleano. E o identificador legal do animal e o que liga o
-- Petfy a registro de animal perdido. A coluna antiga fica: o contrato ja publicado
-- devolve microchip como boolean, e o servico mantem os dois coerentes.
ALTER TABLE pets ADD COLUMN microchip_number varchar(32);

-- Castracao afeta protocolo vacinal, peso esperado e risco de doenca. A data importa
-- tanto quanto o fato.
ALTER TABLE pets ADD COLUMN castrated     boolean;
ALTER TABLE pets ADD COLUMN castrated_at  date;

CREATE INDEX idx_pets_microchip_number ON pets (microchip_number)
    WHERE microchip_number IS NOT NULL;

-- -------------------------------------------------------------------------
-- 3. Alergia e condicao cronica
-- -------------------------------------------------------------------------
-- Tabela, e nao campo de texto no pet. Sao duas coisas na mesma tabela porque tem a
-- mesma forma - o que a pessoa tem, desde quando, ainda vale - e a mesma razao de
-- existir: aparecer em DESTAQUE, e nao enterrado numa descricao. Alergia a anestesico
-- perdida no meio de um texto corrido e o tipo de informacao que so se descobre que
-- faltava depois.
--
-- Uma tabela com kind, e nao duas: separadas, toda tela e todo endpoint precisariam
-- consultar as duas e juntar, e a proxima categoria - intolerancia alimentar,
-- predisposicao de raca - abriria uma terceira.
CREATE TABLE pet_health_conditions (
    pet_health_condition_id  uuid          NOT NULL,
    pet_id                   uuid          NOT NULL,
    -- PetHealthConditionKind: ALERGIA | CONDICAO_CRONICA
    kind                     varchar(24)   NOT NULL,
    description              varchar(255)  NOT NULL,
    -- Gravidade, so para alergia. Nula para condicao cronica, que nao se mede assim.
    -- PetHealthConditionSeverity: LEVE | MODERADA | GRAVE
    severity                 varchar(16),
    notes                    varchar(500),
    since                    date,
    -- Encerrada em vez de apagada: condicao que passou faz parte do historico do animal,
    -- e apagar a linha esconderia que ela existiu. Nula significa ativa.
    resolved_at              date,
    creation_date            timestamp     NOT NULL,
    update_date              timestamp,
    CONSTRAINT pk_pet_health_conditions PRIMARY KEY (pet_health_condition_id),
    CONSTRAINT fk_pet_health_conditions_pet
        FOREIGN KEY (pet_id) REFERENCES pets (pet_id),
    -- gravidade so faz sentido em alergia. Em codigo a regra seria contornada por insert
    -- direto; aqui o banco recusa
    CONSTRAINT ck_pet_health_conditions_gravidade_so_em_alergia
        CHECK (severity IS NULL OR kind = 'ALERGIA')
);

CREATE INDEX idx_pet_health_conditions_pet ON pet_health_conditions (pet_id);

-- A tabela aponta para pets, entao entra no PetPurger - e o
-- PetPurgerCoverageContainerTest recusa o build se ficar de fora da lista.
