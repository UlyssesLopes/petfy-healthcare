package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    List<HealthRecord> findByAnimalAnimalIdOrderByEventDateDesc(UUID animalId);

    /** Alcancados pela pessoa - ver {@code AnimalRepository.findAlcancadosPor}. */
    @Query(ALCANCADOS_POR)
    List<HealthRecord> findAlcancadosPor(@Param("personId") UUID personId,
                                        @Param("agora") LocalDateTime agora);

    /** Listagem paginada de todos os registros do tutor autenticado. */
    @Query(ALCANCADOS_POR)
    Page<HealthRecord> findAlcancadosPor(@Param("personId") UUID personId,
                                        @Param("agora") LocalDateTime agora,
                                        Pageable pageable);

    String ALCANCADOS_POR = "select h from HealthRecord h where exists ("
            + "  select 1 from Custody c where c.animal = h.animal "
            + "    and c.holderPerson.personId = :personId and c.endedAt is null) "
            + "or exists ("
            + "  select 1 from Grant g where g.animal = h.animal "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora)) "
            + "order by h.eventDate desc";

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
