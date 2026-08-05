package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetClinicAccessServiceImplTest {

    @Mock
    private PetClinicAccessRepository petClinicAccessRepository;

    @Mock
    private PetAccessGuard petAccessGuard;

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private PetClinicAccessServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").build();
    }

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").tutors(PetTutores.titular(owner(OWNER_ID))).build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private PetClinicAccess acesso(LocalDateTime revokedAt) {
        return PetClinicAccess.builder()
                .petClinicAccessId(UUID.randomUUID())
                .pet(pet())
                .clinic(clinic())
                .grantedAt(LocalDateTime.now().minusDays(5))
                .revokedAt(revokedAt)
                .build();
    }

    private PetfyHealthcareException petNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    @Nested
    @DisplayName("grant")
    class Grant {

        @Test
        @DisplayName("deve conceder acesso da clinica ao pet")
        void deveConcederAcesso() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getClinicName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getGrantedAt()).isNotNull();
        }

        @Test
        @DisplayName("reconceder deve reativar a concessao revogada, e nao criar outra")
        void reconcederDeveReativar() {
            var revogado = acesso(LocalDateTime.now().minusDays(1));

            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(revogado));
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            assertThat(result.isActive()).isTrue();
            assertThat(result.getRevokedAt()).isNull();
            assertThat(result.getPetClinicAccessId()).isEqualTo(revogado.getPetClinicAccessId());
        }

        /** A recusa do guard tem de vir antes de qualquer escrita na concessao. */
        @Test
        @DisplayName("recusa do guard impede a concessao")
        void recusaDoGuardNaoConcede() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenThrow(petNaoEncontrado());

            assertThatThrownBy(() -> service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(petClinicAccessRepository, clinicRepository);
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND quando a clinica nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(petClinicAccessRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("list")
    class List_ {

        @Test
        @DisplayName("deve listar as concessoes do pet, ativas e revogadas")
        void deveListarConcessoes() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(PET_ID))
                    .thenReturn(List.of(acesso(null), acesso(LocalDateTime.now())));

            var result = service.list(PET_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("active").containsExactly(true, false);
        }

        @Test
        @DisplayName("recusa do guard impede a listagem")
        void recusaDoGuardNaoLista() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenThrow(petNaoEncontrado());

            assertThatThrownBy(() -> service.list(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(petClinicAccessRepository);
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("deve marcar a data de revogacao")
        void deveMarcarDataDeRevogacao() {
            var ativo = acesso(null);

            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(ativo));

            service.revoke(PET_ID, CLINIC_ID);

            assertThat(ativo.getRevokedAt()).isNotNull();
            assertThat(ativo.isActive()).isFalse();
            verify(petClinicAccessRepository).save(ativo);
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaData() {
            var original = LocalDateTime.now().minusDays(2);
            var jaRevogado = acesso(original);

            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(jaRevogado));

            service.revoke(PET_ID, CLINIC_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(petClinicAccessRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_ACCESS_NOT_FOUND quando nao ha concessao")
        void deveLancarQuandoNaoHaConcessao() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(PET_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(110, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("recusa do guard impede a revogacao")
        void recusaDoGuardNaoRevoga() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenThrow(petNaoEncontrado());

            assertThatThrownBy(() -> service.revoke(PET_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(petClinicAccessRepository);
        }
    }

    /**
     * Decidir que clinica ve o pet e escrita nos tres casos, inclusive na
     * listagem: a lista de quem tem acesso e informacao de gestao, e nao parte da
     * carteira que o tutor VIEWER acompanha.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("conceder acesso exige escrita")
        void concederExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar as concessoes exige escrita")
        void listarExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(PET_ID)).thenReturn(List.of());

            service.list(PET_ID);

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("revogar acesso exige escrita")
        void revogarExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));

            service.revoke(PET_ID, CLINIC_ID);

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }
    }
}
