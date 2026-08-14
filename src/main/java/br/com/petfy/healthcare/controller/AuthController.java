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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final br.com.petfy.healthcare.service.SessionRenewal sessionRenewal;
    private final br.com.petfy.healthcare.security.RefreshCookie refreshCookie;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Entra na conta",
               description = "Devolve o JWT. Trocar a senha invalida os tokens emitidos antes, entao o "
                             + "cliente recebe 401 num token que ainda nao expirou - e precisa "
                             + "distinguir isso de expiracao.")
    /*
     * O USER AGENT VEM PELO CONTROLLER, e nao por um `HttpServletRequest` injetado no servico.
     *
     * Ele e a unica coisa que a Tela 36 tem para a pessoa reconhecer o proprio aparelho, e chega
     * aqui como o que e: um cabecalho. Injetar o request no servico faria toda a regra de login
     * depender do servlet para ler uma string, e os testes teriam de montar um mundo HTTP para
     * afirmar sobre senha.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        var autenticada = authService.login(request, userAgent);

        /*
         * O JWT VAI NO CORPO E O REFRESH VAI NO COOKIE, e os dois caminhos sao a decisao.
         *
         * O cliente precisa LER o JWT para manda-lo no header, entao ele nao pode ser httpOnly. O
         * refresh, ao contrario, o cliente nunca precisa ler — so precisa que o navegador o guarde e
         * o reenvie. Poe-lo no corpo devolveria ao XSS exatamente o que o cookie httpOnly existe
         * para tirar do alcance dele.
         */
        return ResponseEntity.ok()
                .header(refreshCookie.header(),
                        refreshCookie.paraDefinir(autenticada.refreshToken(),
                                autenticada.manterConectado()))
                .body(autenticada.corpo());
    }

    @Operation(summary = "Troca o token vencido por um novo, sem pedir a senha",
               description = "Le o refresh do cookie httpOnly e devolve um JWT novo. E o que faz a "
                             + "sessao sobreviver a RECARGA DA PAGINA: o JWT vive em memoria no "
                             + "cliente por decisao contra XSS, e recarregar sempre o perdia. "
                             + "ROTACIONA SEMPRE — o refresh apresentado morre aqui, entao um cookie "
                             + "copiado deixa de valer no primeiro refresh legitimo do dono. Recusa "
                             + "com 401 e um estado so: inexistente, ja rotacionado, expirado e de "
                             + "sessao encerrada respondem igual.")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(HttpServletRequest request) {
        var renovada = sessionRenewal.renovar(refreshCookie.ler(request).orElse(""));

        return ResponseEntity.ok()
                // a escolha feita no login manda aqui: renovar nao promove a temporaria
                .header(refreshCookie.header(),
                        refreshCookie.paraDefinir(renovada.refreshToken(),
                                renovada.sessao().persistente()))
                .body(LoginResponseDTO.builder()
                        .token(renovada.token())
                        .tokenType("Bearer")
                        .expiresInMinutes(renovada.expiresInMinutes())
                        .personId(renovada.pessoa().getPersonId())
                        // lido AGORA, e nao carimbado: credencial suspensa hoje muda a resposta de
                        // hoje, e o refresh e justamente onde uma sessao longa reencontra a verdade
                        .professional(authService.temCredencialAtiva(renovada.pessoa().getEmail()))
                        .build());
    }

    @Operation(summary = "Sai da conta",
               description = "Encerra a entrada no servidor e apaga o cookie. ANTES ISTO NAO "
                             + "EXISTIA: o cliente jogava o token fora e o JWT seguia valido ate "
                             + "expirar. Responde 204 mesmo sem cookie — quem entrou antes da V51 "
                             + "nao tem um, e sair nao pode falhar por isso.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionRenewal.sair(refreshCookie.ler(request).orElse(null));

        return ResponseEntity.noContent()
                .header(refreshCookie.header(), refreshCookie.paraApagar())
                .build();
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
