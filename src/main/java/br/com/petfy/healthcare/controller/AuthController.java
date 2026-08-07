package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.EmailVerificationConfirmDTO;
import br.com.petfy.healthcare.domain.dto.EmailVerificationResendDTO;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordResetConfirmDTO;
import br.com.petfy.healthcare.domain.dto.PasswordResetRequestDTO;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Entra na conta",
               description = "Devolve o JWT. Trocar a senha invalida os tokens emitidos antes, entao o "
                             + "cliente recebe 401 num token que ainda nao expirou - e precisa "
                             + "distinguir isso de expiracao.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Responde 202 sempre, tenha ou nao conta com aquele e-mail. O 202 e honesto
     * aqui: a requisicao foi aceita, e o que acontece depois nao e assunto de
     * quem pediu - se fosse, o endpoint publico viraria um verificador de quem
     * tem conta cadastrada.
     */
    @Operation(summary = "Pede a recuperacao de senha",
               description = "Responde igual exista ou nao a conta: dizer que o e-mail nao existe "
                             + "entregaria quem tem conta aqui a quem tenta descobrir.")
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirma a nova senha com o token recebido",
               description = "Derruba as sessoes abertas: quem trocou a senha porque desconfiou de "
                             + "acesso indevido nao teria ganhado nada se o token do invasor "
                             + "continuasse valendo.")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.noContent().build();
    }

    /** Como o pedido de recuperacao: 202 sempre, para nao revelar quem tem conta. */
    @Operation(summary = "Reenvia a confirmacao de e-mail",
               description = "O e-mail e confirmado antes de o produto notificar qualquer coisa: "
                             + "lembrete de vacina indo para endereco errado e dado de saude entregue "
                             + "a estranho.")
    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailVerificationResendDTO request) {
        emailVerificationService.resend(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirma o e-mail com o token recebido")
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmVerification(@Valid @RequestBody EmailVerificationConfirmDTO request) {
        emailVerificationService.confirm(request);
        return ResponseEntity.noContent().build();
    }

}
