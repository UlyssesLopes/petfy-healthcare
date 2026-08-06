package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnimalShareRepository extends JpaRepository<AnimalShare, UUID> {

    /** Busca por hash, e nao por token: o token nao esta guardado. */
    Optional<AnimalShare> findByTokenHash(String tokenHash);

    List<AnimalShare> findByAnimalOrderByCreationDateDesc(Animal animal);

    List<AnimalShare> findByAnimalTutorsPersonPersonId(UUID personId);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
