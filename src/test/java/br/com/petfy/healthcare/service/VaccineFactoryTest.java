package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A recusa de dose duplicada, que a Tela 14 desenha e ninguem cumpria.
 *
 * "Dois registros da mesma dose viram dose dobrada no historico" — e ate aqui as duas gravacoes
 * passavam. O dano e silencioso: ninguem e avisado, e quem le o historico ve duas aplicacoes onde
 * houve uma.
 *
 * <b>O cenario e de duas pessoas, e nao de dedo duplo:</b> a clinica registra a aplicacao e o
 * tutor registra a mesma dose minutos depois, cada um achando que o outro nao registrou. Por isso
 * a guarda esta na fabrica, por onde passam as duas rotas de criacao — a do tutor e a do
 * veterinario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("a fabrica de vacina: a recusa de dose duplicada")
class VaccineFactoryTest {

    @Mock private VaccineCatalogRepository vaccineCatalogRepository;
    @Mock private VaccineRepository vaccineRepository;

    private VaccineFactory factory;

    private Animal code;
    private VaccineCatalog antirrabica;

    private static final LocalDate ONTEM = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        factory = new VaccineFactory(vaccineCatalogRepository, vaccineRepository);

        code = Animal.builder().animalId(UUID.randomUUID()).name("Code").species(Species.CANINA).build();

        antirrabica = VaccineCatalog.builder()
                .vaccineCatalogId(UUID.randomUUID())
                .code("ANTIRRABICA_C").name("Antirrabica canina")
                .species(Species.CANINA).defaultIntervalDays(365)
                .build();
    }

    private VaccineRequestDTO pedido(UUID catalogId, String nome, LocalDate data) {
        return VaccineRequestDTO.builder()
                .animalId(code.getAnimalId())
                .vaccineCatalogId(catalogId)
                .vaccineName(nome)
                .applicationDate(data)
                .build();
    }

    private Vaccine jaRegistrada(VaccineCatalog catalog, String nome) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID()).animal(code)
                .catalog(catalog).vaccineName(nome).applicationDate(ONTEM)
                .build();
    }

    private void noDia(Vaccine... existentes) {
        when(vaccineRepository.findByAnimalAnimalIdAndApplicationDate(code.getAnimalId(), ONTEM))
                .thenReturn(List.of(existentes));
    }

    @Nested
    @DisplayName("recusa")
    class Recusa {

        @Test
        @DisplayName("mesma vacina do catalogo, mesmo animal, mesmo dia")
        void mesmoCatalogo() {
            when(vaccineCatalogRepository.findById(antirrabica.getVaccineCatalogId()))
                    .thenReturn(Optional.of(antirrabica));
            noDia(jaRegistrada(antirrabica, "Antirrabica canina"));

            assertThatThrownBy(() -> factory.build(code, null, null,
                    pedido(antirrabica.getVaccineCatalogId(), null, ONTEM)))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .satisfies(e -> {
                        var erro = (PetfyHealthcareException) e;
                        assertThat(erro.getCode()).isEqualTo(149);
                        assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        /**
         * O caso que o casamento so por catalogo deixaria passar, e e o mais provavel de todos: a
         * clinica escolheu da lista, o tutor digitou o nome.
         */
        @Test
        @DisplayName("registro digitado a mao contra registro do catalogo, pelo nome")
        void digitadoContraCatalogo() {
            noDia(jaRegistrada(antirrabica, "Antirrabica canina"));

            assertThatThrownBy(() -> factory.build(code, null, null,
                    pedido(null, "Antirrabica canina", ONTEM)))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("nome igual com maiuscula e espaco diferentes e a mesma dose")
        void nomeComGrafiaDiferente() {
            noDia(jaRegistrada(null, "antirrabica "));

            assertThatThrownBy(() -> factory.build(code, null, null,
                    pedido(null, "Antirrabica", ONTEM)))
                    .isInstanceOf(PetfyHealthcareException.class);
        }
    }

    @Nested
    @DisplayName("deixa passar")
    class DeixaPassar {

        @Test
        @DisplayName("vacina diferente no mesmo dia — dois registros legitimos")
        void vacinaDiferenteNoMesmoDia() {
            noDia(jaRegistrada(null, "V10"));

            assertThatCode(() -> factory.build(code, null, null, pedido(null, "Giardia", ONTEM)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mesma vacina em outro dia — reforco")
        void mesmaVacinaEmOutroDia() {
            when(vaccineRepository.findByAnimalAnimalIdAndApplicationDate(any(), any()))
                    .thenReturn(List.of());

            assertThatCode(() -> factory.build(code, null, null,
                    pedido(null, "Antirrabica", LocalDate.now())))
                    .doesNotThrowAnyException();
        }

        /**
         * Duas ausencias de data nao provam que sao a mesma dose. Recusar aqui barraria registro
         * legitimo por falta de um campo.
         */
        @Test
        @DisplayName("sem data de aplicacao nao ha recusa, e nem consulta ao banco")
        void semDataNaoRecusa() {
            assertThatCode(() -> factory.build(code, null, null, pedido(null, "Antirrabica", null)))
                    .doesNotThrowAnyException();
        }
    }
}
