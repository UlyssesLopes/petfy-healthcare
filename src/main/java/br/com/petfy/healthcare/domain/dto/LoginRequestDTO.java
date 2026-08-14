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

    /**
     * Se a sessao sobrevive ao fechamento do navegador.
     *
     * Ausente e {@code true}: e o comportamento que o login sempre teve, e cliente antigo nao muda
     * de vida por causa de um campo novo.
     */
    private Boolean keepSignedIn;

    public boolean manterConectado() {
        return keepSignedIn == null || keepSignedIn;
    }

}
