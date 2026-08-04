package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

}
