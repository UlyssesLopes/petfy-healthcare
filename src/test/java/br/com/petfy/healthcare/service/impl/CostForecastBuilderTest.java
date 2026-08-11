package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.CostForecastResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.ClassGroup;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A aritmetica de "o que vem pela frente" (Tela 38).
 *
 * <b>Sem mock nenhum, e e por isso que a peca existe separada.</b> Um erro aqui nao aparece como
 * excecao: aparece como um numero plausivel e errado na frente do tutor — e numero plausivel ninguem
 * confere. Entao a conta e testada como conta.
 *
 * <b>"Isto nao e previsao de gasto: e o que JA ESTA MARCADO no registro dele."</b> Todo caso abaixo
 * mede que a linha saiu de um fato escrito, e nao de uma media.
 */
@DisplayName("o que vem pela frente, e quanto")
class CostForecastBuilderTest {

    private final CostForecastBuilder builder = new CostForecastBuilder();

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 11);

    private final Animal code = Animal.builder().animalId(UUID.randomUUID()).name("Code").build();

    private final VaccineCatalog antirrabica = VaccineCatalog.builder()
            .vaccineCatalogId(UUID.randomUUID()).code("ANTIRRABICA").name("Antirrabica").build();

    /**
     * <b>O caso do desenho, inteiro.</b> Antirrabica vencida, antiparasitario em setembro, a
     * mensalidade que se repete — e o total multiplicando o que se repete.
     */
    @Test
    @DisplayName("a dose vencida vem primeiro, e a mensalidade conta doze vezes no total")
    void oCasoDoDesenho() {
        Vaccine dose = vacina("Antirrabica", antirrabica, HOJE.minusYears(1).minusDays(23),
                HOJE.minusDays(23));

        Antiparasitic antip = antiparasitario("Antipulgas", HOJE.minusMonths(3), HOJE.plusMonths(1));

        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(dose), List.of(antip), List.of(matricula("530.00")),
                List.of(custoDaDose(dose, "90.00", HOJE.minusYears(1).minusDays(23))));

        assertThat(previsao.getItems())
                .extracting(item -> item.getKind())
                .as("do mais atrasado ao mais distante, e o que se repete no fim")
                .containsExactly("DOSE_DE_VACINA", "ANTIPARASITARIO", "CRECHE_MENSALIDADE");

        assertThat(previsao.getItems().get(0).isOverdue())
                .as("'antirrabica · vencida ha 23 dias' e a linha que muda o que a pessoa faz hoje")
                .isTrue();

        assertThat(previsao.getItems().get(0).getAmount())
                .as("o preco vem da dose anterior DESTE animal")
                .isEqualByComparingTo("90.00");

        // 90 (dose, 1x) + 530 x 12 (mensalidade) = 6.450. O antiparasitario nunca teve valor.
        assertThat(previsao.getTotal()).isEqualByComparingTo("6450.00");
        assertThat(previsao.getItemsWithoutAmount())
                .as("o antiparasitario entrou sem preco, e o total precisa dizer isso")
                .isEqualTo(1);
    }

    /**
     * <b>Sem `timesInTwelveMonths` a previsao de um ano diria menos da metade do que o animal vai
     * custar.</b> A mensalidade de R$ 530 entraria como R$ 530.
     */
    @Test
    @DisplayName("a mensalidade deve entrar doze vezes, e nao uma")
    void mensalidadeDozeVezes() {
        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(), List.of(), List.of(matricula("530.00")), List.of());

        assertThat(previsao.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getTimesInTwelveMonths()).isEqualTo(12);
                    assertThat(item.getDueOn())
                            .as("nao tem uma data: tem todas, e o desenho escreve 'todo mes'")
                            .isNull();
                });

        assertThat(previsao.getTotal()).isEqualByComparingTo("6360.00");
    }

    /**
     * <b>SEM PROXIMA DOSE NAO E LINHA.</b> Dose unica e registro antigo sem prazo existem, e
     * inventar uma data poria no orcamento do tutor um gasto que ninguem marcou.
     */
    @Test
    @DisplayName("dose sem data de reforco nao deve virar linha")
    void doseSemPrazoNaoEntra() {
        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(vacina("Giardia", null, HOJE.minusMonths(2), null)),
                List.of(), List.of(), List.of());

        assertThat(previsao.getItems()).isEmpty();
        assertThat(previsao.getTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("dose marcada para depois de doze meses fica fora da janela")
    void doseAlemDaJanelaNaoEntra() {
        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(vacina("V10", null, HOJE.minusMonths(1), HOJE.plusMonths(13))),
                List.of(), List.of(), List.of());

        assertThat(previsao.getItems()).isEmpty();
    }

    /**
     * <b>Uma linha por SERIE, e nao por dose registrada.</b> Um animal de dez anos tem dez
     * antirrabicas na carteira, e nove delas nao vao acontecer de novo: a que carrega a proxima data
     * e a ultima aplicada.
     */
    @Test
    @DisplayName("dez antirrabicas na carteira devem virar UMA linha, a da ultima dose")
    void umaLinhaPorSerie() {
        List<Vaccine> carteira = List.of(
                vacina("Antirrabica", antirrabica, HOJE.minusYears(3), HOJE.minusYears(2)),
                vacina("Antirrabica", antirrabica, HOJE.minusYears(2), HOJE.minusYears(1)),
                vacina("Antirrabica", antirrabica, HOJE.minusMonths(6), HOJE.plusMonths(6)));

        CostForecastResponseDTO previsao = builder.montar(HOJE, carteira, List.of(), List.of(), List.of());

        assertThat(previsao.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getDueOn()).isEqualTo(HOJE.plusMonths(6));
                    assertThat(item.isOverdue())
                            .as("a serie esta em dia: as doses velhas nao a tornam atrasada")
                            .isFalse();
                });
    }

    /**
     * <b>A MAIS RECENTE GANHA, porque preco de vacina sobe.</b> A dose de 2019 nao diz o que a de
     * 2027 vai custar; a do ano passado diz o mais proximo que os fatos permitem.
     */
    @Test
    @DisplayName("o preco deve vir da dose anterior mais recente, e nao da mais antiga")
    void precoVemDaMaisRecente() {
        Vaccine antiga = vacina("Antirrabica", antirrabica, HOJE.minusYears(3), HOJE.minusYears(2));
        Vaccine recente = vacina("Antirrabica", antirrabica, HOJE.minusYears(1), HOJE.plusMonths(1));

        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(antiga, recente), List.of(), List.of(),
                List.of(custoDaDose(antiga, "60.00", HOJE.minusYears(3)),
                        custoDaDose(recente, "90.00", HOJE.minusYears(1))));

        assertThat(previsao.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getAmount()).isEqualByComparingTo("90.00");
                    assertThat(item.getAmountFrom())
                            .as("um valor previsto sem procedencia e um palpite com cara de fato")
                            .isEqualTo("DOSE_ANTERIOR:" + HOJE.minusYears(1));
                });
    }

    /**
     * <b>O preco NAO atravessa series.</b> Precificar a antirrabica com o que a V10 custou seria o
     * mesmo erro de precificar pelo ultimo custo de SAUDE — e e o erro que a ligacao pelo id existe
     * para impedir.
     */
    @Test
    @DisplayName("o valor de uma vacina nao deve precificar outra")
    void precoNaoAtravessaSeries() {
        VaccineCatalog v10 = VaccineCatalog.builder()
                .vaccineCatalogId(UUID.randomUUID()).code("V10").name("V10").build();

        Vaccine doseDeV10 = vacina("V10", v10, HOJE.minusYears(1), HOJE.minusYears(1));
        Vaccine doseDeRaiva = vacina("Antirrabica", antirrabica, HOJE.minusMonths(11), HOJE.plusMonths(1));

        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(doseDeV10, doseDeRaiva), List.of(), List.of(),
                List.of(custoDaDose(doseDeV10, "120.00", HOJE.minusYears(1))));

        assertThat(previsao.getItems())
                .filteredOn(item -> "Antirrabica".equals(item.getDescription()))
                .singleElement()
                .satisfies(item -> assertThat(item.getAmount())
                        .as("a antirrabica nunca teve valor informado, e o da V10 nao serve")
                        .isNull());
    }

    /**
     * <b>Casa por NOME quando nao ha catalogo</b>, pelo mesmo criterio da comprovacao da creche: a
     * `catalog` e nula em dose de texto livre e em tudo que entrou antes do catalogo existir, e
     * ignorar isso faria o produto nao achar preco anterior para um animal de carteira antiga.
     */
    @Test
    @DisplayName("dose sem catalogo deve casar o preco pelo nome")
    void casaPorNomeSemCatalogo() {
        Vaccine anterior = vacina("antirrabica", null, HOJE.minusYears(1), HOJE.minusYears(1));
        Vaccine proxima = vacina("Antirrabica ", null, HOJE.minusMonths(11), HOJE.plusMonths(1));

        CostForecastResponseDTO previsao = builder.montar(HOJE,
                List.of(anterior, proxima), List.of(), List.of(),
                List.of(custoDaDose(anterior, "90.00", HOJE.minusYears(1))));

        assertThat(previsao.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getAmount()).isEqualByComparingTo("90.00"));
    }

    /**
     * <b>Doze racoes lancadas nao sao doze racoes por mes.</b> A caixinha "dura cerca de um mes"
     * afirma "isto se repete", e nao "isto e um gasto novo" — somar todas diria que o tutor compra
     * uma racao por mes mais as onze anteriores.
     */
    @Test
    @DisplayName("compras mensais repetidas devem virar UMA linha, a mais recente")
    void comprasMensaisNaoSomam() {
        CostForecastResponseDTO previsao = builder.montar(HOJE, List.of(), List.of(), List.of(),
                List.of(
                        compraMensal("Racao", "270.00", HOJE.minusMonths(2)),
                        compraMensal("Racao", "289.00", HOJE.minusMonths(1)),
                        compraMensal("Remedio", "80.00", HOJE.minusMonths(1))));

        assertThat(previsao.getItems()).hasSize(2);
        assertThat(previsao.getItems())
                .filteredOn(item -> "Racao".equals(item.getDescription()))
                .singleElement()
                .satisfies(item -> assertThat(item.getAmount())
                        .as("a mais recente: e o preco que vale hoje")
                        .isEqualByComparingTo("289.00"));

        // (289 + 80) x 12
        assertThat(previsao.getTotal()).isEqualByComparingTo("4428.00");
    }

    /** Compra avulsa nao entra: sem a caixinha marcada, ela nao afirma que se repete. */
    @Test
    @DisplayName("compra sem recorrencia nao deve entrar na previsao")
    void compraAvulsaNaoEntra() {
        AnimalCost avulsa = compraMensal("Coleira", "60.00", HOJE.minusMonths(1));
        avulsa.setRecurrence(null);

        assertThat(builder.montar(HOJE, List.of(), List.of(), List.of(), List.of(avulsa)).getItems())
                .isEmpty();
    }

    /** Matricula sem mensalidade combinada nao vira linha: nao ha valor a prever. */
    @Test
    @DisplayName("matricula sem mensalidade nao deve entrar")
    void matriculaSemMensalidadeNaoEntra() {
        Enrollment semValor = matricula("530.00");
        semValor.setMonthlyFee(null);

        assertThat(builder.montar(HOJE, List.of(), List.of(), List.of(semValor), List.of()).getItems())
                .isEmpty();
    }

    @Test
    @DisplayName("animal sem nada marcado deve responder previsao vazia, e nao erro")
    void semNadaMarcado() {
        CostForecastResponseDTO previsao =
                builder.montar(HOJE, List.of(), List.of(), List.of(), List.of());

        assertThat(previsao.getItems()).isEmpty();
        assertThat(previsao.getTotal()).isEqualByComparingTo("0");
        assertThat(previsao.getItemsWithoutAmount()).isZero();
    }

    /* -------------------------------------------------------------------------- bastidores */

    private Vaccine vacina(String nome, VaccineCatalog catalogo, LocalDate aplicada, LocalDate proxima) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .animal(code)
                .vaccineName(nome)
                .catalog(catalogo)
                .applicationDate(aplicada)
                .nextDoseDate(proxima)
                .creationDate(LocalDateTime.now())
                .build();
    }

    private Antiparasitic antiparasitario(String nome, LocalDate aplicada, LocalDate proxima) {
        return Antiparasitic.builder()
                .antiparasiticId(UUID.randomUUID())
                .animal(code)
                .name(nome)
                .applicationDate(aplicada)
                .nextDoseDate(proxima)
                .creationDate(LocalDateTime.now())
                .build();
    }

    private Enrollment matricula(String mensalidade) {
        Organization creche = Organization.builder()
                .organizationId(UUID.randomUUID()).name("Creche Quintal").build();

        return Enrollment.builder()
                .enrollmentId(UUID.randomUUID())
                .animal(code)
                .classGroup(ClassGroup.builder()
                        .classGroupId(UUID.randomUUID()).name("Tarde").organization(creche).build())
                .monthlyFee(new BigDecimal(mensalidade))
                .requestedAt(LocalDateTime.now())
                .build();
    }

    private AnimalCost custoDaDose(Vaccine dose, String valor, LocalDate quando) {
        return AnimalCost.builder()
                .animalCostId(UUID.randomUUID())
                .animal(code)
                .description(dose.getVaccineName())
                .amount(new BigDecimal(valor))
                .kind(AnimalCostKind.ATENDIMENTO)
                .category(AnimalCostCategory.SAUDE)
                .occurredAt(quando.atStartOfDay())
                .sourceVaccineId(dose.getVaccineId())
                .creationDate(LocalDateTime.now())
                .build();
    }

    private AnimalCost compraMensal(String descricao, String valor, LocalDate quando) {
        return AnimalCost.builder()
                .animalCostId(UUID.randomUUID())
                .animal(code)
                .description(descricao)
                .amount(new BigDecimal(valor))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.ALIMENTACAO)
                .recurrence(CostRecurrence.MENSAL)
                .occurredAt(quando.atStartOfDay())
                .creationDate(LocalDateTime.now())
                .build();
    }

}
