-- Onde o dinheiro foi (Tela 37).
--
-- O bloco 3 gravou o custo; o bloco 4 le. E a leitura pediu uma coluna que o bloco 3 nao tinha
-- razao para ter.
--
-- ------------------------------------------------------- por que o `kind` nao respondia isso
--
-- O `kind` diz de que EVENTO o valor saiu: ATENDIMENTO, CRECHE_MENSALIDADE, CRECHE_DIARIA, COMPRA.
-- A categoria diz em QUE o dinheiro foi gasto. As duas divergem no caso que mais importa: <b>racao
-- e remedio saem os dois de uma COMPRA do tutor</b>, e o desenho da Tela 37 os poe em fatias
-- diferentes do "onde foi".
--
-- E a divergencia nao e detalhe de apresentacao — e uma frase:
--
--   <b>"Saude e o menor pedaco do gasto do Code — e e o UNICO QUE CRESCE SOZINHO QUANDO E ADIADO.
--   Os outros tres sao escolha sua."</b>
--
-- Sem separar alimentacao de saude, essa frase perde o sentido, e com ela a tese da Tela 38 (o
-- custo de adiar). Derivar a categoria do `kind` daria tres fatias onde o desenho tem quatro, e
-- jogaria racao e remedio no mesmo saco.
--
-- ------------------------------------------------------------------- quem preenche, e com o que
--
--   * ATENDIMENTO      -> SAUDE        (o servidor sabe: e ato clinico)
--   * CRECHE_MENSALIDADE
--     e CRECHE_DIARIA   -> CRECHE       (o servidor sabe)
--   * COMPRA           -> o botao da Tela 42, que passa a ESCOLHER a categoria em vez de virar
--                        texto de descricao. "Racao" -> ALIMENTACAO, "Remedio" -> SAUDE,
--                        "Outro" -> OUTRO.
--
-- <b>HIGIENE nasce sem ninguem para escrever nela</b>, e isso e esperado: quem cobra banho e o
-- petshop, que e o bloco 8. Uma fatia sem valor nao aparece na Tela 37, entao a categoria existir
-- vazia nao custa nada — e no dia em que o petshop registrar valor ela ja tem lugar.
ALTER TABLE animal_costs ADD COLUMN category varchar(16);

-- ------------------------------------------------ o retroativo, e por que ele nao inventa nada
--
-- As linhas que ja existem tem `kind`, e o `kind` responde por tres dos quatro casos SEM ADIVINHAR:
-- atendimento e saude, mensalidade e diaria sao creche. Deixa-las nulas faria o "onde foi" abrir
-- com uma fatia "sem categoria" que ninguem consegue explicar, para um dado que o proprio schema
-- ja sabia classificar.
--
-- A COMPRA e a unica que fica em OUTRO, e ela e a unica em que isso e honesto: ate esta migration a
-- descricao era texto livre ("Racao", "Remedio", "Outro"), e casar por texto para decidir a fatia
-- seria exatamente o LIKE que a V37 recusou nos dias da semana. O tutor que quiser corrigir lanca
-- de novo; o produto nao vai afirmar que uma compra antiga era racao porque a palavra batia.
UPDATE animal_costs SET category = 'SAUDE'  WHERE kind = 'ATENDIMENTO';
UPDATE animal_costs SET category = 'CRECHE' WHERE kind IN ('CRECHE_MENSALIDADE', 'CRECHE_DIARIA');
UPDATE animal_costs SET category = 'OUTRO'  WHERE kind = 'COMPRA';

-- NOT NULL vem DEPOIS do retroativo, e e o que impede a proxima linha de nascer sem fatia.
--
-- Sem a restricao, um caminho de escrita que esquecesse a categoria passaria no teste e apareceria
-- na Tela 37 como dinheiro que sumiu do grafico — a soma das fatias nao fecharia com o total, e o
-- tutor nao teria como saber qual dos dois numeros acreditar.
ALTER TABLE animal_costs ALTER COLUMN category SET NOT NULL;

ALTER TABLE animal_costs ADD CONSTRAINT ck_animal_costs_category
    CHECK (category IN ('SAUDE', 'ALIMENTACAO', 'CRECHE', 'HIGIENE', 'OUTRO'));

-- A Tela 37 le por animal e por periodo, e agrupa por categoria. O indice do bloco 3 e
-- (animal_id, occurred_at DESC), que ja serve a lista "cada valor veio de um evento"; a soma por
-- fatia percorre o mesmo recorte, entao nao ha indice novo aqui. Sera medido quando doer.
