package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A peca que decide quem enxerga o animal de quem.
 *
 * O que este teste protege nao e o codigo do guard, e a promessa dele: animal que a
 * pessoa nao alcanca responde <b>404</b>, nunca 403 - porque um 403 confirmaria
 * que aquele id existe e permitiria varrer a base. Por isso cada caso de negativa
 * verifica o status, e nao so que "deu erro".
 */
@ExtendWith(MockitoExtension.class)
class AnimalAccessGuardTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private PetTutorRepository petTutorRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @InjectMocks
    private AnimalAccessGuard animalAccessGuard;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Person autenticado() {
        Person person = Person.builder()
                .personId(OWNER_ID)
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

    /** Coloca a pessoa autenticada como tutora do animal, no papel informado. */
    private PetTutor tutorNoPapel(PetTutorRole role) {
        PetTutor vinculo = PetTutores.vinculo(autenticado(), role);
        vinculo.setAnimal(animal());

        when(petTutorRepository.findByAnimalAnimalIdAndPersonPersonId(ANIMAL_ID, OWNER_ID))
                .thenReturn(Optional.of(vinculo));

        return vinculo;
    }

    /** A pessoa autenticada nao e tutora deste animal - exista ele ou nao. */
    private void semVinculo() {
        autenticado();
        when(petTutorRepository.findByAnimalAnimalIdAndPersonPersonId(ANIMAL_ID, OWNER_ID))
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

    private void assertPapelInsuficiente(Consumer<UUID> chamada) {
        assertThatThrownBy(() -> chamada.accept(ANIMAL_ID))
                .isInstanceOf(PetfyHealthcareException.class)
                .hasMessage(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getMessage())
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.FORBIDDEN, ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode());
    }

    @Nested
    @DisplayName("requireLeitura")
    class RequireLeitura {

        @ParameterizedTest(name = "{0} le a carteira")
        @EnumSource(PetTutorRole.class)
        @DisplayName("qualquer papel alcanca a leitura")
        void qualquerPapelLe(PetTutorRole role) {

            tutorNoPapel(role);
            animalExiste();

            assertThat(animalAccessGuard.requireLeitura(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("quem nao e tutor recebe 404, e nao 403")
        void naoTutorRecebe404() {

            semVinculo();

            assertNaoEncontrado(animalAccessGuard::requireLeitura);
        }
    }

    @Nested
    @DisplayName("requireEscrita")
    class RequireEscrita {

        @ParameterizedTest(name = "{0} escreve")
        @EnumSource(value = PetTutorRole.class, names = {"HOLDER", "EDITOR"})
        @DisplayName("titular e editor escrevem")
        void titularEEditorEscrevem(PetTutorRole role) {

            tutorNoPapel(role);
            animalExiste();

            assertThat(animalAccessGuard.requireEscrita(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("VIEWER recebe 403: ja e tutor, entao o status nao revela nada novo")
        void viewerRecebe403() {

            tutorNoPapel(PetTutorRole.VIEWER);

            assertPapelInsuficiente(animalAccessGuard::requireEscrita);
        }

        @Test
        @DisplayName("papel insuficiente barra antes de carregar o animal")
        void papelInsuficienteNaoCarregaOAnimal() {

            tutorNoPapel(PetTutorRole.VIEWER);

            assertPapelInsuficiente(animalAccessGuard::requireEscrita);

            verify(animalRepository, never()).findById(any());
        }

        @Test
        @DisplayName("quem nao e tutor recebe 404, e nao 403")
        void naoTutorRecebe404() {

            semVinculo();

            assertNaoEncontrado(animalAccessGuard::requireEscrita);
        }
    }

    @Nested
    @DisplayName("requireTitular")
    class RequireTitular {

        @Test
        @DisplayName("so o titular passa")
        void titularPassa() {

            tutorNoPapel(PetTutorRole.HOLDER);
            animalExiste();

            assertThat(animalAccessGuard.requireTitular(ANIMAL_ID).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @ParameterizedTest(name = "{0} nao transfere titularidade nem apaga o animal")
        @EnumSource(value = PetTutorRole.class, names = {"EDITOR", "VIEWER"})
        @DisplayName("editor e viewer recebem 403")
        void demaisPapeisRecebem403(PetTutorRole role) {

            tutorNoPapel(role);

            assertPapelInsuficiente(animalAccessGuard::requireTitular);
        }

        @Test
        @DisplayName("quem nao e tutor recebe 404, e nao 403")
        void naoTutorRecebe404() {

            semVinculo();

            assertNaoEncontrado(animalAccessGuard::requireTitular);
        }
    }

    @Nested
    @DisplayName("animal que sumiu entre o vinculo e a leitura")
    class AnimalSumiu {

        /**
         * O vinculo existe mas o animal nao: acontece quando outro tutor apaga o animal
         * entre uma requisicao e outra. A resposta tem de ser a mesma de quem nunca
         * alcancou o animal - qualquer diferenca ai vira sinal de que o id existiu.
         */
        @Test
        @DisplayName("responde 404 mesmo com vinculo valido")
        void animalApagadoResponde404() {

            tutorNoPapel(PetTutorRole.HOLDER);
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

            assertNaoEncontrado(animalAccessGuard::requireTitular);
        }
    }

    @Nested
    @DisplayName("vinculoDoAutenticado")
    class VinculoDoAutenticado {

        @Test
        @DisplayName("devolve o vinculo com o papel de quem chamou")
        void devolveOVinculo() {

            PetTutor esperado = tutorNoPapel(PetTutorRole.EDITOR);

            PetTutor vinculo = animalAccessGuard.vinculoDoAutenticado(ANIMAL_ID);

            assertThat(vinculo).isSameAs(esperado);
            assertThat(vinculo.getRole()).isEqualTo(PetTutorRole.EDITOR);
        }

        @Test
        @DisplayName("sem vinculo responde 404 sem tocar no animal")
        void semVinculoResponde404() {

            semVinculo();

            assertNaoEncontrado(animalAccessGuard::vinculoDoAutenticado);

            verifyNoInteractions(animalRepository);
        }
    }

    @Nested
    @DisplayName("alcanca")
    class Alcanca {

        @Test
        @DisplayName("verdadeiro para quem tem vinculo")
        void verdadeiroParaTutor() {

            autenticado();
            when(petTutorRepository.existsByAnimalAnimalIdAndPersonPersonId(ANIMAL_ID, OWNER_ID)).thenReturn(true);

            assertThat(animalAccessGuard.alcanca(ANIMAL_ID)).isTrue();
        }

        /**
         * Diferente dos require*, aqui a ausencia de vinculo e resposta, nao erro:
         * quem pergunta esta decidindo o que mostrar, e nao autorizando uma acao.
         */
        @Test
        @DisplayName("falso para quem nao tem vinculo, sem lancar excecao")
        void falsoParaNaoTutor() {

            autenticado();
            when(petTutorRepository.existsByAnimalAnimalIdAndPersonPersonId(ANIMAL_ID, OWNER_ID)).thenReturn(false);

            assertThat(animalAccessGuard.alcanca(ANIMAL_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("requisicao sem dono autenticado")
    class SemAutenticacao {

        @Test
        @DisplayName("propaga o 401 do CurrentPersonProvider sem consultar vinculo")
        void propaga401() {

            when(currentPersonProvider.require()).thenThrow(new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                    ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                    HttpStatus.UNAUTHORIZED));

            assertThatThrownBy(() -> animalAccessGuard.requireLeitura(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(petTutorRepository, animalRepository);
        }
    }

}
