package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ContextKind;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("contexto ativo")
class ActiveContextServiceImplTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID CRECHE_ID = UUID.randomUUID();
    private static final UUID CLINICA_ID = UUID.randomUUID();

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private CurrentProfessionalProvider currentProfessionalProvider;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ProfessionalCredentialRepository credentialRepository;

    private ActiveContextServiceImpl service;

    private final Person pessoa = Person.builder()
            .personId(PERSON_ID).name("Dra. Marina").email("marina@petfy.com").build();

    @BeforeEach
    void setUp() {
        service = new ActiveContextServiceImpl(
                currentPersonProvider, currentProfessionalProvider, membershipRepository, credentialRepository);
        when(currentPersonProvider.require()).thenReturn(pessoa);
    }

    private Organization organizacao(UUID id, String nome, OrganizationCapability... capacidades) {
        return Organization.builder()
                .organizationId(id).name(nome)
                .capabilities(new LinkedHashSet<>(Set.of(capacidades)))
                .build();
    }

    private Membership vinculo(Organization organizacao, MembershipRole papel) {
        return Membership.builder()
                .membershipId(UUID.randomUUID()).person(pessoa).organization(organizacao).role(papel).build();
    }

    private void comCredencial(boolean tem) {
        when(credentialRepository.existsAtivaPorEmail(eq(pessoa.getEmail()), eq(CredentialStatus.SUSPENSO)))
                .thenReturn(tem);
    }

    private void semDeclaracao() {
        when(currentProfessionalProvider.organizacaoDeclarada(any(), any())).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("sem header declarado")
    class SemHeader {

        @Test
        @DisplayName("sem vinculo deve atuar por si, e isso e o autonomo e nao cadastro incompleto")
        void semVinculoAtuaPorSi() {
            semDeclaracao();
            comCredencial(true);
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of());

            var resposta = service.contextoAtivo(null);

            assertThat(resposta.isAmbiguous()).isFalse();
            assertThat(resposta.isProfessional()).isTrue();
            assertThat(resposta.getActive().getKind()).isEqualTo(ContextKind.PESSOA);
            assertThat(resposta.getActive().getOrganizationId()).isNull();
            assertThat(resposta.getAvailable()).singleElement()
                    .satisfies(o -> assertThat(o.getKind()).isEqualTo(ContextKind.PESSOA));
        }

        @Test
        @DisplayName("com um vinculo deve atuar por ele, sem precisar declarar")
        void comUmVinculoAtuaPorEle() {
            semDeclaracao();
            comCredencial(true);
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of(
                    vinculo(organizacao(CLINICA_ID, "Clinica Bicho Feliz",
                            OrganizationCapability.REGISTRAR_ATO_CLINICO), MembershipRole.VETERINARIO)));

            var resposta = service.contextoAtivo(null);

            assertThat(resposta.isAmbiguous()).isFalse();
            assertThat(resposta.getActive().getKind()).isEqualTo(ContextKind.ORGANIZACAO);
            assertThat(resposta.getActive().getOrganizationId()).isEqualTo(CLINICA_ID);
            assertThat(resposta.getActive().getRole()).isEqualTo(MembershipRole.VETERINARIO);
            assertThat(resposta.getActive().getCapabilities())
                    .containsExactly(OrganizationCapability.REGISTRAR_ATO_CLINICO);
        }

        /**
         * O caso que da 409 numa escrita. Aqui e campo, e nao erro: e a 9.5 dizendo que
         * a ambiguidade tem de virar decisao na entrada em vez de erro no meio.
         */
        @Test
        @DisplayName("com mais de um vinculo deve devolver ambiguo em vez de escolher um")
        void comDoisVinculosEAmbiguo() {
            semDeclaracao();
            comCredencial(true);
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of(
                    vinculo(organizacao(CLINICA_ID, "Clinica Bicho Feliz"), MembershipRole.VETERINARIO),
                    vinculo(organizacao(CRECHE_ID, "Creche Pata Legal"), MembershipRole.MONITOR)));

            var resposta = service.contextoAtivo(null);

            assertThat(resposta.isAmbiguous()).isTrue();
            assertThat(resposta.getActive()).isNull();
            assertThat(resposta.getAvailable())
                    .extracting(o -> o.getOrganizationId())
                    .containsExactly(CLINICA_ID, CRECHE_ID);
        }

        /**
         * A rota fica fora de /professional/** exatamente por isto: exigir credencial
         * aqui esconderia do monitor os proprios contextos, e ele age em nome da creche
         * sem nunca ter CRMV.
         */
        @Test
        @DisplayName("monitor sem credencial deve ver os contextos, marcado como nao profissional")
        void monitorSemCredencialVeOsContextos() {
            semDeclaracao();
            comCredencial(false);
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of(
                    vinculo(organizacao(CRECHE_ID, "Creche Pata Legal",
                            OrganizationCapability.REGISTRAR_OBSERVACAO), MembershipRole.MONITOR)));

            var resposta = service.contextoAtivo(null);

            assertThat(resposta.isProfessional()).isFalse();
            assertThat(resposta.getActive().getOrganizationId()).isEqualTo(CRECHE_ID);
            assertThat(resposta.getActive().getCapabilities())
                    .containsExactly(OrganizationCapability.REGISTRAR_OBSERVACAO);
        }
    }

    @Nested
    @DisplayName("com header declarado")
    class ComHeader {

        @Test
        @DisplayName("deve atuar pela organizacao declarada, e deixar de ser ambiguo")
        void declaradoResolveAAmbiguidade() {
            var creche = organizacao(CRECHE_ID, "Creche Pata Legal",
                    OrganizationCapability.GERIR_TURMA_E_VAGA);
            comCredencial(true);
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of(
                    vinculo(organizacao(CLINICA_ID, "Clinica Bicho Feliz"), MembershipRole.VETERINARIO),
                    vinculo(creche, MembershipRole.MONITOR)));
            when(currentProfessionalProvider.organizacaoDeclarada(eq(pessoa), eq(CRECHE_ID.toString())))
                    .thenReturn(Optional.of(creche));

            var resposta = service.contextoAtivo(CRECHE_ID.toString());

            assertThat(resposta.isAmbiguous()).isFalse();
            assertThat(resposta.getActive().getOrganizationId()).isEqualTo(CRECHE_ID);
            // o papel vem do vinculo que ja estava carregado, e nao de uma segunda consulta
            assertThat(resposta.getActive().getRole()).isEqualTo(MembershipRole.MONITOR);
            assertThat(resposta.getActive().getCapabilities())
                    .containsExactly(OrganizationCapability.GERIR_TURMA_E_VAGA);
        }

        /**
         * O 403 continua morando no provider, num lugar so. Este teste existe para
         * garantir que esta leitura nao o engole devolvendo "sem contexto" - declarar
         * vinculo que nao se tem e erro, e nao ausencia de escolha.
         */
        @Test
        @DisplayName("deve propagar o 403 quando a pessoa nao e membro da organizacao declarada")
        void naoMembroPropaga403() {
            var estranha = UUID.randomUUID().toString();
            when(membershipRepository.findAtivosDaPessoa(PERSON_ID)).thenReturn(List.of());
            when(currentProfessionalProvider.organizacaoDeclarada(eq(pessoa), eq(estranha)))
                    .thenThrow(new PetfyHealthcareException("nao e membro", 403, HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> service.contextoAtivo(estranha))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * A faixa "confirme seu e-mail" da moldura do produto.
     *
     * <b>Ela so tem tres desfechos, e dois deles sao errados:</b> aparecer sempre, mentindo para
     * quem ja confirmou; nao existir, calando quem nao recebe aviso nenhum e nao sabe por que; ou
     * este campo. O cabecalho ja le esta rota em toda tela, entao a informacao chega sem uma
     * segunda chamada.
     */
    @Nested
    @DisplayName("a faixa de e-mail nao confirmado")
    class EmailVerificado {

        private void euSou(Person quem) {
            when(currentPersonProvider.require()).thenReturn(quem);
            when(membershipRepository.findAtivosDaPessoa(quem.getPersonId())).thenReturn(List.of());
            when(currentProfessionalProvider.organizacaoDeclarada(any(), any())).thenReturn(Optional.empty());
            when(credentialRepository.existsAtivaPorEmail(eq(quem.getEmail()), eq(CredentialStatus.SUSPENSO)))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("quem nunca confirmou deve chegar marcado como nao verificado")
        void naoConfirmado() {
            euSou(Person.builder().personId(UUID.randomUUID()).name("Vera").email("vera@petfy.com").build());

            assertThat(service.contextoAtivo(null).isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("quem ja confirmou nao deve ver a faixa")
        void confirmado() {
            euSou(Person.builder().personId(UUID.randomUUID()).name("Vera").email("vera@petfy.com")
                    .emailVerifiedAt(java.time.LocalDateTime.now().minusDays(3)).build());

            assertThat(service.contextoAtivo(null).isEmailVerified()).isTrue();
        }

        /**
         * O campo espelha {@code podeReceberNotificacao()}, e nao uma segunda leitura do instante:
         * se a regra de "pode receber" mudar, a faixa muda com ela em vez de continuar dizendo o
         * que era verdade antes.
         */
        @Test
        @DisplayName("deve dizer o mesmo que a regra de quem pode receber notificacao")
        void espelhaARegraDeNotificacao() {
            var confirmada = Person.builder().personId(UUID.randomUUID()).name("Vera")
                    .email("vera@petfy.com").emailVerifiedAt(java.time.LocalDateTime.now()).build();
            euSou(confirmada);

            assertThat(service.contextoAtivo(null).isEmailVerified())
                    .isEqualTo(confirmada.podeReceberNotificacao());
        }
    }

}
