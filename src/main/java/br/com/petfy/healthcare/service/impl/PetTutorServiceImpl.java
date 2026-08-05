package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetTutorServiceImpl implements PetTutorService {

    private final PetTutorRepository petTutorRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final OwnerRepository ownerRepository;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final PetAccessGuard petAccessGuard;
    private final OpaqueTokenService opaqueTokenService;

    @Value("${petfy.pet-tutor-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    /**
     * Convidar e do titular. Um EDITOR que pudesse convidar contornaria a regra:
     * bastaria convidar um comparsa como HOLDER para tomar o pet de quem o
     * cadastrou.
     *
     * O convite existe em vez de vinculo direto porque o caso comum e o conjuge
     * que <b>ainda nao tem conta</b> - exigir cadastro previo mataria o fluxo onde
     * ele comeca. E vincular alguem sem que aceite faria a pessoa passar a receber
     * lembrete que nao pediu.
     */
    @Override
    @Transactional
    public PetTutorInviteResponseDTO invite(UUID petId, PetTutorInviteRequestDTO request) {
        Pet pet = petAccessGuard.requireTitular(petId);
        Owner emissor = currentOwnerProvider.require();

        recusarSeJaETutor(petId, request.getEmail());

        int validade = request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        PetTutorInvite invite = petTutorInviteRepository.save(PetTutorInvite.builder()
                .pet(pet)
                .createdBy(emissor)
                .tokenHash(opaqueTokenService.hash(token))
                .email(request.getEmail().trim())
                .role(request.getRole())
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(invite, token);
    }

    /**
     * Aceitar exige estar autenticado, e o e-mail da conta tem de ser o do
     * convite. Sem isso o link viraria portador: quem o recebesse encaminhado
     * entraria no historico de saude de um animal que nao e dele.
     *
     * <b>Token invalido, expirado, revogado, ja usado e destinado a outra pessoa
     * respondem igual.</b> Distinguir diria a quem tenta adivinhar qual parte
     * errou - e um convite de pet e o que separa um estranho da carteira inteira.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO accept(String token) {
        Owner aceitante = currentOwnerProvider.require();

        PetTutorInvite invite = petTutorInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .filter(i -> i.getEmail().equalsIgnoreCase(aceitante.getEmail()))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        UUID petId = invite.getPet().getPetId();

        if (petTutorRepository.existsByPetPetIdAndOwnerOwnerId(petId, aceitante.getOwnerId())) {
            throw jaETutor();
        }

        // A transferencia rebaixa o titular atual ANTES de o novo vinculo entrar.
        // O indice unico parcial da V15 admite um HOLDER por pet: inserir primeiro
        // deixaria dois na tabela e o Postgres recusaria o insert.
        if (invite.transfereTitularidade()) {
            rebaixarTitular(petId);
        }

        PetTutor vinculo = petTutorRepository.save(PetTutor.builder()
                .pet(invite.getPet())
                .owner(aceitante)
                .role(invite.getRole())
                .invitedBy(invite.getCreatedBy())
                .creationDate(LocalDateTime.now())
                .build());

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);
        petTutorInviteRepository.save(invite);

        return toResponse(vinculo);
    }

    /**
     * Qualquer tutor ve a lista. Saber quem alcanca o historico de saude do seu
     * pet e parte da privacidade, e nao o contrario - inclusive para quem so
     * acompanha.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorResponseDTO> listTutors(UUID petId) {
        petAccessGuard.requireLeitura(petId);

        return petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Convite pendente e informacao de gestao: so o titular, que os emitiu. */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorInviteResponseDTO> listInvites(UUID petId) {
        petAccessGuard.requireTitular(petId);

        return petTutorInviteRepository.findByPetPetIdOrderByCreationDateDesc(petId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeInvite(UUID petId, UUID petTutorInviteId) {
        petAccessGuard.requireTitular(petId);

        PetTutorInvite invite = petTutorInviteRepository.findById(petTutorInviteId)
                .filter(i -> i.getPet().getPetId().equals(petId))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            petTutorInviteRepository.save(invite);
        }
    }

    /**
     * Duas saidas pela mesma porta, com regras diferentes:
     *
     * <ul>
     *   <li><b>O titular remove um co-tutor</b> - tirar acesso ao pet e decisao de
     *       quem responde por ele.</li>
     *   <li><b>Um co-tutor remove a si mesmo</b> - quem nao quer mais acompanhar
     *       sai sozinho, sem depender do titular. Exigir autorizacao para sair
     *       prenderia a pessoa a notificacoes de um pet que nao e dela.</li>
     * </ul>
     *
     * O titular nunca sai por aqui, nem por vontade propria: o indice do banco
     * exige exatamente um HOLDER, e um pet sem titular ficaria sem ninguem que
     * pudesse convidar ou apaga-lo. Transfere primeiro, ou apaga o pet.
     */
    @Override
    @Transactional
    public void removeTutor(UUID petId, UUID ownerId) {
        UUID autenticadoId = currentOwnerProvider.require().getOwnerId();
        boolean saindoSozinho = autenticadoId.equals(ownerId);

        if (saindoSozinho) {
            // provar que e tutor basta: sair nao exige nivel nenhum
            petAccessGuard.requireLeitura(petId);
        } else {
            petAccessGuard.requireTitular(petId);
        }

        PetTutor vinculo = buscarVinculo(petId, ownerId);

        if (vinculo.isHolder()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getMessage(),
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getCode(),
                    HttpStatus.CONFLICT);
        }

        petTutorRepository.delete(vinculo);
    }

    /**
     * Troca entre EDITOR e VIEWER. HOLDER nao entra: promover alguem a titular
     * rebaixa o titular atual, entao nao e mudar o papel de um tutor - e a
     * transferencia, que pede confirmacao propria.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO changeRole(UUID petId, UUID ownerId, PetTutorRoleUpdateRequestDTO request) {
        petAccessGuard.requireTitular(petId);

        if (request.getRole() == PetTutorRole.HOLDER) {
            throw transferenciaExigida();
        }

        PetTutor vinculo = buscarVinculo(petId, ownerId);

        // rebaixar o titular por aqui deixaria o pet sem nenhum, e o indice do
        // banco recusaria - mas a resposta certa nao e 500: e apontar a porta
        if (vinculo.isHolder()) {
            throw transferenciaExigida();
        }

        vinculo.setRole(request.getRole());
        vinculo.setUpdateDate(LocalDateTime.now());

        return toResponse(petTutorRepository.save(vinculo));
    }

    /**
     * Adocao, venda, separacao: o pet troca de titular sem trocar de historico.
     *
     * Quem recebe precisa <b>ja ser tutor</b>. Transferir para quem esta fora do
     * pet e o convite com papel HOLDER - la a pessoa aceita, e aqui nao haveria
     * como pedir consentimento de quem passaria a responder pelo animal.
     *
     * O titular antigo vira EDITOR em vez de sair: quem cuidou do animal ate
     * ontem continua enxergando a carteira, e o novo titular decide se remove.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO transferHolder(UUID petId, UUID toOwnerId) {
        petAccessGuard.requireTitular(petId);

        PetTutor destino = buscarVinculo(petId, toOwnerId);

        // transferir para quem ja e o titular e no-op, e nao erro: o estado final
        // pedido e o estado atual
        if (destino.isHolder()) {
            return toResponse(destino);
        }

        // mesma ordem da aceitacao: rebaixa antes de promover, senao os dois
        // HOLDER coexistem e o indice unico parcial recusa
        rebaixarTitular(petId);

        destino.setRole(PetTutorRole.HOLDER);
        destino.setUpdateDate(LocalDateTime.now());

        return toResponse(petTutorRepository.save(destino));
    }

    /**
     * Rebaixa o titular atual a EDITOR e descarrega no banco.
     *
     * O {@code flush} nao e zelo: sem ele o Hibernate pode emitir o insert ou o
     * update do novo titular antes deste update, e o indice unico parcial veria
     * dois HOLDER no mesmo pet. Foi exatamente assim que a exclusao de conta
     * quebrou - ver OwnerServiceImpl.
     */
    private void rebaixarTitular(UUID petId) {
        petTutorRepository.findByPetPetIdAndRole(petId, PetTutorRole.HOLDER)
                .ifPresent(titular -> {
                    titular.setRole(PetTutorRole.EDITOR);
                    titular.setUpdateDate(LocalDateTime.now());
                    petTutorRepository.save(titular);
                });

        petTutorRepository.flush();
    }

    /**
     * Convidar quem ja cuida do pet nao e engano de digitacao a ser silenciado: a
     * chave unica recusaria o vinculo no aceite, e a pessoa levaria o erro no
     * lugar de quem convidou.
     */
    private void recusarSeJaETutor(UUID petId, String email) {
        ownerRepository.findByEmail(email.trim())
                .filter(owner -> petTutorRepository.existsByPetPetIdAndOwnerOwnerId(petId, owner.getOwnerId()))
                .ifPresent(owner -> {
                    throw jaETutor();
                });
    }

    private PetTutor buscarVinculo(UUID petId, UUID ownerId) {
        return petTutorRepository.findByPetPetIdAndOwnerOwnerId(petId, ownerId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private static PetfyHealthcareException conviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private static PetfyHealthcareException jaETutor() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ALREADY_A_TUTOR.getMessage(),
                ErrorMessageEnum.ALREADY_A_TUTOR.getCode(),
                HttpStatus.CONFLICT);
    }

    private static PetfyHealthcareException transferenciaExigida() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getMessage(),
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                HttpStatus.CONFLICT);
    }

    private PetTutorResponseDTO toResponse(PetTutor vinculo) {
        return PetTutorResponseDTO.builder()
                .petTutorId(vinculo.getPetTutorId())
                .petId(vinculo.getPet().getPetId())
                .ownerId(vinculo.getOwner().getOwnerId())
                .ownerName(vinculo.getOwner().getName())
                .ownerEmail(vinculo.getOwner().getEmail())
                .role(vinculo.getRole())
                .holder(vinculo.isHolder())
                .invitedByOwnerName(vinculo.getInvitedBy() != null ? vinculo.getInvitedBy().getName() : null)
                .creationDate(vinculo.getCreationDate())
                .build();
    }

    private PetTutorInviteResponseDTO toResponse(PetTutorInvite invite, String token) {
        return PetTutorInviteResponseDTO.builder()
                .petTutorInviteId(invite.getPetTutorInviteId())
                .petId(invite.getPet().getPetId())
                .petName(invite.getPet().getName())
                .token(token)
                .email(invite.getEmail())
                .role(invite.getRole())
                .createdByOwnerName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .transfersHolder(invite.transfereTitularidade())
                .build();
    }

}
