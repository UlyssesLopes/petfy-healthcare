package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import br.com.petfy.healthcare.domain.entity.ConsentRecord;
import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.domain.entity.DueItemSilence;
import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.GroupApproval;
import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import br.com.petfy.healthcare.domain.entity.GroupApprovalStatus;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import br.com.petfy.healthcare.service.PersonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exclusao de conta contra Postgres de verdade.
 *
 * Existe porque este bug passou por teste unitario sem ser notado: com
 * repositorio mockado, apagar o person "funciona" - quem recusa e a chave
 * estrangeira, que so existe no banco. As tabelas de token de recuperacao e de
 * confirmacao apontam para persons, e como todo cadastro gera um token de
 * confirmacao, a partir da V12 nenhuma conta conseguia mais ser apagada.
 *
 * O H2 tambem nao serviria aqui: as consultas por UUID que sustentam a limpeza
 * voltam vazias nele - ver UuidQueriesContainerTest.
 */
@SpringBootTest
@DisplayName("exclusao de conta contra Postgres real")
class PersonDeletionContainerTest extends PostgresContainerTest {

    @Autowired private PersonRepository personRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;

    private Person person;

    @BeforeEach
    void setUp() {
        person = personRepository.save(Person.builder()
                .name("Ulysses")
                .email("exclusao-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private void comTokenDeConfirmacao() {
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .person(person)
                .tokenHash("hash-confirmacao-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void comTokenDeRecuperacao() {
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .person(person)
                .tokenHash("hash-recuperacao-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .creationDate(LocalDateTime.now())
                .build());
    }

    /**
     * O caso real: todo cadastro gera token de confirmacao, entao esta e a
     * situacao de qualquer conta que tenha passado pelo fluxo normal.
     */
    @Test
    @Transactional
    @DisplayName("deve apagar a conta que tem token de confirmacao de e-mail")
    void deveApagarContaComTokenDeConfirmacao() {
        comTokenDeConfirmacao();

        emailVerificationTokenRepository.deleteByPersonPersonId(person.getPersonId());
        personRepository.delete(person);
        personRepository.flush();

        assertThat(personRepository.findById(person.getPersonId())).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("deve apagar a conta que pediu recuperacao de senha")
    void deveApagarContaComTokenDeRecuperacao() {
        comTokenDeRecuperacao();

        passwordResetTokenRepository.deleteByPersonPersonId(person.getPersonId());
        personRepository.delete(person);
        personRepository.flush();

        assertThat(personRepository.findById(person.getPersonId())).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("deve apagar a conta que tem os dois tipos de token")
    void deveApagarContaComOsDoisTokens() {
        comTokenDeConfirmacao();
        comTokenDeRecuperacao();

        passwordResetTokenRepository.deleteByPersonPersonId(person.getPersonId());
        emailVerificationTokenRepository.deleteByPersonPersonId(person.getPersonId());
        personRepository.delete(person);
        personRepository.flush();

        assertThat(personRepository.findById(person.getPersonId())).isEmpty();
    }

    /**
     * A limpeza nao pode passar do dono da conta: apagar token de outro tutor
     * derrubaria a recuperacao de senha dele no meio do caminho.
     */
    @Test
    @Transactional
    @DisplayName("a limpeza deve atingir apenas os tokens do proprio dono")
    void limpezaDeveAtingirApenasOProprioDono() {
        comTokenDeConfirmacao();

        Person outro = personRepository.save(Person.builder()
                .name("Maria")
                .email("outra-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .person(outro)
                .tokenHash("hash-da-outra-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .creationDate(LocalDateTime.now())
                .build());

        emailVerificationTokenRepository.deleteByPersonPersonId(person.getPersonId());

        assertThat(emailVerificationTokenRepository.findByPersonPersonIdAndUsedAtIsNull(outro.getPersonId()))
                .hasSize(1);
    }

    /**
     * O ramo multi-tutor da exclusao, exercitando o servico de verdade.
     *
     * Os casos acima replicam as chamadas de repositorio; estes chamam
     * {@code deleteCurrentPerson()}, porque o que esta em jogo aqui e a <b>ordem</b>
     * em que ele emite os comandos. A V15 exige exatamente um HOLDER por animal via
     * indice unico parcial, e a heranca de titularidade promove o sucessor antes de
     * o vinculo de quem sai ter saido - dois HOLDER no mesmo animal. Se o Postgres
     * enxergar esse estado intermediario, a exclusao de conta quebra em producao, e
     * nenhum teste de mock veria isso.
     */
    @Nested
    @DisplayName("exclusao com animal compartilhado")
    class AnimalCompartilhado {

        @Autowired private PersonService personService;
        @Autowired private AnimalRepository animalRepository;
        @Autowired private CustodyRepository custodyRepository;
        @Autowired private GrantRepository grantRepository;
        @Autowired private GroupApprovalRepository groupApprovalRepository;
        @Autowired private OrganizationRepository organizationRepository;
        @Autowired private VaccineRepository vaccineRepository;
        @Autowired private AnimalWeightHistoryRepository animalWeightHistoryRepository;
        @Autowired private AntiparasiticRepository antiparasiticRepository;
        @Autowired private ConsentRecordRepository consentRecordRepository;
        @Autowired private DueItemSilenceRepository dueItemSilenceRepository;

        private Person maria;
        private Animal rex;

        @BeforeEach
        void compartilhaRexComMaria() {
            maria = personRepository.save(Person.builder()
                    .name("Maria")
                    .email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash")
                    .build());

            rex = animalRepository.save(Animal.builder().name("Rex").species(Species.CANINA)
                    .creationDate(LocalDateTime.now()).build());

            // person responde pelo animal; maria entrou depois por concessao. Depois do
            // P2b sao duas tabelas diferentes, e e essa diferenca que este teste passa a
            // exercitar: apagar a conta de quem responde tem de passar a custodia para
            // quem tem a concessao mais antiga, e nao apagar o animal.
            custodyRepository.saveAndFlush(Custody.builder()
                    .animal(rex).holderPerson(person).nature(CustodyNature.DEFINITIVA)
                    .startedAt(LocalDateTime.now().minusDays(10)).build());

            concessao(maria, LocalDateTime.now().minusDays(2));
        }

        private void concessao(Person para, LocalDateTime quando) {
            grantRepository.saveAndFlush(Grant.builder()
                    .animal(rex).granteePerson(para).level(GrantLevel.EDITOR)
                    .scopes(new java.util.LinkedHashSet<>(java.util.Set.of(GrantScope.CARTEIRA)))
                    .grantedAt(quando).build());
        }

        private void autenticar(Person como) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(como.getEmail(), "n/a", List.of()));
        }

        @AfterEach
        void limpar() {
            SecurityContextHolder.clearContext();
        }

        /**
         * <b>O CONTRATO MUDOU NA TELA 36, e este caso e o que ele substitui.</b>
         *
         * Ate aqui, o titular que fechava a conta via a titularidade passar sozinha para o co-tutor
         * mais antigo — em silencio, e sem que ninguem escolhesse. O produto (3.4) decidiu outra
         * coisa, e o desenho a escreve: <i>"o Code e o Bartolomeu precisam de alguem antes que voce
         * saia. Passe cada um para outra pessoa"</i>, e o botao so fica <i>"disponivel quando nenhum
         * animal estiver sob sua responsabilidade"</i>.
         *
         * Entregar a responsabilidade a quem nunca disse sim e o que deixou de acontecer.
         */
        @Test
        @DisplayName("nao encerra a conta enquanto o titular responde pelo animal")
        void naoEncerraEnquantoRespondePeloAnimal() {
            autenticar(person);

            assertThatThrownBy(() -> personService.deleteCurrentPerson())
                    .isInstanceOf(br.com.petfy.healthcare.exception.PetfyHealthcareException.class);

            assertThat(personRepository.findById(person.getPersonId())).isPresent();
            assertThat(animalRepository.findById(rex.getAnimalId())).isPresent();
        }

        /**
         * E dado o destino, a conta encerra e o animal fica de pe com quem o recebeu.
         *
         * <b>O caminho de saida existe e nao e novo</b>: transferir a titularidade, encerrar a linha
         * do tempo se o animal morreu, ou apagar o cadastro. O que deixou de existir e o desfecho que
         * ninguem escolheu.
         */
        @Test
        @DisplayName("dado destino ao animal, a conta encerra e ele sobrevive")
        void comDestinoDadoAContaEncerra() {
            passarRexPara(maria);

            autenticar(person);
            personService.deleteCurrentPerson();

            assertThat(personRepository.findById(person.getPersonId())).isEmpty();
            assertThat(animalRepository.findById(rex.getAnimalId())).isPresent();
            assertThat(custodyRepository.findEmCurso(rex.getAnimalId()))
                    .get()
                    .satisfies(nova -> assertThat(nova.getHolderPerson().getPersonId())
                            .isEqualTo(maria.getPersonId()));
        }

        /**
         * Passa a custodia do Rex adiante, como a transferencia de titularidade faz.
         *
         * A ordem importa e e a mesma de sempre: encerra a anterior, da flush, e so entao abre a nova
         * — o indice unico parcial admite no maximo UMA custodia em curso por animal.
         */
        private void passarRexPara(Person quemRecebe) {
            Custody atual = custodyRepository.findEmCurso(rex.getAnimalId()).orElseThrow();
            atual.setEndedAt(LocalDateTime.now());
            atual.setEndReason(CustodyEndReason.TRANSFERENCIA);
            custodyRepository.saveAndFlush(atual);

            Custody nova = custodyRepository.saveAndFlush(Custody.builder()
                    .animal(rex).holderPerson(quemRecebe).nature(CustodyNature.DEFINITIVA)
                    .startedAt(LocalDateTime.now()).build());

            atual.setSuccessor(nova);
            custodyRepository.saveAndFlush(atual);
        }

        /**
         * <b>O ANIMAL UNICO NAO MORRE MAIS COM A CONTA, e essa e a mudanca que mais importa.</b>
         *
         * Este caso afirmava o contrario ate a Tela 36: o animal sem outro tutor era apagado com a
         * carteira inteira — vacinas, peso, antiparasitario — porque a conta fechou. Sete anos de
         * registro de um animal VIVO destruidos por um desfecho que ninguem escolheu.
         *
         * O que sobreviveu do caso antigo e a razao pratica dele: peso e antiparasitario chegaram no
         * passo 9 e nao entraram na cascata, e apagar a conta de quem tinha registrado uma pesagem
         * respondia 500. A cascata continua sendo exercitada — so que depois de o animal ter recebido
         * um destino, que e quando ela pode rodar.
         */
        @Test
        @DisplayName("o animal unico impede o encerramento, e nao morre com a conta")
        void animalUnicoImpedeOEncerramento() {
            Animal nina = animalRepository.saveAndFlush(Animal.builder()
                    .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
            custodyRepository.saveAndFlush(Custody.builder()
                    .animal(nina).holderPerson(person).nature(CustodyNature.DEFINITIVA)
                    .startedAt(LocalDateTime.now()).build());

            vaccineRepository.saveAndFlush(Vaccine.builder()
                    .animal(nina).vaccineName("V10").applicationDate(LocalDate.now().minusMonths(2))
                    .creationDate(LocalDateTime.now()).build());
            animalWeightHistoryRepository.saveAndFlush(AnimalWeightHistory.builder()
                    .animal(nina).weight(8.0).measuredAt(LocalDate.now().minusMonths(1))
                    .creationDate(LocalDateTime.now()).build());
            antiparasiticRepository.saveAndFlush(Antiparasitic.builder()
                    .animal(nina).name("Vermifugo").kind(AntiparasiticKind.DEWORMER)
                    .applicationDate(LocalDate.now().minusMonths(1))
                    .creationDate(LocalDateTime.now()).updateDate(LocalDateTime.now()).build());

            autenticar(person);

            assertThatThrownBy(() -> personService.deleteCurrentPerson())
                    .isInstanceOf(br.com.petfy.healthcare.exception.PetfyHealthcareException.class);

            // a conta continua, e a carteira da Nina INTEIRA continua
            assertThat(personRepository.findById(person.getPersonId())).isPresent();
            assertThat(animalRepository.findById(nina.getAnimalId())).isPresent();
            assertThat(animalWeightHistoryRepository
                    .findByAnimalAnimalIdOrderByMeasuredAtDesc(nina.getAnimalId())).isNotEmpty();
            assertThat(antiparasiticRepository
                    .findByAnimalAnimalIdOrderByApplicationDateDesc(nina.getAnimalId())).isNotEmpty();
        }

        /**
         * O registro de consentimento aponta para persons, e o schema nao tem
         * ON DELETE CASCADE: sem sair antes, a FK segura o delete da conta - a mesma
         * familia de bug que travou a exclusao na V12 e na V15.
         *
         * E ele sai de fato: guardar prova de consentimento de quem pediu para ser
         * esquecido inverteria o proposito da prova.
         */
        @Test
        @DisplayName("conta com consentimento registrado e apagada, e o registro sai junto")
        void contaComConsentimentoEApagada() {
            // o Rex precisa de destino antes: desde a Tela 36 a conta so encerra quando ninguem
            // depende dela
            passarRexPara(maria);

            consentRecordRepository.saveAndFlush(ConsentRecord.builder()
                    .person(person)
                    .document(ConsentDocument.PRIVACY_POLICY)
                    .documentVersion("2026-08-05")
                    .acceptedAt(LocalDateTime.now())
                    .ipAddress("203.0.113.7")
                    .userAgent("Mozilla/5.0")
                    .build());

            autenticar(person);

            personService.deleteCurrentPerson();

            assertThat(personRepository.findById(person.getPersonId())).isEmpty();
            assertThat(consentRecordRepository
                    .findByPersonPersonIdOrderByAcceptedAtDesc(person.getPersonId())).isEmpty();
        }

        /**
         * <b>Nao existe guarda de schema para FK que aponta para {@code persons}.</b>
         *
         * Ela existe para {@code animals}, no {@code AnimalPurgerCoverageContainerTest}, que
         * le o {@code information_schema} e cobra tabela nova. Para pessoa a cobertura e
         * comportamental, e este teste e a linha que falta: sem apagar o silencio antes da
         * pessoa, {@code DELETE /persons/me} responderia 500 para qualquer um que tivesse
         * silenciado uma pendencia - e o silencio e a funcionalidade que existe justamente
         * para o produto nao ser desinstalado.
         */
        @Test
        @DisplayName("conta que silenciou pendencia e apagada, e o silencio sai junto")
        void contaComSilencioEApagada() {
            passarRexPara(maria);

            dueItemSilenceRepository.saveAndFlush(DueItemSilence.builder()
                    .person(person)
                    .kind(DueItemKind.DOSE_DE_VACINA)
                    .sourceId(UUID.randomUUID())
                    .silencedAt(LocalDateTime.now())
                    .build());

            autenticar(person);

            personService.deleteCurrentPerson();

            assertThat(personRepository.findById(person.getPersonId())).isEmpty();
            assertThat(dueItemSilenceRepository.findByPersonPersonId(person.getPersonId())).isEmpty();
        }

        /**
         * <b>O acordo de duas pessoas num animal que SOBREVIVE à exclusão.</b>
         *
         * O defeito estava aberto desde o bloco 6 e ficava escondido: o caminho comum apagava o animal
         * junto, e o {@code AnimalPurger} levava o pedido com ele. O caso que faltava é o do animal
         * que fica — e ele **passou a ser a regra** quando o encerramento passou a exigir destino para
         * cada animal, porque agora todo animal de quem sai sobrevive por definição.
         *
         * Sem o delete, o `DELETE /persons/me` responde 500 por
         * `fk_group_approvals_requested_by`. E não há guarda de schema para FK que aponta para
         * `persons` — só para `animals`.
         */
        @Test
        @DisplayName("o pedido de concordancia num animal que fica nao segura a exclusao")
        void contaComAcordoDeDuasPessoasEApagada() {
            Organization grupo = organizationRepository.saveAndFlush(Organization.builder()
                    .name("Grupo Gatos da Benedito " + UUID.randomUUID())
                    .creationDate(LocalDateTime.now())
                    .build());

            // person PEDIU e maria DECIDIU: duas das quatro colunas que apontam para persons
            groupApprovalRepository.saveAndFlush(GroupApproval.builder()
                    .organization(grupo)
                    .kind(GroupApprovalKind.ADOCAO)
                    .animal(rex)
                    .toPerson(maria)
                    .reason("Paula visita a praca ha meses.")
                    .status(GroupApprovalStatus.CONCORDADO)
                    .requestedBy(person)
                    .requestedAt(LocalDateTime.now().minusDays(2))
                    .decidedBy(maria)
                    .decidedAt(LocalDateTime.now().minusDays(1))
                    .build());

            // o Rex fica: passa para a maria, e e justamente por isso que ele sobrevive ao purge
            passarRexPara(maria);

            autenticar(person);

            personService.deleteCurrentPerson();

            assertThat(personRepository.findById(person.getPersonId())).isEmpty();
            // e o animal continua de pe, com quem o recebeu
            assertThat(animalRepository.findById(rex.getAnimalId())).isPresent();
        }

        /** Co-tutor sai: o animal nao muda de titular e continua de pe. */
        @Test
        @DisplayName("co-tutor sai e o titular segue titular")
        void coTutorSaiSemMexerNoTitular() {
            autenticar(maria);

            personService.deleteCurrentPerson();

            assertThat(animalRepository.findById(rex.getAnimalId())).isPresent();
            // quem sai tinha concessao, nao custodia: quem responde pelo animal nao e
            // tocado, e a concessao dela desaparece junto com a conta
            assertThat(custodyRepository.findEmCurso(rex.getAnimalId()))
                    .get()
                    .satisfies(atual -> assertThat(atual.getHolderPerson().getPersonId())
                            .isEqualTo(person.getPersonId()));
            assertThat(grantRepository.findVigentesDePessoasNoAnimal(rex.getAnimalId(), LocalDateTime.now()))
                    .isEmpty();
        }
    }

}
