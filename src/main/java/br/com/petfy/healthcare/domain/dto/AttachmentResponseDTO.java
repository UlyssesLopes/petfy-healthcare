package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadado do anexo. O conteudo vem por rota propria, em stream.
 *
 * Nao ha campo de URL: o download passa pela API para ser autorizado a cada chamada -
 * ver o javadoc de {@code AttachmentStorage} para por que nao se usa URL assinada em
 * dado de saude.
 *
 * O {@code storageKey} nao volta. E detalhe de infraestrutura, muda quando o storage
 * mudar, e um cliente que o conhecesse acabaria tentando montar caminho com ele.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO {

    private UUID attachmentId;

    private UUID animalId;

    private UUID vaccineId;

    private UUID healthRecordId;

    private String originalFilename;

    /** Tipo detectado pelo conteudo, e nao o que o upload declarou. */
    private String contentType;

    private Long sizeBytes;

    /** Permite ao cliente conferir que o byte que baixou e o que foi guardado. */
    private String checksumSha256;

    private String description;

    private String uploadedByOwnerName;

    private LocalDateTime creationDate;

}
