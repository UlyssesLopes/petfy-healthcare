package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetShareRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetShareServiceImplTest {

    @Mock
    private PetShareRepository petShareRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    private PetShareServiceImpl service;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SHARE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate HOJE = LocalDate.now();

    @BeforeEach
    void setUp() {
        // calculator real: o valor do teste esta em conferir o status que o link
        // mostra, e nao em repetir a regra num mock
        service = new PetShareServiceImpl(petShareRepository, petRepository, vaccineRepository,
                currentOwnerProvider, new VaccineStatusCalculator(), new OpaqueTokenService());
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 30);
        ReflectionTestUtils.setField(service, "windowDays", 30);
    }

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br")
                .phone("11999999999").address("Rua A, 100").password("hash").build();
    }

    private Pet petDe(UUID ownerId) {
        return Pet.builder().petId(PET_ID).name("Rex").type("Cachorro").breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15)).gender("Macho").owner(owner(ownerId)).build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    private PetShare shareAtivo() {
        return PetShare.builder()
                .petShareId(SHARE_ID)
                .pet(petDe(OWNER_ID))
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
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, null);

            assertThat(result.getToken()).isNotBlank();
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("nao deve guardar o token, apenas o hash")
        void naoDeveGuardarOToken() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
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
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.createShare(PET_ID, null).getToken())
                    .isNotEqualTo(service.createShare(PET_ID, null).getToken());
        }

        @Test
        @DisplayName("deve usar a validade padrao quando o request nao informa")
        void deveUsarValidadePadrao() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, null);

            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("deve respeitar a validade informada no request")
        void deveRespeitarValidadeInformada() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(petShareRepository.save(any(PetShare.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.createShare(PET_ID, PetShareRequestDTO.builder().expiresInDays(3).build());

            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(4));
        }

        @Test
        @DisplayName("nao deve permitir compartilhar pet de outro dono")
        void naoDevePermitirCompartilharPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

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
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
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
            autenticadoComo(OWNER_ID);
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(share));

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

            autenticadoComo(OWNER_ID);
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(jaRevogado));

            service.revokeShare(SHARE_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(petShareRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve permitir revogar link de pet de outro dono")
        void naoDevePermitirRevogarDeOutroDono() {
            var deOutro = shareAtivo();
            deOutro.setPet(petDe(OUTRO_OWNER_ID));

            autenticadoComo(OWNER_ID);
            when(petShareRepository.findById(SHARE_ID)).thenReturn(Optional.of(deOutro));

            assertThatThrownBy(() -> service.revokeShare(SHARE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(107, HttpStatus.NOT_FOUND);

            verify(petShareRepository, never()).save(any());
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
                    .pet(petDe(OWNER_ID))
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
            // o link nao passa pelo CurrentOwnerProvider
            verify(currentOwnerProvider, never()).require();
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
}
