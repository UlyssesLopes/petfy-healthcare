package br.com.petfy.healthcare.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final org.springframework.security.crypto.password.PasswordEncoder encoder =
            new SecurityConfig().passwordEncoder();

    @Test
    @DisplayName("deve expor um encoder BCrypt")
    void deveExporEncoderBCrypt() {
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("o hash gerado nao deve conter a senha original e deve validar contra ela")
    void hashNaoDeveConterSenhaOriginal() {
        var senha = "s3nhaForte";

        var hash = encoder.encode(senha);

        assertThat(hash).isNotEqualTo(senha).startsWith("$2");
        assertThat(encoder.matches(senha, hash)).isTrue();
        assertThat(encoder.matches("senhaErrada", hash)).isFalse();
    }

    @Test
    @DisplayName("duas chamadas para a mesma senha devem gerar hashes diferentes - o salt e aleatorio")
    void deveGerarHashesDiferentesParaMesmaSenha() {
        var senha = "s3nhaForte";

        assertThat(encoder.encode(senha)).isNotEqualTo(encoder.encode(senha));
    }
}
