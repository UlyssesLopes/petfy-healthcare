package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    @Query("select p from Attendance p where p.enrollment.enrollmentId = :enrollmentId "
            + "and p.day = :dia")
    Optional<Attendance> findDoDia(@Param("enrollmentId") UUID enrollmentId,
                                   @Param("dia") LocalDate dia);

    /** O dia inteiro da turma, para a Tela 17 nao pedir um registro por animal. */
    @Query("select p from Attendance p where p.enrollment.classGroup.classGroupId = :classGroupId "
            + "and p.day = :dia")
    List<Attendance> findDoDiaNaTurma(@Param("classGroupId") UUID classGroupId,
                                      @Param("dia") LocalDate dia);
}
