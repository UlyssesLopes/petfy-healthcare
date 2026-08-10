package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequestDTO {

    /** "Sai as 15h, com a avo": quem busca, quando nao e quem costuma buscar. */
    @Size(max = 200)
    private String pickupNote;
}
