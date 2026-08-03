-- Controle de reenvio do lembrete de vacina.
--
-- Vacina vencida continua vencida indefinidamente. Sem marcar quando o aviso
-- saiu, a rotina diaria mandaria o mesmo lembrete todo dia ate o tutor vacinar
-- o pet - o caminho mais curto para virar spam e ser ignorado.
--
-- Fica na propria vacina em vez de tabela de log porque o que interessa e "ja
-- avisei sobre esta dose recentemente?". Historico de envio e outro problema, e
-- so vale a pena quando existir necessidade real de auditoria.

ALTER TABLE vaccines ADD COLUMN last_reminder_sent_at timestamp;

-- a rotina varre por data da proxima dose e depois filtra por ultimo envio
CREATE INDEX idx_vaccines_reminder ON vaccines (last_reminder_sent_at);
