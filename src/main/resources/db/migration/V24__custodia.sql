-- P2b da Fase 6: custodia separada de acesso, e a tabela de tutores se dissolve.
--
-- O pet_tutors decidia duas coisas com o mesmo campo: quem responde pelo animal e
-- quem alcanca o registro dele. Sao opostas - a clinica le e escreve por concessao
-- e nunca vai responder pelo animal; o tutor responde e nao precisa de concessao
-- de ninguem. Misturar fazia "conceder acesso a clinica" parecer parente de
-- "transferir titularidade", e obrigaria a creche a virar co-tutora de 40 animais.
--
-- HOLDER migra para custodia. EDITOR e VIEWER migram para concessao de pessoa,
-- criada no P2a.

CREATE TABLE custodies (
    custody_id           uuid        NOT NULL,
    animal_id            uuid        NOT NULL,
    holder_person_id     uuid,
    holder_clinic_id     uuid,
    nature               varchar(16) NOT NULL,
    started_at           timestamp   NOT NULL,
    expected_end_at      timestamp,
    ended_at             timestamp,
    end_reason           varchar(24),
    successor_custody_id uuid,
    CONSTRAINT pk_custodies PRIMARY KEY (custody_id),
    CONSTRAINT fk_custodies_animal FOREIGN KEY (animal_id) REFERENCES animals (animal_id),
    CONSTRAINT fk_custodies_holder_person FOREIGN KEY (holder_person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_custodies_holder_clinic FOREIGN KEY (holder_clinic_id) REFERENCES clinics (clinic_id),
    CONSTRAINT fk_custodies_successor FOREIGN KEY (successor_custody_id) REFERENCES custodies (custody_id),

    -- Exatamente um responsavel: pessoa ou organizacao, nunca os dois nem nenhum.
    CONSTRAINT ck_custodies_um_responsavel CHECK (
        (CASE WHEN holder_person_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN holder_clinic_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    ),

    -- Encerrar exige dizer por que. Sem isto seria possivel fechar uma custodia sem
    -- deixar rastro do motivo, e a linha do tempo do animal perderia o fato.
    CONSTRAINT ck_custodies_fim_tem_motivo CHECK (
        (ended_at IS NULL AND end_reason IS NULL)
        OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)
    )

    -- O QUE NAO DEU PARA TRAZER PARA CA, e por que fica dito em voz alta.
    --
    -- O quarto invariante do produto - nenhuma custodia termina sem sucessor -
    -- nasceu aqui como CHECK, e foi removido: ele e o indice unico parcial abaixo
    -- nao podem ser satisfeitos ao mesmo tempo por nenhuma ordem de escrita.
    --
    -- Encerrar a custodia antiga exige apontar para a nova, que ainda nao existe.
    -- Criar a nova antes deixa duas em curso no mesmo instante, e o indice recusa.
    -- Nao ha ordem que passe pelos dois, e CHECK nao e deferivel no Postgres.
    --
    -- Onde a regra vive, entao: no servico, que so encerra apontando destino, e no
    -- CustodyFlowContainerTest, que exercita os cinco movimentos contra Postgres
    -- real. E mais fraco que uma constraint e esta admitido como tal - o que nao se
    -- faz e deixar um CHECK que parece garantir e nunca chega a rodar.
);

-- No maximo uma custodia em curso por animal. Indice unico PARCIAL, como o de um
-- titular por animal que ele substitui: o historico tem quantas custodias
-- encerradas quiser, e e ele que conta a vida do animal.
CREATE UNIQUE INDEX uk_custodies_uma_em_curso_por_animal
    ON custodies (animal_id) WHERE ended_at IS NULL;

CREATE INDEX idx_custodies_animal ON custodies (animal_id);
CREATE INDEX idx_custodies_holder_person ON custodies (holder_person_id);

-- ---------------------------------------------------------------- HOLDER vira custodia
-- Natureza DEFINITIVA: eram tutores comuns, sem prazo. started_at vem da data do
-- vinculo, e nao de agora - a custodia comecou quando o vinculo comecou, e datar
-- pela migration apagaria isso da linha do tempo.
INSERT INTO custodies (custody_id, animal_id, holder_person_id, nature, started_at)
SELECT t.pet_tutor_id, t.pet_id, t.owner_id, 'DEFINITIVA', t.creation_date
FROM pet_tutors t
WHERE t.role = 'HOLDER';

-- ---------------------------------------------------------------- EDITOR e VIEWER viram concessao
-- granted_by fica nulo: invited_by_owner_id e quem convidou, que nem sempre e quem
-- concedeu, e inventar essa autoria seria gravar um fato que ninguem registrou.
INSERT INTO grants (grant_id, animal_id, grantee_person_id, level, granted_at)
SELECT t.pet_tutor_id, t.pet_id, t.owner_id, t.role, t.creation_date
FROM pet_tutors t
WHERE t.role IN ('EDITOR', 'VIEWER');

-- Escopo total, e nao minimo: e o que um co-tutor sempre alcancou. Estreitar aqui
-- seria decidir retroativamente, e em silencio, que a avo que acompanha a carteira
-- na verdade nao devia ver o prontuario.
INSERT INTO grant_scopes (grant_id, scope)
SELECT t.pet_tutor_id, s.scope
FROM pet_tutors t
CROSS JOIN (VALUES ('CARTEIRA'), ('CONDICOES'), ('PRONTUARIO'),
                   ('PESO'), ('ANEXOS'), ('CONTATO')) AS s(scope)
WHERE t.role IN ('EDITOR', 'VIEWER');

-- ---------------------------------------------------------------- e a tabela some
DROP TABLE pet_tutors;
