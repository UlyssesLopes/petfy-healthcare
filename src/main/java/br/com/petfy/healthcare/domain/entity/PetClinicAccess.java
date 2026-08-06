package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pet_clinic_access")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetClinicAccess {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID petClinicAccessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    private LocalDateTime revokedAt;

    public boolean isActive() {
        return revokedAt == null;
    }

}
