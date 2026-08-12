package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.ServiceAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAppointmentRepository extends JpaRepository<ServiceAppointment, UUID> {

    /**
     * "Segunda, 10 de agosto · 8 banhos hoje" — a agenda de um dia.
     *
     * <b>Do primeiro ao ultimo horario, e nao por ordem de digitacao.</b> A tela e uma coluna de
     * horarios: quem trabalha nela le de cima para baixo e sabe o que vem em seguida.
     *
     * <b>O `join fetch` do animal nao e otimizacao prematura</b>, e este projeto ja pagou por
     * descobrir isso: sem ele, montar o DTO com o nome de cada animal faz uma consulta por linha e
     * estoura fora da transacao.
     */
    @Query("select a from ServiceAppointment a join fetch a.animal "
            + "where a.organization.organizationId = :organizationId "
            + "and a.scheduledAt between :de and :ate "
            + "order by a.scheduledAt asc")
    List<ServiceAppointment> findDoDia(@Param("organizationId") UUID organizationId,
                                       @Param("de") LocalDateTime de,
                                       @Param("ate") LocalDateTime ate);

    /** O mesmo animal, no mesmo petshop, no mesmo horario — o duplo clique em "Agendar banho". */
    @Query("select a from ServiceAppointment a "
            + "where a.organization.organizationId = :organizationId "
            + "and a.animal.animalId = :animalId and a.scheduledAt = :quando")
    Optional<ServiceAppointment> findNoHorario(@Param("organizationId") UUID organizationId,
                                               @Param("animalId") UUID animalId,
                                               @Param("quando") LocalDateTime quando);

    List<ServiceAppointment> findByAnimalAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
