package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.OrganizationVaccineRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationVaccineRequirementRepository
        extends JpaRepository<OrganizationVaccineRequirement, UUID> {

    @Query("select r from OrganizationVaccineRequirement r join fetch r.vaccineCatalog "
            + "where r.organization.organizationId = :organizationId")
    List<OrganizationVaccineRequirement> findDaOrganizacao(@Param("organizationId") UUID organizationId);
}
