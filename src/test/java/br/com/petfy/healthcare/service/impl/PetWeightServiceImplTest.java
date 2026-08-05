package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.PetWeightRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetWeightHistoryRepository;
import br.com.petfy.healthcare.security.PetAccessGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetWeightServiceImplTest {

    @Mock
    private PetWeightHistoryRepository weightHistoryRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetAccessGuard petAccessGuard;

    @InjectMocks
    private PetWeightServiceImpl petWeightService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Pet pet(Double pesoAtual) {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .species(Species.CANINA)
                .weight(pesoAtual)
                .tutors(PetTutores.titular(owner(OWNER_ID)))
                .build();
    }

    private PetWeightHistory medicao(Pet pet, double peso, LocalDate quando) {
        return PetWeightHistory.builder()
                .weightHistoryId(UUID.randomUUID())
                .pet(pet)
                .weight(peso)
                .measuredAt(quando)
                .creationDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();
    }


    @Nested
    @DisplayName("addWeight")
    class AddWeight {

        @Test
        @DisplayName("grava a medicao vinculada ao pet")
        void gravaMedicao() {

            var pet = pet(12.5);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet);
            when(weightHistoryRepository.save(any(PetWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByPetPetIdOrderByMeasuredAtDesc(PET_ID))
                    .thenReturn(Optional.of(medicao(pet, 14.0, LocalDate.of(2026, 3, 10))));

            var request = PetWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .note("consulta de rotina")
                    .build();

            var result = petWeightService.addWeight(PET_ID, request);

            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getWeight()).isEqualTo(14.0);
            assertThat(result.getNote()).isEqualTo("consulta de rotina");

            var captor = ArgumentCaptor.forClass(PetWeightHistory.class);
            verify(weightHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("a medicao mais recente vira o espelho em Pet.weight")
        void medicaoMaisRecenteAtualizaOEspelho() {

            var pet = pet(12.5);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet);
            when(weightHistoryRepository.save(any(PetWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByPetPetIdOrderByMeasuredAtDesc(PET_ID))
                    .thenReturn(Optional.of(medicao(pet, 14.0, LocalDate.of(2026, 3, 10))));

            petWeightService.addWeight(PET_ID, PetWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .build());

            var captor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(captor.capture());
            assertThat(captor.getValue().getWeight()).isEqualTo(14.0);
        }

        @Test
        @DisplayName("medicao historica antiga nao faz o espelho regredir")
        void medicaoAntigaNaoRegrideOEspelho() {

            var pet = pet(14.0);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet);
            when(weightHistoryRepository.save(any(PetWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            // o tutor lancou uma pesagem esquecida de janeiro; a de marco continua
            // sendo a mais recente
            when(weightHistoryRepository.findFirstByPetPetIdOrderByMeasuredAtDesc(PET_ID))
                    .thenReturn(Optional.of(medicao(pet, 14.0, LocalDate.of(2026, 3, 10))));

            petWeightService.addWeight(PET_ID, PetWeightRequestDTO.builder()
                    .weight(9.0)
                    .measuredAt(LocalDate.of(2026, 1, 5))
                    .build());

            var captor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(captor.capture());
            assertThat(captor.getValue().getWeight()).isEqualTo(14.0);
        }
    }

    @Nested
    @DisplayName("listWeights")
    class ListWeights {

        @Test
        @DisplayName("devolve a serie da mais recente para a mais antiga")
        void devolveSerieOrdenada() {

            var pet = pet(14.0);
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet);
            when(weightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(PET_ID))
                    .thenReturn(List.of(
                            medicao(pet, 14.0, LocalDate.of(2026, 3, 10)),
                            medicao(pet, 9.0, LocalDate.of(2026, 1, 5))));

            var result = petWeightService.listWeights(PET_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getWeight()).isEqualTo(14.0);
            assertThat(result.get(1).getWeight()).isEqualTo(9.0);
        }
    }

    /**
     * O nivel que cada operacao exige do guard - ver
     * {@link br.com.petfy.healthcare.security.PetAccessGuardTest} para o que o
     * guard faz com esse nivel.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("registrar pesagem exige escrita")
        void registrarExigeEscrita() {

            var pet = pet(12.5);
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet);
            when(weightHistoryRepository.save(any(PetWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByPetPetIdOrderByMeasuredAtDesc(PET_ID))
                    .thenReturn(Optional.empty());

            petWeightService.addWeight(PET_ID, PetWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .build());

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Acompanhar a curva de peso e leitura: a avo que so olha a carteira ve a
         * serie sem poder lancar pesagem.
         */
        @Test
        @DisplayName("listar a serie exige so leitura")
        void listarExigeSoLeitura() {

            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet(14.0));
            when(weightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(PET_ID)).thenReturn(List.of());

            petWeightService.listWeights(PET_ID);

            verify(petAccessGuard).requireLeitura(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
        }
    }

}
