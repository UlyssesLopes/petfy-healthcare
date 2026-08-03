package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Owner;
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

    private final JwtService jwtService = new JwtService(SEGREDO, 120);

    private Owner owner() {
        return Owner.builder()
                .ownerId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("ulysses@petfy.com.br")
                .build();
    }

    @Test
    @DisplayName("deve gerar um token que devolve o email do owner")
    void deveGerarTokenQueDevolveEmail() {
        var token = jwtService.generateToken(owner());

        assertThat(jwtService.extractEmail(token)).contains("ulysses@petfy.com.br");
    }

    @Test
    @DisplayName("o token nao deve carregar a senha do owner")
    void tokenNaoDeveCarregarSenha() {
        var owner = owner();
        owner.setPassword("$2a$10$hashDaSenha");

        var token = jwtService.generateToken(owner);

        assertThat(token).doesNotContain("hashDaSenha");
    }

    @Test
    @DisplayName("deve recusar token assinado com outro segredo")
    void deveRecusarTokenDeOutroSegredo() {
        var tokenDeOutraChave = new JwtService(OUTRO_SEGREDO, 120).generateToken(owner());

        assertThat(jwtService.extractEmail(tokenDeOutraChave)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar token expirado")
    void deveRecusarTokenExpirado() {
        var jwtExpirado = new JwtService(SEGREDO, -1);

        var token = jwtExpirado.generateToken(owner());

        assertThat(jwtService.extractEmail(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nao-e-token", "a.b.c", "Bearer xpto"})
    @DisplayName("deve recusar token malformado sem lancar excecao")
    void deveRecusarTokenMalformado(String token) {
        assertThat(jwtService.extractEmail(token)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar segredo curto demais para HS256 na criacao do bean")
    void deveRecusarSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto", 120))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}
