package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationConfirmDTO {

    @NotBlank(message = "token e obrigatorio")
    private String token;

}
