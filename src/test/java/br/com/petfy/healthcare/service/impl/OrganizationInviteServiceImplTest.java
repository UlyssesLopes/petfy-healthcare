package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.OrganizationInvite;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationInviteRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
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
class OrganizationInviteServiceImplTest {

    @Mock
    private OrganizationInviteRepository organizationInviteRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private CurrentProfessionalProvider currentProfessionalProvider;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ProfessionalCredentialRepository professionalCredentialRepository;

    @Mock
    private br.com.petfy.healthcare.notification.InviteNotifier inviteNotifier;

    private OrganizationInviteServiceImpl service;

    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");
    private static final UUID INVITE_ID = UUID.fromString("bbbbbbbb-5555-5555-5555-555555555555");
    private static final UUID VET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        // token service real: o valor do teste esta em conferir que o token nao e
        // guardado em claro, e nao em repetir o hash num mock
        service = new OrganizationInviteServiceImpl(organizationInviteRepository, personRepository,
                membershipRepository, professionalCredentialRepository, currentProfessionalProvider,
                currentPersonProvider, new OpaqueTokenService(),
                new MembershipResponseFactory(professionalCredentialRepository), inviteNotifier);
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 7);
    }

    private Organization organization(UUID id) {
        return Organization.builder().organizationId(id).name("Clinica Bicho Feliz").build();
    }

    private Person vetDa(UUID organizationId) {
        return Person.builder().personId(VET_ID).name("Dra. Marina").build();
    }

    /** O contexto substituiu o campo organization da pessoa - ver ProfessionalContext. */
    private br.com.petfy.healthcare.security.ProfessionalContext contextoDa(UUID organizationId) {
        return br.com.petfy.healthcare.Contextos.por(vetDa(organizationId), organization(organizationId));
    }

    /**
     * <b>Convidar deixou de exigir credencial profissional.</b> A administradora do abrigo e a da
     * creche nao tem CRMV, e o `requireContext` as barrava com "an active professional credential
     * is required" num ato que nao tem nada a ver com credencial. Agora quem autoriza e o
     * VINCULO, e por isso o duble aqui e `organizacaoDeclarada` e nao mais `requireContext`.
     */
    private void autenticadoComoVetDa(UUID organizationId) {
        org.mockito.Mockito.lenient()
                .when(currentPersonProvider.require()).thenReturn(vetDa(organizationId));
        org.mockito.Mockito.lenient()
                .when(currentProfessionalProvider.require()).thenReturn(vetDa(organizationId));
        when(currentProfessionalProvider.organizacaoDeclarada(any()))
                .thenReturn(java.util.Optional.of(organization(organizationId)));
    }

    private OrganizationInvite convite(String email, LocalDateTime expiresAt,
                                 LocalDateTime acceptedAt, LocalDateTime revokedAt) {
        return OrganizationInvite.builder()
                .organizationInviteId(INVITE_ID)
                .organization(organization(CLINIC_ID))
                .createdBy(vetDa(CLINIC_ID))
                .tokenHash("hash")
                .email(email)
                .expiresAt(expiresAt)
                .acceptedAt(acceptedAt)
                .revokedAt(revokedAt)
                .creationDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    private OrganizationInvite conviteAberto() {
        return convite(null, LocalDateTime.now().plusDays(7), null, null);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve emitir o convite para a clinica de quem esta autenticado")
        void deveEmitirParaAClinicaDoEmissor() {
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.save(any(OrganizationInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(OrganizationInviteRequestDTO.builder().build());

            assertThat(result.getOrganizationId()).isEqualTo(CLINIC_ID);
            assertThat(result.getCreatedByVetName()).isEqualTo("Dra. Marina");
            assertThat(result.isUsable()).isTrue();
        }

        @Test
        @DisplayName("deve devolver o token apenas na criacao, e guardar so o hash")
        void deveDevolverTokenApenasNaCriacao() {
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.save(any(OrganizationInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(null);

            assertThat(result.getToken()).isNotBlank();

            var captor = ArgumentCaptor.forClass(OrganizationInvite.class);
            verify(organizationInviteRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isNotEqualTo(result.getToken());
        }

        @Test
        @DisplayName("a entidade de convite nao deve ter campo para o token em claro")
        void entidadeNaoDeveGuardarTokenEmClaro() {
            assertThat(OrganizationInvite.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("tokenHash")
                    .doesNotContain("token");
        }

        @Test
        @DisplayName("deve respeitar a validade informada")
        void deveRespeitarValidadeInformada() {
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.save(any(OrganizationInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(OrganizationInviteRequestDTO.builder().expiresInDays(2).build());

            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(3));
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        private void baseTem(OrganizationInvite invite) {
            when(organizationInviteRepository.findByTokenHash(any())).thenReturn(Optional.ofNullable(invite));
        }

        @Test
        @DisplayName("deve aceitar convite aberto e valido")
        void deveAceitarConviteAberto() {
            baseTem(conviteAberto());

            assertThat(service.validate("qualquer-token", "novo@vet.com.br").getOrganization().getOrganizationId())
                    .isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve recusar convite inexistente")
        void deveRecusarConviteInexistente() {
            baseTem(null);

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(111, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("deve recusar convite expirado")
        void deveRecusarConviteExpirado() {
            baseTem(convite(null, LocalDateTime.now().minusDays(1), null, null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("deve recusar convite revogado")
        void deveRecusarConviteRevogado() {
            baseTem(convite(null, LocalDateTime.now().plusDays(7), null, LocalDateTime.now()));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("deve recusar convite ja usado - e de uso unico")
        void deveRecusarConviteJaUsado() {
            baseTem(convite(null, LocalDateTime.now().plusDays(7), LocalDateTime.now().minusHours(1), null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("convite endereçado deve recusar email diferente, mesmo com o token certo")
        void conviteEnderecadoDeveRecusarOutroEmail() {
            baseTem(convite("convidada@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "estranho@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("convite endereçado deve aceitar o email destinatario, ignorando maiusculas")
        void conviteEnderecadoDeveAceitarODestinatario() {
            baseTem(convite("convidada@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThat(service.validate("qualquer-token", "Convidada@Vet.Com.Br")).isNotNull();
        }

        @Test
        @DisplayName("todos os motivos de recusa devem responder a mesma coisa")
        void todosOsMotivosDevemResponderIgual() {
            var mensagens = List.of(
                    capturaMensagem(null),
                    capturaMensagem(convite(null, LocalDateTime.now().minusDays(1), null, null)),
                    capturaMensagem(convite(null, LocalDateTime.now().plusDays(7), null, LocalDateTime.now())),
                    capturaMensagem(convite(null, LocalDateTime.now().plusDays(7), LocalDateTime.now(), null)),
                    capturaMensagem(convite("outra@vet.com.br", LocalDateTime.now().plusDays(7), null, null)));

            assertThat(mensagens).containsOnly("Invite not found or no longer valid");
        }

        private String capturaMensagem(OrganizationInvite invite) {
            when(organizationInviteRepository.findByTokenHash(any())).thenReturn(Optional.ofNullable(invite));
            try {
                service.validate("qualquer-token", "novo@vet.com.br");
                throw new AssertionError("deveria ter lancado PetfyHealthcareException");
            } catch (PetfyHealthcareException e) {
                return e.getMessage();
            }
        }
    }

    @Nested
    @DisplayName("markAccepted")
    class MarkAccepted {

        @Test
        @DisplayName("deve consumir o convite registrando quem aceitou")
        void deveConsumirRegistrandoQuemAceitou() {
            var invite = conviteAberto();
            var novoVet = vetDa(CLINIC_ID);
            when(personRepository.findById(VET_ID)).thenReturn(Optional.of(novoVet));

            service.markAccepted(invite, VET_ID);

            assertThat(invite.getAcceptedAt()).isNotNull();
            assertThat(invite.getAcceptedBy()).isEqualTo(novoVet);
            assertThat(invite.isUsable(LocalDateTime.now())).isFalse();
            verify(organizationInviteRepository).save(invite);
        }
    }

    @Nested
    @DisplayName("listFromMyOrganization e revoke")
    class ListarERevogar {

        @Test
        @DisplayName("deve listar apenas os convites da clinica do vet autenticado")
        void deveListarApenasDaPropriaClinica() {
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.findByOrganizationOrganizationIdOrderByCreationDateDesc(CLINIC_ID))
                    .thenReturn(List.of(conviteAberto()));

            var result = service.listFromMyOrganization();

            assertThat(result).singleElement()
                    .satisfies(i -> assertThat(i.getToken()).isNull());
        }

        @Test
        @DisplayName("deve revogar convite da propria clinica")
        void deveRevogarDaPropriaClinica() {
            var invite = conviteAberto();
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite));

            service.revoke(INVITE_ID);

            assertThat(invite.getRevokedAt()).isNotNull();
            verify(organizationInviteRepository).save(invite);
        }

        @Test
        @DisplayName("nao deve revogar convite de outra clinica")
        void naoDeveRevogarDeOutraClinica() {
            autenticadoComoVetDa(OUTRA_CLINIC_ID);
            when(organizationInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(conviteAberto()));

            assertThatThrownBy(() -> service.revoke(INVITE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(111);

            verify(organizationInviteRepository, never()).save(any());
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaData() {
            var original = LocalDateTime.now().minusDays(2);
            var jaRevogado = convite(null, LocalDateTime.now().plusDays(7), null, original);

            autenticadoComoVetDa(CLINIC_ID);
            when(organizationInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(jaRevogado));

            service.revoke(INVITE_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(organizationInviteRepository, never()).save(any());
        }
    }

    /**
     * O aceite de quem JA tem conta — o caminho que faltava.
     *
     * Ate aqui o unico aceite era o {@code inviteToken} na CRIACAO da conta, e a consequencia era
     * absurda: a veterinaria que ja usa o Petfy, convidada pela clinica, so entraria criando uma
     * segunda conta com outro e-mail.
     */
    @Nested
    @DisplayName("accept: quem ja tem conta entra na equipe")
    class Accept {

        private Person convidada;

        @BeforeEach
        void autenticada() {
            convidada = Person.builder().personId(VET_ID).name("Dra. Marina")
                    .email("marina@vet.com.br").build();
            org.mockito.Mockito.lenient().when(currentPersonProvider.require()).thenReturn(convidada);
            org.mockito.Mockito.lenient().when(personRepository.findById(VET_ID))
                    .thenReturn(Optional.of(convidada));
        }

        private void baseTem(OrganizationInvite invite) {
            when(organizationInviteRepository.findByTokenHash(any())).thenReturn(Optional.ofNullable(invite));
        }

        private void semVinculoAtivo() {
            when(membershipRepository.findAtivoDaPessoaNaOrganizacao(VET_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());
        }

        private OrganizationInvite conviteCom(MembershipRole funcao) {
            var invite = conviteAberto();
            invite.setRole(funcao);
            return invite;
        }

        @Test
        @DisplayName("deve criar o vinculo com a organizacao do convite e consumi-lo")
        void deveCriarVinculoEConsumir() {
            baseTem(conviteCom(MembershipRole.MONITOR));
            semVinculoAtivo();
            when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

            var vinculo = service.accept("token-na-mao");

            assertThat(vinculo.getOrganizationId()).isEqualTo(CLINIC_ID);
            assertThat(vinculo.getOrganizationName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(vinculo.getPersonId()).isEqualTo(VET_ID);
            assertThat(vinculo.getRole()).isEqualTo(MembershipRole.MONITOR);
            assertThat(vinculo.getJoinedAt()).isNotNull();

            verify(organizationInviteRepository).save(any(OrganizationInvite.class));
        }

        /** A funcao vem do convite, escrita por quem ja e da organizacao — nunca do request. */
        @Test
        @DisplayName("a funcao vem do convite, e nao de quem aceita")
        void funcaoVemDoConvite() {
            baseTem(conviteCom(MembershipRole.VOLUNTARIO));
            semVinculoAtivo();
            when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.accept("token-na-mao").getRole()).isEqualTo(MembershipRole.VOLUNTARIO);
        }

        /**
         * <b>A diferenca deliberada em relacao ao cadastro.</b> La, sem CRMV a deducao e
         * ADMINISTRADOR — mas la a pessoa esta CRIANDO a propria organizacao. Aqui ela esta
         * entrando numa que ja existe, e um convite que se esqueceu de dizer a funcao nao pode
         * ser lido como "entregue a administracao da clinica a esta pessoa".
         */
        @Test
        @DisplayName("convite antigo sem funcao nao entrega a administracao a ninguem")
        void conviteSemFuncaoNaoViraAdministrador() {
            baseTem(conviteAberto());
            semVinculoAtivo();
            when(professionalCredentialRepository.findByPersonPersonId(VET_ID)).thenReturn(List.of());
            when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.accept("token-na-mao").getRole()).isEqualTo(MembershipRole.VOLUNTARIO);
        }

        @Test
        @DisplayName("convite antigo sem funcao, com credencial, entra como VETERINARIO")
        void conviteSemFuncaoComCredencial() {
            baseTem(conviteAberto());
            semVinculoAtivo();
            when(professionalCredentialRepository.findByPersonPersonId(VET_ID))
                    .thenReturn(List.of(br.com.petfy.healthcare.domain.entity.ProfessionalCredential.builder()
                            .council("CRMV").uf("SP").number("12345").build()));
            when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.accept("token-na-mao").getRole()).isEqualTo(MembershipRole.VETERINARIO);
        }

        /**
         * <b>E nao consome o convite ao recusar.</b> Consumir primeiro gastaria um convite de uso
         * unico para em seguida recusar o aceite, e a pessoa ficaria sem vinculo e sem convite.
         */
        @Test
        @DisplayName("quem ja e membro ativo e recusado, e o convite continua de pe")
        void jaMembroERecusado() {
            baseTem(conviteCom(MembershipRole.VETERINARIO));
            when(membershipRepository.findAtivoDaPessoaNaOrganizacao(VET_ID, CLINIC_ID))
                    .thenReturn(Optional.of(Membership.builder()
                            .membershipId(UUID.randomUUID())
                            .person(convidada).organization(organization(CLINIC_ID))
                            .role(MembershipRole.VETERINARIO).joinedAt(LocalDateTime.now().minusYears(1))
                            .build()));

            assertThatThrownBy(() -> service.accept("token-na-mao"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(153, HttpStatus.CONFLICT);

            verify(membershipRepository, never()).save(any());
            verify(organizationInviteRepository, never()).save(any());
        }

        /**
         * Quem saiu pode voltar: o vinculo desligado tem {@code leftAt} e a busca de ativo nao o
         * encontra, entao nasce um vinculo novo — com a data de agora e a funcao de agora, que e
         * o que de fato aconteceu.
         */
        @Test
        @DisplayName("quem foi desligado pode ser convidado de novo")
        void desligadoPodeVoltar() {
            baseTem(conviteCom(MembershipRole.MONITOR));
            semVinculoAtivo();
            when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

            var vinculo = service.accept("token-na-mao");

            assertThat(vinculo.getRole()).isEqualTo(MembershipRole.MONITOR);
            assertThat(vinculo.getJoinedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        }

        @Test
        @DisplayName("convite enderecado a outra pessoa nao pode ser aceito por quem tem o link")
        void conviteDeOutraPessoa() {
            baseTem(convite("outra@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThatThrownBy(() -> service.accept("token-na-mao"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(111);

            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("preview le o convite sem consumir")
        void previewNaoConsome() {
            baseTem(conviteCom(MembershipRole.MONITOR));

            var previa = service.preview("token-na-mao");

            assertThat(previa.getOrganizationName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(previa.getRole()).isEqualTo(MembershipRole.MONITOR);
            assertThat(previa.getInvitedByName()).isEqualTo("Dra. Marina");
            assertThat(previa.getExpiresAt()).isNotNull();

            verify(organizationInviteRepository, never()).save(any());
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("preview recusa o mesmo que o aceite recusaria")
        void previewRecusaOQueOAceiteRecusaria() {
            baseTem(convite("outra@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThatThrownBy(() -> service.preview("token-na-mao"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(111);
        }
    }
}
