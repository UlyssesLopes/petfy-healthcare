package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O fluxo de co-tutor contra Postgres de verdade.
 *
 * Existe porque a parte perigosa do 8b nao e regra de negocio, e sim <b>ordem de
 * comandos</b>: a V15 garante exatamente um HOLDER por animal com indice unico
 * parcial, e toda troca de titularidade passa por um instante em que dois
 * candidatos existem. Mock nao tem indice, e foi assim que a exclusao de conta
 * quebrou antes - ver PersonDeletionContainerTest.
 *
 * O convite tambem trouxe chaves estrangeiras novas para animals e persons, que so
 * recusam o delete no banco.
 */
@SpringBootTest
@Transactional
@DisplayName("fluxo de co-tutor contra Postgres real")
class PetTutorFlowContainerTest extends PostgresContainerTest {

    @Autowired private PetTutorService petTutorService;
    @Autowired private AnimalService animalService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private PetTutorInviteRepository petTutorInviteRepository;

    private Person ulysses;
    private Person maria;
    private Animal rex;

    @BeforeEach
    void setUp() {
        ulysses = person("ulysses");
        maria = person("maria");

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        vinculo(ulysses, PetTutorRole.HOLDER);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Person person(String prefixo) {
        return personRepository.saveAndFlush(Person.builder()
                .name(prefixo)
                .email(prefixo + "-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private PetTutor vinculo(Person de, PetTutorRole papel) {
        return petTutorRepository.saveAndFlush(PetTutor.builder()
                .animal(rex).person(de).role(papel).creationDate(LocalDateTime.now()).build());
    }

    private void autenticar(Person como) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(como.getEmail(), "n/a", List.of()));
    }

    private String convidar(Person emissor, Person para, PetTutorRole papel) {
        autenticar(emissor);
        return petTutorService.invite(rex.getAnimalId(), PetTutorInviteRequestDTO.builder()
                .email(para.getEmail()).role(papel).build()).getToken();
    }

    private List<PetTutor> tutoresDoRex() {
        return petTutorRepository.findByAnimalAnimalIdOrderByRoleAscCreationDateAsc(rex.getAnimalId());
    }

    private long holdersDoRex() {
        return tutoresDoRex().stream().filter(PetTutor::isHolder).count();
    }

    @Nested
    @DisplayName("convite e aceite")
    class ConviteEAceite {

        @Test
        @DisplayName("co-tutor aceito passa a alcancar o animal, no papel do convite")
        void coTutorAceitoAlcancaOAnimal() {
            String token = convidar(ulysses, maria, PetTutorRole.EDITOR);

            autenticar(maria);
            var vinculo = petTutorService.accept(token);

            assertThat(vinculo.getRole()).isEqualTo(PetTutorRole.EDITOR);
            assertThat(vinculo.getAnimalId()).isEqualTo(rex.getAnimalId());
            assertThat(animalRepository.findByTutorsPersonPersonId(maria.getPersonId()))
                    .extracting(Animal::getName).containsExactly("Rex");
        }

        /** Uso unico: o mesmo token nao entra duas vezes. */
        @Test
        @DisplayName("o mesmo convite nao pode ser aceito duas vezes")
        void conviteNaoAceitaDuasVezes() {
            String token = convidar(ulysses, maria, PetTutorRole.EDITOR);

            autenticar(maria);
            petTutorService.accept(token);

            assertThatThrownBy(() -> petTutorService.accept(token))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                            HttpStatus.NOT_FOUND);
        }

        /**
         * O e-mail trava o destinatario: sem isso o link viraria portador, e quem o
         * recebesse encaminhado entraria no historico de saude de um animal alheio.
         */
        @Test
        @DisplayName("quem nao e o destinatario nao aceita, e recebe a mesma resposta de token invalido")
        void terceiroNaoAceita() {
            String token = convidar(ulysses, maria, PetTutorRole.EDITOR);
            Person estranho = person("estranho");

            autenticar(estranho);

            assertThatThrownBy(() -> petTutorService.accept(token))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode());

            assertThat(animalRepository.findByTutorsPersonPersonId(estranho.getPersonId())).isEmpty();
        }

        @Test
        @DisplayName("convite revogado nao e aceito")
        void conviteRevogadoNaoEAceito() {
            String token = convidar(ulysses, maria, PetTutorRole.EDITOR);
            UUID inviteId = petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(rex.getAnimalId())
                    .get(0).getPetTutorInviteId();

            autenticar(ulysses);
            petTutorService.revokeInvite(rex.getAnimalId(), inviteId);

            autenticar(maria);
            assertThatThrownBy(() -> petTutorService.accept(token))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode());
        }

        /** O token nao fica guardado: a tabela tem o hash, e ele nao e o token. */
        @Test
        @DisplayName("o convite guarda hash, nao o token")
        void conviteGuardaHash() {
            String token = convidar(ulysses, maria, PetTutorRole.EDITOR);

            assertThat(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(rex.getAnimalId()))
                    .singleElement()
                    .satisfies(invite -> assertThat(invite.getTokenHash()).isNotBlank().isNotEqualTo(token));
        }

        @Test
        @DisplayName("nao se convida quem ja e tutor do animal")
        void naoConvidaQuemJaETutor() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(ulysses);

            var request = PetTutorInviteRequestDTO.builder()
                    .email(maria.getEmail()).role(PetTutorRole.EDITOR).build();

            assertThatThrownBy(() -> petTutorService.invite(rex.getAnimalId(), request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ALREADY_A_TUTOR.getCode(), HttpStatus.CONFLICT);
        }
    }

    /**
     * O nucleo do risco. Cada caso aqui passaria em mock e falharia no banco se a
     * ordem rebaixa-depois-promove fosse invertida.
     */
    @Nested
    @DisplayName("titularidade")
    class Titularidade {

        @Test
        @DisplayName("convite HOLDER aceito troca o titular sem violar o indice")
        void conviteHolderTrocaOTitular() {
            String token = convidar(ulysses, maria, PetTutorRole.HOLDER);

            autenticar(maria);
            var novoVinculo = petTutorService.accept(token);
            petTutorRepository.flush();

            assertThat(novoVinculo.getRole()).isEqualTo(PetTutorRole.HOLDER);
            assertThat(holdersDoRex()).isEqualTo(1);

            // quem transferiu continua enxergando a carteira, agora como EDITOR
            assertThat(petTutorRepository.findByAnimalAnimalIdAndPersonPersonId(rex.getAnimalId(), ulysses.getPersonId()))
                    .get()
                    .satisfies(antigo -> assertThat(antigo.getRole()).isEqualTo(PetTutorRole.EDITOR));
        }

        @Test
        @DisplayName("transferir para quem ja e tutor troca os dois papeis")
        void transferirParaTutorExistente() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(ulysses);

            var resultado = petTutorService.transferHolder(rex.getAnimalId(), maria.getPersonId());
            petTutorRepository.flush();

            assertThat(resultado.getRole()).isEqualTo(PetTutorRole.HOLDER);
            assertThat(resultado.getPersonId()).isEqualTo(maria.getPersonId());
            assertThat(holdersDoRex()).isEqualTo(1);
            assertThat(petTutorRepository.findByAnimalAnimalIdAndPersonPersonId(rex.getAnimalId(), ulysses.getPersonId()))
                    .get()
                    .satisfies(antigo -> assertThat(antigo.getRole()).isEqualTo(PetTutorRole.EDITOR));
        }

        /**
         * Depois de transferir, quem passou a titularidade perde o que so o titular
         * faz - inclusive transferir de volta por conta propria.
         */
        @Test
        @DisplayName("quem transferiu deixa de poder transferir de volta")
        void quemTransferiuPerdeOPoder() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(ulysses);
            petTutorService.transferHolder(rex.getAnimalId(), maria.getPersonId());
            petTutorRepository.flush();

            assertThatThrownBy(() -> petTutorService.transferHolder(rex.getAnimalId(), ulysses.getPersonId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode(), HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("transferir para o proprio titular nao muda nada")
        void transferirParaSiMesmoNaoMudaNada() {
            autenticar(ulysses);

            var resultado = petTutorService.transferHolder(rex.getAnimalId(), ulysses.getPersonId());

            assertThat(resultado.getRole()).isEqualTo(PetTutorRole.HOLDER);
            assertThat(holdersDoRex()).isEqualTo(1);
        }

        @Test
        @DisplayName("transferir para quem nao e tutor responde TUTOR_NOT_FOUND")
        void transferirParaNaoTutor() {
            autenticar(ulysses);

            assertThatThrownBy(() -> petTutorService.transferHolder(rex.getAnimalId(), maria.getPersonId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("papel e saida")
    class PapelESaida {

        @Test
        @DisplayName("titular troca o papel do co-tutor entre EDITOR e VIEWER")
        void titularTrocaOPapel() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(ulysses);

            var resultado = petTutorService.changeRole(rex.getAnimalId(), maria.getPersonId(),
                    PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.EDITOR).build());

            assertThat(resultado.getRole()).isEqualTo(PetTutorRole.EDITOR);
        }

        @Test
        @DisplayName("promover a HOLDER pelo PATCH e recusado, apontando a transferencia")
        void patchNaoPromoveAHolder() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(ulysses);

            var request = PetTutorRoleUpdateRequestDTO.builder().role(PetTutorRole.HOLDER).build();

            assertThatThrownBy(() -> petTutorService.changeRole(rex.getAnimalId(), maria.getPersonId(), request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                            HttpStatus.CONFLICT);

            assertThat(holdersDoRex()).isEqualTo(1);
        }

        @Test
        @DisplayName("co-tutor sai do animal por conta propria")
        void coTutorSaiSozinho() {
            vinculo(maria, PetTutorRole.EDITOR);
            autenticar(maria);

            petTutorService.removeTutor(rex.getAnimalId(), maria.getPersonId());
            petTutorRepository.flush();

            assertThat(animalRepository.findByTutorsPersonPersonId(maria.getPersonId())).isEmpty();
            assertThat(animalRepository.findById(rex.getAnimalId())).isPresent();
        }

        @Test
        @DisplayName("co-tutor nao remove outro co-tutor")
        void coTutorNaoRemoveOutro() {
            vinculo(maria, PetTutorRole.EDITOR);
            Person joao = person("joao");
            vinculo(joao, PetTutorRole.EDITOR);

            autenticar(maria);

            assertThatThrownBy(() -> petTutorService.removeTutor(rex.getAnimalId(), joao.getPersonId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode(), HttpStatus.FORBIDDEN);
        }

        /**
         * Nem o proprio titular sai por aqui: o indice exige exatamente um HOLDER, e
         * um animal sem titular ficaria sem ninguem que pudesse convidar ou apaga-lo.
         */
        @Test
        @DisplayName("o titular nao se remove: transfere primeiro ou apaga o animal")
        void titularNaoSeRemove() {
            autenticar(ulysses);

            assertThatThrownBy(() -> petTutorService.removeTutor(rex.getAnimalId(), ulysses.getPersonId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getCode(), HttpStatus.CONFLICT);

            assertThat(holdersDoRex()).isEqualTo(1);
        }

        @Test
        @DisplayName("quem nao alcanca o animal recebe 404 na lista de tutores")
        void estranhoNaoVeALista() {
            Person estranho = person("estranho");
            autenticar(estranho);

            assertThatThrownBy(() -> petTutorService.listTutors(rex.getAnimalId()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }

        /** Saber quem alcanca o animal e parte da privacidade, inclusive para o VIEWER. */
        @Test
        @DisplayName("o co-tutor VIEWER ve quem mais alcanca o animal")
        void viewerVeALista() {
            vinculo(maria, PetTutorRole.VIEWER);
            autenticar(maria);

            assertThat(petTutorService.listTutors(rex.getAnimalId()))
                    .hasSize(2)
                    .extracting("role")
                    .containsExactly(PetTutorRole.HOLDER, PetTutorRole.VIEWER);
        }
    }

    /**
     * O convite trouxe chaves estrangeiras novas para animals e persons. Como o schema
     * nao tem ON DELETE CASCADE em lugar nenhum, quem recusa o delete e o banco - e
     * so aqui isso aparece.
     */
    @Nested
    @DisplayName("convite pendente nao pode travar exclusao")
    class ConvitePendenteEExclusao {

        @Test
        @DisplayName("apagar o animal com convite pendente funciona")
        void apagarAnimalComConvitePendente() {
            convidar(ulysses, maria, PetTutorRole.EDITOR);

            autenticar(ulysses);
            animalService.deleteAnimal(rex.getAnimalId());
            animalRepository.flush();

            assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
            assertThat(petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(rex.getAnimalId())).isEmpty();
        }
    }

}
