package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Custody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustodyRepository extends JpaRepository<Custody, UUID> {

    /**
     * A custodia em curso do animal.
     *
     * Devolve Optional e nao a entidade porque o animal pode estar entre custodias
     * num instante - obito e perda encerram sem sucessor. Quem chama trata o vazio
     * como "ninguem responde por este animal agora", que e um estado legitimo.
     *
     * O indice unico parcial no banco garante que ha no maximo uma.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId and c.endedAt is null")
    Optional<Custody> findEmCurso(@Param("animalId") UUID animalId);

    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderPerson.personId = :personId and c.endedAt is null")
    Optional<Custody> findEmCursoDaPessoa(@Param("animalId") UUID animalId,
                                          @Param("personId") UUID personId);

    /** Os animais por que a pessoa responde agora. */
    @Query("select c from Custody c where c.holderPerson.personId = :personId and c.endedAt is null")
    List<Custody> findEmCursoDaPessoa(@Param("personId") UUID personId);

    /** A historia inteira do animal, da custodia mais antiga para a mais recente. */
    List<Custody> findByAnimalAnimalIdOrderByStartedAtAsc(UUID animalId);

    /**
     * Toda custodia da pessoa, encerrada ou nao.
     *
     * Usada na exclusao de conta: uma custodia encerrada e historia do animal, e
     * apagar a pessoa nao pode deixar a linha com um buraco sem nome - mas a linha
     * tambem nao pode segurar a exclusao por chave estrangeira.
     */
    List<Custody> findByHolderPersonPersonId(UUID personId);

    List<Custody> findByAnimalAnimalIdIn(List<UUID> animalIds);

}
