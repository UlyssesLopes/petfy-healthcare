package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findByOwnerOwnerId(UUID ownerId);

}
