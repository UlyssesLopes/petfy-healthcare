package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.VaccineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetPetServiceImplTest {

    @Mock
    private PetClinicAccessRepository petClinicAccessRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private CurrentVetProvider currentVetProvider;

    @Mock
    private VaccineCatalogRepository vaccineCatalogRepository;

    private VetPetServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");

    @BeforeEach
    void setUp() {
        // factory real: o que interessa aqui e que a vacina saia carimbada com a
        // clinica certa, e nao repetir a regra de montagem num mock
        service = new VetPetServiceImpl(petClinicAccessRepository, vaccineRepository,
                currentVetProvider, new VaccineFactory(vaccineCatalogRepository));
    }

    private Clinic clinic(UUID id) {
        return Clinic.builder().clinicId(id).name("Clinica Bicho Feliz").build();
    }

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").type("Cachorro").weight(12.5)
                .owner(Owner.builder().ownerId(UUID.randomUUID()).name("Ulysses")
                        .email("ulysses@petfy.com.br").phone("11999999999").build())
                .build();
    }

    private PetClinicAccess acesso(LocalDateTime revokedAt) {
        return PetClinicAccess.builder()
                .petClinicAccessId(UUID.randomUUID())
                .pet(pet())
                .clinic(clinic(CLINIC_ID))
                .grantedAt(LocalDateTime.now().minusDays(3))
                .revokedAt(revokedAt)
                .build();
    }

    private void vetDaClinica(UUID clinicId) {
        when(currentVetProvider.require()).thenReturn(Vet.builder()
                .vetId(UUID.randomUUID()).name("Dra. Marina").clinic(clinic(clinicId)).build());
    }

    @Nested
    @DisplayName("listAccessiblePets")
    class ListAccessiblePets {

        @Test
        @DisplayName("deve listar apenas os pets com concessao ativa para a clinica do vet")
        void deveListarPetsComConcessaoAtiva() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(CLINIC_ID))
                    .thenReturn(List.of(acesso(null)));

            var result = service.listAccessiblePets();

            assertThat(result).singleElement().satisfies(p -> {
                assertThat(p.getPetId()).isEqualTo(PET_ID);
                assertThat(p.getName()).isEqualTo("Rex");
                assertThat(p.getOwnerName()).isEqualTo("Ulysses");
                assertThat(p.getAccessGrantedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("nao deve expor os dados de contato do tutor")
        void naoDeveExporContatoDoTutor() {
            assertThat(VetPetDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("ownerName")
                    .doesNotContain("ownerEmail", "ownerPhone", "ownerAddress", "ownerId");
        }

        @Test
        @DisplayName("deve devolver lista vazia quando a clinica nao foi autorizada por ninguem")
        void deveDevolverListaVaziaSemAutorizacoes() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(CLINIC_ID))
                    .thenReturn(List.of());

            assertThat(service.listAccessiblePets()).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerVaccine")
    class RegisterVaccine {

        private VaccineRequestDTO request() {
            return VaccineRequestDTO.builder()
                    .vaccineName("V10")
                    .applicationDate(LocalDate.of(2026, 8, 1))
                    .build();
        }

        @Test
        @DisplayName("deve registrar a vacina no pet autorizado")
        void deveRegistrarVacinaNoPetAutorizado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.registerVaccine(PET_ID, request());

            assertThat(result.getVaccineName()).isEqualTo("V10");
            assertThat(result.getPetId()).isEqualTo(PET_ID);
        }

        @Test
        @DisplayName("deve carimbar a vacina com a clinica do vet, e nao com a do payload")
        void deveCarimbarComAClinicaDoVet() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var comOutraClinicaNoCorpo = VaccineRequestDTO.builder()
                    .vaccineName("V10")
                    .clinicId(OUTRA_CLINIC_ID)
                    .applicationDate(LocalDate.of(2026, 8, 1))
                    .build();

            service.registerVaccine(PET_ID, comOutraClinicaNoCorpo);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getClinic().getClinicId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve usar o petId do path, ignorando o que vier no corpo")
        void deveUsarPetIdDoPath() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var comOutroPetNoCorpo = VaccineRequestDTO.builder()
                    .petId(UUID.randomUUID())
                    .vaccineName("V10")
                    .build();

            service.registerVaccine(PET_ID, comOutroPetNoCorpo);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getPet().getPetId()).isEqualTo(PET_ID);
        }

        @Test
        @DisplayName("nao deve registrar em pet sem concessao para a clinica do vet")
        void naoDeveRegistrarSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerVaccine(PET_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve registrar quando a concessao foi revogada")
        void naoDeveRegistrarComConcessaoRevogada() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(LocalDateTime.now().minusDays(1))));

            assertThatThrownBy(() -> service.registerVaccine(PET_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listVaccines")
    class ListVaccines {

        @Test
        @DisplayName("deve listar as vacinas do pet autorizado")
        void deveListarVacinasDoPetAutorizado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(PET_ID))
                    .thenReturn(List.of(Vaccine.builder()
                            .vaccineId(UUID.randomUUID()).vaccineName("V10").pet(pet()).build()));

            assertThat(service.listVaccines(PET_ID)).hasSize(1);
        }

        @Test
        @DisplayName("nao deve expor o historico de pet sem concessao")
        void naoDeveExporHistoricoSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listVaccines(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(vaccineRepository, never()).findByPetPetIdOrderByApplicationDateDesc(any());
        }
    }
}
