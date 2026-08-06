package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.ClinicActivityNotifier;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
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

    private final GrantRepository grantRepository;
    private final VaccineRepository vaccineRepository;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final VaccineFactory vaccineFactory;
    private final ClinicActivityNotifier clinicActivityNotifier;
    private final VaccineCorrectionLog vaccineCorrectionLog;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordCorrectionLog healthRecordCorrectionLog;
    private final SensitiveAccessLogger sensitiveAccessLogger;

    /**
     * Janela de correcao em dias. Curta de proposito: cobre o erro percebido
     * logo apos o atendimento, nao a reescrita de historico antigo.
     */
    @Value("${petfy.vet.correction-window-days:7}")
    private int correctionWindowDays;

    @Override
    public List<VetPetDTO> listAccessibleAnimals() {
        return acessosAtivosDaClinica()
                .stream()
                .map(this::toVetPet)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaccineResponseDTO> listVaccines(UUID animalId) {
        registrarLeitura(exigirAcessoAoAnimal(animalId).getAnimal(), AccessedResource.VACCINES);

        return vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VaccineResponseDTO registerVaccine(UUID animalId, VaccineRequestDTO request) {
        Person vet = currentProfessionalProvider.require();
        Animal animal = exigirAcessoAoAnimal(animalId).getAnimal();

        // a clinica vem do vet autenticado, nunca do payload: aceitar clinicId do
        // cliente deixaria um vet registrar vacina em nome de outra clinica
        Vaccine vaccine = vaccineRepository.save(vaccineFactory.build(animal, vet.getClinic(), request));

        // o tutor precisa saber o que a clinica escreveu no animal dele
        clinicActivityNotifier.vaccineRecorded(vaccine);

        return toResponse(vaccine);
    }

    /**
     * Correcao, e nao reescrita: o estado anterior fica gravado, so vale para
     * registro da propria clinica e so dentro de uma janela curta. Depois disso
     * o registro congela - corrigir um lancamento de meses atras nao e conserto
     * de digitacao, e o tutor e quem decide o que fica na carteira dele.
     */
    @Override
    public VaccineResponseDTO correctVaccine(UUID animalId, UUID vaccineId, VaccineRequestDTO request) {
        Person vet = currentProfessionalProvider.require();
        exigirAcessoAoAnimal(animalId);

        Vaccine vaccine = vaccineRepository.findById(vaccineId)
                .filter(v -> v.getAnimal().getAnimalId().equals(animalId))
                .filter(v -> registradaPelaClinica(v, vet))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        exigirJanelaAberta(vaccine.getCreationDate());

        // o snapshot precisa sair antes dos setters, senao grava o estado novo
        vaccineCorrectionLog.recordByProfessional(vaccine, vet, vet.getClinic());

        if (request.getVaccineName() != null) vaccine.setVaccineName(request.getVaccineName());
        if (request.getApplicationDate() != null) vaccine.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) vaccine.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) vaccine.setDescription(request.getDescription());
        vaccine.setUpdateDate(LocalDateTime.now());

        Vaccine salva = vaccineRepository.save(vaccine);

        // o tutor precisa saber que a clinica mexeu no que ja estava la
        clinicActivityNotifier.vaccineCorrected(salva);

        return toResponse(salva);
    }

    /**
     * Rastro de qualquer vacina do animal, e nao so das registradas pela propria
     * clinica: o vet ja enxerga a carteira inteira em listVaccines, e saber que
     * um registro foi alterado faz parte de ler aquele registro.
     */
    @Override
    public List<VaccineCorrectionResponseDTO> listCorrections(UUID animalId, UUID vaccineId) {
        registrarLeitura(exigirAcessoAoAnimal(animalId).getAnimal(), AccessedResource.VACCINE_CORRECTIONS);

        vaccineRepository.findById(vaccineId)
                .filter(v -> v.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        return vaccineCorrectionLog.list(vaccineId);
    }

    @Override
    public List<HealthRecordResponseDTO> listHealthRecords(UUID animalId) {
        registrarLeitura(exigirAcessoAoAnimal(animalId).getAnimal(), AccessedResource.HEALTH_RECORDS);

        return healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponseDTO registerHealthRecord(UUID animalId, HealthRecordRequestDTO request) {
        Person vet = currentProfessionalProvider.require();
        Animal animal = exigirAcessoAoAnimal(animalId).getAnimal();

        // mesma regra da vacina: a clinica vem do vet autenticado, nunca do
        // payload, e o animalId vem do path
        HealthRecord record = healthRecordRepository.save(HealthRecord.builder()
                .animal(animal)
                .clinic(vet.getClinic())
                .eventType(request.getEventType())
                .category(request.getCategory())
                .diagnosis(request.getDiagnosis())
                .eventDate(request.getEventDate())
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .build());

        clinicActivityNotifier.healthRecordRecorded(record);

        return toResponse(record);
    }

    /** Mesmos limites da correcao de vacina: propria clinica, dentro da janela, com rastro. */
    @Override
    public HealthRecordResponseDTO correctHealthRecord(UUID animalId, UUID healthRecordId,
                                                       HealthRecordRequestDTO request) {
        Person vet = currentProfessionalProvider.require();
        exigirAcessoAoAnimal(animalId);

        HealthRecord record = healthRecordRepository.findById(healthRecordId)
                .filter(r -> r.getAnimal().getAnimalId().equals(animalId))
                .filter(r -> r.getClinic() != null
                        && vet.getClinic().getClinicId().equals(r.getClinic().getClinicId()))
                .orElseThrow(this::registroNaoEncontrado);

        exigirJanelaAberta(record.getCreationDate());

        // o snapshot precisa sair antes dos setters, senao grava o estado novo
        healthRecordCorrectionLog.recordByProfessional(record, vet, vet.getClinic());

        if (request.getEventType() != null) record.setEventType(request.getEventType());
        if (request.getCategory() != null) record.setCategory(request.getCategory());
        if (request.getDiagnosis() != null) record.setDiagnosis(request.getDiagnosis());
        if (request.getEventDate() != null) record.setEventDate(request.getEventDate());
        if (request.getDescription() != null) record.setDescription(request.getDescription());
        record.setUpdateDate(LocalDateTime.now());

        HealthRecord salvo = healthRecordRepository.save(record);

        clinicActivityNotifier.healthRecordCorrected(salvo);

        return toResponse(salvo);
    }

    @Override
    public List<HealthRecordCorrectionResponseDTO> listHealthRecordCorrections(UUID animalId, UUID healthRecordId) {
        registrarLeitura(exigirAcessoAoAnimal(animalId).getAnimal(), AccessedResource.HEALTH_RECORD_CORRECTIONS);

        healthRecordRepository.findById(healthRecordId)
                .filter(r -> r.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(this::registroNaoEncontrado);

        return healthRecordCorrectionLog.list(healthRecordId);
    }

    private PetfyHealthcareException registroNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getMessage(),
                ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private HealthRecordResponseDTO toResponse(HealthRecord record) {
        return HealthRecordResponseDTO.builder()
                .healthRecordId(record.getHealthRecordId())
                .eventType(record.getEventType())
                .category(record.getCategory())
                .diagnosis(record.getDiagnosis())
                .eventDate(record.getEventDate())
                .description(record.getDescription())
                .animalId(record.getAnimal().getAnimalId())
                .clinicId(record.getClinic() != null ? record.getClinic().getClinicId() : null)
                .creationDate(record.getCreationDate())
                .updateDate(record.getUpdateDate())
                .build();
    }

    /**
     * Clinica que registrou. Vacina lancada pelo proprio tutor nao e da clinica
     * corrigir.
     *
     * A comparacao parte do id do vet, que sempre existe, e nao do id da vacina:
     * assim clinica sem id nao vira NullPointerException no meio de uma checagem
     * de permissao.
     */
    private boolean registradaPelaClinica(Vaccine vaccine, Person vet) {
        return vaccine.getClinic() != null
                && vet.getClinic().getClinicId().equals(vaccine.getClinic().getClinicId());
    }

    private void exigirJanelaAberta(LocalDateTime registro) {
        if (registro == null || registro.isBefore(LocalDateTime.now().minusDays(correctionWindowDays))) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getMessage(),
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private List<Grant> acessosAtivosDaClinica() {
        UUID clinicId = currentProfessionalProvider.require().getClinic().getClinicId();
        return grantRepository.findVigentesDaClinica(clinicId, LocalDateTime.now());
    }

    /**
     * Animal sem concessao ativa para a clinica do vet responde ANIMAL_NOT_FOUND, e nao
     * 403: para o veterinario, um animal que a clinica dele nao atende e
     * indistinguivel de um animal que nao existe.
     */
    /**
     * Registra que este veterinario leu o recurso.
     *
     * Vale so para leitura. Escrita - registrar e corrigir - ja deixa rastro proprio
     * em {@code vaccine_corrections} e {@code health_record_corrections}, e avisa o
     * tutor por e-mail na hora. Era a leitura que passava invisivel.
     */
    private void registrarLeitura(Animal animal, AccessedResource recurso) {
        sensitiveAccessLogger.vetLeu(currentProfessionalProvider.require(), animal, recurso);
    }

    private Grant exigirAcessoAoAnimal(UUID animalId) {
        UUID clinicId = currentProfessionalProvider.require().getClinic().getClinicId();

        return grantRepository.findVigenteDaClinicaNoAnimal(animalId, clinicId, LocalDateTime.now())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VetPetDTO toVetPet(Grant access) {
        Animal animal = access.getAnimal();

        return VetPetDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .type(animal.getType())
                .breed(animal.getBreed())
                .bornDate(animal.getBornDate())
                .gender(animal.getGender())
                .weight(animal.getWeight())
                .personName(animal.getHolder().map(Person::getName).orElse(null))
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
                .animalId(vaccine.getAnimal().getAnimalId())
                .clinicId(vaccine.getClinic() != null ? vaccine.getClinic().getClinicId() : null)
                .vaccineCatalogId(vaccine.getCatalog() != null ? vaccine.getCatalog().getVaccineCatalogId() : null)
                .creationDate(vaccine.getCreationDate())
                .updateDate(vaccine.getUpdateDate())
                .build();
    }

}
