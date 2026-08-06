package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.config.PetfyMetrics;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PetfyMetrics petfyMetrics;

    /**
     * Uma busca so, numa tabela so.
     *
     * <b>O que sumiu daqui.</b> Antes o login procurava em {@code owners} e, se
     * nao achasse, em {@code vets}, e o papel saia de qual das duas respondeu.
     * Isso obrigava o cadastro a conferir o e-mail nos dois lados para o login nao
     * ficar ambiguo - checagem que o banco nao garantia e que dependia de ninguem
     * esquecer. Com pessoa unica a unicidade e do banco, e o papel nao existe.
     *
     * <b>O que a resposta diz no lugar do papel.</b> {@code professional} nao e um
     * papel disfarcado: e o estado atual da credencial, lido agora, e serve para o
     * cliente saber se oferece a area profissional. Se a credencial for suspensa
     * amanha, a resposta de amanha muda - o que nao aconteceria com um papel
     * carimbado dentro do token.
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Optional<Person> encontrado = personRepository.findByEmail(request.getEmail());

        // a mesma resposta para e-mail inexistente e para senha errada: distinguir
        // os dois casos entregaria de graca quais e-mails estao cadastrados
        Optional<Person> validas = encontrado
                .filter(p -> passwordEncoder.matches(request.getPassword(), p.getPassword()));

        if (validas.isEmpty()) {
            petfyMetrics.loginAttempt("failure");
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                    ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                    HttpStatus.UNAUTHORIZED);
        }

        Person person = validas.get();
        petfyMetrics.loginAttempt("success");

        return LoginResponseDTO.builder()
                .token(jwtService.generateToken(person.getEmail(), person.getPersonId()))
                .tokenType("Bearer")
                .expiresInMinutes(jwtService.getExpirationMinutes())
                .personId(person.getPersonId())
                .professional(credentialRepository.existsAtivaPorEmail(
                        person.getEmail(), CredentialStatus.SUSPENSO))
                .build();
    }

}
