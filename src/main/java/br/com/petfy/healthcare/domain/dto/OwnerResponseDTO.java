package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OwnerResponseDTO {

    private UUID ownerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

}
