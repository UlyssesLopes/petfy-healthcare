package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    List<HealthRecord> findByPetPetIdOrderByEventDateDesc(UUID petId);

    List<HealthRecord> findByPetOwnerOwnerIdOrderByEventDateDesc(UUID ownerId);

    void deleteByPetOwnerOwnerId(UUID ownerId);

}
