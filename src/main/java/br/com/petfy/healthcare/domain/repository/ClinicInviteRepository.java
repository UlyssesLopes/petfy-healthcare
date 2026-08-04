package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicInviteRepository extends JpaRepository<ClinicInvite, UUID> {

    /** Busca por hash, e nao por token: o token nao esta guardado. */
    Optional<ClinicInvite> findByTokenHash(String tokenHash);

    List<ClinicInvite> findByClinicClinicIdOrderByCreationDateDesc(UUID clinicId);

}
