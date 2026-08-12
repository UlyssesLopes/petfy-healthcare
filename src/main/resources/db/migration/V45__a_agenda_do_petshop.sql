-- A agenda de banho e tosa (Tela 18).
--
-- <b>"O petshop e AGENDA, nao diaria de cuidado: alta frequencia, quatro linhas de saude e nenhuma
-- autoridade clinica."</b> E a frase que decide esta migracao inteira, e a pergunta que ela responde
-- primeiro e por que isto nao e uma `enrollment`.
--
-- ------------------------------------------------------- por que nao e matricula com presenca
--
-- A creche (Telas 17 e 41) tem MATRICULA: vaga numa turma, dias combinados da semana, mensalidade, e
-- uma presenca por dia letivo. Tudo isso existe porque a creche e um vinculo continuo — o animal
-- pertence a uma turma, e a turma tem capacidade.
--
-- O petshop nao tem nada disso. Nao ha vaga a ocupar, nao ha turma, nao ha mensalidade, e o animal
-- volta quando o pelo cresce. Modelar como matricula obrigaria a inventar uma turma por petshop e uma
-- matricula perpetua por animal, e a Tela 17 passaria a mostrar cachorros que nunca vao a creche
-- nenhuma. <b>A unidade aqui e o COMPROMISSO de um dia e uma hora</b>, e ela nao existia.
--
-- ------------------------------------------------------- e por que nao e um `animal_cost`
--
-- Porque o agendamento acontece ANTES de qualquer valor, e a maior parte dele nunca vira valor
-- nenhum: o banho e marcado na terca para sexta, e o preco — se for lancado — e um fato do dia do
-- banho. Um custo com data futura afirmaria um gasto que ainda nao houve.
CREATE TABLE service_appointments (
    service_appointment_id  uuid         NOT NULL,

    -- Quem presta o servico. O petshop.
    organization_id         uuid         NOT NULL,

    animal_id               uuid         NOT NULL,

    -- Dia E HORA, e e isto que separa esta tabela da presenca na creche.
    --
    -- A presenca guarda `date` porque a pergunta la e "ele veio hoje?". Aqui a agenda do dia e uma
    -- coluna de horarios: "08h00 Nina, 09h30 Code, 11h00 Amora". Guardar so o dia obrigaria o petshop
    -- a ordenar oito banhos por ordem de digitacao.
    scheduled_at            timestamp    NOT NULL,

    -- "Banho e tosa higienica", "tosa na tesoura", "banho e hidratacao".
    --
    -- <b>Texto livre, e nao um catalogo de servicos.</b> Um enum ou uma tabela de precos seria o
    -- comeco de um sistema de gestao de petshop — e este produto e o registro da vida do animal, nao
    -- o PDV de quem o atende. Cada petshop escreve o que faz do jeito que fala com o cliente.
    service                 varchar(120) NOT NULL,

    status                  varchar(24)  NOT NULL,

    -- "Marcar entrada" e "Entregue as 9h20". Nulos ate acontecerem.
    checked_in_at           timestamp,
    completed_at            timestamp,

    -- Quem marcou o compromisso. Nao e quem atendeu: o atendimento e assinado pela observacao que o
    -- fecha, e essa assinatura ja tem dono proprio.
    created_by_person_id    uuid         NOT NULL,

    creation_date           timestamp    NOT NULL,

    CONSTRAINT pk_service_appointments PRIMARY KEY (service_appointment_id),
    CONSTRAINT fk_service_appointments_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_service_appointments_animal FOREIGN KEY (animal_id)
        REFERENCES animals (animal_id),
    CONSTRAINT fk_service_appointments_created_by FOREIGN KEY (created_by_person_id)
        REFERENCES persons (person_id),

    CONSTRAINT ck_service_appointments_status
        CHECK (status IN ('AGENDADO', 'EM_ATENDIMENTO', 'CONCLUIDO', 'FALTOU')),

    -- OS CARIMBOS TEM DE COMBINAR COM O ESTADO. Sem isto, um AGENDADO com `completed_at` preenchido
    -- apareceria na agenda de hoje como pendente e no historico como feito — e as duas telas
    -- discordariam sobre o mesmo banho.
    CONSTRAINT ck_service_appointments_carimbos CHECK (
        (status = 'AGENDADO'       AND checked_in_at IS NULL     AND completed_at IS NULL) OR
        (status = 'EM_ATENDIMENTO' AND checked_in_at IS NOT NULL AND completed_at IS NULL) OR
        (status = 'CONCLUIDO'      AND completed_at IS NOT NULL) OR
        (status = 'FALTOU'         AND completed_at IS NULL)
    ),

    -- Entregar antes de receber nao acontece.
    CONSTRAINT ck_service_appointments_ordem
        CHECK (completed_at IS NULL OR checked_in_at IS NULL OR completed_at >= checked_in_at)
);

-- UM COMPROMISSO POR ANIMAL, POR PETSHOP, POR HORARIO.
--
-- Nao e higiene: e o duplo clique no botao "Agendar banho". Sem o indice, o mesmo banho apareceria
-- duas vezes as 09h30, e o tosador marcaria entrada num e deixaria o outro pendurado na agenda o dia
-- inteiro.
CREATE UNIQUE INDEX ux_service_appointments_horario
    ON service_appointments (organization_id, animal_id, scheduled_at);

-- "Segunda, 10 de agosto · 8 banhos hoje" — a consulta que a tela faz o tempo todo.
CREATE INDEX ix_service_appointments_do_dia
    ON service_appointments (organization_id, scheduled_at);

-- O historico do animal naquele petshop, para a tela do tutor e para o purge.
CREATE INDEX ix_service_appointments_do_animal
    ON service_appointments (animal_id, scheduled_at DESC);
