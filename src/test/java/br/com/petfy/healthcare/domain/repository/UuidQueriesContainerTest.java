package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.PetShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.entity.Vet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas por UUID, contra Postgres.
 *
 * Todas estas voltavam vazias no H2 por causa do BINARY(255) de tamanho fixo, o
 * que impedia afirmar qualquer coisa sobre as linhas que elas trazem. Como sao
 * a base do escopo por dono e do acesso do veterinario, "provavelmente funciona
 * no Postgres" era a maior divida da suite: se alguma trouxesse de menos, o
 * tutor deixaria de ver os proprios dados; de mais, veria os de outro.
 */
@SpringBootTest
@Transactional
class UuidQueriesContainerTest extends PostgresContainerTest {

    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private PetShareRepository petShareRepository;
    @Autowired private VetRepository vetRepository;
    @Autowired private PetClinicAccessRepository petClinicAccessRepository;
    @Autowired private ClinicInviteRepository clinicInviteRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;

    private Owner ulysses;
    private Owner maria;
    private Pet rex;
    private Pet nina;
    private Clinic bichoFeliz;
    private Vet marina;

    @BeforeEach
    void setUp() {
        ulysses = ownerRepository.save(Owner.builder()
                .name("Ulysses").email("ulysses-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());
        maria = ownerRepository.save(Owner.builder()
                .name("Maria").email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());

        rex = petComTitular("Rex", ulysses);
        nina = petComTitular("Nina", maria);

        bichoFeliz = clinicRepository.save(Clinic.builder().name("Clinica Bicho Feliz").build());

        marina = vetRepository.save(Vet.builder()
                .name("Dra. Marina").email("marina-" + UUID.randomUUID() + "@vet.com.br")
                .password("hash").clinic(bichoFeliz).build());
    }

    /**
     * O pet e o vinculo sao gravados em duas chamadas, como em
     * {@code PetServiceImpl.createPet}: {@code Pet.tutors} e {@code mappedBy} sem
     * cascade, entao salvar o pet nao grava tutor nenhum. Montar o vinculo so em
     * memoria - o que a suite fazia enquanto {@code Pet} tinha {@code owner} -
     * deixaria toda consulta por tutor voltando vazia, e o teste diria que o
     * escopo por dono nao traz nada quando o que falta e a linha no banco.
     */
    private Pet petComTitular(String nome, Owner titular) {
        Pet pet = petRepository.save(Pet.builder().name(nome).species(Species.CANINA).build());

        PetTutor vinculo = petTutorRepository.save(PetTutor.builder()
                .pet(pet)
                .owner(titular)
                .role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now())
                .build());

        pet.setTutors(new ArrayList<>(List.of(vinculo)));
        return pet;
    }

    private Vaccine vacina(Pet pet, String nome, LocalDate proximaDose) {
        return vaccineRepository.save(Vaccine.builder()
                .pet(pet).vaccineName(nome)
                .applicationDate(LocalDate.now().minusYears(1))
                .nextDoseDate(proximaDose).build());
    }

    @Nested
    @DisplayName("escopo por dono")
    class EscopoPorDono {

        @Test
        @DisplayName("pets devem ser filtrados pelo dono, sem vazar os do outro")
        void petsFiltradosPeloDono() {
            var doUlysses = petRepository.findByTutorsOwnerOwnerId(ulysses.getOwnerId());

            assertThat(doUlysses).extracting(Pet::getName).containsExactly("Rex");
            assertThat(petRepository.findByTutorsOwnerOwnerId(maria.getOwnerId()))
                    .extracting(Pet::getName).containsExactly("Nina");
        }

        /**
         * O caso que so passa a existir depois da V15: o mesmo pet aparece para os
         * dois tutores, e nenhum dos dois alcanca o pet de terceiro. E a promessa
         * do passo 8 verificada contra o Postgres, e nao contra mock.
         */
        @Test
        @DisplayName("pet com dois tutores aparece para os dois, e para mais ninguem")
        void petCompartilhadoApareceParaOsDoisTutores() {
            petTutorRepository.save(PetTutor.builder()
                    .pet(rex).owner(maria).role(PetTutorRole.EDITOR)
                    .invitedBy(ulysses).creationDate(LocalDateTime.now()).build());

            assertThat(petRepository.findByTutorsOwnerOwnerId(ulysses.getOwnerId()))
                    .extracting(Pet::getName).containsExactly("Rex");
            assertThat(petRepository.findByTutorsOwnerOwnerId(maria.getOwnerId()))
                    .extracting(Pet::getName).containsExactlyInAnyOrder("Rex", "Nina");

            var estranho = ownerRepository.save(Owner.builder()
                    .name("Estranho").email("estranho-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash").build());
            assertThat(petRepository.findByTutorsOwnerOwnerId(estranho.getOwnerId())).isEmpty();
        }

        /** O co-tutor EDITOR ve a carteira do pet compartilhado, nao so o cadastro. */
        @Test
        @DisplayName("vacinas do pet compartilhado aparecem para o co-tutor")
        void vacinasDoPetCompartilhadoAparecemParaOCoTutor() {
            petTutorRepository.save(PetTutor.builder()
                    .pet(rex).owner(maria).role(PetTutorRole.EDITOR)
                    .invitedBy(ulysses).creationDate(LocalDateTime.now()).build());
            vacina(rex, "V10", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findByPetTutorsOwnerOwnerId(maria.getOwnerId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("vacinas devem ser filtradas pelo dono do pet")
        void vacinasFiltradasPeloDonoDoPet() {
            vacina(rex, "V10", LocalDate.now().plusDays(10));
            vacina(nina, "V8", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findByPetTutorsOwnerOwnerId(ulysses.getOwnerId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("historico deve ser filtrado pelo dono do pet, do mais recente ao mais antigo")
        void historicoFiltradoEOrdenado() {
            healthRecordRepository.save(HealthRecord.builder()
                    .pet(rex).eventType("Antiga").eventDate(LocalDate.now().minusDays(30)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .pet(rex).eventType("Recente").eventDate(LocalDate.now().minusDays(1)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .pet(nina).eventType("De outro dono").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findByPetTutorsOwnerOwnerIdOrderByEventDateDesc(ulysses.getOwnerId()))
                    .extracting(HealthRecord::getEventType)
                    .containsExactly("Recente", "Antiga");
        }

        @Test
        @DisplayName("historico por pet deve trazer so o daquele pet")
        void historicoPorPet() {
            healthRecordRepository.save(HealthRecord.builder()
                    .pet(rex).eventType("Consulta").eventDate(LocalDate.now()).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .pet(nina).eventType("Cirurgia").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(rex.getPetId()))
                    .extracting(HealthRecord::getEventType).containsExactly("Consulta");
        }

        @Test
        @DisplayName("vacinas por pet devem vir da mais recente para a mais antiga")
        void vacinasPorPetOrdenadas() {
            vaccineRepository.save(Vaccine.builder().pet(rex).vaccineName("Antiga")
                    .applicationDate(LocalDate.now().minusYears(2)).build());
            vaccineRepository.save(Vaccine.builder().pet(rex).vaccineName("Recente")
                    .applicationDate(LocalDate.now().minusDays(5)).build());

            assertThat(vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(rex.getPetId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("Recente", "Antiga");
        }
    }

    @Nested
    @DisplayName("acesso do veterinario")
    class AcessoDoVeterinario {

        @Test
        @DisplayName("concessao deve ser encontrada pelo par pet e clinica")
        void concessaoEncontradaPeloPar() {
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .pet(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());

            assertThat(petClinicAccessRepository
                    .findByPetPetIdAndClinicClinicId(rex.getPetId(), bichoFeliz.getClinicId()))
                    .isPresent();

            assertThat(petClinicAccessRepository
                    .findByPetPetIdAndClinicClinicId(nina.getPetId(), bichoFeliz.getClinicId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("a clinica deve enxergar so os pets com concessao ativa")
        void clinicaEnxergaApenasConcessoesAtivas() {
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .pet(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .pet(nina).clinic(bichoFeliz).grantedAt(LocalDateTime.now())
                    .revokedAt(LocalDateTime.now()).build());

            assertThat(petClinicAccessRepository
                    .findByClinicClinicIdAndRevokedAtIsNull(bichoFeliz.getClinicId()))
                    .extracting(a -> a.getPet().getName())
                    .containsExactly("Rex");
        }

        @Test
        @DisplayName("convites devem ser filtrados pela clinica")
        void convitesFiltradosPelaClinica() {
            clinicInviteRepository.save(br.com.petfy.healthcare.domain.entity.ClinicInvite.builder()
                    .clinic(bichoFeliz).createdBy(marina).tokenHash("hash-" + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(clinicInviteRepository
                    .findByClinicClinicIdOrderByCreationDateDesc(bichoFeliz.getClinicId()))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("busca por token e email")
    class BuscaPorTokenEEmail {

        @Test
        @DisplayName("link de compartilhamento deve ser encontrado pelo hash do token")
        void linkEncontradoPeloHash() {
            var hash = "hash-" + UUID.randomUUID();
            petShareRepository.save(PetShare.builder()
                    .pet(rex).tokenHash(hash)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(petShareRepository.findByTokenHash(hash)).isPresent();
            assertThat(petShareRepository.findByTokenHash("outro-hash")).isEmpty();
        }

        @Test
        @DisplayName("links devem ser filtrados pelo pet")
        void linksFiltradosPeloPet() {
            petShareRepository.save(PetShare.builder()
                    .pet(rex).tokenHash("hash-" + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(petShareRepository.findByPetOrderByCreationDateDesc(rex)).hasSize(1);
            assertThat(petShareRepository.findByPetOrderByCreationDateDesc(nina)).isEmpty();
        }

        @Test
        @DisplayName("tutor e veterinario devem ser encontrados por email - a base do login")
        void loginEncontraTutorEVeterinario() {
            assertThat(ownerRepository.findByEmail(ulysses.getEmail())).isPresent();
            assertThat(vetRepository.findByEmail(marina.getEmail())).isPresent();
            assertThat(vetRepository.existsByEmail(marina.getEmail())).isTrue();
            assertThat(ownerRepository.findByEmail("ninguem@petfy.com.br")).isEmpty();
        }
    }

    @Nested
    @DisplayName("agenda e rastro")
    class AgendaERastro {

        @Test
        @DisplayName("a rotina de lembretes deve pegar vencidas e as ate o limite, nao alem")
        void rotinaDeLembretesPegaAsCertas() {
            vacina(rex, "Vencida", LocalDate.now().minusDays(5));
            vacina(rex, "No limite", LocalDate.now().plusDays(30));
            vacina(rex, "Alem", LocalDate.now().plusDays(31));

            assertThat(vaccineRepository.findByNextDoseDateLessThanEqual(LocalDate.now().plusDays(30)))
                    .extracting(Vaccine::getVaccineName)
                    .containsExactlyInAnyOrder("Vencida", "No limite");
        }

        @Test
        @DisplayName("o rastro deve ser filtrado pela vacina, da correcao mais recente para a mais antiga")
        void rastroFiltradoEOrdenado() {
            var vacina = vacina(rex, "V10", LocalDate.now().plusDays(10));
            var outra = vacina(rex, "V8", LocalDate.now().plusDays(10));

            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(vacina).correctedByOwner(ulysses).previousVaccineName("Antiga")
                    .correctedAt(LocalDateTime.now().minusDays(2)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(vacina).correctedByVet(marina).previousVaccineName("Recente")
                    .correctedAt(LocalDateTime.now().minusHours(1)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(outra).correctedByOwner(ulysses).previousVaccineName("De outra vacina")
                    .correctedAt(LocalDateTime.now()).build());

            assertThat(vaccineCorrectionRepository
                    .findByVaccineVaccineIdOrderByCorrectedAtDesc(vacina.getVaccineId()))
                    .extracting(VaccineCorrection::getPreviousVaccineName)
                    .containsExactly("Recente", "Antiga");
        }
    }
}
