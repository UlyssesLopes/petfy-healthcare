package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma pendencia que esta pessoa nao quer mais ser cobrada.
 *
 * <b>Silencia a cobranca, e nao o registro.</b> Nada aqui toca vacina, antiparasitario ou
 * orientacao: a proxima dose continua calculada, a orientacao continua vigente, e a linha do
 * tempo continua recebendo tudo. E a regra dura do 4.2 - <i>"o tutor precisa poder silenciar
 * sem que o registro pare. Produto de saude que nao pode ser calado e desinstalado, e ai
 * para de registrar tambem"</i>.
 *
 * <b>Por pessoa, e nao por animal.</b> Dois tutores dividem o cuidado e dividem a cobranca;
 * um silenciar nao pode calar o outro, porque seria uma pessoa decidindo o que a outra ve
 * sobre a saude do mesmo animal.
 */
@Entity
@Table(name = "due_item_silences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueItemSilence {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "due_item_silence_id", updatable = false, nullable = false)
    private UUID dueItemSilenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DueItemKind kind;

    /**
     * O registro de onde a pendencia deriva.
     *
     * Obrigatorio, e e isso que torna {@code CONSENTIMENTO_PENDENTE} impossivel de gravar
     * aqui - ele nao deriva de registro nenhum, e bloqueia o resto do produto.
     */
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "silenced_at", nullable = false)
    private LocalDateTime silencedAt;

}
