package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinic {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID clinicId;

    private String name;

    private String ownerVetName;

    private String phone;

    private String email;

    private String cnpj;

    private String address;

    private String city;

    private String state;

    private String cep;

    private String description;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}
