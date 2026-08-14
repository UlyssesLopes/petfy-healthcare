-- Os aparelhos conectados (Tela 36), e a linha que ficou por construir desde o bloco 9.
--
-- O desenho pede "3 sessoes abertas. A mais antiga e de 11/2025", e a tela responde que NAO SABE.
-- A razao registrada foi: "este produto autentica com JWT sem estado — o servidor nao sabe quantos
-- tokens validos existem, e nao teria como invalidar um deles".
--
-- ------------------------------------------------------------ a premissa estava errada
--
-- <b>O servidor JA consulta o banco a cada requisicao autenticada.</b> O `TokenFreshness` faz um
-- `findPasswordChangedAtByEmail` em TODA rota autenticada, desde o P1, para recusar token emitido
-- antes de uma troca de senha. O custo que este arquivo supostamente introduzia — "deixar de ser
-- stateless" — ja estava pago, e por um recurso que existe pela mesma razao: derrubar uma sessao
-- que a pessoa nao reconhece.
--
-- Entao o que falta nao e arquitetura. E uma linha por sessao.
--
-- ------------------------------------------------------ o que esta tabela NAO e
--
-- <b>Nao e sessao de servidor.</b> O token continua sendo a credencial, continua sendo assinado, e
-- continua valendo pelo prazo dele sem que ninguem precise consultar nada para EMITIR. O que a
-- tabela permite e o inverso: dizer que um token especifico deixou de valer antes da hora.
--
-- <b>E nao e refresh token.</b> Recarregar a pagina continua deslogando — o token vive em memoria no
-- cliente, e isso e decisao do ROADMAP contra XSS. O refresh em cookie httpOnly e outro trabalho, e
-- depende da topologia de dominio; esta tabela e pre-requisito dele, e nao substituto.
CREATE TABLE person_sessions (
    person_session_id uuid PRIMARY KEY,

    person_id uuid NOT NULL REFERENCES persons(person_id) ON DELETE CASCADE,

    created_at timestamp NOT NULL,

    -- Nulo e "ainda vale". A sessao encerrada NAO e apagada: "3 sessoes abertas, a mais antiga e de
    -- 11/2025" e uma leitura sobre as vigentes, mas quem encerra uma sessao por suspeita de acesso
    -- indevido tem interesse em que o registro de que ela existiu permaneca.
    revoked_at timestamp,

    -- COMO A PESSOA RECONHECE O APARELHO, e nada alem disso.
    --
    -- O user agent, cru, como o navegador o mandou. Nao guardamos IP: ele nao ajuda ninguem a
    -- reconhecer o proprio aparelho — "189.4.x.x" nao diz nada a quem esta lendo —, e guardar
    -- localizacao de quem usa o produto e exatamente o que a Tela 34 recusa quando diz "nao
    -- guardamos quem fez a busca, e por onde".
    user_agent varchar(400)
);

-- A pergunta e sempre "as minhas, das mais novas para as mais velhas".
CREATE INDEX ix_person_sessions_pessoa ON person_sessions (person_id, created_at DESC);
