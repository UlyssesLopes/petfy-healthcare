package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
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
    private AnimalAccessGuard animalAccessGuard;

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private PetClinicAccessServiceImpl service;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").tutors(PetTutores.titular(owner(OWNER_ID))).build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private PetClinicAccess acesso(LocalDateTime revokedAt) {
        return PetClinicAccess.builder()
                .petClinicAccessId(UUID.randomUUID())
                .animal(animal())
                .clinic(clinic())
                .grantedAt(LocalDateTime.now().minusDays(5))
                .revokedAt(revokedAt)
                .build();
    }

    private PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    @Nested
    @DisplayName("grant")
    class Grant {

        @Test
        @DisplayName("deve conceder acesso da clinica ao animal")
        void deveConcederAcesso() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.grant(ANIMAL_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getClinicName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getGrantedAt()).isNotNull();
        }

        @Test
        @DisplayName("reconceder deve reativar a concessao revogada, e nao criar outra")
        void reconcederDeveReativar() {
            var revogado = acesso(LocalDateTime.now().minusDays(1));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(revogado));
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.grant(ANIMAL_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            assertThat(result.isActive()).isTrue();
            assertThat(result.getRevokedAt()).isNull();
            assertThat(result.getPetClinicAccessId()).isEqualTo(revogado.getPetClinicAccessId());
        }

        /** A recusa do guard tem de vir antes de qualquer escrita na concessao. */
        @Test
        @DisplayName("recusa do guard impede a concessao")
        void recusaDoGuardNaoConcede() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> service.grant(ANIMAL_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verifyNoInteractions(petClinicAccessRepository, clinicRepository);
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND quando a clinica nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.grant(ANIMAL_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(petClinicAccessRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("list")
    class List_ {

        @Test
        @DisplayName("deve listar as concessoes do animal, ativas e revogadas")
        void deveListarConcessoes() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID))
                    .thenReturn(List.of(acesso(null), acesso(LocalDateTime.now())));

            var result = service.list(ANIMAL_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("active").containsExactly(true, false);
        }

        @Test
        @DisplayName("recusa do guard impede a listagem")
        void recusaDoGuardNaoLista() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> service.list(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

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

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(ativo));

            service.revoke(ANIMAL_ID, CLINIC_ID);

            assertThat(ativo.getRevokedAt()).isNotNull();
            assertThat(ativo.isActive()).isFalse();
            verify(petClinicAccessRepository).save(ativo);
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaData() {
            var original = LocalDateTime.now().minusDays(2);
            var jaRevogado = acesso(original);

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(jaRevogado));

            service.revoke(ANIMAL_ID, CLINIC_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(petClinicAccessRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_ACCESS_NOT_FOUND quando nao ha concessao")
        void deveLancarQuandoNaoHaConcessao() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(ANIMAL_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(110, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("recusa do guard impede a revogacao")
        void recusaDoGuardNaoRevoga() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> service.revoke(ANIMAL_ID, CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verifyNoInteractions(petClinicAccessRepository);
        }
    }

    /**
     * Decidir que clinica ve o animal e escrita nos tres casos, inclusive na
     * listagem: a lista de quem tem acesso e informacao de gestao, e nao parte da
     * carteira que o tutor VIEWER acompanha.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("conceder acesso exige escrita")
        void concederExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());
            when(petClinicAccessRepository.save(any(PetClinicAccess.class))).thenAnswer(i -> i.getArgument(0));

            service.grant(ANIMAL_ID, ClinicAccessRequestDTO.builder().clinicId(CLINIC_ID).build());

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar as concessoes exige escrita")
        void listarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of());

            service.list(ANIMAL_ID);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("revogar acesso exige escrita")
        void revogarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));

            service.revoke(ANIMAL_ID, CLINIC_ID);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }
    }
}
