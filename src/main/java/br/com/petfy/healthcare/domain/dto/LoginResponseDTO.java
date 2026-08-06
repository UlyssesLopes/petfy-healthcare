package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;

    private String tokenType;

    private long expiresInMinutes;

    /** OWNER ou VET - o cliente precisa saber que tela abrir. */
    private String role;

    /** Preenchido apenas quando role e OWNER. */
    private UUID personId;

    /** Preenchido apenas quando role e VET. */
    private UUID vetId;

}
