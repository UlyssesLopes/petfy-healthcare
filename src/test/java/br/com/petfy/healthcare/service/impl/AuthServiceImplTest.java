package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.config.PetfyMetrics;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.UserRole;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    /**
     * A metrica de tentativa de login e efeito colateral, nao regra: mockada para
     * os testes seguirem falando so sobre autenticacao.
     */
    @Mock
    private PetfyMetrics petfyMetrics;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "ulysses@petfy.com.br";
    private static final String HASH = "$2a$10$hashDaSenha";

    private Person person() {
        return Person.builder().personId(OWNER_ID).email(EMAIL).password(HASH).build();
    }

    private Vet vet() {
        return Vet.builder().vetId(VET_ID).email(EMAIL).password(HASH)
                .clinic(Clinic.builder().clinicId(UUID.randomUUID()).name("Clinica").build()).build();
    }

    private LoginRequestDTO request(String senha) {
        return LoginRequestDTO.builder().email(EMAIL).password(senha).build();
    }

    @Test
    @DisplayName("deve autenticar tutor com papel OWNER")
    void deveAutenticarTutorComPapelPerson() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(EMAIL, UserRole.OWNER, OWNER_ID)).thenReturn("token-de-tutor");
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        var result = authService.login(request("s3nhaForte"));

        assertThat(result.getToken()).isEqualTo("token-de-tutor");
        assertThat(result.getRole()).isEqualTo("OWNER");
        assertThat(result.getPersonId()).isEqualTo(OWNER_ID);
        assertThat(result.getVetId()).isNull();
    }

    @Test
    @DisplayName("deve autenticar veterinario com papel VET")
    void deveAutenticarVeterinarioComPapelVet() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(vetRepository.findByEmail(EMAIL)).thenReturn(Optional.of(vet()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(EMAIL, UserRole.VET, VET_ID)).thenReturn("token-de-vet");
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        var result = authService.login(request("s3nhaForte"));

        assertThat(result.getToken()).isEqualTo("token-de-vet");
        assertThat(result.getRole()).isEqualTo("VET");
        assertThat(result.getVetId()).isEqualTo(VET_ID);
        assertThat(result.getPersonId()).isNull();
    }

    @Test
    @DisplayName("o papel deve vir de onde o email esta cadastrado, nunca do request")
    void papelDeveVirDoCadastroENaoDoRequest() {
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
    @DisplayName("deve lancar 401 quando o email nao existe em nenhuma das duas tabelas")
    void deveLancar401QuandoEmailNaoExiste() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(vetRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

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
        when(vetRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        var msgEmailInexistente = capturaMensagem(request("s3nhaForte"));

        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("errada", HASH)).thenReturn(false);
        var msgSenhaErrada = capturaMensagem(request("errada"));

        assertThat(msgEmailInexistente)
                .isEqualTo(msgSenhaErrada)
                .isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("nao deve consultar a tabela de vets quando o email ja e de um tutor")
    void naoDeveConsultarVetsQuandoEmailEDeTutor() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(eq(EMAIL), eq(UserRole.OWNER), any())).thenReturn("token");

        authService.login(request("s3nhaForte"));

        verify(vetRepository, never()).findByEmail(any());
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
