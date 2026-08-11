-- Turma, matricula e presenca: a operacao da creche.
--
-- A `GERIR_TURMA_E_VAGA` era uma capacidade declarada e vazia desde o P3 — a organizacao
-- podia dizer que geria turma, e nao havia turma. Esta migracao e o que estava atras dela, e
-- ela existe para duas telas da entrega:
--
--   * Tela 10 · "Matricula na creche — o produto responde pela saude". A creche nao decide se
--     o animal esta apto: o PRODUTO responde, comparando o que a organizacao exige com o que a
--     carteira do animal tem.
--   * Tela 17 · "Segunda-feira, 7h30 — a operacao do dia", que o desenho chama de "a tela mais
--     usada da creche".
--
-- ------------------------------------------------ tres tabelas, e por que nao duas nem quatro
--
-- TURMA e MATRICULA sao separadas porque vaga e um limite da turma, e nao do animal: "Turma
-- Tarde · 12 de 15 vagas" e uma propriedade do grupo. E PRESENCA e separada da matricula porque
-- o animal matriculado falta — juntar as duas faria "matriculado" e "esta aqui hoje" virarem a
-- mesma coisa, que e o erro que a Tela 17 existe para nao cometer.
--
-- Nao ha tabela de "aptidao": ela e CALCULADA a cada leitura, comparando exigencia com
-- carteira. Guardar o resultado criaria uma quarta verdade que envelhece — e a frase da Tela 10
-- e explicita sobre isso: "a matricula fica guardada e se completa sozinha assim que a dose for
-- registrada". Algo que se completa sozinho nao pode depender de alguem lembrar de recalcular.

-- --------------------------------------------------------------------------------- turma
CREATE TABLE class_groups (
    class_group_id  uuid         NOT NULL,
    organization_id uuid         NOT NULL,
    name            varchar(120) NOT NULL,

    -- O limite de vagas. Nulo e "sem limite declarado": hospedagem costuma nao ter, e o
    -- desenho mostra "Hospedagem" como uma aba ao lado das turmas.
    capacity        integer,

    -- Turma encerrada nao apaga: as matriculas dela sao historia do animal.
    ended_at        timestamp,
    creation_date   timestamp    NOT NULL,

    CONSTRAINT pk_class_groups PRIMARY KEY (class_group_id),
    CONSTRAINT fk_class_groups_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT ck_class_groups_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX ix_class_groups_organization ON class_groups (organization_id) WHERE ended_at IS NULL;

-- Nome unico por organizacao enquanto a turma existe: duas "Turma Tarde" na mesma creche e
-- erro de digitacao, e quem paga e a monitora que marca entrada na turma errada.
CREATE UNIQUE INDEX ux_class_groups_nome_vigente
    ON class_groups (organization_id, lower(name)) WHERE ended_at IS NULL;

-- ----------------------------------------------------------------------------- matricula
CREATE TABLE enrollments (
    enrollment_id         uuid        NOT NULL,
    animal_id             uuid        NOT NULL,
    class_group_id        uuid        NOT NULL,

    -- PENDENTE, ATIVA ou ENCERRADA. A pendente e a que a Tela 10 desenha: ela existe, esta
    -- guardada, e vira ativa quando a comprovacao de saude fecha.
    status                varchar(20) NOT NULL,

    requested_at          timestamp   NOT NULL,
    activated_at          timestamp,
    ended_at              timestamp,

    -- Quem matriculou, e em nome de que organizacao: o nucleo de evento do P4 vale aqui
    -- tambem. Matricula e um ato de alguem, nao um estado que aparece.
    created_by_person_id  uuid,

    CONSTRAINT pk_enrollments PRIMARY KEY (enrollment_id),
    CONSTRAINT fk_enrollments_animal FOREIGN KEY (animal_id) REFERENCES animals (animal_id),
    CONSTRAINT fk_enrollments_class_group FOREIGN KEY (class_group_id)
        REFERENCES class_groups (class_group_id),
    CONSTRAINT fk_enrollments_created_by FOREIGN KEY (created_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT ck_enrollments_status CHECK (status IN ('PENDENTE', 'ATIVA', 'ENCERRADA'))
);

-- Uma matricula viva por animal e turma. O indice e PARCIAL — o animal que saiu da creche e
-- voltou dois anos depois tem duas matriculas na mesma turma, e as duas sao verdade.
CREATE UNIQUE INDEX ux_enrollments_viva
    ON enrollments (animal_id, class_group_id) WHERE ended_at IS NULL;

CREATE INDEX ix_enrollments_turma ON enrollments (class_group_id) WHERE ended_at IS NULL;

-- ------------------------------------------------------------------------------ presenca
CREATE TABLE attendances (
    attendance_id        uuid        NOT NULL,
    enrollment_id        uuid        NOT NULL,

    -- O DIA, e nao o instante: a operacao pergunta "quem vem hoje", e a chave natural do
    -- registro e a data. Os horarios de entrada e saida sao dados dentro dele.
    day                  date        NOT NULL,

    -- ESPERADO, PRESENTE, SAIU ou FALTA. "ESPERADO" e o estado inicial de quem tem matricula
    -- ativa e ainda nao chegou — e por isso a Tela 17 sabe dizer "14 esperados · 6 ja chegaram"
    -- sem ninguem ter marcado nada.
    status               varchar(20) NOT NULL,

    checked_in_at        timestamp,
    checked_out_at       timestamp,

    -- "Sai as 15h, com a avo": quem busca, quando nao e quem costuma buscar. Texto livre de
    -- proposito — a avo nao tem conta no Petfy e nao deveria precisar de uma.
    pickup_note          varchar(200),

    recorded_by_person_id uuid,
    creation_date         timestamp  NOT NULL,

    CONSTRAINT pk_attendances PRIMARY KEY (attendance_id),
    CONSTRAINT fk_attendances_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments (enrollment_id),
    CONSTRAINT fk_attendances_recorded_by FOREIGN KEY (recorded_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT ck_attendances_status CHECK (status IN ('ESPERADO', 'PRESENTE', 'SAIU', 'FALTA')),

    -- Saida sem entrada nao existe, e a checagem mora no banco porque este e o invariante que
    -- uma tela apressada quebra: dois cliques em "marcar saida" sem ninguem ter chegado.
    CONSTRAINT ck_attendances_saida_exige_entrada
        CHECK (checked_out_at IS NULL OR checked_in_at IS NOT NULL)
);

-- Um registro por matricula e dia. Marcar entrada duas vezes e o gesto mais provavel numa
-- manha de creche, e o indice e o que faz o segundo clique nao virar um segundo registro.
CREATE UNIQUE INDEX ux_attendances_dia ON attendances (enrollment_id, day);

CREATE INDEX ix_attendances_dia ON attendances (day);

-- ------------------------------------------------- o que a organizacao exige da carteira
--
-- A frase que esta tabela sustenta e da Tela 10: "Gripe canina — sem registro. NAO E EXIGIDA
-- pela Creche Quintal". Sem ela, o produto teria de escolher entre exigir tudo do catalogo (e
-- barrar animal saudavel por uma vacina que aquela creche nao pede) ou nao exigir nada (e
-- deixar entrar animal com antirrabica vencida, que e o risco que a creche corre por lei).
--
-- E exigencia por ORGANIZACAO, e nao por turma: a creche exige o que exige de quem entra na
-- porta dela, e nao um conjunto diferente por horario.
CREATE TABLE organization_vaccine_requirements (
    requirement_id     uuid      NOT NULL,
    organization_id    uuid      NOT NULL,
    vaccine_catalog_id uuid      NOT NULL,
    creation_date      timestamp NOT NULL,

    CONSTRAINT pk_organization_vaccine_requirements PRIMARY KEY (requirement_id),
    CONSTRAINT fk_ovr_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_ovr_catalog FOREIGN KEY (vaccine_catalog_id)
        REFERENCES vaccine_catalog (vaccine_catalog_id),
    CONSTRAINT ux_ovr_organizacao_vacina UNIQUE (organization_id, vaccine_catalog_id)
);
