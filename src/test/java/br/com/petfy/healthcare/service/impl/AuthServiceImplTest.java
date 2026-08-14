package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.config.PetfyMetrics;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quatro casos deste teste foram <b>removidos</b> no P1b em vez de adaptados:
 * autenticar com papel OWNER, autenticar com papel VET, "o papel deve vir de onde
 * o e-mail esta cadastrado" e "nao deve consultar a tabela de vets quando o e-mail
 * ja e de um tutor".
 *
 * Os quatro cobriam a mesma regra - a busca em duas tabelas e o papel derivado de
 * qual delas respondeu -, e essa regra deixou de existir. Adapta-los para voltarem
 * a passar deixaria quatro testes que nao protegem nada e sugerem cobertura de uma
 * decisao apagada. E a lição registrada no 8a.
 *
 * O que substitui os quatro esta em {@code deveAutenticarQualquerPessoaPelaMesmaBusca}
 * e {@code respostaDeveDizerSeHaCredencialAtiva}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ProfessionalCredentialRepository credentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    /* Toda entrada vira um aparelho na lista da Tela 36, desde a V50. */
    @Mock
    private br.com.petfy.healthcare.domain.repository.PersonSessionRepository personSessionRepository;

    /**
     * A metrica de tentativa de login e efeito colateral, nao regra: mockada para
     * os testes seguirem falando so sobre autenticacao.
     */
    @Mock
    private PetfyMetrics petfyMetrics;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID PERSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "ulysses@petfy.com.br";
    private static final String HASH = "$2a$10$hashDaSenha";

    private Person person() {
        return Person.builder().personId(PERSON_ID).email(EMAIL).password(HASH).build();
    }

    private LoginRequestDTO request(String senha) {
        return LoginRequestDTO.builder().email(EMAIL).password(senha).build();
    }

    @Test
    @DisplayName("deve autenticar qualquer pessoa pela mesma busca, sem papel")
    void deveAutenticarQualquerPessoaPelaMesmaBusca() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(eq(EMAIL), eq(PERSON_ID), any())).thenReturn("token");
        when(personSessionRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        var result = authService.login(request("s3nhaForte"));

        assertThat(result.getToken()).isEqualTo("token");
        assertThat(result.getPersonId()).isEqualTo(PERSON_ID);
    }

    /**
     * {@code professional} nao e papel disfarcado: e lido do estado atual da
     * credencial a cada login, e nao gravado no token. Se a credencial for suspensa
     * amanha, a resposta de amanha muda.
     */
    @Test
    @DisplayName("a resposta deve dizer se ha credencial profissional ativa agora")
    void respostaDeveDizerSeHaCredencialAtiva() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(eq(EMAIL), eq(PERSON_ID), any())).thenReturn("token");
        when(personSessionRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(credentialRepository.existsAtivaPorEmail(EMAIL, CredentialStatus.SUSPENSO)).thenReturn(true);

        assertThat(authService.login(request("s3nhaForte")).isProfessional()).isTrue();
    }

    @Test
    @DisplayName("quem nao tem credencial nao vira profissional na resposta")
    void semCredencialNaoEProfissional() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(eq(EMAIL), eq(PERSON_ID), any())).thenReturn("token");
        when(personSessionRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(credentialRepository.existsAtivaPorEmail(EMAIL, CredentialStatus.SUSPENSO)).thenReturn(false);

        assertThat(authService.login(request("s3nhaForte")).isProfessional()).isFalse();
    }

    /** O request nunca teve como declarar papel, e continua sem ter o que declarar. */
    @Test
    @DisplayName("o request de login deve carregar apenas email e senha")
    void requestDeveCarregarApenasEmailESenha() {
        assertThat(LoginRequestDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    @DisplayName("deve lancar 401 sem gerar token quando a senha esta errada")
    void deveLancar401QuandoSenhaErrada() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("errada", HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request("errada")))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting("code", "httpStatus")
                .containsExactly(401, HttpStatus.UNAUTHORIZED);

        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("deve lancar 401 sem sequer conferir a senha quando o email nao existe")
    void deveLancar401QuandoEmailNaoExiste() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request("s3nhaForte")))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("a mensagem deve ser identica para email inexistente e senha errada")
    void mensagemDeveSerIdenticaNosDoisCasos() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        var msgEmailInexistente = capturaMensagem(request("s3nhaForte"));

        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("errada", HASH)).thenReturn(false);
        var msgSenhaErrada = capturaMensagem(request("errada"));

        assertThat(msgEmailInexistente)
                .isEqualTo(msgSenhaErrada)
                .isEqualTo("Invalid email or password");
    }

    private String capturaMensagem(LoginRequestDTO request) {
        try {
            authService.login(request);
            throw new AssertionError("deveria ter lancado PetfyHealthcareException");
        } catch (PetfyHealthcareException e) {
            return e.getMessage();
        }
    }
}
