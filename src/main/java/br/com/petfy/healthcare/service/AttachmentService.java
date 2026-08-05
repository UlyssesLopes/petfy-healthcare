package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Arquivos anexados ao pet: carteirinha de papel, laudo, resultado de exame.
 *
 * Todo anexo pertence a um pet, que e a ancora de autorizacao - quem pode ver o arquivo
 * e quem pode ver o pet. {@code vaccineId} e {@code healthRecordId} sao opcionais e
 * dizem o que o arquivo documenta.
 */
public interface AttachmentService {

    AttachmentResponseDTO upload(UUID petId, MultipartFile file,
                                 UUID vaccineId, UUID healthRecordId, String description);

    List<AttachmentResponseDTO> listByPet(UUID petId, UUID vaccineId, UUID healthRecordId);

    /** O conteudo em si. Autoriza pelo pet do anexo, nunca por um petId informado. */
    AttachmentContent download(UUID attachmentId);

    void delete(UUID attachmentId);

}
