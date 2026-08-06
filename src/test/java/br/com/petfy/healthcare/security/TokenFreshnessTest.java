package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenFreshnessTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private TokenFreshness tokenFreshness;

    private static final String EMAIL = "ulysses@petfy.com.br";

    private static final LocalDateTime TROCA = LocalDateTime.of(2026, 8, 4, 12, 0);

    private static Instant instante(LocalDateTime momento) {
        return momento.atZone(ZoneId.systemDefault()).toInstant();
    }

    private JwtPrincipal person(LocalDateTime emissao) {
        return new JwtPrincipal(EMAIL, instante(emissao));
    }

    @Test
    @DisplayName("token emitido antes da troca de senha deve ficar para tras")
    void tokenAnteriorDeveFicarParaTras() {
        when(personRepository.findPasswordChangedAtByEmail(EMAIL)).thenReturn(Optional.of(TROCA));

        assertThat(tokenFreshness.isStale(person(TROCA.minusMinutes(5)))).isTrue();
    }

    @Test
    @DisplayName("token emitido depois da troca de senha deve seguir valendo")
    void tokenPosteriorDeveSeguirValendo() {
        when(personRepository.findPasswordChangedAtByEmail(EMAIL)).thenReturn(Optional.of(TROCA));

        assertThat(tokenFreshness.isStale(person(TROCA.plusSeconds(1)))).isFalse();
    }

    /**
     * O iat do JWT tem precisao de segundo e a troca e gravada com fracao de
     * segundo, entao token do mesmo segundo da troca e ambiguo. Recusar custa um
     * login novo; aceitar deixaria viva a sessao que a troca deveria derrubar.
     */
    @Test
    @DisplayName("token do mesmo instante da troca deve ficar para tras, no lado seguro da duvida")
    void tokenDoMesmoInstanteDeveFicarParaTras() {
        when(personRepository.findPasswordChangedAtByEmail(EMAIL)).thenReturn(Optional.of(TROCA));

        assertThat(tokenFreshness.isStale(person(TROCA))).isTrue();
    }

    @Test
    @DisplayName("quem nunca trocou de senha nao tem token a invalidar")
    void quemNuncaTrocouNaoTemTokenAInvalidar() {
        when(personRepository.findPasswordChangedAtByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(tokenFreshness.isStale(person(TROCA.minusYears(1)))).isFalse();
    }

    /**
     * O caso que existia aqui - "token de vet deve ser checado contra a tabela de
     * vets, nao a de owners" - foi removido no P1b em vez de adaptado. Ele cobria
     * uma ramificacao por papel que deixou de existir: ha uma tabela so, e escolher
     * errado nao e mais um erro possivel. Reescreve-lo para passar seria manter um
     * teste que nao protege nada e sugere cobertura que nao existe.
     *
     * O que sobrou dele esta abaixo: qualquer pessoa, com ou sem credencial
     * profissional, passa pela mesma checagem.
     */
    @Test
    @DisplayName("a checagem nao depende de quem a pessoa e, e sim de quando o token saiu")
    void checagemNaoDependeDeQuemAPessoaE() {
        when(personRepository.findPasswordChangedAtByEmail(EMAIL)).thenReturn(Optional.of(TROCA));

        assertThat(tokenFreshness.isStale(person(TROCA.minusMinutes(5)))).isTrue();
        assertThat(tokenFreshness.isStale(person(TROCA.plusMinutes(5)))).isFalse();
    }

}
