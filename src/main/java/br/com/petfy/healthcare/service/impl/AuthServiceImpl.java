package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.config.PetfyMetrics;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.UserRole;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OwnerRepository ownerRepository;
    private final VetRepository vetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PetfyMetrics petfyMetrics;

    /**
     * Tutor e veterinario entram pelo mesmo endpoint. O papel sai de qual tabela
     * o email aparece, e nao de um campo do request: deixar o cliente declarar o
     * proprio papel seria escolher a propria permissao.
     *
     * O cadastro garante que um email nao existe nos dois lados, entao a ordem
     * da busca nao muda o resultado.
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Optional<Credenciais> encontrado = ownerRepository.findByEmail(request.getEmail())
                .map(owner -> new Credenciais(owner.getOwnerId(), owner.getEmail(),
                        owner.getPassword(), UserRole.OWNER))
                .or(() -> vetRepository.findByEmail(request.getEmail())
                        .map(vet -> new Credenciais(vet.getVetId(), vet.getEmail(),
                                vet.getPassword(), UserRole.VET)));

        // a mesma resposta para email inexistente e para senha errada: distinguir
        // os dois casos entregaria de graca quais emails estao cadastrados
        Optional<Credenciais> validas = encontrado
                .filter(c -> passwordEncoder.matches(request.getPassword(), c.senhaComHash));

        if (validas.isEmpty()) {
            petfyMetrics.loginAttempt("failure");
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                    ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                    HttpStatus.UNAUTHORIZED);
        }

        Credenciais credenciais = validas.get();
        petfyMetrics.loginAttempt("success");

        return LoginResponseDTO.builder()
                .token(jwtService.generateToken(credenciais.email, credenciais.role, credenciais.id))
                .tokenType("Bearer")
                .expiresInMinutes(jwtService.getExpirationMinutes())
                .role(credenciais.role.name())
                .ownerId(credenciais.role == UserRole.OWNER ? credenciais.id : null)
                .vetId(credenciais.role == UserRole.VET ? credenciais.id : null)
                .build();
    }

    /** Achata owner e vet no que o login precisa, para o fluxo nao se ramificar. */
    private static final class Credenciais {
        private final UUID id;
        private final String email;
        private final String senhaComHash;
        private final UserRole role;

        private Credenciais(UUID id, String email, String senhaComHash, UserRole role) {
            this.id = id;
            this.email = email;
            this.senhaComHash = senhaComHash;
            this.role = role;
        }
    }

}
