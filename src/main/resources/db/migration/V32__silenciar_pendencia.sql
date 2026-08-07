-- Silenciar pendencia.
--
-- "Silencio e funcionalidade. O tutor precisa poder silenciar SEM QUE O REGISTRO PARE.
-- Produto de saude que nao pode ser calado e desinstalado, e ai para de registrar tambem."
-- (PRODUTO 4.2, regra dura)
--
-- E o DESIGN 5.3 diz onde a acao mora: "silenciar mora na pendencia, nao em preferencias.
-- Silencio e funcionalidade, e funcionalidade escondida em configuracao nao e oferecida -
-- e escondida."
--
-- ------------------------------------------------------ o registro nao para, e e o ponto
--
-- Esta tabela nao toca vacina, antiparasitario nem orientacao. A proxima dose continua
-- calculada, a orientacao continua vigente, a linha do tempo continua recebendo tudo. O
-- que muda e apenas o que aparece no feed de quem silenciou - literalmente calar a
-- cobranca, e nao desligar o cuidado.
--
-- ---------------------------------------------------------------- por pessoa, e nao por animal
--
-- A chave inclui person_id porque a cobranca e por pessoa: dois tutores dividem o cuidado
-- e dividem a cobranca (8a), e um silenciar nao pode calar o outro. Silenciar para todos
-- seria uma pessoa decidindo o que a outra ve sobre a saude do mesmo animal.
--
-- ---------------------------------------------------------- por item, e nao por tipo
--
-- A chave inclui source_id, e nao so o tipo. "Silenciar todas as doses de vacina" seria
-- preferencia global disfarcada - exatamente o que o 5.3 recusa. E como a pendencia e
-- derivada da fonte, o silencio se resolve sozinho no caso comum: registrada a proxima
-- dose, nasce outra pendencia com outro source_id, e ela cobra normalmente. Silenciar nao
-- vira esquecer para sempre.
--
-- ------------------------------------------- consentimento nao entra, e a recusa e no servico
--
-- CONSENTIMENTO_PENDENTE nao tem source_id, e nao e por acaso: ele nao deriva de um
-- registro, e o proprio PRODUTO diz que ele bloqueia o resto do produto. Silencia-lo seria
-- esconder o bloqueio, e o usuario descobriria ao bater nele - que e o cenario que o 4.2
-- descreve como o que nao pode acontecer. A recusa fica no servico, com mensagem propria; o
-- banco apenas exige source_id, o que ja o torna impossivel de gravar aqui.

CREATE TABLE due_item_silences (
    due_item_silence_id uuid        NOT NULL,
    person_id           uuid        NOT NULL,
    kind                varchar(32) NOT NULL,
    source_id           uuid        NOT NULL,
    silenced_at         timestamp   NOT NULL,

    CONSTRAINT pk_due_item_silences PRIMARY KEY (due_item_silence_id),
    CONSTRAINT fk_due_item_silences_person
        FOREIGN KEY (person_id) REFERENCES persons (person_id),

    -- Silenciar duas vezes e a mesma coisa que silenciar uma. O indice unico e o que faz a
    -- operacao ser idempotente no banco, e nao so na intencao do servico - mesma postura da
    -- revogacao de acesso, que responde no-op em vez de erro na segunda chamada.
    CONSTRAINT uk_due_item_silences_pessoa_item UNIQUE (person_id, kind, source_id)
);

-- A consulta e sempre "o que esta pessoa silenciou", para filtrar o feed dela.
CREATE INDEX idx_due_item_silences_person ON due_item_silences (person_id);
