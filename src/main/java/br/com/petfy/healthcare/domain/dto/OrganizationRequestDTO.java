package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestDTO {

    @NotBlank(message = "nome e obrigatorio")
    private String name;

    private String ownerVetName;

    private String phone;

    @Email(message = "email invalido")
    private String email;

    private String cnpj;

    private String address;

    private String city;

    private String state;

    private String cep;

    private String description;
}