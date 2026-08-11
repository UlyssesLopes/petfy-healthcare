package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalMergeStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O pedido como quem decide o le — <b>a comparacao inteira, e nao um aviso</b>.
 *
 * "Ele recebe o pedido, ve exatamente esta comparacao e decide." Um pedido que so dissesse "a
 * Clinica Vet Norte quer unir dois cadastros" obrigaria o tutor a abrir dois lugares e comparar de
 * cabeca — e ele aceitaria sem olhar, que e o pior desfecho possivel para um ato irreversivel.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalMergeRequestResponseDTO {

    private UUID animalMergeRequestId;

    private AnimalMergeStatus status;

    /** Quem pediu, e em nome de quem. A organizacao e nula quando a pessoa agiu por si. */
    private String requestedByName;

    private String organizationName;

    private String reason;

    private LocalDateTime creationDate;

    private LocalDateTime decidedAt;

    private String decidedByName;

    /** Os dois lados da comparacao, na ordem em que a tela os desenha. */
    private LadoDaUniao absorbed;

    private LadoDaUniao surviving;

    /**
     * Onde os dois discordam, campo a campo.
     *
     * <b>Calculado na leitura enquanto o pedido esta PENDENTE, e lido do que foi guardado depois
     * de decidido.</b> Antes da decisao os dois cadastros existem e podem ate mudar; depois, o
     * valor descartado so existe porque foi gravado no instante em que se descartou.
     */
    private List<Divergencia> divergences;

    /**
     * Um lado da comparacao. Traz o que a Tela 32 mostra: identidade, quem responde, e o TAMANHO
     * da linha do tempo — "147 eventos, desde 14/02/2019" contra "1, hoje" e o que faz quem decide
     * entender qual dos dois e o cadastro real do animal.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LadoDaUniao {

        private UUID animalId;

        private String name;

        private String species;

        private String breed;

        private String microchipNumber;

        /** Nulo quando ninguem responde por ele — o "nenhum ainda" do desenho. */
        private String holderName;

        private long eventCount;

        /** Quando comeca a linha do tempo deste cadastro. Nulo quando ele nao tem evento nenhum. */
        private LocalDateTime firstEventAt;

        /** "4 pessoas, 3 organizacoes" — quantas maos registraram neste cadastro. */
        private long peopleCount;

        private long organizationCount;

    }

    /** "Onde os dois discordam": nome, nascimento, e o que mais existir. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Divergencia {

        /** O nome do campo, para a tela traduzir. Nunca uma frase pronta em portugues. */
        private String field;

        private String survivingValue;

        private String absorbedValue;

    }

}
