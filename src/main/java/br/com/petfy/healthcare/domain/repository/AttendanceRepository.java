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

    /**
     * Quantos dias o animal esteve na creche no periodo — "142 dias na creche" (Tela 48).
     *
     * <b>Conta PRESENTE e SAIU, e nao a matricula.</b> Um animal matriculado o ano inteiro que foi
     * doze vezes esteve doze dias na creche, e nao trezentos e sessenta e cinco. E FALTA nao conta
     * pelo motivo obvio, mas vale dizer: ela existe no registro e diz que ele era esperado e nao veio.
     *
     * <b>Distinto por dia</b>, porque um animal com duas matriculas na mesma creche — a que encerrou
     * e a que comecou — contaria o mesmo dia duas vezes.
     */
    @Query("select count(distinct a.day) from Attendance a "
            + "where a.enrollment.animal.animalId = :animalId "
            + "and a.day between :de and :ate "
            + "and a.status in (br.com.petfy.healthcare.domain.entity.AttendanceStatus.PRESENTE, "
            + "                 br.com.petfy.healthcare.domain.entity.AttendanceStatus.SAIU)")
    long contarDiasNaCrecheNoPeriodo(@Param("animalId") UUID animalId,
                                     @Param("de") LocalDate de,
                                     @Param("ate") LocalDate ate);

    /** Neta do animal: a presenca aponta para a matricula, que aponta para o animal. */
    void deleteByEnrollmentAnimalAnimalIdIn(java.util.List<UUID> animalIds);
}
