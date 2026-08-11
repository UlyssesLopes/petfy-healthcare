-- Por onde o valor entra (Telas 40, 41 e 42).
--
-- A tese esta na abertura do desenho, e ela decide o schema inteiro: <b>"o custo do animal so
-- existe se o dado entrar sem esforco. Um campo dentro do que ja estava sendo registrado, e nada
-- alem disso: nenhuma tela nova para a clinica, nenhum trabalho a mais para a creche."</b>
--
-- E a frase seguinte e a que impede o erro obvio: "o campo e opcional, e um evento sem valor e
-- NORMAL — nunca um erro, nunca um alerta. Se informar valor virar obrigacao, a clinica para de
-- registrar o atendimento, e ai o produto perde o que importa de verdade."
--
-- --------------------------------------------------------------- por que uma tabela, e nao colunas
--
-- A tentacao era `valor` e `pago` no `health_records` e pronto. Nao serve por tres razoes:
--
--   * UM ATENDIMENTO TEM MAIS DE UM ITEM. O desenho mostra "adicionar outro item" — consulta,
--     exame, medicacao aplicada. Duas colunas guardariam a soma e perderiam do que ela e feita,
--     que e exatamente o que o tutor quer ler.
--   * O CUSTO NAO VEM SO DE ATENDIMENTO. Vem da mensalidade da creche, da diaria avulsa, e da
--     racao que o tutor compra no mercado — e essa ultima nao tem evento clinico nenhum atras.
--   * A LEITURA DO VALOR TEM REGRA PROPRIA, diferente da leitura do evento. Guardar preco dentro
--     do prontuario faria quem alcanca o prontuario alcancar o preco junto, e o desenho recusa
--     isso explicitamente.
--
-- --------------------------------------------------- "so o tutor ve", e por que isso nao e zelo
--
-- <b>"O que a Clinica Vet Norte cobra do Marcelo nao e assunto da creche, do petshop nem de outra
-- clinica. (...) NENHUM ESCOPO DE ACESSO CONCEDE PRECO JUNTO COM SAUDE."</b>
--
-- Por isso o custo NAO entra na `animal_timeline` e nao ganha `GrantScope`. Quem le e quem
-- responde pelo animal, e mais ninguem — a checagem e por CUSTODIA, num lugar so.
--
-- E o produto recusa uma tentacao que o dado permitiria: com preco de milhares de atendimentos
-- seria facil mostrar ao tutor que a mesma consulta custa menos duas ruas adiante. "Registro
-- clinico que vira comparador de preco deixa de ser lugar seguro para a clinica registrar a
-- verdade — e sem a clinica registrando, nao ha produto." Nao ha, e nao havera, consulta que
-- agregue preco por organizacao.

CREATE TABLE animal_costs (
    animal_cost_id          uuid           NOT NULL,
    animal_id               uuid           NOT NULL,

    -- "O que foi cobrado": consulta dermatologica, mensalidade, racao.
    description             varchar(200)   NOT NULL,

    -- numeric e nao float: dinheiro em ponto flutuante e um erro que aparece na terceira soma.
    amount                  numeric(12, 2) NOT NULL,

    -- ATENDIMENTO, CRECHE_MENSALIDADE, CRECHE_DIARIA, COMPRA.
    --
    -- Existe para o tutor LER agrupado — "Creche Quintal · mensalidade" separado de "diaria
    -- avulsa de 22/07" —, e nao para o produto tratar cada um de um jeito.
    kind                    varchar(24)    NOT NULL,

    -- "Ja foi pago". Nulo em quem nao respondeu, e nao false: o desenho oferece a caixa e nao a
    -- obriga, e false diria "nao foi pago" sobre algo que ninguem afirmou.
    paid                    boolean,

    -- "Dura cerca de um mes" — a caixinha da Tela 42.
    --
    -- <b>E o que transforma uma compra avulsa em custo mensal previsivel</b>, e e tambem o que
    -- permite ao abrigo dizer ao adotante que a racao custa R$ 190 por mes, todo mes. Sem ela o
    -- produto so saberia somar o passado.
    --
    -- MENSAL ou nulo. Nao ha SEMANAL nem ANUAL porque nenhuma tela pede, e um enum com valores
    -- que ninguem escreve e um convite para alguem escrever sem pensar.
    recurrence              varchar(16),

    -- Quando o gasto aconteceu. Separado da data de lancamento pela mesma razao de todo evento
    -- deste produto: quem lanca hoje a nota de ontem lancou ontem.
    occurred_at             timestamp      NOT NULL,

    -- O nucleo de evento: quem registrou, e em nome de quem.
    --
    -- "Cada valor tem um evento por tras, com autor e data — e por isso PODE SER CONTESTADO como
    -- qualquer outro registro." Um valor sem autor seria um numero que apareceu sozinho na conta
    -- do tutor, e ele nao teria a quem perguntar.
    recorded_by_person_id   uuid,
    organization_id         uuid,

    -- De qual evento este valor saiu, quando saiu de um.
    --
    -- Nulos os dois na compra do tutor: racao de mercado nao tem organizacao atras, e e por isso
    -- que ela e o UNICO lancamento manual do produto.
    source_health_record_id uuid,
    source_enrollment_id    uuid,

    creation_date           timestamp      NOT NULL,

    CONSTRAINT pk_animal_costs PRIMARY KEY (animal_cost_id),
    CONSTRAINT fk_animal_costs_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_animal_costs_person FOREIGN KEY (recorded_by_person_id)
        REFERENCES persons (person_id),
    CONSTRAINT fk_animal_costs_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_animal_costs_health_record FOREIGN KEY (source_health_record_id)
        REFERENCES health_records (health_record_id),
    CONSTRAINT fk_animal_costs_enrollment FOREIGN KEY (source_enrollment_id)
        REFERENCES enrollments (enrollment_id),

    -- Valor negativo nao e gasto, e o produto nao tem estorno: o desenho e explicito em que o
    -- Petfy "nao cobra, nao emite boleto e nao processa pagamento". Corrigir um valor errado e
    -- correcao, e nao um lancamento ao contrario.
    CONSTRAINT ck_animal_costs_amount CHECK (amount >= 0)
);

CREATE INDEX ix_animal_costs_animal ON animal_costs (animal_id, occurred_at DESC);

-- ------------------------------------------------------- o que a creche combinou (Tela 41)
--
-- Mora na MATRICULA e nao numa tabela propria porque e uma propriedade do combinado daquele
-- animal naquela turma: mudar de turma e recombinar, e a mensalidade antiga fica com a matricula
-- antiga, onde ela conta a verdade sobre o periodo em que valeu.
--
-- "O Petfy nao cobra, nao emite boleto e nao processa pagamento. Ele guarda o que foi combinado,
-- para que o tutor veja o custo real do animal e ninguem precise perguntar por telefone."
ALTER TABLE enrollments ADD COLUMN monthly_fee numeric(12, 2);

-- "Vence todo dia 05". Nulo quando nao se combinou dia — e comum, e nao pendencia.
ALTER TABLE enrollments ADD COLUMN due_day integer;

ALTER TABLE enrollments ADD CONSTRAINT ck_enrollments_due_day
    CHECK (due_day IS NULL OR (due_day BETWEEN 1 AND 28));

-- A diaria avulsa: "usada quando o animal vem fora dos dias combinados".
--
-- <b>E ela que entra sozinha.</b> A creche marca a entrada num dia fora da combinacao e o evento
-- de entrada carrega o valor — "ninguem digitou nada". E esse encadeamento que faz o custo se
-- manter atualizado sem virar tarefa.
ALTER TABLE enrollments ADD COLUMN daily_rate numeric(12, 2);

ALTER TABLE enrollments ADD CONSTRAINT ck_enrollments_valores
    CHECK ((monthly_fee IS NULL OR monthly_fee >= 0)
       AND (daily_rate IS NULL OR daily_rate >= 0));
