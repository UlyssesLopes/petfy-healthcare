-- Quanto tempo um gasto dura (Tela 42, e a previsao da Tela 39).
--
-- <b>O PRODUTO CONFUNDIA DUAS COISAS, e a caixinha "dura cerca de um mes" era a confusao.</b>
--
-- Ela gravava `recurrence = MENSAL`, e o `CostRecurrence` tem exatamente um valor. Com isso, "a racao
-- dura um mes" e "a mensalidade da creche se repete todo mes" viraram o MESMO fato — e eles nao sao:
--
--   * a mensalidade REPETE: ela chega todo mes, e o valor de cada mes e o valor cheio;
--   * a racao COBRE um periodo: um gasto unico de R$ 190 que atende dois meses custa R$ 95 por mes,
--     e volta a acontecer daqui a dois meses.
--
-- Enquanto so existia "um mes", os dois coincidiam por acidente e ninguem viu. A pergunta que o
-- tutor faz — "e a racao que dura dois meses?" — nao tinha resposta: a caixinha era booleana, e
-- marcar dizia "todo mes", que e o dobro do que ele gasta.
--
-- ------------------------------------------------------------------ por que uma coluna, e nao mais um valor no enum
--
-- A tentacao e `CostRecurrence.BIMESTRAL, TRIMESTRAL, SEMESTRAL`. Nao serve: o intervalo e um NUMERO
-- que o tutor conhece, e nao uma lista que o produto escolhe. A racao do gato dura 45 dias, a do
-- cachorro grande dura 20, e um enum obrigaria cada um a mentir para o vizinho mais proximo.
--
-- <b>Em MESES, e nao em dias</b>, e a razao e a tela que consome: a previsao da Tela 39 pensa em doze
-- meses, e "de quantos em quantos meses isto volta" e a pergunta que ela responde. Dias dariam
-- precisao que ninguem tem — quem compra racao nao sabe se dura 28 ou 31.
ALTER TABLE animal_costs ADD COLUMN covers_months integer;

-- O intervalo tem de caber num ano de previsao, e tem de ser um intervalo.
--
-- Zero nao e "nao se repete" — para isso a coluna fica NULA. Zero seria um gasto que volta infinitas
-- vezes por mes, e a previsao dividiria por ele.
ALTER TABLE animal_costs ADD CONSTRAINT ck_animal_costs_cobertura
    CHECK (covers_months IS NULL OR (covers_months >= 1 AND covers_months <= 12));

-- O QUE JA ESTAVA MARCADO CONTINUA VALENDO O QUE VALIA.
--
-- Toda compra com `recurrence = MENSAL` foi lancada por alguem que marcou "dura cerca de um mes" —
-- entao ela cobre UM mes, e o backfill afirma exatamente o que aquela pessoa afirmou. Deixar nulo
-- faria a previsao esquecer o que o tutor ja tinha dito.
--
-- <b>A mensalidade da creche NAO entra</b>, e e o ponto da separacao: ela repete, e nao cobre um
-- periodo. O `kind` e o que distingue os dois, e por isso o filtro esta nele.
UPDATE animal_costs
   SET covers_months = 1
 WHERE recurrence = 'MENSAL' AND kind = 'COMPRA';
