package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    /** Usado internamente (agenda, lembretes) onde paginacao nao se aplica. */
    List<Animal> findByTutorsPersonPersonId(UUID personId);

    /** Usado pela listagem paginada do controller. */
    Page<Animal> findByTutorsPersonPersonId(UUID personId, Pageable pageable);

    void deleteByAnimalIdIn(List<UUID> animalIds);

}
