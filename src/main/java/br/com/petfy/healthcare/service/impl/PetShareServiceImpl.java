package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetShareResponseDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.PetShareRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.PetShareService;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetShareServiceImpl implements PetShareService {

    private final PetShareRepository petShareRepository;
    private final VaccineRepository vaccineRepository;
    private final PetAccessGuard petAccessGuard;
    private final VaccineStatusCalculator vaccineStatusCalculator;
    private final OpaqueTokenService opaqueTokenService;
    private final SensitiveAccessLogger sensitiveAccessLogger;

    @Value("${petfy.share.default-expiration-days:30}")
    private int defaultExpirationDays;

    /** Mesma janela da agenda, para o link e o app nao discordarem sobre o que e "vencendo". */
    @Value("${petfy.reminders.window-days:30}")
    private int windowDays;

    @Override
    public PetShareResponseDTO createShare(UUID petId, PetShareRequestDTO request) {
        Pet pet = petAccessGuard.requireEscrita(petId);

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        PetShare share = petShareRepository.save(PetShare.builder()
                .pet(pet)
                .tokenHash(opaqueTokenService.hash(token))
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(share, token);
    }

    @Override
    public List<PetShareResponseDTO> listShares(UUID petId) {
        Pet pet = petAccessGuard.requireEscrita(petId);

        return petShareRepository.findByPetOrderByCreationDateDesc(pet)
                .stream()
                .map(share -> toResponse(share, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revokeShare(UUID petShareId) {
        // basta alcancar o pet: quem cuida do pet pode cortar um link que corre
        // por fora, sem depender de quem o criou
        PetShare share = petShareRepository.findById(petShareId)
                .filter(s -> petAccessGuard.alcanca(s.getPet().getPetId()))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.SHARE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.SHARE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (share.getRevokedAt() == null) {
            share.setRevokedAt(LocalDateTime.now());
            petShareRepository.save(share);
        }
    }

    @Override
    public SharedVaccineCardDTO viewSharedCard(String token) {
        LocalDateTime agora = LocalDateTime.now();

        // token invalido, revogado e expirado respondem igual: distinguir diria a
        // quem tem um link velho que aquele pet existe
        PetShare share = petShareRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(s -> s.isActive(agora))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.SHARE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.SHARE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        Pet pet = share.getPet();
        LocalDate hoje = LocalDate.now();

        // Registra depois de o link ser validado, e nao antes: token invalido nao e
        // acesso ao pet, e logar tentativa de token inexistente enche a tabela de um
        // pet que ninguem conseguiu abrir - ou, pior, de um petId que nao existe.
        sensitiveAccessLogger.linkPublicoAberto(pet);

        List<SharedVaccineCardDTO.SharedVaccineDTO> vacinas =
                vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(pet.getPetId())
                        .stream()
                        .map(vaccine -> toSharedVaccine(vaccine, hoje))
                        .collect(Collectors.toList());

        return SharedVaccineCardDTO.builder()
                .petName(pet.getName())
                .petType(pet.getType())
                .petBreed(pet.getBreed())
                .petBornDate(pet.getBornDate())
                .petGender(pet.getGender())
                .ownerName(pet.getHolder().map(Owner::getName).orElse(null))
                .referenceDate(hoje)
                .expiresAt(share.getExpiresAt())
                .vaccines(vacinas)
                .build();
    }

    private SharedVaccineCardDTO.SharedVaccineDTO toSharedVaccine(Vaccine vaccine, LocalDate hoje) {
        return SharedVaccineCardDTO.SharedVaccineDTO.builder()
                .vaccineName(vaccine.getVaccineName())
                .applicationDate(vaccine.getApplicationDate())
                .nextDoseDate(vaccine.getNextDoseDate())
                .status(vaccineStatusCalculator.classify(vaccine.getNextDoseDate(), hoje, windowDays))
                .clinicName(vaccine.getClinic() != null ? vaccine.getClinic().getName() : null)
                .build();
    }

    private PetShareResponseDTO toResponse(PetShare share, String token) {
        return PetShareResponseDTO.builder()
                .petShareId(share.getPetShareId())
                .petId(share.getPet().getPetId())
                .token(token)
                .expiresAt(share.getExpiresAt())
                .revokedAt(share.getRevokedAt())
                .creationDate(share.getCreationDate())
                .active(share.isActive(LocalDateTime.now()))
                .build();
    }

}
