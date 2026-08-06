package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TimelineRepository extends JpaRepository<TimelineEntry, UUID> {

    /**
     * A linha do tempo de um animal, do mais recente para o mais antigo.
     *
     * <b>Ordena por {@code occurredAt}, e nao por {@code recordedAt}</b> - e por isso
     * que a vacina de 2019 cadastrada hoje aparece em 2019. O desempate por
     * {@code recordedAt} existe para a ordem ser estavel entre paginas: sem ele, dois
     * eventos do mesmo dia poderiam trocar de lugar entre a pagina 1 e a 2, e o cliente
     * veria um item duas vezes e outro nunca.
     *
     * <b>Paginada</b> porque a linha do tempo de um animal de dez anos e a maior
     * colecao que este produto vai servir - e a unica que cresce para sempre.
     */
    @Query("select t from TimelineEntry t where t.animalId = :animalId "
            + "order by t.occurredAt desc, t.recordedAt desc")
    Page<TimelineEntry> findDoAnimal(@Param("animalId") UUID animalId, Pageable pageable);

}
