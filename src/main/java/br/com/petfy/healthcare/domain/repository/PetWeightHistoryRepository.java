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

    /**
     * Usado ao apagar o pet - ver {@code PetPurger}. Faltava: a serie de peso
     * chegou no passo 9 e nenhuma das duas cascatas foi atualizada, o que travou o
     * DELETE /owners/me para qualquer pet com pesagem.
     */
    void deleteByPetPetIdIn(List<UUID> petIds);

}
