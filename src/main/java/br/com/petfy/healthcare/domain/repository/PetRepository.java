package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    /** Usado internamente (agenda, lembretes) onde paginacao nao se aplica. */
    List<Pet> findByOwnerOwnerId(UUID ownerId);

    /** Usado pela listagem paginada do controller. */
    Page<Pet> findByOwnerOwnerId(UUID ownerId, Pageable pageable);

}
