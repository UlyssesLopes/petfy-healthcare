package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AttendanceRequestDTO;
import br.com.petfy.healthcare.domain.dto.AttendanceResponseDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupResponseDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentAgreementRequestDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthProofItemDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.Attendance;
import br.com.petfy.healthcare.domain.entity.AttendanceStatus;
import br.com.petfy.healthcare.domain.entity.ClassGroup;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.EnrollmentStatus;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.OrganizationVaccineRequirement;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AttendanceRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.ClassGroupRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationVaccineRequirementRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.CrecheService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A operacao da creche.
 *
 * <b>A regra central esta em {@link #comprovar}, e ela e a tese da Tela 10:</b> "o produto responde
 * pela saude". A creche nao decide se o animal esta apto e o tutor nao precisa provar nada — o
 * servidor cruza o que a organizacao exige com o que a carteira tem, e devolve o resultado linha por
 * linha. Deixar isso para a tela faria cada tela reimplementar a regra, e a primeira que errasse
 * deixaria entrar um animal com antirrabica vencida.
 *
 * <b>A aptidao e sempre CALCULADA, e nunca guardada.</b> Uma dose vence sozinha e outra e registrada
 * por uma clinica do outro lado da cidade: um campo `apto` no banco estaria errado sem ninguem ter
 * feito nada. E o que sustenta a frase "a matricula se completa sozinha assim que a dose for
 * registrada" — algo que se completa sozinho nao pode depender de alguem lembrar de recalcular.
 */
@Service
@RequiredArgsConstructor
public class CrecheServiceImpl implements CrecheService {

    private final ClassGroupRepository classGroupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AnimalCostRepository animalCostRepository;
    private final OrganizationVaccineRequirementRepository requirementRepository;
    private final VaccineCatalogRepository vaccineCatalogRepository;
    private final VaccineRepository vaccineRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final CurrentPersonProvider currentPersonProvider;
    private final GrantRepository grantRepository;
    private final AnimalAccessGuard animalAccessGuard;

    /* ------------------------------------------------------------------------------ turma */

    @Override
    @Transactional(readOnly = true)
    public List<ClassGroupResponseDTO> listClassGroups() {
        UUID organizationId = organizacaoQueGereTurma().getOrganizationId();

        return classGroupRepository.findVigentesDaOrganizacao(organizationId).stream()
                .map(turma -> ClassGroupResponseDTO.builder()
                        .classGroupId(turma.getClassGroupId())
                        .name(turma.getName())
                        .capacity(turma.getCapacity())
                        .occupied(enrollmentRepository.contarVivasDaTurma(turma.getClassGroupId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ClassGroupResponseDTO createClassGroup(ClassGroupRequestDTO request) {
        Organization organizacao = organizacaoQueGereTurma();

        ClassGroup turma = classGroupRepository.save(ClassGroup.builder()
                .organization(organizacao)
                .name(request.getName().trim())
                .capacity(request.getCapacity())
                .creationDate(LocalDateTime.now())
                .build());

        return ClassGroupResponseDTO.builder()
                .classGroupId(turma.getClassGroupId())
                .name(turma.getName())
                .capacity(turma.getCapacity())
                .occupied(0)
                .build();
    }

    /* ------------------------------------------------------------------------- exigencias */

    @Override
    @Transactional(readOnly = true)
    public List<VaccineRequirementResponseDTO> listRequirements() {
        return requirementRepository
                .findDaOrganizacao(organizacaoQueGereTurma().getOrganizationId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VaccineRequirementResponseDTO addRequirement(VaccineRequirementRequestDTO request) {
        Organization organizacao = organizacaoQueGereTurma();

        VaccineCatalog item = vaccineCatalogRepository.findById(request.getVaccineCatalogId())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        return toResponse(requirementRepository.save(OrganizationVaccineRequirement.builder()
                .organization(organizacao)
                .vaccineCatalog(item)
                .creationDate(LocalDateTime.now())
                .build()));
    }

    @Override
    @Transactional
    public void removeRequirement(UUID requirementId) {
        UUID organizationId = organizacaoQueGereTurma().getOrganizationId();

        OrganizationVaccineRequirement exigencia = requirementRepository.findById(requirementId)
                .filter(r -> r.getOrganization().getOrganizationId().equals(organizationId))
                .orElseThrow(this::naoEncontrado);

        requirementRepository.delete(exigencia);
    }

    /* ---------------------------------------------------------------------------- matricula */

    @Override
    @Transactional
    public EnrollmentResponseDTO enroll(UUID animalId, UUID classGroupId) {
        Organization organizacao = organizacaoQueGereTurma();

        /*
         * A CRECHE PRECISA DE ACESSO CONCEDIDO, e nao de custodia.
         *
         * A Tela 10 escreve a origem do alcance na propria tela: "Marcelo Dias concedeu acesso a
         * saude do Code em 04/08/2026". Matricular sem concessao seria a creche entrar num animal
         * que ninguem lhe apresentou — e `requireLeitura` e o nivel certo, porque matricular nao
         * escreve na saude de ninguem.
         */
        Animal animal = animalAlcancadoPelaOrganizacao(animalId, organizacao);

        ClassGroup turma = classGroupRepository
                .findDaOrganizacao(classGroupId, organizacao.getOrganizationId())
                .orElseThrow(this::naoEncontrado);

        // rematricular quem ja esta na turma e no-op: o estado final pedido e o estado atual
        Optional<Enrollment> viva = enrollmentRepository
                .findVivaDoAnimalNaTurma(animalId, classGroupId);

        if (viva.isPresent()) {
            return toResponse(viva.get(), comprovar(animal, organizacao), true);
        }

        exigirVagaLivre(turma);

        List<HealthProofItemDTO> comprovacao = comprovar(animal, organizacao);

        Enrollment matricula = enrollmentRepository.save(Enrollment.builder()
                .animal(animal)
                .classGroup(turma)
                .status(impede(comprovacao) ? EnrollmentStatus.PENDENTE : EnrollmentStatus.ATIVA)
                .requestedAt(LocalDateTime.now())
                .activatedAt(impede(comprovacao) ? null : LocalDateTime.now())
                .createdBy(currentPersonProvider.require())
                .build());

        return toResponse(matricula, comprovacao, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> listEnrollments(UUID classGroupId) {
        Organization organizacao = organizacaoQueGereTurma();

        classGroupRepository.findDaOrganizacao(classGroupId, organizacao.getOrganizationId())
                .orElseThrow(this::naoEncontrado);

        return enrollmentRepository.findVivasDaTurma(classGroupId).stream()
                // a creche LE o que ela mesma combinou: precisa, para conferir e corrigir
                .map(matricula -> toResponse(matricula, comprovar(matricula.getAnimal(), organizacao), true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> listEnrollmentsOfAnimal(UUID animalId) {
        // do lado do tutor: quem le e quem alcanca o animal, e nao um membro de creche
        animalAccessGuard.requireLeitura(animalId);

        /*
         * O COMBINADO SO VAI PARA QUEM RESPONDE PELO ANIMAL, e a leitura da matricula nao.
         *
         * Esta rota e alcancada por concessao — e a Tela 10 vista por quem recebeu acesso. O
         * petshop com acesso a carteira do Code precisa ver que ele tem matricula na creche; nao
         * precisa, e nao pode, ver quanto o Marcelo paga por ela. "Nenhum escopo de acesso concede
         * preco junto com saude" — a regra do custo vale no unico outro lugar em que ha dinheiro.
         *
         * Uma pergunta a mais por chamada, e nao por matricula: o animal e o mesmo nas tres linhas.
         */
        boolean mostrarCombinado = animalAccessGuard.respondePor(animalId);

        return enrollmentRepository.findVivasDoAnimal(animalId).stream()
                .map(matricula -> toResponse(matricula,
                        comprovar(matricula.getAnimal(), matricula.getClassGroup().getOrganization()),
                        mostrarCombinado))
                .toList();
    }

    /**
     * "Combinado com o tutor" — a caixa da Tela 41.
     *
     * <b>Quem grava e a creche que gere a turma</b>, e nao quem responde pelo animal. E a mesma
     * assimetria do custo: escreve quem registra, le so quem responde. A creche e quem sabe o que
     * combinou; exigir que o tutor digitasse o proprio boleto e o oposto de "o custo do animal so
     * existe se o dado entrar sem esforco".
     *
     * <b>Substitui o combinado inteiro, campo por campo, inclusive com nulo.</b> Quem renegocia diz
     * de novo o que passou a valer — e uma mensalidade que sobrasse de um combinado antigo iria
     * para a conta do tutor sem ninguem ter dito nada.
     */
    @Override
    @Transactional
    public EnrollmentResponseDTO setAgreement(UUID enrollmentId, EnrollmentAgreementRequestDTO request) {
        Enrollment matricula = matriculaDaMinhaOrganizacao(enrollmentId);

        matricula.setMonthlyFee(request.getMonthlyFee());
        matricula.setDueDay(request.getDueDay());
        matricula.setDailyRate(request.getDailyRate());

        /*
         * A COLECAO E ESVAZIADA E PREENCHIDA, e nao trocada por outra.
         *
         * Trocar a instancia de uma colecao gerenciada e o gesto que o Hibernate persegue com
         * "a collection with cascade=all-delete-orphan was no longer referenced" — e o erro nao
         * aparece aqui, mas no flush, longe deste metodo.
         */
        Set<DayOfWeek> dias = diasCombinados(request.getWeekdays());

        if (matricula.getWeekdays() == null) {
            matricula.setWeekdays(new LinkedHashSet<>());
        }

        matricula.getWeekdays().clear();
        matricula.getWeekdays().addAll(dias);

        Enrollment salva = enrollmentRepository.save(matricula);

        return toResponse(salva,
                comprovar(salva.getAnimal(), salva.getClassGroup().getOrganization()),
                true);
    }

    /**
     * Os nomes de dia que o cliente mandou, virados em {@link DayOfWeek}.
     *
     * <b>Um nome invalido e recusado, e nao ignorado.</b> Ignorar faria a creche combinar tres dias
     * e o servidor guardar dois — e o dia que sumiu viraria diaria avulsa na conta do tutor, todo
     * mes, sem que ninguem soubesse de onde veio.
     */
    private Set<DayOfWeek> diasCombinados(List<String> nomes) {
        if (nomes == null) {
            return new LinkedHashSet<>();
        }

        Set<DayOfWeek> dias = new LinkedHashSet<>();

        for (String nome : nomes) {
            try {
                dias.add(DayOfWeek.valueOf(nome.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException erro) {
                throw new PetfyHealthcareException(
                        ErrorMessageEnum.INVALID_WEEKDAY.getMessage(),
                        ErrorMessageEnum.INVALID_WEEKDAY.getCode(),
                        HttpStatus.BAD_REQUEST);
            }
        }

        return dias;
    }

    /* ------------------------------------------------------------------------------- o dia */

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> listDay(UUID classGroupId, LocalDate day) {
        Organization organizacao = organizacaoQueGereTurma();

        classGroupRepository.findDaOrganizacao(classGroupId, organizacao.getOrganizationId())
                .orElseThrow(this::naoEncontrado);

        LocalDate dia = day == null ? LocalDate.now() : day;

        Map<UUID, Attendance> registrados = attendanceRepository
                .findDoDiaNaTurma(classGroupId, dia).stream()
                .collect(Collectors.toMap(p -> p.getEnrollment().getEnrollmentId(), Function.identity()));

        List<AttendanceResponseDTO> dia_ = new ArrayList<>();

        for (Enrollment matricula : enrollmentRepository.findVivasDaTurma(classGroupId)) {
            List<HealthProofItemDTO> comprovacao = comprovar(matricula.getAnimal(), organizacao);
            Attendance registro = registrados.get(matricula.getEnrollmentId());

            dia_.add(toResponse(matricula, registro, dia, comprovacao));
        }

        return dia_;
    }

    @Override
    @Transactional
    public AttendanceResponseDTO checkIn(UUID enrollmentId, AttendanceRequestDTO request) {
        Enrollment matricula = matriculaDaMinhaOrganizacao(enrollmentId);
        Organization organizacao = matricula.getClassGroup().getOrganization();
        List<HealthProofItemDTO> comprovacao = comprovar(matricula.getAnimal(), organizacao);

        /*
         * ENTRADA DE ANIMAL IMPEDIDO E RECUSADA AQUI, e nao apenas avisada na tela.
         *
         * A Tela 17 escreve a consequencia inteira: "V10 venceu ontem — nao pode entrar. A turma
         * inteira depende disso". Uma tela que so mostra o aviso deixa a decisao para quem esta com
         * quinze cachorros na porta as 7h30, e a lei nao cobra a tela.
         */
        if (impede(comprovacao)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ENROLLMENT_HEALTH_PROOF_MISSING.getMessage(),
                    ErrorMessageEnum.ENROLLMENT_HEALTH_PROOF_MISSING.getCode(),
                    HttpStatus.CONFLICT);
        }

        LocalDate hoje = LocalDate.now();
        Attendance registro = attendanceRepository.findDoDia(enrollmentId, hoje)
                .orElseGet(() -> novoRegistro(matricula, hoje));

        // o segundo clique em "marcar entrada" nao move a hora de quem ja chegou
        if (registro.getCheckedInAt() == null) {
            registro.setCheckedInAt(LocalDateTime.now());
        }

        registro.setStatus(AttendanceStatus.PRESENTE);

        if (request != null && request.getPickupNote() != null && !request.getPickupNote().isBlank()) {
            registro.setPickupNote(request.getPickupNote().trim());
        }

        Attendance salvo = attendanceRepository.save(registro);

        lancarDiariaSeForDiaAvulso(matricula, hoje);

        return toResponse(matricula, salvo, hoje, comprovacao);
    }

    /**
     * A diaria avulsa, que entra sozinha.
     *
     * <b>E o encadeamento central do bloco 3, e a frase do desenho e literal:</b> "a diaria avulsa
     * entrou sozinha: a creche marcou a entrada do Code num dia fora da combinacao, e o evento de
     * entrada carregou o valor. NINGUEM DIGITOU NADA. E esse encadeamento que faz o custo se manter
     * atualizado sem virar tarefa."
     *
     * <b>Tres condicoes, e cada uma e uma recusa a cobrar por suposicao:</b>
     *
     * <ul>
     *   <li><b>Ha dias combinados.</b> Sem eles nao existe "fora do combinado" — ver
     *       {@link Enrollment#foraDoCombinado}. O silencio da creche nao vira cobranca.</li>
     *   <li><b>Ha diaria combinada.</b> Um dia extra sem valor combinado e um dia extra, e nao um
     *       valor que o servidor teria de inventar.</li>
     *   <li><b>Ainda nao ha diaria deste dia.</b> A creche que marca a entrada, desfaz por engano e
     *       marca de novo nao pode cobrar duas diarias — e o tutor descobriria isso no fim do mes,
     *       quando ninguem mais lembra do que aconteceu naquela terca.</li>
     * </ul>
     *
     * <b>Falhar aqui derruba o check-in inteiro, e e proposital.</b> A alternativa seria engolir a
     * excecao para "nao atrapalhar a porta das 7h30" — e ai o animal entraria com uma diaria
     * perdida, que ninguem jamais saberia que faltou. Um check-in que falha, alguem repete.
     */
    private void lancarDiariaSeForDiaAvulso(Enrollment matricula, LocalDate dia) {
        if (!matricula.foraDoCombinado(dia.getDayOfWeek()) || matricula.getDailyRate() == null) {
            return;
        }

        if (animalCostRepository.existeDiariaNoDia(
                matricula.getEnrollmentId(), dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())) {
            return;
        }

        Organization creche = matricula.getClassGroup().getOrganization();

        animalCostRepository.save(AnimalCost.builder()
                .animal(matricula.getAnimal())
                .description("Diaria avulsa")
                .amount(matricula.getDailyRate())
                .kind(AnimalCostKind.CRECHE_DIARIA)
                .category(AnimalCostCategory.CRECHE)
                // `paid` NULO, e nao false: "ninguem disse". O produto nao sabe se o tutor pagou a
                // diaria no dia, e afirmar que nao pagou seria inventar uma divida.
                .sourceEnrollmentId(matricula.getEnrollmentId())
                .occurredAt(LocalDateTime.now())
                // o evento por tras do valor: quem marcou a entrada, pela creche. "Cada valor tem
                // um evento por tras, com autor e data — e por isso pode ser CONTESTADO."
                .recordedBy(currentPersonProvider.require())
                .organization(creche)
                .creationDate(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public AttendanceResponseDTO checkOut(UUID enrollmentId) {
        Enrollment matricula = matriculaDaMinhaOrganizacao(enrollmentId);
        LocalDate hoje = LocalDate.now();

        Attendance registro = attendanceRepository.findDoDia(enrollmentId, hoje)
                .filter(p -> p.getCheckedInAt() != null)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ATTENDANCE_NOT_CHECKED_IN.getMessage(),
                        ErrorMessageEnum.ATTENDANCE_NOT_CHECKED_IN.getCode(),
                        HttpStatus.CONFLICT));

        if (registro.getCheckedOutAt() == null) {
            registro.setCheckedOutAt(LocalDateTime.now());
        }

        registro.setStatus(AttendanceStatus.SAIU);

        return toResponse(matricula, attendanceRepository.save(registro), hoje,
                comprovar(matricula.getAnimal(), matricula.getClassGroup().getOrganization()));
    }

    @Override
    @Transactional
    public AttendanceResponseDTO markAbsence(UUID enrollmentId) {
        Enrollment matricula = matriculaDaMinhaOrganizacao(enrollmentId);
        LocalDate hoje = LocalDate.now();

        Attendance registro = attendanceRepository.findDoDia(enrollmentId, hoje)
                .orElseGet(() -> novoRegistro(matricula, hoje));

        registro.setStatus(AttendanceStatus.FALTA);

        return toResponse(matricula, attendanceRepository.save(registro), hoje,
                comprovar(matricula.getAnimal(), matricula.getClassGroup().getOrganization()));
    }

    /* ----------------------------------------------------------- a regra que decide tudo */

    /**
     * Cruza o que a organizacao exige com o que a carteira do animal tem.
     *
     * <b>Tres estados por linha, e a diferenca entre dois deles e a alma do produto:</b> `EM_DIA`,
     * `VENCIDA` e `SEM_REGISTRO`. "Nao sabemos" nao e "esta ruim" (secao 05 da identidade) — mas as
     * duas impedem a matricula, porque o produto nao pode afirmar que um animal esta protegido por
     * uma dose que ninguem viu.
     *
     * <b>O casamento e por catalogo, com queda para nome.</b> A `Vaccine.catalog` e nula para dose
     * digitada em texto livre e para tudo que entrou antes do catalogo existir — ignorar esses
     * registros faria o produto dizer "sem registro" para um animal vacinado, e a creche barraria
     * quem esta em dia. O `matchedByName` viaja no DTO justamente para quem le saber que aquela linha
     * vale menos.
     */
    private List<HealthProofItemDTO> comprovar(Animal animal, Organization organizacao) {
        List<OrganizationVaccineRequirement> exigencias =
                requirementRepository.findDaOrganizacao(organizacao.getOrganizationId());

        if (exigencias.isEmpty()) {
            return List.of();
        }

        List<Vaccine> registradas =
                vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animal.getAnimalId());

        LocalDate hoje = LocalDate.now();
        List<HealthProofItemDTO> comprovacao = new ArrayList<>();

        for (OrganizationVaccineRequirement exigencia : exigencias) {
            VaccineCatalog item = exigencia.getVaccineCatalog();

            Optional<Vaccine> porCatalogo = registradas.stream()
                    .filter(v -> v.getCatalog() != null
                            && v.getCatalog().getVaccineCatalogId().equals(item.getVaccineCatalogId()))
                    .max(Comparator.comparing(Vaccine::getApplicationDate,
                            Comparator.nullsFirst(Comparator.naturalOrder())));

            Optional<Vaccine> achada = porCatalogo.isPresent() ? porCatalogo : registradas.stream()
                    .filter(v -> v.getCatalog() == null
                            && v.getVaccineName() != null
                            && item.getName() != null
                            && v.getVaccineName().trim().equalsIgnoreCase(item.getName().trim()))
                    .max(Comparator.comparing(Vaccine::getApplicationDate,
                            Comparator.nullsFirst(Comparator.naturalOrder())));

            if (achada.isEmpty()) {
                comprovacao.add(HealthProofItemDTO.builder()
                        .vaccineCatalogId(item.getVaccineCatalogId())
                        .vaccineName(item.getName())
                        .state("SEM_REGISTRO")
                        .blocks(true)
                        .matchedByName(false)
                        .build());
                continue;
            }

            Vaccine dose = achada.get();

            /*
             * SEM PROXIMA DOSE NAO E VENCIDA.
             *
             * Vacina de dose unica e registro antigo sem `nextDoseDate` existem, e chamar os dois de
             * vencidos barraria animal em dia por falta de um campo. O que temos e uma dose aplicada;
             * o que nao temos e prazo — e a ausencia de prazo nao e prova contra ninguem.
             */
            boolean vencida = dose.getNextDoseDate() != null && dose.getNextDoseDate().isBefore(hoje);

            comprovacao.add(HealthProofItemDTO.builder()
                    .vaccineCatalogId(item.getVaccineCatalogId())
                    .vaccineName(item.getName())
                    .state(vencida ? "VENCIDA" : "EM_DIA")
                    .lastApplicationDate(dose.getApplicationDate())
                    .nextDoseDate(dose.getNextDoseDate())
                    .blocks(vencida)
                    .matchedByName(porCatalogo.isEmpty())
                    .build());
        }

        return comprovacao;
    }

    private boolean impede(List<HealthProofItemDTO> comprovacao) {
        return comprovacao.stream().anyMatch(HealthProofItemDTO::isBlocks);
    }

    /* -------------------------------------------------------------------------- bastidores */

    /**
     * A organizacao declarada, e ela precisa gerir turma.
     *
     * O `GERIR_TURMA_E_VAGA` deixa de ser flag vazia aqui: uma clinica veterinaria com acesso
     * concedido nao matricula ninguem, e o erro precisa dizer isso em vez de 404.
     */
    private Organization organizacaoQueGereTurma() {
        /*
         * `organizacaoDeclarada`, E NAO `requireContext`.
         *
         * O `requireContext` exige credencial profissional ativa, e a monitora que marca entrada as
         * 7h30 nao tem CRMV — nem deveria. O proprio `CurrentProfessionalProvider` documenta esta
         * escolha no metodo que estou usando: "existe para quem age sem credencial profissional. O
         * monitor da creche que manda um tema de casa e membro, e nao tem CRMV". Descobri isso pelo
         * teste: doze casos falharam com "an active professional credential is required", e a
         * credencial nao tem nada a ver com marcar presenca.
         *
         * O que autoriza aqui e o VINCULO com a organizacao mais a CAPACIDADE dela — e o
         * `organizacaoDeclarada` ja confere o vinculo.
         */
        Organization organizacao = currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));

        if (!organizacao.pode(OrganizationCapability.GERIR_TURMA_E_VAGA)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getMessage(),
                    ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return organizacao;
    }

    /**
     * O animal que a ORGANIZACAO alcanca, e nao o que a pessoa alcanca.
     *
     * <b>O `AnimalAccessGuard` nao serve aqui, e o teste foi quem mostrou.</b> Ele resolve alcance por
     * custodia ou concessao DA PESSOA — e a creche alcanca o animal por concessao da ORGANIZACAO:
     * "Marcelo Dias concedeu acesso a saude do Code" foi concedido a Creche Quintal, e nao a Vera. Doze
     * casos falharam com "animal not found" antes disso ficar claro.
     *
     * 404 e nao 403, como em toda parte: dizer "existe e voce nao pode ver" ja e informacao sobre o
     * animal de outra pessoa.
     */
    private Animal animalAlcancadoPelaOrganizacao(UUID animalId, Organization organizacao) {
        return grantRepository
                .findVigenteDaClinicaNoAnimal(animalId, organizacao.getOrganizationId(), LocalDateTime.now())
                .map(Grant::getAnimal)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Enrollment matriculaDaMinhaOrganizacao(UUID enrollmentId) {
        UUID organizationId = organizacaoQueGereTurma().getOrganizationId();

        return enrollmentRepository.findById(enrollmentId)
                .filter(Enrollment::estaViva)
                .filter(e -> e.getClassGroup().getOrganization().getOrganizationId().equals(organizationId))
                .orElseThrow(this::naoEncontrado);
    }

    /**
     * A vaga e limite, e o limite vale para a PENDENTE tambem.
     *
     * A matricula pendente guarda a vaga — e o que a Tela 10 promete ao dizer que ela "se completa
     * sozinha". Nao contar deixaria a creche vender a mesma vaga duas vezes e descobrir na porta.
     */
    private void exigirVagaLivre(ClassGroup turma) {
        if (turma.getCapacity() == null) {
            return;
        }

        if (enrollmentRepository.contarVivasDaTurma(turma.getClassGroupId()) >= turma.getCapacity()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CLASS_GROUP_FULL.getMessage(),
                    ErrorMessageEnum.CLASS_GROUP_FULL.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private Attendance novoRegistro(Enrollment matricula, LocalDate dia) {
        return Attendance.builder()
                .enrollment(matricula)
                .day(dia)
                .status(AttendanceStatus.ESPERADO)
                .recordedBy(currentPersonProvider.require())
                .creationDate(LocalDateTime.now())
                .build();
    }

    private PetfyHealthcareException naoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.CLASS_GROUP_NOT_FOUND.getMessage(),
                ErrorMessageEnum.CLASS_GROUP_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private VaccineRequirementResponseDTO toResponse(OrganizationVaccineRequirement exigencia) {
        VaccineCatalog item = exigencia.getVaccineCatalog();

        return VaccineRequirementResponseDTO.builder()
                .requirementId(exigencia.getRequirementId())
                .vaccineCatalogId(item.getVaccineCatalogId())
                .vaccineName(item.getName())
                .species(item.getSpecies() == null ? null : item.getSpecies().name())
                .build();
    }

    /**
     * A matricula na resposta.
     *
     * <b>O {@code mostrarCombinado} nao tem valor padrao de proposito.</b> Um parametro com padrao
     * seria "mostra, a menos que alguem lembre de esconder" — e quem esquecesse entregaria a
     * mensalidade do tutor a quem tem so uma concessao de leitura. Obrigar cada chamada a
     * responder faz a decisao aparecer no lugar onde ela e tomada.
     */
    private EnrollmentResponseDTO toResponse(Enrollment matricula, List<HealthProofItemDTO> comprovacao,
                                             boolean mostrarCombinado) {
        Person quemMatriculou = matricula.getCreatedBy();
        ClassGroup turma = matricula.getClassGroup();

        return EnrollmentResponseDTO.builder()
                .monthlyFee(mostrarCombinado ? matricula.getMonthlyFee() : null)
                .dueDay(mostrarCombinado ? matricula.getDueDay() : null)
                .dailyRate(mostrarCombinado ? matricula.getDailyRate() : null)
                // os DIAS acompanham o dinheiro, e nao a matricula: "3 dias por semana" e a
                // primeira metade da frase que termina em "R$ 530/mes", e o desenho os poe na
                // mesma linha do combinado
                .weekdays(mostrarCombinado ? diasEmNome(matricula) : null)
                .enrollmentId(matricula.getEnrollmentId())
                .animalId(matricula.getAnimal().getAnimalId())
                .animalName(matricula.getAnimal().getName())
                .classGroupId(turma.getClassGroupId())
                .classGroupName(turma.getName())
                .organizationName(turma.getOrganization().getName())
                .status(matricula.getStatus().name())
                .requestedAt(matricula.getRequestedAt())
                .activatedAt(matricula.getActivatedAt())
                .endedAt(matricula.getEndedAt())
                .createdByName(quemMatriculou == null ? null : quemMatriculou.getName())
                // COPIA, e nao a lista montada por referencia: a resposta atravessa a fronteira da
                // transacao, e foi assim que o `scopes` do Grant respondeu 500 na serializacao
                .healthProof(List.copyOf(comprovacao))
                .build();
    }

    /**
     * Os dias combinados em nome, na ordem da semana.
     *
     * <b>Ordenado, e nao na ordem em que a creche clicou.</b> "Segunda, quarta e sexta" e "quarta,
     * segunda e sexta" sao o mesmo combinado, e uma tela que mostrasse a segunda ordem faria o
     * tutor achar que algo mudou.
     */
    private List<String> diasEmNome(Enrollment matricula) {
        if (matricula.getWeekdays() == null) {
            return List.of();
        }

        return matricula.getWeekdays().stream()
                .sorted()
                .map(DayOfWeek::name)
                .toList();
    }

    private AttendanceResponseDTO toResponse(Enrollment matricula, Attendance registro, LocalDate dia,
                                             List<HealthProofItemDTO> comprovacao) {
        Animal animal = matricula.getAnimal();
        boolean bloqueado = impede(comprovacao);

        return AttendanceResponseDTO.builder()
                .attendanceId(registro == null ? null : registro.getAttendanceId())
                .enrollmentId(matricula.getEnrollmentId())
                .animalId(animal.getAnimalId())
                .animalName(animal.getName())
                .species(animal.getSpecies() == null ? null : animal.getSpecies().name())
                .day(dia)
                // quem tem matricula ativa e nao tem registro do dia nasce ESPERADO, e e isso que faz
                // a Tela 17 saber dizer "14 esperados · 6 ja chegaram" sem ninguem ter marcado nada
                .status(registro == null ? AttendanceStatus.ESPERADO.name() : registro.getStatus().name())
                .checkedInAt(registro == null ? null : registro.getCheckedInAt())
                .checkedOutAt(registro == null ? null : registro.getCheckedOutAt())
                .pickupNote(registro == null ? null : registro.getPickupNote())
                .todayNeeds(List.copyOf(hojePrecisa(animal)))
                .blocked(bloqueado)
                .blockedReason(bloqueado ? primeiraQueImpede(comprovacao) : null)
                .build();
    }

    /**
     * "Hoje precisa": o que a creche tem de saber sobre este animal hoje.
     *
     * Sai das orientacoes vigentes — "Amoxicilina ao meio-dia" do desenho. <b>As condicoes do animal
     * NAO entram aqui</b>, e a ausencia e escolha: "sem frango" e alergia, que vive em `CONDICOES`, e
     * a creche so ve condicao se o tutor tiver concedido aquele escopo. Misturar as duas faria esta
     * linha entregar alergia a quem recebeu apenas a carteira.
     */
    private List<String> hojePrecisa(Animal animal) {
        /*
         * LE DO REPOSITORIO, e nao do CareInstructionService — e a razao nao e gosto.
         *
         * A primeira versao chamava o servico e engolia a excecao dele para o caso de a creche nao
         * alcancar a orientacao. O servico e `@Transactional`: quando ele lanca, a transacao fica
         * marcada rollback-only, e engolir a excecao fazia o COMMIT do dia inteiro falhar depois com
         * `UnexpectedRollback`. Quatro casos do teste morreram assim, e a mensagem nao apontava para
         * lugar nenhum perto daqui.
         *
         * A guarda de alcance ja aconteceu antes: quem chega neste ponto passou pela concessao da
         * organizacao no animal. Ler direto e mais honesto do que chamar algo para ignorar o erro.
         */
        LocalDate hoje = LocalDate.now();

        return careInstructionRepository.findByAnimalAnimalIdOrderByStartsOnDesc(animal.getAnimalId())
                .stream()
                .filter(orientacao -> orientacao.getRevokedAt() == null)
                .filter(orientacao -> orientacao.getEndsOn() == null || !orientacao.getEndsOn().isBefore(hoje))
                .map(orientacao -> orientacao.getDescription())
                .filter(descricao -> descricao != null && !descricao.isBlank())
                .toList();
    }

    private String primeiraQueImpede(List<HealthProofItemDTO> comprovacao) {
        return comprovacao.stream()
                .filter(HealthProofItemDTO::isBlocks)
                .map(item -> item.getVaccineName() + " · " + item.getState())
                .findFirst()
                .orElse(null);
    }
}
