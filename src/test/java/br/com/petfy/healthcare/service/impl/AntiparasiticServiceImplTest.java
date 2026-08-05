package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AntiparasiticRequestDTO;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticCatalog;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AntiparasiticCatalogRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntiparasiticServiceImplTest {

    @Mock
    private AntiparasiticRepository antiparasiticRepository;

    @Mock
    private AntiparasiticCatalogRepository catalogRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private AntiparasiticServiceImpl antiparasiticService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ANTI_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID CATALOG_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final LocalDate APLICACAO = LocalDate.of(2026, 3, 10);

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Pet petDe(UUID ownerId, Species especie) {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .species(especie)
                .owner(owner(ownerId))
                .build();
    }

    private AntiparasiticCatalog catalogo(Species especie, Integer intervalo) {
        return AntiparasiticCatalog.builder()
                .antiparasiticCatalogId(CATALOG_ID)
                .code("VERMIFUGO_CAO_3M")
                .name("Vermifugo canino (trimestral)")
                .kind(AntiparasiticKind.DEWORMER)
                .species(especie)
                .defaultIntervalDays(intervalo)
                .build();
    }

    private Antiparasitic registroDe(UUID ownerId) {
        return Antiparasitic.builder()
                .antiparasiticId(ANTI_ID)
                .pet(petDe(ownerId, Species.CANINA))
                .name("Vermifugo canino (trimestral)")
                .kind(AntiparasiticKind.DEWORMER)
                .applicationDate(APLICACAO)
                .creationDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .updateDate(LocalDateTime.of(2026, 3, 10, 9, 0))
                .build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("sem catalogo, nome e kind vem do request")
        void semCatalogoUsaORequest() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));
            when(antiparasiticRepository.save(any(Antiparasitic.class))).thenAnswer(i -> i.getArgument(0));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .name("Vermifugo generico")
                    .kind(AntiparasiticKind.FLEA_TICK)
                    .applicationDate(APLICACAO)
                    .build();

            var result = antiparasiticService.create(request);

            assertThat(result.getName()).isEqualTo("Vermifugo generico");
            assertThat(result.getKind()).isEqualTo(AntiparasiticKind.FLEA_TICK);
            assertThat(result.getAntiparasiticCatalogId()).isNull();
        }

        @Test
        @DisplayName("com catalogo, nome e kind saem do catalogo e a proxima dose e calculada")
        void comCatalogoCalculaProximaDose() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));
            when(catalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogo(Species.CANINA, 90)));
            when(antiparasiticRepository.save(any(Antiparasitic.class))).thenAnswer(i -> i.getArgument(0));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .antiparasiticCatalogId(CATALOG_ID)
                    .applicationDate(APLICACAO)
                    .build();

            var result = antiparasiticService.create(request);

            assertThat(result.getName()).isEqualTo("Vermifugo canino (trimestral)");
            assertThat(result.getKind()).isEqualTo(AntiparasiticKind.DEWORMER);
            assertThat(result.getNextDoseDate()).isEqualTo(APLICACAO.plusDays(90));
        }

        @Test
        @DisplayName("data explicita vence o intervalo do catalogo")
        void dataExplicitaVenceOCatalogo() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));
            when(catalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogo(Species.CANINA, 90)));
            when(antiparasiticRepository.save(any(Antiparasitic.class))).thenAnswer(i -> i.getArgument(0));

            var orientacaoDoVeterinario = LocalDate.of(2026, 4, 1);
            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .antiparasiticCatalogId(CATALOG_ID)
                    .applicationDate(APLICACAO)
                    .nextDoseDate(orientacaoDoVeterinario)
                    .build();

            var result = antiparasiticService.create(request);

            assertThat(result.getNextDoseDate()).isEqualTo(orientacaoDoVeterinario);
        }

        @Test
        @DisplayName("catalogo de especie diferente da do pet responde 409")
        void catalogoDeOutraEspecieResponde409() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.FELINA)));
            when(catalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalogo(Species.CANINA, 90)));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .antiparasiticCatalogId(CATALOG_ID)
                    .applicationDate(APLICACAO)
                    .build();

            assertThatThrownBy(() -> antiparasiticService.create(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.SPECIES_MISMATCH.getCode(), HttpStatus.CONFLICT);

            verify(antiparasiticRepository, never()).save(any());
        }

        @Test
        @DisplayName("sem catalogo e sem name responde 400")
        void semCatalogoESemNomeResponde400() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .kind(AntiparasiticKind.DEWORMER)
                    .applicationDate(APLICACAO)
                    .build();

            assertThatThrownBy(() -> antiparasiticService.create(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("sem catalogo e sem kind responde 400")
        void semCatalogoESemKindResponde400() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .name("Vermifugo generico")
                    .applicationDate(APLICACAO)
                    .build();

            assertThatThrownBy(() -> antiparasiticService.create(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("pet de outro dono responde 404, nao 403")
        void petDeOutroDonoResponde404() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID, Species.CANINA)));

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .name("Vermifugo generico")
                    .kind(AntiparasiticKind.DEWORMER)
                    .build();

            assertThatThrownBy(() -> antiparasiticService.create(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.PET_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("catalogo inexistente responde 404")
        void catalogoInexistenteResponde404() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));
            when(catalogRepository.findById(CATALOG_ID)).thenReturn(Optional.empty());

            var request = AntiparasiticRequestDTO.builder()
                    .petId(PET_ID)
                    .antiparasiticCatalogId(CATALOG_ID)
                    .build();

            assertThatThrownBy(() -> antiparasiticService.create(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ANTIPARASITIC_CATALOG_NOT_FOUND.getCode(),
                            HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("campo ausente preserva o valor existente")
        void campoAusentePreservaOValor() {
            autenticadoComo(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(registroDe(OWNER_ID)));
            when(antiparasiticRepository.save(any(Antiparasitic.class))).thenAnswer(i -> i.getArgument(0));

            var somenteDescricao = AntiparasiticRequestDTO.builder()
                    .description("aplicado pela clinica")
                    .build();

            var result = antiparasiticService.update(ANTI_ID, somenteDescricao);

            assertThat(result.getDescription()).isEqualTo("aplicado pela clinica");
            assertThat(result.getName()).isEqualTo("Vermifugo canino (trimestral)");
            assertThat(result.getKind()).isEqualTo(AntiparasiticKind.DEWORMER);
            assertThat(result.getApplicationDate()).isEqualTo(APLICACAO);
        }

        @Test
        @DisplayName("mover para pet de especie diferente da do catalogo responde 409")
        void moverParaOutraEspecieResponde409() {
            autenticadoComo(OWNER_ID);

            var existente = registroDe(OWNER_ID);
            existente.setCatalog(catalogo(Species.CANINA, 90));
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(existente));

            var gatoId = UUID.fromString("77777777-7777-7777-7777-777777777777");
            var gato = Pet.builder()
                    .petId(gatoId)
                    .name("Mia")
                    .species(Species.FELINA)
                    .owner(owner(OWNER_ID))
                    .build();
            when(petRepository.findById(gatoId)).thenReturn(Optional.of(gato));

            var request = AntiparasiticRequestDTO.builder().petId(gatoId).build();

            assertThatThrownBy(() -> antiparasiticService.update(ANTI_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.SPECIES_MISMATCH.getCode(), HttpStatus.CONFLICT);

            verify(antiparasiticRepository, never()).save(any());
        }

        @Test
        @DisplayName("registro de outro dono responde 404")
        void registroDeOutroDonoResponde404() {
            autenticadoComo(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(registroDe(OUTRO_OWNER_ID)));

            var request = AntiparasiticRequestDTO.builder().description("x").build();

            assertThatThrownBy(() -> antiparasiticService.update(ANTI_ID, request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("carimba a data de atualizacao")
        void carimbaDataDeAtualizacao() {
            autenticadoComo(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(registroDe(OWNER_ID)));
            when(antiparasiticRepository.save(any(Antiparasitic.class))).thenAnswer(i -> i.getArgument(0));

            antiparasiticService.update(ANTI_ID,
                    AntiparasiticRequestDTO.builder().description("x").build());

            var captor = ArgumentCaptor.forClass(Antiparasitic.class);
            verify(antiparasiticRepository).save(captor.capture());
            assertThat(captor.getValue().getUpdateDate())
                    .isAfter(LocalDateTime.of(2026, 3, 10, 9, 0));
        }
    }

    @Nested
    @DisplayName("delete e getById")
    class DeleteEGetById {

        @Test
        @DisplayName("delete apaga o registro do proprio dono")
        void deleteApagaDoProprioDono() {
            autenticadoComo(OWNER_ID);
            var existente = registroDe(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(existente));

            antiparasiticService.delete(ANTI_ID);

            verify(antiparasiticRepository).delete(existente);
        }

        @Test
        @DisplayName("delete de registro de outro dono responde 404 e nao apaga")
        void deleteDeOutroDonoNaoApaga() {
            autenticadoComo(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(registroDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> antiparasiticService.delete(ANTI_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(antiparasiticRepository, never()).delete(any());
        }

        @Test
        @DisplayName("getById devolve o registro do proprio dono")
        void getByIdDoProprioDono() {
            autenticadoComo(OWNER_ID);
            when(antiparasiticRepository.findById(ANTI_ID)).thenReturn(Optional.of(registroDe(OWNER_ID)));

            var result = antiparasiticService.getById(ANTI_ID);

            assertThat(result.getAntiparasiticId()).isEqualTo(ANTI_ID);
            assertThat(result.getPetId()).isEqualTo(PET_ID);
        }
    }

    @Nested
    @DisplayName("listByPet")
    class ListByPet {

        @Test
        @DisplayName("lista os antiparasitarios do pet do dono autenticado")
        void listaDoPetDoDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.CANINA)));
            when(antiparasiticRepository.findByPetPetIdOrderByApplicationDateDesc(PET_ID))
                    .thenReturn(List.of(registroDe(OWNER_ID)));

            var result = antiparasiticService.listByPet(PET_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKind()).isEqualTo(AntiparasiticKind.DEWORMER);
        }

        @Test
        @DisplayName("pet de outro dono responde 404 sem consultar a listagem")
        void petDeOutroDonoNaoLista() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID, Species.CANINA)));

            assertThatThrownBy(() -> antiparasiticService.listByPet(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(antiparasiticRepository, never()).findByPetPetIdOrderByApplicationDateDesc(any());
        }
    }

    @Nested
    @DisplayName("listCatalog")
    class ListCatalog {

        @Test
        @DisplayName("sem petId devolve o catalogo inteiro")
        void semPetIdDevolveTudo() {
            when(catalogRepository.findAllByOrderBySpeciesAscNameAsc())
                    .thenReturn(List.of(catalogo(Species.CANINA, 90), catalogo(Species.FELINA, 90)));

            var result = antiparasiticService.listCatalog(null);

            assertThat(result).hasSize(2);
            verify(catalogRepository, never()).findBySpeciesOrderByNameAsc(any());
        }

        @Test
        @DisplayName("com petId filtra pela especie do pet")
        void comPetIdFiltraPelaEspecie() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID, Species.FELINA)));
            when(catalogRepository.findBySpeciesOrderByNameAsc(Species.FELINA))
                    .thenReturn(List.of(catalogo(Species.FELINA, 90)));

            var result = antiparasiticService.listCatalog(PET_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSpecies()).isEqualTo(Species.FELINA);
            verify(catalogRepository, never()).findAllByOrderBySpeciesAscNameAsc();
        }

        @Test
        @DisplayName("pet de outro dono responde 404 em vez de vazar o catalogo dele")
        void petDeOutroDonoResponde404() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID, Species.CANINA)));

            assertThatThrownBy(() -> antiparasiticService.listCatalog(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

}
