package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalShareResponseDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalShareRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.AnimalShareService;
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
public class AnimalShareServiceImpl implements AnimalShareService {

    private final AnimalShareRepository animalShareRepository;
    private final VaccineRepository vaccineRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final VaccineStatusCalculator vaccineStatusCalculator;
    private final OpaqueTokenService opaqueTokenService;
    private final SensitiveAccessLogger sensitiveAccessLogger;

    @Value("${petfy.share.default-expiration-days:30}")
    private int defaultExpirationDays;

    /** Mesma janela da agenda, para o link e o app nao discordarem sobre o que e "vencendo". */
    @Value("${petfy.reminders.window-days:30}")
    private int windowDays;

    @Override
    public AnimalShareResponseDTO createShare(UUID animalId, AnimalShareRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        AnimalShare share = animalShareRepository.save(AnimalShare.builder()
                .animal(animal)
                .tokenHash(opaqueTokenService.hash(token))
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(share, token);
    }

    @Override
    public List<AnimalShareResponseDTO> listShares(UUID animalId) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        return animalShareRepository.findByAnimalOrderByCreationDateDesc(animal)
                .stream()
                .map(share -> toResponse(share, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revokeShare(UUID animalShareId) {
        // basta alcancar o animal: quem cuida do animal pode cortar um link que corre
        // por fora, sem depender de quem o criou
        AnimalShare share = animalShareRepository.findById(animalShareId)
                .filter(s -> animalAccessGuard.alcanca(s.getAnimal().getAnimalId()))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.SHARE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.SHARE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (share.getRevokedAt() == null) {
            share.setRevokedAt(LocalDateTime.now());
            animalShareRepository.save(share);
        }
    }

    @Override
    public SharedVaccineCardDTO viewSharedCard(String token) {
        LocalDateTime agora = LocalDateTime.now();

        // token invalido, revogado e expirado respondem igual: distinguir diria a
        // quem tem um link velho que aquele animal existe
        AnimalShare share = animalShareRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(s -> s.isActive(agora))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.SHARE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.SHARE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        Animal animal = share.getAnimal();
        LocalDate hoje = LocalDate.now();

        // Registra depois de o link ser validado, e nao antes: token invalido nao e
        // acesso ao animal, e logar tentativa de token inexistente enche a tabela de um
        // animal que ninguem conseguiu abrir - ou, pior, de um animalId que nao existe.
        sensitiveAccessLogger.linkPublicoAberto(animal);

        List<SharedVaccineCardDTO.SharedVaccineDTO> vacinas =
                vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animal.getAnimalId())
                        .stream()
                        .map(vaccine -> toSharedVaccine(vaccine, hoje))
                        .collect(Collectors.toList());

        return SharedVaccineCardDTO.builder()
                .animalName(animal.getName())
                .animalType(animal.getType())
                .animalBreed(animal.getBreed())
                .animalBornDate(animal.getBornDate())
                .animalGender(animal.getGender())
                .personName(animal.getHolder().map(Person::getName).orElse(null))
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

    private AnimalShareResponseDTO toResponse(AnimalShare share, String token) {
        return AnimalShareResponseDTO.builder()
                .animalShareId(share.getAnimalShareId())
                .animalId(share.getAnimal().getAnimalId())
                .token(token)
                .expiresAt(share.getExpiresAt())
                .revokedAt(share.getRevokedAt())
                .creationDate(share.getCreationDate())
                .active(share.isActive(LocalDateTime.now()))
                .build();
    }

}
