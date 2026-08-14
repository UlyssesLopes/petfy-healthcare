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

    /** Qual entrada na conta emitiu este token. Ausente nos anteriores a V50. */
    private static final String CLAIM_SESSION_ID = "sid";

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

    /** Sem sessao: o token vale ate expirar, e so a troca de senha o derruba. */
    public String generateToken(String email, UUID subjectId) {
        return generateToken(email, subjectId, null);
    }

    /**
     * Com sessao, e e o que a Tela 36 precisava.
     *
     * <b>O {@code sid} e o que permite encerrar UM aparelho.</b> Sem ele o unico jeito de derrubar
     * uma entrada era trocar a senha, que derruba todas — e a tela dizia isso porque era verdade.
     *
     * <b>Token sem {@code sid} continua valendo</b>, e o precedente e o do claim {@code role} que
     * morreu no P1: quem esta logado agora nao e deslogado por uma migracao. O que ele perde e
     * poder ser encerrado individualmente, ate expirar.
     */
    public String generateToken(String email, UUID subjectId, UUID sessionId) {
        Instant agora = Instant.now();

        var builder = Jwts.builder()
                .subject(email)
                .claim(CLAIM_SUBJECT_ID, subjectId.toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiration)));

        if (sessionId != null) {
            builder.claim(CLAIM_SESSION_ID, sessionId.toString());
        }

        return builder.signWith(key).compact();
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

            return Optional.of(new JwtPrincipal(email, issuedAt.toInstant(), sessaoDe(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Nulo quando o token e anterior a V50, ou quando o claim veio corrompido.
     *
     * <b>Um {@code sid} ilegivel vira ausencia, e nao token invalido.</b> Recusar seria transformar
     * um defeito de leitura num deslogue — e o token continua assinado por nos, dentro da validade,
     * com a troca de senha ainda valendo sobre ele.
     */
    private UUID sessaoDe(Claims claims) {
        Object sid = claims.get(CLAIM_SESSION_ID);

        if (sid == null) {
            return null;
        }

        try {
            return UUID.fromString(sid.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpirationMinutes() {
        return expiration.toMinutes();
    }

}
