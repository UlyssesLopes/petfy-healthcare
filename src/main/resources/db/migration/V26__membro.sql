-- P3b da Fase 6: uma pessoa esta em N organizacoes, e o vinculo singular acaba.
--
-- persons.organization_id era o mesmo campo desde antes de existir Person: um
-- @ManyToOne obrigatorio em vets, que fazia o veterinario em duas clinicas precisar
-- de duas contas - com dois e-mails, o que o namespace unico impedia. Era a divida
-- registrada em "Ainda nao levantado com voce".
--
-- E organizacao continua sendo OPCIONAL para atuar: zero vinculos e um estado
-- legitimo, e nao um cadastro incompleto. O veterinario autonomo e uma pessoa com
-- credencial que recebe acesso direto de quem tem custodia.

CREATE TABLE memberships (
    membership_id   uuid        NOT NULL,
    person_id       uuid        NOT NULL,
    organization_id uuid        NOT NULL,
    role            varchar(24) NOT NULL,
    joined_at       timestamp   NOT NULL,
    left_at         timestamp,
    CONSTRAINT pk_memberships PRIMARY KEY (membership_id),
    CONSTRAINT fk_memberships_person FOREIGN KEY (person_id) REFERENCES persons (person_id),
    CONSTRAINT fk_memberships_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
);

-- A mesma pessoa nao entra duas vezes na mesma organizacao <b>ao mesmo tempo</b>.
-- Indice PARCIAL, e nao unico simples: sair e voltar e historia normal - quem
-- trabalhou numa clinica, saiu e voltou dois anos depois tem dois vinculos, e o
-- encerrado e biografia.
CREATE UNIQUE INDEX uk_memberships_um_ativo_por_par
    ON memberships (person_id, organization_id) WHERE left_at IS NULL;

CREATE INDEX idx_memberships_person ON memberships (person_id);
CREATE INDEX idx_memberships_organization ON memberships (organization_id);

-- ---------------------------------------------------------------- o vinculo singular vira membro
-- Funcao VETERINARIO para quem tem credencial, ADMINISTRADOR para quem nao tem: todo
-- vinculo existente nasceu do cadastro de vet, mas nem todo trouxe CRMV preenchido -
-- o campo era opcional. Chamar de veterinario quem nao tem registro seria gravar um
-- fato que ninguem afirmou.
--
-- joined_at vem da criacao da conta, e nao de agora: a pessoa entrou na organizacao
-- quando a conta foi criada, e datar pela migration apagaria isso.
INSERT INTO memberships (membership_id, person_id, organization_id, role, joined_at)
SELECT gen_random_uuid(), p.person_id, p.organization_id,
       CASE WHEN EXISTS (SELECT 1 FROM professional_credentials c
                          WHERE c.person_id = p.person_id
                            AND c.status <> 'SUSPENSO')
            THEN 'VETERINARIO' ELSE 'ADMINISTRADOR' END,
       COALESCE(p.creation_date, now())
FROM persons p
WHERE p.organization_id IS NOT NULL;

-- ---------------------------------------------------------------- e a coluna some
DROP INDEX IF EXISTS idx_persons_organization;
ALTER TABLE persons DROP CONSTRAINT fk_persons_organization;
ALTER TABLE persons DROP COLUMN organization_id;
