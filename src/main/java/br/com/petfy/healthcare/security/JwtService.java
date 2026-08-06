package br.com.petfy.healthcare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_SUBJECT_ID = "subjectId";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${petfy.jwt.secret}") String secret,
                      @Value("${petfy.jwt.expiration-minutes:120}") long expirationMinutes) {
        // HS256 exige chave de no minimo 256 bits; o Keys.hmacShaKeyFor recusa
        // segredo curto, o que faz a aplicacao falhar na subida em vez de
        // aceitar silenciosamente um token fraco
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(String email, UUID subjectId) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_SUBJECT_ID, subjectId.toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * Devolve vazio para qualquer token invalido - assinatura errada, expirado,
     * malformado ou ausente. Quem chama nao precisa distinguir os casos: em todos
     * eles a requisicao segue sem autenticacao.
     *
     * O claim de papel saiu no P1. Token emitido antes disso continua valendo ate
     * expirar: o claim a mais e ignorado, e o que a pessoa alcanca passa a sair
     * dos vinculos dela em vez de vir carimbado dentro do token.
     */
    public Optional<JwtPrincipal> extractPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            Date issuedAt = claims.getIssuedAt();

            // sem iat nao ha como saber se o token e anterior a uma troca de
            // senha, entao ele e tratado como invalido em vez de passar como se
            // fosse recente. Todo token emitido aqui tem iat
            if (email == null || issuedAt == null) {
                return Optional.empty();
            }

            return Optional.of(new JwtPrincipal(email, issuedAt.toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationMinutes() {
        return expiration.toMinutes();
    }

}
