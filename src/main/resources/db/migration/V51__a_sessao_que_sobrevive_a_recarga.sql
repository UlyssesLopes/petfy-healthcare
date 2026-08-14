-- A sessao que sobrevive a recarga da pagina.
--
-- <b>O TOKEN VIVE EM MEMORIA NO CLIENTE, E ISSO NAO MUDA AQUI.</b> `localStorage` esta fora desde o
-- inicio, e o motivo esta escrito no `sessao.ts`: "e a exposicao a XSS que o ROADMAP.md recusa por
-- escrito. O custo aceito e login a cada recarga da pagina".
--
-- O mesmo arquivo nomeia o gatilho para pagar esse custo de outro jeito: "no dia em que o refresh em
-- cookie httpOnly entrar, ele passa a chamar /auth/refresh. O gatilho da troca esta no ROADMAP.md: o
-- dominio proprio pondo front e API sob o mesmo site, ou <b>o login a cada recarga se mostrar
-- insuportavel no uso real</b>".
--
-- O segundo aconteceu. Recarregar a pagina deslogava, e no uso real isso acontece o tempo todo.
--
-- ------------------------------------------------------- por que nao ha tabela nova
--
-- <b>A `person_sessions` da V50 e o lugar.</b> Ela ja e "uma entrada na conta" — a Tela 36 lista, e
-- encerrar uma delas ja derruba o token daquela entrada. O refresh e a MESMA entrada vista pelo
-- outro lado: enquanto ela vale, o cliente troca um token vencido por um novo.
--
-- Uma tabela separada faria "sessao encerrada" e "refresh revogado" serem dois fatos que precisariam
-- concordar — e o dia em que discordassem, alguem continuaria entrando numa sessao que a pessoa
-- encerrou olhando para a tela.
ALTER TABLE person_sessions ADD COLUMN refresh_token_hash varchar(64);

-- Um por sessao: rotacionar substitui, e nao acumula.
CREATE UNIQUE INDEX ux_person_sessions_refresh ON person_sessions (refresh_token_hash)
    WHERE refresh_token_hash IS NOT NULL;

-- ATE QUANDO A ENTRADA VALE, e e o prazo que a pessoa sente.
--
-- <b>E longo de proposito — trinta dias.</b> O JWT continua durando duas horas, e e ele que autoriza
-- cada requisicao; este prazo diz por quanto tempo o navegador pode pedir um JWT novo sem digitar a
-- senha. Curto demais reintroduz o problema que a migration existe para resolver, so que semanal.
--
-- <b>E encurtar isto NAO e a trava de seguranca</b>: a trava e o encerramento da sessao, que ja
-- existe e derruba na proxima requisicao, e a troca de senha, que derruba todas.
ALTER TABLE person_sessions ADD COLUMN refresh_expires_at timestamp;

-- AS SESSOES QUE JA EXISTEM FICAM SEM REFRESH, e e o comportamento certo.
--
-- Elas nasceram sem cookie nenhum no navegador de quem entrou; inventar um hash aqui criaria um
-- refresh que ninguem tem como apresentar. Quem esta logado agora continua logado ate o JWT expirar,
-- e ganha o refresh no proximo login — que e o mesmo criterio do `sid` na V50.
