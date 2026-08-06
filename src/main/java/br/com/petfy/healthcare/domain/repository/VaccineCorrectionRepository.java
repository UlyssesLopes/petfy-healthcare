package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccineCorrectionRepository extends JpaRepository<VaccineCorrection, UUID> {

    List<VaccineCorrection> findByVaccineVaccineIdOrderByCorrectedAtDesc(UUID vaccineId);

    void deleteByVaccineAnimalAnimalIdIn(List<UUID> animalIds);

}
