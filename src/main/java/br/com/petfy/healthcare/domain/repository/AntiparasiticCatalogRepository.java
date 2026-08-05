package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AntiparasiticCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AntiparasiticCatalogRepository extends JpaRepository<AntiparasiticCatalog, UUID> {
}
