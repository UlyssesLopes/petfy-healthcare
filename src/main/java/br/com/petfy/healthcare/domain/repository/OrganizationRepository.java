package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /** Listagem paginada usada pelo controller. */
    Page<Organization> findAll(Pageable pageable);

}
