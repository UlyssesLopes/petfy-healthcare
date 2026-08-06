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
     * Telefone de quem responde pelo animal. So aparece com o escopo CONTATO, que
     * o tutor precisa conceder de propria vontade - e o unico dado pessoal dele
     * que o link pode carregar, e existe para o caso de emergencia.
     */
    private String personPhone;

    /** O que este link alcanca. Vem junto para o leitor distinguir vazio de negado. */
    private Set<GrantScope> scopes;

    private LocalDate referenceDate;

    private LocalDateTime expiresAt;

    private List<SharedVaccineDTO> vaccines;

    /** Alergias e condicoes cronicas. Vazio sem o escopo CONDICOES. */
    private List<SharedConditionDTO> conditions;

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

        private String clinicName;

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
