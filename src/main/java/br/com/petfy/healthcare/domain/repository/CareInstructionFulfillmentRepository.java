package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.CareInstructionFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareInstructionFulfillmentRepository
        extends JpaRepository<CareInstructionFulfillment, UUID> {

    /**
     * O ultimo cumprimento de uma orientacao.
     *
     * <b>E o que impede cobrar duas pessoas pela mesma coisa.</b> Dois tutores dando o
     * mesmo remedio e dano, nao incomodo: a pendencia consulta isto e diz quem ja fez,
     * em vez de aparecer identica para os dois.
     */
    Optional<CareInstructionFulfillment>
        findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(UUID careInstructionId);

    List<CareInstructionFulfillment>
        findByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(UUID careInstructionId);

    /**
     * Apaga os cumprimentos de todas as orientacoes dos animais informados.
     *
     * <b>Subquery, e nao {@code f.careInstruction.animal.animalId}.</b> Bulk delete em
     * JPQL nao aceita join implicito - a navegacao encadeada compila e falha ao montar a
     * query, no boot. Terceira vez nesta fase que uma query guardou o modelo antigo ou uma
     * forma que o banco recusa, e nenhuma das tres aparece no compilador.
     */
    @Query("delete from CareInstructionFulfillment f where f.careInstruction in ("
            + "  select i from CareInstruction i where i.animal.animalId in :animalIds)")
    @Modifying
    void deleteByAnimalIdIn(@Param("animalIds") List<UUID> animalIds);

}
