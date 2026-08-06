package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.AnimalHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
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
class AnimalHealthConditionServiceImplTest {

    @Mock private AnimalHealthConditionRepository animalHealthConditionRepository;
    @Mock private AnimalAccessGuard animalAccessGuard;

    @InjectMocks
    private AnimalHealthConditionServiceImpl service;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONDITION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .tutors(PetTutores.titular(Person.builder().personId(OWNER_ID).name("Ulysses").build()))
                .build();
    }

    private AnimalHealthCondition alergia() {
        return AnimalHealthCondition.builder()
                .animalHealthConditionId(CONDITION_ID)
                .animal(animal())
                .kind(AnimalHealthConditionKind.ALERGIA)
                .description("Anestesico local")
                .severity(AnimalHealthConditionSeverity.GRAVE)
                .creationDate(LocalDateTime.now())
                .build();
    }

    private AnimalHealthCondition cronica() {
        return AnimalHealthCondition.builder()
                .animalHealthConditionId(CONDITION_ID)
                .animal(animal())
                .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                .description("Diabetes")
                .creationDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("registra alergia com gravidade")
        void registraAlergia() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(ANIMAL_ID, AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Anestesico local")
                    .severity(AnimalHealthConditionSeverity.GRAVE)
                    .since(LocalDate.now().minusYears(1))
                    .build());

            assertThat(result.getKind()).isEqualTo(AnimalHealthConditionKind.ALERGIA);
            assertThat(result.getSeverity()).isEqualTo(AnimalHealthConditionSeverity.GRAVE);
            assertThat(result.isAtiva()).isTrue();
        }

        @Test
        @DisplayName("registra condicao cronica sem gravidade")
        void registraCronica() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(ANIMAL_ID, AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .build());

            assertThat(result.getKind()).isEqualTo(AnimalHealthConditionKind.CONDICAO_CRONICA);
            assertThat(result.getSeverity()).isNull();
        }

        /**
         * O banco tambem recusa, por CHECK. As duas checagens nao sao redundantes: o banco
         * impede que um insert direto contorne, e o servico impede que o cliente receba
         * erro de integridade como 500 - aqui e 400.
         */
        @Test
        @DisplayName("gravidade em condicao cronica responde 400 sem gravar")
        void gravidadeEmCronicaResponde400() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            var request = AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .severity(AnimalHealthConditionSeverity.GRAVE)
                    .build();

            assertThatThrownBy(() -> service.create(ANIMAL_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode(),
                            HttpStatus.BAD_REQUEST);

            verify(animalHealthConditionRepository, never()).save(any());
        }

        /** Condicao que ja chegou encerrada: registro retroativo do que o animal teve. */
        @Test
        @DisplayName("aceita condicao ja encerrada, para registro retroativo")
        void aceitaJaEncerrada() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(ANIMAL_ID, AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Giardiase")
                    .since(LocalDate.now().minusYears(2))
                    .resolvedAt(LocalDate.now().minusYears(1))
                    .build());

            assertThat(result.isAtiva()).isFalse();
            assertThat(result.getResolvedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("campo ausente preserva o valor existente")
        void campoAusentePreserva() {
            var existente = alergia();
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(ANIMAL_ID, CONDITION_ID, AnimalHealthConditionRequestDTO.builder()
                    .notes("confirmada em cirurgia")
                    .build());

            assertThat(result.getNotes()).isEqualTo("confirmada em cirurgia");
            assertThat(result.getDescription()).isEqualTo("Anestesico local");
            assertThat(result.getSeverity()).isEqualTo(AnimalHealthConditionSeverity.GRAVE);
            assertThat(result.getUpdateDate()).isNotNull();
        }

        /**
         * Encerrar e preencher resolvedAt, e nao apagar: condicao que passou faz parte do
         * historico do animal.
         */
        @Test
        @DisplayName("encerrar a condicao preenche resolvedAt sem apagar o registro")
        void encerrarNaoApaga() {
            var existente = cronica();
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(ANIMAL_ID, CONDITION_ID, AnimalHealthConditionRequestDTO.builder()
                    .resolvedAt(LocalDate.now())
                    .build());

            assertThat(result.isAtiva()).isFalse();
            verify(animalHealthConditionRepository, never()).delete(any());
        }

        /**
         * Trocar o tipo nao e corrigir um campo, e dizer que era outra coisa desde o
         * comeco - e deixaria a gravidade pendurada num tipo que nao a aceita.
         */
        @Test
        @DisplayName("trocar o kind responde 409")
        void trocarKindResponde409() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));

            var request = AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .build();

            assertThatThrownBy(() -> service.update(ANIMAL_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CONDITION_KIND_IS_IMMUTABLE.getCode(),
                            HttpStatus.CONFLICT);

            verify(animalHealthConditionRepository, never()).save(any());
        }

        /** Reanimalir o mesmo kind nao e troca: nao ha o que recusar. */
        @Test
        @DisplayName("reenviar o mesmo kind e aceito")
        void mesmoKindEAceito() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(ANIMAL_ID, CONDITION_ID, AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Anestesico local e dipirona")
                    .build());

            assertThat(result.getDescription()).isEqualTo("Anestesico local e dipirona");
        }

        /**
         * A gravidade e validada contra o tipo <b>ja gravado</b>, e nao contra o do
         * request: como o kind e imutavel, validar contra o payload deixaria passar
         * gravidade em condicao cronica sempre que o cliente omitisse o kind.
         */
        @Test
        @DisplayName("gravidade em condicao cronica e recusada mesmo com kind omitido")
        void gravidadeEmCronicaComKindOmitido() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(cronica()));

            var request = AnimalHealthConditionRequestDTO.builder()
                    .severity(AnimalHealthConditionSeverity.LEVE)
                    .build();

            assertThatThrownBy(() -> service.update(ANIMAL_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode());

            verify(animalHealthConditionRepository, never()).save(any());
        }

        /**
         * Sem o filtro por animal, o titular de um animal alteraria a condicao de outro so por
         * ter o id - e a autorizacao teria sido feita sobre o animal errado.
         */
        @Test
        @DisplayName("condicao de outro animal responde 404")
        void condicaoDeOutroAnimalResponde404() {
            var deOutroAnimal = AnimalHealthCondition.builder()
                    .animalHealthConditionId(CONDITION_ID)
                    .animal(Animal.builder().animalId(UUID.randomUUID()).name("Nina").build())
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Poeira")
                    .build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(deOutroAnimal));

            var request = AnimalHealthConditionRequestDTO.builder().notes("x").build();

            assertThatThrownBy(() -> service.update(ANIMAL_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CONDITION_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listByAnimal e delete")
    class ListarERemover {

        @Test
        @DisplayName("lista o que o repositorio devolve, com o atalho de ativa")
        void listaComAtalhoDeAtiva() {
            var encerrada = cronica();
            encerrada.setResolvedAt(LocalDate.now().minusMonths(1));

            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(ANIMAL_ID))
                    .thenReturn(List.of(alergia(), encerrada));

            var result = service.listByAnimal(ANIMAL_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isAtiva()).isTrue();
            assertThat(result.get(1).isAtiva()).isFalse();
        }

        @Test
        @DisplayName("remover apaga o registro criado por engano")
        void removerApaga() {
            var existente = alergia();
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));

            service.delete(ANIMAL_ID, CONDITION_ID);

            verify(animalHealthConditionRepository).delete(existente);
        }
    }

    /**
     * A tabela operacao -> nivel, no mesmo formato dos outros servicos desde a V15.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("registrar condicao exige escrita")
        void registrarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.save(any(AnimalHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.create(ANIMAL_ID, AnimalHealthConditionRequestDTO.builder()
                    .kind(AnimalHealthConditionKind.ALERGIA).description("Poeira").build());

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Ler exige apenas leitura: o VIEWER precisa saber a que o animal e alergico - e a
         * quem so acompanha e que essa informacao mais falta.
         */
        @Test
        @DisplayName("listar condicoes exige so leitura")
        void listarExigeSoLeitura() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(ANIMAL_ID))
                    .thenReturn(List.of());

            service.listByAnimal(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireEscrita(any());
        }

        @Test
        @DisplayName("remover condicao exige escrita")
        void removerExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(animalHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));

            service.delete(ANIMAL_ID, CONDITION_ID);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }
    }

}
