package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.PetHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetHealthCondition;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionSeverity;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.PetHealthConditionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
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
class PetHealthConditionServiceImplTest {

    @Mock private PetHealthConditionRepository petHealthConditionRepository;
    @Mock private PetAccessGuard petAccessGuard;

    @InjectMocks
    private PetHealthConditionServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONDITION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").species(Species.CANINA)
                .tutors(PetTutores.titular(Owner.builder().ownerId(OWNER_ID).name("Ulysses").build()))
                .build();
    }

    private PetHealthCondition alergia() {
        return PetHealthCondition.builder()
                .petHealthConditionId(CONDITION_ID)
                .pet(pet())
                .kind(PetHealthConditionKind.ALERGIA)
                .description("Anestesico local")
                .severity(PetHealthConditionSeverity.GRAVE)
                .creationDate(LocalDateTime.now())
                .build();
    }

    private PetHealthCondition cronica() {
        return PetHealthCondition.builder()
                .petHealthConditionId(CONDITION_ID)
                .pet(pet())
                .kind(PetHealthConditionKind.CONDICAO_CRONICA)
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(PET_ID, PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.ALERGIA)
                    .description("Anestesico local")
                    .severity(PetHealthConditionSeverity.GRAVE)
                    .since(LocalDate.now().minusYears(1))
                    .build());

            assertThat(result.getKind()).isEqualTo(PetHealthConditionKind.ALERGIA);
            assertThat(result.getSeverity()).isEqualTo(PetHealthConditionSeverity.GRAVE);
            assertThat(result.isAtiva()).isTrue();
        }

        @Test
        @DisplayName("registra condicao cronica sem gravidade")
        void registraCronica() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(PET_ID, PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .build());

            assertThat(result.getKind()).isEqualTo(PetHealthConditionKind.CONDICAO_CRONICA);
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());

            var request = PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .severity(PetHealthConditionSeverity.GRAVE)
                    .build();

            assertThatThrownBy(() -> service.create(PET_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode(),
                            HttpStatus.BAD_REQUEST);

            verify(petHealthConditionRepository, never()).save(any());
        }

        /** Condicao que ja chegou encerrada: registro retroativo do que o animal teve. */
        @Test
        @DisplayName("aceita condicao ja encerrada, para registro retroativo")
        void aceitaJaEncerrada() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.create(PET_ID, PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(PET_ID, CONDITION_ID, PetHealthConditionRequestDTO.builder()
                    .notes("confirmada em cirurgia")
                    .build());

            assertThat(result.getNotes()).isEqualTo("confirmada em cirurgia");
            assertThat(result.getDescription()).isEqualTo("Anestesico local");
            assertThat(result.getSeverity()).isEqualTo(PetHealthConditionSeverity.GRAVE);
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(PET_ID, CONDITION_ID, PetHealthConditionRequestDTO.builder()
                    .resolvedAt(LocalDate.now())
                    .build());

            assertThat(result.isAtiva()).isFalse();
            verify(petHealthConditionRepository, never()).delete(any());
        }

        /**
         * Trocar o tipo nao e corrigir um campo, e dizer que era outra coisa desde o
         * comeco - e deixaria a gravidade pendurada num tipo que nao a aceita.
         */
        @Test
        @DisplayName("trocar o kind responde 409")
        void trocarKindResponde409() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));

            var request = PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
                    .build();

            assertThatThrownBy(() -> service.update(PET_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CONDITION_KIND_IS_IMMUTABLE.getCode(),
                            HttpStatus.CONFLICT);

            verify(petHealthConditionRepository, never()).save(any());
        }

        /** Repetir o mesmo kind nao e troca: nao ha o que recusar. */
        @Test
        @DisplayName("reenviar o mesmo kind e aceito")
        void mesmoKindEAceito() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var result = service.update(PET_ID, CONDITION_ID, PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.ALERGIA)
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(cronica()));

            var request = PetHealthConditionRequestDTO.builder()
                    .severity(PetHealthConditionSeverity.LEVE)
                    .build();

            assertThatThrownBy(() -> service.update(PET_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode());

            verify(petHealthConditionRepository, never()).save(any());
        }

        /**
         * Sem o filtro por pet, o titular de um pet alteraria a condicao de outro so por
         * ter o id - e a autorizacao teria sido feita sobre o pet errado.
         */
        @Test
        @DisplayName("condicao de outro pet responde 404")
        void condicaoDeOutroPetResponde404() {
            var deOutroPet = PetHealthCondition.builder()
                    .petHealthConditionId(CONDITION_ID)
                    .pet(Pet.builder().petId(UUID.randomUUID()).name("Nina").build())
                    .kind(PetHealthConditionKind.ALERGIA)
                    .description("Poeira")
                    .build();

            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(deOutroPet));

            var request = PetHealthConditionRequestDTO.builder().notes("x").build();

            assertThatThrownBy(() -> service.update(PET_ID, CONDITION_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CONDITION_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listByPet e delete")
    class ListarERemover {

        @Test
        @DisplayName("lista o que o repositorio devolve, com o atalho de ativa")
        void listaComAtalhoDeAtiva() {
            var encerrada = cronica();
            encerrada.setResolvedAt(LocalDate.now().minusMonths(1));

            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findByPetOrdenadasPorRelevancia(PET_ID))
                    .thenReturn(List.of(alergia(), encerrada));

            var result = service.listByPet(PET_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isAtiva()).isTrue();
            assertThat(result.get(1).isAtiva()).isFalse();
        }

        @Test
        @DisplayName("remover apaga o registro criado por engano")
        void removerApaga() {
            var existente = alergia();
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(existente));

            service.delete(PET_ID, CONDITION_ID);

            verify(petHealthConditionRepository).delete(existente);
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
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.save(any(PetHealthCondition.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.create(PET_ID, PetHealthConditionRequestDTO.builder()
                    .kind(PetHealthConditionKind.ALERGIA).description("Poeira").build());

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Ler exige apenas leitura: o VIEWER precisa saber a que o animal e alergico - e a
         * quem so acompanha e que essa informacao mais falta.
         */
        @Test
        @DisplayName("listar condicoes exige so leitura")
        void listarExigeSoLeitura() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findByPetOrdenadasPorRelevancia(PET_ID))
                    .thenReturn(List.of());

            service.listByPet(PET_ID);

            verify(petAccessGuard).requireLeitura(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
        }

        @Test
        @DisplayName("remover condicao exige escrita")
        void removerExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petHealthConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(alergia()));

            service.delete(PET_ID, CONDITION_ID);

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }
    }

}
