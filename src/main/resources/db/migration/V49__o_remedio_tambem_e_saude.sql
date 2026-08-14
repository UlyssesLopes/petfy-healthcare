-- O remedio que o animal esta tomando, e nao so o que ele custou.
--
-- <b>REMEDIO E RACAO VIRAVAM CUSTO E NADA MAIS.</b> A Tela 42 lanca "Remedio, R$ 90" e o produto
-- guarda um numero na categoria SAUDE — e mais nada. Um remedio que o animal ESTA TOMANDO e fato de
-- saude: quem cuida dele nesta semana precisa saber que ha um comprimido as 8h, e a linha do tempo
-- precisa mostrar que houve tratamento. Nada disso saia de um valor.
--
-- ------------------------------------------------------- o modelo ja existia, e a ligacao faltava
--
-- <b>Nao ha entidade nova aqui, e essa e a parte boa.</b> A `care_instructions` nasceu no P3 como
-- "um conceito, tres usos que estavam separados: prescricao do veterinario, medicacao e tratamento
-- continuo, e tema de casa da creche" — e ela NAO exige credencial profissional, de proposito:
-- "prescricao de veterinario, medicacao de tutor e tema de casa tem a mesma forma, e so a primeira
-- envolve CRMV".
--
-- Ou seja: o tutor sempre pode dizer "o Code esta tomando isto". O que faltava era o gasto e o
-- tratamento serem a MESMA COISA vista de dois lados.
--
-- --------------------------------------------------- por que uma ligacao, e nao um campo de texto
--
-- A alternativa e `animal_costs.remedio varchar` — o nome do remedio no proprio custo. Nao serve: um
-- texto ali nao entra na linha do tempo, nao gera pendencia, nao tem prazo e nao pode ser cumprido.
-- Seria o mesmo "custo e nada mais" com uma palavra a mais dentro.
--
-- <b>A ligacao e a mesma que a dose de vacina ja tem</b> (`source_vaccine_id`), e pelo mesmo motivo:
-- ela e o que faz a previsao ter preco. Um tratamento que dura 21 dias e volta a cada seis meses e
-- exatamente o tipo de gasto que a Tela 39 existe para antecipar — e sem a ligacao o unico chute
-- disponivel seria "o ultimo custo de categoria SAUDE", que cobraria o vermifugo com o preco de uma
-- consulta.
ALTER TABLE animal_costs ADD COLUMN source_care_instruction_id uuid
    REFERENCES care_instructions(care_instruction_id) ON DELETE SET NULL;

-- ON DELETE SET NULL, e nao CASCADE: apagar a orientacao nao pode apagar o gasto.
--
-- O dinheiro saiu. Suspender um tratamento e informacao clinica — a `care_instructions` inclusive
-- ENCERRA em vez de apagar, por essa razao —, mas se um dia uma orientacao for removida, o que a
-- pessoa gastou continua tendo acontecido. Um custo que some da conta porque alguem mexeu no
-- tratamento faria a soma do ano mentir.

CREATE INDEX ix_animal_costs_orientacao ON animal_costs (source_care_instruction_id)
    WHERE source_care_instruction_id IS NOT NULL;
