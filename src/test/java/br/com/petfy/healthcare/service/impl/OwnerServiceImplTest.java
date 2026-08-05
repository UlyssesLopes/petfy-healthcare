package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetShareRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.EmailVerificationService;
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
import java.util.List;
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
    private VetRepository vetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetTutorRepository petTutorRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private PetShareRepository petShareRepository;

    @Mock
    private PetClinicAccessRepository petClinicAccessRepository;

    @Mock
    private VaccineCorrectionRepository vaccineCorrectionRepository;

    @Mock
    private HealthRecordCorrectionRepository healthRecordCorrectionRepository;

    @InjectMocks
    private OwnerServiceImpl ownerService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
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

        /**
         * Sem esta checagem, o cadastro duplicado bate no UNIQUE do banco e sai
         * como 500 opaco, escondendo do cliente que o problema e o e-mail.
         */
        @Test
        @DisplayName("deve recusar com 409 quando o e-mail ja pertence a outro owner")
        void deveRecusarQuandoEmailJaUsadoPorOwner() {
            var request = new OwnerRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null);
            when(ownerRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(true);

            assertThatThrownBy(() -> ownerService.createOwner(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage());

            verify(ownerRepository, org.mockito.Mockito.never()).save(any());
            verify(emailVerificationService, org.mockito.Mockito.never()).sendVerification(any());
        }

        /** Owner e vet compartilham o mesmo namespace de e-mail. */
        @Test
        @DisplayName("deve recusar quando o e-mail ja pertence a um vet")
        void deveRecusarQuandoEmailJaUsadoPorVet() {
            var request = new OwnerRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null);
            when(ownerRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(false);
            when(vetRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(true);

            assertThatThrownBy(() -> ownerService.createOwner(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage());

            verify(ownerRepository, org.mockito.Mockito.never()).save(any());
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

        /**
         * A ordem importa e nao e detalhe de implementacao: as correcoes apontam
         * para vacina e historico, esses apontam para pet, pet aponta para owner,
         * e os tokens apontam para owner tambem. Apagar fora da ordem faz o banco
         * recusar por violacao de chave estrangeira. Este teste com mock nao pega
         * isso sozinho - quem pega e o OwnerDeletionContainerTest, contra Postgres
         * de verdade - mas garante que a sequencia nao seja alterada por engano.
         */
        @Test
        @DisplayName("pet sem outro tutor morre junto, na ordem netas -> filhas -> pets")
        void petSemOutroTutorMorreJunto() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);

            var pet = Pet.builder().petId(PET_ID).name("Rex").build();
            var vinculo = PetTutores.vinculo(autenticado, PetTutorRole.HOLDER);
            vinculo.setPet(pet);

            when(petTutorRepository.findByOwnerOwnerId(OWNER_ID)).thenReturn(List.of(vinculo));
            when(petTutorRepository.countByPetPetId(PET_ID)).thenReturn(1L);

            ownerService.deleteCurrentOwner();

            var somenteEstePet = List.of(PET_ID);
            var ordem = org.mockito.Mockito.inOrder(
                    petTutorRepository,
                    vaccineCorrectionRepository, healthRecordCorrectionRepository,
                    vaccineRepository, healthRecordRepository,
                    petShareRepository, petClinicAccessRepository,
                    petRepository,
                    passwordResetTokenRepository, emailVerificationTokenRepository,
                    ownerRepository);

            // os vinculos saem primeiro: seguram pet e owner ao mesmo tempo
            ordem.verify(petTutorRepository).deleteByOwnerOwnerId(OWNER_ID);
            ordem.verify(vaccineCorrectionRepository).deleteByVaccinePetPetIdIn(somenteEstePet);
            ordem.verify(healthRecordCorrectionRepository).deleteByHealthRecordPetPetIdIn(somenteEstePet);
            ordem.verify(vaccineRepository).deleteByPetPetIdIn(somenteEstePet);
            ordem.verify(healthRecordRepository).deleteByPetPetIdIn(somenteEstePet);
            ordem.verify(petShareRepository).deleteByPetPetIdIn(somenteEstePet);
            ordem.verify(petClinicAccessRepository).deleteByPetPetIdIn(somenteEstePet);
            ordem.verify(petRepository).deleteByPetIdIn(somenteEstePet);
            ordem.verify(passwordResetTokenRepository).deleteByOwnerOwnerId(OWNER_ID);
            ordem.verify(emailVerificationTokenRepository).deleteByOwnerOwnerId(OWNER_ID);
            ordem.verify(ownerRepository).delete(autenticado);
        }

        /**
         * O pedido de exclusao de um tutor nao autoriza destruir o historico de
         * saude de um pet que continua tendo quem responda por ele.
         */
        @Test
        @DisplayName("pet com outro tutor sobrevive: nada dele e apagado")
        void petComOutroTutorSobrevive() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);

            var pet = Pet.builder().petId(PET_ID).name("Rex").build();
            var meuVinculo = PetTutores.vinculo(autenticado, PetTutorRole.EDITOR);
            meuVinculo.setPet(pet);

            when(petTutorRepository.findByOwnerOwnerId(OWNER_ID)).thenReturn(List.of(meuVinculo));
            when(petTutorRepository.countByPetPetId(PET_ID)).thenReturn(2L);

            ownerService.deleteCurrentOwner();

            verify(petTutorRepository).deleteByOwnerOwnerId(OWNER_ID);
            verify(petRepository, never()).deleteByPetIdIn(any());
            verify(vaccineRepository, never()).deleteByPetPetIdIn(any());
            verify(healthRecordRepository, never()).deleteByPetPetIdIn(any());
            verify(ownerRepository).delete(autenticado);
        }

        /**
         * O indice do banco exige exatamente um HOLDER por pet, entao o titular
         * nao pode simplesmente sumir: alguem herda.
         */
        @Test
        @DisplayName("titular que sai passa a titularidade ao tutor mais antigo")
        void titularQueSaiPassaATitularidade() {
            var autenticado = existingOwner();
            when(currentOwnerProvider.require()).thenReturn(autenticado);

            var pet = Pet.builder().petId(PET_ID).name("Rex").build();

            var meuVinculo = PetTutores.vinculo(autenticado, PetTutorRole.HOLDER);
            meuVinculo.setPet(pet);
            meuVinculo.setCreationDate(LocalDateTime.of(2026, 1, 1, 10, 0));

            var maria = Owner.builder().ownerId(OUTRO_OWNER_ID).name("Maria").build();
            var vinculoDaMaria = PetTutores.vinculo(maria, PetTutorRole.VIEWER);
            vinculoDaMaria.setPet(pet);
            vinculoDaMaria.setCreationDate(LocalDateTime.of(2026, 2, 1, 10, 0));

            when(petTutorRepository.findByOwnerOwnerId(OWNER_ID)).thenReturn(List.of(meuVinculo));
            when(petTutorRepository.countByPetPetId(PET_ID)).thenReturn(2L);
            when(petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(PET_ID))
                    .thenReturn(List.of(meuVinculo, vinculoDaMaria));

            ownerService.deleteCurrentOwner();

            var captor = ArgumentCaptor.forClass(PetTutor.class);
            verify(petTutorRepository).save(captor.capture());
            assertThat(captor.getValue().getOwner().getOwnerId()).isEqualTo(OUTRO_OWNER_ID);
            assertThat(captor.getValue().getRole()).isEqualTo(PetTutorRole.HOLDER);
        }
    }
}
