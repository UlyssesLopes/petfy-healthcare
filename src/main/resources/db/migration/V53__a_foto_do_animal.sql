-- A foto de perfil do animal: qual dos anexos dele e a foto. Nulo e "sem foto".
--
-- Ate aqui a foto do cadastro subia como um anexo qualquer, e nenhuma tela sabia qual anexo
-- era o rosto do animal. Apagar o anexo tira a foto, e nada mais muda.
ALTER TABLE animals ADD COLUMN photo_attachment_id uuid
    REFERENCES attachments (attachment_id) ON DELETE SET NULL;
