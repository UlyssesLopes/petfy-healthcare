package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonNotification;
import br.com.petfy.healthcare.domain.repository.PersonNotificationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.PersonNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O aviso in-app, contra o banco de verdade.
 *
 * <b>O que so o banco prova:</b> que o aviso e de UMA pessoa. O mesmo fato — "Ana entrou no Code" —
 * vira uma linha para cada tutor, e lido e nao-lido e de cada leitor. Um teste com mock afirmaria
 * sobre a regra ja tendo assumido a separacao que a consulta precisa fazer.
 */
@SpringBootTest
@DisplayName("o aviso in-app, contra Postgres real")
class AvisoInAppContainerTest extends PostgresContainerTest {

    @Autowired private PersonNotificationService service;
    @Autowired private PersonNotificationRepository personNotificationRepository;
    @Autowired private PersonRepository personRepository;

    private Person eu;
    private Person outra;

    @BeforeEach
    void montar() {
        eu = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("aviso-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        outra = personRepository.saveAndFlush(Person.builder()
                .name("Ana Prado")
                .email("aviso-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        autenticar(eu);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Person person) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(person.getEmail(), "n/a", List.of()));
    }

    private PersonNotification avisoDe(Person dono, String assunto) {
        return personNotificationRepository.saveAndFlush(PersonNotification.builder()
                .person(dono)
                .subject(assunto)
                .body("o corpo inteiro do aviso")
                .event("tutor que entrou no animal")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("o feed traz so os meus, do mais novo para o mais velho")
    void feedESoMeu() {
        avisoDe(eu, "primeiro");
        avisoDe(eu, "segundo");
        avisoDe(outra, "o da Ana");

        var pagina = service.listMine(PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting("subject")
                .containsExactly("segundo", "primeiro");
    }

    @Test
    @DisplayName("a contagem de nao lidos e por pessoa")
    void contagemEPorPessoa() {
        avisoDe(eu, "um");
        avisoDe(eu, "dois");
        avisoDe(outra, "o da Ana");

        assertThat(service.countMineUnread()).isEqualTo(2);

        autenticar(outra);
        assertThat(service.countMineUnread()).isEqualTo(1);
    }

    /**
     * "Quando ela viu" e a PRIMEIRA vez que viu. Uma tela que marca ao rolar chamaria isto a cada
     * passagem, e reescrever empurraria a data para frente — apagando a informacao que o campo
     * existe para guardar.
     */
    @Test
    @DisplayName("ler duas vezes nao reescreve a data")
    void lerDuasVezesNaoReescreve() {
        var aviso = avisoDe(eu, "um");

        service.markAsRead(aviso.getPersonNotificationId());

        /*
         * A comparacao e sobre o que ESTA GRAVADO, e nao sobre o que o metodo devolveu: o Postgres
         * guarda microssegundos e o LocalDateTime tem nanos, entao a primeira resposta vem da
         * memoria com mais precisao do que o banco recebeu — e as duas nunca seriam iguais por um
         * motivo que nao tem nada a ver com a regra.
         */
        LocalDateTime gravadoNaPrimeira = personNotificationRepository
                .findById(aviso.getPersonNotificationId()).orElseThrow().getReadAt();

        service.markAsRead(aviso.getPersonNotificationId());

        LocalDateTime gravadoNaSegunda = personNotificationRepository
                .findById(aviso.getPersonNotificationId()).orElseThrow().getReadAt();

        assertThat(gravadoNaPrimeira).isNotNull();
        assertThat(gravadoNaSegunda).isEqualTo(gravadoNaPrimeira);
        assertThat(service.countMineUnread()).isZero();
    }

    @Test
    @DisplayName("marcar todos como lidos devolve quantos deixaram de estar por ler")
    void marcarTodos() {
        avisoDe(eu, "um");
        avisoDe(eu, "dois");
        var lido = avisoDe(eu, "tres");
        service.markAsRead(lido.getPersonNotificationId());
        avisoDe(outra, "o da Ana");

        assertThat(service.markAllAsRead()).isEqualTo(2);
        assertThat(service.countMineUnread()).isZero();

        // o da Ana continua por ler: marcar todos e "todos os MEUS"
        autenticar(outra);
        assertThat(service.countMineUnread()).isEqualTo(1);
    }

    /**
     * 404 e nao 403: confirmar que o aviso existe ja diria algo sobre a vida de outra pessoa.
     */
    @Test
    @DisplayName("o aviso de outra pessoa nao existe para mim")
    void avisoAlheioResponde404() {
        var daAna = avisoDe(outra, "o da Ana");

        assertThatThrownBy(() -> service.markAsRead(daAna.getPersonNotificationId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> {
                    var erro = (PetfyHealthcareException) e;
                    assertThat(erro.getCode()).isEqualTo(192);
                    assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        assertThat(personNotificationRepository.findById(daAna.getPersonNotificationId()))
                .get()
                .satisfies(a -> assertThat(a.getReadAt()).isNull());
    }
}
