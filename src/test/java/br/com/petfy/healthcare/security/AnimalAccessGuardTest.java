package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A peca que decide quem enxerga o animal de quem.
 *
 * O que este teste protege nao e o codigo do guard, e a promessa dele: animal que a
 * pessoa nao alcanca responde <b>404</b>, nunca 403 - porque um 403 confirmaria que
 * aquele id existe e permitiria varrer a base. Por isso cada caso de negativa
 * verifica o status, e nao so que "deu erro".
 *
 * <b>Reescrito no P2b, e nao adaptado.</b> A versao anterior parametrizava por
 * {@code PetTutorRole} e afirmava, com isso, que titular e co-tutor eram graus da
 * mesma coisa. Nao sao: quem responde pelo animal alcanca por custodia, sem prazo e
 * sem concessao de ninguem; quem recebeu acesso alcanca por concessao, com nivel e
 * prazo. Trocar o enum e manter a forma do teste teria preservado a afirmacao errada.
 *
 * Os casos que sobreviveram inteiros sao os que falam de status - 404 para quem nao
 * alcanca, 403 so para quem alcanca e falta nivel -, porque essa promessa nao mudou.
 */
@ExtendWith(MockitoExtension.class)
class AnimalAccessGuardTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private CustodyRepository custodyRepository;

    @Mock
    private GrantRepository grantRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @InjectMocks
    private AnimalAccessGuard animalAccessGuard;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERSON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Person autenticado() {
        Person person = Person.builder()
                .personId(PERSON_ID)
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .build();

        when(currentPersonProvider.require()).thenReturn(person);
        return person;
    }

    private Animal animal() {
        return Animal.builder()
                .animalId(ANIMAL_ID)
                .name("Rex")
                .species(Species.CANINA)
                .build();
    }

    /** A pessoa autenticada responde pelo animal. */
    private Custody responsavel() {
        Custody custodia = Custodias.emCurso(autenticado());
        custodia.setAnimal(animal());

        when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, PERSON_ID))
                .thenReturn(Optional.of(custodia));

        return custodia;
    }

    /** A pessoa autenticada alcanca o animal por concessao, no nivel informado. */
    private Grant comConcessao(GrantLevel nivel) {
        Grant concessao = Grant.builder()
                .grantId(UUID.randomUUID())
                .animal(animal())
                .granteePerson(autenticado())
                .level(nivel)
                .grantedAt(LocalDateTime.now())
                .build();

        when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, PERSON_ID)).thenReturn(Optional.empty());
        when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(PERSON_ID), any()))
                .thenReturn(Optional.of(concessao));

        return concessao;
    }

    /** A pessoa autenticada nao alcanca o animal de forma nenhuma - exista ele ou nao. */
    private void semAlcance() {
        autenticado();
        when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, PERSON_ID)).thenReturn(Optional.empty());
        when(grantRepository.findVigenteDaPessoaNoAnimal(eq(ANIMAL_ID), eq(PERSON_ID), any()))
                .thenReturn(Optional.empty());
    }

    private void animalExiste() {
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(animal()));
    }

    private void assertNaoEncontrado(Consumer<UUID> chamada) {
        assertThatThrownBy(() -> chamada.accept(ANIMAL_ID))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasMessage(ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage())
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.NOT_FOUND, ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode());
    }

    private void assertNivelInsuficiente(Consumer<UUID> chamada) {
        assertThatThrownBy(() -> chamada.accept(ANIMAL_ID))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasMessage(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getMessage())
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.FORBIDDEN, ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode());
    }

    @Nested
    @DisplayName("requireLeitura")
    class RequireLeitura {

        @Test
        @DisplayName("quem responde pelo animal le")
        void responsavelLe() {
            responsavel();
            animalExiste();

            assertThat(animalAccessGuard.requireLeitura(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @ParameterizedTest(name = "concessao {0} le a carteira")
        @EnumSource(GrantLevel.class)
        @DisplayName("qualquer nivel de concessao alcanca a leitura")
        void qualquerNivelLe(GrantLevel nivel) {
            comConcessao(nivel);
            animalExiste();

            assertThat(animalAccessGuard.requireLeitura(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("quem nao alcanca recebe 404, e nao 403")
        void semAlcanceRecebe404() {
            semAlcance();

            assertNaoEncontrado(animalAccessGuard::requireLeitura);
        }
    }

    @Nested
    @DisplayName("requireEscrita")
    class RequireEscrita {

        @Test
        @DisplayName("quem responde pelo animal escreve")
        void responsavelEscreve() {
            responsavel();
            animalExiste();

            assertThat(animalAccessGuard.requireEscrita(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("concessao EDITOR escreve")
        void editorEscreve() {
            comConcessao(GrantLevel.EDITOR);
            animalExiste();

            assertThat(animalAccessGuard.requireEscrita(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("VIEWER recebe 403: ja alcanca o animal, entao o status nao revela nada novo")
        void viewerRecebe403() {
            comConcessao(GrantLevel.VIEWER);

            assertNivelInsuficiente(animalAccessGuard::requireEscrita);
        }

        @Test
        @DisplayName("nivel insuficiente barra antes de carregar o animal")
        void nivelInsuficienteNaoCarregaOAnimal() {
            comConcessao(GrantLevel.VIEWER);

            assertNivelInsuficiente(animalAccessGuard::requireEscrita);

            verify(animalRepository, never()).findById(any());
        }

        @Test
        @DisplayName("quem nao alcanca recebe 404, e nao 403")
        void semAlcanceRecebe404() {
            semAlcance();

            assertNaoEncontrado(animalAccessGuard::requireEscrita);
        }
    }

    @Nested
    @DisplayName("requireCustodia")
    class RequireCustodia {

        @Test
        @DisplayName("so quem responde pelo animal passa")
        void responsavelPassa() {
            responsavel();
            animalExiste();

            assertThat(animalAccessGuard.requireCustodia(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        /**
         * <b>Nenhum nivel de concessao chega aqui</b>, e nao por rigor: quem tem acesso
         * concedido poderia conceder acesso a um comparsa e, no limite, tomar o animal
         * de quem responde por ele. Era a mesma razao pela qual o EDITOR nunca podia
         * convidar.
         */
        @ParameterizedTest(name = "concessao {0} nao concede acesso nem transfere custodia")
        @EnumSource(GrantLevel.class)
        @DisplayName("concessao nenhuma substitui custodia, em nivel nenhum")
        void concessaoNaoSubstituiCustodia(GrantLevel nivel) {
            comConcessao(nivel);

            assertNivelInsuficiente(animalAccessGuard::requireCustodia);
        }

        @Test
        @DisplayName("quem nao alcanca recebe 404, e nao 403")
        void semAlcanceRecebe404() {
            semAlcance();

            assertNaoEncontrado(animalAccessGuard::requireCustodia);
        }
    }

    @Nested
    @DisplayName("animal que sumiu entre o vinculo e a leitura")
    class AnimalSumiu {

        /**
         * A custodia existe mas o animal nao: acontece quando quem responde apaga o
         * animal entre uma requisicao e outra. A resposta tem de ser a mesma de quem
         * nunca alcancou o animal - qualquer diferenca ai vira sinal de que o id
         * existiu.
         */
        @Test
        @DisplayName("responde 404 mesmo com custodia valida")
        void animalApagadoResponde404() {
            responsavel();
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

            assertNaoEncontrado(animalAccessGuard::requireCustodia);
        }
    }

    @Nested
    @DisplayName("custodiaDoAutenticado")
    class CustodiaDoAutenticado {

        /**
         * Substituiu {@code vinculoDoAutenticado}, que devolvia o PetTutor e servia para
         * qualquer um dos tres papeis. Quem quer saber sobre concessao pergunta sobre
         * concessao - misturar as duas coisas num retorno so foi o que este passo
         * desfez.
         */
        @Test
        @DisplayName("devolve a custodia de quem chamou")
        void devolveACustodia() {
            Custody esperada = responsavel();

            assertThat(animalAccessGuard.custodiaDoAutenticado(ANIMAL_ID)).contains(esperada);
        }

        /**
         * Vazio, e nao erro: quem pergunta esta decidindo o que mostrar, e nao
         * autorizando uma acao. Quem tem concessao alcanca o animal sem ter custodia, e
         * isso e um estado normal.
         */
        @Test
        @DisplayName("devolve vazio para quem alcanca por concessao, sem lancar excecao")
        void vazioParaQuemSoTemConcessao() {
            autenticado();
            when(custodyRepository.findEmCursoDaPessoa(ANIMAL_ID, PERSON_ID)).thenReturn(Optional.empty());

            assertThat(animalAccessGuard.custodiaDoAutenticado(ANIMAL_ID)).isEmpty();
            verifyNoInteractions(animalRepository);
        }
    }

    @Nested
    @DisplayName("alcanca")
    class Alcanca {

        @Test
        @DisplayName("verdadeiro para quem responde pelo animal")
        void verdadeiroParaResponsavel() {
            responsavel();

            assertThat(animalAccessGuard.alcanca(ANIMAL_ID)).isTrue();
        }

        @Test
        @DisplayName("verdadeiro para quem tem concessao vigente")
        void verdadeiroParaConcessao() {
            comConcessao(GrantLevel.VIEWER);

            assertThat(animalAccessGuard.alcanca(ANIMAL_ID)).isTrue();
        }

        /**
         * Diferente dos require*, aqui a ausencia de alcance e resposta, nao erro: quem
         * pergunta esta decidindo o que mostrar, e nao autorizando uma acao.
         */
        @Test
        @DisplayName("falso para quem nao alcanca, sem lancar excecao")
        void falsoParaQuemNaoAlcanca() {
            semAlcance();

            assertThat(animalAccessGuard.alcanca(ANIMAL_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("requisicao sem pessoa autenticada")
    class SemAutenticacao {

        @Test
        @DisplayName("propaga o 401 do CurrentPersonProvider sem consultar vinculo nenhum")
        void propaga401() {
            when(currentPersonProvider.require()).thenThrow(new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                    ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                    HttpStatus.UNAUTHORIZED));

            assertThatThrownBy(() -> animalAccessGuard.requireLeitura(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(custodyRepository, grantRepository, animalRepository);
        }
    }

}
