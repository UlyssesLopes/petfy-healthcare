package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A ficha fechada de um animal que morreu (Tela 33, o cartao "depois").
 *
 * <b>Sem tarja preta, sem laco, sem memorial.</b> "A ficha fica igual as outras — so parou de
 * pedir coisas." Por isso este DTO nao carrega nada de luto: sao os mesmos fatos que qualquer
 * ficha tem, mais a data em que a linha fechou.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedLifeResponseDTO {

    private UUID animalId;

    private String name;

    /** Nulo em animal cuja data de nascimento nunca foi sabida — metade dos resgatados. */
    private LocalDate bornDate;

    private LocalDate deceasedOn;

    private String place;

    private String farewellNote;

    /** Quando o tutor conseguiu vir preencher, que quase nunca e o dia em que aconteceu. */
    private LocalDateTime recordedAt;

    /**
     * O comeco e o fim da custodia de quem esta lendo.
     *
     * <b>Sao dois campos, e nao um "sete anos com voce" ja calculado.</b> O tempo com o tutor nao
     * e a idade do animal — quem adotou aos cinco anos nao teve o bicho a vida toda —, e o
     * servidor devolver a frase pronta esconderia essa diferenca justamente na tela em que ela
     * importa. O cliente formata; os dois fatos vem daqui.
     */
    private LocalDateTime holderSince;

    private LocalDateTime holderUntil;

    /** "Eventos registrados · 153". */
    private long eventCount;

    /** "Quem cuidou dele · 6 pessoas, 3 organizacoes" — quem assinou algo na vida dele. */
    private long caregiverPersonCount;

    private long caregiverOrganizationCount;

}
