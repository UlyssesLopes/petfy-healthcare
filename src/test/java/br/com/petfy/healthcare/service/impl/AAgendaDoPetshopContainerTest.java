package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentCloseRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentRequestDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ServiceAppointmentService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A agenda do petshop, contra Postgres real (Tela 18).
 *
 * <b>"Se o sistema fica bom com um tosador vendo quatro linhas, o escopo funciona. É o teste mais duro
 * da tela 09."</b> É literalmente o que esta classe verifica — e o que ela mostra é que **não há
 * nenhuma regra de petshop na guarda**: o que existe é um tutor que concedeu `CONDICOES` e
 * `OBSERVACOES` e não concedeu `PRONTUARIO`.
 *
 * <b>Container, e não unidade, por causa do índice e dos CHECKs.</b> O índice único de (petshop,
 * animal, horário) é o que recusa o duplo clique em "Agendar banho", e o CHECK dos carimbos é o que
 * impede um `AGENDADO` com hora de entrega — o estado em que a agenda de hoje e o histórico
 * discordariam sobre o mesmo banho.
 */
@SpringBootTest
@DisplayName("a agenda do petshop, contra Postgres real")
class AAgendaDoPetshopContainerTest extends PostgresContainerTest {

    @Autowired private ServiceAppointmentService appointmentService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private AnimalHealthConditionRepository conditionRepository;
    @Autowired private ObservationRepository observationRepository;

    private Person marcelo;
    private Person tiago;
    private Organization petshop;
    private Animal code;
    private Grant concessaoDoPetshop;

    @BeforeEach
    void montarOPetshop() {
        marcelo = pessoa("Marcelo Dias");
        tiago = pessoa("Tiago Moreira");

        petshop = organizationRepository.saveAndFlush(Organization.builder()
                .name("Banho do Tiago")
                .capabilities(Set.of(OrganizationCapability.REGISTRAR_OBSERVACAO))
                .creationDate(LocalDateTime.now())
                .build());

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(petshop).person(tiago)
                .role(MembershipRole.MONITOR)
                .joinedAt(LocalDateTime.now())
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(marcelo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(7))
                .build());

        /*
         * O ESCOPO MINIMO DO DESENHO: condicoes, observacoes e carteira. NADA de prontuario — e e
         * isso que faz o teste da tela 09 valer alguma coisa.
         */
        concessaoDoPetshop = grantRepository.saveAndFlush(Grant.builder()
                .animal(code)
                .granteeOrganization(petshop)
                .level(GrantLevel.EDITOR)
                .scopes(new LinkedHashSet<>(List.of(
                        GrantScope.CONDICOES, GrantScope.OBSERVACOES, GrantScope.CARTEIRA)))
                .grantedBy(marcelo)
                .grantedAt(LocalDateTime.now())
                .build());

        agirComo(tiago, petshop);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * <b>O caso que dá nome à tela.</b> "O que você precisa saber antes de encostar nele" — e nada
     * além disso. "É tudo o que Marcelo compartilhou, e é tudo o que o banho exige."
     */
    @Test
    @DisplayName("as linhas de seguranca vem do escopo, e nenhuma delas e prontuario")
    void asQuatroLinhas() {
        condicaoCronica("Displasia no quadril - nao erguer pelas patas traseiras");
        alergia("Alergia a proteina de frango");
        observacao("Medo de secador quente. No ultimo banho, tremeu.");

        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(9, 30)));

        assertThat(agendado.isInTheDark()).isFalse();

        assertThat(agendado.getSafetyNotes())
                .extracting(n -> n.getText())
                .contains("Displasia no quadril - nao erguer pelas patas traseiras",
                        "Alergia a proteina de frango",
                        "Medo de secador quente. No ultimo banho, tremeu.");

        // a condicao GRAVE e o losango vermelho do desenho
        assertThat(agendado.getSafetyNotes())
                .filteredOn(n -> n.getText().startsWith("Displasia"))
                .singleElement()
                .satisfies(n -> assertThat(n.getSeverity()).isEqualTo("MANEJO"));

        // e a carteira entra como uma linha so, com o estado dela
        assertThat(agendado.getSafetyNotes())
                .extracting(n -> n.getText())
                .contains("VACINACAO_EM_DIA");
    }

    /**
     * <b>"Acesso ao Petfy venceu em 31/07 — você está no escuro."</b>
     *
     * É o estado mais importante desta tela depois do normal, e o que a maioria dos produtos
     * esconderia: um cartão sem as quatro linhas parece um animal sem restrição nenhuma.
     */
    @Test
    @DisplayName("com a concessao vencida, a agenda diz que o petshop esta no escuro")
    void noEscuro() {
        condicaoCronica("Displasia no quadril");

        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(11, 0)));
        assertThat(agendado.isInTheDark()).isFalse();

        // o tutor revoga, ou o prazo passa
        concessaoDoPetshop.setRevokedAt(LocalDateTime.now());
        grantRepository.saveAndFlush(concessaoDoPetshop);

        assertThat(appointmentService.doDia(LocalDate.now()))
                .filteredOn(a -> a.getServiceAppointmentId()
                        .equals(agendado.getServiceAppointmentId()))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.isInTheDark()).isTrue();
                    assertThat(a.getSafetyNotes()).isEmpty();
                    // o compromisso continua na agenda: o animal vem as 11h de qualquer forma
                    assertThat(a.getAnimalName()).isEqualTo("Code");
                });
    }

    /**
     * Sem esta recusa, qualquer organização poria qualquer animal na própria agenda — e a agenda é
     * justamente onde aparece o que o tutor compartilhou.
     */
    @Test
    @DisplayName("nao se agenda um animal que o petshop nao alcanca")
    void animalQueNaoAlcanca() {
        Animal deOutro = animalRepository.saveAndFlush(Animal.builder()
                .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        var pedido = ServiceAppointmentRequestDTO.builder()
                .animalId(deOutro.getAnimalId())
                .scheduledAt(LocalTimeDoDia(8, 0))
                .service("Banho")
                .build();

        assertThatThrownBy(() -> appointmentService.agendar(pedido))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.ANIMAL_NOT_REACHED_BY_ORGANIZATION.getCode());
    }

    @Test
    @DisplayName("o mesmo animal no mesmo horario duas vezes e recusado")
    void oDuploClique() {
        appointmentService.agendar(paraAs(LocalTimeDoDia(9, 30)));

        assertThatThrownBy(() -> appointmentService.agendar(paraAs(LocalTimeDoDia(9, 30))))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.APPOINTMENT_ALREADY_SCHEDULED.getCode());
    }

    /**
     * A máquina de estados: agendar, marcar entrada, entregar. Pular um passo faria a agenda de hoje e
     * o histórico discordarem sobre o mesmo banho.
     */
    @Test
    @DisplayName("entregar um animal que nunca chegou e recusado")
    void naoEntregaSemEntrada() {
        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(14, 0)));

        assertThatThrownBy(() -> appointmentService.entregar(
                agendado.getServiceAppointmentId(), null))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.APPOINTMENT_WRONG_STATE.getCode());
    }

    /**
     * <b>"Entregar e avisar Marcelo."</b> O texto vira OBSERVAÇÃO — e nunca ato clínico. "Descreva o
     * que viu, não o que acha que é. Isso entra na linha do tempo como observação sua, e o veterinário
     * decide o resto."
     */
    @Test
    @DisplayName("o texto da entrega vira observacao na linha do tempo do animal")
    void aEntregaViraObservacao() {
        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(9, 30)));
        appointmentService.marcarEntrada(agendado.getServiceAppointmentId());

        var entregue = appointmentService.entregar(agendado.getServiceAppointmentId(),
                ServiceAppointmentCloseRequestDTO.builder()
                        .note("Vermelhidao na barriga, parecida com a do banho passado.")
                        .build());

        assertThat(entregue.getStatus()).isEqualTo(ServiceAppointmentStatus.CONCLUIDO);
        assertThat(entregue.getCompletedAt()).isNotNull();

        assertThat(observationRepository.findByAnimalAnimalIdOrderByObservedAtDesc(code.getAnimalId()))
                .extracting(o -> o.getDescription())
                .contains("Vermelhidao na barriga, parecida com a do banho passado.");
    }

    /**
     * <b>Um banho sem novidade não é fato de saúde.</b> Enchê-la de "deu banho" enterraria a
     * vermelhidão na barriga que alguém vir no banho seguinte.
     */
    @Test
    @DisplayName("entregar sem texto nao escreve nada na linha do tempo")
    void aEntregaSemTextoNaoRegistraNada() {
        long antes = observationRepository
                .findByAnimalAnimalIdOrderByObservedAtDesc(code.getAnimalId()).size();

        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(8, 0)));
        appointmentService.marcarEntrada(agendado.getServiceAppointmentId());
        appointmentService.entregar(agendado.getServiceAppointmentId(), null);

        assertThat(observationRepository
                .findByAnimalAnimalIdOrderByObservedAtDesc(code.getAnimalId()))
                .hasSize((int) antes);
    }

    /** A agenda do dia vem em ordem de horário — quem trabalha nela lê de cima para baixo. */
    @Test
    @DisplayName("a agenda do dia vem ordenada por horario")
    void aAgendaVemOrdenada() {
        appointmentService.agendar(paraAs(LocalTimeDoDia(14, 0)));
        appointmentService.agendar(paraAs(LocalTimeDoDia(8, 0)));
        appointmentService.agendar(paraAs(LocalTimeDoDia(11, 0)));

        assertThat(appointmentService.doDia(LocalDate.now()))
                .extracting(a -> a.getScheduledAt().getHour())
                .containsExactly(8, 11, 14);
    }

    /** O agendamento de outro petshop responde 404: um 403 confirmaria que ele existe. */
    @Test
    @DisplayName("o agendamento de outro petshop nao existe para mim")
    void oAgendamentoDeOutroPetshop() {
        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(9, 30)));

        Organization outro = organizationRepository.saveAndFlush(Organization.builder()
                .name("Outro Petshop")
                .capabilities(Set.of(OrganizationCapability.REGISTRAR_OBSERVACAO))
                .creationDate(LocalDateTime.now())
                .build());

        Person alguem = pessoa("Alguem Deloutro");
        membershipRepository.saveAndFlush(Membership.builder()
                .organization(outro).person(alguem)
                .role(MembershipRole.MONITOR)
                .joinedAt(LocalDateTime.now())
                .build());

        agirComo(alguem, outro);

        assertThatThrownBy(() -> appointmentService.marcarEntrada(agendado.getServiceAppointmentId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.APPOINTMENT_NOT_FOUND.getCode());
    }

    /** A ausência é informação, como a falta na creche. */
    @Test
    @DisplayName("o animal que nao veio fica registrado como falta")
    void oQueNaoVeio() {
        var agendado = appointmentService.agendar(paraAs(LocalTimeDoDia(11, 0)));

        assertThat(appointmentService.faltou(agendado.getServiceAppointmentId()).getStatus())
                .isEqualTo(ServiceAppointmentStatus.FALTOU);
    }

    private ServiceAppointmentRequestDTO paraAs(LocalDateTime quando) {
        return ServiceAppointmentRequestDTO.builder()
                .animalId(code.getAnimalId())
                .scheduledAt(quando)
                .service("Banho e tosa higienica")
                .build();
    }

    private LocalDateTime LocalTimeDoDia(int hora, int minuto) {
        return LocalDate.now().atTime(hora, minuto);
    }

    /** A cronica NAO TEM gravidade: o CHECK do schema so a admite em alergia, desde o P3. */
    private void condicaoCronica(String descricao) {
        conditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(code).recordedBy(marcelo)
                .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                .description(descricao)
                .since(LocalDate.now().minusYears(2))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void alergia(String descricao) {
        conditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(code).recordedBy(marcelo)
                .kind(AnimalHealthConditionKind.ALERGIA)
                .description(descricao)
                .severity(AnimalHealthConditionSeverity.MODERADA)
                .since(LocalDate.now().minusYears(2))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void observacao(String texto) {
        observationRepository.saveAndFlush(Observation.builder()
                .animal(code)
                .description(texto)
                .observedAt(LocalDateTime.now().minusDays(30))
                .recordedBy(marcelo)
                .recordedAt(LocalDateTime.now())
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

    private void agirComo(Person pessoa, Organization organizacao) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));

        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader(CurrentProfessionalProvider.HEADER_ORGANIZACAO,
                organizacao.getOrganizationId().toString());

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
    }
}
