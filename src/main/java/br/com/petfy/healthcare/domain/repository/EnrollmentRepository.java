package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /**
     * As matriculas vivas da turma, com o animal ja carregado.
     *
     * O `join fetch` nao e otimizacao prematura: a Tela 17 le a turma inteira para montar o dia, e
     * sem ele uma turma de 15 animais faz 16 consultas. Foi um teste que denunciou esse mesmo N+1
     * na lista do abrigo, quebrando com LazyInitializationException.
     */
    @Query("select e from Enrollment e join fetch e.animal "
            + "where e.classGroup.classGroupId = :classGroupId and e.endedAt is null "
            + "order by e.animal.name")
    List<Enrollment> findVivasDaTurma(@Param("classGroupId") UUID classGroupId);

    @Query("select e from Enrollment e where e.animal.animalId = :animalId "
            + "and e.classGroup.classGroupId = :classGroupId and e.endedAt is null")
    Optional<Enrollment> findVivaDoAnimalNaTurma(@Param("animalId") UUID animalId,
                                                 @Param("classGroupId") UUID classGroupId);

    /** Quantas vagas a turma ja ocupou — pendente CONTA, porque a vaga esta guardada. */
    @Query("select count(e) from Enrollment e where e.classGroup.classGroupId = :classGroupId "
            + "and e.endedAt is null")
    long contarVivasDaTurma(@Param("classGroupId") UUID classGroupId);

    /** Todas as matriculas vivas do animal, em qualquer organizacao — usada na Tela 10 do tutor. */
    @Query("select e from Enrollment e join fetch e.classGroup "
            + "where e.animal.animalId = :animalId and e.endedAt is null")
    List<Enrollment> findVivasDoAnimal(@Param("animalId") UUID animalId);

    /** Usada pelo AnimalPurger: apagar o animal apaga a matricula dele. */
    void deleteByAnimalAnimalIdIn(java.util.List<UUID> animalIds);
}
