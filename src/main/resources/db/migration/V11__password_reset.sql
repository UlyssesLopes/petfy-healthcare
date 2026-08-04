-- Recuperacao de senha por e-mail.
--
-- Sem isto, quem esquece a senha perde a conta e o historico do pet junto: nao
-- existe caminho de volta que nao passe por alguem mexer no banco na mao.
--
-- Guarda o hash, nao o token, pelo mesmo motivo de pet_shares: o token e a unica
-- credencial do fluxo, entao um vazamento do banco nao pode entregar as
-- recuperacoes em aberto. A consequencia e que o token so existe uma vez, no
-- envio - nao ha como reexibir o que nao esta guardado.
--
-- used_at em vez de apagar a linha: token ja usado precisa continuar
-- reconhecivel, senao reenviar o mesmo link responderia igual a token
-- inexistente e esconderia o uso indevido de quem interceptou o e-mail.
--
-- So owners. O vet ainda nao troca a propria senha, e uma tabela com duas chaves
-- estrangeiras opcionais - uma por papel - so faz sentido quando o segundo papel
-- existir de verdade.

CREATE TABLE password_reset_tokens (
    password_reset_token_id uuid         NOT NULL,
    owner_id                uuid         NOT NULL,
    token_hash              varchar(255) NOT NULL,
    expires_at              timestamp    NOT NULL,
    used_at                 timestamp,
    creation_date           timestamp    NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (password_reset_token_id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_owner FOREIGN KEY (owner_id) REFERENCES owners (owner_id)
);

-- a busca do fluxo de confirmacao e sempre por hash; o indice de owner serve
-- para invalidar os pedidos anteriores quando um novo e emitido
CREATE INDEX idx_password_reset_tokens_owner ON password_reset_tokens (owner_id);
