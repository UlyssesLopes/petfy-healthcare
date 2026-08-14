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
 * Desde a V51 ela tambem guarda o refresh: o token vive em memoria no cliente, por decisao contra
 * XSS, e e daqui que ele e reemitido quando a pagina recarrega.
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

    /**
     * O hash do refresh token desta entrada.
     *
     * <b>Hash, e nao o token</b> — mesmo criterio do convite e da recuperacao de senha: quem le o
     * banco nao pode sair usando as sessoes de ninguem.
     *
     * <b>Um por sessao, e rotacionar SUBSTITUI.</b> Cada troca emite um token novo e apaga o
     * anterior: um cookie roubado deixa de valer no primeiro refresh legitimo que o dono fizer.
     *
     * Nulo nas sessoes anteriores a V51 — elas nasceram sem cookie no navegador de ninguem, e
     * inventar um hash aqui criaria um refresh que nao existe do outro lado.
     */
    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    /**
     * Ate quando esta entrada pode pedir um token novo sem senha.
     *
     * <b>Nao confundir com a validade do JWT</b>, que continua sendo de duas horas e e o que
     * autoriza cada requisicao. Este prazo e o que a pessoa sente: e por quanto tempo o navegador
     * dela nao vai pedir a senha de novo.
     */
    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    /**
     * Se o cookie desta entrada sobrevive ao fechamento do navegador.
     *
     * Guardado porque o navegador devolve o cookie mas nao o maxAge dele: sem isto, o refresh
     * reemitiria como persistente a sessao que a pessoa pediu que fosse temporaria.
     */
    @Column(name = "refresh_persistent")
    private Boolean refreshPersistent;

    /** Nulo e persistente — e o que toda sessao anterior a V52 era. */
    public boolean persistente() {
        return refreshPersistent == null || refreshPersistent;
    }

    public boolean vigente() {
        return revokedAt == null;
    }

    /**
     * Se esta entrada ainda pode trocar o token vencido por um novo.
     *
     * <b>Encerrada nao renova</b>, e e o que liga a Tela 36 a esta migration: encerrar um aparelho
     * derruba o token atual na proxima requisicao E impede que aquele navegador consiga outro.
     */
    public boolean podeRenovar(LocalDateTime agora) {
        return vigente()
                && refreshTokenHash != null
                && refreshExpiresAt != null
                && refreshExpiresAt.isAfter(agora);
    }

}
