package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Gestao de quem cuida do animal: convite, papel, saida e titularidade.
 *
 * A 8a deu ao animal uma tabela de tutores e uma peca so decidindo acesso
 * ({@code AnimalAccessGuard}); aqui essa tabela finalmente ganha um segundo tutor.
 */
public interface PetTutorService {

    /** Titular convida alguem para o animal. Devolve o token uma unica vez. */
    PetTutorInviteResponseDTO invite(UUID animalId, PetTutorInviteRequestDTO request);

    /** Quem recebeu o convite aceita, autenticado, e passa a ser tutor. */
    PetTutorResponseDTO accept(String token);

    /** Quem cuida do animal, em qualquer papel. Visivel a qualquer tutor. */
    List<PetTutorResponseDTO> listTutors(UUID animalId);

    /** Convites do animal, usados e pendentes. So o titular. */
    List<PetTutorInviteResponseDTO> listInvites(UUID animalId);

    void revokeInvite(UUID animalId, UUID petTutorInviteId);

    /** O titular remove um co-tutor; um co-tutor remove a si mesmo. */
    void removeTutor(UUID animalId, UUID ownerId);

    /** Titular troca o papel de um co-tutor entre EDITOR e VIEWER. */
    PetTutorResponseDTO changeRole(UUID animalId, UUID ownerId, PetTutorRoleUpdateRequestDTO request);

    /** Titular passa a titularidade a quem ja e tutor do animal. */
    PetTutorResponseDTO transferHolder(UUID animalId, UUID toOwnerId);

}
