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
        return jwtService.generateToken(EMAIL, SUBJECT_ID);
    }

    @Test
    @DisplayName("deve gerar um token que devolve o email")
    void deveGerarTokenComEmail() {
        var principal = jwtService.extractPrincipal(tokenDeTutor());

        assertThat(principal).isPresent();
        assertThat(principal.get().getEmail()).isEqualTo(EMAIL);
    }

    /**
     * Substituiu dois casos que verificavam o papel dentro do token. O papel
     * morreu no P1b, e este teste protege o motivo: papel no token e uma copia do
     * vinculo, e copia envelhece. Credencial suspensa no meio da validade
     * continuaria autorizando ato clinico ate o token expirar.
     *
     * A assercao e sobre o corpo do JWT, e nao sobre o JwtPrincipal: um claim que
     * voltasse a ser emitido sem ninguem ler passaria despercebido por qualquer
     * teste que so olhasse o objeto.
     */
    @Test
    @DisplayName("o token nao deve carregar papel nenhum")
    void tokenNaoDeveCarregarPapel() {
        String corpo = new String(java.util.Base64.getUrlDecoder()
                .decode(tokenDeTutor().split("\\.")[1]), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(corpo).doesNotContain("role").doesNotContain("ROLE_");
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
                .generateToken(EMAIL, SUBJECT_ID);

        assertThat(jwtService.extractPrincipal(tokenDeOutraChave)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar token expirado")
    void deveRecusarTokenExpirado() {
        var token = new JwtService(SEGREDO, -1).generateToken(EMAIL, SUBJECT_ID);

        assertThat(jwtService.extractPrincipal(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nao-e-token", "a.b.c", "Bearer xpto"})
    @DisplayName("deve recusar token malformado sem lancar excecao")
    void deveRecusarTokenMalformado(String token) {
        assertThat(jwtService.extractPrincipal(token)).isEmpty();
    }

    /**
     * Dois casos foram substituidos por este no P1b: "deve recusar token com papel
     * desconhecido" e "deve recusar token sem o claim de papel". Os dois cobriam
     * uma regra que deixou de existir, e o segundo passaria por acidente - o token
     * que ele montava tambem nao tinha iat, entao seria recusado por outro motivo,
     * dando cobertura aparente a uma regra apagada.
     *
     * O que vale agora: token emitido antes do P1b ainda carrega role, e continua
     * valendo ate expirar. O claim e simplesmente ignorado.
     */
    @Test
    @DisplayName("token antigo, com claim de papel, deve continuar valendo ate expirar")
    void tokenAntigoComPapelDeveContinuarValendo() {
        var comPapelAntigo = io.jsonwebtoken.Jwts.builder()
                .subject(EMAIL)
                .claim("role", "SUPERUSUARIO")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SEGREDO.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractPrincipal(comPapelAntigo))
                .get()
                .extracting(JwtPrincipal::getEmail)
                .isEqualTo(EMAIL);
    }

    /**
     * Sem iat nao ha como saber se o token e anterior a uma troca de senha, entao
     * ele e tratado como invalido em vez de passar como se fosse recente.
     */
    @Test
    @DisplayName("deve recusar token sem instante de emissao")
    void deveRecusarTokenSemIat() {
        var semIat = io.jsonwebtoken.Jwts.builder()
                .subject(EMAIL)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SEGREDO.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractPrincipal(semIat)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar segredo curto demais para HS256 na criacao do bean")
    void deveRecusarSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto", 120))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }

}
