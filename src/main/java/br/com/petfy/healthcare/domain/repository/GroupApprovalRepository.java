package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.GroupApproval;
import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import br.com.petfy.healthcare.domain.entity.GroupApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupApprovalRepository extends JpaRepository<GroupApproval, UUID> {

    /**
     * O que espera decisao no grupo.
     *
     * <b>Traz TUDO que esta pendente, inclusive o que a propria pessoa pediu.</b> Esconder o
     * proprio pedido pareceria mais limpo e seria pior: quem pediu precisa ver que o pedido
     * continua parado — e e isso que o faz procurar alguem para olhar.
     */
    @Query("select a from GroupApproval a where a.organization.organizationId = :organizationId "
            + "and a.status = br.com.petfy.healthcare.domain.entity.GroupApprovalStatus.PENDENTE "
            + "order by a.requestedAt desc")
    List<GroupApproval> findPendentesDoGrupo(@Param("organizationId") UUID organizationId);

    /**
     * O mesmo ato, sobre o mesmo animal, ja pedido.
     *
     * Pedir duas vezes daria a duas pessoas a mesma pergunta em duplicata — e as duas
     * concordariam com o que aconteceria uma vez so.
     */
    Optional<GroupApproval> findByOrganizationOrganizationIdAndKindAndAnimalAnimalIdAndStatus(
            UUID organizationId, GroupApprovalKind kind, UUID animalId, GroupApprovalStatus status);

    Optional<GroupApproval> findByOrganizationOrganizationIdAndKindAndTargetPersonPersonIdAndStatus(
            UUID organizationId, GroupApprovalKind kind, UUID personId, GroupApprovalStatus status);

    List<GroupApproval> findByAnimalAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
