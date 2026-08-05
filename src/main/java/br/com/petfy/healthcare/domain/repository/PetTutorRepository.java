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

    Optional<PetTutor> findByPetPetIdAndOwnerOwnerId(UUID petId, UUID ownerId);

    List<PetTutor> findByPetPetIdOrderByRoleAscCreationDateAsc(UUID petId);

    List<PetTutor> findByOwnerOwnerId(UUID ownerId);

    Optional<PetTutor> findByPetPetIdAndRole(UUID petId, PetTutorRole role);

    boolean existsByPetPetIdAndOwnerOwnerId(UUID petId, UUID ownerId);

    /**
     * Usado ao apagar a conta: um pet do qual esta pessoa e a unica tutora morre
     * com ela; um pet que tem outros tutores sobrevive e so perde este vinculo.
     */
    long countByPetPetId(UUID petId);

    void deleteByOwnerOwnerId(UUID ownerId);

}
