package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "animal_shares")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalShare {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID animalShareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /** Hash do token. O token em si nao e guardado - ver V4__animal_share.sql. */
    @Column(unique = true, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime creationDate;

    public boolean isActive(LocalDateTime agora) {
        return revokedAt == null && expiresAt.isAfter(agora);
    }

}
