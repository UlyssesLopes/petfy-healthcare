package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AttendanceResponseDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupResponseDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthProofItemDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
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

    /* -------------------------------------------------------------------------- bastidores */

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
