package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ObservationRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Observation;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.ObservationRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("observacao")
class ObservationServiceImplTest {

    private static final UUID ANIMAL_ID = UUID.randomUUID();

    @Mock private ObservationRepository observationRepository;
    @Mock private AnimalAccessGuard animalAccessGuard;
    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private CurrentProfessionalProvider currentProfessionalProvider;

    private ObservationServiceImpl service;

    private final Person maria = Person.builder()
            .personId(UUID.randomUUID()).name("Maria").email("maria@petfy.com").build();

    private final Animal rex = Animal.builder().animalId(ANIMAL_ID).name("Rex").build();

    @BeforeEach
    void setUp() {
        service = new ObservationServiceImpl(
                observationRepository, animalAccessGuard, currentPersonProvider, currentProfessionalProvider);
    }

    private ObservationRequestDTO pedido(String descricao) {
        return ObservationRequestDTO.builder().description(descricao).build();
    }

    private Observation capturarSalva() {
        var captor = ArgumentCaptor.forClass(Observation.class);
        verify(observationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @BeforeEach
        void escritaLiberada() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(rex);
            when(currentPersonProvider.require()).thenReturn(maria);
            when(observationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        /**
         * <b>A regra do 3.11 virando teste:</b> "observacao, qualquer um com acesso -
         * tutor, monitor, lar transitorio, voluntario". Exigir credencial ou capacidade
         * transformaria observacao em ato clinico por via de autorizacao, que e a
         * distincao que este conceito existe para manter.
         */
        @Test
        @DisplayName("deve exigir escrita, e nada de credencial nem de capacidade")
        void exigeEscritaEMaisNada() {
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.empty());

            service.create(ANIMAL_ID, pedido("Nao comeu hoje"));

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(currentProfessionalProvider, never()).requireContext();
            verify(currentProfessionalProvider, never()).require();
        }

        @Test
        @DisplayName("sem observedAt deve assumir agora, e nao deixar nulo")
        void semObservedAtAssumeAgora() {
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.empty());
            var antes = LocalDateTime.now().minusSeconds(1);

            service.create(ANIMAL_ID, pedido("Mancou da direita"));

            assertThat(capturarSalva().getObservedAt()).isAfter(antes);
        }

        /**
         * O caso que da nome ao campo: a creche registra as 18h o que viu as 9h, e a linha
         * do tempo tem de colocar o fato as 9h (3.9). Guardar so a digitacao faria a
         * cronologia mentir sobre a ordem, que e o que permite perceber padrao.
         */
        @Test
        @DisplayName("com observedAt no passado deve guardar o fato la, e a digitacao agora")
        void guardaOFatoEADigitacaoSeparados() {
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.empty());
            var asNove = LocalDateTime.now().withHour(9).withMinute(0).minusDays(1);

            service.create(ANIMAL_ID, ObservationRequestDTO.builder()
                    .description("Nao quis passear").observedAt(asNove).build());

            var salva = capturarSalva();
            assertThat(salva.getObservedAt()).isEqualTo(asNove);
            assertThat(salva.getRecordedAt()).isAfter(asNove);
        }

        @Test
        @DisplayName("deve assinar pela organizacao declarada no header")
        void assinaPelaOrganizacaoDeclarada() {
            var creche = Organization.builder()
                    .organizationId(UUID.randomUUID()).name("Creche Pata Legal").build();
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.of(creche));

            var resposta = service.create(ANIMAL_ID, pedido("Brigou no parquinho"));

            assertThat(capturarSalva().getOrganization()).isEqualTo(creche);
            assertThat(resposta.getOrganizationName()).isEqualTo("Creche Pata Legal");
        }

        /**
         * Sem header o registro sai da pessoa, e nao de uma organizacao escolhida em
         * silencio. "A creche Pata Legal observou" e "a Maria observou" nao sao o mesmo
         * fato (3.2), e contexto nao e editavel depois (5.7).
         */
        @Test
        @DisplayName("sem organizacao declarada deve sair da pessoa, sem escolher nenhuma")
        void semDeclaracaoSaiDaPessoa() {
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.empty());

            var resposta = service.create(ANIMAL_ID, pedido("Comeu bem"));

            assertThat(capturarSalva().getOrganization()).isNull();
            assertThat(resposta.getOrganizationName()).isNull();
            assertThat(resposta.getRecordedByName()).isEqualTo("Maria");
        }

        /** O alerta do 4.5: sinaliza urgencia sem ser ato clinico nem emergencia medica. */
        @Test
        @DisplayName("deve guardar a urgencia quando marcada")
        void guardaAUrgencia() {
            when(currentProfessionalProvider.organizacaoDeclarada(eq(maria))).thenReturn(Optional.empty());

            service.create(ANIMAL_ID, ObservationRequestDTO.builder()
                    .description("Vomitou tres vezes desde a manha").urgent(true).build());

            assertThat(capturarSalva().isUrgent()).isTrue();
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        /** Ler exige apenas leitura: quem so acompanha precisa saber o que foi visto. */
        @Test
        @DisplayName("deve exigir leitura, e ordenar por quando foi visto")
        void exigeLeituraEOrdenaPeloFato() {
            when(observationRepository.findByAnimalAnimalIdOrderByObservedAtDesc(ANIMAL_ID))
                    .thenReturn(List.of(Observation.builder()
                            .observationId(UUID.randomUUID()).animal(rex)
                            .description("Mancou").observedAt(LocalDateTime.now())
                            .recordedAt(LocalDateTime.now()).recordedBy(maria).build()));

            var resultado = service.listByAnimal(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireEscrita(any());
            assertThat(resultado).singleElement()
                    .satisfies(o -> assertThat(o.getDescription()).isEqualTo("Mancou"));
        }
    }

    /**
     * Nao ha update nem delete, e a ausencia e a promessa 5.2 valendo: observacao e relato
     * de fato num instante, e mudar o relato depois nao e conserto de digitacao. Quem viu
     * outra coisa registra outra observacao, e as duas ficam.
     */
    @Test
    @DisplayName("o servico nao expoe alterar nem apagar observacao")
    void naoExpoeAlterarNemApagar() {
        assertThat(br.com.petfy.healthcare.service.ObservationService.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .containsExactlyInAnyOrder("create", "listByAnimal");
    }

}
