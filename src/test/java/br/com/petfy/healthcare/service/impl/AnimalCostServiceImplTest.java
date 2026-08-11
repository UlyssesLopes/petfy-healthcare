package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
 * O custo do animal, e a assimetria que o define: <b>escreve quem registra, le so quem responde</b>.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("o custo do animal")
class AnimalCostServiceImplTest {

    @Mock private AnimalCostRepository animalCostRepository;
    @Mock private AnimalAccessGuard animalAccessGuard;
    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private CurrentProfessionalProvider currentProfessionalProvider;

    private AnimalCostServiceImpl service;

    private static final UUID ANIMAL = UUID.randomUUID();

    private final Person marcelo = Person.builder()
            .personId(UUID.randomUUID()).name("Marcelo Dias").build();
    private final Animal code = Animal.builder().animalId(ANIMAL).name("Code").build();

    @BeforeEach
    void setUp() {
        service = new AnimalCostServiceImpl(animalCostRepository, animalAccessGuard,
                currentPersonProvider, currentProfessionalProvider);

        lenient().when(currentPersonProvider.require()).thenReturn(marcelo);
        lenient().when(currentProfessionalProvider.organizacaoDeclarada(any())).thenReturn(Optional.empty());
    }

    /**
     * <b>A regra que o desenho crava:</b> "nenhum escopo de acesso concede preco junto com saude".
     *
     * O teste mede que a leitura passa por {@code requireCustodia} e nao por {@code requireLeitura}
     * — sao guardas diferentes, e a segunda deixaria a creche ler o que a clinica cobrou.
     */
    @Test
    @DisplayName("ler o custo deve exigir CUSTODIA, e nao apenas alcancar o animal")
    void lerExigeCustodia() {
        when(animalAccessGuard.requireCustodia(ANIMAL))
                .thenThrow(new PetfyHealthcareException("nao responde", 403, HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.doAnimal(ANIMAL))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);

        verify(animalCostRepository, never()).findByAnimalAnimalIdOrderByOccurredAtDesc(any());
    }

    /**
     * <b>Escrever pede ESCRITA, e nao custodia.</b> Se pedisse custodia, o custo so entraria se o
     * tutor digitasse — e o desenho abre recusando isso: "o custo do animal so existe se o dado
     * entrar sem esforco". A clinica lanca o valor do atendimento que ela mesma acabou de fazer.
     */
    @Test
    @DisplayName("lancar deve exigir escrita, para a clinica poder lancar o que ela cobrou")
    void lancarExigeEscritaENaoCustodia() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Consulta dermatologica").amount(new BigDecimal("180.00")).build());

        verify(animalAccessGuard).requireEscrita(ANIMAL);
        verify(animalAccessGuard, never()).requireCustodia(any());
    }

    /**
     * "Ja foi pago" nulo e "ninguem disse", e nao "nao foi pago".
     *
     * O desenho oferece a caixa e nao a obriga, e {@code false} afirmaria sobre algo que ninguem
     * afirmou — o tutor leria "nao pago" num atendimento que ele pagou na saida.
     */
    @Test
    @DisplayName("nao informar se foi pago deve gravar nulo, e nunca false")
    void pagoNaoInformadoFicaNulo() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        var salvo = service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Consulta").amount(new BigDecimal("180.00")).build());

        assertThat(salvo.getPaid()).isNull();
    }

    /** Sem tipo informado e COMPRA: o lancamento do tutor, que e o unico manual do produto. */
    @Test
    @DisplayName("sem tipo informado deve virar COMPRA")
    void semTipoViraCompra() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        var salvo = service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Racao").amount(new BigDecimal("289.00"))
                .recurrence(CostRecurrence.MENSAL).build());

        assertThat(salvo.getKind()).isEqualTo(AnimalCostKind.COMPRA);
        assertThat(salvo.getRecurrence()).isEqualTo(CostRecurrence.MENSAL);
    }

    /**
     * O valor carrega quem o registrou.
     *
     * "Cada valor tem um evento por tras, com autor e data — e por isso pode ser contestado como
     * qualquer outro registro." Um numero sem nome ao lado apareceria sozinho na conta do tutor.
     */
    @Test
    @DisplayName("o valor deve guardar quem o registrou")
    void guardaQuemRegistrou() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(AnimalCost.builder()
                        .animalCostId(UUID.randomUUID()).animal(code)
                        .description("Consulta").amount(new BigDecimal("180.00"))
                        .kind(AnimalCostKind.ATENDIMENTO).occurredAt(LocalDateTime.now())
                        .recordedBy(marcelo).creationDate(LocalDateTime.now())
                        .build()));

        assertThat(service.doAnimal(ANIMAL))
                .singleElement()
                .satisfies(custo -> {
                    assertThat(custo.getRecordedByName()).isEqualTo("Marcelo Dias");
                    assertThat(custo.getAmount()).isEqualByComparingTo("180.00");
                });
    }

}
