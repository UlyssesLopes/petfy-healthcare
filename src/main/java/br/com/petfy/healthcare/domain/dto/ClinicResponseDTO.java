package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicResponseDTO {

    private UUID clinicId;

    private String name;

    private String ownerVetName;

    private String phone;

    private String email;

    private String cnpj;

    private String address;

    private String city;

    private String state;

    private String cep;

    private String description;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;
}
