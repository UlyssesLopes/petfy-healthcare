package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Os gastos de um animal.
 *
 * <b>Nao ha consulta que agregue por organizacao, e nunca havera.</b> Com preco de milhares de
 * atendimentos seria facil mostrar ao tutor que a mesma consulta custa menos duas ruas adiante — e
 * "registro clinico que vira comparador de preco deixa de ser lugar seguro para a clinica registrar
 * a verdade". A ausencia desta consulta e uma decisao de produto, e nao uma lacuna.
 */
public interface AnimalCostRepository extends JpaRepository<AnimalCost, UUID> {

    List<AnimalCost> findByAnimalAnimalIdOrderByOccurredAtDesc(UUID animalId);

    /** Para o purger: o custo some com o animal. */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

    /**
     * Ja existe custo lancado por esta matricula neste dia?
     *
     * <b>E o que impede a diaria avulsa de entrar duas vezes.</b> A creche que marca a entrada,
     * desfaz por engano e marca de novo nao pode cobrar duas diarias do tutor — e ele descobriria
     * isso no fim do mes, quando ninguem mais lembra do que aconteceu naquela terca.
     */
    @org.springframework.data.jpa.repository.Query(
            "select count(c) > 0 from AnimalCost c where c.sourceEnrollmentId = :enrollmentId "
                    + "and c.kind = br.com.petfy.healthcare.domain.entity.AnimalCostKind.CRECHE_DIARIA "
                    + "and c.occurredAt >= :inicioDoDia and c.occurredAt < :fimDoDia")
    boolean existeDiariaNoDia(
            @org.springframework.data.repository.query.Param("enrollmentId") UUID enrollmentId,
            @org.springframework.data.repository.query.Param("inicioDoDia") java.time.LocalDateTime inicioDoDia,
            @org.springframework.data.repository.query.Param("fimDoDia") java.time.LocalDateTime fimDoDia);

}
