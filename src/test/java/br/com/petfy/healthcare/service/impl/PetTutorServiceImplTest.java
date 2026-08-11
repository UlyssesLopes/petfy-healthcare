package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.PetTutorActivityNotifier;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Convite, acesso concedido e troca de quem responde pelo animal.
 *
 * <b>Reescrito no P2b, e nao adaptado.</b> A versao anterior era organizada por
 * papel - HOLDER, EDITOR, VIEWER -, porque os tres moravam na mesma tabela. Agora
 * sao duas coisas diferentes, e a diferenca e o que este teste passou a proteger:
 * convidar alguem como EDITOR e <b>conceder acesso</b>; convidar como HOLDER e
 * <b>transferir a responsabilidade</b>. Eram o mesmo insert antes.
 *
 * O que sobreviveu inteiro e a familia de casos sobre <b>ordem</b>: encerrar a
 * custodia antes de abrir a seguinte. O indice unico parcial trocou de tabela, nao
 * de natureza - e mock nao tem indice, entao a garantia de verdade esta no
 * PetTutorFlowContainerTest e aqui fica a sequencia de comandos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PetTutorServiceImplTest {

    @Mock
    private CustodyRepository custodyRepository;

    @Mock
    private GrantRepository grantRepository;

    @Mock
    private PetTutorInviteRepository petTutorInviteRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private AnimalAccessGuard animalAccessGuard;

    @Mock
    private PetTutorActivityNotifier petTutorActivityNotifier;

    private PetTutorServiceImpl petTutorService;

    private final OpaqueTokenService tokens = new OpaqueTokenService();

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MARIA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INVITE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        petTutorService = new PetTutorServiceImpl(custodyRepository, grantRepository,
                petTutorInviteRepository, personRepository, currentPersonProvider,
                animalAccessGuard, tokens, petTutorActivityNotifier);
        ReflectionTestUtils.setField(petTutorService, "defaultExpirationDays", 7);
    }

    private Person ulysses() {
        return Person.builder().personId(OWNER_ID).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Person maria() {
        return Person.builder().personId(MARIA_ID).name("Maria").email("maria@petfy.com.br").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA).build();
    }

    private void autenticado(Person person) {
        when(currentPersonProvider.require()).thenReturn(person);
    }

    private Custody custodiaDe(Person person) {
        Custody custodia = Custodias.emCurso(person);
        custodia.setAnimal(animal());
        return custodia;
    }

    private Grant concessaoPara(Person person, GrantLevel nivel) {
        return Grant.builder()
                .grantId(UUID.randomUUID())
                .animal(animal())
                .granteePerson(person)
                .level(nivel)
                .grantedAt(LocalDateTime.now())
                .build();
    }

    private PetTutorInvite convite(String email, PetTutorRole papel, String token, LocalDateTime expira) {
        return PetTutorInvite.builder()
                .petTutorInviteId(INVITE_ID)
                .animal(animal())
                .createdBy(ulysses())
                .tokenHash(tokens.hash(token))
                .email(email)
                .role(papel)
                .expiresAt(expira)
                .creationDate(LocalDateTime.now())
                .build();
    }

    private void devolveOQueSalva() {
        when(grantRepository.save(any(Grant.class))).thenAnswer(i -> i.getArgument(0));
        when(custodyRepository.saveAndFlush(any(Custody.class))).thenAnswer(i -> i.getArgument(0));
        when(custodyRepository.save(any(Custody.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void naoAlcanca(UUID personId) {
        when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, personId)).thenReturn(Optional.empty());
        when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(personId), any()))
                .thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("invite")
    class Invite {

        private PetTutorInviteRequestDTO paraMaria(PetTutorRole papel) {
            return PetTutorInviteRequestDTO.builder().email("maria@petfy.com.br").role(papel).build();
        }

        private void responsavelConvidando() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(personRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("devolve o token uma unica vez e guarda apenas o hash")
        void devolveTokenEGuardaHash() {
            responsavelConvidando();

            var resposta = petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.EDITOR));

            assertThat(resposta.getToken()).isNotBlank();

            var captor = ArgumentCaptor.forClass(PetTutorInvite.class);
            verify(petTutorInviteRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash())
                    .isNotBlank()
                    .isNotEqualTo(resposta.getToken());
        }

        @Test
        @DisplayName("convite com papel HOLDER se anuncia como transferencia")
        void conviteHolderSeAnuncia() {
            responsavelConvidando();

            assertThat(petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.HOLDER)).isTransfersHolder())
                    .isTrue();
        }

        @Test
        @DisplayName("usa a validade padrao quando o request nao informa")
        void usaValidadePadrao() {
            responsavelConvidando();

            var resposta = petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.EDITOR));

            assertThat(resposta.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
            assertThat(resposta.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
        }

        /**
         * Convidar quem ja alcanca o animal nao e engano a ser silenciado: a pessoa
         * levaria o erro no aceite, no lugar de quem convidou.
         */
        @Test
        @DisplayName("convidar quem ja responde pelo animal responde 409 sem gravar convite")
        void naoConvidaQuemJaResponde() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(personRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.of(maria()));
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID))
                    .thenReturn(Optional.of(custodiaDe(maria())));

            assertThatThrownBy(() -> petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.EDITOR)))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(petTutorInviteRepository, never()).save(any());
        }

        @Test
        @DisplayName("convidar quem ja tem concessao responde 409 sem gravar convite")
        void naoConvidaQuemJaTemConcessao() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(personRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.of(maria()));
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.VIEWER)));

            assertThatThrownBy(() -> petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.EDITOR)))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(petTutorInviteRepository, never()).save(any());
        }

        /** O caso comum: o conjuge que ainda nao tem conta. */
        @Test
        @DisplayName("convida quem ainda nao tem conta")
        void convidaQuemNaoTemConta() {
            responsavelConvidando();

            assertThat(petTutorService.invite(ANIMAL_ID, paraMaria(PetTutorRole.EDITOR)).getToken())
                    .isNotBlank();
        }
    }

    @Nested
    @DisplayName("accept")
    class Accept {

        private static final String TOKEN = "token-de-teste";

        private void conviteNaBase(PetTutorRole papel, LocalDateTime expira) {
            when(petTutorInviteRepository.findByTokenHash(tokens.hash(TOKEN)))
                    .thenReturn(Optional.of(convite("maria@petfy.com.br", papel, TOKEN, expira)));
        }

        private void conviteValido(PetTutorRole papel) {
            conviteNaBase(papel, LocalDateTime.now().plusDays(3));
        }

        /**
         * Convite EDITOR cria <b>concessao</b>, e nao custodia. E a distincao que o P2
         * existe para criar: quem recebeu acesso nao passa a responder pelo animal.
         */
        @Test
        @DisplayName("convite EDITOR cria concessao, e nao custodia")
        void conviteEditorCriaConcessao() {
            autenticado(maria());
            conviteValido(PetTutorRole.EDITOR);
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            var resposta = petTutorService.accept(TOKEN);

            assertThat(resposta.getRelacao()).isEqualTo("EDITOR");
            assertThat(resposta.isHolder()).isFalse();

            var captor = ArgumentCaptor.forClass(Grant.class);
            verify(grantRepository).save(captor.capture());
            assertThat(captor.getValue().getGranteePerson().getPersonId()).isEqualTo(MARIA_ID);
            assertThat(captor.getValue().getGrantedBy().getPersonId()).isEqualTo(OWNER_ID);
            verify(custodyRepository, never()).saveAndFlush(any(Custody.class));
        }

        @Test
        @DisplayName("convite VIEWER cria concessao de leitura")
        void conviteViewerCriaConcessaoDeLeitura() {
            autenticado(maria());
            conviteValido(PetTutorRole.VIEWER);
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            assertThat(petTutorService.accept(TOKEN).getRelacao()).isEqualTo("VIEWER");
        }

        @Test
        @DisplayName("marca o convite como usado, com quem aceitou")
        void marcaConviteComoUsado() {
            autenticado(maria());
            var invite = convite("maria@petfy.com.br", PetTutorRole.EDITOR, TOKEN,
                    LocalDateTime.now().plusDays(3));
            when(petTutorInviteRepository.findByTokenHash(tokens.hash(TOKEN))).thenReturn(Optional.of(invite));
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            petTutorService.accept(TOKEN);

            assertThat(invite.getAcceptedAt()).isNotNull();
            assertThat(invite.getAcceptedBy().getPersonId()).isEqualTo(MARIA_ID);
            verify(petTutorInviteRepository).save(invite);
        }

        /**
         * A ordem e a regra: encerrar a custodia atual antes de abrir a seguinte. O
         * indice unico parcial admite no maximo uma em curso por animal, e abrir antes
         * de fechar deixa duas - o Postgres recusa, e mock nao recusa nada.
         */
        @Test
        @DisplayName("convite HOLDER encerra a custodia atual antes de abrir a nova")
        void conviteHolderEncerraAntesDeAbrir() {
            autenticado(maria());
            conviteValido(PetTutorRole.HOLDER);
            naoAlcanca(MARIA_ID);
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();

            petTutorService.accept(TOKEN);

            var captor = ArgumentCaptor.forClass(Custody.class);
            verify(custodyRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(captor.capture());

            var primeira = captor.getAllValues().get(0);
            var segunda = captor.getAllValues().get(1);

            assertThat(primeira.getEndedAt()).isNotNull();
            assertThat(primeira.getEndReason()).isEqualTo(CustodyEndReason.TRANSFERENCIA);
            assertThat(segunda.getHolderPerson().getPersonId()).isEqualTo(MARIA_ID);
            assertThat(segunda.getNature()).isEqualTo(CustodyNature.DEFINITIVA);
        }

        /**
         * Quem respondia continua enxergando a carteira, agora por concessao: nao perde
         * o acesso por ter transferido.
         */
        /**
         * <b>LEITURA, e nao EDITOR.</b> Ate a rodada da Tela 11 o titular anterior saia com
         * escrita, e o desenho dizia o contrario: quem passa a responder pelo animal e quem
         * decide sobre ele. Ver tambem {@code revogarAcessosHerdados} — este caso guarda o nivel,
         * e o de baixo guarda a revogacao.
         */
        @Test
        @DisplayName("convite HOLDER deixa o responsavel anterior com LEITURA")
        void conviteHolderDeixaAnteriorComoLeitor() {
            autenticado(maria());
            conviteValido(PetTutorRole.HOLDER);
            naoAlcanca(MARIA_ID);
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();

            petTutorService.accept(TOKEN);

            var captor = ArgumentCaptor.forClass(Grant.class);
            verify(grantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anySatisfy(g -> {
                assertThat(g.getGranteePerson().getPersonId()).isEqualTo(OWNER_ID);
                assertThat(g.getLevel()).isEqualTo(GrantLevel.VIEWER);
                assertThat(g.getRevokedAt()).isNull();
            });
        }

        /**
         * <b>Acessos nao sao herdados.</b> Quem autorizou a clinica a ver o prontuario foi o
         * titular ANTERIOR, e quem passa a responder nao herda as decisoes dele.
         */
        @Test
        @DisplayName("o aceite de HOLDER revoga as concessoes que existiam no animal")
        void conviteHolderRevogaOsAcessosHerdados() {
            autenticado(maria());
            conviteValido(PetTutorRole.HOLDER);
            naoAlcanca(MARIA_ID);
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();

            Grant daClinica = Grant.builder()
                    .grantId(UUID.randomUUID()).animal(animal()).level(GrantLevel.EDITOR).build();
            when(grantRepository.findTodasVigentesNoAnimal(eq(ANIMAL_ID), any()))
                    .thenReturn(List.of(daClinica));

            petTutorService.accept(TOKEN);

            assertThat(daClinica.getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("convite EDITOR nao encerra custodia nenhuma")
        void conviteEditorNaoEncerraCustodia() {
            autenticado(maria());
            conviteValido(PetTutorRole.EDITOR);
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            petTutorService.accept(TOKEN);

            verify(custodyRepository, never()).saveAndFlush(any(Custody.class));
        }

        @Test
        @DisplayName("convite de outro e-mail responde igual a token inexistente")
        void emailDivergenteRespondeIgual() {
            autenticado(Person.builder().personId(UUID.randomUUID())
                    .email("estranho@petfy.com.br").build());
            conviteValido(PetTutorRole.EDITOR);

            assertThatThrownBy(() -> petTutorService.accept(TOKEN))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("convite expirado nao cria nada")
        void conviteExpiradoNaoCriaNada() {
            autenticado(maria());
            conviteNaBase(PetTutorRole.EDITOR, LocalDateTime.now().minusDays(1));

            assertThatThrownBy(() -> petTutorService.accept(TOKEN))
                    .isInstanceOf(PetfyHealthcareException.class);

            verify(grantRepository, never()).save(any());
            verify(custodyRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("o e-mail casa sem diferenciar maiuscula de minuscula")
        void emailCasaIgnorandoCaixa() {
            autenticado(Person.builder().personId(MARIA_ID).email("Maria@Petfy.Com.Br").build());
            conviteValido(PetTutorRole.EDITOR);
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            assertThat(petTutorService.accept(TOKEN)).isNotNull();
        }

        @Test
        @DisplayName("quem ja alcanca o animal nao entra duas vezes")
        void jaAlcancaNaoEntraDuasVezes() {
            autenticado(maria());
            conviteValido(PetTutorRole.EDITOR);
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.VIEWER)));

            assertThatThrownBy(() -> petTutorService.accept(TOKEN))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(grantRepository, never()).save(any());
        }

        /**
         * Quem aceita ainda nao alcanca o animal, entao pedir o guard aqui seria pedir o
         * que a pessoa nao tem. O token e a credencial deste fluxo.
         */
        @Test
        @DisplayName("aceitar nao consulta o guard")
        void aceitarNaoConsultaOGuard() {
            autenticado(maria());
            conviteValido(PetTutorRole.EDITOR);
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();

            petTutorService.accept(TOKEN);

            verifyNoInteractions(animalAccessGuard);
        }
    }

    @Nested
    @DisplayName("transferHolder")
    class TransferHolder {

        private void responsavelTransferindo() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.EDITOR)));
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();
        }

        @Test
        @DisplayName("encerra a custodia atual antes de abrir a do destino")
        void encerraAntesDeAbrir() {
            responsavelTransferindo();

            petTutorService.transferHolder(ANIMAL_ID, MARIA_ID);

            var captor = ArgumentCaptor.forClass(Custody.class);
            verify(custodyRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(captor.capture());

            assertThat(captor.getAllValues().get(0).getEndedAt()).isNotNull();
            assertThat(captor.getAllValues().get(1).getHolderPerson().getPersonId()).isEqualTo(MARIA_ID);
        }

        /**
         * Quem passa a responder nao precisa mais de concessao, e manter as duas deixaria
         * a mesma pessoa alcancando o animal por dois caminhos - o dia em que divergissem
         * seria um vazamento.
         */
        @Test
        @DisplayName("a concessao do destino e revogada quando ele passa a responder")
        void concessaoDoDestinoERevogada() {
            responsavelTransferindo();

            petTutorService.transferHolder(ANIMAL_ID, MARIA_ID);

            var captor = ArgumentCaptor.forClass(Grant.class);
            verify(grantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anySatisfy(g -> {
                assertThat(g.getGranteePerson().getPersonId()).isEqualTo(MARIA_ID);
                assertThat(g.getRevokedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("o responsavel antigo continua alcancando, como LEITOR")
        void responsavelAntigoContinuaAlcancando() {
            responsavelTransferindo();

            petTutorService.transferHolder(ANIMAL_ID, MARIA_ID);

            var captor = ArgumentCaptor.forClass(Grant.class);
            verify(grantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues()).anySatisfy(g -> {
                assertThat(g.getGranteePerson().getPersonId()).isEqualTo(OWNER_ID);
                assertThat(g.getLevel()).isEqualTo(GrantLevel.VIEWER);
                assertThat(g.getRevokedAt()).isNull();
            });
        }

        /** O estado final pedido e o estado atual: no-op, e nao erro. */
        @Test
        @DisplayName("transferir para quem ja responde nao mexe em nada")
        void transferirParaOResponsavelEnoOp() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID))
                    .thenReturn(Optional.of(custodiaDe(maria())));

            assertThat(petTutorService.transferHolder(ANIMAL_ID, MARIA_ID).isHolder()).isTrue();

            verify(custodyRepository, never()).saveAndFlush(any(Custody.class));
            verifyNoInteractions(petTutorActivityNotifier);
        }

        @Test
        @DisplayName("transferir para quem nao alcanca responde 404 sem encerrar nada")
        void transferirParaQuemNaoAlcanca() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            naoAlcanca(MARIA_ID);

            assertThatThrownBy(() -> petTutorService.transferHolder(ANIMAL_ID, MARIA_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(custodyRepository, never()).saveAndFlush(any(Custody.class));
        }
    }

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("troca o nivel da concessao")
        void trocaONivel() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.EDITOR)));
            devolveOQueSalva();

            var resposta = petTutorService.changeRole(ANIMAL_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.VIEWER).build());

            assertThat(resposta.getRelacao()).isEqualTo("VIEWER");
        }

        @Test
        @DisplayName("papel HOLDER e recusado antes de tocar na concessao")
        void holderERecusado() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());

            assertThatThrownBy(() -> petTutorService.changeRole(ANIMAL_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.HOLDER).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(grantRepository, never()).save(any());
        }

        /**
         * Quem responde pelo animal nao tem nivel a trocar - ele nao alcanca por
         * concessao. A resposta certa nao e 404: e apontar a porta da transferencia.
         */
        @Test
        @DisplayName("trocar o nivel de quem responde aponta a transferencia")
        void trocarNivelDeQuemRespondeApontaATransferencia() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID))
                    .thenReturn(Optional.of(custodiaDe(maria())));

            assertThatThrownBy(() -> petTutorService.changeRole(ANIMAL_ID, MARIA_ID,
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.VIEWER).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(grantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeTutor")
    class RemoveTutor {

        @Test
        @DisplayName("quem responde pelo animal revoga a concessao de outra pessoa")
        void responsavelRevogaConcessao() {
            autenticado(ulysses());
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            var concessao = concessaoPara(maria(), GrantLevel.EDITOR);
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessao));
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.removeTutor(ANIMAL_ID, MARIA_ID);

            assertThat(concessao.getRevokedAt()).isNotNull();
            verify(grantRepository).save(concessao);
        }

        /**
         * Sair nao exige nivel nenhum: exigir autorizacao para sair prenderia a pessoa a
         * notificacoes de um animal que nao e dela.
         */
        @Test
        @DisplayName("quem tem concessao revoga a propria, e para isso basta alcancar o animal")
        void saiSozinho() {
            autenticado(maria());
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.EDITOR)));
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.removeTutor(ANIMAL_ID, MARIA_ID);

            verify(animalAccessGuard, never()).requireCustodia(any());
        }

        @Test
        @DisplayName("remover outra pessoa exige responder pelo animal")
        void removerOutroExigeCustodia() {
            autenticado(ulysses());
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenThrow(new PetfyHealthcareException(
                    "nao", 403, HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> petTutorService.removeTutor(ANIMAL_ID, MARIA_ID))
                    .isInstanceOf(PetfyHealthcareException.class);

            verify(grantRepository, never()).save(any());
        }

        /**
         * Quem responde pelo animal nao sai por aqui, e agora por uma razao mais forte
         * que o indice do banco: sair sem sucessor deixaria o animal sem ninguem que
         * responda por ele - o quarto invariante do produto.
         */
        @Test
        @DisplayName("quem responde pelo animal nao se remove")
        void responsavelNaoSeRemove() {
            autenticado(ulysses());
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, OWNER_ID))
                    .thenReturn(Optional.of(custodiaDe(ulysses())));

            assertThatThrownBy(() -> petTutorService.removeTutor(ANIMAL_ID, OWNER_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(grantRepository, never()).save(any());
        }

        @Test
        @DisplayName("remover quem nao alcanca o animal responde 404")
        void removerQuemNaoAlcanca() {
            autenticado(ulysses());
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            naoAlcanca(MARIA_ID);

            assertThatThrownBy(() -> petTutorService.removeTutor(ANIMAL_ID, MARIA_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listagens e revogacao")
    class Listagens {

        /**
         * Quem responde pelo animal vem primeiro, sempre. Era o que a ordenacao por papel
         * fazia, e a tela depende disso para mostrar o responsavel no topo.
         */
        @Test
        @DisplayName("a lista traz quem responde primeiro, e depois quem tem concessao")
        void listaTrazResponsavelPrimeiro() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any()))
                    .thenReturn(List.of(concessaoPara(maria(), GrantLevel.VIEWER)));

            assertThat(petTutorService.listTutors(ANIMAL_ID))
                    .extracting("relacao", "personName")
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("CUSTODIA", "Ulysses"),
                            org.assertj.core.groups.Tuple.tuple("VIEWER", "Maria"));
        }

        @Test
        @DisplayName("a listagem de convites nao reexibe o token")
        void listagemDeConvitesNaoReexibeToken() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(convite("maria@petfy.com.br", PetTutorRole.EDITOR,
                            "token", LocalDateTime.now().plusDays(3))));

            assertThat(petTutorService.listInvites(ANIMAL_ID))
                    .singleElement()
                    .satisfies(i -> assertThat(i.getToken()).isNull());
        }

        @Test
        @DisplayName("revogar duas vezes nao mexe na data original")
        void revogarDuasVezesNaoMexeNaData() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            var invite = convite("maria@petfy.com.br", PetTutorRole.EDITOR, "token",
                    LocalDateTime.now().plusDays(3));
            when(petTutorInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite));

            petTutorService.revokeInvite(ANIMAL_ID, INVITE_ID);
            var primeira = invite.getRevokedAt();
            petTutorService.revokeInvite(ANIMAL_ID, INVITE_ID);

            assertThat(invite.getRevokedAt()).isEqualTo(primeira);
            verify(petTutorInviteRepository, org.mockito.Mockito.times(1)).save(invite);
        }

        @Test
        @DisplayName("convite de outro animal responde 404")
        void conviteDeOutroAnimalResponde404() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            var deOutro = convite("maria@petfy.com.br", PetTutorRole.EDITOR, "token",
                    LocalDateTime.now().plusDays(3));
            deOutro.setAnimal(Animal.builder().animalId(UUID.randomUUID()).build());
            when(petTutorInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(deOutro));

            assertThatThrownBy(() -> petTutorService.revokeInvite(ANIMAL_ID, INVITE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * A tabela "operacao no nivel exigido", que e o que sobrou sob responsabilidade
     * deste servico depois que a autorizacao virou peca unica.
     *
     * Pedir leitura onde precisa de custodia nao quebra nenhum teste de comportamento,
     * mas deixaria quem tem acesso concedido convidar um comparsa - e, no limite, tomar
     * o animal de quem responde por ele. Estes casos existem para pegar exatamente isso.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("convidar exige responder pelo animal")
        void convidarExigeCustodia() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(personRepository.findByEmail("maria@petfy.com.br")).thenReturn(Optional.empty());
            when(petTutorInviteRepository.save(any(PetTutorInvite.class))).thenAnswer(i -> i.getArgument(0));

            petTutorService.invite(ANIMAL_ID, PetTutorInviteRequestDTO.builder()
                    .email("maria@petfy.com.br").role(PetTutorRole.EDITOR).build());

            verify(animalAccessGuard).requireCustodia(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
            verify(animalAccessGuard, never()).requireEscrita(any());
        }

        @Test
        @DisplayName("listar convites exige responder pelo animal")
        void listarConvitesExigeCustodia() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of());

            petTutorService.listInvites(ANIMAL_ID);

            verify(animalAccessGuard).requireCustodia(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("listar quem cuida exige so leitura")
        void listarTutoresExigeSoLeitura() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.listTutors(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireCustodia(any());
        }

        @Test
        @DisplayName("listar quem cuida nao gera aviso")
        void listarTutoresNaoGeraAviso() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.listTutors(ANIMAL_ID);

            verifyNoInteractions(petTutorActivityNotifier);
        }
    }

    @Nested
    @DisplayName("aviso a quem cuida")
    class Aviso {

        private static final String TOKEN = "token-de-aviso";

        /**
         * Os destinatarios vem de consulta, e nao da colecao do animal: ela e lazy e
         * acabou de ser mexida, entao leria um conjunto que pode nao refletir o que foi
         * gravado - e o aviso iria para as pessoas erradas, que e o unico jeito de este
         * recurso piorar a privacidade em vez de melhorar.
         */
        @Test
        @DisplayName("aceitar convite avisa quem a consulta devolve")
        void aceitarAvisaQuemAConsultaDevolve() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash(TOKEN)))
                    .thenReturn(Optional.of(convite("maria@petfy.com.br", PetTutorRole.EDITOR, TOKEN,
                            LocalDateTime.now().plusDays(3))));
            naoAlcanca(MARIA_ID);
            devolveOQueSalva();
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any()))
                    .thenReturn(List.of(concessaoPara(maria(), GrantLevel.EDITOR)));

            petTutorService.accept(TOKEN);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Person>> captor = ArgumentCaptor.forClass(List.class);
            verify(petTutorActivityNotifier).tutorEntrou(any(), captor.capture(), any(), any());
            assertThat(captor.getValue()).hasSize(2);
        }

        /**
         * Um aviso so, e nao dois: aceitar convite de HOLDER e entrar no animal e passar
         * a responder por ele ao mesmo tempo, e a segunda ja diz que ha gente nova
         * cuidando do animal.
         */
        @Test
        @DisplayName("aceitar convite de HOLDER avisa a titularidade, e nao a entrada")
        void aceitarHolderAvisaSoATitularidade() {
            autenticado(maria());
            when(petTutorInviteRepository.findByTokenHash(tokens.hash(TOKEN)))
                    .thenReturn(Optional.of(convite("maria@petfy.com.br", PetTutorRole.HOLDER, TOKEN,
                            LocalDateTime.now().plusDays(3))));
            naoAlcanca(MARIA_ID);
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.accept(TOKEN);

            verify(petTutorActivityNotifier).titularidadeMudou(any(), any(), any(), any(), any());
            verify(petTutorActivityNotifier, never()).tutorEntrou(any(), any(), any(), any());
        }

        @Test
        @DisplayName("transferir avisa a titularidade")
        void transferirAvisaATitularidade() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.EDITOR)));
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            devolveOQueSalva();
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.transferHolder(ANIMAL_ID, MARIA_ID);

            verify(petTutorActivityNotifier).titularidadeMudou(any(), any(),
                    any(Person.class), any(Person.class), any(Person.class));
        }

        @Test
        @DisplayName("remover avisa quem ficou")
        void removerAvisa() {
            autenticado(ulysses());
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID)).thenReturn(Optional.empty());
            when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(MARIA_ID), any()))
                    .thenReturn(Optional.of(concessaoPara(maria(), GrantLevel.EDITOR)));
            when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(Optional.of(custodiaDe(ulysses())));
            when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());

            petTutorService.removeTutor(ANIMAL_ID, MARIA_ID);

            verify(petTutorActivityNotifier).tutorSaiu(any(), any(),
                    any(Person.class), any(Person.class));
        }

        /** No-op nao avisa: nada mudou. */
        @Test
        @DisplayName("transferencia que e no-op nao avisa ninguem")
        void transferenciaNoOpNaoAvisa() {
            when(animalAccessGuard.requireCustodia(ANIMAL_ID)).thenReturn(animal());
            autenticado(ulysses());
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, MARIA_ID))
                    .thenReturn(Optional.of(custodiaDe(maria())));

            petTutorService.transferHolder(ANIMAL_ID, MARIA_ID);

            verifyNoInteractions(petTutorActivityNotifier);
        }
    }

}
