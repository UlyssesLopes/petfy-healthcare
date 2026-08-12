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

    /**
     * O convite antes de aceitar (Telas 19, 20 e 21).
     *
     * <b>Ler nao consome</b>, pela mesma razao do convite de organizacao: abrir o link para entender o
     * que esta sendo oferecido nao pode gastar o direito de entrar.
     */
    br.com.petfy.healthcare.domain.dto.PetTutorInvitePreviewResponseDTO preview(String token);

    /** Quem recebeu o convite aceita, autenticado, e passa a ser tutor. */
    PetTutorResponseDTO accept(String token);

    /**
     * Quem recebeu diz nao.
     *
     * <b>Nao pede motivo</b>, e a ausencia e deliberada: recusar dividir o cuidado de um animal — ou
     * recusar recebe-lo — e uma decisao pessoal, e um campo de justificativa faria o produto pedir a
     * quem disse nao que explicasse o nao. "Se recusar, Marcelo e avisado e nada muda para o Code."
     */
    void reject(String token);

    /** Quem cuida do animal, em qualquer papel. Visivel a qualquer tutor. */
    List<PetTutorResponseDTO> listTutors(UUID animalId);

    /** Convites do animal, usados e pendentes. So o titular. */
    List<PetTutorInviteResponseDTO> listInvites(UUID animalId);

    void revokeInvite(UUID animalId, UUID petTutorInviteId);

    /** O titular remove um co-tutor; um co-tutor remove a si mesmo. */
    void removeTutor(UUID animalId, UUID personId);

    /** Titular troca o papel de um co-tutor entre EDITOR e VIEWER. */
    PetTutorResponseDTO changeRole(UUID animalId, UUID personId, PetTutorRoleUpdateRequestDTO request);

    /** Titular passa a titularidade a quem ja e tutor do animal. */
    PetTutorResponseDTO transferHolder(UUID animalId, UUID toPersonId);

}
