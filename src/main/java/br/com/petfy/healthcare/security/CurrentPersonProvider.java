package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentPersonProvider {

    private final PersonRepository personRepository;

    /**
     * Person da requisicao em andamento, resolvido pelo email que o filtro JWT
     * colocou no SecurityContext.
     *
     * A SecurityFilterChain ja garante que rota protegida so chega aqui
     * autenticada. O 401 abaixo cobre o caso de o person ter sido removido depois
     * que o token foi emitido - o token continua valido ate expirar, mas nao ha
     * mais a quem associar a requisicao.
     */
    public Person require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw naoAutenticado();
        }

        return personRepository.findByEmail(authentication.getName())
                .orElseThrow(this::naoAutenticado);
    }

    private PetfyHealthcareException naoAutenticado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                HttpStatus.UNAUTHORIZED);
    }

}
