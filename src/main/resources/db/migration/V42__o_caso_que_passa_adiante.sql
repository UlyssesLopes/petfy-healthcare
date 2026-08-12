-- O encaminhamento: o caso inteiro chegando ao especialista (Tela 45).
--
-- <b>A REGRA CENTRAL ESTA NO DESENHO, e e ela que torna esta tabela possivel:</b> "Marcelo Dias
-- precisa autorizar. Encaminhar e voce indicando o caminho; conceder acesso continua sendo dele,
-- como sempre foi. O acesso do Roberto vale 90 dias e depois fecha sozinho."
--
-- Ou seja: <b>o encaminhamento NAO CONCEDE NADA.</b> Ele e um pedido que a clinica faz, o tutor
-- decide, e o que o aceite produz e um `grants` comum com `expires_at` — a mesma linha que a
-- concessao a uma clinica ja usava desde o P2b. Nao ha modelo novo de acesso aqui, e essa e a
-- noticia: se o encaminhamento gravasse acesso proprio, existiriam duas respostas para "quem
-- alcanca este animal", e o `AnimalAccessGuard` teria de consultar as duas para sempre.
--
-- ------------------------------------------------------- por que nao e o `pet_tutor_invites`
--
-- O convite tambem espera decisao de quem recebe, e para ai. No convite, quem decide E O
-- BENEFICIARIO: o conjuge aceita entrar no animal, e o token no e-mail dele e a credencial disso.
-- Aqui quem decide e UM TERCEIRO — o tutor —, e quem se beneficia nao e consultado em momento
-- nenhum. Sao tres partes decidindo em tempos diferentes, e o convite so sabe modelar duas.
--
-- ------------------------------------------------------- por que nao e o `group_approvals`
--
-- O acordo de duas pessoas do bloco 6 tem quem decide INDETERMINADO de proposito: qualquer outra
-- pessoa do grupo serve. Aqui e o contrario — <b>so quem responde pelo animal autoriza</b>, e
-- ninguem mais, porque o que esta sendo decidido e conceder acesso ao prontuario. Essa e a
-- distincao que o `requireCustodia` sempre guardou.
CREATE TABLE referrals (
    referral_id              uuid         NOT NULL,

    animal_id                uuid         NOT NULL,

    -- Quem encaminha. A pessoa, e nao a clinica: encaminhar e um ato profissional com autoria, e
    -- "a Clinica Vet Norte encaminhou" esconderia quem examinou o animal.
    referred_by_person_id    uuid         NOT NULL,

    -- Em nome de que clinica. NULAVEL, porque o veterinario autonomo encaminha tambem — e a tela
    -- diz "Ana Ferreira, pela Clinica Vet Norte" quando ha uma, e so o nome quando nao ha.
    from_organization_id     uuid,

    -- O especialista. PESSOA, e nao organizacao, e e a diferenca mais importante entre esta tabela
    -- e a concessao a uma clinica.
    --
    -- Na concessao comum o beneficiario e a organizacao de proposito: "quem atende hoje pode nao
    -- ser quem atende no retorno, entao o tutor autoriza a organizacao, e nao um profissional".
    -- Encaminhar e o caso oposto, e o desenho e explicito sobre isso: "Ele ja registrou o raio-X do
    -- Code em 2023". Encaminha-se para o Roberto porque e o Roberto — mandar o caso para a Clinica
    -- Anhangabau entregaria a ortopedia a quem estiver na recepcao.
    to_person_id             uuid         NOT NULL,

    -- O porque, escrito por quem encaminha. NOT NULL, e e o unico campo de texto obrigatorio desta
    -- tabela.
    --
    -- Ele e lido por duas pessoas com perguntas diferentes: o tutor decide autorizar com base nele,
    -- e o especialista descobre por ele o que esta sendo perguntado. Um encaminhamento sem motivo
    -- pediria ao tutor que autorizasse o desconhecido, e entregaria ao especialista um prontuario
    -- sem pergunta — que e a foto de WhatsApp que esta tela existe para acabar.
    reason                   text         NOT NULL,

    -- Quantos dias o acesso vale, se autorizado. 90 e o que a tela promete.
    --
    -- <b>Guardado no PEDIDO, e nao constante no servico</b>, porque o tutor precisa ver o prazo
    -- ANTES de autorizar: "O acesso do Roberto vale 90 dias e depois fecha sozinho" e parte do que
    -- ele esta decidindo. Uma constante no codigo faria a tela de decisao afirmar um numero que
    -- nada no pedido sustenta, e mudar o default amanha reescreveria o passado.
    access_days              integer      NOT NULL,

    status                   varchar(16)  NOT NULL,

    requested_at             timestamp    NOT NULL,

    -- Quem autorizou ou recusou, e quando. Nulos enquanto pendente.
    decided_by_person_id     uuid,
    decided_at               timestamp,

    -- A concessao que o aceite produziu.
    --
    -- <b>Esta coluna e a prova de que o encaminhamento nao concede.</b> Ela aponta para a linha em
    -- `grants` que o tutor criou ao autorizar — com `granted_by` sendo ELE, revogavel por ele a
    -- qualquer momento, aparecendo na lista de acessos do animal como qualquer outro. O
    -- encaminhamento apenas registra qual concessao nasceu dele.
    --
    -- E o contrario do que a Tela 33 recusou: la a leitura de quem respondia ate o obito NAO virou
    -- concessao gravada porque nao havia quem concedesse nem quem revogasse. Aqui ha os dois, com
    -- nome e data — entao e concessao de verdade.
    grant_id                 uuid,

    CONSTRAINT pk_referrals PRIMARY KEY (referral_id),
    CONSTRAINT fk_referrals_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_referrals_referred_by FOREIGN KEY (referred_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_referrals_from_organization FOREIGN KEY (from_organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_referrals_to_person FOREIGN KEY (to_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_referrals_decided_by FOREIGN KEY (decided_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_referrals_grant FOREIGN KEY (grant_id)
        REFERENCES grants (grant_id),

    CONSTRAINT ck_referrals_status
        CHECK (status IN ('PENDENTE', 'AUTORIZADO', 'RECUSADO')),

    -- NAO SE ENCAMINHA PARA SI MESMO. Nao e higiene: um encaminhamento de uma pessoa para ela
    -- mesma seria um pedido de acesso disfarcado de segunda opiniao, e o tutor autorizaria lendo
    -- "Ana encaminhou para a Ana" sem entender o que estava concedendo.
    CONSTRAINT ck_referrals_outra_pessoa
        CHECK (to_person_id <> referred_by_person_id),

    -- O prazo tem de ser um prazo. Zero dia concederia um acesso que nasce vencido, e o
    -- especialista abriria o link para encontrar 404 — depois de o tutor ter autorizado de verdade.
    CONSTRAINT ck_referrals_prazo
        CHECK (access_days > 0 AND access_days <= 365),

    -- Decidido tem quem decidiu; pendente nao tem. Os dois campos andam juntos ou nao andam — o
    -- mesmo CHECK do `group_approvals`, pela mesma razao.
    CONSTRAINT ck_referrals_decisao_completa CHECK (
        (status =  'PENDENTE' AND decided_by_person_id IS NULL     AND decided_at IS NULL) OR
        (status <> 'PENDENTE' AND decided_by_person_id IS NOT NULL AND decided_at IS NOT NULL)
    ),

    -- <b>SO O AUTORIZADO TEM CONCESSAO, e o autorizado TEM.</b> Um recusado com `grant_id`
    -- preenchido seria um acesso vivo que a tela de decisao mostra como negado; um autorizado sem
    -- concessao seria o oposto — a tela diz que o especialista alcanca o animal, e ele recebe 404.
    -- Os dois erros sao invisiveis no cliente, e ambos aparecem semanas depois.
    CONSTRAINT ck_referrals_concessao_do_autorizado CHECK (
        (status =  'AUTORIZADO' AND grant_id IS NOT NULL) OR
        (status <> 'AUTORIZADO' AND grant_id IS NULL)
    )
);

-- UM PENDENTE POR ANIMAL E POR DESTINATARIO.
--
-- Encaminhar duas vezes o mesmo animal ao mesmo especialista daria ao tutor a mesma pergunta em
-- duplicata, e autorizar as duas criaria duas concessoes para a mesma pessoa — que e exatamente o
-- que o `grant` de clinica evita reativando a linha vigente em vez de acumular.
--
-- <b>Quem encaminha NAO entra na chave</b>, e isso e deliberado: se a Ana ja encaminhou o Code
-- para o Roberto e o pedido espera decisao, um colega da mesma clinica encaminhando de novo nao
-- adiciona informacao — adiciona uma segunda pergunta sobre o mesmo caso.
CREATE UNIQUE INDEX ux_referrals_pendente
    ON referrals (animal_id, to_person_id)
    WHERE status = 'PENDENTE';

-- O tutor abre e ve o que espera decisao dele. A consulta parte do animal, porque e por custodia
-- que se descobre quais animais sao dele.
CREATE INDEX ix_referrals_pendentes_do_animal
    ON referrals (animal_id) WHERE status = 'PENDENTE';

-- O especialista abre e ve o que chegou para ele — a caixa de entrada do lado que recebe.
CREATE INDEX ix_referrals_recebidos
    ON referrals (to_person_id, requested_at DESC);

-- =============================================================== o que vai junto
--
-- <b>O RECORTE E POR ESCOPO, E NAO POR EVENTO — e essa e a decisao mais delicada do bloco.</b>
--
-- O desenho mostra quatro caixas, e o que ele seleciona sao EVENTOS: "o raio-X de 2023", "4
-- observacoes da Creche Quintal entre 02/06 e 05/08". A concessao deste produto so sabe conceder
-- por TIPO — `PRONTUARIO`, `PESO`, `OBSERVACOES` —, e essa e a unidade que o `AnimalAccessGuard`
-- aplica em toda leitura desde o P2b.
--
-- Tentar o recorte por evento significaria uma segunda linguagem de acesso: uma lista de ids ao
-- lado dos escopos, que toda consulta mascarada teria de consultar. E o resultado seria pior que
-- o compromisso, porque o recorte mente com facilidade: quatro observacoes selecionadas hoje nao
-- dizem nada sobre a quinta, escrita amanha pela mesma creche sobre o mesmo problema — e o
-- especialista, tratando o caso, nao a veria.
--
-- Entao as caixas SAO os escopos, com o subtitulo contado do animal de verdade ("12 atendimentos
-- desde 2019", que o `tamanhoDe` da linha do tempo ja sabe responder), e o que motivou o
-- encaminhamento vai no `reason`, que e texto livre. <b>A tela nao promete na caixa um recorte que
-- a concessao nao sabe fazer.</b>
CREATE TABLE referral_scopes (
    referral_id  uuid        NOT NULL,
    scope        varchar(24) NOT NULL,

    CONSTRAINT pk_referral_scopes PRIMARY KEY (referral_id, scope),

    -- SEM `ON DELETE CASCADE`, como o `grant_scopes` que esta tabela copia.
    --
    -- O cascade caberia e seria a segunda fonte de verdade sobre a mesma regra: neste schema a
    -- limpeza mora no `AnimalPurger`, em codigo, para ficar visivel e testavel — e ha um teste que
    -- recusa cascade em qualquer FK que chegue a `animals`. Com o cascade, o dia em que o purger
    -- estivesse errado o banco encobriria, e ninguem descobriria qual das duas manda.
    CONSTRAINT fk_referral_scopes_referral FOREIGN KEY (referral_id)
        REFERENCES referrals (referral_id)
);

-- Nao ha CHECK listando os escopos validos, ao contrario do `status` acima.
--
-- <b>E a mesma escolha que `grant_scopes` fez</b>, e a razao e que este conjunto cresce: um escopo
-- de genetica, um de comportamento, um de nutricao. Um CHECK aqui obrigaria uma migration a cada
-- valor novo do `GrantScope`, em duas tabelas, e a que ficasse para tras rejeitaria em silencio o
-- escopo que a outra aceita. O `status` e diferente: aqueles tres valores sao a maquina de estado
-- inteira, e um quarto seria uma mudanca de regra, nao um item de catalogo.

-- =============================================================== a especialidade
--
-- "Roberto Lins · ortopedia · Clinica Anhangabau" — e sem esta coluna a busca de profissional
-- mostraria "Roberto Lins · CRMV-SP 12345", que nao responde a pergunta de quem encaminha. Quem
-- procura um especialista procura pela especialidade; o numero do conselho serve para conferir
-- que a pessoa e quem diz ser, depois de encontrada.
--
-- <b>Na credencial, e nao em `persons`</b>, pela mesma razao que a credencial existe em tabela
-- propria: a especialidade e do registro profissional, e uma pessoa com registro em dois conselhos
-- pode declarar coisas diferentes em cada um. Em `persons` ela seguiria a pessoa mesmo depois de a
-- credencial ser suspensa.
--
-- <b>Texto livre, e nao enum</b>, e a escolha e consciente. Uma lista fechada seria filtravel e
-- mais limpa na busca, mas exigiria decidir hoje a lista inteira das especialidades veterinarias —
-- e cada uma que faltasse seria um profissional que nao consegue se descrever, num campo cujo
-- unico proposito e ele se descrever.
--
-- Nulavel, e o normal e nulo: toda credencial que existe hoje fica sem especialidade, e o clinico
-- geral nao tem uma. A busca nao pode exigir este campo — ver o servico.
ALTER TABLE professional_credentials ADD COLUMN specialty varchar(80);
