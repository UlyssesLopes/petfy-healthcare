package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.CareInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CareInstructionRepository extends JpaRepository<CareInstruction, UUID> {

    List<CareInstruction> findByAnimalAnimalIdOrderByStartsOnDesc(UUID animalId);

    /**
     * As orientacoes valendo hoje nos animais informados.
     *
     * <b>Consulta por animal, e nao por pessoa.</b> A orientacao segue a custodia: quem
     * deve cumpri-la e quem responde pelo animal <i>agora</i>, e nao quem estava lá
     * quando o veterinario prescreveu. Filtrar por emissor faria o tratamento
     * desaparecer da lista de quem assumiu o animal no meio dele.
     */
    @Query("select i from CareInstruction i where i.animal.animalId in :animalIds "
            + "and i.revokedAt is null and i.startsOn <= :hoje "
            + "and (i.endsOn is null or i.endsOn >= :hoje)")
    List<CareInstruction> findVigentesNosAnimais(@Param("animalIds") List<UUID> animalIds,
                                                 @Param("hoje") LocalDate hoje);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
