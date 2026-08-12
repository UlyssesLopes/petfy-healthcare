package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.CostForecastItemDTO;
import br.com.petfy.healthcare.domain.dto.CostForecastResponseDTO;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "O que vem pela frente, e o custo de adiar" (Tela 38).
 *
 * <b>PECA PROPRIA, e nao um metodo no {@code AnimalCostServiceImpl}.</b> Ela nao decide acesso, nao
 * escreve nada e nao toca em repositorio: recebe o que ja foi lido e devolve a previsao. Isso a
 * torna testavel sem mock nenhum — e a aritmetica desta tela e exatamente o que precisa de teste
 * denso, porque um erro aqui aparece como um numero plausivel e errado na frente do tutor.
 *
 * <b>"ISTO NAO E PREVISAO DE GASTO: E O QUE JA ESTA MARCADO NO REGISTRO DELE."</b> Quatro fontes,
 * todas fatos escritos:
 *
 * <ul>
 *   <li><b>Dose de vacina</b> com data de proxima dose — a carteira ja diz quando.</li>
 *   <li><b>Antiparasitario</b> no intervalo de reforco, pela mesma razao.</li>
 *   <li><b>Mensalidade da creche</b>, do combinado da Tela 41.</li>
 *   <li><b>Compra marcada como mensal</b> — a caixinha "dura cerca de um mes" da Tela 42, que e o
 *       que "transforma uma compra avulsa em custo mensal previsivel".</li>
 * </ul>
 *
 * <b>O QUE FICA DE FORA, e por que:</b> a orientacao em curso NAO entra como linha propria. O
 * remedio de um tratamento de sete dias ja foi pago e ja esta na conta como custo do atendimento —
 * repeti-lo aqui contaria o mesmo dinheiro duas vezes. O tratamento continuo do desenho ("o
 * condroprotetor do Teco, sem previsao de parar") aparece pela compra mensal, que e onde o dinheiro
 * dele realmente sai todo mes.
 *
 * <b>E O PROGNOSTICO NAO ENTRA NUNCA.</b> "Ela pode dizer que adiar a vacina custa dias de creche
 * perdidos, porque isso e aritmetica sobre fatos registrados. Nunca vai dizer que tratar a displasia
 * agora sai mais barato que operar depois — isso e prognostico clinico, e o Petfy nao faz
 * prognostico."
 */
@Component
@RequiredArgsConstructor
public class CostForecastBuilder {

    private static final int MESES = 12;

    public CostForecastResponseDTO montar(LocalDate hoje,
                                          List<Vaccine> vacinas,
                                          List<Antiparasitic> antiparasitarios,
                                          List<Enrollment> matriculasVivas,
                                          List<AnimalCost> custos) {
        LocalDate limite = hoje.plusMonths(MESES);
        Map<String, PrecoAnterior> precos = precosPorSerie(vacinas, antiparasitarios, custos);

        List<CostForecastItemDTO> itens = new ArrayList<>();

        itens.addAll(dosesDeVacina(vacinas, hoje, limite, precos));
        itens.addAll(antiparasitarios(antiparasitarios, hoje, limite, precos));
        itens.addAll(mensalidades(matriculasVivas));
        itens.addAll(comprasMensais(custos));

        /*
         * O QUE JA VENCEU VEM PRIMEIRO, e o que se repete vai para o fim.
         *
         * "Antirrabica · vencida ha 23 dias" e a linha que muda o que a pessoa faz hoje; a
         * mensalidade e a que ela ja conhece. O desenho poe exatamente nessa ordem.
         */
        itens.sort(Comparator.comparing(CostForecastItemDTO::getDueOn,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return CostForecastResponseDTO.builder()
                .items(List.copyOf(itens))
                .total(somar(itens))
                .itemsWithoutAmount((int) itens.stream().filter(i -> i.getAmount() == null).count())
                .build();
    }

    /* ------------------------------------------------------------------------- as fontes */

    /**
     * A proxima dose de cada serie de vacina.
     *
     * <b>Uma linha por SERIE, e nao por dose registrada.</b> Um animal de dez anos tem dez
     * antirrabicas na carteira, e nove delas nao vao acontecer de novo — a que importa e a mais
     * recente, porque e a dela que sai a proxima data.
     */
    private List<CostForecastItemDTO> dosesDeVacina(List<Vaccine> vacinas, LocalDate hoje,
                                                    LocalDate limite,
                                                    Map<String, PrecoAnterior> precos) {
        List<CostForecastItemDTO> itens = new ArrayList<>();

        for (Vaccine dose : maisRecentePorSerie(vacinas, CostForecastBuilder::serieDaVacina,
                Vaccine::getApplicationDate)) {
            LocalDate quando = dose.getNextDoseDate();

            // SEM PROXIMA DOSE NAO E LINHA: dose unica e registro antigo sem prazo existem, e
            // inventar uma data para eles poria no orcamento do tutor um gasto que ninguem marcou
            if (quando == null || quando.isAfter(limite)) {
                continue;
            }

            itens.add(linha("DOSE_DE_VACINA", dose.getVaccineId(), dose.getVaccineName(),
                    AnimalCostCategory.SAUDE, quando, hoje, 1, precos.get(serieDaVacina(dose))));
        }

        return itens;
    }

    private List<CostForecastItemDTO> antiparasitarios(List<Antiparasitic> registros, LocalDate hoje,
                                                       LocalDate limite,
                                                       Map<String, PrecoAnterior> precos) {
        List<CostForecastItemDTO> itens = new ArrayList<>();

        for (Antiparasitic registro : maisRecentePorSerie(registros,
                CostForecastBuilder::serieDoAntiparasitario, Antiparasitic::getApplicationDate)) {
            LocalDate quando = registro.getNextDoseDate();

            if (quando == null || quando.isAfter(limite)) {
                continue;
            }

            itens.add(linha("ANTIPARASITARIO", registro.getAntiparasiticId(), registro.getName(),
                    AnimalCostCategory.SAUDE, quando, hoje, 1,
                    precos.get(serieDoAntiparasitario(registro))));
        }

        return itens;
    }

    /**
     * A mensalidade combinada, doze vezes.
     *
     * <b>Sem data e "todo mes"</b>, e nao uma data inventada: o desenho escreve exatamente "todo
     * mes" nessa linha. O dia de vencimento existe no combinado e nao entra aqui — ele diz quando
     * pagar, e nao quando o gasto acontece.
     */
    private List<CostForecastItemDTO> mensalidades(List<Enrollment> matriculasVivas) {
        return matriculasVivas.stream()
                .filter(matricula -> matricula.getMonthlyFee() != null)
                .map(matricula -> CostForecastItemDTO.builder()
                        .kind("CRECHE_MENSALIDADE")
                        .sourceId(matricula.getEnrollmentId())
                        .description(matricula.getClassGroup().getOrganization().getName())
                        .category(AnimalCostCategory.CRECHE)
                        .timesInTwelveMonths(MESES)
                        .amount(matricula.getMonthlyFee())
                        .amountFrom("CRECHE_COMBINADO")
                        .build())
                .toList();
    }

    /**
     * "Dura cerca de um mes" — a caixinha da Tela 42, doze vezes.
     *
     * <b>So a MAIS RECENTE de cada descricao entra.</b> Um tutor que lanca racao todo mes com a
     * caixinha marcada tem doze linhas iguais na base; somar as doze diria que ele gasta doze racoes
     * por mes. O que a caixinha afirma e "isto se repete", e nao "isto e um gasto novo".
     */
    private List<CostForecastItemDTO> comprasMensais(List<AnimalCost> custos) {
        Map<String, AnimalCost> maisRecentePorDescricao = new HashMap<>();

        for (AnimalCost custo : custos) {
            if (custo.getRecurrence() != CostRecurrence.MENSAL) {
                continue;
            }

            String chave = custo.getDescription() == null ? "" : custo.getDescription().trim().toLowerCase();

            maisRecentePorDescricao.merge(chave, custo,
                    (antigo, novo) -> novo.getOccurredAt().isAfter(antigo.getOccurredAt()) ? novo : antigo);
        }

        return maisRecentePorDescricao.values().stream()
                .map(custo -> CostForecastItemDTO.builder()
                        .kind("COMPRA_MENSAL")
                        .sourceId(custo.getAnimalCostId())
                        .description(custo.getDescription())
                        .category(custo.getCategory())
                        .timesInTwelveMonths(MESES)
                        .amount(custo.getAmount())
                        .amountFrom("COMPRA_MENSAL_ANTERIOR")
                        .build())
                .toList();
    }

    /* --------------------------------------------------------------------- o preco anterior */

    /**
     * Quanto custou a ultima vez, por serie.
     *
     * <b>A LIGACAO E PELO ID DA DOSE, e nao por texto.</b> O custo aponta para a `vaccines` pelo
     * `sourceVaccineId` desde a V39; daquela dose sai o item de catalogo, e do item de catalogo sai
     * a serie. Casar "Antirrabica" por descricao seria o LIKE que este projeto recusa desde os dias
     * da semana — e aqui ele erraria para dentro do bolso do tutor.
     *
     * <b>A mais recente ganha</b>, porque preco de vacina sobe: a dose de 2019 nao diz o que a de
     * 2027 vai custar, e a de ano passado diz o mais proximo que os fatos permitem.
     */
    private Map<String, PrecoAnterior> precosPorSerie(List<Vaccine> vacinas,
                                                      List<Antiparasitic> antiparasitarios,
                                                      List<AnimalCost> custos) {
        Map<UUID, String> serieDaDose = new HashMap<>();

        vacinas.forEach(dose -> serieDaDose.put(dose.getVaccineId(), serieDaVacina(dose)));
        antiparasitarios.forEach(registro ->
                serieDaDose.put(registro.getAntiparasiticId(), serieDoAntiparasitario(registro)));

        Map<String, PrecoAnterior> precos = new HashMap<>();

        for (AnimalCost custo : custos) {
            UUID origem = custo.getSourceVaccineId() != null
                    ? custo.getSourceVaccineId()
                    : custo.getSourceAntiparasiticId();

            if (origem == null) {
                continue;
            }

            String serie = serieDaDose.get(origem);

            if (serie == null) {
                continue;
            }

            PrecoAnterior candidato = new PrecoAnterior(custo.getAmount(), custo.getOccurredAt().toLocalDate());

            precos.merge(serie, candidato,
                    (antigo, novo) -> novo.quando().isAfter(antigo.quando()) ? novo : antigo);
        }

        return precos;
    }

    /**
     * A chave da serie.
     *
     * <b>Pelo catalogo quando ha catalogo, e pelo nome quando nao ha.</b> A `Vaccine.catalog` e nula
     * para dose digitada em texto livre e para tudo que entrou antes do catalogo existir — ignorar
     * esses registros faria o produto nao achar preco anterior nenhum para um animal cuja carteira
     * inteira e antiga. E o mesmo criterio do {@code comprovar} da creche.
     */
    private static String serieDaVacina(Vaccine dose) {
        return dose.getCatalog() != null
                ? "vacina:catalogo:" + dose.getCatalog().getVaccineCatalogId()
                : "vacina:nome:" + normalizar(dose.getVaccineName());
    }

    private static String serieDoAntiparasitario(Antiparasitic registro) {
        return registro.getCatalog() != null
                ? "antip:catalogo:" + registro.getCatalog().getAntiparasiticCatalogId()
                : "antip:nome:" + normalizar(registro.getName());
    }

    private static String normalizar(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase();
    }

    /* -------------------------------------------------------------------------- bastidores */

    private CostForecastItemDTO linha(String kind, UUID sourceId, String descricao,
                                      AnimalCostCategory categoria, LocalDate quando, LocalDate hoje,
                                      int vezes, PrecoAnterior preco) {
        return CostForecastItemDTO.builder()
                .kind(kind)
                .sourceId(sourceId)
                .description(descricao)
                .category(categoria)
                .dueOn(quando)
                .overdue(quando.isBefore(hoje))
                .timesInTwelveMonths(vezes)
                .amount(preco == null ? null : preco.valor())
                // a procedencia do numero: um valor previsto sem ela e um palpite com cara de fato
                .amountFrom(preco == null ? null : "DOSE_ANTERIOR:" + preco.quando())
                .build();
    }

    /**
     * A mais recente de cada serie.
     *
     * Um animal de dez anos tem dez antirrabicas, e nove delas nao vao acontecer de novo: a que
     * carrega a proxima data e a ultima aplicada.
     */
    private <T> List<T> maisRecentePorSerie(List<T> registros,
                                            java.util.function.Function<T, String> serie,
                                            java.util.function.Function<T, LocalDate> aplicadaEm) {
        Map<String, T> porSerie = new HashMap<>();

        for (T registro : registros) {
            porSerie.merge(serie.apply(registro), registro, (antigo, novo) -> {
                LocalDate dataAntiga = aplicadaEm.apply(antigo);
                LocalDate dataNova = aplicadaEm.apply(novo);

                if (dataNova == null) {
                    return antigo;
                }

                return dataAntiga == null || dataNova.isAfter(dataAntiga) ? novo : antigo;
            });
        }

        return List.copyOf(porSerie.values());
    }

    /**
     * O total, multiplicando o que se repete.
     *
     * <b>{@code timesInTwelveMonths} nao e decoracao:</b> sem ele a mensalidade de R$ 530 entraria
     * como R$ 530 num ano, e a previsao diria menos da metade do que o animal vai custar.
     */
    private BigDecimal somar(List<CostForecastItemDTO> itens) {
        return itens.stream()
                .filter(item -> item.getAmount() != null)
                .map(item -> item.getAmount().multiply(BigDecimal.valueOf(item.getTimesInTwelveMonths())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Quanto custou, e quando — a data existe para a tela poder dizer de onde tirou o numero. */
    private record PrecoAnterior(BigDecimal valor, LocalDate quando) {
    }

}
