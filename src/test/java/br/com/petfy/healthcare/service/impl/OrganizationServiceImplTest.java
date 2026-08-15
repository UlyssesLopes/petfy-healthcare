package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
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
import org.springframework.http.HttpStatus;

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
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private CurrentProfessionalProvider currentProfessionalProvider;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    /** Quem cria a organizacao, e que precisa sair de la com vinculo. */
    private static final Person QUEM_CRIOU = Person.builder()
            .personId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .email("dona@creche.com.br")
            .build();

    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");

    /** Person cuja clinica e a informada - quem tem permissao de manter o cadastro. */
    private void autenticadoComoVetDa(UUID organizationId) {
        var pessoa = Person.builder().personId(UUID.randomUUID()).build();
        org.mockito.Mockito.lenient().when(currentProfessionalProvider.require()).thenReturn(pessoa);
        when(currentProfessionalProvider.requireContext()).thenReturn(
                br.com.petfy.healthcare.Contextos.por(pessoa,
                        Organization.builder().organizationId(organizationId).build()));
    }

    private Organization existingOrganization() {
        return Organization.builder()
                .organizationId(CLINIC_ID)
                .name("Clinica Bicho Feliz")
                .ownerVetName("Dra. Marina")
                .phone("1133334444")
                .email("contato@bichofeliz.com.br")
                .cnpj("12345678000199")
                .address("Av. Central, 500")
                .city("Sao Paulo")
                .state("SP")
                .cep("01000000")
                .description("Clinica geral")
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createOrganization")
    class CreateOrganization {

        @Test
        @DisplayName("criar clinica nao deve exigir ser veterinario - o tutor precisa registrar onde vacinou")
        void criarNaoDeveExigirSerVeterinario() {
            when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrganization());
            when(currentPersonProvider.require()).thenReturn(QUEM_CRIOU);

            organizationService.createOrganization(OrganizationRequestDTO.builder().name("Clinica Bicho Feliz").build());

            /*
             * O `require()` do provider PROFISSIONAL exige credencial ativa e responde 403 sem
             * ela. Quem cria a organizacao pode ser o dono da creche sem CRMV nenhum, entao o
             * caminho continua sendo o provider de PESSOA — e este verify e o que impede alguem
             * de trocar um pelo outro sem perceber que fechou a porta para o tutor.
             */
            verify(currentProfessionalProvider, never()).require();
            verify(currentProfessionalProvider, never()).requireContext();
        }

        @Test
        @DisplayName("deve gravar o vinculo de quem criou, como ADMINISTRADOR - senao a organizacao nasce inalcancavel")
        void deveGravarVinculoDeQuemCriou() {
            when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrganization());
            when(currentPersonProvider.require()).thenReturn(QUEM_CRIOU);

            organizationService.createOrganization(OrganizationRequestDTO.builder().name("Clinica Bicho Feliz").build());

            var captor = ArgumentCaptor.forClass(Membership.class);
            verify(membershipRepository).save(captor.capture());

            var vinculo = captor.getValue();
            assertThat(vinculo.getPerson()).isEqualTo(QUEM_CRIOU);
            assertThat(vinculo.getOrganization().getOrganizationId()).isEqualTo(CLINIC_ID);
            assertThat(vinculo.getRole()).isEqualTo(MembershipRole.ADMINISTRADOR);

            /*
             * ATIVO, e e isso que o contexto le: `findAtivosDaPessoa` filtra por `leftAt` nulo, e
             * um vinculo que nascesse encerrado deixaria a organizacao fora do "Agindo como" —
             * exatamente o defeito que este teste existe para impedir de voltar.
             */
            assertThat(vinculo.getJoinedAt()).isNotNull();
            assertThat(vinculo.estaAtivo()).isTrue();
        }

        @Test
        @DisplayName("deve persistir a clinica com os dados do request e data de criacao")
        void devePersistirClinica() {
            var request = OrganizationRequestDTO.builder()
                    .name("Clinica Bicho Feliz")
                    .ownerVetName("Dra. Marina")
                    .cnpj("12345678000199")
                    .city("Sao Paulo")
                    .build();
            when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrganization());
            when(currentPersonProvider.require()).thenReturn(QUEM_CRIOU);

            var result = organizationService.createOrganization(request);

            assertThat(result.getOrganizationId()).isEqualTo(CLINIC_ID);
            assertThat(result.getName()).isEqualTo("Clinica Bicho Feliz");

            var captor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(captor.capture());
            assertThat(captor.getValue().getCnpj()).isEqualTo("12345678000199");
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getOrganizationById")
    class GetOrganizationById {

        @Test
        @DisplayName("deve retornar a clinica quando existe")
        void deveRetornarClinicaQuandoExiste() {
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingOrganization()));

            var result = organizationService.getOrganizationById(CLINIC_ID);

            assertThat(result.getOrganizationId()).isEqualTo(CLINIC_ID);
            assertThat(result.getOwnerVetName()).isEqualTo("Dra. Marina");
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.getOrganizationById(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Organization not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(103, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listAllOrganizations")
    class ListAllOrganizations {

        @Test
        @DisplayName("deve mapear todas as clinicas retornadas pelo repositorio")
        void deveMapearTodasAsClinicas() {
            var pageable = PageRequest.of(0, 20);
            when(organizationRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(existingOrganization())));

            assertThat(organizationService.listAllOrganizations(pageable).getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateOrganization")
    class UpdateOrganization {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingOrganization()));
            autenticadoComoVetDa(CLINIC_ID);
            when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

            var request = OrganizationRequestDTO.builder().phone("1155556666").build();
            var result = organizationService.updateOrganization(CLINIC_ID, request);

            assertThat(result.getPhone()).isEqualTo("1155556666");
            assertThat(result.getName()).isEqualTo("Clinica Bicho Feliz");
            assertThat(result.getCnpj()).isEqualTo("12345678000199");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("deve responder 403 quando quem altera e vet de outra clinica")
        void deveResponder403QuandoVetDeOutraClinica() {
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.of(existingOrganization()));
            autenticadoComoVetDa(OUTRA_CLINIC_ID);

            assertThatThrownBy(() -> organizationService.updateOrganization(CLINIC_ID,
                    OrganizationRequestDTO.builder().name("Invadida").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(109, HttpStatus.FORBIDDEN);

            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(organizationRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.updateOrganization(CLINIC_ID, OrganizationRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Organization not found");

            verify(organizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteOrganization")
    class DeleteOrganization {

        @Test
        @DisplayName("deve remover a clinica quando o vet e dela")
        void deveRemoverQuandoExiste() {
            when(organizationRepository.existsById(CLINIC_ID)).thenReturn(true);
            autenticadoComoVetDa(CLINIC_ID);

            organizationService.deleteOrganization(CLINIC_ID);

            verify(organizationRepository).deleteById(CLINIC_ID);
        }

        @Test
        @DisplayName("deve responder 403 quando o vet e de outra clinica")
        void deveResponder403QuandoVetDeOutraClinica() {
            when(organizationRepository.existsById(CLINIC_ID)).thenReturn(true);
            autenticadoComoVetDa(OUTRA_CLINIC_ID);

            assertThatThrownBy(() -> organizationService.deleteOrganization(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Only a vet from this organization can do that")
                    .extracting("code", "httpStatus")
                    .containsExactly(109, HttpStatus.FORBIDDEN);

            verify(organizationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(organizationRepository.existsById(CLINIC_ID)).thenReturn(false);

            assertThatThrownBy(() -> organizationService.deleteOrganization(CLINIC_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Organization not found");

            verify(organizationRepository, never()).deleteById(any());
        }
    }
}
