package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma entrada na conta — o "aparelho conectado" da Tela 36.
 *
 * <b>Ela nao e sessao de servidor.</b> O token continua sendo a credencial, assinado, e continua
 * valendo pelo prazo dele sem que ninguem consulte nada para emitir. O que esta linha permite e o
 * inverso: dizer que um token especifico deixou de valer <b>antes da hora</b>.
 *
 * <b>E nao e refresh token.</b> Recarregar a pagina continua deslogando — o token vive em memoria no
 * cliente, e isso e decisao contra XSS. Esta tabela e pre-requisito do refresh, e nao substituto.
 */
@Entity
@Table(name = "person_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSession {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "person_session_id", updatable = false, nullable = false)
    private UUID personSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Nulo e "ainda vale".
     *
     * <b>A sessao encerrada nao e apagada:</b> quem encerra por suspeita de acesso indevido tem
     * interesse em que o registro de que ela existiu permaneca.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Como a pessoa reconhece o aparelho, e nada alem disso.
     *
     * <b>Nao ha IP.</b> Ele nao ajuda ninguem a reconhecer o proprio aparelho — "189.4.x.x" nao diz
     * nada a quem esta lendo — e guardar por onde alguem entra e o que a Tela 34 recusa quando diz
     * "nao guardamos quem fez a busca, e por onde".
     */
    @Column(name = "user_agent", length = 400)
    private String userAgent;

    public boolean vigente() {
        return revokedAt == null;
    }

}
