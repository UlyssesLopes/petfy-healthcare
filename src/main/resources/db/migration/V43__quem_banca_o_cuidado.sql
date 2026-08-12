-- Apadrinhar: quem banca parte do custo de um animal de abrigo (Tela 46).
--
-- <b>ESTA TABELA NAO MOVE DINHEIRO, E ISSO E A DECISAO CENTRAL.</b> Nao ha meio de pagamento em
-- lugar nenhum deste produto — nem gateway, nem cobranca, nem saldo —, e inventar um aqui seria
-- construir a metade menos interessante do problema. O que o desenho promete e outra coisa, e o
-- produto ja sabe fazer: <i>"quando o remedio dele for comprado, voce vai ver o evento — com data,
-- valor e quem comprou."</i>
--
-- Entao o apadrinhamento e um COMPROMISSO registrado: quem banca, o que banca, desde quando. O
-- valor corre fora do Petfy, e o que o padrinho recebe em troca e a prestacao de contas que o
-- produto ja dava ao tutor — os eventos de custo, assinados e datados.
--
-- ------------------------------------------------------- por que nao e um `grant` com escopo novo
--
-- A tentacao e obvia: o padrinho precisa LER algo do animal, e ler algo do animal e o que `grants`
-- governa. Nao serve, por duas razoes.
--
-- A primeira e que o desenho fecha a porta com todas as letras: <i>"Nenhum historico clinico aberto
-- ao padrinho. Ele ve o que banca, e o que o abrigo escolher mostrar."</i> Nenhum escopo existente
-- entrega isso — o custo nao E escopo, ele mora fora da linha do tempo justamente porque "nenhum
-- escopo de acesso concede preco junto com saude".
--
-- A segunda e o que uma linha em `grants` significaria para quem responde pelo animal: ela apareceria
-- na tela de acessos do abrigo como se o padrinho pudesse ler o animal. Um escopo novo `CUSTO` que
-- guarda nenhuma aplica seria pior ainda — uma promessa de recorte que a guarda nao cumpre.
--
-- <b>O padrinho, portanto, nao alcanca o animal.</b> Ele alcanca o apadrinhamento dele, e o servico
-- devolve os eventos de custo a partir dessa linha. O `AnimalAccessGuard` nao aprendeu nada aqui.
CREATE TABLE sponsorships (
    sponsorship_id        uuid           NOT NULL,

    animal_id             uuid           NOT NULL,

    sponsor_person_id     uuid           NOT NULL,

    -- Quem responde pelo animal quando o apadrinhamento comecou.
    --
    -- NAO E redundante com a custodia: a custodia muda — o Teco pode ser adotado —, e o
    -- apadrinhamento precisa lembrar a quem ele foi oferecido. Sem esta coluna, um animal que sai do
    -- abrigo deixaria o padrinho bancando algo para uma organizacao que nao cuida mais dele.
    organization_id       uuid           NOT NULL,

    -- O QUE se banca, em texto. "O remedio da artrose", "a racao", "a consulta".
    --
    -- <b>Texto, e nao referencia obrigatoria a uma linha de custo</b>, porque o desenho oferece
    -- quatro botoes e o quarto e "Outro · valor livre". Amarrar a um `animal_costs` existente
    -- impediria o quarto botao, que e o unico que cobre o que o abrigo ainda nao lancou.
    description           varchar(200)   NOT NULL,

    -- Quanto, por mes. `numeric` e nao `double`, como todo dinheiro deste schema.
    amount                numeric(12,2)  NOT NULL,

    -- De qual linha de custo este valor saiu, quando saiu de uma.
    --
    -- Nulavel por causa do "Outro". Quando existe, e o que liga "R$ 80 · o remedio da artrose" ao
    -- gasto real que o abrigo lancou — e o que permite a tela dizer que o valor nao foi inventado
    -- pelo produto: <i>"O que o abrigo gasta com ele por mes"</i>.
    source_cost_id        uuid,

    started_on            date           NOT NULL,

    -- Quando o padrinho pediu para parar, e quando de fato para.
    --
    -- <b>SAO DUAS DATAS PORQUE A TELA PROMETE TRINTA DIAS:</b> <i>"Pode parar quando quiser, sem
    -- justificar. O abrigo e avisado com 30 dias para se organizar."</i> Uma coluna so faria o
    -- encerramento ser imediato — e o abrigo descobriria no dia em que o remedio nao fosse comprado.
    --
    -- E o pedido nao pede motivo, de proposito: "sem justificar" e parte da promessa. Um campo de
    -- justificativa aqui faria o padrinho explicar ao abrigo por que parou de ajudar.
    cancel_requested_at   timestamp,
    ends_on               date,

    status                varchar(24)    NOT NULL,

    creation_date         timestamp      NOT NULL,

    CONSTRAINT pk_sponsorships PRIMARY KEY (sponsorship_id),
    CONSTRAINT fk_sponsorships_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_sponsorships_person FOREIGN KEY (sponsor_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_sponsorships_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_sponsorships_cost FOREIGN KEY (source_cost_id)
        REFERENCES animal_costs (animal_cost_id),

    CONSTRAINT ck_sponsorships_status
        CHECK (status IN ('ATIVO', 'ENCERRAMENTO_PEDIDO', 'ENCERRADO')),

    -- Valor tem de ser valor. Zero seria um apadrinhamento que nao banca nada, e ele apareceria na
    -- lista do abrigo somando zero ao custo coberto — pior que nao existir, porque ocupa uma linha.
    CONSTRAINT ck_sponsorships_valor CHECK (amount > 0),

    -- As DUAS datas do encerramento andam juntas, e o estado tem de combinar com elas. Sem isto,
    -- um `ENCERRAMENTO_PEDIDO` sem `ends_on` nunca terminaria, e um `ATIVO` com `ends_on` terminaria
    -- sem ninguem ter pedido.
    CONSTRAINT ck_sponsorships_encerramento CHECK (
        (status = 'ATIVO'               AND cancel_requested_at IS NULL     AND ends_on IS NULL) OR
        (status = 'ENCERRAMENTO_PEDIDO' AND cancel_requested_at IS NOT NULL AND ends_on IS NOT NULL) OR
        (status = 'ENCERRADO'           AND ends_on IS NOT NULL)
    ),

    -- O fim nao pode ser antes do comeco.
    CONSTRAINT ck_sponsorships_ordem_das_datas
        CHECK (ends_on IS NULL OR ends_on >= started_on)
);

-- UM APADRINHAMENTO VIVO POR PESSOA, ANIMAL E COISA BANCADA.
--
-- Bancar duas vezes "o remedio da artrose" do mesmo animal nao e generosidade dobrada: e a mesma
-- pessoa tendo clicado duas vezes, e o abrigo somaria R$ 160 de um custo que continua sendo R$ 80.
-- Quem quiser dar mais usa o "Outro", que tem descricao propria.
CREATE UNIQUE INDEX ux_sponsorships_vivo
    ON sponsorships (animal_id, sponsor_person_id, lower(description))
    WHERE status <> 'ENCERRADO';

-- "O que voce banca" — a lista do padrinho, do mais recente para o mais antigo.
CREATE INDEX ix_sponsorships_do_padrinho
    ON sponsorships (sponsor_person_id, creation_date DESC);

-- "Quem banca os nossos animais" — a lista do abrigo.
CREATE INDEX ix_sponsorships_da_organizacao
    ON sponsorships (organization_id) WHERE status <> 'ENCERRADO';

-- =============================================================== o animal que aceita padrinho
--
-- <b>Nem todo animal pode ser apadrinhado, e a coluna existe para o abrigo DECIDIR isso.</b>
--
-- Sem ela, a alternativa seria inferir: "todo animal sob custodia de organizacao aceita padrinho".
-- Isso poria o cao que chegou ontem, ainda sem diagnostico, na mesma vitrine do Teco — e poria
-- tambem o animal que o abrigo nao quer expor. A decisao de oferecer um animal a padrinhos e do
-- abrigo, e uma inferencia a tomaria por ele.
--
-- Nulavel e falso por omissao: nenhum animal existente passa a aceitar padrinho por causa desta
-- migracao.
ALTER TABLE animals ADD COLUMN accepts_sponsorship boolean NOT NULL DEFAULT false;
