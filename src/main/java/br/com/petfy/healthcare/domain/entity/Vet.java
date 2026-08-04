package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vet {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID vetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    /** Registro profissional. Nao ha verificacao - ver limitacoes no README. */
    private String crmv;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    /**
     * Instante da ultima troca de senha. Nulo em quem nunca trocou. Hoje o vet
     * ainda nao troca a propria senha, mas a coluna existe para o filtro tratar
     * os dois papeis pelo mesmo caminho.
     */
    private LocalDateTime passwordChangedAt;

}
