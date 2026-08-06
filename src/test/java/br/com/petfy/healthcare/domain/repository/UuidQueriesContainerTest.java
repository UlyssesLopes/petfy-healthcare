package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
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

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private OrganizationInviteRepository organizationInviteRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;

    private Person ulysses;
    private Person maria;
    private Animal rex;
    private Animal nina;
    private Organization bichoFeliz;

    /**
     * Escopo minimo para as concessoes deste teste.
     *
     * Nao vazio de proposito: escopo vazio e um acesso que nao alcanca nada, e a
     * entidade trata isso como negacao. Um teste que gravasse concessao sem escopo
     * estaria exercitando um estado que o produto nao quer produzir.
     */
    private static java.util.Set<GrantScope> escopoCarteira() {
        return new java.util.LinkedHashSet<>(java.util.Set.of(GrantScope.CARTEIRA));
    }
    private Person marina;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.save(Person.builder()
                .name("Ulysses").email("ulysses-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());
        maria = personRepository.save(Person.builder()
                .name("Maria").email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());

        rex = animalComTitular("Rex", ulysses);
        nina = animalComTitular("Nina", maria);

        bichoFeliz = organizationRepository.save(Organization.builder().name("Clinica Bicho Feliz").build());

        marina = personRepository.save(Person.builder()
                .name("Dra. Marina").email("marina-" + UUID.randomUUID() + "@vet.com.br")
                .password("hash").organization(bichoFeliz).build());
    }

    /**
     * O animal e o vinculo sao gravados em duas chamadas, como em
     * {@code AnimalServiceImpl.createAnimal}: {@code Animal.tutors} e {@code mappedBy} sem
     * cascade, entao salvar o animal nao grava tutor nenhum. Montar o vinculo so em
     * memoria - o que a suite fazia enquanto {@code Animal} tinha {@code person} -
     * deixaria toda consulta por tutor voltando vazia, e o teste diria que o
     * escopo por dono nao traz nada quando o que falta e a linha no banco.
     */
    private Animal animalComTitular(String nome, Person titular) {
        Animal animal = animalRepository.save(Animal.builder().name(nome).species(Species.CANINA).build());

        Custody custodia = custodyRepository.save(Custody.builder()
                .animal(animal)
                .holderPerson(titular)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build());

        animal.setCustodies(new ArrayList<>(List.of(custodia)));
        return animal;
    }

    private Vaccine vacina(Animal animal, String nome, LocalDate proximaDose) {
        return vaccineRepository.save(Vaccine.builder()
                .animal(animal).vaccineName(nome)
                .applicationDate(LocalDate.now().minusYears(1))
                .nextDoseDate(proximaDose).build());
    }

    @Nested
    @DisplayName("escopo por dono")
    class EscopoPorDono {

        @Test
        @DisplayName("animals devem ser filtrados pelo dono, sem vazar os do outro")
        void animalsFiltradosPeloDono() {
            var doUlysses = animalRepository.findAlcancadosPor(ulysses.getPersonId(), java.time.LocalDateTime.now());

            assertThat(doUlysses).extracting(Animal::getName).containsExactly("Rex");
            assertThat(animalRepository.findAlcancadosPor(maria.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(Animal::getName).containsExactly("Nina");
        }

        /**
         * O caso que so passa a existir depois da V15: o mesmo animal aparece para os
         * dois tutores, e nenhum dos dois alcanca o animal de terceiro. E a promessa
         * do passo 8 verificada contra o Postgres, e nao contra mock.
         */
        @Test
        @DisplayName("animal com dois tutores aparece para os dois, e para mais ninguem")
        void animalCompartilhadoApareceParaOsDoisTutores() {
            // maria alcanca o Rex por concessao, nao por custodia: depois do P2b sao duas
            // tabelas, e a consulta de "meus animais" tem de cobrir as duas
            grantRepository.save(Grant.builder()
                    .animal(rex).granteePerson(maria).level(GrantLevel.EDITOR)
                    .scopes(escopoCarteira()).grantedBy(ulysses)
                    .grantedAt(LocalDateTime.now()).build());

            assertThat(animalRepository.findAlcancadosPor(ulysses.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(Animal::getName).containsExactly("Rex");
            assertThat(animalRepository.findAlcancadosPor(maria.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(Animal::getName).containsExactlyInAnyOrder("Rex", "Nina");

            var estranho = personRepository.save(Person.builder()
                    .name("Estranho").email("estranho-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash").build());
            assertThat(animalRepository.findAlcancadosPor(estranho.getPersonId(), java.time.LocalDateTime.now())).isEmpty();
        }

        /** O co-tutor EDITOR ve a carteira do animal compartilhado, nao so o cadastro. */
        @Test
        @DisplayName("vacinas do animal compartilhado aparecem para o co-tutor")
        void vacinasDoAnimalCompartilhadoAparecemParaOCoTutor() {
            // maria alcanca o Rex por concessao, nao por custodia: depois do P2b sao duas
            // tabelas, e a consulta de "meus animais" tem de cobrir as duas
            grantRepository.save(Grant.builder()
                    .animal(rex).granteePerson(maria).level(GrantLevel.EDITOR)
                    .scopes(escopoCarteira()).grantedBy(ulysses)
                    .grantedAt(LocalDateTime.now()).build());
            vacina(rex, "V10", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findAlcancadasPor(maria.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("vacinas devem ser filtradas pelo dono do animal")
        void vacinasFiltradasPeloDonoDoAnimal() {
            vacina(rex, "V10", LocalDate.now().plusDays(10));
            vacina(nina, "V8", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findAlcancadasPor(ulysses.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("historico deve ser filtrado pelo dono do animal, do mais recente ao mais antigo")
        void historicoFiltradoEOrdenado() {
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Antiga").eventDate(LocalDate.now().minusDays(30)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Recente").eventDate(LocalDate.now().minusDays(1)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(nina).eventType("De outro dono").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findAlcancadosPor(ulysses.getPersonId(), java.time.LocalDateTime.now()))
                    .extracting(HealthRecord::getEventType)
                    .containsExactly("Recente", "Antiga");
        }

        @Test
        @DisplayName("historico por animal deve trazer so o daquele animal")
        void historicoPorAnimal() {
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Consulta").eventDate(LocalDate.now()).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(nina).eventType("Cirurgia").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(rex.getAnimalId()))
                    .extracting(HealthRecord::getEventType).containsExactly("Consulta");
        }

        @Test
        @DisplayName("vacinas por animal devem vir da mais recente para a mais antiga")
        void vacinasPorAnimalOrdenadas() {
            vaccineRepository.save(Vaccine.builder().animal(rex).vaccineName("Antiga")
                    .applicationDate(LocalDate.now().minusYears(2)).build());
            vaccineRepository.save(Vaccine.builder().animal(rex).vaccineName("Recente")
                    .applicationDate(LocalDate.now().minusDays(5)).build());

            assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(rex.getAnimalId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("Recente", "Antiga");
        }
    }

    @Nested
    @DisplayName("acesso do veterinario")
    class AcessoDoVeterinario {

        @Test
        @DisplayName("concessao deve ser encontrada pelo par animal e clinica")
        void concessaoEncontradaPeloPar() {
            grantRepository.save(Grant.builder()
                    .animal(rex).granteeOrganization(bichoFeliz).level(GrantLevel.EDITOR).scopes(escopoCarteira()).grantedAt(LocalDateTime.now()).build());

            assertThat(grantRepository
                    .findVigenteDaClinicaNoAnimal(rex.getAnimalId(), bichoFeliz.getOrganizationId(), LocalDateTime.now()))
                    .isPresent();

            assertThat(grantRepository
                    .findVigenteDaClinicaNoAnimal(nina.getAnimalId(), bichoFeliz.getOrganizationId(), LocalDateTime.now()))
                    .isEmpty();
        }

        @Test
        @DisplayName("a clinica deve enxergar so os animals com concessao ativa")
        void clinicaEnxergaApenasConcessoesAtivas() {
            grantRepository.save(Grant.builder()
                    .animal(rex).granteeOrganization(bichoFeliz).level(GrantLevel.EDITOR).scopes(escopoCarteira()).grantedAt(LocalDateTime.now()).build());
            grantRepository.save(Grant.builder()
                    .animal(nina).granteeOrganization(bichoFeliz).level(GrantLevel.EDITOR).scopes(escopoCarteira()).grantedAt(LocalDateTime.now())
                    .revokedAt(LocalDateTime.now()).build());

            assertThat(grantRepository
                    .findVigentesDaClinica(bichoFeliz.getOrganizationId(), LocalDateTime.now()))
                    .extracting(a -> a.getAnimal().getName())
                    .containsExactly("Rex");
        }

        @Test
        @DisplayName("convites devem ser filtrados pela clinica")
        void convitesFiltradosPelaClinica() {
            organizationInviteRepository.save(br.com.petfy.healthcare.domain.entity.OrganizationInvite.builder()
                    .organization(bichoFeliz).createdBy(marina).tokenHash("hash-" + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());

            assertThat(organizationInviteRepository
                    .findByOrganizationOrganizationIdOrderByCreationDateDesc(bichoFeliz.getOrganizationId()))
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
            grantRepository.save(Grant.builder()
                    .animal(rex).tokenHash(hash).level(GrantLevel.VIEWER).scopes(escopoCarteira()).grantedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build());

            assertThat(grantRepository.findByTokenHash(hash)).isPresent();
            assertThat(grantRepository.findByTokenHash("outro-hash")).isEmpty();
        }

        @Test
        @DisplayName("links devem ser filtrados pelo animal")
        void linksFiltradosPeloAnimal() {
            grantRepository.save(Grant.builder()
                    .animal(rex).tokenHash("hash-" + UUID.randomUUID()).level(GrantLevel.VIEWER).scopes(escopoCarteira()).grantedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build());

            assertThat(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(rex.getAnimalId())).hasSize(1);
            assertThat(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(nina.getAnimalId())).isEmpty();
        }

        @Test
        @DisplayName("tutor e veterinario devem ser encontrados por email - a base do login")
        void loginEncontraTutorEVeterinario() {
            assertThat(personRepository.findByEmail(ulysses.getEmail())).isPresent();
            assertThat(personRepository.findByEmail(marina.getEmail())).isPresent();
            assertThat(personRepository.existsByEmail(marina.getEmail())).isTrue();
            assertThat(personRepository.findByEmail("ninguem@petfy.com.br")).isEmpty();
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
                    .vaccine(vacina).correctedBy(ulysses).previousVaccineName("Antiga")
                    .correctedAt(LocalDateTime.now().minusDays(2)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(vacina).correctedBy(marina).correctedInOrganization(marina.getOrganization()).previousVaccineName("Recente")
                    .correctedAt(LocalDateTime.now().minusHours(1)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(outra).correctedBy(ulysses).previousVaccineName("De outra vacina")
                    .correctedAt(LocalDateTime.now()).build());

            assertThat(vaccineCorrectionRepository
                    .findByVaccineVaccineIdOrderByCorrectedAtDesc(vacina.getVaccineId()))
                    .extracting(VaccineCorrection::getPreviousVaccineName)
                    .containsExactly("Recente", "Antiga");
        }
    }
}
