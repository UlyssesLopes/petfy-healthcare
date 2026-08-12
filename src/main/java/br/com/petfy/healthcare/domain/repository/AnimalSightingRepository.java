package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalSighting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalSightingRepository extends JpaRepository<AnimalSighting, UUID> {

    /**
     * O avistamento desta pessoa, neste animal, neste dia.
     *
     * Existe para o gesto ser <b>idempotente</b>: Sandra passa na praca de manha e a noite e toca
     * duas vezes no mesmo botao. A segunda nao pode virar linha nova nem erro — ela ja fez o que
     * queria fazer.
     */
    Optional<AnimalSighting> findByAnimalAnimalIdAndRecordedByPersonIdAndSeenOn(
            UUID animalId, UUID personId, LocalDate seenOn);

    /**
     * O ultimo avistamento de cada animal de um lote — a coluna "visto por ultimo".
     *
     * <b>Em lote, e agregada.</b> A tela mostra catorze gatos; uma consulta por linha seria o
     * mesmo N+1 que a V29 tirou da linha do tempo, e aqui ela roda na tela mais aberta do grupo.
     *
     * Devolve o dia e quem viu, porque a celula diz as duas coisas: "hoje, por Sandra". O
     * desempate por {@code recordedAt} existe para o caso de duas pessoas registrarem o mesmo
     * dia — a mais recente e a que aparece, e a escolha e arbitraria mas <b>estavel</b>: sem ela,
     * a mesma tela recarregada mostraria nomes diferentes.
     */
    @Query("select s.animal.animalId as animalId, s.seenOn as seenOn, s.recordedBy.name as quem "
            + "from AnimalSighting s "
            + "where s.animal.animalId in :animalIds "
            + "  and s.seenOn = (select max(s2.seenOn) from AnimalSighting s2 "
            + "                  where s2.animal = s.animal) "
            + "order by s.recordedAt desc")
    List<UltimoAvistamento> ultimoDeCada(@Param("animalIds") List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

    /** Projecao da consulta acima. */
    interface UltimoAvistamento {

        UUID getAnimalId();

        LocalDate getSeenOn();

        String getQuem();

    }

}
