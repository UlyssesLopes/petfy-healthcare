package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aceite, de um documento, numa versao, por um titular.
 *
 * Linha imutavel por escolha: nao ha setter de negocio nem caminho de update. Aceite
 * corrigido depois nao e mais aceite - e outra linha, com outra data. O que existe e
 * a revogacao pela via que a LGPD ja da, que e apagar a conta.
 */
@Entity
@Table(name = "consent_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRecord {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID consentRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConsentDocument document;

    /**
     * Versao do documento aceito, e nao um booleano.
     *
     * E o campo que faz a tabela valer: politica muda, e "aceitou = true" nao diz com
     * o que a pessoa concordou depois da mudanca. Formato e data (2026-08-05), porque
     * numero de versao sequencial exige quem o incremente e cedo ou tarde alguem
     * esquece.
     */
    @Column(name = "document_version", nullable = false, length = 32)
    private String documentVersion;

    @Column(nullable = false)
    private LocalDateTime acceptedAt;

    /**
     * Evidencia do aceite, quando ha requisicao para extrai-la.
     *
     * Nula em aceite que nao vem de requisicao HTTP. Nao e obrigatoria de proposito:
     * a ausencia da evidencia nao invalida o consentimento, e exigi-la faria a
     * criacao de conta falhar por causa de um proxy mal configurado.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

}
