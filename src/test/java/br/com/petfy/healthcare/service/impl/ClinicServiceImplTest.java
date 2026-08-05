package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

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
class ClinicServiceImplTest {

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private CurrentVetProvider currentVetProvider;

    @InjectMocks
    private ClinicServiceImpl clinicService;

    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");

    /** Vet cuja clinica e a informada - quem tem permissao de manter o cadastro. */
    private void autenticadoComoVetDa(UUID clinicId) {
        when(currentVetProvider.require()).thenReturn(Vet.builder()
                .vetId(UUID.randomUUID())
                .clinic(Clinic.builder().clinicId(clinicId).build())
                .build());
    }

    private Clinic existingClinic() {
        return Clinic.builder()
                .clinicId(CLINIC_ID)
                .name("Clinica Bicho Feliz")
                .ownerVetName("Dra. Marina")
                .phone("1133334444")
                .email("contato@bichofeliz.com.br")
                .cnpj("12345678000199")
                .address("Av. Central, 500")
                .city("Sao Paulo")
                .state("SP")
                .cep("01000000")
                .description("Clinica geral")
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createClinic")
    class CreateClinic {

        @Test
        @DisplayName("criar clinica nao deve exigir ser veterinario - o tutor precisa registrar onde vacinou")
        void criarNaoDeveExigirSerVeterinario() {
            when(clinicRepository.save(any(Clinic.class))).thenReturn(existingClinic());

            clinicService.createClinic(ClinicRequestDTO.builder().name("Clinica Bicho Feliz").build());

            verify(currentVetProvider, never()).require();
        }

        @Test
        @DisplayName("deve persistir a clinica com os dados do request e data de criacao")
        void devePersistirClinica() {
            var request = ClinicRequestDTO.builder()
                    .name("Clinica Bicho Feliz")
                    .ownerVetName("Dra. Marina")
                    .cnpj("12345678000199")
                    .city("Sao Paulo")
                    .build();
            when(clinicRepository.save(any(Clinic.class))).thenReturn(existingClinic());

            var result = clinicService.createClinic(request);

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getName()).isEqualTo("Clinica Bicho Feliz");

            var captor = ArgumentCaptor.forClass(Clinic.class);
            verify(clinicRepository).save(captor.capture());
            assertThat(captor.getValue().getCnpj()).isEqualTo("12345678000199");
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getClinicById")
    class GetClinicById {

        @Test
        @DisplayName("deve retornar a clinica quando existe")
        void deveRetornarClinicaQuandoExiste() {
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingClinic()));

            var result = clinicService.getClinicById(CLINIC_ID);

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getOwnerVetName()).isEqualTo("Dra. Marina");
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicService.getClinicById(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(103, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listAllClinics")
    class ListAllClinics {

        @Test
        @DisplayName("deve mapear todas as clinicas retornadas pelo repositorio")
        void deveMapearTodasAsClinicas() {
            var pageable = PageRequest.of(0, 20);
            when(clinicRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(existingClinic())));

            assertThat(clinicService.listAllClinics(pageable).getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateClinic")
    class UpdateClinic {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingClinic()));
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicRepository.save(any(Clinic.class))).thenAnswer(i -> i.getArgument(0));

            var request = ClinicRequestDTO.builder().phone("1155556666").build();
            var result = clinicService.updateClinic(CLINIC_ID, request);

            assertThat(result.getPhone()).isEqualTo("1155556666");
            assertThat(result.getName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(result.getCnpj()).isEqualTo("12345678000199");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("deve responder 403 quando quem altera e vet de outra clinica")
        void deveResponder403QuandoVetDeOutraClinica() {
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingClinic()));
            autenticadoComoVetDa(OUTRA_CLINIC_ID);

            assertThatThrownBy(() -> clinicService.updateClinic(CLINIC_ID,
                    ClinicRequestDTO.builder().name("Invadida").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(109, HttpStatus.FORBIDDEN);

            verify(clinicRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicService.updateClinic(CLINIC_ID, ClinicRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(clinicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteClinic")
    class DeleteClinic {

        @Test
        @DisplayName("deve remover a clinica quando o vet e dela")
        void deveRemoverQuandoExiste() {
            when(clinicRepository.existsById(CLINIC_ID)).thenReturn(true);
            autenticadoComoVetDa(CLINIC_ID);

            clinicService.deleteClinic(CLINIC_ID);

            verify(clinicRepository).deleteById(CLINIC_ID);
        }

        @Test
        @DisplayName("deve responder 403 quando o vet e de outra clinica")
        void deveResponder403QuandoVetDeOutraClinica() {
            when(clinicRepository.existsById(CLINIC_ID)).thenReturn(true);
            autenticadoComoVetDa(OUTRA_CLINIC_ID);

            assertThatThrownBy(() -> clinicService.deleteClinic(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Only a vet from this clinic can do that")
                    .extracting("code", "httpStatus")
                    .containsExactly(109, HttpStatus.FORBIDDEN);

            verify(clinicRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(clinicRepository.existsById(CLINIC_ID)).thenReturn(false);

            assertThatThrownBy(() -> clinicService.deleteClinic(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(clinicRepository, never()).deleteById(any());
        }
    }
}
