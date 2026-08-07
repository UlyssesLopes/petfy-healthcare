package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Observation;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import br.com.petfy.healthcare.domain.entity.TimelineEventType;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A linha do tempo contra Postgres de verdade.
 *
 * <b>Este teste nao tem como ser feito com mock.</b> A linha do tempo e uma view SQL
 * com {@code UNION ALL} sobre seis tabelas: o que ela devolve nao esta em codigo Java
 * nenhum, esta na V27. Um mock de {@code TimelineRepository} confirmaria apenas que o
 * servico chama o repositorio - e a pergunta que importa e se o banco monta a
 * cronologia certa.
 *
 * O H2 tambem nao serviria: a view usa {@code ::timestamp} e {@code COALESCE} sobre
 * colunas de tipos diferentes por tabela, e "provavelmente funciona no Postgres" e
 * exatamente a divida que os ContainerTest existem para nao deixar acumular.
 */
@SpringBootTest
@Transactional
@DisplayName("linha do tempo contra Postgres real")
class TimelineContainerTest extends PostgresContainerTest {

    @Autowired private TimelineRepository timelineRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private AnimalWeightHistoryRepository animalWeightHistoryRepository;
    @Autowired private AnimalHealthConditionRepository animalHealthConditionRepository;
    @Autowired private ProfessionalCredentialRepository professionalCredentialRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Autowired private ObservationRepository observationRepository;

    private Person ulysses;
    private Animal rex;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.saveAndFlush(Person.builder()
                .name("Ulysses")
                .email("timeline-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
    }

    /**
     * <b>O caso que da nome ao produto.</b> O tutor cadastra hoje a vacina de 2019 da
     * carteirinha de papel, e ela tem de aparecer em 2019 - nao hoje. Linha do tempo
     * ordenada por data de digitacao mente sobre a ordem dos fatos de saude, e e
     * justamente a ordem que permite perceber padrao.
     */
    @Test
    @DisplayName("ordena por quando aconteceu, e nao por quando foi registrado")
    void ordenaPeloFatoENaoPeloRegistro() {
        // registrada agora, aplicada em 2019
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("Antirrabica 2019")
                .applicationDate(LocalDate.of(2019, 5, 10))
                .creationDate(LocalDateTime.now())
                .build());

        // registrada antes, aplicada em 2026
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("V10 2026")
                .applicationDate(LocalDate.of(2026, 3, 1))
                .creationDate(LocalDateTime.now().minusDays(30))
                .build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .extracting(TimelineEntry::getSummary)
                .containsExactly("V10 2026", "Antirrabica 2019");
    }

    /**
     * Os seis tipos numa lista so - e o ponto da view. Antes disso a cronologia exigia
     * seis chamadas e uma ordenacao em memoria, feita por cada cliente do seu jeito.
     */
    @Test
    @DisplayName("junta os tipos de evento numa lista so")
    void juntaOsTiposNumaListaSo() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("V10")
                .applicationDate(LocalDate.now().minusDays(10))
                .creationDate(LocalDateTime.now()).build());

        healthRecordRepository.saveAndFlush(HealthRecord.builder()
                .animal(rex).recordedBy(ulysses).category(HealthEventCategory.CONSULTA)
                .eventType("Consulta").eventDate(LocalDate.now().minusDays(5))
                .creationDate(LocalDateTime.now()).build());

        animalWeightHistoryRepository.saveAndFlush(AnimalWeightHistory.builder()
                .animal(rex).recordedBy(ulysses).weight(12.5)
                .measuredAt(LocalDate.now().minusDays(2))
                .creationDate(LocalDateTime.now()).build());

        animalHealthConditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(rex).recordedBy(ulysses).kind(AnimalHealthConditionKind.ALERGIA)
                .description("Anestesico local").since(LocalDate.now().minusDays(20))
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .extracting(TimelineEntry::getEventType)
                .containsExactly(TimelineEventType.PESAGEM, TimelineEventType.ATENDIMENTO,
                        TimelineEventType.VACINA, TimelineEventType.CONDICAO);
    }

    /**
     * A linha do tempo e do animal, e nao da conta: e por isso que ela nao recomeca na
     * transferencia. A view filtra por animal e nao sabe o que e custodia - o adotante
     * recebe a vida inteira porque nao ha nada no caminho que a corte.
     */
    @Test
    @DisplayName("nao vaza evento de outro animal")
    void naoVazaEventoDeOutroAnimal() {
        Animal nina = animalRepository.saveAndFlush(Animal.builder()
                .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(nina).recordedBy(ulysses).vaccineName("V10 da Nina")
                .applicationDate(LocalDate.now().minusDays(1))
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20))).isEmpty();
        assertThat(timelineRepository.findDoAnimal(nina.getAnimalId(), PageRequest.of(0, 20)))
                .hasSize(1);
    }

    /**
     * Vacina planejada do protocolo de filhote nao aconteceu: ela tem proxima dose e
     * nenhuma aplicacao. Deixa-la entrar poria no passado do animal um fato que ainda
     * nao existe, e a linha do tempo passaria a afirmar que ele foi vacinado.
     */
    @Test
    @DisplayName("vacina planejada, sem aplicacao, nao entra na linha do tempo")
    void vacinaPlanejadaNaoEntra() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).vaccineName("V10 planejada")
                .applicationDate(null)
                .nextDoseDate(LocalDate.now().plusDays(21))
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20))).isEmpty();
    }

    /**
     * Autoria e o que o P4 existe para trazer. Nulo continua sendo resposta valida - os
     * eventos anteriores a V27 nao tem autor, e a migration nao inventou um.
     */
    @Test
    @DisplayName("carrega quem registrou, e aceita nulo em quem nao tem")
    void carregaQuemRegistrou() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("Com autor")
                .applicationDate(LocalDate.now().minusDays(1))
                .creationDate(LocalDateTime.now()).build());

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).vaccineName("Sem autor")
                .applicationDate(LocalDate.now().minusDays(2))
                .creationDate(LocalDateTime.now()).build());

        // o nome vem resolvido pela propria view desde a V29, e isso trava o fim de um
        // N+1: o servico fazia personRepository.findById() por entrada, entao uma pagina
        // de vinte eventos custava vinte consultas - na leitura que mais cresce aqui
        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .extracting(TimelineEntry::getSummary, TimelineEntry::getRecordedByPersonId,
                        TimelineEntry::getRecordedByName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Com autor", ulysses.getPersonId(), "Ulysses"),
                        org.assertj.core.groups.Tuple.tuple("Sem autor", null, null));
    }

    /**
     * A credencial que a secao 5.2 do DESIGN cobra, com o estado dela.
     *
     * <b>O estado e o ponto, e nao o numero.</b> CRMV apenas informado tem de aparecer
     * <i>como informado</i>: o produto nao pode dar selo de verificado que nao conferiu
     * (5.10). Sem o status, a tela ou omite a credencial ou mente sobre ela.
     */
    @Test
    @DisplayName("carrega a credencial de quem registrou, com o estado dela")
    void carregaACredencialComEstado() {
        professionalCredentialRepository.saveAndFlush(ProfessionalCredential.builder()
                .person(ulysses).council("CRMV").uf("SP").number("12345")
                .status(CredentialStatus.INFORMADO)
                .creationDate(LocalDateTime.now()).build());

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("Aplicada por quem tem CRMV")
                .applicationDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .singleElement()
                .satisfies(entrada -> {
                    assertThat(entrada.getCredentialLabel()).isEqualTo("CRMV-SP 12345");
                    assertThat(entrada.getCredentialStatus()).isEqualTo(CredentialStatus.INFORMADO);
                });
    }

    /**
     * A credencial suspensa perde para a que autoriza ato clinico.
     *
     * <b>E deterministico de proposito.</b> A tabela admite mais de uma credencial por
     * pessoa e a tela mostra uma linha; sem o {@code ORDER BY} da view a escolha seria a
     * que o Postgres devolvesse primeiro, e a mesma tela mudaria de credencial entre duas
     * leituras. Isto so falha contra banco de verdade.
     */
    @Test
    @DisplayName("com duas credenciais deve escolher a que nao esta suspensa")
    void escolheACredencialQueAutoriza() {
        professionalCredentialRepository.saveAndFlush(ProfessionalCredential.builder()
                .person(ulysses).council("CRMV").uf("RJ").number("99999")
                .status(CredentialStatus.SUSPENSO)
                .creationDate(LocalDateTime.now()).build());

        professionalCredentialRepository.saveAndFlush(ProfessionalCredential.builder()
                .person(ulysses).council("CRMV").uf("SP").number("11111")
                .status(CredentialStatus.INFORMADO)
                .creationDate(LocalDateTime.now().minusYears(5))
                .build());

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("V10")
                .applicationDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .singleElement()
                .satisfies(entrada -> assertThat(entrada.getCredentialLabel())
                        .as("a suspensa e mais recente, e ainda assim perde: quem autoriza ato clinico vem primeiro")
                        .isEqualTo("CRMV-SP 11111"));
    }

    /**
     * A contagem de correcoes, para a tela marcar sucessao sem chamar a rota de correcoes
     * de cada evento so para descobrir que a maioria nao tem nenhuma.
     */
    @Test
    @DisplayName("conta as correcoes do evento, e zero em quem nao admite correcao")
    void contaAsCorrecoes() {
        var vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).recordedBy(ulysses).vaccineName("Corrigida")
                .applicationDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        vaccineCorrectionRepository.saveAndFlush(VaccineCorrection.builder()
                .vaccine(vacina).correctedBy(ulysses)
                .previousVaccineName("Nome errado")
                .correctedAt(LocalDateTime.now()).build());

        animalWeightHistoryRepository.saveAndFlush(AnimalWeightHistory.builder()
                .animal(rex).weight(12.5).measuredAt(LocalDate.now().minusDays(1))
                .creationDate(LocalDateTime.now()).build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .extracting(TimelineEntry::getEventType, TimelineEntry::getCorrectionCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TimelineEventType.VACINA, 1L),
                        org.assertj.core.groups.Tuple.tuple(TimelineEventType.PESAGEM, 0L));
    }

    /**
     * A observacao entra na linha do tempo, e entra pelo instante em que foi VISTA.
     *
     * <b>O caso e o da creche:</b> registra as 18h o que viu as 9h. Se a view usasse
     * {@code recorded_at}, o que a creche viu de manha apareceria depois do atendimento da
     * tarde - e a ordem dos fatos de saude e justamente o que permite perceber padrao
     * (3.9).
     */
    @Test
    @DisplayName("observacao entra na linha do tempo pelo instante em que foi vista")
    void observacaoEntraPeloInstanteVisto() {
        var asNove = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);

        observationRepository.saveAndFlush(Observation.builder()
                .animal(rex).recordedBy(ulysses)
                .description("Mancou da direita")
                .observedAt(asNove)
                // digitado nove horas depois, como acontece no fim do expediente
                .recordedAt(asNove.plusHours(9))
                .creationDate(asNove.plusHours(9))
                .build());

        assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                .singleElement()
                .satisfies(entrada -> {
                    assertThat(entrada.getEventType()).isEqualTo(TimelineEventType.OBSERVACAO);
                    assertThat(entrada.getSummary()).isEqualTo("Mancou da direita");
                    assertThat(entrada.getOccurredAt()).isEqualTo(asNove);
                    assertThat(entrada.getRecordedAt()).isEqualTo(asNove.plusHours(9));
                    assertThat(entrada.getRecordedByName()).isEqualTo("Ulysses");
                    assertThat(entrada.isHealthData())
                            .as("observacao e sempre dado de saude: o quase do 3.11 abriria "
                                + "classificacao por linha, e o lado seguro e o restritivo")
                            .isTrue();
                });
    }

    /**
     * <b>Observacao tem escopo proprio, e nao o do prontuario.</b>
     *
     * Quem mais escreve observacao e a creche. Se ela caisse em PRONTUARIO, dar a creche
     * acesso ao que ela mesma escreve entregaria junto todo atendimento clinico do animal
     * - o exemplo que a doc do GrantScope usa como o problema que o escopo veio resolver.
     */
    @Test
    @DisplayName("observacao exige o escopo de observacoes, e nao o de prontuario")
    void observacaoTemEscopoProprio() {
        assertThat(TimelineEventType.OBSERVACAO.escopoExigido())
                .isEqualTo(GrantScope.OBSERVACOES)
                .isNotEqualTo(GrantScope.PRONTUARIO);
    }

    /**
     * A view e somente leitura, e {@code @Immutable} e o que faz o Hibernate recusar
     * antes de o banco recusar. Sem isso, uma escrita acidental viraria erro de SQL em
     * producao em vez de erro de uso no desenvolvimento.
     */
    @Test
    @DisplayName("a linha do tempo nao aceita escrita")
    void naoAceitaEscrita() {
        assertThat(TimelineEntry.class.getAnnotation(org.hibernate.annotations.Immutable.class))
                .as("sem @Immutable o Hibernate tentaria gravar na view")
                .isNotNull();
    }

}
