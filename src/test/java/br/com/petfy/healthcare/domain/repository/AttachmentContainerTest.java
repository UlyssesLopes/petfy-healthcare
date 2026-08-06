package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Attachment;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.storage.AttachmentStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Anexos contra Postgres de verdade.
 *
 * Duas coisas que so o banco recusa: o CHECK que impede um anexo de documentar uma
 * vacina <b>e</b> um atendimento ao mesmo tempo, e as quatro chaves estrangeiras que a
 * tabela trouxe. As FKs sao a mesma familia de bug que travou o {@code DELETE /persons/me}
 * duas vezes e o {@code DELETE /animals/{id}} desde sempre.
 *
 * E uma que nenhum teste de banco pega sozinho: os <b>bytes</b> saindo junto com o animal.
 * O storage nao participa da transacao, e arquivo orfao com laudo dentro e dado pessoal
 * nao apagado.
 */
@SpringBootTest
@DisplayName("anexos contra Postgres real")
class AttachmentContainerTest extends PostgresContainerTest {

    @Autowired private AttachmentRepository attachmentRepository;
    @Autowired private AttachmentStorage attachmentStorage;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private AnimalService animalService;

    private Person ulysses;
    private Animal rex;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.saveAndFlush(Person.builder()
                .name("Ulysses")
                .email("anexos-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(rex).holderPerson(ulysses).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ulysses.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Attachment.AttachmentBuilder anexo() {
        var armazenado = attachmentStorage.store(rex.getAnimalId(),
                new ByteArrayInputStream("%PDF-1.7 laudo".getBytes(StandardCharsets.US_ASCII)));

        return Attachment.builder()
                .animal(rex)
                .originalFilename("laudo.pdf")
                .contentType("application/pdf")
                .sizeBytes(armazenado.sizeBytes())
                .checksumSha256(armazenado.checksumSha256())
                .storageKey(armazenado.storageKey())
                .uploadedBy(ulysses)
                .creationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("o que o banco recusa")
    class OQueOBancoRecusa {

        /**
         * A regra "no maximo um dono especifico" mora num CHECK, e nao numa validacao em
         * codigo: validacao em codigo e contornada por um insert direto, e a partir dai a
         * listagem por vacina e a por atendimento devolvem a mesma linha.
         */
        @Test
        @Transactional
        @DisplayName("anexo nao pode documentar uma vacina e um atendimento ao mesmo tempo")
        void naoPodeTerOsDoisDonos() {
            var vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                    .animal(rex).vaccineName("V10").applicationDate(LocalDate.now())
                    .creationDate(LocalDateTime.now()).build());

            var atendimento = healthRecordRepository.saveAndFlush(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Consulta").eventDate(LocalDate.now())
                    .creationDate(LocalDateTime.now()).build());

            var invalido = anexo().vaccine(vacina).healthRecord(atendimento).build();

            assertThatThrownBy(() -> attachmentRepository.saveAndFlush(invalido))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @Transactional
        @DisplayName("anexo so de vacina e aceito")
        void soDeVacinaEAceito() {
            var vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                    .animal(rex).vaccineName("V10").applicationDate(LocalDate.now())
                    .creationDate(LocalDateTime.now()).build());

            var salvo = attachmentRepository.saveAndFlush(anexo().vaccine(vacina).build());

            assertThat(salvo.getAttachmentId()).isNotNull();
        }

        @Test
        @Transactional
        @DisplayName("anexo do animal, sem vacina nem atendimento, e aceito")
        void doAnimalEmSiEAceito() {
            var salvo = attachmentRepository.saveAndFlush(anexo().build());

            assertThat(salvo.getVaccine()).isNull();
            assertThat(salvo.getHealthRecord()).isNull();
        }

        /** A chave de storage e unica: duas linhas apontando para o mesmo arquivo fariam
         *  o delete de uma quebrar o download da outra. */
        @Test
        @Transactional
        @DisplayName("duas linhas nao podem apontar para a mesma chave de storage")
        void chaveDeStorageEUnica() {
            var primeiro = attachmentRepository.saveAndFlush(anexo().build());

            var duplicado = anexo().storageKey(primeiro.getStorageKey()).build();

            assertThatThrownBy(() -> attachmentRepository.saveAndFlush(duplicado))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    /**
     * O caminho que o {@code AnimalPurgerCoverageContainerTest} garante estar coberto - aqui
     * ele e exercitado de ponta a ponta, incluindo o disco.
     */
    @Nested
    @DisplayName("apagar o animal leva o anexo e o arquivo")
    class ApagarOAnimal {

        @Test
        @DisplayName("a linha e os bytes saem junto com o animal")
        void linhaEBytesSaemJunto() {
            var salvo = attachmentRepository.saveAndFlush(anexo().build());
            String chave = salvo.getStorageKey();

            // o arquivo existe antes
            assertThat(attachmentStorage.read(chave)).isNotNull();

            animalService.deleteAnimal(rex.getAnimalId());

            assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
            assertThat(attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(rex.getAnimalId()))
                    .isEmpty();

            // e o arquivo tambem: laudo orfao no disco e dado pessoal nao apagado
            assertThatThrownBy(() -> attachmentStorage.read(chave))
                    .isInstanceOf(RuntimeException.class);
        }

        /**
         * Anexo pendurado numa vacina: sem sair antes dela, a FK
         * {@code fk_attachments_vaccine} segura o delete da vacina, que por sua vez segura
         * o do animal.
         */
        @Test
        @DisplayName("anexo de vacina nao trava o delete do animal")
        void anexoDeVacinaNaoTrava() {
            var vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                    .animal(rex).vaccineName("V10").applicationDate(LocalDate.now())
                    .creationDate(LocalDateTime.now()).build());

            attachmentRepository.saveAndFlush(anexo().vaccine(vacina).build());

            animalService.deleteAnimal(rex.getAnimalId());

            assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
        }

        @Test
        @DisplayName("anexo de atendimento nao trava o delete do animal")
        void anexoDeAtendimentoNaoTrava() {
            var atendimento = healthRecordRepository.saveAndFlush(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Consulta").eventDate(LocalDate.now())
                    .creationDate(LocalDateTime.now()).build());

            attachmentRepository.saveAndFlush(anexo().healthRecord(atendimento).build());

            animalService.deleteAnimal(rex.getAnimalId());

            assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
        }
    }

    /**
     * O anexo pertence ao <b>animal</b>, nao a quem fez o upload: num animal que sobrevive
     * porque tem outro tutor, apagar o laudo porque quem o subiu fechou a conta
     * destruiria dado de saude de um animal que continua tendo quem responda por ele.
     */
    @Nested
    @DisplayName("uploader que apaga a conta")
    class UploaderQueSai {

        @Test
        @Transactional
        @DisplayName("desassociar o uploader mantem o anexo")
        void desassociarMantemOAnexo() {
            var salvo = attachmentRepository.saveAndFlush(anexo().build());

            attachmentRepository.desassociarUploader(ulysses.getPersonId());
            attachmentRepository.flush();

            var recarregado = attachmentRepository.findById(salvo.getAttachmentId());

            assertThat(recarregado).isPresent();
            assertThat(recarregado.get().getStorageKey()).isEqualTo(salvo.getStorageKey());
        }

        /** So o uploader informado: desassociar em massa apagaria a autoria de todos. */
        @Test
        @Transactional
        @DisplayName("nao desassocia o anexo de outro tutor")
        void naoDesassociaDeOutro() {
            var maria = personRepository.saveAndFlush(Person.builder()
                    .name("Maria").email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash").build());

            var daMaria = attachmentRepository.saveAndFlush(anexo().uploadedBy(maria).build());

            attachmentRepository.desassociarUploader(ulysses.getPersonId());
            attachmentRepository.flush();

            assertThat(attachmentRepository.findById(daMaria.getAttachmentId()))
                    .isPresent();
        }
    }

}
