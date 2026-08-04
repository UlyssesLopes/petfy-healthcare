package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "email e obrigatorio")
    private String email;

    @NotBlank(message = "senha e obrigatoria")
    private String password;

}
