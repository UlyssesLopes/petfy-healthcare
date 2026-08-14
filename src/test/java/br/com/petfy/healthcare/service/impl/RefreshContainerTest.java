package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtPrincipal;
import br.com.petfy.healthcare.security.TokenFreshness;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.PersonService;
import br.com.petfy.healthcare.service.PersonSessionService;
import br.com.petfy.healthcare.service.SessionRenewal;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A sessao que sobrevive a recarga da pagina.
 *
 * <b>O `sessao.ts` previu esta mudanca antes de ela existir:</b> "no dia em que o refresh em cookie
 * httpOnly entrar, ele passa a chamar /auth/refresh. O gatilho da troca esta no ROADMAP.md: o
 * dominio proprio pondo front e API sob o mesmo site, ou o login a cada recarga se mostrar
 * insuportavel no uso real". O segundo aconteceu.
 */
@SpringBootTest
@DisplayName("a renovacao de sessao, contra Postgres real")
class RefreshContainerTest extends PostgresContainerTest {

    @Autowired private AuthService authService;
    @Autowired private SessionRenewal sessionRenewal;
    @Autowired private PersonSessionService personSessionService;
    @Autowired private PersonSessionRepository personSessionRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenFreshness tokenFreshness;
    @Autowired private PersonService personService;

    private static final String SENHA = "Petfy!2026";

    private String meuEmail;

    @BeforeEach
    void montar() {
        meuEmail = "refresh-" + UUID.randomUUID() + "@petfy.com.br";

        personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email(meuEmail)
                .password(passwordEncoder.encode(SENHA))
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(meuEmail, "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private AuthService.Autenticada entrar() {
        return authService.login(LoginRequestDTO.builder().email(meuEmail).password(SENHA).build(),
                "Mozilla/5.0 (Windows NT 10.0) Chrome/141");
    }

    /**
     * A renovacao nao promove a entrada temporaria.
     *
     * O navegador devolve o cookie, mas nao o prazo dele — se a escolha nao sobrevivesse a
     * rotacao, o primeiro refresh transformaria em 30 dias a sessao que a pessoa pediu que
     * durasse ate ela fechar o navegador.
     */
    @Test
    @DisplayName("renovar preserva a escolha de nao continuar conectado")
    void renovarPreservaAEscolha() {
        var entrada = authService.login(LoginRequestDTO.builder()
                .email(meuEmail).password(SENHA).keepSignedIn(false).build(), "Chrome/141");

        assertThat(entrada.manterConectado()).isFalse();

        var renovada = sessionRenewal.renovar(entrada.refreshToken());

        assertThat(renovada.sessao().persistente()).isFalse();
    }

    @Test
    @DisplayName("o refresh troca o token vencido por um novo, sem senha")
    void renovaSemSenha() {
        var entrada = entrar();

        var renovada = sessionRenewal.renovar(entrada.refreshToken());

        assertThat(renovada.token()).isNotBlank();
        assertThat(renovada.pessoa().getEmail()).isEqualTo(meuEmail);
        // e a MESMA entrada: renovar nao cria um aparelho novo na Tela 36
        assertThat(personSessionService.listMine()).hasSize(1);
    }

    /**
     * ROTACIONA SEMPRE.
     *
     * <b>E o que torna um cookie copiado quase inutil:</b> ele deixa de valer no primeiro refresh
     * legitimo que o dono fizer, e o intruso fica com um papel sem valor em vez de acesso
     * permanente.
     */
    @Test
    @DisplayName("o refresh usado nao serve duas vezes")
    void rotaciona() {
        var entrada = entrar();

        var primeira = sessionRenewal.renovar(entrada.refreshToken());

        assertThat(primeira.refreshToken()).isNotEqualTo(entrada.refreshToken());

        assertThatThrownBy(() -> sessionRenewal.renovar(entrada.refreshToken()))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> assertThat(((PetfyHealthcareException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        // e o novo continua valendo: rotacionar nao pode punir quem esta usando
        assertThat(sessionRenewal.renovar(primeira.refreshToken()).token()).isNotBlank();
    }

    /**
     * <b>Encerrar um aparelho na Tela 36 tem de fechar as duas portas.</b> Se o refresh continuasse
     * renovando, encerrar seria um gesto decorativo: o navegador pegaria um token novo em seguida, e
     * a pessoa que clicou em "encerrar" acreditaria ter encerrado.
     */
    @Test
    @DisplayName("a sessao encerrada na Tela 36 nao renova mais")
    void encerradaNaoRenova() {
        var entrada = entrar();
        UUID sessao = personSessionService.listMine().get(0).getPersonSessionId();

        personSessionService.revoke(sessao);

        assertThatThrownBy(() -> sessionRenewal.renovar(entrada.refreshToken()))
                .isInstanceOf(PetfyHealthcareException.class);
    }

    /**
     * A TROCA DE SENHA TAMBEM DERRUBA A RENOVACAO.
     *
     * Sem isto, quem trocou a senha por suspeita de acesso indevido veria o token do invasor cair na
     * proxima requisicao — e o navegador dele pegar um token novo em seguida. A trava que a pessoa
     * acionou seria contornada pelo mecanismo que existe para ela nao digitar a senha de novo.
     *
     * <b>Este teste comecou afirmando que a renovacao encerrava a sessao</b>, e caiu: o encerramento
     * acontecia dentro da transacao que em seguida lanca, e o rollback o desfazia — uma tranca que
     * parecia existir. A correcao foi mover a revogacao para onde ela pertence, o momento da troca
     * de senha, e deixar aqui so a recusa.
     */
    @Test
    @DisplayName("o refresh anterior a troca de senha e recusado")
    void trocaDeSenhaDerrubaARenovacao() {
        var entrada = entrar();

        Person pessoa = personRepository.findByEmail(meuEmail).orElseThrow();
        pessoa.setPasswordChangedAt(LocalDateTime.now().plusSeconds(1));
        personRepository.saveAndFlush(pessoa);

        assertThatThrownBy(() -> sessionRenewal.renovar(entrada.refreshToken()))
                .isInstanceOf(PetfyHealthcareException.class);
    }

    /**
     * E A TELA 36 PASSA A DIZER A VERDADE DEPOIS DE UMA TROCA DE SENHA.
     *
     * <b>Ate a V51 ela mentia:</b> os tokens paravam de autenticar pelo `iat`, e as sessoes
     * continuavam listadas como abertas — a lista mostrava aberto o que ja nao abria. "Trocar a
     * senha derruba todas de uma vez" era uma frase da tela sem efeito no que a tela mostrava.
     */
    @Test
    @DisplayName("trocar a senha encerra todas as entradas, e a lista mostra isso")
    void trocarSenhaEncerraTodasAsEntradas() {
        entrar();
        entrar();

        assertThat(personSessionService.listMine())
                .hasSize(2)
                .allSatisfy(aparelho -> assertThat(aparelho.getRevokedAt()).isNull());

        personService.changePassword(new PasswordChangeRequestDTO(SENHA, "OutraSenha!2026"));

        assertThat(personSessionService.listMine())
                .hasSize(2)
                .allSatisfy(aparelho -> assertThat(aparelho.getRevokedAt()).isNotNull());
    }

    /**
     * Sair passou a ter efeito no servidor. Antes o cliente esquecia o token e o JWT seguia valido
     * ate expirar — num computador emprestado, isso e a diferenca entre sair e parecer que saiu.
     */
    @Test
    @DisplayName("sair encerra a entrada e apaga o refresh do banco")
    void sairEncerraAEntrada() {
        var entrada = entrar();
        UUID sessao = personSessionService.listMine().get(0).getPersonSessionId();

        sessionRenewal.sair(entrada.refreshToken());

        assertThat(personSessionRepository.findById(sessao))
                .get()
                .satisfies(s -> {
                    assertThat(s.getRevokedAt()).isNotNull();
                    // o segredo nao fica guardado sem uso
                    assertThat(s.getRefreshTokenHash()).isNull();
                });

        // e o token daquela entrada para de autenticar
        assertThat(tokenFreshness.isStale(new JwtPrincipal(meuEmail, Instant.now(), sessao)))
                .isTrue();
    }

    @Test
    @DisplayName("sair sem cookie nenhum nao quebra")
    void sairSemCookieNaoQuebra() {
        sessionRenewal.sair(null);
        sessionRenewal.sair("");
        sessionRenewal.sair("nao-e-um-refresh");
    }

    /**
     * Um estado so: inexistente, ja rotacionado, expirado e de sessao encerrada respondem igual. A
     * diferenca entre "expirado" e "encerrado" e justamente a informacao que interessa a quem roubou
     * o cookie.
     */
    @Test
    @DisplayName("refresh inventado e recusado com 401, como todos os outros casos")
    void refreshInventadoERecusado() {
        assertThatThrownBy(() -> sessionRenewal.renovar("inventado"))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> assertThat(((PetfyHealthcareException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
