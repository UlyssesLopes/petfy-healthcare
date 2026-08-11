-- O valor da dose, e o que ele permite prever (Tela 38).
--
-- A tese da Tela 38 e uma frase: <b>"isto nao e previsao de gasto: e o que JA ESTA MARCADO no
-- registro dele. Vacinas com data de reforco, tratamento em curso e a mensalidade que se repete."</b>
--
-- E o desenho poe um numero em cada linha: antirrabica R$ 90, antiparasitario R$ 65, V10 R$ 120.
-- Nenhum desses valores tinha fonte no modelo, e essa era a lacuna:
--
--   * a `animal_costs` ligava o valor ao ATENDIMENTO (`source_health_record_id`) e a MATRICULA
--     (`source_enrollment_id`), e uma dose de vacina nao e nenhum dos dois — ela mora na
--     `vaccines`, com catalogo e data de proxima dose proprios;
--   * precificar um reforco pelo ultimo custo de categoria SAUDE cobraria a antirrabica com o
--     preco de uma consulta dermatologica, que e pior do que nao ter preco nenhum.
--
-- Com a ligacao, o preco do reforco do ano que vem sai da DOSE DO MESMO ITEM DE CATALOGO daquele
-- animal — "aritmetica sobre fatos registrados", que e o limite que o proprio desenho declara para
-- esta leitura: "ela pode dizer que adiar a vacina custa dias de creche perdidos, porque isso e
-- aritmetica sobre fatos registrados. Nunca vai dizer que tratar a displasia agora sai mais barato
-- que operar depois — isso e prognostico clinico, e o Petfy nao faz prognostico."

ALTER TABLE animal_costs ADD COLUMN source_vaccine_id uuid;

ALTER TABLE animal_costs ADD CONSTRAINT fk_animal_costs_vaccine
    FOREIGN KEY (source_vaccine_id) REFERENCES vaccines (vaccine_id);

-- O antiparasitario entra pela mesma razao e no mesmo movimento: o desenho da Tela 38 poe as duas
-- linhas lado a lado, e as duas tem intervalo de reforco calculado. Deixar uma de fora faria a
-- previsao ter preco para metade do que ela lista.
ALTER TABLE animal_costs ADD COLUMN source_antiparasitic_id uuid;

ALTER TABLE animal_costs ADD CONSTRAINT fk_animal_costs_antiparasitic
    FOREIGN KEY (source_antiparasitic_id) REFERENCES antiparasitics (antiparasitic_id);

-- --------------------------------------------------------------- por que nao ha indice aqui
--
-- A busca do preco anterior acontece dentro de uma leitura que JA carrega os custos do animal e as
-- doses do animal — o resumo da Tela 37 faz isso, e a previsao da 38 aproveita o mesmo recorte. O
-- casamento e em memoria, por serie, e nao uma consulta por dose. Um indice aqui serviria a uma
-- consulta que nao existe.
--
-- --------------------------------------------------------------- o purger ja estava na ordem
--
-- A `animal_costs` sai ANTES de `vaccines` e de `antiparasitics` no `AnimalPurger` desde a V36,
-- porque ja apontava para `health_records` e `enrollments`. As duas chaves novas nao mudam a ordem
-- — e o `AnimalPurgerCoverageContainerTest` cobre isso, porque a tabela ja esta declarada e a
-- consulta dele e transitiva.
