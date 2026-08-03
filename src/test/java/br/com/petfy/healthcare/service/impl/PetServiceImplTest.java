package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
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
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private PetServiceImpl petService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Pet petDe(UUID ownerId) {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .type("Cachorro")
                .breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15))
                .weight(12.5)
                .gender("Macho")
                .owner(owner(ownerId))
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
                .build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    @Nested
    @DisplayName("createPet")
    class CreatePet {

        @Test
        @DisplayName("deve vincular o pet ao owner autenticado")
        void deveVincularPetAoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            when(petRepository.save(any(Pet.class))).thenReturn(petDe(OWNER_ID));

            var result = petService.createPet(request());

            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);

            var captor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(captor.capture());
            assertThat(captor.getValue().getOwner().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("o request nao deve conseguir escolher o dono do pet")
        void requestNaoDeveEscolherODono() {
            assertThat(PetRequestDTO.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("ownerId");
        }
    }

    @Nested
    @DisplayName("getPetById")
    class GetPetById {

        @Test
        @DisplayName("deve retornar o pet quando pertence ao owner autenticado")
        void deveRetornarPetDoProprioOwner() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));

            var result = petService.getPetById(PET_ID);

            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getName()).isEqualTo("Rex");
        }

        @Test
        @DisplayName("deve responder PET_NOT_FOUND para pet de outro dono, sem revelar que existe")
        void deveResponderNotFoundParaPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> petService.getPetById(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND quando o pet nao existe")
        void deveLancarQuandoNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.getPetById(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");
        }
    }

    @Nested
    @DisplayName("listAllPets")
    class ListAllPets {

        @Test
        @DisplayName("deve listar apenas os pets do owner autenticado")
        void deveListarApenasPetsDoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findByOwnerOwnerId(OWNER_ID)).thenReturn(List.of(petDe(OWNER_ID)));

            var result = petService.listAllPets();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwnerId()).isEqualTo(OWNER_ID);
            verify(petRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("updatePet")
    class UpdatePet {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            var result = petService.updatePet(PET_ID, PetRequestDTO.builder().weight(14.0).build());

            assertThat(result.getWeight()).isEqualTo(14.0);
            assertThat(result.getName()).isEqualTo("Rex");
            assertThat(result.getBreed()).isEqualTo("Vira-lata");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve permitir alterar pet de outro dono")
        void naoDevePermitirAlterarPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> petService.updatePet(PET_ID, PetRequestDTO.builder().name("Invadido").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(petRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deletePet")
    class DeletePet {

        @Test
        @DisplayName("deve remover o pet do proprio owner")
        void deveRemoverPetDoProprioOwner() {
            var pet = petDe(OWNER_ID);
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

            petService.deletePet(PET_ID);

            verify(petRepository).delete(pet);
        }

        @Test
        @DisplayName("nao deve permitir remover pet de outro dono")
        void naoDevePermitirRemoverPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> petService.deletePet(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(petRepository, never()).delete(any());
        }
    }
}
