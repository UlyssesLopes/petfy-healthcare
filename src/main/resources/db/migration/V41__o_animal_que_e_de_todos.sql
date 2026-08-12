-- O animal comunitario: a colonia da praca, o gato do predio (Telas 43 e 44).
--
-- <b>NAO HA MODELO NOVO DE CUSTODIA AQUI, e essa e a noticia.</b> "O Petfy nunca modelou
-- propriedade: modelou custodia, que passa de mao em mao. Um grupo de vizinhos e so mais um tipo
-- de custodia, e a arquitetura ja sabia disso desde o resgate do Teco." O grupo e uma
-- `organizations` com `DETER_CUSTODIA` — informal, sem CNPJ —, e a ONG que banca a castracao
-- entra como uma SEGUNDA organizacao, com concessao de leitura. Nenhuma das duas e dona.
--
-- O que falta, e o que esta migracao traz, sao quatro coisas que a colonia tem e a casa nao:
-- o animal SOME, ninguem manda sozinho, quem cuida faz turnos, e a castracao se marca antes de
-- acontecer.

-- =============================================================== 1. o avistamento
--
-- <b>"Visto por ultimo" e o sinal vital daqui, e o desenho diz por que:</b> "um gato de rua nao
-- falta a creche nem deixa de comer em casa: ele some. Marcar que voce viu o animal hoje e o
-- gesto mais frequente desta tela, e e o que permite ao grupo perceber, em vez de descobrir
-- tarde, que alguem desapareceu."
--
-- ---------------------------------------------------- por que nao e uma `observation`
--
-- A tentacao e obvia: observacao ja e "o que alguem viu", ja tem autoria e ja entra na linha do
-- tempo. E ela nao serve, por duas razoes.
--
-- A primeira e de VOLUME. Este registro acontece todo dia, por seis pessoas, em catorze gatos —
-- e a linha do tempo do animal viraria uma coluna de "vi o gato" que enterra a ferida no pescoco
-- registrada na terca. A observacao existe para o que MUDOU; o avistamento e o contrario: ele
-- vale justamente quando nada mudou.
--
-- A segunda e de PERGUNTA. O que a tela precisa saber e "quantos dias desde o ultimo", e isso e
-- um `max(seen_on)` por animal. Sobre `observations` seria um filtro por texto — o LIKE que este
-- projeto recusa desde os dias da semana.
CREATE TABLE animal_sightings (
    animal_sighting_id     uuid      NOT NULL,
    animal_id              uuid      NOT NULL,

    -- O DIA, e nao o instante. Quem alimenta a colonia as 7h nao registra as 7h: registra quando
    -- senta, a noite. Guardar hora daria precisao que o dado nao tem, e faria "visto hoje" e
    -- "visto ontem a noite" parecerem distantes.
    seen_on                date      NOT NULL,

    -- Quem viu. Sem isto a tela nao pode dizer "hoje, por Sandra" — e esse nome e o que faz o
    -- grupo saber que alguem esteve la, e nao so que o gato estava.
    recorded_by_person_id  uuid      NOT NULL,

    -- Em nome de que grupo. NAO E redundante com o animal: um gato pode ser avistado por quem
    -- nao e do grupo, e a coluna diz de qual colonia aquele avistamento fala.
    organization_id        uuid,

    recorded_at            timestamp NOT NULL,

    CONSTRAINT pk_animal_sightings PRIMARY KEY (animal_sighting_id),
    CONSTRAINT fk_animal_sightings_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_animal_sightings_person FOREIGN KEY (recorded_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_animal_sightings_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id)
);

-- UM AVISTAMENTO POR PESSOA, POR ANIMAL, POR DIA.
--
-- Nao e higiene de dados: e o gesto. Sandra passa na praca de manha e a noite, e toca duas vezes
-- no mesmo botao — a segunda nao pode virar linha nova. Marta vendo o mesmo gato no mesmo dia E
-- informacao (duas pessoas confirmam), e por isso a pessoa entra na chave.
CREATE UNIQUE INDEX ux_animal_sightings_do_dia
    ON animal_sightings (animal_id, recorded_by_person_id, seen_on);

-- A consulta da tela e "qual foi o ultimo, por animal", e ela roda para catorze gatos de uma vez.
CREATE INDEX ix_animal_sightings_ultimo ON animal_sightings (animal_id, seen_on DESC);

-- =============================================================== 2. o acordo de duas pessoas
--
-- <b>"Sem dono, a protecao contra o gesto irreversivel de uma pessoa so e o acordo de duas."</b>
--
-- Num animal com tutor, o irreversivel e barrado por quem responde: o `requireCustodia` pergunta
-- "e voce?" e acabou. Na colonia essa pergunta nao tem resposta — seis pessoas respondem juntas,
-- e qualquer uma delas passa no `requireCustodia` pela organizacao. Sem esta tabela, UMA pessoa
-- daria um gato para adocao, encerraria uma linha do tempo ou tiraria outra do grupo sozinha.
--
-- ------------------------------------------------- por que nao e o `animal_merge_requests`
--
-- Os dois sao "pedido que espera decisao", e param aqui. No merge, quem decide e determinado:
-- quem RESPONDE pelo animal. Aqui quem decide e indeterminado de proposito — <b>qualquer outra
-- pessoa do grupo serve</b>, e essa indeterminacao e o mecanismo, nao um detalhe. Uma coluna
-- `decided_by` apontando para pessoa fixa mudaria o significado: viraria "peca a Marta", e o
-- gesto pararia no dia em que a Marta viajasse.
CREATE TABLE group_approvals (
    group_approval_id      uuid         NOT NULL,

    -- O grupo em cujo nome o ato aconteceria. E ele que define quem pode concordar.
    organization_id        uuid         NOT NULL,

    -- ADOCAO, OBITO ou REMOCAO_DE_MEMBRO. Os tres que o desenho lista como "o que precisa de
    -- mais de uma pessoa" — e a lista e fechada de proposito: marcar avistamento, registrar
    -- ferida e levar ao veterinario sao de qualquer um, e travar isso mataria a tela.
    kind                   varchar(24)  NOT NULL,

    -- Sobre qual animal. Nulo em REMOCAO_DE_MEMBRO, que fala de uma pessoa.
    animal_id              uuid,

    -- Sobre qual pessoa. Nulo nos dois atos que falam de animal.
    target_person_id       uuid,

    -- Para quem o animal iria, na adocao. Nulo nos outros dois.
    to_person_id           uuid,

    -- O porque, escrito por quem pediu. Quem concorda le isto e nada mais — um pedido que so diz
    -- "concorde" nao da a segunda pessoa nada com que decidir.
    reason                 text,

    -- A data do obito, so em OBITO.
    --
    -- ELA VEM NO PEDIDO E NAO NA CONCORDANCIA, e a razao e que o dado e de quem viu: quem
    -- encontrou o gato morto sabe quando foi, e quem concorda tres dias depois nao sabe. Usar a
    -- data da decisao inventaria um fato — e no animal com tutor este e justamente o campo que
    -- ninguem consegue reconstruir depois.
    deceased_on            date,

    status                 varchar(16)  NOT NULL,

    requested_by_person_id uuid         NOT NULL,
    requested_at           timestamp    NOT NULL,

    -- Quem concordou ou recusou, e quando. Nulos enquanto pendente.
    decided_by_person_id   uuid,
    decided_at             timestamp,

    CONSTRAINT pk_group_approvals PRIMARY KEY (group_approval_id),
    CONSTRAINT fk_group_approvals_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_group_approvals_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_group_approvals_target FOREIGN KEY (target_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_group_approvals_to_person FOREIGN KEY (to_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_group_approvals_requested_by FOREIGN KEY (requested_by_person_id)
        REFERENCES persons (person_id),

    CONSTRAINT ck_group_approvals_kind
        CHECK (kind IN ('ADOCAO', 'OBITO', 'REMOCAO_DE_MEMBRO')),
    CONSTRAINT ck_group_approvals_status
        CHECK (status IN ('PENDENTE', 'CONCORDADO', 'RECUSADO')),

    -- O ALVO TEM DE EXISTIR, E TEM DE SER O DO TIPO. Sem este CHECK, um pedido de adocao sem
    -- animal seria gravado e so explodiria na hora de executar — depois de a segunda pessoa ja
    -- ter concordado com um pedido que nao dizia sobre o que era.
    CONSTRAINT ck_group_approvals_alvo CHECK (
        (kind = 'ADOCAO'            AND animal_id IS NOT NULL AND to_person_id IS NOT NULL
                                    AND target_person_id IS NULL) OR
        (kind = 'OBITO'             AND animal_id IS NOT NULL AND to_person_id IS NULL
                                    AND target_person_id IS NULL AND deceased_on IS NOT NULL) OR
        (kind = 'REMOCAO_DE_MEMBRO' AND target_person_id IS NOT NULL AND animal_id IS NULL
                                    AND to_person_id IS NULL)
    ),

    -- QUEM PEDE NAO CONCORDA CONSIGO, e este e o CHECK que carrega a regra inteira. Ele existe no
    -- banco alem do servico pela mesma razao da gravidade de alergia: o servico impede que o
    -- cliente receba erro de integridade como 500, e o banco impede o insert direto — mas so o
    -- banco impede que uma refatoracao futura apague a regra sem ninguem notar.
    CONSTRAINT ck_group_approvals_duas_pessoas
        CHECK (decided_by_person_id IS NULL OR decided_by_person_id <> requested_by_person_id),

    -- Decidido tem quem decidiu; pendente nao tem. Os dois campos andam juntos ou nao andam.
    CONSTRAINT ck_group_approvals_decisao_completa CHECK (
        (status = 'PENDENTE'  AND decided_by_person_id IS NULL AND decided_at IS NULL) OR
        (status <> 'PENDENTE' AND decided_by_person_id IS NOT NULL AND decided_at IS NOT NULL)
    )
);

-- Um pedido pendente por ato e alvo: pedir duas vezes a adocao do mesmo gato daria a duas pessoas
-- a mesma pergunta em duplicata, e as duas concordariam com o que aconteceria uma vez so.
CREATE UNIQUE INDEX ux_group_approvals_pendente_animal
    ON group_approvals (organization_id, kind, animal_id)
    WHERE status = 'PENDENTE' AND animal_id IS NOT NULL;

CREATE UNIQUE INDEX ux_group_approvals_pendente_pessoa
    ON group_approvals (organization_id, kind, target_person_id)
    WHERE status = 'PENDENTE' AND target_person_id IS NOT NULL;

-- Quem abre o grupo precisa ver o que espera decisao dele.
CREATE INDEX ix_group_approvals_pendentes
    ON group_approvals (organization_id) WHERE status = 'PENDENTE';

-- =============================================================== 3. o turno de quem cuida
--
-- "Alimenta de manha", "Alimenta a noite", "Leva ao veterinario".
--
-- <b>Texto livre, e nao valor novo no `MembershipRole`.</b> O enum diz o que a pessoa PODE fazer
-- — e por isso ele governa permissao. Isto diz o que ela FAZ, e "de manha" contra "a noite" e
-- escala, nao papel: os dois seriam o mesmo valor de enum, e a tela perderia exatamente a
-- informacao que mostra. Um enum que tentasse cobrir viraria uma lista de turnos que nao acaba.
--
-- Opcional, e o normal e nulo: a equipe de uma clinica nao tem turno declarado, e a tela dela
-- nunca pediu isso.
ALTER TABLE memberships ADD COLUMN contribution varchar(80);

-- =============================================================== 4. a castracao marcada
--
-- O desenho mostra tres estados na coluna de situacao: "Castrado em 03/2022", "Castracao marcada,
-- 22/08" e "Sem informacao". O do meio nao existia.
--
-- <b>E NAO PODE SER O `castrated_at` COM DATA FUTURA</b>, que era a saida barata: aquele campo
-- afirma que o animal FOI castrado, e um mutirao marcado para o dia 22 nao castrou ninguem. O
-- animal que nao aparece no dia do mutirao ficaria registrado como castrado para sempre — e a
-- proxima lista de "falta castrar" o deixaria de fora, que e o defeito mais caro possivel numa
-- tela cujo proposito e nao perder gato nenhum.
--
-- Coluna propria, entao, e o servico limpa quando a castracao acontece: um animal castrado nao
-- tem castracao marcada.
ALTER TABLE animals ADD COLUMN neutering_scheduled_for date;

-- Nao ha CHECK de "marcada so no futuro": a data vira passado sozinha no dia seguinte, sem
-- ninguem escrever nada, e um CHECK sobre CURRENT_DATE nem seria aceito pelo Postgres. Marcacao
-- vencida e sinal — quer dizer que o mutirao passou e ninguem registrou o que houve.
