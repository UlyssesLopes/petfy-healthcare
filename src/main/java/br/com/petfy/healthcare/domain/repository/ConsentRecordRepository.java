package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import br.com.petfy.healthcare.domain.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByOwnerOwnerIdOrderByAcceptedAtDesc(UUID ownerId);

    boolean existsByOwnerOwnerIdAndDocumentAndDocumentVersion(
            UUID ownerId, ConsentDocument document, String documentVersion);

    /**
     * Usado ao apagar a conta: a evidencia do aceite e dado pessoal do titular, entao
     * sai junto - ver o javadoc da V16 para por que nao se guarda a prova de
     * consentimento de quem pediu para ser esquecido.
     */
    void deleteByOwnerOwnerId(UUID ownerId);

}
