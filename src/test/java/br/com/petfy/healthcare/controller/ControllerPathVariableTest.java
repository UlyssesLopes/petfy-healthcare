package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.service.ClinicService;
import br.com.petfy.healthcare.service.OwnerService;
import br.com.petfy.healthcare.service.PetService;
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
    @DisplayName("PetController")
    class PetControllerTest {

        @Mock
        private PetService petService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new PetController(petService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /pets/{petId} deve repassar o id da rota para o service")
        void getDeveRepassarIdDaRota() throws Exception {
            when(petService.getPetById(ID)).thenReturn(PetResponseDTO.builder().petId(ID).name("Rex").build());

            mockMvc().perform(get("/pets/{petId}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.petId").value(ID.toString()))
                    .andExpect(jsonPath("$.name").value("Rex"));

            verify(petService).getPetById(ID);
        }

        @Test
        @DisplayName("DELETE /pets/{petId} deve repassar o id da rota e responder 204")
        void deleteDeveRepassarIdDaRota() throws Exception {
            mockMvc().perform(delete("/pets/{petId}", ID))
                    .andExpect(status().isNoContent());

            verify(petService).deletePet(ID);
        }
    }

    @Nested
    @DisplayName("OwnerController")
    class OwnerControllerTest {

        @Mock
        private OwnerService ownerService;

        private MockMvc mockMvc;

        private MockMvc mockMvc() {
            if (mockMvc == null) {
                mockMvc = MockMvcBuilders.standaloneSetup(new OwnerController(ownerService)).build();
            }
            return mockMvc;
        }

        @Test
        @DisplayName("GET /owners/{ownerId} deve repassar o id da rota para o service")
        void getDeveRepassarIdDaRota() throws Exception {
            when(ownerService.getOwnerById(ID)).thenReturn(OwnerResponseDTO.builder().ownerId(ID).name("Ulysses").build());

            mockMvc().perform(get("/owners/{ownerId}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ownerId").value(ID.toString()));

            verify(ownerService).getOwnerById(ID);
        }

        @Test
        @DisplayName("DELETE /owners/{ownerId} deve repassar o id da rota e responder 204")
        void deleteDeveRepassarIdDaRota() throws Exception {
            mockMvc().perform(delete("/owners/{ownerId}", ID))
                    .andExpect(status().isNoContent());

            verify(ownerService).deleteOwner(ID);
        }

        @Test
        @DisplayName("GET /owners/all nao deve colidir com a rota de busca por id")
        void listagemNaoDeveColidirComBuscaPorId() throws Exception {
            when(ownerService.listAllOwners()).thenReturn(java.util.List.of());

            mockMvc().perform(get("/owners/all"))
                    .andExpect(status().isOk());

            verify(ownerService).listAllOwners();
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
}
