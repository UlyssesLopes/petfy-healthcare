-- Verificacao de e-mail do tutor.
--
-- O e-mail e ao mesmo tempo a chave do login e o canal por onde saem lembrete de
-- vacina e aviso de vacina registrada. Sem confirmar que o endereco e de quem se
-- cadastrou, o sistema pode passar a mandar nome do pet e do tutor para a caixa
-- de um estranho - que e o risco real, e nao o login.
--
-- Por isso e-mail nao verificado nao bloqueia entrar: bloquear cria atrito no
-- cadastro para proteger contra outra coisa. Quem nao verificou entra e usa, mas
-- nao recebe notificacao ate confirmar.
--
-- Nulo em quem nunca verificou, inclusive nas contas que ja existiam: elas sao
-- anteriores a esta regra e nao ha como afirmar que aqueles enderecos foram
-- confirmados. Na pratica, elas param de receber notificacao ate verificarem.
--
-- Tabela de token separada da de recuperacao de senha, e nao uma generica com
-- coluna de proposito, pelo mesmo motivo da V9: chave estrangeira de verdade e
-- regras proprias - aqui a validade e longa, la e curta, porque o token de
-- recuperacao troca a senha e este apenas confirma um endereco.

ALTER TABLE owners ADD COLUMN email_verified_at timestamp;

CREATE TABLE email_verification_tokens (
    email_verification_token_id uuid         NOT NULL,
    owner_id                    uuid         NOT NULL,
    token_hash                  varchar(255) NOT NULL,
    expires_at                  timestamp    NOT NULL,
    used_at                     timestamp,
    creation_date               timestamp    NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (email_verification_token_id),
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_owner FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

CREATE INDEX idx_email_verification_tokens_owner ON email_verification_tokens (owner_id);
