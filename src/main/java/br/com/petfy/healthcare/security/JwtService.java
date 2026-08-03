package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Owner;
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

@Service
public class JwtService {

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

    public String generateToken(Owner owner) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .setSubject(owner.getEmail())
                .claim("ownerId", owner.getOwnerId().toString())
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(agora.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * Devolve vazio para qualquer token invalido - assinatura errada, expirado,
     * malformado ou ausente. Quem chama nao precisa distinguir os casos: em
     * todos eles a requisicao segue sem autenticacao.
     */
    public Optional<String> extractEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationMinutes() {
        return expiration.toMinutes();
    }

}
