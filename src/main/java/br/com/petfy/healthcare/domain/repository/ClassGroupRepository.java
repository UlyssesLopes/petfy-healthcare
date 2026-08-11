package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, UUID> {

    @Query("select t from ClassGroup t where t.organization.organizationId = :organizationId "
            + "and t.endedAt is null order by t.name")
    List<ClassGroup> findVigentesDaOrganizacao(@Param("organizationId") UUID organizationId);

    /**
     * A turma pela organizacao, e nao pelo id solto.
     *
     * <b>Ler por id sem amarrar na organizacao seria a porta para uma creche marcar entrada na
     * turma de outra.</b> O id e um UUID e ninguem adivinha — mas "ninguem adivinha" nao e
     * autorizacao, e o dia em que um id vazar por log a diferenca aparece.
     */
    @Query("select t from ClassGroup t where t.classGroupId = :classGroupId "
            + "and t.organization.organizationId = :organizationId")
    Optional<ClassGroup> findDaOrganizacao(@Param("classGroupId") UUID classGroupId,
                                           @Param("organizationId") UUID organizationId);
}
