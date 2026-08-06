package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetTutorRepository extends JpaRepository<PetTutor, UUID> {

    Optional<PetTutor> findByAnimalAnimalIdAndPersonPersonId(UUID animalId, UUID personId);

    List<PetTutor> findByAnimalAnimalIdOrderByRoleAscCreationDateAsc(UUID animalId);

    List<PetTutor> findByPersonPersonId(UUID personId);

    Optional<PetTutor> findByAnimalAnimalIdAndRole(UUID animalId, PetTutorRole role);

    boolean existsByAnimalAnimalIdAndPersonPersonId(UUID animalId, UUID personId);

    /**
     * Usado ao apagar a conta: um animal do qual esta pessoa e a unica tutora morre
     * com ela; um animal que tem outros tutores sobrevive e so perde este vinculo.
     */
    long countByAnimalAnimalId(UUID animalId);

    void deleteByPersonPersonId(UUID personId);

    /** Usado ao apagar o animal - ver {@code AnimalPurger}. */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
