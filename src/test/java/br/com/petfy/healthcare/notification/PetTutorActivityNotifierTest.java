package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PetTutorActivityNotifierTest {

    @Mock
    private AsyncNotificationDispatcher dispatcher;

    @InjectMocks
    private PetTutorActivityNotifier notifier;

    private static final UUID ULYSSES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MARIA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID JOAO_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private Person confirmado(UUID id, String nome) {
        return Person.builder()
                .personId(id).name(nome).email(nome.toLowerCase() + "@petfy.com.br")
                .emailVerifiedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private Person naoConfirmado(UUID id, String nome) {
        return Person.builder()
                .personId(id).name(nome).email(nome.toLowerCase() + "@petfy.com.br")
                .build();
    }

    private Animal rex() {
        return Animal.builder().animalId(UUID.randomUUID()).name("Rex").species(Species.CANINA).build();
    }

    private List<Notification> enviadas() {
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher, org.mockito.Mockito.atLeastOnce()).dispatch(captor.capture(), anyString());
        return captor.getAllValues();
    }

    @Nested
    @DisplayName("tutorEntrou")
    class TutorEntrou {

        @Test
        @DisplayName("avisa os outros tutores, com o nome e o papel de quem entrou")
        void avisaOsOutrosTutores() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorEntrou(rex(), List.of(ulysses, maria), maria, PetTutorRole.EDITOR);

            var enviada = enviadas().get(0);
            assertThat(enviada.getToEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(enviada.getSubject()).isEqualTo("Maria entrou no Rex");
            assertThat(enviada.getLines()).anySatisfy(linha ->
                    assertThat(linha).contains("Maria", "cuidar do Rex"));
            assertThat(enviada.getLines()).anySatisfy(linha ->
                    assertThat(linha).contains("editor"));
        }

        /** Quem acabou de aceitar o convite nao precisa ser informado do que fez. */
        @Test
        @DisplayName("nao avisa quem entrou")
        void naoAvisaQuemEntrou() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorEntrou(rex(), List.of(ulysses, maria), maria, PetTutorRole.EDITOR);

            verify(dispatcher, times(1)).dispatch(any(Notification.class), anyString());
            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .containsExactly("ulysses@petfy.com.br")
                    .doesNotContain("maria@petfy.com.br");
        }

        /**
         * O papel importa na mensagem: "passou a cuidar do Rex" e diferente de quem
         * pode registrar vacina e de quem so acompanha.
         */
        @Test
        @DisplayName("descreve o papel em vez de mostrar o enum")
        void descreveOPapel() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorEntrou(rex(), List.of(ulysses, maria), maria, PetTutorRole.VIEWER);

            assertThat(enviadas().get(0).getLines())
                    .anySatisfy(linha -> assertThat(linha).contains("leitor", "acompanha sem alterar"))
                    .noneSatisfy(linha -> assertThat(linha).contains("VIEWER"));
        }

        /*
         * ESTE TESTE MUDOU DE LADO NA V48, e o que ele guardava continua guardado.
         *
         * Ele dizia "nao envia para e-mail nao confirmado", com `verifyNoInteractions(dispatcher)`.
         * A razao era certa — o nome do animal e dos tutores nao vai para a caixa de um estranho —,
         * e o e-mail continua nao saindo por ela.
         *
         * <b>O que mudou e que o dispatcher deixou de ser sinonimo de e-mail.</b> Atras dele ha
         * agora dois canais, e o in-app so aparece para quem ja entrou na conta: nao ha caixa de
         * estranho nenhuma. Suprimir aqui em cima calava tambem esse, e deixava sem aviso justamente
         * quem nao le e-mail do produto.
         */
        @Test
        @DisplayName("o tutor sem e-mail confirmado recebe no app, e so o e-mail dele nao sai")
        void semEmailConfirmadoRecebeSoNoApp() {
            var ulysses = naoConfirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorEntrou(rex(), List.of(ulysses, maria), maria, PetTutorRole.EDITOR);

            var enviadas = enviadas();

            assertThat(enviadas).hasSize(1);
            assertThat(enviadas.get(0).getToEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(enviadas.get(0).isPorEmail()).isFalse();
        }

        /** Falhar para um tutor nao pode calar os outros. */
        @Test
        @DisplayName("falha para um tutor nao impede o aviso aos demais")
        void falhaParaUmNaoImpedeOsDemais() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var joao = confirmado(JOAO_ID, "Joao");
            var maria = confirmado(MARIA_ID, "Maria");

            doThrow(new RuntimeException("pool em shutdown"))
                    .doNothing()
                    .when(dispatcher).dispatch(any(Notification.class), anyString());

            notifier.tutorEntrou(rex(), List.of(ulysses, joao, maria), maria, PetTutorRole.EDITOR);

            verify(dispatcher, times(2)).dispatch(any(Notification.class), anyString());
        }
    }

    @Nested
    @DisplayName("titularidadeMudou")
    class TitularidadeMudou {

        @Test
        @DisplayName("diz quem era, quem passou a ser, e o que o titular decide")
        void dizDeQuemParaQuem() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");
            var joao = confirmado(JOAO_ID, "Joao");

            notifier.titularidadeMudou(rex(), List.of(ulysses, maria, joao), ulysses, maria, ulysses);

            var enviada = enviadas().get(0);
            assertThat(enviada.getSubject()).isEqualTo("Novo titular do Rex");
            assertThat(enviada.getLines()).anySatisfy(linha ->
                    assertThat(linha).contains("de Ulysses para Maria"));
            assertThat(enviada.getLines()).anySatisfy(linha ->
                    assertThat(linha).contains("convites", "exclusao"));
        }

        /**
         * Vai para todos os tutores, e nao so para os dois envolvidos: quem e o
         * titular define quem pode convidar e apagar o animal, entao interessa a quem
         * cuida do animal.
         */
        @Test
        @DisplayName("avisa todos os tutores, nao so os dois envolvidos")
        void avisaTodosOsTutores() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");
            var joao = confirmado(JOAO_ID, "Joao");

            notifier.titularidadeMudou(rex(), List.of(ulysses, maria, joao), ulysses, maria, ulysses);

            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .containsExactlyInAnyOrder("maria@petfy.com.br", "joao@petfy.com.br");
        }

        @Test
        @DisplayName("nao avisa quem executou a transferencia")
        void naoAvisaQuemAgiu() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.titularidadeMudou(rex(), List.of(ulysses, maria), ulysses, maria, ulysses);

            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .doesNotContain("ulysses@petfy.com.br");
        }

        /**
         * O titular antigo nao sai do animal, vira EDITOR - e a mensagem diz isso, senao
         * quem recebe conclui que a pessoa perdeu o acesso.
         */
        @Test
        @DisplayName("deixa claro que o titular antigo continua tutor")
        void dizQueOAntigoContinuaTutor() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.titularidadeMudou(rex(), List.of(ulysses, maria), ulysses, maria, maria);

            assertThat(enviadas().get(0).getLines()).anySatisfy(linha ->
                    assertThat(linha).contains("Ulysses continua tutor"));
        }
    }

    @Nested
    @DisplayName("tutorSaiu")
    class TutorSaiu {

        @Test
        @DisplayName("avisa quem fica que a pessoa deixou de cuidar do animal")
        void avisaQuemFica() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorSaiu(rex(), List.of(ulysses), maria, maria);

            var enviada = enviadas().get(0);
            assertThat(enviada.getToEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(enviada.getSubject()).isEqualTo("Maria saiu do Rex");
        }

        /**
         * O caso que justifica este aviso existir: perder acesso ao historico de um
         * animal que se cuidava sem receber uma linha sobre isso e descobrir tentando
         * abrir a carteira.
         */
        @Test
        @DisplayName("quem foi removido pelo titular tambem e avisado")
        void quemFoiRemovidoEAvisado() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var joao = confirmado(JOAO_ID, "Joao");
            var maria = confirmado(MARIA_ID, "Maria");

            // ulysses removeu, entao nao recebe: joao fica e e avisado, maria saiu e
            // e avisada de que perdeu o acesso
            notifier.tutorSaiu(rex(), List.of(ulysses, joao), maria, ulysses);

            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .containsExactlyInAnyOrder("joao@petfy.com.br", "maria@petfy.com.br");
            assertThat(enviadas())
                    .filteredOn(n -> n.getToEmail().equals("maria@petfy.com.br"))
                    .singleElement()
                    .satisfies(n -> {
                        assertThat(n.getSubject()).contains("acesso ao Rex foi encerrado");
                        assertThat(n.getLines()).anySatisfy(linha ->
                                assertThat(linha).contains("deixou de ter acesso"));
                    });
        }

        /** Quem saiu por conta propria sabe que saiu. */
        @Test
        @DisplayName("quem saiu sozinho nao recebe aviso de que saiu")
        void quemSaiuSozinhoNaoEAvisado() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorSaiu(rex(), List.of(ulysses), maria, maria);

            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .containsExactly("ulysses@petfy.com.br");
        }

        @Test
        @DisplayName("nao avisa o titular que removeu")
        void naoAvisaQuemRemoveu() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var joao = confirmado(JOAO_ID, "Joao");
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorSaiu(rex(), List.of(ulysses, joao), maria, ulysses);

            assertThat(enviadas()).extracting(Notification::getToEmail)
                    .doesNotContain("ulysses@petfy.com.br")
                    .containsExactlyInAnyOrder("joao@petfy.com.br", "maria@petfy.com.br");
        }
    }

    @Nested
    @DisplayName("rodape")
    class Rodape {

        /**
         * Todo aviso desta classe termina apontando o que fazer se a mudanca nao for
         * reconhecida - e o que transforma o aviso em algo acionavel em vez de so
         * informativo.
         */
        @Test
        @DisplayName("todo aviso termina com a orientacao de revisar os tutores")
        void todoAvisoTerminaComOrientacao() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");
            var animal = rex();

            notifier.tutorEntrou(animal, List.of(ulysses, maria), maria, PetTutorRole.EDITOR);
            notifier.titularidadeMudou(animal, List.of(ulysses, maria), ulysses, maria, ulysses);
            notifier.tutorSaiu(animal, List.of(ulysses), maria, ulysses);

            assertThat(enviadas()).allSatisfy(enviada ->
                    assertThat(enviada.getLines()).last()
                            .isEqualTo("Se nao reconhece esta mudanca, revise os tutores do animal no Petfy."));
        }

        @Test
        @DisplayName("nenhum aviso vaza o e-mail dos outros tutores")
        void nenhumAvisoVazaEmailDeTerceiro() {
            var ulysses = confirmado(ULYSSES_ID, "Ulysses");
            var maria = confirmado(MARIA_ID, "Maria");
            var joao = confirmado(JOAO_ID, "Joao");

            notifier.titularidadeMudou(rex(), List.of(ulysses, maria, joao), ulysses, maria, ulysses);

            // o corpo cita nomes, que os tutores do mesmo animal ja conhecem; e-mail de
            // terceiro nao entra - quem quiser ve na lista de tutores, autenticado
            assertThat(enviadas()).allSatisfy(enviada ->
                    assertThat(String.join(" ", enviada.getLines()))
                            .doesNotContain("@petfy.com.br"));
        }
    }

    @Nested
    @DisplayName("lista sem ninguem a avisar")
    class SemDestinatario {

        /** Animal de um tutor so: nao ha a quem contar que ele mesmo entrou. */
        @Test
        @DisplayName("tutor unico nao gera aviso nenhum")
        void tutorUnicoNaoGeraAviso() {
            var maria = confirmado(MARIA_ID, "Maria");

            notifier.tutorEntrou(rex(), List.of(maria), maria, PetTutorRole.HOLDER);

            verify(dispatcher, never()).dispatch(any(), anyString());
        }
    }

}
