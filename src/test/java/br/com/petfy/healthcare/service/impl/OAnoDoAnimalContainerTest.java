package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalYearService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O ano do animal, contra Postgres real (Tela 48).
 *
 * <b>"Não é retrospectiva."</b> É um documento clínico, e <i>"por isso ele inclui o que deu errado —
 * os 23 dias com a vacina vencida, a displasia sem reavaliação desde 2023. Um resumo que só mostra o
 * bonito não serve para cuidar."</i>
 *
 * <b>Container, e não unidade, porque metade dos números vem da view.</b> A contagem de eventos e a
 * de quem cuidou saem de `animal_timeline`, que é uma view com `UNION ALL` sobre seis tabelas — o que
 * ela devolve não está em código Java nenhum. Um mock confirmaria que o serviço chama o repositório, e
 * a pergunta que importa é se o ano fecha com os números certos.
 */
@SpringBootTest
@DisplayName("o ano do animal, contra Postgres real")
class OAnoDoAnimalContainerTest extends PostgresContainerTest {

    @Autowired private AnimalYearService animalYearService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private AnimalWeightHistoryRepository weightRepository;
    @Autowired private AnimalHealthConditionRepository conditionRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private OrganizationRepository organizationRepository;

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 12);

    private Person marcelo;
    private Animal code;

    @BeforeEach
    void montarOAno() {
        marcelo = pessoa("Marcelo Dias");

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA)
                .bornDate(LocalDate.of(2019, 2, 14))
                .creationDate(LocalDateTime.now())
                .build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(marcelo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(7))
                .build());

        agirComo(marcelo);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /** "Agosto de 2025 a agosto de 2026 · sétimo ano dele." */
    @Test
    @DisplayName("a janela e de doze meses, e o ano de vida vem do nascimento")
    void aJanelaEOAnoDeVida() {
        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getFrom()).isEqualTo(LocalDate.of(2025, 8, 12));
        assertThat(ano.getTo()).isEqualTo(HOJE);
        // nasceu em 02/2019: em 08/2026 ele esta no oitavo ano de vida
        assertThat(ano.getYearOfLife()).isEqualTo(8);
    }

    /**
     * <b>O cálculo que faz este documento valer.</b> "A antirrábica ficou 23 dias vencida em julho."
     *
     * A irregularidade não está gravada em lugar nenhum: ela é a leitura de duas linhas — a dose que
     * venceu e a que veio depois.
     */
    @Test
    @DisplayName("acha os dias em que a vacina esteve vencida, entre uma dose e a seguinte")
    void osDiasVencidos() {
        // venceu em 01/07/2026, e a proxima dose so veio em 24/07/2026: 23 dias
        vacina("Antirrabica", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 7, 1));
        vacina("Antirrabica", LocalDate.of(2026, 7, 24), LocalDate.of(2027, 7, 24));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getLapses())
                .filteredOn(l -> "Antirrabica".equals(l.getVaccineName()))
                .singleElement()
                .satisfies(l -> {
                    assertThat(l.getDays()).isEqualTo(23);
                    assertThat(l.getOverdueSince()).isEqualTo(LocalDate.of(2026, 7, 1));
                    assertThat(l.getRegularizedOn()).isEqualTo(LocalDate.of(2026, 7, 24));
                });
    }

    /**
     * <b>A vacina que venceu e ninguém regularizou.</b> O `regularizedOn` nulo é informação, e não
     * ausência de dado: ela está vencida agora, e o documento diz isso.
     */
    @Test
    @DisplayName("a vacina ainda vencida conta ate hoje, e sem data de regularizacao")
    void aVacinaAindaVencida() {
        vacina("V10", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getLapses())
                .filteredOn(l -> "V10".equals(l.getVaccineName()))
                .singleElement()
                .satisfies(l -> {
                    assertThat(l.getRegularizedOn()).isNull();
                    // de 01/06/2026 a 12/08/2026
                    assertThat(l.getDays()).isEqualTo(72);
                });
    }

    /**
     * <b>A irregularidade é recortada na borda da janela.</b> Uma vacina que venceu em novembro do
     * ano anterior e foi regularizada em setembro conta só os dias dentro do ano de que o documento
     * fala — dizer "300 dias vencida" num ano em que ele esteve irregular por 30 seria mentir contra o
     * próprio animal.
     */
    @Test
    @DisplayName("a irregularidade que comecou antes da janela conta so o pedaco de dentro")
    void aIrregularidadeRecortada() {
        // venceu em 12/07/2025, antes da janela (que comeca em 12/08/2025), e so foi regularizada
        // em 11/09/2025 — dentro dela
        vacina("Giardia", LocalDate.of(2024, 7, 12), LocalDate.of(2025, 7, 12));
        vacina("Giardia", LocalDate.of(2025, 9, 11), LocalDate.of(2026, 9, 11));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getLapses())
                .filteredOn(l -> "Giardia".equals(l.getVaccineName()))
                .singleElement()
                .satisfies(l -> {
                    assertThat(l.getOverdueSince()).isEqualTo(LocalDate.of(2025, 8, 12));
                    // de 12/08 a 11/09 sao 30 dias, e nao os 61 desde o vencimento
                    assertThat(l.getDays()).isEqualTo(30);
                });
    }

    /**
     * "A displasia dele não foi reavaliada desde 2023."
     *
     * O critério é a própria linha da condição: se ninguém a tocou dentro da janela, ninguém a
     * reavaliou. Deduzir de "houve consulta, logo foi reavaliada" afirmaria algo que nenhum registro
     * sustenta.
     */
    @Test
    @DisplayName("a condicao cronica aberta e intocada no ano aparece como nao reavaliada")
    void aCondicaoNaoReavaliada() {
        condicao("Displasia coxofemoral", LocalDate.of(2023, 5, 10),
                LocalDateTime.of(2023, 5, 10, 9, 0), null);

        // e uma que foi tocada DENTRO da janela nao aparece
        condicao("Alergia a frango", LocalDate.of(2022, 1, 1),
                LocalDateTime.of(2026, 3, 2, 9, 0), null);

        // nem uma que foi resolvida
        condicao("Otite", LocalDate.of(2021, 1, 1),
                LocalDateTime.of(2021, 6, 1, 9, 0), LocalDate.of(2021, 8, 1));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getNotReassessed())
                .extracting(p -> p.getDescription())
                .contains("Displasia coxofemoral")
                .doesNotContain("Alergia a frango", "Otite");

        assertThat(ano.getNotReassessed())
                .filteredOn(p -> "Displasia coxofemoral".equals(p.getDescription()))
                .singleElement()
                .satisfies(p -> assertThat(p.getLastTouchedOn().getYear()).isEqualTo(2023));
    }

    /**
     * "8,6 kg · era 7,5 kg em agosto passado."
     *
     * As duas pontas da janela, e não uma média: a média de doze meses esconderia exatamente o que a
     * frase mostra, que é que ele engordou.
     */
    @Test
    @DisplayName("o peso vem das duas pontas da janela, e ignora o de fora")
    void oPesoDasDuasPontas() {
        peso(6.9, LocalDate.of(2025, 1, 10));   // fora da janela
        peso(7.5, LocalDate.of(2025, 8, 20));
        peso(8.6, LocalDate.of(2026, 8, 1));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getWeightNow()).isEqualTo(8.6);
        assertThat(ano.getWeightBefore()).isEqualTo(7.5);
    }

    /**
     * <b>A janela é a regra, e ela vale para tudo.</b> O atendimento de dois anos atrás não entra no
     * ano — e é isso que separa este documento de um extrato da vida inteira.
     */
    @Test
    @DisplayName("o que aconteceu fora da janela nao entra na contagem")
    void oQueEstaForaNaoEntra() {
        atendimento("Consulta de rotina", LocalDate.of(2026, 3, 4));
        atendimento("Dermatite", LocalDate.of(2026, 8, 2));
        atendimento("Consulta antiga", LocalDate.of(2024, 1, 15));

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getAppointments()).isEqualTo(2);
        assertThat(ano.getRecords()).isGreaterThanOrEqualTo(2);
    }

    /**
     * "Quem cuidou dele este ano", do que mais registrou para o que menos — e a dupla (pessoa,
     * organização) é a unidade, porque é assim que o desenho escreve.
     */
    @Test
    @DisplayName("quem cuidou vem contado e ordenado")
    void quemCuidouDele() {
        Organization creche = organizationRepository.saveAndFlush(Organization.builder()
                .name("Creche Quintal").creationDate(LocalDateTime.now()).build());

        Person rafaela = pessoa("Rafaela Lopes");

        atendimentoPor("Banho e tosa", LocalDate.of(2026, 5, 5), rafaela, creche);
        atendimentoPor("Retorno", LocalDate.of(2026, 6, 6), rafaela, creche);
        atendimentoPor("Consulta", LocalDate.of(2026, 7, 7), marcelo, null);

        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getCaregivers()).isNotEmpty();

        assertThat(ano.getCaregivers().get(0).getRecords())
                .as("a lista nao veio do que mais registrou para o que menos")
                .isGreaterThanOrEqualTo(ano.getCaregivers()
                        .get(ano.getCaregivers().size() - 1).getRecords());

        assertThat(ano.getCaregivers())
                .filteredOn(c -> "Rafaela Lopes".equals(c.getPersonName()))
                .anySatisfy(c -> {
                    assertThat(c.getOrganizationName()).isEqualTo("Creche Quintal");
                    assertThat(c.getRecords()).isEqualTo(2);
                });
    }

    /** Um animal sem nada registrado não é um erro: é um ano em branco, e o documento diz isso. */
    @Test
    @DisplayName("o ano vazio responde com zeros, e nao com erro")
    void oAnoVazio() {
        var ano = animalYearService.resumo(code.getAnimalId(), HOJE);

        assertThat(ano.getRecords()).isZero();
        assertThat(ano.getDaycareDays()).isZero();
        assertThat(ano.getBoardingDays()).isZero();
        assertThat(ano.getWeightNow()).isNull();
        assertThat(ano.getLapses()).isEmpty();
    }

    /** Sem `to`, a janela termina hoje — e é o caso comum de quem abre a tela. */
    @Test
    @DisplayName("sem data, a janela termina hoje")
    void semDataTerminaHoje() {
        var ano = animalYearService.resumo(code.getAnimalId(), null);

        assertThat(ano.getTo()).isEqualTo(LocalDate.now());
        assertThat(ano.getFrom()).isEqualTo(LocalDate.now().minusYears(1));
    }

    private void vacina(String nome, LocalDate aplicada, LocalDate proxima) {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code).recordedBy(marcelo)
                .vaccineName(nome)
                .applicationDate(aplicada)
                .nextDoseDate(proxima)
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void peso(double quanto, LocalDate quando) {
        weightRepository.saveAndFlush(AnimalWeightHistory.builder()
                .animal(code).recordedBy(marcelo)
                .weight(quanto).measuredAt(quando)
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void condicao(String descricao, LocalDate desde,
                          LocalDateTime tocadaEm, LocalDate resolvida) {
        conditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(code).recordedBy(marcelo)
                .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                .description(descricao)
                .since(desde)
                .resolvedAt(resolvida)
                .creationDate(tocadaEm)
                .updateDate(tocadaEm)
                .build());
    }

    private void atendimento(String tipo, LocalDate quando) {
        atendimentoPor(tipo, quando, marcelo, null);
    }

    private void atendimentoPor(String tipo, LocalDate quando, Person quem, Organization onde) {
        healthRecordRepository.saveAndFlush(HealthRecord.builder()
                .animal(code).recordedBy(quem).organization(onde)
                .category(HealthEventCategory.CONSULTA)
                .eventType(tipo)
                .eventDate(quando)
                .creationDate(LocalDateTime.now())
                .build());
    }

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome)
                .email(nome.split(" ")[0].toLowerCase() + "-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private void agirComo(Person pessoa) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }
}
