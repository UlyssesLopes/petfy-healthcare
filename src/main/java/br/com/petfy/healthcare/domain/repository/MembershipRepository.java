package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /**
     * Os vinculos ativos da pessoa.
     *
     * Devolve lista, e nao Optional, porque N vinculos e o caso que este passo existe
     * para permitir. Quem chama decide o que fazer com mais de um - e a resposta nao e
     * escolher o primeiro em silencio.
     */
    @Query("select m from Membership m where m.person.personId = :personId and m.leftAt is null")
    List<Membership> findAtivosDaPessoa(@Param("personId") UUID personId);

    @Query("select m from Membership m where m.person.personId = :personId "
            + "and m.organization.organizationId = :organizationId and m.leftAt is null")
    Optional<Membership> findAtivoDaPessoaNaOrganizacao(@Param("personId") UUID personId,
                                                       @Param("organizationId") UUID organizationId);

    /** Os membros de uma organizacao agora. */
    @Query("select m from Membership m where m.organization.organizationId = :organizationId "
            + "and m.leftAt is null")
    List<Membership> findAtivosDaOrganizacao(@Param("organizationId") UUID organizationId);

    /**
     * Todo vinculo da pessoa, ativo ou nao. Usado na exclusao de conta: a linha aponta
     * para persons e seguraria o delete.
     */
    List<Membership> findByPersonPersonId(UUID personId);

}
