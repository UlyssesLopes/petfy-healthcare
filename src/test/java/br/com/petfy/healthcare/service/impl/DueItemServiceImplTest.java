package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.ConsentStatusResponseDTO;
import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.CareInstructionFulfillment;
import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionFulfillmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * A pendencia e derivada, e o que este teste protege e a derivacao.
 *
 * Cada caso mede uma decisao que nao esta no schema: que orientacao cumprida dentro do
 * intervalo nao cobra, que convite cobra quem convidou, que consentimento pendente vai
 * primeiro por nao ter data.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DueItemServiceImplTest {

    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private AnimalRepository animalRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private AntiparasiticRepository antiparasiticRepository;
    @Mock private CareInstructionRepository careInstructionRepository;
    @Mock private CareInstructionFulfillmentRepository fulfillmentRepository;
    @Mock private PetTutorInviteRepository petTutorInviteRepository;
    @Mock private ConsentService consentService;

    @InjectMocks
    private DueItemServiceImpl service;

    private static final UUID PERSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID INSTRUCTION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private Person pessoa() {
        return Person.builder().personId(PERSON_ID).name("Ulysses").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .custodies(Custodias.titular(pessoa()))
                .build();
    }

    private CareInstruction instrucao(int intervalDays, LocalDate startsOn) {
        return CareInstruction.builder()
                .careInstructionId(INSTRUCTION_ID)
                .animal(animal())
                .description("Fenobarbital")
                .intervalDays(intervalDays)
                .startsOn(startsOn)
                .build();
    }

    @BeforeEach
    void tudoVazioPorPadrao() {
        when(currentPersonProvider.require()).thenReturn(pessoa());
        when(animalRepository.findAlcancadosPor(eq(PERSON_ID), any())).thenReturn(List.of(animal()));
        when(vaccineRepository.findAlcancadasPor(eq(PERSON_ID), any())).thenReturn(List.of());
        when(antiparasiticRepository.findAlcancadosPor(eq(PERSON_ID), any())).thenReturn(List.of());
        when(careInstructionRepository.findVigentesNosAnimais(anyList(), any())).thenReturn(List.of());
        when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(any()))
                .thenReturn(List.of());
        when(fulfillmentRepository
                .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(any()))
                .thenReturn(Optional.empty());
        when(consentService.statusDoAutenticado())
                .thenReturn(ConsentStatusResponseDTO.builder().tudoAceito(true).build());
    }

    @Nested
    @DisplayName("dose que vence")
    class Dose {

        /** A janela e do futuro. O que ja venceu entra sempre, e vem marcado. */
        @Test
        @DisplayName("dose atrasada entra marcada como atrasada")
        void doseAtrasadaEntraMarcada() {
            when(vaccineRepository.findAlcancadasPor(eq(PERSON_ID), any())).thenReturn(List.of(
                    Vaccine.builder()
                            .vaccineId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                            .animal(animal())
                            .vaccineName("V10")
                            .nextDoseDate(LocalDate.now().minusDays(5))
                            .build()));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).hasSize(1);
            assertThat(itens.get(0).getKind()).isEqualTo(DueItemKind.DOSE_DE_VACINA);
            assertThat(itens.get(0).isOverdue()).isTrue();
        }

        /**
         * Fora da janela nao cobra. Cobrar dose de dentro de um ano hoje treina o usuario
         * a ignorar a lista, que e o caminho mais curto para ela perder valor.
         */
        @Test
        @DisplayName("dose depois da janela nao entra")
        void doseDepoisDaJanelaNaoEntra() {
            when(vaccineRepository.findAlcancadasPor(eq(PERSON_ID), any())).thenReturn(List.of(
                    Vaccine.builder()
                            .vaccineId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                            .animal(animal())
                            .vaccineName("V10")
                            .nextDoseDate(LocalDate.now().plusDays(90))
                            .build()));

            assertThat(service.doAutenticado(30)).isEmpty();
        }

        @Test
        @DisplayName("antiparasitario no reforco entra")
        void antiparasitarioNoReforcoEntra() {
            when(antiparasiticRepository.findAlcancadosPor(eq(PERSON_ID), any())).thenReturn(List.of(
                    Antiparasitic.builder()
                            .antiparasiticId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                            .animal(animal())
                            .name("Vermifugo")
                            .nextDoseDate(LocalDate.now())
                            .build()));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).extracting(DueItemResponseDTO::getKind)
                    .containsExactly(DueItemKind.ANTIPARASITARIO);
            assertThat(itens.get(0).isOverdue()).isFalse();
        }
    }

    @Nested
    @DisplayName("orientacao")
    class Orientacao {

        /** Nunca cumprida cobra desde o inicio da vigencia. */
        @Test
        @DisplayName("nunca cumprida cobra desde o comeco")
        void nuncaCumpridaCobraDesdeOComeco() {
            LocalDate comecou = LocalDate.now().minusDays(3);

            when(careInstructionRepository.findVigentesNosAnimais(anyList(), any()))
                    .thenReturn(List.of(instrucao(1, comecou)));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).hasSize(1);
            assertThat(itens.get(0).getKind()).isEqualTo(DueItemKind.ORIENTACAO);
            assertThat(itens.get(0).getDueOn()).isEqualTo(comecou);
            assertThat(itens.get(0).isOverdue()).isTrue();
            assertThat(itens.get(0).getLastFulfilledAt()).isNull();
        }

        /**
         * Cumprida dentro do intervalo nao cobra. E o caso que separa uma lista util de
         * uma que grita o tempo todo.
         */
        @Test
        @DisplayName("cumprida hoje com intervalo de 7 dias nao cobra")
        void cumpridaDentroDoIntervaloNaoCobra() {
            CareInstruction instrucao = instrucao(7, LocalDate.now().minusDays(20));

            when(careInstructionRepository.findVigentesNosAnimais(anyList(), any()))
                    .thenReturn(List.of(instrucao));
            when(fulfillmentRepository
                    .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(INSTRUCTION_ID))
                    .thenReturn(Optional.of(CareInstructionFulfillment.builder()
                            .careInstruction(instrucao)
                            .confirmedBy(pessoa())
                            .fulfilledAt(LocalDateTime.now())
                            .recordedAt(LocalDateTime.now())
                            .build()));

            assertThat(service.doAutenticado(30)).isEmpty();
        }

        /**
         * Cumprida ontem com intervalo de um dia cobra hoje - e a resposta diz quem
         * cumpriu. <b>E o que impede dois tutores darem o mesmo remedio</b>: sem este
         * campo a pendencia apareceria identica para os dois.
         */
        @Test
        @DisplayName("cumprida ontem com intervalo de um dia cobra hoje, e diz quem cumpriu")
        void cumpridaOntemCobraHojeEDizQuemCumpriu() {
            CareInstruction instrucao = instrucao(1, LocalDate.now().minusDays(20));
            Person coTutora = Person.builder()
                    .personId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .name("Co-tutora").build();

            when(careInstructionRepository.findVigentesNosAnimais(anyList(), any()))
                    .thenReturn(List.of(instrucao));
            when(fulfillmentRepository
                    .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(INSTRUCTION_ID))
                    .thenReturn(Optional.of(CareInstructionFulfillment.builder()
                            .careInstruction(instrucao)
                            .confirmedBy(coTutora)
                            .fulfilledAt(LocalDateTime.now().minusDays(1))
                            .recordedAt(LocalDateTime.now().minusDays(1))
                            .build()));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).hasSize(1);
            assertThat(itens.get(0).getDueOn()).isEqualTo(LocalDate.now());
            assertThat(itens.get(0).isOverdue()).isFalse();
            assertThat(itens.get(0).getLastFulfilledByName()).isEqualTo("Co-tutora");
        }
    }

    @Nested
    @DisplayName("convite")
    class Convite {

        /**
         * Cobra quem convidou. Quem recebeu talvez nem tenha conta, e o produto nao cobra
         * quem nao alcanca.
         */
        @Test
        @DisplayName("convite que eu enviei cobra a mim")
        void conviteQueEuEnvieiCobraAMim() {
            when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(convite(pessoa())));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).extracting(DueItemResponseDTO::getKind)
                    .containsExactly(DueItemKind.CONVITE_PENDENTE);
            assertThat(itens.get(0).getDescription()).isEqualTo("convidada@exemplo.com");
        }

        @Test
        @DisplayName("convite enviado por outro tutor nao cobra a mim")
        void conviteDeOutroNaoCobraAMim() {
            Person outro = Person.builder()
                    .personId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .name("Co-tutor").build();

            when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(convite(outro)));

            assertThat(service.doAutenticado(30)).isEmpty();
        }

        @Test
        @DisplayName("convite ja aceito nao cobra ninguem")
        void conviteJaAceitoNaoCobra() {
            PetTutorInvite aceito = convite(pessoa());
            aceito.setAcceptedAt(LocalDateTime.now().minusDays(1));

            when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(aceito));

            assertThat(service.doAutenticado(30)).isEmpty();
        }

        private PetTutorInvite convite(Person criadoPor) {
            return PetTutorInvite.builder()
                    .petTutorInviteId(UUID.fromString("88888888-8888-8888-8888-888888888888"))
                    .animal(animal())
                    .createdBy(criadoPor)
                    .email("convidada@exemplo.com")
                    .role(PetTutorRole.EDITOR)
                    .tokenHash("hash")
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .creationDate(LocalDateTime.now())
                    .build();
        }
    }

    @Nested
    @DisplayName("consentimento")
    class Consentimento {

        /**
         * Sem data, e cobrando agora - e por isso vem primeiro. Ele bloqueia o resto do
         * produto, e deixa-lo no fim faria o usuario percorrer a lista inteira para
         * descobrir por que nada funciona.
         */
        @Test
        @DisplayName("consentimento pendente vem primeiro e nao tem animal nem data")
        void consentimentoPendenteVemPrimeiro() {
            when(consentService.statusDoAutenticado())
                    .thenReturn(ConsentStatusResponseDTO.builder().tudoAceito(false).build());
            when(vaccineRepository.findAlcancadasPor(eq(PERSON_ID), any())).thenReturn(List.of(
                    Vaccine.builder()
                            .vaccineId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                            .animal(animal())
                            .vaccineName("V10")
                            .nextDoseDate(LocalDate.now().minusDays(30))
                            .build()));

            List<DueItemResponseDTO> itens = service.doAutenticado(30);

            assertThat(itens).extracting(DueItemResponseDTO::getKind)
                    .containsExactly(DueItemKind.CONSENTIMENTO_PENDENTE, DueItemKind.DOSE_DE_VACINA);
            assertThat(itens.get(0).getDueOn()).isNull();
            assertThat(itens.get(0).getAnimalId()).isNull();
            assertThat(itens.get(0).isOverdue()).isTrue();
        }

        @Test
        @DisplayName("tudo aceito nao gera pendencia de consentimento")
        void tudoAceitoNaoGeraPendencia() {
            assertThat(service.doAutenticado(30)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ordenacao e alcance")
    class OrdenacaoEAlcance {

        /** Do mais atrasado ao menos urgente: a lista responde "o que primeiro?". */
        @Test
        @DisplayName("ordena do mais atrasado ao menos urgente")
        void ordenaDoMaisAtrasado() {
            when(vaccineRepository.findAlcancadasPor(eq(PERSON_ID), any())).thenReturn(List.of(
                    Vaccine.builder()
                            .vaccineId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                            .animal(animal()).vaccineName("Depois")
                            .nextDoseDate(LocalDate.now().plusDays(10)).build(),
                    Vaccine.builder()
                            .vaccineId(UUID.fromString("44444444-4444-4444-4444-444444444445"))
                            .animal(animal()).vaccineName("Antes")
                            .nextDoseDate(LocalDate.now().minusDays(10)).build()));

            assertThat(service.doAutenticado(30)).extracting(DueItemResponseDTO::getDescription)
                    .containsExactly("Antes", "Depois");
        }

        /**
         * Quem nao alcanca animal nenhum nao e consultado por orientacao nem por convite -
         * e nao por economia: {@code in ()} com lista vazia e SQL invalido em alguns
         * bancos, e a consulta por animal nao tem sentido sem animal.
         */
        @Test
        @DisplayName("sem animal alcancado nao consulta orientacao nem convite")
        void semAnimalNaoConsulta() {
            when(animalRepository.findAlcancadosPor(eq(PERSON_ID), any())).thenReturn(List.of());

            assertThat(service.doAutenticado(30)).isEmpty();

            org.mockito.Mockito.verify(careInstructionRepository, org.mockito.Mockito.never())
                    .findVigentesNosAnimais(anyList(), any());
            org.mockito.Mockito.verify(petTutorInviteRepository, org.mockito.Mockito.never())
                    .findByAnimalAnimalIdOrderByCreationDateDesc(any());
        }
    }

}
