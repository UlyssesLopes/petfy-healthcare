package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuppyProtocolServiceTest {

    @Mock
    private VaccineCatalogRepository vaccineCatalogRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @InjectMocks
    private PuppyProtocolService puppyProtocolService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void configurarLimiteFilhote() {
        ReflectionTestUtils.setField(puppyProtocolService, "maxAgeDays", 120);
    }

    private Pet petComIdade(long dias, Species species) {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .bornDate(LocalDate.now().minusDays(dias))
                .species(species)
                .owner(Owner.builder().ownerId(UUID.randomUUID()).build())
                .build();
    }

    private VaccineCatalog catalogo(String code, Species species, int doses, Integer intervalo, boolean mandatory) {
        return VaccineCatalog.builder()
                .vaccineCatalogId(UUID.randomUUID())
                .code(code)
                .name(code)
                .species(species)
                .defaultIntervalDays(365)
                .initialDoseCount(doses)
                .initialDoseIntervalDays(intervalo)
                .mandatory(mandatory)
                .build();
    }

    @Nested
    @DisplayName("filhote")
    class Filhote {

        @Test
        @DisplayName("deve gerar todas as doses do protocolo inicial de todas as vacinas mandatorias")
        void deveGerarProtocoloInicial() {
            Pet pet = petComIdade(45, Species.CANINA);
            when(vaccineCatalogRepository.findBySpeciesAndMandatoryTrueOrderByNameAsc(Species.CANINA))
                    .thenReturn(List.of(
                            catalogo("V10", Species.CANINA, 3, 21, true),
                            catalogo("ANTIRRABICA_C", Species.CANINA, 1, null, true)));

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            var captor = ArgumentCaptor.forClass(List.class);
            verify(vaccineRepository).saveAll(captor.capture());

            List<Vaccine> geradas = captor.getValue();
            assertThat(geradas).hasSize(4);  // 3 doses V10 + 1 antirrabica
            assertThat(geradas).allSatisfy(v -> {
                assertThat(v.getPet()).isSameAs(pet);
                assertThat(v.getApplicationDate()).isNull();  // dose planejada, nao aplicada
                assertThat(v.getNextDoseDate()).isNotNull();
            });
            // primeira dose de cada catalogo comeca hoje; a segunda 21 dias depois
            assertThat(geradas.get(0).getNextDoseDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("filhote sem vacina mandatoria da especie no catalogo nao gera nada")
        void filhoteSemMandatoriaNaoGera() {
            Pet pet = petComIdade(45, Species.CANINA);
            when(vaccineCatalogRepository.findBySpeciesAndMandatoryTrueOrderByNameAsc(Species.CANINA))
                    .thenReturn(List.of());

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            verify(vaccineRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }

        @Test
        @DisplayName("no limite: filhote com exatamente 120 dias ainda ganha o protocolo")
        void filhoteNoLimite() {
            Pet pet = petComIdade(120, Species.FELINA);
            when(vaccineCatalogRepository.findBySpeciesAndMandatoryTrueOrderByNameAsc(Species.FELINA))
                    .thenReturn(List.of(catalogo("V4_FELINA", Species.FELINA, 2, 30, true)));

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            verify(vaccineRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
        }
    }

    @Nested
    @DisplayName("adulto")
    class Adulto {

        @Test
        @DisplayName("adulto de 3 anos nao ganha o protocolo inicial")
        void adultoNaoGeraNada() {
            Pet pet = petComIdade(365 * 3, Species.CANINA);

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            verify(vaccineRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
            verify(vaccineCatalogRepository, never()).findBySpeciesAndMandatoryTrueOrderByNameAsc(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("um dia alem do limite ja e adulto")
        void umDiaAlemDoLimite() {
            Pet pet = petComIdade(121, Species.CANINA);

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            verify(vaccineRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }

        @Test
        @DisplayName("pet sem data de nascimento nao ganha nada - nao da para saber se e filhote")
        void semBornDateNaoGeraNada() {
            Pet pet = Pet.builder()
                    .petId(PET_ID)
                    .name("Rex")
                    .species(Species.CANINA)
                    .owner(Owner.builder().ownerId(UUID.randomUUID()).build())
                    .build();

            puppyProtocolService.gerarEsquemaInicialSePuppy(pet);

            verify(vaccineRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }
    }

}
