package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClosedLifeResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalDeath;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.EnrollmentStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.AnimalDeathNotifier;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O fim da linha do tempo (Tela 33).
 *
 * <b>O que estes casos protegem nao e o caminho feliz — e a diferenca entre encerrar e apagar.</b>
 * Um defeito aqui nao aparece como erro: aparece como um tutor que perdeu o animal recebendo
 * lembrete de vacina, ou como uma creche esperando na segunda-feira um bicho que morreu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("o fim da linha do tempo")
class AnimalDeathServiceImplTest {

    @Mock private AnimalAccessGuard animalAccessGuard;
    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private AnimalDeathRepository animalDeathRepository;
    @Mock private CustodyRepository custodyRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private TimelineRepository timelineRepository;
    @Mock private AnimalDeathNotifier animalDeathNotifier;
    @Mock private TimelineRepository.Tamanho tamanho;

    private AnimalDeathServiceImpl service;

    private static final UUID ANIMAL = UUID.randomUUID();

    private final Person marcelo = Person.builder()
            .personId(UUID.randomUUID()).name("Marcelo Dias").build();

    private final Animal code = Animal.builder()
            .animalId(ANIMAL).name("Code").bornDate(LocalDate.of(2019, 3, 14)).build();

    @BeforeEach
    void setUp() {
        service = new AnimalDeathServiceImpl(animalAccessGuard, currentPersonProvider,
                animalDeathRepository, custodyRepository, enrollmentRepository,
                timelineRepository, animalDeathNotifier);

        lenient().when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        lenient().when(currentPersonProvider.require()).thenReturn(marcelo);
        lenient().when(animalDeathRepository.saveAndFlush(any(AnimalDeath.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));
        lenient().when(custodyRepository.findEmCurso(ANIMAL)).thenReturn(Optional.empty());
        lenient().when(enrollmentRepository.findVivasDoAnimal(ANIMAL)).thenReturn(List.of());
        lenient().when(timelineRepository.tamanhoDe(ANIMAL)).thenReturn(tamanho);
        lenient().when(tamanho.getEventos()).thenReturn(153L);
        lenient().when(tamanho.getPessoas()).thenReturn(6L);
        lenient().when(tamanho.getOrganizacoes()).thenReturn(3L);
    }

    private AnimalDeathRequestDTO pedido(LocalDate quando) {
        return AnimalDeathRequestDTO.builder().deceasedOn(quando).build();
    }

    @Test
    @DisplayName("encerra a custodia com OBITO e sem sucessor")
    void encerraCustodia() {
        Custody emCurso = Custody.builder()
                .animal(code).holderPerson(marcelo)
                .startedAt(LocalDateTime.of(2019, 4, 1, 10, 0)).build();

        when(custodyRepository.findEmCurso(ANIMAL)).thenReturn(Optional.of(emCurso));
        when(custodyRepository.saveAndFlush(any(Custody.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        service.registrar(ANIMAL, pedido(LocalDate.now().minusDays(4)));

        assertThat(emCurso.getEndReason()).isEqualTo(CustodyEndReason.OBITO);
        assertThat(emCurso.getEndedAt()).isNotNull();
        // o quarto invariante do produto admite a excecao aqui, e ela e o assunto do enum:
        // animal morto nao precisa de alguem que responda por ele
        assertThat(emCurso.getSuccessor()).isNull();
    }

    @Test
    @DisplayName("encerra a matricula viva, e nao so a ativa")
    void encerraMatriculas() {
        Enrollment pendente = Enrollment.builder()
                .animal(code).status(EnrollmentStatus.PENDENTE).build();
        Enrollment ativa = Enrollment.builder()
                .animal(code).status(EnrollmentStatus.ATIVA).build();

        when(enrollmentRepository.findVivasDoAnimal(ANIMAL)).thenReturn(List.of(pendente, ativa));

        service.registrar(ANIMAL, pedido(LocalDate.now()));

        // a pendente tambem espera um animal que nao vem: deixar so a ativa faria a creche
        // continuar cobrando a comprovacao de saude de um bicho morto
        assertThat(pendente.getStatus()).isEqualTo(EnrollmentStatus.ENCERRADA);
        assertThat(ativa.getStatus()).isEqualTo(EnrollmentStatus.ENCERRADA);
        assertThat(pendente.getEndedAt()).isNotNull();
        verify(enrollmentRepository).saveAll(List.of(pendente, ativa));
    }

    @Test
    @DisplayName("avisa quem cuidava do animal")
    void avisa() {
        service.registrar(ANIMAL, pedido(LocalDate.now()));

        verify(animalDeathNotifier).animalMorreu(code, marcelo);
    }

    @Test
    @DisplayName("exige quem responde pelo animal, e nao quem tem escrita concedida")
    void exigeCustodia() {
        service.registrar(ANIMAL, pedido(LocalDate.now()));

        // a veterinaria que atendeu na ultima noite registra o obito como ato clinico dela; fechar
        // a linha do tempo e de quem responde, e nenhum nivel de concessao chega la
        verify(animalAccessGuard).requireCustodia(ANIMAL);
        verify(animalAccessGuard, never()).requireEscrita(any());
    }

    @Test
    @DisplayName("recusa data no futuro")
    void recusaFuturo() {
        assertThatThrownBy(() -> service.registrar(ANIMAL, pedido(LocalDate.now().plusDays(1))))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(animalDeathRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("recusa data anterior ao nascimento")
    void recusaAntesDoNascimento() {
        assertThatThrownBy(() -> service.registrar(ANIMAL, pedido(LocalDate.of(2018, 1, 1))))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasMessageContaining("anterior ao nascimento");
    }

    @Test
    @DisplayName("recusa encerrar duas vezes, em vez de sobrescrever a data")
    void recusaDuasVezes() {
        when(animalDeathRepository.existsById(ANIMAL)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(ANIMAL, pedido(LocalDate.now())))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // sobrescrever trocaria a data que o tutor informou da primeira vez — o unico campo do
        // formulario que ele nao consegue reconstruir depois
        verify(animalDeathRepository, never()).saveAndFlush(any());
        verify(animalDeathNotifier, never()).animalMorreu(any(), any());
    }

    @Test
    @DisplayName("aceita animal sem custodia em curso: a perda ja pode ter encerrado")
    void semCustodiaEmCurso() {
        ClosedLifeResponseDTO ficha = service.registrar(ANIMAL, pedido(LocalDate.now()));

        assertThat(ficha.getDeceasedOn()).isEqualTo(LocalDate.now());
        assertThat(ficha.getHolderSince()).isNull();
    }

    @Test
    @DisplayName("texto em branco vira nulo, e nao despedida vazia")
    void brancoViraNulo() {
        AnimalDeathRequestDTO comBrancos = AnimalDeathRequestDTO.builder()
                .deceasedOn(LocalDate.now())
                .place("   ")
                .farewellNote("")
                .build();

        ClosedLifeResponseDTO ficha = service.registrar(ANIMAL, comBrancos);

        assertThat(ficha.getPlace()).isNull();
        assertThat(ficha.getFarewellNote()).isNull();
    }

    @Test
    @DisplayName("a ficha fechada traz os numeros do cartao de depois")
    void numerosDoCartao() {
        ClosedLifeResponseDTO ficha = service.registrar(ANIMAL, pedido(LocalDate.now()));

        assertThat(ficha.getEventCount()).isEqualTo(153L);
        assertThat(ficha.getCaregiverPersonCount()).isEqualTo(6L);
        assertThat(ficha.getCaregiverOrganizationCount()).isEqualTo(3L);
        assertThat(ficha.getBornDate()).isEqualTo(LocalDate.of(2019, 3, 14));
    }

    @Test
    @DisplayName("a ficha fechada responde 404 no animal que nao foi encerrado")
    void fichaDeAnimalVivo() {
        when(animalAccessGuard.requireLeitura(ANIMAL)).thenReturn(code);
        when(animalDeathRepository.findById(ANIMAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.daFichaFechada(ANIMAL))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

}
