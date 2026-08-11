package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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

    /**
     * A ultima visita de cada animal, em uma consulta so — a segunda coluna que faltava na Tela
     * 03.
     *
     * Devolve {@code [animalId, maxEventDate]} por animal. O agrupamento e do BANCO e nao do
     * Java: trazer todos os registros de 318 animais para descobrir uma data por animal seria
     * carregar o historico inteiro da organizacao para exibir uma coluna.
     */
    @Query("select h.animal.animalId, max(h.eventDate) from HealthRecord h "
            + "where h.animal.animalId in :animalIds group by h.animal.animalId")
    List<Object[]> ultimaVisitaPorAnimal(@Param("animalIds") Collection<UUID> animalIds);

    /**
     * Quantos animais, entre os informados, tiveram registro a partir de uma data — o "atendidos
     * este mes" do cabecalho.
     *
     * Conta ANIMAIS e nao registros: dois atendimentos do mesmo animal no mes sao um animal
     * atendido, e a frase do desenho fala de quantos passaram pela organizacao.
     */
    @Query("select count(distinct h.animal.animalId) from HealthRecord h "
            + "where h.animal.animalId in :animalIds and h.eventDate >= :desde")
    long contarAnimaisAtendidosDesde(@Param("animalIds") Collection<UUID> animalIds,
                                     @Param("desde") LocalDate desde);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
