-- O aviso que fica, e nao so o que sai por e-mail.
--
-- <b>O PRODUTO AVISA POR E-MAIL DESDE O P4, E NUNCA AVISOU DENTRO DELE.</b> A Tela 03 lista quem
-- esta vencendo e nao avisa ninguem; a 47 escreve "voce e avisado na hora" e o produto nao tinha
-- como cumprir. Quem nao confirmou o e-mail — e quem confirmou mas nao abre a caixa — nao recebia
-- nada, e a unica forma de saber que alguem entrou no seu animal era abrir o app e comparar a lista
-- de tutores com o que se lembrava dela.
--
-- ----------------------------------------------------- por que uma tabela, e nao um recalculo
--
-- A tentacao e "a tela pergunta ao servidor o que mudou desde a ultima visita" e nao guardar nada.
-- Nao serve, por duas razoes:
--
--   * <b>o aviso e um FATO, e nao uma consulta.</b> "Ana entrou no Code" e verdade mesmo depois de
--     Ana sair; recalcular a partir do estado atual apagaria o aviso junto com o que ele contava;
--   * <b>lido e nao-lido e do leitor.</b> Duas pessoas cuidam do mesmo animal e cada uma leu o que
--     leu — isso nao existe em lugar nenhum do estado do animal.
--
-- ------------------------------------------------------ o texto vem pronto, e nao por referencia
--
-- Guardamos ASSUNTO E CORPO, e nao "tipo do evento + ids para remontar depois". Remontar significa
-- que o aviso muda quando o mundo muda: "Ana passou a cuidar do Code" viraria "alguem passou a
-- cuidar do Code" no dia em que Ana apagasse a conta, e o histórico de avisos passaria a mentir
-- sobre o que a pessoa leu.
--
-- <b>E e o mesmo texto do e-mail, de proposito.</b> O canal in-app nasce da mesma `Notification` que
-- o e-mail — duas redacoes do mesmo evento divergiriam no primeiro ajuste de frase, e a pessoa que
-- recebe os dois leria coisas diferentes sobre o mesmo fato.
CREATE TABLE person_notifications (
    person_notification_id uuid PRIMARY KEY,

    -- O DONO DO AVISO, e apagar a conta apaga o que ela tinha para ler: aviso e dado de quem
    -- recebeu, nao registro do animal. O CASCADE fecha o buraco antes de ele existir — a
    -- `group_approvals` custou um 500 em producao por nao ter feito isso.
    person_id uuid NOT NULL REFERENCES persons(person_id) ON DELETE CASCADE,

    subject varchar(200) NOT NULL,
    body text NOT NULL,

    -- O evento que produziu o aviso, como o dispatcher ja o nomeia ("tutor que entrou no animal").
    -- Serve para agrupar e para depurar, e NAO para remontar o texto.
    event varchar(64),

    created_at timestamp NOT NULL,

    -- Nulo e "nao lido". Uma data em vez de um booleano porque "quando ela viu" responde perguntas
    -- que "se ela viu" nao responde — e nao custa nada a mais.
    read_at timestamp
);

-- O feed e sempre "os meus, do mais novo para o mais velho", e a contagem de nao-lidos e a mesma
-- consulta com um filtro. Um indice serve aos dois.
CREATE INDEX ix_person_notifications_pessoa ON person_notifications (person_id, created_at DESC);
