package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um tutor do pet, na listagem de quem cuida dele.
 *
 * O e-mail entra de proposito: saber <b>quem</b> alcanca o historico de saude do
 * seu pet e parte da privacidade, nao vazamento dela - e sem o e-mail nao ha como
 * distinguir dois tutores de mesmo nome antes de remover um. So quem ja e tutor do
 * pet ve esta lista.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorResponseDTO {

    private UUID petTutorId;

    private UUID petId;

    private UUID ownerId;

    private String ownerName;

    private String ownerEmail;

    private PetTutorRole role;

    private boolean holder;

    /** Quem convidou. Nulo nos vinculos que a V15 criou no backfill. */
    private String invitedByOwnerName;

    private LocalDateTime creationDate;

}
