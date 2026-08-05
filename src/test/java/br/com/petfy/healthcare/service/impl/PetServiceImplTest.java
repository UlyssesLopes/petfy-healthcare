package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.PetPurger;
import br.com.petfy.healthcare.service.PuppyProtocolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetTutorRepository petTutorRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @Mock
    private PetAccessGuard petAccessGuard;

    @Mock
    private PuppyProtocolService puppyProtocolService;

    @Mock
    private PetPurger petPurger;

    @InjectMocks
    private PetServiceImpl petService;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Pet petDe(UUID ownerId) {
        return Pet.builder()
                .petId(PET_ID)
                .name("Rex")
                .type("Cachorro")
                .breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15))
                .weight(12.5)
                .gender("Macho")
                .species(Species.CANINA)
                .tutors(PetTutores.titular(owner(ownerId)))
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    private PetRequestDTO request() {
        return PetRequestDTO.builder()
                .name("Rex")
                .type("Cachorro")
                .breed("Vira-lata")
                .bornDate(LocalDate.of(2021, 3, 15))
                .weight(12.5)
                .gender("Macho")
                .species(Species.CANINA)
                .build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    @Nested
    @DisplayName("createPet")
    class CreatePet {

        @Test
        @DisplayName("deve vincular o pet ao owner autenticado")
        void deveVincularPetAoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            when(petRepository.save(any(Pet.class))).thenReturn(petDe(OWNER_ID));

            var result = petService.createPet(request());

            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);

            var captor = ArgumentCaptor.forClass(Pet.class);
            verify(petRepository).save(captor.capture());
            assertThat(captor.getValue().getCreationDate()).isNotNull();

            // o vinculo e o que diz quem manda no pet, e nasce como HOLDER: a
            // partir da V15 nao ha campo owner no pet para conferir
            var vinculo = ArgumentCaptor.forClass(PetTutor.class);
            verify(petTutorRepository).save(vinculo.capture());
            assertThat(vinculo.getValue().getOwner().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(vinculo.getValue().getRole()).isEqualTo(PetTutorRole.HOLDER);
        }

        @Test
        @DisplayName("o request nao deve conseguir escolher o dono do pet")
        void requestNaoDeveEscolherODono() {
            assertThat(PetRequestDTO.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("ownerId");
        }
    }

    @Nested
    @DisplayName("getPetById")
    class GetPetById {

        @Test
        @DisplayName("devolve o pet que o guard liberou")
        void devolveOPetQueOGuardLiberou() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(petDe(OWNER_ID));

            var result = petService.getPetById(PET_ID);

            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getName()).isEqualTo("Rex");
        }

        @Test
        @DisplayName("ownerId na resposta e o titular, e nao quem esta lendo")
        void ownerIdNaRespostaEOTitular() {
            var pet = petDe(OWNER_ID);
            pet.getTutors().add(PetTutores.vinculo(owner(OUTRO_OWNER_ID), PetTutorRole.EDITOR));
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(pet);

            assertThat(petService.getPetById(PET_ID).getOwnerId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("listAllPets")
    class ListAllPets {

        @Test
        @DisplayName("deve listar apenas os pets do owner autenticado")
        void deveListarApenasPetsDoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(0, 20);
            when(petRepository.findByTutorsOwnerOwnerId(OWNER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(petDe(OWNER_ID))));

            var result = petService.listAllPets(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOwnerId()).isEqualTo(OWNER_ID);
            verify(petRepository, never()).findAll();
        }

        @Test
        @DisplayName("a listagem paginada nao pode cair no findAll, que ignoraria o dono")
        void listagemPaginadaNaoUsaFindAll() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(3, 50);
            when(petRepository.findByTutorsOwnerOwnerId(OWNER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            petService.listAllPets(pageable);

            verify(petRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        }
    }

    @Nested
    @DisplayName("updatePet")
    class UpdatePet {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            var result = petService.updatePet(PET_ID, PetRequestDTO.builder().weight(14.0).build());

            assertThat(result.getWeight()).isEqualTo(14.0);
            assertThat(result.getName()).isEqualTo("Rex");
            assertThat(result.getBreed()).isEqualTo("Vira-lata");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("especie fica de fora do PUT parcial")
        void especieNaoMudaNoPutParcial() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            var result = petService.updatePet(PET_ID,
                    PetRequestDTO.builder().species(Species.FELINA).build());

            assertThat(result.getSpecies()).isEqualTo(Species.CANINA);
        }
    }

    @Nested
    @DisplayName("deletePet")
    class DeletePet {

        @Test
        @DisplayName("apaga o pet que o guard liberou")
        void apagaOPetQueOGuardLiberou() {
            var pet = petDe(OWNER_ID);
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet);

            petService.deletePet(PET_ID);

            verify(petRepository).delete(pet);
        }

        /**
         * Vacina, historico, peso, antiparasitario, share, acesso de clinica,
         * vinculo e convite apontam para o pet, e o schema nao tem ON DELETE CASCADE
         * em lugar nenhum: sem a limpeza a FK segura o delete e apagar o pet
         * responde 500 - o que aconteceu de verdade para qualquer pet com vacina.
         *
         * A sequencia mora no PetPurger, compartilhada com a exclusao de conta, e o
         * que este teste guarda e a <b>ordem</b>: limpar antes de apagar o pet. A
         * ordem interna esta no PetPurgerTest e a recusa do banco no
         * PetDeletionContainerTest.
         */
        @Test
        @DisplayName("limpa o que pende do pet antes de apaga-lo")
        void limpaAntesDeApagar() {
            var pet = petDe(OWNER_ID);
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(pet);

            petService.deletePet(PET_ID);

            var ordem = inOrder(petPurger, petRepository);
            ordem.verify(petPurger).purgeConteudo(List.of(PET_ID));
            ordem.verify(petRepository).delete(pet);
        }
    }

    /**
     * O nivel que cada operacao exige do guard.
     *
     * Antes da V15 cada metodo comparava o dono na propria implementacao, e o
     * teste de servico verificava isso. A comparacao virou {@link PetAccessGuard};
     * o que sobra aqui - e e o que estes casos protegem - e o servico pedir o
     * <b>nivel certo</b>. Pedir leitura onde precisa de escrita nao quebra nenhum
     * teste de comportamento, mas deixa um leitor editar o pet.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("ler o pet exige leitura")
        void lerExigeLeitura() {
            when(petAccessGuard.requireLeitura(PET_ID)).thenReturn(petDe(OWNER_ID));

            petService.getPetById(PET_ID);

            verify(petAccessGuard).requireLeitura(PET_ID);
        }

        @Test
        @DisplayName("editar o cadastro exige escrita, nao leitura")
        void editarExigeEscrita() {
            when(petAccessGuard.requireEscrita(PET_ID)).thenReturn(petDe(OWNER_ID));
            when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));

            petService.updatePet(PET_ID, PetRequestDTO.builder().name("Rex II").build());

            verify(petAccessGuard).requireEscrita(PET_ID);
            verify(petAccessGuard, never()).requireLeitura(any());
        }

        @Test
        @DisplayName("apagar o pet exige titular: nem editor apaga o pet dos outros")
        void apagarExigeTitular() {
            when(petAccessGuard.requireTitular(PET_ID)).thenReturn(petDe(OWNER_ID));

            petService.deletePet(PET_ID);

            verify(petAccessGuard).requireTitular(PET_ID);
            verify(petAccessGuard, never()).requireEscrita(any());
        }
    }
}
