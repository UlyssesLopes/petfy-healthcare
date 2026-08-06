package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, UUID> {

    /** Busca por hash, e nao por token: o token nao esta guardado. */
    Optional<OrganizationInvite> findByTokenHash(String tokenHash);

    List<OrganizationInvite> findByOrganizationOrganizationIdOrderByCreationDateDesc(UUID organizationId);

}
