package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthRecordCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthRecordCorrectionRepository extends JpaRepository<HealthRecordCorrection, UUID> {

    List<HealthRecordCorrection> findByHealthRecordHealthRecordIdOrderByCorrectedAtDesc(UUID healthRecordId);

    void deleteByHealthRecordPetPetIdIn(List<UUID> petIds);

}
