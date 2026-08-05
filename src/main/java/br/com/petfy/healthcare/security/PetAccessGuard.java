package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decide se a pessoa autenticada alcanca um pet, e em que nivel.
 *
 * Existe como peca unica porque antes da V15 essa decisao estava repetida em
 * duas dezenas de lugares como {@code pet.getOwner().getOwnerId().equals(...)}.
 * Enquanto a regra era "dono unico", repetir era so feio; com tres papeis e
 * varios tutores, cada copia vira uma chance de alguem esquecer um caso e
 * transformar leitura em escrita - ou pior, deixar passar pet de terceiro.
 *
 * <b>Pet inalcancavel responde 404, nunca 403.</b> Um 403 confirmaria que aquele
 * id existe, permitindo varrer ids para descobrir o que ha na base. A distincao
 * so aparece quando a pessoa ja alcanca o pet e falta nivel: ai o 403 e correto,
 * porque nao revela nada que ela ja nao saiba.
 */
@Component
@RequiredArgsConstructor
public class PetAccessGuard {

    private final PetRepository petRepository;
    private final PetTutorRepository petTutorRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    /** Le a carteira: qualquer papel serve. */
    public Pet requireLeitura(UUID petId) {
        return require(petId, PetTutorRole.VIEWER);
    }

    /** Registra vacina, corrige peso, edita o cadastro: EDITOR ou o titular. */
    public Pet requireEscrita(UUID petId) {
        return require(petId, PetTutorRole.EDITOR);
    }

    /** Convida, remove tutor, transfere titularidade, apaga o pet: so o titular. */
    public Pet requireTitular(UUID petId) {
        return require(petId, PetTutorRole.HOLDER);
    }

    public PetTutor vinculoDoAutenticado(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petTutorRepository.findByPetPetIdAndOwnerOwnerId(petId, ownerId)
                .orElseThrow(PetAccessGuard::petNaoEncontrado);
    }

    public boolean alcanca(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();
        return petTutorRepository.existsByPetPetIdAndOwnerOwnerId(petId, ownerId);
    }

    private Pet require(UUID petId, PetTutorRole nivelExigido) {
        PetTutor vinculo = vinculoDoAutenticado(petId);

        if (!vinculo.getRole().permite(nivelExigido)) {
            // aqui o 403 e seguro: quem chegou ate este ponto ja e tutor do pet,
            // entao a resposta nao revela existencia de nada novo
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INSUFFICIENT_PET_ROLE.getMessage(),
                    ErrorMessageEnum.INSUFFICIENT_PET_ROLE.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return petRepository.findById(petId)
                .orElseThrow(PetAccessGuard::petNaoEncontrado);
    }

    private static PetfyHealthcareException petNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

}
