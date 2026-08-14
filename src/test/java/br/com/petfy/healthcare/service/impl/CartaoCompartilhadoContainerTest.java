package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.AnimalShareService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O cartao que o link do tutor abre (Tela 04), contra Postgres real.
 *
 * <b>Este teste existe por causa de um 500 que nenhum teste via.</b> O {@code viewSharedCard} nao
 * era transacional, e {@code Grant.scopes} e uma colecao lazy — a primeira pergunta "este link
 * alcanca a carteira?" estourava com LazyInitializationException. Os testes de unidade passavam
 * porque o Grant deles vem de um mock, e a rota nao tinha tela nenhuma chamando para revelar.
 */
@SpringBootTest
@DisplayName("o cartao compartilhado, contra Postgres real")
class CartaoCompartilhadoContainerTest extends PostgresContainerTest {

    @Autowired private AnimalShareService animalShareService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private AnimalHealthConditionRepository conditionRepository;
    @Autowired private CareInstructionRepository careInstructionRepository;

    private Animal code;
    private Person tutor;

    @BeforeEach
    void montar() {
        tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("cartao-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .phone("11999998888")
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(tutor).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tutor.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private String linkCom(GrantScope... escopos) {
        return animalShareService.createShare(code.getAnimalId(), AnimalShareRequestDTO.builder()
                .expiresInDays(7)
                .scopes(new java.util.LinkedHashSet<>(List.of(escopos)))
                .build()).getToken();
    }

    /** O caso que estourava: abrir o cartao, com qualquer escopo, fora de transacao. */
    @Test
    @DisplayName("abrir o link nao estoura ao ler os escopos")
    void abrirNaoEstoura() {
        SharedVaccineCardDTO cartao = animalShareService.viewSharedCard(linkCom(GrantScope.CARTEIRA));

        assertThat(cartao.getAnimalName()).isEqualTo("Code");
        assertThat(cartao.getScopes()).containsExactly(GrantScope.CARTEIRA);
    }

    @Test
    @DisplayName("com CONDICOES, a alergia e a medicacao em curso saem juntas")
    void condicoesTrazemAMedicacao() {
        conditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(code).recordedBy(tutor)
                .kind(AnimalHealthConditionKind.ALERGIA)
                .description("Proteina de frango")
                .creationDate(LocalDateTime.now())
                .build());

        careInstructionRepository.saveAndFlush(CareInstruction.builder()
                .animal(code).recordedBy(tutor)
                .description("Amoxicilina 250 mg, 12/12h")
                .intervalDays(1)
                .startsOn(LocalDate.now().minusDays(1))
                .endsOn(LocalDate.now().plusDays(5))
                .creationDate(LocalDateTime.now())
                .build());

        SharedVaccineCardDTO cartao = animalShareService.viewSharedCard(linkCom(GrantScope.CONDICOES));

        assertThat(cartao.getConditions()).extracting(SharedVaccineCardDTO.SharedConditionDTO::getDescription)
                .containsExactly("Proteina de frango");
        assertThat(cartao.getOngoingCare()).containsExactly("Amoxicilina 250 mg, 12/12h");
    }

    /**
     * O escopo e a fronteira, e ela vale nos dois sentidos.
     *
     * Sem CONDICOES o cartao nao carrega alergia nem medicacao — e e por isso que a tela precisa
     * dizer que o link nao alcanca aquilo, em vez de mostrar um bloco vazio que parece dizer que
     * o animal nao tem alergia nenhuma.
     */
    @Test
    @DisplayName("sem CONDICOES nao sai alergia nem medicacao")
    void semCondicoesNaoSaiNada() {
        conditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                .animal(code).recordedBy(tutor)
                .kind(AnimalHealthConditionKind.ALERGIA)
                .description("Proteina de frango")
                .creationDate(LocalDateTime.now())
                .build());

        SharedVaccineCardDTO cartao = animalShareService.viewSharedCard(linkCom(GrantScope.CARTEIRA));

        assertThat(cartao.getConditions()).isEmpty();
        assertThat(cartao.getOngoingCare()).isEmpty();
    }

    @Test
    @DisplayName("com CONTATO sai quem responde pelo animal, com telefone")
    void contatoTrazQuemResponde() {
        SharedVaccineCardDTO cartao = animalShareService.viewSharedCard(linkCom(GrantScope.CONTATO));

        assertThat(cartao.getContacts())
                .extracting(SharedVaccineCardDTO.SharedContactDTO::getName,
                            SharedVaccineCardDTO.SharedContactDTO::getPhone,
                            SharedVaccineCardDTO.SharedContactDTO::getKind)
                .containsExactly(org.assertj.core.api.Assertions.tuple(
                        "Marcelo Dias", "11999998888", "TUTOR"));
    }

    @Test
    @DisplayName("sem CONTATO nao sai telefone de ninguem")
    void semContatoNaoSaiTelefone() {
        assertThat(animalShareService.viewSharedCard(linkCom(GrantScope.CARTEIRA)).getContacts())
                .isEmpty();
    }

    /** Token inventado responde igual a revogado e a vencido: 404, sem dizer qual dos tres. */
    @Test
    @DisplayName("token que nao existe e recusado")
    void tokenInventadoERecusado() {
        assertThatThrownBy(() -> animalShareService.viewSharedCard("nao-existe"))
                .isInstanceOf(PetfyHealthcareException.class);
    }

}
