package br.com.petfy.healthcare.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-32-caracteres";
    private static final String EMAIL = "ulysses@petfy.com.br";
    private static final UUID SUBJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtService jwtService = new JwtService(SEGREDO, 120);

    private String tokenDeTutor() {
        return jwtService.generateToken(EMAIL, UserRole.OWNER, SUBJECT_ID);
    }

    @Test
    @DisplayName("deve gerar um token que devolve o email e o papel")
    void deveGerarTokenComEmailEPapel() {
        var principal = jwtService.extractPrincipal(tokenDeTutor());

        assertThat(principal).isPresent();
        assertThat(principal.get().getEmail()).isEqualTo(EMAIL);
        assertThat(principal.get().getRole()).isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("deve preservar o papel de veterinario")
    void devePreservarPapelDeVeterinario() {
        var token = jwtService.generateToken("vet@clinica.com.br", UserRole.VET, SUBJECT_ID);

        assertThat(jwtService.extractPrincipal(token))
                .get()
                .extracting(JwtPrincipal::getRole)
                .isEqualTo(UserRole.VET);
    }

    @Test
    @DisplayName("o token nao deve carregar a senha")
    void tokenNaoDeveCarregarSenha() {
        assertThat(tokenDeTutor()).doesNotContain("hashDaSenha");
    }

    @Test
    @DisplayName("deve recusar token assinado com outro segredo")
    void deveRecusarTokenDeOutroSegredo() {
        var tokenDeOutraChave = new JwtService(OUTRO_SEGREDO, 120)
                .generateToken(EMAIL, UserRole.OWNER, SUBJECT_ID);

        assertThat(jwtService.extractPrincipal(tokenDeOutraChave)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar token expirado")
    void deveRecusarTokenExpirado() {
        var token = new JwtService(SEGREDO, -1).generateToken(EMAIL, UserRole.OWNER, SUBJECT_ID);

        assertThat(jwtService.extractPrincipal(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nao-e-token", "a.b.c", "Bearer xpto"})
    @DisplayName("deve recusar token malformado sem lancar excecao")
    void deveRecusarTokenMalformado(String token) {
        assertThat(jwtService.extractPrincipal(token)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar token com papel desconhecido, em vez de tratar como sem papel")
    void deveRecusarPapelDesconhecido() {
        var comPapelEstranho = io.jsonwebtoken.Jwts.builder()
                .setSubject(EMAIL)
                .claim("role", "SUPERUSUARIO")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SEGREDO.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractPrincipal(comPapelEstranho)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar token sem o claim de papel")
    void deveRecusarTokenSemPapel() {
        var semPapel = io.jsonwebtoken.Jwts.builder()
                .setSubject(EMAIL)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SEGREDO.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractPrincipal(semPapel)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar segredo curto demais para HS256 na criacao do bean")
    void deveRecusarSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto", 120))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }

    @Test
    @DisplayName("o papel deve virar authority com o prefixo que o Spring Security espera")
    void papelDeveVirarAuthorityComPrefixo() {
        assertThat(UserRole.OWNER.asAuthority()).isEqualTo("ROLE_OWNER");
        assertThat(UserRole.VET.asAuthority()).isEqualTo("ROLE_VET");
    }
}
