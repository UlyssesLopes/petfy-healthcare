package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private OwnerServiceImpl ownerService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String HASH = "$2a$10$hashDeMentiraParaOTeste";

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
    @DisplayName("getCurrentOwner")
    class GetCurrentOwner {

        @Test
        @DisplayName("deve devolver o owner autenticado, sem receber id de fora")
        void deveDevolverOwnerAutenticado() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());

            var result = ownerService.getCurrentOwner();

            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(result.getName()).isEqualTo("Ulysses");
        }
    }

    @Nested
    @DisplayName("updateCurrentOwner")
    class UpdateCurrentOwner {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            var request = new OwnerRequestDTO(null, null, null, "11888888888", null);
            var result = ownerService.updateCurrentOwner(request);

            assertThat(result.getPhone()).isEqualTo("11888888888");
            assertThat(result.getName()).isEqualTo("Ulysses");
            assertThat(result.getEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(result.getAddress()).isEqualTo("Rua A, 100");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve alterar a senha - troca de senha pede endpoint proprio")
        void naoDeveAlterarSenha() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            ownerService.updateCurrentOwner(new OwnerRequestDTO(null, null, "nova-senha", null, null));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("senha-atual");
            verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
        }

        @Test
        @DisplayName("deve atualizar sempre o owner do token, nunca um id vindo do payload")
        void deveAtualizarSempreOOwnerDoToken() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            ownerService.updateCurrentOwner(new OwnerRequestDTO("Outro Nome", null, null, null, null));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("deve gravar o hash da nova senha quando a atual confere")
        void deveGravarHashDaNovaSenha() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true);
            when(passwordEncoder.matches("s3nhaNova", "senha-atual")).thenReturn(false);
            when(passwordEncoder.encode("s3nhaNova")).thenReturn(HASH);
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            ownerService.changePassword(new PasswordChangeRequestDTO("senha-atual-em-claro", "s3nhaNova"));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo(HASH);
            assertThat(captor.getValue().getUpdateDate()).isNotNull();
        }

        /**
         * Sem exigir a senha atual, um token roubado bastaria para trocar a senha
         * e tomar a conta em definitivo, sem o dono conseguir voltar.
         */
        @Test
        @DisplayName("deve recusar quando a senha atual nao confere, sem gravar nada")
        void deveRecusarQuandoSenhaAtualNaoConfere() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());
            when(passwordEncoder.matches("chute", "senha-atual")).thenReturn(false);

            assertThatThrownBy(() -> ownerService.changePassword(
                    new PasswordChangeRequestDTO("chute", "s3nhaNova")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getMessage());

            verify(ownerRepository, org.mockito.Mockito.never()).save(any());
            verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
        }

        /**
         * Trocar a senha por ela mesma passaria como sucesso e daria a quem esta
         * reagindo a um vazamento a impressao de ter rodado a credencial.
         */
        @Test
        @DisplayName("deve recusar quando a nova senha e igual a atual")
        void deveRecusarQuandoNovaSenhaEIgualAAtual() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true, true);

            assertThatThrownBy(() -> ownerService.changePassword(
                    new PasswordChangeRequestDTO("senha-atual-em-claro", "senha-atual-em-claro")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getMessage());

            verify(ownerRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("deve trocar sempre a senha do owner do token")
        void deveTrocarSempreSenhaDoOwnerDoToken() {
            when(currentOwnerProvider.require()).thenReturn(existingOwner());
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true);
            when(passwordEncoder.matches("s3nhaNova", "senha-atual")).thenReturn(false);
            when(passwordEncoder.encode("s3nhaNova")).thenReturn(HASH);
            when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> i.getArgument(0));

            ownerService.changePassword(new PasswordChangeRequestDTO("senha-atual-em-claro", "s3nhaNova"));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("deleteCurrentOwner")
    class DeleteCurrentOwner {

        @Test
        @DisplayName("deve remover o owner autenticado")
        void deveRemoverOwnerAutenticado() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);

            ownerService.deleteCurrentOwner();

            verify(ownerRepository).delete(autenticado);
        }
    }
}
