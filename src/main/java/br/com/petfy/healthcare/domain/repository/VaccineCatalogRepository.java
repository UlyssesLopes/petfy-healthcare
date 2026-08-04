package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccineCatalogRepository extends JpaRepository<VaccineCatalog, UUID> {

    List<VaccineCatalog> findAllByOrderBySpeciesAscNameAsc();

    List<VaccineCatalog> findBySpeciesOrderByNameAsc(Species species);

    /**
     * Usado pelo schedule automatico do filhote: so as vacinas mandatorias da
     * especie viram doses planejadas.
     */
    List<VaccineCatalog> findBySpeciesAndMandatoryTrueOrderByNameAsc(Species species);

}
