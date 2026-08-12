package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.BoardingRequestDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.BoardingService;
import br.com.petfy.healthcare.service.TimelineService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uma semana de hospedagem, contra Postgres real (Tela 47).
 *
 * <b>"Hospedagem é custódia temporária."</b> Não há modelo novo: uma estadia é uma `Custody` com
 * `nature = TRANSITORIA`, `holderOrganization` e `expectedEndAt` — três campos que existem desde o P2
 * e nunca tiveram caminho HTTP.
 *
 * <b>Container, e não unidade, por causa de UM índice.</b> O índice único parcial admite no máximo
 * uma custódia em curso por animal, e é ele que torna a entrega uma sequência com ordem obrigatória —
 * encerrar, dar flush, e só então abrir. Um teste de mock passaria com as duas abertas ao mesmo tempo
 * e o Postgres derrubaria a operação inteira em produção. É a mesma armadilha que o 8b encontrou na
 * troca de titularidade.
 */
@SpringBootTest
@DisplayName("a hospedagem, contra Postgres real")
class UmaSemanaDeHospedagemContainerTest extends PostgresContainerTest {

    @Autowired private BoardingService boardingService;
    @Autowired private TimelineService timelineService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ObservationRepository observationRepository;

    private Person marcelo;
    private Person rafaela;
    private Organization creche;
    private Organization clinicaSemCustodia;
    private Animal code;

    @BeforeEach
    void montarAViagem() {
        marcelo = pessoa("Marcelo Dias");
        rafaela = pessoa("Rafaela Lopes");

        creche = organizacao("Creche Quintal", OrganizationCapability.DETER_CUSTODIA);
        clinicaSemCustodia = organizacao("Clinica Vet Norte",
                OrganizationCapability.REGISTRAR_ATO_CLINICO);

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(creche).person(rafaela)
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

        agirComo(marcelo, null);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * <b>O caso que define a tela.</b> A custódia passa para a creche, e o Marcelo — que deixou de
     * responder pelo animal — continua enxergando a vida dele.
     *
     * Sem a concessão criada na entrega, o `AnimalAccessGuard` responderia 404 ao tutor em viagem, no
     * único momento em que esta tela existe para ele olhar.
     */
    @Test
    @DisplayName("a custodia passa para a creche, e quem entregou continua lendo")
    void aCustodiaPassaEQuemEntregouContinuaLendo() {
        var estadia = boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        assertThat(estadia.getOrganizationName()).isEqualTo("Creche Quintal");
        assertThat(estadia.getDayOfStay()).isEqualTo(1);
        assertThat(estadia.getTotalDays()).isEqualTo(7);
        assertThat(estadia.getReturnsToName()).isEqualTo("Marcelo Dias");

        // quem responde agora e a CRECHE
        assertThat(custodyRepository.findEmCurso(code.getAnimalId()))
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.getHolderOrganization().getOrganizationId())
                            .isEqualTo(creche.getOrganizationId());
                    assertThat(c.getNature()).isEqualTo(CustodyNature.TRANSITORIA);
                    assertThat(c.getExpectedEndAt()).isNotNull();
                });

        // e o Marcelo nao responde mais — mas alcanca, por uma concessao que ELE concedeu
        assertThat(custodyRepository.findEmCursoDaPessoa(code.getAnimalId(), marcelo.getPersonId()))
                .isEmpty();

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), marcelo.getPersonId(), LocalDateTime.now()))
                .as("quem entregou o animal perdeu o acesso a ele")
                .isPresent()
                .get()
                .satisfies(g -> {
                    assertThat(g.getGrantedBy().getPersonId()).isEqualTo(marcelo.getPersonId());
                    assertThat(g.getLevel()).isEqualTo(GrantLevel.EDITOR);
                    // SEM PRAZO: a volta e que encerra, e ela nao acontece no relogio
                    assertThat(g.getExpiresAt()).isNull();
                });

        // e a tela dele abre: leitura pela concessao
        assertThat(boardingService.emCurso(code.getAnimalId()).getAnimalName()).isEqualTo("Code");
    }

    /**
     * "Isto é o que aconteceu com ele desde que saiu de casa."
     *
     * O recorte é de JANELA, e ele precisa existir no servidor: filtrar no cliente responderia errado
     * porque a página tem vinte itens e a estadia pode ter mais.
     */
    @Test
    @DisplayName("a linha do tempo recortada mostra so o que aconteceu na estadia")
    void aLinhaDoTempoDaEstadia() {
        // uma observacao ANTES de sair de casa
        observacao("Comeu tudo em casa", LocalDateTime.now().minusDays(3));

        var estadia = boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        // e uma DEPOIS de entrar
        observacao("Dormiu no sol a tarde inteira", LocalDateTime.now().plusMinutes(1));

        var daEstadia = timelineService.doAnimal(
                code.getAnimalId(), false, false, estadia.getStartedAt(), PageRequest.of(0, 20));

        assertThat(daEstadia.getContent())
                .isNotEmpty()
                .allSatisfy(evento -> assertThat(evento.getOccurredAt())
                        .isAfterOrEqualTo(estadia.getStartedAt()));

        // e a linha inteira continua trazendo as duas
        var inteira = timelineService.doAnimal(
                code.getAnimalId(), false, false, null, PageRequest.of(0, 20));

        assertThat(inteira.getTotalElements()).isGreaterThan(daEstadia.getTotalElements());
    }

    /**
     * A volta devolve a custódia a quem entregou — e tira a concessão que a estadia criou.
     *
     * "Quem recebeu devolve para alguém, nunca para lugar nenhum."
     */
    @Test
    @DisplayName("a volta devolve a custodia e revoga a concessao da estadia")
    void aVoltaDevolveACustodia() {
        boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        var devolvida = boardingService.devolver(code.getAnimalId());

        assertThat(devolvida.getEndedAt()).isNotNull();

        assertThat(custodyRepository.findEmCursoDaPessoa(code.getAnimalId(), marcelo.getPersonId()))
                .as("a custodia nao voltou para quem entregou")
                .isPresent();

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), marcelo.getPersonId(), LocalDateTime.now()))
                .as("a concessao da estadia continuou viva depois da volta")
                .isEmpty();
    }

    /**
     * <b>A concessão que já existia NÃO é revogada na volta.</b>
     *
     * A co-tutora tinha acesso antes da viagem e continua com ele depois: revogar aqui tiraria um
     * acesso que ninguém pediu para tirar, e o sintoma apareceria semanas depois, longe daqui.
     */
    @Test
    @DisplayName("a concessao de terceiro sobrevive a estadia")
    void aConcessaoDeTerceiroSobrevive() {
        Person juliana = pessoa("Juliana Dias");

        grantRepository.saveAndFlush(Grant.builder()
                .animal(code)
                .granteePerson(juliana)
                .level(GrantLevel.EDITOR)
                .scopes(new java.util.LinkedHashSet<>(Set.of(GrantScope.CARTEIRA)))
                .grantedBy(marcelo)
                .grantedAt(LocalDateTime.now())
                .build());

        boardingService.hospedar(code.getAnimalId(), paraACreche(7));
        boardingService.devolver(code.getAnimalId());

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), juliana.getPersonId(), LocalDateTime.now()))
                .as("a volta revogou uma concessao que nao era da estadia")
                .isPresent();
    }

    /** A creche registra a saída no balcão — e é o caminho comum. */
    @Test
    @DisplayName("a creche tambem registra a volta")
    void aCrecheRegistraAVolta() {
        boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        agirComo(rafaela, creche);

        assertThat(boardingService.devolver(code.getAnimalId()).getEndedAt()).isNotNull();
    }

    /** Quem não está com o animal nem o entregou não encerra nada. */
    @Test
    @DisplayName("um terceiro nao registra a volta")
    void umTerceiroNaoRegistraAVolta() {
        boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        Person estranho = pessoa("Alguem Qualquer");
        agirComo(estranho, null);

        assertThatThrownBy(() -> boardingService.devolver(code.getAnimalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.CANNOT_END_BOARDING.getCode());
    }

    @Test
    @DisplayName("hospedar duas vezes e recusado")
    void naoHospedaDuasVezes() {
        boardingService.hospedar(code.getAnimalId(), paraACreche(7));

        // agora quem responde e a creche, entao e ela que tenta de novo
        agirComo(rafaela, creche);

        assertThatThrownBy(() -> boardingService.hospedar(code.getAnimalId(), paraACreche(7)))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.ALREADY_BOARDED.getCode());
    }

    /**
     * Uma clínica que só registra ato clínico não passa a responder pelo animal por uma semana. A
     * capacidade existe no modelo desde a Tela 15 justamente para separar essas coisas.
     */
    @Test
    @DisplayName("organizacao que nao pode deter custodia nao hospeda")
    void semCapacidadeDeCustodiaNaoHospeda() {
        var paraAClinica = BoardingRequestDTO.builder()
                .organizationId(clinicaSemCustodia.getOrganizationId())
                .expectedReturnOn(LocalDate.now().plusDays(7))
                .build();

        assertThatThrownBy(() -> boardingService.hospedar(code.getAnimalId(), paraAClinica))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.ORGANIZATION_CANNOT_BOARD.getCode());
    }

    @Test
    @DisplayName("volta prevista no passado e recusada")
    void voltaNoPassado() {
        var ontem = BoardingRequestDTO.builder()
                .organizationId(creche.getOrganizationId())
                .expectedReturnOn(LocalDate.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> boardingService.hospedar(code.getAnimalId(), ontem))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.INVALID_RETURN_DATE.getCode());
    }

    /**
     * Entregar o animal é o mesmo ato que transferir titularidade, e nenhum nível de concessão chega
     * lá: a creche que já tem acesso não pode se auto-hospedar.
     */
    @Test
    @DisplayName("quem so tem concessao nao entrega o animal para hospedagem")
    void concessaoNaoEntregaOAnimal() {
        Person vet = pessoa("Ana Ferreira");

        grantRepository.saveAndFlush(Grant.builder()
                .animal(code)
                .granteePerson(vet)
                .level(GrantLevel.EDITOR)
                .scopes(new java.util.LinkedHashSet<>(Set.of(GrantScope.PRONTUARIO)))
                .grantedBy(marcelo)
                .grantedAt(LocalDateTime.now())
                .build());

        agirComo(vet, null);

        assertThatThrownBy(() -> boardingService.hospedar(code.getAnimalId(), paraACreche(7)))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode());
    }

    @Test
    @DisplayName("animal que nao esta hospedado responde 404")
    void semEstadiaEmCurso() {
        assertThatThrownBy(() -> boardingService.emCurso(code.getAnimalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.BOARDING_NOT_FOUND.getCode());
    }

    private BoardingRequestDTO paraACreche(int dias) {
        return BoardingRequestDTO.builder()
                .organizationId(creche.getOrganizationId())
                .expectedReturnOn(LocalDate.now().plusDays(dias))
                .build();
    }

    private void observacao(String texto, LocalDateTime quando) {
        observationRepository.saveAndFlush(Observation.builder()
                .animal(code)
                .description(texto)
                .observedAt(quando)
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

    private Organization organizacao(String nome, OrganizationCapability capacidade) {
        return organizationRepository.saveAndFlush(Organization.builder()
                .name(nome)
                .capabilities(Set.of(capacidade))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void agirComo(Person pessoa, Organization organizacao) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));

        MockHttpServletRequest requisicao = new MockHttpServletRequest();

        if (organizacao != null) {
            requisicao.addHeader(CurrentProfessionalProvider.HEADER_ORGANIZACAO,
                    organizacao.getOrganizationId().toString());
        }

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
    }
}
