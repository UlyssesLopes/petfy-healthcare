package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Vet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VetRepository extends JpaRepository<Vet, UUID> {

    Optional<Vet> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Ver o equivalente em OwnerRepository: o filtro trata os dois papeis igual. */
    @Query("select v.passwordChangedAt from Vet v where v.email = :email")
    Optional<LocalDateTime> findPasswordChangedAtByEmail(@Param("email") String email);

}
