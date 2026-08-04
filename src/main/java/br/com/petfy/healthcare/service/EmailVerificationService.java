package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.EmailVerificationConfirmDTO;
import br.com.petfy.healthcare.domain.dto.EmailVerificationResendDTO;
import br.com.petfy.healthcare.domain.entity.Owner;

public interface EmailVerificationService {

    /** Chamado no cadastro. Falha de envio nao desfaz a criacao da conta. */
    void sendVerification(Owner owner);

    /**
     * Reenvio pedido pelo tutor. Como o de recuperacao de senha, responde igual
     * exista ou nao a conta, e tem cooldown.
     */
    void resend(EmailVerificationResendDTO request);

    void confirm(EmailVerificationConfirmDTO request);

}
