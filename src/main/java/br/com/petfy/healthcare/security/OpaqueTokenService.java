package br.com.petfy.healthcare.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Token opaco para links que valem por si - carteira compartilhada e convite de
 * clinica.
 *
 * Centralizado porque duas implementacoes de hash divergindo e exatamente o tipo
 * de erro que passa despercebido em codigo de seguranca: uma delas ficaria mais
 * fraca sem ninguem notar.
 */
@Component
public class OpaqueTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 32 bytes de SecureRandom: o token e a unica barreira, precisa ser inadivinhavel. */
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 e nao BCrypt: a busca precisa ser por igualdade, e o token ja tem
     * 256 bits de entropia - nao ha o que proteger contra forca bruta como numa
     * senha escolhida por gente.
     */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

}
