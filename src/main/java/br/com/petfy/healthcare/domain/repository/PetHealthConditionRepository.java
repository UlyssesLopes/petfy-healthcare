package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetHealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetHealthConditionRepository extends JpaRepository<PetHealthCondition, UUID> {

    /**
     * Ativas primeiro, depois as encerradas, cada grupo da mais recente para a mais antiga.
     *
     * A pergunta que esta lista responde e "o que vale para este animal hoje", e o que foi
     * encerrado e contexto - por isso {@code resolvedAt} nulo vem na frente.
     *
     * <b>JPQL explicito, e nao nome derivado.</b> {@code NULLS FIRST} nao existe na
     * gramatica de nome de metodo do Spring Data: escrito como
     * {@code OrderByResolvedAtAscNullsFirst...} o parser procura uma propriedade chamada
     * "resolvedAtAscNullsFirst", nao acha, e o contexto da aplicacao <b>nem sobe</b> -
     * falha na inicializacao, nao em runtime.
     */
    @Query("select c from PetHealthCondition c where c.pet.petId = :petId "
            + "order by c.resolvedAt asc nulls first, c.creationDate desc")
    List<PetHealthCondition> findByPetOrdenadasPorRelevancia(UUID petId);

    /** Usado ao apagar o pet - ver {@code PetPurger}. */
    void deleteByPetPetIdIn(List<UUID> petIds);

}
