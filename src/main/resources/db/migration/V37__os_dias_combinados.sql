-- Os dias combinados da matricula (Tela 41).
--
-- A V36 guardou os tres valores do combinado — mensalidade, dia de vencimento e diaria avulsa —
-- e deixou a diaria sem quem a dispare. Esta migration existe por causa de uma frase do desenho
-- que nao tinha nenhum dado atras dela:
--
--   <b>"Diaria avulsa: usada quando o Code vem FORA DOS DIAS COMBINADOS."</b>
--
-- E do outro lado, o que o tutor le sem perguntar: "Creche Quintal · mensalidade — 3 DIAS POR
-- SEMANA, vence dia 5". As mesmas palavras voltam nas Telas 37-39.
--
-- ----------------------------------------------------------- por que a coluna precisou existir
--
-- <b>Nao havia, em lugar nenhum do modelo, o dia em que o animal e esperado.</b> A matricula tinha
-- turma, estado e datas; a presenca nascia ESPERADO para toda matricula viva, todo dia. Sem os
-- dias, "fora do combinado" nao e uma pergunta que o servidor saiba responder — e a diaria avulsa,
-- que e o encadeamento central do bloco, so poderia entrar se alguem digitasse. O desenho recusa
-- isso com todas as letras: "a diaria avulsa entrou sozinha (...) NINGUEM DIGITOU NADA".
--
-- As duas saidas sem coluna foram olhadas e recusadas:
--
--   * "diaria so quando nao ha mensalidade" contradiz o proprio exemplo do desenho, que mostra
--     R$ 530/mes E uma diaria de 22/07 no mesmo animal, na mesma matricula;
--   * "a creche marca avulso no check-in" transfere para quem esta com quinze cachorros na porta
--     as 7h30 uma decisao sobre a conta do tutor, e apaga o "ninguem digitou nada".
--
-- ------------------------------------------------------------------- tabela, e nao varchar com
--
-- Uma coluna `weekdays varchar(32)` guardando "MONDAY,WEDNESDAY,FRIDAY" caberia. Nao serve: o
-- banco deixaria de saber o que ha ali dentro, e "quem vem na quarta" viraria um LIKE que casa
-- com qualquer coisa. E o mesmo criterio que fez `grant_scopes` ser tabela.
CREATE TABLE enrollment_weekdays (
    enrollment_id uuid        NOT NULL,

    -- MONDAY..SUNDAY, o nome do java.time.DayOfWeek.
    --
    -- O NOME E NAO O NUMERO: 1 e segunda na ISO e domingo em metade das bibliotecas de tela, e
    -- essa e a classe de erro que aparece uma vez por ano, no dia errado, para um animal so.
    weekday       varchar(12) NOT NULL,

    CONSTRAINT pk_enrollment_weekdays PRIMARY KEY (enrollment_id, weekday),
    CONSTRAINT fk_enrollment_weekdays_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments (enrollment_id)
);

-- ------------------------------------------------------------------ conjunto vazio e "nao sei"
--
-- Matricula sem nenhum dia declarado e o caso comum, e nao pendencia: a creche que nunca abriu a
-- caixa do combinado tem zero linhas aqui. <b>E dai a diaria NUNCA entra sozinha</b> — sem saber
-- quais dias sao os combinados, todo dia seria "fora do combinado", e o produto passaria a cobrar
-- do tutor por um dado que ninguem informou. O silencio nao pode virar cobranca.
