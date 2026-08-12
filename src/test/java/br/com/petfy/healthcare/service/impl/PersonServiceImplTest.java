package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.dto.PersonRequestDTO;
import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.ConsentRecordRepository;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.OrganizationInviteService;
import br.com.petfy.healthcare.service.OrganizationService;
import br.com.petfy.healthcare.service.ConsentService;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.AnimalPurger;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ProfessionalCredentialRepository credentialRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationInviteService organizationInviteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CustodyRepository custodyRepository;

    @Mock
    private GrantRepository grantRepository;

    @Mock
    private AnimalPurger animalPurger;

    @Mock
    private ConsentService consentService;

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @Mock
    private br.com.petfy.healthcare.domain.repository.DueItemSilenceRepository dueItemSilenceRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private PetTutorInviteRepository petTutorInviteRepository;

    @Mock
    private br.com.petfy.healthcare.domain.repository.ReferralRepository referralRepository;

    @Mock
    private br.com.petfy.healthcare.domain.repository.SponsorshipRepository sponsorshipRepository;

    @Mock
    private br.com.petfy.healthcare.domain.repository.GroupApprovalRepository groupApprovalRepository;

    @InjectMocks
    private PersonServiceImpl personService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String HASH = "$2a$10$hashDeMentiraParaOTeste";

    /**
     * Uma concessao a pessoa. Depois do P2b o co-tutor nao e mais uma linha da
     * mesma tabela do titular - e concessao, e o helper reflete isso.
     */
    private Grant concessaoPara(Person person, GrantLevel nivel) {
        return Grant.builder()
                .grantId(UUID.randomUUID())
                .granteePerson(person)
                .level(nivel)
                .grantedAt(LocalDateTime.now())
                .build();
    }

    private Person existingPerson() {
        return Person.builder()
                .personId(OWNER_ID)
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .password("senha-atual")
                .phone("11999999999")
                .address("Rua A, 100")
                .creationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createPerson")
    class CreatePerson {

        @Test
        @DisplayName("deve persistir o person com os dados do request e data de criacao")
        void devePersistirPersonComDadosDoRequest() {
            var request = new PersonRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", "11999999999", "Rua A, 100", true, null, null, null, null);
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(personRepository.save(any(Person.class))).thenReturn(existingPerson());

            var result = personService.createPerson(request);

            assertThat(result.getPersonId()).isEqualTo(OWNER_ID);
            assertThat(result.getEmail()).isEqualTo("ulysses@petfy.com.br");

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Ulysses");
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve persistir a senha em texto puro")
        void naoDevePersistirSenhaEmTextoPuro() {
            var request = new PersonRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null, true, null, null, null, null);
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(personRepository.save(any(Person.class))).thenReturn(existingPerson());

            personService.createPerson(request);

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword())
                    .isEqualTo(HASH)
                    .isNotEqualTo("s3nhaForte");
            verify(passwordEncoder).encode("s3nhaForte");
        }

        @Test
        @DisplayName("o response de person nao deve carregar o campo password")
        void responseNaoDeveCarregarPassword() {
            assertThat(PersonResponseDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("password");
        }

        /**
         * Sem esta checagem, o cadastro duplicado bate no UNIQUE do banco e sai
         * como 500 opaco, escondendo do cliente que o problema e o e-mail.
         */
        @Test
        @DisplayName("deve recusar com 409 quando o e-mail ja pertence a outro person")
        void deveRecusarQuandoEmailJaUsadoPorPerson() {
            var request = new PersonRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null, true, null, null, null, null);
            when(personRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(true);

            assertThatThrownBy(() -> personService.createPerson(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage());

            verify(personRepository, org.mockito.Mockito.never()).save(any());
            verify(emailVerificationService, org.mockito.Mockito.never()).sendVerification(any());
        }

        /** Person e vet compartilham o mesmo namespace de e-mail. */
        @Test
        @DisplayName("deve recusar quando o e-mail ja pertence a um vet")
        void deveRecusarQuandoEmailJaUsadoPorVet() {
            var request = new PersonRequestDTO("Ulysses", "ulysses@petfy.com.br", "s3nhaForte", null, null, true, null, null, null, null);
            when(personRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(false);
            when(personRepository.existsByEmail("ulysses@petfy.com.br")).thenReturn(true);

            assertThatThrownBy(() -> personService.createPerson(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage());

            verify(personRepository, org.mockito.Mockito.never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentPerson")
    class GetCurrentPerson {

        @Test
        @DisplayName("deve devolver o person autenticado, sem receber id de fora")
        void deveDevolverPersonAutenticado() {
            when(currentPersonProvider.require()).thenReturn(existingPerson());

            var result = personService.getCurrentPerson();

            assertThat(result.getPersonId()).isEqualTo(OWNER_ID);
            assertThat(result.getName()).isEqualTo("Ulysses");
        }
    }

    @Nested
    @DisplayName("updateCurrentPerson")
    class UpdateCurrentPerson {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(currentPersonProvider.require()).thenReturn(existingPerson());
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

            var request = new PersonRequestDTO(null, null, null, "11888888888", null, true, null, null, null, null);
            var result = personService.updateCurrentPerson(request);

            assertThat(result.getPhone()).isEqualTo("11888888888");
            assertThat(result.getName()).isEqualTo("Ulysses");
            assertThat(result.getEmail()).isEqualTo("ulysses@petfy.com.br");
            assertThat(result.getAddress()).isEqualTo("Rua A, 100");
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve alterar a senha - troca de senha pede endpoint proprio")
        void naoDeveAlterarSenha() {
            when(currentPersonProvider.require()).thenReturn(existingPerson());
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

            personService.updateCurrentPerson(new PersonRequestDTO(null, null, "nova-senha", null, null, true, null, null, null, null));

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("senha-atual");
            verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
        }

        @Test
        @DisplayName("deve atualizar sempre o person do token, nunca um id vindo do payload")
        void deveAtualizarSempreOPersonDoToken() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

            personService.updateCurrentPerson(new PersonRequestDTO("Outro Nome", null, null, null, null, true, null, null, null, null));

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
            assertThat(captor.getValue().getPersonId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("deve gravar o hash da nova senha quando a atual confere")
        void deveGravarHashDaNovaSenha() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true);
            when(passwordEncoder.matches("s3nhaNova", "senha-atual")).thenReturn(false);
            when(passwordEncoder.encode("s3nhaNova")).thenReturn(HASH);
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

            personService.changePassword(new PasswordChangeRequestDTO("senha-atual-em-claro", "s3nhaNova"));

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
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
            when(currentPersonProvider.require()).thenReturn(existingPerson());
            when(passwordEncoder.matches("chute", "senha-atual")).thenReturn(false);

            assertThatThrownBy(() -> personService.changePassword(
                    new PasswordChangeRequestDTO("chute", "s3nhaNova")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getMessage());

            verify(personRepository, org.mockito.Mockito.never()).save(any());
            verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
        }

        /**
         * Trocar a senha por ela mesma passaria como sucesso e daria a quem esta
         * reagindo a um vazamento a impressao de ter rodado a credencial.
         */
        @Test
        @DisplayName("deve recusar quando a nova senha e igual a atual")
        void deveRecusarQuandoNovaSenhaEIgualAAtual() {
            when(currentPersonProvider.require()).thenReturn(existingPerson());
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true, true);

            assertThatThrownBy(() -> personService.changePassword(
                    new PasswordChangeRequestDTO("senha-atual-em-claro", "senha-atual-em-claro")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getMessage());

            verify(personRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("deve trocar sempre a senha do person do token")
        void deveTrocarSempreSenhaDoPersonDoToken() {
            when(currentPersonProvider.require()).thenReturn(existingPerson());
            when(passwordEncoder.matches("senha-atual-em-claro", "senha-atual")).thenReturn(true);
            when(passwordEncoder.matches("s3nhaNova", "senha-atual")).thenReturn(false);
            when(passwordEncoder.encode("s3nhaNova")).thenReturn(HASH);
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

            personService.changePassword(new PasswordChangeRequestDTO("senha-atual-em-claro", "s3nhaNova"));

            var captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(captor.capture());
            assertThat(captor.getValue().getPersonId()).isEqualTo(OWNER_ID);
        }
    }

    @Nested
    @DisplayName("deleteCurrentPerson")
    class DeleteCurrentPerson {

        /**
         * A ordem importa e nao e detalhe de implementacao: as correcoes apontam
         * para vacina e historico, esses apontam para animal, animal aponta para person,
         * e os tokens apontam para person tambem. Apagar fora da ordem faz o banco
         * recusar por violacao de chave estrangeira. Este teste com mock nao pega
         * isso sozinho - quem pega e o PersonDeletionContainerTest, contra Postgres
         * de verdade - mas garante que a sequencia nao seja alterada por engano.
         */
        @Test
        @DisplayName("animal sem outro tutor morre junto, na ordem netas -> filhas -> animals")
        void animalSemOutroTutorMorreJunto() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);

            // SEM CUSTODIA EM CURSO: desde a Tela 36 a conta so encerra quando ninguem depende dela,
            // e a ordem que este caso guarda so acontece nesse caminho
            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of());

            personService.deleteCurrentPerson();

            // A sequencia de deletes do animal saiu daqui para o AnimalPurger, porque ela
            // era identica a do DELETE /animals/{id} e as duas divergiram. O que este
            // teste guarda e a posicao do purge no fluxo da conta; a ordem interna
            // dele esta no AnimalPurgerTest, e a recusa do banco no
            // AnimalDeletionContainerTest.
            var ordem = inOrder(custodyRepository, animalPurger,
                    passwordResetTokenRepository, emailVerificationTokenRepository, personRepository);

            // as custodias saem primeiro: seguram animal e person ao mesmo tempo
            ordem.verify(custodyRepository).deleteAll(any());
            // sem custodia nao ha animal a apagar: a lista vai vazia, e o purge sai cedo
            ordem.verify(animalPurger).purge(List.of());
            ordem.verify(passwordResetTokenRepository).deleteByPersonPersonId(OWNER_ID);
            ordem.verify(emailVerificationTokenRepository).deleteByPersonPersonId(OWNER_ID);
            ordem.verify(personRepository).delete(autenticado);
        }

        /**
         * O pedido de exclusao de um tutor nao autoriza destruir o historico de
         * saude de um animal que continua tendo quem responda por ele.
         */
        @Test
        @DisplayName("animal com outro tutor sobrevive: nada dele e apagado")
        void animalComOutroTutorSobrevive() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);

            // quem sai alcanca o animal por concessao, e nao por custodia: nao responde
            // por animal nenhum, entao nao ha animal para morrer nem sucessor a abrir
            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of());


            personService.deleteCurrentPerson();

            verify(custodyRepository).deleteAll(any());
            // o purge roda com lista vazia: nenhum animal morre, e o convite de
            // terceiro para este animal nao e desta conta - segue valendo para quem ficou
            verify(animalPurger).purge(List.of());
            verify(personRepository).delete(autenticado);
        }

        /**
         * Convite aponta para o animal, para quem convidou e para quem aceitou. Como o
         * schema nao tem ON DELETE CASCADE, cada uma dessas tres FKs segura um dos
         * deletes seguintes - e nenhuma delas aparece em teste de mock por si. O que
         * este caso trava e a ordem; a recusa de verdade esta no
         * PersonDeletionContainerTest.
         */
        @Test
        @DisplayName("os convites da conta saem antes dos vinculos e dos animals")
        void convitesSaemAntesDosVinculosEDosAnimals() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);

            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of());

            personService.deleteCurrentPerson();

            var ordem = inOrder(petTutorInviteRepository, custodyRepository, animalPurger, personRepository);
            ordem.verify(petTutorInviteRepository).deleteByCreatedByPersonId(OWNER_ID);
            ordem.verify(petTutorInviteRepository).deleteByAcceptedByPersonId(OWNER_ID);
            ordem.verify(custodyRepository).deleteAll(any());
            // sem custodia nao ha animal a apagar: a lista vai vazia, e o purge sai cedo
            ordem.verify(animalPurger).purge(List.of());
            ordem.verify(personRepository).delete(autenticado);
        }

        /**
         * <b>NINGUEM HERDA MAIS, e o que este caso substitui era justamente a heranca.</b>
         *
         * Ate a Tela 36 o titular que saia via a titularidade passar sozinha para o tutor mais antigo
         * — e a pessoa promovida descobria depois que passara a responder por um animal. O produto
         * (3.4) decidiu que o destino e escolhido por quem sai, e nao inferido pelo sistema.
         *
         * <b>A regra de indice que o caso antigo protegia continua valendo em outro lugar:</b> a
         * transferencia de titularidade e a hospedagem passam custodia adiante, e as duas encerram
         * antes de abrir, com flush no meio.
         */
        @Test
        @DisplayName("ninguem e promovido: o encerramento recusa em vez de escolher sucessor")
        void ninguemEPromovido() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);

            var animal = Animal.builder().animalId(ANIMAL_ID).name("Rex").build();

            var meuVinculo = Custodias.emCurso(autenticado);
            meuVinculo.setAnimal(animal);

            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of(meuVinculo));

            assertThatThrownBy(() -> personService.deleteCurrentPerson())
                    .isInstanceOf(PetfyHealthcareException.class);

            verify(custodyRepository, never()).save(any(Custody.class));
            verify(animalPurger, never()).purge(any());
            verify(personRepository, never()).delete(any());
        }

        /** Co-tutor que sai nao mexe em titularidade: nao ha o que herdar. */
        @Test
        @DisplayName("co-tutor que sai nao promove ninguem")
        void coTutorQueSaiNaoPromoveNinguem() {
            var autenticado = existingPerson();
            when(currentPersonProvider.require()).thenReturn(autenticado);

            // quem sai alcanca o animal por concessao, e nao por custodia: nao responde
            // por animal nenhum, entao nao ha animal para morrer nem sucessor a abrir
            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of());


            personService.deleteCurrentPerson();

            verify(custodyRepository).deleteAll(any());
            verify(custodyRepository, never()).save(any(Custody.class));
            
        }
    }
}
