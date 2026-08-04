package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Vet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetRepository extends JpaRepository<Vet, UUID> {

    Optional<Vet> findByEmail(String email);

    boolean existsByEmail(String email);

}
