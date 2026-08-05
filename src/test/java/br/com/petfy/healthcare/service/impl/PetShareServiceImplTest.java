package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.PetShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.PetShareRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.security.OpaqueTokenService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetShareServiceImplTest {

    @Mock
    private PetShareRepository petShareRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private PetAccessGuard petAccessGuard;

    @Mock
    private SensitiveAccessLogger sensitiveAccessLogger;

    private PetShareServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SHARE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate HOJE = LocalDate.now();

    @BeforeEach
    void setUp() {
        // calculator real: o valor do teste esta em conferir o status que o link
        // mostra, e nao em repetir a regra num mock
        service = new PetShareServiceImpl(petShareRepository, vaccineRepository,
                petAccessGuard, new VaccineStatusCalculator(), new OpaqueTokenService(),
                sensitiveAccessLogger);
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 30);
        ReflectionTestUtils.setField(service, "windowDays", 30);
    }

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br")
                .phone("11999999999").address("Rua A, 100").password("hash").build();
    }

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").type("Cachorro").breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15)).gender("Macho").tutors(PetTutores.titular(owner(OWNER_ID))).build();
    }

    private PetfyHealthcareException petNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private PetShare shareAtivo() {
        return PetShare.builder()
                .petShareId(SHARE_ID)
                .pet(pet())
                .tokenHash("hash-qualquer")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .creationDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("createShare")
    class CreateShare {

        @Test
        @DisplayName("deve devolver o token apenas na criacao")
        void deveDevolverTokenApenasNaCriacao() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, null);

            assertThat(result.getToken()).isNotBlank();
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("nao deve guardar o token, apenas o hash")
        void naoDeveGuardarOToken() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, null);

            var captor = ArgumentCaptor.forClass(PetShare.class);
            verify(petShareRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash())
                    .isNotBlank()
                    .isNotEqualTo(result.getToken());
        }

        @Test
        @DisplayName("a entidade de share nao deve ter campo para o token em claro")
        void entidadeNaoDeveTerCampoDeTokenEmClaro() {
            assertThat(PetShare.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("tokenHash")
                    .doesNotContain("token");
        }

        @Test
        @DisplayName("dois links do mesmo pet devem ter tokens diferentes")
        void doisLinksDevemTerTokensDiferentes() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.createShare(PET_ID, null).getToken())
                    .isNotEqualTo(service.createShare(PET_ID, null).getToken());
        }

        @Test
        @DisplayName("deve usar a validade padrao quando o request nao informa")
        void deveUsarValidadePadrao() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, null);

            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("deve respeitar a validade informada no request")
        void deveRespeitarValidadeInformada() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, PetShareRequestDTO.builder().expiresInDays(3).build());

            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(4));
        }

        @Test
        @DisplayName("recusa do guard impede a criacao do link")
        void recusaDoGuardNaoCriaLink() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenThrow(petNaoEncontrado());

            assertThatThrownBy(() -> service.createShare(PET_ID, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(petShareRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listShares")
    class ListShares {

        @Test
        @DisplayName("nao deve devolver o token nas listagens")
        void naoDeveDevolverTokenNasListagens() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.findByPetOrderByCreationDateDesc(any())).thenReturn(List.of(shareAtivo()));

            assertThat(service.listShares(PET_ID)).singleElement()
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
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(share));
            when(petAccessGuard.alcanca(PET_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            assertThat(share.getRevokedAt()).isNotNull();
            assertThat(share.isActive(LocalDateTime.now())).isFalse();
            verify(petShareRepository).save(share);
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaDataOriginal() {
            var jaRevogado = shareAtivo();
            var original = LocalDateTime.now().minusDays(3);
            jaRevogado.setRevokedAt(original);

            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(jaRevogado));
            when(petAccessGuard.alcanca(PET_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(petShareRepository, never()).save(any());
        }

        /**
         * Qualquer tutor do pet corta o link, inclusive quem nao o criou: o link
         * expoe a carteira do pet, e nao um recurso de quem o gerou.
         */
        @Test
        @DisplayName("basta alcancar o pet para revogar, em qualquer papel")
        void bastaAlcancarOPet() {
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(shareAtivo()));
            when(petAccessGuard.alcanca(PET_ID)).thenReturn(true);

            service.revokeShare(SHARE_ID);

            verify(petAccessGuard, never()).requireEscrita(any());
            verify(petAccessGuard, never()).requireTitular(any());
        }

        @Test
        @DisplayName("link de pet fora do alcance responde SHARE_NOT_FOUND e nao e revogado")
        void linkForaDoAlcanceNaoERevogado() {
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(shareAtivo()));
            when(petAccessGuard.alcanca(PET_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.revokeShare(SHARE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);

            verify(petShareRepository, never()).save(any());
        }

        @Test
        @DisplayName("link inexistente responde SHARE_NOT_FOUND sem perguntar ao guard")
        void linkInexistenteResponde404() {
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeShare(SHARE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);

            verify(petAccessGuard, never()).alcanca(any());
        }
    }

    @Nested
    @DisplayName("viewSharedCard")
    class ViewSharedCard {

        private void linkValidoCom(Vaccine... vacinas) {
            when(petShareRepository.findByTokenHash(any())).thenReturn(Optional.of(shareAtivo()));
            when(vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(PET_ID)).thenReturn(List.of(vacinas));
        }

        private Vaccine vacina(String nome, LocalDate proximaDose) {
            return Vaccine.builder()
                    .vaccineId(UUID.randomUUID())
                    .vaccineName(nome)
                    .applicationDate(HOJE.minusYears(1))
                    .nextDoseDate(proximaDose)
                    .clinic(Clinic.builder().clinicId(UUID.randomUUID()).name("Clinica Bicho Feliz").build())
                    .pet(pet())
                    .build();
        }

        @Test
        @DisplayName("deve mostrar a carteira sem exigir autenticacao")
        void deveMostrarCarteiraSemAutenticacao() {
            linkValidoCom(vacina("V10", HOJE.plusDays(200)));

            var card = service.viewSharedCard("token-qualquer");

            assertThat(card.getPetName()).isEqualTo("Rex");
            assertThat(card.getOwnerName()).isEqualTo("Ulysses");
            assertThat(card.getVaccines()).hasSize(1);
            // o link vale por si: nao ha pessoa autenticada sobre quem perguntar,
            // entao o guard nao pode entrar nesse caminho
            verifyNoInteractions(petAccessGuard);
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

        @Test
        @DisplayName("nao deve expor dados de contato do tutor")
        void naoDeveExporContatoDoTutor() {
            assertThat(SharedVaccineCardDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("ownerName")
                    .doesNotContain("ownerEmail", "ownerPhone", "ownerAddress", "ownerId");
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
            when(petShareRepository.findByTokenHash(any())).thenReturn(Optional.empty());

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
            when(petShareRepository.findByTokenHash(any())).thenReturn(Optional.of(expirado));

            assertThatThrownBy(() -> service.viewSharedCard("token-qualquer"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Share link not found or no longer valid");
        }

        @Test
        @DisplayName("deve recusar link revogado")
        void deveRecusarLinkRevogado() {
            var revogado = shareAtivo();
            revogado.setRevokedAt(LocalDateTime.now().minusMinutes(1));
            when(petShareRepository.findByTokenHash(any())).thenReturn(Optional.of(revogado));

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

            when(petShareRepository.findByTokenHash(any()))
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
     * expor a carteira do pet para fora e decisao de quem cuida dele, e nao de
     * quem so acompanha.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("criar o link exige escrita")
        void criarExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            service.createShare(PET_ID, null);

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar os links exige escrita")
        void listarExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(pet());
            when(petShareRepository.findByPetOrderByCreationDateDesc(any())).thenReturn(List.of());

            service.listShares(PET_ID);

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }
    }
}
