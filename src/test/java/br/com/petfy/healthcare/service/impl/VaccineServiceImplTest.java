package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.VaccineFactory;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineServiceImplTest {

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private VaccineCatalogRepository vaccineCatalogRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private AnimalAccessGuard animalAccessGuard;

    @Mock
    private VaccineCorrectionLog vaccineCorrectionLog;

    private VaccineServiceImpl vaccineService;

    @BeforeEach
    void setUp() {
        // factory e calculator reais: sao a regra que estes testes verificam,
        // entao mocka-los esvaziaria o teste. Construido aqui, e nao por
        // @InjectMocks, porque a factory depende de um mock que so existe agora
        vaccineService = new VaccineServiceImpl(vaccineRepository, organizationRepository,
                currentPersonProvider, animalAccessGuard, new VaccineStatusCalculator(),
                new VaccineFactory(vaccineCatalogRepository, vaccineRepository), vaccineCorrectionLog);
    }

    private static final UUID CATALOG_ID = UUID.fromString("a1000000-0000-4000-8000-000000000002");

    private VaccineCatalog catalogoV10() {
        return VaccineCatalog.builder()
                .vaccineCatalogId(CATALOG_ID)
                .code("V10")
                .name("V10 (Polivalente canina)")
                .species(Species.CANINA)
                .defaultIntervalDays(365)
                .build();
    }

    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Person person(UUID id) {
        return Person.builder().personId(id).email("ulysses@petfy.com.br").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .custodies(Custodias.titular(person(OWNER_ID))).build();
    }

    private Organization organization() {
        return Organization.builder().organizationId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private Vaccine vacina() {
        return Vaccine.builder()
                .vaccineId(VACCINE_ID)
                .animal(animal())
                .organization(organization())
                .vaccineName("Antirrabica")
                .applicationDate(LocalDate.of(2025, 6, 1))
                .nextDoseDate(LocalDate.of(2026, 6, 1))
                .description("Dose anual")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    private void autenticadoComo(UUID personId) {
        when(currentPersonProvider.require()).thenReturn(person(personId));
    }

    /** Se a pessoa autenticada chega ao animal da vacina - decisao do guard. */
    private void alcancaOAnimal(boolean alcanca) {
        when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(alcanca);
    }

    private PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    @Nested
    @DisplayName("createVaccine")
    class CreateVaccine {

        @Test
        @DisplayName("deve vincular a vacina ao animal e a clinica informados")
        void deveVincularAoAnimalEClinica() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.of(organization()));
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(vacina());

            var request = VaccineRequestDTO.builder()
                    .animalId(ANIMAL_ID).organizationId(CLINIC_ID).vaccineName("Antirrabica")
                    .applicationDate(LocalDate.of(2025, 6, 1)).build();

            var result = vaccineService.createVaccine(request);

            assertThat(result.getVaccineId()).isEqualTo(VACCINE_ID);
            assertThat(result.getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(result.getOrganizationId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve criar a vacina sem clinica quando organizationId nao e informado")
        void deveCriarSemClinica() {
            var semClinica = Vaccine.builder()
                    .vaccineId(VACCINE_ID).animal(animal()).vaccineName("Antirrabica").build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(semClinica);

            var result = vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).vaccineName("Antirrabica").build());

            assertThat(result.getOrganizationId()).isNull();
            verifyNoInteractions(organizationRepository);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getOrganization()).isNull();
        }

        @Test
        @DisplayName("deve calcular a proxima dose somando o intervalo do catalogo a data de aplicacao")
        void deveCalcularProximaDosePeloCatalogo() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogoV10()));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.createVaccine(VaccineRequestDTO.builder()
                    .animalId(ANIMAL_ID)
                    .vaccineCatalogId(CATALOG_ID)
                    .applicationDate(LocalDate.of(2025, 6, 1))
                    .build());

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getNextDoseDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("deve usar o nome do catalogo quando o request nao manda nome")
        void deveUsarNomeDoCatalogo() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogoV10()));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.createVaccine(VaccineRequestDTO.builder()
                    .animalId(ANIMAL_ID).vaccineCatalogId(CATALOG_ID)
                    .applicationDate(LocalDate.of(2025, 6, 1)).build());

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getVaccineName()).isEqualTo("V10 (Polivalente canina)");
            assertThat(captor.getValue().getCatalog().getVaccineCatalogId()).isEqualTo(CATALOG_ID);
        }

        @Test
        @DisplayName("data explicita deve vencer o calculo do catalogo - o vet pode orientar diferente")
        void dataExplicitaDeveVencerOCatalogo() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogoV10()));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.createVaccine(VaccineRequestDTO.builder()
                    .animalId(ANIMAL_ID).vaccineCatalogId(CATALOG_ID)
                    .applicationDate(LocalDate.of(2025, 6, 1))
                    .nextDoseDate(LocalDate.of(2025, 12, 1))
                    .build());

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getNextDoseDate()).isEqualTo(LocalDate.of(2025, 12, 1));
        }

        @Test
        @DisplayName("deve aceitar vacina em texto livre, sem catalogo")
        void deveAceitarVacinaEmTextoLivre() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.createVaccine(VaccineRequestDTO.builder()
                    .animalId(ANIMAL_ID).vaccineName("Vacina importada").build());

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getVaccineName()).isEqualTo("Vacina importada");
            assertThat(captor.getValue().getCatalog()).isNull();
            assertThat(captor.getValue().getNextDoseDate()).isNull();
        }

        @Test
        @DisplayName("deve recusar com 409 quando o catalogo e de especie diferente do animal")
        void deveRecusarQuandoEspecieNaoBate() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(
                    Animal.builder().animalId(ANIMAL_ID).name("Mia").species(Species.FELINA)
                            .custodies(Custodias.titular(person(OWNER_ID))).build());
            when(vaccineCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogoV10()));  // CANINA

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).vaccineCatalogId(CATALOG_ID)
                            .applicationDate(LocalDate.of(2025, 6, 1)).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(vaccineRepository, never()).save(any(Vaccine.class));
        }

        @Test
        @DisplayName("deve recusar com 400 quando nao ha nem nome nem catalogo")
        void deveRecusarSemNomeESemCatalogo() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar VACCINE_CATALOG_NOT_FOUND quando o catalogo informado nao existe")
        void deveLancarQuandoCatalogoNaoExiste() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).vaccineCatalogId(CATALOG_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine catalog entry not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(106, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }

        /**
         * Animal inalcancavel e animal inexistente respondem igual, e a decisao e do
         * guard. O que cabe ao servico e nao gravar nada quando ele recusa.
         */
        @Test
        @DisplayName("recusa do guard sobe intacta e nada e salvo")
        void recusaDoGuardNaoSalva() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).vaccineName("Antirrabica").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).organizationId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Organization not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(103, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateVaccine")
    class UpdateVaccine {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(true);
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = vaccineService.updateVaccine(VACCINE_ID,
                    VaccineRequestDTO.builder().description("Reforco").build());

            assertThat(result.getDescription()).isEqualTo("Reforco");
            assertThat(result.getVaccineName()).isEqualTo("Antirrabica");
            assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        /** O rastro sai antes dos setters, com quem corrigiu. */
        @Test
        @DisplayName("registra a correcao em nome de quem esta autenticado")
        void registraACorrecaoEmNomeDeQuemCorrige() {
            var existente = vacina();
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(existente));
            alcancaOAnimal(true);
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.updateVaccine(VACCINE_ID, VaccineRequestDTO.builder().description("Reforco").build());

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(vaccineCorrectionLog).recordByPerson(same(existente), captor.capture());
            assertThat(captor.getValue().getPersonId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("vacina de animal fora do alcance nao e alterada")
        void vacinaForaDoAlcanceNaoEAlterada() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> vaccineService.updateVaccine(VACCINE_ID,
                    VaccineRequestDTO.builder().description("Invadido").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).save(any());
            verifyNoInteractions(vaccineCorrectionLog);
        }

        @Test
        @DisplayName("deve lancar VACCINE_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.updateVaccine(VACCINE_ID, VaccineRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(104, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getVaccineById")
    class GetVaccineById {

        @Test
        @DisplayName("deve retornar a vacina quando o animal e alcancavel")
        void deveRetornarQuandoAlcancavel() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(true);

            assertThat(vaccineService.getVaccineById(VACCINE_ID).getVaccineName()).isEqualTo("Antirrabica");
        }

        @Test
        @DisplayName("vacina de animal fora do alcance responde VACCINE_NOT_FOUND")
        void vacinaForaDoAlcanceResponde404() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> vaccineService.getVaccineById(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(104, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listCorrections")
    class ListCorrections {

        @Test
        @DisplayName("deve devolver o rastro da vacina alcancavel")
        void deveDevolverRastroDaVacinaAlcancavel() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(true);
            when(vaccineCorrectionLog.list(VACCINE_ID)).thenReturn(List.of());

            assertThat(vaccineService.listCorrections(VACCINE_ID)).isEmpty();
            verify(vaccineCorrectionLog).list(VACCINE_ID);
        }

        @Test
        @DisplayName("nao deve devolver o rastro de vacina fora do alcance")
        void naoDeveDevolverRastroForaDoAlcance() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> vaccineService.listCorrections(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineCorrectionLog, never()).list(any());
        }
    }

    @Nested
    @DisplayName("listAllVaccines")
    class ListAllVaccines {

        @Test
        @DisplayName("deve listar apenas as vacinas dos animals em que a pessoa e tutora")
        void deveListarApenasDosAnimalsEmQueETutora() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(0, 20);
            when(vaccineRepository.findAlcancadasPor(eq(OWNER_ID), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(vacina())));

            assertThat(vaccineService.listAllVaccines(pageable).getContent()).hasSize(1);
            verify(vaccineRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("deleteVaccine")
    class DeleteVaccine {

        @Test
        @DisplayName("deve remover a vacina alcancavel")
        void deveRemoverAAlcancavel() {
            var vaccine = vacina();
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vaccine));
            alcancaOAnimal(true);

            vaccineService.deleteVaccine(VACCINE_ID);

            verify(vaccineRepository).delete(vaccine);
        }

        @Test
        @DisplayName("vacina de animal fora do alcance nao e removida")
        void vacinaForaDoAlcanceNaoERemovida() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> vaccineService.deleteVaccine(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).delete(any());
        }
    }

    /**
     * O nivel que cada operacao exige do guard. Registrar vacina e escrita;
     * ler, corrigir e apagar a vacina passam por {@code alcanca}, porque quem
     * chega ao animal chega a carteira dele.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("registrar vacina exige escrita")
        void registrarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.createVaccine(
                    VaccineRequestDTO.builder().animalId(ANIMAL_ID).vaccineName("Antirrabica").build());

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("mover a vacina para outro animal exige escrita no destino")
        void moverExigeEscritaNoDestino() {
            var destinoId = UUID.fromString("77777777-7777-7777-7777-777777777777");
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina()));
            alcancaOAnimal(true);
            when(animalAccessGuard.requireEscrita(destinoId)).thenReturn(Animal.builder()
                    .animalId(destinoId).name("Bob").species(Species.CANINA)
                    .custodies(Custodias.titular(person(OWNER_ID))).build());
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            vaccineService.updateVaccine(VACCINE_ID, VaccineRequestDTO.builder().animalId(destinoId).build());

            verify(animalAccessGuard).requireEscrita(destinoId);
        }

        /**
         * A agenda e a listagem geral nao passam pelo guard: filtram pela consulta
         * por tutor, entao animal de terceiro nunca entra no conjunto.
         */
        @Test
        @DisplayName("a listagem geral filtra pela consulta, sem consultar o guard")
        void listagemGeralNaoConsultaOGuard() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(0, 20);
            when(vaccineRepository.findAlcancadasPor(eq(OWNER_ID), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            vaccineService.listAllVaccines(pageable);

            verifyNoInteractions(animalAccessGuard);
        }
    }
}
