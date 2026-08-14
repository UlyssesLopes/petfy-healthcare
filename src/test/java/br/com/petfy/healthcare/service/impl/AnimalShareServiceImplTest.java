package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.AnimalContacts;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalShareServiceImplTest {

    @Mock
    private GrantRepository grantRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private AnimalAccessGuard animalAccessGuard;

    @Mock
    private SensitiveAccessLogger sensitiveAccessLogger;

    @Mock
    private AnimalHealthConditionRepository conditionRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private CareInstructionRepository careInstructionRepository;

    @Mock
    private AnimalContacts animalContacts;

    private AnimalShareServiceImpl service;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SHARE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate HOJE = LocalDate.now();

    @BeforeEach
    void setUp() {
        // calculator real: o valor do teste esta em conferir o status que o link
        // mostra, e nao em repetir a regra num mock
        service = new AnimalShareServiceImpl(grantRepository, vaccineRepository,
                conditionRepository, careInstructionRepository, animalContacts,
                animalAccessGuard, currentPersonProvider,
                new VaccineStatusCalculator(), new OpaqueTokenService(), sensitiveAccessLogger);
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 30);
        ReflectionTestUtils.setField(service, "windowDays", 30);
    }

    private Person person(UUID id) {
        return Person.builder().personId(id).name("Ulysses").email("ulysses@petfy.com.br")
                .phone("11999999999").address("Rua A, 100").password("hash").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").type("Cachorro").breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15)).gender("Macho").custodies(Custodias.titular(person(OWNER_ID))).build();
    }

    private PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private Grant shareAtivo() {
        return Grant.builder()
                .grantId(SHARE_ID)
                .animal(animal())
                .tokenHash("hash-qualquer")
                .level(GrantLevel.VIEWER)
                .scopes(new java.util.LinkedHashSet<>(java.util.Set.of(GrantScope.CARTEIRA)))
                .grantedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();
    }

    @Nested
    @DisplayName("createShare")
    class CreateShare {

        @Test
        @DisplayName("deve devolver o token apenas na criacao")
        void deveDevolverTokenApenasNaCriacao() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(ANIMAL_ID, null);

            assertThat(result.getToken()).isNotBlank();
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("nao deve guardar o token, apenas o hash")
        void naoDeveGuardarOToken() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(ANIMAL_ID, null);

            var captor = ArgumentCaptor.forClass(Grant.class);
            verify(grantRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash())
                    .isNotBlank()
                    .isNotEqualTo(result.getToken());
        }

        @Test
        @DisplayName("a entidade de share nao deve ter campo para o token em claro")
        void entidadeNaoDeveTerCampoDeTokenEmClaro() {
            assertThat(Grant.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("tokenHash")
                    .doesNotContain("token");
        }

        @Test
        @DisplayName("dois links do mesmo animal devem ter tokens diferentes")
        void doisLinksDevemTerTokensDiferentes() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.createShare(ANIMAL_ID, null).getToken())
                    .isNotEqualTo(service.createShare(ANIMAL_ID, null).getToken());
        }

        @Test
        @DisplayName("deve usar a validade padrao quando o request nao informa")
        void deveUsarValidadePadrao() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(ANIMAL_ID, null);

            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("deve respeitar a validade informada no request")
        void deveRespeitarValidadeInformada() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(ANIMAL_ID, AnimalShareRequestDTO.builder().expiresInDays(3).build());

            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(4));
        }

        @Test
        @DisplayName("recusa do guard impede a criacao do link")
        void recusaDoGuardNaoCriaLink() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> service.createShare(ANIMAL_ID, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(grantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listShares")
    class ListShares {

        @Test
        @DisplayName("nao deve devolver o token nas listagens")
        void naoDeveDevolverTokenNasListagens() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(any())).thenReturn(List.of(shareAtivo()));

            assertThat(service.listShares(ANIMAL_ID)).singleElement()
                    .satisfies(share -> {
                        assertThat(share.getToken()).isNull();
                        assertThat(share.isActive()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("revokeShare")
    class RevokeShare {

        @Test
        @DisplayName("deve marcar a data de revogacao")
        void deveMarcarDataDeRevogacao() {
            var share = shareAtivo();
            when(grantRepository.findById(SHARE_ID)).thenReturn(Optional.of(share));
            when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            assertThat(share.getRevokedAt()).isNotNull();
            assertThat(share.estaVigente(LocalDateTime.now())).isFalse();
            verify(grantRepository).save(share);
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaDataOriginal() {
            var jaRevogado = shareAtivo();
            var original = LocalDateTime.now().minusDays(3);
            jaRevogado.setRevokedAt(original);

            when(grantRepository.findById(SHARE_ID)).thenReturn(Optional.of(jaRevogado));
            when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(grantRepository, never()).save(any());
        }

        /**
         * Qualquer tutor do animal corta o link, inclusive quem nao o criou: o link
         * expoe a carteira do animal, e nao um recurso de quem o gerou.
         */
        @Test
        @DisplayName("basta alcancar o animal para revogar, em qualquer papel")
        void bastaAlcancarOAnimal() {
            when(grantRepository.findById(SHARE_ID)).thenReturn(Optional.of(shareAtivo()));
            when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            verify(animalAccessGuard, never()).requireEscrita(any());
            verify(animalAccessGuard, never()).requireCustodia(any());
        }

        @Test
        @DisplayName("link de animal fora do alcance responde SHARE_NOT_FOUND e nao e revogado")
        void linkForaDoAlcanceNaoERevogado() {
            when(grantRepository.findById(SHARE_ID)).thenReturn(Optional.of(shareAtivo()));
            when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.revokeShare(SHARE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);

            verify(grantRepository, never()).save(any());
        }

        @Test
        @DisplayName("link inexistente responde SHARE_NOT_FOUND sem perguntar ao guard")
        void linkInexistenteResponde404() {
            when(grantRepository.findById(SHARE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeShare(SHARE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);

            verify(animalAccessGuard, never()).alcanca(any());
        }
    }

    @Nested
    @DisplayName("viewSharedCard")
    class ViewSharedCard {

        private void linkValidoCom(Vaccine... vacinas) {
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(shareAtivo()));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID)).thenReturn(List.of(vacinas));
        }

        private Vaccine vacina(String nome, LocalDate proximaDose) {
            return Vaccine.builder()
                    .vaccineId(UUID.randomUUID())
                    .vaccineName(nome)
                    .applicationDate(HOJE.minusYears(1))
                    .nextDoseDate(proximaDose)
                    .organization(Organization.builder().organizationId(UUID.randomUUID()).name("Clinica Bicho Feliz").build())
                    .animal(animal())
                    .build();
        }

        @Test
        @DisplayName("deve mostrar a carteira sem exigir autenticacao")
        void deveMostrarCarteiraSemAutenticacao() {
            linkValidoCom(vacina("V10", HOJE.plusDays(200)));

            var card = service.viewSharedCard("token-qualquer");

            assertThat(card.getAnimalName()).isEqualTo("Rex");
            assertThat(card.getPersonName()).isEqualTo("Ulysses");
            assertThat(card.getVaccines()).hasSize(1);
            // o link vale por si: nao ha pessoa autenticada sobre quem perguntar,
            // entao o guard nao pode entrar nesse caminho
            verifyNoInteractions(animalAccessGuard);
        }

        @Test
        @DisplayName("deve classificar cada vacina, que e o que interessa a quem pede a carteira")
        void deveClassificarCadaVacina() {
            linkValidoCom(
                    vacina("Atrasada", HOJE.minusDays(5)),
                    vacina("Vencendo", HOJE.plusDays(10)),
                    vacina("Em dia", HOJE.plusDays(200)),
                    vacina("Sem proxima", null));

            var card = service.viewSharedCard("token-qualquer");

            assertThat(card.getVaccines())
                    .extracting(SharedVaccineCardDTO.SharedVaccineDTO::getStatus)
                    .containsExactly(VaccineStatus.OVERDUE, VaccineStatus.DUE_SOON,
                            VaccineStatus.UP_TO_DATE, VaccineStatus.NO_NEXT_DOSE);
        }

        /**
         * A regra mudou no P2a, e o teste mudou com ela.
         *
         * Antes o link nunca carregava contato, e conferir a ausencia do campo bastava.
         * Agora ele pode carregar o telefone - mas <b>so com o escopo CONTATO</b>, que
         * o tutor concede de propria vontade, e que existe para o cartao de emergencia.
         * O que se protege agora e o gate, e nao a ausencia: e-mail, endereco e id
         * continuam fora em qualquer escopo.
         */
        @Test
        @DisplayName("telefone do tutor so sai com o escopo CONTATO; email e endereco nunca")
        void contatoDoTutorSoSaiComEscopo() {
            assertThat(SharedVaccineCardDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("personName", "contacts")
                    .doesNotContain("personEmail", "personAddress", "personId");
        }

        @Test
        @DisplayName("sem o escopo CONTATO nao sai contato nenhum")
        void semEscopoContatoNaoSaiTelefone() {
            var semContato = shareAtivo();
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(semContato));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of());

            assertThat(service.viewSharedCard("token-qualquer").getContacts()).isEmpty();
            // sem o escopo nem se pergunta a quem ligar
            verifyNoInteractions(animalContacts);
        }

        @Test
        @DisplayName("com o escopo CONTATO sai o tutor e a clinica que atende")
        void comEscopoContatoSaiTelefone() {
            var comContato = shareAtivo();
            comContato.getScopes().add(GrantScope.CONTATO);
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(comContato));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of());
            when(animalContacts.de(any())).thenReturn(List.of(
                    new AnimalContacts.Contato("Marcelo Dias", "11999999999", AnimalContacts.Contato.TUTOR),
                    new AnimalContacts.Contato("Clinica Vet Norte", "1133334444", AnimalContacts.Contato.ORGANIZACAO)));

            /* Quem responde pelo animal vem primeiro: e quem pode ir busca-lo. */
            assertThat(service.viewSharedCard("token-qualquer").getContacts())
                    .extracting(SharedVaccineCardDTO.SharedContactDTO::getName,
                                SharedVaccineCardDTO.SharedContactDTO::getPhone)
                    .containsExactly(
                            tuple("Marcelo Dias", "11999999999"),
                            tuple("Clinica Vet Norte", "1133334444"));
        }

        /**
         * A medicacao acompanha a condicao, e nao a carteira.
         *
         * Quem concede "o que ele tem" concede junto "o que ele esta tomando por causa disso" —
         * e e o bloco que o desenho da Tela 04 poe entre condicoes e vacinacao.
         */
        @Test
        @DisplayName("a medicacao em curso sai com o escopo CONDICOES, e so ela")
        void medicacaoEmCursoSaiComCondicoes() {
            var comCondicoes = shareAtivo();
            comCondicoes.getScopes().add(GrantScope.CONDICOES);
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(comCondicoes));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of());
            when(conditionRepository.findByAnimalOrdenadasPorRelevancia(ANIMAL_ID))
                    .thenReturn(List.of());
            when(careInstructionRepository.findVigentesNosAnimais(eq(List.of(ANIMAL_ID)), any()))
                    .thenReturn(List.of(CareInstruction.builder()
                            .description("Amoxicilina 250 mg, 12/12h").build()));

            assertThat(service.viewSharedCard("token-qualquer").getOngoingCare())
                    .containsExactly("Amoxicilina 250 mg, 12/12h");
        }

        @Test
        @DisplayName("sem o escopo CONDICOES a medicacao em curso nao sai")
        void semCondicoesNaoSaiMedicacao() {
            var semCondicoes = shareAtivo();
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(semCondicoes));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of());

            assertThat(service.viewSharedCard("token-qualquer").getOngoingCare()).isEmpty();
            verifyNoInteractions(careInstructionRepository);
        }

        @Test
        @DisplayName("nao deve expor o historico de saude - o link e a carteira, nao o prontuario")
        void naoDeveExporHistoricoDeSaude() {
            assertThat(SharedVaccineCardDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("healthRecords");
        }

        @Test
        @DisplayName("deve recusar token que nao existe")
        void deveRecusarTokenInexistente() {
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.viewSharedCard("token-invalido"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("deve recusar link expirado")
        void deveRecusarLinkExpirado() {
            var expirado = shareAtivo();
            expirado.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(expirado));

            assertThatThrownBy(() -> service.viewSharedCard("token-qualquer"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Share link not found or no longer valid");
        }

        @Test
        @DisplayName("deve recusar link revogado")
        void deveRecusarLinkRevogado() {
            var revogado = shareAtivo();
            revogado.setRevokedAt(LocalDateTime.now().minusMinutes(1));
            when(grantRepository.findByTokenHash(any())).thenReturn(Optional.of(revogado));

            assertThatThrownBy(() -> service.viewSharedCard("token-qualquer"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Share link not found or no longer valid");
        }

        @Test
        @DisplayName("token inexistente, expirado e revogado devem responder igual")
        void tresCasosDevemResponderIgual() {
            var expirado = shareAtivo();
            expirado.setExpiresAt(LocalDateTime.now().minusDays(1));
            var revogado = shareAtivo();
            revogado.setRevokedAt(LocalDateTime.now());

            when(grantRepository.findByTokenHash(any()))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(expirado))
                    .thenReturn(Optional.of(revogado));

            var mensagens = List.of(
                    capturaMensagem(), capturaMensagem(), capturaMensagem());

            assertThat(mensagens).containsOnly("Share link not found or no longer valid");
        }

        private String capturaMensagem() {
            try {
                service.viewSharedCard("token-qualquer");
                throw new AssertionError("deveria ter lancado PetfyHealthcareException");
            } catch (PetfyHealthcareException e) {
                return e.getMessage();
            }
        }
    }

    /**
     * O nivel que cada operacao exige do guard. Criar e listar link e escrita:
     * expor a carteira do animal para fora e decisao de quem cuida dele, e nao de
     * quem so acompanha.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("criar o link exige escrita")
        void criarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));

            service.createShare(ANIMAL_ID, null);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar os links exige escrita")
        void listarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(any())).thenReturn(List.of());

            service.listShares(ANIMAL_ID);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }
    }
}
