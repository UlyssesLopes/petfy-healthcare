-- Instante da ultima troca de senha, por conta.
--
-- A API e stateless: o token vale ate expirar e nada no servidor sabe que ele
-- deixou de ser desejado. Sem esta coluna, trocar a senha nao expulsa ninguem -
-- quem trocou por suspeita de acesso indevido continua com a outra sessao
-- ativa, que e o oposto do que a troca deveria conseguir.
--
-- Com ela, o filtro compara o iat do token com este instante e recusa o que foi
-- emitido antes. Fica em owners e vets porque a recuperacao de senha vai valer
-- para os dois, mesmo que hoje so o owner troque a propria senha.
--
-- Nulo de proposito para quem nunca trocou: nulo significa "nada a invalidar",
-- e nao exige carimbar uma data inventada nas contas que ja existem.

ALTER TABLE owners ADD COLUMN password_changed_at timestamp;
ALTER TABLE vets   ADD COLUMN password_changed_at timestamp;
