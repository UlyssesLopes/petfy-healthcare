package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.FoundAnimalCardDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * "Achei um animal na rua" (Tela 34).
 *
 * <b>O risco desta rota nao e ela quebrar — e ela entregar demais.</b> Publica, sem conta e
 * indexada por um numero, ela devolve nome e telefone; os casos aqui cercam os dois lados: o que
 * ela precisa mostrar a quem esta com o animal, e o que ela nunca pode mostrar a ninguem.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("a busca de animal encontrado")
class FoundAnimalServiceImplTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private AnimalDeathRepository animalDeathRepository;
    @Mock private AnimalHealthConditionRepository conditionRepository;
    @Mock private CareInstructionRepository careInstructionRepository;
    @Mock private CustodyRepository custodyRepository;
    @Mock private GrantRepository grantRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private VaccineStatusCalculator vaccineStatusCalculator;
    @Mock private SensitiveAccessLogger sensitiveAccessLogger;

    private FoundAnimalServiceImpl service;

    private static final String CHIP = "981020003451275";
    private static final UUID ANIMAL = UUID.randomUUID();

    private final Person marcelo = Person.builder()
            .personId(UUID.randomUUID()).name("Marcelo Dias").phone("11999990000").build();

    private final Animal code = Animal.builder()
            .animalId(ANIMAL).name("Code").type("Cao").breed("SRD").weight(8.4)
            .microchipNumber(CHIP).creationDate(LocalDateTime.of(2019, 3, 14, 9, 0)).build();

    @BeforeEach
    void setUp() {
        service = new FoundAnimalServiceImpl(animalRepository, animalDeathRepository,
                conditionRepository, careInstructionRepository, custodyRepository,
                grantRepository, vaccineRepository, vaccineStatusCalculator,
                sensitiveAccessLogger);

        lenient().when(animalRepository.findComMicrochip(CHIP)).thenReturn(List.of(code));
        lenient().when(animalDeathRepository.existsById(any())).thenReturn(false);
        lenient().when(custodyRepository.findEmCurso(ANIMAL)).thenReturn(Optional.of(
                Custody.builder().animal(code).holderPerson(marcelo).build()));
        lenient().when(grantRepository.findVigentesDeOrganizacoesNoAnimal(any(), any()))
                .thenReturn(List.of());
        lenient().when(conditionRepository.findByAnimalOrdenadasPorRelevancia(ANIMAL))
                .thenReturn(List.of());
        lenient().when(careInstructionRepository.findVigentesNosAnimais(anyList(), any()))
                .thenReturn(List.of());
        lenient().when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL))
                .thenReturn(List.of());
        lenient().when(vaccineStatusCalculator.classify(any(), any(), anyInt())).thenReturn(null);
    }

    @Test
    @DisplayName("traz quem responde pelo animal em primeiro, com telefone")
    void contatoDoTutor() {
        FoundAnimalCardDTO cartao = service.procurar(CHIP);

        assertThat(cartao.getContacts()).hasSize(1);
        assertThat(cartao.getContacts().get(0).getName()).isEqualTo("Marcelo Dias");
        assertThat(cartao.getContacts().get(0).getPhone()).isEqualTo("11999990000");
        assertThat(cartao.getContacts().get(0).getKind()).isEqualTo("TUTOR");
    }

    @Test
    @DisplayName("traz a clinica com acesso vivo depois do tutor")
    void contatoDaClinica() {
        Organization vetNorte = Organization.builder()
                .organizationId(UUID.randomUUID()).name("Clinica Vet Norte")
                .phone("1133330000").build();

        when(grantRepository.findVigentesDeOrganizacoesNoAnimal(any(), any()))
                .thenReturn(List.of(Grant.builder().animal(code).granteeOrganization(vetNorte).build()));

        FoundAnimalCardDTO cartao = service.procurar(CHIP);

        // a ordem e a do desenho, e nao e cosmetica: quem responde pelo animal e quem pode ir
        // busca-lo
        assertThat(cartao.getContacts()).hasSize(2);
        assertThat(cartao.getContacts().get(0).getKind()).isEqualTo("TUTOR");
        assertThat(cartao.getContacts().get(1).getName()).isEqualTo("Clinica Vet Norte");
    }

    @Test
    @DisplayName("separa alergia de condicao, e deixa de fora a que ja foi resolvida")
    void alergiaECondicao() {
        when(conditionRepository.findByAnimalOrdenadasPorRelevancia(ANIMAL)).thenReturn(List.of(
                AnimalHealthCondition.builder()
                        .kind(AnimalHealthConditionKind.ALERGIA)
                        .description("Proteina de frango").build(),
                AnimalHealthCondition.builder()
                        .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                        .description("Displasia coxofemoral leve").build(),
                AnimalHealthCondition.builder()
                        .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                        .description("Otite")
                        .resolvedAt(LocalDate.of(2025, 1, 10)).build()));

        FoundAnimalCardDTO cartao = service.procurar(CHIP);

        assertThat(cartao.getAllergies()).containsExactly("Proteina de frango");
        // a encerrada faria quem socorre agir sobre um fato que deixou de valer
        assertThat(cartao.getConditions()).containsExactly("Displasia coxofemoral leve");
    }

    @Test
    @DisplayName("traz a medicacao em curso")
    void medicacaoEmCurso() {
        when(careInstructionRepository.findVigentesNosAnimais(anyList(), any())).thenReturn(List.of(
                CareInstruction.builder()
                        .description("Amoxicilina 250 mg, 12/12h, ate 12/08").build()));

        assertThat(service.procurar(CHIP).getOngoingCare())
                .containsExactly("Amoxicilina 250 mg, 12/12h, ate 12/08");
    }

    @Test
    @DisplayName("registra a busca no log que o tutor le, sem identificar quem buscou")
    void registraNoLog() {
        service.procurar(CHIP);

        verify(sensitiveAccessLogger).microchipProcurado(code);
    }

    @Test
    @DisplayName("numero que nao esta no Petfy responde 404 com codigo proprio")
    void naoEncontrado() {
        when(animalRepository.findComMicrochip(CHIP)).thenReturn(List.of());

        assertThatThrownBy(() -> service.procurar(CHIP))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.MICROCHIP_NOT_FOUND.getCode());

        // sem animal nao ha acesso a registrar, e logar a tentativa encheria a tabela de linhas
        // sem animal nenhum
        verify(sensitiveAccessLogger, never()).microchipProcurado(any());
    }

    @Test
    @DisplayName("animal com a linha do tempo encerrada responde como numero inexistente")
    void animalEncerrado() {
        when(animalDeathRepository.existsById(ANIMAL)).thenReturn(true);

        assertThatThrownBy(() -> service.procurar(CHIP))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("com dois cadastros, responde o que tem quem responda pelo animal")
    void duplicadoPrefereQuemTemCustodia() {
        Animal recemAberto = Animal.builder()
                .animalId(UUID.randomUUID()).name("Cao sem nome").microchipNumber(CHIP)
                .creationDate(LocalDateTime.now()).build();

        when(animalRepository.findComMicrochip(CHIP)).thenReturn(List.of(recemAberto, code));
        when(custodyRepository.findEmCurso(recemAberto.getAnimalId())).thenReturn(Optional.empty());

        // o cadastro que a clinica abriu as 22h com tres campos e o mais novo — e seria o
        // escolhido por qualquer criterio ingenuo de "o mais recente"
        assertThat(service.procurar(CHIP).getAnimalName()).isEqualTo("Code");
    }

    @Test
    @DisplayName("nao devolve nada alem do cartao: sem id de animal e sem historico")
    void naoVazaAlemDoCartao() {
        // O caso olha o FORMATO do cartao, e nao uma resposta: acrescentar id de animal, endereco
        // ou diagnostico a este DTO tem de quebrar um teste em vez de passar despercebido numa
        // revisao — esta e a unica rota do produto que responde a quem nao tem conta nenhuma.
        assertThat(FoundAnimalCardDTO.class.getDeclaredFields())
                .extracting(campo -> campo.getName().toLowerCase())
                .noneMatch(nome -> nome.endsWith("id")
                        || nome.contains("address")
                        || nome.contains("endereco")
                        || nome.contains("diagnos")
                        || nome.contains("record"));
    }

}
