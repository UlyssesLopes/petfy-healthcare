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

    private UUID ownerId;

}
