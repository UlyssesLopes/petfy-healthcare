package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnimalWeightHistoryRepository extends JpaRepository<AnimalWeightHistory, UUID> {

    /** Serie historica ordenada da mais recente para a mais antiga. */
    List<AnimalWeightHistory> findByAnimalAnimalIdOrderByMeasuredAtDesc(UUID animalId);

    /** Ultima medicao registrada, usada para espelhar Animal.weight. */
    Optional<AnimalWeightHistory> findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(UUID animalId);

    /**
     * Usado ao apagar o animal - ver {@code AnimalPurger}. Faltava: a serie de peso
     * chegou no passo 9 e nenhuma das duas cascatas foi atualizada, o que travou o
     * DELETE /owners/me para qualquer animal com pesagem.
     */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
