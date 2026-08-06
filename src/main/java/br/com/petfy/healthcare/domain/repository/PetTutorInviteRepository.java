package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetTutorInviteRepository extends JpaRepository<PetTutorInvite, UUID> {

    /** Busca por hash, e nao por token: o token nao esta guardado. */
    Optional<PetTutorInvite> findByTokenHash(String tokenHash);

    List<PetTutorInvite> findByAnimalAnimalIdOrderByCreationDateDesc(UUID animalId);

    /**
     * Usado ao apagar o animal e ao apagar a conta: convite pendente aponta para o
     * animal e para quem o criou, entao seguraria os deletes seguintes.
     */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

    void deleteByCreatedByOwnerId(UUID ownerId);

    /**
     * Quem aceitou um convite e depois apaga a conta: a linha aponta para ele por
     * accepted_by_owner_id, entao seguraria o delete do owner. O convite e
     * credencial de uso unico e ja expirada, nao historico de saude - apagar segue
     * a mesma politica do passo 10, que escolheu apagar em vez de anonimizar.
     */
    void deleteByAcceptedByOwnerId(UUID ownerId);

}
