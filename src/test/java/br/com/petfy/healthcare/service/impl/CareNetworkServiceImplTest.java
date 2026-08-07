package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.CareNetworkMemberDTO;
import br.com.petfy.healthcare.domain.dto.CareNetworkReach;
import br.com.petfy.healthcare.domain.dto.ContextKind;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("rede de quem cuida")
class CareNetworkServiceImplTest {

    private static final UUID ANIMAL_ID = UUID.randomUUID();

    @Mock private CustodyRepository custodyRepository;
    @Mock private GrantRepository grantRepository;
    @Mock private TimelineRepository timelineRepository;
    @Mock private AnimalAccessGuard animalAccessGuard;

    private CareNetworkServiceImpl service;

    private final Animal rex = Animal.builder().animalId(ANIMAL_ID).name("Rex").build();

    private final Person ulysses = Person.builder()
            .personId(UUID.randomUUID()).name("Ulysses").email("ulysses@petfy.com").build();

    private final Person marina = Person.builder()
            .personId(UUID.randomUUID()).name("Dra. Marina").email("marina@petfy.com").build();

    private final Organization creche = Organization.builder()
            .organizationId(UUID.randomUUID()).name("Creche Pata Legal").build();

    @BeforeEach
    void setUp() {
        service = new CareNetworkServiceImpl(
                custodyRepository, grantRepository, timelineRepository, animalAccessGuard);
    }

    private void semContribuicoes() {
        when(timelineRepository.ultimaContribuicaoPorPessoa(ANIMAL_ID)).thenReturn(List.of());
        when(timelineRepository.ultimaContribuicaoPorOrganizacao(ANIMAL_ID)).thenReturn(List.of());
    }

    private TimelineRepository.UltimaContribuicao contribuicao(UUID id, LocalDateTime em) {
        return new TimelineRepository.UltimaContribuicao() {
            public UUID getPessoaId() {
                return id;
            }

            public LocalDateTime getEm() {
                return em;
            }
        };
    }

    private Custody custodiaDe(Person pessoa) {
        return Custody.builder().custodyId(UUID.randomUUID()).animal(rex)
                .holderPerson(pessoa).startedAt(LocalDateTime.now().minusYears(2)).build();
    }

    private Grant concessaoPara(Person pessoa, LocalDateTime expiraEm, LocalDateTime revogadaEm) {
        return Grant.builder().grantId(UUID.randomUUID()).animal(rex)
                .granteePerson(pessoa).grantedBy(ulysses).level(GrantLevel.EDITOR)
                .scopes(new LinkedHashSet<>(Set.of(GrantScope.PRONTUARIO)))
                .grantedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(expiraEm).revokedAt(revogadaEm).build();
    }

    @Nested
    @DisplayName("quem entra")
    class QuemEntra {

        /**
         * Quem responde vem primeiro, sempre. Era o que a ordenacao por papel fazia antes de
         * o papel deixar de existir na Fase 6, e a tela depende disso: a 5.4 mostra a rede
         * como quem cuida, e quem responde e o comeco dela.
         */
        @Test
        @DisplayName("quem detem a custodia vem primeiro, e alcanca tudo")
        void custodiaVemPrimeiro() {
            semContribuicoes();
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses)));
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID))
                    .thenReturn(List.of(concessaoPara(marina, null, null)));

            var rede = service.doAnimal(ANIMAL_ID);

            assertThat(rede).hasSize(2);
            assertThat(rede.get(0)).satisfies(primeiro -> {
                assertThat(primeiro.getName()).isEqualTo("Ulysses");
                assertThat(primeiro.getReach()).isEqualTo(CareNetworkReach.CUSTODIA);
                assertThat(primeiro.isHolder()).isTrue();
                assertThat(primeiro.getScopes())
                        .as("escopo vazio na custodia nao significa nada: significa tudo")
                        .isEmpty();
            });
            assertThat(rede.get(1).getReach()).isEqualTo(CareNetworkReach.CONCESSAO);
        }

        /**
         * <b>O caso que a rota de tutores nao cobria.</b> Ela filtra {@code holderPerson},
         * entao um animal sob custodia de uma ONG ou de um abrigo aparecia <i>sem ninguem
         * respondendo por ele</i> - e a 3.4 admite custodia de organizacao desde o P2b.
         */
        @Test
        @DisplayName("custodia de organizacao entra, e nao fica sem ninguem respondendo")
        void custodiaDeOrganizacaoEntra() {
            semContribuicoes();
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(
                    Custody.builder().custodyId(UUID.randomUUID()).animal(rex)
                            .holderOrganization(creche)
                            .startedAt(LocalDateTime.now().minusMonths(3)).build()));
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of());

            assertThat(service.doAnimal(ANIMAL_ID)).singleElement().satisfies(membro -> {
                assertThat(membro.getKind()).isEqualTo(ContextKind.ORGANIZACAO);
                assertThat(membro.getOrganizationId()).isEqualTo(creche.getOrganizationId());
                assertThat(membro.getPersonId()).isNull();
                assertThat(membro.isHolder()).isTrue();
            });
        }

        /**
         * A pergunta da 5.4 e quem alcanca o animal <b>agora</b>. Mostrar acesso vencido como
         * se fosse ativo e o pior erro possivel numa tela cujo assunto e quem pode ver o que.
         */
        @Test
        @DisplayName("concessao revogada ou vencida nao aparece")
        void concessaoMortaNaoAparece() {
            semContribuicoes();
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of(
                    concessaoPara(marina, null, LocalDateTime.now().minusDays(1)),
                    concessaoPara(marina, LocalDateTime.now().minusHours(2), null),
                    concessaoPara(ulysses, LocalDateTime.now().plusDays(30), null)));

            assertThat(service.doAnimal(ANIMAL_ID))
                    .singleElement()
                    .satisfies(m -> assertThat(m.getName()).isEqualTo("Ulysses"));
        }

        /**
         * Link de compartilhamento e alcance anonimo, e nao alguem que cuida. Fica fora, e o
         * motivo tambem e de seguranca: o token dele e a credencial, e devolve-lo numa
         * leitura de tela entregaria a chave.
         */
        @Test
        @DisplayName("link de compartilhamento nao entra na rede")
        void linkNaoEntra() {
            semContribuicoes();
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of(
                    Grant.builder().grantId(UUID.randomUUID()).animal(rex)
                            .tokenHash("hash-do-link").grantedAt(LocalDateTime.now()).build()));

            assertThat(service.doAnimal(ANIMAL_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ultima contribuicao")
    class UltimaContribuicao {

        /** E o que mostra que a rede esta viva: "registrou hoje, registrou ontem" (5.4). */
        @Test
        @DisplayName("deve trazer a ultima contribuicao de cada um, sem consultar por membro")
        void trazAUltimaContribuicao() {
            var ontem = LocalDateTime.now().minusDays(1);
            when(timelineRepository.ultimaContribuicaoPorPessoa(ANIMAL_ID))
                    .thenReturn(List.of(contribuicao(marina.getPersonId(), ontem)));
            when(timelineRepository.ultimaContribuicaoPorOrganizacao(ANIMAL_ID)).thenReturn(List.of());
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses)));
            when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID))
                    .thenReturn(List.of(concessaoPara(marina, null, null)));

            var rede = service.doAnimal(ANIMAL_ID);

            assertThat(rede).extracting(CareNetworkMemberDTO::getName, CareNetworkMemberDTO::getLastContributionAt)
                    .containsExactly(
                            // quem responde e nunca registrou nada: nulo e um fato sobre a
                            // rede, e nao um dado faltando
                            org.assertj.core.groups.Tuple.tuple("Ulysses", null),
                            org.assertj.core.groups.Tuple.tuple("Dra. Marina", ontem));

            // duas agregacoes, e nenhuma consulta por membro - seria o N+1 que a V29 tirou
            // da linha do tempo
            verify(timelineRepository).ultimaContribuicaoPorPessoa(ANIMAL_ID);
            verify(timelineRepository).ultimaContribuicaoPorOrganizacao(ANIMAL_ID);
            verify(timelineRepository, never()).findDoAnimal(any(), any());
        }
    }

    /**
     * <b>A 5.4 e explicita:</b> "o que aparece aqui e quem tem alcance de fato - custodia e
     * acesso -, nunca uma lista de contatos". A rota de tutores devolvia {@code personEmail};
     * esta nao pode, e o teste trava isso na forma do DTO em vez de confiar no mapeamento.
     */
    @Test
    @DisplayName("nao expoe e-mail nem telefone: alcance nao e agenda")
    void naoExpoeContato() {
        assertThat(CareNetworkMemberDTO.class.getDeclaredFields())
                .extracting(Field::getName)
                .contains("name", "lastContributionAt")
                .doesNotContain("personEmail", "email", "personPhone", "phone", "address");
    }

    /** Saber quem mais cuida nao e privilegio de quem responde pelo animal. */
    @Test
    @DisplayName("exige apenas leitura, e nao escrita")
    void exigeApenasLeitura() {
        semContribuicoes();
        when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
        when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of());

        service.doAnimal(ANIMAL_ID);

        verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
        verify(animalAccessGuard, never()).requireEscrita(any());
    }

}
