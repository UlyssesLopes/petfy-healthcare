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
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.OrganizationActivityNotifier;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.security.ProfessionalContext;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineFactory;
import br.com.petfy.healthcare.service.VetPetService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetServiceImpl implements VetPetService {

    private final GrantRepository grantRepository;
    private final CustodyRepository custodyRepository;
    private final VaccineRepository vaccineRepository;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final VaccineFactory vaccineFactory;
    private final OrganizationActivityNotifier organizationActivityNotifier;
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
    public Page<VetPetDTO> listAccessibleAnimals(String busca, Pageable pageable) {
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        LocalDateTime agora = LocalDateTime.now();
        String termo = termoDeBusca(busca);
        UUID organizationId = contexto.atuaPorOrganizacao()
                ? contexto.organization().getOrganizationId()
                : null;

        // o autonomo cai no ramo da pessoa, e e por isso que a area de organizacao
        // nao exige organizacao nenhuma para funcionar (PRODUTO 9.3)
        Page<Grant> pagina = organizationId != null
                ? grantRepository.buscarVigentesDaClinica(organizationId, agora, termo, pageable)
                : grantRepository.buscarVigentesDaPessoa(contexto.person().getPersonId(), agora, termo, pageable);

        return pagina.map(this::toVetPet);
    }

    /**
     * A lista do abrigo: quem a organizacao RESPONDE, e nao quem ela alcanca.
     *
     * <b>Exige organizacao declarada, e o erro dele nao e de permissao.</b> Um voluntario que
     * atua por dois abrigos precisa dizer por qual esta agindo — responder pela lista errada
     * seria pior que recusar. O `ORGANIZATION_CONTEXT_REQUIRED` (138) ja diz exatamente isso, e a tela oferece a
     * escolha em vez de parecer porta fechada (PRODUTO 9.5).
     *
     * O `accessGrantedAt` do DTO carrega aqui o INICIO DA CUSTODIA — e o "desde" que a Tela 12
     * mostra. Reusar o campo e deliberado: os dois respondem "desde quando este animal esta com
     * a gente", e criar um segundo campo para a mesma pergunta deixaria a tela escolhendo qual
     * ler.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VetPetDTO> listAnimalsInCustody(String busca, Pageable pageable) {
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();

        if (!contexto.atuaPorOrganizacao()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                    ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                    HttpStatus.CONFLICT);
        }

        return custodyRepository
                .buscarEmCursoDaOrganizacao(contexto.organization().getOrganizationId(),
                        termoDeBusca(busca), pageable)
                .map(this::toVetPet);
    }

    /**
     * Busca em branco vira {@code %}, que casa com tudo.
     *
     * O ramo condicional fica aqui e nao no JPQL de proposito: {@code :busca is null}
     * na consulta faz o Postgres reclamar que nao consegue inferir o tipo do
     * parametro, e a alternativa seria duas consultas quase iguais. Assim o SQL e um
     * so, e a regra de "sem busca lista tudo" mora em um lugar onde da para testar.
     */
    private String termoDeBusca(String busca) {
        return busca == null || busca.isBlank() ? "%" : "%" + busca.trim().toLowerCase() + "%";
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
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        Person vet = contexto.person();
        exigirCapacidadeClinica(contexto);
        Animal animal = exigirAcessoAoAnimal(animalId).getAnimal();

        // a clinica vem do vet autenticado, nunca do payload: aceitar organizationId do
        // cliente deixaria um vet registrar vacina em nome de outra clinica
        Vaccine vaccine = vaccineRepository.save(vaccineFactory.build(animal, contexto.organization(), vet, request));

        // o tutor precisa saber o que a clinica escreveu no animal dele
        organizationActivityNotifier.vaccineRecorded(vaccine);

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
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        Person vet = contexto.person();
        exigirCapacidadeClinica(contexto);
        exigirAcessoAoAnimal(animalId);

        Vaccine vaccine = vaccineRepository.findById(vaccineId)
                .filter(v -> v.getAnimal().getAnimalId().equals(animalId))
                .filter(v -> registradaPeloContexto(v, contexto))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        exigirJanelaAberta(vaccine.getCreationDate());

        // o snapshot precisa sair antes dos setters, senao grava o estado novo
        vaccineCorrectionLog.recordByProfessional(vaccine, vet, contexto.organization());

        if (request.getVaccineName() != null) vaccine.setVaccineName(request.getVaccineName());
        if (request.getApplicationDate() != null) vaccine.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) vaccine.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) vaccine.setDescription(request.getDescription());
        vaccine.setUpdateDate(LocalDateTime.now());

        Vaccine salva = vaccineRepository.save(vaccine);

        // o tutor precisa saber que a clinica mexeu no que ja estava la
        organizationActivityNotifier.vaccineCorrected(salva);

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
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        Person vet = contexto.person();
        exigirCapacidadeClinica(contexto);
        Animal animal = exigirAcessoAoAnimal(animalId).getAnimal();

        // mesma regra da vacina: a clinica vem do vet autenticado, nunca do
        // payload, e o animalId vem do path
        HealthRecord record = healthRecordRepository.save(HealthRecord.builder()
                .animal(animal)
                .organization(contexto.organization())
                .recordedBy(vet)
                .eventType(request.getEventType())
                .category(request.getCategory())
                .diagnosis(request.getDiagnosis())
                .eventDate(request.getEventDate())
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .build());

        organizationActivityNotifier.healthRecordRecorded(record);

        return toResponse(record);
    }

    /** Mesmos limites da correcao de vacina: propria clinica, dentro da janela, com rastro. */
    @Override
    public HealthRecordResponseDTO correctHealthRecord(UUID animalId, UUID healthRecordId,
                                                       HealthRecordRequestDTO request) {
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        Person vet = contexto.person();
        exigirCapacidadeClinica(contexto);
        exigirAcessoAoAnimal(animalId);

        HealthRecord record = healthRecordRepository.findById(healthRecordId)
                .filter(r -> r.getAnimal().getAnimalId().equals(animalId))
                .filter(r -> r.getOrganization() != null
                        && contexto.atuaPorOrganizacao()
                        && contexto.organization().getOrganizationId().equals(r.getOrganization().getOrganizationId()))
                .orElseThrow(this::registroNaoEncontrado);

        exigirJanelaAberta(record.getCreationDate());

        // o snapshot precisa sair antes dos setters, senao grava o estado novo
        healthRecordCorrectionLog.recordByProfessional(record, vet, contexto.organization());

        if (request.getEventType() != null) record.setEventType(request.getEventType());
        if (request.getCategory() != null) record.setCategory(request.getCategory());
        if (request.getDiagnosis() != null) record.setDiagnosis(request.getDiagnosis());
        if (request.getEventDate() != null) record.setEventDate(request.getEventDate());
        if (request.getDescription() != null) record.setDescription(request.getDescription());
        record.setUpdateDate(LocalDateTime.now());

        HealthRecord salvo = healthRecordRepository.save(record);

        organizationActivityNotifier.healthRecordCorrected(salvo);

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
                .organizationId(record.getOrganization() != null ? record.getOrganization().getOrganizationId() : null)
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
    private boolean registradaPeloContexto(Vaccine vaccine, ProfessionalContext contexto) {
        // contexto sem organizacao so corrige o que ele mesmo registrou sem organizacao,
        // e vacina lancada pelo tutor nao tem nem uma nem outra - continua fora do
        // alcance da correcao profissional, que era a regra desde o comeco
        if (!contexto.atuaPorOrganizacao()) {
            return false;
        }

        return vaccine.getOrganization() != null
                && contexto.organization().getOrganizationId()
                        .equals(vaccine.getOrganization().getOrganizationId());
    }

    /**
     * Ato clinico exige que a organizacao possa registra-lo.
     *
     * <b>E aqui que a capacidade deixa de ser decorativa.</b> Antes bastava ser vet de
     * uma clinica, e "clinica" carregava implicitamente o direito de escrever no
     * prontuario. Com creche e abrigo na mesma tabela, o implicito viraria buraco: um
     * monitor de creche com credencial - existe, e nao e raro - escreveria diagnostico
     * no historico medico do animal, e o valor clinico do registro morre.
     *
     * Contexto sem organizacao passa: quem atua por si e limitado pela propria
     * credencial, que o ProfessionalAccessManager ja conferiu.
     */
    private void exigirCapacidadeClinica(ProfessionalContext contexto) {
        if (!contexto.permite(OrganizationCapability.REGISTRAR_ATO_CLINICO)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getMessage(),
                    ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void exigirJanelaAberta(LocalDateTime registro) {
        if (registro == null || registro.isBefore(LocalDateTime.now().minusDays(correctionWindowDays))) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getMessage(),
                    ErrorMessageEnum.CORRECTION_WINDOW_EXPIRED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Os animais que este contexto alcanca.
     *
     * <b>Duas consultas, e a segunda e o veterinario autonomo.</b> Quando a pessoa atua
     * por uma organizacao, o tutor autorizou a organizacao - quem atende hoje pode nao
     * ser quem atende no retorno. Quando ela atua por si, o tutor autorizou a pessoa. O
     * modelo antigo so sabia da primeira, e era por isso que atendimento domiciliar
     * obrigava a inventar uma clinica.
     */
    private List<Grant> acessosAtivosDoContexto() {
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        LocalDateTime agora = LocalDateTime.now();

        return contexto.atuaPorOrganizacao()
                ? grantRepository.findVigentesDaClinica(contexto.organization().getOrganizationId(), agora)
                : grantRepository.findVigentesDaPessoa(contexto.person().getPersonId(), agora);
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
        sensitiveAccessLogger.vetLeu(currentProfessionalProvider.requireContext(), animal, recurso);
    }

    private Grant exigirAcessoAoAnimal(UUID animalId) {
        ProfessionalContext contexto = currentProfessionalProvider.requireContext();
        LocalDateTime agora = LocalDateTime.now();

        return (contexto.atuaPorOrganizacao()
                ? grantRepository.findVigenteDaClinicaNoAnimal(
                        animalId, contexto.organization().getOrganizationId(), agora)
                : grantRepository.findVigenteDaPessoaNoAnimal(
                        animalId, contexto.person().getPersonId(), agora))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    /**
     * O animal sob custodia da organizacao, sem tutor humano atras.
     *
     * O `personName` fica nulo <b>e isso e a informacao</b>: "sem tutor humano desde o resgate",
     * como a Tela 13 escreve. Preencher com o nome do abrigo faria a coluna "tutor" mentir —
     * quem responde e uma organizacao, e a tela precisa poder dizer isso.
     */
    private VetPetDTO toVetPet(Custody custodia) {
        Animal animal = custodia.getAnimal();

        return VetPetDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .type(animal.getType())
                .breed(animal.getBreed())
                .bornDate(animal.getBornDate())
                .gender(animal.getGender())
                .weight(animal.getWeight())
                .accessGrantedAt(custodia.getStartedAt())
                .build();
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
                .organizationId(vaccine.getOrganization() != null ? vaccine.getOrganization().getOrganizationId() : null)
                .vaccineCatalogId(vaccine.getCatalog() != null ? vaccine.getCatalog().getVaccineCatalogId() : null)
                .creationDate(vaccine.getCreationDate())
                .updateDate(vaccine.getUpdateDate())
                .build();
    }

}
