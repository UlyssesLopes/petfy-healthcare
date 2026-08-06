package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um arquivo anexado ao animal.
 *
 * O {@code animal} e obrigatorio e faz dois trabalhos: e a ancora de autorizacao - toda
 * pergunta sobre quem pode ver este arquivo se reduz a quem pode ver este animal, que o
 * {@code AnimalAccessGuard} ja responde - e a ancora de limpeza, porque o
 * {@code AnimalPurger} apaga por animalId.
 *
 * {@code vaccine} e {@code healthRecord} dizem o que o arquivo documenta, e no maximo
 * um dos dois esta preenchido - garantido por CHECK no banco, e nao por validacao em
 * codigo que um insert direto contornaria. Os dois nulos significa anexo do animal em si:
 * foto, RG animal.
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

public class Attachment extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id")
    private Vaccine vaccine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    /** Nome que o cliente enviou. Nunca entra na montagem de caminho - ver storageKey. */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /** Tipo <b>detectado pelo conteudo</b>, e nao o declarado no upload. */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    /**
     * Caminho no storage, gerado pelo servidor. Nenhum pedaco vem do cliente: nome de
     * arquivo de terceiro dentro de um caminho e travessia de diretorio esperando
     * acontecer.
     */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(length = 500)
    private String description;

    /**
     * Quem subiu. Nulo se essa pessoa apagou a conta depois - o arquivo pertence ao
     * animal, que sobrevive se houver outro tutor.
     */
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

}
