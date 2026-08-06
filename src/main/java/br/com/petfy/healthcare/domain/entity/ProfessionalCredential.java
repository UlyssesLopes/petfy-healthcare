package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O registro profissional de uma pessoa. Hoje so o CRMV.
 *
 * <b>Tabela, e nao coluna em persons.</b> Sao duas razoes, e nenhuma e estetica:
 * o registro e por conselho e por estado, entao uma pessoa pode ter mais de um; e
 * ele tem estado proprio, que muda sem que a pessoa mude.
 *
 * <b>Nao e chave de identidade.</b> A conta segue identificada por e-mail. Fosse
 * identidade, um profissional suspenso perderia a conta, a veterinaria que usa o
 * Petfy so como tutora precisaria de CRMV para existir, e o recem-formado sem
 * registro nao entraria. E a credencial decidindo capacidade, nao identidade.
 */
@Entity
@Table(name = "professional_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalCredential {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "professional_credential_id", updatable = false, nullable = false)
    private UUID professionalCredentialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * O conselho que emite. Hoje sempre CRMV, e como coluna em vez de constante
     * porque o dia em que houver outro nao deve ser uma migration de dado.
     */
    @Column(nullable = false, length = 16)
    private String council;

    /** A UF do registro. O CRMV e estadual, e o mesmo numero se repete entre UFs. */
    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false, length = 32)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CredentialStatus status;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    public boolean autorizaAtoClinico() {
        return status != null && status.autorizaAtoClinico();
    }

}
