package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um acesso de terceiro ao dado de saude de um animal.
 *
 * Append-only: nao ha caminho de update nem de delete individual. Log de auditoria
 * que pode ser editado nao e log de auditoria - a unica remocao e em massa, quando o
 * animal deixa de existir e nao ha mais tutor a quem responder.
 *
 * O ator e guardado como tipo + id + <b>nome no momento do acesso</b>, e nao por
 * chave estrangeira para {@code vets}. Duas razoes: o veterinario pode fechar a conta
 * depois, e o registro de que ele leu o historico nao pode virar linha sem nome; e
 * uma FK faria a exclusao de conta de veterinario esbarrar neste log, que e a mesma
 * familia de bug que travou o {@code DELETE /persons/me} duas vezes.
 */
@Entity
@Table(name = "sensitive_access_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveAccessLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID sensitiveAccessLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private AccessActorType actorType;

    /** Nulo para {@link AccessActorType#SHARE_LINK}: quem abre o link nao tem conta. */
    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    /** Qual clinica, porque e a clinica que o tutor autorizou - nao a pessoa. */
    @Column(name = "organization_name")
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccessedResource resource;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

}
