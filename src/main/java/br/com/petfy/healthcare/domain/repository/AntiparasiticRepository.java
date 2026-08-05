package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AntiparasiticRepository extends JpaRepository<Antiparasitic, UUID> {

    List<Antiparasitic> findByPetTutorsOwnerOwnerId(UUID ownerId);

    List<Antiparasitic> findByPetPetIdOrderByApplicationDateDesc(UUID petId);

    /**
     * Usado pela rotina de lembretes, simetrico ao VaccineRepository.
     * O filtro de cooldown fica no service para manter a regra testavel sem banco.
     */
    List<Antiparasitic> findByNextDoseDateLessThanEqual(LocalDate limite);

}
