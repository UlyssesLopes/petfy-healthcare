package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O aviso que fica dentro do produto.
 *
 * <b>O Petfy avisa por e-mail desde o P4, e nunca avisou dentro dele.</b> A Tela 03 lista quem esta
 * vencendo e nao avisa ninguem; a 47 escreve "voce e avisado na hora" e o produto nao tinha como
 * cumprir. Quem nao confirmou o e-mail nao recebia nada, e a unica forma de descobrir que alguem
 * entrou no seu animal era abrir o app e comparar a lista de tutores com o que se lembrava dela.
 *
 * <b>O texto vem pronto, e nao por referencia.</b> Guardar "tipo do evento + ids" faria o aviso
 * mudar quando o mundo muda: "Ana passou a cuidar do Code" viraria outra coisa no dia em que Ana
 * apagasse a conta, e o que a pessoa leu passaria a ser diferente do que esta escrito. Aviso e um
 * FATO — vale depois de o fato deixar de valer.
 *
 * <b>E o texto e o MESMO do e-mail</b>, montado a partir da mesma {@link
 * br.com.petfy.healthcare.notification.Notification}: duas redacoes do mesmo evento divergiriam no
 * primeiro ajuste de frase, e quem recebe os dois leria coisas diferentes sobre o mesmo fato.
 */
@Entity
@Table(name = "person_notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonNotification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "person_notification_id", updatable = false, nullable = false)
    private UUID personNotificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** O evento, como o dispatcher ja o nomeia. Serve para agrupar e depurar, nao para remontar. */
    @Column(length = 64)
    private String event;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Nulo e "nao lido".
     *
     * Uma data em vez de um booleano porque "quando ela viu" responde perguntas que "se ela viu"
     * nao responde, e nao custa nada a mais.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

}
