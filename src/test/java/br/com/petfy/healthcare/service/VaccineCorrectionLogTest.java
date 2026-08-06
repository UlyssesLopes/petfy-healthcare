package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineCorrectionLogTest {

    @Mock
    private VaccineCorrectionRepository vaccineCorrectionRepository;

    @InjectMocks
    private VaccineCorrectionLog vaccineCorrectionLog;

    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private Vaccine vacina() {
        return Vaccine.builder()
                .vaccineId(VACCINE_ID)
                .vaccineName("V10")
                .applicationDate(LocalDate.of(2026, 8, 1))
                .nextDoseDate(LocalDate.of(2027, 8, 1))
                .description("antes da correcao")
                .build();
    }

    private Person vet() {
        return Person.builder().personId(UUID.randomUUID()).name("Dra. Marina")
                .clinic(Clinic.builder().clinicId(UUID.randomUUID()).name("Clinica Bicho Feliz").build())
                .build();
    }

    private Person person() {
        return Person.builder().personId(UUID.randomUUID()).name("Ulysses").build();
    }

    @Nested
    @DisplayName("gravacao")
    class Gravacao {

        @Test
        @DisplayName("deve guardar o estado ANTERIOR, e nao o novo")
        void deveGuardarOEstadoAnterior() {
            vaccineCorrectionLog.recordByProfessional(vacina(), vet(), vet().getClinic());

            var captor = ArgumentCaptor.forClass(VaccineCorrection.class);
            verify(vaccineCorrectionRepository).save(captor.capture());

            assertThat(captor.getValue().getPreviousVaccineName()).isEqualTo("V10");
            assertThat(captor.getValue().getPreviousApplicationDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(captor.getValue().getPreviousNextDoseDate()).isEqualTo(LocalDate.of(2027, 8, 1));
            assertThat(captor.getValue().getPreviousDescription()).isEqualTo("antes da correcao");
            assertThat(captor.getValue().getCorrectedAt()).isNotNull();
        }

        /**
         * Substituiu "correcao de veterinario nao deve marcar tutor, e vice-versa".
         *
         * Aquele caso protegia a exclusividade entre duas colunas de autor, uma
         * para cada tipo de conta. Nao ha mais dois tipos: ha sempre um autor, e o
         * que muda e se ele agiu por uma organizacao. A regra que passou a valer -
         * e que este caso protege - e que o autor nunca fica vazio, e que a clinica
         * so aparece quando existiu.
         */
        @Test
        @DisplayName("o autor e sempre uma pessoa; a clinica so aparece quando houve contexto")
        void autorSempreExisteEClinicaSoQuandoHouveContexto() {
            vaccineCorrectionLog.recordByProfessional(vacina(), vet(), vet().getClinic());
            var comContexto = capturar();
            assertThat(comContexto.getCorrectedBy()).isNotNull();
            assertThat(comContexto.getCorrectedInClinic()).isNotNull();

            vaccineCorrectionLog.recordByPerson(vacina(), person());
            var semContexto = capturar();
            assertThat(semContexto.getCorrectedBy()).isNotNull();
            assertThat(semContexto.getCorrectedInClinic()).isNull();
        }

        private VaccineCorrection capturar() {
            var captor = ArgumentCaptor.forClass(VaccineCorrection.class);
            verify(vaccineCorrectionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            return captor.getValue();
        }
    }

    @Nested
    @DisplayName("leitura")
    class Leitura {

        private VaccineCorrection correcao(Person autor, br.com.petfy.healthcare.domain.entity.Clinic clinic) {
            return VaccineCorrection.builder()
                    .vaccineCorrectionId(UUID.randomUUID())
                    .vaccine(vacina())
                    .correctedBy(autor)
                    .correctedInClinic(clinic)
                    .previousVaccineName("V8")
                    .previousApplicationDate(LocalDate.of(2026, 7, 1))
                    .previousDescription("texto antigo")
                    .correctedAt(LocalDateTime.now().minusDays(1))
                    .build();
        }

        /**
         * O papel saiu da resposta. Quem le distingue os dois casos pelo contexto:
         * "a Ana, pela Clinica Norte, corrigiu" tem clinica; "a Ana corrigiu" nao.
         */
        @Test
        @DisplayName("correcao feita em nome de uma clinica deve trazer a clinica")
        void correcaoComContextoDeveTrazerAClinica() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of(correcao(vet(), vet().getClinic())));

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).singleElement().satisfies(c -> {
                assertThat(c.getCorrectedByName()).isEqualTo("Dra. Marina");
                assertThat(c.getCorrectedByClinicName()).isEqualTo("Clinica Bicho Feliz");
            });
        }

        @Test
        @DisplayName("correcao feita pela pessoa por si nao deve trazer clinica")
        void correcaoSemContextoNaoDeveTrazerClinica() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of(correcao(person(), null)));

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).singleElement().satisfies(c -> {
                assertThat(c.getCorrectedByName()).isEqualTo("Ulysses");
                assertThat(c.getCorrectedByClinicName()).isNull();
            });
        }

        @Test
        @DisplayName("deve devolver os valores anteriores, que e o que permite ver o que mudou")
        void deveDevolverValoresAnteriores() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of(correcao(vet(), null)));

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).singleElement().satisfies(c -> {
                assertThat(c.getPreviousVaccineName()).isEqualTo("V8");
                assertThat(c.getPreviousApplicationDate()).isEqualTo(LocalDate.of(2026, 7, 1));
                assertThat(c.getPreviousDescription()).isEqualTo("texto antigo");
            });
        }

        @Test
        @DisplayName("registro nunca corrigido deve devolver rastro vazio, e nao erro")
        void registroNuncaCorrigidoDeveDevolverVazio() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of());

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).isEmpty();
        }

        @Test
        @DisplayName("o rastro nao deve expor a senha nem o email de quem corrigiu")
        void rastroNaoDeveExporDadosSensiveis() {
            assertThat(br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("correctedByEmail", "password");
        }
    }
}
