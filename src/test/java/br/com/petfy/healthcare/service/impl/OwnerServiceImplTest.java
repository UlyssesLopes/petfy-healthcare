package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.InjectMocks;

import java.lang.reflect.Field;
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
class OwnerServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OwnerServiceImpl ownerService;

    private static final String HASH = "$2a$10$hashDeMentiraParaOTeste";

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Owner existingOwner() {
        return Owner.builder()
                .ownerId(OWNER_ID)
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .password("senha-atual")
                .phone("11999999999")
                .address("Rua A, 100")
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createOwner")
    class CreateOwner {

        @Test
        @DisplayName("deve persistir o owner com os dados do request e data de criacao")
        void devePersistirOwnerComDadosDoRequest() {
            var request = new OwnerRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", "11999999999", "Rua A, 100");
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(ownerRepository.save(any(Owner.class))).thenReturn(existingOwner());

            var result = ownerService.createOwner(request);

            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(result.getEmail()).isEqualTo("ulysses@petfy.com.br");

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Ulysses");
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve persistir a senha em texto puro")
        void naoDevePersistirSenhaEmTextoPuro() {
            var request = new OwnerRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null);
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(ownerRepository.save(any(Owner.class))).thenReturn(existingOwner());

            ownerService.createOwner(request);

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword())
                    .isEqualTo(HASH)
                    .isNotEqualTo("s3nhaForte");
            verify(passwordEncoder).encode("s3nhaForte");
        }

        @Test
        @DisplayName("o response de owner nao deve carregar o campo password")
        void responseNaoDeveCarregarPassword() {
            assertThat(OwnerResponseDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("password");
        }
    }

    @Nested
    @DisplayName("getOwnerById")
    class GetOwnerById {

        @Test
        @DisplayName("deve retornar o owner quando existe")
        void deveRetornarOwnerQuandoExiste() {
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(existingOwner()));

            var result = ownerService.getOwnerById(OWNER_ID);

            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(result.getName()).isEqualTo("Ulysses");
        }

        @Test
        @DisplayName("deve lancar OWNER_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ownerService.getOwnerById(OWNER_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Owner not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(101, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listAllOwners")
    class ListAllOwners {

        @Test
        @DisplayName("deve mapear todos os owners retornados pelo repositorio")
        void deveMapearTodosOsOwners() {
            when(ownerRepository.findAll()).thenReturn(List.of(existingOwner()));

            var result = ownerService.listAllOwners();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwnerId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando nao ha owners")
        void deveRetornarListaVaziaQuandoNaoHaOwners() {
            when(ownerRepository.findAll()).thenReturn(List.of());

            assertThat(ownerService.listAllOwners()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateOwner")
    class UpdateOwner {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            var owner = existingOwner();
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            var request = new OwnerRequestDTO(null, null, null, "11888888888", null);
            var result = ownerService.updateOwner(OWNER_ID, request);

            assertThat(result.getPhone()).isEqualTo("11888888888");
            assertThat(result.getName()).isEqualTo("Ulysses");
            assertThat(result.getEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(result.getAddress()).isEqualTo("Rua A, 100");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve alterar a senha - updateOwner ignora o campo password")
        void naoDeveAlterarSenha() {
            var owner = existingOwner();
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            ownerService.updateOwner(OWNER_ID, new OwnerRequestDTO(null, null, "nova-senha", null, null));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("senha-atual");
        }

        @Test
        @DisplayName("deve lancar OWNER_NOT_FOUND sem tentar salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ownerService.updateOwner(OWNER_ID, new OwnerRequestDTO()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Owner not found");

            verify(ownerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteOwner")
    class DeleteOwner {

        @Test
        @DisplayName("deve remover o owner quando existe")
        void deveRemoverQuandoExiste() {
            when(ownerRepository.existsById(OWNER_ID)).thenReturn(true);

            ownerService.deleteOwner(OWNER_ID);

            verify(ownerRepository).deleteById(OWNER_ID);
        }

        @Test
        @DisplayName("deve lancar OWNER_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(ownerRepository.existsById(OWNER_ID)).thenReturn(false);

            assertThatThrownBy(() -> ownerService.deleteOwner(OWNER_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Owner not found");

            verify(ownerRepository, never()).deleteById(any());
        }
    }
}
