package br.com.petfy.healthcare.domain.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicRequestDTO {

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
}