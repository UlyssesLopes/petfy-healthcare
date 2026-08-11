package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A equipe da organizacao — a metade da Tela 16 que nao tinha de onde sair.
 *
 * O contrato devolvia CONVITES e nada devolvia quem ja entrou, entao a tela dizia isso em vez de
 * desenhar uma tabela vazia: "vazio nao e a mesma coisa que nao existe".
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("a equipe: listar, ajustar funcao e desligar")
class OrganizationMemberServiceImplTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ProfessionalCredentialRepository professionalCredentialRepository;
    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private CurrentProfessionalProvider currentProfessionalProvider;

    private OrganizationMemberServiceImpl service;

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID OUTRA_ORG = UUID.randomUUID();

    private Person vera;
    private Organization creche;

    @BeforeEach
    void setUp() {
        service = new OrganizationMemberServiceImpl(membershipRepository,
                professionalCredentialRepository, currentPersonProvider, currentProfessionalProvider);

        vera = Person.builder().personId(UUID.randomUUID()).name("Vera Quintal")
                .email("vera@petfy.com.br").build();
        creche = Organization.builder().organizationId(ORG).name("Creche Quintal").build();

        lenient().when(currentPersonProvider.require()).thenReturn(vera);
        lenient().when(currentProfessionalProvider.organizacaoDeclarada(any()))
                .thenReturn(Optional.of(creche));
    }

    private Membership vinculo(Person pessoa, MembershipRole funcao, Organization organizacao) {
        return Membership.builder()
                .membershipId(UUID.randomUUID())
                .person(pessoa).organization(organizacao).role(funcao)
                .joinedAt(LocalDateTime.now().minusMonths(6))
                .build();
    }

    private void euSou(MembershipRole funcao) {
        lenient().when(membershipRepository.findAtivoDaPessoaNaOrganizacao(vera.getPersonId(), ORG))
                .thenReturn(Optional.of(vinculo(vera, funcao, creche)));
    }

    private Person outra(String nome) {
        return Person.builder().personId(UUID.randomUUID()).name(nome)
                .email(nome.toLowerCase() + "@petfy.com.br").build();
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("qualquer membro ve a equipe, com funcao e desde quando")
        void qualquerMembroVe() {
            euSou(MembershipRole.MONITOR);
            when(membershipRepository.findAtivosDaOrganizacao(ORG))
                    .thenReturn(List.of(vinculo(vera, MembershipRole.MONITOR, creche)));
            when(professionalCredentialRepository.findByPersonPersonId(vera.getPersonId()))
                    .thenReturn(List.of());

            var equipe = service.listMembers();

            assertThat(equipe).hasSize(1);
            assertThat(equipe.get(0).getPersonName()).isEqualTo("Vera Quintal");
            assertThat(equipe.get(0).getRole()).isEqualTo(MembershipRole.MONITOR);
            assertThat(equipe.get(0).getJoinedAt()).isNotNull();
        }

        /**
         * Funcao e credencial sao coisas diferentes, e a tabela do desenho mostra as duas: ser
         * VETERINARIO e o papel na organizacao, e o CRMV e o que o conselho registrou.
         */
        @Test
        @DisplayName("o registro profissional vem junto de quem tem, e e nulo de quem nao tem")
        void credencialDeQuemTem() {
            euSou(MembershipRole.ADMINISTRADOR);
            Person marina = outra("Marina");

            when(membershipRepository.findAtivosDaOrganizacao(ORG))
                    .thenReturn(List.of(vinculo(marina, MembershipRole.VETERINARIO, creche),
                            vinculo(vera, MembershipRole.MONITOR, creche)));
            when(professionalCredentialRepository.findByPersonPersonId(marina.getPersonId()))
                    .thenReturn(List.of(ProfessionalCredential.builder()
                            .council("CRMV").uf("SP").number("12345").build()));
            when(professionalCredentialRepository.findByPersonPersonId(vera.getPersonId()))
                    .thenReturn(List.of());

            var equipe = service.listMembers();

            assertThat(equipe).extracting("personName", "professionalCredential")
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple("Marina", "CRMV-SP 12345"),
                            org.assertj.core.groups.Tuple.tuple("Vera Quintal", null));
        }
    }

    @Nested
    @DisplayName("ajustar funcao")
    class Ajustar {

        @Test
        @DisplayName("monitor nao ajusta funcao de ninguem")
        void monitorNaoAjusta() {
            euSou(MembershipRole.MONITOR);

            assertThatThrownBy(() -> service.changeMemberRole(UUID.randomUUID(), MembershipRole.VETERINARIO))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .satisfies(e -> {
                        var erro = (PetfyHealthcareException) e;
                        assertThat(erro.getCode()).isEqualTo(151);
                        assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    });

            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("administrador ajusta")
        void administradorAjusta() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership alvo = vinculo(outra("Joana"), MembershipRole.VOLUNTARIO, creche);

            when(membershipRepository.findById(alvo.getMembershipId())).thenReturn(Optional.of(alvo));
            when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(professionalCredentialRepository.findByPersonPersonId(any())).thenReturn(List.of());

            var ajustado = service.changeMemberRole(alvo.getMembershipId(), MembershipRole.MONITOR);

            assertThat(ajustado.getRole()).isEqualTo(MembershipRole.MONITOR);
        }

        /**
         * Sem administrador ninguem convida, ajusta nem desliga: a organizacao vira um cadastro
         * que so o suporte destrava. Rebaixar a si mesmo e o caminho mais provavel de chegar la
         * sem perceber.
         */
        @Test
        @DisplayName("rebaixar o ultimo administrador e recusado")
        void ultimoAdministradorNaoRebaixa() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership eu = vinculo(vera, MembershipRole.ADMINISTRADOR, creche);

            when(membershipRepository.findById(eu.getMembershipId())).thenReturn(Optional.of(eu));
            when(membershipRepository.findAtivosDaOrganizacao(ORG))
                    .thenReturn(List.of(eu, vinculo(outra("Joana"), MembershipRole.MONITOR, creche)));

            assertThatThrownBy(() -> service.changeMemberRole(eu.getMembershipId(), MembershipRole.MONITOR))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .satisfies(e -> assertThat(((PetfyHealthcareException) e).getCode()).isEqualTo(152));

            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("havendo outro administrador, rebaixar passa")
        void comOutroAdministradorPassa() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership eu = vinculo(vera, MembershipRole.ADMINISTRADOR, creche);

            when(membershipRepository.findById(eu.getMembershipId())).thenReturn(Optional.of(eu));
            when(membershipRepository.findAtivosDaOrganizacao(ORG))
                    .thenReturn(List.of(eu, vinculo(outra("Joana"), MembershipRole.ADMINISTRADOR, creche)));
            when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(professionalCredentialRepository.findByPersonPersonId(any())).thenReturn(List.of());

            assertThatCode(() -> service.changeMemberRole(eu.getMembershipId(), MembershipRole.MONITOR))
                    .doesNotThrowAnyException();
        }

        /** 404 e nao 403: dizer "existe e voce nao pode" ja e informacao sobre outra organizacao. */
        @Test
        @DisplayName("vinculo de outra organizacao responde 404")
        void vinculoDeOutraOrganizacao() {
            euSou(MembershipRole.ADMINISTRADOR);
            Organization outraOrg = Organization.builder().organizationId(OUTRA_ORG).name("Outra").build();
            Membership alheio = vinculo(outra("Joana"), MembershipRole.MONITOR, outraOrg);

            when(membershipRepository.findById(alheio.getMembershipId())).thenReturn(Optional.of(alheio));

            assertThatThrownBy(() -> service.changeMemberRole(alheio.getMembershipId(), MembershipRole.VOLUNTARIO))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .satisfies(e -> {
                        var erro = (PetfyHealthcareException) e;
                        assertThat(erro.getCode()).isEqualTo(150);
                        assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("desligar")
    class Desligar {

        @Test
        @DisplayName("marca a saida em vez de apagar o vinculo")
        void marcaASaida() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership alvo = vinculo(outra("Joana"), MembershipRole.MONITOR, creche);

            when(membershipRepository.findById(alvo.getMembershipId())).thenReturn(Optional.of(alvo));

            service.removeMember(alvo.getMembershipId());

            assertThat(alvo.getLeftAt()).isNotNull();
            verify(membershipRepository).save(alvo);
            verify(membershipRepository, never()).delete(any());
        }

        @Test
        @DisplayName("desligar quem ja saiu nao e erro, e a primeira data e que vale")
        void idempotente() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership alvo = vinculo(outra("Joana"), MembershipRole.MONITOR, creche);
            LocalDateTime saidaOriginal = LocalDateTime.now().minusDays(30);
            alvo.setLeftAt(saidaOriginal);

            when(membershipRepository.findById(alvo.getMembershipId())).thenReturn(Optional.of(alvo));

            service.removeMember(alvo.getMembershipId());

            assertThat(alvo.getLeftAt()).isEqualTo(saidaOriginal);
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("desligar o ultimo administrador e recusado")
        void ultimoAdministradorNaoSai() {
            euSou(MembershipRole.ADMINISTRADOR);
            Membership eu = vinculo(vera, MembershipRole.ADMINISTRADOR, creche);

            when(membershipRepository.findById(eu.getMembershipId())).thenReturn(Optional.of(eu));
            when(membershipRepository.findAtivosDaOrganizacao(ORG)).thenReturn(List.of(eu));

            assertThatThrownBy(() -> service.removeMember(eu.getMembershipId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .satisfies(e -> assertThat(((PetfyHealthcareException) e).getCode()).isEqualTo(152));

            assertThat(eu.getLeftAt()).isNull();
        }
    }
}
