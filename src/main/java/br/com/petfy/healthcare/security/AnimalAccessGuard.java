package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decide se a pessoa autenticada alcanca um animal, e em que nivel.
 *
 * Existe como peca unica porque antes da V15 essa decisao estava reanimalida em
 * duas dezenas de lugares como {@code animal.getOwner().getOwnerId().equals(...)}.
 * Enquanto a regra era "dono unico", reanimalir era so feio; com tres papeis e
 * varios tutores, cada copia vira uma chance de alguem esquecer um caso e
 * transformar leitura em escrita - ou pior, deixar passar animal de terceiro.
 *
 * <b>Animal inalcancavel responde 404, nunca 403.</b> Um 403 confirmaria que aquele
 * id existe, permitindo varrer ids para descobrir o que ha na base. A distincao
 * so aparece quando a pessoa ja alcanca o animal e falta nivel: ai o 403 e correto,
 * porque nao revela nada que ela ja nao saiba.
 */
@Component
@RequiredArgsConstructor
public class AnimalAccessGuard {

    private final AnimalRepository animalRepository;
    private final PetTutorRepository petTutorRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    /** Le a carteira: qualquer papel serve. */
    public Animal requireLeitura(UUID animalId) {
        return require(animalId, PetTutorRole.VIEWER);
    }

    /** Registra vacina, corrige peso, edita o cadastro: EDITOR ou o titular. */
    public Animal requireEscrita(UUID animalId) {
        return require(animalId, PetTutorRole.EDITOR);
    }

    /** Convida, remove tutor, transfere titularidade, apaga o animal: so o titular. */
    public Animal requireTitular(UUID animalId) {
        return require(animalId, PetTutorRole.HOLDER);
    }

    public PetTutor vinculoDoAutenticado(UUID animalId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petTutorRepository.findByAnimalAnimalIdAndOwnerOwnerId(animalId, ownerId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);
    }

    public boolean alcanca(UUID animalId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();
        return petTutorRepository.existsByAnimalAnimalIdAndOwnerOwnerId(animalId, ownerId);
    }

    private Animal require(UUID animalId, PetTutorRole nivelExigido) {
        PetTutor vinculo = vinculoDoAutenticado(animalId);

        if (!vinculo.getRole().permite(nivelExigido)) {
            // aqui o 403 e seguro: quem chegou ate este ponto ja e tutor do animal,
            // entao a resposta nao revela existencia de nada novo
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getMessage(),
                    ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return animalRepository.findById(animalId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);
    }

    private static PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

}
