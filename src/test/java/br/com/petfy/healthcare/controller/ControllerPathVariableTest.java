package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.service.ClinicService;
import br.com.petfy.healthcare.service.OwnerExportService;
import br.com.petfy.healthcare.service.OwnerService;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.VaccineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o id declarado na rota chega ao service.
 *
 * O nome do path variable e o nome do parametro do metodo precisam bater; quando
 * divergem, o Spring nao resolve a variavel e o endpoint quebra em runtime, o que
 * a compilacao nao acusa.
 */
@ExtendWith(MockitoExtension.class)
class ControllerPathVariableTest {

    private static final UUID ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Nested
    @DisplayName("AnimalController")
    class AnimalControllerTest {

        @Mock
        private AnimalService animalService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new AnimalController(animalService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /animals/{animalId} deve repassar o id da rota para o service")
        void getDeveRepassarIdDaRota() throws Exception {
            when(animalService.getAnimalById(ID)).thenReturn(AnimalResponseDTO.builder().animalId(ID).name("Rex").build());

            mockMvc().perform(get("/animals/{animalId}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.animalId").value(ID.toString()))
                    .andExpect(jsonPath("$.name").value("Rex"));

            verify(animalService).getAnimalById(ID);
        }

        @Test
        @DisplayName("DELETE /animals/{animalId} deve repassar o id da rota e responder 204")
        void deleteDeveRepassarIdDaRota() throws Exception {
            mockMvc().perform(delete("/animals/{animalId}", ID))
                    .andExpect(status().isNoContent());

            verify(animalService).deleteAnimal(ID);
        }
    }

    @Nested
    @DisplayName("OwnerController")
    class OwnerControllerTest {

        @Mock
        private OwnerService ownerService;

        @Mock
        private OwnerExportService ownerExportService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new OwnerController(ownerService, ownerExportService)).build();
            }
            return mockMvc;
        }

        // owner nao tem rota por id: um usuario so acessa a si mesmo, entao o id
        // vem do token. Sobra o /me, que nao tem path variable para errar.

        @Test
        @DisplayName("GET /owners/me deve resolver o owner pelo token, sem id na rota")
        void getMeDeveResolverPeloToken() throws Exception {
            when(ownerService.getCurrentOwner()).thenReturn(OwnerResponseDTO.builder().ownerId(ID).name("Ulysses").build());

            mockMvc().perform(get("/owners/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ownerId").value(ID.toString()));

            verify(ownerService).getCurrentOwner();
        }

        @Test
        @DisplayName("DELETE /owners/me deve responder 204")
        void deleteMeDeveResponder204() throws Exception {
            mockMvc().perform(delete("/owners/me"))
                    .andExpect(status().isNoContent());

            verify(ownerService).deleteCurrentOwner();
        }

        @Test
        @DisplayName("nao deve existir rota de listagem de todos os owners")
        void naoDeveExistirListagemDeOwners() throws Exception {
            mockMvc().perform(get("/owners/all"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("VaccineController")
    class VaccineControllerTest {

        @Mock
        private VaccineService vaccineService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new VaccineController(vaccineService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /vaccines/{vaccineId} deve repassar o id da rota para o service")
        void getDeveRepassarIdDaRota() throws Exception {
            when(vaccineService.getVaccineById(ID))
                    .thenReturn(VaccineResponseDTO.builder().vaccineId(ID).vaccineName("Antirrabica").build());

            mockMvc().perform(get("/vaccines/{vaccineId}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vaccineId").value(ID.toString()));

            verify(vaccineService).getVaccineById(ID);
        }

        @Test
        @DisplayName("DELETE /vaccines/{vaccineId} deve repassar o id da rota e responder 204")
        void deleteDeveRepassarIdDaRota() throws Exception {
            mockMvc().perform(delete("/vaccines/{vaccineId}", ID))
                    .andExpect(status().isNoContent());

            verify(vaccineService).deleteVaccine(ID);
        }
    }

    @Nested
    @DisplayName("ClinicController")
    class ClinicControllerTest {

        @Mock
        private ClinicService clinicService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new ClinicController(clinicService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /clinics/{clinicId} deve repassar o id da rota para o service")
        void getDeveRepassarIdDaRota() throws Exception {
            when(clinicService.getClinicById(ID))
                    .thenReturn(ClinicResponseDTO.builder().clinicId(ID).name("Clinica Bicho Feliz").build());

            mockMvc().perform(get("/clinics/{clinicId}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clinicId").value(ID.toString()));

            verify(clinicService).getClinicById(ID);
        }

        @Test
        @DisplayName("DELETE /clinics/{clinicId} deve repassar o id da rota e responder 204")
        void deleteDeveRepassarIdDaRota() throws Exception {
            mockMvc().perform(delete("/clinics/{clinicId}", ID))
                    .andExpect(status().isNoContent());

            verify(clinicService).deleteClinic(ID);
        }
    }

    /**
     * As rotas de tutor sao as unicas com <b>duas</b> path variables do mesmo tipo.
     * Trocar animalId por ownerId compila e passa por revisao sem chamar atencao, e o
     * efeito em producao seria mexer no tutor errado - ou tratar um ownerId como
     * animal, que o guard recusaria com 404 e faria parecer bug de dado.
     *
     * Por isso os dois ids aqui sao valores diferentes: com o mesmo UUID nos dois,
     * a inversao passaria batida.
     */
    @Nested
    @DisplayName("PetTutorController")
    class PetTutorControllerTest {

        private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
        private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID INVITE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

        @Mock
        private PetTutorService petTutorService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new PetTutorController(petTutorService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /animals/{animalId}/tutors deve repassar o id da rota")
        void listarTutoresRepassaOId() throws Exception {
            when(petTutorService.listTutors(ANIMAL_ID)).thenReturn(java.util.List.of(
                    PetTutorResponseDTO.builder().animalId(ANIMAL_ID).ownerId(OWNER_ID).holder(true).build()));

            mockMvc().perform(get("/animals/{animalId}/tutors", ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ownerId").value(OWNER_ID.toString()));

            verify(petTutorService).listTutors(ANIMAL_ID);
        }

        @Test
        @DisplayName("DELETE /animals/{animalId}/tutors/{ownerId} nao pode trocar os dois ids")
        void removerTutorNaoTrocaOsIds() throws Exception {
            mockMvc().perform(delete("/animals/{animalId}/tutors/{ownerId}", ANIMAL_ID, OWNER_ID))
                    .andExpect(status().isNoContent());

            verify(petTutorService).removeTutor(ANIMAL_ID, OWNER_ID);
        }

        @Test
        @DisplayName("DELETE do convite nao pode trocar animalId por inviteId")
        void revogarConviteNaoTrocaOsIds() throws Exception {
            mockMvc().perform(delete("/animals/{animalId}/tutors/invites/{petTutorInviteId}", ANIMAL_ID, INVITE_ID))
                    .andExpect(status().isNoContent());

            verify(petTutorService).revokeInvite(ANIMAL_ID, INVITE_ID);
        }

        @Test
        @DisplayName("POST de transferencia nao pode trocar os dois ids")
        void transferirNaoTrocaOsIds() throws Exception {
            when(petTutorService.transferHolder(ANIMAL_ID, OWNER_ID)).thenReturn(
                    PetTutorResponseDTO.builder().animalId(ANIMAL_ID).ownerId(OWNER_ID).holder(true).build());

            mockMvc().perform(post("/animals/{animalId}/tutors/{ownerId}/transfer-holder", ANIMAL_ID, OWNER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.holder").value(true));

            verify(petTutorService).transferHolder(ANIMAL_ID, OWNER_ID);
        }
    }

    @Nested
    @DisplayName("PetTutorInviteController")
    class PetTutorInviteControllerTest {

        @Mock
        private PetTutorService petTutorService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new PetTutorInviteController(petTutorService)).build();
            }
            return mockMvc;
        }

        /**
         * O token e String, e nao UUID: e opaco de proposito, entao a rota nao pode
         * exigir formato de UUID nem revelar o formato interno.
         */
        @Test
        @DisplayName("POST /pet-tutor-invites/{token}/accept deve repassar o token cru")
        void aceitarRepassaOTokenCru() throws Exception {
            when(petTutorService.accept("token-opaco-qualquer")).thenReturn(
                    PetTutorResponseDTO.builder().animalId(UUID.randomUUID()).build());

            mockMvc().perform(post("/pet-tutor-invites/{token}/accept", "token-opaco-qualquer"))
                    .andExpect(status().isCreated());

            verify(petTutorService).accept("token-opaco-qualquer");
        }
    }
}
