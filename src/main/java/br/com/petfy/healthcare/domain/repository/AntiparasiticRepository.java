package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AntiparasiticRepository extends JpaRepository<Antiparasitic, UUID> {

    /** Alcancados pela pessoa - ver {@code AnimalRepository.findAlcancadosPor}. */
    @Query("select a from Antiparasitic a where exists ("
            + "  select 1 from Custody c where c.animal = a.animal "
            + "    and c.holderPerson.personId = :personId and c.endedAt is null) "
            + "or exists ("
            + "  select 1 from Grant g where g.animal = a.animal "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora))")
    List<Antiparasitic> findAlcancadosPor(@Param("personId") UUID personId,
                                          @Param("agora") LocalDateTime agora);

    List<Antiparasitic> findByAnimalAnimalIdOrderByApplicationDateDesc(UUID animalId);

    /**
     * Usado pela rotina de lembretes, simetrico ao VaccineRepository.
     * O filtro de cooldown fica no service para manter a regra testavel sem banco.
     */
    List<Antiparasitic> findByNextDoseDateLessThanEqual(LocalDate limite);

    /** Usado ao apagar o animal - ver {@code AnimalPurger}. Faltava, junto com o peso. */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
