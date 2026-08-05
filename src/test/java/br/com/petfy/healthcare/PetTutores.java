package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta os vinculos de tutor nos testes.
 *
 * Ate a V15 bastava {@code Pet.builder().owner(alguem)}. Agora o vinculo e uma
 * entidade propria, e repetir a montagem dela em cada teste deixaria o teste
 * falando de tabela de juncao em vez de falar da regra que ele verifica.
 */
public final class PetTutores {

    private PetTutores() {
    }

    /** Lista com um titular so - o equivalente ao dono unico de antes da V15. */
    public static List<PetTutor> titular(Owner owner) {
        List<PetTutor> tutores = new ArrayList<>();
        tutores.add(vinculo(owner, PetTutorRole.HOLDER));
        return tutores;
    }

    /** Titular mais co-tutores, na ordem informada. */
    public static List<PetTutor> titularE(Owner titular, PetTutorRole papelDoSegundo, Owner segundo) {
        List<PetTutor> tutores = titular(titular);
        tutores.add(vinculo(segundo, papelDoSegundo));
        return tutores;
    }

    public static PetTutor vinculo(Owner owner, PetTutorRole role) {
        return PetTutor.builder()
                .petTutorId(UUID.randomUUID())
                .owner(owner)
                .role(role)
                .creationDate(LocalDateTime.now())
                .build();
    }

    /** Liga os vinculos ao pet depois que ele existe, fechando os dois lados. */
    public static Pet comTutores(Pet pet, List<PetTutor> tutores) {
        tutores.forEach(t -> t.setPet(pet));
        pet.setTutors(tutores);
        return pet;
    }

}
