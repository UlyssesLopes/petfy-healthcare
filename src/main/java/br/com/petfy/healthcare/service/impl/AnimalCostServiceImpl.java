package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalCostPayerDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostResponseDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostSliceDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostSummaryResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.dto.CostForecastResponseDTO;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O custo do animal (Telas 40, 41 e 42).
 *
 * <b>A ASSIMETRIA E A REGRA INTEIRA: escreve quem registra, le so quem responde.</b>
 *
 * Escrever exige o mesmo que registrar qualquer evento — a clinica lanca o valor do atendimento
 * que ela mesma acabou de fazer, e a creche lanca a mensalidade que ela combinou. Se escrever
 * exigisse custodia, o custo so entraria se o tutor digitasse, e o desenho abre justamente
 * recusando isso: "o custo do animal so existe se o dado entrar sem esforco".
 *
 * <b>Ler exige CUSTODIA, e nenhum escopo substitui.</b> "O que a Clinica Vet Norte cobra do
 * Marcelo nao e assunto da creche, do petshop nem de outra clinica. (...) Nenhum escopo de acesso
 * concede preco junto com saude." E por isso que o custo nao entra na linha do tempo: la quem
 * governa a leitura e o escopo, e aqui o escopo nao pode governar nada.
 *
 * <b>A clinica nao le o que ela mesma escreveu, e isso e proposital.</b> Ela sabe o que cobrou —
 * esta no sistema dela. O que ela nao pode ver e o que as OUTRAS cobraram, e uma regra que
 * abrisse excecao para "o meu" precisaria distinguir os dois na leitura, com um filtro que a
 * primeira consulta mal escrita derruba.
 */
@Service
@RequiredArgsConstructor
public class AnimalCostServiceImpl implements AnimalCostService {

    /**
     * As duas janelas do desenho: "ultimos 12 meses" e "desde 2019".
     *
     * <b>Sao duas e nao um numero livre de meses</b>, porque o desenho oferece dois botoes. Um
     * parametro aberto convidaria uma tela a pedir 90 dias, e ai a media mensal passaria a comparar
     * recortes que ninguem desenhou.
     */
    private static final String JANELA_DOZE_MESES = "DOZE_MESES";
    private static final String JANELA_SEMPRE = "SEMPRE";
    private static final int MESES_DA_JANELA = 12;

    private final AnimalCostRepository animalCostRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    /* as quatro fontes da previsao (Tela 38), e a peca que faz a conta com elas */
    private final VaccineRepository vaccineRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CostForecastBuilder costForecastBuilder;

    /**
     * O que o tutor ve: tudo que se gastou com o animal.
     *
     * <b>{@code requireCustodia} e nao {@code requireLeitura}</b> — e a unica leitura deste
     * produto que exige responder pelo animal em vez de alcanca-lo.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AnimalCostResponseDTO> doAnimal(UUID animalId) {
        animalAccessGuard.requireCustodia(animalId);

        return animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(animalId)
                .stream()
                .map(AnimalCostServiceImpl::toResponse)
                .toList();
    }

    /**
     * Lanca um valor.
     *
     * <b>{@code requireEscrita}</b>: quem pode registrar um evento no animal pode dizer quanto ele
     * custou. E o mesmo alcance que ja permitiu registrar o atendimento — pedir mais aqui faria o
     * valor ficar de fora justamente de quem tem o dado na mao.
     */
    @Override
    @Transactional
    public AnimalCostResponseDTO lancar(UUID animalId, AnimalCostRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person eu = currentPersonProvider.require();

        AnimalCostKind tipo = request.getKind() == null ? AnimalCostKind.COMPRA : request.getKind();

        AnimalCost custo = animalCostRepository.save(AnimalCost.builder()
                .animal(animal)
                .description(request.getDescription().trim())
                .amount(request.getAmount())
                .kind(tipo)
                .category(categoriaDe(tipo, request.getCategory()))
                .paid(request.getPaid())
                .recurrence(request.getRecurrence())
                .coversMonths(request.getCoversMonths())
                .occurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt())
                .sourceHealthRecordId(request.getSourceHealthRecordId())
                .sourceEnrollmentId(request.getSourceEnrollmentId())
                .sourceVaccineId(request.getSourceVaccineId())
                .sourceAntiparasiticId(request.getSourceAntiparasiticId())
                .sourceCareInstructionId(request.getSourceCareInstructionId())
                .recordedBy(eu)
                .organization(currentProfessionalProvider.organizacaoDeclarada(eu).orElse(null))
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(custo);
    }

    /**
     * "Quanto o Code custou" (Tela 37).
     *
     * <b>Soma em memoria, e nao em quatro consultas agregadas.</b> Os totais, as fatias e os
     * pagadores saem todos do MESMO recorte, e uma consulta por painel abriria a porta para os
     * numeros divergirem — a soma das fatias deixando de fechar com o total e o defeito que faz o
     * tutor nao saber em qual dos dois acreditar. O recorte e por animal e cabe em memoria: a
     * propria tela lista "cada valor veio de um evento", entao a lista inteira ja atravessa a
     * fronteira de qualquer forma.
     *
     * <b>Le TUDO e recorta depois</b>, porque o cartao "desde 2019" nao muda quando a janela muda:
     * ele precisa do total de sempre e do ano do primeiro valor, que uma consulta limitada a 12
     * meses nao teria como devolver.
     */
    @Override
    @Transactional(readOnly = true)
    public AnimalCostSummaryResponseDTO resumo(UUID animalId, String window) {
        animalAccessGuard.requireCustodia(animalId);

        List<AnimalCost> tudo = animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(animalId);

        boolean sempre = JANELA_SEMPRE.equalsIgnoreCase(window);
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime desde = sempre ? null : agora.minusMonths(MESES_DA_JANELA);

        List<AnimalCost> recorte = desde == null
                ? tudo
                : tudo.stream().filter(custo -> !custo.getOccurredAt().isBefore(desde)).toList();

        BigDecimal total = somar(recorte);

        Optional<LocalDateTime> primeiro = tudo.stream()
                .map(AnimalCost::getOccurredAt)
                .min(LocalDateTime::compareTo);

        return AnimalCostSummaryResponseDTO.builder()
                .window(sempre ? JANELA_SEMPRE : JANELA_DOZE_MESES)
                .from(desde)
                .total(total)
                .monthlyAverage(mediaMensal(total, sempre ? mesesDesde(primeiro, agora) : MESES_DA_JANELA))
                .totalEver(somar(tudo))
                .firstYear(primeiro.map(LocalDateTime::getYear).orElse(null))
                .byCategory(fatias(recorte))
                .byPayer(pagadores(recorte))
                .build();
    }

    /**
     * "Os proximos 12 meses do Code" (Tela 38).
     *
     * <b>Le as quatro fontes e entrega a aritmetica ao {@link CostForecastBuilder}.</b> A conta mora
     * numa peca sem repositorio de proposito: ela e o que precisa de teste denso, porque um erro ali
     * aparece como um numero plausivel e errado na frente do tutor — e um numero plausivel ninguem
     * confere.
     */
    @Override
    @Transactional(readOnly = true)
    public CostForecastResponseDTO previsao(UUID animalId) {
        animalAccessGuard.requireCustodia(animalId);

        return costForecastBuilder.montar(
                LocalDate.now(),
                vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId),
                antiparasiticRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId),
                enrollmentRepository.findVivasDoAnimal(animalId),
                animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(animalId));
    }

    /**
     * "Onde foi", fatia por fatia.
     *
     * <b>Categoria sem valor no recorte nao aparece</b>, e nao aparece com zero: uma fatia de R$ 0
     * na legenda de um grafico e uma linha que a pessoa le, tenta entender e nao ganha nada com. E
     * o mesmo criterio do "nenhum losango" da Tela 40 — ausencia nao e pendencia.
     *
     * Ordenado do maior para o menor porque e assim que a barra e lida, e porque a frase do desenho
     * depende da ordem: "saude e o MENOR pedaco do gasto do Code".
     */
    private static List<AnimalCostSliceDTO> fatias(List<AnimalCost> recorte) {
        Map<AnimalCostCategory, BigDecimal> porCategoria = new EnumMap<>(AnimalCostCategory.class);

        for (AnimalCost custo : recorte) {
            porCategoria.merge(custo.getCategory(), custo.getAmount(), BigDecimal::add);
        }

        return porCategoria.entrySet().stream()
                .sorted(Map.Entry.<AnimalCostCategory, BigDecimal>comparingByValue().reversed())
                .map(fatia -> AnimalCostSliceDTO.builder()
                        .category(fatia.getKey())
                        .amount(fatia.getValue())
                        .build())
                .toList();
    }

    /**
     * "Quem pagou o que."
     *
     * <b>O CRITERIO E `organization == null`, e nao o `kind`.</b> Quem lancou em nome proprio pagou;
     * quem registrou em nome de uma organizacao informou o valor, e nao disse quem o pagou. Num
     * atendimento o autor e a veterinaria — chama-la de pagadora seria inventar um fato.
     *
     * Por isso a linha sem nome existe: e onde entra tudo que veio de organizacao. Escondida, a soma
     * das linhas nao fecharia com o total logo acima, e o cartao pareceria dizer que o resto do
     * dinheiro nao existiu.
     */
    private static List<AnimalCostPayerDTO> pagadores(List<AnimalCost> recorte) {
        Map<String, BigDecimal> porPessoa = new LinkedHashMap<>();
        Map<String, Set<AnimalCostCategory>> categoriasDaPessoa = new LinkedHashMap<>();

        for (AnimalCost custo : recorte) {
            // a chave vazia e a linha sem nome: LinkedHashMap nao aceita nula, e o DTO devolve nulo
            String quem = custo.getOrganization() != null || custo.getRecordedBy() == null
                    ? ""
                    : custo.getRecordedBy().getName();

            porPessoa.merge(quem, custo.getAmount(), BigDecimal::add);
            categoriasDaPessoa
                    .computeIfAbsent(quem, chave -> EnumSet.noneOf(AnimalCostCategory.class))
                    .add(custo.getCategory());
        }

        return porPessoa.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(linha -> AnimalCostPayerDTO.builder()
                        .personName(linha.getKey().isEmpty() ? null : linha.getKey())
                        .amount(linha.getValue())
                        .categories(List.copyOf(categoriasDaPessoa.get(linha.getKey())))
                        .build())
                .toList();
    }

    private static BigDecimal somar(List<AnimalCost> custos) {
        return custos.stream()
                .map(AnimalCost::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * A media por mes.
     *
     * <b>Divide pelos meses do RECORTE, e nao pelos meses em que houve gasto.</b> Um animal que
     * custou R$ 1.200 em dois meses do ano custou R$ 100 por mes no ano; dividir por dois daria
     * R$ 600 e diria ao tutor que o animal custa seis vezes mais do que custa.
     *
     * HALF_UP e duas casas porque o resultado e dinheiro na tela, e nao um numero intermediario.
     */
    private static BigDecimal mediaMensal(BigDecimal total, long meses) {
        if (meses <= 0) {
            return total.setScale(2, RoundingMode.HALF_UP);
        }

        return total.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
    }

    /** Quantos meses o "desde sempre" cobre. Pelo menos um: um animal de um dia nao divide por zero. */
    private static long mesesDesde(Optional<LocalDateTime> primeiro, LocalDateTime agora) {
        return primeiro
                .map(inicio -> Math.max(1, ChronoUnit.MONTHS.between(inicio, agora) + 1))
                .orElse(1L);
    }

    /**
     * Onde o dinheiro foi (Tela 37).
     *
     * <b>O SERVIDOR DECIDE ONDE O `kind` JA RESPONDE, e nao aceita ser contrariado ali.</b>
     * Atendimento e SAUDE e mensalidade e CRECHE por definicao — deixar o cliente mandar outra coisa
     * criaria duas verdades sobre a mesma linha, e a que estivesse errada apareceria como uma fatia
     * torta no grafico do tutor, sem ninguem saber de onde veio.
     *
     * <b>A COMPRA e a unica que o cliente classifica</b>, e ela precisa: racao e remedio saem do
     * mesmo `kind` e vao para fatias diferentes, e so quem tocou no botao da Tela 42 sabe qual.
     * Nulo ali vira OUTRO, que e o terceiro botao — e nao uma falta a corrigir.
     */
    private static AnimalCostCategory categoriaDe(AnimalCostKind tipo, AnimalCostCategory pedida) {
        return switch (tipo) {
            case ATENDIMENTO, VACINA -> AnimalCostCategory.SAUDE;
            case CRECHE_MENSALIDADE, CRECHE_DIARIA -> AnimalCostCategory.CRECHE;
            case COMPRA -> pedida == null ? AnimalCostCategory.OUTRO : pedida;
        };
    }

    private static AnimalCostResponseDTO toResponse(AnimalCost custo) {
        return AnimalCostResponseDTO.builder()
                .animalCostId(custo.getAnimalCostId())
                .description(custo.getDescription())
                .amount(custo.getAmount())
                .kind(custo.getKind())
                .category(custo.getCategory())
                .paid(custo.getPaid())
                .recurrence(custo.getRecurrence())
                .coversMonths(custo.getCoversMonths())
                .sourceCareInstructionId(custo.getSourceCareInstructionId())
                .occurredAt(custo.getOccurredAt())
                .recordedByName(custo.getRecordedBy() == null ? null : custo.getRecordedBy().getName())
                .organizationName(custo.getOrganization() == null ? null : custo.getOrganization().getName())
                .sourceHealthRecordId(custo.getSourceHealthRecordId())
                .sourceEnrollmentId(custo.getSourceEnrollmentId())
                .build();
    }

}
