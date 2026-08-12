package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * O cartao que quem achou um animal na rua abre pelo microchip (Tela 34).
 *
 * <b>Nao reusa o {@link SharedVaccineCardDTO}, e a diferenca nao e de campos — e de origem.</b>
 * Aquele cartao mostra o que o TUTOR escolheu mostrar naquele link, e carrega os escopos e a
 * validade para o leitor distinguir vazio de negado. Aqui nao houve escolha de ninguem: o produto
 * decide o conjunto, ele e sempre o mesmo, e nao expira. Um DTO com {@code scopes} e
 * {@code expiresAt} sempre nulos convidaria o cliente a tratar este cartao como aquele.
 *
 * <b>Por que e o cartao inteiro:</b> "quem encontrou pode ser a unica pessoa com o animal nas
 * proximas horas. Se ele estiver em tratamento, ou se comer algo que lhe faz mal, esconder isso
 * nao protege ninguem."
 *
 * <b>E o que continua fora:</b> historico clinico, diagnostico, endereco. "O que o cartao nunca
 * traz continua fora, aqui como em qualquer outro lugar."
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoundAnimalCardDTO {

    private String animalName;

    private String animalType;

    private String animalBreed;

    private String animalGender;

    /** Nulo em quase todo animal resgatado, e a tela nao pede desculpa por isso. */
    private LocalDate animalBornDate;

    /** Ajuda a confirmar que e o mesmo animal que esta na frente de quem procurou. */
    private Double animalWeight;

    /**
     * Para quem ligar: quem responde pelo animal, e as organizacoes que cuidam dele.
     *
     * <b>Uma lista, e nao os dois botoes fixos do desenho.</b> O mockup mostra "Ligar para
     * Marcelo" e "Ligar para a Clinica Vet Norte" porque o Code tem uma clinica; um animal com
     * duas perderia a segunda, e um animal cujo tutor nao pos telefone ficaria com um botao que
     * nao liga para lugar nenhum.
     */
    private List<FoundContactDTO> contacts;

    /** O bloco que mais importa nas proximas horas: o que nao se pode dar ao animal. */
    private List<String> allergies;

    private List<String> conditions;

    /**
     * "Amoxicilina 250 mg, 12/12h, ate 12/08."
     *
     * <b>So o que esta em curso hoje.</b> A orientacao encerrada diria a quem socorre que o animal
     * toma um remedio que ele nao toma mais — e quem esta com o animal na mao agiria sobre isso.
     */
    private List<String> ongoingCare;

    private List<FoundVaccineDTO> vaccines;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoundContactDTO {

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
    public static class FoundVaccineDTO {

        private String vaccineName;

        private LocalDate nextDoseDate;

        private VaccineStatus status;

    }

}
