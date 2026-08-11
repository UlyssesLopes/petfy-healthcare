package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AnimalWeightHistoryRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O motor da uniao contra Postgres real (Tela 32).
 *
 * <b>Contra o banco de verdade e nao contra mock, e por uma razao especifica:</b> o que este
 * codigo faz e um {@code update} em massa em oito tabelas, e mock nao tem chave estrangeira, nao
 * tem contexto de persistencia e nao reclama quando uma linha fica para tras. O defeito que
 * importa aqui — evento que nao move e ninguem percebe — e invisivel em memoria.
 */
@SpringBootTest
@DisplayName("a uniao de cadastros: evento move, vinculo nao")
class AnimalMergerContainerTest extends PostgresContainerTest {

    @Autowired private AnimalMerger animalMerger;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private AnimalWeightHistoryRepository weightRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private GrantRepository grantRepository;

    private Person marcelo;
    private Person clinica;
    private Animal code;
    private Animal codinho;

    @BeforeEach
    void setUp() {
        marcelo = pessoa("Marcelo Dias");
        clinica = pessoa("Ana Ferreira");

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).microchipNumber("981020003451271")
                .creationDate(LocalDateTime.now().minusYears(7)).build());

        codinho = animalRepository.saveAndFlush(Animal.builder()
                .name("Codinho").species(Species.CANINA).microchipNumber("981020003451271")
                .creationDate(LocalDateTime.now()).build());
    }

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome).email("merge-" + UUID.randomUUID() + "@petfy.com.br").password("hash")
                .build());
    }

    /**
     * "As duas linhas do tempo viram uma so. NENHUM EVENTO E DESCARTADO."
     *
     * E a promessa que a tela faz a quem esta prestes a aceitar algo irreversivel.
     */
    @Test
    @DisplayName("os eventos do absorvido devem passar todos para o sobrevivente")
    void eventosMovem() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(codinho).recordedBy(clinica).vaccineName("Antirrabica")
                .applicationDate(LocalDate.now().minusDays(1))
                .creationDate(LocalDateTime.now()).build());

        healthRecordRepository.saveAndFlush(HealthRecord.builder()
                .animal(codinho).recordedBy(clinica).category(HealthEventCategory.CONSULTA)
                .eventType("Consulta").eventDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        weightRepository.saveAndFlush(AnimalWeightHistory.builder()
                .animal(codinho).recordedBy(clinica).weight(8.6)
                .measuredAt(LocalDate.now()).creationDate(LocalDateTime.now()).build());

        int movidos = animalMerger.mover(codinho.getAnimalId(), code.getAnimalId());

        assertThat(movidos).isEqualTo(3);
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(code.getAnimalId())).hasSize(1);
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(codinho.getAnimalId())).isEmpty();
        assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(code.getAnimalId())).hasSize(1);
        assertThat(weightRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(code.getAnimalId()))
                .hasSize(1);
    }

    /**
     * <b>"Cada evento continua assinado por quem o registrou, com a data de lancamento original."</b>
     *
     * Reatribuir autoria transformaria uma uniao de cadastros numa falsificacao de prontuario — e
     * e o tipo de coisa que passaria despercebida, porque o evento continuaria la.
     */
    @Test
    @DisplayName("mover nao pode reatribuir autoria nem reescrever a data de lancamento")
    void autoriaEDataSobrevivem() {
        LocalDateTime lancadoEm = LocalDateTime.now().minusDays(30).withNano(0);

        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(codinho).recordedBy(clinica).vaccineName("V10")
                .applicationDate(LocalDate.now().minusDays(40))
                .creationDate(lancadoEm).build());

        animalMerger.mover(codinho.getAnimalId(), code.getAnimalId());

        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(code.getAnimalId()))
                .singleElement()
                .satisfies(vacina -> {
                    assertThat(vacina.getRecordedBy().getPersonId()).isEqualTo(clinica.getPersonId());
                    assertThat(vacina.getCreationDate()).isEqualTo(lancadoEm);
                });
    }

    /**
     * <b>"Unir nao transfere custodia."</b>
     *
     * Se a custodia movesse, o animal ficaria com dois responsaveis — e "quem responde por ele"
     * nao admite dois. Pior: quem cadastrou o duplicado passaria a responder por um animal que
     * nunca foi dele.
     */
    @Test
    @DisplayName("a custodia do absorvido nao pode ir junto")
    void custodiaNaoMove() {
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(codinho).holderPerson(clinica).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(marcelo).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(7)).build());

        animalMerger.mover(codinho.getAnimalId(), code.getAnimalId());

        assertThat(custodyRepository.findEmCurso(code.getAnimalId()))
                .as("quem responde pelo Code continua sendo o Marcelo")
                .get()
                .satisfies(c -> assertThat(c.getHolderPerson().getPersonId())
                        .isEqualTo(marcelo.getPersonId()));

        assertThat(custodyRepository.findByAnimalAnimalIdIn(java.util.List.of(codinho.getAnimalId())))
                .as("a custodia do absorvido fica onde estava, contando quem cadastrou aquele registro")
                .hasSize(1);
    }

    /**
     * <b>"O acesso da sua clinica continua sendo o que ele concedeu."</b>
     *
     * Mover a concessao daria a quem PEDIU a uniao acesso ao animal inteiro — 147 eventos de sete
     * anos que ninguem lhe concedeu. E o defeito mais grave que esta operacao poderia ter, e o
     * mais silencioso: ninguem recebe aviso quando ganha acesso a mais do que pediu.
     */
    @Test
    @DisplayName("a concessao do absorvido nao pode virar acesso ao animal inteiro")
    void concessaoNaoMove() {
        grantRepository.saveAndFlush(Grant.builder()
                .animal(codinho).granteePerson(clinica).level(GrantLevel.VIEWER)
                .grantedBy(clinica).grantedAt(LocalDateTime.now()).build());

        animalMerger.mover(codinho.getAnimalId(), code.getAnimalId());

        assertThat(grantRepository.findByAnimalAnimalIdIn(java.util.List.of(code.getAnimalId())))
                .as("a clinica que pediu a uniao nao ganha acesso ao cadastro que sobreviveu")
                .isEmpty();

        assertThat(grantRepository.findByAnimalAnimalIdIn(java.util.List.of(codinho.getAnimalId())))
                .hasSize(1);
    }

    /**
     * O contexto de persistencia guarda entidades ja carregadas com o animal ANTIGO, e o
     * {@code update} em JPQL vai direto ao banco sem atualiza-las. Sem o {@code clear}, quem ler o
     * animal logo depois nesta mesma transacao veria o estado de antes — e gravaria em cima.
     */
    @Test
    @DisplayName("depois de mover, uma releitura deve enxergar o estado novo e nao o de antes")
    void contextoNaoGuardaOEstadoAntigo() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(codinho).recordedBy(clinica).vaccineName("Giardia")
                .applicationDate(LocalDate.now()).creationDate(LocalDateTime.now()).build());

        // carrega antes, para o contexto guardar a versao com o animal antigo
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(codinho.getAnimalId())).hasSize(1);

        animalMerger.mover(codinho.getAnimalId(), code.getAnimalId());

        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(codinho.getAnimalId())).isEmpty();
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(code.getAnimalId())).hasSize(1);
    }

    /** Mover um cadastro sem nada nao e erro: e o caso do duplicado recem-criado e vazio. */
    @Test
    @DisplayName("mover um cadastro sem eventos nao deve falhar")
    void semEventosNaoFalha() {
        assertThat(animalMerger.mover(codinho.getAnimalId(), code.getAnimalId())).isZero();
    }

}
