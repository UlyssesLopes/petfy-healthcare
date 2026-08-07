package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.DueItemKind;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Algo que reivindica acao de alguem, com prazo.
 *
 * <b>Nao e gravada, e derivada.</b> Nao existe tabela de pendencia: ela e lida da dose
 * que vence, da orientacao a cumprir, do convite aguardando resposta. Uma tabela
 * precisaria ser mantida em sincronia com cinco fontes, e a primeira a divergir
 * cobraria algo que ja foi feito - ou deixaria de cobrar algo que falta.
 *
 * <b>Por que precisa de nome:</b> da lugar unico a tudo que o produto vier a cobrar do
 * usuario. Sem ela, cada funcionalidade nova inventa o proprio aviso, no proprio canto,
 * e o tutor passa a ter cinco lugares para olhar.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueItemResponseDTO {

    private DueItemKind kind;

    /** O id do que gera a pendencia - a vacina, a orientacao, o convite. */
    private UUID sourceId;

    /** Nulo no que nao pertence a um animal, como o consentimento pendente. */
    private UUID animalId;

    private String animalName;

    private String description;

    /**
     * Quando vence. Nulo no que nao tem data - consentimento pendente cobra agora, e
     * nao numa data.
     */
    private LocalDate dueOn;

    /** Verdadeiro quando o prazo ja passou. */
    private boolean overdue;

    /**
     * Quando e por quem isto foi cumprido pela ultima vez.
     *
     * <b>Existe para nao cobrar duas pessoas pela mesma coisa sem dizer que a outra ja
     * fez.</b> Dois tutores dando o mesmo remedio e dano, nao incomodo - e sem este
     * campo a pendencia apareceria identica para os dois.
     */
    private LocalDateTime lastFulfilledAt;

    private String lastFulfilledByName;

    /**
     * Se esta pessoa silenciou esta pendencia.
     *
     * <b>Sempre falso no feed default</b>, porque o que ela silenciou nao aparece la - e o
     * ponto de silenciar. Vem verdadeiro em {@code ?includeSilenced=true}, que e como a tela
     * oferece voltar a ser cobrada: sem essa leitura, silenciar seria irreversivel pela
     * interface, e a acao mora <i>na</i> pendencia (DESIGN 5.3).
     *
     * Silenciar nao para o registro: a proxima dose continua calculada e a linha do tempo
     * continua recebendo tudo (PRODUTO 4.2).
     */
    private boolean silenced;

}
