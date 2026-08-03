package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "ulysses@petfy.com.br";
    private static final String HASH = "$2a$10$hashDaSenha";

    private Owner owner() {
        return Owner.builder().ownerId(OWNER_ID).email(EMAIL).password(HASH).build();
    }

    private LoginRequestDTO request(String senha) {
        return LoginRequestDTO.builder().email(EMAIL).password(senha).build();
    }

    @Test
    @DisplayName("deve devolver token quando as credenciais conferem")
    void deveDevolverTokenQuandoCredenciaisConferem() {
        when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner()));
        when(passwordEncoder.matches("s3nhaForte", HASH)).thenReturn(true);
        when(jwtService.generateToken(any(Owner.class))).thenReturn("token-jwt");
        when(jwtService.getExpirationMinutes()).thenReturn(120L);

        var result = authService.login(request("s3nhaForte"));

        assertThat(result.getToken()).isEqualTo("token-jwt");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresInMinutes()).isEqualTo(120L);
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("deve lancar 401 sem gerar token quando a senha esta errada")
    void deveLancar401QuandoSenhaErrada() {
        when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner()));
        when(passwordEncoder.matches("errada", HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request("errada")))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting("code", "httpStatus")
                .containsExactly(401, HttpStatus.UNAUTHORIZED);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("deve lancar 401 sem consultar a senha quando o email nao existe")
    void deveLancar401QuandoEmailNaoExiste() {
        when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request("s3nhaForte")))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("a mensagem deve ser identica para email inexistente e senha errada")
    void mensagemDeveSerIdenticaNosDoisCasos() {
        when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        var msgEmailInexistente = capturaMensagem(request("s3nhaForte"));

        when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner()));
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
