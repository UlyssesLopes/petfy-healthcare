package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetWeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetWeightHistoryRepository extends JpaRepository<PetWeightHistory, UUID> {

    /** Serie historica ordenada da mais recente para a mais antiga. */
    List<PetWeightHistory> findByPetPetIdOrderByMeasuredAtDesc(UUID petId);

    /** Ultima medicao registrada, usada para espelhar Pet.weight. */
    Optional<PetWeightHistory> findFirstByPetPetIdOrderByMeasuredAtDesc(UUID petId);

}
