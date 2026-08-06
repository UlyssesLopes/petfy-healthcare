package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta os vinculos de tutor nos testes.
 *
 * Ate a V15 bastava {@code Animal.builder().person(alguem)}. Agora o vinculo e uma
 * entidade propria, e repetir a montagem dela em cada teste deixaria o teste
 * falando de tabela de juncao em vez de falar da regra que ele verifica.
 */
public final class PetTutores {

    private PetTutores() {
    }

    /** Lista com um titular so - o equivalente ao dono unico de antes da V15. */
    public static List<PetTutor> titular(Person person) {
        List<PetTutor> tutores = new ArrayList<>();
        tutores.add(vinculo(person, PetTutorRole.HOLDER));
        return tutores;
    }

    /** Titular mais co-tutores, na ordem informada. */
    public static List<PetTutor> titularE(Person titular, PetTutorRole papelDoSegundo, Person segundo) {
        List<PetTutor> tutores = titular(titular);
        tutores.add(vinculo(segundo, papelDoSegundo));
        return tutores;
    }

    public static PetTutor vinculo(Person person, PetTutorRole role) {
        return PetTutor.builder()
                .petTutorId(UUID.randomUUID())
                .person(person)
                .role(role)
                .creationDate(LocalDateTime.now())
                .build();
    }

    /** Liga os vinculos ao animal depois que ele existe, fechando os dois lados. */
    public static Animal comTutores(Animal animal, List<PetTutor> tutores) {
        tutores.forEach(t -> t.setAnimal(animal));
        animal.setTutors(tutores);
        return animal;
    }

}
