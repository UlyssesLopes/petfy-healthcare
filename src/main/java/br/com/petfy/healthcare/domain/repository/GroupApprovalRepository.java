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

    /**
     * Todo pedido em que esta pessoa aparece — para a exclusao de conta.
     *
     * <b>As QUATRO colunas, e nao so quem pediu.</b> A tabela aponta para {@code persons} quatro
     * vezes — quem pediu, sobre quem, para quem, e quem decidiu — e a primeira e NOT NULL. Esquecer
     * qualquer uma faz o {@code DELETE /persons/me} responder 500 para quem tiver participado de um
     * acordo de duas pessoas num animal que SOBREVIVE a exclusao.
     *
     * <b>O animal que morre com a conta ja estava coberto</b> pelo {@code AnimalPurger}, e e por isso
     * que o defeito ficou escondido desde o bloco 6: o caminho comum apagava o animal junto, e com
     * ele o pedido. O caso que faltava e o do animal que fica — e ele passou a ser a regra quando o
     * encerramento passou a exigir que cada animal tenha destino.
     *
     * <b>Nao ha guarda de schema para FK que aponta para {@code persons}</b>, so para {@code animals}
     * — entao esta consulta e escrita a mao, como o silencio de pendencia e a credencial antes dela.
     */
    @Query("select a from GroupApproval a where a.requestedBy.personId = :personId "
            + "or a.targetPerson.personId = :personId "
            + "or a.toPerson.personId = :personId "
            + "or a.decidedBy.personId = :personId")
    List<GroupApproval> findEnvolvendoPessoa(@Param("personId") UUID personId);

}
