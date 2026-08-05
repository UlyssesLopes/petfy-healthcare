package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AntiparasiticCatalog;
import br.com.petfy.healthcare.domain.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AntiparasiticCatalogRepository extends JpaRepository<AntiparasiticCatalog, UUID> {

    List<AntiparasiticCatalog> findAllByOrderBySpeciesAscNameAsc();

    List<AntiparasiticCatalog> findBySpeciesOrderByNameAsc(Species species);

}
