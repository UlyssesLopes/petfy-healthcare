package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentVetProvider {

    private final VetRepository vetRepository;

    /**
     * Veterinario da requisicao em andamento.
     *
     * Nao basta o token ser valido: um token de tutor nao resolve para vet aqui,
     * mesmo que a rota so exija autenticacao. A busca e na tabela de vets, entao
     * um email de owner simplesmente nao encontra.
     */
    public Vet require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw naoAutenticado();
        }

        return vetRepository.findByEmail(authentication.getName())
                .orElseThrow(this::naoAutenticado);
    }

    private PetfyHealthcareException naoAutenticado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                HttpStatus.UNAUTHORIZED);
    }

}
