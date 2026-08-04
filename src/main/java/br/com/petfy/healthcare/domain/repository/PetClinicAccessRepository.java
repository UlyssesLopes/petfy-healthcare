package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetClinicAccessRepository extends JpaRepository<PetClinicAccess, UUID> {

    Optional<PetClinicAccess> findByPetPetIdAndClinicClinicId(UUID petId, UUID clinicId);

    List<PetClinicAccess> findByPetPetIdOrderByGrantedAtDesc(UUID petId);

    /** Usado pelo vet para listar os pets que a clinica dele pode atender. */
    List<PetClinicAccess> findByClinicClinicIdAndRevokedAtIsNull(UUID clinicId);

}
