package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetShareRepository extends JpaRepository<PetShare, UUID> {

    /** Busca por hash, e nao por token: o token nao esta guardado. */
    Optional<PetShare> findByTokenHash(String tokenHash);

    List<PetShare> findByPetOrderByCreationDateDesc(Pet pet);

    List<PetShare> findByPetTutorsOwnerOwnerId(UUID ownerId);

    void deleteByPetPetIdIn(List<UUID> petIds);

}
