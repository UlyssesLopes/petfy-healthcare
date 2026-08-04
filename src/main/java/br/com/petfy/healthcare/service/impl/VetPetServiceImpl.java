package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.VaccineRecordedNotifier;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.VaccineFactory;
import br.com.petfy.healthcare.service.VetPetService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetServiceImpl implements VetPetService {

    private final PetClinicAccessRepository petClinicAccessRepository;
    private final VaccineRepository vaccineRepository;
    private final CurrentVetProvider currentVetProvider;
    private final VaccineFactory vaccineFactory;
    private final VaccineRecordedNotifier vaccineRecordedNotifier;
    private final VaccineCorrectionLog vaccineCorrectionLog;

    /**
     * Janela de correcao em dias. Curta de proposito: cobre o erro percebido
     * logo apos o atendimento, nao a reescrita de historico antigo.
     */
    @Value("${petfy.vet.correction-window-days:7}")
    private int correctionWindowDays;

    @Override
    public List<VetPetDTO> listAccessiblePets() {
        return acessosAtivosDaClinica()
                .stream()
                .map(this::toVetPet)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaccineResponseDTO> listVaccines(UUID petId) {
        exigirAcessoAoPet(petId);

        return vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VaccineResponseDTO registerVaccine(UUID petId, VaccineRequestDTO request) {
        Vet vet = currentVetProvider.require();
        Pet pet = exigirAcessoAoPet(petId).getPet();

        // a clinica vem do vet autenticado, nunca do payload: aceitar clinicId do
        // cliente deixaria um vet registrar vacina em nome de outra clinica
        Vaccine vaccine = vaccineRepository.save(vaccineFactory.build(pet, vet.getClinic(), request));

        // o tutor precisa saber o que a clinica escreveu no pet dele
        vaccineRecordedNotifier.notifyOwner(vaccine);

        return toResponse(vaccine);
    }

    /**
     * Correcao, e nao reescrita: o estado anterior fica gravado, so vale para
     * registro da propria clinica e so dentro de uma janela curta. Depois disso
     * o registro congela - corrigir um lancamento de meses atras nao e conserto
     * de digitacao, e o tutor e quem decide o que fica na carteira dele.
     */
    @Override
    public VaccineResponseDTO correctVaccine(UUID petId, UUID vaccineId, VaccineRequestDTO request) {
        Vet vet = currentVetProvider.require();
        exigirAcessoAoPet(petId);

        Vaccine vaccine = vaccineRepository.findById(vaccineId)
                .filter(v -> v.getPet().getPetId().equals(petId))
                .filter(v -> registradaPelaClinica(v, vet))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        exigirJanelaAberta(vaccine);

        // o snapshot precisa sair antes dos setters, senao grava o estado novo
        vaccineCorrectionLog.recordByVet(vaccine, vet);

        if (request.getVaccineName() != null) vaccine.setVaccineName(request.getVaccineName());
        if (request.getApplicationDate() != null) vaccine.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) vaccine.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) vaccine.setDescription(request.getDescription());
        vaccine.setUpdateDate(LocalDateTime.now());

        Vaccine salva = vaccineRepository.save(vaccine);

        // o tutor precisa saber que a clinica mexeu no que ja estava la
        vaccineRecordedNotifier.notifyCorrection(salva);

        return toResponse(salva);
    }

    /**
     * Rastro de qualquer vacina do pet, e nao so das registradas pela propria
     * clinica: o vet ja enxerga a carteira inteira em listVaccines, e saber que
     * um registro foi alterado faz parte de ler aquele registro.
     */
    @Override
    public List<VaccineCorrectionResponseDTO> listCorrections(UUID petId, UUID vaccineId) {
        exigirAcessoAoPet(petId);

        vaccineRepository.findById(vaccineId)
                .filter(v -> v.getPet().getPetId().equals(petId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        return vaccineCorrectionLog.list(vaccineId);
    }

    /**
     * Clinica que registrou. Vacina lancada pelo proprio tutor nao e da clinica
     * corrigir.
     *
     * A comparacao parte do id do vet, que sempre existe, e nao do id da vacina:
     * assim clinica sem id nao vira NullPointerException no meio de uma checagem
     * de permissao.
     */
    private boolean registradaPelaClinica(Vaccine vaccine, Vet vet) {
        return vaccine.getClinic() != null
                && vet.getClinic().getClinicId().equals(vaccine.getClinic().getClinicId());
    }

    private void exigirJanelaAberta(Vaccine vaccine) {
        LocalDateTime registro = vaccine.getCreationDate();

        if (registro == null || registro.isBefore(LocalDateTime.now().minusDays(correctionWindowDays))) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getMessage(),
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private List<PetClinicAccess> acessosAtivosDaClinica() {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();
        return petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(clinicId);
    }

    /**
     * Pet sem concessao ativa para a clinica do vet responde PET_NOT_FOUND, e nao
     * 403: para o veterinario, um pet que a clinica dele nao atende e
     * indistinguivel de um pet que nao existe.
     */
    private PetClinicAccess exigirAcessoAoPet(UUID petId) {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();

        return petClinicAccessRepository.findByPetPetIdAndClinicClinicId(petId, clinicId)
                .filter(PetClinicAccess::isActive)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VetPetDTO toVetPet(PetClinicAccess access) {
        Pet pet = access.getPet();

        return VetPetDTO.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .bornDate(pet.getBornDate())
                .gender(pet.getGender())
                .weight(pet.getWeight())
                .ownerName(pet.getOwner().getName())
                .accessGrantedAt(access.getGrantedAt())
                .build();
    }

    private VaccineResponseDTO toResponse(Vaccine vaccine) {
        return VaccineResponseDTO.builder()
                .vaccineId(vaccine.getVaccineId())
                .vaccineName(vaccine.getVaccineName())
                .applicationDate(vaccine.getApplicationDate())
                .nextDoseDate(vaccine.getNextDoseDate())
                .description(vaccine.getDescription())
                .petId(vaccine.getPet().getPetId())
                .clinicId(vaccine.getClinic() != null ? vaccine.getClinic().getClinicId() : null)
                .vaccineCatalogId(vaccine.getCatalog() != null ? vaccine.getCatalog().getVaccineCatalogId() : null)
                .creationDate(vaccine.getCreationDate())
                .updateDate(vaccine.getUpdateDate())
                .build();
    }

}
