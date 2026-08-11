-- O mesmo animal, cadastrado duas vezes (Tela 32).
--
-- O caso nao e hipotetico e nao e raro: o gato da praca cadastrado pela protetora, depois pela
-- clinica que o atendeu, depois pelo abrigo que o recolheu. Cada um viu um animal; o Petfy tem
-- tres. E o microchip — que existe justamente para dizer "este e aquele" — nao era conferido
-- contra nada.
--
-- ------------------------------------------------------------------ o que esta migracao NAO faz
--
-- ELA NAO IMPEDE O CADASTRO DUPLICADO, e isso e deliberado. A veterinaria que atende um animal
-- as 22h nao pode ser barrada porque o microchip bate com um cadastro que ela nao alcanca: ela
-- precisa registrar o atendimento AGORA. Uma restricao UNIQUE em `microchip_number` transformaria
-- o caso delicado num erro de gravacao, e o erro de gravacao vira um cadastro sem microchip —
-- que e pior, porque destroi a unica pista de que os dois sao o mesmo bicho.
--
-- Entao o duplicado e DETECTADO e vira um pedido, e nao uma recusa.
--
-- ------------------------------------------------------------- quem decide, e por que nao a clinica
--
-- "Voce nao pode unir sozinha. Quem responde pelo Code e o Marcelo. Ele recebe o pedido, ve
-- exatamente esta comparacao e decide." E a mesma regra da transferencia de titularidade:
-- NINGUEM MEXE NA VIDA REGISTRADA DE UM ANIMAL SEM QUEM RESPONDE POR ELE.
--
-- A uniao e irreversivel — duas linhas do tempo viram uma, e desfazer exigiria saber de qual
-- cadastro cada evento veio, o que so seria possivel guardando a origem em cada linha. Por ser
-- irreversivel, ela nao pode ser decisao de quem tem acesso: tem de ser de quem responde.

-- ------------------------------------------------------------------- o cadastro que nao sobrevive
--
-- ELE NAO E APAGADO, e essa e a mesma escolha que o vinculo desligado e a orientacao encerrada
-- ja fizeram neste schema: encerra-se, nao se apaga.
--
-- Duas razoes concretas. A primeira e que alguem tem o link do cadastro antigo — a clinica que
-- pediu a uniao guardou o id, e um 404 diria que o animal nunca existiu, quando o que aconteceu
-- foi o oposto: ele virou parte de outro. A segunda e que a uniao precisa poder ser AUDITADA, e
-- uma linha apagada nao conta o que houve.
ALTER TABLE animals ADD COLUMN merged_into_animal_id uuid;

ALTER TABLE animals ADD CONSTRAINT fk_animals_merged_into
    FOREIGN KEY (merged_into_animal_id) REFERENCES animals (animal_id);

-- Quem sobreviveu nao e apontado por si mesmo, e quem foi absorvido nao absorve ninguem: sem
-- isto, uma cadeia A->B->A tornaria a leitura do sobrevivente um laco infinito.
ALTER TABLE animals ADD CONSTRAINT ck_animals_merged_into_nao_e_ele_mesmo
    CHECK (merged_into_animal_id IS NULL OR merged_into_animal_id <> animal_id);

CREATE INDEX ix_animals_merged_into ON animals (merged_into_animal_id)
    WHERE merged_into_animal_id IS NOT NULL;

-- ---------------------------------------------------------------- "sao animais diferentes"
--
-- O desenho pede isto com todas as letras: "se forem diferentes, o microchip repetido fica
-- marcado nos dois cadastros — provavelmente ha um erro de digitacao em algum lugar, e alguem
-- vai precisar saber disso".
--
-- E uma marca e nao um erro: o produto nao sabe qual dos dois esta errado, e adivinhar
-- apagaria o numero certo metade das vezes. Quem sabe e quem tem o animal na frente e o leitor
-- de microchip na mao.
ALTER TABLE animals ADD COLUMN microchip_conflict boolean NOT NULL DEFAULT false;

-- --------------------------------------------------------------------------- o pedido de uniao
CREATE TABLE animal_merge_requests (
    animal_merge_request_id uuid      NOT NULL,

    -- O que seria ABSORVIDO, e o que SOBREVIVE.
    --
    -- Os nomes dizem o desfecho e nao a ordem de chegada de proposito: "origem" e "destino"
    -- deixariam ambiguo qual das duas linhas do tempo continua existindo com o proprio id, e e
    -- exatamente essa a pergunta que quem le o pedido esta fazendo.
    absorbed_animal_id      uuid      NOT NULL,
    surviving_animal_id     uuid      NOT NULL,

    -- Quem pediu, e em nome de quem. A organizacao e nula quando a pessoa agiu por si — o
    -- veterinario autonomo que percebeu a duplicata.
    requested_by_person_id  uuid      NOT NULL,
    organization_id         uuid,

    -- "Por que voce acha que e o mesmo animal." Obrigatorio, e o desenho ja o preenche com o
    -- exemplo certo: "mesmo microchip. Chegou com a Juliana, que disse ser co-tutora do Code".
    --
    -- Sem motivo, quem decide recebe um pedido que so diz "una" — e nao tem como julgar. O
    -- motivo tambem vai para o evento da uniao, para quem ler daqui a cinco anos entender.
    reason                  varchar(500) NOT NULL,

    -- PENDENTE, ACEITO, RECUSADO. Recusado e "sao animais diferentes".
    status                  varchar(16) NOT NULL,

    -- Quem decidiu, e quando. Nulos enquanto pendente.
    decided_by_person_id    uuid,
    decided_at              timestamp,

    -- O que os dois cadastros diziam de diferente NO MOMENTO DA DECISAO, em JSON.
    --
    -- <b>Guardado no pedido, e nao recalculado na leitura.</b> "Onde ha conflito, o cadastro
    -- mais antigo prevalece e o outro valor fica guardado no evento da uniao. Nada e escolhido
    -- em silencio" — e um valor descartado que nao foi guardado no instante em que se descartou
    -- nao volta nunca. Recalcular depois leria o cadastro ja unido, onde o valor perdido nao
    -- existe mais.
    --
    -- JSON e nao coluna por campo porque a lista de campos em conflito e a lista de campos do
    -- animal, e ela cresce: RGA e tatuagem ja estao previstos e nao existem ainda.
    discarded_values        text,

    creation_date           timestamp NOT NULL,

    CONSTRAINT pk_animal_merge_requests PRIMARY KEY (animal_merge_request_id),

    CONSTRAINT fk_amr_absorbed FOREIGN KEY (absorbed_animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_amr_surviving FOREIGN KEY (surviving_animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_amr_requested_by FOREIGN KEY (requested_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_amr_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_amr_decided_by FOREIGN KEY (decided_by_person_id)
        REFERENCES persons (person_id),

    -- Unir um animal a ele mesmo nao e pedido, e erro de cliente.
    CONSTRAINT ck_amr_dois_animais CHECK (absorbed_animal_id <> surviving_animal_id)
);

-- Um pedido pendente por PAR, e nao por animal.
--
-- O animal pode ter duplicata com dois cadastros diferentes — acontece com o gato de rua visto
-- por tres pessoas —, e cada par e uma decisao propria. O que nao pode e o mesmo par ser pedido
-- duas vezes: quem responde receberia o mesmo pedido em duplicata e aceitaria os dois, e o
-- segundo tentaria absorver um cadastro que ja foi absorvido.
CREATE UNIQUE INDEX ux_amr_pendente_por_par
    ON animal_merge_requests (absorbed_animal_id, surviving_animal_id)
    WHERE status = 'PENDENTE';

-- Quem responde pelo animal precisa achar o que esta esperando decisao dele.
CREATE INDEX ix_amr_sobrevivente_pendente
    ON animal_merge_requests (surviving_animal_id)
    WHERE status = 'PENDENTE';
