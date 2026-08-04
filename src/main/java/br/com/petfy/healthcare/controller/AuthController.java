package br.com.petfy.healthcare.controller;

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
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.noContent().build();
    }

    /** Como o pedido de recuperacao: 202 sempre, para nao revelar quem tem conta. */
    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailVerificationResendDTO request) {
        emailVerificationService.resend(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmVerification(@Valid @RequestBody EmailVerificationConfirmDTO request) {
        emailVerificationService.confirm(request);
        return ResponseEntity.noContent().build();
    }

}
