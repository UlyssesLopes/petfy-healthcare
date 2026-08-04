package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.entity.Vet;
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

    private Vet vet() {
        return Vet.builder().vetId(UUID.randomUUID()).name("Dra. Marina")
                .clinic(Clinic.builder().clinicId(UUID.randomUUID()).name("Clinica Bicho Feliz").build())
                .build();
    }

    private Owner owner() {
        return Owner.builder().ownerId(UUID.randomUUID()).name("Ulysses").build();
    }

    @Nested
    @DisplayName("gravacao")
    class Gravacao {

        @Test
        @DisplayName("deve guardar o estado ANTERIOR, e nao o novo")
        void deveGuardarOEstadoAnterior() {
            vaccineCorrectionLog.recordByVet(vacina(), vet());

            var captor = ArgumentCaptor.forClass(VaccineCorrection.class);
            verify(vaccineCorrectionRepository).save(captor.capture());

            assertThat(captor.getValue().getPreviousVaccineName()).isEqualTo("V10");
            assertThat(captor.getValue().getPreviousApplicationDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(captor.getValue().getPreviousNextDoseDate()).isEqualTo(LocalDate.of(2027, 8, 1));
            assertThat(captor.getValue().getPreviousDescription()).isEqualTo("antes da correcao");
            assertThat(captor.getValue().getCorrectedAt()).isNotNull();
        }

        @Test
        @DisplayName("correcao de veterinario nao deve marcar tutor, e vice-versa")
        void naoDeveMarcarOsDoisAutores() {
            vaccineCorrectionLog.recordByVet(vacina(), vet());
            var porVet = capturar();
            assertThat(porVet.getCorrectedByVet()).isNotNull();
            assertThat(porVet.getCorrectedByOwner()).isNull();

            vaccineCorrectionLog.recordByOwner(vacina(), owner());
            var porTutor = capturar();
            assertThat(porTutor.getCorrectedByOwner()).isNotNull();
            assertThat(porTutor.getCorrectedByVet()).isNull();
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

        private VaccineCorrection correcao(Vet vet, Owner owner) {
            return VaccineCorrection.builder()
                    .vaccineCorrectionId(UUID.randomUUID())
                    .vaccine(vacina())
                    .correctedByVet(vet)
                    .correctedByOwner(owner)
                    .previousVaccineName("V8")
                    .previousApplicationDate(LocalDate.of(2026, 7, 1))
                    .previousDescription("texto antigo")
                    .correctedAt(LocalDateTime.now().minusDays(1))
                    .build();
        }

        @Test
        @DisplayName("deve identificar correcao feita por veterinario, com a clinica dele")
        void deveIdentificarCorrecaoDeVeterinario() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of(correcao(vet(), null)));

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).singleElement().satisfies(c -> {
                assertThat(c.getCorrectedByRole()).isEqualTo("VET");
                assertThat(c.getCorrectedByName()).isEqualTo("Dra. Marina");
                assertThat(c.getCorrectedByClinicName()).isEqualTo("Clinica Bicho Feliz");
            });
        }

        @Test
        @DisplayName("deve identificar correcao feita pelo tutor, sem clinica")
        void deveIdentificarCorrecaoDoTutor() {
            when(vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(VACCINE_ID))
                    .thenReturn(List.of(correcao(null, owner())));

            assertThat(vaccineCorrectionLog.list(VACCINE_ID)).singleElement().satisfies(c -> {
                assertThat(c.getCorrectedByRole()).isEqualTo("OWNER");
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
