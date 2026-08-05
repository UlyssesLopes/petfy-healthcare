package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.PetWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import br.com.petfy.healthcare.service.OwnerService;
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

/**
 * Exclusao de conta contra Postgres de verdade.
 *
 * Existe porque este bug passou por teste unitario sem ser notado: com
 * repositorio mockado, apagar o owner "funciona" - quem recusa e a chave
 * estrangeira, que so existe no banco. As tabelas de token de recuperacao e de
 * confirmacao apontam para owners, e como todo cadastro gera um token de
 * confirmacao, a partir da V12 nenhuma conta conseguia mais ser apagada.
 *
 * O H2 tambem nao serviria aqui: as consultas por UUID que sustentam a limpeza
 * voltam vazias nele - ver UuidQueriesContainerTest.
 */
@SpringBootTest
@DisplayName("exclusao de conta contra Postgres real")
class OwnerDeletionContainerTest extends PostgresContainerTest {

    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;

    private Owner owner;

    @BeforeEach
    void setUp() {
        owner = ownerRepository.save(Owner.builder()
                .name("Ulysses")
                .email("exclusao-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private void comTokenDeConfirmacao() {
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .owner(owner)
                .tokenHash("hash-confirmacao-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void comTokenDeRecuperacao() {
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .owner(owner)
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

        emailVerificationTokenRepository.deleteByOwnerOwnerId(owner.getOwnerId());
        ownerRepository.delete(owner);
        ownerRepository.flush();

        assertThat(ownerRepository.findById(owner.getOwnerId())).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("deve apagar a conta que pediu recuperacao de senha")
    void deveApagarContaComTokenDeRecuperacao() {
        comTokenDeRecuperacao();

        passwordResetTokenRepository.deleteByOwnerOwnerId(owner.getOwnerId());
        ownerRepository.delete(owner);
        ownerRepository.flush();

        assertThat(ownerRepository.findById(owner.getOwnerId())).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("deve apagar a conta que tem os dois tipos de token")
    void deveApagarContaComOsDoisTokens() {
        comTokenDeConfirmacao();
        comTokenDeRecuperacao();

        passwordResetTokenRepository.deleteByOwnerOwnerId(owner.getOwnerId());
        emailVerificationTokenRepository.deleteByOwnerOwnerId(owner.getOwnerId());
        ownerRepository.delete(owner);
        ownerRepository.flush();

        assertThat(ownerRepository.findById(owner.getOwnerId())).isEmpty();
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

        Owner outro = ownerRepository.save(Owner.builder()
                .name("Maria")
                .email("outra-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .owner(outro)
                .tokenHash("hash-da-outra-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .creationDate(LocalDateTime.now())
                .build());

        emailVerificationTokenRepository.deleteByOwnerOwnerId(owner.getOwnerId());

        assertThat(emailVerificationTokenRepository.findByOwnerOwnerIdAndUsedAtIsNull(outro.getOwnerId()))
                .hasSize(1);
    }

    /**
     * O ramo multi-tutor da exclusao, exercitando o servico de verdade.
     *
     * Os casos acima replicam as chamadas de repositorio; estes chamam
     * {@code deleteCurrentOwner()}, porque o que esta em jogo aqui e a <b>ordem</b>
     * em que ele emite os comandos. A V15 exige exatamente um HOLDER por pet via
     * indice unico parcial, e a heranca de titularidade promove o sucessor antes de
     * o vinculo de quem sai ter saido - dois HOLDER no mesmo pet. Se o Postgres
     * enxergar esse estado intermediario, a exclusao de conta quebra em producao, e
     * nenhum teste de mock veria isso.
     */
    @Nested
    @DisplayName("exclusao com pet compartilhado")
    class PetCompartilhado {

        @Autowired private OwnerService ownerService;
        @Autowired private PetRepository petRepository;
        @Autowired private PetTutorRepository petTutorRepository;
        @Autowired private VaccineRepository vaccineRepository;
        @Autowired private PetWeightHistoryRepository petWeightHistoryRepository;
        @Autowired private AntiparasiticRepository antiparasiticRepository;

        private Owner maria;
        private Pet rex;

        @BeforeEach
        void compartilhaRexComMaria() {
            maria = ownerRepository.save(Owner.builder()
                    .name("Maria")
                    .email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash")
                    .build());

            rex = petRepository.save(Pet.builder().name("Rex").species(Species.CANINA)
                    .creationDate(LocalDateTime.now()).build());

            // owner e o titular; maria entrou depois como co-tutora
            vinculo(owner, PetTutorRole.HOLDER, LocalDateTime.now().minusDays(10));
            vinculo(maria, PetTutorRole.EDITOR, LocalDateTime.now().minusDays(2));
        }

        private void vinculo(Owner de, PetTutorRole papel, LocalDateTime quando) {
            petTutorRepository.saveAndFlush(PetTutor.builder()
                    .pet(rex).owner(de).role(papel).creationDate(quando).build());
        }

        private void autenticar(Owner como) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(como.getEmail(), "n/a", List.of()));
        }

        @AfterEach
        void limpar() {
            SecurityContextHolder.clearContext();
        }

        /**
         * O titular fecha a conta e o pet tem outro tutor: o pet sobrevive e a
         * titularidade passa para quem fica. E aqui que os dois HOLDER coexistem.
         */
        @Test
        @DisplayName("titular sai, pet sobrevive e a titularidade passa ao co-tutor")
        void titularSaiEPetSobrevive() {
            autenticar(owner);

            ownerService.deleteCurrentOwner();

            assertThat(petRepository.findById(rex.getPetId())).isPresent();
            assertThat(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(rex.getPetId()))
                    .singleElement()
                    .satisfies(restante -> {
                        assertThat(restante.getOwner().getOwnerId()).isEqualTo(maria.getOwnerId());
                        assertThat(restante.getRole()).isEqualTo(PetTutorRole.HOLDER);
                    });
        }

        /**
         * O pet unico da conta morre com ela, e leva a carteira inteira.
         *
         * Peso e antiparasitario chegaram no passo 9 e nao entraram na cascata da
         * exclusao: a partir dali, apagar a conta de quem tinha registrado uma
         * pesagem respondia 500 e o pedido de exclusao ficava sem atendimento. Nao
         * aparecia em mock nem nos casos acima, que criavam pet sem historico.
         */
        @Test
        @DisplayName("pet unico da conta morre com a carteira inteira, peso e antiparasitario incluidos")
        void petUnicoMorreComACarteiraInteira() {
            Pet nina = petRepository.saveAndFlush(Pet.builder()
                    .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
            petTutorRepository.saveAndFlush(PetTutor.builder()
                    .pet(nina).owner(owner).role(PetTutorRole.HOLDER)
                    .creationDate(LocalDateTime.now()).build());

            vaccineRepository.saveAndFlush(Vaccine.builder()
                    .pet(nina).vaccineName("V10").applicationDate(LocalDate.now().minusMonths(2))
                    .creationDate(LocalDateTime.now()).build());
            petWeightHistoryRepository.saveAndFlush(PetWeightHistory.builder()
                    .pet(nina).weight(8.0).measuredAt(LocalDate.now().minusMonths(1))
                    .creationDate(LocalDateTime.now()).build());
            antiparasiticRepository.saveAndFlush(Antiparasitic.builder()
                    .pet(nina).name("Vermifugo").kind(AntiparasiticKind.DEWORMER)
                    .applicationDate(LocalDate.now().minusMonths(1))
                    .creationDate(LocalDateTime.now()).updateDate(LocalDateTime.now()).build());

            autenticar(owner);

            ownerService.deleteCurrentOwner();

            assertThat(ownerRepository.findById(owner.getOwnerId())).isEmpty();
            assertThat(petRepository.findById(nina.getPetId())).isEmpty();
            assertThat(petWeightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(nina.getPetId())).isEmpty();
            assertThat(antiparasiticRepository.findByPetPetIdOrderByApplicationDateDesc(nina.getPetId())).isEmpty();

            // rex tem outro tutor e sobrevive, com a titularidade passada a maria
            assertThat(petRepository.findById(rex.getPetId())).isPresent();
        }

        /** Co-tutor sai: o pet nao muda de titular e continua de pe. */
        @Test
        @DisplayName("co-tutor sai e o titular segue titular")
        void coTutorSaiSemMexerNoTitular() {
            autenticar(maria);

            ownerService.deleteCurrentOwner();

            assertThat(petRepository.findById(rex.getPetId())).isPresent();
            assertThat(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(rex.getPetId()))
                    .singleElement()
                    .satisfies(restante -> {
                        assertThat(restante.getOwner().getOwnerId()).isEqualTo(owner.getOwnerId());
                        assertThat(restante.getRole()).isEqualTo(PetTutorRole.HOLDER);
                    });
        }
    }

}
