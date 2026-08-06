package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByAnimalAnimalIdOrderByCreationDateDesc(UUID animalId);

    List<Attachment> findByVaccineVaccineIdOrderByCreationDateDesc(UUID vaccineId);

    List<Attachment> findByHealthRecordHealthRecordIdOrderByCreationDateDesc(UUID healthRecordId);

    /**
     * As chaves de storage dos animals informados.
     *
     * Existe porque apagar as linhas nao apaga os arquivos: quem guarda bytes e o
     * storage, que nao participa da transacao. O {@code AnimalPurger} precisa das chaves
     * <b>antes</b> de apagar as linhas, senao perde a unica referencia ao que ficou no
     * disco - e arquivo orfao com dado de saude e dado pessoal nao apagado.
     */
    @Query("select a.storageKey from Attachment a where a.animal.animalId in :animalIds")
    List<String> findStorageKeysByAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

    /**
     * Desassocia quem subiu, sem apagar o anexo.
     *
     * Usado ao apagar a conta. O anexo pertence ao <b>animal</b>, nao a quem fez o upload:
     * num animal que sobrevive porque tem outro tutor, apagar o laudo porque quem o subiu
     * fechou a conta destruiria dado de saude de um animal que continua tendo quem
     * responda por ele - a mesma regra que a V15 aplicou ao animal inteiro.
     *
     * Sem isto, a FK {@code uploaded_by_owner_id} segura o delete do owner: mais uma da
     * familia que travou o {@code DELETE /owners/me} duas vezes.
     */
    @Modifying
    @Query("update Attachment a set a.uploadedBy = null where a.uploadedBy.ownerId = :ownerId")
    void desassociarUploader(UUID ownerId);

}
