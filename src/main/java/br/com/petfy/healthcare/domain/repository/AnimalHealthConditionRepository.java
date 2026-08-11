package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnimalHealthConditionRepository extends JpaRepository<AnimalHealthCondition, UUID> {

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
    @Query("select c from AnimalHealthCondition c where c.animal.animalId = :animalId "
            + "order by c.resolvedAt asc nulls first, c.creationDate desc")
    List<AnimalHealthCondition> findByAnimalOrdenadasPorRelevancia(UUID animalId);

    /**
     * Quais dos animais informados tem condicao ABERTA — o "em tratamento" da Tela 03.
     *
     * <b>Em tratamento e condicao sem `resolvedAt`</b>, e nao um campo proprio: o produto ja diz
     * "o que vale para este animal hoje" por essa ausencia, e criar um segundo lugar para a mesma
     * verdade e criar a chance de os dois divergirem.
     */
    @Query("select distinct c.animal.animalId from AnimalHealthCondition c "
            + "where c.animal.animalId in :animalIds and c.resolvedAt is null")
    List<UUID> idsComCondicaoAberta(@Param("animalIds") Collection<UUID> animalIds);

    /** Usado ao apagar o animal - ver {@code AnimalPurger}. */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
