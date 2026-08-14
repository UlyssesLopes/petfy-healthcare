package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtPrincipal;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.TokenFreshness;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.PersonSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "3 sessoes abertas. A mais antiga e de 11/2025" — a unica linha da Tela 36 que tinha ficado por
 * construir.
 *
 * <b>A justificativa para nao construi-la nao se sustentou.</b> Estava escrito que "este produto
 * autentica com JWT sem estado: o servidor nao sabe quantos tokens validos existem, e nao teria como
 * invalidar um deles". Mas o `TokenFreshness` ja consultava o banco em TODA rota autenticada, desde
 * o P1, para derrubar token anterior a uma troca de senha. O custo estava pago; faltava a linha.
 */
@SpringBootTest
@DisplayName("os aparelhos conectados, contra Postgres real")
class AparelhosConectadosContainerTest extends PostgresContainerTest {

    @Autowired private AuthService authService;
    @Autowired private PersonSessionService personSessionService;
    @Autowired private PersonSessionRepository personSessionRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private TokenFreshness tokenFreshness;

    private static final String SENHA = "Petfy!2026";

    private Person eu;
    private String meuEmail;

    @BeforeEach
    void montar() {
        meuEmail = "sessao-" + UUID.randomUUID() + "@petfy.com.br";

        eu = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email(meuEmail)
                .password(passwordEncoder.encode(SENHA))
                .build());

        autenticar(meuEmail);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", List.of()));
    }

    private void entrar(String aparelho) {
        authService.login(LoginRequestDTO.builder().email(meuEmail).password(SENHA).build(), aparelho);
    }

    @Test
    @DisplayName("cada entrada vira um aparelho na lista, com o navegador e a data")
    void cadaEntradaViraUmAparelho() {
        entrar("Mozilla/5.0 (Windows NT 10.0) Chrome/141");
        entrar("Mozilla/5.0 (iPhone) Safari/605");

        var aparelhos = personSessionService.listMine();

        assertThat(aparelhos).hasSize(2);
        assertThat(aparelhos).extracting("userAgent")
                .anySatisfy(ua -> assertThat((String) ua).contains("Chrome"))
                .anySatisfy(ua -> assertThat((String) ua).contains("iPhone"));
        assertThat(aparelhos).allSatisfy(a -> assertThat(a.getCreatedAt()).isNotNull());
    }

    /**
     * <b>O que a tela nao conseguia fazer antes:</b> derrubar UM aparelho. A unica ferramenta era
     * trocar a senha, que derruba todos — e a tela dizia isso porque era verdade.
     */
    @Test
    @DisplayName("encerrar um aparelho faz o token dele parar de autenticar")
    void encerrarDerrubaSoAquele() {
        entrar("Chrome");
        entrar("Safari");

        var aparelhos = personSessionService.listMine();
        var derrubado = aparelhos.get(0);
        var mantido = aparelhos.get(1);

        personSessionService.revoke(derrubado.getPersonSessionId());

        assertThat(tokenFreshness.isStale(
                new JwtPrincipal(meuEmail, java.time.Instant.now(), derrubado.getPersonSessionId())))
                .isTrue();

        assertThat(tokenFreshness.isStale(
                new JwtPrincipal(meuEmail, java.time.Instant.now(), mantido.getPersonSessionId())))
                .isFalse();
    }

    /**
     * O token anterior a V50 nao tem `sid`, e derruba-lo seria deslogar todo mundo numa migracao.
     * Ele continua sujeito a troca de senha, que era a unica trava que tinha.
     */
    @Test
    @DisplayName("token sem sessao continua autenticando")
    void tokenAntigoContinuaValendo() {
        assertThat(tokenFreshness.isStale(new JwtPrincipal(meuEmail, java.time.Instant.now())))
                .isFalse();
    }

    @Test
    @DisplayName("o token novo carrega a sessao, e o filtro a le de volta")
    void oTokenCarregaASessao() {
        entrar("Chrome");
        UUID sessao = personSessionService.listMine().get(0).getPersonSessionId();

        String token = jwtService.generateToken(meuEmail, eu.getPersonId(), sessao);

        assertThat(jwtService.extractPrincipal(token))
                .get()
                .satisfies(p -> assertThat(p.getSessionId()).isEqualTo(sessao));
    }

    /**
     * Encerrar nao apaga — a mesma razao de a revogacao de acesso nao apagar o que a organizacao
     * registrou.
     */
    @Test
    @DisplayName("encerrar guarda a data, e encerrar de novo nao a reescreve")
    void encerrarNaoApaga() {
        entrar("Chrome");
        UUID sessao = personSessionService.listMine().get(0).getPersonSessionId();

        personSessionService.revoke(sessao);
        var primeira = personSessionRepository.findById(sessao).orElseThrow().getRevokedAt();

        personSessionService.revoke(sessao);
        var segunda = personSessionRepository.findById(sessao).orElseThrow().getRevokedAt();

        assertThat(primeira).isNotNull();
        assertThat(segunda).isEqualTo(primeira);
        assertThat(personSessionService.listMine()).hasSize(1);
    }

    @Test
    @DisplayName("a sessao de outra pessoa nao existe para mim")
    void sessaoAlheiaResponde404() {
        entrar("Chrome");
        UUID minha = personSessionService.listMine().get(0).getPersonSessionId();

        Person ana = personRepository.saveAndFlush(Person.builder()
                .name("Ana Prado")
                .email("sessao-" + UUID.randomUUID() + "@petfy.com.br")
                .password(passwordEncoder.encode(SENHA))
                .build());

        autenticar(ana.getEmail());

        assertThatThrownBy(() -> personSessionService.revoke(minha))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> {
                    var erro = (PetfyHealthcareException) e;
                    assertThat(erro.getCode()).isEqualTo(193);
                    assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        assertThat(personSessionRepository.findById(minha))
                .get()
                .satisfies(s -> assertThat(s.getRevokedAt()).isNull());
    }
}
