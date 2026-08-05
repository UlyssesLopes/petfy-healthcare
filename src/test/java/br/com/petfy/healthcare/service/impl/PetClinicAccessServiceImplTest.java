package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private PetRepository petRepository;

    @Mock
    private PetAccessGuard petAccessGuard;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private PetClinicAccessServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").build();
    }

    private Pet petDe(UUID ownerId) {
        return Pet.builder().petId(PET_ID).name("Rex").tutors(br.com.petfy.healthcare.PetTutores.titular(owner(ownerId))).build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    private PetClinicAccess acesso(LocalDateTime revokedAt) {
        return PetClinicAccess.builder()
                .petClinicAccessId(UUID.randomUUID())
                .pet(petDe(OWNER_ID))
                .clinic(clinic())
                .grantedAt(LocalDateTime.now().minusDays(5))
                .revokedAt(revokedAt)
                .build();
    }

    @Nested
    @DisplayName("grant")
    class Grant {

        @Test
        @DisplayName("deve conceder acesso da clinica ao pet")
        void deveConcederAcesso() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
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

            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(revogado));
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            assertThat(result.isActive()).isTrue();
            assertThat(result.getRevokedAt()).isNull();
            assertThat(result.getPetClinicAccessId()).isEqualTo(revogado.getPetClinicAccessId());
        }

        @Test
        @DisplayName("nao deve permitir conceder acesso a pet de outro dono")
        void naoDevePermitirConcederPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OUTRO_OWNER_ID));

            assertThatThrownBy(() -> service.grant(PET_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(petClinicAccessRepository);
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND quando a clinica nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
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
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(PET_ID))
                    .thenReturn(List.of(acesso(null), acesso(LocalDateTime.now())));

            var result = service.list(PET_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("active").containsExactly(true, false);
        }

        @Test
        @DisplayName("nao deve listar concessoes de pet de outro dono")
        void naoDeveListarDePetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OUTRO_OWNER_ID));

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

            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
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

            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(jaRevogado));

            service.revoke(PET_ID, CLINIC_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(petClinicAccessRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_ACCESS_NOT_FOUND quando nao ha concessao")
        void deveLancarQuandoNaoHaConcessao() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petClinicAccessRepository.findByPetPetIdAndClinicClinicId(PET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(PET_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(110, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("nao deve permitir revogar concessao de pet de outro dono")
        void naoDevePermitirRevogarDePetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OUTRO_OWNER_ID));

            assertThatThrownBy(() -> service.revoke(PET_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(petClinicAccessRepository);
        }
    }
}
