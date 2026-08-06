package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * A pessoa da requisicao em andamento, exigindo credencial profissional ativa.
 *
 * Substitui o CurrentVetProvider. A diferenca nao e de nome: antes um tutor
 * simplesmente nao era encontrado, porque a busca era na tabela {@code vets} e o
 * e-mail dele nao estava la. Com pessoa unica todo mundo e encontrado, entao o
 * que separa quem pratica ato clinico de quem nao pratica passa a ser a
 * credencial - que e o que 5.10 diz governar isso.
 *
 * <b>403 e nao 404.</b> A pessoa esta autenticada e sabe que existe; o que falta
 * e capacidade. Nao ha nada a esconder dela sobre a propria conta.
 */
@Component
@RequiredArgsConstructor
public class CurrentProfessionalProvider {

    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;

    public Person require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw naoAutenticado();
        }

        Person person = personRepository.findByEmail(authentication.getName())
                .orElseThrow(this::naoAutenticado);

        if (!credentialRepository.existsAtivaPorEmail(person.getEmail(), CredentialStatus.SUSPENSO)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.PROFESSIONAL_CREDENTIAL_REQUIRED.getMessage(),
                    ErrorMessageEnum.PROFESSIONAL_CREDENTIAL_REQUIRED.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return person;
    }

    private PetfyHealthcareException naoAutenticado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                HttpStatus.UNAUTHORIZED);
    }

}
