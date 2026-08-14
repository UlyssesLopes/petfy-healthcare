package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.notification.NovoAparelhoNotifier;
import br.com.petfy.healthcare.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * "Entrada em um aparelho novo" (Tela 28), contra Postgres real.
 *
 * <b>A consulta e o motivo deste teste existir.</b> Ela compara o User-Agent, e comparar com nulo
 * em SQL nao da falso — da desconhecido. Um `user_agent = null` escrito ingenuamente faria todo
 * cliente sem cabecalho parecer um aparelho novo a cada login, e o alerta de seguranca viraria
 * ruido diario. Isso so aparece contra banco de verdade.
 */
@SpringBootTest
@DisplayName("o aviso de aparelho novo, contra Postgres real")
class AparelhoNovoContainerTest extends PostgresContainerTest {

    @Autowired private AuthService authService;
    @Autowired private PersonRepository personRepository;
    @Autowired private PersonSessionRepository personSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /* O canal e efeito colateral: aqui se afirma QUANDO ele e chamado, e nao o que ele escreve. */
    @MockBean private NovoAparelhoNotifier novoAparelhoNotifier;

    private static final String SENHA = "Petfy!2026";

    private String meuEmail;

    @BeforeEach
    void montar() {
        meuEmail = "aparelho-" + UUID.randomUUID() + "@petfy.com.br";

        personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email(meuEmail)
                .password(passwordEncoder.encode(SENHA))
                .build());
    }

    private void entrarCom(String userAgent) {
        authService.login(LoginRequestDTO.builder().email(meuEmail).password(SENHA).build(),
                userAgent);
    }

    @Test
    @DisplayName("a primeira entrada nao avisa; a de outro aparelho avisa")
    void avisaSoQuandoOAparelhoMuda() {
        entrarCom("Mozilla/5.0 (Windows NT 10.0) Chrome/141");
        verify(novoAparelhoNotifier, never()).entrou(any(), any());

        entrarCom("Mozilla/5.0 (Linux; Android 14) Chrome/141 Mobile");
        verify(novoAparelhoNotifier).entrou(any(), any());
    }

    @Test
    @DisplayName("voltar ao aparelho de sempre nao avisa de novo")
    void oMesmoAparelhoNaoAvisaDuasVezes() {
        String meuNotebook = "Mozilla/5.0 (Windows NT 10.0) Chrome/141";

        entrarCom(meuNotebook);
        entrarCom(meuNotebook);
        entrarCom(meuNotebook);

        verify(novoAparelhoNotifier, never()).entrou(any(), any());
    }

    /**
     * O caso que a comparacao ingenua com nulo quebraria.
     *
     * Sem User-Agent — script, `curl`, cliente antigo — a coluna fica nula, e `user_agent = null`
     * nunca e verdade em SQL. Cada entrada pareceria um aparelho novo.
     */
    @Test
    @DisplayName("cliente sem User-Agent nao vira aparelho novo a cada entrada")
    void semUserAgentNaoAvisaSempre() {
        entrarCom(null);
        entrarCom(null);
        entrarCom(null);

        verify(novoAparelhoNotifier, never()).entrou(any(), any());
    }

    /**
     * <b>Encerrar o proprio aparelho e entrar de novo dele nao e aparelho novo.</b>
     *
     * A entrada encerrada continua na tabela — ela e o registro de que aconteceu, e a Tela 36 a
     * mostra. Ignora-la aqui faria quem encerra sessoes por higiene receber um alerta toda vez.
     */
    @Test
    @DisplayName("entrada encerrada continua contando como aparelho conhecido")
    void encerradaContaComoConhecida() {
        String meuNotebook = "Mozilla/5.0 (Windows NT 10.0) Chrome/141";

        entrarCom(meuNotebook);
        personSessionRepository.findAll().forEach(sessao -> {
            sessao.setRevokedAt(java.time.LocalDateTime.now());
            personSessionRepository.saveAndFlush(sessao);
        });

        entrarCom(meuNotebook);

        verify(novoAparelhoNotifier, never()).entrou(any(), any());
    }

    /** O aviso aponta para uma entrada que existe: e ela que o e-mail manda encerrar. */
    @Test
    @DisplayName("o aviso carrega a entrada que acabou de nascer")
    void oAvisoCarregaAEntrada() {
        entrarCom("Mozilla/5.0 (Windows NT 10.0) Chrome/141");
        entrarCom("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Safari/604");

        var avisada = org.mockito.ArgumentCaptor
                .forClass(br.com.petfy.healthcare.domain.entity.PersonSession.class);
        verify(novoAparelhoNotifier).entrou(any(), avisada.capture());

        assertThat(avisada.getValue().getPersonSessionId()).isNotNull();
        assertThat(personSessionRepository.findById(avisada.getValue().getPersonSessionId()))
                .isPresent();
    }

}
