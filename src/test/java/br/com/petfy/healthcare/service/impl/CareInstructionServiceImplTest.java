package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.CareInstructionFulfillment;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.CareInstructionFulfillmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
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
import java.util.EnumSet;
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
class CareInstructionServiceImplTest {

    @Mock private CareInstructionRepository careInstructionRepository;
    @Mock private CareInstructionFulfillmentRepository fulfillmentRepository;
    @Mock private AnimalAccessGuard animalAccessGuard;
    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private CurrentProfessionalProvider currentProfessionalProvider;

    @InjectMocks
    private CareInstructionServiceImpl service;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTRUCTION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ORGANIZATION_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private Person pessoa() {
        return Person.builder().personId(PERSON_ID).name("Ulysses").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .custodies(Custodias.titular(pessoa()))
                .build();
    }

    private CareInstruction instrucao() {
        return CareInstruction.builder()
                .careInstructionId(INSTRUCTION_ID)
                .animal(animal())
                .recordedBy(pessoa())
                .description("Meia dose de fenobarbital de 12 em 12 horas")
                .intervalDays(1)
                .startsOn(LocalDate.now().minusDays(10))
                .creationDate(LocalDateTime.now())
                .build();
    }

    private CareInstructionRequestDTO request() {
        return CareInstructionRequestDTO.builder()
                .description("Meia dose de fenobarbital de 12 em 12 horas")
                .intervalDays(1)
                .startsOn(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        /**
         * Escrita, e nao leitura: a orientacao manda alguem fazer algo com o animal, e
         * quem so acompanha nao manda.
         */
        @Test
        @DisplayName("exige escrita e grava a autoria")
        void exigeEscritaEGravaAutoria() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(currentProfessionalProvider.organizacaoDeclarada(any())).thenReturn(Optional.empty());
            when(careInstructionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.create(ANIMAL_ID, request());

            ArgumentCaptor<CareInstruction> captor = ArgumentCaptor.forClass(CareInstruction.class);
            verify(careInstructionRepository).save(captor.capture());

            assertThat(captor.getValue().getRecordedBy().getPersonId()).isEqualTo(PERSON_ID);
            assertThat(captor.getValue().getAnimal().getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(captor.getValue().getRevokedAt()).isNull();
        }

        /**
         * Sem organizacao declarada, a orientacao sai assinada so pela pessoa - o tutor
         * combinando o remedio de casa, que e um dos tres usos que a orientacao serve.
         */
        @Test
        @DisplayName("sem header de organizacao a orientacao nao tem organizacao")
        void semHeaderNaoTemOrganizacao() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(currentProfessionalProvider.organizacaoDeclarada(any())).thenReturn(Optional.empty());
            when(careInstructionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CareInstructionResponseDTO resposta = service.create(ANIMAL_ID, request());

            assertThat(resposta.getOrganizationName()).isNull();
        }

        /**
         * A capacidade exigida e REGISTRAR_OBSERVACAO, e nao a clinica: tema de casa de
         * creche e rotina de lar transitorio sao orientacoes, e exigir ato clinico
         * reintroduziria a exigencia de ser clinica que o P3 removeu.
         */
        @Test
        @DisplayName("organizacao que pode observar assina a orientacao, mesmo sem capacidade clinica")
        void organizacaoQuePodeObservarAssina() {
            Organization creche = Organization.builder()
                    .organizationId(ORGANIZATION_ID)
                    .name("Creche do Bairro")
                    .capabilities(EnumSet.of(OrganizationCapability.REGISTRAR_OBSERVACAO,
                            OrganizationCapability.GERIR_TURMA_E_VAGA))
                    .build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(currentProfessionalProvider.organizacaoDeclarada(any())).thenReturn(Optional.of(creche));
            when(careInstructionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CareInstructionResponseDTO resposta = service.create(ANIMAL_ID, request());

            assertThat(resposta.getOrganizationName()).isEqualTo("Creche do Bairro");
        }

        @Test
        @DisplayName("organizacao sem a capacidade de observar nao emite orientacao")
        void organizacaoSemCapacidadeNaoEmite() {
            Organization semNada = Organization.builder()
                    .organizationId(ORGANIZATION_ID)
                    .name("Petshop")
                    .capabilities(EnumSet.noneOf(OrganizationCapability.class))
                    .build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(currentProfessionalProvider.organizacaoDeclarada(any())).thenReturn(Optional.of(semNada));

            assertThatThrownBy(() -> service.create(ANIMAL_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN)
                    .hasFieldOrPropertyWithValue("code",
                            ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getCode());

            verify(careInstructionRepository, never()).save(any());
        }

        /**
         * Prazo que termina antes de comecar e erro de digitacao. O CHECK do banco tambem
         * recusa; aqui o cliente recebe 400 em vez de erro de integridade.
         */
        @Test
        @DisplayName("endsOn anterior a startsOn responde 400, e nao erro de integridade")
        void prazoIncoerenteResponde400() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentPersonProvider.require()).thenReturn(pessoa());

            CareInstructionRequestDTO invalido = CareInstructionRequestDTO.builder()
                    .description("Antibiotico por 7 dias")
                    .intervalDays(1)
                    .startsOn(LocalDate.now())
                    .endsOn(LocalDate.now().minusDays(1))
                    .build();

            assertThatThrownBy(() -> service.create(ANIMAL_ID, invalido))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

            verify(careInstructionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("encerra sem apagar, gravando quem encerrou")
        void encerraSemApagar() {
            CareInstruction instrucao = instrucao();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(instrucao));
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(careInstructionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CareInstructionResponseDTO resposta = service.revoke(ANIMAL_ID, INSTRUCTION_ID);

            assertThat(resposta.getRevokedAt()).isNotNull();
            assertThat(resposta.isVigente()).isFalse();
            assertThat(instrucao.getRevokedBy().getPersonId()).isEqualTo(PERSON_ID);
            verify(careInstructionRepository, never()).delete(any());
        }

        /**
         * Idempotente, como a revogacao de concessao - que perdeu a idempotencia no P2a e
         * so foi pega por um teste igual a este. Reescrever a data apagaria de quem foi a
         * decisao clinica.
         */
        @Test
        @DisplayName("revogar de novo nao mexe na data nem em quem encerrou")
        void revogarDeNovoNaoMexeNaData() {
            LocalDateTime original = LocalDateTime.now().minusDays(3);
            Person outra = Person.builder()
                    .personId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .name("Veterinaria").build();

            CareInstruction jaRevogada = instrucao();
            jaRevogada.setRevokedAt(original);
            jaRevogada.setRevokedBy(outra);

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(jaRevogada));

            CareInstructionResponseDTO resposta = service.revoke(ANIMAL_ID, INSTRUCTION_ID);

            assertThat(resposta.getRevokedAt()).isEqualTo(original);
            assertThat(jaRevogada.getRevokedBy().getName()).isEqualTo("Veterinaria");
            verify(careInstructionRepository, never()).save(any());
        }

        /**
         * 404, e nao 403: a orientacao de outro animal nao existe para quem pergunta pelo
         * caminho deste. Sem o filtro, a autorizacao teria sido feita sobre o animal
         * errado.
         */
        @Test
        @DisplayName("orientacao de outro animal responde 404")
        void orientacaoDeOutroAnimalResponde404() {
            CareInstruction deOutro = instrucao();
            deOutro.setAnimal(Animal.builder()
                    .animalId(UUID.fromString("99999999-9999-9999-9999-999999999999")).build());

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(deOutro));

            assertThatThrownBy(() -> service.revoke(ANIMAL_ID, INSTRUCTION_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND)
                    .hasFieldOrPropertyWithValue("code",
                            ErrorMessageEnum.CARE_INSTRUCTION_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("confirmFulfillment")
    class ConfirmFulfillment {

        /**
         * O instante do fato e o informado, e nao o da digitacao: o tutor confirma as 22h
         * o remedio que deu as 8h, e a pendencia tem de contar do fato.
         */
        @Test
        @DisplayName("grava o instante do fato e o da digitacao separados")
        void gravaFatoEDigitacaoSeparados() {
            LocalDateTime deManha = LocalDateTime.now().minusHours(14);

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(instrucao()));
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(fulfillmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.confirmFulfillment(ANIMAL_ID, INSTRUCTION_ID,
                    CareInstructionFulfillmentRequestDTO.builder().fulfilledAt(deManha).build());

            ArgumentCaptor<CareInstructionFulfillment> captor =
                    ArgumentCaptor.forClass(CareInstructionFulfillment.class);
            verify(fulfillmentRepository).save(captor.capture());

            assertThat(captor.getValue().getFulfilledAt()).isEqualTo(deManha);
            assertThat(captor.getValue().getRecordedAt()).isAfter(deManha);
            assertThat(captor.getValue().getConfirmedBy().getPersonId()).isEqualTo(PERSON_ID);
        }

        @Test
        @DisplayName("sem fulfilledAt no corpo, cumpriu agora")
        void semFulfilledAtCumpriuAgora() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(instrucao()));
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(fulfillmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.confirmFulfillment(ANIMAL_ID, INSTRUCTION_ID,
                    new CareInstructionFulfillmentRequestDTO());

            ArgumentCaptor<CareInstructionFulfillment> captor =
                    ArgumentCaptor.forClass(CareInstructionFulfillment.class);
            verify(fulfillmentRepository).save(captor.capture());

            assertThat(captor.getValue().getFulfilledAt()).isNotNull();
        }

        @Test
        @DisplayName("cumprir orientacao revogada responde 409")
        void cumprirRevogadaResponde409() {
            CareInstruction revogada = instrucao();
            revogada.setRevokedAt(LocalDateTime.now().minusDays(1));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID)).thenReturn(Optional.of(revogada));

            assertThatThrownBy(() -> service.confirmFulfillment(ANIMAL_ID, INSTRUCTION_ID,
                    new CareInstructionFulfillmentRequestDTO()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                    .hasFieldOrPropertyWithValue("code",
                            ErrorMessageEnum.CARE_INSTRUCTION_NOT_IN_EFFECT.getCode());

            verify(fulfillmentRepository, never()).save(any());
        }

        /**
         * A vigencia e conferida no instante do <b>fato</b>.
         *
         * O tratamento terminou ontem e o tutor confirma hoje o remedio que deu ontem: o
         * cumprimento e verdadeiro, e recusa-lo perderia o ultimo dia de aderencia de
         * todo tratamento com prazo. Conferir contra hoje recusaria registro verdadeiro.
         */
        @Test
        @DisplayName("cumprimento de ontem em tratamento que terminou ontem e aceito")
        void cumprimentoDeOntemEmTratamentoEncerradoOntem() {
            CareInstruction terminadaOntem = instrucao();
            terminadaOntem.setEndsOn(LocalDate.now().minusDays(1));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID))
                    .thenReturn(Optional.of(terminadaOntem));
            when(currentPersonProvider.require()).thenReturn(pessoa());
            when(fulfillmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.confirmFulfillment(ANIMAL_ID, INSTRUCTION_ID,
                    CareInstructionFulfillmentRequestDTO.builder()
                            .fulfilledAt(LocalDateTime.now().minusDays(1)).build());

            verify(fulfillmentRepository).save(any());
        }

        @Test
        @DisplayName("cumprimento de hoje em tratamento que terminou ontem responde 409")
        void cumprimentoDeHojeEmTratamentoEncerradoOntem() {
            CareInstruction terminadaOntem = instrucao();
            terminadaOntem.setEndsOn(LocalDate.now().minusDays(1));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(careInstructionRepository.findById(INSTRUCTION_ID))
                    .thenReturn(Optional.of(terminadaOntem));

            assertThatThrownBy(() -> service.confirmFulfillment(ANIMAL_ID, INSTRUCTION_ID,
                    new CareInstructionFulfillmentRequestDTO()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("listByAnimal")
    class ListByAnimal {

        /**
         * Ler exige apenas leitura: saber que o animal toma anticonvulsivante e
         * informacao de quem acompanha, e nao privilegio de quem escreve.
         */
        @Test
        @DisplayName("exige leitura e devolve o ultimo cumprimento junto")
        void exigeLeituraEDevolveUltimoCumprimento() {
            CareInstruction instrucao = instrucao();

            when(careInstructionRepository.findByAnimalAnimalIdOrderByStartsOnDesc(ANIMAL_ID))
                    .thenReturn(List.of(instrucao));
            when(fulfillmentRepository
                    .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(INSTRUCTION_ID))
                    .thenReturn(Optional.of(CareInstructionFulfillment.builder()
                            .careInstruction(instrucao)
                            .confirmedBy(Person.builder()
                                    .personId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                                    .name("Co-tutora").build())
                            .fulfilledAt(LocalDateTime.now().minusHours(2))
                            .recordedAt(LocalDateTime.now())
                            .build()));

            List<CareInstructionResponseDTO> lista = service.listByAnimal(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).getLastFulfilledByName()).isEqualTo("Co-tutora");
            assertThat(lista.get(0).isVigente()).isTrue();
        }
    }

}
