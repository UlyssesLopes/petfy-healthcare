package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Arquivos anexados ao animal: carteirinha de papel, laudo, resultado de exame.
 *
 * Todo anexo pertence a um animal, que e a ancora de autorizacao - quem pode ver o arquivo
 * e quem pode ver o animal. {@code vaccineId} e {@code healthRecordId} sao opcionais e
 * dizem o que o arquivo documenta.
 */
public interface AttachmentService {

    AttachmentResponseDTO upload(UUID animalId, MultipartFile file,
                                 UUID vaccineId, UUID healthRecordId, String description);

    List<AttachmentResponseDTO> listByAnimal(UUID animalId, UUID vaccineId, UUID healthRecordId);

    /** O conteudo em si. Autoriza pelo animal do anexo, nunca por um animalId informado. */
    AttachmentContent download(UUID attachmentId);

    void delete(UUID attachmentId);

}
