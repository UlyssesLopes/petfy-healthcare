package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import br.com.petfy.healthcare.domain.entity.Organization;
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
import static org.assertj.core.api.Assertions.within;
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

    /* --------------------------------------------------------- onde o dinheiro foi (Tela 37) */

    /**
     * <b>O servidor decide a categoria onde o `kind` ja responde, e nao aceita ser contrariado.</b>
     *
     * Atendimento e SAUDE por definicao. Deixar o cliente mandar ALIMENTACAO criaria duas verdades
     * sobre a mesma linha, e a errada apareceria como uma fatia torta no grafico do tutor.
     */
    @Test
    @DisplayName("atendimento deve ser SAUDE mesmo que o cliente peca outra categoria")
    void atendimentoESaudeSempre() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        var salvo = service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Consulta").amount(new BigDecimal("180.00"))
                .kind(AnimalCostKind.ATENDIMENTO)
                .category(AnimalCostCategory.ALIMENTACAO)
                .build());

        assertThat(salvo.getCategory()).isEqualTo(AnimalCostCategory.SAUDE);
    }

    @Test
    @DisplayName("a diaria e a mensalidade da creche devem ser CRECHE")
    void crecheECreche() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Diaria avulsa").amount(new BigDecimal("88.00"))
                .kind(AnimalCostKind.CRECHE_DIARIA).build()).getCategory())
                .isEqualTo(AnimalCostCategory.CRECHE);
    }

    /**
     * <b>A compra e a unica que o cliente classifica, e ela precisa.</b> Racao e remedio saem do
     * mesmo `kind` e vao para fatias diferentes — so quem tocou no botao da Tela 42 sabe qual.
     */
    @Test
    @DisplayName("a compra deve aceitar a categoria do botao, e virar OUTRO sem ela")
    void compraAceitaCategoria() {
        when(animalAccessGuard.requireEscrita(ANIMAL)).thenReturn(code);
        when(animalCostRepository.save(any(AnimalCost.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Racao").amount(new BigDecimal("289.00"))
                .category(AnimalCostCategory.ALIMENTACAO).build()).getCategory())
                .isEqualTo(AnimalCostCategory.ALIMENTACAO);

        assertThat(service.lancar(ANIMAL, AnimalCostRequestDTO.builder()
                .description("Outro").amount(new BigDecimal("40.00")).build()).getCategory())
                .as("o terceiro botao da Tela 42, e nao uma falta a corrigir")
                .isEqualTo(AnimalCostCategory.OUTRO);
    }

    /* ------------------------------------------------------------- o resumo (Tela 37) */

    @Test
    @DisplayName("o resumo deve exigir CUSTODIA — o total e o numero que alguem de fora quer saber")
    void resumoExigeCustodia() {
        when(animalAccessGuard.requireCustodia(ANIMAL))
                .thenThrow(new PetfyHealthcareException("nao responde", 403, HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.resumo(ANIMAL, null))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);

        verify(animalCostRepository, never()).findByAnimalAnimalIdOrderByOccurredAtDesc(any());
    }

    /**
     * <b>A janela recorta o total, e o "desde sempre" nao se move.</b> O desenho mostra os tres
     * cartoes juntos com "ultimos 12 meses" selecionado: o terceiro nao muda quando o recorte muda,
     * e uma consulta limitada a 12 meses nao teria como devolve-lo.
     */
    @Test
    @DisplayName("a janela de 12 meses deve recortar o total sem mexer no total de sempre")
    void janelaRecortaSemMexerNoTotalDeSempre() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(
                        custo("Consulta", "180.00", AnimalCostCategory.SAUDE, 1),
                        custo("Mensalidade", "530.00", AnimalCostCategory.CRECHE, 30),
                        // fora da janela: aconteceu ha tres anos
                        custo("Consulta antiga", "1000.00", AnimalCostCategory.SAUDE, 1100)));

        var dozeMeses = service.resumo(ANIMAL, "DOZE_MESES");

        assertThat(dozeMeses.getWindow()).isEqualTo("DOZE_MESES");
        assertThat(dozeMeses.getTotal()).isEqualByComparingTo("710.00");
        assertThat(dozeMeses.getTotalEver())
                .as("o cartao 'desde 2019' nao muda quando a janela muda")
                .isEqualByComparingTo("1710.00");
        assertThat(dozeMeses.getFirstYear())
                .isEqualTo(LocalDateTime.now().minusDays(1100).getYear());

        var sempre = service.resumo(ANIMAL, "SEMPRE");

        assertThat(sempre.getWindow()).isEqualTo("SEMPRE");
        assertThat(sempre.getTotal()).isEqualByComparingTo("1710.00");
    }

    /** Janela desconhecida cai em DOZE_MESES: o padrao do desenho, e nao um 400. */
    @Test
    @DisplayName("janela nula ou desconhecida deve virar DOZE_MESES")
    void janelaDesconhecidaViraDozeMeses() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of());

        assertThat(service.resumo(ANIMAL, null).getWindow()).isEqualTo("DOZE_MESES");
        assertThat(service.resumo(ANIMAL, "TRIMESTRE").getWindow()).isEqualTo("DOZE_MESES");
    }

    /**
     * <b>A media divide pelos meses do RECORTE, e nao pelos meses em que houve gasto.</b>
     *
     * Um animal que custou R$ 1.200 em dois meses do ano custou R$ 100 por mes no ano. Dividir por
     * dois daria R$ 600 e diria ao tutor que o animal custa seis vezes mais do que custa.
     */
    @Test
    @DisplayName("a media mensal deve dividir pelos 12 meses da janela, e nao pelos meses com gasto")
    void mediaDividePelaJanela() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(
                        custo("Cirurgia", "600.00", AnimalCostCategory.SAUDE, 10),
                        custo("Internacao", "600.00", AnimalCostCategory.SAUDE, 40)));

        assertThat(service.resumo(ANIMAL, "DOZE_MESES").getMonthlyAverage())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("animal sem custo nenhum deve responder zero, e nao nulo nem erro")
    void semCustoNenhum() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of());

        var resumo = service.resumo(ANIMAL, "SEMPRE");

        assertThat(resumo.getTotal()).isEqualByComparingTo("0");
        assertThat(resumo.getMonthlyAverage())
                .as("um animal de um dia nao pode dividir por zero")
                .isEqualByComparingTo("0.00");
        assertThat(resumo.getFirstYear()).isNull();
        assertThat(resumo.getByCategory()).isEmpty();
        assertThat(resumo.getByPayer()).isEmpty();
    }

    /**
     * <b>Categoria sem valor no recorte nao aparece, e nao aparece com zero.</b> Uma fatia de R$ 0 na
     * legenda de um grafico e uma linha que a pessoa le, tenta entender e nao ganha nada com — o
     * mesmo critério do "nenhum losango" da Tela 40.
     *
     * E a ORDEM importa porque a frase do desenho depende dela: "saude e o MENOR pedaco do gasto".
     */
    @Test
    @DisplayName("as fatias devem vir do maior para o menor, e sem categoria vazia")
    void fatiasOrdenadasESemVazia() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(
                        custo("Consulta", "180.00", AnimalCostCategory.SAUDE, 2),
                        custo("Mensalidade", "530.00", AnimalCostCategory.CRECHE, 5),
                        custo("Diaria", "88.00", AnimalCostCategory.CRECHE, 8),
                        custo("Racao", "289.00", AnimalCostCategory.ALIMENTACAO, 12)));

        assertThat(service.resumo(ANIMAL, "DOZE_MESES").getByCategory())
                .extracting(fatia -> fatia.getCategory())
                .as("HIGIENE nao existe aqui porque o petshop ainda nao registra valor")
                .containsExactly(AnimalCostCategory.CRECHE, AnimalCostCategory.ALIMENTACAO,
                        AnimalCostCategory.SAUDE);

        assertThat(service.resumo(ANIMAL, "DOZE_MESES").getByCategory().get(0).getAmount())
                .as("a creche soma mensalidade e diaria na mesma fatia")
                .isEqualByComparingTo("618.00");
    }

    /**
     * <b>QUEM PAGOU E QUEM LANCOU EM NOME PROPRIO, e o criterio e a organizacao ser nula.</b>
     *
     * Num atendimento o autor e a veterinaria: ela informou o valor, e nao disse quem o pagou.
     * Chama-la de pagadora seria inventar um fato — e o cartao existe para mostrar, nao para cobrar.
     */
    @Test
    @DisplayName("so tem pagador o que uma pessoa lancou; o resto vai para a linha sem nome")
    void pagadorSoDeQuemLancouEmNomeProprio() {
        Organization clinica = Organization.builder()
                .organizationId(UUID.randomUUID()).name("Clinica Vet Norte").build();
        Person ana = Person.builder().personId(UUID.randomUUID()).name("Ana Ferreira").build();

        AnimalCost daClinica = custo("Consulta", "180.00", AnimalCostCategory.SAUDE, 3);
        daClinica.setRecordedBy(ana);
        daClinica.setOrganization(clinica);

        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(
                        daClinica,
                        custo("Racao", "289.00", AnimalCostCategory.ALIMENTACAO, 12)));

        var pagadores = service.resumo(ANIMAL, "DOZE_MESES").getByPayer();

        assertThat(pagadores).hasSize(2);
        assertThat(pagadores.get(0).getPersonName())
                .as("do maior para o menor: a racao veio de quem lancou em nome proprio")
                .isEqualTo("Marcelo Dias");
        assertThat(pagadores.get(0).getAmount()).isEqualByComparingTo("289.00");
        assertThat(pagadores.get(0).getCategories()).containsExactly(AnimalCostCategory.ALIMENTACAO);

        assertThat(pagadores.get(1).getPersonName())
                .as("a veterinaria informou o valor, e nao disse quem pagou")
                .isNull();
        assertThat(pagadores.get(1).getAmount()).isEqualByComparingTo("180.00");
    }

    /** As linhas somam o total: se a linha sem nome fosse escondida, a conta nao fecharia. */
    @Test
    @DisplayName("a soma das linhas de pagador deve fechar com o total")
    void pagadoresFechamComOTotal() {
        AnimalCost daClinica = custo("Consulta", "180.00", AnimalCostCategory.SAUDE, 3);
        daClinica.setOrganization(Organization.builder()
                .organizationId(UUID.randomUUID()).name("Clinica Vet Norte").build());

        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(daClinica,
                        custo("Racao", "289.00", AnimalCostCategory.ALIMENTACAO, 12)));

        var resumo = service.resumo(ANIMAL, "DOZE_MESES");

        assertThat(resumo.getByPayer().stream()
                .map(linha -> linha.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(resumo.getTotal());

        assertThat(resumo.getByCategory().stream()
                .map(fatia -> fatia.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .as("a soma das fatias tambem: dois numeros que nao fecham fazem o tutor "
                        + "nao saber em qual acreditar")
                .isEqualByComparingTo(resumo.getTotal());
    }

    /**
     * <b>O inicio do recorte viaja, e e o que faz a LISTA fechar com o total.</b>
     *
     * A Tela 37 mostra "cada valor veio de um evento" logo abaixo dos tres numeros. Se a tela
     * calculasse "hoje menos 12 meses" por conta propria, as duas contas divergiriam na virada do
     * mes e no dia 31 — e o tutor veria uma lista que nao soma o numero em cima dela.
     */
    @Test
    @DisplayName("o resumo deve devolver o inicio do recorte, e nulo em SEMPRE")
    void devolveOInicioDoRecorte() {
        when(animalAccessGuard.requireCustodia(ANIMAL)).thenReturn(code);
        when(animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(ANIMAL))
                .thenReturn(List.of(custo("Racao", "289.00", AnimalCostCategory.ALIMENTACAO, 5)));

        assertThat(service.resumo(ANIMAL, "DOZE_MESES").getFrom())
                .as("doze meses atras, decidido no servidor")
                .isCloseTo(LocalDateTime.now().minusMonths(12),
                        within(1, java.time.temporal.ChronoUnit.MINUTES));

        assertThat(service.resumo(ANIMAL, "SEMPRE").getFrom())
                .as("sem corte: a lista e a vida inteira, e o total tambem")
                .isNull();
    }

    /** Um custo do recorte, lancado por Marcelo em nome proprio, ha `diasAtras` dias. */
    private AnimalCost custo(String descricao, String valor, AnimalCostCategory categoria,
                             int diasAtras) {
        return AnimalCost.builder()
                .animalCostId(UUID.randomUUID())
                .animal(code)
                .description(descricao)
                .amount(new BigDecimal(valor))
                .kind(AnimalCostKind.COMPRA)
                .category(categoria)
                .occurredAt(LocalDateTime.now().minusDays(diasAtras))
                .recordedBy(marcelo)
                .creationDate(LocalDateTime.now())
                .build();
    }

}
