package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.HealthRecordCorrection;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.PetShare;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.PetWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.service.PetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apagar o pet contra Postgres de verdade.
 *
 * Oito entidades apontam para {@code pets} e o schema nao tem
 * {@code ON DELETE CASCADE} em lugar nenhum, entao quem recusa cada delete e a
 * chave estrangeira - que so existe no banco. Com repositorio mockado, apagar um
 * pet "funciona" mesmo quando ele tem carteira inteira pendurada.
 *
 * O caso coberto aqui e o normal, e nao o extremo: pet com vacina, historico,
 * peso, antiparasitario, link de compartilhamento, acesso de clinica e as
 * correcoes de cada um. Um pet sem nada disso e um pet cadastrado ha cinco
 * minutos.
 */
@SpringBootTest
@Transactional
@DisplayName("apagar pet contra Postgres real")
class PetDeletionContainerTest extends PostgresContainerTest {

    @Autowired private PetService petService;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private VetRepository vetRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    @Autowired private PetShareRepository petShareRepository;
    @Autowired private PetClinicAccessRepository petClinicAccessRepository;
    @Autowired private PetWeightHistoryRepository petWeightHistoryRepository;
    @Autowired private AntiparasiticRepository antiparasiticRepository;

    private Owner ulysses;
    private Pet rex;
    private Clinic bichoFeliz;

    @BeforeEach
    void setUp() {
        ulysses = ownerRepository.saveAndFlush(Owner.builder()
                .name("Ulysses")
                .email("ulysses-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = petRepository.saveAndFlush(Pet.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        petTutorRepository.saveAndFlush(PetTutor.builder()
                .pet(rex).owner(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());

        bichoFeliz = clinicRepository.saveAndFlush(Clinic.builder().name("Clinica Bicho Feliz").build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ulysses.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    /** A carteira que qualquer pet com algum uso tem. */
    private void comHistoricoCompleto() {
        Vaccine vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                .pet(rex).clinic(bichoFeliz).vaccineName("Antirrabica")
                .applicationDate(LocalDate.now().minusMonths(6))
                .nextDoseDate(LocalDate.now().plusMonths(6))
                .creationDate(LocalDateTime.now()).build());

        vaccineCorrectionRepository.saveAndFlush(VaccineCorrection.builder()
                .vaccine(vacina).correctedByOwner(ulysses).previousVaccineName("Antirabica")
                .correctedAt(LocalDateTime.now()).build());

        HealthRecord atendimento = healthRecordRepository.saveAndFlush(HealthRecord.builder()
                .pet(rex).clinic(bichoFeliz).eventType("Consulta")
                .eventDate(LocalDate.now().minusMonths(2))
                .creationDate(LocalDateTime.now()).build());

        healthRecordCorrectionRepository.saveAndFlush(HealthRecordCorrection.builder()
                .healthRecord(atendimento).correctedByOwner(ulysses).previousEventType("Retorno")
                .correctedAt(LocalDateTime.now()).build());

        petWeightHistoryRepository.saveAndFlush(PetWeightHistory.builder()
                .pet(rex).weight(12.5).measuredAt(LocalDate.now().minusMonths(1))
                .creationDate(LocalDateTime.now()).build());

        antiparasiticRepository.saveAndFlush(Antiparasitic.builder()
                .pet(rex).name("Vermifugo").kind(AntiparasiticKind.DEWORMER)
                .applicationDate(LocalDate.now().minusMonths(3))
                .creationDate(LocalDateTime.now()).updateDate(LocalDateTime.now()).build());

        petShareRepository.saveAndFlush(PetShare.builder()
                .pet(rex).tokenHash("hash-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .creationDate(LocalDateTime.now()).build());

        petClinicAccessRepository.saveAndFlush(PetClinicAccess.builder()
                .pet(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());
    }

    /**
     * O caso que o teste de mock dava como funcionando.
     */
    @Test
    @DisplayName("apagar o pet leva a carteira inteira junto")
    void apagarPetComHistoricoCompleto() {
        comHistoricoCompleto();

        petService.deletePet(rex.getPetId());
        petRepository.flush();

        assertThat(petRepository.findById(rex.getPetId())).isEmpty();

        UUID petId = rex.getPetId();
        assertThat(vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(petId)).isEmpty();
        assertThat(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(petId)).isEmpty();
        assertThat(petWeightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(petId)).isEmpty();
        assertThat(antiparasiticRepository.findByPetPetIdOrderByApplicationDateDesc(petId)).isEmpty();
        assertThat(petShareRepository.findByPetOrderByCreationDateDesc(rex)).isEmpty();
        assertThat(petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(petId)).isEmpty();
    }

    /**
     * O tutor continua de pe: apagar o pet nao e apagar a conta. Vale dizer porque
     * a limpeza passa por owners nas correcoes, e um delete a mais ali levaria a
     * conta junto.
     */
    @Test
    @DisplayName("apagar o pet nao apaga o tutor nem a clinica")
    void apagarPetNaoApagaTutorNemClinica() {
        comHistoricoCompleto();

        petService.deletePet(rex.getPetId());
        petRepository.flush();

        assertThat(ownerRepository.findById(ulysses.getOwnerId())).isPresent();
        assertThat(clinicRepository.findById(bichoFeliz.getClinicId())).isPresent();
    }

    /** Pet recem-cadastrado, sem nada pendurado: o caso que ja funcionava. */
    @Test
    @DisplayName("apagar pet sem historico continua funcionando")
    void apagarPetSemHistorico() {
        petService.deletePet(rex.getPetId());
        petRepository.flush();

        assertThat(petRepository.findById(rex.getPetId())).isEmpty();
    }

    /**
     * O pet de outro tutor nao pode ser tocado pela limpeza: os deletes sao por
     * petId, e um deles escrito com o filtro errado levaria a carteira do pet do
     * vizinho.
     */
    @Test
    @DisplayName("a limpeza nao passa do pet apagado")
    void limpezaNaoPassaDoPetApagado() {
        comHistoricoCompleto();

        Pet nina = petRepository.saveAndFlush(Pet.builder()
                .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
        petTutorRepository.saveAndFlush(PetTutor.builder()
                .pet(nina).owner(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .pet(nina).vaccineName("V10").applicationDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        petService.deletePet(rex.getPetId());
        petRepository.flush();

        assertThat(petRepository.findById(nina.getPetId())).isPresent();
        assertThat(vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(nina.getPetId()))
                .extracting(Vaccine::getVaccineName).containsExactly("V10");
    }

}
