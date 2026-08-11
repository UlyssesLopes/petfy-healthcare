package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalMergeRequest;
import br.com.petfy.healthcare.domain.entity.AnimalMergeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalMergeRequestRepository extends JpaRepository<AnimalMergeRequest, UUID> {

    /**
     * O que esta esperando decisao de quem responde por este animal.
     *
     * <b>Busca pelo SOBREVIVENTE</b>, porque e ele que tem dono: quem decide e quem responde pelo
     * cadastro que fica, e o pedido aparece na tela dele.
     */
    List<AnimalMergeRequest> findBySurvivingAnimalIdAndStatus(UUID animalId, AnimalMergeStatus status);

    /**
     * O pedido pendente para este par, se houver.
     *
     * <b>Confere os dois sentidos.</b> Duas clinicas podem perceber a mesma duplicata e pedir a
     * uniao em direcoes opostas — uma achando que o cadastro dela deve sobreviver, a outra o
     * contrario. Sao o mesmo pedido para quem decide, e deixar os dois abertos poria a mesma
     * pergunta duas vezes na frente dele, com respostas que se contradizem.
     */
    @Query("select p from AnimalMergeRequest p where p.status = :status "
            + "and ((p.absorbed.animalId = :um and p.surviving.animalId = :outro) "
            + "  or (p.absorbed.animalId = :outro and p.surviving.animalId = :um))")
    Optional<AnimalMergeRequest> findPendenteEntre(@Param("um") UUID um,
                                                   @Param("outro") UUID outro,
                                                   @Param("status") AnimalMergeStatus status);

    /** Tudo que envolve este animal, dos dois lados — para a tela dizer o que ja houve. */
    @Query("select p from AnimalMergeRequest p where p.absorbed.animalId = :animalId "
            + "or p.surviving.animalId = :animalId order by p.creationDate desc")
    List<AnimalMergeRequest> findEnvolvendo(@Param("animalId") UUID animalId);

    /**
     * Para o {@code AnimalPurger}: o pedido aponta para DOIS animals, e some com qualquer um deles.
     *
     * <b>Aponta para os dois, e por isso a busca e por qualquer um dos lados.</b> Apagar so o que
     * cita o animal como absorvido deixaria de pe o pedido em que ele e o sobrevivente, e o delete
     * do animal falharia por FK — que foi exatamente o que o
     * {@code AnimalPurgerCoverageContainerTest} acusou quando esta tabela nasceu.
     */
    @Query("select p from AnimalMergeRequest p where p.absorbed.animalId in :animalIds "
            + "or p.surviving.animalId in :animalIds")
    List<AnimalMergeRequest> findEnvolvendoQualquer(@Param("animalIds") List<UUID> animalIds);

}
