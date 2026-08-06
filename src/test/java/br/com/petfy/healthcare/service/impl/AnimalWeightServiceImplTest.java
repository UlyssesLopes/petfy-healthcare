package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.AnimalWeightRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AnimalWeightHistoryRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
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
class AnimalWeightServiceImplTest {

    @Mock
    private AnimalWeightHistoryRepository weightHistoryRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalAccessGuard animalAccessGuard;

    @InjectMocks
    private AnimalWeightServiceImpl animalWeightService;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Person person(UUID id) {
        return Person.builder().personId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Animal animal(Double pesoAtual) {
        return Animal.builder()
                .animalId(ANIMAL_ID)
                .name("Rex")
                .species(Species.CANINA)
                .weight(pesoAtual)
                .tutors(PetTutores.titular(person(OWNER_ID)))
                .build();
    }

    private AnimalWeightHistory medicao(Animal animal, double peso, LocalDate quando) {
        return AnimalWeightHistory.builder()
                .weightHistoryId(UUID.randomUUID())
                .animal(animal)
                .weight(peso)
                .measuredAt(quando)
                .creationDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();
    }


    @Nested
    @DisplayName("addWeight")
    class AddWeight {

        @Test
        @DisplayName("grava a medicao vinculada ao animal")
        void gravaMedicao() {

            var animal = animal(12.5);
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal);
            when(weightHistoryRepository.save(any(AnimalWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID))
                    .thenReturn(Optional.of(medicao(animal, 14.0, LocalDate.of(2026, 3, 10))));

            var request = AnimalWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .note("consulta de rotina")
                    .build();

            var result = animalWeightService.addWeight(ANIMAL_ID, request);

            assertThat(result.getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(result.getWeight()).isEqualTo(14.0);
            assertThat(result.getNote()).isEqualTo("consulta de rotina");

            var captor = ArgumentCaptor.forClass(AnimalWeightHistory.class);
            verify(weightHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("a medicao mais recente vira o espelho em Animal.weight")
        void medicaoMaisRecenteAtualizaOEspelho() {

            var animal = animal(12.5);
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal);
            when(weightHistoryRepository.save(any(AnimalWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID))
                    .thenReturn(Optional.of(medicao(animal, 14.0, LocalDate.of(2026, 3, 10))));

            animalWeightService.addWeight(ANIMAL_ID, AnimalWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .build());

            var captor = ArgumentCaptor.forClass(Animal.class);
            verify(animalRepository).save(captor.capture());
            assertThat(captor.getValue().getWeight()).isEqualTo(14.0);
        }

        @Test
        @DisplayName("medicao historica antiga nao faz o espelho regredir")
        void medicaoAntigaNaoRegrideOEspelho() {

            var animal = animal(14.0);
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal);
            when(weightHistoryRepository.save(any(AnimalWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            // o tutor lancou uma pesagem esquecida de janeiro; a de marco continua
            // sendo a mais recente
            when(weightHistoryRepository.findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID))
                    .thenReturn(Optional.of(medicao(animal, 14.0, LocalDate.of(2026, 3, 10))));

            animalWeightService.addWeight(ANIMAL_ID, AnimalWeightRequestDTO.builder()
                    .weight(9.0)
                    .measuredAt(LocalDate.of(2026, 1, 5))
                    .build());

            var captor = ArgumentCaptor.forClass(Animal.class);
            verify(animalRepository).save(captor.capture());
            assertThat(captor.getValue().getWeight()).isEqualTo(14.0);
        }
    }

    @Nested
    @DisplayName("listWeights")
    class ListWeights {

        @Test
        @DisplayName("devolve a serie da mais recente para a mais antiga")
        void devolveSerieOrdenada() {

            var animal = animal(14.0);
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal);
            when(weightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID))
                    .thenReturn(List.of(
                            medicao(animal, 14.0, LocalDate.of(2026, 3, 10)),
                            medicao(animal, 9.0, LocalDate.of(2026, 1, 5))));

            var result = animalWeightService.listWeights(ANIMAL_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getWeight()).isEqualTo(14.0);
            assertThat(result.get(1).getWeight()).isEqualTo(9.0);
        }
    }

    /**
     * O nivel que cada operacao exige do guard - ver
     * {@link br.com.petfy.healthcare.security.AnimalAccessGuardTest} para o que o
     * guard faz com esse nivel.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("registrar pesagem exige escrita")
        void registrarExigeEscrita() {

            var animal = animal(12.5);
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal);
            when(weightHistoryRepository.save(any(AnimalWeightHistory.class))).thenAnswer(i -> i.getArgument(0));
            when(weightHistoryRepository.findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID))
                    .thenReturn(Optional.empty());

            animalWeightService.addWeight(ANIMAL_ID, AnimalWeightRequestDTO.builder()
                    .weight(14.0)
                    .measuredAt(LocalDate.of(2026, 3, 10))
                    .build());

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Acompanhar a curva de peso e leitura: a avo que so olha a carteira ve a
         * serie sem poder lancar pesagem.
         */
        @Test
        @DisplayName("listar a serie exige so leitura")
        void listarExigeSoLeitura() {

            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal(14.0));
            when(weightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID)).thenReturn(List.of());

            animalWeightService.listWeights(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireEscrita(any());
        }
    }

}
