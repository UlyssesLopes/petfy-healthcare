package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "owners")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Owner {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID ownerId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private String address;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    /**
     * Instante da ultima troca de senha. Nulo em quem nunca trocou, o que
     * significa que nao ha token a invalidar.
     */
    private LocalDateTime passwordChangedAt;

    /**
     * Instante em que o tutor confirmou o proprio e-mail. Nulo suspende as
     * notificacoes, mas nao o login - ver V12__email_verification.sql.
     */
    private LocalDateTime emailVerifiedAt;

    public boolean podeReceberNotificacao() {
        return emailVerifiedAt != null;
    }

}
