package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AttendanceResponseDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupResponseDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentAgreementRequestDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthProofItemDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.CrecheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A operacao da creche: turma, vaga, comprovacao de saude e o dia.
 *
 * <b>Contra banco de verdade porque tres invariantes desta fase moram em indice, e mock nao tem
 * indice:</b> uma matricula viva por animal e turma, um registro de presenca por matricula e dia, e
 * "saida sem entrada nao existe". O segundo e o mais provavel de ser exercitado por acidente — dois
 * cliques em "marcar entrada" numa manha de creche.
 *
 * A classe tambem <b>serializa</b> as respostas: a comprovacao viaja como lista dentro do DTO, e foi
 * exatamente uma lista entregue por referencia que fez a rota de acessos responder 500 na escrita da
 * resposta, ja fora da transacao.
 */
@SpringBootTest
@DisplayName("a creche: turma, vaga, comprovacao e o dia, contra Postgres real")
class CrecheContainerTest extends PostgresContainerTest {

    @Autowired private CrecheService crecheService;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private VaccineCatalogRepository vaccineCatalogRepository;
    @Autowired private AnimalCostRepository animalCostRepository;

    private Organization creche;
    private Person vera;
    private Person tutor;
    private Animal code;
    private VaccineCatalog antirrabica;

    @BeforeEach
    void montarACreche() {
        creche = organizationRepository.saveAndFlush(Organization.builder()
                .name("Creche Quintal " + UUID.randomUUID())
                .creationDate(LocalDateTime.now())
                .capabilities(Set.of(OrganizationCapability.GERIR_TURMA_E_VAGA,
                        OrganizationCapability.REGISTRAR_OBSERVACAO))
                .build());

        vera = personRepository.saveAndFlush(Person.builder()
                .name("Vera Quintal")
                .email("vera-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("marcelo-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(creche).person(vera)
                .role(MembershipRole.ADMINISTRADOR)
                .joinedAt(LocalDateTime.now())
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(tutor).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(6))
                .build());

        // "Marcelo Dias concedeu acesso a saude do Code": e assim que a creche alcanca o animal
        grantRepository.saveAndFlush(Grant.builder()
                .animal(code).granteeOrganization(creche)
                .level(GrantLevel.EDITOR).grantedBy(tutor)
                .grantedAt(LocalDateTime.now())
                .scopes(Set.of(GrantScope.CARTEIRA, GrantScope.CONDICOES))
                .build());

        // O id do catalogo e ATRIBUIDO, e nao gerado: os itens nascem de migracao com id fixo, para
        // o mesmo codigo de vacina ter o mesmo id em todo ambiente. Quem cria um item em teste
        // precisa dizer o id.
        antirrabica = vaccineCatalogRepository.saveAndFlush(VaccineCatalog.builder()
                .vaccineCatalogId(UUID.randomUUID())
                .code("ANTIRRABICA-" + UUID.randomUUID())
                .name("Antirrabica")
                .species(Species.CANINA)
                .defaultIntervalDays(365)
                .initialDoseCount(1)
                .initialDoseIntervalDays(0)
                .mandatory(true)
                .build());

        agirComoVera();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /* ------------------------------------------------------------------------ a comprovacao */

    @Test
    @DisplayName("sem exigencia declarada, a matricula nasce ATIVA")
    void semExigenciaAtiva() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").capacity(15).build());

        EnrollmentResponseDTO matricula = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThat(matricula.getStatus()).isEqualTo("ATIVA");
        assertThat(matricula.getHealthProof()).isEmpty();
    }

    /**
     * <b>SEM_REGISTRO impede, e nao e o mesmo que VENCIDA.</b> "Nao sabemos" nao e "esta ruim" — mas
     * o produto tambem nao pode afirmar que o animal esta protegido por uma dose que ninguem viu.
     */
    @Test
    @DisplayName("exigencia sem dose nenhuma deixa a matricula PENDENTE, com SEM_REGISTRO")
    void semDoseFicaPendente() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Manha").build());

        EnrollmentResponseDTO matricula = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThat(matricula.getStatus()).isEqualTo("PENDENTE");
        assertThat(matricula.getHealthProof()).hasSize(1);

        HealthProofItemDTO linha = matricula.getHealthProof().get(0);
        assertThat(linha.getState()).isEqualTo("SEM_REGISTRO");
        assertThat(linha.isBlocks()).isTrue();
    }

    @Test
    @DisplayName("dose vencida impede; dose em dia libera, e o casamento e pelo catalogo")
    void venceEDepoisLibera() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        // vencida ontem
        Vaccine vencida = vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code).catalog(antirrabica).vaccineName("Antirrabica")
                .applicationDate(LocalDate.now().minusYears(1).minusDays(1))
                .nextDoseDate(LocalDate.now().minusDays(1))
                .creationDate(LocalDateTime.now())
                .build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());

        EnrollmentResponseDTO pendente = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());
        assertThat(pendente.getStatus()).isEqualTo("PENDENTE");
        assertThat(pendente.getHealthProof().get(0).getState()).isEqualTo("VENCIDA");
        assertThat(pendente.getHealthProof().get(0).isMatchedByName()).isFalse();

        /*
         * A DOSE NOVA COMPLETA A MATRICULA SOZINHA, e este e o teste da frase da Tela 10: "a
         * matricula fica guardada e se completa sozinha assim que a dose for registrada". Ninguem
         * tocou na matricula entre as duas leituras — o que mudou foi a carteira.
         */
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code).catalog(antirrabica).vaccineName("Antirrabica")
                .applicationDate(LocalDate.now())
                .nextDoseDate(LocalDate.now().plusYears(1))
                .creationDate(LocalDateTime.now())
                .build());

        List<EnrollmentResponseDTO> depois = crecheService.listEnrollments(turma.getClassGroupId());

        assertThat(depois).hasSize(1);
        assertThat(depois.get(0).getHealthProof().get(0).getState()).isEqualTo("EM_DIA");
        assertThat(depois.get(0).getHealthProof().get(0).isBlocks()).isFalse();
        assertThat(vencida.getVaccineId()).isNotNull();
    }

    /**
     * A `Vaccine.catalog` e nula para dose digitada em texto livre e para tudo que entrou antes do
     * catalogo existir. Ignorar esses registros faria o produto dizer "sem registro" para animal
     * vacinado — e a creche barraria quem esta em dia.
     */
    @Test
    @DisplayName("dose sem catalogo casa por nome, e a resposta avisa que casou assim")
    void casaPorNome() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code).vaccineName("antirrabica")
                .applicationDate(LocalDate.now().minusMonths(2))
                .nextDoseDate(LocalDate.now().plusMonths(10))
                .creationDate(LocalDateTime.now())
                .build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());

        HealthProofItemDTO linha = crecheService
                .enroll(code.getAnimalId(), turma.getClassGroupId())
                .getHealthProof().get(0);

        assertThat(linha.getState()).isEqualTo("EM_DIA");
        assertThat(linha.isMatchedByName())
                .as("quem le precisa saber que esta linha vale menos")
                .isTrue();
    }

    /* ------------------------------------------------------------------------------- a vaga */

    @Test
    @DisplayName("a vaga e limite, e a matricula pendente ocupa vaga")
    void vagaCheia() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma de um").capacity(1).build());

        crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        Animal nina = animalRepository.saveAndFlush(Animal.builder()
                .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(nina).holderPerson(tutor).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());
        grantRepository.saveAndFlush(Grant.builder()
                .animal(nina).granteeOrganization(creche).level(GrantLevel.EDITOR)
                .grantedBy(tutor).grantedAt(LocalDateTime.now())
                .scopes(Set.of(GrantScope.CARTEIRA)).build());

        assertThatThrownBy(() -> crecheService.enroll(nina.getAnimalId(), turma.getClassGroupId()))
                .isInstanceOf(PetfyHealthcareException.class);

        assertThat(crecheService.listClassGroups())
                .anySatisfy(t -> assertThat(t.getOccupied()).isEqualTo(1));
    }

    /* -------------------------------------------------------------------------------- o dia */

    @Test
    @DisplayName("quem tem matricula ativa nasce ESPERADO no dia, sem ninguem marcar nada")
    void nasceEsperado() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        List<AttendanceResponseDTO> dia = crecheService.listDay(turma.getClassGroupId(), LocalDate.now());

        assertThat(dia).hasSize(1);
        assertThat(dia.get(0).getStatus()).isEqualTo("ESPERADO");
        assertThat(dia.get(0).getAttendanceId())
                .as("nada foi gravado ainda: o estado e derivado da matricula")
                .isNull();
    }

    @Test
    @DisplayName("entrada, saida, e o segundo clique nao move a hora nem cria outro registro")
    void entradaESaida() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        EnrollmentResponseDTO matricula = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        AttendanceResponseDTO chegou = crecheService.checkIn(matricula.getEnrollmentId(), null);
        assertThat(chegou.getStatus()).isEqualTo("PRESENTE");
        assertThat(chegou.getCheckedInAt()).isNotNull();

        AttendanceResponseDTO segundoClique = crecheService.checkIn(matricula.getEnrollmentId(), null);
        assertThat(segundoClique.getAttendanceId()).isEqualTo(chegou.getAttendanceId());

        /*
         * COMPARADO EM MILISSEGUNDOS, e nao no instante cru.
         *
         * A primeira resposta traz o horario que acabou de ser montado em memoria, com nanos; a
         * segunda vem do banco, e o `timestamp` do Postgres guarda microssegundos. Sao o MESMO
         * instante — o segundo clique nao mexeu na hora —, e a diferenca era so o arredondamento da
         * ida ao banco. A assercao estava errada, e nao o codigo.
         */
        assertThat(segundoClique.getCheckedInAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(chegou.getCheckedInAt().truncatedTo(ChronoUnit.MILLIS));

        AttendanceResponseDTO saiu = crecheService.checkOut(matricula.getEnrollmentId());
        assertThat(saiu.getStatus()).isEqualTo("SAIU");
        assertThat(saiu.getCheckedOutAt()).isNotNull();
    }

    /**
     * "V10 venceu ontem — nao pode entrar. A turma inteira depende disso." A recusa mora no servidor
     * de proposito: uma tela que so mostra o aviso deixa a decisao para quem esta com quinze cachorros
     * na porta as 7h30, e a lei nao cobra a tela.
     */
    @Test
    @DisplayName("animal com comprovacao aberta nao entra, e a recusa e do servidor")
    void impedidoNaoEntra() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        EnrollmentResponseDTO pendente = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThatThrownBy(() -> crecheService.checkIn(pendente.getEnrollmentId(), null))
                .isInstanceOf(PetfyHealthcareException.class);

        List<AttendanceResponseDTO> dia = crecheService.listDay(turma.getClassGroupId(), null);
        assertThat(dia.get(0).isBlocked()).isTrue();
        assertThat(dia.get(0).getBlockedReason()).contains("Antirrabica");
    }

    @Test
    @DisplayName("saida sem entrada e recusada, e o banco tambem recusaria")
    void saidaSemEntrada() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        EnrollmentResponseDTO matricula = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThatThrownBy(() -> crecheService.checkOut(matricula.getEnrollmentId()))
                .isInstanceOf(PetfyHealthcareException.class);
    }

    @Test
    @DisplayName("falta nao exige entrada, e fica no registro do dia")
    void falta() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        EnrollmentResponseDTO matricula = crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThat(crecheService.markAbsence(matricula.getEnrollmentId()).getStatus()).isEqualTo("FALTA");
    }

    /* ---------------------------------------------------------------- ler E serializar */

    @Test
    @DisplayName("o dia e a matricula atravessam leitura e serializacao")
    void serializaSemQuebrar() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());

        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        assertThatCode(() -> {
            objectMapper.writeValueAsString(crecheService.listDay(turma.getClassGroupId(), null));
            objectMapper.writeValueAsString(crecheService.listEnrollments(turma.getClassGroupId()));
            objectMapper.writeValueAsString(crecheService.listClassGroups());
            objectMapper.writeValueAsString(crecheService.listRequirements());
        }).doesNotThrowAnyException();
    }

    /** Do lado do tutor: quem alcanca o animal ve o que a creche esta esperando dele. */
    @Test
    @DisplayName("o tutor ve a matricula do animal dele, com a comprovacao")
    void oLadoDoTutor() {
        crecheService.addRequirement(VaccineRequirementRequestDTO.builder()
                .vaccineCatalogId(antirrabica.getVaccineCatalogId()).build());
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").build());
        crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());

        agirComoTutor();

        List<EnrollmentResponseDTO> minhas = crecheService.listEnrollmentsOfAnimal(code.getAnimalId());

        assertThat(minhas).hasSize(1);
        assertThat(minhas.get(0).getOrganizationName()).startsWith("Creche Quintal");
        assertThat(minhas.get(0).getHealthProof()).hasSize(1);
    }

    /* ------------------------------------------------- o combinado, e a diaria que entra sozinha */

    /**
     * <b>Substitui o combinado inteiro, e nao emenda campo a campo.</b> Quem renegocia diz de novo
     * o que passou a valer — e uma mensalidade que sobrasse do combinado anterior iria para a conta
     * do tutor sem ninguem ter dito nada.
     */
    @Test
    @DisplayName("o combinado e gravado inteiro, e o segundo combinado apaga o que ele nao repetir")
    void oCombinadoSubstituiInteiro() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();

        EnrollmentResponseDTO combinada = crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder()
                        .monthlyFee(new BigDecimal("530.00"))
                        .dueDay(5)
                        .dailyRate(new BigDecimal("88.00"))
                        .weekdays(List.of("WEDNESDAY", "MONDAY", "FRIDAY"))
                        .build());

        assertThat(combinada.getMonthlyFee()).isEqualByComparingTo("530.00");
        assertThat(combinada.getDueDay()).isEqualTo(5);
        assertThat(combinada.getDailyRate()).isEqualByComparingTo("88.00");
        assertThat(combinada.getWeekdays())
                .as("na ordem da semana, e nao na ordem em que a creche clicou: "
                        + "'segunda, quarta e sexta' e 'quarta, segunda e sexta' sao o mesmo combinado")
                .containsExactly("MONDAY", "WEDNESDAY", "FRIDAY");

        EnrollmentResponseDTO recombinada = crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder().monthlyFee(new BigDecimal("600.00")).build());

        assertThat(recombinada.getMonthlyFee()).isEqualByComparingTo("600.00");
        assertThat(recombinada.getDueDay()).isNull();
        assertThat(recombinada.getDailyRate())
                .as("a diaria que sobrasse de um combinado antigo cobraria o tutor sozinha")
                .isNull();
        assertThat(recombinada.getWeekdays()).isEmpty();
    }

    /**
     * <b>O encadeamento central do bloco 3</b>, e a frase do desenho e literal: "a diaria avulsa
     * entrou sozinha: a creche marcou a entrada do Code num dia fora da combinacao, e o evento de
     * entrada carregou o valor. Ninguem digitou nada."
     */
    @Test
    @DisplayName("check-in em dia fora do combinado lanca a diaria sozinho, com autor e organizacao")
    void aDiariaEntraSozinha() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();
        combinarExcluindoHoje(matricula.getEnrollmentId(), new BigDecimal("88.00"));

        crecheService.checkIn(matricula.getEnrollmentId(), null);

        List<AnimalCost> custos = animalCostRepository
                .findByAnimalAnimalIdOrderByOccurredAtDesc(code.getAnimalId());

        assertThat(custos).hasSize(1);

        AnimalCost diaria = custos.get(0);
        assertThat(diaria.getKind()).isEqualTo(AnimalCostKind.CRECHE_DIARIA);
        assertThat(diaria.getAmount()).isEqualByComparingTo("88.00");
        assertThat(diaria.getSourceEnrollmentId()).isEqualTo(matricula.getEnrollmentId());
        assertThat(diaria.getPaid())
                .as("'ninguem disse' — afirmar que nao foi pago seria inventar uma divida")
                .isNull();
        assertThat(diaria.getRecordedBy().getPersonId())
                .as("cada valor tem um evento por tras, com autor — e por isso pode ser contestado")
                .isEqualTo(vera.getPersonId());
        assertThat(diaria.getOrganization().getOrganizationId()).isEqualTo(creche.getOrganizationId());
    }

    @Test
    @DisplayName("no dia combinado a entrada nao lanca nada: a mensalidade ja cobre o dia")
    void diaCombinadoNaoCobra() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();

        crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder()
                        .monthlyFee(new BigDecimal("530.00"))
                        .dailyRate(new BigDecimal("88.00"))
                        .weekdays(List.of(LocalDate.now().getDayOfWeek().name()))
                        .build());

        crecheService.checkIn(matricula.getEnrollmentId(), null);

        assertThat(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(code.getAnimalId()))
                .isEmpty();
    }

    /**
     * <b>O silencio da creche nao vira cobranca.</b> Sem dias declarados nao existe "fora do
     * combinado" — e ler o vazio como "todo dia e avulso" faria cada entrada virar diaria na conta
     * de um tutor que nunca combinou nada disso.
     */
    @Test
    @DisplayName("sem dias declarados a diaria nunca entra, mesmo com valor combinado")
    void semDiasNaoCobra() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();

        crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder().dailyRate(new BigDecimal("88.00")).build());

        crecheService.checkIn(matricula.getEnrollmentId(), null);

        assertThat(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(code.getAnimalId()))
                .isEmpty();
    }

    @Test
    @DisplayName("dia avulso sem diaria combinada nao inventa valor nenhum")
    void semValorNaoCobra() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();
        combinarExcluindoHoje(matricula.getEnrollmentId(), null);

        crecheService.checkIn(matricula.getEnrollmentId(), null);

        assertThat(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(code.getAnimalId()))
                .isEmpty();
    }

    /**
     * A creche que marca a entrada, desfaz por engano e marca de novo nao pode cobrar duas diarias —
     * e o tutor descobriria isso no fim do mes, quando ninguem mais lembra do que aconteceu naquela
     * terca.
     */
    @Test
    @DisplayName("o segundo clique em marcar entrada nao cobra a diaria duas vezes")
    void aDiariaNaoDobra() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();
        combinarExcluindoHoje(matricula.getEnrollmentId(), new BigDecimal("88.00"));

        crecheService.checkIn(matricula.getEnrollmentId(), null);
        crecheService.checkIn(matricula.getEnrollmentId(), null);
        crecheService.checkOut(matricula.getEnrollmentId());
        crecheService.checkIn(matricula.getEnrollmentId(), null);

        assertThat(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(code.getAnimalId()))
                .hasSize(1);
    }

    /**
     * <b>"Nenhum escopo de acesso concede preco junto com saude."</b> A regra que mantem o custo
     * fora da linha do tempo vale tambem no unico outro lugar do produto em que ha dinheiro: o
     * combinado viaja no DTO da matricula, que quem tem concessao le.
     */
    @Test
    @DisplayName("quem so alcanca o animal ve a matricula, e nao ve o combinado")
    void oCombinadoNaoVazaPorConcessao() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();

        crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder()
                        .monthlyFee(new BigDecimal("530.00"))
                        .dueDay(5)
                        .dailyRate(new BigDecimal("88.00"))
                        .weekdays(List.of("MONDAY"))
                        .build());

        Person petshop = personRepository.saveAndFlush(Person.builder()
                .name("Rita do Petshop")
                .email("rita-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        grantRepository.saveAndFlush(Grant.builder()
                .animal(code).granteePerson(petshop).level(GrantLevel.VIEWER)
                .grantedBy(tutor).grantedAt(LocalDateTime.now())
                .scopes(Set.of(GrantScope.CARTEIRA)).build());

        agirComo(petshop);

        EnrollmentResponseDTO comConcessao =
                crecheService.listEnrollmentsOfAnimal(code.getAnimalId()).get(0);

        assertThat(comConcessao.getClassGroupName())
                .as("a matricula ela ve: precisa saber que o Code tem creche")
                .isNotNull();
        assertThat(comConcessao.getMonthlyFee()).isNull();
        assertThat(comConcessao.getDueDay()).isNull();
        assertThat(comConcessao.getDailyRate()).isNull();
        assertThat(comConcessao.getWeekdays()).isNull();

        agirComoTutor();

        EnrollmentResponseDTO doTutor =
                crecheService.listEnrollmentsOfAnimal(code.getAnimalId()).get(0);

        assertThat(doTutor.getMonthlyFee())
                .as("quem responde pelo animal le o que ele custa")
                .isEqualByComparingTo("530.00");
        assertThat(doTutor.getWeekdays()).containsExactly("MONDAY");
    }

    /**
     * Ignorar o dia invalido faria a creche combinar tres dias e o servidor guardar dois — e o dia
     * que sumiu viraria diaria avulsa na conta do tutor, todo mes, sem que ninguem soubesse de onde
     * veio.
     */
    @Test
    @DisplayName("dia da semana escrito em outra lingua e recusado, e nao ignorado")
    void diaInvalidoERecusado() {
        EnrollmentResponseDTO matricula = matricularNaTurmaTarde();

        assertThatThrownBy(() -> crecheService.setAgreement(matricula.getEnrollmentId(),
                EnrollmentAgreementRequestDTO.builder()
                        .weekdays(List.of("MONDAY", "SEGUNDA")).build()))
                .isInstanceOf(PetfyHealthcareException.class);
    }

    /* -------------------------------------------------------------------------- bastidores */

    private EnrollmentResponseDTO matricularNaTurmaTarde() {
        ClassGroupResponseDTO turma = crecheService.createClassGroup(
                ClassGroupRequestDTO.builder().name("Turma Tarde").capacity(15).build());

        return crecheService.enroll(code.getAnimalId(), turma.getClassGroupId());
    }

    /**
     * Combina todos os dias da semana MENOS o de hoje.
     *
     * O teste nao pode escolher o dia da entrada — o check-in e sempre de hoje —, entao quem se
     * move e o combinado. Assim o caso vale numa segunda e num domingo, sem depender de quando a
     * suite roda.
     */
    private void combinarExcluindoHoje(UUID enrollmentId, BigDecimal diaria) {
        List<String> menosHoje = java.util.Arrays.stream(DayOfWeek.values())
                .filter(dia -> dia != LocalDate.now().getDayOfWeek())
                .map(DayOfWeek::name)
                .toList();

        crecheService.setAgreement(enrollmentId, EnrollmentAgreementRequestDTO.builder()
                .monthlyFee(new BigDecimal("530.00"))
                .dueDay(5)
                .dailyRate(diaria)
                .weekdays(menosHoje)
                .build());
    }

    private void agirComo(Person pessoa) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private void agirComoVera() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(vera.getEmail(), "n/a", List.of()));

        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader(CurrentProfessionalProvider.HEADER_ORGANIZACAO,
                creche.getOrganizationId().toString());

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
    }

    private void agirComoTutor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tutor.getEmail(), "n/a", List.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }
}
