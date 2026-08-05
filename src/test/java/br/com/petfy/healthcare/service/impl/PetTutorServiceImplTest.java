package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.PetTutorActivityNotifier;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.security.PetAccessGuard;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetTutorServiceImplTest {

    @Mock
    private PetTutorRepository petTutorRepository;

    @Mock
    private PetTutorInviteRepository petTutorInviteRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @Mock
    private PetAccessGuard petAccessGuard;

    @Mock
    private PetTutorActivityNotifier petTutorActivityNotifier;

    private PetTutorServiceImpl petTutorService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MARIA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INVITE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        // OpaqueTokenService real: o valor do teste esta em o token gerado nao ser
        // o que fica guardado, e um mock devolveria o que mandassem
        petTutorService = new PetTutorServiceImpl(petTutorRepository, petTutorInviteRepository,
                ownerRepository, currentOwnerProvider, petAccessGuard, new OpaqueTokenService(),
                petTutorActivityNotifier);
        ReflectionTestUtils.setField(petTutorService, "defaultExpirationDays", 7);
    }

    private Owner ulysses() {
        return Owner.builder().ownerId(OWNER_ID).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Owner maria() {
        return Owner.builder().ownerId(MARIA_ID).name("Maria").email("maria@petfy.com.br").build();
    }

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").species(Species.CANINA)
                .tutors(PetTutores.titular(ulysses())).build();
    }

    private void autenticado(Owner owner) {
        when(currentOwnerProvider.require()).thenReturn(owner);
    }

    private PetTutor vinculoDe(Owner owner, PetTutorRole papel) {
        PetTutor vinculo = PetTutores.vinculo(owner, papel);
        vinculo.setPet(pet());
        return vinculo;
    }

    @Nested
    @DisplayName("invite")
    class Invite {

        private PetTutorInviteRequestDTO paraMaria(PetTutorRole papel) {
            return PetTutorInviteRequestDTO.builder().email("maria@petfy.com.br").role(papel).build();
        }

        @Test
        @DisplayName("devolve o token uma unica vez e guarda apenas o hash")
        void devolveTokenEGuardaHash() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.invite(PET_ID, paraMaria(PetTutorRole.EDITOR));

            assertThat(result.getToken()).isNotBlank();
            assertThat(result.getRole()).isEqualTo(PetTutorRole.EDITOR);
            assertThat(result.isTransfersHolder()).isFalse();

            var captor = ArgumentCaptor.forClass(PetTutorInvite.class);
            verify(petTutorInviteRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash())
                    .isNotBlank()
                    .isNotEqualTo(result.getToken());
            assertThat(captor.getValue().getEmail()).isEqualTo("maria@petfy.com.br");
        }

        @Test
        @DisplayName("convite com papel HOLDER se anuncia como transferencia")
        void conviteHolderSeAnuncia() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(petTutorService.invite(PET_ID, paraMaria(PetTutorRole.HOLDER)).isTransfersHolder())
                    .isTrue();
        }

        @Test
        @DisplayName("usa a validade padrao quando o request nao informa")
        void usaValidadePadrao() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.invite(PET_ID, paraMaria(PetTutorRole.VIEWER));

            assertThat(result.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusDays(6))
                    .isBefore(LocalDateTime.now().plusDays(8));
        }

        /**
         * Quem ja e tutor nao entra de novo: a chave unica (pet, owner) recusaria o
         * vinculo no aceite, e o erro cairia em quem recebeu o convite em vez de em
         * quem o mandou errado.
         */
        @Test
        @DisplayName("convidar quem ja e tutor responde 409 sem gravar convite")
        void naoConvidaQuemJaETutor() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.of(maria()));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(true);

            var request = paraMaria(PetTutorRole.EDITOR);

            assertThatThrownBy(() -> petTutorService.invite(PET_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ALREADY_A_TUTOR.getCode(), HttpStatus.CONFLICT);

            verify(petTutorInviteRepository, never()).save(any());
        }

        /** Convidar quem ainda nao tem conta e o caso comum, e nao um erro. */
        @Test
        @DisplayName("convida quem ainda nao tem conta")
        void convidaQuemNaoTemConta() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(petTutorService.invite(PET_ID, paraMaria(PetTutorRole.EDITOR)).getToken()).isNotBlank();

            verify(petTutorRepository, never()).existsByPetPetIdAndOwnerOwnerId(any(), any());
        }
    }

    @Nested
    @DisplayName("accept")
    class Accept {

        private final OpaqueTokenService tokens = new OpaqueTokenService();

        private PetTutorInvite convitePara(String email, PetTutorRole papel, String token) {
            return PetTutorInvite.builder()
                    .petTutorInviteId(INVITE_ID)
                    .pet(pet())
                    .createdBy(ulysses())
                    .tokenHash(tokens.hash(token))
                    .email(email)
                    .role(papel)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .creationDate(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("cria o vinculo no papel do convite e registra quem convidou")
        void criaVinculoNoPapelDoConvite() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.accept("t");

            assertThat(result.getRole()).isEqualTo(PetTutorRole.EDITOR);
            assertThat(result.getOwnerId()).isEqualTo(MARIA_ID);
            assertThat(result.getInvitedByOwnerName()).isEqualTo("Ulysses");
        }

        @Test
        @DisplayName("marca o convite como usado, com quem aceitou")
        void marcaConviteComoUsado() {
            var invite = convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t");
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t"))).thenReturn(Optional.of(invite));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.accept("t");

            var captor = ArgumentCaptor.forClass(PetTutorInvite.class);
            verify(petTutorInviteRepository).save(captor.capture());
            assertThat(captor.getValue().getAcceptedAt()).isNotNull();
            assertThat(captor.getValue().getAcceptedBy().getOwnerId()).isEqualTo(MARIA_ID);
            assertThat(captor.getValue().isUsable(LocalDateTime.now())).isFalse();
        }

        /**
         * A ordem e a regra: o indice unico parcial da V15 admite um HOLDER por pet,
         * entao rebaixar o titular atual tem de acontecer - e chegar ao banco - antes
         * de o novo vinculo HOLDER ser inserido.
         */
        @Test
        @DisplayName("convite HOLDER rebaixa o titular atual antes de inserir o novo")
        void conviteHolderRebaixaAntesDeInserir() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);

            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.HOLDER, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.findByPetPetIdAndRole(PET_ID, PetTutorRole.HOLDER))
                    .thenReturn(Optional.of(titularAtual));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.accept("t");

            assertThat(result.getRole()).isEqualTo(PetTutorRole.HOLDER);
            assertThat(titularAtual.getRole()).isEqualTo(PetTutorRole.EDITOR);

            var ordem = inOrder(petTutorRepository);
            ordem.verify(petTutorRepository).save(titularAtual);
            ordem.verify(petTutorRepository).flush();
            ordem.verify(petTutorRepository).save(any(PetTutor.class));
        }

        /** Convite comum nao mexe em titularidade nenhuma. */
        @Test
        @DisplayName("convite EDITOR nao rebaixa o titular")
        void conviteEditorNaoRebaixa() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.accept("t");

            verify(petTutorRepository, never()).findByPetPetIdAndRole(any(), any());
        }

        /**
         * Token invalido, expirado, revogado, ja usado e destinado a outra pessoa
         * respondem igual: distinguir diria a quem tenta adivinhar qual parte errou.
         */
        @Test
        @DisplayName("convite de outro e-mail responde igual a token inexistente")
        void emailDivergenteRespondeIgual() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("outra@petfy.com.br", PetTutorRole.EDITOR, "t")));

            assertThatThrownBy(() -> petTutorService.accept("t"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                            HttpStatus.NOT_FOUND);

            verify(petTutorRepository, never()).save(any());
        }

        @Test
        @DisplayName("convite expirado nao cria vinculo")
        void conviteExpiradoNaoCriaVinculo() {
            var expirado = convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t");
            expirado.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t"))).thenReturn(Optional.of(expirado));

            assertThatThrownBy(() -> petTutorService.accept("t"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode());

            verify(petTutorRepository, never()).save(any());
        }

        @Test
        @DisplayName("o e-mail casa sem diferenciar maiuscula de minuscula")
        void emailCasaIgnorandoCaixa() {
            autenticado(Owner.builder().ownerId(MARIA_ID).name("Maria").email("MARIA@petfy.com.br").build());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.VIEWER, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(petTutorService.accept("t").getRole()).isEqualTo(PetTutorRole.VIEWER);
        }

        @Test
        @DisplayName("quem ja e tutor do pet nao entra duas vezes")
        void jaETutorNaoEntraDuasVezes() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(true);

            assertThatThrownBy(() -> petTutorService.accept("t"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ALREADY_A_TUTOR.getCode(), HttpStatus.CONFLICT);

            verify(petTutorRepository, never()).save(any());
        }

        /** Aceitar nao passa pelo guard: quem aceita ainda nao alcanca o pet. */
        @Test
        @DisplayName("aceitar nao consulta o guard")
        void aceitarNaoConsultaOGuard() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convitePara("maria@petfy.com.br", PetTutorRole.EDITOR, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.accept("t");

            verifyNoInteractions(petAccessGuard);
        }
    }

    @Nested
    @DisplayName("transferHolder")
    class TransferHolder {

        @Test
        @DisplayName("rebaixa o titular antes de promover o destino")
        void rebaixaAntesDePromover() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);
            var destino = vinculoDe(maria(), PetTutorRole.VIEWER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));
            when(petTutorRepository.findByPetPetIdAndRole(PET_ID, PetTutorRole.HOLDER))
                    .thenReturn(Optional.of(titularAtual));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.transferHolder(PET_ID, MARIA_ID);

            assertThat(result.getRole()).isEqualTo(PetTutorRole.HOLDER);
            assertThat(titularAtual.getRole()).isEqualTo(PetTutorRole.EDITOR);

            var ordem = inOrder(petTutorRepository);
            ordem.verify(petTutorRepository).save(titularAtual);
            ordem.verify(petTutorRepository).flush();
            ordem.verify(petTutorRepository).save(destino);
        }

        /**
         * O titular antigo vira EDITOR, e nao sai: quem cuidou do animal ate ontem
         * continua enxergando a carteira, e o novo titular decide se remove.
         */
        @Test
        @DisplayName("o titular antigo continua tutor, como EDITOR")
        void titularAntigoContinuaTutor() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);
            var destino = vinculoDe(maria(), PetTutorRole.VIEWER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));
            when(petTutorRepository.findByPetPetIdAndRole(PET_ID, PetTutorRole.HOLDER))
                    .thenReturn(Optional.of(titularAtual));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.transferHolder(PET_ID, MARIA_ID);

            assertThat(titularAtual.getRole()).isEqualTo(PetTutorRole.EDITOR);
            verify(petTutorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("transferir para quem ja e titular nao mexe em nada")
        void transferirParaOTitularEnoOp() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, OWNER_ID))
                    .thenReturn(Optional.of(titularAtual));

            var result = petTutorService.transferHolder(PET_ID, OWNER_ID);

            assertThat(result.getRole()).isEqualTo(PetTutorRole.HOLDER);
            verify(petTutorRepository, never()).save(any());
            verify(petTutorRepository, never()).flush();
        }

        @Test
        @DisplayName("transferir para quem nao e tutor responde 404 sem rebaixar ninguem")
        void transferirParaNaoTutor() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> petTutorService.transferHolder(PET_ID, MARIA_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);

            verify(petTutorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("troca o papel do co-tutor e carimba a atualizacao")
        void trocaOPapel() {
            var destino = vinculoDe(maria(), PetTutorRole.VIEWER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            var result = petTutorService.changeRole(PET_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.EDITOR).build());

            assertThat(result.getRole()).isEqualTo(PetTutorRole.EDITOR);
            assertThat(destino.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("papel HOLDER e recusado antes de tocar no vinculo")
        void holderERecusado() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());

            var request = PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.HOLDER).build();

            assertThatThrownBy(() -> petTutorService.changeRole(PET_ID, MARIA_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                            HttpStatus.CONFLICT);

            verify(petTutorRepository, never()).findByPetPetIdAndOwnerOwnerId(any(), any());
        }

        /**
         * Rebaixar o titular por aqui deixaria o pet sem nenhum, e o indice do banco
         * recusaria - mas a resposta certa nao e 500: e apontar a transferencia.
         */
        @Test
        @DisplayName("rebaixar o proprio titular aponta a transferencia, e nao quebra no banco")
        void rebaixarOTitularApontaATransferencia() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, OWNER_ID))
                    .thenReturn(Optional.of(titularAtual));

            var request = PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.VIEWER).build();

            assertThatThrownBy(() -> petTutorService.changeRole(PET_ID, OWNER_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                            HttpStatus.CONFLICT);

            assertThat(titularAtual.getRole()).isEqualTo(PetTutorRole.HOLDER);
            verify(petTutorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeTutor")
    class RemoveTutor {

        @Test
        @DisplayName("o titular remove um co-tutor")
        void titularRemoveCoTutor() {
            var destino = vinculoDe(maria(), PetTutorRole.EDITOR);

            autenticado(ulysses());
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));

            petTutorService.removeTutor(PET_ID, MARIA_ID);

            verify(petTutorRepository).delete(destino);
        }

        /**
         * Sair nao exige nivel: exigir autorizacao do titular para largar o pet
         * prenderia a pessoa a notificacoes de um animal que nao e dela.
         */
        @Test
        @DisplayName("co-tutor sai sozinho, e para isso basta ser tutor")
        void coTutorSaiSozinho() {
            var meuVinculo = vinculoDe(maria(), PetTutorRole.VIEWER);

            autenticado(maria());
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(meuVinculo));

            petTutorService.removeTutor(PET_ID, MARIA_ID);

            verify(petTutorRepository).delete(meuVinculo);
            verify(petAccessGuard, never()).requireTitular(any());
        }

        @Test
        @DisplayName("remover outra pessoa exige ser titular")
        void removerOutroExigeTitular() {
            var destino = vinculoDe(maria(), PetTutorRole.EDITOR);

            autenticado(ulysses());
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));

            petTutorService.removeTutor(PET_ID, MARIA_ID);

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Nem o proprio titular sai por aqui: o indice exige exatamente um HOLDER, e
         * um pet sem titular ficaria sem ninguem que pudesse convidar ou apaga-lo.
         */
        @Test
        @DisplayName("o titular nao se remove")
        void titularNaoSeRemove() {
            var meuVinculo = vinculoDe(ulysses(), PetTutorRole.HOLDER);

            autenticado(ulysses());
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, OWNER_ID))
                    .thenReturn(Optional.of(meuVinculo));

            assertThatThrownBy(() -> petTutorService.removeTutor(PET_ID, OWNER_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getCode(), HttpStatus.CONFLICT);

            verify(petTutorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("remover quem nao e tutor responde 404")
        void removerNaoTutor() {
            autenticado(ulysses());
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> petTutorService.removeTutor(PET_ID, MARIA_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);

            verify(petTutorRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("listagens e revogacao")
    class ListagensERevogacao {

        @Test
        @DisplayName("a lista de tutores devolve papel, nome e e-mail de cada um")
        void listaDeTutores() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(vinculoDe(ulysses(), PetTutorRole.HOLDER),
                            vinculoDe(maria(), PetTutorRole.VIEWER)));

            var result = petTutorService.listTutors(PET_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isHolder()).isTrue();
            assertThat(result.get(1).getOwnerEmail()).isEqualTo("maria@petfy.com.br");
            assertThat(result.get(1).isHolder()).isFalse();
        }

        @Test
        @DisplayName("a listagem de convites nao reexibe o token")
        void listagemDeConvitesNaoReexibeToken() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorInviteRepository.findByPetPetIdOrderByCreationDateDesc(PET_ID))
                    .thenReturn(List.of(PetTutorInvite.builder()
                            .petTutorInviteId(INVITE_ID)
                            .pet(pet())
                            .createdBy(ulysses())
                            .tokenHash("hash")
                            .email("maria@petfy.com.br")
                            .role(PetTutorRole.EDITOR)
                            .expiresAt(LocalDateTime.now().plusDays(5))
                            .creationDate(LocalDateTime.now())
                            .build()));

            assertThat(petTutorService.listInvites(PET_ID))
                    .singleElement()
                    .satisfies(invite -> {
                        assertThat(invite.getToken()).isNull();
                        assertThat(invite.isUsable()).isTrue();
                    });
        }

        @Test
        @DisplayName("revogar duas vezes nao mexe na data original")
        void revogarDuasVezesNaoMexeNaData() {
            var original = LocalDateTime.now().minusDays(1);
            var invite = PetTutorInvite.builder()
                    .petTutorInviteId(INVITE_ID)
                    .pet(pet())
                    .createdBy(ulysses())
                    .tokenHash("hash")
                    .email("maria@petfy.com.br")
                    .role(PetTutorRole.EDITOR)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .revokedAt(original)
                    .creationDate(LocalDateTime.now())
                    .build();

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite));

            petTutorService.revokeInvite(PET_ID, INVITE_ID);

            assertThat(invite.getRevokedAt()).isEqualTo(original);
            verify(petTutorInviteRepository, never()).save(any());
        }

        /**
         * O convite tem de ser daquele pet. Sem o filtro, o titular de um pet
         * revogaria convite de outro so por ter o id - e o 404 nao conta qual dos
         * dois motivos falhou.
         */
        @Test
        @DisplayName("convite de outro pet responde 404")
        void conviteDeOutroPetResponde404() {
            var deOutroPet = PetTutorInvite.builder()
                    .petTutorInviteId(INVITE_ID)
                    .pet(Pet.builder().petId(UUID.randomUUID()).name("Nina").build())
                    .createdBy(ulysses())
                    .tokenHash("hash")
                    .email("maria@petfy.com.br")
                    .role(PetTutorRole.EDITOR)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .creationDate(LocalDateTime.now())
                    .build();

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(deOutroPet));

            assertThatThrownBy(() -> petTutorService.revokeInvite(PET_ID, INVITE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                            HttpStatus.NOT_FOUND);
        }
    }

    /**
     * A tabela operacao -> nivel, no mesmo formato dos outros servicos desde a V15.
     * Pedir leitura onde precisa de titular nao quebra nenhum teste de comportamento
     * acima, mas deixa um co-tutor convidar gente para o pet de outro.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("convidar exige titular")
        void convidarExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.invite(PET_ID, PetTutorInviteRequestDTO.builder()
                    .email("maria@petfy.com.br").role(PetTutorRole.EDITOR).build());

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar convites exige titular")
        void listarConvitesExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorInviteRepository.findByPetPetIdOrderByCreationDateDesc(PET_ID)).thenReturn(List.of());

            petTutorService.listInvites(PET_ID);

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("revogar convite exige titular")
        void revogarConviteExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorInviteRepository.findById(INVITE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petTutorService.revokeInvite(PET_ID, INVITE_ID))
                    .isInstanceOf(PetfyHealthcareException.class);

            verify(petAccessGuard).requireTitular(PET_ID);
        }

        @Test
        @DisplayName("transferir titularidade exige titular")
        void transferirExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, OWNER_ID))
                    .thenReturn(Optional.of(vinculoDe(ulysses(), PetTutorRole.HOLDER)));

            petTutorService.transferHolder(PET_ID, OWNER_ID);

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
        }

        @Test
        @DisplayName("trocar papel exige titular")
        void trocarPapelExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(vinculoDe(maria(), PetTutorRole.VIEWER)));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.changeRole(PET_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.EDITOR).build());

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
        }

        /** Consultar quem cuida do pet nao avisa ninguem. */
        @Test
        @DisplayName("listar tutores nao gera aviso")
        void listarTutoresNaoGeraAviso() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID)).thenReturn(List.of());

            petTutorService.listTutors(PET_ID);

            verifyNoInteractions(petTutorActivityNotifier);
        }

        /** Ver quem alcanca o pet e leitura: o VIEWER tambem precisa saber. */
        @Test
        @DisplayName("listar tutores exige so leitura")
        void listarTutoresExigeSoLeitura() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID)).thenReturn(List.of());

            petTutorService.listTutors(PET_ID);

            verify(petAccessGuard).requireLeitura(PET_ID);
            verify(petAccessGuard, never()).requireTitular(any());
            verify(petAccessGuard, never()).requireEscrita(any());
        }
    }

    /**
     * Quem e avisado de cada mudanca.
     *
     * O que o notificador <b>diz</b> esta no PetTutorActivityNotifierTest; o que este
     * nested guarda e a lista de destinatarios, que e a parte que o servico decide -
     * e a unica forma de este recurso piorar a privacidade em vez de melhorar seria
     * mandar o aviso para o conjunto errado de pessoas.
     */
    @Nested
    @DisplayName("aviso aos tutores")
    class AvisoAosTutores {

        private final OpaqueTokenService tokens = new OpaqueTokenService();

        private PetTutorInvite convite(PetTutorRole papel, String token) {
            return PetTutorInvite.builder()
                    .petTutorInviteId(INVITE_ID)
                    .pet(pet())
                    .createdBy(ulysses())
                    .tokenHash(tokens.hash(token))
                    .email("maria@petfy.com.br")
                    .role(papel)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .creationDate(LocalDateTime.now())
                    .build();
        }

        /**
         * Os destinatarios vem da consulta, e nao de {@code Pet.getTutorOwners()}: a
         * colecao e lazy e acabou de ser mexida, entao ler dela daria uma lista que
         * pode nao refletir o vinculo que acabou de ser gravado.
         */
        @Test
        @DisplayName("aceitar convite avisa os tutores da consulta, nao a colecao do pet")
        void aceitarAvisaOsTutoresDaConsulta() {
            var joao = Owner.builder().ownerId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                    .name("Joao").email("joao@petfy.com.br").build();

            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convite(PetTutorRole.EDITOR, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));
            // a consulta traz os tres, incluindo o vinculo recem-criado
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(vinculoDe(ulysses(), PetTutorRole.HOLDER),
                            vinculoDe(joao, PetTutorRole.VIEWER),
                            vinculoDe(maria(), PetTutorRole.EDITOR)));

            petTutorService.accept("t");

            var destinatarios = ArgumentCaptor.forClass(List.class);
            verify(petTutorActivityNotifier).tutorEntrou(any(Pet.class), destinatarios.capture(),
                    any(Owner.class), any(PetTutorRole.class));

            assertThat(destinatarios.getValue()).extracting("ownerId")
                    .containsExactly(OWNER_ID, joao.getOwnerId(), MARIA_ID);
        }

        /**
         * Um aviso so: aceitar convite de HOLDER e entrar no pet e virar titular ao
         * mesmo tempo, e a mudanca de titularidade ja diz que ha gente nova cuidando
         * do animal. Dois e-mails para o mesmo fato viram ruido.
         */
        @Test
        @DisplayName("aceitar convite de HOLDER avisa a titularidade, e nao a entrada")
        void aceitarHolderAvisaSoATitularidade() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);

            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash("t")))
                    .thenReturn(Optional.of(convite(PetTutorRole.HOLDER, "t")));
            when(petTutorRepository.existsByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID)).thenReturn(false);
            when(petTutorRepository.findByPetPetIdAndRole(PET_ID, PetTutorRole.HOLDER))
                    .thenReturn(Optional.of(titularAtual));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(vinculoDe(maria(), PetTutorRole.HOLDER),
                            vinculoDe(ulysses(), PetTutorRole.EDITOR)));

            petTutorService.accept("t");

            var anterior = ArgumentCaptor.forClass(Owner.class);
            var novo = ArgumentCaptor.forClass(Owner.class);
            verify(petTutorActivityNotifier).titularidadeMudou(any(Pet.class), any(List.class),
                    anterior.capture(), novo.capture(), any(Owner.class));

            assertThat(anterior.getValue().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(novo.getValue().getOwnerId()).isEqualTo(MARIA_ID);

            verify(petTutorActivityNotifier, never())
                    .tutorEntrou(any(), any(), any(), any());
        }

        @Test
        @DisplayName("transferir avisa a titularidade, com quem agiu para ser excluido")
        void transferirAvisaATitularidade() {
            var titularAtual = vinculoDe(ulysses(), PetTutorRole.HOLDER);
            var destino = vinculoDe(maria(), PetTutorRole.VIEWER);

            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));
            when(petTutorRepository.findByPetPetIdAndRole(PET_ID, PetTutorRole.HOLDER))
                    .thenReturn(Optional.of(titularAtual));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(destino, titularAtual));

            petTutorService.transferHolder(PET_ID, MARIA_ID);

            var quemAgiu = ArgumentCaptor.forClass(Owner.class);
            verify(petTutorActivityNotifier).titularidadeMudou(any(Pet.class), any(List.class),
                    any(Owner.class), any(Owner.class), quemAgiu.capture());

            assertThat(quemAgiu.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        }

        /** Transferir para quem ja e titular nao muda nada, entao nao avisa nada. */
        @Test
        @DisplayName("transferencia que e no-op nao avisa ninguem")
        void transferenciaNoOpNaoAvisa() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, OWNER_ID))
                    .thenReturn(Optional.of(vinculoDe(ulysses(), PetTutorRole.HOLDER)));

            petTutorService.transferHolder(PET_ID, OWNER_ID);

            verifyNoInteractions(petTutorActivityNotifier);
        }

        /**
         * O flush entre o delete e a consulta e o que faz a lista chegar sem quem
         * saiu. Sem ele o delete fica pendente, a consulta ainda o traz, e o aviso
         * iria tambem para quem acabou de perder o acesso - contando-lhe que ele
         * mesmo saiu.
         */
        @Test
        @DisplayName("remover tutor descarrega o delete antes de montar a lista de avisos")
        void removerDescarregaAntesDeAvisar() {
            var destino = vinculoDe(maria(), PetTutorRole.EDITOR);

            autenticado(ulysses());
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(destino));
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(vinculoDe(ulysses(), PetTutorRole.HOLDER)));

            petTutorService.removeTutor(PET_ID, MARIA_ID);

            var ordem = inOrder(petTutorRepository, petTutorActivityNotifier);
            ordem.verify(petTutorRepository).delete(destino);
            ordem.verify(petTutorRepository).flush();
            ordem.verify(petTutorRepository).findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID);
            ordem.verify(petTutorActivityNotifier).tutorSaiu(any(Pet.class), any(List.class),
                    any(Owner.class), any(Owner.class));
        }

        /** Convidar nao avisa: ninguem entrou no pet ainda. */
        @Test
        @DisplayName("convidar nao avisa os tutores - ainda nao ha mudanca")
        void convidarNaoAvisa() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            autenticado(ulysses());
            when(ownerRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.invite(PET_ID, PetTutorInviteRequestDTO.builder()
                    .email("maria@petfy.com.br").role(PetTutorRole.EDITOR).build());

            verifyNoInteractions(petTutorActivityNotifier);
        }

        /** Trocar EDITOR por VIEWER nao muda quem alcanca o pet, so o que pode fazer. */
        @Test
        @DisplayName("trocar papel entre EDITOR e VIEWER nao avisa")
        void trocarPapelNaoAvisa() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet());
            when(petTutorRepository.findByPetPetIdAndOwnerOwnerId(PET_ID, MARIA_ID))
                    .thenReturn(Optional.of(vinculoDe(maria(), PetTutorRole.VIEWER)));
            when(petTutorRepository.save(any(PetTutor.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.changeRole(PET_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.EDITOR).build());

            verifyNoInteractions(petTutorActivityNotifier);
        }
    }

}
