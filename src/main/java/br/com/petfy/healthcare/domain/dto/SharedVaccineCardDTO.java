package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * O que um link de compartilhamento mostra.
 *
 * <b>Ate o P2 isto era sempre a carteira, e so ela.</b> Continua sendo o default,
 * pelo mesmo motivo de antes: quem pede a carteira - hotel, creche, banho e tosa -
 * precisa saber se as vacinas estao em dia, nao que o animal fez uma cirurgia.
 *
 * O que mudou e que o tutor pode alargar, escolhendo o escopo na criacao do link.
 * E o que permite o <b>cartao de emergencia</b> da decisao 13: sem quebra-vidro, a
 * compensacao e o tutor preparar antes um link com alergia, condicoes e contato,
 * para quem socorre ler sem precisar acorda-lo. Quem autoriza continua sendo ele,
 * antecipadamente, em vez de o sistema decidir por ele no susto.
 *
 * <b>Os campos fora do escopo vem vazios, e nao ausentes</b> - quem le sabe que
 * perguntou e que a resposta foi "isto nao foi concedido", em vez de ficar na
 * duvida se o animal simplesmente nao tem alergia registrada. Por isso {@code
 * scopes} vem junto.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedVaccineCardDTO {

    private String animalName;

    private String animalType;

    private String animalBreed;

    private LocalDate animalBornDate;

    private String animalGender;

    private String personName;

    /**
     * Para quem ligar: quem responde pelo animal, e as organizacoes que o atendem.
     *
     * Vazio sem o escopo CONTATO, que o tutor concede de propria vontade - e o unico
     * dado pessoal dele que o link pode carregar, e existe para o caso de emergencia.
     */
    private List<SharedContactDTO> contacts;

    /** O que este link alcanca. Vem junto para o leitor distinguir vazio de negado. */
    private Set<GrantScope> scopes;

    private LocalDate referenceDate;

    private LocalDateTime expiresAt;

    private List<SharedVaccineDTO> vaccines;

    /** Alergias e condicoes cronicas. Vazio sem o escopo CONDICOES. */
    private List<SharedConditionDTO> conditions;

    /**
     * "Amoxicilina 250 mg, 12/12h, ate 12/08" — so o que esta em curso hoje.
     *
     * A orientacao ja encerrada diria a quem socorre que o animal toma um remedio que
     * ele nao toma mais. Vazio sem o escopo CONDICOES, que e onde a medicacao mora:
     * quem concede "o que ele tem" concede junto "o que ele esta tomando por causa disso".
     */
    private List<String> ongoingCare;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedContactDTO {

        private String name;

        /** Nulo quando o cadastro nao tem telefone. O cartao mostra o nome mesmo assim. */
        private String phone;

        /** TUTOR ou ORGANIZACAO — muda o que quem liga espera do outro lado. */
        private String kind;

    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedVaccineDTO {

        private String vaccineName;

        private LocalDate applicationDate;

        private LocalDate nextDoseDate;

        private VaccineStatus status;

        private String organizationName;

    }

    /**
     * A alergia e a informacao mais valiosa deste documento numa emergencia, e e
     * exatamente a que o link nao mostrava antes do P2.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedConditionDTO {

        private AnimalHealthConditionKind kind;

        private String description;

        private AnimalHealthConditionSeverity severity;

        /** Preenchido em condicao ja encerrada. Nao se apaga, encerra-se. */
        private LocalDate resolvedAt;

    }

}
