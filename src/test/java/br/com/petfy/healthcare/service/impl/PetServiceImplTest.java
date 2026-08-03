package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
class PetServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private PetServiceImpl petService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Pet existingPet() {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .type("Cachorro")
                .breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15))
                .weight(12.5)
                .gender("Macho")
                .owner(owner(OWNER_ID))
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    private PetRequestDTO request() {
        return PetRequestDTO.builder()
                .name("Rex")
                .type("Cachorro")
                .breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15))
                .weight(12.5)
                .gender("Macho")
                .ownerId(OWNER_ID)
                .build();
    }

    @Nested
    @DisplayName("createPet")
    class CreatePet {

        @Test
        @DisplayName("deve vincular o pet ao owner informado")
        void deveVincularPetAoOwner() {
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner(OWNER_ID)));
            when(petRepository.save(any(Pet.class))).thenReturn(existingPet());

            var result = petService.createPet(request());

            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);

            var captor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(captor.capture());
            assertThat(captor.getValue().getOwner().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("deve lancar OWNER_NOT_FOUND sem salvar quando o owner nao existe")
        void deveLancarQuandoOwnerNaoExiste() {
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.createPet(request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Owner not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(101, HttpStatus.NOT_FOUND);

            verify(petRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPetById")
    class GetPetById {

        @Test
        @DisplayName("deve retornar o pet quando existe")
        void deveRetornarPetQuandoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(existingPet()));

            var result = petService.getPetById(PET_ID);

            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getName()).isEqualTo("Rex");
            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.getPetById(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listAllPets")
    class ListAllPets {

        @Test
        @DisplayName("deve mapear todos os pets retornados pelo repositorio")
        void deveMapearTodosOsPets() {
            when(petRepository.findAll()).thenReturn(List.of(existingPet()));

            var result = petService.listAllPets();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Rex");
        }
    }

    @Nested
    @DisplayName("updatePet")
    class UpdatePet {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(existingPet()));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            var request = PetRequestDTO.builder().weight(14.0).build();
            var result = petService.updatePet(PET_ID, request);

            assertThat(result.getWeight()).isEqualTo(14.0);
            assertThat(result.getName()).isEqualTo("Rex");
            assertThat(result.getBreed()).isEqualTo("Vira-lata");
            assertThat(result.getBornDate()).isEqualTo(LocalDate.of(2021, 3, 15));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("deve transferir o pet quando um novo owner e informado")
        void deveTransferirPetParaNovoOwner() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(existingPet()));
            when(ownerRepository.findById(OUTRO_OWNER_ID)).thenReturn(Optional.of(owner(OUTRO_OWNER_ID)));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            var result = petService.updatePet(PET_ID, PetRequestDTO.builder().ownerId(OUTRO_OWNER_ID).build());

            assertThat(result.getOwnerId()).isEqualTo(OUTRO_OWNER_ID);
        }

        @Test
        @DisplayName("deve lancar OWNER_NOT_FOUND sem salvar quando o novo owner nao existe")
        void deveLancarQuandoNovoOwnerNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(existingPet()));
            when(ownerRepository.findById(OUTRO_OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.updatePet(PET_ID, PetRequestDTO.builder().ownerId(OUTRO_OWNER_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Owner not found");

            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem salvar quando o pet nao existe")
        void deveLancarQuandoPetNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.updatePet(PET_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(petRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deletePet")
    class DeletePet {

        @Test
        @DisplayName("deve remover o pet quando existe")
        void deveRemoverQuandoExiste() {
            var pet = existingPet();
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

            petService.deletePet(PET_ID);

            verify(petRepository).delete(pet);
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.deletePet(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(petRepository, never()).delete(any());
        }
    }
}
