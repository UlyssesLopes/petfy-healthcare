package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O que um link de compartilhamento mostra.
 *
 * E a carteira de vacinacao, nao o prontuario: o historico de saude fica de fora
 * de proposito. Quem pede a carteira - hotel, creche, banho e tosa - precisa
 * saber se as vacinas estao em dia, nao que o animal fez uma cirurgia.
 *
 * Do tutor sai apenas o nome, para identificar o responsavel. E-mail, telefone e
 * endereco nao aparecem: o link e publico para quem tem a URL.
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

    private LocalDate referenceDate;

    private LocalDateTime expiresAt;

    private List<SharedVaccineDTO> vaccines;

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

}
